package com.consultantvendor.utils.dialogs

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import android.view.View
import android.view.Window
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
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

    var is_video = false
    private val JPEG_FILE_PREFIX = "IMG_"
    private val JPEG_FILE_SUFFIX = ".jpg"


    var mPicturePath: String? = null
    var mIsGallery: Boolean? = null


    private lateinit var permissionUtil: PermissionUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionUtil = PermissionUtil(this)
        permissionUtil.registerLauncher(this)
    }


    fun showImageDialog(b: Boolean) {


        is_video = b


        val dialog: Dialog = Dialog(this)
        var view = DialogProfileBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(view.root)

        view.tvGallery.setOnClickListener {
            mIsGallery = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                permissionUtil.checkPermissions(
                    permissions = arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES
                    ),
                    onGranted = {
                        if (!is_video) {
                            openGallery()
                        } else {
                            openVideoGallery()
                        }
                    },
                    onDenied = {
                        // Handle the case where permissions are denied but not permanently
                    },
                    onPermanentlyDenied = {
                        // Optionally handle additional logic here after showing the settings dialog
                    }
                )
            } else {

                permissionUtil.checkPermissions(
                    permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    onGranted = {
                        if (!is_video) {
                            openGallery()
                        } else {
                            openVideoGallery()
                        }
                    },
                    onDenied = {
                        // Handle the case where permissions are denied but not permanently
                    },
                    onPermanentlyDenied = {
                        // Optionally handle additional logic here after showing the settings dialog
                    }
                )


            }


            dialog.dismiss()
        }

        view.ivClose.setOnClickListener { dialog.dismiss() }

        view.tvCamera.setOnClickListener {
            mIsGallery = false

            permissionUtil.checkPermissions(
                permissions = arrayOf(
                    Manifest.permission.CAMERA
                ),
                onGranted = {
                    if (!is_video) {
                        /*     startCameraIntent(this)*/
                        openCameraChat()
                    } else {
                        startCameraIntentVideo(this)
                    }
                },
                onDenied = {
                    // Handle the case where permissions are denied but not permanently
                },
                onPermanentlyDenied = {
                    // Optionally handle additional logic here after showing the settings dialog
                }
            )





            dialog.dismiss()
        }
        view.tvPdf.setOnClickListener {

            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            permissionUtil.checkPermissions(
                permissions = permissions,
                onGranted = {
                    openPdf()
                },
                onDenied = {
                    // Handle the case where permissions are denied but not permanently
                },
                onPermanentlyDenied = {
                    // Optionally handle additional logic here after showing the settings dialog

                }
            )





            dialog.dismiss()
        }
        dialog.show()
    }

    var imageUri : Uri?= null

    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
                if (it.resultCode === RESULT_OK) {


                    val realPath  = getRealPath(imageUri?:Uri.EMPTY,this)
                    getImage(realPath, Uri.EMPTY)
                }


            }
        )

    private fun openCameraChat() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(cameraIntent)
    }


    fun selectMultipleImageDialog() {


        val dialog: Dialog = Dialog(this)
        var view = DialogProfileBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(view.root)

        view.tvGallery.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {


                permissionUtil.checkPermissions(
                    permissions = arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES
                    ),
                    onGranted = {
                        openMultipleGallery()
                    },
                    onDenied = {
                        // Handle the case where permissions are denied but not permanently
                    },
                    onPermanentlyDenied = {
                        // Optionally handle additional logic here after showing the settings dialog
                    }
                )
            } else {
                permissionUtil.checkPermissions(
                    permissions = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    onGranted = {
                        openMultipleGallery()
                    },
                    onDenied = {
                        // Handle the case where permissions are denied but not permanently
                    },
                    onPermanentlyDenied = {
                        // Optionally handle additional logic here after showing the settings dialog
                    }
                )
            }


            dialog.dismiss()
        }

        view.ivClose.setOnClickListener { dialog.dismiss() }

        view.tvCamera.setOnClickListener {
            permissionUtil.checkPermissions(
                permissions = arrayOf(
                    Manifest.permission.CAMERA
                ),
                onGranted = {
//                    registerActivityResult.launch(
//                        Intent(requireContext(), DrawerActivity::class.java).putExtra(
//                            PAGE_TO_OPEN, CAMERAMULTIPLE
//                        )
//                    )
                },
                onDenied = {
                    // Handle the case where permissions are denied but not permanently
                },
                onPermanentlyDenied = {
                    // Optionally handle additional logic here after showing the settings dialog
                }
            )
            dialog.dismiss()
        }
        dialog.show()
    }

