package com.consultantvendor.ui.dashboard.home.reports

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.models.responses.Response
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentAddReportsBinding
import com.consultantvendor.ui.adapter.DiagnosisAdapter
import com.consultantvendor.ui.adapter.MedicineAdapter
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.ui.dashboard.home.prescription.digital.DiagnosisListAdapter
import com.consultantvendor.ui.dashboard.home.prescription.digital.DialogMedicineFragment
import com.consultantvendor.ui.dashboard.home.prescription.digital.InsuranceAdapter
import com.consultantvendor.ui.dashboard.home.prescription.digital.medicineListAdapter
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelDiagnosis
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelMedicine
import com.consultantvendor.ui.dashboard.home.prescription.model.ResponseInsurance
import com.consultantvendor.utils.BasePhotoUplaodFragment
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.DiagnosisDialogFragment
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.consultantvendor.utils.editTextScroll
import com.consultantvendor.utils.getAge
import com.consultantvendor.utils.getRequestBody
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.longToast
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AddReportFragment : BasePhotoUplaodFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    lateinit var binding: FragmentAddReportsBinding

    private var rootView: View? = null

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel

    private lateinit var viewModelUpload: UploadFileViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private var request: Request? = null

    private var isLastPage = false

    private var isFirstPage = true


    private var itemDiagnosis = ArrayList<Response>()

    private var diagnosisDialog: DiagnosisDialogFragment? = null


    private var medicneAdapter: MedicineAdapter? = null

    private var adpterDiagnosis: DiagnosisAdapter? = null

    private var isSearchDiagnosis = false

    private var spinnerInsuranceAdapter: InsuranceAdapter? = null

    private val itemsInsurance = ArrayList<ResponseInsurance>()

    private var docUrl = ""

    private var prescription_type = ""

    private var filltype = ""

    var adpterDiagnosisList: DiagnosisListAdapter? = null

    val itemDiagnosisList = ArrayList<ItemModelDiagnosis>()

    var adpterMedicineList: medicineListAdapter? = null

    var itemMedicineList = ArrayList<ItemModelMedicine>()

    var insuraceId = ""

    var item_number = ""

    var isEditMedicine = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_reports, container, false
            )
            rootView = binding.root

            initialise()
            setAdapter()
            setEditPrescriptionData()
            listeners()
            bindObservers()
        }
        return rootView
    }


    @SuppressLint("SetTextI18n")
    private fun initialise() {
        addPrescriptionViewModel =
            ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]

        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())

        editTextScroll(binding.etPrescriptionNotes)

        request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as Request

        binding.tvName.text = request?.from_user?.name
        binding.tvMobileNumber.text = request?.from_user?.phone

        request?.from_user?.profile?.dob
            ?.takeIf { it.isNotEmpty() }
            ?.let { dob ->
                binding.tvDob.append(dob)
                binding.tvAge.text = "${getAge(dob)} ${getString(R.string.years_old)}"
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
        loadImage(
            binding.ivPic, request?.from_user?.profile_image,
            R.drawable.ic_profile_placeholder
        )

        binding.tvAppointmentV.text = "${
            DateUtils.dateTimeFormatFromUTC(
                DateFormat.MON_DATE_YEAR,
                request?.bookingDateUTC
            )
        } · " +
                DateUtils.dateTimeFormatFromUTC(
                    DateFormat.TIME_FORMAT,
                    request?.bookingDateUTC
                )

        if (!request?.to_user?.name.isNullOrEmpty()) {
            binding.tvDoctorName.append(request?.to_user?.name)
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setEditPrescriptionData() {
        val medicalReport = request?.medicalReport ?: return

        binding.tvHeader.text = getString(R.string.edit_precription)

        if (medicalReport.prescription_type == "insurance") {
            binding.spnPrescriptionType.setSelection(1)
        } else {
            binding.spnPrescriptionType.setSelection(2)
        }

        insuraceId = medicalReport.insurance_id.toString()

        if (medicalReport.fill_type == "form") {
            binding.spnFillType.setSelection(1)
        } else {
            binding.spnFillType.setSelection(2)

            medicalReport.prescription_file?.isEmpty()?.let {
                if (!it)
                    docUrl = medicalReport.prescription_file.toString()


                if (docUrl.endsWith(".pdf", ignoreCase = true)) {
                    // It's a PDF
                    binding.ivDoc.setImageResource(R.drawable.ic_pdf)
                    binding.tvFileName.text = docUrl
                } else {
                    loadImage(binding.ivDoc,docUrl,R.drawable.image_placeholder)
                    binding.tvFileName.text = docUrl
                }


            }

        }

        if (medicalReport.diagnosis?.isNotEmpty() == true) {
            medicalReport.diagnosis.forEach {
                itemDiagnosisList.add(ItemModelDiagnosis(it.code, it.title))
            }
            adpterDiagnosisList?.notifyDataSetChanged()

        }

        if (medicalReport.prescription?.isNotEmpty() == true) {
            medicalReport.prescription.forEach {
                itemMedicineList.add(
                    ItemModelMedicine(
                        it.description,
                        it.doses, it.frequency, it.duration, "", it.quantity
                    )
                )
            }
            adpterMedicineList?.notifyDataSetChanged()
        }

        binding.etNotes.setText(medicalReport.report_detals)

    }

    private fun setAdapter() {
        insuraceId.takeIf { it.isNotEmpty() }?.let {
            val index=   itemsInsurance.indexOfFirst { it._id==insuraceId }
            val item=itemsInsurance.get(index)
            itemsInsurance.removeAt(index)
            itemsInsurance.add(0,item)
        }

        spinnerInsuranceAdapter = InsuranceAdapter(this, itemsInsurance)
        binding.spnInsurance.adapter = spinnerInsuranceAdapter

        adpterDiagnosisList = DiagnosisListAdapter(itemDiagnosisList)
        binding.rvDiagnosisList.adapter = adpterDiagnosisList

        adpterMedicineList = medicineListAdapter(this, itemMedicineList)
        binding.rvMedicinelist.adapter = adpterMedicineList

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.spnInsurance.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    insuraceId = itemsInsurance[position]._id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: handle if nothing is selected
                }
            }


        binding.spnPrescriptionType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position == 1) {
                        binding.spnInsurance.visible()
                        addPrescriptionViewModel.getInsurance()
                        prescription_type = "insurance"
                    } else if (position == 2) {
                        binding.spnInsurance.visibility = View.GONE
                        prescription_type = "cash"
                        insuraceId = ""
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: handle if nothing is selected
                }
            }


        binding.spnFillType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position == 1) {
                        binding.clfillform.visible()
                        binding.clUploadPrescription.gone()
                        filltype = "form"
                    } else if (position == 2) {
                        binding.clfillform.gone()
                        binding.clUploadPrescription.visible()
                        filltype = "upload-prescription"
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }


        binding.etDiagnosis.setOnClickListener {
            hitApiDiagnosis(true, "", null, false)
        }

        binding.btnAddMedicine.setOnClickListener {
            val fragment =
                DialogMedicineFragment(this, isEditMedicine, request?.medicalReport?.prescription)
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
        }



        binding.clUploadPrescription.setOnClickListener {
            showImageDialog(false, true, true,true)
        }

        binding.tvDone.setOnClickListener {

            if (binding.spnPrescriptionType.selectedItemPosition == 0) {
                binding.spnPrescriptionType.showSnackBar(getString(R.string.select_payment_type))
                return@setOnClickListener
            }

            if (binding.spnFillType.selectedItemPosition == 0) {
                binding.spnFillType.showSnackBar(getString(R.string.select_fill_type))
                return@setOnClickListener
            }

            if (binding.spnFillType.selectedItemPosition == 1) {

                if (itemDiagnosisList.isEmpty()) {
                    binding.etDiagnosis.showSnackBar(getString(R.string.select_diagnosis_code_and_title))
                    return@setOnClickListener
                }

                if (itemMedicineList.isEmpty()) {
                    binding.etDiagnosis.showSnackBar(getString(R.string.add_medicine))
                    return@setOnClickListener
                }
            }

            if (binding.spnFillType.selectedItemPosition == 2) {
                if (docUrl.isEmpty()) {
                    binding.ivDoc.showSnackBar(getString(R.string.upload_prescription))
                    return@setOnClickListener
                }
            }

//            if (binding.etNotes.text.toString().trim().isEmpty()) {
//                binding.etNotes.showSnackBar(getString(R.string.add_notes))
//                return@setOnClickListener
//            }

            if (isConnectedToInternet(requireContext(), true)) {
                val hashMap = HashMap<String, Any>()
                hashMap["request_id"] = request?.id.toString()
//                hashMap["report_detals"] = binding.etNotes.text.toString()
                hashMap["prescription_type"] = prescription_type
                hashMap["fill_type"] = filltype
                hashMap["item_no"] = item_number
                hashMap["insurance_id"] = insuraceId

                val gson = Gson()
                hashMap["pre_scriptions"] = gson.toJson(itemMedicineList)
                hashMap["diagnosis"] = gson.toJson(itemDiagnosisList)
                hashMap["prescription_file"] = docUrl

                addPrescriptionViewModel.addReports(hashMap)
            }
        }


    }

    fun hitApiDiagnosis(
        firstHit: Boolean,
        etSearch: String,
        diagnosisAdapter: DiagnosisAdapter?,
        isSearch: Boolean
    ) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            isSearchDiagnosis = isSearch
            adpterDiagnosis = diagnosisAdapter

            val hashMap = HashMap<String, String>()

            hashMap["page"] = "1"
            hashMap["code"] = etSearch
            hashMap["description"] = ""
            addPrescriptionViewModel.getDiagnosis(hashMap)

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        addPrescriptionViewModel.getInsurance.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    itemsInsurance.clear()
                    itemsInsurance.addAll(it.data?.response ?: emptyList())
                    insuraceId.takeIf { it.isNotEmpty() }?.let {
                     val index=   itemsInsurance.indexOfFirst { it._id==insuraceId }
                        val item=itemsInsurance.get(index)
                        itemsInsurance.removeAt(index)
                        itemsInsurance.add(0,item)
                    }
                    spinnerInsuranceAdapter?.notifyDataSetChanged()

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


        addPrescriptionViewModel.getdiagnosis.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    progressDialog.setLoading(false)

                    itemDiagnosis.clear()
                    itemDiagnosis.addAll(it.data?.response ?: emptyList())
                    if (!isSearchDiagnosis) {
                        showDiagnosisDialog()
                    } else {
                        adpterDiagnosis?.notifyDataSetChanged()
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

        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    val imageUrl = it.data?.image_name

                    if (imageUrl != null && imageUrl.endsWith(".pdf", ignoreCase = true)) {
                        // It's a PDF
                        docUrl = it.data?.image_name.toString()
                        binding.ivDoc.setImageResource(R.drawable.ic_pdf)
                        binding.tvFileName.text = docUrl
                    } else {
                        docUrl = it.data?.image_name.toString()
                        loadImage(binding.ivDoc,docUrl,R.drawable.image_placeholder)
                        binding.tvFileName.text = docUrl
                    }


                }

                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    progressDialogImage.setLoading(true)
                }
            }
        })

        addPrescriptionViewModel.addReports.observe(requireActivity(), Observer {
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

    fun setdiagnosisText(note: String) {
        binding.etDiagnosis.setText(note)
    }

    fun showDiagnosisDialog() {
        diagnosisDialog = DiagnosisDialogFragment(
            onNoteSelected = { note ->
                setdiagnosisText(note)
            },
            this, itemDiagnosis
        )
        diagnosisDialog?.show(childFragmentManager, "DiagnosisDialog")
    }

    override fun getVideo(uri: String?, i: Int) {

    }

    override fun getPdf(uri: String?) {
        val fileToUpload = File(uri)

        val docImage = DocImage()
        docImage.type = DocType.PDF
        docImage.imageFile = fileToUpload
        uploadFileOnServer(docImage)
    }

    override fun getImage(uri: String?, data: Uri) {
        var fileToUpload: File? = File(uri)
        uri?.let {
            val rotatedFile = File(it) // this is the rotated image created earlier
            if (rotatedFile.exists()) {
                val docImage = DocImage().apply {
                    type = DocType.IMAGE
                    imageFile = rotatedFile
                }

                fileToUpload = rotatedFile
                uploadFileOnServer(docImage)
            } else {
//                showError("Rotated image not found.")
            }
        }
    }


    private fun uploadFileOnServer(docImage: DocImage?) {
        val hashMap = HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }
}


