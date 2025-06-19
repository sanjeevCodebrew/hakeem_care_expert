package com.consultantvendor.utils.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.ResponseMedicine
import com.consultantvendor.data.models.responses.Response
import com.consultantvendor.ui.adapter.DiagnosisAdapter
import com.consultantvendor.ui.adapter.MedicineAdapter
import com.consultantvendor.ui.dashboard.home.prescription.digital.DigitalPrescriptionFragment
import com.consultantvendor.utils.visible

class DiagnosisDialogFragment(
    private val onNoteSelected: (String) -> Unit,

    private val fragment: DigitalPrescriptionFragment,
    private val itemList: ArrayList<ResponseMedicine>,
    private val itemDiagnosis: ArrayList<Response>,
    private val isDiagnosis: Boolean
) : DialogFragment() {

    private var diagnosisAdapter: DiagnosisAdapter? = null
    private var isSelectDiagnosis = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_diagnosis_list, container, false)
        val tvTitle: TextView = view.findViewById(R.id.dialogTitle)
        val etSearch: EditText = view.findViewById(R.id.etSearch)
        val ivSearch: ImageView = view.findViewById(R.id.ivSearch)
        val ivCross: ImageView = view.findViewById(R.id.ivCancel)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewDialog)
        val tvDone: TextView = view.findViewById(R.id.tvDone)


            tvTitle.text = fragment.getString(R.string.select_diagnosis)
            diagnosisAdapter = DiagnosisAdapter(itemDiagnosis) { selectedItem ->
//                fragment.binding.etNotes.setText(itemDiagnosis[selectedItem].title)

                fragment.binding.headerRow.visible()
                fragment.binding.tvCodeV.text = itemDiagnosis[selectedItem].code
                fragment.binding.tvTitleV.text = itemDiagnosis[selectedItem].title

                dialog?.dismiss()
            }
            recyclerView.adapter = diagnosisAdapter

        ivSearch.setOnClickListener {
         /*   if (!isDiagnosis) {
                isSelectMedicine = true
                fragment.hitApi(true, etSearch.text.toString(), medicineAdapter, isSelectMedicine)
            } else {
                isSelectDiagnosis = true
                fragment.hitApiDiagnosis(
                    true,
                    etSearch.text.toString(),
                    diagnosisAdapter,
                    isSelectDiagnosis
                )
            }*/

            isSelectDiagnosis = true
            fragment.hitApiDiagnosis(
                true,
                etSearch.text.toString(),
                diagnosisAdapter,
                isSelectDiagnosis
            )
        }

        ivCross.setOnClickListener {
            dialog?.dismiss()
        }

        tvDone.setOnClickListener {
            val searchText = etSearch.text.toString().trim()
            if (searchText.isNotEmpty()) {
                onNoteSelected(searchText)
                dialog?.dismiss()
            }
        }


        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}