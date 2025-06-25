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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.data.models.responses.Prescription
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.DialogMedicineBinding
import com.consultantvendor.ui.adapter.MedicineAdapter
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelMedicine
import com.consultantvendor.ui.dashboard.home.reports.AddReportFragment
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerDialogFragment
import javax.inject.Inject

class DialogMedicineFragment(
    private val fragment: Fragment?,
    private var isEditMedicine: Boolean,
    private val prescription: List<Prescription>?
) : DaggerDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel

    private lateinit var binding: DialogMedicineBinding

    private var itemMedicine = ArrayList<ResponseMedicine>()

    private var medicineAdapter: MedicineAdapter? = null

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


        if (!isEditMedicine) {
            hitApiMedicineList(true)
        }
        else{

            if (fragment is AddReportFragment) {
                binding.clOptions.visible()
                fragment.itemMedicineList.clear()
                prescription?.forEach {
                    binding.etSearch.setText(it.description)
                    binding.etDoses.setText(it.doses)
                    binding.etFrequency.setText(it.frequency)
                    binding.etduration.setText(it.duration)
                    binding.etQuantity.setText(it.quantity)
                }
            }

        }

        binding.rvMedicine.isNestedScrollingEnabled = false
        binding.rvMedicine.setHasFixedSize(true)


    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    private fun listeners() {

        binding.ivCross.setOnClickListener {
            dialog?.dismiss()
        }

        binding.etSearch.setOnClickListener {
            binding.clOptions.gone()
            hitApiMedicineList(true)
        }

        binding.ivSearch.setOnClickListener {
            hitApiMedicineList(true)
        }

        binding.rvMedicine.setOnTouchListener { _, event ->
            binding.rvMedicine.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.tvSave.setOnClickListener {
            when {
                binding.etDoses.text.toString().trim().isEmpty() -> {
                    binding.etDoses.showSnackBar(getString(R.string.add_doses))
                    return@setOnClickListener
                }
                binding.etFrequency.text.toString().trim().isEmpty() -> {
                    binding.etFrequency.showSnackBar(getString(R.string.add_frequency))
                    return@setOnClickListener
                }
                binding.etduration.text.toString().trim().isEmpty() -> {
                    binding.etduration.showSnackBar(getString(R.string.add_duration))
                    return@setOnClickListener
                }
                binding.etQuantity.text.toString().trim().isEmpty() -> {
                    binding.etQuantity.showSnackBar(getString(R.string.add_quantity))
                    return@setOnClickListener
                }

            }


            if (fragment is AddReportFragment) {
                fragment.itemMedicineList.add(
                    ItemModelMedicine(
                        description = binding.etSearch.text.toString(),
                        doses = binding.etDoses.text.toString(),
                        frequency = binding.etFrequency.text.toString(),
                        duration = binding.etduration.text.toString(),
                        item_no = fragment.item_number,
                        quantity = binding.etQuantity.text.toString()
                    )

                )


               if (fragment.isEditMedicine){
                   fragment.isEditMedicine =false
               }
               fragment.adpterMedicineList?.notifyDataSetChanged()
            }
            dialog?.dismiss()

        }
    }

    private fun setAdapter() {
        medicineAdapter = MedicineAdapter(itemMedicine) { selectedItem ->
            binding.etSearch.setText(itemMedicine[selectedItem].description)
            binding.rvMedicine.gone()
            binding.tvMedicineName.gone()
            binding.tvAction.gone()
            binding.clOptions.visible()

            if (fragment is AddReportFragment)
            fragment.item_number = itemMedicine[selectedItem].sku

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
            hashMap["description"] = binding.etSearch.text.toString()
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
