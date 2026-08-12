package com.consultantvendor.ui.dashboard.home.appointment.patientfile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.IcdDiagnosisItem

class DiagnosisSuggestionAdapter(
    private val onItemClick: (IcdDiagnosisItem) -> Unit
) : RecyclerView.Adapter<DiagnosisSuggestionAdapter.ViewHolder>() {

    private var items = listOf<IcdDiagnosisItem>()

    fun submitList(list: List<IcdDiagnosisItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnosis_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)

        fun bind(item: IcdDiagnosisItem) {
            tvCode.text = item.code_id ?: ""
            tvTitle.text = item.ascii_desc ?: item.ascii_short_desc ?: ""
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
