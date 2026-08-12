package com.consultantvendor.ui.dashboard.home.appointment.patientfile

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DiagnosisItem
import com.consultantvendor.data.models.requests.DoctorNotesRequest
import com.consultantvendor.data.models.responses.IcdDiagnosisItem
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentDoctorNotesBinding
import com.consultantvendor.ui.dashboard.home.AppointmentViewModel
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.showSnackBar
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class DoctorNotesFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentDoctorNotesBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var appointmentViewModel: AppointmentViewModel
    private lateinit var prescriptionViewModel: AddPrescriptionViewModel

    private var rootView: View? = null
    private var request: Request? = null

    private val selectedDiagnoses = mutableListOf<DiagnosisItem>()
    private var diagnosisSuggestions = listOf<IcdDiagnosisItem>()
    private lateinit var suggestionAdapter: DiagnosisSuggestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_doctor_notes, container, false)
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
        appointmentViewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        prescriptionViewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]

        val user = request?.from_user ?: return
        loadImage(binding.ivPic, user.profile_image, R.drawable.ic_profile_placeholder)
        binding.tvName.text = user.name ?: ""
        binding.tvPatientId.text = user.id ?: ""
        val profile = user.profile
        val gender = profile?.gender ?: ""
        val dob = profile?.dob ?: ""
        binding.tvGenderDob.text = listOf(gender, dob).filter { it.isNotEmpty() }.joinToString(" · ")

        suggestionAdapter = DiagnosisSuggestionAdapter { item ->
            val diagnosis = DiagnosisItem(
                code = item.code_id ?: "",
                title = item.ascii_desc ?: item.ascii_short_desc ?: ""
            )
            if (selectedDiagnoses.none { it.code == diagnosis.code }) {
                selectedDiagnoses.add(diagnosis)
                addDiagnosisChip(diagnosis)
            }
            binding.etDiagnosisSearch.setText("")
            binding.rvDiagnosisSuggestions.visibility = View.GONE
        }
        binding.rvDiagnosisSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDiagnosisSuggestions.adapter = suggestionAdapter
    }

    private fun addDiagnosisChip(diagnosis: DiagnosisItem) {
        val chip = TextView(requireContext()).apply {
            text = "${diagnosis.code} — ${diagnosis.title}  ✕"
            setPadding(24, 12, 24, 12)
            setBackgroundResource(R.drawable.drawable_theme_stroke_4)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            textSize = 13f
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 0)
            layoutParams = params
            setOnClickListener {
                selectedDiagnoses.remove(diagnosis)
                binding.llSelectedDiagnoses.removeView(this)
            }
        }
        binding.llSelectedDiagnoses.addView(chip)
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.etDiagnosisSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    if (isConnectedToInternet(requireContext(), false)) {
                        val hashMap = HashMap<String, String>()
                        hashMap["page"] = "1"
                        hashMap["code"] = ""
                        hashMap["description"] = query
                        prescriptionViewModel.getDiagnosis(hashMap)
                    }
                } else {
                    binding.rvDiagnosisSuggestions.visibility = View.GONE
                }
            }
        })

        binding.tvSave.setOnClickListener {
            if (validate()) {
                hitApiSaveDoctorNotes()
            }
        }
    }

    private fun validate(): Boolean {
        if (binding.etChiefComplain.text.toString().trim().isEmpty()) {
            binding.etChiefComplain.showSnackBar(getString(R.string.please_enter_chief_complain))
            return false
        }
        return true
    }

    private fun hitApiSaveDoctorNotes() {
        if (!isConnectedToInternet(requireContext(), true)) return

        val doctorNotesRequest = DoctorNotesRequest(
            request_id = request?.id ?: "",
            chiefComplain = binding.etChiefComplain.text.toString().trim(),
            briefHistory = binding.etBriefHistory.text.toString().trim(),
            diagnoses = selectedDiagnoses.toList(),
            plan = binding.etPlan.text.toString().trim()
        )

        appointmentViewModel.saveDoctorNotes(doctorNotesRequest)
    }

    private fun bindObservers() {
        prescriptionViewModel.getdiagnosis.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            if (it.status == Status.SUCCESS) {
                diagnosisSuggestions = it.data?.data?.data ?: emptyList()
                suggestionAdapter.submitList(diagnosisSuggestions)
                binding.rvDiagnosisSuggestions.visibility =
                    if (diagnosisSuggestions.isEmpty()) View.GONE else View.VISIBLE
            }
        })

        appointmentViewModel.saveDoctorNotes.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.visibility = View.GONE
                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()
                }
                Status.ERROR -> {
                    binding.clLoader.root.visibility = View.GONE
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.root.visibility = View.VISIBLE
                }
            }
        })
    }
}
