package com.consultantvendor.ui.dashboard.settings.prewritten

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.PreWrittenPrescription
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.ItemPreWrittenSheetBinding
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class PreWrittenPrescriptionBottomSheet(
    private val onSelected: (PreWrittenPrescription) -> Unit
) : BottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var viewModel: AddPrescriptionViewModel
    private lateinit var progressDialog: ProgressDialog
    private val items = ArrayList<PreWrittenPrescription>()
    private lateinit var adapter: SheetAdapter
    private var pendingDeletePosition = -1

    override fun onAttach(context: android.content.Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_pre_written_prescription, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        view.findViewById<View>(R.id.ivClose).setOnClickListener { dismiss() }

        val rvPrescriptions = view.findViewById<RecyclerView>(R.id.rvPrescriptions)
        val tvEmpty = view.findViewById<View>(R.id.tvEmpty)

        // Transparent container so rounded corners on root layout show through
        dialog?.setOnShowListener {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.background = ColorDrawable(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
            }
        }

        rvPrescriptions.layoutManager = LinearLayoutManager(requireContext())
        adapter = SheetAdapter()
        rvPrescriptions.adapter = adapter

        viewModel.deletePreWrittenPrescription.observe(viewLifecycleOwner, Observer { resource ->
            resource ?: return@Observer
            when (resource.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    if (pendingDeletePosition in items.indices) {
                        items.removeAt(pendingDeletePosition)
                        adapter.notifyItemRemoved(pendingDeletePosition)
                        adapter.notifyItemRangeChanged(pendingDeletePosition, items.size)
                        tvEmpty.isVisible = items.isEmpty()
                    }
                    pendingDeletePosition = -1
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    pendingDeletePosition = -1
                    ApisRespHandler.handleError(resource.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModel.getPreWrittenPrescriptions.observe(viewLifecycleOwner, Observer { resource ->
            resource ?: return@Observer
            when (resource.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    items.clear()
                    items.addAll(resource.data?.data ?: emptyList())
                    adapter.notifyDataSetChanged()
                    tvEmpty.isVisible = items.isEmpty()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(resource.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModel.getPreWrittenPrescriptions()
    }

    // ── inline adapter ────────────────────────────────────────────────────────
    private inner class SheetAdapter : RecyclerView.Adapter<SheetAdapter.VH>() {

        inner class VH(val binding: ItemPreWrittenSheetBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_pre_written_sheet,
                parent,
                false
            )
        )

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val b = holder.binding

            b.tvName.text = item.diagnosis?.firstOrNull()?.title
                ?: item.title?.takeIf { it.isNotEmpty() }
                ?: item.report_detals?.takeIf { it.isNotEmpty() }
                ?: ""

            b.tvPrescriptionType.text =
                item.prescription_type?.replaceFirstChar { it.uppercase() } ?: ""

            b.tvFillType.text = when (item.fill_type?.lowercase()) {
                "form", "self" -> "Form"
                "upload-prescription", "upload prescription" -> "Upload Prescription"
                else -> item.fill_type?.replaceFirstChar { it.uppercase() } ?: ""
            }

            val diagCount = item.diagnosis?.size ?: 0
            val medCount = item.prescription?.size ?: 0
            val countParts = mutableListOf<String>()
            if (diagCount > 0) countParts.add("Diagnosis: $diagCount")
            if (medCount > 0) countParts.add("Medicines: $medCount")
            b.tvCounts.text = countParts.joinToString("  •  ")

            val notes = item.report_detals?.trim()?.takeIf { it.isNotEmpty() }
                ?: item.description?.trim()?.takeIf { it.isNotEmpty() }
            b.tvNotes.isVisible = notes != null
            b.tvNotes.text = notes ?: ""

            b.ivDelete.setOnClickListener {
                val id = item.id ?: return@setOnClickListener
                pendingDeletePosition = holder.adapterPosition
                viewModel.deletePreWrittenPrescription(id)
            }

            holder.itemView.setOnClickListener {
                onSelected(item)
                dismiss()
            }
        }
    }
}
