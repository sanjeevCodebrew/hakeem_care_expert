package com.consultantvendor.utils

import android.Manifest
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
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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

    var is_video = false
    private val JPEG_FILE_PREFIX = "IMG_"
    private val JPEG_FILE_SUFFIX = ".jpg"


    var mPicturePath: String? = null
    var mIsGallery: Boolean? = null
    var bitmap: Bitmap? = null
    var reqcode = 0
    var triescamera = 0
    var triesgallary = 0


    @RequiresApi(Build.VERSION_CODES.M)
    fun showImageDialog(b: Boolean,isCamera:Boolean,isPdf:Boolean) {
        is_video = b


        val dialog: Dialog = Dialog(requireActivity())
        var view = ItemDialogImageBinding.inflate(layoutInflater)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(view.root)

        if (!isCamera){
            view.tvCamera.gone()
        }


        view.tvGallery.setOnClickListener {
            mIsGallery = true
            if (!is_video) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    checkconditongallary()

                } else {

                    checkgallry()


                }
            } else {
                openVideoGallery()
            }

            dialog.dismiss()
        }

        view.ivClose.setOnClickListener { dialog.dismiss() }

        view.tvCamera.setOnClickListener {
            mIsGallery = false

            if (!is_video) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    checkconditioncamera()

                } else {

                    checkcamera()

                }
            } else {
                startCameraIntentVideo(requireActivity())
            }



            dialog.dismiss()
        }
        dialog.show()
    }

