package com.consultantvendor.ui.dashboard.language

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentLanguageBinding
import com.consultantvendor.ui.dashboard.HomeActivity
import com.consultantvendor.ui.loginSignUp.login.LoginActivity
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
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

    private lateinit var viewModelLanguage: LanguageViewModel

    private var language = ""

    private  var progressDialog: ProgressDialog?=null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_language, container, false)
            rootView = binding.root

            initialise()
            listeners()
            bindeObserver()
        }
        return rootView
    }


    private fun initialise() {
        if (::viewModelFactory.isInitialized) {
            viewModelLanguage = ViewModelProvider(this, viewModelFactory)[LanguageViewModel::class.java]
        }
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
                    language = "en"
                    LocaleHelper.setLocale(requireActivity(), "en", prefsManager)
                } else {
                    prefsManager.save(USER_LANGUAGE, "ar")
                    language = "ar"
                    LocaleHelper.setLocale(requireActivity(), "ar", prefsManager)
                }

                /*prefsManager.save(USER_LANGUAGE, "en")
                LocaleHelper.setLocale(requireActivity(), "en", prefsManager)*/

                /*get updated pages*/
                if (BuildConfig.FLAVOR == "consult")
                    userRepository.getPages()

                if (userRepository.isUserLoggedIn()) {
//                    requireActivity().setResult(Activity.RESULT_CANCELED)
//                    ActivityCompat.finishAffinity(requireActivity())
//
//                    startActivity(Intent(activity, HomeActivity::class.java)
//                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    val hashMap = HashMap<String, String>()
                    hashMap["language"] = language
                    viewModelLanguage.postLanguage(hashMap)
                } else
                    requireActivity().finish()
            }
        }
    }

    private fun bindeObserver() {

        viewModelLanguage.postLanguage.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog?.setLoading(false)
                    requireActivity().setResult(Activity.RESULT_CANCELED)
                    ActivityCompat.finishAffinity(requireActivity())

                    startActivity(Intent(activity, HomeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                }
                Status.ERROR -> {
                    progressDialog?.setLoading(false)
                }
                Status.LOADING -> {
                    progressDialog?.setLoading(true)
                }
            }
        })

    }
}
