package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.requests.DigitalPrescription
import com.consultantvendor.data.models.requests.Doases
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.models.responses.Response
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentDigitalPrescriptionBinding
import com.consultantvendor.ui.adapter.DiagnosisAdapter
import com.consultantvendor.ui.adapter.MedicineAdapter
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.ui.dashboard.home.prescription.model.ResponseInsurance
import com.consultantvendor.ui.dashboard.home.prescription.model.itemModelDiagnosis
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.DiagnosisDialogFragment
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class DigitalPrescriptionFragment : BasePhotoUplaodFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    lateinit var binding: FragmentDigitalPrescriptionBinding

    private var rootView: View? = null

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel

    private lateinit var viewModelUpload: UploadFileViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private var doseadAdapter: ItemDoasesAdapter? = null

    private var itemDoases = ArrayList<Doases>()

    private var itemPrescription = ArrayList<DigitalPrescription>()

    private var prescriptionAdapter: ItemPrescriptionAdapter? = null

    private var request: Request? = null

    private var addPrescription: AddPrescription? = null

    private var editPosition = -1

    private var isLastPage = false

    private var isFirstPage = true

    private var itemMedicine = ArrayList<ResponseMedicine>()

    private var itemDiagnosis = ArrayList<Response>()

    private var itemDiagnosisList = ArrayList<itemModelDiagnosis>()

    private var isDiagnosis = false

    private var diagnosisDialog: DiagnosisDialogFragment? = null

    var duration = ""
    var quantity = ""
    var etDuration: EditText? = null
    var etQuantity: EditText? = null

    private var medicneAdapter: MedicineAdapter? = null

    private var adpterDiagnosis: DiagnosisAdapter? = null

    private var adpterDiagnosisList: DiagnosisListAdapter? = null

    private var isMedicineSelect = false

    private var isDiagnosisSelect = false

    private var spinnerInsuranceAdapter: InsuranceAdapter? = null

    private val itemsInsurance = ArrayList<ResponseInsurance>()

    private var imageUrl  = ""

    private var prescription_type  = ""

    private var filltype  = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_digital_prescription,
                container,
                false
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
//        editTextScroll(binding.etNotes)
//        editTextScroll(binding.etLabTest)
        request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as Request

        binding.tvName.text = request?.from_user?.name
        binding.tvMobileNumber.text = request?.from_user?.phone
        if (!request?.from_user?.profile?.dob.isNullOrEmpty()) {
            binding.tvDob.append(request?.from_user?.profile?.dob)
        }
        binding.tvAge.text =
            "${getAge(request?.from_user?.profile?.dob)} ${getString(R.string.years_old)}"
        if (!request?.from_user?.profile?.gender.isNullOrEmpty()) {
            binding.tvGender.append(request?.from_user?.profile?.gender)
        }
        if (!request?.id.isNullOrEmpty()) {
            binding.tvId.append(request?.id)
        }
        if (!request?.from_user?.profile?.weight.isNullOrEmpty()) {
            binding.tvWeight.append(request?.from_user?.profile?.weight)
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
                "${
                    DateUtils.dateTimeFormatFromUTC(
                        DateFormat.TIME_FORMAT,
                        request?.bookingDateUTC
                    )
                }"

        if (!request?.to_user?.name.isNullOrEmpty()) {
            binding.tvDoctorName.append(request?.to_user?.name)
        }


        val hint = getString(R.string.prescription_type)
        val coloredHint = SpannableString("$hint *")
        coloredHint.setSpan(
            ForegroundColorSpan(Color.RED),
            coloredHint.length - 1,
            coloredHint.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvPrescriptionType.hint = coloredHint


    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setEditPrescriptionData() {
        if (request?.pre_scription != null) {
            val prescription = request?.pre_scription
//            binding.etMedicineName.setText(prescription?.medicines?.get(0)?.medicine_name)
            binding.etPrescriptionNotes.setText(prescription?.pre_scription_notes)
//            binding.etNotes.setText(prescription?.pre_scription_notes)
//
//            binding.etDoses.setText(prescription?.medicines?.get(0)?.doses)
//            binding.etfrequency.setText(prescription?.medicines?.get(0)?.dosage_type)

//            binding.etLabTest.setText(prescription?.lab_notes)

            itemPrescription.clear()
            itemPrescription.addAll(prescription?.medicines ?: emptyList())
            prescriptionAdapter?.notifyDataSetChanged()

            if (itemPrescription.isNotEmpty()) {
                binding.tvPrescriptions.visible()
                binding.rvPrescriptions.visible()
            }
        }
    }

    private fun setAdapter() {

        itemDoases = ArrayList()
        var doase = Doases()
        doase.time = getString(R.string.breakfast)
        doase.checked = true
        itemDoases.add(doase)

        doase = Doases()
        doase.time = getString(R.string.lunch)
        itemDoases.add(doase)

        doase = Doases()
        doase.time = getString(R.string.dinner)
        itemDoases.add(doase)

//        doseadAdapter = ItemDoasesAdapter(this, itemDoases)
//        binding.rvDoasesTiming.adapter = doseadAdapter

        prescriptionAdapter = ItemPrescriptionAdapter(this, itemPrescription)
        binding.rvPrescriptions.adapter = prescriptionAdapter

        spinnerInsuranceAdapter = InsuranceAdapter(this, itemsInsurance)
        binding.spnInsurance.adapter = spinnerInsuranceAdapter

        spinnerInsuranceAdapter = InsuranceAdapter(this, itemsInsurance)
        binding.spnInsurance.adapter = spinnerInsuranceAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }


        binding.spnPrescriptionType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()

//                if (selectedItem == "Prescription") {
//
//                }

                    if (position == 1) {
                        binding.spnInsurance.visible()
                        addPrescriptionViewModel.getInsurance()
                    } else {
                        binding.spnInsurance.visibility = View.GONE
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
                    } else {
                        binding.clfillform.gone()
                        binding.clUploadPrescription.visible()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optional: handle if nothing is selected
                }
            }


        binding.etDiagnosis.setOnClickListener {
            hitApiDiagnosis(true, "", null, false)
        }



        binding.clUploadPrescription.setOnClickListener {
            showImageDialog(false,false,true)

        }


