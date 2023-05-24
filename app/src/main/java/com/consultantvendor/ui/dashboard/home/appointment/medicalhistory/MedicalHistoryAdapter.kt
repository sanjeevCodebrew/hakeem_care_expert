package com.consultantvendor.ui.dashboard.home.appointment.medicalhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.MedicalHistory
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemMedicalHistoryBinding
import com.consultantvendor.utils.getDoctorName
import com.consultantvendor.utils.loadImage


class MedicalHistoryAdapter(private val fragmentMain: MedicalHistoryFragment, private val items: ArrayList<UserData>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_medical_history, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemMedicalHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserData) = with(binding) {
            tvName.text = getDoctorName(item)
            tvDesc.text = item.category ?: fragmentMain.getString(R.string.na)
            loadImage(binding.ivPic, item.profile_image,
                    R.drawable.image_placeholder)

            val medical_history = ArrayList<MedicalHistory>()
            medical_history.addAll(item.medical_history ?: emptyList())
            val adapter = MedicalHistoryItemAdapter(fragmentMain, medical_history)
            rvListingItems.adapter = adapter
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
