package com.consultantvendor.ui.dashboard.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.appFeatures
import com.consultantvendor.data.models.responses.Page
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.ProviderType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentSettingsBinding
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class SettingsFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentSettingsBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: LoginViewModel

    private var items = ArrayList<Page>()

    private lateinit var adapter: PagesAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
            setUserProfile()

        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

    }

    private fun setAdapter() {
        items.clear()

        items.add(
            Page(
                title = getString(R.string.account_setting),
                icon = R.drawable.ic_profile_setting
            )
        )
        if (BuildConfig.FLAVOR == "homeDoctor")
            items.add(Page(title = getString(R.string.bank_info), icon = R.drawable.ic_bank))
        else if (BuildConfig.FLAVOR == "nurseLynx")
            items.add(Page(title = getString(R.string.contacts), icon = R.drawable.ic_contacts))

        if (userRepository.getUser()?.provider_type == ProviderType.email)
            items.add(
                Page(
                    title = getString(R.string.change_password),
                    icon = R.drawable.ic_password
                )
            )
        //if (appFeatures.needLanguageScreen)
            items.add(Page(title = getString(R.string.language), icon = R.drawable.ic_language))

        items.add(Page(title = getString(R.string.chat), icon = R.drawable.ic_chat_profile))
        items.add(Page(title = getString(R.string.history), icon = R.drawable.ic_history))
        if (appFeatures.freeExpertAdvise)
            items.add(
                Page(
                    title = getString(R.string.free_expert_advice),
                    icon = R.drawable.ic_class_profile
                )
            )
        //if (appFeatures.needClasses)
        if (BuildConfig.FLAVOR == "taradoc")
            items.add(Page(title = getString(R.string.broadcasts), icon = R.drawable.ic_class_profile))
        else
            items.add(Page(title = getString(R.string.classes), icon = R.drawable.ic_class_profile))
        items.add(
            Page(
                title = getString(R.string.notification),
                icon = R.drawable.ic_notification_drawer
            )
        )
        items.add(Page(title = getString(R.string.invite_people), icon = R.drawable.ic_invite))

        if (BuildConfig.FLAVOR != "taradoc") {
            val pages = appClientDetails.pages
           /* items.add(
                Page(
                    title = pages?.get(0)?.title,
                    slug = pages?.get(0)?.slug,
                    app_type = pages?.get(0)?.app_type,
                    icon = pages?.get(0)?.icon
                ))*/
            appClientDetails.pages?.forEach {
                items.add(
                    Page(
                        title = it.title,
                        slug = it.slug,
                        app_type = it.app_type,
                        icon = R.drawable.ic_info
                    )
                )
            }
        }

        items.add(Page(title = getString(R.string.logout), icon = R.drawable.ic_logout))

        adapter = PagesAdapter(this, items)
        binding.rvPages.adapter = adapter
    }

    private fun setUserProfile() {
        val userData = userRepository.getUser()

        binding.tvName.text = getDoctorName(userData)
        binding.tvAge.text = "${getString(R.string.age)} ${getAge(userData?.profile?.dob)}"
        loadImage(binding.ivPic, userData?.profile_image, R.drawable.ic_profile_placeholder)

        binding.tvVersion.text =
            getString(R.string.version, getVersion(requireActivity()).versionName)
    }

    private fun listeners() {
        binding.ivPic.setOnClickListener {
            val itemImages = java.util.ArrayList<String>()
            itemImages.add(
                getImageBaseUrl(
                    ImageFolder.UPLOADS,
                    userRepository.getUser()?.profile_image
                )
            )
            viewImageFull(requireActivity(), itemImages, 0)
        }

        binding.tvName.setOnClickListener {
            goToProfile()
        }


    }

    fun itemClicked(pos: Int) {
        when (items[pos].title) {
            getString(R.string.account_setting) -> {
                goToProfile()
            }
            getString(R.string.bank_info) -> {
                openScreen(DrawerActivity.BANK_INFO)
            }
            getString(R.string.change_password) -> {
                openScreen(DrawerActivity.CHANGE_PASSWORD)
            }
            getString(R.string.language) -> {
                openScreen(DrawerActivity.LANGUAGE_SCREEN)
            }
            getString(R.string.chat) -> {
                openScreen(DrawerActivity.USER_CHAT)
            }
            getString(R.string.history) -> {
                openScreen(DrawerActivity.HISTORY)
            }
            getString(R.string.free_expert_advice) -> {
                openScreen(DrawerActivity.MY_QUESTION)
            }
            getString(R.string.classes) -> {
                openScreen(DrawerActivity.CLASSES)
            }
            getString(R.string.notification) -> {
                openScreen(DrawerActivity.NOTIFICATION)
            }
            getString(R.string.invite_people) -> {
                shareDeepLink1(DeepLink.INVITE, requireActivity(), userRepository.getUser())
            }
            getString(R.string.contacts) -> {
                openScreen(DrawerActivity.CONTACT_LIST)
            }
            getString(R.string.logout) -> {
                showLogoutDialog()
            }
            else -> {
                openPageLink(
                    requireContext(),
                    items[pos].title,
                    items[pos].slug,
                    items[pos].app_type
                )
            }
        }
    }

    private fun goToProfile() {
        startActivityForResult(
            Intent(requireContext(), DrawerActivity::class.java)
                .putExtra(PAGE_TO_OPEN, DrawerActivity.PROFILE), AppRequestCode.PROFILE_UPDATE
        )

    }

    private fun openScreen(page: String) {
        startActivity(
            Intent(requireContext(), DrawerActivity::class.java)
                .putExtra(PAGE_TO_OPEN, page)
        )
    }


    private fun showLogoutDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(
            requireContext(), R.string.logout,
            R.string.logout_dialog_message, R.string.yes, R.string.no, false,
            object : AlertDialogUtil.OnOkCancelDialogListener {
                override fun onOkButtonClicked() {
                    viewModel.logout()
                }

                override fun onCancelButtonClicked() {
                }
            }).show()
    }

    private fun bindObservers() {
        viewModel.logout.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    logoutUser(requireActivity(), prefsManager)
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
            setUserProfile()
        }
    }

}