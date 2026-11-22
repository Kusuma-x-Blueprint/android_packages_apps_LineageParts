/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod project
 * SPDX-FileCopyrightText: The LineageOS project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.input;

import static org.lineageos.internal.util.DeviceKeysConstants.*;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ButtonSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {
    private static final String TAG = "SystemSettings";

    private static final String KEY_CAMERA_LAUNCH = "camera_launch";
    private static final String KEY_CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";
    private static final String KEY_CAMERA_WAKE_SCREEN = "camera_wake_screen";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_TORCH_LONG_PRESS_POWER_GESTURE =
            "torch_long_press_power_gesture";
    private static final String KEY_TORCH_LONG_PRESS_POWER_TIMEOUT =
            "torch_long_press_power_timeout";
    private static final String KEY_CLICK_PARTIAL_SCREENSHOT =
            "click_partial_screenshot";
    public static final String KEY_SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_EXTRAS = "extras_category";

    private SwitchPreferenceCompat mCameraWakeScreen;
    private SwitchPreferenceCompat mCameraSleepOnRelease;
    private ListPreference mVolumeKeyCursorControl;
    private ListPreference mTorchLongPressPowerTimeout;

    private Handler mHandler;

    private LineageHardwareManager mHardware;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHardware = LineageHardwareManager.getInstance(getActivity());

        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = requireActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final boolean hasPowerKey = DeviceUtils.hasPowerKey();
        final boolean hasCameraKey = DeviceUtils.hasCameraKey(getActivity());
        final boolean hasVolumeKeys = DeviceUtils.hasVolumeKeys(getActivity());

        final boolean showCameraWake = DeviceUtils.canWakeUsingCameraKey(getActivity());

        final PreferenceCategory powerCategory = prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory volumeCategory = prefScreen.findPreference(CATEGORY_VOLUME);
        final PreferenceCategory cameraCategory = prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory extrasCategory = prefScreen.findPreference(CATEGORY_EXTRAS);

        // Long press power while display is off to activate torchlight
        SwitchPreferenceCompat torchLongPressPowerGesture =
                findPreference(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
        final int torchLongPressPowerTimeout = LineageSettings.System.getInt(resolver,
                LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, 0);
        mTorchLongPressPowerTimeout = initList(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT,
                torchLongPressPowerTimeout);

        mHandler = new Handler(Looper.getMainLooper());

        if (hasPowerKey) {
            if (!DeviceUtils.deviceSupportsFlashLight(requireActivity())) {
                powerCategory.removePreference(torchLongPressPowerGesture);
                powerCategory.removePreference(mTorchLongPressPowerTimeout);
            }
        }
        if (!hasPowerKey || powerCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(powerCategory);
        }

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

        if (hasVolumeKeys) {
            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);
        } else {
            extrasCategory.removePreference(findPreference(KEY_CLICK_PARTIAL_SCREENSHOT));
        }
        if (!hasVolumeKeys || volumeCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(volumeCategory);
        }

        if (mCameraWakeScreen != null) {
            if (mCameraSleepOnRelease != null && !res.getBoolean(
                    org.lineageos.platform.internal.R.bool.config_singleStageCameraKey)) {
                mCameraSleepOnRelease.setDependency(KEY_CAMERA_WAKE_SCREEN);
            }
        }
    }

    private ListPreference initList(String key, int value) {
        ListPreference list = getPreferenceScreen().findPreference(key);
        if (list == null) return null;
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        LineageSettings.System.putInt(getContentResolver(), setting, Integer.parseInt(value));
    }

    private void handleSystemListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);
        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.parseInt(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleSystemListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mTorchLongPressPowerTimeout) {
            handleListChange(mTorchLongPressPowerTimeout, newValue,
                    LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT);
            return true;
        }
        return false;
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

            if (!DeviceUtils.hasVolumeKeys(context)) {
                result.add(CATEGORY_VOLUME);
                result.add(KEY_VOLUME_KEY_CURSOR_CONTROL);
                result.add(KEY_VOLUME_MUSIC_CONTROLS);
                result.add(KEY_CLICK_PARTIAL_SCREENSHOT);
            }

            if (!DeviceUtils.deviceSupportsFlashLight(context)) {
                result.add(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
                result.add(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT);
            }

            return result;
        }
    };
}
