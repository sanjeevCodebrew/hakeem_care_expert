package com.consultantvendor.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.appFeatures
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.Config
import com.consultantvendor.ui.loginSignUp.login.LoginActivity
import com.consultantvendor.ui.webview.WebViewActivity
import com.consultantvendor.utils.DateUtils.dateFormatForBackend
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.iosParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.ktx.Firebase
//import com.stfalcon.imageviewer.StfalconImageViewer
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.models.sort.SortingTypes
import id.zelory.compressor.Compressor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.File
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import timber.log.Timber

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.hideShowView(listIsEmpty: Boolean) {
    visibility = if (listIsEmpty) View.VISIBLE
    else View.GONE
}

fun getRequestBody(string: String?): RequestBody {
    return RequestBody.create("text/plain".toMediaTypeOrNull(), string ?: "")
}

fun View.showSnackBar(msg: String) {
    try {
        val snackBar = Snackbar.make(this, msg, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        val textView =
//            snackBarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
//        textView.maxLines = 3
        snackBar.setAction(R.string.ok) { snackBar.dismiss() }
        snackBarView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
        snackBar.setActionTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        snackBar.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun logoutUser(activity: Activity?, prefsManager: PrefsManager) {

    Timber.d("clearData")

    val notificationManager =
        activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancelAll()


    prefsManager.remove(USER_DATA)


    activity.setResult(Activity.RESULT_CANCELED)
    ActivityCompat.finishAffinity(activity)

//    activity.startActivity(Intent(activity, SignUpActivity::class.java)
//            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
    activity.startActivity(
        Intent(activity, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )

}


/*Digits should be 0,1,2,3*/
fun getCountFormat(digits: Int, count: Int?): String {
    when (digits) {
        1 -> return if (count ?: 0 <= 9)
            count.toString()
        else
            String.format(Locale.ENGLISH, "%d+", 9)

        2 -> return if (count ?: 0 <= 99)
            count.toString()
        else
            String.format(Locale.ENGLISH, "%d+", 99)

        3 -> return if (count ?: 0 <= 999)
            count.toString()
        else
            String.format(Locale.ENGLISH, "%d+", 999)

        else -> return if (count ?: 0 <= 9999)
            count.toString()
        else
            String.format(Locale.ENGLISH, "%d+", 9999)

    }
}


fun Context.longToast(text: CharSequence) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun addFragment(fragmentManager: FragmentManager?, fragment: Fragment, id: Int) {
    fragmentManager?.beginTransaction()
        ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
        ?.add(id, fragment)?.commit()
}

fun addFragmentAnim(fragmentManager: FragmentManager?, fragment: Fragment, id: Int) {
    fragmentManager?.beginTransaction()
        ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        ?.add(id, fragment)?.commit()
}

fun addFragmentToBackStack(fragmentManager: FragmentManager?, fragment: Fragment, id: Int) {
    fragmentManager?.beginTransaction()?.setCustomAnimations(0, 0, 0, 0)
        ?.add(id, fragment)?.addToBackStack("")?.commit()
}

fun replaceFragmentNoBackStack(fragmentManager: FragmentManager?, fragment: Fragment, id: Int) {
    fragmentManager?.beginTransaction()?.replace(id, fragment)?.commit()
}

fun replaceFragment(fragmentManager: FragmentManager?, fragment: Fragment, id: Int) {
    fragmentManager?.beginTransaction()?.replace(id, fragment, fragment::class.simpleName)
        ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
        ?.addToBackStack(null)?.commit()
}


fun replaceResultFragment(
    fragment: Fragment,
    targetFragment: Fragment,
    container: Int,
    requestCode: Int
) {
    val ft = fragment.requireActivity().supportFragmentManager.beginTransaction()
    targetFragment.setTargetFragment(fragment, requestCode)
    ft.addToBackStack("")
    ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
    ft.replace(container, targetFragment, fragment.tag)
    ft.commit()
}

fun resultFragmentIntent(
    fragment: Fragment,
    fragmentTarget: Fragment,
    requestCode: Int,
    intent: Intent?
) {
    val intentMain = intent ?: Intent(fragment.requireContext(), fragmentTarget::class.java)
    fragmentTarget.onActivityResult(requestCode, RESULT_OK, intentMain)
    fragment.activity?.supportFragmentManager?.popBackStack()
}


fun resultFragmentIntentNoPop(
    fragment: Fragment,
    fragmentTarget: Fragment,
    requestCode: Int,
    intent: Intent?
) {
    val intentMain = intent ?: Intent(fragment.requireContext(), fragmentTarget::class.java)
    fragmentTarget.onActivityResult(requestCode, RESULT_OK, intentMain)
}


fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInputFromWindow(
        applicationWindowToken,
        InputMethodManager.SHOW_FORCED, 0
    )
}

fun getAge(date: String?): String {
    if (date == null)
        return "NA"
    else {
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()

        val dateChange = dateFormatForBackend(DateFormat.DATE_FORMAT, DateFormat.MON_DATE_YEAR, date)
        val formatter = SimpleDateFormat(DateFormat.MON_DATE_YEAR, Locale.ENGLISH)
        try {
            dob.time = formatter.parse(dateChange)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        val ageInt = age

        return ageInt.toString()
    }
}

fun getAgeSelected(date: String?): Int {
    if (date == null)
        return 0
    else {
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()

        val formatter = SimpleDateFormat(DateFormat.MON_DATE_YEAR, Locale.ENGLISH)
        try {
            dob.time = formatter.parse(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        val ageInt = age

        return ageInt
    }
}


val requestOptions = RequestOptions()
    .dontAnimate()
    .dontTransform()

fun loadImage(ivImage: ImageView, image: String?, placeholder: Int) {
    val imageLink = getImageBaseUrl(ImageFolder.UPLOADS, image)
    val imageThumbnail = getImageBaseUrl(ImageFolder.THUMBS, image)

    val glide = Glide.with(ivImage.context)

    glide.load(imageLink)
        .apply(requestOptions)
        .placeholder(placeholder)
        .thumbnail(glide.load(imageThumbnail))
        .into(ivImage)
}

fun getImageBaseUrl(folderType: String, image: String?): String {
    return "${appClientDetails.media_url}$folderType$image"
}

fun pxFromDp(context: Context, dp: Float): Float {
    return dp * context.resources.displayMetrics.density
}

fun getVersion(activity: Activity): PackageInfo {
    return activity.packageManager.getPackageInfo(activity.packageName, 0)
}

fun disableButton(btn: View?) {
    btn?.isEnabled = false

    Handler().postDelayed({
        btn?.isEnabled = true
    }, 1500)// set time as per your requirement
}

fun getDoctorName(userData: UserData?): String {
    return if (userData?.profile?.title.isNullOrEmpty())
        userData?.name ?: ""
    else
        "${userData?.profile?.title ?: ""} ${userData?.name}"
}

fun getCurrency(amount: String?): String {
    val format = NumberFormat.getCurrencyInstance(Locale.ENGLISH)
    format.maximumFractionDigits = 2
    try {
        format.currency = Currency.getInstance(appClientDetails.currency)
    } catch (_: Exception) {

    }

    return if (amount.isNullOrEmpty())
        format.format(0).replace("0.00", " NA")
    else {
        format.format(amount.toDouble()).replace(".00", "")
    }
}

fun getCurrencySymbol(): String {
    val format = NumberFormat.getCurrencyInstance()
    try {
        format.currency = Currency.getInstance(appClientDetails.currency)
    } catch (_: Exception) {

    }

    return format.currency.symbol
}

fun getUnitPrice(unit: Double?, context: Context): String {
    return when {
        unit == null -> "NA"
        unit >= 3600 -> "${(unit / 3600)} ${context.getString(R.string.hr)}"
        else -> "${(unit / 60)} ${context.getString(R.string.min)}"
    }
}

@SuppressLint("ClickableViewAccessibility")
fun editTextScroll(editText: EditText) {
    editText.setOnTouchListener { v, event ->
        v.parent.requestDisallowInterceptTouchEvent(true)
        if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            v.parent.requestDisallowInterceptTouchEvent(false)
        }
        false
    }
}

fun getUserRating(rating: String?): String {
    val ratingNew = rating ?: "0.0"

    return String.format(Locale.ENGLISH, "%.1f", ratingNew.toFloat())
}

fun compressImage(activity: Activity?, actualImageFile: File?): File {
    Timber.e(actualImageFile?.length().toString())

    /*mb approximate*/
    val resultFile: File? = when {
        actualImageFile?.length() ?: 0 < (1 * 1024 * 1024) -> actualImageFile
        actualImageFile?.length() ?: 0 < (3 * 1024 * 1024) -> {
            Compressor(activity)
                .setQuality(70)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .compressToFile(actualImageFile)
        }

        else -> {
            Compressor(activity)
                .setQuality(50)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .compressToFile(actualImageFile)
        }
    }

    Timber.e(resultFile?.length().toString())

    return resultFile ?: File("")
}

//fun viewImageFull(activity: Activity, itemsImage: ArrayList<String>, pos: Int) {
//
//    /*val hierarchyBuilder = GenericDraweeHierarchyBuilder
//        .newInstance(activity.resources)
//        .setFailureImage(R.drawable.image_placeholder)
//        .setProgressBarImage(R.drawable.image_placeholder)
//        .setPlaceholderImage(R.drawable.image_placeholder)
//
//    ImageViewer.Builder(activity, itemsImage)
//        .setStartPosition(pos)
//        .hideStatusBar(false)
//        .setCustomDraweeHierarchyBuilder(hierarchyBuilder)
//        .show()*/
//
////    StfalconImageViewer.Builder(activity, itemsImage) { view, image ->
////        Glide.with(view.context).load(image).into(view)
////    }.show()
//}

fun viewImageFull(activity: Activity, itemsImage: ArrayList<String>, pos: Int) {
    val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    dialog.setContentView(R.layout.dialog_image)

    val photoView = dialog.findViewById<PhotoView>(R.id.photoView)

    Glide.with(activity)
        .load(itemsImage[pos])
        .into(photoView)

    dialog.show()
}

fun placePicker(fragment: Fragment?, activityMain: Activity) {
    val activity: Activity = if (fragment != null)
        fragment.activity as Activity
    else
        activityMain

    val fields =
        listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
        .build(activity)

    if (activityMain.packageName.equals(BuildConfig.APPLICATION_ID)) {
        if (fragment == null)
            activity.startActivityForResult(intent, AppRequestCode.AUTOCOMPLETE_REQUEST_CODE)
        else
            fragment.startActivityForResult(intent, AppRequestCode.AUTOCOMPLETE_REQUEST_CODE)
    }
}

fun getPathUri(context: Context, uri: Uri): String? {
    val projection =
        arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(uri, projection, null, null, null)
        ?: return ""
    val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
    cursor.moveToFirst()
    val result = cursor.getString(column_index)
    cursor.close()
    return result
}

fun setAcceptTerms(activity: Activity): SpannableString {
    val termText = if (BuildConfig.FLAVOR == "taradoc") {
        activity.getString(R.string.terms2)
    } else
        activity.getString(R.string.terms)

    val term = String.format(
        "%s%s %s %s", activity.getString(R.string.you_agree_to_our_terms),
        termText, activity.getString(R.string.and), activity.getString(R.string.privacy)
    )

    val string = SpannableString.valueOf(term)
    /*Color*/
    string.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(activity, R.color.colorPrimary)),
        term.indexOf(termText), term.indexOf(" " + activity.getString(R.string.and) + " "), 0
    )
    string.setSpan(
        ForegroundColorSpan(ContextCompat.getColor(activity, R.color.colorPrimary)),
        term.indexOf(activity.getString(R.string.privacy)), term.length, 0
    )
    /*Bold*/
//    string.setSpan(StyleSpan(Typeface.BOLD), term.indexOf(termText),
//            term.indexOf(" " + activity.getString(R.string.and) + " "), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//    string.setSpan(StyleSpan(Typeface.BOLD), term.indexOf(activity.getString(R.string.privacy)),
//            term.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    /*Click*/
    string.setSpan(
        Terms(), term.indexOf(termText),
        term.indexOf(" " + activity.getString(R.string.and) + " "), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    string.setSpan(
        Privacy(), term.indexOf(activity.getString(R.string.privacy)),
        term.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    return string
}

fun openPageLink(context: Context, name: String?, link: String?, app_type: String?) {
    val intentLink = Intent(context, WebViewActivity::class.java)
        .putExtra(WebViewActivity.LINK_TITLE, name)

    if (app_type != null)
        intentLink.putExtra(WebViewActivity.LINK_URL, "$link?app_type=$app_type")
    else
        intentLink.putExtra(WebViewActivity.LINK_URL, link)

    context.startActivity(intentLink)
}

class Privacy : ClickableSpan() {

    override fun onClick(tv: View) {
        //tv.context.longToast("Clicked")
        appClientDetails.pages?.forEach {
            if (it.slug == PageLink.PRIVACY_POLICY) {
                openPageLink(
                    tv.context, tv.context.getString(R.string.privacy),
                    it.slug, it.app_type
                )
            }
        }
    }

    override fun updateDrawState(ds: TextPaint) {// override updateDrawState
        ds.isUnderlineText = false // set to false to remove underline
    }
}

class Terms : ClickableSpan() {

    override fun onClick(tv: View) {
        //viewPage(PageLink.TERMS)
        appClientDetails.pages?.forEach {
            if (it.slug == PageLink.TERMS_CONDITIONS) {
                if (BuildConfig.FLAVOR == "taradoc")
                    openPageLink(
                        tv.context, tv.context.getString(R.string.terms2),
                        it.slug, it.app_type
                    )
                else
                    openPageLink(
                        tv.context, tv.context.getString(R.string.terms_and_conditions),
                        it.slug, it.app_type
                    )
            }
        }
    }

    override fun updateDrawState(ds: TextPaint) {// override updateDrawState
        ds.isUnderlineText = false // set to false to remove underline
    }
}


fun getAddress(place: Place): String {
    val finalAddress: String
    val name = place.name.toString()
    val placeAddress = place.address.toString()

    if (place.address?.contains(name) == true) {
        finalAddress = placeAddress
    } else {
        finalAddress = "$name, $placeAddress"
    }
    return finalAddress
}

fun getCity(place: Place, context: Context): String {
    var finalAddress: String = ""

    try {

        val geocoder = Geocoder(context, Locale.getDefault())
        var addresses: List<Address> = ArrayList()
        addresses = geocoder.getFromLocation(place.latLng!!.latitude, place.latLng!!.longitude, 1) as List<Address>
        finalAddress = addresses[0].locality

    } catch (e: Exception) {

    }
    return finalAddress
}


fun slideRecyclerItem(viewToAnimate: View, context: Context) {
    val animation: Animation = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom)
    viewToAnimate.startAnimation(animation)
}

/*Share*/
fun shareDeepLink(deepLink: String, activity: Activity, userData: UserData?) {
    val progressDialog = ProgressDialog(activity)
    progressDialog.setLoading(true)

    val longLink = "${Config.baseURL}${"https://hakeemcare.page.link/FgzB"}"
    Timber.e("deeplincheck: " + longLink)

    val shortLinkTask = Firebase.dynamicLinks.shortLinkAsync {
        link = Uri.parse(longLink)
        domainUriPrefix = "https://${activity.getString(R.string.deep_link_url)}"
        // Open links with this app on Android
        androidParameters(BuildConfig.APPLICATION_ID) { }
        // Open links with com.example.ios on iOS
        iosParameters(activity.getString(R.string.deep_link_ios_bundle)) { }

        socialMetaTagParameters {
            title = activity.getString(R.string.app_name)
            description = activity.getString(R.string.invite_text)
            imageUrl = Uri.parse(getImageBaseUrl(ImageFolder.UPLOADS, appClientDetails.applogo))
        }
    }.addOnSuccessListener { result ->
        progressDialog.setLoading(false)

        // Short link created
        val shortLink = result.shortLink

        /*Share Intent*/
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share))

        val completeMsg = if (appFeatures.needInviteCode)
            "${activity.getString(R.string.app_name)}\n$shortLink\n" +
                    "${activity.getString(R.string.use_code, userData?.reference_code)}"
        else
            "${activity.getString(R.string.app_name)}\n$shortLink"

        shareIntent.putExtra(Intent.EXTRA_TEXT, completeMsg)

        shareIntent.type = "text/plain"
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (activity.packageName.equals(BuildConfig.APPLICATION_ID))
            activity.startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.share)))

    }.addOnFailureListener {
        // Error
        //ivShare.showSnackBar(getString(R.string.error))
        progressDialog.setLoading(false)
    }
}


