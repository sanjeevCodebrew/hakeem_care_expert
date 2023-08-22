package com.consultantvendor.ui.dashboard.home.items

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.databinding.RvItemHealthToolsBinding
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.PAGE_TO_OPEN


class HealthToolsAdapter(private val fragmentMain: Fragment, private val items: ArrayList<String>) :
        RecyclerView.Adapter<HealthToolsAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.rv_item_health_tools, parent, false))
    }

    override fun getItemCount(): Int = items.size


    inner class ViewHolder(val binding: RvItemHealthToolsBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.clHealthTools.setOnClickListener {
                when (adapterPosition) {
                    0 -> openPage(DrawerActivity.BMI_CHECKER)
                    1 -> openPage(DrawerActivity.WATER_INTAKE)
                    2 -> openPage(DrawerActivity.PROTEIN_INTAKE)
                    3 -> openPage(DrawerActivity.PREGNANCY_CALCULATOR)
                }
            }
        }

        private fun openPage(page: String) {
            fragmentMain.startActivity(Intent(fragmentMain.requireContext(), DrawerActivity::class.java)
                    .putExtra(PAGE_TO_OPEN, page))
        }

        fun bind(item: String) = with(binding) {
            val context = binding.root.context
            tvName.text = item

            when (adapterPosition) {
                0 -> {
                    tvDesc.text = context.getString(R.string.health_tool_1_desc)
                    ivImage.setImageResource(R.drawable.ic_health_tool_1)
                }
                1 -> {
                    tvDesc.text = context.getString(R.string.health_tool_2_desc)
                    ivImage.setImageResource(R.drawable.ic_health_tool_2)
                }
                2 -> {
                    tvDesc.text = context.getString(R.string.health_tool_3_desc)
                    ivImage.setImageResource(R.drawable.ic_health_tool_3)
                }
                3 -> {
                    tvDesc.text = context.getString(R.string.health_tool_4_desc)
                    ivImage.setImageResource(R.drawable.ic_health_tool_4)
                }
            }
        }
    }
}
