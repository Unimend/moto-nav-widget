package com.unimend.motonav;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class SceneStore {
    private final SharedPreferences prefs;
    SceneStore(Context c){prefs=c.getSharedPreferences("map_scene",Context.MODE_PRIVATE);}
    void save(JSONObject data,double lat,double lon){prefs.edit().putString("json",data.toString()).putLong("lat",Double.doubleToRawLongBits(lat)).putLong("lon",Double.doubleToRawLongBits(lon)).putLong("time",System.currentTimeMillis()).apply();}
    JSONObject data(){try{return new JSONObject(prefs.getString("json","{}"));}catch(Exception e){return new JSONObject();}}
    double lat(){return Double.longBitsToDouble(prefs.getLong("lat",Double.doubleToRawLongBits(Double.NaN)));}
    double lon(){return Double.longBitsToDouble(prefs.getLong("lon",Double.doubleToRawLongBits(Double.NaN)));}
    long time(){return prefs.getLong("time",0);}
    boolean ready(){return data().optJSONArray("features")!=null&&!Double.isNaN(lat())&&!Double.isNaN(lon());}
}