@SuppressLint("StringFormatInvalid")
fun shareDeepLink1(deepLink: String, activity: Activity, userData: UserData?) {
    val progressDialog = ProgressDialog(activity)
    progressDialog.setLoading(true)


    var longLink = ""
    var titleM = ""
    var descriptionM = ""
    var imageUrlM = Uri.parse("")
    when (deepLink) {
        DeepLink.USER_PROFILE -> {
            longLink = "${Config.baseURL}${deepLink}?id=${userData?.id}"

            titleM = "${userData?.categoryData?.name} | ${userData?.name}"
            descriptionM = userData?.profile?.bio ?: ""
            imageUrlM = if (userData?.profile_image == null)
                Uri.parse(getImageBaseUrl(ImageFolder.UPLOADS, appClientDetails.applogo ?: ""))
            else
                Uri.parse(getImageBaseUrl(ImageFolder.UPLOADS, userData.profile_image ?: ""))
        }

        DeepLink.INVITE -> {

//            longLink = "${Config.baseURL}${deepLink}"
            longLink = "https://hakeemcare.page.link/FgzB"

            Timber.e("shareDeepLink: " + longLink)

            titleM = activity.getString(R.string.app_name)
            descriptionM = activity.getString(R.string.invite_text)
            imageUrlM =
                Uri.parse(getImageBaseUrl(ImageFolder.UPLOADS, appClientDetails.applogo ?: ""))
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share))

            val completeMsg = if (appFeatures.needInviteCode)
                "${activity.getString(R.string.app_name)}\n$longLink\n" +
                        "${activity.getString(R.string.use_code, userData?.reference_code)}"
            else
                "${activity.getString(R.string.app_name)}\n$longLink"

            shareIntent.putExtra(Intent.EXTRA_TEXT, completeMsg)

            shareIntent.type = "text/plain"
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (activity.packageName.equals(BuildConfig.APPLICATION_ID))
                activity.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        activity.getString(R.string.share)
                    )
                )

        }
    }

    val shortLinkTask = Firebase.dynamicLinks.shortLinkAsync {
        link = Uri.parse(longLink)
        domainUriPrefix = "https://${activity.getString(R.string.deep_link_url)}"
        // Open links with this app on Android
        androidParameters(BuildConfig.APPLICATION_ID) { }
        // Open links with com.example.ios on iOS
        iosParameters(activity.getString(R.string.deep_link_ios_bundle)) { }

        socialMetaTagParameters {
            title = titleM
            description = descriptionM
            imageUrl = imageUrlM
        }
    }.addOnSuccessListener { result ->
        progressDialog.setLoading(false)

        // Short link created
        val shortLink = result.shortLink

        /*Share Intent*/
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share))

        val completeMsg = if (appFeatures.needInviteCode)
            "${activity.getString(R.string.app_name)}\n$shortLink\n${
                activity.getString(
                    R.string.use_code,
                    userData?.reference_code
                )
            }"
        else
            "${activity.getString(R.string.app_name)}\n$shortLink"

        shareIntent.putExtra(Intent.EXTRA_TEXT, completeMsg)

        shareIntent.type = "text/plain"
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (activity.packageName.equals(BuildConfig.APPLICATION_ID))
            activity.startActivity(
                Intent.createChooser(
                    shareIntent,
                    activity.getString(R.string.share)
                )
            )

    }.addOnFailureListener {
        // Error
        //ivShare.showSnackBar(getString(R.string.error))
        progressDialog.setLoading(false)
    }
}


