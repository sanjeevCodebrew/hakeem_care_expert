package com.consultantvendor.ui.dashboard.home.appointment.detail

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.databinding.BottomExtraChargesBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.utils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import javax.inject.Inject


class BottomExtraChargesFragment(val fragment: AppointmentDetailsFragment) : DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: BottomExtraChargesBinding


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_extra_charges, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        listeners()
    }

    private fun initialise() {
        editTextScroll(binding.etDescription)
        binding.ilPayment.prefixText = getCurrencySymbol()
    }

    private fun listeners() {
        binding.tvPayment.setOnClickListener {
            binding.etPayment.hideKeyboard()
            when {
                binding.etPayment.text.toString().isEmpty() || binding.etPayment.text.toString().toInt() < 1 -> {
                    requireActivity().longToast(getString(R.string.enter_amount))
                }
                binding.etDescription.text.toString().isEmpty() -> {
                    requireActivity().longToast(getString(R.string.enter_description))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    dialog?.dismiss()
                    fragment.extraPayment(binding.etPayment.text.toString(), binding.etDescription.text.toString())
                }
            }
        }
    }
}
