package com.consultantvendor.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.data.models.DrugItem
import com.consultantvendor.databinding.ItemLayoutDiagnosisBinding
import com.consultantvendor.databinding.ItemLayoutMedicineBinding

class MedicineAdapter(
    private val items: ArrayList<DrugItem>,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLayoutMedicineBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnSelect.setOnClickListener {
                onSelect(absoluteAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLayoutMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvCode.text = item.ingredients
        holder.binding.tvTitle.text = item.strength
    }
}