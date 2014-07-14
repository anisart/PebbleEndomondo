package ru.anisart.pebble.endomondo;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.view.accessibility.AccessibilityEvent;
import com.getpebble.android.kit.PebbleKit;

public class AccessibilityPebbleEndomondoService extends AccessibilityService {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHandler.init(getApplicationContext());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO: News feed and others notification will be captured too
        // return if Pebble is not connected
        if (!PebbleKit.isWatchConnected(this)) return;

        Notification notification = (Notification) event.getParcelableData();
        long postTime = event.getEventTime();
        NotificationHandler.handleNotification(getApplicationContext(), notification, postTime);
    }

    @Override
    public void onInterrupt() {
        //TODO: do something
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationHandler.deinit(getApplicationContext());
    }
}
