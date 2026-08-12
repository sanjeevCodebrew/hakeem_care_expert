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
import com.consultantvendor.data.models.DrugItem
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
import com.consultantvendor.utils.longToast
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

    private var itemMedicine = ArrayList<DrugItem>()

    private var medicineAdapter: MedicineAdapter? = null

    private var isLastPage = false

    private var isFirstPage = true

    private var isAddManualy = false

    private var medicineName = ""


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


    @SuppressLint("SuspiciousIndentation")
    private fun initialise() {

        addPrescriptionViewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]


      /*  if (!isEditMedicine) {
            hitApiMedicineList(true)
        }
        else
        {
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

        }*/
     /*   if (fragment is AddReportFragment) {
            if (isEditMedicine) {
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
        }*/

        if (fragment is AddReportFragment && isEditMedicine) {

            binding.clOptions.visible()

            val medicines = fragment.itemMedicineList
            val idx = fragment.editMedicineIndex
            if (idx in medicines.indices) {
                val m = medicines[idx]
                binding.etSearch.setText(m.description)
                binding.etDoses.setText(m.doses)
                binding.etFrequency.setText(m.frequency)
                binding.etduration.setText(m.duration)
                binding.etQuantity.setText(m.quantity)
            }
            fragment.itemMedicineList.clear()
        }

        if (fragment is com.consultantvendor.ui.dashboard.settings.prewritten.AddPreWrittenPrescriptionFragment && isEditMedicine) {

            binding.clOptions.visible()

            val medicines = fragment.itemMedicineList
            val idx = fragment.editMedicineIndex
            if (idx in medicines.indices) {
                val m = medicines[idx]
                binding.etSearch.setText(m.description)
                binding.etDoses.setText(m.doses)
                binding.etFrequency.setText(m.frequency)
                binding.etduration.setText(m.duration)
                binding.etQuantity.setText(m.quantity)
            }
            fragment.itemMedicineList.clear()
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

        binding.tvAddManualy.setOnClickListener {
            binding.clOptions.visible()
            binding.ilAddName.visible()
            binding.etSearch.setText("")
            isAddManualy  = true
        }

        binding.tvGetFromList.setOnClickListener {
            binding.clOptions.gone()
            binding.ilAddName.gone()
            binding.etSearch.setText("")
            binding.etDoses.setText("")
            binding.etFrequency.setText("")
            binding.etduration.setText("")
            binding.etQuantity.setText("")
            hitApiMedicineList(true)
            isAddManualy  = false
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

            if (isAddManualy){
                medicineName = binding.etAddName.text.toString()
                binding.ilAddName.visible()
            }
            else{
                binding.ilAddName.gone()
                medicineName = binding.etSearch.text.toString()
            }


            if (fragment is AddReportFragment) {
                fragment.itemMedicineList.add(
                    ItemModelMedicine(
                        description = medicineName,
                        doses = binding.etDoses.text.toString(),
                        frequency = binding.etFrequency.text.toString(),
                        duration = binding.etduration.text.toString(),
                        item_no = fragment.item_number,
                        quantity = binding.etQuantity.text.toString()
                    )
                )
               if (fragment.isEditMedicine){
                   fragment.isEditMedicine = false
               }
               fragment.adpterMedicineList?.notifyDataSetChanged()
            } else if (fragment is com.consultantvendor.ui.dashboard.settings.prewritten.AddPreWrittenPrescriptionFragment) {
                fragment.itemMedicineList.add(
                    ItemModelMedicine(
                        description = medicineName,
                        doses = binding.etDoses.text.toString(),
                        frequency = binding.etFrequency.text.toString(),
                        duration = binding.etduration.text.toString(),
                        item_no = "",
                        quantity = binding.etQuantity.text.toString()
                    )
                )
                if (fragment.isEditMedicine) {
                    fragment.isEditMedicine = false
                }
                fragment.adpterMedicineList?.notifyDataSetChanged()
            }
            dialog?.dismiss()

        }
    }

    private fun setAdapter() {
        medicineAdapter = MedicineAdapter(itemMedicine) { selectedItem ->
            val drug = itemMedicine[selectedItem]
            // Show generic name (ingredients) + strength — matches iOS design; avoids brand-name switch
            val label = listOfNotNull(drug.ingredients, drug.strength)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            binding.etSearch.setText(label)
            binding.rvMedicine.gone()
            binding.tvMedicineName.gone()
            binding.tvAction.gone()
            binding.clOptions.visible()

            if (fragment is AddReportFragment)
                fragment.item_number = drug.code ?: ""
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
//            hashMap["page"] = "1"
//            hashMap["itemNumber"] = ""
//            hashMap["description"] = binding.etSearch.text.toString()
            hashMap["name"] = binding.etSearch.text.toString()
            hashMap["code"] = ""
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
                    itemMedicine.addAll(it.data?.data?.data ?: emptyList())

                    if (!itemMedicine.isNotEmpty()){
                       requireActivity().longToast("Please search medicine using trade name only")
                    }

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