fun askForOption(fragment: Fragment?, activity: Activity, view: View) {
    val context: Context = fragment?.requireContext() ?: activity

    val popup = PopupMenu(context, view)
    popup.menuInflater.inflate(R.menu.menu_attach_new, popup.menu)

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.item_image -> {
                selectImages(fragment, activity)
            }

            R.id.item_pdf -> {
                selectDocument(fragment, activity)
            }
        }
        true
    }

    popup.show()
}

fun openCamera(activity: Activity, fragment: Fragment?) {
    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    activity.startActivityForResult(cameraIntent, AppRequestCode.CAMERA)
}

fun selectImages(fragment: Fragment?, activity: Activity) {
    val filePickerBuilder = FilePickerBuilder.instance
        .setMaxCount(1)
        .setActivityTheme(R.style.LibAppTheme)
        .setActivityTitle(activity.getString(R.string.select_image))
        .enableVideoPicker(false)
        .enableCameraSupport(false)
        .showGifs(false)
        .showFolderView(true)
        .enableSelectAll(false)
        .enableImagePicker(true)
        .setCameraPlaceholder(R.drawable.ic_camera)
        .withOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

    if (fragment != null)
        filePickerBuilder.pickPhoto(fragment, AppRequestCode.IMAGE_PICKER)
    else
        filePickerBuilder.pickPhoto(activity, AppRequestCode.IMAGE_PICKER)
}

