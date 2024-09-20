/*
 * Copyright (C) 2019 The Android Open Source Project
 *               2024 Kusuma
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

package org.lineageos.lineageparts.profiles;

import android.content.Context;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import org.lineageos.lineageparts.PreferenceControllerMixin;
import org.lineageos.lineageparts.R;

import lineageos.providers.LineageSettings;

public class ProfilesMainSwitchController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnMainSwitchChangeListener {

    private static final String KEY = "profiles_preference_main_switch";
    private final Context mContext;

    MainSwitchPreference mSwitch;

    public ProfilesMainSwitchController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            Preference pref = screen.findPreference(getPreferenceKey());
            if (pref != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    int profilesEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                            LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1);
                    boolean isChecked = profilesEnabled != 0;
                    LineageSettings.System.putInt(mContext.getContentResolver(),
                            LineageSettings.System.SYSTEM_PROFILES_ENABLED, isChecked ? 0 : 1);
                    return true;
                });
                mSwitch = (MainSwitchPreference) pref;
                mSwitch.addOnSwitchChangeListener(this);
                updateState(mSwitch);
            }
        }
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.updateStatus(isChecked);
        }
    }

    @Override
    public void updateState(Preference preference) {
        int profilesEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1);
        setChecked(profilesEnabled != 0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        int enableProfile = isChecked ? 1 : 0;
        LineageSettings.System.putInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, enableProfile);
    }
}
