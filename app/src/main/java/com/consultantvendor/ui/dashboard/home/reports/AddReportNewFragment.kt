package com.consultantvendor.ui.dashboard.home.reports

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentAddReportNewBinding
import com.consultantvendor.databinding.FragmentAddReportsBinding
import com.consultantvendor.databinding.FragmentManualPrescriptionBinding
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.ui.dashboard.home.prescription.manual.ImagesAdapter
import com.consultantvendor.utils.BasePhotoUplaodFragment
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.PrescriptionType
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.consultantvendor.utils.getAge
import com.consultantvendor.utils.getRequestBody
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.showSnackBar
import dagger.android.support.DaggerFragment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AddReportNewFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentAddReportNewBinding

    private var rootView: View? = null

    private lateinit var viewModelUpload: UploadFileViewModel

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private var request: Request? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_add_report_new,
                container,
                false
            )
            rootView = binding.root

            initialise()
            listeners()
            setEditReport()
            bindObservers()


        }
        return rootView
    }

    private fun setEditReport() {
        val medicalReport = request?.prescriptionReport ?: return
        binding.etReportDescription.setText(medicalReport.report_detals)
    }

    private fun initialise() {
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        addPrescriptionViewModel =
            ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())

        request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as? Request

        binding.tvName.text = request?.from_user?.name
        binding.tvMobileNumber.text = request?.from_user?.phone

        request?.from_user?.profile?.dob
            ?.takeIf { it.isNotEmpty() }
            ?.let { dob ->
                binding.tvDob.append(dob)
                // if you want age, uncomment this:
                // binding.tvAge.text = "${getAge(dob)} ${getString(R.string.years_old)}"
            }

        request?.from_user?.profile?.gender
            ?.takeIf { it.isNotEmpty() }
            ?.let { gender ->
                binding.tvGender.append(gender)
            }

        request?.id
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                binding.tvId.append(request?.from_user?.national_id ?: "")
            }

        request?.from_user?.profile?.weight
            ?.takeIf { it.isNotEmpty() }
            ?.let { weight ->
                binding.tvWeight.append(weight)
            }


        loadImage(binding.ivPic, request?.from_user?.profile_image,
            R.drawable.ic_profile_placeholder)

    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvDone.setOnClickListener {
            when {
                binding.etReportDescription.text.toString().trim().isEmpty() -> {
                    binding.etReportDescription.showSnackBar(getString(R.string.record_details))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["report_detals"] = binding.etReportDescription.text.toString()
                    hashMap["request_id"] = request?.id.toString()
                    addPrescriptionViewModel.addPrescription(hashMap)
                }
            }
        }
    }

    private fun bindObservers() {

        addPrescriptionViewModel.addPrescription.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()

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

}