//    fun showImageDialogForPdfAndVideo() {
//
//
//        val dialog: Dialog = Dialog(this)
//        var view = DialogProfileBinding.inflate(layoutInflater)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.setContentView(view.root)
//
//        view.tvGallery.text= "PDF"
//        view.tvCamera.text= "Video"
//        view.tvGallery.setOnClickListener {
//
//            openPdf()
//
//            dialog.dismiss()
//        }
//
//        view.ivClose.setOnClickListener { dialog.dismiss() }
//
//        view.tvCamera.setOnClickListener {
//
//            showImageDialog(true)
//
//            dialog.dismiss()
//        }
//        dialog.show()
//    }

    open fun startCameraIntentVideo(context: Context) {
        val takePictureIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        cameraIntentVideo.launch(takePictureIntent)
    }

    open fun openVideoGallery() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        cameraIntentVideo.launch(intent)
    }

    var cameraIntentVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val videoUri = it!!.data?.data
            if (it!!.data?.data != null) {

                val mimeType: String =
                    this.contentResolver.getType(videoUri!!)!!            //Save file to upload on server
                val file = saveVideoToAppScopeStorage(this, videoUri, mimeType)
                getVideo(file.toString(), 2)
            }
        }

    fun saveVideoToAppScopeStorage(context: Context, videoUri: Uri?, mimeType: String?): File? {
        if (videoUri == null || mimeType == null) {
            return null
        }

        val fileName = "capturedVideo${Calendar.getInstance().time.time}.mp4"

        val inputStream = context.contentResolver.openInputStream(videoUri)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM), fileName)
        file.deleteOnExit()
        file.createNewFile()
        val out = FileOutputStream(file)
        val bos = BufferedOutputStream(out)

        val buf = ByteArray(1024)
        inputStream?.read(buf)
        do {
            bos.write(buf)
        } while (inputStream?.read(buf) !== -1)

        //out.close()
        bos.close()
        inputStream.close()

        return file
    }

    abstract fun getVideo(uri: String?, i: Int)


    open fun openPdf() {
        var chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooseFileIntent.type = "application/pdf"
        chooseFileIntent.action = Intent.ACTION_GET_CONTENT
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file")
        startActivityForResult(chooseFileIntent, 3)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (data != null) {
                if (getRealPath(data.data!!, this) != null) {
                    getImage(getRealPath(data.data!!, this), data.data!!)
                } else {
                    Glide.with(this)
                        .asBitmap()
                        .load(data.data)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(object : CustomTarget<Bitmap?>() {

                            override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap?>?
                            ) {
                                val file: String = getRealPath(
                                    Uri.fromFile(saveImageToExternalStorage(resource)),
                                    baseContext
                                )!!
                                getImage(
                                    file,
                                    Uri.fromFile(saveImageToExternalStorage(resource))
                                )
                            }
                        })
                }
            }
        } else if (requestCode == 2 && resultCode == -1) {
            val file: String = getRealPath(
                Uri.fromFile(
                    saveImageToExternalStorage(
                        getFile(
                            mPicturePath,
                            this
                        )!!
                    )
                ), this
            )!!
            getImage(
                mPicturePath, Uri.fromFile(
                    saveImageToExternalStorage(
                        getFile(mPicturePath, this)!!
                    )
                )
            )

        } else if (requestCode == 3 && resultCode == -1) {
            val uri: Uri = data?.data!!
            val a = getPDFPath(uri);

// val file = File(a) // /document/document:30082
// val profReq = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
// var request_image = MultipartBody.Part.createFormData("doc", file.name, profReq)
// val pdfDocument=request_image
            getPdf(a)
        } else if (requestCode == 6) {

            val clipData = data?.clipData
            val uris = ArrayList<Uri>()
            val filePaths = ArrayList<String>()

// Extract URIs from clipData or single data
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } else {
                data?.data?.let { uris.add(it) }
            }

