package com.consultantvendor.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.consultantvendor.data.models.responses.UserSession
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityHomeBinding
import com.consultantvendor.ui.dashboard.home.appointment.requests.BottomServiceRequestFragment
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.ui.loginSignUp.SignUpActivity
import com.consultantvendor.ui.loginSignUp.login.BottomLoginFragment
import com.consultantvendor.utils.AppSocket
import com.consultantvendor.utils.EXTRA_TAB
import com.consultantvendor.utils.LocaleHelper
import com.consultantvendor.utils.MultiLoginManager
import com.consultantvendor.utils.PAGE_TO_OPEN
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.SessionManager
import com.consultantvendor.utils.UPDATE_NUMBER
import com.consultantvendor.utils.isConnectedToInternet
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

    private var sessions = mutableListOf<UserSession>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)

        initialise()

        setNavigation()
        listeners()
        bindObservers()
        getPendingRequest()

        openBottomPreviousLogin()
    }


        private fun openBottomPreviousLogin() {
        val users = SessionManager.getSessions(this)
        if (users.isNotEmpty()) {
            val fragment = BottomLoginFragment(this)
            fragment.show(supportFragmentManager, fragment.tag)
        }
    }


    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)
        appSocket.init()

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

        Log.d("AccessToken", userRepository.getUser()?.token ?: "")

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

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            val params = Bundle()
            params.putString("dev_name", "Zorawar")
            params.putString("dev_description", "Quality Tester")
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "dev_test")
            Log.e("TAG", "chkLogFirebaseAnalytics :" + params)
        }

    }


    private fun listeners() {
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


    private val registerActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
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
            } catch (e: Exception) {
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
