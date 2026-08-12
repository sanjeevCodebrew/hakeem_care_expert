package com.consultantvendor.ui.dashboard.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Banner
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentBannerBinding
import com.consultantvendor.databinding.FragmentHomeBannerBinding
import com.consultantvendor.utils.loadImage
import com.google.firebase.installations.BuildConfig
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class HomeBannerFragment(private val fragment: Fragment?, private val banner: Banner) : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentHomeBannerBinding

    private var rootView: View? = null


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_banner, container, false)
            rootView = binding.root

            initialise()
        }
        return rootView
    }

    private fun initialise() {
        if (fragment is HomeFragment) {
            binding.clBanner.setBackgroundResource(0)
            loadImage(binding.ivImage, banner.image_mobile, R.drawable.drawable_alternate_button)

//            if(BuildConfig.FLAVOR != "taradoc")
//                binding.ivImage.setOnClickListener {
//                    when (banner.banner_type) {
//                        BannerType.SERVICE_PROVIDER -> {
//                            startActivity(Intent(binding.root.context, DoctorDetailActivity::class.java)
//                                .putExtra(DOCTOR_ID, banner.sp_id))
//                        }
//                        BannerType.CATEGORY -> {
//                            if (banner.category?.is_subcategory == true) {
//                                startActivity(
//                                    Intent(requireContext(), DrawerActivity::class.java)
//                                    .putExtra(PAGE_TO_OPEN, DrawerActivity.SUB_CATEGORY)
//                                    .putExtra(SubCategoryFragment.CATEGORY_PARENT_ID, banner.category))
//                            } else {
//                                startActivity(
//                                    Intent(requireContext(), DoctorListActivity::class.java)
//                                    .putExtra(SubCategoryFragment.CATEGORY_PARENT_ID, banner.category))
//                            }
//                        }
//                        BannerType.CLASS_ -> {
//                            if (userRepository.getUser() == null) {
//                                val fragment = WelcomeFragment()
//                                fragment.show(requireActivity().supportFragmentManager, fragment.tag)
//                            } else {
//                                startActivity(
//                                    Intent(requireContext(), DrawerActivity::class.java)
//                                    .putExtra(PAGE_TO_OPEN, DrawerActivity.CLASSES_DETAILS)
//                                    .putExtra(CLASS_ID, banner.class_id))
//                            }
//                        }
//                    }
//                }
        } else {
//            binding.clBanner.setBackgroundResource(R.drawable.drawable_theme_trans)
//            binding.tvCode.visible()
//            binding.tvText.visible()
//            binding.tvUsers.visible()

//            val discount = if (banner.discount_type == "percentage")
//                "${banner.discount_value}%"
//            else
//                banner.discount_value
//
//            val service = when {
//                banner.service != null -> banner.service?.name
//                banner.category != null -> banner.category?.name
//                else -> ""
//            }
//
//            binding.tvText.text = getHtmlText(getString(R.string.code_text, discount,
//                service, banner.end_date))
//            binding.tvCode.text = getHtmlText(getString(R.string.use_code_s, banner.coupon_code))
//            binding.tvUsers.text = getHtmlText(getString(R.string.s_user_remaining, banner.limit.toString()))
        }
    }
}

object BannerType {
    const val SERVICE_PROVIDER = "service_provider"
    const val CATEGORY = "category"
    const val CLASS_ = "class"
}