//        binding.spnDosagesType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parentView: AdapterView<*>,
//                                        selectedItemView: View?, position: Int, id: Long) {
//                doseadAdapter?.notifyDataSetChanged()
//            }
//
//            override fun onNothingSelected(parentView: AdapterView<*>) {
//            }
//        }

        /*       binding.tvAdd.setOnClickListener {
                   binding.tvAdd.hideKeyboard()
                   when {
                       binding.etMedicineName.text.toString().trim().isEmpty() -> {
                           binding.etMedicineName.showSnackBar(getString(R.string.medicine_name))
                       }
       //                binding.spnDuration.selectedItemPosition == 0 -> {
       //                    binding.tvDuration.showSnackBar(getString(R.string.duration))
       //                }
       //                binding.spnDosagesType.selectedItemPosition == 0 -> {
       //                    binding.tvDosagesType.showSnackBar(getString(R.string.dosage_type))
       //                }

       //                binding.etDoses.text.toString().trim().isEmpty()->{
       //                    binding.etMedicineName.showSnackBar(getString(R.string.dosage))
       //                }
       //                binding.etfrequency.text.toString().trim().isEmpty()->{
       //                    binding.etMedicineName.showSnackBar(getString(R.string.frequency))
       //                }

                       else -> {
                           var addItem = false
                           itemDoases.forEachIndexed { index, doases ->
                               if (doases.checked == true) {
                                   addItem = true

                                   etDuration= binding.rvDoasesTiming.findViewHolderForAdapterPosition(index)?.itemView?.findViewById<EditText>(R.id.etDuration)
                                   etQuantity = binding.rvDoasesTiming.findViewHolderForAdapterPosition(index)?.itemView?.findViewById<EditText>(R.id.etQuantity)
                                   duration = etDuration?.text?.toString()?.takeIf { it.isNotBlank() } ?: "0"
                                   quantity = etQuantity?.text?.toString()?.takeIf { it.isNotBlank() } ?: "0"

       //                            if (doases.duration.isNullOrEmpty()) {
       //                                addItem = false
       //                                binding.etMedicineName.showSnackBar(getString(R.string.select_doasages_for, doases.time))
       //                                return@setOnClickListener
       //                            }
                               }
                           }

                           if (addItem) {
                               val prescription = DigitalPrescription()
                               prescription.medicine_name = binding.etMedicineName.text.toString().trim()
                               prescription.duration = duration
                               prescription.quantity = quantity
                               prescription.doses = binding.etDoses.text.toString().trim()
                               prescription.dosage_type = binding.etfrequency.text.toString().trim()
                               prescription.quantity = quantity
                               prescription.dosage_timing = ArrayList()

                               itemDoases.forEach {
                                   val digitalDose: Doases
                                   if (it.checked == true) {
                                       digitalDose = Doases()
                                       digitalDose.time = it.time
                                       digitalDose.with = it.with
                                       digitalDose.duration = it.duration
                                       digitalDose.quantity = it.quantity
                                       digitalDose.doses = it.doses
                                       digitalDose.dosage_type = it.dosage_type
                                       digitalDose.checked = null

                                       prescription.dosage_timing?.add(digitalDose)
                                   }
                               }
                               *//*If edit item*//*
                        if (binding.tvAdd.text == getString(R.string.edit))
                            itemPrescription.set(editPosition, prescription)
                        else
                            itemPrescription.add(prescription)
                            prescriptionAdapter?.notifyDataSetChanged()

                        editPosition = -1
                        requireActivity().longToast(getString(R.string.prescription_added))

                        if (itemPrescription.size == 1) {
                            binding.tvPrescriptions.visible()
                            binding.rvPrescriptions.visible()
                        }

                        *//*Clear item*//*
                        binding.tvReset.performClick()

                    }

//                   else {
//                        binding.etMedicineName.showSnackBar(getString(R.string.select_dosage_timings))
//                    }
                }
            }
        }*/

        /*        binding.tvReset.setOnClickListener {
                    binding.tvAdd.hideKeyboard()
                    editPosition = -1

                    binding.tvAdd.text = getString(R.string.add)
                    binding.etMedicineName.setText("")
                    binding.etDoses.setText("")
                    binding.etfrequency.setText("")
        //            binding.spnDuration.setSelection(0)
        //            binding.spnDosagesType.setSelection(0)
                    etDuration?.setText("")
                    etQuantity?.setText("")

                    itemDoases.forEachIndexed { index, doases ->
                        itemDoases[index].checked = index == 0
                        itemDoases[index].with = getString(R.string.before)
                        itemDoases[index].doses = ""
                        itemDoases[index].dosage_type = ""
                    }
                    doseadAdapter?.notifyDataSetChanged()
                }*/

        binding.tvDone.setOnClickListener {
//            binding.tvAdd.hideKeyboard()
            when {
//                itemPrescription.isEmpty() -> {
//                    binding.etMedicineName.showSnackBar(getString(R.string.add_digital_prescription))
//                }
//
//                binding.etNotes.text.toString().trim().isEmpty()->{
//                    binding.etMedicineName.showSnackBar(getString(R.string.select_diagnosis))
//                }
                /* binding.etPrescriptionNotes.text.toString().trim().isEmpty() -> {
                     binding.tvDosagesType.showSnackBar(getString(R.string.add_notes))
                 }*/
//                binding.etNotes.text.toString().trim().isEmpty() -> {
//                    binding.tvDosagesType.showSnackBar(getString(R.string.add_diagnosis))
//                }
                binding.etNotes.text.toString().trim().isEmpty() -> {
                    binding.etNotes.showSnackBar(getString(R.string.add_notes))
                }

                isConnectedToInternet(requireContext(), true) -> {
//                    addPrescription = AddPrescription()
//                    addPrescription?.request_id = request?.id
//                    addPrescription?.type = PrescriptionType.DIGITAL
//
//                    addPrescription?.pre_scription_notes = binding.etNotes.text.toString().trim()
//                    addPrescription?.lab_notes = binding.etLabTest.text.toString().trim()
//                    addPrescription?.pre_scriptions = ArrayList()
//                    addPrescription?.pre_scriptions?.addAll(itemPrescription)
//
//                    addPrescriptionViewModel.prescreptions(addPrescription ?: AddPrescription())


                    val hashMap = HashMap<String, Any>()
                    hashMap["request_id"] = request?.id.toString()
                    hashMap["report_detals"] = binding.etNotes.text.toString()
                    hashMap["prescription_type"] = prescription_type
                    hashMap["fill_type"] = filltype
                    hashMap["insurance_id"] = "123456789"


                    val prescriptions = arrayListOf<HashMap<String, Any>>()

                    prescriptions.add(
                        hashMapOf(
                            "item_no" to "001",
                            "description" to "Paracetamol 500mg",
                            "doses" to "1 tablet",
                            "frequency" to "Twice a day",
                            "duration" to "5 days",
                            "quantity" to "10"
                        )
                    )

                    prescriptions.add(
                        hashMapOf(
                            "item_no" to "002",
                            "description" to "Ibuprofen 400mg",
                            "doses" to "1 tablet",
                            "frequency" to "Three times a day",
                            "duration" to "3 days",
                            "quantity" to "9"
                        )
                    )

                    hashMap["pre_scriptions"] = prescriptions

                    val diagnosis = arrayListOf<HashMap<String, Any>>()

                    diagnosis.add(
                        hashMapOf(
                            "code" to "A01",
                            "title" to "Typhoid Fever"
                        )
                    )

                    diagnosis.add(
                        hashMapOf(
                            "code" to "J11",
                            "title" to "Influenza"
                        )
                    )

                    hashMap["diagnosis"] = diagnosis
                    addPrescriptionViewModel.addReports(hashMap)

                }
            }
        }




