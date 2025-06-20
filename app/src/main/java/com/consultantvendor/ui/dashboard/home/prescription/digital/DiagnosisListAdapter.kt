package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.databinding.ItemDiagnosisListBinding
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelDiagnosis

class DiagnosisListAdapter(
    private val items: ArrayList<ItemModelDiagnosis>,
) : RecyclerView.Adapter<DiagnosisListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDiagnosisListBinding) : RecyclerView.ViewHolder(binding.root) {

        init {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiagnosisListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvCodeV.text = item.code
        holder.binding.tvTitleV.text = item.title

        holder.binding.btnDelete.setOnClickListener {
            removeItem(position)
        }
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}