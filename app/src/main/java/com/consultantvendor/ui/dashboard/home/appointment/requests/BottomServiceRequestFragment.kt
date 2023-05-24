package com.consultantvendor.ui.dashboard.home.appointment.requests

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentServiceRequestBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.ui.dashboard.home.AppointmentViewModel
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject


class BottomServiceRequestFragment(private val request: Request) : DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: FragmentServiceRequestBinding

    private lateinit var viewModel: AppointmentViewModel

    private lateinit var progressDialog: ProgressDialog

    private var timeLimit = 60000L

    private var timer: CountDownTimer? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.isDraggable = false
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_service_request, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        listeners()
        setData()
        setTimer()
        bindObservers()
    }

    private fun initialise() {
        progressDialog = ProgressDialog(requireActivity())
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
    }

    private fun setTimer() {
        requireActivity().runOnUiThread {
            timeLimit = (request.remain_second ?: 45) * 1000
            binding.progressBar.max = timeLimit.toInt()
            timer = object : CountDownTimer(timeLimit, 16) {
                override fun onFinish() {
                    try {
                        dismiss()
                    }catch (e:Exception){}
                    //onAcceptRequest.onOrderTimeoutOrError()
                }

                override fun onTick(millisUntilFinished: Long) {
                    binding.progressBar.progress = timeLimit.toInt() - millisUntilFinished.toInt()
                }
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        startTimer()
    }

    private fun setData() {
        binding.tvName.text = request.from_user?.name
        loadImage(binding.ivPic, request.from_user?.profile_image,
                R.drawable.ic_profile_placeholder)

        binding.tvServiceTypeV.text = request.service_type

        if (request.booking_end_date.isNullOrEmpty()) {
            binding.tvBookingDateV.text = DateUtils.dateTimeFormatFromUTC(DateFormat.MON_DATE_YEAR, request.bookingDateUTC)
            binding.tvBookingTimeV.text = DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.bookingDateUTC)
        } else {
            val dateBooking = "${DateUtils.dateTimeFormatFromUTC(DateFormat.MON_DATE_YEAR, request.bookingDateUTC)} - " +
                    "${DateUtils.dateTimeFormatFromUTC(DateFormat.MON_DATE_YEAR, request.booking_end_date)}"
            binding.tvBookingDateV.text = dateBooking

            val timeBooking = "${DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.bookingDateUTC)} - " +
                    "${DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.booking_end_date)}"
            binding.tvBookingTimeV.text = timeBooking
        }

        binding.tvBookingPriceV.text = getCurrency(request.price)

        when (request.main_service_type) {
            ConsultType.HOME_VISIT -> {
                if (request.extra_detail?.service_address != null) {
                    binding.tvLocation.visible()
                    binding.tvViewMap.visible()
                    binding.tvLocation.text = request.extra_detail?.service_address
                }
            }
        }
    }

    private fun listeners() {
        binding.tvCancel.setOnClickListener {
            if (isConnectedToInternet(requireActivity(), true)) {
                val hashMap = HashMap<String, Any>()
                hashMap["id"] = request.id ?: ""

                viewModel.cancelRequestV2(hashMap)
            }
        }

        binding.tvAccept.setOnClickListener {
            showAcceptRequestDialog()
        }

        binding.tvViewMap.setOnClickListener {
            val address = request.extra_detail
            mapIntent(requireActivity(), address?.service_address ?: "",
                    address?.lat?.toDouble() ?: 0.0,
                    address?.long?.toDouble() ?: 0.0)
        }
    }

    private fun showAcceptRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(requireActivity(), R.string.accept_request,
                R.string.accept_request_message, R.string.accept_request, R.string.cancel, false,
                object : AlertDialogUtil.OnOkCancelDialogListener {
                    override fun onOkButtonClicked() {
                        hitApiAcceptRequest()
                    }

                    override fun onCancelButtonClicked() {
                    }
                }).show()
    }

    private fun hitApiAcceptRequest() {
        if (isConnectedToInternet(requireActivity(), true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["id"] = request.id ?: ""

            viewModel.acceptRequestV2(hashMap)
        }
    }

    private fun bindObservers() {
        viewModel.acceptRequest.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    dismiss()
                    timer?.cancel()
                    startActivity(Intent(requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT_DETAILS)
                            .putExtra(EXTRA_REQUEST_ID, it.data?.request_detail?.id))

                    val broadcastIntent = Intent()
                    broadcastIntent.action = PushType.NEW_REQUEST
                    broadcastIntent.putExtra(EXTRA_REQUEST_ID, it.data?.request_detail?.id)

                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(broadcastIntent)

                }
                Status.ERROR -> {
                    dismiss()
                    timer?.cancel()
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

        viewModel.cancelRequest.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    dismiss()
                    timer?.cancel()
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

    private fun startTimer() {
        timer?.start()
    }


}
