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

import android.app.appfunctions.AppFunctionException;
import android.app.appfunctions.AppFunctionService;
import android.app.appfunctions.ExecuteAppFunctionRequest;
import android.app.appfunctions.ExecuteAppFunctionResponse;
import android.app.appsearch.GenericDocument;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.SigningInfo;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
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

    /** 参数名（调用方通过 ExecuteAppFunctionRequest 的参数 GenericDocument 传入）。 */
    private static final String PARAM_PAGE = "page";

    /** 结果文档 schema（保留 README 可读性）。 */
    private static final String RESULT_SCHEMA = "SettingsPageResult";
    private static final String RESULT_PROP_OK = "ok";
    private static final String RESULT_PROP_PAGE = "page";
    private static final String RESULT_PROP_STATUS = "status";
    private static final String RESULT_PROP_ERROR = "error";

    /** 成功状态值。 */
    private static final String STATUS_LAUNCH_REQUESTED = "launch_requested";

    /** 失败错误码，与 SystemControlService.openSettingsPage 的返回保持一致。 */
    private static final String ERROR_UNSUPPORTED_PAGE = "unsupported_page";
    private static final String ERROR_ACTIVITY_NOT_FOUND = "activity_not_found";
    private static final String ERROR_PERMISSION_DENIED = "permission_denied";

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

        if (!FUNCTION_OPEN_SETTINGS_PAGE.equals(functionId)) {
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
            openSettingsPage(request.getParameters(), callback);
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
