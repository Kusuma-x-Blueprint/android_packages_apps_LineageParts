/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.PreferenceControllerFragment;

import lineageos.app.Profile;
import lineageos.app.ProfileManager;

public class ProfilesSettings extends PreferenceControllerFragment {
    private static final String TAG = "ProfilesSettings";

    public static final String EXTRA_PROFILE = "Profile";
    public static final String EXTRA_NEW_PROFILE = "new_profile_mode";

    private ProfilesPreference mProfilesPreference;
    private ProfilesMenuController mMenuController;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.profiles_settings;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMenuController.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        mProfilesPreference = new ProfilesPreference(context, lifecycle);
        controllers.add(mProfilesPreference);
        mMenuController = new ProfilesMenuController(context, lifecycle, mProfilesPreference);
        controllers.add(mMenuController);
        controllers.add(new ProfilesMainSwitchController(context));
        controllers.add(new ProfilesAddController(context, lifecycle));
        return controllers;
    }

    public static final SummaryProvider SUMMARY_PROVIDER = (context, key) -> {
        ProfileManager pm = ProfileManager.getInstance(context);
        if (!pm.isProfilesEnabled()) {
            return context.getString(R.string.profile_settings_summary_off);
        }

        Profile p = pm.getActiveProfile();
        return p != null ? p.getName() : null;
    };
}
