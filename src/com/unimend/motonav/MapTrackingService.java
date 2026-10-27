package com.unimend.motonav;

import android.Manifest;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class MapTrackingService extends Service implements LocationListener {
    static final String START="com.unimend.motonav.START_TRACKING",STOP="com.unimend.motonav.STOP_TRACKING";
    private static final String CHANNEL="moto_map_tracking"; private static final int NOTICE=92;
    private static final long WIDGET_INTERVAL_MS=15000L,SCENE_INTERVAL_MS=60000L;
    private LocationManager locations; private PowerManager power; private final ExecutorService io=Executors.newSingleThreadExecutor(); private long lastWidget,lastScene;
    private boolean listening;
    private final BroadcastReceiver screenState=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(Intent.ACTION_SCREEN_ON.equals(i.getAction()))startLocations();else if(Intent.ACTION_SCREEN_OFF.equals(i.getAction()))stopLocations();}};
    static void start(Context c){if(!new TrackingStore(c).enabled()||NavWidget.ids(c).length==0||c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;Intent i=new Intent(c,MapTrackingService.class).setAction(START);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    static void stop(Context c){try{c.startService(new Intent(c,MapTrackingService.class).setAction(STOP));}catch(Exception ignored){}}
    @Override public void onCreate(){super.onCreate();locations=(LocationManager)getSystemService(LOCATION_SERVICE);power=(PowerManager)getSystemService(POWER_SERVICE);registerReceiver(screenState,new IntentFilter(Intent.ACTION_SCREEN_ON));registerReceiver(screenState,new IntentFilter(Intent.ACTION_SCREEN_OFF));NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"桌面地图跟随",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&STOP.equals(intent.getAction())){stopTracking();return START_NOT_STICKY;}if(new NavSessionStore(this).active()||!new TrackingStore(this).enabled()||NavWidget.ids(this).length==0){stopSelf();return START_NOT_STICKY;}startForeground(NOTICE,notification());startLocations();return START_STICKY;}
    private Notification notification(){Intent open=new Intent(this,NavActivity.class);PendingIntent content=PendingIntent.getActivity(this,3,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("MOTO NAV · 低功耗地图跟随").setContentText("亮屏时使用网络定位，导航时才启用 GPS").setContentIntent(content).setOngoing(true).build();}
    private void startLocations(){if(listening||power==null||!power.isInteractive())return;try{Location best=null;for(String provider:locations.getProviders(true)){Location x=locations.getLastKnownLocation(provider);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}if(best!=null&&System.currentTimeMillis()-best.getTime()<600000L)onLocationChanged(best);locations.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,0,0,this,Looper.getMainLooper());if(locations.isProviderEnabled(LocationManager.NETWORK_PROVIDER))locations.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,30000,25,this,Looper.getMainLooper());listening=true;}catch(SecurityException e){stopTracking();}}
    private void stopLocations(){try{locations.removeUpdates(this);}catch(Exception ignored){}listening=false;}
    @Override public void onLocationChanged(Location l){new LiveLocationStore(this).update(l.getLatitude(),l.getLongitude(),l.hasAccuracy()?l.getAccuracy():0);long now=System.currentTimeMillis();if(now-lastWidget>WIDGET_INTERVAL_MS){lastWidget=now;NavWidget.publish(this);}SceneStore scene=new SceneStore(this);if(now-lastScene>SCENE_INTERVAL_MS&&(!scene.ready()||distance(scene.lat(),scene.lon(),l.getLatitude(),l.getLongitude())>600)){lastScene=now;io.execute(()->{try{JSONObject data=OfflineSceneSource.fetch(this,l.getLatitude(),l.getLongitude());if(data!=null){new SceneStore(this).save(data,l.getLatitude(),l.getLongitude());NavWidget.publish(this);}}catch(Exception e){android.util.Log.w("MotoNav","tracking scene",e);}});}}
    private double distance(double aLat,double aLon,double bLat,double bLon){double y=(bLat-aLat)*111320.0,x=(bLon-aLon)*111320.0*Math.cos(Math.toRadians((aLat+bLat)/2));return Math.hypot(x,y);}
    private void stopTracking(){stopLocations();stopForeground(true);stopSelf();}
    @Override public void onDestroy(){stopLocations();try{unregisterReceiver(screenState);}catch(Exception ignored){}io.shutdownNow();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
    @Override public void onProviderEnabled(String p){} @Override public void onProviderDisabled(String p){} @Override public void onStatusChanged(String p,int s,Bundle b){}
}
