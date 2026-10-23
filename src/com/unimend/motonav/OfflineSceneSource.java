package com.unimend.motonav;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.*;
import org.json.*;

final class OfflineSceneSource {
    private static final String ASSET="offline/hangzhou-core-v2.sqlite";
    private static final String FILE="hangzhou-core-v2.sqlite";
    private static final long ASSET_BYTES=18_829_312L;
    private static final double MIN_LON=119.95,MIN_LAT=30.10,MAX_LON=120.35,MAX_LAT=30.40;

    static JSONObject fetch(Context context,double lat,double lon)throws Exception{
        if(lon<MIN_LON||lon>MAX_LON||lat<MIN_LAT||lat>MAX_LAT)return null;
        File database=new File(context.getFilesDir(),FILE);ensureDatabase(context,database);
        SQLiteDatabase db=SQLiteDatabase.openDatabase(database.getPath(),null,SQLiteDatabase.OPEN_READONLY);
        try{
            int latE6=(int)Math.round(lat*1_000_000),lonE6=(int)Math.round(lon*1_000_000);
            int latDelta=(int)Math.ceil(1300.0/111_195.0*1_000_000.0);
            int lonDelta=(int)Math.ceil(1300.0/(111_195.0*Math.max(.25,Math.cos(Math.toRadians(lat))))*1_000_000.0);
            JSONArray features=new JSONArray();
            int areas=query(db,features,"areas","area",240,latE6,lonE6,latDelta,lonDelta);
            int buildings=query(db,features,"buildings","building",800,latE6,lonE6,latDelta,lonDelta);
            int lines=query(db,features,"lines","line",160,latE6,lonE6,latDelta,lonDelta);
            int roads=query(db,features,"roads","road",600,latE6,lonE6,latDelta,lonDelta);
            JSONArray labels=queryLabels(db,latE6,lonE6,latDelta,lonDelta);
            if(features.length()==0)return null;
            return new JSONObject().put("features",features).put("labels",labels).put("roads",roads).put("buildings",buildings).put("areas",areas).put("lines",lines).put("partial",false).put("offline",true);
        }finally{db.close();}
    }

    private static int query(SQLiteDatabase db,JSONArray out,String table,String kind,int limit,int lat,int lon,int dy,int dx)throws Exception{
        String sql="SELECT f.class,f.points FROM "+table+" f "+
            "WHERE f.max_lat_e6>=? AND f.min_lat_e6<=? AND f.max_lon_e6>=? AND f.min_lon_e6<=? "+
            "ORDER BY abs(((f.min_lat_e6+f.max_lat_e6)/2)-?)+abs(((f.min_lon_e6+f.max_lon_e6)/2)-?), f.id LIMIT "+limit;
        Cursor c=db.rawQuery(sql,new String[]{String.valueOf(lat-dy),String.valueOf(lat+dy),String.valueOf(lon-dx),String.valueOf(lon+dx),String.valueOf(lat),String.valueOf(lon)});
        int count=0;try{while(c.moveToNext()){byte[] bytes=c.getBlob(1);if(bytes==null||bytes.length<16)continue;ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);JSONArray points=new JSONArray();while(b.remaining()>=8&&points.length()<96)points.put(new JSONArray().put(b.getInt()/1_000_000.0).put(b.getInt()/1_000_000.0));if(points.length()<(("building".equals(kind)||"area".equals(kind))?3:2))continue;int cls=c.getInt(0);out.put(new JSONObject().put("kind",kind).put("class",cls).put("major","road".equals(kind)&&cls<=2).put("points",points));count++;}}finally{c.close();}return count;
    }

    private static JSONArray queryLabels(SQLiteDatabase db,int lat,int lon,int dy,int dx)throws Exception{
        JSONArray out=new JSONArray();Cursor c=db.rawQuery("SELECT name,class,lat_e6,lon_e6 FROM labels WHERE lat_e6 BETWEEN ? AND ? AND lon_e6 BETWEEN ? AND ? ORDER BY class, length(name) LIMIT 260",new String[]{String.valueOf(lat-dy),String.valueOf(lat+dy),String.valueOf(lon-dx),String.valueOf(lon+dx)});
        try{while(c.moveToNext())out.put(new JSONObject().put("name",c.getString(0)).put("class",c.getInt(1)).put("lat",c.getInt(2)/1_000_000.0).put("lon",c.getInt(3)/1_000_000.0));}finally{c.close();}return out;
    }

    private static void ensureDatabase(Context context,File output)throws Exception{
        long expected=ASSET_BYTES;
        if(output.isFile()&&output.length()==expected)return;
        File temp=new File(output.getPath()+".tmp");try(InputStream in=context.getAssets().open(ASSET);OutputStream out=new FileOutputStream(temp)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);}
        if(temp.length()!=expected){temp.delete();throw new IOException("Offline map copy is incomplete");}
        if(output.exists()&&!output.delete())throw new IOException("Cannot replace offline map");if(!temp.renameTo(output))throw new IOException("Cannot install offline map");
    }
}
