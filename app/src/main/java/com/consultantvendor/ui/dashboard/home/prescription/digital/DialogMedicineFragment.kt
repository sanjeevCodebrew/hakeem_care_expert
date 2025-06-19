package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.DialogMedicineBinding
import com.consultantvendor.ui.adapter.MedicineAdapter
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerDialogFragment
import javax.inject.Inject

class DialogMedicineFragment(private val fragment: DigitalPrescriptionFragment) : DaggerDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel


    private lateinit var binding: DialogMedicineBinding

    private var itemMedicine = ArrayList<ResponseMedicine>()

    private var medicineAdapter: MedicineAdapter? = null

    private var isSelectMedicine = false

    private var isLastPage = false

    private var isFirstPage = true


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_medicine, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        setAdapter()
        listeners()
        bindObservers()
    }


    private fun initialise() {

        addPrescriptionViewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]


        hitApiMedicineList(true)

        binding.rvMedicine.isNestedScrollingEnabled = false
        binding.rvMedicine.setHasFixedSize(true)


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {

        binding.ivCross.setOnClickListener {
            dialog?.dismiss()
        }

        binding.etMedicineName.setOnClickListener {
            binding.clOptions.gone()
            hitApiMedicineList(true)
        }
        binding.rvMedicine.setOnTouchListener { _, event ->
            binding.rvMedicine.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.tvSave.setOnClickListener {
            when {
                binding.etDoses.text.toString().trim().isEmpty() -> {
                    binding.etDoses.showSnackBar("add doses")
                    return@setOnClickListener
                }

                binding.etFrequency.text.toString().trim().isEmpty() -> {
                    binding.etFrequency.showSnackBar("add frequency")
                    return@setOnClickListener
                }

                binding.etduration.text.toString().trim().isEmpty() -> {
                    binding.etduration.showSnackBar("add duration")
                    return@setOnClickListener
                }

                binding.etQuantity.text.toString().trim().isEmpty() -> {
                    binding.etQuantity.showSnackBar("add quantitiy")
                    return@setOnClickListener
                }
            }

            fragment.binding.tvMedicineName.setText(binding.etMedicineName.text.toString())
            fragment.binding.tvDoses.setText(binding.etDoses.text.toString())
            fragment.binding.tvfrequency.setText(binding.etFrequency.text.toString())
            fragment.binding.tvDuration.setText(binding.etduration.text.toString())
            fragment.binding.tvQuantity.setText(binding.etQuantity.text.toString())

            fragment.binding.layoutPrescriptionHeader.visible()
            fragment.binding.layoutMedicineRow.visible()
            fragment.binding.btnDelete.visible()
            fragment.binding.btnEdit.visible()

            dialog?.dismiss()


        }
    }

    private fun setAdapter() {
        medicineAdapter = MedicineAdapter(itemMedicine) { selectedItem ->
            binding.etMedicineName.setText(itemMedicine[selectedItem].description)
            binding.rvMedicine.gone()
            binding.tvMedicineName.gone()
            binding.tvAction.gone()
            binding.clOptions.visible()

        }
        binding.rvMedicine.adapter = medicineAdapter
    }


    private fun hitApiMedicineList( firstHit: Boolean,) {

        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }
            val hashMap = HashMap<String, String>()
            hashMap["page"] = "1"
            hashMap["itemNumber"] = ""
            hashMap["description"] = ""
            addPrescriptionViewModel.getItemList(hashMap)

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {

        addPrescriptionViewModel.getItemList.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.gone()

                    binding.rvMedicine.visible()
                    binding.tvMedicineName.visible()
                    binding.tvAction.visible()

                    itemMedicine.clear()
                    itemMedicine.addAll(it.data?.response?:emptyList())
                    medicineAdapter?.notifyDataSetChanged()
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

    }


}
