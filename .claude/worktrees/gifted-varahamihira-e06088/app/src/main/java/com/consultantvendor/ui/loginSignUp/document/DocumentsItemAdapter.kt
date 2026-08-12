package com.consultantvendor.ui.loginSignUp.document

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.AdditionalFieldDocument
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemDocumentItemBinding
import com.consultantvendor.utils.ImageFolder
import com.consultantvendor.utils.getImageBaseUrl
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.requestOptions


class DocumentsItemAdapter(private val fragment: DocumentsFragment, private val positionMain: Int,
                           private val items: ArrayList<AdditionalFieldDocument>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_document_item, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemDocumentItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.ivEdit.setOnClickListener {
                fragment.addDocument(positionMain, adapterPosition)
            }

            binding.ivDelete.setOnClickListener {
                items.removeAt(adapterPosition)
                fragment.deleteDocument()
                notifyDataSetChanged()
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: AdditionalFieldDocument) = with(binding) {

            if (item.status == "declined") {
                tvName.setTextColor(Color.parseColor("#FF0000"))
                tvName.text = item.title?.plus("(Rejected)")
            } else {
                tvName.text = item.title
            }

            tvDesc.text = item.description
            if(item.file_name?.contains(".pdf") == true) {
                val glide = Glide.with(ivImage.context)
                glide.load(R.drawable.ic_pdf)
                    .apply(requestOptions)
                    .placeholder(R.drawable.image_placeholder)
                    .into(ivImage)
            }else {
                loadImage(ivImage, item.file_name, R.drawable.image_placeholder)
            }

        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}

