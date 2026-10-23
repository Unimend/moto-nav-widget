package com.unimend.motonav;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

final class AmapGeocoder {
    static Result resolve(String key,String query)throws Exception{
        Result[] results=search(key,query);return results[0];
    }
    static Result[] search(String key,String query,double originLat,double originLon)throws Exception{
        if(key==null||!key.trim().matches("[0-9a-fA-F]{32}"))throw new IOException("高德 Web 服务 Key 应为 32 位字符");
        if(query==null||query.trim().length()<2)throw new IOException("请输入有效地址或地点名称");
        try{return resolveNearby(key.trim(),query.trim(),originLat,originLon);}catch(IOException ignored){return search(key,query);}
    }
    static Result[] search(String key,String query)throws Exception{
        if(key==null||!key.trim().matches("[0-9a-fA-F]{32}"))throw new IOException("高德 Web 服务 Key 应为 32 位字符");
        if(query==null||query.trim().length()<2)throw new IOException("请输入有效地址或地点名称");
        IOException poiError;
        try{return resolvePois(key.trim(),query.trim());}catch(IOException e){poiError=e;}
        try{return new Result[]{resolveAddress(key.trim(),query.trim())};}catch(IOException e){
            if(isKeyError(poiError.getMessage()))throw poiError;
            throw new IOException("地点搜索失败："+friendly(poiError.getMessage())+"；地址解析失败："+friendly(e.getMessage()));
        }
    }
    private static Result[] resolveNearby(String key,String query,double originLat,double originLon)throws IOException{
        double[] gcj=Coordinates.wgsToGcj(originLat,originLon);String location=String.format(java.util.Locale.US,"%.6f,%.6f",gcj[1],gcj[0]);String url="https://restapi.amap.com/v5/place/around?key="+enc(key)+"&keywords="+enc(query)+"&location="+enc(location)+"&radius=15000&sortrule=distance&region="+enc("杭州")+"&city_limit=true&show_fields=navi&page_size=5&output=json";return parsePois(request(url,"附近地点搜索"),query);
    }
    private static Result[] resolvePois(String key,String query)throws IOException{
        String url="https://restapi.amap.com/v5/place/text?key="+enc(key)+"&keywords="+enc(query)+"&region="+enc("杭州")+"&city_limit=true&show_fields=navi&page_size=5&output=json";
        return parsePois(request(url,"地点搜索"),query);
    }
    private static Result[] parsePois(JSONObject root,String query)throws IOException{JSONArray pois=root.optJSONArray("pois");if(pois==null||pois.length()==0)throw new IOException("没有找到匹配地点");java.util.ArrayList<Result> out=new java.util.ArrayList<>();for(int i=0;i<pois.length()&&out.size()<5;i++){JSONObject poi=pois.optJSONObject(i);if(poi==null)continue;String location=poi.optString("location");JSONObject navi=poi.optJSONObject("navi");if(navi!=null&&navi.optString("entr_location").contains(","))location=navi.optString("entr_location");String name=poi.optString("name",query),address=poi.optString("address","");if(address.length()>0&&!address.equals("[]"))name=name+" · "+address;try{Result base=result(location,name);out.add(new Result(base.lat,base.lon,base.name,poi.optInt("distance",-1)));}catch(IOException ignored){}}if(out.isEmpty())throw new IOException("地点结果没有有效坐标");return out.toArray(new Result[out.size()]);}
    private static Result resolveAddress(String key,String address)throws IOException{
        String url="https://restapi.amap.com/v3/geocode/geo?key="+enc(key)+"&address="+enc(address)+"&city="+enc("杭州")+"&output=json";JSONObject root=request(url,"地址解析");JSONArray items=root.optJSONArray("geocodes");if(items==null||items.length()==0)throw new IOException("没有找到该地址");JSONObject item=items.optJSONObject(0);if(item==null)throw new IOException("地址结果无效");return result(item.optString("location"),item.optString("formatted_address",address));
    }
    private static JSONObject request(String url,String label)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","Unimend-MotoNavWidget/0.8");int code=c.getResponseCode();InputStream stream=code/100==2?c.getInputStream():c.getErrorStream();String body=read(stream);c.disconnect();if(code/100!=2)throw new IOException(label+" HTTP "+code);try{JSONObject root=new JSONObject(body);if(!"1".equals(root.optString("status")))throw new IOException(friendly(root.optString("info",label+"失败")));return root;}catch(JSONException e){throw new IOException(label+"返回无效数据");}
    }
    private static Result result(String location,String name)throws IOException{
        String[] xy=location.split(",");if(xy.length!=2)throw new IOException("地点没有有效坐标");try{double[] wgs=Coordinates.gcjToWgs(Double.parseDouble(xy[1]),Double.parseDouble(xy[0]));return new Result(wgs[0],wgs[1],name);}catch(NumberFormatException e){throw new IOException("地点坐标格式错误");}
    }
    private static String friendly(String info){if("INVALID_USER_KEY".equals(info))return "Key 无效，请检查是否为 Web 服务 Key";if("ENGINE_RESPONSE_DATA_ERROR".equals(info))return "服务端无法解析该查询";return info;}
    private static boolean isKeyError(String info){return info!=null&&(info.contains("Key 无效")||info.contains("INVALID_USER_KEY"));}
    private static String enc(String value)throws UnsupportedEncodingException{return URLEncoder.encode(value,"UTF-8");}
    static final class Result {final double lat,lon;final String name;final int distance;Result(double lat,double lon,String name){this(lat,lon,name,-1);}Result(double lat,double lon,String name,int distance){this.lat=lat;this.lon=lon;this.name=name;this.distance=distance;}}
    private static String read(InputStream input)throws IOException{if(input==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=input.read(b))>0)out.write(b,0,n);input.close();return new String(out.toByteArray(),StandardCharsets.UTF_8);}
}
