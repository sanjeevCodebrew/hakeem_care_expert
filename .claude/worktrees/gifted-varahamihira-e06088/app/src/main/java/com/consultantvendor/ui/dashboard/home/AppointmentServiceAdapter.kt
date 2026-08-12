package com.consultantvendor.ui.dashboard.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Service
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemAppointmentServiceBinding
import com.consultantvendor.databinding.ItemPagingLoaderBinding


class AppointmentServiceAdapter(private val fragment: Fragment, private val items: ArrayList<Service>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_appointment_service, parent, false
                    )
            )
        } else {
            ViewHolderLoader(
                    DataBindingUtil.inflate(
                            LayoutInflater.from(parent.context),
                            R.layout.item_paging_loader, parent, false
                    )
            )
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: ItemAppointmentServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.clService.setOnClickListener {

            }
        }

        fun bind(item: Service) = with(binding) {

            cbName.text = item.service_name ?: ""

            cbName.isChecked = item.isSelected
            if (item.isSelected) {
                clService.setBackgroundResource(R.drawable.drawable_theme_60)
            } else {
                clService.setBackgroundResource(R.drawable.drawable_service_inactive)
            }

            clService.setOnClickListener {
                if (!item.isSelected) {
                    items.forEachIndexed { index, service ->
                        items[index].isSelected = index == adapterPosition
                    }
                    notifyDataSetChanged()

                    if (fragment is AppointmentFragment)
                        fragment.onServiceSelected(item)
                    else if (fragment is HomeFragment)
                        fragment.onServiceSelected(item)
                }
            }

        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
