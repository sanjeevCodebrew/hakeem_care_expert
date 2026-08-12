package com.consultantvendor.ui.dashboard.zid

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.ZidCheckoutItem
import com.consultantvendor.data.models.responses.ZidCheckoutRequest
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentZidProductsBinding
import com.consultantvendor.data.models.responses.ZidProduct
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.longToast
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ZidFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentZidProductsBinding
    private lateinit var viewModel: ZidViewModel
    private lateinit var adapter: ZidProductsAdapter

    private lateinit var progressDialog: ProgressDialog
    private val items = ArrayList<ZidProduct>()

    private var currentPage = 1
    private var hasMorePages = true
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_zid_products, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, viewModelFactory)[ZidViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        binding.btnCheckout.setOnClickListener {
            if (!isConnectedToInternet(requireContext(), true)) return@setOnClickListener
            val selected = adapter.getSelectedProducts()
            if (selected.isEmpty()) return@setOnClickListener
            val checkoutItems = selected.map { (product, chosenQty) ->
                ZidCheckoutItem(
                    sku = product.sku ?: "",
                    quantity = chosenQty
                )
            }
            viewModel.checkout(
                ZidCheckoutRequest(
                    request_id = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID),
                    products = checkoutItems
                )
            )
        }

        setupRecyclerView()
        setupSearch()
        bindObservers()
        loadPage(1)
    }

    private fun setupRecyclerView() {
        adapter = ZidProductsAdapter(
            items,
            onSelectionChanged = { count ->
                if (count > 0) binding.btnCheckout.visible() else binding.btnCheckout.gone()
            },
            onProductTapped = { product, position ->
                showQuantityDialog(product, position)
            }
        )
        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.layoutManager = layoutManager
        binding.rvProducts.adapter = adapter

        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (!isLoading && hasMorePages && lastVisible >= total - 3) {
                    loadPage(currentPage + 1)
                }
            }
        })
    }

    private fun setupSearch() {
        // Local search — filters the already-loaded list; no API call needed
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.ivClearSearch.isVisible = query.isNotEmpty()
                adapter.filter(query)
                binding.tvEmpty.isVisible = adapter.itemCount == 0
            }
        })
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            adapter.filter("")
            binding.tvEmpty.isVisible = adapter.itemCount == 0
        }
    }

    private fun showQuantityDialog(product: ZidProduct, position: Int) {
        val maxQty = product.quantity ?: 0
        val sku = product.sku ?: return
        if (maxQty <= 1 || product.is_infinite == true) {
            adapter.setProductSelected(sku, 1, position)
            return
        }
        val quantities = Array(maxQty) { "${it + 1}" }
        var checkedIndex = 0
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_quantity))
            .setSingleChoiceItems(quantities, 0) { _, which -> checkedIndex = which }
            .setPositiveButton(R.string.done) { _, _ ->
                adapter.setProductSelected(sku, checkedIndex + 1, position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadPage(page: Int) {
        isLoading = true
        currentPage = page
        viewModel.getZidProducts(page)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        viewModel.zidProducts.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    isLoading = false
                    val page = it.data?.data
                    val newItems = page?.results ?: emptyList()
                    hasMorePages = page?.next != null

                    if (currentPage == 1) items.clear()
                    items.addAll(newItems)
                    // Submit to adapter so local search has the full list
                    adapter.submitAllItems(items)
                    // Re-apply current search filter
                    val q = binding.etSearch.text?.toString() ?: ""
                    if (q.isNotBlank()) adapter.filter(q)

                    if (adapter.itemCount == 0) binding.tvEmpty.visible() else binding.tvEmpty.gone()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    isLoading = false
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                    binding.tvEmpty.gone()
                }
            }
        })

        viewModel.zidCheckout.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    val checkoutUrl = it.data?.checkout_url
                    if (checkoutUrl.isNullOrBlank()) {
                        requireActivity().longToast(it.data?.message ?: getString(R.string.checkout))
                    } else {
//                        showCheckoutUrlDialog(checkoutUrl)
                        requireActivity().longToast(it.data.message ?: getString(R.string.checkout))
                    }
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })
    }

    private fun showCheckoutUrlDialog(checkoutUrl: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.checkout)
            .setMessage(checkoutUrl)
            .setPositiveButton(R.string.share) { _, _ ->
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, checkoutUrl)
                        },
                        getString(R.string.share)
                    )
                )
            }
            .setNeutralButton(R.string.copy) { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("checkout_url", checkoutUrl))
                requireActivity().longToast(getString(R.string.link_copied))
            }
            .setNegativeButton(R.string.done, null)
            .show()
    }
}
