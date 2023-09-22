package com.consultantvendor.ui.dashboard.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.appFeatures
import com.consultantvendor.data.models.responses.*
import com.consultantvendor.data.network.ApiKeys
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentHomeBinding
import com.consultantvendor.ui.adapter.CommonFragmentPagerAdapter
import com.consultantvendor.ui.calling.CallingActivity
import com.consultantvendor.ui.chat.chatdetail.ChatDetailActivity
import com.consultantvendor.ui.dashboard.feeds.FeedViewModel
import com.consultantvendor.ui.dashboard.home.appointment.appointmentStatus.AppointmentStatusActivity
import com.consultantvendor.ui.dashboard.home.items.ArticleAdapter
import com.consultantvendor.ui.dashboard.home.items.HealthToolsAdapter
import com.consultantvendor.ui.dashboard.success.NetworkIssueFragment
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.ui.loginSignUp.welcome.BannerFragment
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.item_no_data.view.*
import kotlinx.android.synthetic.main.nav_header_home.view.*
import java.util.*
import javax.inject.Inject


class HomeFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentHomeBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModelHome: HomeViewModel

    private lateinit var viewModel: AppointmentViewModel

    private lateinit var viewModelLogin: LoginViewModel

    private lateinit var viewModelFeed: FeedViewModel

    private var items = ArrayList<Request>()

    private lateinit var adapter: AppointmentAdapter

    private var requestItem: Request? = null

    private var isReceiverRegistered = false

    private var itemsArticle = ArrayList<Feed>()

    private var itemsBlogs = ArrayList<Feed>()

    private lateinit var adapterArticle: ArticleAdapter

    private lateinit var adapterBlogs: ArticleAdapter

    private var itemsService = ArrayList<Service>()

    private lateinit var serviceAdapter: AppointmentServiceAdapter

    private var serviceId = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setAdapter()
            bindObservers()
        }
        return rootView
    }


    private fun initialise() {
        progressDialog = ProgressDialog(requireActivity())
        viewModelHome = ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]
        viewModelLogin = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        viewModelFeed = ViewModelProvider(this, viewModelFactory)[FeedViewModel::class.java]

        //throw RuntimeException("Test Crash") // Force a crash

        /*Side Drawer*/
        if (appFeatures.needSideDrawer) {
//            binding.ivDrawer.visible()
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            handleHeader()
        } else {
//            binding.ivDrawer.gone()
//            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        Log.e("TAG", "authToken "+prefsManager.getObject(USER_DATA, UserData::class.java)?.token)

//        binding.ivNotification.visible()
//        binding.tvUnreadCount.visible()
    }

    private fun handleHeader() {
        val userData = userRepository.getUser()
        val headerView = binding.navView.getHeaderView(0)
// set User Name
        headerView.tvName.text = userData?.name
        loadImage(headerView.ivPic, userData?.profile_image, R.drawable.ic_profile_placeholder)

        headerView.ivPic.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.PROFILE))
        }
    }

    private fun setAdapter() {
        adapter = AppointmentAdapter(this, items)
        binding.rvListing.adapter = adapter

        binding.clLoader.setBackgroundResource(R.color.colorWhite)
        hitApi()

        adapterArticle = ArticleAdapter(this, itemsArticle)
        binding.rvArticle.adapter = adapterArticle

        adapterBlogs = ArticleAdapter(this, itemsBlogs)
        binding.rvBlogs.adapter = adapterBlogs

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

        /*Only if Health tools needed*/
        if (appFeatures.needHealthTools) {
            binding.tvHealthTools.visible()
            binding.rvHealthTools.visible()

            val itemsHealth = arrayListOf(getString(R.string.health_tool_1), getString(R.string.health_tool_2),
                    getString(R.string.health_tool_3), getString(R.string.health_tool_4))
            val adapterHealth = HealthToolsAdapter(this, itemsHealth)
            binding.rvHealthTools.adapter = adapterHealth
        }
    }

    fun onServiceSelected(item: Service) {
        serviceId = item.service_id ?: ""
        binding.clLoader.visible()

        hitRequestApi()
    }

    fun clickFavourite(item: Feed) {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["favorite"] = if (item.is_favorite == true) "0" else "1"
            viewModelFeed.addFavorite(item.id ?: "", hashMap)
        }
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            /*Home*/
            viewModelHome.home()

            if (BuildConfig.FLAVOR=="taradoc"){
                viewModelHome.banners()
            }else if (BuildConfig.FLAVOR=="nurseLynx"){
                viewModelHome.banners()
            }
            hitRequestApi()
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun hitRequestApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap[ApiKeys.PER_PAGE] = "5"
            hashMap["service_type"] = CallType.ALL

            if (serviceId.isNotEmpty())
                hashMap["service_id"] = serviceId
            viewModel.request(hashMap)
        }
    }

    private fun listeners() {
//        binding.ivDrawer.setOnClickListener {
//            binding.drawerLayout.openDrawer(GravityCompat.START)
//        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi()
        }

        binding.tvMoreAppointment.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT), AppRequestCode.APPOINTMENT_UPDATE)
        }

        binding.tvMoreArticles.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, BlogType.ARTICLE), AppRequestCode.ARTICLE_CHANGES)
        }

        binding.tvMoreBlogs.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, BlogType.BLOG), AppRequestCode.ARTICLE_CHANGES)
        }

        binding.tvPostArticles.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.ADD_ARTICLE), AppRequestCode.ARTICLE_CHANGES)
        }

        binding.tvPostBlogs.setOnClickListener {
            startActivityForResult(Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.ADD_BLOG), AppRequestCode.ARTICLE_CHANGES)
        }

    }

    private fun bindObservers() {
        viewModel.pendingRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.swipeRefresh.isRefreshing = false
                    binding.clLoader.gone()

                    items.clear()
                    items.addAll(it.data?.requests ?: emptyList())

                    adapter.notifyDataSetChanged()
                    adapter.setAllItemsLoaded(true)

                    binding.clNoData.hideShowView(items.isEmpty())
                    binding.clLoader.setBackgroundResource(0)

                    if (it.data?.isAprroved == false) {
                        binding.clNoData.visible()
                        binding.clNoData.setBackgroundResource(R.color.colorWhite)
                        binding.clNoData.hideShowView(true)
                        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_profile_empty_state)
                        binding.clNoData.tvNoData.text = getString(R.string.profile_unapproved)
                        binding.clNoData.tvNoDataDesc.text = getString(R.string.profile_unapproved_desc)
                    } else {
                        binding.clNoData.gone()

                        binding.clNoDataAppointment.hideShowView(items.isEmpty())
                        binding.clNoDataAppointment.tvNoData.text = getString(R.string.no_requests)
                        binding.clNoDataAppointment.tvNoDataDesc.text = getString(R.string.no_requests_desc)

                        binding.tvMoreAppointment.hideShowView(items.size >= 5)
                    }
                }
                Status.ERROR -> {
                    adapter.setAllItemsLoaded(true)

                    binding.clLoader.gone()
                    binding.swipeRefresh.isRefreshing = false
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    if (!binding.swipeRefresh.isRefreshing)
                        binding.clLoader.visible()
                }
            }
        })

        viewModelHome.home.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    if (appFeatures.needArticles) {
                        binding.tvArticles.visible()
                        binding.tvMoreArticles.visible()
                        binding.tvPostArticles.visible()
                        itemsArticle.clear()
                        itemsArticle.addAll(it.data?.top_articles ?: emptyList())
                        adapterArticle.notifyDataSetChanged()

                        binding.tvMoreArticles.hideShowView(itemsArticle.size >= 4)

                        binding.clNoArticle.tvNoData.text = getString(R.string.no_article)
                        binding.clNoArticle.tvNoDataDesc.text = getString(R.string.no_article_desc)
                        binding.clNoArticle.hideShowView(itemsArticle.isEmpty())
                    }

                    if (appFeatures.needBlogs) {
                        binding.tvBlogs.visible()
                        binding.tvMoreBlogs.visible()
                        binding.tvPostBlogs.visible()

                        itemsBlogs.clear()
                        itemsBlogs.addAll(it.data?.top_blogs ?: emptyList())
                        adapterBlogs.notifyDataSetChanged()

                        binding.tvMoreBlogs.hideShowView(itemsBlogs.size >= 4)

                        binding.clNoBlog.tvNoData.text = getString(R.string.no_blog)
                        binding.clNoBlog.tvNoDataDesc.text = getString(R.string.no_blog_desc)
                        binding.clNoBlog.hideShowView(itemsBlogs.isEmpty())
                    }

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })

        viewModel.acceptRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    hitApi()
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
                    hitApi()

                    when (requestItem?.main_service_type) {
                        ConsultType.CHAT -> {
                            requireActivity().longToast(getString(R.string.starting_chat))

                            startActivity(Intent(requireActivity(), ChatDetailActivity::class.java)
                                    .putExtra(USER_ID, requestItem?.from_user?.id)
                                    .putExtra(USER_NAME, requestItem?.from_user?.name)
                                    .putExtra(EXTRA_REQUEST_ID, requestItem?.id)
                                    .putExtra(EXTRA_IS_FIRST, true)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        }
                        ConsultType.AUDIO_CALL, ConsultType.VIDEO_CALL -> {
                            requireActivity().longToast(getString(R.string.starting_call))

                            startActivity(Intent(requireContext(), CallingActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(EXTRA_REQUEST_ID, requestItem))
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

                    hitApi()
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
                    hitApi()

                    if (requestItem?.status != CallAction.START_SERVICE) {
                        requestItem?.status = CallAction.START
                        startActivityForResult(Intent(requireActivity(), AppointmentStatusActivity::class.java)
                                .putExtra(EXTRA_REQUEST_ID, requestItem), AppRequestCode.APPOINTMENT_DETAILS)
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

        viewModelLogin.logout.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    logoutUser(requireActivity(), prefsManager)
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

        viewModelFeed.addFavorite.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    if (isConnectedToInternet(requireContext(), true)) {
                        progressDialog.setLoading(true)
                        viewModelHome.home()
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


        viewModelHome.banners.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    val itemsBanner = ArrayList<Banner>()
                    itemsBanner.addAll(it.data?.banners ?: emptyList())

                    val adapter = CommonFragmentPagerAdapter(childFragmentManager)
                    itemsBanner.forEach {
                        adapter.addTab("", HomeBannerFragment(this, it))
                    }
                    binding.viewPagerBanner.adapter = adapter
                    binding.pageIndicatorView.setViewPager(binding.viewPagerBanner)

                    binding.viewPagerBanner.hideShowView(itemsBanner.isNotEmpty())
                    binding.pageIndicatorView.hideShowView(itemsBanner.size > 1)

                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })

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
                startActivityForResult(Intent(requireActivity(), AppointmentStatusActivity::class.java)
                        .putExtra(EXTRA_REQUEST_ID, request), AppRequestCode.APPOINTMENT_DETAILS)
            }
            CallAction.START_SERVICE -> {
                showMarkCompleteDialog()
            }
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
            val hashMap = HashMap<String, Any>()
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

    override fun onResume() {
        super.onResume()
        registerReceiver()
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
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshData, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshData)
            isReceiverRegistered = false
        }
    }

    private val refreshData = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.REQUEST_COMPLETED, PushType.COMPLETED, PushType.NEW_REQUEST, PushType.CANCELED_REQUEST,
                PushType.REQUEST_FAILED, PushType.RESCHEDULED_REQUEST, PushType.PROFILE_APPROVED,
                NetworkIssueFragment.NETWORK_ISSUE -> {
                    hitApi()
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.APPOINTMENT_UPDATE -> {
                    hitApi()
                }
                AppRequestCode.ARTICLE_CHANGES -> {
                    progressDialog.setLoading(true)
                    viewModelHome.home()
                }
                AppRequestCode.ADD_PRESCRIPTION -> {
                    hitApi()
                }
                AppRequestCode.APPOINTMENT_DETAILS -> {
                    hitRequestApi()
                }
            }
        }
    }
}




