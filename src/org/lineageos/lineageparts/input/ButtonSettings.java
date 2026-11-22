/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod project
 * SPDX-FileCopyrightText: The LineageOS project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;
import org.lineageos.lineageparts.utils.GenericUtils;
import lineageos.preference.ConstraintsHelper;

import java.util.Set;

public class ButtonSettings extends SettingsPreferenceFragment implements Searchable {

    public static final String ACTION_ADDITIONAL_BUTTONS_SETTINGS =
            "org.lineageos.settings.device.ADDITIONAL_BUTTONS_SETTINGS";

    private static final String KEY_CAMERA_LAUNCH = "camera_launch";
    private static final String KEY_CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";
    private static final String KEY_CAMERA_WAKE_SCREEN = "camera_wake_screen";
    private static final String KEY_ADDITIONAL_BUTTONS = "additional_buttons";
    public static final String KEY_SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";

    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_EXTRAS = "extras_category";

    private SwitchPreferenceCompat mCameraWakeScreen;
    private SwitchPreferenceCompat mCameraSleepOnRelease;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isSupported(getActivity())) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final boolean hasCameraKey = DeviceUtils.hasCameraKey(getActivity());
        final boolean showCameraWake = DeviceUtils.canWakeUsingCameraKey(getActivity());

        final PreferenceCategory cameraCategory = prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory extrasCategory = prefScreen.findPreference(CATEGORY_EXTRAS);

        if (hasCameraKey) {
            mCameraWakeScreen = findPreference(KEY_CAMERA_WAKE_SCREEN);
            mCameraSleepOnRelease = findPreference(KEY_CAMERA_SLEEP_ON_RELEASE);

            if (!showCameraWake) {
                cameraCategory.removePreference(mCameraWakeScreen);
            }
            // Only show 'Camera sleep on release' if the device has a focus key
            if (res.getBoolean(
                    org.lineageos.platform.internal.R.bool.config_singleStageCameraKey)) {
                cameraCategory.removePreference(mCameraSleepOnRelease);
            }
        }
        if (!hasCameraKey || cameraCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(cameraCategory);
        }

        if (!hasAdditionalButtons(getActivity()) || extrasCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(extrasCategory);
        }

        if (mCameraWakeScreen != null) {
            if (mCameraSleepOnRelease != null && !res.getBoolean(
                    org.lineageos.platform.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setDependency(KEY_CAMERA_WAKE_SCREEN);
            }
        }
    }

    public static boolean hasAdditionalButtons(Context context) {
        return ConstraintsHelper.resolveIntent(context, ACTION_ADDITIONAL_BUTTONS_SETTINGS);
    }

    public static boolean isSupported(Context context) {
        return DeviceUtils.hasCameraKey(context) || hasAdditionalButtons(context);
    }

    public static void restoreButtonSettings(Context context) {
        GenericUtils.setComponentEnabled(context, ButtonSettings.class.getName(),
                isSupported(context));
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean isKeySwapperSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_SWAP);
    }

    public static void restoreKeyDisabler(Context context) {
        if (!isKeyDisablerSupported(context)) {
            return;
        }

        boolean enabled = LineageSettings.System.getIntForUser(context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;

        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        hardware.set(LineageHardwareManager.FEATURE_KEY_DISABLE, enabled);
    }

    public static void restoreKeySwapper(Context context) {
        if (!isKeySwapperSupported(context)) {
            return;
        }

        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = Settings.Secure.getInt(context.getContentResolver(),
                KEY_SWAP_CAPACITIVE_KEYS,
                preferences.getBoolean(KEY_SWAP_CAPACITIVE_KEYS, false) ? 1 : 0) != 0;
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        hardware.set(LineageHardwareManager.FEATURE_KEY_SWAP, enabled);
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (!DeviceUtils.hasCameraKey(context)) {
                result.add(CATEGORY_CAMERA);
                result.add(KEY_CAMERA_LAUNCH);
                result.add(KEY_CAMERA_SLEEP_ON_RELEASE);
                result.add(KEY_CAMERA_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingCameraKey(context)) {
                result.add(KEY_CAMERA_WAKE_SCREEN);
            }

            if (!hasAdditionalButtons(context)) {
                result.add(CATEGORY_EXTRAS);
                result.add(KEY_ADDITIONAL_BUTTONS);
            }

            return result;
        }
    };
}
