/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps a bounded in-memory history of the pages visited inside the Settings task so the custom
 * window title bar (see {@code settings_homepage_container.xml}) can offer working back / forward
 * buttons.
 *
 * <p>Every Settings page is a separate Activity ({@code Settings} / {@code SubSettings}) that only
 * differs by the fragment it hosts, so entries are keyed by a canonical form of the Intent that
 * started the page rather than by Activity class alone.
 *
 * <p>A page is recorded when it resumes. If it is already known, the cursor simply moves onto it
 * (this is what the system back button does) and the entries ahead of it are kept so the forward
 * button can return to them. Recording a brand new page drops everything ahead of the cursor.
 */
public final class SettingsNavigationHistory {

    /** Upper bound of the retained history, to keep memory usage bounded. */
    private static final int MAX_ENTRIES = 100;

    /** Receives history changes so the title bar can refresh the buttons' state. */
    public interface Listener {
        /** Called whenever {@link #canGoBack()} or {@link #canGoForward()} may have changed. */
        void onHistoryChanged(boolean canGoBack, boolean canGoForward);
    }

    private static SettingsNavigationHistory sInstance;

    private final List<Entry> mEntries = new ArrayList<>();
    private int mIndex = -1;
    @Nullable
    private Listener mListener;

    private SettingsNavigationHistory() {
    }

    /** Returns the process wide history instance. */
    public static synchronized SettingsNavigationHistory get() {
        if (sInstance == null) {
            sInstance = new SettingsNavigationHistory();
        }
        return sInstance;
    }

    /** Registers a listener, or clears it when {@code null} is passed. */
    public void setListener(@Nullable Listener listener) {
        mListener = listener;
        notifyListener();
    }

    /**
     * Clears {@code listener} only if it is still the registered one, so an Activity that is
     * being destroyed after its replacement already registered does not unregister the new one.
     */
    public void clearListener(@Nullable Listener listener) {
        if (mListener == listener) {
            mListener = null;
        }
    }

    /** @return whether there is a page before the current one. */
    public boolean canGoBack() {
        return mIndex > 0;
    }

    /** @return whether there is a page after the current one. */
    public boolean canGoForward() {
        return mIndex >= 0 && mIndex < mEntries.size() - 1;
    }

    /**
     * Records the page that just became visible and moves the cursor onto it.
     *
     * @param intent the Intent the resumed page was started with, may be {@code null}
     */
    public void record(@Nullable Intent intent) {
        if (intent == null || intent.getComponent() == null) {
            return;
        }
        final String key = keyOf(intent);
        final int existing = indexOf(key);
        if (existing >= 0) {
            mIndex = existing;
        } else {
            // A brand new page invalidates everything that was ahead of the cursor.
            while (mEntries.size() > mIndex + 1) {
                mEntries.remove(mEntries.size() - 1);
            }
            mEntries.add(new Entry(key, new Intent(intent)));
            while (mEntries.size() > MAX_ENTRIES) {
                mEntries.remove(0);
            }
            mIndex = mEntries.size() - 1;
        }
        notifyListener();
    }

    /**
     * Navigates to the previous page.
     *
     * @return whether a navigation was started
     */
    public boolean goBack(Context context) {
        if (!canGoBack()) {
            return false;
        }
        mIndex--;
        startPage(context, mEntries.get(mIndex).intent);
        notifyListener();
        return true;
    }

    /**
     * Navigates to the next page.
     *
     * @return whether a navigation was started
     */
    public boolean goForward(Context context) {
        if (!canGoForward()) {
            return false;
        }
        mIndex++;
        startPage(context, mEntries.get(mIndex).intent);
        notifyListener();
        return true;
    }

    private static void startPage(Context context, Intent intent) {
        final Intent launch = new Intent(intent);
        // Clear whatever sits above the target page so back/forward behaves like a browser,
        // while reusing an already existing instance of it when possible.
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(launch);
    }

    private int indexOf(String key) {
        for (int i = 0; i < mEntries.size(); i++) {
            if (mEntries.get(i).key.equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void notifyListener() {
        if (mListener != null) {
            mListener.onHistoryChanged(canGoBack(), canGoForward());
        }
    }

    /**
     * Builds a stable identity for a page. {@link Intent#filterEquals} is not enough because it
     * ignores extras, and the hosted fragment is carried in the extras.
     */
    private static String keyOf(Intent intent) {
        final StringBuilder builder = new StringBuilder();
        builder.append(intent.getComponent() != null
                ? intent.getComponent().flattenToShortString() : "");
        builder.append('|').append(intent.getAction());
        builder.append('|').append(intent.getDataString());
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            appendBundle(builder, extras, /* skipFragmentArgs= */ true);
            final Bundle fragmentArgs =
                    extras.getBundle(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            if (fragmentArgs != null) {
                appendBundle(builder, fragmentArgs, /* skipFragmentArgs= */ false);
            }
        }
        return builder.toString();
    }

    private static void appendBundle(
            StringBuilder builder, Bundle bundle, boolean skipFragmentArgs) {
        final List<String> keys = new ArrayList<>(bundle.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (skipFragmentArgs && SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS.equals(key)) {
                continue;
            }
            builder.append('|').append(key).append('=').append(bundle.get(key));
        }
    }

    private static final class Entry {
        final String key;
        final Intent intent;

        Entry(String key, Intent intent) {
            this.key = key;
            this.intent = intent;
        }
    }
}
