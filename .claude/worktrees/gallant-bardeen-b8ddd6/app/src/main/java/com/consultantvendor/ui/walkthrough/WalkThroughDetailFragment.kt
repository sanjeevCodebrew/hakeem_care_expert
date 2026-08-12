package com.consultantvendor.ui.walkthrough

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Page
import com.consultantvendor.databinding.FragmentWalkthroughDetailBinding
import com.consultantvendor.utils.POSITION
import dagger.android.support.DaggerFragment


class WalkThroughDetailFragment : DaggerFragment() {

    private lateinit var binding: FragmentWalkthroughDetailBinding

    private var rootView: View? = null


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_walkthrough_detail, container, false)
            rootView = binding.root

            initialise()
        }
        return rootView
    }

    private fun initialise() {
        val data = arguments?.getSerializable(POSITION) as Page

        binding.ivImage.setImageResource(data.icon ?: R.drawable.ic_1)
        binding.tvTitle.text = data.title
        binding.tvDesc.text = data.desc
    }
}
