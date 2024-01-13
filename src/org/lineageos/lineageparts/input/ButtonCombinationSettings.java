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

import android.os.Bundle;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;

public class ButtonCombinationSettings extends SettingsPreferenceFragment 
        implements Searchable {

    private static final String TAG = "ButtonCombinationSettings";

    private static final String CUSTOM_GESTURE_ACTION_SCREENSHOT = "screenshot";
    private static final String KEY_PARTIAL_SCRENSHOT = "click_partial_screenshot";

    private String mPowerVolDownAction;

    private SwitchPreference mPartialScreenshot;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.button_combination_settings);

        mPowerVolDownAction = Settings.Secure.getStringForUser(
                resolver, Settings.Secure.POWER_VOL_DOWN_ACTION, UserHandle.USER_CURRENT);

        mPartialScreenshot = findPreference(KEY_PARTIAL_SCREENSHOT);

        updatePartialScreenshot();
    }

    private void updatePartialScreenshot() {
        if (isPowerVolDownScreenshot) {
            mPartialScreenshot.setVisible(true)
        } else {
            mPartialScreenshot.setVisible(false)
        }
    }

    private static boolean isPowerVolDownScreenshot(Context context) {
        return mPowerVolDownAction != null
            && CUSTOM_GESTURE_ACTION_SCREENSHOT.equals(mPowerVolDownAction);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPowerVolDownAction) {
            updatePartialScreenshot();
            return true;
        }
    return false;
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (isPowerVolDownScreenshot) {
                result.add(KEY_PARTIAL_SCRENSHOT);
            }
        }
    }
}
