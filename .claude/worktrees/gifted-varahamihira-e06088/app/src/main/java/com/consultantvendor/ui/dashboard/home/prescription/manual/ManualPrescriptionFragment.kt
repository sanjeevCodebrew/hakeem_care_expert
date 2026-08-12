package com.consultantvendor.ui.dashboard.home.prescription.manual

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.Request
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentManualPrescriptionBinding
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.dashboard.home.prescription.AddPrescriptionViewModel
import com.consultantvendor.utils.AppRequestCode
import com.consultantvendor.utils.BasePhotoUplaodFragment
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.EXTRA_REQUEST_ID
/*import com.consultantvendor.utils.PermissionUtils
import com.consultantvendor.utils.PermissionUtils.cameraAndStorageAccess
import com.consultantvendor.utils.PermissionUtils.hasPermissions*/
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.PrescriptionType
import com.consultantvendor.utils.compressImage
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.consultantvendor.utils.getAge
import com.consultantvendor.utils.getRequestBody
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.loadImage
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

class ManualPrescriptionFragment : BasePhotoUplaodFragment(){

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentManualPrescriptionBinding

    private var rootView: View? = null

    private lateinit var viewModelUpload: UploadFileViewModel

    private lateinit var addPrescriptionViewModel: AddPrescriptionViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private var imagesAdapter: ImagesAdapter? = null

    private var itemImages = ArrayList<DocImage>()

    private var request: Request? = null

    private var addPrescription: AddPrescription? = null

