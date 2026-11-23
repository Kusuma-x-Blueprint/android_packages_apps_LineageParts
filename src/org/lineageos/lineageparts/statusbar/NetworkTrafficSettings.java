/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.statusbar;

import android.content.ContentResolver;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import lineageos.preference.LineageSecureSettingSwitchPreference;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class NetworkTrafficSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener  {

    private static final String TAG = "NetworkTrafficSettings";

    private ListPreference mNetTrafficMode;
    private LineageSecureSettingSwitchPreference mNetTrafficAutohide;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.network_traffic_settings);
        getActivity().setTitle(R.string.network_traffic_settings_title);

        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficMode = findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_MODE);
        mNetTrafficMode.setOnPreferenceChangeListener(this);
        int mode = LineageSettings.Secure.getInt(resolver,
                LineageSettings.Secure.NETWORK_TRAFFIC_MODE, 0);
        mNetTrafficMode.setValue(String.valueOf(mode));

        mNetTrafficAutohide = findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE);
        mNetTrafficAutohide.setOnPreferenceChangeListener(this);

        updateEnabledStates(mode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetTrafficMode) {
            int mode = Integer.parseInt((String) newValue);
            LineageSettings.Secure.putInt(getActivity().getContentResolver(),
                    LineageSettings.Secure.NETWORK_TRAFFIC_MODE, mode);
            updateEnabledStates(mode);
        }
        return true;
    }

    private void updateEnabledStates(int mode) {
        final boolean enabled = mode != 0;
        mNetTrafficAutohide.setEnabled(enabled);
    }
}
