package com.consultantvendor.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class PermissionUtil(private val context: Context) {

    private var requestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    private var onPermissionPermanentlyDenied: (() -> Unit)? = null

    fun registerLauncher(fragment: Fragment) {
        requestPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionsResult(permissions)
        }
    }

    fun registerLauncher(activity: Activity) {
        requestPermissionLauncher = (activity as androidx.activity.ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionsResult(permissions)
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        var shouldShowDialog = false
        permissions.entries.forEach { (permission, granted) ->
            if (granted) {
                onPermissionGranted?.invoke()
            }
            else
            {
                if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)) {
                    onPermissionDenied?.invoke()
                } else {
                    shouldShowDialog = true
                }
            }
        }

        if (shouldShowDialog) {
            showSettingsDialog()
            onPermissionPermanentlyDenied?.invoke()
        }
    }

    fun checkPermissions(
        permissions: Array<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {},
        onPermanentlyDenied: () -> Unit = {}
    ) {
        onPermissionGranted = onGranted
        onPermissionDenied = onDenied
        onPermissionPermanentlyDenied = onPermanentlyDenied

        val deniedPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (deniedPermissions.isNotEmpty()) {
            requestPermissionLauncher?.launch(deniedPermissions)
        } else {
            onPermissionGranted?.invoke()
        }
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Permission Required")
        builder.setMessage("This app requires permission to function properly. Please grant the permission in the app settings.")
        builder.setPositiveButton("Go to Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}