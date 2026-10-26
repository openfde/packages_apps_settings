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

package com.android.settings.appfunctions;

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MIN;
import static com.android.settingslib.display.BrightnessUtils.convertLinearToGammaFloat;

import android.app.UiModeManager;
import android.app.appfunctions.AppFunctionException;
import android.app.appfunctions.AppFunctionService;
import android.app.appfunctions.ExecuteAppFunctionRequest;
import android.app.appfunctions.ExecuteAppFunctionResponse;
import android.app.appsearch.GenericDocument;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.SigningInfo;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通过 App Functions 暴露“打开指定系统设置页面”的能力，供系统智能体（Agent）或其他调用方使用
 * {@link android.app.appfunctions.AppFunctionManager#executeAppFunction} 打开对应的设置页面。
 *
 * <p>支持的功能：
 * <ul>
 *     <li>{@code openSettingsPage}：参数 {@code page}（必填），已知取值与 FdeSystemControl 的
 *     {@code SystemControlService.openSettingsPage} 保持一致：
 *     {@code wifi} / {@code display} / {@code sound} / {@code apps} / {@code storage}。</li>
 *     <li>{@code adjustBrightness}：调节屏幕亮度。参数 {@code action}（必填）：
 *     {@code dim}（调暗）/ {@code brighten}（调亮），{@code step}（可选，默认 10）为每次
 *     调节的亮度百分比。亮度读写的换算逻辑与
 *     {@code src/com/android/settings/display/BrightnessLevelPreference.kt} 保持一致。</li>
 *     <li>{@code setDarkTheme}：开启/关闭深色主题。参数 {@code action}（必填）：
 *     {@code on}（开启）/ {@code off}（关闭），通过
 *     {@link UiModeManager#setNightModeActivated(boolean)} 切换，行为与
 *     {@code DarkModeActivationPreferenceController} 一致。</li>
 *     <li>{@code adjustMediaVolume}：调节媒体音量。参数 {@code action}（必填）：
 *     {@code volume_up}（调高）/ {@code volume_down}（调低），{@code step}（可选，默认 10）
 *     为每次调节的音量百分比，映射到媒体音量的分档范围
 *     {@code [getStreamMinVolume, getStreamMaxVolume]}，通过
 *     {@link AudioManager#setStreamVolume(int, int, int)} 写回。</li>
 *     <li>{@code openApp}：按包名或应用名打开某个应用。参数 {@code packageName}（可选）或
 *     {@code appName}（可选），至少提供一个，同时提供时以 {@code packageName} 优先：
 *     <ul>
 *         <li>{@code packageName}：直接调用
 *         {@link PackageManager#getLaunchIntentForPackage(String)} 获取启动 Intent。</li>
 *         <li>{@code appName}：对所有可启动应用的应用名做忽略大小写的精确/包含匹配，
 *         命中唯一应用后启动。</li>
 *     </ul></li>
 * </ul>
 *
 * <p>功能列表在 {@code assets/fdesettings_appfunctions.xml} 中声明，AndroidManifest 中通过
 * {@code <property android:name="android.app.appfunctions"
 * android:value="fdesettings_appfunctions.xml"/>} 关联。
 */
@Keep
public class OpenSettingsPageAppFunctionService extends AppFunctionService {
    private static final String TAG = "OpenSettingsPageAppFunctionService";

    /** 功能 ID，与 {@code assets/fdesettings_appfunctions.xml} 中的 function_id 保持一致。 */
    public static final String FUNCTION_OPEN_SETTINGS_PAGE = "openSettingsPage";

    /** 功能 ID：调节屏幕亮度，与 {@code assets/fdesettings_appfunctions.xml} 保持一致。 */
    public static final String FUNCTION_ADJUST_BRIGHTNESS = "adjustBrightness";

    /** 功能 ID：调节媒体音量，与 {@code assets/fdesettings_appfunctions.xml} 保持一致。 */
    public static final String FUNCTION_ADJUST_MEDIA_VOLUME = "adjustMediaVolume";

    /** 功能 ID：开启/关闭深色主题，与 {@code assets/fdesettings_appfunctions.xml} 保持一致。 */
    public static final String FUNCTION_SET_DARK_THEME = "setDarkTheme";

    /** 功能 ID：按包名或应用名打开某个应用，与 {@code assets/fdesettings_appfunctions.xml} 保持一致。 */
    public static final String FUNCTION_OPEN_APP = "openApp";

    /** 参数名（调用方通过 ExecuteAppFunctionRequest 的参数 GenericDocument 传入）。 */
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_STEP = "step";
    private static final String PARAM_PACKAGE_NAME = "packageName";
    private static final String PARAM_APP_NAME = "appName";

    /** adjustBrightness 的 action 取值（与系统智能体约定的语义保持一致）。 */
    private static final String ACTION_DIM = "dim";
    private static final String ACTION_BRIGHTEN = "brighten";

    /** adjustMediaVolume 的 action 取值：volume_up（调高）/ volume_down（调低）。 */
    private static final String ACTION_VOLUME_UP = "volume_up";
    private static final String ACTION_VOLUME_DOWN = "volume_down";

    /** setDarkTheme 的 action 取值：on（开启深色主题）/ off（关闭深色主题）。 */
    private static final String ACTION_ON = "on";
    private static final String ACTION_OFF = "off";

    /** adjustBrightness 每次默认调节的亮度百分比，以及亮度的取值范围。 */
    private static final int DEFAULT_BRIGHTNESS_STEP_PERCENT = 10;
    private static final int MIN_BRIGHTNESS_PERCENT = 0;
    private static final int MAX_BRIGHTNESS_PERCENT = 100;

    /** adjustMediaVolume 每次默认调节的音量百分比，以及音量的百分比取值范围。 */
    private static final int DEFAULT_VOLUME_STEP_PERCENT = 10;
    private static final int MIN_VOLUME_PERCENT = 0;
    private static final int MAX_VOLUME_PERCENT = 100;

    /** 结果文档 schema（保留 README 可读性）。 */
    private static final String RESULT_SCHEMA = "SettingsPageResult";
    private static final String RESULT_SCHEMA_BRIGHTNESS = "BrightnessAdjustResult";
    private static final String RESULT_SCHEMA_MEDIA_VOLUME = "MediaVolumeResult";
    private static final String RESULT_SCHEMA_DARK_THEME = "DarkThemeResult";
    private static final String RESULT_SCHEMA_OPEN_APP = "OpenAppResult";
    private static final String RESULT_PROP_OK = "ok";
    private static final String RESULT_PROP_PAGE = "page";
    private static final String RESULT_PROP_STATUS = "status";
    private static final String RESULT_PROP_ERROR = "error";
    private static final String RESULT_PROP_ACTION = "action";
    private static final String RESULT_PROP_BEFORE = "before";
    private static final String RESULT_PROP_AFTER = "after";
    private static final String RESULT_PROP_PACKAGE_NAME = "packageName";
    private static final String RESULT_PROP_APP_NAME = "appName";
    private static final String RESULT_PROP_ENABLED = "enabled";

    /** 成功状态值。 */
    private static final String STATUS_LAUNCH_REQUESTED = "launch_requested";
    private static final String STATUS_BRIGHTNESS_ADJUSTED = "brightness_adjusted";
    private static final String STATUS_MEDIA_VOLUME_ADJUSTED = "media_volume_adjusted";
    private static final String STATUS_DARK_THEME_SET = "dark_theme_set";

    /** 失败错误码，与 SystemControlService.openSettingsPage 的返回保持一致。 */
    private static final String ERROR_UNSUPPORTED_PAGE = "unsupported_page";
    private static final String ERROR_ACTIVITY_NOT_FOUND = "activity_not_found";
    private static final String ERROR_PERMISSION_DENIED = "permission_denied";
    private static final String ERROR_INVALID_ACTION = "invalid_action";
    private static final String ERROR_BRIGHTNESS_UNAVAILABLE = "brightness_unavailable";
    private static final String ERROR_MEDIA_VOLUME_UNAVAILABLE = "media_volume_unavailable";
    private static final String ERROR_INVALID_APP = "invalid_app";
    private static final String ERROR_APP_NOT_FOUND = "app_not_found";
    private static final String ERROR_SYSTEM_ERROR = "system_error";

    /** 页面 key -> 对应的系统设置 Intent，与 FdeSystemControl 的 SystemControlService 一致。 */
    private static final Map<String, Intent> SETTINGS_PAGES = new HashMap<>();

    static {
        SETTINGS_PAGES.put("wifi", new Intent(Settings.ACTION_WIFI_SETTINGS));
        SETTINGS_PAGES.put("display", new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        SETTINGS_PAGES.put("sound", new Intent(Settings.ACTION_SOUND_SETTINGS));
        SETTINGS_PAGES.put("apps", new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        SETTINGS_PAGES.put("storage", new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
    }

    @Override
    public void onExecuteFunction(
            @NonNull ExecuteAppFunctionRequest request,
            @NonNull String callingPackage,
            @NonNull SigningInfo callingPackageSigningInfo,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String functionId = request.getFunctionIdentifier();
        Log.d(TAG, "onExecuteFunction: " + functionId + " from " + callingPackage);

        if (!FUNCTION_OPEN_SETTINGS_PAGE.equals(functionId)
                && !FUNCTION_ADJUST_BRIGHTNESS.equals(functionId)
                && !FUNCTION_ADJUST_MEDIA_VOLUME.equals(functionId)
                && !FUNCTION_SET_DARK_THEME.equals(functionId)
                && !FUNCTION_OPEN_APP.equals(functionId)) {
            callback.onError(
                    new AppFunctionException(
                            AppFunctionException.ERROR_FUNCTION_NOT_FOUND,
                            "Unknown function: " + functionId));
            return;
        }

        try {
            if (cancellationSignal.isCanceled()) {
                callback.onError(
                        new AppFunctionException(
                                AppFunctionException.ERROR_CANCELLED, "Execution cancelled"));
                return;
            }
            if (FUNCTION_OPEN_SETTINGS_PAGE.equals(functionId)) {
                openSettingsPage(request.getParameters(), callback);
            } else if (FUNCTION_ADJUST_BRIGHTNESS.equals(functionId)) {
                adjustBrightness(request.getParameters(), callback);
            } else if (FUNCTION_ADJUST_MEDIA_VOLUME.equals(functionId)) {
                adjustMediaVolume(request.getParameters(), callback);
            } else if (FUNCTION_SET_DARK_THEME.equals(functionId)) {
                setDarkTheme(request.getParameters(), callback);
            } else {
                openApp(request.getParameters(), callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "onExecuteFunction failed for " + functionId, e);
            callback.onError(
                    new AppFunctionException(
                            AppFunctionException.ERROR_SYSTEM_ERROR,
                            "Failed to execute function: " + e.getMessage()));
        }
    }

    /**
     * 打开指定的设置页面。
     *
     * <p>{@link AppFunctionService#onExecuteFunction} 运行在主线程，这里只做轻量的参数解析 +
     * 启动 Activity，不会产生阻塞主线程的耗时操作。
     */
    private void openSettingsPage(
            @NonNull GenericDocument parameters,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String page = parameters.getPropertyString(PARAM_PAGE);
        if (page == null || page.isEmpty() || !SETTINGS_PAGES.containsKey(page)) {
            callback.onResult(new ExecuteAppFunctionResponse(buildErrorResult(page, ERROR_UNSUPPORTED_PAGE)));
            return;
        }

        Intent intent = new Intent(SETTINGS_PAGES.get(page));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            Log.i(TAG, "Settings page requested: " + page);
            callback.onResult(new ExecuteAppFunctionResponse(buildOkResult(page)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Settings page not found: " + page, e);
            callback.onResult(new ExecuteAppFunctionResponse(buildErrorResult(page, ERROR_ACTIVITY_NOT_FOUND)));
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied opening settings page: " + page, e);
            callback.onResult(new ExecuteAppFunctionResponse(buildErrorResult(page, ERROR_PERMISSION_DENIED)));
        }
    }

    /**
     * 调节屏幕亮度（调暗 / 调亮）。
     *
     * <p>亮度读写的换算逻辑与
     * {@code src/com/android/settings/display/BrightnessLevelPreference.kt} 保持一致：
     * <ul>
     *     <li>读：通过 {@link Display#getBrightnessInfo()} 拿到当前线性亮度，再用
     *     {@code BrightnessUtils.convertLinearToGammaFloat} 换算到 gamma 空间并映射为 0~100
     *     的百分比。</li>
     *     <li>写：在当前位置上增加/减少 {@code step} 个百分比后，通过
     *     {@link DisplayManager#setBrightness(int, float, int)} 以
     *     {@link DisplayManager#BRIGHTNESS_UNIT_PERCENTAGE} 单位写回。</li>
     * </ul>
     *
     * <p>参数：
     * <ul>
     *     <li>{@code action}（必填）：{@code dim}（调暗）或 {@code brighten}（调亮）。</li>
     *     <li>{@code step}（可选，默认 10）：每次调节的亮度百分比，取值 1~100。</li>
     * </ul>
     *
     * <p>{@link AppFunctionService#onExecuteFunction} 运行在主线程，这里的亮度读取/写回都是
     * 轻量 IPC，不会产生阻塞主线程的耗时操作。
     */
    private void adjustBrightness(
            @NonNull GenericDocument parameters,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String action = parameters.getPropertyString(PARAM_ACTION);
        int direction;
        if (ACTION_DIM.equals(action)) {
            direction = -1;
        } else if (ACTION_BRIGHTEN.equals(action)) {
            direction = 1;
        } else {
            Log.w(TAG, "Invalid brightness action: " + action);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildBrightnessErrorResult(action, ERROR_INVALID_ACTION)));
            return;
        }

        int current = getCurrentBrightnessPercent();
        if (current < 0) {
            Log.w(TAG, "Brightness is unavailable for adjustment");
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildBrightnessErrorResult(action, ERROR_BRIGHTNESS_UNAVAILABLE)));
            return;
        }

        int step = DEFAULT_BRIGHTNESS_STEP_PERCENT;
        long[] stepValues = parameters.getPropertyLongArray(PARAM_STEP);
        if (stepValues != null && stepValues.length > 0) {
            step = (int) Math.max(1, Math.min(MAX_BRIGHTNESS_PERCENT, stepValues[0]));
        }
        int target =
                Math.max(
                        MIN_BRIGHTNESS_PERCENT,
                        Math.min(MAX_BRIGHTNESS_PERCENT, current + direction * step));

        DisplayManager displayManager = getSystemService(DisplayManager.class);
        if (displayManager == null) {
            Log.w(TAG, "DisplayManager is unavailable");
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildBrightnessErrorResult(action, ERROR_BRIGHTNESS_UNAVAILABLE)));
            return;
        }
        Display display = getDisplay();
        int displayId = display != null ? display.getDisplayId() : Display.DEFAULT_DISPLAY;
        displayManager.setBrightness(displayId, target, DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE);

        Log.i(TAG, "Brightness adjusted: " + action + " from " + current + "% to " + target + "%");
        callback.onResult(
                new ExecuteAppFunctionResponse(
                        buildBrightnessOkResult(action, current, target)));
    }

    /**
     * 调节媒体音量（调高 / 调低）。
     *
     * <p>音量的读写基于 {@link AudioManager} 的 {@link AudioManager#STREAM_MUSIC} 流：
     * <ul>
     *     <li>读：{@link AudioManager#getStreamVolume(int)}，并换算为 0~100 的百分比
     *     （相对 {@code [getStreamMinVolume, getStreamMaxVolume]} 分档范围）。</li>
     *     <li>写：将 {@code step} 个百分比换算为分档步长 delta，在当前位置上增加/减少后，
     *     通过 {@link AudioManager#setStreamVolume(int, int, int)} 写回并夹取到合法范围。</li>
     * </ul>
     *
     * <p>参数：
     * <ul>
     *     <li>{@code action}（必填）：{@code volume_up}（调高）或 {@code volume_down}（调低）。</li>
     *     <li>{@code step}（可选，默认 10）：每次调节的音量百分比，取值 1~100。</li>
     * </ul>
     *
     * <p>{@link AppFunctionService#onExecuteFunction} 运行在主线程，这里的音量读取/写回都是
     * 轻量 IPC，不会产生阻塞主线程的耗时操作。
     */
    private void adjustMediaVolume(
            @NonNull GenericDocument parameters,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String action = parameters.getPropertyString(PARAM_ACTION);
        int direction;
        if (ACTION_VOLUME_UP.equals(action)) {
            direction = 1;
        } else if (ACTION_VOLUME_DOWN.equals(action)) {
            direction = -1;
        } else {
            Log.w(TAG, "Invalid media volume action: " + action);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildMediaVolumeErrorResult(action, ERROR_INVALID_ACTION)));
            return;
        }

        AudioManager audioManager = getSystemService(AudioManager.class);
        if (audioManager == null) {
            Log.w(TAG, "AudioManager is unavailable");
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildMediaVolumeErrorResult(action, ERROR_MEDIA_VOLUME_UNAVAILABLE)));
            return;
        }

        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= min) {
            Log.w(TAG, "Media volume range is invalid: min=" + min + ", max=" + max);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildMediaVolumeErrorResult(action, ERROR_MEDIA_VOLUME_UNAVAILABLE)));
            return;
        }
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int step = DEFAULT_VOLUME_STEP_PERCENT;
        long[] stepValues = parameters.getPropertyLongArray(PARAM_STEP);
        if (stepValues != null && stepValues.length > 0) {
            step = (int) Math.max(MIN_VOLUME_PERCENT + 1, Math.min(MAX_VOLUME_PERCENT, stepValues[0]));
        }
        // 将百分比换算为分档步长（至少 1 档）。
        int delta = Math.max(1, (int) Math.round((max - min) * step / 100.0));
        int target = Math.max(min, Math.min(max, current + direction * delta));

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, /* flags= */ 0);

        int beforePercent = Math.round((current - min) * 100.0f / (max - min));
        int afterPercent = Math.round((target - min) * 100.0f / (max - min));
        Log.i(TAG, "Media volume adjusted: " + action + " from " + beforePercent
                + "% to " + afterPercent + "%");
        callback.onResult(
                new ExecuteAppFunctionResponse(
                        buildMediaVolumeOkResult(action, beforePercent, afterPercent)));
    }

    /**
     * 读取当前显示屏的亮度百分比（0~100），读取失败时返回 -1。
     *
     * <p>换算逻辑与 {@code BrightnessLevelPreference.kt} 一致：线性亮度 -> gamma 空间 ->
     * 百分比。
     */
    private int getCurrentBrightnessPercent() {
        Display display = getDisplay();
        if (display == null) {
            return -1;
        }

        BrightnessInfo info = display.getBrightnessInfo();
        if (info == null) {
            return -1;
        }

        int gammaSpace = convertLinearToGammaFloat(
                info.brightness,
                info.brightnessMinimum,
                info.brightnessMaximum);

        return (int) Math.round(
                toPercentage(
                        gammaSpace,
                        GAMMA_SPACE_MIN,
                        GAMMA_SPACE_MAX) * 100.0);
    }

    /** 将 gamma 空间的值映射到 0.0~1.0，与 {@code BrightnessLevelPreference.kt} 的
     *  toPercentage 实现一致。 */
    private double toPercentage(int value, int min, int max) {
        if (value > max) {
            return 1.0;
        }
        if (value < min) {
            return 0.0;
        }
        return (value - min) / (double) (max - min);
    }

    /**
     * 开启/关闭深色主题。
     *
     * <p>参数：
     * <ul>
     *     <li>{@code action}（必填）：{@code on}（开启深色主题）或 {@code off}（关闭深色主题）。</li>
     * </ul>
     *
     * <p>通过 {@link UiModeManager#setNightModeActivated(boolean)} 切换，与
     * {@code DarkModeActivationPreferenceController.onPreferenceChange} 的行为一致，均为轻量
     * 系统调用，不会阻塞主线程。
     */
    private void setDarkTheme(
            @NonNull GenericDocument parameters,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String action = parameters.getPropertyString(PARAM_ACTION);
        boolean enable;
        if (ACTION_ON.equals(action)) {
            enable = true;
        } else if (ACTION_OFF.equals(action)) {
            enable = false;
        } else {
            Log.w(TAG, "Invalid dark theme action: " + action);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildDarkThemeErrorResult(action, ERROR_INVALID_ACTION)));
            return;
        }

        UiModeManager uiModeManager = getSystemService(UiModeManager.class);
        if (uiModeManager == null) {
            Log.w(TAG, "UiModeManager is unavailable");
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildDarkThemeErrorResult(action, ERROR_SYSTEM_ERROR)));
            return;
        }
        uiModeManager.setNightModeActivated(enable);

        Log.i(TAG, "Dark theme " + (enable ? "enabled" : "disabled"));
        callback.onResult(
                new ExecuteAppFunctionResponse(buildDarkThemeOkResult(action, enable)));
    }

    /**
     * 按包名或应用名打开某个应用。
     *
     * <p>参数（至少提供一个，同时提供时以 {@code packageName} 优先）：
     * <ul>
     *     <li>{@code packageName}（可选）：应用包名，通过
     *     {@link PackageManager#getLaunchIntentForPackage(String)} 获取启动 Intent。</li>
     *     <li>{@code appName}（可选）：应用名称（label），对所有可启动应用的应用名做
     *     忽略大小写的精确匹配，若唯一命中则启动；无精确命中时再做包含匹配并启动第一个
     *     命中项。</li>
     * </ul>
     *
     * <p>{@link AppFunctionService#onExecuteFunction} 运行在主线程，这里仅做一次
     * {@code queryIntentActivities}（系统已缓存的可启动应用列表）+ 启动 Activity，均为
     * 轻量 IPC，不会产生阻塞主线程的耗时操作。
     */
    private void openApp(
            @NonNull GenericDocument parameters,
            @NonNull OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        String packageName = parameters.getPropertyString(PARAM_PACKAGE_NAME);
        String appName = parameters.getPropertyString(PARAM_APP_NAME);
        boolean hasPackageName = packageName != null && !packageName.isEmpty();
        boolean hasAppName = appName != null && !appName.isEmpty();

        if (!hasPackageName && !hasAppName) {
            Log.w(TAG, "openApp requires packageName or appName");
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildOpenAppErrorResult(packageName, appName, ERROR_INVALID_APP)));
            return;
        }

        Intent intent;
        if (hasPackageName) {
            intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                Log.w(TAG, "App not found by package name: " + packageName);
                callback.onResult(
                        new ExecuteAppFunctionResponse(
                                buildOpenAppErrorResult(packageName, appName, ERROR_APP_NOT_FOUND)));
                return;
            }
        } else {
            intent = resolveLaunchIntentByAppName(appName);
            if (intent == null) {
                Log.w(TAG, "App not found by app name: " + appName);
                callback.onResult(
                        new ExecuteAppFunctionResponse(
                                buildOpenAppErrorResult(packageName, appName, ERROR_APP_NOT_FOUND)));
                return;
            }
        }

        String resolvedPackage =
                intent.getPackage() != null ? intent.getPackage() : packageName;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            Log.i(TAG, "App launch requested: " + resolvedPackage);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildOpenAppOkResult(resolvedPackage, appName)));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "App activity not found: " + resolvedPackage, e);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildOpenAppErrorResult(resolvedPackage, appName, ERROR_ACTIVITY_NOT_FOUND)));
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied opening app: " + resolvedPackage, e);
            callback.onResult(
                    new ExecuteAppFunctionResponse(
                            buildOpenAppErrorResult(resolvedPackage, appName, ERROR_PERMISSION_DENIED)));
        }
    }

    /**
     * 通过应用名（label）解析启动 Intent。
     *
     * <p>遍历所有带 {@code CATEGORY_LAUNCHER} 入口的应用，按 label 做忽略大小写的匹配：
     * <ol>
     *     <li>精确匹配（全等）：若唯一命中，直接返回该应用的启动 Intent。</li>
     *     <li>包含匹配（子串）：无精确命中时采用，返回第一个包含命中的启动 Intent。</li>
     * </ol>
     *
     * <p>存在多个匹配项时会记录 warning 日志并取第一个，保证调用方总能得到确定的启动行为；
     * 没有任何匹配时返回 {@code null}。
     */
    @Nullable
    private Intent resolveLaunchIntentByAppName(@NonNull String appName) {
        String target = appName.trim();
        if (target.isEmpty()) {
            return null;
        }

        PackageManager packageManager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos =
                packageManager.queryIntentActivities(mainIntent, /* flags= */ 0);

        List<ResolveInfo> exactMatches = new ArrayList<>();
        List<ResolveInfo> containsMatches = new ArrayList<>();
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        for (ResolveInfo resolveInfo : resolveInfos) {
            CharSequence label = resolveInfo.loadLabel(packageManager);
            if (label == null) {
                continue;
            }
            String labelStr = label.toString().trim();
            if (labelStr.equalsIgnoreCase(target)) {
                exactMatches.add(resolveInfo);
            } else if (labelStr.toLowerCase(Locale.ROOT).contains(lowerTarget)) {
                containsMatches.add(resolveInfo);
            }
        }

        List<ResolveInfo> candidates =
                !exactMatches.isEmpty() ? exactMatches : containsMatches;
        if (candidates.isEmpty()) {
            Log.w(TAG, "No app matches app name: " + appName);
            return null;
        }
        if (candidates.size() > 1) {
            Log.w(TAG, "Multiple apps match app name \"" + appName + "\", using the first one");
        }

        String packageName = candidates.get(0).activityInfo.packageName;
        return packageManager.getLaunchIntentForPackage(packageName);
    }

    private GenericDocument buildDarkThemeOkResult(
            @NonNull String action, boolean enable) {
        return new GenericDocument.Builder<>(
                        getPackageName(),
                        FUNCTION_SET_DARK_THEME + "_result",
                        RESULT_SCHEMA_DARK_THEME)
                .setPropertyBoolean(RESULT_PROP_OK, true)
                .setPropertyString(RESULT_PROP_ACTION, action)
                .setPropertyBoolean(RESULT_PROP_ENABLED, enable)
                .setPropertyString(RESULT_PROP_STATUS, STATUS_DARK_THEME_SET)
                .build();
    }

    private GenericDocument buildDarkThemeErrorResult(
            @Nullable String action, @NonNull String error) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                                getPackageName(),
                                FUNCTION_SET_DARK_THEME + "_result",
                                RESULT_SCHEMA_DARK_THEME)
                        .setPropertyBoolean(RESULT_PROP_OK, false)
                        .setPropertyString(RESULT_PROP_ERROR, error);
        if (action != null) {
            builder.setPropertyString(RESULT_PROP_ACTION, action);
        }
        return builder.build();
    }

    private GenericDocument buildOpenAppOkResult(
            @Nullable String packageName, @Nullable String appName) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                                getPackageName(),
                                FUNCTION_OPEN_APP + "_result",
                                RESULT_SCHEMA_OPEN_APP)
                        .setPropertyBoolean(RESULT_PROP_OK, true)
                        .setPropertyString(RESULT_PROP_STATUS, STATUS_LAUNCH_REQUESTED);
        if (packageName != null && !packageName.isEmpty()) {
            builder.setPropertyString(RESULT_PROP_PACKAGE_NAME, packageName);
        }
        if (appName != null && !appName.isEmpty()) {
            builder.setPropertyString(RESULT_PROP_APP_NAME, appName);
        }
        return builder.build();
    }

    private GenericDocument buildOpenAppErrorResult(
            @Nullable String packageName,
            @Nullable String appName,
            @NonNull String error) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                                getPackageName(),
                                FUNCTION_OPEN_APP + "_result",
                                RESULT_SCHEMA_OPEN_APP)
                        .setPropertyBoolean(RESULT_PROP_OK, false)
                        .setPropertyString(RESULT_PROP_ERROR, error);
        if (packageName != null && !packageName.isEmpty()) {
            builder.setPropertyString(RESULT_PROP_PACKAGE_NAME, packageName);
        }
        if (appName != null && !appName.isEmpty()) {
            builder.setPropertyString(RESULT_PROP_APP_NAME, appName);
        }
        return builder.build();
    }

    private GenericDocument buildBrightnessOkResult(
            @NonNull String action, int before, int after) {
        return new GenericDocument.Builder<>(
                        getPackageName(),
                        FUNCTION_ADJUST_BRIGHTNESS + "_result",
                        RESULT_SCHEMA_BRIGHTNESS)
                .setPropertyBoolean(RESULT_PROP_OK, true)
                .setPropertyString(RESULT_PROP_ACTION, action)
                .setPropertyLong(RESULT_PROP_BEFORE, before)
                .setPropertyLong(RESULT_PROP_AFTER, after)
                .setPropertyString(RESULT_PROP_STATUS, STATUS_BRIGHTNESS_ADJUSTED)
                .build();
    }

    private GenericDocument buildBrightnessErrorResult(
            @Nullable String action, @NonNull String error) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                                getPackageName(),
                                FUNCTION_ADJUST_BRIGHTNESS + "_result",
                                RESULT_SCHEMA_BRIGHTNESS)
                        .setPropertyBoolean(RESULT_PROP_OK, false)
                        .setPropertyString(RESULT_PROP_ERROR, error);
        if (action != null) {
            builder.setPropertyString(RESULT_PROP_ACTION, action);
        }
        return builder.build();
    }

    private GenericDocument buildMediaVolumeOkResult(
            @NonNull String action, int before, int after) {
        return new GenericDocument.Builder<>(
                        getPackageName(),
                        FUNCTION_ADJUST_MEDIA_VOLUME + "_result",
                        RESULT_SCHEMA_MEDIA_VOLUME)
                .setPropertyBoolean(RESULT_PROP_OK, true)
                .setPropertyString(RESULT_PROP_ACTION, action)
                .setPropertyLong(RESULT_PROP_BEFORE, before)
                .setPropertyLong(RESULT_PROP_AFTER, after)
                .setPropertyString(RESULT_PROP_STATUS, STATUS_MEDIA_VOLUME_ADJUSTED)
                .build();
    }

    private GenericDocument buildMediaVolumeErrorResult(
            @Nullable String action, @NonNull String error) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                                getPackageName(),
                                FUNCTION_ADJUST_MEDIA_VOLUME + "_result",
                                RESULT_SCHEMA_MEDIA_VOLUME)
                        .setPropertyBoolean(RESULT_PROP_OK, false)
                        .setPropertyString(RESULT_PROP_ERROR, error);
        if (action != null) {
            builder.setPropertyString(RESULT_PROP_ACTION, action);
        }
        return builder.build();
    }

    private GenericDocument buildOkResult(@NonNull String page) {
        return new GenericDocument.Builder<>(
                        getPackageName(), FUNCTION_OPEN_SETTINGS_PAGE + "_result", RESULT_SCHEMA)
                .setPropertyBoolean(RESULT_PROP_OK, true)
                .setPropertyString(RESULT_PROP_PAGE, page)
                .setPropertyString(RESULT_PROP_STATUS, STATUS_LAUNCH_REQUESTED)
                .build();
    }

    private GenericDocument buildErrorResult(@Nullable String page, @NonNull String error) {
        GenericDocument.Builder<?> builder =
                new GenericDocument.Builder<>(
                        getPackageName(), FUNCTION_OPEN_SETTINGS_PAGE + "_result", RESULT_SCHEMA)
                        .setPropertyBoolean(RESULT_PROP_OK, false)
                        .setPropertyString(RESULT_PROP_ERROR, error);
        if (page != null) {
            builder.setPropertyString(RESULT_PROP_PAGE, page);
        }
        return builder.build();
    }
}
