package com.consultantvendor.utils.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.IcdDiagnosisItem
import com.consultantvendor.ui.adapter.DiagnosisAdapter
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelDiagnosis
import com.consultantvendor.ui.dashboard.home.reports.AddReportFragment
import com.consultantvendor.ui.dashboard.settings.prewritten.AddPreWrittenPrescriptionFragment

class DiagnosisDialogFragment(
    private val onNoteSelected: (String) -> Unit,
    private val fragment: androidx.fragment.app.Fragment,
    private val itemDiagnosis: ArrayList<IcdDiagnosisItem>,
) : DialogFragment() {

    var diagnosisAdapter: DiagnosisAdapter? = null
    private var isSelectDiagnosis = false

    @SuppressLint("NotifyDataSetChanged", "MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_diagnosis_list, container, false)
        val tvTitle: TextView = view.findViewById(R.id.dialogTitle)
        val tvGetFromList: TextView = view.findViewById(R.id.tvGetFromApi)
        val etSearch: EditText = view.findViewById(R.id.etSearch)
        val ivSearch: ImageView = view.findViewById(R.id.ivSearch)
        val ivCross: ImageView = view.findViewById(R.id.ivCancel)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewDialog)
        val tvDone: TextView = view.findViewById(R.id.tvDone)

        tvTitle.text = fragment.getString(R.string.select_diagnosis)
        diagnosisAdapter = DiagnosisAdapter(itemDiagnosis) { selectedItem ->
            if (selectedItem < 0 || selectedItem >= itemDiagnosis.size) {
                dismissAllowingStateLoss()
                return@DiagnosisAdapter
            }
            val diagnosis = ItemModelDiagnosis(
                code = itemDiagnosis[selectedItem].code_id,
                title = itemDiagnosis[selectedItem].ascii_desc
            )
            if (fragment is AddReportFragment) {
                fragment.itemDiagnosisList.add(diagnosis)
                fragment.adpterDiagnosisList?.notifyDataSetChanged()
            } else if (fragment is AddPreWrittenPrescriptionFragment) {
                fragment.itemDiagnosisList.add(diagnosis)
                fragment.adpterDiagnosisList?.notifyDataSetChanged()
            }
            dialog?.dismiss()
        }
        recyclerView.adapter = diagnosisAdapter

        ivSearch.setOnClickListener {
            isSelectDiagnosis = true
            if (fragment is AddReportFragment) {
                fragment.hitApiDiagnosis(true, etSearch.text.toString(), diagnosisAdapter, true)
            } else if (fragment is AddPreWrittenPrescriptionFragment) {
                fragment.hitApiDiagnosis(true, etSearch.text.toString(), diagnosisAdapter, true)
            }
        }

        tvGetFromList.setOnClickListener {
            if (fragment is AddReportFragment) {
                fragment.isSearchDiagnosis = false
                fragment.hitApiDiagnosis(true, "", null, false)
            } else if (fragment is AddPreWrittenPrescriptionFragment) {
                fragment.hitApiDiagnosis(true, "", null, false)
            }
        }

        ivCross.setOnClickListener {
            dialog?.dismiss()
        }

        tvDone.setOnClickListener {
            val searchText = etSearch.text.toString().trim()

            if (searchText.isNotEmpty()) {
                val parts = searchText.split(" ", limit = 2)
                val code = parts.getOrNull(0) ?: ""
                val title = parts.getOrNull(1) ?: ""
                val diagnosis = ItemModelDiagnosis(code = code, title = title)

                if (fragment is AddReportFragment) {
                    fragment.itemDiagnosisList.add(diagnosis)
                    fragment.adpterDiagnosisList?.notifyDataSetChanged()
                } else if (fragment is AddPreWrittenPrescriptionFragment) {
                    fragment.itemDiagnosisList.add(diagnosis)
                    fragment.adpterDiagnosisList?.notifyDataSetChanged()
                }

                onNoteSelected(searchText)
                dialog?.dismiss()
            }
        }

        return view
    }

    fun refreshAdapter() {
        diagnosisAdapter?.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