fun selectDocument(fragment: Fragment?, activity: Activity) {
    val pdfs = arrayOf("pdf")

    val filePickerBuilder = FilePickerBuilder.Companion.instance
        .setMaxCount(1)
        .setActivityTheme(R.style.LibAppTheme)
        .sortDocumentsBy(SortingTypes.NAME)
        .setActivityTitle(activity.getString(R.string.select_document))
        .addFileSupport(activity.getString(R.string.pdf), pdfs, R.drawable.ic_pdf)
        .showFolderView(true)
        .enableDocSupport(false)
        .withOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)

    if (fragment != null)
        filePickerBuilder.pickFile(fragment, AppRequestCode.DOC_PICKER)
    else
        filePickerBuilder.pickFile(activity, AppRequestCode.DOC_PICKER)
}

fun mapIntent(activity: Activity, name: String, lat: Double, lng: Double) {
    try {
        val url = "http://maps.google.com/maps?daddr=$lat,$lng($name)"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.maps")

        if (activity.packageName.equals(BuildConfig.APPLICATION_ID))
            activity.startActivity(intent)
    } catch (e: Exception) {

        activity.longToast(activity.getString(R.string.map_not_found))
    }
}


fun getWaterLiters(activity: Activity, value: Int, unitNeeded: Boolean): String {
    val valueInLiters = String.format(Locale.ENGLISH, "%.3f", value / 1000.0)

    return if (unitNeeded) {
        if (value < 1000)
            "$value ${activity.getString(R.string.ml)}"
        else
            "${valueInLiters.removeSuffix(".000")} ${activity.getString(R.string.lt)}"
    } else valueInLiters.removeSuffix(".000")
}

