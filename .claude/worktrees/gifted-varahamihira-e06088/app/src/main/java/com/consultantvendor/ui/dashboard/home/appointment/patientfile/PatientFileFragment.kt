package com.consultantvendor.ui.dashboard.home.appointment.patientfile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.databinding.FragmentPatientFileBinding
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.getAge
import com.consultantvendor.utils.loadImage
import dagger.android.support.DaggerFragment

class PatientFileFragment : DaggerFragment() {

    private lateinit var binding: FragmentPatientFileBinding

    private var rootView: View? = null

    private var request: Request? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_patient_file,
                container,
                false
            )
            rootView = binding.root

            request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as? Request

            initialise()
            listeners()
        }
        return rootView
    }

    private fun initialise() {
        val user = request?.from_user ?: return

        loadImage(binding.ivPic, user.profile_image, R.drawable.ic_profile_placeholder)

        binding.tvName.text = user.name ?: ""
        binding.tvPatientId.text = user.id ?: ""

        val profile = user.profile
        val gender = profile?.gender ?: ""
        val dob = profile?.dob ?: ""
        val country = profile?.country ?: ""

        binding.tvGenderBasic.text = gender
        binding.tvDobBasic.text = dob
        binding.tvAge.text = if (dob.isNotEmpty()) getAge(dob).toString() else ""
        binding.tvGender.text = gender
        binding.tvDob.text = dob
        binding.tvNationality.text = country
        binding.tvPhone.text = user.phone ?: ""
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvEditPatientFile.setOnClickListener {
            // Edit patient file functionality can be added here
        }

        binding.clAddAttachment.setOnClickListener {
            // Add attachment functionality can be added here
        }
    }
}
