package ru.anisart.pebble.endomondo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationHandler {

    public final static UUID PEBBLE_APP_UUID = UUID.fromString("51dd80f9-257d-4b3b-a81f-1016b0ef94af");
    private final static String PREF_AUTOSTART = "autostart";
    private final static String PREF_FORCESTART = "forcestart";
    private final static String PREF_FORCESTART_DELAY = "forcestart_delay";

    private static boolean active = false;

    private static String notificationText;
    private static Pattern pattern = Pattern.compile(": (.+\\n?)");
    private static Notification.Action action;
    private static int batteryLevel;
    private static long lastNotificationTime = 0;
    private static long lastSuccessDelivery = 0;
    private static long maxTimeDiff = 60000L;
    private static SharedPreferences preferences;

    private static BroadcastReceiver batInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        }
    };

    public static boolean isActive() {
        return active;
    }

    public static void init(Context context) {
        active = true;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            PebbleKit.registerReceivedDataHandler(context, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
                @Override
                public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                    if ((data.getUnsignedInteger(1) == 0) && (action != null)) {
                        final PendingIntent actionIntent = action.actionIntent;
                        Intent intent = new Intent();
                        try {
                            actionIntent.send(context, 0, intent);
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                        action = null;
                    }

                    PebbleKit.sendAckToPebble(context, transactionId);
                }
            });
        }
        context.registerReceiver(batInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        PebbleKit.registerReceivedAckHandler(context, new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                lastSuccessDelivery = lastNotificationTime;
            }
        });

        PebbleKit.registerReceivedNackHandler(context, new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                boolean forceStart = preferences.getBoolean(PREF_FORCESTART, false);
                if (forceStart) {
                    long forceStartDelay = Long.parseLong(preferences.getString(PREF_FORCESTART_DELAY, "-1"));
                    if (lastNotificationTime - lastSuccessDelivery > forceStartDelay) {
                        PebbleKit.startAppOnPebble(context, PEBBLE_APP_UUID);
                    }
                }
            }
        });
    }

    public static void deinit(Context context) {
        active = false;
        context.unregisterReceiver(batInfoReceiver);
    }

    public static void handleNotification(Context context, Notification notification, long postTime) {
        if (notification != null) {
            NotificationParser parser = new NotificationParser(context, notification);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                action = notification.actions[0];
            }
            notificationText = parser.text.trim();
        } else {
            notificationText = context.getResources().getString(R.string.no_notification_text);
        }
        boolean autostartEnabled = preferences.getBoolean(PREF_AUTOSTART, false);
        if (autostartEnabled && (postTime - lastNotificationTime > maxTimeDiff)) {
            PebbleKit.startAppOnPebble(context, PEBBLE_APP_UUID);
        }
        lastNotificationTime = postTime;
        sentDataToPebble(context);
    }

    private static void sentDataToPebble(Context context) {
        Matcher matcher = pattern.matcher(notificationText);
        String dataString = "";
        while (matcher.find()) {
            dataString += matcher.group(1);
        }

        PebbleDictionary data = new PebbleDictionary();
        data.addString(0, dataString);
        data.addUint8(1, (byte) batteryLevel);
        PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, data);
    }

    public static boolean isNotificationListenerSupported()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }
}
