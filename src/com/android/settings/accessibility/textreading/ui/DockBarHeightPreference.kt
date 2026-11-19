/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.textreading.ui

import android.Manifest
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_BUTTON_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_SLIDER_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.MIN_COMMIT_DELAY
import com.android.settings.accessibility.shared.utils.shouldShowFocusRingsInSuw
import com.android.settings.accessibility.textreading.data.DockBarHeightDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import com.google.android.material.slider.Slider
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DockBarHeightPreference(
    context: Context,
    val isUiOnly: Boolean,
) :
    IntRangeValuePreference,
    SliderPreferenceBinding,
    Slider.OnSliderTouchListener,
    Slider.OnChangeListener,
    PreferenceLifecycleProvider {

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return arrayOf(MUSTPASS_SET)
    }

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.WRITE_SETTINGS)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int {
        return ReadWritePermit.ALLOW
    }

    override val supportsWrite = true

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    private val dockBarHeightDataStore by lazy { DockBarHeightDataStore(context = context) }
    private val dockBarHeights by lazy { dockBarHeightDataStore.dockBarHeight.value.values }
    private val dockBarHeightLabels by lazy {
        dockBarHeights
            .map { value ->
                context.getString(R.string.dock_bar_height_percentage, (value * 100).toInt())
            }
            .toTypedArray()
    }
    private var isDraggingSlider = false

    private val _dockBarHeightPreview by lazy {
        MutableStateFlow(dockBarHeightDataStore.dockBarHeight.value)
    }

    /**
     * [dockBarHeightPreview] is the temporary dock bar height while the user is dragging and
     * haven't commit the change.
     */
    val dockBarHeightPreview by lazy { _dockBarHeightPreview.asStateFlow() }

    private val debounceCommitController by lazy {
        DebounceConfigurationChangeCommitController(minCommitDelay = MIN_COMMIT_DELAY)
    }

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.dock_bar_height_purpose

    override val title: Int
        get() = R.string.dock_size

    override val summary: Int
        get() = R.string.short_summary_dock_size

    override val keywords: Int
        get() = R.string.keywords_dock_bar_height

    override fun createWidget(context: Context): SliderPreference {
        val widget =
            if (context.isInSetupWizard()) {
                // Use TooltipSliderPreference in setup wizard for the temp fix for b/421323125.
                TooltipSliderPreference(context)
            } else {
                SliderPreference(context)
            }
        widget.apply {
            setIconStart(R.drawable.ic_remove_24dp)
            setIconStartContentDescription(R.string.dock_size_make_smaller_desc)
            setIconEnd(R.drawable.ic_add_24dp)
            setIconEndContentDescription(R.string.dock_size_make_larger_desc)
            setTickVisible(true)
            setDefaultValue(_dockBarHeightPreview.value.currentIndex)
            setExtraChangeListener(this@DockBarHeightPreference)
            setExtraTouchListener(this@DockBarHeightPreference)
        }
        return widget
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // The PreferenceMetadata is persistent to allow GET/SET api to access the storage.
        // Set the preference widget to non-persistent to prevent it trying to save the value to
        // datastore while the user is dragging, or when we want to have some delay to show the
        // preview before committing the changes.
        preference as SliderPreference
        preference.run {
            value = _dockBarHeightPreview.value.currentIndex
            setSliderStateDescription(dockBarHeightLabels[value])
            isPersistent = false
            // This change makes the row that contains the slider unable to be focused, but allows
            // the slider and its buttons to be focusable.
            if (shouldShowFocusRingsInSuw(context)) {
                isSelectable = false
            }
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        // This is needed to prevent slider value gets overwrites by [View#onRestoreInstanceState].
        context.findPreference<SliderPreference>(KEY)?.value =
            _dockBarHeightPreview.value.currentIndex
    }

    override fun getIncrementStep(context: Context): Int {
        return 1
    }

    override fun getMinValue(context: Context): Int {
        return 0
    }

    override fun getMaxValue(context: Context): Int {
        return dockBarHeights.size - 1
    }

    override fun storage(context: Context): KeyValueStore {
        return dockBarHeightDataStore
    }

    override fun onStartTrackingTouch(slider: Slider) {
        isDraggingSlider = true
    }

    override fun onStopTrackingTouch(slider: Slider) {
        isDraggingSlider = false
        // call data store to save the value
        commitChange(CHANGE_BY_SLIDER_DELAY, slider.value.toInt())
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        _dockBarHeightPreview.value = _dockBarHeightPreview.value.copy(currentIndex = value.toInt())

        if (!isDraggingSlider) {
            // if not dragging call datastore to save the value
            commitChange(CHANGE_BY_BUTTON_DELAY, value.toInt())
        }
    }

    @VisibleForTesting
    internal fun commitChange(delay: Duration, index: Int) {
        debounceCommitController.commitDelayed(delay) {
            dockBarHeightDataStore.setInt(
                KEY,
                index,
            )
        }
    }

    companion object {
        const val KEY = "dock_bar_height"
    }
}
