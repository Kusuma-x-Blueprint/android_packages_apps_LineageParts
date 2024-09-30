/*
 * SPDX-FileCopyrightText: 2024 Kusuma
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.profiles.triggers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lineageos.app.Profile;

import org.json.JSONArray;
import org.json.JSONObject;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.profiles.ProfilesSettings;

public class AppTriggerFragment extends Fragment {

    private Profile mProfile;
    private RecyclerView mRecyclerView;
    private AppAdapter mAppAdapter;
    private List<ApplicationInfo> mAppList;
    private PackageManager mPackageManager;

    public static AppTriggerFragment newInstance(Profile profile) {
        AppTriggerFragment fragment = new AppTriggerFragment();
        Bundle extras = new Bundle();
        extras.putParcelable(ProfilesSettings.EXTRA_PROFILE, profile);
        fragment.setArguments(extras);
        return fragment;
    }

    public AppTriggerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        }
        mPackageManager = requireContext().getPackageManager();
        mAppList = getLaunchableApps();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile_app_empty_view, container, false);

        mRecyclerView = rootView.findViewById(R.id.container);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAppAdapter = new AppAdapter(requireContext(), mAppList, mProfile.getUuid().toString());
        mRecyclerView.setAdapter(mAppAdapter);

        return rootView;
    }

    /**
     * This method fetches only launchable apps (apps with launcher activities) and sorts them by app label.
     * It optimizes fetching by avoiding unnecessary calls to PackageManager and sorting apps using a parallel stream.
     * 
     * @return List of ApplicationInfo containing launchable apps only.
     */
    private List<ApplicationInfo> getLaunchableApps() {
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(launchIntent, 0);

        return resolveInfos.parallelStream()
                .map(resolveInfo -> resolveInfo.activityInfo.applicationInfo)
                .distinct()
                .sorted(Comparator.comparing(app -> app.loadLabel(mPackageManager).toString().toLowerCase()))
                .collect(Collectors.toList());
    }

    public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

        private List<ApplicationInfo> appList;
        private PackageManager mPackageManager;
        private Set<String> selectedApps;
        private Set<String> appsSelectedByOtherProfiles;
        private Context context;
        private String profileUuid;

        public AppAdapter(Context context, List<ApplicationInfo> appList, String profileUuid) {
            this.appList = appList;
            this.mPackageManager = context.getPackageManager();
            this.context = context;
            this.profileUuid = profileUuid;
            this.appsSelectedByOtherProfiles = new HashSet<>();
            this.selectedApps = loadSelectedApps();
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.profile_app_trigger_list, parent, false);
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            ApplicationInfo appInfo = appList.get(position);
            boolean isSelectedByOtherProfile = appsSelectedByOtherProfiles.contains(appInfo.packageName);
            holder.bind(appInfo, selectedApps.contains(appInfo.packageName), isSelectedByOtherProfile);
        }

        @Override
        public int getItemCount() {
            return appList.size();
        }

        /**
         * Load selected apps for the current profile (identified by profileUuid) from the JSON string in Settings.System.
         * Also track apps that are selected by other profiles and store them in appsSelectedByOtherProfiles.
         */
        private Set<String> loadSelectedApps() {
            Set<String> selectedAppsSet = new HashSet<>();
            try {
                String jsonString = Settings.System.getString(context.getContentResolver(), Settings.System.PROFILE_APP_TRIGGER_LIST);
                if (jsonString != null && !jsonString.isEmpty()) {
                    JSONObject json = new JSONObject(jsonString);
                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String uuid = keys.next();
                        JSONArray appsArray = json.getJSONArray(uuid);
                        if (uuid.equals(profileUuid)) {
                            for (int i = 0; i < appsArray.length(); i++) {
                                selectedAppsSet.add(appsArray.getString(i).trim());
                            }
                        } else {
                            for (int i = 0; i < appsArray.length(); i++) {
                                appsSelectedByOtherProfiles.add(appsArray.getString(i).trim());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return selectedAppsSet;
        }

        /**
         * Save the selected apps for the current profile (identified by profileUuid) to JSON in Settings.System.
         */
        private void saveSelectedApps() {
            try {
                String jsonString = Settings.System.getString(context.getContentResolver(), Settings.System.PROFILE_APP_TRIGGER_LIST);
                JSONObject json;
                if (jsonString == null || jsonString.isEmpty()) {
                    json = new JSONObject();
                } else {
                    json = new JSONObject(jsonString);
                }
                JSONArray appsArray = new JSONArray();
                for (String appPackageName : selectedApps) {
                    appsArray.put(appPackageName);
                }
                json.put(profileUuid, appsArray);
                Settings.System.putString(context.getContentResolver(), Settings.System.PROFILE_APP_TRIGGER_LIST, json.toString());
                Log.d("AppAdapter", "Saved selected apps for profile " + profileUuid + ": " + appsArray.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        class AppViewHolder extends RecyclerView.ViewHolder {

            private ImageView appIcon;
            private TextView appLabel;
            private TextView appPackageName;
            private CheckBox appCheckbox;

            public AppViewHolder(@NonNull View itemView) {
                super(itemView);
                appIcon = itemView.findViewById(R.id.icon);
                appLabel = itemView.findViewById(R.id.label);
                appPackageName = itemView.findViewById(R.id.packageName);
                appCheckbox = itemView.findViewById(R.id.checkBox);
            }

            public void bind(ApplicationInfo appInfo, boolean isSelected, boolean isSelectedByOtherProfile) {
                View rootView = itemView;
                String label = appInfo.loadLabel(mPackageManager).toString();
                String packageName = appInfo.packageName;
                Drawable icon = appInfo.loadIcon(mPackageManager);

                appLabel.setText(label);
                appIcon.setImageDrawable(icon);
                appCheckbox.setChecked(isSelected);
                appPackageName.setText(packageName);

                // Disable checkbox and preference if it's already selected by another profile
                if (isSelectedByOtherProfile) {
                    rootView.setEnabled(false);
                    appCheckbox.setEnabled(false);
                    appLabel.setAlpha(0.6f);
                    appPackageName.setAlpha(0.6f);
                } else {
                    rootView.setEnabled(true);
                    appCheckbox.setEnabled(true);
                    appLabel.setAlpha(1.0f);
                    appPackageName.setAlpha(1.0f);
                }

                // Handle checkbox click (only if enabled)
                appCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (appCheckbox.isEnabled()) {
                        if (isChecked) {
                            selectedApps.add(appInfo.packageName);
                        } else {
                            selectedApps.remove(appInfo.packageName);
                        }
                        saveSelectedApps();
                    }
                });

                // Handle the entire item click (checkbox toggle on item click, only if enabled)
                itemView.setOnClickListener(v -> {
                    if (appCheckbox.isEnabled()) {
                        boolean currentState = appCheckbox.isChecked();
                        appCheckbox.setChecked(!currentState);
                    }
                });
            }
        }
    }
}
