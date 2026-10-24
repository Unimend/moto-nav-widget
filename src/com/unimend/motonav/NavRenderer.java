package com.unimend.motonav;

import android.content.Context;
import android.graphics.*;
import java.util.Locale;
import java.util.HashSet;
import org.json.*;

final class NavRenderer {
    private static final int WHITE=Color.WHITE, GREY=Color.rgb(139,139,139), DARK=Color.rgb(36,36,36), RED=Color.rgb(235,48,54), GREEN=Color.rgb(65,191,111);
    private final Canvas c; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final float w,h,s;
    private final Typeface doto,zpix,sans=Typeface.create("sans-serif",Typeface.BOLD); private final Context context; private final double previewLat,previewLon,previewActualLat,previewActualLon; private final JSONObject previewData; private final NavSessionStore session; private final boolean navigating;
    private NavRenderer(Context context,Bitmap b,double previewLat,double previewLon,JSONObject previewData,double previewActualLat,double previewActualLon) { this.context=context.getApplicationContext();this.previewLat=previewLat;this.previewLon=previewLon;this.previewData=previewData;this.previewActualLat=previewActualLat;this.previewActualLon=previewActualLon;session=new NavSessionStore(context);navigating=session.active()&&!Double.isNaN(session.lat());c=new Canvas(b); w=b.getWidth();h=b.getHeight();s=h/360f;c.scale(s,s);doto=Typeface.createFromAsset(context.getAssets(),"doto.ttf");zpix=Typeface.createFromAsset(context.getAssets(),"zpix.ttf"); }
    static Bitmap render(Context context,int width,int height,NavStore store,int id,int frame) {
        Bitmap b=Bitmap.createBitmap(Math.max(480,width),Math.max(180,height),Bitmap.Config.ARGB_8888); b.setDensity(Bitmap.DENSITY_NONE);
        new NavRenderer(context,b,Double.NaN,Double.NaN,null,Double.NaN,Double.NaN).draw(store,id,frame); return b;
    }
    static Bitmap renderPreview(Context context,int width,int height,NavStore store,int id,double lat,double lon) {Bitmap b=Bitmap.createBitmap(Math.max(480,width),Math.max(180,height),Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);new NavRenderer(context,b,lat,lon,null,Double.NaN,Double.NaN).draw(store,id,-1);return b;}
    static Bitmap renderPreviewData(Context context,int width,int height,NavStore store,int id,double lat,double lon,JSONObject data,double actualLat,double actualLon) {Bitmap b=Bitmap.createBitmap(Math.max(480,width),Math.max(180,height),Bitmap.Config.ARGB_8888);b.setDensity(Bitmap.DENSITY_NONE);new NavRenderer(context,b,lat,lon,data,actualLat,actualLon).draw(store,id,-1);return b;}
    private void draw(NavStore store,int id,int frame) {
        float W=w/s;
        c.drawColor(Color.TRANSPARENT);
        Path mask=new Path();mask.addRoundRect(new RectF(0,0,W,360),16,16,Path.Direction.CW);c.clipPath(mask);
        c.drawColor(Color.BLACK);
        if(!realScene(W)){
            text("OSM MAP",W/2,154,42,WHITE,doto,Align.CENTER);
            text("点击载入当前位置",W/2,207,25,GREY,zpix,Align.CENTER);
        }else if(navigating)drawNavigationOverlay(W);
    }
    private void drawAmap(NavStore store,int frame){
        float W=w/s;
        realScene(W);
        float divider=Math.max(690,W*.61f);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(15);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(WHITE);c.drawLine(122,239,122,107,p);p.setStyle(Paint.Style.FILL);p.setColor(RED);c.drawCircle(122,88,13,p);c.drawCircle(122,260,7,p);
        text("AMAP",70,47,28,RED,doto,Align.CENTER);
        String message=clean(store.liveText());
        text("高德导航中",262,108,58,WHITE,zpix,Align.LEFT);
        text(message.length()==0?"后台导航已连接":message,262,170,31,WHITE,zpix,Align.LEFT);
        text("LIVE · NOTIFICATION",262,218,24,GREEN,doto,Align.LEFT);
        text("等待下一转向数据",262,266,27,GREY,zpix,Align.LEFT);
        p.setStyle(Paint.Style.FILL);p.setColor(RED);c.drawRect(divider-13,42,divider-7,76,p);
        text("OSM SCENE",divider+12,57,21,GREY,doto,Align.LEFT);
        progress(42,315,W-84,.18f);pageDots(W-31,frame,0);
    }
    private boolean realScene(float W){
        SceneStore scene=new SceneStore(context);if(!scene.ready()&&previewData==null)return false;JSONObject data=previewData!=null?previewData:scene.data();JSONArray fs=data.optJSONArray("features");if(fs==null)return false;double baseActualLat=!Double.isNaN(previewActualLat)?previewActualLat:scene.lat(),baseActualLon=!Double.isNaN(previewActualLon)?previewActualLon:scene.lon(),actualLat=navigating?session.lat():baseActualLat,actualLon=navigating?session.lon():baseActualLon,lat0=navigating?actualLat:(Double.isNaN(previewLat)?actualLat:previewLat),lon0=navigating?actualLon:(Double.isNaN(previewLon)?actualLon:previewLon),cos=Math.cos(Math.toRadians(lat0));float inset=10,cx=W/2f,cy=navigating?235f:180f,viewMeters=1040f;if(navigating){if(session.nextDistance()<140)viewMeters=600f;else if(session.speed()>16)viewMeters=1800f;else if(session.speed()>7)viewMeters=1350f;else viewMeters=900f;}float scale=Math.min(W-inset*2,360f-inset*2)/viewMeters;Path viewport=new Path();viewport.addRoundRect(new RectF(inset,inset,W-inset,360-inset),12,12,Path.Direction.CW);int viewportSave=c.save();c.clipPath(viewport);int rotationSave=c.save();if(navigating)c.rotate(-session.bearing(),cx,cy);
        for(int pass=0;pass<4;pass++)for(int i=0;i<fs.length();i++){JSONObject f=fs.optJSONObject(i);if(f==null)continue;String kind=f.optString("kind");int wanted="area".equals(kind)?0:("building".equals(kind)?1:("line".equals(kind)?2:3));if(wanted!=pass)continue;JSONArray pts=f.optJSONArray("points");if(pts==null||pts.length()<2)continue;Path path=path(pts,lat0,lon0,cos,cx,cy,scale);int cls=f.optInt("class");
            if(pass==0){path.close();p.setStyle(Paint.Style.FILL);p.setColor(cls==0?Color.rgb(13,29,34):(cls<=3?Color.rgb(20,27,23):Color.rgb(24,25,26)));c.drawPath(path,p);}
            else if(pass==1){path.close();p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(28,29,30));c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(Color.rgb(50,52,54));c.drawPath(path,p);}
            else if(pass==2){p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(cls==0?2.8f:1.4f);p.setColor(cls==0?Color.rgb(34,68,78):Color.rgb(74,74,74));c.drawPath(path,p);}
            else {p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);float sw=cls==0?4.8f:(cls<=2?3.5f:(cls==3?2.3f:1.3f));p.setStrokeWidth(sw+2.0f);p.setColor(Color.rgb(8,9,10));c.drawPath(path,p);p.setStrokeWidth(sw);int roadColor=cls==0?Color.rgb(183,48,58):(cls==1?Color.rgb(205,199,168):(cls==2?Color.rgb(126,145,156):(cls==3?Color.rgb(103,106,109):Color.rgb(64,67,69))));p.setColor(roadColor);c.drawPath(path,p);}
        }
        drawRoute(lat0,lon0,cos,cx,cy,scale);
        JSONArray labels=data.optJSONArray("labels");if(labels!=null){RectF[] used=new RectF[48];HashSet<String> names=new HashSet<>();int usedCount=0;for(int i=0;i<labels.length()&&usedCount<used.length;i++){JSONObject l=labels.optJSONObject(i);String name=l==null?"":l.optString("name");if(name.length()==0||name.length()>18||names.contains(name))continue;int cls=l.optInt("class",2);float size=cls==0?8.5f:9.2f;float x=cx+(float)((l.optDouble("lon")-lon0)*111320*cos)*scale,y=cy-(float)((l.optDouble("lat")-lat0)*111320)*scale;if(x<22||x>W-22||y<18||y>342)continue;String[] rows=labelRows(name);p.setTypeface(zpix);p.setTextSize(size);p.setAntiAlias(false);float widest=0;Rect b=new Rect();for(String row:rows){p.getTextBounds(row,0,row.length(),b);widest=Math.max(widest,b.width());}float total=rows.length==1?size:size*2.35f;RectF box=new RectF(x-widest/2f-3,y-total/2f-3,x+widest/2f+3,y+total/2f+3);if(box.left<14||box.right>W-14||box.top<14||box.bottom>346)continue;boolean overlap=false;for(int u=0;u<usedCount;u++)if(RectF.intersects(box,used[u])){overlap=true;break;}if(overlap)continue;names.add(name);used[usedCount++]=box;int labelSave=c.save();if(navigating)c.rotate(session.bearing(),x,y);mapLabel(rows,x,y,size,cls==0?Color.rgb(220,220,220):Color.rgb(181,181,181));c.restoreToCount(labelSave);}}
        c.restoreToCount(rotationSave);float markerX=navigating?cx:cx+(float)((actualLon-lon0)*111320*cos)*scale,markerY=navigating?cy:cy-(float)((actualLat-lat0)*111320)*scale;if(markerX>inset&&markerX<W-inset&&markerY>inset&&markerY<360-inset){float pulse=(System.currentTimeMillis()%1400)/1400f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(Color.argb(navigating?(int)(70*(1-pulse)):40,235,48,54));c.drawCircle(markerX,markerY,7+pulse*5,p);if(navigating){Path pointer=new Path();pointer.moveTo(markerX,markerY-10);pointer.lineTo(markerX-5,markerY+6);pointer.lineTo(markerX,markerY+3);pointer.lineTo(markerX+5,markerY+6);pointer.close();p.setStyle(Paint.Style.FILL);p.setColor(RED);c.drawPath(pointer,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setColor(WHITE);c.drawPath(pointer,p);}else{p.setStyle(Paint.Style.FILL);p.setColor(RED);c.drawCircle(markerX,markerY,3.5f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setColor(WHITE);c.drawCircle(markerX,markerY,6,p);}}
        text("© OPENSTREETMAP",18,336,10,Color.rgb(132,132,132),doto,Align.LEFT);c.restoreToCount(viewportSave);return true;
    }
    private Path path(JSONArray pts,double lat0,double lon0,double cos,float cx,float cy,float scale){Path path=new Path();for(int j=0;j<pts.length();j++){JSONArray q=pts.optJSONArray(j);if(q==null)continue;float x=cx+(float)((q.optDouble(1)-lon0)*111320*cos)*scale,y=cy-(float)((q.optDouble(0)-lat0)*111320)*scale;if(j==0)path.moveTo(x,y);else path.lineTo(x,y);}return path;}
    private void drawRoute(double lat0,double lon0,double cos,float cx,float cy,float scale){JSONArray points=new RouteStore(context).route().optJSONArray("points");if(points==null||points.length()<2)return;Path route=path(points,lat0,lon0,cos,cx,cy,scale);p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(8.5f);p.setColor(Color.rgb(4,5,6));c.drawPath(route,p);p.setStrokeWidth(5.2f);p.setColor(Color.WHITE);c.drawPath(route,p);}
    private void drawNavigationOverlay(float W){boolean stale=System.currentTimeMillis()-session.fixTime()>30000;p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(225,0,0,0));c.drawRoundRect(new RectF(18,18,W-18,91),10,10,p);drawTurnIcon(50,54,session.action(),session.instruction(),session.offRoute()?RED:WHITE);String title=stale?"等待定位":(session.offRoute()?"偏离路线":shortText(session.instruction(),9));text(title,79,43,18,session.offRoute()?RED:WHITE,zpix,Align.LEFT);text(distanceText(session.nextDistance()),W-30,43,23,WHITE,doto,Align.RIGHT);text(shortText(session.road(),10),79,72,13,GREY,zpix,Align.LEFT);text("剩余 "+distanceText(session.remaining())+" · "+timeText(session.duration()),W-30,72,13,GREY,zpix,Align.RIGHT);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(28,28,28));c.drawRect(22,347,W-22,350,p);p.setColor(RED);float total=Math.max(1,new RouteStore(context).route().optInt("distance",session.remaining()));float done=Math.max(0,Math.min(1,1-session.remaining()/total)),end=22+(W-44)*done;c.drawRect(22,347,end,350,p);float pulse=(System.currentTimeMillis()%1200)/1200f;p.setColor(Color.argb((int)(190-100*pulse),235,48,54));c.drawCircle(end,348.5f,3+2*pulse,p);}
    private void drawTurnIcon(float x,float y,String action,String instruction,int color){String value=((action==null?"":action)+" "+(instruction==null?"":instruction)).toLowerCase(Locale.ROOT);boolean left=value.contains("左"),right=value.contains("右"),around=value.contains("掉头")||value.contains("调头");p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4.2f);p.setStrokeCap(Paint.Cap.SQUARE);p.setStrokeJoin(Paint.Join.MITER);p.setColor(color);Path q=new Path();if(around){q.moveTo(x+7,y+15);q.lineTo(x+7,y-4);q.quadTo(x+7,y-15,x-4,y-15);q.quadTo(x-15,y-15,x-15,y-4);q.lineTo(x-15,y+4);q.moveTo(x-22,y-2);q.lineTo(x-15,y+6);q.lineTo(x-8,y-2);}else if(left||right){float dir=left?-1:1;q.moveTo(x,y+15);q.lineTo(x,y-5);q.quadTo(x,y-13,x+dir*8,y-13);q.lineTo(x+dir*16,y-13);q.moveTo(x+dir*10,y-19);q.lineTo(x+dir*17,y-13);q.lineTo(x+dir*10,y-7);}else{q.moveTo(x,y+16);q.lineTo(x,y-16);q.moveTo(x-7,y-9);q.lineTo(x,y-17);q.lineTo(x+7,y-9);}c.drawPath(q,p);}
    private String shortText(String value,int max){if(value==null||value.length()==0)return "继续行驶";return value.length()>max?value.substring(0,max):value;}
    private String distanceText(int meters){return meters>=1000?String.format(Locale.US,"%.1fKM",meters/1000f):meters+"M";}
    private String timeText(int seconds){int minutes=Math.max(1,seconds/60);return minutes>=60?(minutes/60)+"H"+(minutes%60)+"M":minutes+"MIN";}
    private String[] labelRows(String value){if(value.length()<=7)return new String[]{value};int cut=(value.length()+1)/2;return new String[]{value.substring(0,cut),value.substring(cut)};}
    private void mapLabel(String[] rows,float x,float y,float size,int color){p.setTypeface(zpix);p.setTextSize(size);p.setAntiAlias(false);float gap=size*1.25f,first=y-(rows.length-1)*gap/2f;for(int i=0;i<rows.length;i++){String value=rows[i];Rect r=new Rect();p.getTextBounds(value,0,value.length(),r);float tx=x-(r.left+r.right)/2f,base=first+i*gap-(r.top+r.bottom)/2f;p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(3.2f);p.setColor(Color.rgb(5,6,7));c.drawText(value,tx,base,p);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawText(value,tx,base,p);}p.setAntiAlias(true);}
    private String clean(String value){if(value==null)return "";value=value.replace(">>","").trim();return value.length()>18?value.substring(0,18):value;}
    private void demoScene(float W,int frame,boolean running) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.MITER);p.setStrokeWidth(2.2f);p.setColor(Color.rgb(36,41,44));
        float shift=running&&frame>=0?(frame*4)%18:0;
        building(new float[]{34-shift,73,96-shift,55,139-shift,69,132-shift,125,65-shift,132});
        building(new float[]{530-shift,43,617-shift,50,624-shift,108,548-shift,119});
        building(new float[]{W-250-shift,78,W-164-shift,61,W-137-shift,132,W-231-shift,140});
        building(new float[]{W-476-shift,220,W-392-shift,201,W-358-shift,254,W-453-shift,272});
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(7);p.setColor(Color.rgb(27,27,27));
        Path a=new Path();a.moveTo(36,272);a.cubicTo(170,240,250,285,398,266);a.cubicTo(540,246,W-220,268,W-30,246);c.drawPath(a,p);
        p.setStrokeWidth(3);p.setColor(Color.rgb(46,46,46));
        int roadShift=running&&frame>=0?(frame*13)%62:0;
        for(float x=-20+roadShift;x<W+40;x+=62)c.drawLine(x,278,x+22,274,p);
    }
    private void building(float[] xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();c.drawPath(path,p);}
    private void arrow(float x,float y,int step) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.SQUARE);p.setStrokeJoin(Paint.Join.MITER);p.setStrokeWidth(24);p.setColor(WHITE);
        Path path=new Path();
        if(step==0){path.moveTo(x,y+67);path.lineTo(x,y-15);path.quadTo(x,y-63,x+48,y-63);path.lineTo(x+73,y-63);path.moveTo(x+47,y-91);path.lineTo(x+76,y-62);path.lineTo(x+47,y-33);}
        else if(step==1){path.moveTo(x,y+67);path.lineTo(x,y-16);path.quadTo(x,y-63,x-48,y-63);path.lineTo(x-73,y-63);path.moveTo(x-47,y-91);path.lineTo(x-76,y-62);path.lineTo(x-47,y-33);}
        else if(step==2){path.moveTo(x,y+72);path.lineTo(x,y-74);path.moveTo(x-31,y-43);path.lineTo(x,y-77);path.lineTo(x+31,y-43);}
        else {path.moveTo(x,y+70);path.lineTo(x,y-30);path.quadTo(x,y-70,x-41,y-70);path.quadTo(x-82,y-70,x-82,y-28);path.lineTo(x-82,y+1);path.moveTo(x-112,y-27);path.lineTo(x-82,y+4);path.lineTo(x-52,y-27);}
        c.drawPath(path,p);
        p.setStyle(Paint.Style.FILL);p.setColor(RED);c.drawCircle(x,y+92,7,p);
    }
    private void progress(float x,float y,float width,float value) {
        p.setStrokeCap(Paint.Cap.SQUARE);p.setStrokeWidth(9);p.setColor(DARK);c.drawLine(x,y,x+width,y,p);p.setColor(RED);c.drawLine(x,y,x+width*value,y,p);
        p.setStyle(Paint.Style.FILL);c.drawCircle(x+width*value,y,8,p);
    }
    private void pageDots(float x,int frame,int step){p.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++){p.setColor(i==step%3?(frame>=0?RED:WHITE):Color.rgb(57,57,57));c.drawCircle(x,269+i*23,5.5f,p);}}
    private enum Align{LEFT,CENTER,RIGHT}
    private void text(String value,float x,float y,float size,int color,Typeface face,Align align){p.setStyle(Paint.Style.FILL);p.setTypeface(face);p.setTextSize(size);p.setColor(color);p.setAntiAlias(face!=zpix);Rect r=new Rect();p.getTextBounds(value,0,value.length(),r);float tx=x-r.left;if(align==Align.CENTER)tx=x-(r.left+r.right)/2f;else if(align==Align.RIGHT)tx=x-r.right;c.drawText(value,tx,y-(r.top+r.bottom)/2f,p);p.setAntiAlias(true);}
}

