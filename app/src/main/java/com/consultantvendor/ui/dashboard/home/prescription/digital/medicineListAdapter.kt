package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.databinding.ItemMedicineListBinding
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelMedicine

class medicineListAdapter(
    private var fragment: DigitalPrescriptionFragment,
    private val items: ArrayList<ItemModelMedicine>
) : RecyclerView.Adapter<medicineListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMedicineListBinding) : RecyclerView.ViewHolder(binding.root) {

        init {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicineListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvMedicineName.text = item.medicine_name
        holder.binding.tvDoses.text = item.doses
        holder.binding.tvfrequency.text = item.frequency
        holder.binding.tvDuration.text = item.duration
        holder.binding.tvQuantity.text = item.quantity


        holder.binding.btnDelete.setOnClickListener {
            removeItem(position)
        }

        holder.binding.btnEdit.setOnClickListener {
            fragment.isEditMedicine = true
            fragment.binding.btnAddMedicine.performClick()
        }

    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
}