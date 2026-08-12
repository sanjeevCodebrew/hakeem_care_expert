package com.consultantvendor.ui.dashboard.settings.prewritten

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.PreWrittenPrescription
import com.consultantvendor.databinding.ItemPreWrittenPrescriptionBinding

class PreWrittenPrescriptionAdapter(
    private val fragment: PreWrittenPrescriptionFragment,
    private val items: ArrayList<PreWrittenPrescription>
) : RecyclerView.Adapter<PreWrittenPrescriptionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPreWrittenPrescriptionBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_pre_written_prescription,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val binding: ItemPreWrittenPrescriptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PreWrittenPrescription) {
            binding.tvName.text = item.diagnosis?.firstOrNull()?.title
                ?: item.title?.takeIf { it.isNotEmpty() }
                ?: item.report_detals?.takeIf { it.isNotEmpty() }
                ?: ""

            val prescType = item.prescription_type?.replaceFirstChar { it.uppercase() } ?: ""
            val fillType = when (item.fill_type?.lowercase()) {
                "form", "self" -> "Form"
                "upload-prescription", "upload prescription" -> "Upload Prescription"
                else -> item.fill_type?.replaceFirstChar { it.uppercase() } ?: ""
            }
            binding.tvPrescriptionType.text = prescType ?: ""
            binding.tvFillType.text = fillType ?: ""

            val diagCount = item.diagnosis?.size ?: 0
            val medCount = item.prescription?.size ?: 0
            val countParts = mutableListOf<String>()
            if (diagCount > 0) countParts.add("Diagnosis: $diagCount")
            if (medCount > 0) countParts.add("Medicines: $medCount")
            binding.tvCounts.text = countParts.joinToString("  •  ")

            val notes = item.report_detals?.trim()?.takeIf { it.isNotEmpty() }
                ?: item.description?.trim()?.takeIf { it.isNotEmpty() }
            if (notes != null) {
                binding.tvNotes.isVisible = true
                binding.tvNotes.text = notes
            } else {
                binding.tvNotes.isVisible = false
            }

            binding.tvEdit.setOnClickListener {
                fragment.onEditClicked(item)
            }
            binding.ivDelete.setOnClickListener {
                fragment.onDeleteClicked(item, adapterPosition)
            }
        }
    }
}
