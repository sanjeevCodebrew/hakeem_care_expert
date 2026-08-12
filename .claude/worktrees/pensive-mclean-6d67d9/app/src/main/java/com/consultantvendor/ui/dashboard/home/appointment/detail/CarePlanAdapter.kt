package com.consultantvendor.ui.dashboard.home.appointment.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Filter
import com.consultantvendor.databinding.RvItemCarePlanBinding
import com.consultantvendor.databinding.RvItemCarePlanOptionBinding
import com.consultantvendor.utils.CallAction
import com.consultantvendor.utils.hideShowView


class CarePlanAdapter(private val fragment: Fragment,private val planOption: PlanOption, private val items: ArrayList<Filter>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (planOption) {
            PlanOption.FIRST_OPTION ->
                (holder as ViewHolderOption).bind(items[position])
            else ->
                (holder as ViewHolder).bind(items[position])
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (planOption) {
            PlanOption.FIRST_OPTION ->
                ViewHolderOption(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                        R.layout.rv_item_care_plan_option, parent, false))
               else ->
                ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                        R.layout.rv_item_care_plan, parent, false))
        }

    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: RvItemCarePlanBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {

        }

        fun bind(item: Filter) = with(binding) {
            tvPlan.text = item.title

            val listOptions = ArrayList<Filter>()
            listOptions.addAll(items[adapterPosition].tier_options ?: emptyList())
            val adapter = CarePlanAdapter(fragment, PlanOption.FIRST_OPTION, listOptions)
            binding.rvListing.adapter = adapter
        }
    }

    inner class ViewHolderOption(val binding: RvItemCarePlanOptionBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvDone.setOnClickListener {
                if (fragment is AppointmentDetailsFragment)
                    fragment.updateCarePlan(items[adapterPosition])
            }
        }

        fun bind(item: Filter) = with(binding) {
            val context = binding.root.context

            val help = if (item.type == "1") context.getString(R.string.need_some_help)
            else context.getString(R.string.need_much_help)

            tvPlan.text = "${item.title} ($help)"

            cbDone.hideShowView(item.status != CallAction.PENDING)
            tvDone.hideShowView(item.status == CallAction.PENDING)

        }
    }

    enum class PlanOption {
        MAIN, FIRST_OPTION
    }
}
