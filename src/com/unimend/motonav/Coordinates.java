package com.unimend.motonav;

final class Coordinates {
    private static final double PI=Math.PI,A=6378245.0,EE=0.00669342162296594323;
    static double[] wgsToGcj(double lat,double lon){if(outside(lat,lon))return new double[]{lat,lon};double dLat=latDelta(lon-105,lat-35),dLon=lonDelta(lon-105,lat-35),rad=lat/180*PI,magic=Math.sin(rad);magic=1-EE*magic*magic;double root=Math.sqrt(magic);dLat=dLat*180/((A*(1-EE))/(magic*root)*PI);dLon=dLon*180/(A/root*Math.cos(rad)*PI);return new double[]{lat+dLat,lon+dLon};}
    static double[] gcjToWgs(double lat,double lon){double wLat=lat,wLon=lon;for(int i=0;i<4;i++){double[] g=wgsToGcj(wLat,wLon);wLat-=g[0]-lat;wLon-=g[1]-lon;}return new double[]{wLat,wLon};}
    private static boolean outside(double lat,double lon){return lon<72.004||lon>137.8347||lat<0.8293||lat>55.8271;}
    private static double latDelta(double x,double y){double r=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x));r+=(20*Math.sin(6*x*PI)+20*Math.sin(2*x*PI))*2/3;r+=(20*Math.sin(y*PI)+40*Math.sin(y/3*PI))*2/3;r+=(160*Math.sin(y/12*PI)+320*Math.sin(y*PI/30))*2/3;return r;}
    private static double lonDelta(double x,double y){double r=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x));r+=(20*Math.sin(6*x*PI)+20*Math.sin(2*x*PI))*2/3;r+=(20*Math.sin(x*PI)+40*Math.sin(x/3*PI))*2/3;r+=(150*Math.sin(x/12*PI)+300*Math.sin(x/30*PI))*2/3;return r;}
}
