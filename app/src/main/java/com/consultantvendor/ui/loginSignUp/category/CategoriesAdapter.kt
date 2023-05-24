package com.consultantvendor.ui.loginSignUp.category

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Categories
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemCategoryBinding
import com.consultantvendor.databinding.RvItemCategoryReverseBinding
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.visible


class CategoriesAdapter(private val fragment: CategoryFragment, private val items: ArrayList<Categories>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING) {
            when (BuildConfig.FLAVOR) {
                "heal" ->
                    (holder as ViewHolderReverse).bind(items[position])
                else ->
                    (holder as ViewHolder).bind(items[position])
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            when (BuildConfig.FLAVOR) {
                "heal" ->
                    ViewHolderReverse(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                            R.layout.rv_item_category_reverse, parent, false))
                else ->
                    ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                            R.layout.rv_item_category, parent, false))
            }
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            if (fragment.userRepository.getUserLanguage() == "ar")
                binding.ivCategory.rotationY = 180f

            if (BuildConfig.FLAVOR == "taradoc")
                binding.tvName.setTextColor(Color.parseColor("#1b1616"))

            if (BuildConfig.FLAVOR == "nurseLynx")
                binding.ivInfo.visible()
            else
                binding.ivInfo.gone()

            binding.clMain.setOnClickListener {
                fragment.clickItem(items[bindingAdapterPosition])
            }

            binding.ivInfo.setOnClickListener {
                fragment.showInformationDialog(items[bindingAdapterPosition])
            }

        }

        fun bind(item: Categories) = with(binding) {
            if (item.color_code.isNullOrEmpty())
                clCategory.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
            else
                clCategory.setBackgroundColor(Color.parseColor(item.color_code))

            tvName.text = item.name
            loadImage(binding.ivCategory, item.image, 0)
        }
    }

    inner class ViewHolderReverse(val binding: RvItemCategoryReverseBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            if (fragment.userRepository.getUserLanguage() == "ar")
                binding.ivCategory.rotationY = 180f

            if (BuildConfig.FLAVOR == "nurseLynx")
                binding.ivInfo.visible()
            else
                binding.ivInfo.gone()

            binding.clMain.setOnClickListener {
                fragment.clickItem(items[bindingAdapterPosition])
            }

            binding.ivInfo.setOnClickListener {
                fragment.showInformationDialog(items[bindingAdapterPosition])
            }

        }

        fun bind(item: Categories) = with(binding) {
            if (item.color_code.isNullOrEmpty())
                clCategory.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
            else
                clCategory.setBackgroundColor(Color.parseColor(item.color_code))

            tvName.text = item.name

            loadImage(binding.ivCategory, item.image, 0)
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
