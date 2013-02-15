/*
    Copyright (c) 2009, 2010 Josiah Barber (aka Darshan)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.BatteryIndicator;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Date;

public class BatteryIndicatorService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Intent notificationIntent;

    private SharedPreferences settings;
    private KeyguardLock kl;

    private Boolean keyguardDisabled = false;

    private Resources res;
    private Str str;

    public static final String LOG_TAG = "BatteryIndicatorService";
    private L l = new L(LOG_TAG);


    private static final int STATUS_UNPLUGGED     = 0;
    private static final int STATUS_UNKNOWN       = 1;
    private static final int STATUS_CHARGING      = 2;
    private static final int STATUS_DISCHARGING   = 3;
    private static final int STATUS_NOT_CHARGING  = 4;
    private static final int STATUS_FULLY_CHARGED = 5;
    private static final int STATUS_MAX = STATUS_FULLY_CHARGED;

    private static final int PLUGGED_UNPLUGGED = 0;
    private static final int PLUGGED_AC        = 1;
    private static final int PLUGGED_USB       = 2;
    private static final int PLUGGED_UNKNOWN   = 3;
    private static final int PLUGGED_WIRELESS  = 4;
    private static final int PLUGGED_MAX       = PLUGGED_WIRELESS;

    private static final int HEALTH_UNKNOWN     = 1;
    private static final int HEALTH_GOOD        = 2;
    private static final int HEALTH_OVERHEAT    = 3;
    private static final int HEALTH_DEAD        = 4;
    private static final int HEALTH_OVERVOLTAGE = 5;
    private static final int HEALTH_FAILURE     = 6;
    private static final int HEALTH_COLD        = 7;
    private static final int HEALTH_MAX         = HEALTH_COLD;


    public static final String KEY_SERVICE_DESIRED = "serviceDesired";

    private static final int plainIcon0 = R.drawable.plain000;
    private static final int small_plainIcon0 = R.drawable.small_plain000;
    private static final int chargingIcon0 = R.drawable.charging000;
    private static final int small_chargingIcon0 = R.drawable.small_charging000;

    static {
        Log.i(LOG_TAG, "Static block");
    }

    @Override
    public void onCreate() {
        l.i("onCreate()");

        res = getResources();
        str = new Str();

        registerReceiver(mBatteryInfoReceiver, batteryChanged);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        notificationIntent = new Intent(getApplicationContext(), BatteryIndicator.class);

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock(getPackageName());

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
    }

    @Override
    public void onDestroy() {
        l.i("onDestroy()");
        setEnablednessOfKeyguard(true);

        unregisterReceiver(mBatteryInfoReceiver);
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        l.i("onBind()");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BatteryIndicatorService getService() {
            return BatteryIndicatorService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            l.i("start of onReceive()");
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;
            l.i("Indeed ACTION_BATTERY_CHANGED");

            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 100);
            int status = intent.getIntExtra("status", STATUS_UNKNOWN);
            int health = intent.getIntExtra("health", HEALTH_UNKNOWN);
            int plugged = intent.getIntExtra("plugged", PLUGGED_UNKNOWN);
            int temperature = intent.getIntExtra("temperature", 0);
            int voltage = intent.getIntExtra("voltage", 0);
            //String technology = intent.getStringExtra("technology");

            int percent = level * 100 / scale;

            l.i("percent: " + percent);
            l.i("status: " + status);
            l.i("plugged: " + plugged);
            l.i("temperature: " + temperature);

            l.i("hack start");
            java.io.File hack_file = new java.io.File("/sys/class/power_supply/battery/charge_counter");
            if (hack_file.exists()) {
                l.i("hack file exists");
                try {
                    java.io.FileReader fReader = new java.io.FileReader(hack_file);
                    java.io.BufferedReader bReader = new java.io.BufferedReader(fReader, 8);
                    String line = bReader.readLine();
                    bReader.close();
                    int charge_counter = Integer.valueOf(line);
                    l.i("charge_counter: " + charge_counter);

                    if (charge_counter < percent + 10 && charge_counter > percent - 10) {
                        if (charge_counter > 100) // This happens
                            charge_counter = 100;

                        if (charge_counter < 0)   // This could happen?
                            charge_counter = 0;

                        percent = charge_counter;
                        l.i("set percent to: " + percent);
                    } else {
                        l.e("charge_counter file exists but with value " + charge_counter +
                              " which is inconsistent with percent: " + percent);
                    }
                } catch (java.io.FileNotFoundException e) {
                    /* These error messages are only really useful to me and might as well be left hardwired here in English. */
                    l.e("charge_counter file doesn't exist");
                    e.printStackTrace();
                } catch (java.io.IOException e) {
                    l.e("Error reading charge_counter file");
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    l.e("Read charge_counter file but couldn't convert contents to int");
                    e.printStackTrace();
                }
            } else {
                l.i("hack file does NOT exist");
            }
            l.i("hack end");

            if (status  > STATUS_MAX) status  = STATUS_UNKNOWN;
            if (health  > HEALTH_MAX) health  = HEALTH_UNKNOWN;
            if (plugged > PLUGGED_MAX) plugged = PLUGGED_UNKNOWN;

            /* I Take advantage of (count on) R.java having resources alphabetical and incrementing by one */

            int icon;

            String default_set = "builtin.classic";
            if (android.os.Build.VERSION.SDK_INT >= 11)
                default_set = "builtin.plain_number";

            String icon_set = settings.getString(SettingsActivity.KEY_ICON_SET, "null");
            if (icon_set.equals("null")) {
                icon_set = default_set;

                SharedPreferences.Editor settings_editor = settings.edit();
                settings_editor.putString(SettingsActivity.KEY_ICON_SET, default_set);
                settings_editor.commit();
            }

            if (icon_set.equals("builtin.plain_number")) {
                icon = (status == STATUS_CHARGING ? chargingIcon0 : plainIcon0) + percent;
            } else if (icon_set.equals("builtin.smaller_number")) {
                icon = (status == STATUS_CHARGING ? small_chargingIcon0 : small_plainIcon0) + percent;
            } else {
                icon = R.drawable.b000 + percent;
            }

            /* Just treating any unplugged status as simply "Unplugged" now.
               Note that the main activity now assumes that the status is always 0, 2, or 5 */
            if (plugged == 0) status = 0; /* TODO: use static class CONSTANTS instead of numbers */

            l.i("final plugged: " + plugged);

            String statusStr = str.statuses[status];
            if (status == 2) statusStr += " " + str.pluggeds[plugged]; /* Add '(AC)' or '(USB)' if charging */

            String temp_s;
            if (settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false)){
                temp_s = String.valueOf((java.lang.Math.round(temperature * 9 / 5.0) / 10.0) + 32.0) +
                    str.degree_symbol + str.fahrenheit_symbol;
            } else {
                temp_s = String.valueOf(temperature / 10.0) + str.degree_symbol + str.celsius_symbol;
            }

            int last_status = settings.getInt("last_status", -1);
            /* There's a bug, at least on 1.5, or maybe depending on the hardware (I've noticed it on the MyTouch with 1.5)
               where USB is recognized as AC at first, then quickly changed to USB.  So we need to test if plugged changed. */
            int last_plugged = settings.getInt("last_plugged", -1);
            long last_status_cTM = settings.getLong("last_status_cTM", -1);
            int last_percent = settings.getInt("last_percent", -1);
            int previous_charge = settings.getInt("previous_charge", 100);
            long currentTM = System.currentTimeMillis();
            long statusDuration;
            String last_status_since = settings.getString("last_status_since", null);

            SharedPreferences.Editor editor = settings.edit();
            if (last_status != status || last_status_cTM == -1 || last_percent == -1 ||
                last_status_cTM > currentTM || last_status_since == null || last_plugged != plugged ||
                (plugged == 0 && percent > previous_charge + 20))
            {
                last_status_since = formatTime(new Date());
                statusDuration = 0;

                editor.putString("last_status_since", last_status_since);
                editor.putLong("last_status_cTM", currentTM);
                editor.putInt("last_status", status);
                editor.putInt("last_percent", percent);
                editor.putInt("last_plugged", plugged);
                editor.putInt("previous_charge", percent);

                last_status_cTM = currentTM;

                if (last_status != status && settings.getBoolean(SettingsActivity.KEY_AUTO_DISABLE_LOCKING, false)) {
                    if (last_status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, true);
                        setEnablednessOfKeyguard(false);
                    } else if (status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false);
                        setEnablednessOfKeyguard(true);

                        /* If the screen was on (no active keyguard) when the device was plugged in (disabling the
                             keyguard), and the screen is off now, then the keyguard is still disabled. That's
                             stupid.  As a workaround, let's aquire a wakelock that forces the screen to turn on,
                             then release it. This is unfortunate but seems better than not doing it, which would
                             result in no keyguard when you unplug and throw your phone in your pocket. */
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                                                                  PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                                                  PowerManager.ON_AFTER_RELEASE, getPackageName());
                        wl.acquire();
                        wl.release();
                    }
                }
            } else {
                statusDuration = currentTM - last_status_cTM;

                if (percent % 10 == 0)
                    editor.putInt("previous_charge", percent);
            }
            editor.commit();

            /* Add half an hour, then divide.  Should end up rounding to the closest hour. */
            int statusDurationHours = (int)((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));

            String contentTitle = "";

            if (settings.getBoolean(SettingsActivity.KEY_CHARGE_AS_TEXT, false))
                contentTitle += percent + str.percent_symbol + " ";

            int status_dur_est = 12; // TODO: Better to get integer value of default from misc.xml

            if (statusDurationHours < status_dur_est) {
                contentTitle += statusStr + " " + str.since + " " + last_status_since;
            } else {
                contentTitle += statusStr + " " + str.for_n_hours(statusDurationHours);
            }

            String contentText = str.healths[health] + " / " + temp_s;

            if (voltage > 500)
                contentText += " / " + String.valueOf(voltage / 1000.0) + str.volt_symbol;

            Notification notification = new Notification(icon, null, 0);

            if (android.os.Build.VERSION.SDK_INT >= 16) {
                notification.priority = -1;
            }

            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

            startForeground(1, notification);
            l.i("end of onReceive()");
        }
    };

    private String formatTime(Date d) {
        String format = android.provider.Settings.System.getString(getContentResolver(),
                                                                android.provider.Settings.System.TIME_12_24);
        if (format == null || format.equals("12")) {
            return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT,
                                                        java.util.Locale.getDefault()).format(d);
        } else {
            return (new java.text.SimpleDateFormat("HH:mm")).format(d);
        }
    }

    /* Old versions of Android (haven't experimented to determine exactly which are included), at least on
         the emulator, really don't want you to call reenableKeyguard() if you haven't first disabled it.
         So to stay compatible with older devices, let's add an extra setting and add this function. */
    private void setEnablednessOfKeyguard(boolean enabled) {
        if (enabled) {
            if (keyguardDisabled) {
                kl.reenableKeyguard();
                keyguardDisabled = false;
            }
        } else {
            if (! keyguardDisabled) {
                kl.disableKeyguard();
                keyguardDisabled = true;
            }
        }
    }

    public void reloadSettings() {
        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
        else
            setEnablednessOfKeyguard(true);

        //unregisterReceiver(mBatteryInfoReceiver); /* It appears that there's no need to unregister first */
        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }

    private class Str {
        public Resources r;

        public String degree_symbol;
        public String fahrenheit_symbol;
        public String celsius_symbol;
        public String volt_symbol;
        public String percent_symbol;
        public String since;
        public String default_status_dur_est;
        public String default_red_thresh;
        public String default_amber_thresh;
        public String default_green_thresh;
        public String default_max_log_age;

        private String[] statuses;
        private String[] healths;
        private String[] pluggeds;

        public Str() {
            degree_symbol          = res.getString(R.string.degree_symbol);
            fahrenheit_symbol      = res.getString(R.string.fahrenheit_symbol);
            celsius_symbol         = res.getString(R.string.celsius_symbol);
            volt_symbol            = res.getString(R.string.volt_symbol);
            percent_symbol         = res.getString(R.string.percent_symbol);
            since                  = res.getString(R.string.since);
            default_status_dur_est = res.getString(R.string.default_status_dur_est);
            default_red_thresh     = res.getString(R.string.default_red_thresh);
            default_amber_thresh   = res.getString(R.string.default_amber_thresh);
            default_green_thresh   = res.getString(R.string.default_green_thresh);
            default_max_log_age    = res.getString(R.string.default_max_log_age);

            statuses = res.getStringArray(R.array.statuses);
            healths  = res.getStringArray(R.array.healths);
            pluggeds = res.getStringArray(R.array.pluggeds);
        }

        public String for_n_hours(int n) {
            return String.format(res.getQuantityString(R.plurals.for_n_hours, n), n);
        }
    }
}
