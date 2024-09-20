/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.UUID;

import org.lineageos.lineageparts.PartsActivity;
import org.lineageos.lineageparts.PreferenceControllerMixin;
import org.lineageos.lineageparts.R;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;
import lineageos.providers.LineageSettings;

public class ProfilesPreference extends AbstractPreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, LifecycleObserver,
        OnResume, OnPause, PreferenceControllerMixin {

    private final String KEY = "profiles_preference";

    private final Context mContext;

    private static final int PROFILE_DETAILS = 1;

    private ProfileChangeReceiver mProfileChangeReceiver;
    private ProfileManager mProfileManager;
    private SettingObserver mSettingObserver;

    PreferenceScreen mScreen;

    private int nextPreferenceOrder = 0;

    public ProfilesPreference(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mProfileManager = ProfileManager.getInstance(mContext);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        refreshList();
        mProfileChangeReceiver = new ProfileChangeReceiver();
        mSettingObserver = new SettingObserver(screen);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProfileManager.INTENT_ACTION_PROFILE_SELECTED);
        filter.addAction(ProfileManager.INTENT_ACTION_PROFILE_UPDATED);
        if (mProfileChangeReceiver != null) {
            mContext.registerReceiver(mProfileChangeReceiver, filter);
        }
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false, null);
        }
        refreshList();
    }

    @Override
    public void onPause() {
        if (mProfileChangeReceiver != null) {
            mContext.unregisterReceiver(mProfileChangeReceiver);
        }
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        String selectedKey = preference.getKey();
        setSelectedProfile(selectedKey);
        if (mScreen != null) {
            for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
                Preference pref = mScreen.getPreference(i);
                if (pref instanceof SelectorWithWidgetPreference) {
                    updateState(pref);
                }
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        int profilesEnabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.SYSTEM_PROFILES_ENABLED, 1);

        // Get active profile, if null
        Profile prof = mProfileManager.getActiveProfile();
        String selectedKey = prof != null ? prof.getUuid().toString() : null;

        if (preference instanceof SelectorWithWidgetPreference) {
            SelectorWithWidgetPreference pref = (SelectorWithWidgetPreference) preference;
            if (profilesEnabled == 0) {
                pref.setEnabled(false);
                pref.setChecked(false);
            } else {
            pref.setEnabled(true);
                if (TextUtils.equals(selectedKey, pref.getKey())) {
                    pref.setChecked(true);
                } else {
                    pref.setChecked(false);
                }
            }
        }
    }
    
    public void refreshList() {
        for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
            Preference preference = mScreen.getPreference(i);
            if (preference instanceof SelectorWithWidgetPreference) {
                mScreen.removePreference(preference);
                i--; // Adjust index due to removal
            }
        }

        for (Profile profile : mProfileManager.getProfiles()) {
            Bundle args = new Bundle();
            args.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
            args.putBoolean(ProfilesSettings.EXTRA_NEW_PROFILE, false);
    
            SelectorWithWidgetPreference ppref = new SelectorWithWidgetPreference(mContext);
            ppref.setKey(profile.getUuid().toString());
            ppref.setTitle(profile.getName());
            ppref.setOnClickListener(this);
            ppref.setExtraWidgetOnClickListener(v -> startProfileConfigActivity(args));
    
            // Update the state of the preference based on the active profile
            updateState(ppref);
    
            // Set the order of the new preference based on the main switch
            if (nextPreferenceOrder == 0) {
                Preference mainSwitch = mScreen.findPreference("mProfilesSettingsMainSwitch");
                if (mainSwitch != null) {
                    nextPreferenceOrder = mainSwitch.getOrder() + 1; // Start order after the switch
                } else {
                    nextPreferenceOrder = 1; // Fallback if the switch isn't found
                }
            }
    
            // Set the order and add the new preference
            ppref.setOrder(nextPreferenceOrder);
            nextPreferenceOrder++;
    
            mScreen.addPreference(ppref); 
        }
    }
    
    private void startProfileConfigActivity(Bundle args) {
        if (mContext instanceof PartsActivity) {
            PartsActivity pa = (PartsActivity) mContext;
            pa.startPreferencePanel(SetupActionsFragment.class.getCanonicalName(), args,
                    R.string.profile_profile_manage, null, null, PROFILE_DETAILS);
        }
    }

    private void setSelectedProfile(String key) {
        try {
            UUID selectedUuid = UUID.fromString(key);
            mProfileManager.setActiveProfile(selectedUuid);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    public class ProfileChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ProfileManager.INTENT_ACTION_PROFILE_SELECTED.equals(action)
                    || ProfileManager.INTENT_ACTION_PROFILE_UPDATED.equals(intent.getAction())) {
                if (mScreen != null) {
                    for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
                        Preference pref = mScreen.getPreference(i);
                        updateState(pref);
                    }
                }
            }
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Uri URI_SYSTEM_PROFILES_ENABLED = 
                LineageSettings.System.getUriFor(LineageSettings.System.SYSTEM_PROFILES_ENABLED);

        private final PreferenceScreen mScreen;

        SettingObserver(PreferenceScreen screen) {
            super(Handler.getMain());
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
                if (mScreen != null) {
                    for (int i = 0; i < mScreen.getPreferenceCount(); i++) {
                        Preference pref = mScreen.getPreference(i);
                        updateState(pref);
                    }
                }
            }
        }
    }
}
