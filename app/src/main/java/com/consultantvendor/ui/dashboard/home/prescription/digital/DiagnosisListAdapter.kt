package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.data.models.responses.Response
import com.consultantvendor.databinding.ItemDiagnosisListBinding
import com.consultantvendor.databinding.ItemLayoutDiagnosisBinding
import com.consultantvendor.ui.dashboard.home.prescription.model.itemModelDiagnosis

class DiagnosisListAdapter(
    private val items: ArrayList<itemModelDiagnosis>,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<DiagnosisListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDiagnosisListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnSelect.setOnClickListener {
                onSelect(absoluteAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiagnosisListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]


        holder.binding.tvCode.text = item.code
        holder.binding.tvTitle.text = item.title
    }
}