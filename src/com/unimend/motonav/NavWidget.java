package com.unimend.motonav;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.View;
import android.widget.RemoteViews;

public class NavWidget extends AppWidgetProvider {
    static final String NEXT="com.unimend.motonav.NEXT";
    private static final android.util.SparseIntArray animations=new android.util.SparseIntArray();
    static int[] ids(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,NavWidget.class));}
    static void draw(Context c,int id,int frame){
        AppWidgetManager m=AppWidgetManager.getInstance(c); Bundle o=m.getAppWidgetOptions(id); float density=c.getResources().getDisplayMetrics().density;
        int height=Math.max(480,Math.min(1400,Math.round(Math.max(220,o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,260)-2)*density)));
        // OPlus Launcher 3x5 gives this 3x3 host an observed 840:856 portrait ratio.
        // Render to that ratio so FIT_CENTER does not add a second pair of letterbox margins.
        int width=Math.max(480,Math.min(1400,Math.round(height*840f/856f)));
        NavStore store=new NavStore(c);RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
        try{Bitmap b=NavRenderer.render(c,width,height,store,id,frame);v.setImageViewBitmap(R.id.content,b);v.setViewVisibility(R.id.placeholder,View.GONE);}catch(Exception e){android.util.Log.e("NavWidget","render",e);}
        Intent open=new Intent(c,NavActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setOnClickPendingIntent(R.id.open,PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        Intent next=new Intent(c,NavWidget.class).setAction(NEXT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setOnClickPendingIntent(R.id.next,PendingIntent.getBroadcast(c,id,next,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));
        v.setContentDescription(R.id.content,"当前位置的 OpenStreetMap 离线地图");m.updateAppWidget(id,v);
    }
    static void publish(Context c){for(int id:ids(c))draw(c,id,-1);}
    static void animate(Context c,int id){final Context app=c.getApplicationContext();final int token; synchronized(animations){token=animations.get(id,0)+1;animations.put(id,token);}Handler h=new Handler(Looper.getMainLooper());for(int i=0;i<8;i++){final int f=i;h.postDelayed(()->{synchronized(animations){if(animations.get(id)!=token)return;}draw(app,id,f);},i*150L);}h.postDelayed(()->{synchronized(animations){if(animations.get(id)!=token)return;animations.delete(id);}draw(app,id,-1);},1350L);}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){publish(c);MapTrackingService.start(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){draw(c,id,-1);}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)new NavStore(c).delete(id);if(ids(c).length==0)MapTrackingService.stop(c);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(NEXT.equals(i.getAction())){int id=i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(id<0)return;new NavStore(c).next(id);animate(c,id);}else if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())){publish(c);MapTrackingService.start(c);}}
}

