/*
 * Copyright (C) 2019 The Android Open Source Project
 * Copyright (C) 2023 Project Lineage Remix Open Source
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

package org.lineageos.lineageparts.input;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;

import java.util.Set;

public class ButtonCombinationSettings extends SettingsPreferenceFragment 
        implements Preference.OnPreferenceChangeListener, Searchable {

    private static final String CUSTOM_GESTURE_ACTION_SCREENSHOT = "screenshot";
    private static final String KEY_PARTIAL_SCREENSHOT = "click_partial_screenshot";
    private static final String KEY_POWER_VOL_DOWN = "power_vol_down_action";

    private String mPowerVolDownAction;

    private SwitchPreference mPartialScreenshot;
    private ListPreference mPowerVolDown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_combination_settings);

        final ContentResolver resolver = requireActivity().getContentResolver();

        mPowerVolDownAction = Settings.Secure.getStringForUser(
                resolver, Settings.Secure.POWER_VOL_DOWN_ACTION, UserHandle.USER_CURRENT);

        mPartialScreenshot = findPreference(KEY_PARTIAL_SCREENSHOT);
        mPowerVolDown = findPreference(KEY_POWER_VOL_DOWN);

        mPowerVolDown.setOnPreferenceChangeListener(this);;
        updatePartialScreenshot(getContext());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPowerVolDown) {
            updatePartialScreenshot(getContext());
            return true;
        }
    return false;
    }

    private void updatePartialScreenshot(Context context) {
        if (isPowerVolDownScreenshot(context)) {
            mPartialScreenshot.setVisible(true);
        } else {
            mPartialScreenshot.setVisible(false);
        }
    }

    private static boolean isPowerVolDownScreenshot(Context context) {
        String currentPowerVolDownAction = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.POWER_VOL_DOWN_ACTION, UserHandle.USER_CURRENT);

        return currentPowerVolDownAction != null
            && CUSTOM_GESTURE_ACTION_SCREENSHOT.equals(currentPowerVolDownAction);
    }


    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (!isPowerVolDownScreenshot(context)) {
                result.add(KEY_PARTIAL_SCREENSHOT);
            }
            return result;
        }
    };
}
