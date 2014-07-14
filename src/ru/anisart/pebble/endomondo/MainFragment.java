package ru.anisart.pebble.endomondo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.getpebble.android.kit.PebbleKit;

public class MainFragment extends PreferenceFragment {

    private final static String PREF_START_BUTTON = "btnStart";
    private final static String PREF_FORCESTART_DELAY = "forcestart_delay";
    private final static String PREF_INSTALL_BUTTON = "btnInstall";
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        addPreferencesFromResource(R.xml.preferences);

        Preference startButton = (Preference) findPreference(PREF_START_BUTTON);
        startButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PebbleKit.isWatchConnected(context)) {
                    PebbleKit.startAppOnPebble(context, NotificationHandler.PEBBLE_APP_UUID);
                    Toast.makeText(context, R.string.toast_start, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.toast_no_conn, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        final ListPreference forceStartDelayList = (ListPreference) findPreference(PREF_FORCESTART_DELAY);
        forceStartDelayList.setSummary(forceStartDelayList.getEntry());
        forceStartDelayList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String nv = (String) newValue;
                forceStartDelayList.setValue(nv);
                forceStartDelayList.setSummary(forceStartDelayList.getEntry());
                return true;
            }
        });

        Preference installButton = (Preference) findPreference(PREF_INSTALL_BUTTON);
        installButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/53b87838f92e8fe592000077"));
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!NotificationHandler.isActive()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dialog_title).
                    setNegativeButton(R.string.dialog_negative, null);
            if (NotificationHandler.isNotificationListenerSupported()) {
                builder.setMessage(R.string.dialog_message_notification).
                        setPositiveButton(R.string.dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            }
                        });
            } else {
                builder.setMessage(R.string.dialog_message_accessibility).
                        setPositiveButton(R.string.dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                            }
                        });
            }
            builder.create().
                    show();
        }
    }
}