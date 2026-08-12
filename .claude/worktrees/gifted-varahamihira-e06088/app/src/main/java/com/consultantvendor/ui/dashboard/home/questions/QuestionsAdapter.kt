package com.consultantvendor.ui.dashboard.home.questions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Feed
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemQuestionBinding
import com.consultantvendor.ui.dashboard.home.questions.detail.QuestionDetailFragment
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.getDoctorName
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.replaceFragment


class QuestionsAdapter(private val fragmentMain: QuestionsFragment, private val items: ArrayList<Feed>) :
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
                    R.layout.rv_item_question, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemQuestionBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val fragment = QuestionDetailFragment()
                val bundle = Bundle()
                bundle.putString(EXTRA_REQUEST_ID, items[adapterPosition].id)
                fragment.arguments = bundle
                replaceFragment(fragmentMain.requireActivity().supportFragmentManager,
                        fragment, R.id.container)
            }
        }

        fun bind(item: Feed) = with(binding) {
            binding.tvName.text = getDoctorName(item.created_by)
            loadImage(binding.ivPic, item.created_by?.profile_image,
                    R.drawable.image_placeholder)

            tvTitle.text = item.title
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
