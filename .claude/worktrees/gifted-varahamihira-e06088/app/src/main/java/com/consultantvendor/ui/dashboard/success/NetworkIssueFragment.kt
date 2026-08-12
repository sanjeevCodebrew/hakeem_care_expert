package com.consultantvendor.ui.dashboard.success

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.databinding.ItemNoInternetBinding
import com.consultantvendor.utils.isConnectedToInternet
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class NetworkIssueFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ItemNoInternetBinding

    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.item_no_internet, container, false)
            rootView = binding.root

            initialise()
        }
        return rootView
    }


    private fun initialise() {
        binding.tvRetry.setOnClickListener {
            if (isConnectedToInternet(requireContext(), false)) {
                val intent = Intent()
                intent.action = NETWORK_ISSUE
                LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)

                requireActivity().finish()
            }
        }
    }


    companion object {
        const val NETWORK_ISSUE = "NETWORK_ISSUE"
    }
}
