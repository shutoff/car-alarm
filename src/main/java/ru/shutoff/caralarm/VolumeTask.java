package ru.shutoff.caralarm;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;

import java.util.Timer;
import java.util.TimerTask;

public class VolumeTask extends TimerTask {

    int start_level;
    int max_level;
    int count;
    int count_vibro;
    Timer timer;

    static final int max_count = 15;

    AudioManager audioManager;
    Vibrator vibrator;

    VolumeTask(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        start_level = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        max_level = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        count = 0;
        count_vibro = 0;
        timer = new Timer();
        timer.schedule(this, 1000, 1000);
    }

    @Override
    public void run() {
        if (count < max_count)
            count++;
        if (count_vibro == 0) {
            vibrator.vibrate(700);
            count_vibro = 5;
        }
        count_vibro--;
        int level = start_level + (max_level - start_level) * count / max_count;
        audioManager.setStreamVolume(AudioManager.STREAM_RING, level, 0);

    }

    void stop() {
        timer.cancel();
        vibrator.cancel();
        audioManager.setStreamVolume(AudioManager.STREAM_RING, start_level, 0);
    }
}