//    fun showImageDialogForPdfAndVideo() {
//
//
//        val dialog: Dialog = Dialog(requireActivity())
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
                    requireActivity().contentResolver.getType(videoUri!!)!!            //Save file to upload on server
                val file = saveVideoToAppScopeStorage(requireActivity(), videoUri, mimeType)
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


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkcamera() {
//


        reqcode = 2
        requestPermissionsFOR(Manifest.permission.CAMERA)

//            if (checkForCameraPermission()) {
//                startCameraIntent(requireActivity())
//
////                if (checkForWritePermission()) {
////                    startCameraIntent(requireActivity())
////                }else{
////                    setting()
////
////                }
//            }else{
//                setting()
//            }
    }

    private fun setting() {
        val ifFailed = {
            Log.e("tries", "checkconditioncamera: " + triescamera)

            if (triescamera >= 2) {
                showAlert()


            }

        }
        ifFailed.invoke()

    }

    private fun settinggallary() {
        val ifFailed = {
            Log.e("tries", "checkconditioncamera: " + triesgallary)

            if (triesgallary >= 2) {
                showAlert()


            }

        }
        ifFailed.invoke()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkgallry() {


        reqcode = 1

        requestPermissionsFOR(Manifest.permission.READ_EXTERNAL_STORAGE)

//            if (checkForReadPermission()) {
//
//                    openGallery()
//
//            }else{
//                settinggallary()
//            }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkconditioncamera() {

        reqcode = 4

        requestPermissionsFOR(Manifest.permission.CAMERA)

//            if (checkForCameraPermission()) {
//                startCameraIntent(requireActivity())
//
//            }else{
//                setting()
//            }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkconditongallary() {


        reqcode = 3

        requestPermissionsFOR(Manifest.permission.READ_MEDIA_IMAGES)


//            if (checkForReadPermission33()) {
//                openGallery()
//
//            }else{
//                settinggallary()
//            }

    }


    open fun openPdf() {
        var chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooseFileIntent.type = "application/pdf"
        chooseFileIntent.action = Intent.ACTION_GET_CONTENT
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file")
        startActivityForResult(chooseFileIntent, 3)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);



        when (requestCode) {
            1 -> {

                if (triesgallary < 2) {
                    triesgallary++

                    if (ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkgallry()
                        }
                    }


//                        prefManager.settriesgallary(triesgallary)

                }
            }


            2 -> {
                if (triescamera < 2) {
                    triescamera++

                    if (ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkcamera()
                        }
                    }

//                        prefManager.settriescamera(triescamera)

                }

            }

            3 -> {


                if (triesgallary < 2) {
                    triesgallary++

                    if (ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            checkconditongallary()
                        }
                    }

//                            prefManager.settriesgallary(triesgallary)

                }


            }

            4 -> {
                if (triescamera < 2) {
                    triescamera++

                    if (ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            checkconditongallary()
                        }
                    }
//                        prefManager.settriescamera(triescamera)

                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkForReadPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                reqcode
            )
            false
        } else {
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkForReadPermission33(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                reqcode
            )
            false
        } else {
            true
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun checkForWritePermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                reqcode
            )
            false
        } else {
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkForCameraPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(arrayOf(Manifest.permission.CAMERA), reqcode)
            false
        } else {
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (data != null) {
                if (getRealPath(data.data!!, requireActivity()) != null) {
                    getImage(getRealPath(data.data!!, requireActivity()), data.data!!)
                } else {
                    Glide.with(this)
                        .asBitmap()
                        .load(data.data)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(object : CustomTarget<Bitmap?>() {

                            override fun onLoadCleared(@Nullable placeholder: Drawable?) {}
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap?>?
                            ) {
                                val file: String = getRealPath(
                                    Uri.fromFile(saveImageToExternalStorage(resource)),
                                    requireActivity()
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
            try{
                val file: String = getRealPath(
                    Uri.fromFile(
                        saveImageToExternalStorage(
                            getFile(mPicturePath, requireActivity())!!

                        )
                    ), requireActivity()
                )!!
                getImage(
                    file, Uri.fromFile(
                        saveImageToExternalStorage(
                            getFile(mPicturePath, requireActivity())!!
                        )
                    )
                )
            }catch (ex: Exception){
                Toast.makeText(requireContext(),"This functionality is under development", Toast.LENGTH_LONG).show()
            }


        } else if (requestCode == 3 && resultCode == -1) {
            val uri: Uri = data?.data!!
            val a = getPDFPath(uri);

// val file = File(a) // /document/document:30082
// val profReq = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
// var request_image = MultipartBody.Part.createFormData("doc", file.name, profReq)
// val pdfDocument=request_image
            getPdf(a)
        } else {
            Log.e("gndnfkfnkn", "onActivityResult: " + data?.data)
        }


    }

    abstract fun getPdf(uri: String?)
    abstract fun getImage(uri: String?, data: Uri)
    fun getPDFPath(uri: Uri?): String? {
        var absolutePath = ""
        try {
            val inputStream = requireActivity()!!.contentResolver.openInputStream(
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
                requireActivity().contentResolver.query(uri, null, null, null, null)
            val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor?.moveToFirst()
            var mPath = ""
            mPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                requireActivity()!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)
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
                .toString() + "/" + "consultantapp"
            val LOCAL_STORAGE_BASE_PATH_FOR_POSTED_IMAGES: String =
                LOCAL_STORAGE_BASE_PATH_FOR_MEDIA + "/User/Images/"
            f = setUpImageFile(LOCAL_STORAGE_BASE_PATH_FOR_POSTED_IMAGES)
            mPicturePath = f!!.absolutePath
            /* add provider in xml and
             * manifest then add following code for Nougat devices
             * to overcome file uri exposed app crash
             */if (isNougatDevice()) {
                takePictureIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri: Uri = FileProvider.getUriForFile(
                    requireActivity(), "com.mitravouser.provider", f
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

    private fun showAlert() {
        // we are displaying an alert dialog for permissions
        val builder = AlertDialog.Builder(requireContext())

        // below line is the title for our alert dialog.
        builder.setTitle("Need Permissions")

        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")

        builder.setPositiveButton("GOTO SETTINGS") { dialog, which ->
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", activity?.packageName, null)
                intent.data = uri
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:" + activity?.packageName)
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
                    Long.valueOf(id)

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


    private fun requestPermissionsFOR(readMediaImages: String) {
        Dexter.withContext(requireContext())
            .withPermission(readMediaImages)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
//                    Toast.makeText(requireContext(), "Camera permission granted", Toast.LENGTH_SHORT).show()
//                    // Reset the denial count
//                    // Proceed with camera access


                    when (reqcode) {
                        1 -> {

                            openGallery()
                        }

                        2 -> {
                            startCameraIntent(requireActivity())
                        }

                        3 -> {

                            openGallery()

                        }

                        4 -> {
                            startCameraIntent(requireActivity())

                        }
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        showAlert()
                    } else {
                        Toast.makeText(requireContext(), "permission denied", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    Toast.makeText(
                        requireContext(),
                        "permission is needed to use this feature",
                        Toast.LENGTH_SHORT
                    ).show()
                    token?.continuePermissionRequest()
                }
            }).check()
    }
}



