package com.unimend.motonav;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class AmapRouteSource {
    static JSONObject fetch(String key,double originLat,double originLon,double destinationLat,double destinationLon)throws Exception{
        if(key==null||key.trim().length()<8)throw new IOException("请先填写高德 Web 服务 Key");
        double[] origin=Coordinates.wgsToGcj(originLat,originLon),destination=Coordinates.wgsToGcj(destinationLat,destinationLon);
        String url="https://restapi.amap.com/v5/direction/driving?key="+URLEncoder.encode(key.trim(),"UTF-8")+"&origin="+fmt(origin[1])+","+fmt(origin[0])+"&destination="+fmt(destination[1])+","+fmt(destination[0])+"&strategy=32&show_fields=cost,polyline,navi";
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(15000);connection.setRequestProperty("User-Agent","Unimend-MotoNavWidget/0.8");
        int code=connection.getResponseCode();InputStream stream=code/100==2?connection.getInputStream():connection.getErrorStream();String body=read(stream);connection.disconnect();if(code/100!=2)throw new IOException("高德 HTTP "+code);
        JSONObject root=new JSONObject(body);if(!"1".equals(root.optString("status")))throw new IOException(root.optString("info","路线请求失败"));JSONArray paths=root.optJSONObject("route").optJSONArray("paths");if(paths==null||paths.length()==0)throw new IOException("没有可用路线");JSONObject path=paths.getJSONObject(0);JSONArray steps=path.optJSONArray("steps"),points=new JSONArray(),maneuvers=new JSONArray();
        if(steps!=null)for(int i=0;i<steps.length();i++){JSONObject step=steps.getJSONObject(i);int pointIndex=points.length();String polyline=step.optString("polyline");for(String pair:polyline.split(";")){String[] xy=pair.split(",");if(xy.length!=2)continue;double[] wgs=Coordinates.gcjToWgs(Double.parseDouble(xy[1]),Double.parseDouble(xy[0]));if(points.length()>0){JSONArray last=points.optJSONArray(points.length()-1);if(last!=null&&Math.abs(last.optDouble(0)-wgs[0])<1e-8&&Math.abs(last.optDouble(1)-wgs[1])<1e-8)continue;}points.put(new JSONArray().put(wgs[0]).put(wgs[1]));}maneuvers.put(new JSONObject().put("point",pointIndex).put("road",step.optString("road_name")).put("instruction",step.optString("instruction")).put("action",step.optString("action")).put("distance",step.optInt("step_distance")));}
        if(points.length()<2)throw new IOException("路线缺少几何数据");return new JSONObject().put("points",points).put("maneuvers",maneuvers).put("distance",path.optInt("distance")).put("duration",path.optJSONObject("cost")!=null?path.optJSONObject("cost").optInt("duration"):0).put("created",System.currentTimeMillis());
    }
    private static String fmt(double value){return String.format(java.util.Locale.US,"%.6f",value);}
    private static String read(InputStream input)throws IOException{if(input==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))>0)out.write(buffer,0,n);input.close();return new String(out.toByteArray(),StandardCharsets.UTF_8);}
}