    private var fileToUpload: File? = null

//    private val storagePermissionLauncherLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (permissions.values.any { !it }) {
//                PermissionUtils.showAppSettingsDialog(
//                    requireContext(), R.string.media_permission
//                )
//                return@registerForActivityResult
//            }
//            getStorage()
//        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_manual_prescription, container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            setEditPrescriptionData()
            listeners()
            bindObservers()
            hitApi(true)
        }
        return rootView
    }


    private fun initialise() {
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        addPrescriptionViewModel = ViewModelProvider(this, viewModelFactory)[AddPrescriptionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())

        request = requireActivity().intent.getSerializableExtra(EXTRA_REQUEST_ID) as Request

        binding.tvName.text = request?.from_user?.name
        binding.tvAge.text = "${getAge(request?.from_user?.profile?.dob)} ${getString(R.string.years_old)}"
        loadImage(
            binding.ivPic, request?.from_user?.profile_image,
            R.drawable.ic_profile_placeholder
        )

        binding.tvAppointmentV.text = "${DateUtils.dateTimeFormatFromUTC(DateFormat.MON_DATE_YEAR, request?.bookingDateUTC)} · " +
                "${DateUtils.dateTimeFormatFromUTC(DateFormat.TIME_FORMAT, request?.bookingDateUTC)}"
    }

    private fun setEditPrescriptionData() {
        if (request?.pre_scription != null) {
            val prescription = request?.pre_scription
            binding.etRecordDetails.setText(prescription?.title)

            itemImages.clear()
            var docImage: DocImage
            prescription?.images?.forEach {
                docImage = DocImage()
                docImage.image = it
                itemImages.add(docImage)
            }

            imagesAdapter?.notifyDataSetChanged()
        }
    }

    private fun setAdapter() {
        imagesAdapter = ImagesAdapter(this, itemImages)
        binding.rvImages.adapter = imagesAdapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvDone.setOnClickListener {
            when {
                binding.etRecordDetails.text.toString().trim().isEmpty() -> {
                    binding.etRecordDetails.showSnackBar(getString(R.string.record_title))
                }

                itemImages.isEmpty() -> {
                    binding.etRecordDetails.showSnackBar(getString(R.string.select_image))
                }

                isConnectedToInternet(requireContext(), true) -> {
                    addPrescription = AddPrescription()
                    addPrescription?.request_id = request?.id
                    addPrescription?.type = PrescriptionType.MANUAL
                    addPrescription?.title = binding.etRecordDetails.text.toString().trim()

                    addPrescription?.image = ArrayList()

                    itemImages.forEach {
                        if (it.imageFile != null) {
                            uploadFileOnServer(it)
                            return@forEach
                        } else if (!it.image.isNullOrEmpty()) {
                            addPrescription?.image?.add(it.image ?: "")
                        }
                    }

                    if ((addPrescription?.image?.size ?: 0) == itemImages.size)
                        addPrescriptionViewModel.prescreptions(
                            addPrescription
                                ?: AddPrescription()
                        )
                    //uploadFileOnServer(itemImages[0])
                }
            }
        }
    }

    private fun hitApi(firstHit: Boolean) {

    }

    private fun uploadFileOnServer(docImage: DocImage?) {

        val hashMap = java.util.HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }

    private fun bindObservers() {
      /*  viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    addPrescription?.image?.add(it.data?.image_name ?: "")

                    if (addPrescription?.image?.size ?: 0 < itemImages.size) {
                        for (i in (addPrescription?.image?.size ?: 0)..itemImages.size) {
                            val docImage = itemImages[addPrescription?.image?.size ?: 0]

                            if (docImage.imageFile != null) {
                                uploadFileOnServer(docImage)
                                break
                            } else if (!docImage.image.isNullOrEmpty()) {
                                addPrescription?.image?.add(docImage.image ?: "")
                            }
                            //uploadFileOnServer(itemImages[addPrescription?.image?.size ?: 0])
                        }

                        if (addPrescription?.image?.size ?: 0 == itemImages.size)
                            addPrescriptionViewModel.prescreptions(
                                addPrescription
                                    ?: AddPrescription()
                            )
                    }
                    else
                    {
                        addPrescriptionViewModel.prescreptions(addPrescription ?: AddPrescription())
                    }
                }

                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    progressDialogImage.setLoading(true)

                }
            }
        })*/

        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    // Add the uploaded image name
                    addPrescription?.image?.add(it.data?.image_name ?: "")

                    val currentSize = addPrescription?.image?.size ?: 0

                    // Check if all images are uploaded
                    if (currentSize == itemImages.size) {
                        addPrescriptionViewModel.prescreptions(addPrescription ?: AddPrescription())
                    } else {
                        // Upload the next image if not already uploaded
                        val nextDocImage = itemImages[currentSize]
                        if (nextDocImage.imageFile != null) {
                            uploadFileOnServer(nextDocImage)
                        } else if (!nextDocImage.image.isNullOrEmpty()) {
                            addPrescription?.image?.add(nextDocImage.image ?: "")
                            // Re-trigger observer to move to the next one
                            viewModelUpload.uploadFile.postValue(Resource.success(it.data)) // Fake trigger to continue
                        }
                    }
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

        addPrescriptionViewModel.prescreptions.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().finish()

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

    /*Adapter item click*/
    fun clickItem() {
//        if (hasPermissions(cameraAndStorageAccess)) {
//            getStorage()
//        } else {
//            storagePermissionLauncherLauncher.launch(cameraAndStorageAccess)
//        }
//        showImageDialog(false,false,false)
    }

   /* override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.IMAGE_PICKER -> {
                    val docPaths = ArrayList<Uri>()
                    docPaths.addAll(
                        data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA)
                            ?: emptyList()
                    )

                    val fileToUpload = compressImage(requireActivity(), File(ContentUriUtils.getFilePath(requireContext(), docPaths[0])))

                    val docImage = DocImage()
                    docImage.type = DocType.IMAGE
                    docImage.imageFile = fileToUpload

                    itemImages.add(docImage)
                    imagesAdapter?.notifyDataSetChanged()
                }
            }

            *//* if (requestCode == AppRequestCode.IMAGE_PICKER) {
                 val docPaths = ArrayList<Uri>()
                 docPaths.addAll(data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA)
                         ?: emptyList())

                 val fileToUpload = File(getPathUri(requireContext(), docPaths[0]))

                 itemImages.add(fileToUpload)
                 imagesAdapter?.notifyDataSetChanged()
             }*//*
        }
    }*/

    override fun getPdf(uri: String?) {

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun getImage(uri: String?, data: Uri) {

        val selectedImageUri: Uri? = data
        selectedImageUri?.let {
            val file: File? = uriToFile(requireContext(), it)
            file?.let {
                val docImage = DocImage()
                docImage.type = DocType.IMAGE
                docImage.imageFile = it

                itemImages.add(docImage)
                imagesAdapter?.notifyDataSetChanged()

            }
        }
    }

    override fun getVideo(uri: String?, i: Int) {

    }

    fun uriToFile(context: Context, uri: Uri): File? {
        val path = getRealPathFromUri(context, uri)
        return path?.let { File(it) }
    }

    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        var realPath: String? = null
        // Depending on the URI scheme, use different query methods
        if (uri.scheme == "content") {
            // MediaStore (and general content provider) scheme
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    realPath = cursor.getString(columnIndex)
                }
            }
        } else if (uri.scheme == "file") {
            // File scheme
            realPath = uri.path
        }
        return realPath
    }


//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        onRequestPermissionsResult(requestCode, grantResults)
//    }

    //    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//    private fun getStorage() {
//        selectImages(this, requireActivity())
//    }

    /*@OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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