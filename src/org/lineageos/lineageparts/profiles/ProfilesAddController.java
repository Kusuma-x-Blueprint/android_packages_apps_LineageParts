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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.PreferenceControllerMixin;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

import lineageos.app.Profile;
import lineageos.providers.LineageSettings;

public class ProfilesAddController extends AbstractPreferenceController
        implements LifecycleObserver, OnResume, OnPause, PreferenceControllerMixin {
    private static final String KEY = "profiles_preference_add";
    private final Context mContext;

    private Preference mPreference;
    private SettingObserver mSettingObserver;

    public ProfilesAddController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference pref = screen.findPreference(getPreferenceKey());
        if (isAvailable()) {
            mPreference = pref;
            mPreference.setOnPreferenceClickListener((p) -> {
                addProfile();
                return true;
            });
            mSettingObserver = new SettingObserver(mPreference);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    @Override
    public void updateState(Preference preference) {
        int profilesEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1);
        mPreference.setEnabled(profilesEnabled != 0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private void addProfile() {
        Bundle args = new Bundle();
        args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, true);
        args.putParcelable(ProfilesSettings.EXTRA_PROFILE, new Profile(mContext.getString(R.string.new_profile_name)));

        PartsActivity pa = (PartsActivity) mContext;
        pa.startPreferencePanel(SetupTriggersFragment.class.getCanonicalName(), args,
                0, null, null, 0);
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_SYSTEM_PROFILES_ENABLED = 
                LineageSettings.System.getUriFor(LineageSettings.System.SYSTEM_PROFILES_ENABLED);

        private final Preference mPref;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPref = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(URI_SYSTEM_PROFILES_ENABLED, false, this);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null || URI_SYSTEM_PROFILES_ENABLED.equals(uri)) {
                updateState(mPref);
            }
        }
    }
}
