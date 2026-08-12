package com.consultantvendor.ui.dashboard.home.appointment.patientfile

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentVitalSignBinding
import com.consultantvendor.ui.dashboard.home.AppointmentViewModel
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.getAge
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class VitalSignFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentVitalSignBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var viewModel: AppointmentViewModel

    private var rootView: View? = null
    private var request: Request? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vital_sign, container, false)
            rootView = binding.root

            request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as? Request

            initialise()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        progressDialog = ProgressDialog(requireActivity())
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]

        val user = request?.from_user ?: return
        loadImage(binding.ivPic, user.profile_image, R.drawable.ic_profile_placeholder)
        binding.tvName.text = user.name ?: ""
        binding.tvPatientId.text = user.id ?: ""
        val profile = user.profile
        val gender = profile?.gender ?: ""
        val dob = profile?.dob ?: ""
        binding.tvGenderDob.text = listOf(gender, dob).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvSave.setOnClickListener {
            if (validate()) {
                hitApiSaveVitalSigns()
            }
        }
    }

    private fun validate(): Boolean {
        val weightStr = binding.etWeight.text.toString().trim()
        if (weightStr.isEmpty()) {
            binding.etWeight.showSnackBar(getString(R.string.please_enter_weight))
            return false
        }
        val weight = weightStr.toDoubleOrNull()
        if (weight == null || weight < 15 || weight > 450) {
            binding.etWeight.showSnackBar(getString(R.string.weight_range_error))
            return false
        }
        if (binding.etHeight.text.toString().trim().isEmpty()) {
            binding.etHeight.showSnackBar(getString(R.string.please_enter_height))
            return false
        }
        if (binding.etTemperature.text.toString().trim().isEmpty()) {
            binding.etTemperature.showSnackBar(getString(R.string.please_enter_temperature))
            return false
        }
        if (binding.etPulse.text.toString().trim().isEmpty()) {
            binding.etPulse.showSnackBar(getString(R.string.please_enter_pulse))
            return false
        }
        if (binding.etRespiratoryRate.text.toString().trim().isEmpty()) {
            binding.etRespiratoryRate.showSnackBar(getString(R.string.please_enter_respiratory_rate))
            return false
        }
        return true
    }

    private fun hitApiSaveVitalSigns() {
        if (!isConnectedToInternet(requireContext(), true)) return
        val hashMap = HashMap<String, String>()
        hashMap["request_id"] = request?.id ?: ""
        hashMap["weight"] = binding.etWeight.text.toString().trim()
        hashMap["height"] = binding.etHeight.text.toString().trim()
        hashMap["temperature"] = binding.etTemperature.text.toString().trim()
        hashMap["pulse"] = binding.etPulse.text.toString().trim()
        hashMap["respiratory_rate"] = binding.etRespiratoryRate.text.toString().trim()
        viewModel.saveVitalSigns(hashMap)
    }

    private fun bindObservers() {
        viewModel.saveVitalSigns.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.gone()
                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()
                }
                Status.ERROR -> {
                    binding.clLoader.root.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.root.visible()
                }
            }
        })
    }

}
