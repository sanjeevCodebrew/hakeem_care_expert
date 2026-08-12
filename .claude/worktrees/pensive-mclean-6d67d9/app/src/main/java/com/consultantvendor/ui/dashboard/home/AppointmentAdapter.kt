package com.consultantvendor.ui.dashboard.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.LoadingStatus.ITEM
import com.consultantvendor.data.network.LoadingStatus.LOADING
import com.consultantvendor.databinding.ItemPagingLoaderBinding
import com.consultantvendor.databinding.RvItemAppointmentBinding
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.*


class AppointmentAdapter(private val fragment: Fragment, private val items: ArrayList<Request>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var allItemsLoaded = true

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType != LOADING)
            (holder as ViewHolder).bind(items[position])
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM) {
            ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.rv_item_appointment, parent, false))
        } else {
            ViewHolderLoader(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                    R.layout.item_paging_loader, parent, false))
        }
    }

    override fun getItemCount(): Int = if (allItemsLoaded) items.size else items.size + 1

    override fun getItemViewType(position: Int) = if (position >= items.size) LOADING else ITEM

    inner class ViewHolder(val binding: RvItemAppointmentBinding) :
            RecyclerView.ViewHolder(binding.root) {

        init {
            binding.tvAccept.setOnClickListener {
                checkRequest()
            }

            binding.tvCancel.setOnClickListener {
                if (fragment is AppointmentFragment)
                    fragment.cancelAppointment(items[bindingAdapterPosition])
                else if (fragment is HomeFragment)
                    fragment.cancelAppointment(items[bindingAdapterPosition])
            }

            binding.root.setOnClickListener {
                if (fragment is AppointmentFragment)
                    fragment.registerActivityResult.launch(Intent(fragment.requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT_DETAILS)
                            .putExtra(EXTRA_REQUEST_ID, items[bindingAdapterPosition].id))
                else if (fragment is HomeFragment)
                    fragment.startActivityForResult(Intent(fragment.requireContext(), DrawerActivity::class.java)
                            .putExtra(PAGE_TO_OPEN, DrawerActivity.APPOINTMENT_DETAILS)
                            .putExtra(EXTRA_REQUEST_ID, items[bindingAdapterPosition].id), AppRequestCode.APPOINTMENT_DETAILS)
                
            }
        }

        /*Common method*/
        private fun checkRequest() {
            if (fragment is AppointmentFragment)
                fragment.proceedRequest(items[bindingAdapterPosition])
            else if (fragment is HomeFragment)
                fragment.proceedRequest(items[bindingAdapterPosition])
        }

        fun bind(request: Request) = with(binding) {
            val context = binding.root.context

            tvAccept.visible()
            tvCancel.hideShowView(request.canCancel)

            tvName.text = request.from_user?.name
            loadImage(binding.ivPic, request.from_user?.profile_image,
                    R.drawable.ic_profile_placeholder)

            tvDateTime.text = "${DateUtils.dateTimeFormatFromUTC(
                    DateFormat.MON_DATE_YEAR, request.bookingDateUTC)} · " +
                    "${DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request.bookingDateUTC)}"

            tvRequestType.text = request.service_type
            tvPrice.text = fragment.getString(R.string.sar) + request.price

            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))

            when (request.status) {
                CallAction.PENDING -> {
                    tvStatus.text = context.getString(R.string.new_request)
                    tvAccept.text = context.getString(R.string.accept_request)
                }
                CallAction.ACCEPT -> {
                    tvStatus.text = context.getString(R.string.accepted)
                    tvAccept.text = context.getString(R.string.start_request)
                    tvCancel.gone()
                }
                CallAction.INPROGRESS -> {
                    tvStatus.text = context.getString(R.string.inprogess)
                    tvAccept.text = context.getString(R.string.check_request)
                    tvCancel.gone()
                    tvAccept.gone()
                }
                CallAction.START -> {
                    tvStatus.text = context.getString(R.string.inprogess)
                    tvAccept.text = context.getString(R.string.track_status)
                    tvCancel.gone()
                }
                CallAction.REACHED -> {
                    tvStatus.text = context.getString(R.string.reached_destination)
                    tvAccept.text = context.getString(R.string.track_status)
                    tvCancel.gone()
                }
                CallAction.START_SERVICE -> {
                    tvStatus.text = context.getString(R.string.started)
                    tvAccept.text = context.getString(R.string.mark_complete)
                    tvCancel.gone()
                }
                CallAction.COMPLETED -> {
                    tvStatus.text = context.getString(R.string.completed)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.textColorGreen))
                    tvAccept.gone()
                    tvCancel.gone()
                }
                CallAction.FAILED -> {
                    tvAccept.gone()
                    tvStatus.text = context.getString(R.string.no_show)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorCancel))
                    tvCancel.gone()
                }
                CallAction.CANCELED -> {
                    tvStatus.text = context.getString(R.string.canceled)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorCancel))
                    tvAccept.gone()
                    tvCancel.gone()
                }
                CallAction.CANCEL_SERVICE -> {
                    tvStatus.text = context.getString(R.string.canceled_service)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorCancel))
                    tvCancel.gone()
                    tvAccept.gone()
                }
                else -> {
                    tvStatus.text = context.getString(R.string.new_request)
                }
            }

            /*Hide these button*/
            tvAccept.gone()
            tvCancel.gone()
        }
    }

    inner class ViewHolderLoader(val binding: ItemPagingLoaderBinding) :
            RecyclerView.ViewHolder(binding.root)

    fun setAllItemsLoaded(allLoaded: Boolean) {
        allItemsLoaded = allLoaded
    }
}
