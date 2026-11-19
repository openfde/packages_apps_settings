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
import android.util.Log
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DataStore for the dock bar height setting.
 *
 * The value is persisted in [Settings.System.DEFAULT_DEVICE_FONT_SCALE] and ranges from 0.5 to
 * 1.5 (inclusive) with 0.1 increments.
 */
class DockBarHeightDataStore(
    private val context: Context,
    private val settingsSystem: SettingsSystemStore = SettingsSystemStore.get(context),
) : KeyValueStoreDelegate {
    init {
        settingsSystem.run {
            setDefaultValue(DOCK_BAR_HEIGHT_KEY, FONT_SCALE_DEF_VALUE)
        }
    }

    override val keyValueStoreDelegate: KeyValueStore
        get() = settingsSystem

    private val _dockBarHeight = MutableStateFlow(loadDockBarHeight())
    val dockBarHeight = _dockBarHeight.asStateFlow()

    override fun contains(key: String): Boolean {
        return keyValueStoreDelegate.getFloat(DOCK_BAR_HEIGHT_KEY) != null
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return getClosestIndex(_dockBarHeight.value.defaultValue, _dockBarHeight.value.values) as? T?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return _dockBarHeight.value.currentIndex as? T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Int?) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }
        val data = _dockBarHeight.value
        val newValue = value as? Int ?: getDefaultValue(key, Int::class.javaObjectType)!!
        if (data.currentIndex != newValue) {
            keyValueStoreDelegate.setFloat(DOCK_BAR_HEIGHT_KEY, data.values[newValue])
            _dockBarHeight.value = data.copy(currentIndex = newValue)
        }
    }

    fun resetToDefault() {
        setInt(key = "reset", value = null)
    }

    private fun loadDockBarHeight(): DockBarHeight {
        val values =
            (MIN_DOCK_BAR_HEIGHT_TENTHS..MAX_DOCK_BAR_HEIGHT_TENTHS)
                .map { it / TENTHS_TO_VALUE }
                .toFloatArray()
        val defaultValue = keyValueStoreDelegate.getFloat(DOCK_BAR_HEIGHT_KEY) ?: FONT_SCALE_DEF_VALUE
        val currentIndex = getClosestIndex(defaultValue, values)
        return DockBarHeight(currentIndex, values, defaultValue)
    }

    private fun getClosestIndex(value: Float, values: FloatArray): Int {
        var lastValue = values[0]
        for (i in 1 until values.size) {
            val currentValue = values[i]
            if (value < (lastValue + (currentValue - lastValue) * .5f)) {
                return i - 1
            }
            lastValue = currentValue
        }
        return values.size - 1
    }

    companion object {
        private const val DOCK_BAR_HEIGHT_KEY = "dock_scale";//Settings.System.DEFAULT_DEVICE_FONT_SCALE
        internal const val FONT_SCALE_DEF_VALUE = 1.0f

        /** The dock bar height can be adjusted between 0.5 and 1.5 (inclusive). */
        private const val MIN_DOCK_BAR_HEIGHT_TENTHS = 8
        private const val MAX_DOCK_BAR_HEIGHT_TENTHS = 12
        private const val TENTHS_TO_VALUE = 10f
        private const val LOG_TAG = "DockBarHeightDataStore"
    }
}
