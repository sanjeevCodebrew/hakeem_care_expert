package com.consultantvendor.ui.dashboard.location

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.databinding.FragmentLocationBinding
import com.consultantvendor.utils.PermissionUtils
import com.consultantvendor.utils.PermissionUtils.hasPermissions
import com.consultantvendor.utils.PermissionUtils.locationPermission
import com.consultantvendor.utils.PrefsManager
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class LocationFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentLocationBinding

    private var rootView: View? = null

    private val onPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.any { !it }) {
                PermissionUtils.showAppSettingsDialog(
                    requireContext(), R.string.we_will_need_your_location
                )
                return@registerForActivityResult
            }
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_location, container, false)
            rootView = binding.root

            listeners()
        }
        return rootView
    }

    private fun listeners() {
        binding.tvSkip.setOnClickListener {
            requireActivity().finish()
        }

        binding.tvUseLocation.setOnClickListener {
            if (hasPermissions(locationPermission)) {
                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            } else {
                onPermissionLauncher.launch(locationPermission)
            }
        }

    }

    /*private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()

        } else {
            getLocationWithPermissionCheck()
        }
    }*/


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        onRequestPermissionsResult(requestCode, grantResults)
    }

    /*@NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    fun getLocation() {
        checkPermissions()
    }

    @OnShowRationale(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(requireContext(), R.string.we_will_need_your_location, request)
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
            requireContext(), R.string.we_will_need_your_location
        )
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
            requireContext(), R.string.we_will_need_your_location
        )
    }*/
}
