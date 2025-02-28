package com.consultantvendor.ui.dashboard.home.appointment.medicalhistory

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.ApiKeys.AFTER
import com.consultantvendor.data.network.ApiKeys.PER_PAGE
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PER_PAGE_LOAD
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentMedicalHistoryBinding
import com.consultantvendor.ui.dashboard.home.appointment.detail.AppointmentDetailsFragment.Companion.MEDICAL_HISTORY
import com.consultantvendor.ui.dashboard.home.questions.QuestionViewModel
import com.consultantvendor.utils.CallAction
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.editTextScroll
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.hideShowView
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class MedicalHistoryFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentMedicalHistoryBinding

    private var rootView: View? = null

    private lateinit var viewModel: QuestionViewModel

    private lateinit var progressDialog: ProgressDialog

    private var items = ArrayList<UserData>()

    private lateinit var adapter: MedicalHistoryAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private lateinit var request: Request


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_medical_history,
                container, false
            )
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
            hitApi(true)
        }
        return rootView
    }


    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[QuestionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        editTextScroll(binding.etComment)
        request = arguments?.getSerializable(EXTRA_REQUEST_ID) as Request

        /*Id appointment accepted or */
        val canAddComment = (request.status != CallAction.PENDING && request.status != CallAction.CANCELED &&
                request.status != CallAction.FAILED)
        binding.tvAdd.hideShowView(request.medical_history_added == false && canAddComment)

        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_requests_empty_state)
        binding.clNoData.tvNoData.text = getString(R.string.no_medical_history)
        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_medical_history_desc)

    }

    private fun setAdapter() {
        adapter = MedicalHistoryAdapter(this, items)
        binding.rvListing.adapter = adapter
        binding.rvListing.itemAnimator = null
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi(true)
        }

        binding.tvAdd.setOnClickListener {
            showAddHistory(true)
            binding.tvAdd.gone()
        }

        binding.tvCancel.setOnClickListener {
            binding.tvAdd.visible()
            showAddHistory(false)
        }

        binding.tvSubmit.setOnClickListener {
            if (binding.etComment.text.toString().isEmpty()) {
                binding.etComment.showSnackBar(getString(R.string.write_patient_medical_condition))
            } else if (isConnectedToInternet(requireContext(), true)) {
                val hashMap = HashMap<String, Any>()
                hashMap["request_id"] = request.id ?: ""
                hashMap["comment"] = binding.etComment.text.toString()
                viewModel.createMedicalHistory(hashMap)
            }
        }


        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount - 1
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMoreItems && !isLastPage && lastVisibleItemPosition >= totalItemCount) {
                    isLoadingMoreItems = true
                    hitApi(false)
                }
            }
        })
    }

    private fun showAddHistory(show: Boolean) {
        binding.etComment.hideShowView(show)
        binding.tvSubmit.hideShowView(show)
        binding.tvCancel.hideShowView(show)
    }


    private fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            val hashMap = HashMap<String, String>()
            if (!isFirstPage && items.isNotEmpty())
                hashMap[AFTER] = items[items.size - 1].id ?: ""

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()

            hashMap["request_id"] = request.id ?: ""
            viewModel.getMedicalHistory(hashMap)
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.getMedicalHistory.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.gone()
                    binding.swipeRefresh.isRefreshing = false

                    isLoadingMoreItems = false

                    val tempList = it.data?.doctors ?: emptyList()
                    if (isFirstPage) {
                        isFirstPage = false
                        items.clear()
                        items.addAll(tempList)

                        adapter.notifyDataSetChanged()
                    } else {
                        val oldSize = items.size
                        items.addAll(tempList)

                        adapter.notifyItemRangeInserted(oldSize, items.size)
                    }
                    isLastPage = tempList.size < PER_PAGE_LOAD
                    adapter.setAllItemsLoaded(isLastPage)

                    binding.clNoData.root.hideShowView(items.isEmpty())
                }

                Status.ERROR -> {
                    isLoadingMoreItems = false
                    adapter.setAllItemsLoaded(true)

                    binding.swipeRefresh.isRefreshing = false
                    binding.clLoader.root.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    if (!isLoadingMoreItems && !binding.swipeRefresh.isRefreshing)
                        binding.clLoader.root.visible()
                }
            }
        })

        viewModel.createMedicalHistory.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    showAddHistory(false)
                    hitApi(true)

                    val broadcastIntent = Intent()
                    broadcastIntent.action = MEDICAL_HISTORY

                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(broadcastIntent)
                }

                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })
    }


}