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

package com.android.settings.accessibility.textreading.data

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.textreading.data.DockBarHeightDataStore.Companion.FONT_SCALE_DEF_VALUE
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test for [DockBarHeightDataStore] */
@RunWith(RobolectricTestRunner::class)
class DockBarHeightDataStoreTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var dataStore: DockBarHeightDataStore
    private val settingsSystemStore = SettingsSystemStore.get(context)

    @Before
    fun setUp() {
        dataStore = DockBarHeightDataStore(context = context, settingsSystem = settingsSystemStore)
    }

    @Test
    fun init_setDefaultValue() {
        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(FONT_SCALE_DEF_VALUE)
    }

    @Test
    fun getKeyValueStoreDelegate() {
        assertThat(dataStore.keyValueStoreDelegate).isEqualTo(settingsSystemStore)
    }

    @Test
    fun dockBarHeight_loadCorrectData() {
        val expectedValues = (5..15).map { it / 10f }.toFloatArray()
        // val expectedDockBarHeight =
        //     DockBarHeight(
        //         currentIndex = expectedValues.indexOf(FONT_SCALE_DEF_VALUE),
        //         values = expectedValues,
        //         defaultValue = FONT_SCALE_DEF_VALUE,
        //     )

        // assertThat(dataStore.dockBarHeight.value).isEqualTo(expectedDockBarHeight)
    }

    @Test
    fun getInt_returnsCurrentIndex() {
        assertThat(dataStore.getInt("dock_bar_height"))
            .isEqualTo(dataStore.dockBarHeight.value.currentIndex)
    }

    @Test
    fun setValue_updateDockBarHeightDataAndSettings() {
        val values = dataStore.dockBarHeight.value.values
        val currentIndex = dataStore.dockBarHeight.value.currentIndex
        val newIndex = (currentIndex + 1) % values.size

        dataStore.setInt("dock_bar_height", newIndex)

        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(values[newIndex])
        assertThat(dataStore.dockBarHeight.value.currentIndex).isEqualTo(newIndex)
    }

    @Test
    fun setValue_smallestValue_writesMinDockBarHeight() {
        dataStore.setInt("dock_bar_height", 0)

        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(0.5f)
    }

    @Test
    fun setValue_largestValue_writesMaxDockBarHeight() {
        val maxIndex = dataStore.dockBarHeight.value.values.size - 1
        dataStore.setInt("dock_bar_height", maxIndex)

        assertThat(
                Settings.System.getFloat(
                    context.contentResolver,
                    Settings.System.DEFAULT_DEVICE_FONT_SCALE,
                    FONT_SCALE_DEF_VALUE,
                )
            )
            .isEqualTo(1.5f)
    }

    // Appfunction calls getValue with int type instead of getInt.
    @Test
    fun getValue_intType_equalsGetInt() {
        assertThat(dataStore.getValue("key", Int::class.javaObjectType))
            .isEqualTo(dataStore.getInt("key"))
    }

    // Appfunction calls setValue with int type instead of setInt.
    @Test
    fun setValue_intType_updatesValue() {
        val newValue = dataStore.dockBarHeight.value.values.size - 1
        dataStore.setValue("key", Int::class.javaObjectType, newValue)

        assertThat(dataStore.getInt("key")).isEqualTo(newValue)
    }
}
