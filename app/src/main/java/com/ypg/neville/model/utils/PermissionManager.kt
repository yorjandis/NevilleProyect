package com.ypg.neville.model.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        @JvmStatic
        fun checkPermissionREAD_EXTERNAL_STORAGE(context: Context): Boolean {
            val currentAPIVersion = Build.VERSION.SDK_INT
            return if (currentAPIVersion >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // showDialog("External storage", context, Manifest.permission.READ_EXTERNAL_STORAGE);
                    } else {
                        ActivityCompat.requestPermissions(
                            context,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            101
                        )
                    }
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }
    }
}
