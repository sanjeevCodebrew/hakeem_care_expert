package com.consultantvendor.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.data.models.responses.IcdDiagnosisItem
import com.consultantvendor.databinding.ItemLayoutDiagnosisBinding
import com.consultantvendor.ui.dashboard.home.prescription.digital.DigitalPrescriptionFragment

class DiagnosisAdapter(
    private val items: ArrayList<IcdDiagnosisItem>,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<DiagnosisAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLayoutDiagnosisBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnSelect.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSelect(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLayoutDiagnosisBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvCode.text = item.code_id
        holder.binding.tvTitle.text = item.ascii_desc
    }
}