package com.consultantvendor.ui.dashboard.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.models.responses.Service
import com.consultantvendor.data.network.ApiKeys.AFTER
import com.consultantvendor.data.network.ApiKeys.PER_PAGE
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PER_PAGE_LOAD
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentAppointmentBinding
import com.consultantvendor.ui.calling.CallingActivity
import com.consultantvendor.ui.chat.chatdetail.ChatDetailActivity
import com.consultantvendor.ui.dashboard.home.appointment.appointmentStatus.AppointmentStatusActivity
import com.consultantvendor.ui.dashboard.settings.contactlist.ContactViewModel
import com.consultantvendor.ui.dashboard.success.NetworkIssueFragment
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.drawermenu.DrawerActivity.Companion.NOTIFICATION
import com.consultantvendor.utils.AlertDialogUtil
import com.consultantvendor.utils.AppSocket
import com.consultantvendor.utils.CallAction
import com.consultantvendor.utils.CallType
import com.consultantvendor.utils.ConsultType
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.EXTRA_IS_FIRST
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.OnDateSelected
import com.consultantvendor.utils.PAGE_TO_OPEN
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.RequestStatus
import com.consultantvendor.utils.USER_ID
import com.consultantvendor.utils.USER_NAME
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.getCountFormat
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.hideShowView
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.longToast
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class AppointmentFragment : DaggerFragment(), OnDateSelected {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appSocket: AppSocket

    private lateinit var binding: FragmentAppointmentBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: AppointmentViewModel

    private lateinit var viewModelContact: ContactViewModel

    private var items = ArrayList<Request>()

    private var itemsService = ArrayList<Service>()

    private lateinit var adapter: AppointmentAdapter

    private lateinit var serviceAdapter: AppointmentServiceAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false

    private var requestItem: Request? = null

    private var isReceiverRegistered = false

    var selectedDate = ""

    private var serviceId = ""

    var calendar: Calendar? = null

    private var notification_count: Int? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_appointment, container, false)
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
        binding.clLoader.root.setBackgroundResource(R.color.colorWhite)
        /*Get today date*/
        calendar = Calendar.getInstance(Locale.getDefault())
        val sdf = SimpleDateFormat(DateFormat.MON_DATE_YEAR, Locale.ENGLISH)
        binding.tvDate.text = sdf.format(calendar?.time)
        selectedDate = binding.tvDate.text.toString()

        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        viewModelContact = ViewModelProvider(this, viewModelFactory)[ContactViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        if (!requireActivity().intent.hasExtra(PAGE_TO_OPEN)) {
            binding.toolbar.navigationIcon = null
            binding.toolbar.title = getString(R.string.home)
        } else {
            binding.toolbar.title = getString(R.string.appointments)
        }

        binding.tvRequestType.hideShowView(BuildConfig.FLAVOR == "homeDoctor")
        binding.ivEmergency.hideShowView(BuildConfig.FLAVOR == "nurseLynx")
    }

    private fun setAdapter() {
        adapter = AppointmentAdapter(this, items)
        binding.rvListing.adapter = adapter
        binding.rvListing.itemAnimator = null


        /*Services*/
        itemsService.clear()
        val services = userRepository.getUser()?.services
        if (services?.size ?: 0 > 1) {
            val service = Service()
            service.service_name = getString(R.string.all_requests)
            service.service_id = ""
            service.isSelected = true
            itemsService.add(service)

            itemsService.addAll(services ?: emptyList())
        }

        serviceAdapter = AppointmentServiceAdapter(this, itemsService)
        binding.rvServices.adapter = serviceAdapter

        binding.rvServices.hideShowView(itemsService.isNotEmpty())
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvDate.setOnClickListener {
            DateUtils.openDatePicker(requireActivity(), this, null, null)
        }

        binding.ivRemoveDate.setOnClickListener {
            if (selectedDate.isNotEmpty()) {
                selectedDate = ""
                binding.tvDate.text = selectedDate
                hitApi(true)
            }
        }

        binding.ivEmergency.setOnClickListener {
            if (isConnectedToInternet(requireContext(), true)) {
                AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setTitle(getString(R.string.notify))
                    .setMessage(getString(R.string.notifying_to_contacts))
                    .setPositiveButton(getString(R.string.notify)) { dialog, which ->
                        viewModelContact.sendMessage()
                    }.setNegativeButton(getString(R.string.no)) { dialog, which ->
                    }.show()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            hitApi(true)
        }

        binding.ivNotification.setOnClickListener {
            binding.tvUnreadCount.gone()
            startActivity(
                Intent(requireContext(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, NOTIFICATION)
            )
        }

        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount - 1
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLastPage && lastVisibleItemPosition >= totalItemCount) {
                    hitApi(false)
                }
            }
        })

        binding.tvRequestType.setOnClickListener {
            binding.spnRequestType.performClick()
        }

        binding.spnRequestType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?, position: Int, id: Long
            ) {
                binding.tvRequestType.text = binding.spnRequestType.selectedItem.toString()
                hitApi(true)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }
    }

    private fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(requireContext(), true) && !isLoadingMoreItems) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            val hashMap = HashMap<String, String>()
            if (!isFirstPage && items.isNotEmpty())
                hashMap[AFTER] = items[items.size - 1].id ?: ""

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()

            if (selectedDate.isNotEmpty()) {
                val date = DateUtils.dateFormatForBackend(
                    DateFormat.MON_DATE_YEAR,
                    DateFormat.DATE_FORMAT, selectedDate
                )
                hashMap["date"] = date
            }

            hashMap["service_type"] = CallType.ALL

            if (serviceId.isNotEmpty())
                hashMap["service_id"] = serviceId

            if (binding.spnRequestType.selectedItemPosition > 0)
                hashMap["type"] = when (binding.spnRequestType.selectedItemPosition) {
                    1 -> RequestStatus.NEW
                    2 -> RequestStatus.COMPLETED
                    3 -> RequestStatus.CANCELLED
                    else -> ""
                }

            isLoadingMoreItems = true
            viewModel.request(hashMap)
        } else
            binding.swipeRefreshLayout.isRefreshing = false
    }


    fun onServiceSelected(item: Service) {
        serviceId = item.service_id ?: ""

        hitApi(true)
    }

    private fun checkNotificationCount(count: Int?) {
        if (BuildConfig.FLAVOR == "homeDoctor") {
            notification_count = count
            requireActivity().runOnUiThread {
                binding.tvUnreadCount.hideShowView(notification_count != null && notification_count ?: 0 > 0)
                binding.tvUnreadCount.text = getCountFormat(1, notification_count)
            }
        }
    }

    private fun bindObservers() {
        viewModel.pendingRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.clLoader.root.setBackgroundResource(0)
                    binding.clLoader.root.gone()

                    /*Check Notification count*/
                    notification_count = it.data?.notification_count
                    checkNotificationCount(notification_count)

                    isLoadingMoreItems = false

                    val tempList = it.data?.requests ?: emptyList()
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

                    if (it.data?.isAprroved == false) {
                        binding.clNoData.root.setBackgroundResource(R.color.colorWhite)
                        binding.clNoData.root.hideShowView(true)
                        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_profile_empty_state)
                        binding.clNoData.tvNoData.text = getString(R.string.profile_unapproved)
                        binding.clNoData.tvNoDataDesc.text = getString(R.string.profile_unapproved_desc)
                    } else {
                        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_requests_empty_state)
                        binding.clNoData.tvNoData.text = getString(R.string.no_requests)
                        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_requests_desc)
                    }

                }

                Status.ERROR -> {
                    isLoadingMoreItems = false
                    adapter.setAllItemsLoaded(true)

                    binding.clLoader.root.gone()
                    binding.swipeRefreshLayout.isRefreshing = false
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    if (!binding.swipeRefreshLayout.isRefreshing && isFirstPage)
                        binding.clLoader.root.visible()
                }
            }
        })

        viewModel.acceptRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi(true)
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

        viewModel.startRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi(true)

                    when (requestItem?.main_service_type) {
                        ConsultType.CHAT -> {
                            requireActivity().longToast(getString(R.string.starting_chat))

                            appSocket.init()

                            startActivity(
                                Intent(requireActivity(), ChatDetailActivity::class.java)
                                    .putExtra(USER_ID, requestItem?.from_user?.id)
                                    .putExtra(USER_NAME, requestItem?.from_user?.name)
                                    .putExtra(EXTRA_REQUEST_ID, requestItem?.id)
                                    .putExtra(EXTRA_IS_FIRST, true)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }

                        ConsultType.AUDIO_CALL, ConsultType.VIDEO_CALL -> {
                            requireActivity().longToast(getString(R.string.starting_call))

                            startActivity(
                                Intent(requireContext(), CallingActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(EXTRA_REQUEST_ID, requestItem)
                            )
                        }
                    }
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

        viewModel.cancelRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi(true)
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

        viewModel.callStatus.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    hitApi(true)

                    if (requestItem?.status != CallAction.START_SERVICE) {
                        requestItem?.status = CallAction.START
                        registerActivityResult.launch(
                            Intent(requireActivity(), AppointmentStatusActivity::class.java)
                                .putExtra(EXTRA_REQUEST_ID, requestItem)
                        )
                    }
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

        viewModelContact.sendMessage.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    sentMessageHandle(it.data?.contact_added)
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

    private fun sentMessageHandle(contactAdded: Boolean?) {
        if (contactAdded == true) {
            AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle(getString(R.string.notified))
                .setMessage(getString(R.string.notified_contacts))
                .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                }.show()
        } else if (contactAdded == false) {
            AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle(getString(R.string.alert))
                .setMessage(getString(R.string.no_contact))
                .setPositiveButton(getString(R.string.add_new)) { dialog, which ->
                    startActivity(
                        Intent(requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.CONTACT_LIST)
                    )
                }.setNegativeButton(getString(R.string.no)) { dialog, which ->
                }.show()
        }
    }


    fun proceedRequest(request: Request) {
        requestItem = request
        when (request.status) {
            CallAction.PENDING -> {
                showAcceptRequestDialog()
            }

            CallAction.ACCEPT -> {
                showInitiateRequestDialog()
            }

            CallAction.START, CallAction.REACHED -> {
                registerActivityResult.launch(
                    Intent(requireActivity(), AppointmentStatusActivity::class.java)
                        .putExtra(EXTRA_REQUEST_ID, request)
                )
            }

            CallAction.START_SERVICE -> {
                showMarkCompleteDialog()
            }
        }
    }

    val registerActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //val intent = result.data
            hitApi(true)
        }
    }

    private fun showAcceptRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(), R.string.accept_request,
            R.string.accept_request_message, R.string.accept_request, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiAcceptRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun showMarkCompleteDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(), R.string.mark_complete,
            R.string.mark_complete_message, R.string.mark_complete, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiCompleteRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }


    private fun showInitiateRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(), R.string.start_request,
            R.string.start_request_message, R.string.start_request, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiStartRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun hitApiAcceptRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["request_id"] = requestItem?.id ?: ""

            viewModel.acceptRequest(hashMap)
        }
    }

    private fun hitApiCompleteRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            val hashMap = java.util.HashMap<String, Any>()
            hashMap["request_id"] = requestItem?.id ?: ""
            hashMap["status"] = CallAction.COMPLETED

            viewModel.callStatus(hashMap)
        }
    }

    private fun hitApiStartRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {

            when (requestItem?.main_service_type) {
                ConsultType.HOME_VISIT -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["request_id"] = requestItem?.id ?: ""
                    hashMap["status"] = CallAction.START

                    viewModel.callStatus(hashMap)
                }

                else -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["request_id"] = requestItem?.id ?: ""

                    viewModel.startRequest(hashMap)
                }
            }
        }
    }

    fun cancelAppointment(item: Request) {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(),
            R.string.cancel_appointment,
            R.string.cancel_appointment_msg,
            R.string.cancel_appointment,
            R.string.cancel,
            false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    if (isConnectedToInternet(requireContext(), true)) {
                        val hashMap = HashMap<String, String>()
                        hashMap["request_id"] = item.id ?: ""
                        viewModel.cancelRequest(hashMap)
                    }
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }


    companion object {
        const val EXTRA_NUMBER = "extra number"
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()

        binding.ivNotification.hideShowView(userRepository.isUserLoggedIn() && BuildConfig.FLAVOR == "homeDoctor")
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            intentFilter.addAction(PushType.COMPLETED)
            intentFilter.addAction(PushType.NEW_REQUEST)
            intentFilter.addAction(PushType.CANCELED_REQUEST)
            intentFilter.addAction(PushType.REQUEST_FAILED)
            intentFilter.addAction(PushType.RESCHEDULED_REQUEST)
            intentFilter.addAction(PushType.PROFILE_APPROVED)
            intentFilter.addAction(NetworkIssueFragment.NETWORK_ISSUE)

            /*Notification count*/
            intentFilter.addAction(PushType.AMOUNT_RECEIVED)
            intentFilter.addAction(PushType.PAYOUT_PROCESSED)
            intentFilter.addAction(PushType.PAYOUT_FAILED)
            intentFilter.addAction(PushType.BALANCE_ADDED)
            intentFilter.addAction(PushType.BALANCE_FAILED)
            intentFilter.addAction(PushType.PAID_EXTRA_PAYMENT)
            intentFilter.addAction(PushType.FREE_EXPERT_ADVISE)
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshRequests, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshRequests)
            isReceiverRegistered = false
        }
    }

    private val refreshRequests = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.REQUEST_COMPLETED, PushType.COMPLETED, PushType.NEW_REQUEST, PushType.CANCELED_REQUEST,
                PushType.REQUEST_FAILED, PushType.RESCHEDULED_REQUEST, PushType.PROFILE_APPROVED,
                NetworkIssueFragment.NETWORK_ISSUE -> {
                    hitApi(true)
                }

                PushType.AMOUNT_RECEIVED, PushType.PAYOUT_PROCESSED, PushType.PAYOUT_FAILED, PushType.BALANCE_ADDED,
                PushType.BALANCE_FAILED, PushType.PAID_EXTRA_PAYMENT, PushType.FREE_EXPERT_ADVISE -> {
                    checkNotificationCount((notification_count ?: 0) + 1)
                }
            }
        }
    }

    override fun onDateSelected(date: String) {
        binding.tvDate.text = DateUtils.dateFormatChange(
            DateFormat.MON_DATE_YEAR,
            DateFormat.MON_DATE_YEAR, date
        )

        selectedDate = binding.tvDate.text.toString()

        /*Refresh pages*/
        hitApi(true)
    }

}