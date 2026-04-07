package com.example.communityapp.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager

object StatusBarUtils {
    fun applyBeige(activity: Activity) {
        val window = activity.window
        val beigeColor = Color.parseColor("#C9B88A")

        // These two flags MUST be set or Android ignores statusBarColor entirely:
        // FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = "I will draw the status bar background"
        // Clearing FLAG_TRANSLUCENT_STATUS removes the translucent overlay that overrides color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        window.statusBarColor = beigeColor

        // Dark icons on the light beige background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}