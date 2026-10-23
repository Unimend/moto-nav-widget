package com.unimend.motonav;

import android.util.Xml;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;

final class SceneSource {
    static JSONObject fetch(double lat,double lon)throws Exception{
        double dy=0.00215,dx=dy/Math.max(0.25,Math.cos(Math.toRadians(lat)));
        String bbox=String.format(Locale.US,"%.6f,%.6f,%.6f,%.6f",lon-dx,lat-dy,lon+dx,lat+dy);
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.openstreetmap.org/api/0.6/map?bbox="+bbox).openConnection();
        c.setConnectTimeout(6000);c.setReadTimeout(12000);c.setRequestProperty("Accept","application/xml");c.setRequestProperty("User-Agent","Unimend-MotoNavWidget/0.4 (manual cached scene preview)");
        int code=c.getResponseCode();if(code/100!=2)throw new IOException("OpenStreetMap HTTP "+code);
        try(InputStream input=c.getInputStream()){return parse(input);}
        finally{c.disconnect();}
    }

    private static JSONObject parse(InputStream input)throws Exception{
        HashMap<Long,double[]> nodes=new HashMap<>();JSONArray buildings=new JSONArray(),roads=new JSONArray();
        XmlPullParser x=Xml.newPullParser();x.setInput(input,"UTF-8");long wayId=0;ArrayList<Long> refs=null;boolean building=false;String highway=null;
        for(int event=x.getEventType();event!=XmlPullParser.END_DOCUMENT;event=x.next()){
            if(event==XmlPullParser.START_TAG){String name=x.getName();
                if("node".equals(name)){long id=longAttr(x,"id");nodes.put(id,new double[]{doubleAttr(x,"lat"),doubleAttr(x,"lon")});}
                else if("way".equals(name)){wayId=longAttr(x,"id");refs=new ArrayList<>();building=false;highway=null;}
                else if(refs!=null&&"nd".equals(name))refs.add(longAttr(x,"ref"));
                else if(refs!=null&&"tag".equals(name)){String k=x.getAttributeValue(null,"k"),v=x.getAttributeValue(null,"v");if("building".equals(k)&&!"no".equals(v))building=true;else if("highway".equals(k))highway=v;}
            }else if(event==XmlPullParser.END_TAG&&"way".equals(x.getName())&&refs!=null){
                if(building&&buildings.length()<16)addFeature(buildings,refs,nodes,"building",false);
                else if(highway!=null&&roads.length()<12)addFeature(roads,refs,nodes,"road","primary".equals(highway)||"secondary".equals(highway)||"trunk".equals(highway));
                refs=null;wayId=0;
            }
        }
        JSONArray features=new JSONArray();for(int i=0;i<buildings.length();i++)features.put(buildings.get(i));for(int i=0;i<roads.length();i++)features.put(roads.get(i));
        if(features.length()==0)throw new IOException("No mapped buildings or roads nearby");
        return new JSONObject().put("features",features).put("buildings",buildings.length()).put("roads",roads.length()).put("partial",false);
    }

    private static void addFeature(JSONArray out,List<Long> refs,Map<Long,double[]> nodes,String kind,boolean major)throws Exception{
        JSONArray points=new JSONArray();int step=Math.max(1,(int)Math.ceil(refs.size()/32.0));
        for(int i=0;i<refs.size()&&points.length()<32;i+=step){double[] p=nodes.get(refs.get(i));if(p!=null)points.put(new JSONArray().put(p[0]).put(p[1]));}
        if(points.length()<("building".equals(kind)?3:2))return;out.put(new JSONObject().put("kind",kind).put("major",major).put("points",points));
    }
    private static long longAttr(XmlPullParser x,String name){return Long.parseLong(x.getAttributeValue(null,name));}
    private static double doubleAttr(XmlPullParser x,String name){return Double.parseDouble(x.getAttributeValue(null,name));}
}
