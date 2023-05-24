package com.consultantvendor.ui.dashboard.home.questions.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Feed
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemAnswerBinding
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.getDoctorName
import com.consultantvendor.utils.loadImage


class AnswersAdapter(private val fragment: Fragment, private val items: List<Feed>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.rv_item_answer, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemAnswerBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
        }

        fun bind(item: Feed) = with(binding) {
            loadImage(binding.ivPic, item.user?.profile_image, R.drawable.ic_profile_placeholder)
            tvName.text = getDoctorName(item.user)

            tvComment.text = item.answer
            tvTime.text= DateUtils.getTimeAgo(item.created_at)
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
