package com.consultantvendor.ui.walkthrough

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Page
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentWalkthroughBinding
import com.consultantvendor.ui.adapter.CommonFragmentPagerAdapter
import com.consultantvendor.utils.POSITION
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.hideShowView
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class WalkThroughFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentWalkthroughBinding

    private var rootView: View? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_walkthrough, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setBanners()
        }
        return rootView
    }

    private fun initialise() {
        requireActivity().setResult(Activity.RESULT_OK)
    }

    private fun setBanners() {
        val adapter = CommonFragmentPagerAdapter(requireActivity().supportFragmentManager)

        val items = ArrayList<Page>()

        when (BuildConfig.FLAVOR) {
            "consult", "edu", "marketplace", "healthcare" -> {
                items.add(Page(icon = R.drawable.ic_1, title = getString(R.string.walkthrough_1), desc = getString(R.string.walk_through_desc_1)))
                items.add(Page(icon = R.drawable.ic_2, title = getString(R.string.walkthrough_2), desc = getString(R.string.walk_through_desc_2)))
                items.add(Page(icon = R.drawable.ic_3, title = getString(R.string.walkthrough_3), desc = getString(R.string.walk_through_desc_3)))
                items.add(Page(icon = R.drawable.ic_4, title = getString(R.string.walkthrough_4), desc = getString(R.string.walk_through_desc_4)))
            }
            "heal" -> {
                items.add(Page(icon = R.drawable.ic_6, title = getString(R.string.walkthrough_6), desc = getString(R.string.walk_through_desc_6)))
                items.add(Page(icon = R.drawable.ic_2, title = getString(R.string.walkthrough_2), desc = getString(R.string.walk_through_desc_2)))
                items.add(Page(icon = R.drawable.ic_1, title = getString(R.string.walkthrough_1), desc = getString(R.string.walk_through_desc_1)))
                items.add(Page(icon = R.drawable.ic_4, title = getString(R.string.walkthrough_4), desc = getString(R.string.walk_through_desc_4)))
                items.add(Page(icon = R.drawable.ic_5, title = getString(R.string.walkthrough_5), desc = getString(R.string.walk_through_desc_5)))
            }
        }

        items.forEach {
            val fragment = WalkThroughDetailFragment()
            val bundle = Bundle()
            bundle.putSerializable(POSITION, it)
            fragment.arguments = bundle
            adapter.addTab("", fragment)
        }

        binding.viewPager.adapter = adapter

        binding.pageIndicatorView.setViewPager(binding.viewPager)
    }

    private fun listeners() {
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                val show = position == binding.viewPager.adapter?.count?.minus(1)

                binding.tvSkip.hideShowView(!show)
                binding.tvGetStarted.hideShowView(show)
            }
        })

        binding.tvGetStarted.setOnClickListener {
            doneWalkThrough()
        }

        binding.tvSkip.setOnClickListener {
            doneWalkThrough()
        }
    }

    private fun doneWalkThrough() {
        prefsManager.save(WALK_THROUGH_SCREEN, true)
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    companion object {
        const val WALK_THROUGH_SCREEN = "WALK_THROUGH_SCREEN"
    }
}
