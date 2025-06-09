package com.consultantvendor.ui.dashboard.home.appointment.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.Extra_payment
import com.consultantvendor.data.models.responses.Filter
import com.consultantvendor.data.models.responses.Page
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentAppointmentDetailsBinding
import com.consultantvendor.ui.adapter.CheckItemAdapter
import com.consultantvendor.ui.adapter.ImagesDocumentAdapter
import com.consultantvendor.ui.calling.CallingActivity
import com.consultantvendor.ui.chat.chatdetail.ChatDetailActivity
import com.consultantvendor.ui.dashboard.home.AppointmentViewModel
import com.consultantvendor.ui.dashboard.home.appointment.appointmentStatus.AppointmentStatusActivity
import com.consultantvendor.ui.dashboard.home.appointment.medicalhistory.MedicalHistoryFragment
import com.consultantvendor.ui.dashboard.home.prescription.BottomPrescriptionFragment
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.jitsimeet.JitsiActivity
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.set


class AppointmentDetailsFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appSocket: AppSocket


    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentAppointmentDetailsBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: AppointmentViewModel

    private lateinit var request: Request

    private var isReceiverRegistered = false

    private var alertDialog: AlertDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_appointment_details,
                container,
                false
            )
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()
            hitApi()
        }
        return rootView
    }

    private fun initialise() {
        progressDialog = ProgressDialog(requireActivity())
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        binding.clLoader.root.setBackgroundResource(R.color.colorWhite)

        binding.tvMedicalHistory.hideShowView(BuildConfig.FLAVOR == "homeDoctor")


    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvAccept.setOnClickListener {
            proceedRequest()
        }

        binding.tvMedicalHistory.setOnClickListener {
            val fragment = MedicalHistoryFragment()
            val bundle = Bundle()
            bundle.putSerializable(EXTRA_REQUEST_ID, request)
            fragment.arguments = bundle
            replaceResultFragment(this, fragment, R.id.container, AppRequestCode.ADD_CLASS)
        }

        binding.tvCall.setOnClickListener {
            if (isConnectedToInternet(requireContext(), true)) {
                val hashMap = HashMap<String, Any>()
                hashMap["request_id"] = request.id ?: ""
                hashMap["type"] = ConsultType.VIDEO_CALL

                viewModel.startCall(hashMap)
            }
        }

        binding.tvAddPrescription.setOnClickListener {
            proceedRequest()
        }

        binding.tvCancel.setOnClickListener {
            cancelAppointment()
        }

        binding.tvMarkComplete.setOnClickListener {
            showMarkCompleteDialog()
        }


        binding.tvAddReports.setOnClickListener {

            if (request.is_report==false) {
                registerActivityResult.launch(
                    Intent(requireActivity(), DrawerActivity::class.java)
                        .putExtra(PAGE_TO_OPEN, DrawerActivity.ADD_REPORTS)
                        .putExtra(EXTRA_REQUEST_ID, request)
                )
            }
            else
            {
                val popup = PopupMenu(requireContext(), binding.tvAddReports)
                popup.menuInflater.inflate(R.menu.menu_prescription, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.item_view -> {
                            val link = "https://hakeemcare.hakeemcare.com/medical-report?request_id=${request.id}"
                            openPdf(requireActivity(), link,false,true)
                        }
                        R.id.item_edit -> {
                            registerActivityResult.launch(
                                Intent(requireActivity(), DrawerActivity::class.java)
                                    .putExtra(PAGE_TO_OPEN, DrawerActivity.ADD_REPORTS)
                                    .putExtra(EXTRA_REQUEST_ID, request)
                            )
                        }
                        R.id.item_download -> {
                            val link = getString(
                                R.string.pdf_link,
                                BuildConfig.BASE_URL,
                                request.id,
                                BuildConfig.APP_UNIQUE_ID
                            )
                            openPdf(requireActivity(), link, true)
                        }
                    }
                    true
                }
                popup.show()
            }
        }

        binding.tvChat.setOnClickListener {
//            registerActivityResult.launch(
//                Intent(context, ChatDetailActivity::class.java)
//                    .putExtra(USER_ID, request.to_user?.id)
//                    .putExtra(USER_NAME, request.to_user?.name)
//                    .putExtra(EXTRA_REQUEST_ID, request.id)
//                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//            )
            val intent = Intent(requireContext(), ChatDetailActivity::class.java)
                     .putExtra(USER_ID, request.from_user?.id)
                     .putExtra(USER_NAME, request.from_user?.name)
                     .putExtra(EXTRA_REQUEST_ID, request.id)
                     .putExtra(EXTRA_IS_FIRST, true)
                     .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                      startActivity(intent)

        }

        binding.tvViewMap.setOnClickListener {
            val address = request.extra_detail
            mapIntent(
                requireActivity(), address?.service_address ?: "",
                address?.lat?.toDouble() ?: 0.0,
                address?.long?.toDouble() ?: 0.0
            )
        }

        binding.tvAskPayment.setOnClickListener {
            val fragment = BottomExtraChargesFragment(this)
            fragment.show(requireActivity().supportFragmentManager, fragment.tag)
        }
    }

    fun extraPayment(amount: String, description: String) {
        if (isConnectedToInternet(requireContext(), true)) {
            val extraPayment = Extra_payment()
            extraPayment.request_id = request.id ?: ""
            extraPayment.balance = amount
            extraPayment.description = description
            viewModel.extraPayment(extraPayment)
        }
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["request_id"] = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
            viewModel.requestDetail(hashMap)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun setData() {
        binding.tvAccept.visible()
        binding.tvCancel.hideShowView(request.canCancel)
        binding.tvAddPrescription.gone()
        binding.tvMarkComplete.gone()
        binding.tvAskPayment.gone()

        binding.tvCall.hideShowView(BuildConfig.FLAVOR == "nurseLynx")

        binding.tvName.text = request.from_user?.name
        loadImage(
            binding.ivPic, request.from_user?.profile_image,
            R.drawable.ic_profile_placeholder
        )
        binding.tvAge.text =
            "${getString(R.string.age)} ${getAge(request.from_user?.profile?.dob)} ${getString(R.string.years)}"
        binding.tvCountry.text =
            "${getString(R.string.country)}: ${request.from_user?.profile?.country}"
        binding.tvAge.hideShowView(!request.from_user?.profile?.dob.isNullOrEmpty())
        binding.tvCountry.hideShowView(!request.from_user?.profile?.country.isNullOrEmpty())


        binding.tvServiceTypeV.text = request.service_type
        binding.tvDistanceV.text = request.extra_detail?.distance ?: ""
        binding.tvLocation.text = request.extra_detail?.service_address

        binding.tvClinicNameV.text = request.to_user?.clinic_name


        if (request.insurance_name?.isNotEmpty()!! || request.insurance_number?.isNotEmpty()!!) {
            binding.tvInsuranceName.visible()
            binding.tvInsuranceNameV.visible()
            binding.tvResidentId.visible()
            binding.tvResidentIdV.visible()

            binding.tvInsuranceNameV.text = request.insurance_name
            binding.tvResidentIdV.text = request.insurance_number
        }

        if (BuildConfig.FLAVOR == "nurseLynx" && !request.booking_end_date.isNullOrEmpty()) {
            val dateBooking = "${
                DateUtils.dateTimeFormatFromUTC(
                    DateFormat.MON_DATE_YEAR,
                    request.bookingDateUTC
                )
            } - " +
                    DateUtils.dateTimeFormatFromUTC(
                        DateFormat.MON_DATE_YEAR,
                        request.booking_end_date
                    )
            binding.tvBookingDateV.text = dateBooking

            val timeBooking = "${
                DateUtils.dateTimeFormatFromUTC(
                    DateFormat.TIME_FORMAT,
                    request.bookingDateUTC
                )
            } - " +
                    DateUtils.dateTimeFormatFromUTC(
                        DateFormat.TIME_FORMAT,
                        request.booking_end_date
                    )
            binding.tvBookingTimeV.text = timeBooking
        } else {
            binding.tvBookingDateV.text =
                DateUtils.dateTimeFormatFromUTC(DateFormat.MON_DATE_YEAR, request.bookingDateUTC)
            binding.tvBookingTimeV.text =
                DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.bookingDateUTC)
        }

        binding.tvBookingPriceV.text = getCurrency(request.price)

        binding.tvStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.colorPrimary
            )
        )

        if (request.is_prescription == true)
            binding.tvAddPrescription.text = getString(R.string.prescriptions)
        else
            binding.tvAddPrescription.text = getString(R.string.add_prescription)

        if (request.is_report == true){
            binding.tvAddReports.text = getString(R.string.reports)
        }
        else{
            binding.tvAddReports.text = getString(R.string.add_reports)
        }

        when (request.main_service_type) {
            ConsultType.HOME_VISIT -> {
                if (request.extra_detail?.service_address != null) {
                    binding.tvLocation.visible()
                    binding.tvPatientAddress.visible()
                    binding.tvViewMap.visible()
                    binding.tvLocation.text = request.extra_detail?.service_address
                }
            }
        }

        /*Cancel reason*/
        binding.tvCancelReason.hideShowView(!request.cancel_reason.isNullOrEmpty())
        binding.tvCancelReason.text = getString(
            R.string.reason_of_cancel, request.cancel_reason
                ?: ""
        )

        when (request.status) {
            CallAction.PENDING -> {
                binding.tvStatus.text = getString(R.string.new_request)
                binding.tvAccept.text = getString(R.string.accept_request)
            }

            CallAction.ACCEPT -> {
                binding.tvStatus.text = getString(R.string.accepted)
                binding.tvAccept.text = getString(R.string.start_request)
                binding.tvCancel.gone()

                if (request.to_user?.categoryData?.parent_cat_name=="telehealth"|| request.to_user?.categoryData?.parent_cat_name=="urgent-consultation"){
                    binding.tvChat.visible()
                }

                when (request.main_service_type) {
                    ConsultType.AUDIO_CALL, ConsultType.VIDEO_CALL -> {
                        binding.tvMarkComplete.visible()
                        binding.tvCall.gone()
                    }
                }
                extraPayment()
            }

            CallAction.INPROGRESS -> {
                binding.tvStatus.text = getString(R.string.inprogess)
                binding.tvCancel.gone()
                binding.tvAccept.gone()

                binding.tvMarkComplete.visible()
                extraPayment()
            }

            CallAction.START -> {
                binding.tvStatus.text = getString(R.string.inprogess)
                binding.tvAccept.text = getString(R.string.track_status)
                binding.tvCancel.gone()
                extraPayment()
            }

            CallAction.REACHED -> {
                binding.tvStatus.text = getString(R.string.reached_destination)
                binding.tvAccept.text = getString(R.string.track_status)
                binding.tvCancel.gone()
                extraPayment()
            }

            CallAction.START_SERVICE -> {
                binding.tvStatus.text = getString(R.string.started)
                binding.tvAccept.gone()
                binding.tvCancel.gone()

                binding.tvMarkComplete.visible()
                extraPayment()
            }

            CallAction.COMPLETED -> {
                binding.tvStatus.text = getString(R.string.completed)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.textColorGreen
                    )
                )
                binding.tvAccept.gone()
                binding.tvCancel.gone()
                binding.tvCall.gone()

                if (request.categoryData?.cat_slug == "ask-now") {
                    binding.tvAddPrescription.gone()
                    binding.tvAddReports.gone()
                } else {
                    binding.tvAddPrescription.visible()
                    if (request.to_user?.categoryData?.parent_cat_name=="telehealth" || request.to_user?.categoryData?.parent_cat_name=="urgent-consultation")
                    binding.tvChat.visible()
                    binding.tvAddReports.visible()
                }
                extraPayment()
            }

            CallAction.FAILED -> {
                binding.tvAccept.gone()
                binding.tvStatus.text = getString(R.string.no_show)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorCancel
                    )
                )
                binding.tvCancel.gone()
                binding.tvCall.gone()
            }

            CallAction.CANCELED -> {
                binding.tvStatus.text = getString(R.string.canceled)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorCancel
                    )
                )
                binding.tvAccept.gone()
                binding.tvCancel.gone()
                binding.tvCall.gone()
            }

            CallAction.CANCEL_SERVICE -> {
                binding.tvStatus.text = getString(R.string.canceled_service)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorCancel
                    )
                )
                binding.tvCancel.gone()
                binding.tvAccept.gone()
                binding.tvCall.gone()
            }

            else -> {
                binding.tvStatus.text = getString(R.string.new_request)
            }
        }

        /*Symptom*/
        binding.tvSymptomDec.text = request.symptom_details
        binding.tvSymptomDec.hideShowView(binding.tvSymptomDec.text.isNotEmpty())

        val symptomImages = ArrayList<DocImage>()
        symptomImages.addAll(request.symptom_images ?: emptyList())
        val adapterSymptomImage = ImagesDocumentAdapter(this, symptomImages)
        binding.rvSymptomDoc.adapter = adapterSymptomImage
        binding.rvSymptomDoc.hideShowView(symptomImages.isNotEmpty())

        binding.rvSymptomListing.layoutManager = GridLayoutManager(requireContext(), 3)
        val items = ArrayList<Filter>()
        items.addAll(request.symptoms ?: emptyList())
        val adapterSymptom = CheckItemAdapter(this, true, items)
        binding.rvSymptomListing.adapter = adapterSymptom

        binding.tvSymptom.hideShowView(binding.tvSymptomDec.text.isNotEmpty() || items.isNotEmpty())


        /*CarePlan*/
        val itemsCarePlan = ArrayList<Filter>()
        if (request.tier_detail != null) {
            request.tier_detail?.isSelected = true
            itemsCarePlan.add(request.tier_detail ?: Filter())
        }

        binding.tvCarePlan.hideShowView(itemsCarePlan.isNotEmpty())
        binding.rvCatePlan.hideShowView(itemsCarePlan.isNotEmpty())
        val adapterCarePlan = CarePlanAdapter(this, CarePlanAdapter.PlanOption.MAIN, itemsCarePlan)
        binding.rvCatePlan.adapter = adapterCarePlan

        /*Pre Assessment*/
        val itemsPreAssessment = ArrayList<Page>()
        itemsPreAssessment.addAll(request.question_answers ?: emptyList())
        binding.tvPreAssessment.hideShowView(itemsPreAssessment.isNotEmpty())
        binding.tvPreAssessmentV.hideShowView(itemsPreAssessment.isNotEmpty())

        var answers = ""
        itemsPreAssessment.forEachIndexed { index, page ->
            answers += when (index) {
                1 -> "${index + 1}. ${page.question}\nAns: ${page.answer} Kg\n\n"
                2 -> "${index + 1}. ${page.question}\nAns: ${page.answer} cm\n\n"
                else -> "${index + 1}. ${page.question}\nAns: ${page.answer}\n\n"
            }
        }
        binding.tvPreAssessmentV.text = answers
    }

    fun updateCarePlan(item: Filter) {
        AlertDialogUtil.instance.createOkCancelDialog(
            requireActivity(), R.string.mark_complete,
            R.string.mark_complete_message, R.string.mark_complete, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    if (isConnectedToInternet(requireContext(), true)) {
                        val hashMap = HashMap<String, Any>()
                        hashMap["id"] = item.id ?: 0
                        hashMap["request_id"] = request.id ?: ""
                        hashMap["status"] = CallAction.COMPLETED
                        viewModel.updateCarePlan(hashMap)
                    }
                }

                override fun onCancelButtonClicked() {
                }
            }).show()


    }


    private fun extraPayment() {
        if (request.extra_payment == null) {
            when (BuildConfig.FLAVOR) {
                "homeDoctor" -> {
                    binding.tvAskPayment.visible()
                }

                else -> binding.tvAskPayment.gone()
            }
        } else {
            binding.tvAskPayment.gone()
            binding.tvExtraPayment.visible()
            binding.tvExtraPaymentAmount.visible()
            binding.tvExtraPaymentDesc.visible()
            binding.tvExtraStatus.visible()
            binding.tvExtraPaymentAmount.text =
                getString(R.string.amount_s, getCurrency(request.extra_payment?.balance))
            binding.tvExtraPaymentDesc.text = request.extra_payment?.description
            binding.tvExtraStatus.text = "(${request.extra_payment?.status})"

            when (request.extra_payment?.status) {
                CallAction.PENDING ->
                    binding.tvExtraStatus.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorPending
                        )
                    )

                CallAction.PAID ->
                    binding.tvExtraStatus.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.textColorGreen
                        )
                    )
            }
        }
    }

    private fun proceedRequest() {
        when (request.status) {
            CallAction.PENDING -> {
                showAcceptRequestDialog()
            }

            CallAction.ACCEPT -> {
                showInitiateRequestDialog()
            }

            CallAction.COMPLETED -> {
                if (request.is_prescription == true) {
                    if (!request.pre_scription?.type.isNullOrEmpty()) {
                        val popup = PopupMenu(requireContext(), binding.tvAddPrescription)
                        popup.menuInflater.inflate(R.menu.menu_prescription, popup.menu)

                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.item_view -> {
                                    val link = getString(
                                        R.string.pdf_link,
                                        BuildConfig.BASE_URL,
                                        request.id,
                                        BuildConfig.APP_UNIQUE_ID
                                    )
                                    openPdf(requireActivity(), link, true)
                                }

                                R.id.item_edit -> {
                                    registerActivityResult.launch(
                                        Intent(requireActivity(), DrawerActivity::class.java)
                                            .putExtra(PAGE_TO_OPEN, request.pre_scription?.type)
                                            .putExtra(EXTRA_REQUEST_ID, request)
                                    )
                                }

                                R.id.item_download -> {
                                    val link = getString(
                                        R.string.pdf_link,
                                        BuildConfig.BASE_URL,
                                        request.id,
                                        BuildConfig.APP_UNIQUE_ID
                                    )

                                  /*  val finalLink = if (link.contains("?")) "$link&download" else "$link?download"

                                    Log.d("PDF_DOWNLOAD", "Enqueuing download for URL: $finalLink")

                                    val downloadRequest = DownloadManager.Request(Uri.parse(finalLink))
                                        .setTitle("Downloading PDF")
                                        .setDescription("Please wait while the PDF is downloading...")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "report_${request.id}.pdf")
                                        .setAllowedOverMetered(true)
                                        .setAllowedOverRoaming(true)

                                    val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    downloadManager.enqueue(downloadRequest)


                                    requireActivity().longToast("Downloading complete")*/

                                    openPdf(requireActivity(), link, true)
                                }
                            }
                            true
                        }

                        popup.show()
                    }
                }
                else {
                    val fragment = BottomPrescriptionFragment(this, request)
                    fragment.show(requireActivity().supportFragmentManager, fragment.tag)
                }
            }

            CallAction.START, CallAction.REACHED -> {
                registerActivityResult.launch(
                    Intent(requireActivity(), AppointmentStatusActivity::class.java)
                        .putExtra(EXTRA_REQUEST_ID, request)
                )
            }

            CallAction.START_SERVICE -> {
                showMarkCompleteDialog()
            }
        }
    }

    private fun showAcceptRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(
            requireActivity(), R.string.accept_request,
            R.string.accept_request_message, R.string.accept_request, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiAcceptRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun showMarkCompleteDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(
            requireActivity(), R.string.mark_complete,
            R.string.mark_complete_message, R.string.mark_complete, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiCompleteRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun showInitiateRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(
            requireActivity(), R.string.start_request,
            R.string.start_request_message, R.string.start_request, R.string.cancel, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    hitApiStartRequest()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun hitApiAcceptRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["request_id"] = request.id ?: ""

            viewModel.acceptRequest(hashMap)
        }
    }


    private fun hitApiCompleteRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            val hashMap = java.util.HashMap<String, Any>()
            hashMap["request_id"] = request.id ?: ""
            hashMap["status"] = CallAction.COMPLETED

            viewModel.callStatus(hashMap)
        }
    }

    private fun hitApiStartRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            when (request.main_service_type) {
                ConsultType.HOME_VISIT -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["request_id"] = request.id ?: ""
                    hashMap["status"] = CallAction.START

                    viewModel.callStatus(hashMap)
                }

                else -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["request_id"] = request.id ?: ""

                    viewModel.startRequest(hashMap)
                }
            }
        }
    }

    private fun cancelAppointment() {
        val dialogBuilder = AlertDialog.Builder(requireContext())

        val dialogView = layoutInflater.inflate(R.layout.alert_cancel_request, null)
        dialogBuilder.setView(dialogView)

        val etReason: EditText = dialogView.findViewById(R.id.etReason)
        val tvCancel: TextView = dialogView.findViewById(R.id.tvCancel)
        val tvClose: TextView = dialogView.findViewById(R.id.tvClose)

        alertDialog = dialogBuilder.create()
        alertDialog?.show()

        tvCancel.setOnClickListener {
            if (etReason.text.toString().trim().isEmpty()) {
                etReason.showSnackBar(getString(R.string.reason))
            } else {
                if (isConnectedToInternet(requireContext(), true)) {
                    val hashMap = HashMap<String, String>()
                    hashMap["request_id"] = request.id ?: ""
                    hashMap["cancel_reason"] = etReason.text.toString().trim()
                    viewModel.cancelRequest(hashMap)
                }
            }
        }

        tvClose.setOnClickListener {
            alertDialog?.dismiss()
        }
    }

    private fun bindObservers() {
        viewModel.requestDetail.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.setBackgroundResource(0)
                    binding.clLoader.root.gone()
                    request = it.data?.request_detail ?: Request()
                    setData()

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

        viewModel.updateCarePlan.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    hitApi()
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

        viewModel.acceptRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi()
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

        viewModel.startRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi()

                    when (request.main_service_type) {
                        ConsultType.CHAT -> {
                            requireActivity().longToast(getString(R.string.starting_chat))

                            if (!appSocket.isConnected)
                                appSocket.init()

                            startActivity(
                                Intent(requireActivity(), ChatDetailActivity::class.java)
                                    .putExtra(USER_ID, request.from_user?.id)
                                    .putExtra(USER_NAME, request.from_user?.name)
                                    .putExtra(EXTRA_REQUEST_ID, request.id)
                                    .putExtra(EXTRA_IS_FIRST, true)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }

                        ConsultType.AUDIO_CALL, ConsultType.VIDEO_CALL -> {
                            requireActivity().longToast(getString(R.string.starting_call))

                            request.call_id = it.data?.call_id
                            startActivity(
                                Intent(requireContext(), CallingActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(EXTRA_REQUEST_ID, request)
                            )
                        }
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

        viewModel.startCall.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().longToast(getString(R.string.starting_call))

                    request.call_id = it.data?.call_id
                    startActivity(
                        Intent(requireContext(), CallingActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_REQUEST_ID, request)
                    )
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

        viewModel.callStatus.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi()

                    if (request.main_service_type == ConsultType.HOME_VISIT && request.status != CallAction.START_SERVICE) {
                        request.status = CallAction.START
                        registerActivityResult.launch(
                            Intent(requireActivity(), AppointmentStatusActivity::class.java)
                                .putExtra(EXTRA_REQUEST_ID, request)
                        )
                    } else if (it.data?.status == CallAction.COMPLETED) {
                        val broadcastIntent = Intent()
                        broadcastIntent.action = PushType.COMPLETED
                        LocalBroadcastManager.getInstance(requireContext())
                            .sendBroadcast(broadcastIntent)
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

        viewModel.cancelRequest.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    alertDialog?.dismiss()
                    requireActivity().setResult(Activity.RESULT_OK)
                    hitApi()
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

        viewModel.extraPayment.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    hitApi()
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

    val registerActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                //val intent = result.data
                requireActivity().setResult(Activity.RESULT_OK)
                hitApi()
            }
        }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            intentFilter.addAction(PushType.CANCELED_REQUEST)
            intentFilter.addAction(PushType.PATIENT_ADDED_SYMPTOMS)
            intentFilter.addAction(PushType.REQUEST_FAILED)
            intentFilter.addAction(PushType.COMPLETED)
            intentFilter.addAction(PushType.PAID_EXTRA_PAYMENT)
            intentFilter.addAction(MEDICAL_HISTORY)
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(refreshData, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshData)
            isReceiverRegistered = false
        }
    }

    private val refreshData = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.REQUEST_COMPLETED, PushType.COMPLETED, PushType.PATIENT_ADDED_SYMPTOMS, MEDICAL_HISTORY,
                PushType.CANCELED_REQUEST, PushType.REQUEST_FAILED, PushType.PAID_EXTRA_PAYMENT -> {
                    if (request.id == intent.getStringExtra(EXTRA_REQUEST_ID))
                        hitApi()
                }
            }
        }
    }

    companion object {
        const val MEDICAL_HISTORY = "MEDICAL_HISTORY"
    }
}


