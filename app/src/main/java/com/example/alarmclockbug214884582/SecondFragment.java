package com.example.alarmclockbug214884582;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.alarmclockbug214884582.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                setAndCancelAlarmClockManyTimes();
            }
        }, 5);
    }

    private void setAndCancelAlarmClockManyTimes() {
        /*
         * Author: jeroen@jeroen-vandeven.nl
         *
         * Reproduces crash in com.android.systemui.
         *
         * This will schedule an Alarm Clock alarm and then cancel it immediately, 1000 times.
         *
         * Side effects:
         *
         * - Every time one alarm is scheduled, AlarmManagerService sends the
         *   ACTION_NEXT_ALARM_CLOCK_CHANGED broadcast action.
         * - This leads to KeyGuardSliceProvider.onNextAlarmChanged scheduling its own RTC alarm.
         * - Before it does so, the previous version of that alarm is cancelled. Thus, at most one
         *   such alarm (tagged "lock_screen_next_alarm") should ever be scheduled.
         * - However, sometimes this cancel operation by KeyGuard fails with the log message
         *   "Unrecognized alarm listener". Then, the RTC alarms add up as more broadcast actions
         *   are triggered by this loop. Once there are 500 "lock_screen_next_alarm" alarms, the
         *   com.android.systemui will crash with exception "java.lang.IllegalStateException:
         *   Maximum limit of concurrent alarms 500 reached".
         *
         * Expected behavior:
         *
         * - During the execution of this loop, the amount of Alarm Clock alarms scheduled by this
         *   package does not exceed 1.
         * - The loop runs for 1000 times without incident, and in the end there are 0 alarms.
         * - During the execution of this loop, the amount of alarms with tag
         *   "lock_screen_next_alarm" does not exceed 1.
         * - During the execution of this loop, Android doesn't crash.
         *
         * Observed faulty behavior:
         *
         * - During the execution of this loop, the amount of alarms with tag
         *   "lock_screen_next_alarm" increases with each iteration (use adb shell dumpsys alarm).
         * - After ~250 or less (depending on other apps) iterations, there are too many alarms
         *   scheduled by com.android.systemui and it crashes with aforementioned exception.al
         *
         * Issue reference: https://issuetracker.google.com/issues/214884582
         */
        String TAG = "AlarmClockBug";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        Context context = this.requireActivity();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 1000; i++) {
            Log.e(TAG, "Scheduling alarm " + (i + 1) + "st/nd/rd/th time");
            c.add(Calendar.SECOND, 1);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(
                            c.getTimeInMillis(),
                            pendingIntent
                    ),
                    PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
            );
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            am.cancel(pendingIntent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}