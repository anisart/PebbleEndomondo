package ru.anisart.pebble.endomondo;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.*;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.matejdro.pebblenotificationcenter.notifications.NotificationParser;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PebbleEndomondoService extends AccessibilityService {

    private final static String TAG = PebbleEndomondoService.class.getSimpleName();
    public final static UUID PEBBLE_APP_UUID = UUID.fromString("51dd80f9-257d-4b3b-a81f-1016b0ef94af");
    private final static String PREF_AUTOSTART = "autostart";
    private final static String PREF_FORCESTART = "forcestart";
    private final static String PREF_FORCESTART_DELAY = "forcestart_delay";

    private static boolean active = false;

    private String notificationText;
    private Pattern pattern = Pattern.compile(": (.+\\n?)");
    private Notification.Action action;
    private int batteryLevel;
    private long lastNotificationTime = 0;
    private long lastSuccessDelivery = 0;
    private long maxTimeDiff = 60000L;
    private SharedPreferences preferences;

    private BroadcastReceiver batInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        }
    };

    public static boolean isActive() {
        return active;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO: News feed and others notification will be captured too
        // return if Pebble is not connected
        if (!PebbleKit.isWatchConnected(this)) return;

        Notification notification = (Notification) event.getParcelableData();
        NotificationParser parser = new NotificationParser(this, notification);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            action = notification.actions[0];
        }
        notificationText = parser.text.trim();
        long notificationTime = event.getEventTime();
        boolean autostartEnabled = preferences.getBoolean(PREF_AUTOSTART, false);
        if (autostartEnabled && (notificationTime - lastNotificationTime > maxTimeDiff)) {
            PebbleKit.startAppOnPebble(PebbleEndomondoService.this, PEBBLE_APP_UUID);
        }
        lastNotificationTime = notificationTime;
        sentDataToPebble();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        active = true;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
                @Override
                public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                    if ((data.getUnsignedInteger(1) == 0) && (action != null)) {
                        final PendingIntent actionIntent = action.actionIntent;
                        Intent intent = new Intent();
                        try {
                            actionIntent.send(PebbleEndomondoService.this, 0, intent);
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                        action = null;
                    }

                    PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
                }
            });
        }
        registerReceiver(batInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                lastSuccessDelivery = lastNotificationTime;
            }
        });

        PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                boolean forceStart = preferences.getBoolean(PREF_FORCESTART, false);
                if (forceStart) {
                    long forceStartDelay = Long.parseLong(preferences.getString(PREF_FORCESTART_DELAY, "-1"));
                    if (lastNotificationTime - lastSuccessDelivery > forceStartDelay) {
                        PebbleKit.startAppOnPebble(PebbleEndomondoService.this, PEBBLE_APP_UUID);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        active = false;
        unregisterReceiver(batInfoReceiver);
    }

    @Override
    public void onInterrupt() {
        //TODO: do something
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    private void sentDataToPebble() {
        Matcher matcher = pattern.matcher(notificationText);
        String dataString = "";
        while (matcher.find()) {
            dataString += matcher.group(1);
        }
        Log.d(TAG, dataString);

        PebbleDictionary data = new PebbleDictionary();
        data.addString(0, dataString);
        data.addUint8(1, (byte) batteryLevel);
        PebbleKit.sendDataToPebble(this, PEBBLE_APP_UUID, data);
    }
}