//        binding.btnAddMedicine.setOnClickListener {
//            val fragment = DialogMedicineFragment(this)
//            fragment.show(requireActivity().supportFragmentManager, fragment.tag)
//        }
    }




    fun hitApi(
        firstHit: Boolean,
        etSearch: String,
        medicineAdapter: MedicineAdapter?,
        isSelect: Boolean
    ) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            isMedicineSelect = isSelect
            medicneAdapter = medicineAdapter


            val hashMap = HashMap<String, String>()

            hashMap["page"] = "1"
            hashMap["itemNumber"] = ""
            hashMap["description"] = etSearch
            isDiagnosis = false
            addPrescriptionViewModel.getItemList(hashMap)

        }

    }

    fun hitApiDiagnosis(
        firstHit: Boolean,
        etSearch: String,
        diagnosisAdapter: DiagnosisAdapter?,
        isSelectDiagnosis: Boolean
    ) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            adpterDiagnosis = diagnosisAdapter
            isDiagnosisSelect = isSelectDiagnosis

            val hashMap = HashMap<String, String>()

            hashMap["page"] = "1"
            hashMap["code"] = etSearch
            hashMap["description"] = ""
            isDiagnosis = true
            addPrescriptionViewModel.getDiagnosis(hashMap)

        }

    }


    @SuppressLint("NotifyDataSetChanged")
    fun deletePrescription(pos: Int) {
        itemPrescription.removeAt(pos)
        prescriptionAdapter?.notifyDataSetChanged()

//        if (editPosition == pos) {
//            binding.tvReset.performClick()
//        }

        if (itemPrescription.isEmpty())
            binding.tvPrescriptions.gone()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun editPrescription(pos: Int) {
//        binding.tvAdd.hideKeyboard()

        editPosition = pos

//        binding.tvAdd.text = getString(R.string.edit)

        val item = itemPrescription[editPosition]
//        binding.etMedicineName.setText(item.medicine_name)

//        val duration = resources.getStringArray(R.array.duration)
//        binding.spnDuration.setSelection(duration.indexOf(item.duration))
//
//        val dose_type = resources.getStringArray(R.array.dose_type)
//        binding.spnDosagesType.setSelection(dose_type.indexOf(item.dosage_type))

        item.dosage_timing?.forEachIndexed { index, doases ->
            itemDoases.forEachIndexed { indexInternal, doasesInternal ->
                if (doases.time == doasesInternal.time) {
                    itemDoases[indexInternal].checked = true
                    itemDoases[indexInternal].with = doases.with
                    itemDoases[indexInternal].doses = doases.doses
                    itemDoases[indexInternal].dosage_type = doases.dosage_type
                    itemDoases[indexInternal].routes = doases.routes
                    return@forEachIndexed
                }
            }
        }
        doseadAdapter?.notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        addPrescriptionViewModel.prescreptions.observe(requireActivity(), Observer {
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


        addPrescriptionViewModel.getInsurance.observe(requireActivity(), Observer {
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

                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })


        addPrescriptionViewModel.getItemList.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    itemMedicine.clear()
                    itemMedicine.addAll(it.data?.response ?: emptyList())
                    if (!isMedicineSelect) {
                        showDiagnosisDialog(isDiagnosis)
                    } else {
                        medicneAdapter?.notifyDataSetChanged()
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

        addPrescriptionViewModel.getdiagnosis.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {

                    progressDialog.setLoading(false)

                    itemDiagnosis.clear()
                    itemDiagnosis.addAll(it.data?.response ?: emptyList())
                    if (!isDiagnosisSelect) {
                        showDiagnosisDialog(isDiagnosis)
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

                    // Add the uploaded image name
//                    addPrescription?.image?.add(it.data?.image_name ?: "")
                    imageUrl  = it.data?.url.toString()
                    Toast.makeText(requireContext(),imageUrl, Toast.LENGTH_SHORT).show()

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

    fun setNotesText(note: String) {
//        binding.etMedicineName.setText(note)
    }

    fun showDiagnosisDialog(isDiagnosis: Boolean) {
        diagnosisDialog = DiagnosisDialogFragment(
            onNoteSelected = { note ->
                setNotesText(note)
            },
            this,  itemDiagnosis
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

    }

    private fun uploadFileOnServer(docImage: DocImage?) {
        val hashMap = HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }
}