package com.consultantvendor.ui.loginSignUp.service

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Service
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemServiceBinding
import com.consultantvendor.utils.*


class ServiceAdapter(private val fragment: ServiceFragment, private val items: ArrayList<Service>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_service, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvSymbol.text = getCurrencySymbol()
            if (BuildConfig.FLAVOR == "taradoc") {
                binding.tvAddAvailability.setTextColor(Color.parseColor("#E7D27C"))
            }
            binding.tvAddAvailability.setOnClickListener {
                fragment.clickAddAvailability(bindingAdapterPosition)
            }

            binding.etAddress.setOnClickListener {
                fragment.clinicAddress(bindingAdapterPosition)
            }
        }

        fun bind(item: Service) = with(binding) {
            switchActive.setOnCheckedChangeListener(null)

            val context = binding.root.context

            tvName.text = item.name
            switchActive.isChecked = item.isSelected

            if (item.isSelected) {
                groupConsultation.visible()
                tvAddAvailability.visible()

                if (item.price_type == PriceType.PRICE_RANGE) {
                    etConsultationFeeV.isFocusable = true
                    tvPrice.text = context.getString(R.string.price_btw,
                            getCurrency(item.price_minimum.toString()), getCurrency(item.price_maximum.toString()))
                    etConsultationFeeV.setText((item.price ?: "").toString())
                } else {
                    tvPrice.text = context.getString(R.string.fixed_price)
                    etConsultationFeeV.isFocusable = false
                    etConsultationFeeV.setText((item.price_fixed ?: 0).toString())
                }

                tvAddAvailability.hideShowView(item.need_availability == "1")

                tvAddAvailability.text = if (item.isAvailabilityLocal)
                    context.getString(R.string.edit_availability)
                else context.getString(R.string.add_availability)


                tvConsultationUnitV.setText("${getCurrencySymbol()} / ${getUnitPrice(item.unit_price, binding.root.context)}")

                ilAddress.hideShowView(item.main_service_type == ConsultType.CLINIC_VISIT || item.main_service_type == ConsultType.HOME_VISIT)
                ilAddress.hint = context.getString(R.string.clinic_address, item.name)
                etAddress.setText(item.clinic_address?.locationName ?: "")
            } else {
                groupConsultation.gone()
                tvAddAvailability.gone()
                binding.ilAddress.gone()
            }

            switchActive.setOnCheckedChangeListener { buttonView, isChecked ->
                items[bindingAdapterPosition].isSelected = isChecked
                notifyDataSetChanged()
            }

            etConsultationFeeV.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence, start: Int,
                                           before: Int, count: Int) {
                    items[bindingAdapterPosition].price = etConsultationFeeV.text.toString()
                }
            })

        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}

