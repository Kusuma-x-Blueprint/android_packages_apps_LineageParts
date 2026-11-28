/*
 * SPDX-FileCopyrightText: 2014-2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.input;

import static org.lineageos.internal.util.PowerMenuConstants.*;

import android.Manifest;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.controls.ControlsProviderService;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.util.EmergencyAffordanceManager;
import com.android.settingslib.applications.ServiceListing;

import lineageos.app.LineageGlobalActions;

import org.lineageos.internal.util.PowerMenuConstants;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.utils.TelephonyUtils;

import java.util.List;

public class PowerMenuActions extends SettingsPreferenceFragment {
    final static String TAG = "PowerMenuActions";

    private SwitchPreferenceCompat mScreenshotPref;
    private SwitchPreferenceCompat mAirplanePref;
    private SwitchPreferenceCompat mUsersPref;
    private SwitchPreferenceCompat mBugReportPref;
    private SwitchPreferenceCompat mEmergencyPref;
    private SwitchPreferenceCompat mDeviceControlsPref;

    private LineageGlobalActions mLineageGlobalActions;

    private EmergencyAffordanceManager mEmergencyAffordanceManager;
    private boolean mForceEmergCheck = false;

    Context mContext;
    private UserManager mUserManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);
        requireActivity().setTitle(R.string.power_menu_title);
        mContext = requireActivity().getApplicationContext();
        mUserManager = UserManager.get(mContext);
        mLineageGlobalActions = LineageGlobalActions.getInstance(mContext);
        mEmergencyAffordanceManager = new EmergencyAffordanceManager(mContext);

        for (String action : PowerMenuConstants.getAllActions()) {
            if (action.equals(GLOBAL_ACTION_KEY_SCREENSHOT)) {
                mScreenshotPref = findPreference(GLOBAL_ACTION_KEY_SCREENSHOT);
            } else if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
            } else if (action.equals(GLOBAL_ACTION_KEY_USERS)) {
                mUsersPref = findPreference(GLOBAL_ACTION_KEY_USERS);
            } else if (action.equals(GLOBAL_ACTION_KEY_BUGREPORT)) {
                mBugReportPref = findPreference(GLOBAL_ACTION_KEY_BUGREPORT);
            } else if (action.equals(GLOBAL_ACTION_KEY_EMERGENCY)) {
                mEmergencyPref = findPreference(GLOBAL_ACTION_KEY_EMERGENCY);
            } else if (action.equals(GLOBAL_ACTION_KEY_DEVICECONTROLS)) {
                mDeviceControlsPref = findPreference(GLOBAL_ACTION_KEY_DEVICECONTROLS);
            }
        }

        if (!TelephonyUtils.isVoiceCapable(requireActivity())) {
            getPreferenceScreen().removePreference(mEmergencyPref);
            mEmergencyPref = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mScreenshotPref != null) {
            mScreenshotPref.setChecked(mLineageGlobalActions.userConfigContains(
                    GLOBAL_ACTION_KEY_SCREENSHOT));
        }

        if (mAirplanePref != null) {
            mAirplanePref.setChecked(mLineageGlobalActions.userConfigContains(
                    GLOBAL_ACTION_KEY_AIRPLANE));
        }

        if (mUsersPref != null) {
            if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
                getPreferenceScreen().removePreference(mUsersPref);
                mUsersPref = null;
            } else {
                List<UserInfo> users = mUserManager.getUsers();
                boolean enabled = (users.size() > 1);
                mUsersPref.setVisible(enabled);
                mUsersPref.setChecked(mLineageGlobalActions.userConfigContains(
                        GLOBAL_ACTION_KEY_USERS) && enabled);
            }
        }

        if (mBugReportPref != null) {
            mBugReportPref.setChecked(mLineageGlobalActions.userConfigContains(
                    GLOBAL_ACTION_KEY_BUGREPORT));
        }

        if (mEmergencyPref != null) {
            mForceEmergCheck = mEmergencyAffordanceManager.needsEmergencyAffordance();
            mEmergencyPref.setChecked(mLineageGlobalActions.userConfigContains(
                    GLOBAL_ACTION_KEY_EMERGENCY) || mForceEmergCheck);
            mEmergencyPref.setEnabled(!mForceEmergCheck);
        }

        if (mDeviceControlsPref != null) {
            mDeviceControlsPref.setChecked(mLineageGlobalActions.userConfigContains(
                    GLOBAL_ACTION_KEY_DEVICECONTROLS));

            // Show preference if any device control app is installed
            ServiceListing serviceListing = new ServiceListing.Builder(mContext)
                    .setIntentAction(ControlsProviderService.SERVICE_CONTROLS)
                    .setPermission(Manifest.permission.BIND_CONTROLS)
                    .setNoun("Controls Provider")
                    .setSetting("controls_providers")
                    .setTag("controls_providers")
                    .build();
            serviceListing.addCallback(
                    services -> mDeviceControlsPref.setVisible(!services.isEmpty()));
            serviceListing.reload();
        }

        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;

        if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_SCREENSHOT);

        } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mUsersPref) {
            value = mUsersPref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mBugReportPref) {
            value = mBugReportPref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_BUGREPORT);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.BUGREPORT_IN_POWER_MENU, value ? 1 : 0);

        } else if (preference == mEmergencyPref) {
            value = mEmergencyPref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_EMERGENCY);

        } else if (preference == mDeviceControlsPref) {
            value = mDeviceControlsPref.isChecked();
            mLineageGlobalActions.updateUserConfig(value, GLOBAL_ACTION_KEY_DEVICECONTROLS);

        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void updatePreferences() {
        UserInfo currentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        boolean developmentSettings = Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        boolean bugReport = Settings.Global.getInt(
                getContentResolver(), Settings.Global.BUGREPORT_IN_POWER_MENU, 0) == 1;
        boolean isPrimaryUser = currentUser == null || currentUser.isPrimary();
        if (mBugReportPref != null) {
            boolean showBugReport = developmentSettings && isPrimaryUser;
            mBugReportPref.setVisible(showBugReport);
            if (showBugReport) {
                mBugReportPref.setChecked(bugReport);
            }
        }
        if (mEmergencyPref != null) {
            if (mForceEmergCheck) {
                mEmergencyPref.setSummary(R.string.power_menu_emergency_affordance_enabled);
            } else {
                mEmergencyPref.setSummary(null);
            }
        }
    }
}
