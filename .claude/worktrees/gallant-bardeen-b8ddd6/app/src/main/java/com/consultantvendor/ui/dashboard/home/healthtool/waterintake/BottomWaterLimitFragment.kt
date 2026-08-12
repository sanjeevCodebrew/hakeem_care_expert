package com.consultantvendor.ui.dashboard.home.healthtool.waterintake

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.databinding.BottomWaterLimitBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.ui.dashboard.home.healthtool.protienintake.ProteinIntakeFragment
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.hideKeyboard
import com.consultantvendor.utils.showSnackBar
import javax.inject.Inject


class BottomWaterLimitFragment(private val fragment: Fragment) :
    DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: BottomWaterLimitBinding


    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        //dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_water_limit, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        listeners()
    }

    private fun initialise() {
        if (fragment is WaterIntakeFragment) {
            if (fragment.waterIntake?.limit != null)
                binding.etWaterIntake.setText(fragment.waterIntake?.limit.toString())
        } else if (fragment is ProteinIntakeFragment) {
            binding.tvDailyLimit.text = getString(R.string.set_you_protein_limit)
            binding.ilWaterIntake.hint = getString(R.string.protein_intake_grams)
            binding.tvSymbol.text = getString(R.string.g)

            if (fragment.waterIntake?.limit != null)
                binding.etWaterIntake.setText(fragment.waterIntake?.limit.toString())
        }

    }

    private fun listeners() {
        binding.tvSetLimit.setOnClickListener {
            binding.etWaterIntake.hideKeyboard()
            if (binding.etWaterIntake.text.toString().isEmpty()) {
                binding.etWaterIntake.showSnackBar(getString(R.string.set_daily_limit))
            } else {
                if (fragment is WaterIntakeFragment)
                    fragment.setDailyLimit(binding.etWaterIntake.text.toString())
                else if (fragment is ProteinIntakeFragment)
                    fragment.setDailyLimit(binding.etWaterIntake.text.toString())

                dialog?.dismiss()
            }
        }
    }
}
