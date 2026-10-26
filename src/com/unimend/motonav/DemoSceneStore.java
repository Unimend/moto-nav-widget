package com.unimend.motonav;

import org.json.JSONObject;

final class DemoSceneStore {
    private static JSONObject data;
    private static double lat=Double.NaN,lon=Double.NaN;
    static synchronized void set(JSONObject value,double centerLat,double centerLon){data=value;lat=centerLat;lon=centerLon;}
    static synchronized JSONObject data(){return data;}
    static synchronized boolean ready(){return data!=null&&data.optJSONArray("features")!=null;}
    static synchronized double lat(){return lat;}
    static synchronized double lon(){return lon;}
    static synchronized void clear(){data=null;lat=Double.NaN;lon=Double.NaN;}
}
