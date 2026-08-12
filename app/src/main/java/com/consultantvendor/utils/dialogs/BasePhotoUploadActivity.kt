package com.consultantvendor.utils.dialogs

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
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
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.consultantvendor.BuildConfig
import com.consultantvendor.databinding.DialogProfileBinding
import com.consultantvendor.utils.PermissionUtil
import dagger.android.support.DaggerAppCompatActivity
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

abstract class BasePhotoUploadActivity : DaggerAppCompatActivity() {

    private lateinit var cameraUri: Uri

    private var cameraFile: File? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            getImage(null, it)
        }
    }

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            // Persist permission if needed
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // Safely copy the PDF to internal storage
            val path = copyPdfToInternalStorage(it)
            if (path != null) {
                getPdf(path) // Your override method
            } else {
                Toast.makeText(this, "Invalid PDF file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val captureImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            val path = cameraFile?.absolutePath
            getImage(path, cameraUri!!)
        } else {
            Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
        }
    }


    private val captureVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let { getVideo(it.toString(), 2) }
        }
    }

    private fun copyPdfToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri) ?: "document_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            val outputStream = FileOutputStream(pdfFile)
            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            pdfFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    fun showImageDialog(includeVideo: Boolean = false) {
        val dialog = Dialog(this)
        val view = DialogProfileBinding.inflate(layoutInflater)
        dialog.setContentView(view.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.tvGallery.setOnClickListener {
            if (!includeVideo) openPhotoPicker() else openVideoPicker()
            dialog.dismiss()
        }

        view.tvCamera.setOnClickListener {
            checkCameraPermission {
                if (!includeVideo) openCameraImageCapture() else openCameraVideoCapture()
            }
            dialog.dismiss()
        }

        view.tvPdf.setOnClickListener {
            openPdfPicker()
            dialog.dismiss()
        }

        view.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun openPhotoPicker() {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openPdfPicker() {
        pickPdfLauncher.launch(arrayOf("application/pdf"))
    }

    private fun openCameraImageCapture() {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        cameraFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

        cameraUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            cameraFile!!
        )

        captureImageLauncher.launch(cameraUri)
    }

    private fun openCameraVideoCapture() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        captureVideoLauncher.launch(videoIntent)
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        captureVideoLauncher.launch(intent)
    }

    private fun checkCameraPermission(onGranted: () -> Unit) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    abstract fun getPdf(uri: String)
    abstract fun getImage(filePath: String?, data: Uri)
    abstract fun getVideo(path: String, type: Int)
}