package com.unimend.motonav;

import android.content.*;

final class LiveLocationStore {
    private final SharedPreferences p;
    LiveLocationStore(Context c){p=c.getSharedPreferences("live_location",Context.MODE_PRIVATE);}
    void update(double lat,double lon,float accuracy){p.edit().putLong("lat",Double.doubleToRawLongBits(lat)).putLong("lon",Double.doubleToRawLongBits(lon)).putFloat("accuracy",accuracy).putLong("time",System.currentTimeMillis()).apply();}
    void bearing(float value){p.edit().putFloat("bearing",value).putLong("bearing_time",System.currentTimeMillis()).apply();}
    double lat(){return Double.longBitsToDouble(p.getLong("lat",Double.doubleToRawLongBits(Double.NaN)));}
    double lon(){return Double.longBitsToDouble(p.getLong("lon",Double.doubleToRawLongBits(Double.NaN)));}
    long time(){return p.getLong("time",0);}
    float bearing(){return p.getFloat("bearing",Float.NaN);}
    boolean hasBearing(){return !Float.isNaN(bearing())&&System.currentTimeMillis()-p.getLong("bearing_time",0)<10000L;}
    boolean ready(){return !Double.isNaN(lat())&&!Double.isNaN(lon());}
}
