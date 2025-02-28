package com.consultantvendor.ui.dashboard.home.healthtool.bmichecker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentBmiCheckerBinding
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.hideKeyboard
import com.consultantvendor.utils.showSnackBar
import com.ekndev.gaugelibrary.Range
import dagger.android.support.DaggerFragment
import java.text.DecimalFormat
import javax.inject.Inject

class BmiCheckerFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentBmiCheckerBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private val decimalFormat = DecimalFormat("0.00")

    private var selectedGender = ""

    private lateinit var viewModel: LoginViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bmi_checker, container, false)
            rootView = binding.root

            initialise()
            listeners()
        }
        return rootView
    }


    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        selectedGender = getString(R.string.male)
        setSpinnerGender()
        setGauge()
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.ivCalculateBmi.setOnClickListener {

            binding.ivCalculateBmi.hideKeyboard()
            val weight = binding.etWeight.text.toString().trim()
            val height = binding.etHeight.text.toString().trim()
            if (weight.isEmpty() || weight.toInt() <= 0) {
                binding.etWeight.showSnackBar(getString(R.string.error_empty_weight))
                return@setOnClickListener
            } else if (height.isEmpty() || height.toInt() <= 0) {
                binding.etHeight.showSnackBar(getString(R.string.height_empty_validation))
                return@setOnClickListener
            }
            val bmi: Double = calculateBmi(weight.toDouble(), height.toDouble()) ?: 0.0
            binding.tvBmi.text = decimalFormat.format(bmi)
            binding.halfGauge.value = decimalFormat.format(bmi).toDouble()

            if (bmi > 0 && bmi < 18.5) {
                binding.tvBmiStatus.text = getString(R.string.underweight)
                binding.tvRange.text = getString(R.string.range0to18)
                binding.tvBmiStatus.setTextColor(Color.parseColor("#FF52C1F6"))
                setGaugeValue(bmi, 10.0, 18.4)

            } else if (bmi >= 18.5 && bmi < 25) {
                binding.tvBmiStatus.text = getString(R.string.normal)
                binding.tvRange.text = getString(R.string.range18to25)
                binding.tvBmiStatus.setTextColor(Color.parseColor("#FF48D3B3"))
                setGaugeValue(bmi, 20.0, 24.9)


            } else if (bmi >= 25) {
                binding.tvBmiStatus.text = getString(R.string.overweight)
                binding.tvRange.text = getString(R.string.range_above25)
                binding.tvBmiStatus.setTextColor(Color.parseColor("#FFFD5E5E"))
                //  setGaugeValue(bmi,3.0,40.0)
                val perc = (100 * bmi) / 40.0
                binding.halfGauge.value = perc / 3
            }
        }
    }

    private fun setGauge() {
        val range = Range()
        range.color = Color.parseColor("#FF52C1F6")
        range.from = 0.0
        range.to = 10.0

        val range2 = Range()
        range2.color = Color.parseColor("#FF48D3B3")
        range2.from = 10.0
        range2.to = 20.0

        val range3 = Range()
        range3.color = Color.parseColor("#FFFD5E5E")
        range3.from = 20.0
        range3.to = 30.0

        binding.halfGauge.addRange(range)
        binding.halfGauge.addRange(range2)
        binding.halfGauge.addRange(range3)

        binding.halfGauge.minValue = 0.0
        binding.halfGauge.maxValue = 30.0
        binding.halfGauge.value = 0.0
    }

    private fun calculateBmi(weight: Double, height: Double): Double? {
        val bmi: Double? = (weight / (height * height)) * 10000
        return bmi
    }

    private fun setGaugeValue(bmi: Double, blockValue: Double, range: Double) {
        val perc = (100 * bmi) / range
        binding.halfGauge.value = (blockValue * perc) / 100
    }

    private fun setSpinnerGender() {
        val genders = arrayOf(getString(R.string.male), getString(R.string.female))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        binding.spinnerGender.adapter = adapter

        binding.spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                selectedGender = genders[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
    }
}