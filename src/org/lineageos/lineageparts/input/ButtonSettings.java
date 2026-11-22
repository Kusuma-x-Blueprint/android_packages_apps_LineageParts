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
import org.lineageos.lineageparts.utils.TelephonyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ButtonSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {
    private static final String TAG = "SystemSettings";

    private static final String KEY_BACK_WAKE_SCREEN = "back_wake_screen";
    private static final String KEY_CAMERA_LAUNCH = "camera_launch";
    private static final String KEY_CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";
    private static final String KEY_CAMERA_WAKE_SCREEN = "camera_wake_screen";
    private static final String KEY_HOME_WAKE_SCREEN = "home_wake_screen";
    private static final String KEY_MENU_WAKE_SCREEN = "menu_wake_screen";
    private static final String KEY_ASSIST_WAKE_SCREEN = "assist_wake_screen";
    private static final String KEY_APP_SWITCH_WAKE_SCREEN = "app_switch_wake_screen";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_PANEL_POSITION = "volume_panel_on_left";
    private static final String KEY_VOLUME_WAKE_SCREEN = "volume_wake_screen";
    private static final String KEY_VOLUME_ANSWER_CALL = "volume_answer_call";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_VOLUME_MUSIC_CONTROLS = "volbtn_music_controls";
    private static final String KEY_TORCH_LONG_PRESS_POWER_GESTURE =
            "torch_long_press_power_gesture";
    private static final String KEY_TORCH_LONG_PRESS_POWER_TIMEOUT =
            "torch_long_press_power_timeout";
    private static final String KEY_CLICK_PARTIAL_SCREENSHOT =
            "click_partial_screenshot";
    public static final String KEY_SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_CAMERA = "camera_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_EXTRAS = "extras_category";

    private SwitchPreferenceCompat mCameraWakeScreen;
    private SwitchPreferenceCompat mCameraSleepOnRelease;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreferenceCompat mSwapVolumeButtons;
    private SwitchPreferenceCompat mPowerEndCall;
    private SwitchPreferenceCompat mHomeAnswerCall;
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
        final boolean hasHomeKey = DeviceUtils.hasHomeKey(getActivity());
        final boolean hasBackKey = DeviceUtils.hasBackKey(getActivity());
        final boolean hasMenuKey = DeviceUtils.hasMenuKey(getActivity());
        final boolean hasAssistKey = DeviceUtils.hasAssistKey(getActivity());
        final boolean hasAppSwitchKey = DeviceUtils.hasAppSwitchKey(getActivity());
        final boolean hasCameraKey = DeviceUtils.hasCameraKey(getActivity());
        final boolean hasVolumeKeys = DeviceUtils.hasVolumeKeys(getActivity());

        final boolean showHomeWake = DeviceUtils.canWakeUsingHomeKey(getActivity());
        final boolean showBackWake = DeviceUtils.canWakeUsingBackKey(getActivity());
        final boolean showMenuWake = DeviceUtils.canWakeUsingMenuKey(getActivity());
        final boolean showAssistWake = DeviceUtils.canWakeUsingAssistKey(getActivity());
        final boolean showAppSwitchWake = DeviceUtils.canWakeUsingAppSwitchKey(getActivity());
        final boolean showCameraWake = DeviceUtils.canWakeUsingCameraKey(getActivity());
        final boolean showVolumeWake = DeviceUtils.canWakeUsingVolumeKeys(getActivity());

        final PreferenceCategory powerCategory = prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory homeCategory = prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory backCategory = prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory menuCategory = prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory volumeCategory = prefScreen.findPreference(CATEGORY_VOLUME);
        final PreferenceCategory cameraCategory = prefScreen.findPreference(CATEGORY_CAMERA);
        final PreferenceCategory extrasCategory = prefScreen.findPreference(CATEGORY_EXTRAS);

        // Power button ends calls.
        mPowerEndCall = findPreference(KEY_POWER_END_CALL);

        // Long press power while display is off to activate torchlight
        SwitchPreferenceCompat torchLongPressPowerGesture =
                findPreference(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
        final int torchLongPressPowerTimeout = LineageSettings.System.getInt(resolver,
                LineageSettings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, 0);
        mTorchLongPressPowerTimeout = initList(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT,
                torchLongPressPowerTimeout);

        // Home button answers calls.
        mHomeAnswerCall = findPreference(KEY_HOME_ANSWER_CALL);

        mHandler = new Handler(Looper.getMainLooper());

        if (hasPowerKey) {
            if (!TelephonyUtils.isVoiceCapable(requireActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
            if (!DeviceUtils.deviceSupportsFlashLight(requireActivity())) {
                powerCategory.removePreference(torchLongPressPowerGesture);
                powerCategory.removePreference(mTorchLongPressPowerTimeout);
            }
        }
        if (!hasPowerKey || powerCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(powerCategory);
        }

        if (hasHomeKey) {
            if (!showHomeWake) {
                homeCategory.removePreference(findPreference(KEY_HOME_WAKE_SCREEN));
            }

            if (!TelephonyUtils.isVoiceCapable(requireActivity())) {
                homeCategory.removePreference(mHomeAnswerCall);
                mHomeAnswerCall = null;
            }
        }
        if (!hasHomeKey || homeCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(homeCategory);
        }

        if (hasBackKey) {
            if (!showBackWake) {
                backCategory.removePreference(findPreference(KEY_BACK_WAKE_SCREEN));
            }
        }
        if (!hasBackKey || backCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(backCategory);
        }

        if (hasMenuKey) {
            if (!showMenuWake) {
                menuCategory.removePreference(findPreference(KEY_MENU_WAKE_SCREEN));
            }
        }
        if (!hasMenuKey || menuCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(menuCategory);
        }

        if (hasAssistKey) {
            if (!showAssistWake) {
                assistCategory.removePreference(findPreference(KEY_ASSIST_WAKE_SCREEN));
            }
        }
        if (!hasAssistKey || assistCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(assistCategory);
        }

        if (hasAppSwitchKey) {
            if (!showAppSwitchWake) {
                appSwitchCategory.removePreference(findPreference(KEY_APP_SWITCH_WAKE_SCREEN));
            }
        }
        if (!hasAppSwitchKey || appSwitchCategory.getPreferenceCount() == 0) {
            prefScreen.removePreference(appSwitchCategory);
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
            if (!showVolumeWake) {
                volumeCategory.removePreference(findPreference(KEY_VOLUME_WAKE_SCREEN));
            }

            if (!TelephonyUtils.isVoiceCapable(requireActivity())) {
                volumeCategory.removePreference(findPreference(KEY_VOLUME_ANSWER_CALL));
            }

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = LineageSettings.System.getInt(resolver,
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            if (mSwapVolumeButtons != null) {
                mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
            }
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

        SwitchPreferenceCompat volumeWakeScreen = findPreference(KEY_VOLUME_WAKE_SCREEN);
        SwitchPreferenceCompat volumeMusicControls = findPreference(KEY_VOLUME_MUSIC_CONTROLS);

        if (volumeWakeScreen != null) {
            if (volumeMusicControls != null) {
                volumeMusicControls.setDependency(KEY_VOLUME_WAKE_SCREEN);
                volumeWakeScreen.setDisableDependentsState(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = LineageSettings.Secure.getInt(getContentResolver(),
                    LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }

        final boolean navbarForced = isNavbarForced(getActivity());
        enablePreference(mHomeAnswerCall, !navbarForced);
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

    private void enablePreference(Preference pref, boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled);
        }
    }

    private static boolean isNavbarForced(Context context) {
        return LineageSettings.System.getIntForUser(
                context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
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

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value;

            if (mSwapVolumeButtons.isChecked()) {
                /* The native inputflinger service uses the same logic of:
                 *   1 - the volume rocker is on one the sides, relative to the natural
                 *       orientation of the display (true for all phones and most tablets)
                 *   2 - the volume rocker is on the top or bottom, relative to the
                 *       natural orientation of the display (true for some tablets)
                 */
                value = getResources().getInteger(
                        R.integer.config_volumeRockerVsDisplayOrientation);
            } else {
                /* Disable the re-orient functionality */
                value = 0;
            }
            LineageSettings.System.putInt(requireActivity().getContentResolver(),
                    LineageSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        LineageSettings.Secure.putInt(getContentResolver(),
                LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : LineageSettings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (!TelephonyUtils.isVoiceCapable(context)) {
                result.add(KEY_POWER_END_CALL);
                result.add(KEY_HOME_ANSWER_CALL);
                result.add(KEY_VOLUME_ANSWER_CALL);
            }

            if (!DeviceUtils.hasBackKey(context)) {
                result.add(CATEGORY_BACK);
                result.add(KEY_BACK_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingHomeKey(context)) {
                result.add(KEY_BACK_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasHomeKey(context)) {
                result.add(CATEGORY_HOME);
                result.add(KEY_HOME_ANSWER_CALL);
                result.add(KEY_HOME_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingHomeKey(context)) {
                result.add(KEY_HOME_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasMenuKey(context)) {
                result.add(CATEGORY_MENU);
                result.add(KEY_MENU_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingMenuKey(context)) {
                result.add(KEY_MENU_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasAssistKey(context)) {
                result.add(CATEGORY_ASSIST);
                result.add(KEY_ASSIST_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingAssistKey(context)) {
                result.add(KEY_ASSIST_WAKE_SCREEN);
            }

            if (!DeviceUtils.hasAppSwitchKey(context)) {
                result.add(CATEGORY_APPSWITCH);
                result.add(KEY_APP_SWITCH_WAKE_SCREEN);
            } else if (!DeviceUtils.canWakeUsingAppSwitchKey(context)) {
                result.add(KEY_APP_SWITCH_WAKE_SCREEN);
            }

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
                result.add(KEY_SWAP_VOLUME_BUTTONS);
                result.add(KEY_VOLUME_ANSWER_CALL);
                result.add(KEY_VOLUME_KEY_CURSOR_CONTROL);
                result.add(KEY_VOLUME_MUSIC_CONTROLS);
                result.add(KEY_VOLUME_PANEL_POSITION);
                result.add(KEY_VOLUME_WAKE_SCREEN);
                result.add(KEY_CLICK_PARTIAL_SCREENSHOT);
            } else if (!DeviceUtils.canWakeUsingVolumeKeys(context)) {
                result.add(KEY_VOLUME_WAKE_SCREEN);
            }

            if (!DeviceUtils.deviceSupportsFlashLight(context)) {
                result.add(KEY_TORCH_LONG_PRESS_POWER_GESTURE);
                result.add(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT);
            }

            return result;
        }
    };
}
