/*
 * SPDX-FileCopyrightText: 2024 Kusuma
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles.triggers;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.widget.PreferenceImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import lineageos.app.Profile;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.profiles.ProfilesSettings;
import org.lineageos.lineageparts.profiles.TimeProfileUtils;

public class TimeTriggerFragment extends Fragment {
    private Profile mProfile;
    private RecyclerView mRecyclerView;
    private LinearLayout mAddButton;
    private TimePreferenceAdapter mAdapter;
    private List<TimePreference> mTimePreferences = new ArrayList<>();
    private static final String PREF_SELECTED_TIME_PREFIX = "selected_time_";
    private static final int MAX_ALARMS = 5;

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
        if (getArguments() != null) {
            mProfile = getArguments().getParcelable(ProfilesSettings.EXTRA_PROFILE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.profile_time_empty_view, container, false);
        LinearLayout linearLayout = rootView.findViewById(R.id.linear_layout);

        mRecyclerView = rootView.findViewById(R.id.container);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAddButton = (LinearLayout) inflater.inflate(R.layout.preference_material, linearLayout, false);
        LinearLayout addIcon = mAddButton.findViewById(R.id.icon_frame);
        PreferenceImageView addImageView = mAddButton.findViewById(R.id.icon);
        TextView addTitle = mAddButton.findViewById(R.id.title);
        TextView addSummary = mAddButton.findViewById(R.id.summary);
        addIcon.setVisibility(View.VISIBLE);
        addImageView.setImageResource(R.drawable.ic_add_24dp);
        addTitle.setText(R.string.profile_time_add);
        addSummary.setVisibility(View.GONE);
        linearLayout.addView(mAddButton);
        mAddButton.setOnClickListener(v -> addTimePreference());

        mAdapter = new TimePreferenceAdapter(mTimePreferences, getContext(), 
                new TimePreferenceAdapter.OnTimePreferenceInteractionListener() {
            @Override
            public void onDelete(int position) {
                deleteTimePreference(position);
            }

            @Override
            public void onEdit(int position, String time) {
                showTimePickerDialog(position, time);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        loadSavedPreferences();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTimePreferences();
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getContext().getSharedPreferences(
                "profile_prefs", Context.MODE_PRIVATE);
        mTimePreferences.clear();

        for (int i = 0; i < MAX_ALARMS; i++) {
            String selectedTime = prefs.getString(getUniqueTimeKey(
                    mProfile.getUuid(), i), null);
            if (selectedTime != null) {
                mTimePreferences.add(new TimePreference(i, selectedTime)); // Add loaded preference with index
            }
        }
        mAdapter.notifyDataSetChanged();
        updateAddButtonVisibility(mTimePreferences.size());
    }

    private void updateAddButtonVisibility(int preferenceCount) {
        if (preferenceCount >= MAX_ALARMS) {
            mAddButton.setVisibility(View.GONE);
        } else {
            mAddButton.setVisibility(View.VISIBLE);
        }
    }

    private void addTimePreference() {
        SharedPreferences prefs = getContext().getSharedPreferences(
                "profile_prefs", Context.MODE_PRIVATE);
        int nextIndex = getNextAvailableIndex(prefs);
        if (nextIndex < MAX_ALARMS) {
            showTimePickerDialog(nextIndex, null);
        }
    }

    private int getNextAvailableIndex(SharedPreferences prefs) {
        for (int i = 0; i < MAX_ALARMS; i++) {
            if (prefs.getString(getUniqueTimeKey(mProfile.getUuid(), i), null) == null) {
                return i;
            }
        }
        return MAX_ALARMS;
    }

    private void refreshTimePreferences() {
        boolean is24HourFormat = DateFormat.is24HourFormat(getContext());
        loadSavedPreferences();
        for (int i = 0; i < mTimePreferences.size(); i++) {
            String time = mTimePreferences.get(i).time;
            String formattedTime = formatTime(parseTime(time, !is24HourFormat)[0], 
                    parseTime(time, !is24HourFormat)[1], is24HourFormat);
            mTimePreferences.get(i).time = formattedTime;
            mAdapter.notifyItemChanged(i);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void showTimePickerDialog(int alarmIndex, String existingTime) {
        final Calendar calendar = Calendar.getInstance();
        boolean is24HourFormat = DateFormat.is24HourFormat(getContext());

        if (existingTime != null) {
            int[] time = parseTime(existingTime, is24HourFormat);
            calendar.set(Calendar.HOUR_OF_DAY, time[0]);
            calendar.set(Calendar.MINUTE, time[1]);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            getContext(),
            (TimePicker view, int hourOfDay, int minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                String newSelectedTime = formatTime(hourOfDay, minute, is24HourFormat);

                if (existingTime != null && newSelectedTime.equals(existingTime)) {
                    return; // No changes
                }

                saveSelectedTime(newSelectedTime, alarmIndex);

                if (existingTime != null) {
                    // Update the specific TimePreference by finding its index by alarmIndex
                    for (int i = 0; i < mTimePreferences.size(); i++) {
                        if (mTimePreferences.get(i).alarmIndex == alarmIndex) {
                            mTimePreferences.get(i).time = newSelectedTime;
                            mAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                } else {
                    mTimePreferences.add(new TimePreference(alarmIndex, newSelectedTime));
                    mAdapter.notifyItemInserted(mTimePreferences.size() - 1);
                }

                scheduleAlarm(calendar, alarmIndex);
                updateAddButtonVisibility(mTimePreferences.size());
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            is24HourFormat
        );
        timePickerDialog.show();
    }

    private String formatTime(int hourOfDay, int minute, boolean is24HourFormat) {
        if (is24HourFormat) {
            return String.format("%02d:%02d", hourOfDay, minute);
        } else {
            int hour = hourOfDay % 12;
            if (hour == 0) hour = 12;
            String amPm = (hourOfDay >= 12) ? "PM" : "AM";
            return String.format("%d:%02d %s", hour, minute, amPm);
        }
    }

    private int[] parseTime(String timeString, boolean is24HourFormat) {
        int[] time = new int[2];
        try {
            if (timeString.contains("AM") || timeString.contains("PM")) {
                String[] parts = timeString.split(" ");
                String[] timeParts = parts[0].split(":");
                time[0] = Integer.parseInt(timeParts[0]);
                time[1] = Integer.parseInt(timeParts[1]);
                if (parts[1].equals("PM") && time[0] < 12) {
                    time[0] += 12;
                } else if (parts[1].equals("AM") && time[0] == 12) {
                    time[0] = 0;
                }
            } else {
                String[] parts = timeString.split(":");
                time[0] = Integer.parseInt(parts[0]);
                time[1] = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    private void saveSelectedTime(String formattedTime, int alarmIndex) {
        if (mProfile != null) {
            SharedPreferences.Editor editor = getContext().getSharedPreferences(
                    "profile_prefs", Context.MODE_PRIVATE).edit();
            editor.putString(getUniqueTimeKey(mProfile.getUuid(), alarmIndex), formattedTime);
            editor.apply();
        }
    }

    private void deleteTimePreference(int position) {
        TimePreference timePreference = mTimePreferences.get(position);
        SharedPreferences prefs = getContext().getSharedPreferences(
                "profile_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(getUniqueTimeKey(mProfile.getUuid(), timePreference.alarmIndex));
        editor.apply();
        cancelAlarm(timePreference.alarmIndex);
        mTimePreferences.remove(position);
        mAdapter.notifyItemRemoved(position);
        mAdapter.notifyItemRangeChanged(position, mTimePreferences.size());
        updateAddButtonVisibility(mTimePreferences.size());
    }

    private void scheduleAlarm(Calendar time, int alarmIndex) {
        if (mProfile != null) {
            TimeProfileUtils.scheduleAlarm(getContext(), time, mProfile, alarmIndex);
        }
    }

    private void cancelAlarm(int alarmIndex) {
        if (mProfile != null) {
            TimeProfileUtils.cancelAlarm(getContext(), mProfile, alarmIndex);
        }
    }

    private String getUniqueTimeKey(UUID profileUuid, int alarmIndex) {
        return PREF_SELECTED_TIME_PREFIX + profileUuid.toString() + "_" + alarmIndex;
    }

    private static class TimePreferenceAdapter 
            extends RecyclerView.Adapter<TimePreferenceAdapter.ViewHolder> {

        private final List<TimePreference> mTimePreferences;
        private final Context context;
        private final OnTimePreferenceInteractionListener listener;

        interface OnTimePreferenceInteractionListener {
            void onDelete(int position);
            void onEdit(int position, String time);
        }

        public TimePreferenceAdapter(List<TimePreference> mTimePreferences, Context context, 
                OnTimePreferenceInteractionListener listener) {
            this.mTimePreferences = mTimePreferences;
            this.context = context;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.profile_time_trigger_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TimePreference timePreference = mTimePreferences.get(position);
            holder.timeTextView.setText(context.getString(R.string.profile_time_title));
            holder.timeSummaryTextView.setText(timePreference.time);

            LayoutInflater inflater = LayoutInflater.from(context);
            View widgetContent = inflater.inflate(
                    R.layout.profile_time_delete_widget, holder.widgetFrame, false);
            holder.widgetFrame.addView(widgetContent);
            holder.widgetFrame.setOnClickListener(v -> listener.onDelete(position));

            holder.itemView.setOnClickListener(v -> listener.onEdit(position, timePreference.time));
        }

        @Override
        public int getItemCount() {
            return mTimePreferences.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView timeTextView;
            TextView timeSummaryTextView;
            LinearLayout widgetFrame;

            public ViewHolder(View itemView) {
                super(itemView);
                timeTextView = itemView.findViewById(R.id.title);
                timeSummaryTextView = itemView.findViewById(R.id.summary);
                widgetFrame = itemView.findViewById(R.id.widget_frame);
            }
        }
    }

    public class TimePreference {
        int alarmIndex;
        String time;

        public TimePreference(int alarmIndex, String time) {
            this.alarmIndex = alarmIndex;
            this.time = time;
        }
    }
}
