/*
 * SPDX-FileCopyrightText: 2014-2015 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.systemui;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import static com.android.systemui.shared.recents.utilities.Utilities.isLargeScreen;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;

import static org.lineageos.internal.util.DeviceKeysConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import lineageos.preference.LineageSystemSettingListPreference;
import lineageos.providers.LineageSettings;

public class SystemUITunerSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener{
    private static final String TAG = "SystemSettings";

    private static final String CATEGORY_BATTERY = "status_bar_battery_key";
    private static final String CATEGORY_CLOCK = "status_bar_clock_key";

    private static final String ICON_BLACKLIST = "icon_blacklist";

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";

    private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 2;

    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;

    private static final String NETWORK_TRAFFIC_SETTINGS = "network_traffic_settings";

    private static final String CATEGORY_NAVBAR = "navigation_bar_category";

    private static final String KEY_ENABLE_TASKBAR = "enable_taskbar";
    private static final String KEY_NAVIGATION_ARROW_KEYS = "navigation_bar_menu_arrow_keys";
    private static final String KEY_NAV_BAR_INVERSE = "sysui_nav_bar_inverse";
    private static final String KEY_NAVBAR_LAYOUT_VIEWS = "navbar_layout_views";
    private static final String KEY_NAVIGATION_BACK_LONG_PRESS = "navigation_back_long_press";
    private static final String KEY_NAVIGATION_HOME_LONG_PRESS = "navigation_home_long_press";
    private static final String KEY_NAVIGATION_HOME_DOUBLE_TAP = "navigation_home_double_tap";
    private static final String KEY_NAVIGATION_APP_SWITCH_LONG_PRESS =
            "navigation_app_switch_long_press";
    private static final String KEY_EDGE_LONG_SWIPE = "navigation_bar_edge_long_swipe";

    private LineageSystemSettingListPreference mQuickPulldown;
    private LineageSystemSettingListPreference mStatusBarClock;
    private LineageSystemSettingListPreference mStatusBarAmPm;
    private LineageSystemSettingListPreference mStatusBarBatteryShowPercent;

    private PreferenceCategory mStatusBarBatteryCategory;
    private PreferenceCategory mStatusBarClockCategory;

    private SwitchPreference mNavBarInverse;
    private ListPreference mNavBarLayoutViews;
    private SwitchPreference mNavigationArrowKeys;
    private SwitchPreference mEnableTaskbar;
    private ListPreference mNavigationBackLongPressAction;
    private ListPreference mNavigationHomeLongPressAction;
    private ListPreference mNavigationHomeDoubleTapAction;
    private ListPreference mNavigationAppSwitchLongPressAction;
    private ListPreference mEdgeLongSwipeAction;

    private PreferenceCategory mNavigationPreferencesCat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.system_ui_tuner_settings);

        final Resources res = getResources();
        final ContentResolver resolver = requireActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mStatusBarAmPm = findPreference(STATUS_BAR_AM_PM);
        mStatusBarClock = findPreference(STATUS_BAR_CLOCK_STYLE);
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mStatusBarClockCategory = getPreferenceScreen().findPreference(CATEGORY_CLOCK);

        mStatusBarBatteryShowPercent = findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        LineageSystemSettingListPreference statusBarBattery =
                findPreference(STATUS_BAR_BATTERY_STYLE);
        statusBarBattery.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(statusBarBattery.getIntValue(2));

        mStatusBarBatteryCategory = getPreferenceScreen().findPreference(CATEGORY_BATTERY);

        mQuickPulldown = findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));

        mNavigationPreferencesCat = findPreference(CATEGORY_NAVBAR);

        Action defaultBackLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnBackBehavior));
        Action defaultHomeLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnHomeBehavior));
        Action defaultHomeDoubleTapAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_doubleTapOnHomeBehavior));
        Action defaultAppSwitchLongPressAction = Action.fromIntSafe(res.getInteger(
                org.lineageos.platform.internal.R.integer.config_longPressOnAppSwitchBehavior));
        Action backLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_BACK_LONG_PRESS_ACTION,
                defaultBackLongPressAction);
        Action homeLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                defaultHomeLongPressAction);
        Action homeDoubleTapAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                defaultHomeDoubleTapAction);
        Action appSwitchLongPressAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                defaultAppSwitchLongPressAction);
        Action edgeLongSwipeAction = Action.fromSettings(resolver,
                LineageSettings.System.KEY_EDGE_LONG_SWIPE_ACTION,
                Action.NOTHING);

        // Navigation bar back long press
        mNavigationBackLongPressAction = initList(KEY_NAVIGATION_BACK_LONG_PRESS,
                backLongPressAction);

        // Navigation bar home long press
        mNavigationHomeLongPressAction = initList(KEY_NAVIGATION_HOME_LONG_PRESS,
                homeLongPressAction);

        // Navigation bar home double tap
        mNavigationHomeDoubleTapAction = initList(KEY_NAVIGATION_HOME_DOUBLE_TAP,
                homeDoubleTapAction);

        // Navigation bar app switch long press
        mNavigationAppSwitchLongPressAction = initList(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS,
                appSwitchLongPressAction);

        // Edge long swipe gesture
        mEdgeLongSwipeAction = initList(KEY_EDGE_LONG_SWIPE, edgeLongSwipeAction);

        boolean disableNavigationKeysEnabled = LineageSettings.System.getIntForUser(
                requireActivity().getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
        updateDisableNavkeysCategories(disableNavigationKeysEnabled, /* force */ true);

        mEnableTaskbar = findPreference(KEY_ENABLE_TASKBAR);
        if (mEnableTaskbar != null) {
            if (!isLargeScreen(requireContext()) || !hasNavigationBar()) {
                mNavigationPreferencesCat.removePreference(mEnableTaskbar);
            } else {
                mEnableTaskbar.setOnPreferenceChangeListener(this);
                mEnableTaskbar.setChecked(LineageSettings.System.getInt(resolver,
                        LineageSettings.System.ENABLE_TASKBAR,
                        isLargeScreen(requireContext()) ? 1 : 0) == 1);
                toggleTaskBarDependencies(mEnableTaskbar.isChecked());
            }
        }

        // Navigation bar arrow keys while typing
        mNavigationArrowKeys = findPreference(KEY_NAVIGATION_ARROW_KEYS);

        mNavBarInverse = findPreference(KEY_NAV_BAR_INVERSE);

        mNavBarLayoutViews = findPreference(KEY_NAVBAR_LAYOUT_VIEWS);

        List<Integer> unsupportedValues = new ArrayList<>();
        List<String> entries = new ArrayList<>(
                Arrays.asList(res.getStringArray(R.array.hardware_keys_action_entries)));
        List<String> values = new ArrayList<>(
                Arrays.asList(res.getStringArray(R.array.hardware_keys_action_values)));

        // hide split screen option unconditionally - it doesn't work at the moment
        // once someone gets it working again: hide it only for low-ram devices
        // (check ActivityManager.isLowRamDeviceStatic())
        unsupportedValues.add(Action.SPLIT_SCREEN.ordinal());

        for (int unsupportedValue: unsupportedValues) {
            entries.remove(unsupportedValue);
            values.remove(unsupportedValue);
        }

        String[] actionEntries = entries.toArray(new String[0]);
        String[] actionValues = values.toArray(new String[0]);

        mNavigationBackLongPressAction.setEntries(actionEntries);
        mNavigationBackLongPressAction.setEntryValues(actionValues);

        mNavigationHomeLongPressAction.setEntries(actionEntries);
        mNavigationHomeLongPressAction.setEntryValues(actionValues);

        mNavigationHomeDoubleTapAction.setEntries(actionEntries);
        mNavigationHomeDoubleTapAction.setEntryValues(actionValues);

        mNavigationAppSwitchLongPressAction.setEntries(actionEntries);
        mNavigationAppSwitchLongPressAction.setEntryValues(actionValues);

        mEdgeLongSwipeAction.setEntries(actionEntries);
        mEdgeLongSwipeAction.setEntryValues(actionValues);
    }

    @Override
    public void onResume() {
        super.onResume();

        final String curIconBlacklist = Settings.Secure.getString(getContext().getContentResolver(),
                ICON_BLACKLIST);

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "clock")) {
            getPreferenceScreen().removePreference(mStatusBarClockCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarClockCategory);
        }

        if (TextUtils.delimitedStringContains(curIconBlacklist, ',', "battery")) {
            getPreferenceScreen().removePreference(mStatusBarBatteryCategory);
        } else {
            getPreferenceScreen().addPreference(mStatusBarBatteryCategory);
        }

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        }

        final boolean disallowCenteredClock = DeviceUtils.hasCenteredCutout(getActivity());

        // Adjust status bar preferences for RTL
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (disallowCenteredClock) {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch_rtl);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch);
            } else {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_rtl);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values);
            }
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries_rtl);
        } else {
            if (disallowCenteredClock) {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries_notch);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values_notch);
            } else {
                mStatusBarClock.setEntries(R.array.status_bar_clock_position_entries);
                mStatusBarClock.setEntryValues(R.array.status_bar_clock_position_values);
            }
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries);
        }
    }

    private ListPreference initList(String key, Action value) {
        return initList(key, value.ordinal());
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        String key = preference.getKey();
        switch (key) {
            case STATUS_BAR_QUICK_QS_PULLDOWN:
                updateQuickPulldownSummary(value);
                break;
            case STATUS_BAR_CLOCK_STYLE:
                break;
            case STATUS_BAR_BATTERY_STYLE:
                enableStatusBarBatteryDependents(value);
                break;
            case KEY_NAVIGATION_BACK_LONG_PRESS:
                handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_BACK_LONG_PRESS_ACTION);
                break;
            case KEY_NAVIGATION_HOME_LONG_PRESS:
                handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_HOME_LONG_PRESS_ACTION);
                break;
            case KEY_NAVIGATION_HOME_DOUBLE_TAP:
                handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_HOME_DOUBLE_TAP_ACTION);
                break;
            case KEY_NAVIGATION_APP_SWITCH_LONG_PRESS:
                handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
                break;
            case KEY_EDGE_LONG_SWIPE:
                handleListChange((ListPreference) preference, newValue,
                    LineageSettings.System.KEY_EDGE_LONG_SWIPE_ACTION);
                break;
            case KEY_ENABLE_TASKBAR:
            toggleTaskBarDependencies((Boolean) newValue);
            if ((Boolean) newValue && is2ButtonNavigationEnabled(requireContext())) {
                // Let's switch to gestural mode if user previously had 2 buttons enabled.
                setButtonNavigationMode(NAV_BAR_MODE_GESTURAL_OVERLAY);
            }
            LineageSettings.System.putInt(getContentResolver(),
                    LineageSettings.System.ENABLE_TASKBAR, ((Boolean) newValue) ? 1 : 0);
                break;
        }
        return true;
    }

    private void enableStatusBarBatteryDependents(int batteryIconStyle) {
        mStatusBarBatteryShowPercent.setEnabled(batteryIconStyle != STATUS_BAR_BATTERY_STYLE_TEXT);
    }

    private void updateQuickPulldownSummary(int value) {
        String summary="";
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;

            case PULLDOWN_DIR_LEFT:
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary,
                    getResources().getString(
                        (value == PULLDOWN_DIR_LEFT) ^
                        (getResources().getConfiguration().getLayoutDirection()
                            == View.LAYOUT_DIRECTION_RTL)
                        ? R.string.status_bar_quick_qs_pulldown_summary_left
                        : R.string.status_bar_quick_qs_pulldown_summary_right));
                break;
        }
        mQuickPulldown.setSummary(summary);
    }

    private static boolean is2ButtonNavigationEnabled(Context context) {
        return NAV_BAR_MODE_2BUTTON == context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode);
    }

    private static void setButtonNavigationMode(String overlayPackage) {
        IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        try {
            overlayManager.setEnabledExclusiveInCategory(overlayPackage, UserHandle.USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void toggleTaskBarDependencies(boolean enabled) {
        enablePreference(mNavigationArrowKeys, !enabled);
        enablePreference(mNavBarInverse, !enabled);
        enablePreference(mNavigationBackLongPressAction, !enabled);
        enablePreference(mNavigationHomeLongPressAction, !enabled);
        enablePreference(mNavigationHomeDoubleTapAction, !enabled);
        enablePreference(mNavigationAppSwitchLongPressAction, !enabled);
    }

    private void enablePreference(Preference pref, boolean enabled) {
        if (pref != null) {
            pref.setEnabled(enabled);
        }
    }

    private void updateDisableNavkeysCategories(boolean navbarEnabled, boolean force) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Toggle hardkey control availability depending on navbar state */
        if (mNavigationPreferencesCat != null) {
            if (force || navbarEnabled) {
                if (DeviceUtils.isEdgeToEdgeEnabled(requireContext())) {
                    mNavigationPreferencesCat.addPreference(mEdgeLongSwipeAction);

                    mNavigationPreferencesCat.removePreference(mNavigationArrowKeys);
                    mNavigationPreferencesCat.removePreference(mNavBarInverse);
                    mNavigationPreferencesCat.removePreference(mNavBarLayoutViews);
                    mNavigationPreferencesCat.removePreference(mNavigationBackLongPressAction);
                    mNavigationPreferencesCat.removePreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.removePreference(mNavigationHomeDoubleTapAction);
                    mNavigationPreferencesCat.removePreference(mNavigationAppSwitchLongPressAction);
                } else if (DeviceUtils.isSwipeUpEnabled(getContext())) {
                    mNavigationPreferencesCat.addPreference(mNavigationArrowKeys);
                    mNavigationPreferencesCat.addPreference(mNavBarInverse);
                    mNavigationPreferencesCat.addPreference(mNavBarLayoutViews);
                    mNavigationPreferencesCat.addPreference(mNavigationBackLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeDoubleTapAction);

                    mNavigationPreferencesCat.removePreference(mNavigationAppSwitchLongPressAction);
                    mNavigationPreferencesCat.removePreference(mEdgeLongSwipeAction);
                } else {
                    mNavigationPreferencesCat.addPreference(mNavigationArrowKeys);
                    mNavigationPreferencesCat.addPreference(mNavBarInverse);
                    mNavigationPreferencesCat.addPreference(mNavBarLayoutViews);
                    mNavigationPreferencesCat.addPreference(mNavigationBackLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeLongPressAction);
                    mNavigationPreferencesCat.addPreference(mNavigationHomeDoubleTapAction);
                    mNavigationPreferencesCat.addPreference(mNavigationAppSwitchLongPressAction);

                    mNavigationPreferencesCat.removePreference(mEdgeLongSwipeAction);
                }
            }
        }
    }

    private static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar(Display.DEFAULT_DISPLAY);
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        return hasNavigationBar;
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        @Override
        public Set<String> getNonIndexableKeys(Context context) {
            final Set<String> result = new ArraySet<>();

            if (hasNavigationBar()) {
                if (DeviceUtils.isEdgeToEdgeEnabled(context)) {
                    result.add(KEY_EDGE_LONG_SWIPE);
                } else if (DeviceUtils.isSwipeUpEnabled(context)) {
                    result.add(KEY_NAVIGATION_ARROW_KEYS);
                    result.add(KEY_NAV_BAR_INVERSE);
                    result.add(KEY_NAVBAR_LAYOUT_VIEWS);
                    result.add(KEY_NAVIGATION_BACK_LONG_PRESS);
                    result.add(KEY_NAVIGATION_HOME_LONG_PRESS);
                    result.add(KEY_NAVIGATION_HOME_DOUBLE_TAP);
                } else {
                    result.add(KEY_NAVIGATION_ARROW_KEYS);
                    result.add(KEY_NAV_BAR_INVERSE);
                    result.add(KEY_NAVBAR_LAYOUT_VIEWS);
                    result.add(KEY_NAVIGATION_BACK_LONG_PRESS);
                    result.add(KEY_NAVIGATION_HOME_LONG_PRESS);
                    result.add(KEY_NAVIGATION_HOME_DOUBLE_TAP);
                    result.add(KEY_NAVIGATION_APP_SWITCH_LONG_PRESS);
                }
            }
            return result;
        }
    };
}
