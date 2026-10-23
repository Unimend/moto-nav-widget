package com.unimend.motonav;

import android.content.Context;
import android.graphics.*;
import java.util.Locale;
import org.json.*;

final class NavRenderer {
    private static final int WHITE=Color.WHITE, GREY=Color.rgb(139,139,139), DARK=Color.rgb(36,36,36), RED=Color.rgb(235,48,54), GREEN=Color.rgb(65,191,111);
    private final Canvas c; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final float w,h,s;
    private final Typeface doto,zpix,sans=Typeface.create("sans-serif",Typeface.BOLD); private final Context context;
    private NavRenderer(Context context,Bitmap b) { this.context=context.getApplicationContext();c=new Canvas(b); w=b.getWidth();h=b.getHeight();s=h/360f;c.scale(s,s);doto=Typeface.createFromAsset(context.getAssets(),"doto.ttf");zpix=Typeface.createFromAsset(context.getAssets(),"zpix.ttf"); }
    static Bitmap render(Context context,int width,int height,NavStore store,int id,int frame) {
        Bitmap b=Bitmap.createBitmap(Math.max(480,width),Math.max(180,height),Bitmap.Config.ARGB_8888); b.setDensity(Bitmap.DENSITY_NONE);
        new NavRenderer(context,b).draw(store,id,frame); return b;
    }
    private void draw(NavStore store,int id,int frame) {
        if(store.live()){drawAmap(store,frame);return;}
        int step=store.step(id);boolean running=store.running(id);
        float W=w/s;
        c.drawColor(Color.TRANSPARENT);
        demoScene(W,frame,running);
        String[] distance={"280 m","90 m","1.2 km","450 m"}; String[] road={"文一路","学院路","余杭塘路","古翠路"}; int[] speed={42,28,56,35};
        arrow(120,174,step);
        text(distance[step],322,105,76,WHITE,doto,Align.LEFT);
        text(road[step],322,177,40,WHITE,zpix,Align.LEFT);
        text("NEXT",72,49,28,RED,doto,Align.CENTER);
        text(running?"DEMO · NAVIGATING":"DEMO · READY",322,226,25,running?GREEN:GREY,doto,Align.LEFT);
        float speedX=W-116;
        text(String.valueOf(speed[step]),speedX,116,78,WHITE,doto,Align.CENTER);
        text("KM/H",speedX,174,27,GREY,doto,Align.CENTER);
        text("18MIN · 12.4KM",speedX,230,23,GREY,doto,Align.CENTER);
        progress(42,302,W-84,.64f+step*.07f);
        text("64%",W-66,324,24,WHITE,doto,Align.RIGHT);
        pageDots(W-31,frame,step);
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
    private void realScene(float W){
        SceneStore scene=new SceneStore(context);if(!scene.ready())return;JSONArray fs=scene.data().optJSONArray("features");if(fs==null)return;double lat0=scene.lat(),lon0=scene.lon(),cos=Math.cos(Math.toRadians(lat0));float left=Math.max(690,W*.61f),right=W-48,top=78,bottom=288,scale=Math.min((right-left)/500f,(bottom-top)/500f),cx=(left+right)/2f,cy=(top+bottom)/2f;
        int save=c.save();c.clipRect(left,top,right,bottom);
        for(int i=0;i<fs.length();i++){JSONObject f=fs.optJSONObject(i);JSONArray pts=f==null?null:f.optJSONArray("points");if(pts==null||pts.length()<2)continue;boolean building="building".equals(f.optString("kind"));Path path=new Path();for(int j=0;j<pts.length();j++){JSONArray q=pts.optJSONArray(j);if(q==null)continue;float x=cx+(float)((q.optDouble(1)-lon0)*111320*cos)*scale;float y=cy-(float)((q.optDouble(0)-lat0)*111320)*scale;if(j==0)path.moveTo(x,y);else path.lineTo(x,y);}if(building)path.close();p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(building?Paint.Join.MITER:Paint.Join.ROUND);p.setStrokeCap(building?Paint.Cap.SQUARE:Paint.Cap.ROUND);p.setStrokeWidth(building?2.0f:(f.optBoolean("major")?5.0f:3.0f));p.setColor(building?Color.rgb(36,41,44):(f.optBoolean("major")?Color.rgb(64,69,73):Color.rgb(43,48,51)));c.drawPath(path,p);}
        c.restoreToCount(save);
    }
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

