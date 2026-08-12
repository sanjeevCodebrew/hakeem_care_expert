package com.consultantvendor.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.appFeatures
import com.consultantvendor.data.models.PushData
import com.consultantvendor.data.models.responses.UserSession
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityHomeBinding
import com.consultantvendor.ui.calling.Constants
import com.consultantvendor.ui.calling.IncomingCallNotificationService
import com.consultantvendor.ui.chat.chatdetail.ChatDetailActivity
import com.consultantvendor.ui.dashboard.home.appointment.requests.BottomServiceRequestFragment
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.drawermenu.DrawerActivity.Companion.CLASSES
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.ui.loginSignUp.SignUpActivity
import com.consultantvendor.ui.loginSignUp.login.BottomLoginFragment
import com.consultantvendor.utils.AppSocket
import com.consultantvendor.utils.EXTRA_IS_FIRST
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.EXTRA_TAB
import com.consultantvendor.utils.LocaleHelper
import com.consultantvendor.utils.MultiLoginManager
import com.consultantvendor.utils.PAGE_TO_OPEN
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.UPDATE_NUMBER
import com.consultantvendor.utils.USER_DATA
import com.consultantvendor.utils.USER_ID
import com.consultantvendor.utils.USER_NAME
import com.consultantvendor.utils.applyInsets
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.longToast
import com.consultantvendor.utils.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import dagger.android.support.DaggerAppCompatActivity
import java.util.Locale
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import timber.log.Timber



class HomeActivity : DaggerAppCompatActivity() {


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appSocket: AppSocket

    lateinit var binding: ActivityHomeBinding

    private lateinit var viewModel: LoginViewModel

    private var currentNavController: LiveData<NavController>? = null

    lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var geoCoder: Geocoder

    private var isReceiverRegistered = false

    private var isPendingApiProgressing = false

    private var timerPendingApi = Timer()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var progressDialog: ProgressDialog

    private var moh_number = ""

    private lateinit var pushData: PushData

    private var isFromSwitchUser = false

