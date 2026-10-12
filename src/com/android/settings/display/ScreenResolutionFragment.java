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

package com.android.settings.display;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/** Preference fragment used for switch screen resolution */
// LINT.IfChange
@SearchIndexable
public class ScreenResolutionFragment extends RadioButtonPickerFragment {
    private static final String TAG = "ScreenResolution";

    private Resources mResources;
    private static final String SCREEN_RESOLUTION = "user_selected_resolution";
    private static final String SCREEN_RESOLUTION_KEY = "screen_resolution";
    private Display mDefaultDisplay;
    private List<Point> mResolutionList;
    private Set<Point> mResolutions;
    private SpannableString[] mScreenResolutionSummaries;

    private IllustrationPreference mImagePreference;
    private DisplayObserver mDisplayObserver;

    private int mHighWidth;
    private int mFullWidth;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mDefaultDisplay =
                context.getSystemService(DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        mResources = context.getResources();
        mImagePreference = new IllustrationPreference(context);
        mDisplayObserver = new DisplayObserver(context);
        ScreenResolutionController controller =
                new ScreenResolutionController(context, SCREEN_RESOLUTION_KEY);
        Set<Point> resolutionSet = controller.getAllSupportedResolutions();
        mResolutions = resolutionSet;
        mResolutionList = new ArrayList<>(resolutionSet);
        Collections.sort(mResolutionList, (a, b) -> a.x * a.y - b.x * b.y);
        mScreenResolutionSummaries = new SpannableString[mResolutionList.size()];
        for (int i = 0; i < mResolutionList.size(); i++) {
            Point p = mResolutionList.get(i);
            mScreenResolutionSummaries[i] = getResolutionSpannable(p.x, p.y);
        }
        if (!mResolutionList.isEmpty()) {
            mHighWidth = mResolutionList.get(0).x;
            mFullWidth = mResolutionList.get(mResolutionList.size() - 1).x;
        }
        Log.i(TAG, "resolutions=" + mResolutionList.size());
    }


