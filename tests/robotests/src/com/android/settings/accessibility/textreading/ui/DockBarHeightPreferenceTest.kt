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
import android.app.Activity
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.R
import com.android.settings.accessibility.textreading.data.DockBarHeightDataStore
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test for [DockBarHeightPreference]. */
@RunWith(RobolectricTestRunner::class)
class DockBarHeightPreferenceTest {
    @get:Rule
    val activityScenario: ActivityScenarioRule<EmptyFragmentActivity> =
        ActivityScenarioRule(EmptyFragmentActivity::class.java)

    private lateinit var context: Activity
    private lateinit var preference: DockBarHeightPreference
    private lateinit var dataStore: DockBarHeightDataStore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var preferenceScreen: PreferenceScreen

    @Before
    fun setUp() {
        activityScenario.scenario.onActivity { activity -> context = activity }
        preference = DockBarHeightPreference(context, false)
        dataStore = preference.storage(context) as DockBarHeightDataStore
        preferenceManager = PreferenceManager(context)
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
    }

    @Test
    fun getReadPermission_returnEmptyPermission() {
        assertThat(preference.getReadPermissions(context)).isEqualTo(Permissions.EMPTY)
    }

    @Test
    fun getWritePermissions_needsSystemSettingsPermission() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(Permissions.allOf(Manifest.permission.WRITE_SETTINGS))
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context = context, callingPid = 0, callingUid = 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context = context, callingPid = 0, callingUid = 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getSensitivityLevel_returnsNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    @Test
    fun getDockBarHeightPreview_initialStateMatchStorageData() {
        assertThat(preference.dockBarHeightPreview.value)
            .isEqualTo(dataStore.dockBarHeight.value)
    }

    @Test
    fun verifyWidgetInitialState() {
        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)

        assertThat(sliderPreference.title.toString())
            .isEqualTo(context.getString(R.string.dock_bar_height_title))
        assertThat(sliderPreference.summary.toString())
            .isEqualTo(context.getString(R.string.short_summary_dock_size))
        assertThat(sliderPreference.key).isEqualTo(DockBarHeightPreference.KEY)
        assertThat(sliderPreference.value)
            .isEqualTo(dataStore.dockBarHeight.value.currentIndex)
    }

    @Test
    fun verifySliderStateDescription() {
        val valueSize = preference.dockBarHeightPreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value.",
            valueSize > 1,
        )

        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }
        val sliderIndex = slider.value.toInt()
        val initialSliderStateDescription = slider.stateDescription
        val initialValue = preference.dockBarHeightPreview.value.values[sliderIndex]
        val newIndex = (sliderIndex + 1) % valueSize
        assertThat(sliderIndex).isEqualTo(preference.dockBarHeightPreview.value.currentIndex)
        slider.value = newIndex.toFloat()

        val expectedInitialDescription =
            context.getString(
                R.string.dock_bar_height_percentage,
                (initialValue * 100).toInt(),
            )
        assertThat(initialSliderStateDescription).isEqualTo(expectedInitialDescription)
        assertThat(preference.dockBarHeightPreview.value.currentIndex).isEqualTo(newIndex)
    }

    @Test
    fun getKeywords() {
        assertThat(preference.keywords).isEqualTo(R.string.keywords_dock_bar_height)
    }

    @Test
    fun getIncrementStep() {
        assertThat(preference.getIncrementStep(context)).isEqualTo(1)
    }

    @Test
    fun getMinValue() {
        assertThat(preference.getMinValue(context)).isEqualTo(0)
    }

    @Test
    fun getMaxValue() {
        assertThat(preference.getMaxValue(context))
            .isEqualTo(dataStore.dockBarHeight.value.values.size - 1)
    }

    @Test
    fun verifyDockBarHeightPreviewDataChangesWhenSliderValueIsChanged() {
        val valueSize = preference.dockBarHeightPreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value.",
            valueSize > 1,
        )

        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }
        val sliderIndex = slider.value.toInt()
        val newIndex = (sliderIndex + 1) % valueSize
        assertThat(sliderIndex).isEqualTo(preference.dockBarHeightPreview.value.currentIndex)
        slider.value = newIndex.toFloat()

        assertThat(preference.dockBarHeightPreview.value.currentIndex).isEqualTo(newIndex)
    }
}
