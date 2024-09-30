/*
 * SPDX-FileCopyrightText: 2014 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;

import org.lineageos.lineageparts.R;

import lineageos.app.Profile;

public class DndModeItem extends Item {
    private final Profile mProfile;

    public DndModeItem(Profile profile) {
        mProfile = profile;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.dnd_title);
    }

    @Override
    public String getSummary(Context context) {
        return context.getString(getSummaryString(mProfile));
    }

    public static int getSummaryString(Profile profile) {
        switch (profile.getDndMode()) {
            case Profile.DndMode.DEFAULT:
                return R.string.profile_action_none; //"leave unchanged"
            case Profile.DndMode.ENABLE:
                return R.string.profile_action_enable;
            case Profile.DndMode.DISABLE:
                return R.string.profile_action_disable;
            default: return 0;
        }
    }
}