// Process each URI and maintain both lists
            uris.forEach { uri ->
                val realPath = getRealPath(uri, this)
                if (realPath != null) {
                    val file = File(realPath)
                    if (file.exists()) {
                        Log.d("FilePath", "File exists: $realPath")
                        filePaths.add(realPath)
                    } else {
                        Log.e("FileError", "File does not exist: $realPath")
                        // Handle missing file scenario if needed
                    }
                } else {
                    Log.e("FileError", "Unable to get file path for URI: $uri")
                    Glide.with(this)
                        .asBitmap()
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(object : CustomTarget<Bitmap?>() {

                            override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap?>?
                            ) {
                                val fileUri = Uri.fromFile(saveImageToExternalStorage(resource))
                                val filePath = getRealPath(fileUri, baseContext)!!
                                filePaths.add(filePath)
                                uris.add(fileUri)
                                Log.d("FilePath", "File exists after Glide processing: $filePath")
                            }
                        })
                }
            }

            getMultipleImage(filePaths)
        } else {
            Log.e("gndnfkfnkn", "onActivityResult: " + data?.data)
        }


    }

    abstract fun getPdf(uri: String?)
    abstract fun getImage(uri: String?, data: Uri)
    abstract fun getMultipleImage(uri: ArrayList<String>)
    fun getPDFPath(uri: Uri?): String? {
        var absolutePath = ""
        try {
            val inputStream = this!!.contentResolver.openInputStream(
                uri!!
            )
            val pdfInBytes = ByteArray(inputStream!!.available())
            inputStream.read(pdfInBytes)
            var offset = 0
            var numRead = 0
            while (offset < pdfInBytes.size && inputStream.read(
                    pdfInBytes,
                    offset,
                    pdfInBytes.size - offset
                ).also {
                    numRead = it
                } >= 0
            ) {
                offset += numRead
            }
            val returnCursor: Cursor? =
                this.contentResolver.query(uri, null, null, null, null)
            val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor?.moveToFirst()
            var mPath = ""
            mPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                this!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)
                    .toString() + "/" + returnCursor?.getString(nameIndex!!)
            } else {
                Environment.getExternalStorageDirectory()
                    .toString() + "/" + returnCursor?.getString(nameIndex!!)
            }
            val pdfFile = File(mPath)
            val op: OutputStream = FileOutputStream(pdfFile)
            op.write(pdfInBytes)
            absolutePath = pdfFile.path
        } catch (ae: Exception) {
            ae.printStackTrace()
        }
        return absolutePath
    }


    fun openMultipleGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 6)
    }

    fun openGallery() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, 1)
    }

    open fun isNougatDevice(): Boolean {
        return Build.VERSION.SDK_INT >= 24
    }

    open fun startCameraIntent(context: Context) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var f: File? = null
        try {
            val LOCAL_STORAGE_BASE_PATH_FOR_MEDIA = context.getExternalFilesDir("")
                .toString() + "/" + "" +
                    ""
            val LOCAL_STORAGE_BASE_PATH_FOR_POSTED_IMAGES: String =
                LOCAL_STORAGE_BASE_PATH_FOR_MEDIA + "/User/Images/"
            f = createImageFile()
            mPicturePath = f.absolutePath
            /* add provider in xml and
             * manifest then add following code for Nougat devices
             * to overcome file uri exposed app crash
             */if (isNougatDevice()) {
                takePictureIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri: Uri = FileProvider.getUriForFile(
                    this, "${packageName}.provider", f
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
            } else {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            f = null
            mPicturePath = null
        }
        startActivityForResult(takePictureIntent, 2)
    }

    private fun createImageFile(): File {
// Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Get the directory based on the Android version
        val storageDir: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above (Scoped Storage)
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        } else {
            // For Android 9 and below (Legacy Storage)
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        }

        // For Android 9 and below, we need to manually delete files and ensure directory exists
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storageDir?.let {
                if (it.exists() && it.isDirectory) {
                    it.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }

                // Ensure the directory exists
                if (!it.exists()) {
                    it.mkdirs()
                }
            }
        }

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private fun showAlert() {
        // we are displaying an alert dialog for permissions
        val builder = AlertDialog.Builder(this)

        // below line is the title for our alert dialog.
        builder.setTitle("Need Permissions")

        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")

        builder.setPositiveButton("GOTO SETTINGS") { dialog, which ->
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", this?.packageName, null)
                intent.data = uri
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:" + this?.packageName)
            }

            startActivity(intent)
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            // this method is called when user clicks on the negative button.
            dialog.cancel()
        }

        // below line is used to display our dialog
        builder.show()
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getRealPath(uri: Uri, context: Context): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return (Environment.getExternalStorageDirectory()
                        .toString() + "/" + split[1])
                } else {
                    val splitIndex = docId.indexOf(':', 1)
                    val tag = docId.substring(0, splitIndex)
                    val path = docId.substring(splitIndex + 1)
                    val nonPrimaryVolume =
                        getPathToNonPrimaryVolume(context, tag)
                    if (nonPrimaryVolume != null) {
                        val result = "$nonPrimaryVolume/$path"
                        val file = File(result)
                        if (file.exists() && file.canRead()) {
                            return result
                        }
                    }
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)

                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs =
                    arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getPathToNonPrimaryVolume(context: Context, tag: String): String? {
        val volumes = context.externalCacheDirs
        if (volumes != null) {
            for (volume in volumes) {
                if (volume != null) {
                    val path = volume.absolutePath
                    if (path != null) {
                        val index = path.indexOf(tag)
                        if (index != -1) {
                            return path.substring(0, index) + tag
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?, selectionArgs:
        Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(
                uri!!, projection,
                selection, selectionArgs, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    fun setUpImageFile(imageDirectory: String?): File? {
        var imageFile: File? = null
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val storageDir = File(imageDirectory)
            if (null != storageDir) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory")
                        return null
                    }
                }
            }
            imageFile = File.createTempFile(
                JPEG_FILE_PREFIX
                        + System.currentTimeMillis() + "_",
                JPEG_FILE_SUFFIX, storageDir
            )
        }
        return imageFile
    }

    fun saveImageToExternalStorage(finalBitmap: Bitmap): File? {
        val file: File
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString()
        val myDir = File("$root/Mojo")
        myDir.mkdirs()
        val fname = (JPEG_FILE_PREFIX + System.currentTimeMillis() + "_" + JPEG_FILE_SUFFIX)

        file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            return file
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return file
    }

    fun getFile(imgPath: String?, mContext: Context?): Bitmap? {
        val mOrientation: Int
        var bMapRotate: Bitmap? = null
        try {
            if (imgPath != null) {
                val exif = ExifInterface(imgPath)
                mOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imgPath, options)
                options.inSampleSize = calculateInSampleSize(options, 400, 400)
                options.inJustDecodeBounds = false
                bMapRotate = BitmapFactory.decodeFile(imgPath, options)
                when (mOrientation) {
                    6 -> {
                        val matrix = Matrix()
                        matrix.postRotate(90f)
                        bMapRotate = Bitmap.createBitmap(
                            bMapRotate, 0, 0,
                            bMapRotate.width, bMapRotate.height, matrix, true
                        )
                    }

                    8 -> {
                        val matrix = Matrix()
                        matrix.postRotate(270f)
                        bMapRotate = Bitmap.createBitmap(
                            bMapRotate, 0, 0,
                            bMapRotate.width, bMapRotate.height, matrix, true
                        )
                    }

                    3 -> {
                        val matrix = Matrix()
                        matrix.postRotate(180f)
                        bMapRotate = Bitmap.createBitmap(
                            bMapRotate, 0, 0,
                            bMapRotate.width, bMapRotate.height, matrix, true
                        )
                    }
                }
            } else {

            }
        } catch (e: OutOfMemoryError) {
            bMapRotate = null
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            bMapRotate = null
            e.printStackTrace()
        }
        return bMapRotate
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        try {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    private val registerActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                intent?.let {
                    // Get the list of URIs from the intent
                    val capturedImages: ArrayList<Uri>? = it.getParcelableArrayListExtra("CAPTUREIMAGE")

                    // Initialize list to store file paths
                    val filePaths: ArrayList<String> = arrayListOf()

                    capturedImages?.forEach { uri ->
                        // Get the file path from the URI
                        val filePath = getRealPath(uri, this)

                        // Add the file path to the list if it's not null
                        filePath?.let {
                            filePaths.add(it)
                        }
                    }

                    // Pass the list of file paths to the getMultipleImage function
                    getMultipleImage(filePaths)
                }
            }
        }


}