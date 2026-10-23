package com.unimend.motonav;

import android.content.Context;
import android.content.SharedPreferences;

final class NavStore {
    private final SharedPreferences prefs;
    NavStore(Context c) { prefs=c.getSharedPreferences("nav_state",Context.MODE_PRIVATE); }
    int step(int id) { return prefs.getInt("step_"+id,0); }
    boolean running(int id) { return prefs.getBoolean("running_"+id,false); }
    void next(int id) { prefs.edit().putInt("step_"+id,(step(id)+1)%4).apply(); }
    void running(int id,boolean value) { prefs.edit().putBoolean("running_"+id,value).apply(); }
    void delete(int id) { prefs.edit().remove("step_"+id).remove("running_"+id).apply(); }
    void live(String title,String text,String sub) { prefs.edit().putBoolean("amap_live",true).putString("amap_title",safe(title)).putString("amap_text",safe(text)).putString("amap_sub",safe(sub)).putLong("amap_seen",System.currentTimeMillis()).apply(); }
    void liveOff() { prefs.edit().putBoolean("amap_live",false).apply(); }
    boolean live() { return prefs.getBoolean("amap_live",false); }
    String liveTitle() { return prefs.getString("amap_title",""); }
    String liveText() { return prefs.getString("amap_text",""); }
    String liveSub() { return prefs.getString("amap_sub",""); }
    long liveSeen() { return prefs.getLong("amap_seen",0); }
    private String safe(String s) { return s==null?"":s.trim(); }
}

