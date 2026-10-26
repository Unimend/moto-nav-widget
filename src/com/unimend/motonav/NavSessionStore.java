package com.unimend.motonav;

import android.content.*;

final class NavSessionStore {
    private final SharedPreferences p;
    NavSessionStore(Context c){p=c.getSharedPreferences("navigation_session",Context.MODE_PRIVATE);}
    boolean active(){return p.getBoolean("active",false);}
    void start(){p.edit().putBoolean("active",true).putLong("started",System.currentTimeMillis()).apply();}
    void stop(){p.edit().clear().apply();}
    void update(double lat,double lon,float bearing,float speed,int routeIndex,int nextIndex,int nextDistance,int remaining,int duration,String instruction,String road,String action,boolean offRoute){p.edit().putBoolean("active",true).putLong("lat",Double.doubleToRawLongBits(lat)).putLong("lon",Double.doubleToRawLongBits(lon)).putFloat("bearing",bearing).putFloat("speed",speed).putInt("route_index",routeIndex).putInt("next_index",nextIndex).putInt("next_distance",nextDistance).putInt("remaining",remaining).putInt("duration",duration).putString("instruction",instruction==null?"":instruction).putString("road",road==null?"":road).putString("action",action==null?"":action).putBoolean("off_route",offRoute).putLong("fix_time",System.currentTimeMillis()).apply();}
    double lat(){return Double.longBitsToDouble(p.getLong("lat",Double.doubleToRawLongBits(Double.NaN)));}
    double lon(){return Double.longBitsToDouble(p.getLong("lon",Double.doubleToRawLongBits(Double.NaN)));}
    float bearing(){return p.getFloat("bearing",0);}
    void bearing(float value){p.edit().putFloat("bearing",value).apply();}
    float speed(){return p.getFloat("speed",0);}
    int nextDistance(){return p.getInt("next_distance",0);}
    int remaining(){return p.getInt("remaining",0);}
    int duration(){return p.getInt("duration",0);}
    String instruction(){return p.getString("instruction","");}
    String road(){return p.getString("road","");}
    String action(){return p.getString("action","");}
    boolean offRoute(){return p.getBoolean("off_route",false);}
    boolean rerouting(){return p.getBoolean("rerouting",false);}
    void rerouting(boolean value){p.edit().putBoolean("rerouting",value).apply();}
    long fixTime(){return p.getLong("fix_time",0);}
}
