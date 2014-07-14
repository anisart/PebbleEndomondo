package ru.anisart.pebble.endomondo;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.getpebble.android.kit.PebbleKit;

@TargetApi(value = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationPebbleEndomondoService extends NotificationListenerService {

    private final static String PACK_NAME = "com.endomondo.android";
    private final static String PACK_NAME_PRO = "com.endomondo.android.pro";

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHandler.init(getApplicationContext());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        //TODO: News feed and others notification will be captured too
        // return if it is not Endomondo notification
        if (!(packageName.equals(PACK_NAME) || packageName.equals(PACK_NAME_PRO))) return;
        // return if Pebble is not connected
        if (!PebbleKit.isWatchConnected(this)) return;

        Notification notification = sbn.getNotification();
        long postTime = sbn.getPostTime();
        NotificationHandler.handleNotification(getApplicationContext(), notification, postTime);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        // return if it is not Endomondo notification
        if (!(packageName.equals(PACK_NAME) || packageName.equals(PACK_NAME_PRO))) return;
        // return if Pebble is not connected
        if (!PebbleKit.isWatchConnected(this)) return;

        NotificationHandler.handleNotification(getApplicationContext(), null, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationHandler.deinit(getApplicationContext());
    }
}
