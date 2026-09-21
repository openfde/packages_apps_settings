/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.spa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.spa.app.appinfo.AppInfoSettingsProvider
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin
import com.android.settingslib.spa.framework.BrowseActivity
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.util.SESSION_BROWSE
import com.android.settingslib.spa.framework.util.appendSpaParams
import com.google.android.setupcompat.util.WizardManagerHelper

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.graphics.Rect;
import androidx.core.view.OnApplyWindowInsetsListener;
import android.view.View;

class SpaActivity : BrowseActivity() {
    override fun isPageEnabled(page: SettingsPage) =
        super.isPageEnabled(page) && !isSuwAndPageBlocked(page.sppName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(HideNonSystemOverlayMixin(this))
        setupActionBarInTitleBar()
    }

    companion object {
        private const val TAG = "SpaActivity"

        /** The pages that blocked from SUW. */
        private val SuwBlockedPages = setOf(AppInfoSettingsProvider.name)

        @VisibleForTesting
        fun Context.isSuwAndPageBlocked(name: String): Boolean =
            if (name in SuwBlockedPages && !WizardManagerHelper.isDeviceProvisioned(this)) {
                Log.w(TAG, "$name blocked before SUW completed.")
                true
            } else {
                false
            }

        @[JvmStatic JvmOverloads]
        fun Context.startSpaActivity(destination: String, highlightItemKey: String? = null) =
            startActivity(getSpaActivityIntent(destination, highlightItemKey))

        fun Context.getSpaActivityIntent(destination: String, highlightItemKey: String? = null) =
            Intent(this, SpaActivity::class.java)
                .appendSpaParams(
                    destination = destination,
                    highlightItemKey = highlightItemKey,
                    sessionName = SESSION_BROWSE,
                )
    }


    private fun setupActionBarInTitleBar() {
    val controller = window.insetsController
    if (controller != null) {
        controller.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
        )
    }

    val content = findViewById<View>(android.R.id.content) ?: return

    ViewCompat.setOnApplyWindowInsetsListener(content) { v, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
        )

        val caption = windowInsets.getInsets(
            WindowInsetsCompat.Type.captionBar()
        )

        // With a caption bar present keep the action bar at the very top
        // so it shows up in the title bar; otherwise keep the regular status bar padding.
        val top = if (caption.top > 0) 0 else bars.top

        v.setPadding(
            bars.left,
            top,
            bars.right,
            bars.bottom
        )

        WindowInsetsCompat.CONSUMED
    }
}
}
