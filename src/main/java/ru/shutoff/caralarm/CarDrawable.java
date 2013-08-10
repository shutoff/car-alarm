package ru.shutoff.caralarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class CarDrawable {

    static final int UNKNOWN = 0x888888;
    static final int NORMAL  = 0x6180A0;
    static final int GUARD   = 0x6D936D;
    static final int ALARM   = 0xC04141;
    static final int BLACK   = 0x000000;

    Drawable dBg;
    Drawable dCar;
    Drawable dDoors;
    Drawable dDoorsOpen;
    Drawable dHood;
    Drawable dHoodOpen;
    Drawable dTrunk;
    Drawable dTrunkOpen;
    Drawable dLock;
    Drawable dUnlock;
    Drawable dIgnition;

    LayerDrawable drawable;

    int width;
    int height;

    class State {
        int guard;
        int accessory;
        int doors;
        int hood;
        int trunk;
        int engine;
        int ignition;

        State() {
            guard = 0;
            accessory = 0;
            doors = 0;
            hood = 0;
            trunk = 0;
            engine = 0;
            ignition = 0;
        }

        boolean setGuard(int value) {
            if (guard == value)
                return false;
            guard = value;
            return true;
        }

        boolean setAccessory(int value) {
            if (accessory == value)
                return false;
            accessory = value;
            return true;
        }

        boolean setDoors(int value) {
            if (doors == value)
                return false;
            doors = value;
            return true;
        }

        boolean setHood(int value) {
            if (hood == value)
                return false;
            hood = value;
            return true;
        }

        boolean setTrunk(int value) {
            if (trunk == value)
                return false;
            trunk = value;
            return true;
        }

        boolean setEngine(int value) {
            if (engine == value)
                return false;
            engine = value;
            return true;
        }

        boolean setIgnition(int value) {
            if (ignition == value)
                return false;
            ignition = value;
            return true;
        }
    }

    State state = new State();

    CarDrawable(Context ctx) {
        dBg = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bg));
        dBg.setColorFilter(new ColorMatrixColorFilter(createMatrix(BLACK)));

        Bitmap bmpCar = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.car);
        width  = bmpCar.getWidth();
        height = bmpCar.getHeight();

        dCar = new BitmapDrawable(ctx.getResources(), bmpCar);
        dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dDoors = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.doors));
        dDoors.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dDoorsOpen = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.doors_open));
        dDoorsOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));
        dDoorsOpen.setAlpha(0);

        dHood = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.hood));
        dHood.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dHoodOpen = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.hood_open));
        dHoodOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));
        dHoodOpen.setAlpha(0);

        dTrunk = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.trunk));
        dTrunk.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));

        dTrunkOpen = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.trunk_open));
        dTrunkOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));
        dTrunkOpen.setAlpha(0);

        dLock = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.lock));
        dLock.setColorFilter(new ColorMatrixColorFilter(createMatrix(BLACK)));
        dLock.setAlpha(0);

        dUnlock = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.unlock));
        dUnlock.setColorFilter(new ColorMatrixColorFilter(createMatrix(BLACK)));
        dUnlock.setAlpha(0);

        dIgnition = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ignition));
        dIgnition.setColorFilter(new ColorMatrixColorFilter(createMatrix(UNKNOWN)));
        dIgnition.setAlpha(0);

        Drawable[] drawables =
                {
                        dBg,
                        dCar,
                        dDoors,
                        dDoorsOpen,
                        dHood,
                        dHoodOpen,
                        dTrunk,
                        dTrunkOpen,
                        dLock,
                        dUnlock,
                        dIgnition
                };

        drawable = new LayerDrawable(drawables);
    }

    Drawable getDrawable() {
        return drawable;
    }

    Bitmap getBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    boolean update(SharedPreferences preferences) {
        int color = UNKNOWN;
        int alarm = UNKNOWN;

        int guard = preferences.getInt(Names.GUARD, 0);
        boolean res = state.setGuard(guard);
        if (guard > 0){
            dLock.setAlpha(255);
            dUnlock.setAlpha(0);
            color = GUARD;
            alarm = ALARM;
        }else if (guard < 0) {
            dLock.setAlpha(0);
            dUnlock.setAlpha(255);
            color = NORMAL;
            alarm = NORMAL;
        }else{
            dLock.setAlpha(0);
            dUnlock.setAlpha(0);
        }

        int accessory = preferences.getInt(Names.ACCESSORY, 0);
        res |= state.setAccessory(accessory);
        if (accessory > 0){
            dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        }else{
            dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        int doors = preferences.getInt(Names.DOOR, 0);
        res |= state.setDoors(doors);
        if (doors > 0){
            dDoors.setAlpha(0);
            dDoorsOpen.setAlpha(255);
            dDoorsOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        }else{
            dDoorsOpen.setAlpha(0);
            dDoors.setAlpha(255);
            dDoors.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        int hood = preferences.getInt(Names.HOOD, 0);
        res |= state.setHood(hood);
        if (hood > 0){
            dHood.setAlpha(0);
            dHoodOpen.setAlpha(255);
            dHoodOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        }else{
            dHoodOpen.setAlpha(0);
            dHood.setAlpha(255);
            dHood.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        int trunk = preferences.getInt(Names.TRUNK, 0);
        res |= state.setTrunk(trunk);
        if (trunk > 0){
            dTrunk.setAlpha(0);
            dTrunkOpen.setAlpha(255);
            dTrunkOpen.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        }else{
            dTrunkOpen.setAlpha(0);
            dTrunk.setAlpha(255);
            dTrunk.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        int engine = preferences.getInt(Names.ENGINE, 0);
        res |= state.setEngine(engine);
        if (engine > 0){
            dIgnition.setAlpha(255);
            int ignition = preferences.getInt(Names.IGNITION, 0);
            res |= state.setIgnition(ignition);
            if (ignition > 0){
                dIgnition.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
            }else{
                dIgnition.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
            }
        }else{
            dIgnition.setAlpha(0);
        }
        return res;
    }

    static ColorMatrix createMatrix(int color) {
        int red   = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue  = color & 0xFF;
        float matrix[] =
                {
                        red / 255f, 0, 0, 0, 0,
                        0, green / 255f, 0, 0, 0,
                        0, 0, blue / 255f, 0, 0,
                        0, 0, 0, 1, 0
                };
        return new ColorMatrix(matrix);
    }

}
