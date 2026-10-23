package com.unimend.motonav;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.widget.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NavActivity extends Activity {
    private static final AtomicBoolean SCENE_LOADING=new AtomicBoolean(false);
    private int widgetId; private NavStore store; private ImageView preview; private TextView state; private Button sceneButton; private final ExecutorService io=Executors.newSingleThreadExecutor(); private final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle b){super.onCreate(b);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0);store=new NavStore(this);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(22),dp(24),dp(28));body.setBackgroundColor(Color.BLACK);setContentView(body);
        TextView title=text("MOTO NAV · 桌面导航原型",28,Color.WHITE);body.addView(title);preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);body.addView(preview,new LinearLayout.LayoutParams(-1,dp(190)));
        state=text("",16,Color.LTGRAY);state.setLineSpacing(dp(4),1);body.addView(state);
        Button run=new Button(this);run.setText("开始 / 暂停演示导航");body.addView(run);run.setOnClickListener(v->{boolean n=!store.running(widgetId);store.running(widgetId,n);refresh(true);});
        Button next=new Button(this);next.setText("模拟下一条导航指令");body.addView(next);next.setOnClickListener(v->{store.next(widgetId);refresh(true);});
        Button access=new Button(this);access.setText("授权读取高德导航通知");body.addView(access);access.setOnClickListener(v->startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        sceneButton=new Button(this);sceneButton.setText("读取位置并刷新真实道路 / 建筑");body.addView(sceneButton);sceneButton.setOnClickListener(v->refreshScene());
        TextView note=text("这是视觉与交互原型，不会提供真实路线指引。\n\n第一阶段验证：3×1尺寸、下一动作、距离、速度、剩余时间和进度。下一阶段将接入手机 GPS；路线搜索与偏航重算需要合规的地图/导航服务。\n\n视觉参考：MOTO GPS · Waveshare Edition\nhttps://github.com/mx3353672833-debug/moto-gps-waveshare",14,Color.GRAY);android.text.util.Linkify.addLinks(note,android.text.util.Linkify.WEB_URLS);note.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());body.addView(note);
        refresh(false);
    }
    private TextView text(String v,int size,int color){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(8),0,dp(12));return t;}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void refresh(boolean animate){int id=widgetId;preview.setImageBitmap(NavRenderer.render(this,dp(700),dp(220),store,id,-1));boolean enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners")!=null&&Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners").contains(getPackageName());String live=store.live()?"高德后台导航已连接\n通知内容："+store.liveText():"尚未收到高德导航通知";state.setText((widgetId>0?"当前组件 #"+widgetId:"应用预览")+"\n通知读取："+(enabled?"已授权":"未授权")+" · "+live+"\n本应用只处理 com.autonavi.minimap 的导航通知，不保存其他应用通知。");if(widgetId>0){if(animate)NavWidget.animate(this,widgetId);else NavWidget.draw(this,widgetId,-1);}else NavWidget.publish(this);}
    @Override protected void onResume(){super.onResume();if(store!=null)refresh(false);}
    private void refreshScene(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},41);return;}LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);Location best=null;for(String provider:lm.getProviders(true)){Location x=lm.getLastKnownLocation(provider);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}if(best!=null){fetchScene(best);return;}sceneButton.setEnabled(false);sceneButton.setText("正在获取位置…");LocationListener listener=new LocationListener(){public void onLocationChanged(Location l){lm.removeUpdates(this);fetchScene(l);}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,listener);}catch(Exception e){sceneButton.setEnabled(true);sceneButton.setText("无法获取位置，请开启定位");}}
    private void fetchScene(Location l){if(!SCENE_LOADING.compareAndSet(false,true)){sceneButton.setText("场景正在载入，请稍候…");return;}sceneButton.setEnabled(false);sceneButton.setText("正在载入道路与建筑（最多 18 秒）…");final double lat=l.getLatitude(),lon=l.getLongitude();io.execute(()->{try{org.json.JSONObject data=SceneSource.fetch(lat,lon);new SceneStore(this).save(data,lat,lon);handler.post(()->{sceneButton.setEnabled(true);sceneButton.setText("已载入 "+data.optInt("buildings")+" 个建筑 / "+data.optInt("roads")+" 条道路");NavWidget.publish(this);refresh(false);});}catch(Exception e){android.util.Log.w("MotoNav","OSM scene",e);handler.post(()->{sceneButton.setEnabled(true);sceneButton.setText("载入失败：网络无响应，点击重试");});}finally{SCENE_LOADING.set(false);}});}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==41&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)refreshScene();else if(request==41)sceneButton.setText("需要精确位置才能载入真实建筑");}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}

