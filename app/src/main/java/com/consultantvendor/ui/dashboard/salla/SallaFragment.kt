package com.consultantvendor.ui.dashboard.salla

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.SallaCheckoutItem
import com.consultantvendor.data.models.responses.SallaCheckoutOption
import com.consultantvendor.data.models.responses.SallaCheckoutRequest
import com.consultantvendor.data.models.responses.SallaProduct
import com.consultantvendor.data.models.responses.SallaProductOption
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentSallaProductsBinding
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.longToast
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class SallaFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentSallaProductsBinding
    private lateinit var viewModel: SallaViewModel
    private lateinit var adapter: SallaProductsAdapter

    private lateinit var progressDialog: ProgressDialog
    private val items = ArrayList<SallaProduct>()

    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false
    private var searchQuery = ""
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_salla_products, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, viewModelFactory)[SallaViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }

        binding.btnCheckout.setOnClickListener {
            if (!isConnectedToInternet(requireContext(), true)) return@setOnClickListener
            val selected = adapter.getSelectedProducts()
            if (selected.isEmpty()) return@setOnClickListener
            val checkoutItems = selected.map { (product, chosenOptions, chosenQty) ->
                SallaCheckoutItem(
                    identifier_type = "id",
                    identifier = product.id ?: 0L,
                    quantity = chosenQty,
                    options = chosenOptions.takeIf { it.isNotEmpty() }
                )
            }
            viewModel.checkout(
                SallaCheckoutRequest(
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

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.ivClearSearch.isVisible = query.isNotEmpty()
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    if (query != searchQuery) {
                        searchQuery = query
                        loadPage(1)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 500)
            }
        })
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchQuery = binding.etSearch.text.toString()
                loadPage(1)
                true
            } else false
        }
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            searchQuery = ""
            loadPage(1)
        }
    }

    private fun setupRecyclerView() {
        adapter = SallaProductsAdapter(
            items,
            onSelectionChanged = { count ->
                if (count > 0) binding.btnCheckout.visible() else binding.btnCheckout.gone()
            },
            onProductTapped = { product, position ->
                val options = product.options?.filter { !it.values.isNullOrEmpty() }
                if (options.isNullOrEmpty()) {
                    showQuantityDialog(product, emptyList(), position)
                } else {
                    showOptionSelectionDialog(product, options, position)
                }
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
                if (!isLoading && currentPage < totalPages && lastVisible >= total - 3) {
                    loadPage(currentPage + 1)
                }
            }
        })
    }
    private fun showOptionSelectionDialog(
        product: SallaProduct,
        options: List<SallaProductOption>,
        position: Int,
        index: Int = 0,
        selected: MutableMap<Long, SallaCheckoutOption> = mutableMapOf()
    ) {
        if (index >= options.size) {
            showQuantityDialog(product, selected.values.toList(), position)
            return
        }
        val option = options[index]
        val values = option.values ?: return
        val valueNames = values.map { it.name ?: "" }.toTypedArray()
        var checkedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle(option.name ?: getString(R.string.select_option))
            .setSingleChoiceItems(valueNames, 0) { _, which -> checkedIndex = which }
            .setPositiveButton(R.string.next_label) { _, _ ->
                val chosenValue = values[checkedIndex]
                selected[option.id!!] = SallaCheckoutOption(
                    id = option.id,
                    value = chosenValue.id.toString()
                )
                showOptionSelectionDialog(product, options, position, index + 1, selected)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showQuantityDialog(
        product: SallaProduct,
        chosenOptions: List<SallaCheckoutOption>,
        position: Int
    ) {
        val maxQty = product.quantity ?: 0
        if (maxQty <= 1) {
            // No meaningful quantity choice — use 1
            adapter.setProductSelected(product.id ?: return, chosenOptions, 1, position)
            return
        }
        val quantities = Array(maxQty) { "${it + 1}" }
        var checkedIndex = 0
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_quantity))
            .setSingleChoiceItems(quantities, 0) { _, which -> checkedIndex = which }
            .setPositiveButton(R.string.done) { _, _ ->
                adapter.setProductSelected(
                    product.id ?: return@setPositiveButton,
                    chosenOptions,
                    checkedIndex + 1,
                    position
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadPage(page: Int) {
        isLoading = true
        viewModel.getSallaProducts(page, searchQuery)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        viewModel.sallaProducts.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    isLoading = false
                    val response = it.data
                    val newItems = response?.data ?: emptyList()
                    val pagination = response?.pagination

                    if (pagination != null) {
                        currentPage = pagination.currentPage ?: 1
                        totalPages = pagination.totalPages ?: 1
                    }

                    if (currentPage == 1) items.clear()
                    items.addAll(newItems)
                    adapter.notifyDataSetChanged()

                    if (items.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
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

        viewModel.sallaCheckout.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    requireActivity().longToast(it.data ?: getString(R.string.checkout))
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })
    }
}
