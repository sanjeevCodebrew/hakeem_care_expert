package com.consultantvendor.ui.dashboard.feeds

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentAddFeedBinding
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.drawermenu.DrawerActivity
import com.consultantvendor.utils.AppRequestCode
import com.consultantvendor.utils.BlogType
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.PAGE_TO_OPEN
import com.consultantvendor.utils.PermissionUtils
import com.consultantvendor.utils.PermissionUtils.hasPermissions
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.compressImage
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.consultantvendor.utils.editTextScroll
import com.consultantvendor.utils.getRequestBody
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.resultFragmentIntent
import com.consultantvendor.utils.selectImages
import com.consultantvendor.utils.showSnackBar
import dagger.android.support.DaggerFragment
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.utils.ContentUriUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class AddFeedFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentAddFeedBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private lateinit var viewModel: FeedViewModel

    private lateinit var viewModelUpload: UploadFileViewModel

    private var fileToUpload: File? = null

    private var typeOfBlog = BlogType.BLOG

    private val storagePermissionLauncherLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.any { !it }) {
                PermissionUtils.showAppSettingsDialog(
                    requireContext(), R.string.media_permission
                )
                return@registerForActivityResult
            }
            getStorage()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_add_feed, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setEditInformation()
            bindObservers()
        }
        return rootView
    }


    private fun initialise() {
        editTextScroll(binding.etDesc)

        when (requireActivity().intent.getStringExtra(PAGE_TO_OPEN)) {
            BlogType.BLOG, DrawerActivity.ADD_BLOG -> {
                typeOfBlog = BlogType.BLOG
                binding.tvHeader.text = getString(R.string.post_blog)
            }

            BlogType.ARTICLE, DrawerActivity.ADD_ARTICLE -> {
                typeOfBlog = BlogType.ARTICLE

                binding.tvHeader.text = getString(R.string.post_article)
            }

            else -> {
                typeOfBlog = BlogType.ARTICLE
                binding.tvHeader.text = getString(R.string.latest_articles)
            }
        }

        viewModel = ViewModelProvider(this, viewModelFactory)[FeedViewModel::class.java]
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())
    }

    private fun setEditInformation() {

    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.ivNext.setOnClickListener {
            checkValidation()
        }

        binding.ivPic.setOnClickListener {
        /*    if (hasPermissions(cameraAndStorageAccess)) {
                getStorage()
            } else {
                storagePermissionLauncherLauncher.launch(cameraAndStorageAccess)
            }*/
        }
    }


    private fun checkValidation() {
        when {
            fileToUpload == null -> {
                binding.etTitle.showSnackBar(getString(R.string.select_image))
            }

            binding.etTitle.text.toString().isEmpty() -> {
                binding.etTitle.showSnackBar(getString(R.string.title))
            }

            binding.etDesc.text.toString().isEmpty() -> {
                binding.etDesc.showSnackBar(getString(R.string.description))
            }

            isConnectedToInternet(requireContext(), true) -> {
                uploadFileOnServer()
            }
        }
    }


    private fun uploadFileOnServer() {

        val hashMap = HashMap<String, RequestBody>()

        hashMap["type"] = getRequestBody(DocType.IMAGE)

        val body: RequestBody = fileToUpload?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + fileToUpload?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }

    private fun bindObservers() {
        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    /*Create Article*/
                    val hashMap = HashMap<String, String>()
                    hashMap["title"] = binding.etTitle.text.toString()
                    hashMap["description"] = binding.etDesc.text.toString()
                    hashMap["type"] = typeOfBlog
                    hashMap["image"] = it.data?.image_name ?: ""

                    viewModel.feeds(hashMap)

                }

                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    progressDialogImage.setLoading(true)

                }
            }
        })

        viewModel.feeds.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                        resultFragmentIntent(
                            this, targetFragment ?: this,
                            AppRequestCode.ARTICLE_CHANGES, Intent()
                        )
                    else {
                        requireActivity().setResult(Activity.RESULT_OK)
                        requireActivity().finish()
                    }
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

            if (requestCode == AppRequestCode.IMAGE_PICKER) {
                val docPaths = ArrayList<Uri>()
                docPaths.addAll(
                    data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA)
                        ?: emptyList()
                )

                fileToUpload = compressImage(requireActivity(), File(ContentUriUtils.getFilePath(requireContext(), docPaths[0])))
                Glide.with(requireContext()).load(fileToUpload).centerCrop().into(binding.ivPic)
                binding.tvUploadBanner.gone()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun getStorage() {
        selectImages(this, requireActivity())
    }

    /*@NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getStorage() {
        selectImages(this, requireActivity())
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(requireContext(), R.string.media_permission, request)
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
            requireContext(), R.string.media_permission
        )
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
            requireContext(), R.string.media_permission
        )
    }*/

}
