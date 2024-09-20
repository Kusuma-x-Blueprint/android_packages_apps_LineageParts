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
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
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

import lineageos.app.Profile;
import lineageos.app.ProfileManager;
import lineageos.providers.LineageSettings;

public class ProfilesMenuController extends AbstractPreferenceController
        implements LifecycleObserver, OnResume, OnPause, PreferenceControllerMixin {
    private static final String KEY = "profiles_preference_menu";

    private static final int MENU_RESET = Menu.FIRST;

    private final Context mContext;

    private ProfileManager mProfileManager;
    private ProfilesPreference mProfilesPreference;
    private SettingObserver mSettingObserver;
    private MenuItem mResetMenuItem;
    private PreferenceScreen mScreen;

    public ProfilesMenuController(Context context, Lifecycle lifecycle,
            ProfilesPreference profilesPreference) {
        super(context);
        mContext = context;
        mProfilesPreference = profilesPreference;
        mProfileManager = ProfileManager.getInstance(mContext);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        mSettingObserver = new SettingObserver(mScreen);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mResetMenuItem = menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setAlphabeticShortcut('r');
        mResetMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        updateMenuState();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_RESET) {
            resetAll();
            return true;
        }
        return false;
    }

    private void resetAll() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.profile_reset_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.profile_reset_message)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    mProfileManager.resetAll();
                    mProfileManager.setActiveProfile(mProfileManager.getActiveProfile().getUuid());
                    dialog.dismiss();
                    mProfilesPreference.refreshList();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public void updateMenuState() {
        int profilesEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1);
        if (mResetMenuItem != null) {
            mResetMenuItem.setEnabled(profilesEnabled != 0);
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
        // No action needed here.
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_SYSTEM_PROFILES_ENABLED = 
                LineageSettings.System.getUriFor(LineageSettings.System.SYSTEM_PROFILES_ENABLED);

        private final PreferenceScreen mScreen;

        SettingObserver(PreferenceScreen screen) {
            super(new Handler());
            this.mScreen = screen;
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
                updateMenuState();
            }
        }
    }
}
