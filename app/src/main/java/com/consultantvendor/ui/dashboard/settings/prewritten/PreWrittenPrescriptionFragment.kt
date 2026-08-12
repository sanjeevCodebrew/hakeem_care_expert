package com.consultantvendor.ui.dashboard.settings.prewritten

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.PreWrittenPrescription
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentPreWrittenPrescriptionBinding
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.AppRequestCode
import com.consultantvendor.utils.PAGE_TO_OPEN
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class PreWrittenPrescriptionFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentPreWrittenPrescriptionBinding
    private var rootView: View? = null
    private lateinit var viewModel: AddPrescriptionViewModel

    private lateinit var progressDialog: ProgressDialog

    private val items = ArrayList<PreWrittenPrescription>()
    private lateinit var adapter: PreWrittenPrescriptionAdapter
    private var pendingDeletePosition = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_pre_written_prescription, container, false
            )
            rootView = binding.root
            initialise()
            listeners()
            bindObservers()
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        loadPrescriptions()
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        adapter = PreWrittenPrescriptionAdapter(this, items)
        binding.rvPrescriptions.adapter = adapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvAddPrescription.setOnClickListener {
            openAddEditScreen(null)
        }
    }

    private fun loadPrescriptions() {
        viewModel.getPreWrittenPrescriptions()
    }

    fun onEditClicked(prescription: PreWrittenPrescription) {
        openAddEditScreen(prescription)
    }

    fun onDeleteClicked(prescription: PreWrittenPrescription, position: Int) {
        val id = prescription.id ?: return
        if (isConnectedToInternet(requireContext(), true)) {
            pendingDeletePosition = position
            viewModel.deletePreWrittenPrescription(id)
        }
    }

    private fun openAddEditScreen(prescription: PreWrittenPrescription?) {
        val intent = Intent(requireContext(), DrawerActivity::class.java)
            .putExtra(PAGE_TO_OPEN, DrawerActivity.ADD_PRE_WRITTEN_PRESCRIPTION)
        if (prescription != null) {
            intent.putExtra(AddPreWrittenPrescriptionFragment.EXTRA_PRESCRIPTION, prescription)
        }
        startActivityForResult(intent, AppRequestCode.PROFILE_UPDATE)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindObservers() {
        viewModel.deletePreWrittenPrescription.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                   progressDialog.setLoading(false)
                    if (pendingDeletePosition in items.indices) {
                        items.removeAt(pendingDeletePosition)
                        adapter.notifyItemRemoved(pendingDeletePosition)
                        adapter.notifyItemRangeChanged(pendingDeletePosition, items.size)
                        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                    pendingDeletePosition = -1
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    pendingDeletePosition = -1
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> progressDialog.setLoading(true)
            }
        })

        viewModel.getPreWrittenPrescriptions.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    items.clear()
                    items.addAll(it.data?.data ?: emptyList())
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                   progressDialog.setLoading(true)
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            loadPrescriptions()
        }
    }
}
