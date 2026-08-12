package com.consultantvendor.ui.dashboard.home.appointment.patientfile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.databinding.FragmentPatientFileBinding
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PAGE_TO_OPEN
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

        val profile = user.profile
        val age = if (!profile?.dob.isNullOrEmpty()) getAge(profile?.dob).toString() else ""
        val country = profile?.country ?: ""

        binding.tvAge.text = if (age.isNotEmpty()) "${getString(R.string.age)} $age" else ""
        binding.tvNationality.text = if (country.isNotEmpty()) "${getString(R.string.nationality)}: $country" else ""
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.clDoctorNotes.setOnClickListener {
            requireActivity().startActivity(
                Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.DOCTOR_NOTES)
                    .putExtra(EXTRA_REQUEST_ID, request)
            )
        }

        binding.clVitalSign.setOnClickListener {
            requireActivity().startActivity(
                Intent(requireActivity(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, DrawerActivity.VITAL_SIGN)
                    .putExtra(EXTRA_REQUEST_ID, request)
            )
        }
    }
}