    private SpannableString getResolutionSpannable(int width, int height) {
        String resolutionString = width + " x " + height;
        String accessibleText = mResources.getString(
                R.string.screen_resolution_delimiter_a11y, width, height);
        return Utils.createAccessibleSequence(resolutionString, accessibleText);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_resolution_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        updateIllustrationImage(mImagePreference);
        screen.addPreference(mImagePreference);

        final FooterPreference footerPreference = new FooterPreference(screen.getContext());
        footerPreference.setTitle(R.string.screen_resolution_footer);
        footerPreference.setSelectable(false);
        footerPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);
        screen.addPreference(footerPreference);
    }

    @Override
    public void bindPreferenceExtra(
            SelectorWithWidgetPreference pref,
            String key,
            CandidateInfo info,
            String defaultKey,
            String systemDefaultKey) {
        final ScreenResolutionCandidateInfo candidateInfo = (ScreenResolutionCandidateInfo) info;
        final CharSequence summary = candidateInfo.loadSummary();
        if (summary != null) pref.setSummary(summary);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<ScreenResolutionCandidateInfo> candidates = new ArrayList<>();

        for (int i = 0; i < mResolutionList.size(); i++) {
            Point p = mResolutionList.get(i);
            String key = p.x + "x" + p.y;
            String label = p.x + " x " + p.y;
            candidates.add(
                    new ScreenResolutionCandidateInfo(label, null, key, true /* enabled */));
        }

        return candidates;
    }

    /** Get current display mode. */
    @VisibleForTesting
    public Display.Mode getDisplayMode() {
        return mDefaultDisplay.getMode();
    }

    /** Using display manager to set the display mode. */
    @VisibleForTesting
    public void setDisplayMode(final int width, final int height) {
        mDisplayObserver.startObserve();
        ScreenResolutionExtensionsKt.setDisplayModeByWidth(getContext(), width, height);
    }

    /** Parse a "WxH" resolution key into a Point, or null if malformed. */
    @VisibleForTesting
    Point parseResolutionKey(String key) {
        if (key == null) {
            return null;
        }
        int idx = key.indexOf('x');
        if (idx <= 0 || idx >= key.length() - 1) {
            return null;
        }
        try {
            int width = Integer.parseInt(key.substring(0, idx));
            int height = Integer.parseInt(key.substring(idx + 1));
            return new Point(width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected String getDefaultKey() {
        Display.Mode mode = getDisplayMode();

        return mode.getPhysicalWidth() + "x" + mode.getPhysicalHeight();
    }

    @Override
    protected boolean setDefaultKey(final String key) {
        Point wh = parseResolutionKey(key);
        if (wh == null) {
            return false;
        }

        setDisplayMode(wh.x, wh.y);
        updateIllustrationImage(mImagePreference);

        return true;
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference selected) {
        String selectedKey = selected.getKey();
        Point wh = parseResolutionKey(selectedKey);
        if (wh == null) {
            return;
        }
        if (!mDisplayObserver.setPendingResolutionChange(wh.x, wh.y)) {
            return;
        }

        super.onRadioButtonClicked(selected);
    }

    /** Update the resolution image according display mode. */
    private void updateIllustrationImage(IllustrationPreference preference) {
        if (mResolutionList.isEmpty()) {
            return;
        }
        String key = getDefaultKey();
        Point min = mResolutionList.get(0);
        Point max = mResolutionList.get(mResolutionList.size() - 1);
        if (TextUtils.equals(min.x + "x" + min.y, key)) {
            preference.setLottieAnimationResId(R.raw.lottie_screen_resolution_high);
            preference.applyDynamicColor();
        } else {
            preference.setLottieAnimationResId(R.raw.lottie_screen_resolution_full);
            preference.applyDynamicColor();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SCREEN_RESOLUTION;
    }

    /** This is an extension of the CandidateInfo class, which adds summary information. */
    public static class ScreenResolutionCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        ScreenResolutionCandidateInfo(
                CharSequence label, CharSequence summary, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mSummary = summary;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        /** It is the summary for radio options. */
        public CharSequence loadSummary() {
            return mSummary;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.screen_resolution_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return ScreenResolutionExtensionsKt.checkSupportedResolutions(context);
                }
            };

    private static final class DisplayObserver implements DisplayManager.DisplayListener {
        private final @Nullable Context mContext;
        private AtomicInteger mPreviousWidth = new AtomicInteger(-1);

        DisplayObserver(Context context) {
            mContext = context;
        }

        public void startObserve() {
            if (mContext == null) {
                return;
            }

            final DisplayDensityUtils density = new DisplayDensityUtils(mContext);
            final int currentIndex = density.getCurrentIndex();

            if (currentIndex < 0
                    || density.getValues()[currentIndex] == density.getDefaultDensity()) {
                // The default density is in use, no custom density to reset.
                return;
            }

            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.registerDisplayListener(this, null);
        }

        public void stopObserve() {
            if (mContext == null) {
                return;
            }

            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            dm.unregisterDisplayListener(this);
        }

        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                return;
            }

            if (!isResolutionChangeApplied()) {
                return;
            }

            // Reset to the default density of the new resolution, so the DPI follows the
            // resolution switch instead of keeping the previous custom density.
            new DisplayDensityUtils(mContext).clearForcedDisplayDensity();
            stopObserve();
        }

        private int getCurrentWidth() {
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            return dm.getDisplay(Display.DEFAULT_DISPLAY).getMode().getPhysicalWidth();
        }

        private boolean setPendingResolutionChange(int selectedWidth, int selectedHeight) {
            final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
            final Display.Mode current = dm.getDisplay(Display.DEFAULT_DISPLAY).getMode();
            if (selectedWidth == current.getPhysicalWidth()
                    && selectedHeight == current.getPhysicalHeight()) {
                return false;
            }
            if (mPreviousWidth.get() != -1 && !isResolutionChangeApplied()) {
                return false;
            }

            mPreviousWidth.set(current.getPhysicalWidth());

            return true;
        }

        private boolean isResolutionChangeApplied() {
            if (mPreviousWidth.get() == getCurrentWidth()) {
                return false;
            }

            Log.i(TAG,
                    "resolution changed from " + mPreviousWidth.get() + " to " + getCurrentWidth());
            return true;
        }
    }
}
// LINT.ThenChange(ScreenResolutionApiScreen.kt, ScreenResolutionExtensions.kt)
