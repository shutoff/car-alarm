package ru.shutoff.caralarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;

public class SmsMonitor extends BroadcastReceiver {

    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null &&
                ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
            Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
            SmsMessage[] messages = new SmsMessage[pduArray.length];
            for (int i = 0; i < pduArray.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
            }
            String sms_from = messages[0].getOriginatingAddress();
            StringBuilder bodyText = new StringBuilder();
            for (SmsMessage m : messages) {
                bodyText.append(m.getMessageBody());
            }
            String body = bodyText.toString();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String[] cars = preferences.getString(Names.CARS, "").split(",");
            for (String car : cars) {
                String phone_config = digitsOnly(preferences.getString(Names.CAR_PHONE + car, ""));
                if ((phone_config.length() > 0) && phone_config.equals(digitsOnly(sms_from))) {
                    if (processCarMessage(context, body, car))
                        abortBroadcast();
                    return;
                }
            }

            if (body.matches("[0-9A-Fa-f]{30}")) {
                Intent keyIntent = new Intent(context, ApiKeyDialog.class);
                keyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                keyIntent.putExtra(Names.CAR_KEY, body);
                context.startActivity(keyIntent);
            }
        }
    }

    String digitsOnly(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    static String[] notifications = {
            "ALARM Light shock",
            "Low Card Battery",
            "Supply reserve",
            "Supply regular",
            "ERROR LAN-devices",
            "Low reserve voltage",
            "Roaming. Internet OFF"
    };

    static String[] alarms = {
            "ALARM Heavy shock",
            "ALARM Trunk",
            "ALARM Hood",
            "ALARM Doors",
            "ALARM Lock",
            "ALARM MovTilt sensor",
            "ALARM Rogue"
    };

    boolean processCarMessage(Context context, String body, String car_id) {
        if ((State.waitAnswer != null) && (body.substring(0, State.waitAnswer.length()).equalsIgnoreCase(State.waitAnswer))) {
            try {
                State.waitAnswerPI.send(Names.ANSWER_OK);
            } catch (CanceledException e) {
                // ignore
            }
            return true;
        }
        for (int i = 0; i < notifications.length; i++) {
            if (compare(body, notifications[i])) {
                String[] msg = context.getString(R.string.notification).split("\\|");
                showNotification(context, msg[i], car_id);
                return true;
            }
        }
        for (int i = 0; i < alarms.length; i++) {
            if (compare(body, alarms[i])) {
                String[] msg = context.getString(R.string.alarm).split("\\|");
                showAlarm(context, msg[i], car_id);
                return true;
            }
        }
        return false;
    }

    static boolean compare(String body, String message) {
        if (body.length() < message.length())
            return false;
        return body.substring(0, message.length()).equalsIgnoreCase(message);
    }

    private void showNotification(Context context, String text, String car_id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String title = "Car Alarm";
        String[] cars = preferences.getString(Names.CARS, "").split(",");
        if (cars.length > 1) {
            title = preferences.getString(Names.CAR_NAME, "");
            if (title.length() == 0) {
                title = context.getString(R.string.car);
                if (car_id.length() > 0)
                    title += " " + car_id;
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        int id = preferences.getInt(Names.IDS, 0);
        id++;
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(Names.IDS, id);
        ed.commit();

        // Add as notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, builder.build());

        String sound = preferences.getString(Names.NOTIFY, "");
        Uri uri = Uri.parse(sound);
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        if (ringtone == null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(context, uri);
            if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
                player.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                player.setLooping(false);
                player.prepare();
                player.start();
            }
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null)
                vibrator.vibrate(500);
        } catch (Exception err) {
            // ignore
        }
    }

    private void showAlarm(Context context, String text, String car_id) {
        Intent alarmIntent = new Intent(context, Alarm.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmIntent.putExtra(Names.ALARM, text);
        alarmIntent.putExtra(Names.ID, car_id);
        context.startActivity(alarmIntent);
    }

}