    val homeIntent : Intent?=null


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, com.consultantvendor.R.layout.activity_home)
        setContentView(binding.root)

        applyInsets(binding.root)

        clearBadgeCount(this)
        initialise()
        setNavigation()
        bindObservers()
        getPendingRequest()

    }

    fun clearBadgeCount(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API 26+, use NotificationManager
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }

        // Reset count for Samsung, Xiaomi, etc.
        try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
            intent.putExtra("badge_count", 0)
            intent.putExtra("badge_count_package_name", context.packageName)
            intent.putExtra("badge_count_class_name", getLauncherClassName(context))
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLauncherClassName(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        for (resolveInfo in resolveInfos) {
            val pkgName = resolveInfo.activityInfo.applicationInfo.packageName
            if (pkgName.equals(context.packageName, ignoreCase = true)) {
                return resolveInfo.activityInfo.name
            }
        }

        return null
    }


     fun openBottomPreviousLogin() {
        val users = MultiLoginManager.getUsers(this)
        if (users.isNotEmpty()) {
            val fragment = BottomLoginFragment()
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }

    fun hitApiLogin(moh_number: String) {
        val hashMap = HashMap<String, Any>()
        hashMap["moh_number"] = moh_number
        viewModel.drLogin(hashMap)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        progressDialog = ProgressDialog(this)

        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)
        appSocket.init()

        if (intent.hasExtra("comeFrom")
            && intent.getStringExtra("comeFrom") == "switchUser"
        ) {
            moh_number = intent.getStringExtra("moh_number").toString()
            pushData = intent.getSerializableExtra(Constants.INCOMING_CALL_INVITE) as PushData
            isFromSwitchUser = true
            hitApiLogin(moh_number)
        }
        // Android 13 post notification permission
        val permissionState =
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        // If the permission is not granted, request it.
        // If the permission is not granted, request it.
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        Timber.d(userRepository.getUser()?.token ?: "")

        /* Fetch Notification Token */
        userRepository.pushTokenUpdate()

        if (appFeatures.needLocation) {
            /*Ask for location*/
            geoCoder = Geocoder(this, Locale.getDefault())
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            getLastLocation()
        }

        /*Ask for phone number if not added*/
        if (BuildConfig.FLAVOR == "homeDoctor" && userRepository.getUser()?.phone.isNullOrEmpty()) {
            startActivity(
                Intent(this, SignUpActivity::class.java)
                    .putExtra(UPDATE_NUMBER, true)
            )
        }

      /*  firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            val params = Bundle()
            params.putString("dev_name", "Zorawar")
            params.putString("dev_description", "Quality Tester")
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "dev_test")
        }*/

    }

    private fun checkPendingRequest() {
        val timeDelay = 60000L

        timerPendingApi = fixedRateTimer("timerMessage", true, 2000L, timeDelay) {
            getPendingRequest()
        }
    }


    private fun setNavigation() {
        val navGraphIds = when (BuildConfig.FLAVOR) {
            "homeDoctor", "airdoc", "nurseLynx", "meetMd" ->
                listOf(
                    R.navigation.navigation_appointment,
                    R.navigation.navigation_wallet,
                    R.navigation.navigation_revenue,
                    R.navigation.navigation_profile
                )
            else ->
                listOf(
                    R.navigation.navigation_home,
                    R.navigation.navigation_wallet,
                    R.navigation.navigation_revenue,
                    R.navigation.navigation_profile
                )
        }

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.bottomNav.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )

        currentNavController = controller

        if (intent.hasExtra(EXTRA_TAB)) {
            if (intent.getStringExtra(EXTRA_TAB) == "1") {
                binding.bottomNav.selectedItemId = R.id.navigation_wallet
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        getLocationName(location.latitude, location.longitude)
                    }
                }
                requestNewLocationData()

            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                if (this.packageName.equals(BuildConfig.APPLICATION_ID))
                    startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        runOnUiThread {
            val mLocationRequest = LocationRequest.create().apply {
                interval = 0
                fastestInterval = 0
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                numUpdates = 1
            }

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
            )
        }
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            getLocationName(mLastLocation?.latitude ?: 0.0, mLastLocation?.longitude ?: 0.0)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        registerActivityResult.launch(
            Intent(this, DrawerActivity::class.java)
                .putExtra(PAGE_TO_OPEN, DrawerActivity.LOCATION)
        )
    }


    private val registerActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                //val intent = result.data
                getLastLocation()
            }
        }

    private fun getLocationName(lat: Double, lng: Double) {
        runOnUiThread {
            try {
                var locationName = ""

                val addresses = geoCoder.getFromLocation(
                    lat, lng, 1
                ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                if (addresses != null) {
                    if (addresses.isNotEmpty()) {
                        locationName = when {
                            addresses[0].getAddressLine(1) != null -> addresses[0].getAddressLine(
                                1
                            )

                            addresses[0].featureName == null -> addresses[0].adminArea
                            else -> String.format(
                                "%s, %s", addresses[0].featureName,
                                addresses[0].locality
                            )
                        }
                    }
                }
                // binding.itemMain.tvLocation.text = name

                val hashMap = HashMap<String, Any>()
                hashMap["name"] = userRepository.getUser()?.name ?: ""
                hashMap["location_name"] = locationName
                hashMap["lat"] = lat
                hashMap["long"] = lng
                viewModel.updateProfile(hashMap)
            }
            catch (e: Exception) {

            }
        }
    }

    private fun bindObservers() {
        viewModel.pendingRequests.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    isPendingApiProgressing = false
                    /*All Pending request*/
                    it.data?.pending_requests?.forEach {
                        runOnUiThread {
                            val fragment = BottomServiceRequestFragment(it)
                            fragment.show(supportFragmentManager, fragment.tag)
                        }
                    }
                }

                Status.ERROR -> {
                    isPendingApiProgressing = false
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }

                Status.LOADING -> {

                }
            }
        })

        viewModel.drLogin.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    prefsManager.save(USER_DATA, it.data)
                    if (userRepository.isUserLoggedIn()) {
                        if (!isFromSwitchUser) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            longToast("${getString(R.string.switch_to)} ${it.data?.name}")
                            val loggedInUser = UserSession(
                                userId = it.data?.id.toString(),
                                moh = it.data?.moh_number.toString(),
                                token = it.data?.token.toString(),
                                username = it.data?.name.toString(),
                                isSelect = true,
                                profileImageUrl = it.data?.profile_image.toString()
                            )
                            MultiLoginManager.saveUser(this, loggedInUser)
                        }
                        else
                        {
                            startActivity(Intent(this, HomeActivity::class.java))
                            longToast("${getString(R.string.switch_to)} ${it.data?.name}")

                            val loggedInUser = UserSession(
                                userId = it.data?.id.toString(),
                                moh = it.data?.moh_number.toString(),
                                token = it.data?.token.toString(),
                                username = it.data?.name.toString(),
                                isSelect = true,
                                profileImageUrl = it.data?.profile_image.toString())

                            MultiLoginManager.saveUser(this, loggedInUser)

                            when (pushData.pushType) {
                                PushType.CHAT -> {
                                    title = pushData.senderName
                                    intent = Intent(this, ChatDetailActivity::class.java)
                                        .putExtra(USER_ID, pushData.senderId)
                                        .putExtra(USER_NAME, pushData.senderName)
                                        .putExtra(EXTRA_IS_FIRST, true)
                                        .putExtra(EXTRA_REQUEST_ID, pushData.request_id)
                                }

                                PushType.FREE_EXPERT_ADVISE -> {
                                    intent = Intent(this, DrawerActivity::class.java)
                                        .putExtra(PAGE_TO_OPEN, DrawerActivity.QUESTION_DETAILS)
                                        .putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    val broadcastIntent = Intent()
                                    broadcastIntent.action = pushData.pushType
                                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                                }

                                PushType.PROFILE_APPROVED -> {

                                    val broadcastIntent = Intent()
                                    broadcastIntent.action = pushData.pushType
                                    broadcastIntent.putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                                }

                                PushType.NEW_REQUEST, PushType.REQUEST_FAILED, PushType.REQUEST_COMPLETED, PushType.PATIENT_ADDED_SYMPTOMS,
                                PushType.CANCELED_REQUEST, PushType.RESCHEDULED_REQUEST, PushType.UPCOMING_APPOINTMENT,
                                PushType.PAID_EXTRA_PAYMENT -> {
                                    intent = Intent(this, DrawerActivity::class.java)
                                        .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT_DETAILS)
                                        .putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    val broadcastIntent = Intent()
                                    broadcastIntent.action = pushData.pushType
                                    broadcastIntent.putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                                }

                                PushType.BOOKING_REQUEST -> {
                                    val broadcastIntent = Intent()
                                    broadcastIntent.action = pushData.pushType
                                    broadcastIntent.putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                                }

                                PushType.AMOUNT_RECEIVED, PushType.PAYOUT_PROCESSED, PushType.PAYOUT_FAILED,
                                PushType.BALANCE_ADDED, PushType.BALANCE_FAILED -> {
                                homeIntent?.putExtra(EXTRA_TAB, "1")

                                    val broadcastIntent = Intent()
                                    broadcastIntent.action = pushData.pushType
                                    broadcastIntent.putExtra(EXTRA_REQUEST_ID, pushData.request_id)

                                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                                }

                                PushType.ASSINGED_USER -> {
                                    intent = Intent(this, DrawerActivity::class.java)
                                        .putExtra(PAGE_TO_OPEN, CLASSES)
                                }
                                PushType.CALL_ACCEPTED -> {
                                    val callIntent = Intent(this, IncomingCallNotificationService::class.java)
                                    callIntent.action = Constants.ACTION_ACCEPT
                                    callIntent.putExtra(Constants.INCOMING_CALL_INVITE, pushData)

                                    startService(callIntent)

                                }
                                PushType.CALL_CANCELED -> {
                                    handleCanceledCallInvite(pushData)
                                }
                            }
                        }

                    }
                }

                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }

                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })
    }

    private fun handleCanceledCallInvite(pushData: PushData) {

        val intent = Intent(this, IncomingCallNotificationService::class.java)
        intent.action = Constants.ACTION_CANCEL_CALL
        intent.putExtra(Constants.INCOMING_CALL_INVITE, pushData)
        intent.putExtra(EXTRA_REQUEST_ID, pushData.call_id)
        startService(intent)

    }


    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
        timerPendingApi.cancel()
    }

    private fun registerReceiver() {

        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.BOOKING_REQUEST)
            LocalBroadcastManager.getInstance(this).registerReceiver(refreshData, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshData)
            isReceiverRegistered = false
        }
    }

    private val refreshData = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.BOOKING_REQUEST -> {
                    try {
                        getPendingRequest()
                    } catch (e: Exception) {

                    }
                }
            }
        }
    }

    private fun getPendingRequest() {
        runOnUiThread {
            if (BuildConfig.FLAVOR == "nurseLynx" && isConnectedToInternet(this, false)) {
                if (!isPendingApiProgressing) {
                    isPendingApiProgressing = true
                    viewModel.pendingRequests()
                }
            }
        }
    }

}
