package com.consultantvendor.utils.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.ui.adapter.DiagnosisAdapter
import com.consultantvendor.ui.dashboard.home.prescription.digital.DigitalPrescriptionFragment

class DiagnosisDialogFragment(
    private val fragment: DigitalPrescriptionFragment,
    private val itemList: ArrayList<ResponseMedicine>
) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_diagnosis_list, container, false)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewDialog)

        val adapter = DiagnosisAdapter(itemList) { selectedItem ->
//            Toast.makeText(context, "Clicked: $selectedItem", Toast.LENGTH_SHORT).show(
            fragment.binding.etMedicineName.setText(itemList[selectedItem].category)
            fragment.binding.etDoses.setText(itemList[selectedItem].sku)
            fragment.binding.etfrequency.setText(itemList[selectedItem].sfda)
            dialog?.dismiss()
        }

        recyclerView.adapter = adapter

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}