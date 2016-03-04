/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.System.DOUBLE_TAP_SLEEP_GESTURE;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HazySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "HazySettings";

    private static final String DOUBLE_TAP_SLEEP_GESTURE = "double_tap_sleep_gesture";
    private static final String SMART_PULLDOWN = "smart_pulldown";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "status_bar_quick_qs_pulldown";
    private static final String SWITCH_LAST_APP = "switch_last_app";
    private static final String VOLBTN_MUSIC_CONTROLS = "volbtn_music_controls";

    private WarnedListPreference mFontSizePref;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mQuickPulldown;
    private ListPreference mSmartPulldown;

    private SwitchPreference mTapToSleepPreference;
    private SwitchPreference mSwitchLastApp;
    private SwitchPreference mVolBtnMusicControls;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.hazy_settings);

        mTapToSleepPreference = (SwitchPreference) findPreference(DOUBLE_TAP_SLEEP_GESTURE);
        mSwitchLastApp = (SwitchPreference) findPreference(SWITCH_LAST_APP);
        mVolBtnMusicControls = (SwitchPreference) findPreference(VOLBTN_MUSIC_CONTROLS);

        // Quick pulldown
        mQuickPulldown = (ListPreference) findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        int quickPulldown = Settings.System.getInt(resolver,
            Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 0);
        mQuickPulldown.setValue(String.valueOf(quickPulldown));
        mQuickPulldown.setSummary(mQuickPulldown.getEntry());
        mQuickPulldown.setOnPreferenceChangeListener(this);

        mSmartPulldown = (ListPreference) findPreference(SMART_PULLDOWN);
        mSmartPulldown.setOnPreferenceChangeListener(this);
        int smartPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_SMART_PULLDOWN, 0);
        mSmartPulldown.setValue(String.valueOf(smartPulldown));
        updateSmartPulldownSummary(smartPulldown);

    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        final ContentResolver resolver = getActivity().getContentResolver();

        // Update tap to sleep.
        if (mTapToSleepPreference != null) {
            mTapToSleepPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.DOUBLE_TAP_SLEEP_GESTURE, 0) == 1);
            mTapToSleepPreference.setOnPreferenceChangeListener(this);
        }

        // Update switch last app
        if (mSwitchLastApp != null) {
            mSwitchLastApp.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SWITCH_LAST_APP, 0) == 1);
            mSwitchLastApp.setOnPreferenceChangeListener(this);
        }

        // Update Volume button music controls
        if (mVolBtnMusicControls != null) {
            mVolBtnMusicControls.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VOLBTN_MUSIC_CONTROLS, 0) == 1);
            mVolBtnMusicControls.setOnPreferenceChangeListener(this);
        }       
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        final String key = preference.getKey();
        if (preference == mTapToSleepPreference) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), DOUBLE_TAP_SLEEP_GESTURE, value ? 1 : 0);
        }
        if (preference == mSwitchLastApp) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SWITCH_LAST_APP, value ? 1 : 0);
        }
        if (preference == mVolBtnMusicControls) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), VOLBTN_MUSIC_CONTROLS, value ? 1 : 0);
        }
        if (preference == mQuickPulldown) {
            int quickPulldown = Integer.valueOf((String) objValue);
            int index = mQuickPulldown.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN, quickPulldown);
            mQuickPulldown.setSummary(mQuickPulldown.getEntries()[index]);
        }
        if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();
        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.hazy_settings;
                    result.add(sir);

                    return result;
                }
            };
}
