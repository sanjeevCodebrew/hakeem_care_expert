package com.consultantvendor.ui.dashboard.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.SaveAddress
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityAddAddressBinding
import com.consultantvendor.utils.*
import com.consultantvendor.utils.PermissionUtils
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.widget.Autocomplete
import dagger.android.support.DaggerAppCompatActivity
import permissions.dispatcher.*
import java.util.*
import javax.inject.Inject


@RuntimePermissions
class AddAddressActivity : DaggerAppCompatActivity(), GoogleMap.OnCameraChangeListener, OnMapReadyCallback {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: ActivityAddAddressBinding

    private var saveAddress = SaveAddress()

    private var mapFragment: SupportMapFragment? = null

    private var isPlacePicker = false

    private var mMap: GoogleMap? = null

    private lateinit var geoCoder: Geocoder

    lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_address)

        setEditAddress()
        initialise()
        setListeners()
    }

    private fun initialise() {
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment?.getMapAsync(this)

        geoCoder = Geocoder(this, Locale.getDefault())
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLocationWithPermissionCheck()
    }

    private fun setEditAddress() {
        if (intent.hasExtra(EXTRA_ADDRESS)) {
            saveAddress = intent.getSerializableExtra(EXTRA_ADDRESS) as SaveAddress
            saveAddress.addressId = saveAddress._id
            binding.etLocation.setText(saveAddress.locationName)
            isPlacePicker = true
        }
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.tvChange.setOnClickListener {
            placePicker(null, this)
        }

        binding.btnSave.setOnClickListener {
            checkValidations()
        }

        binding.transparentImage.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disallow ScrollView to intercept touch events.
                    binding.scrollMap.requestDisallowInterceptTouchEvent(true)
                    // Disable touch on transparent view
                    false
                }

                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events.
                    binding.scrollMap.requestDisallowInterceptTouchEvent(false)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    binding.scrollMap.requestDisallowInterceptTouchEvent(true)
                    false
                }

                else -> true
            }
        }
    }

    private fun checkValidations() {
        binding.btnSave.hideKeyboard()
        when {
            binding.etLocation.text.toString().isEmpty() -> {
                binding.etLocation.showSnackBar(getString(R.string.address))
            }
            else -> {
                val intent = Intent()
                intent.putExtra(EXTRA_ADDRESS, saveAddress)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppRequestCode.AUTOCOMPLETE_REQUEST_CODE) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                binding.etLocation.setText(getAddress(place))

                saveAddress.locationName = binding.etLocation.text.toString()

                saveAddress.long = place.latLng?.longitude ?: 0.0
                saveAddress.lat = place.latLng?.latitude ?: 0.0

                isPlacePicker = true
                mMap?.moveCamera(CameraUpdateFactory.newLatLng(place.latLng))
                mMap?.animateCamera(CameraUpdateFactory.zoomTo(15f))

                LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)
            }
        }
    }


    override fun onCameraChange(cameraPosition: CameraPosition) {
        if (!isPlacePicker) {
            val latLng = mMap?.cameraPosition?.target

            saveAddress.long = latLng?.longitude ?: 0.0
            saveAddress.lat = latLng?.latitude ?: 0.0
            saveAddress.locationName = getAddress()

            binding.etLocation.setText(saveAddress.locationName)
        }
        isPlacePicker = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.isTrafficEnabled = false
        mMap?.setOnCameraChangeListener(this)

        // mMap?.isMyLocationEnabled = true
        mMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (saveAddress.lat != null) {
            binding.etLocation.setText(saveAddress.locationName)
            val current = LatLng(saveAddress.lat ?: 0.0, saveAddress.long ?: 0.0)
            mMap?.moveCamera(CameraUpdateFactory.newLatLng(current))
            mMap?.animateCamera(CameraUpdateFactory.zoomTo(15f))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val mLastLocation: Location? = task.result
                    if (mLastLocation != null) {
                        val latLng = LatLng(mLastLocation.latitude, mLastLocation.longitude)

                        mMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                        mMap?.animateCamera(CameraUpdateFactory.zoomTo(14f))

                        saveAddress.long = latLng.longitude
                        saveAddress.lat = latLng.latitude
                        saveAddress.locationName = getAddress()

                        binding.etLocation.setText(saveAddress.locationName)
                    }
                }
                requestNewLocationData()
            } else {
                Toast.makeText(this, R.string.we_will_need_your_location, Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

                if (this.packageName.equals(BuildConfig.APPLICATION_ID))
                    startActivity(intent)
            }
        } else {
            getLocationWithPermissionCheck()
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
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

            //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                    Looper.myLooper())

        }
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latLng = LatLng(mLastLocation.latitude, mLastLocation.longitude)

            mMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            mMap?.animateCamera(CameraUpdateFactory.zoomTo(14f))

            saveAddress.long = latLng.longitude
            saveAddress.lat = latLng.latitude
            saveAddress.locationName = getAddress()

            binding.etLocation.setText(saveAddress.locationName)
            //placeLatLng = LatLng(30.7457, 76.7332)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)
    }


    private fun getAddress(): String {
        var locationName = ""
        val addresses: List<Address> = geoCoder.getFromLocation(saveAddress.lat ?: 0.0,
            saveAddress.long
                ?: 0.0, 1) as List<Address> // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        if (addresses.isNotEmpty()) {
            locationName = when {
                addresses[0].getAddressLine(0) != null -> addresses[0].getAddressLine(0)
                addresses[0].featureName != null -> addresses[0].featureName
                addresses[0].locality != null -> addresses[0].locality
                else -> addresses[0].adminArea
            }
        }

        return locationName
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getLocation() {
        if (saveAddress.lat == null)
            getLastLocation()
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(this, R.string.we_will_need_your_location, request)
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
                this,
                R.string.we_will_need_your_location)
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
                this, R.string.we_will_need_your_location)
    }

    companion object {
        const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
    }

}
