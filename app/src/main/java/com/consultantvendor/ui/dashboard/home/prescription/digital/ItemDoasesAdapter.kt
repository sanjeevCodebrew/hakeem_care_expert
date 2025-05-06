package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.Doases
import com.consultantvendor.databinding.RvItemDoasesBinding
import com.consultantvendor.utils.hideShowView


class ItemDoasesAdapter(private val fragment: DigitalPrescriptionFragment, private val items: ArrayList<Doases>) :
        RecyclerView.Adapter<ItemDoasesAdapter.ViewHolder>() {


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.rv_item_doases, parent, false))
    }

    override fun getItemCount(): Int = items.size


    inner class ViewHolder(val binding: RvItemDoasesBinding) :
            RecyclerView.ViewHolder(binding.root) {

        val context = binding.root.context
        val doases = context.resources.getStringArray(R.array.doases)

        init {
            binding.cbBreakfast.setOnCheckedChangeListener { compoundButton, b ->
                binding.group1.hideShowView(b)
                items[adapterPosition].checked = b
            }



//            binding.spnDosage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
//                    if (position == 0)
//                        items[adapterPosition].dose_value = ""
//                    else
//                        items[adapterPosition].dose_value = binding.spnDosage.selectedItem.toString()
//                }
//
//                override fun onNothingSelected(parentView: AdapterView<*>?) {
//                    // your code here
//                }
//            }
//
//
//            binding.spnroute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
//                    if (position == 0)
//                        items[adapterPosition].routes = ""
//                    else
//                        items[adapterPosition].routes = binding.spnroute.selectedItem.toString()
//                }
//
//                override fun onNothingSelected(parentView: AdapterView<*>?) {
//                    // your code here
//                }
//            }

            binding.tvBreakfastSave.setOnClickListener {
                binding.group1.hideShowView(false)
            }

            binding.tvBefore.setOnClickListener {
                makeActiveInactive(binding.tvBefore.text.toString())
            }

            binding.tvAfter.setOnClickListener {
                makeActiveInactive(binding.tvAfter.text.toString())
            }

            binding.tvWith.setOnClickListener {
                makeActiveInactive(binding.tvWith.text.toString())
            }

        }

        private fun makeActiveInactive(text: String) {
            items[adapterPosition].with = text
            when (text) {
                context.getString(R.string.before), "" -> {
                    binding.tvBefore.setBackgroundResource(R.drawable.drawable_doase_left_s)
                    binding.tvBefore.setTextColor(ContextCompat.getColor(context, R.color.colorWhite))
                    binding.tvAfter.setBackgroundResource(R.drawable.drawable_doase_midle)
                    binding.tvAfter.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                    binding.tvWith.setBackgroundResource(R.drawable.drawable_doase_right)
                    binding.tvWith.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                }
                context.getString(R.string.after) -> {
                    binding.tvBefore.setBackgroundResource(R.drawable.drawable_doase_left)
                    binding.tvBefore.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                    binding.tvAfter.setBackgroundResource(R.drawable.drawable_doase_midle_s)
                    binding.tvAfter.setTextColor(ContextCompat.getColor(context, R.color.colorWhite))
                    binding.tvWith.setBackgroundResource(R.drawable.drawable_doase_right)
                    binding.tvWith.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                }
                context.getString(R.string.with) -> {
                    binding.tvBefore.setBackgroundResource(R.drawable.drawable_doase_left)
                    binding.tvBefore.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                    binding.tvAfter.setBackgroundResource(R.drawable.drawable_doase_midle)
                    binding.tvAfter.setTextColor(ContextCompat.getColor(context, R.color.colorBlack))
                    binding.tvWith.setBackgroundResource(R.drawable.drawable_doase_right_s)
                    binding.tvWith.setTextColor(ContextCompat.getColor(context, R.color.colorWhite))
                }
            }
        }

        fun bind(item: Doases) = with(binding) {
//            binding.tvDosage.text = fragment.binding.spnDosagesType.selectedItem.toString()



            cbBreakfast.text = item.time

            cbBreakfast.isChecked = item.checked ?: false
            group1.hideShowView(item.checked == true)


            if (item.checked == true) {
                if (item.duration.isNullOrEmpty()) {
//                    spnDosage.setSelection(0)
                    makeActiveInactive(context.getString(R.string.before))
                } else {
                    makeActiveInactive(item.with ?: "")
                    binding.etDuration.setText(doases.indexOf(item.duration))
                    binding.etQuantity.setText(doases.indexOf(item.quantity))
                }
            } else {
                makeActiveInactive(context.getString(R.string.before))
//                spnDosage.setSelection(0)
            }
        }
    }
}
