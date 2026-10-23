package com.unimend.motonav;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class RouteStore {
    private final SharedPreferences prefs;
    RouteStore(Context context){prefs=context.getSharedPreferences("route_state",Context.MODE_PRIVATE);}
    void save(JSONObject route){prefs.edit().putString("route",route.toString()).putLong("time",System.currentTimeMillis()).apply();}
    JSONObject route(){try{return new JSONObject(prefs.getString("route","{}"));}catch(Exception e){return new JSONObject();}}
    boolean ready(){return route().optJSONArray("points")!=null;}
    String key(){return prefs.getString("amap_key","");}
    String destLon(){return prefs.getString("dest_lon","120.155100");}
    String destLat(){return prefs.getString("dest_lat","30.274100");}
    String address(){return prefs.getString("dest_address","");}
    void begin(String key,String address){prefs.edit().putString("amap_key",key.trim()).putString("dest_address",address.trim()).remove("route").remove("time").apply();}
    void config(String key,String address,String lon,String lat){prefs.edit().putString("amap_key",key.trim()).putString("dest_address",address.trim()).putString("dest_lon",lon.trim()).putString("dest_lat",lat.trim()).apply();}
}
