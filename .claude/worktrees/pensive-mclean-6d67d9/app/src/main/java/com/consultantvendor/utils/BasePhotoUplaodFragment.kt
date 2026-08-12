package com.consultantvendor.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.consultantvendor.databinding.ItemDialogImageBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

import dagger.android.support.DaggerFragment


import java.io.*
import java.lang.Long
import java.util.Calendar


abstract class BasePhotoUplaodFragment : DaggerFragment() {

    private lateinit var permissionUtil: PermissionUtil
    private  var cameraUri: Uri?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionUtil = PermissionUtil(requireActivity())
        permissionUtil.registerLauncher(this)
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleImageResult(it) }
        }

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handlePdfResult(it) }
        }

    private val captureImageLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraUri?.let { handleImageResult(it) }
            }
        }

    private val cameraIntentVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri = it.data?.data
            uri?.let {
                val file = uriToFile(uri)
                getVideo(file?.absolutePath, 2)
            }
        }

    fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    fun openPdfPicker() {
        pdfPickerLauncher.launch("application/pdf")
    }

    fun openCameraImageCapture() {
        val photoFile =
            File.createTempFile("IMG_", ".jpg", requireContext().getExternalFilesDir(null))
        cameraUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        captureImageLauncher.launch(cameraUri)
    }

    fun openVideoGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        cameraIntentVideo.launch(intent)
    }

    fun startCameraIntentVideo(context: Context) {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        }
        cameraIntentVideo.launch(intent)
    }


    private fun fixImageOrientation(context: Context, uri: Uri): File? {
        return try {
            val inputStream1 = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream1)
            inputStream1?.close()

            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.let { ExifInterface(it) }
            exifInputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            Log.d("ImageFix", "EXIF orientation: $orientation")

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                // no flip, only rotation
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // Save to file WITHOUT EXIF
            val file = File(context.cacheDir, "final_upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun handleImageResult(uri: Uri) {
        val rotatedFile = fixImageOrientation(requireContext(), uri)
        rotatedFile?.let {
            getImage(it.absolutePath, Uri.fromFile(it)) // send new rotated file path & uri
        } ?: showError("Image rotation failed")
    }

   /* private fun handleImageResult(uri: Uri) {
        val file = uriToFile(uri)
        file?.let {
            getImage(it.absolutePath, uri)
        } ?: showError("Unable to get image file.")
    }*/

    private fun handlePdfResult(uri: Uri) {
        val file = uriToFile(uri)
        file?.let {
            getPdf(it.absolutePath)
        } ?: showError("Unable to get PDF file.")
    }

    internal fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "temp_file"
            val file = File(requireContext().cacheDir, fileName)
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val returnCursor = requireContext().contentResolver.query(uri, null, null, null, null)
        returnCursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun showImageDialog(
        isVideo: Boolean = false,
        showCamera: Boolean = false,
        showPdf: Boolean = false,
        showGallary: Boolean = false
    ) {
        val dialog = Dialog(requireContext())
        val view = ItemDialogImageBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(view.root)

        view.tvCamera.isVisible = showCamera
        view.tvPdf.isVisible = showPdf
        view.tvGallery.isVisible = showGallary

        view.ivClose.setOnClickListener { dialog.dismiss() }

        view.tvGallery.setOnClickListener {
            if (isVideo) openVideoGallery() else openGallery()
            dialog.dismiss()
        }

        view.tvCamera.setOnClickListener {
            if (isVideo) startCameraIntentVideo(requireContext()) else checkAndLaunchCamera()
            dialog.dismiss()
        }

        view.tvPdf.setOnClickListener {
            checkAndLaunchPdf()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun checkAndLaunchCamera() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        permissionUtil.checkPermissions(
            permissions,
            onGranted = { openCameraImageCapture() },
            onDenied = {},
            onPermanentlyDenied = { showPermissionSettingsDialog() }
        )
    }

    private fun checkAndLaunchPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // No permission needed from Android 13 onwards for PDFs
            openPdfPicker()
        } else {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissionUtil.checkPermissions(
                permissions,
                onGranted = { openPdfPicker() },
                onDenied = {},
                onPermanentlyDenied = { showPermissionSettingsDialog() }
            )
        }
    }

    fun showPermissionSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Need Permissions")
            .setMessage("This app needs permission to use this feature. You can grant them in app settings.")
            .setPositiveButton("GOTO SETTINGS") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    abstract fun getPdf(uri: String?)
    abstract fun getImage(uri: String?, data: Uri)
    abstract fun getVideo(uri: String?, i: Int)
}



