package com.consultantvendor.ui.dashboard.language

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentLanguageBinding
import com.consultantvendor.ui.dashboard.HomeActivity
import com.consultantvendor.utils.*
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class LanguageFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentLanguageBinding

    private var rootView: View? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_language, container, false)
            rootView = binding.root

            initialise()
            listeners()
        }
        return rootView
    }

    private fun initialise() {
        requireActivity().setResult(Activity.RESULT_OK)

        if (userRepository.isUserLoggedIn()) {
            binding.toolbar.visible()

            val lan = userRepository.getUserLanguage()
            if (lan == "en")
                binding.rbEnglish.isChecked = true
            else if (lan == "ar")
                binding.rbArabic.isChecked = true
        } else {
            binding.toolbar.gone()
        }
    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.rbLanguage.setOnCheckedChangeListener { radioGroup, i ->
            if (isConnectedToInternet(requireContext(), true)) {
                if (i == R.id.rbEnglish) {
                    prefsManager.save(USER_LANGUAGE, "en")
                    LocaleHelper.setLocale(requireActivity(), "en", prefsManager)
                } else {
                    prefsManager.save(USER_LANGUAGE, "ar")
                    LocaleHelper.setLocale(requireActivity(), "ar", prefsManager)
                }

                /*prefsManager.save(USER_LANGUAGE, "en")
            LocaleHelper.setLocale(requireActivity(), "en", prefsManager)*/

                /*get updated pages*/
                if (BuildConfig.FLAVOR == "homeDoctor")
                    userRepository.getPages()

                if (userRepository.isUserLoggedIn()) {
                    requireActivity().setResult(Activity.RESULT_CANCELED)
                    ActivityCompat.finishAffinity(requireActivity())

                    startActivity(Intent(activity, HomeActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                } else
                    requireActivity().finish()
            }
        }
    }
}