fun getWaterUnit(activity: Activity, value: Double, unitNeeded: Boolean): String {
    val valueInLiters = String.format(Locale.ENGLISH, "%.3f", value)

    return if (unitNeeded) {
        if (value < 1)
            "$value ${activity.getString(R.string.ml)}"
        else
            "${valueInLiters.removeSuffix(".000")} ${activity.getString(R.string.lt)}"
    } else valueInLiters.removeSuffix(".000")
}

fun getProteinUnit(activity: Activity, value: Int, unitNeeded: Boolean): String {
    val valueUnit = String.format(Locale.ENGLISH, "%.2f", value / 1000.0)

    return if (unitNeeded) {
        if (value < 1000)
            "$value ${activity.getString(R.string.g)}"
        else
            "${valueUnit.removeSuffix(".000")} ${activity.getString(R.string.kg)}"
    } else valueUnit.removeSuffix(".000")
}


fun openPdf(activity: Activity, link: String, prescription: Boolean = false,isReport: Boolean=false) {
    Timber.e(link)
    if (prescription) {
        /*activity.startActivity(
            Intent(activity, WebViewActivity::class.java)
                .putExtra(
                    WebViewActivity.LINK_TITLE,
                    activity.getString(R.string.prescription_details)
                )
                .putExtra(WebViewActivity.PDF_LINK, link)
        )*/
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(link.toUri(), "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        activity.startActivity(intent)
    }
    else if (isReport){
        activity.startActivity(
            Intent(activity, WebViewActivity::class.java)
                .putExtra(
                    WebViewActivity.LINK_TITLE,
                    activity.getString(R.string.report_details)
                )
                .putExtra(WebViewActivity.PDF_LINK, link)
        )
    }
    else {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        if (activity.packageName.equals(BuildConfig.APPLICATION_ID))
            activity.startActivity(browserIntent)
    }
}

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

fun downloadFile(activity: Activity, url: String, imageRequestId: String? = null) {
    try {
        val requestId = imageRequestId ?: Uri.parse(url).getQueryParameter("request_id")

        // Create request for android download manager
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUrl = if (imageRequestId != null) url else "$url&download"
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
        val fileName = "${activity.getString(R.string.app_name)}_$requestId.pdf"
        request.setTitle(fileName)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setVisibleInDownloadsUi(true)

        downloadManager.enqueue(request)
        activity.longToast(activity.getString(R.string.downloading))
    } catch (e: java.lang.Exception) {
    }
}

fun AppCompatActivity.applyInsets(view: View, isLightStatusBar: Boolean = true) {
    // Set status bar color to colorPrimary
    window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)

    // Handle system bar insets (status + nav bars)
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        ViewCompat.onApplyWindowInsets(view, WindowInsetsCompat.CONSUMED)// don't consume so child views can handle insets too
    }

    // Set light/dark status bar icons
    WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = isLightStatusBar
}
