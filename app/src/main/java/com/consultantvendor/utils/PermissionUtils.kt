package com.consultantvendor.utils

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.consultantvendor.BuildConfig
import com.consultantvendor.R

object PermissionUtils {
    /*fun showRationalDialog(context: Context, @StringRes messageResId: Int, request: PermissionRequest) {
        AlertDialog.Builder(context)
                .setPositiveButton(context.getString(R.string.allow), { _, _ -> request.proceed() })
                .setNegativeButton(context.getString(R.string.deny), { _, _ -> request.cancel() })
                .setCancelable(false)
                .setMessage(messageResId)
                .show()
    }*/

    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        (arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED))
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        (arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
    } else {
        (arrayOf(READ_EXTERNAL_STORAGE))
    }

    private const val cameraPermission = Manifest.permission.CAMERA
    val cameraAndStorageAccess = arrayOf(cameraPermission, *storagePermissions)
    val locationPermission = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    fun Fragment.hasPermissions(permissions: Array<String>) =
        requireContext().hasPermissions(permissions)

    fun Context.hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    fun Context.hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!hasPermission(permission)) {
                return false
            }
        }
        return true
    }

    fun showAppSettingsDialog(context: Context, @StringRes messageResId: Int) {
        AlertDialog.Builder(context)
            .setPositiveButton(context.getString(R.string.settings)) { _, _ ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", context.packageName, null)

                if (context.packageName.equals(BuildConfig.APPLICATION_ID))
                    context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, which -> dialog?.dismiss() }
            .setCancelable(false)
            .setMessage(messageResId)
            .show()
    }
}