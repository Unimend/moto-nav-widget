package com.unimend.motonav;

import android.content.*;

final class TrackingStore {
    private final SharedPreferences p;
    TrackingStore(Context c){p=c.getSharedPreferences("tracking",Context.MODE_PRIVATE);}
    boolean enabled(){return p.getBoolean("enabled",true);}
    void enabled(boolean value){p.edit().putBoolean("enabled",value).apply();}
}
