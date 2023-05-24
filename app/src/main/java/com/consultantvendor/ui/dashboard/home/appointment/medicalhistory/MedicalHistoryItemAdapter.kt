package com.consultantvendor.ui.dashboard.home.appointment.medicalhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.MedicalHistory
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemMedicalHistoryItemBinding
import com.consultantvendor.utils.*


class MedicalHistoryItemAdapter(private val fragmentMain: MedicalHistoryFragment, private val items: ArrayList<MedicalHistory>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_medical_history_item, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemMedicalHistoryItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvPrescription.setOnClickListener {
                val link = fragmentMain.getString(R.string.pdf_link, BuildConfig.BASE_URL, items[bindingAdapterPosition].request?.id, BuildConfig.APP_UNIQUE_ID)
                openPdf(fragmentMain.requireActivity(), link, true)
            }
        }

        fun bind(item: MedicalHistory) = with(binding) {
            tvDate.text = fragmentMain.getString(R.string.appointment_date_s, DateUtils.dateTimeFormatFromUTC(
                    DateFormat.MON_DATE_YEAR, item.request?.booking_date))

            tvComment.hideShowView(!item.comment.isNullOrEmpty())
            tvComment.text = item.comment ?: fragmentMain.getString(R.string.na)

            if (item.request?.extra_payment != null) {
                tvServices.visible()

                val extraPayment = item.request?.extra_payment

                val services = "${fragmentMain.getString(R.string.extra_payment)} (${extraPayment?.status})" +
                        "\n${fragmentMain.getString(R.string.amount_s, getCurrency(extraPayment?.balance))}" +
                        "\n${extraPayment?.description}"

                binding.tvServices.text = services
            } else
                tvServices.gone()

            tvPrescription.hideShowView(item.request?.is_prescription == true)
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
