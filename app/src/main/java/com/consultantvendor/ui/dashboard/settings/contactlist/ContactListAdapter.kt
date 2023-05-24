package com.consultantvendor.ui.dashboard.settings.contactlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.ContactEmergency
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemContactBinding


class ContactListAdapter(private val fragment: ContactListFragment, private val items: ArrayList<ContactEmergency>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_contact, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemContactBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.ivDelete.setOnClickListener {
                fragment.deleteContact(items[bindingAdapterPosition])
            }
        }

        fun bind(item: ContactEmergency) = with(binding) {
            tvName.text = item.name

            var phoneNumbers = ""
            item.phone_numbers?.forEach {
                phoneNumbers += "${it.type_label} ${it.phone}\n"
            }

            tvPhoneNumberV.text = phoneNumbers.removeSuffix("\n")
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
