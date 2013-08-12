package ru.shutoff.caralarm;

import android.app.PendingIntent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class State {

    static String waitAnswer;
    static PendingIntent waitAnswerPI;

    static public void appendLog(String text) {
        File logFile = new File("/sdcard/car.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            Date d = new Date();
            buf.append(d.toLocaleString());
            buf.append(" ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
