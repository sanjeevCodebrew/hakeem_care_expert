package com.consultantvendor.ui.dashboard.home.healthtool.waterintake

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DatesAvailability
import com.consultantvendor.databinding.ItemWaterIntakeBinding
import com.consultantvendor.ui.dashboard.home.healthtool.protienintake.ProteinIntakeFragment
import com.consultantvendor.utils.getProteinUnit
import com.consultantvendor.utils.getWaterLiters
import java.util.*

class WaterIntakeAdapter(private val fragment: Fragment, private val items: ArrayList<DatesAvailability>) :
        RecyclerView.Adapter<WaterIntakeAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.item_water_intake, parent, false))
    }

    inner class ViewHolder(val binding: ItemWaterIntakeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DatesAvailability) = with(binding) {

            if (fragment is WaterIntakeFragment)
                cbName.text = getWaterLiters(fragment.requireActivity(), item.intakeAmount
                        ?: 0, true)
            else
                cbName.text = getProteinUnit(fragment.requireActivity(), item.intakeAmount
                        ?: 0, true)

            cbName.isChecked = item.isSelected

            if (item.isSelected) {
                cbName.setBackgroundResource(R.drawable.drawable_theme_60)
                cbName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.colorWhite))
            } else {
                cbName.setBackgroundResource(R.drawable.drawable_stroke_inactive)
                cbName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.colorBlack))
            }


            clWater.setOnClickListener {
                if (!item.isSelected) {
                    for (count: Int in 0 until items.size) {
                        items[count].isSelected = count == adapterPosition
                        notifyItemChanged(count)
                    }

                    if (fragment is WaterIntakeFragment)
                        fragment.onWaterSelected(item)
                    else if (fragment is ProteinIntakeFragment)
                        fragment.onWaterSelected(item)
                }
            }
        }
    }
}
