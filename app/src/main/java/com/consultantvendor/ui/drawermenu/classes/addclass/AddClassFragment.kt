package com.consultantvendor.ui.drawermenu.classes.addclass

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentAddClassBinding
import com.consultantvendor.ui.drawermenu.classes.ClassesViewModel
import com.consultantvendor.ui.loginSignUp.availability.OnTimeSelected
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class AddClassFragment : DaggerFragment(), OnDateSelected, OnTimeSelected {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentAddClassBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: ClassesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding =
                    DataBindingUtil.inflate(inflater, R.layout.fragment_add_class, container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindObservers()

        }
        return rootView
    }


    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[ClassesViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        binding.ilPrice.prefixText = getCurrencySymbol()
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.tvAddClass.setOnClickListener {
            checkValidation()
        }

        binding.etDate.setOnClickListener {
            DateUtils.openDatePicker(requireActivity(), this, null, (System.currentTimeMillis() - 36000))
        }

        binding.etTime.setOnClickListener {
            DateUtils.getTime(requireContext(), listener = this)
        }
    }

    private fun checkValidation() {
        when {
            binding.etTitle.text.toString().trim().isEmpty() -> {
                binding.etTitle.showSnackBar(getString(R.string.class_name))
            }
            binding.etDate.text.toString().trim().isEmpty() -> {
                binding.etDate.showSnackBar(getString(R.string.select_date))
            }
            binding.etTime.text.toString().trim().isEmpty() -> {
                binding.etTime.showSnackBar(getString(R.string.select_time))
            }
            binding.etPrice.text.toString().trim().isEmpty() -> {
                binding.etPrice.showSnackBar(getString(R.string.price_of_class))
            }
            isConnectedToInternet(requireContext(), true) -> {
                val hashMap = HashMap<String, String>()
                hashMap["category_id"] = userRepository.getUser()?.categoryData?.id ?: ""
                hashMap["name"] = binding.etTitle.text.toString().trim()
                hashMap["date"] = DateUtils.dateFormatForBackend(DateFormat.MON_DATE_YEAR,
                        DateFormat.DATE_FORMAT, binding.etDate.text.toString())
                hashMap["time"] = DateUtils.dateFormatForBackend(DateFormat.TIME_FORMAT,
                        DateFormat.TIME_FORMAT_24, binding.etTime.text.toString())
                hashMap["price"] = binding.etPrice.text.toString().trim()
                viewModel.addClass(hashMap)
            }
        }
    }


    private fun bindObservers() {

        viewModel.addClass.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    resultFragmentIntent(this, targetFragment ?: this,
                            AppRequestCode.ADD_CLASS, null)
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

    override fun onDateSelected(date: String) {
        binding.etDate.setText(date)
    }

    override fun onTimeSelected(time: Triple<String, Boolean, Boolean>) {
        binding.etTime.setText(time.first)
    }
}
