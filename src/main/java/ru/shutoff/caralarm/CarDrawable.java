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
    static final int NORMAL = 0x6180A0;
    static final int GUARD = 0x6D936D;
    static final int ALARM = 0xC04141;
    static final int BLACK = 0x000000;

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
    Drawable dValet;

    LayerDrawable drawable;

    int width;
    int height;

    CarDrawable(Context ctx) {
        dBg = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bg));
        dBg.setColorFilter(new ColorMatrixColorFilter(createMatrix(BLACK)));

        Bitmap bmpCar = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.car);
        width = bmpCar.getWidth();
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

        dValet = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeResource(ctx.getResources(), R.drawable.valet));
        dValet.setAlpha(255);

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
                        dIgnition,
                        dValet
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

    void update(SharedPreferences preferences) {
        int color = UNKNOWN;
        int alarm = UNKNOWN;

        if (!preferences.contains(Names.Guard)){
            dLock.setAlpha(0);
            dUnlock.setAlpha(0);
        }else if (preferences.getBoolean(Names.Guard, false)){
            dLock.setAlpha(255);
            dUnlock.setAlpha(0);
            color = GUARD;
            alarm = ALARM;
        }else{
            dLock.setAlpha(0);
            dUnlock.setAlpha(255);
            color = NORMAL;
            alarm = NORMAL;
        }

        if (preferences.getBoolean(Names.ZoneAccessory, false)){
            dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        } else {
            dCar.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        Drawable d;
        if (preferences.getBoolean(Names.Input1, false)){
            dDoors.setAlpha(0);
            dDoorsOpen.setAlpha(255);
            d = dDoorsOpen;
        }else{
            dDoorsOpen.setAlpha(0);
            dDoors.setAlpha(255);
            d = dDoors;
        }
        if (preferences.getBoolean(Names.ZoneDoor, false)){
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        } else {
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        if (preferences.getBoolean(Names.Input4, false)){
            dHood.setAlpha(0);
            dHoodOpen.setAlpha(255);
            d = dHoodOpen;
        }else{
            dHoodOpen.setAlpha(0);
            dHood.setAlpha(255);
            d = dHood;
        }
        if (preferences.getBoolean(Names.ZoneHood, false)){
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        } else {
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        if (preferences.getBoolean(Names.Input2, false)){
            dTrunk.setAlpha(0);
            dTrunkOpen.setAlpha(255);
            d = dTrunkOpen;
        }else{
            dTrunkOpen.setAlpha(0);
            dTrunk.setAlpha(255);
            d = dTrunk;
        }
        if (preferences.getBoolean(Names.ZoneTrunk, false)){
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
        } else {
            d.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
        }

        if (preferences.getBoolean(Names.Input3, false)){
            dIgnition.setAlpha(255);
            if (preferences.getBoolean(Names.ZoneIgnition, false)){
                dIgnition.setColorFilter(new ColorMatrixColorFilter(createMatrix(alarm)));
            } else {
                dIgnition.setColorFilter(new ColorMatrixColorFilter(createMatrix(color)));
            }
        } else {
            dIgnition.setAlpha(0);
        }
    }

    static ColorMatrix createMatrix(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
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
