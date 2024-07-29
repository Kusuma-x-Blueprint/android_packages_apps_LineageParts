/*
 * SPDX-FileCopyrightText: 2024 Kusuma
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles.triggers;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import lineageos.app.Profile;

import org.lineageos.lineageparts.profiles.ProfilesSettings;

public class TimeTriggerFragment extends Fragment{
    Profile mProfile;

    public static TimeTriggerFragment newInstance(Profile profile) {
        TimeTriggerFragment fragment = new TimeTriggerFragment();

        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);

        fragment.setArguments(extras);
        return fragment;
    }

    public TimeTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProfile != null) {
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
