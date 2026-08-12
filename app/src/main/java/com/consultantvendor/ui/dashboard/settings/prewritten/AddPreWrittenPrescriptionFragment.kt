package com.consultantvendor.ui.dashboard.settings.prewritten

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.IcdDiagnosisItem
import com.consultantvendor.data.models.responses.PreWrittenPrescription
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentAddPreWrittenPrescriptionBinding
import com.consultantvendor.ui.adapter.DiagnosisAdapter
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
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.DiagnosisDialogFragment
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.consultantvendor.utils.getRequestBody
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AddPreWrittenPrescriptionFragment : BasePhotoUplaodFragment() {

    companion object {
        const val EXTRA_PRESCRIPTION = "EXTRA_PRESCRIPTION"
    }

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var binding: FragmentAddPreWrittenPrescriptionBinding
    private var rootView: View? = null
    private lateinit var progressDialog: ProgressDialog
    private lateinit var progressDialogImage: ProgressDialogImage
    private lateinit var viewModel: AddPrescriptionViewModel
    private lateinit var viewModelUpload: UploadFileViewModel

    private var prescription: PreWrittenPrescription? = null

    private var prescription_type = ""
    private var filltype = ""
    private var insuraceId = ""
    private var insuranceName = ""
    private var docUrl = ""
    var isEditMedicine = false
    var editMedicineIndex = 0

    private var itemDiagnosis = ArrayList<IcdDiagnosisItem>()
    private var diagnosisDialog: DiagnosisDialogFragment? = null
    private var adpterDiagnosis: DiagnosisAdapter? = null

    private var spinnerInsuranceAdapter: InsuranceAdapter? = null
    private val itemsInsurance = ArrayList<ResponseInsurance>()

    var adpterDiagnosisList: DiagnosisListAdapter? = null
    val itemDiagnosisList = ArrayList<ItemModelDiagnosis>()

    var adpterMedicineList: medicineListAdapter? = null
    var itemMedicineList = ArrayList<ItemModelMedicine>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_pre_written_prescription, container, false
            )
            rootView = binding.root
            initialise()
            setAdapter()
            setEditData()
            listeners()
            bindObservers()
        }
        return rootView
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())
        prescription = requireActivity().intent.getSerializableExtra(EXTRA_PRESCRIPTION) as? PreWrittenPrescription
        binding.spnFillType.isEnabled = true
        binding.clUploadPrescription.gone()
    }

    private fun setAdapter() {
        spinnerInsuranceAdapter = InsuranceAdapter(this, itemsInsurance)
        binding.spnInsurance.adapter = spinnerInsuranceAdapter

        adpterDiagnosisList = DiagnosisListAdapter(itemDiagnosisList)
        binding.rvDiagnosisList.adapter = adpterDiagnosisList

        adpterMedicineList = medicineListAdapter(this, itemMedicineList)
        binding.rvMedicinelist.adapter = adpterMedicineList
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setEditData() {
        val p = prescription ?: return

        binding.etTitle.setText(p.title)
        binding.etNotes.setText(p.report_detals)

        when (p.prescription_type) {
            "insurance" -> binding.spnPrescriptionType.setSelection(1)
            "cash" -> binding.spnPrescriptionType.setSelection(2)
        }

        when (p.fill_type) {
            "form" -> binding.spnFillType.setSelection(1)
            "upload-prescription" -> binding.spnFillType.setSelection(2)
        }

        p.diagnosis?.forEach { itemDiagnosisList.add(it) }
        adpterDiagnosisList?.notifyDataSetChanged()

        p.prescription?.forEach { itemMedicineList.add(it) }
        adpterMedicineList?.notifyDataSetChanged()

        val fileUrl = p.prescription_file?.trim()
        if (!fileUrl.isNullOrEmpty()) {
            docUrl = fileUrl
            binding.clUploadPrescription.visible()
            if (fileUrl.endsWith(".pdf", ignoreCase = true)) {
                binding.ivDoc.setImageResource(R.drawable.ic_pdf)
            } else {
                loadImage(binding.ivDoc, fileUrl, R.drawable.image_placeholder)
            }
            binding.tvFileName.text = fileUrl
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.spnInsurance.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (itemsInsurance.isNotEmpty()) {
                    insuraceId = itemsInsurance[position]._id
                    insuranceName = itemsInsurance[position].titleEN
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spnPrescriptionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    1 -> { // Insurance
                        binding.spnInsurance.visible()
                        viewModel.getInsurance()
                        prescription_type = "insurance"
                        binding.spnFillType.setSelection(2, false)
                        binding.clfillform.gone()
                        binding.clUploadPrescription.visible()
                        filltype = "upload-prescription"
                    }
                    2 -> { // Cash
                        binding.spnInsurance.gone()
                        prescription_type = "cash"
                        insuraceId = ""
                        binding.spnFillType.setSelection(1, false)
                        binding.clfillform.visible()
                        binding.clUploadPrescription.visible()
                        filltype = "form"
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spnFillType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!prescription_type.equals("insurance", ignoreCase = true)) {
                    when (position) {
                        1 -> { binding.clfillform.visible(); binding.clUploadPrescription.visible(); filltype = "form" }
                        0 -> { binding.clfillform.gone(); filltype = "upload-prescription" }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.clUploadPrescription.setOnClickListener {
            showImageDialog(false, true, false, true)
        }

        binding.etDiagnosis.setOnClickListener {
            showDiagnosisDialog()
        }

        binding.btnAddMedicine.setOnClickListener {
            val fragment = DialogMedicineFragment(this, isEditMedicine, null)
            fragment.show(requireActivity().supportFragmentManager, fragment.tag)
        }

        binding.tvDone.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
//            if (title.isEmpty()) {
//                binding.etTitle.showSnackBar(getString(R.string.enter_title))
//                return@setOnClickListener
//            }
            if (binding.spnPrescriptionType.selectedItemPosition == 0) {
                binding.spnPrescriptionType.showSnackBar(getString(R.string.select_payment_type))
                return@setOnClickListener
            }
            if (binding.spnFillType.selectedItemPosition == 0) {
                binding.spnFillType.showSnackBar(getString(R.string.select_fill_type))
                return@setOnClickListener
            }
            if (binding.spnFillType.selectedItemPosition == 1 && itemMedicineList.isEmpty()) {
                binding.etDiagnosis.showSnackBar(getString(R.string.add_medicine))
                return@setOnClickListener
            }
            if (isConnectedToInternet(requireContext(), true)) {
                val gson = Gson()
                val notes = binding.etNotes.text.toString().trim()
                val params = hashMapOf(
                    "title" to title,
                    "description" to notes,
                    "prescription_type" to prescription_type,
                    "fill_type" to filltype,
                    "insurance_id" to insuraceId,
                    "insurance_name" to insuranceName,
                    "pre_scriptions" to gson.toJson(itemMedicineList),
                    "diagnosis" to gson.toJson(itemDiagnosisList),
                    "report_detals" to notes,
                    "prescription_file" to docUrl
                )
                if (prescription?.id != null) {
                    viewModel.updatePreWrittenPrescription(prescription!!.id!!, params)
                } else {
                    viewModel.savePreWrittenPrescription(params)
                }
            }
        }
    }

    fun hitApiDiagnosis(firstHit: Boolean, etSearch: String, diagnosisAdapter: DiagnosisAdapter?, isSearch: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            adpterDiagnosis = diagnosisAdapter
            val hashMap = hashMapOf("name" to etSearch, "code" to etSearch)
            viewModel.getDiagnosis(hashMap)
        }
    }

    fun setdiagnosisText(note: String) {
        binding.etDiagnosis.setText(note)
    }

    fun showDiagnosisDialog() {
        diagnosisDialog = DiagnosisDialogFragment(
            onNoteSelected = { note -> setdiagnosisText(note) },
            this, itemDiagnosis
        )
        diagnosisDialog?.show(childFragmentManager, "DiagnosisDialog")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        viewModel.getInsurance.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    itemsInsurance.clear()
                    itemsInsurance.addAll(it.data?.response ?: emptyList())
                    spinnerInsuranceAdapter?.notifyDataSetChanged()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModel.getdiagnosis.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    itemDiagnosis.clear()
                    itemDiagnosis.addAll(it.data?.data?.data ?: emptyList())
                    diagnosisDialog?.refreshAdapter()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModel.savePreWrittenPrescription.observe(requireActivity(), Observer {
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
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)
                    val imageUrl = it.data?.image_name
                    if (imageUrl != null && imageUrl.endsWith(".pdf", ignoreCase = true)) {
                        docUrl = imageUrl
                        binding.ivDoc.setImageResource(R.drawable.ic_pdf)
                        binding.tvFileName.text = docUrl
                    } else {
                        docUrl = imageUrl.toString()
                        loadImage(binding.ivDoc, docUrl, R.drawable.image_placeholder)
                        binding.tvFileName.text = docUrl
                    }
                }
                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialogImage.setLoading(true)
            }
        })
    }

    override fun getVideo(uri: String?, i: Int) {}

    override fun getPdf(uri: String?) {
        val fileToUpload = File(uri)
        val docImage = DocImage()
        docImage.type = DocType.PDF
        docImage.imageFile = fileToUpload
        uploadFileOnServer(docImage)
    }

    override fun getImage(uri: String?, data: Uri) {
        uri?.let {
            val rotatedFile = File(it)
            if (rotatedFile.exists()) {
                val docImage = DocImage().apply {
                    type = DocType.IMAGE
                    imageFile = rotatedFile
                }
                uploadFileOnServer(docImage)
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
