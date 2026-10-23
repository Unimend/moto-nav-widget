package com.unimend.motonav;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NavNotificationService extends NotificationListenerService {
    private static final String AMAP="com.autonavi.minimap";
    @Override public void onListenerConnected(){super.onListenerConnected();boolean found=false;StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification s:active)if(AMAP.equals(s.getPackageName())&&isNavigation(s)){capture(s);found=true;}if(!found){new NavStore(this).liveOff();NavWidget.publish(this);}}
    @Override public void onNotificationPosted(StatusBarNotification s){if(AMAP.equals(s.getPackageName())&&isNavigation(s))capture(s);}
    @Override public void onNotificationRemoved(StatusBarNotification s){if(!AMAP.equals(s.getPackageName())||!isNavigation(s))return;boolean found=false;StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification x:active)if(AMAP.equals(x.getPackageName())&&isNavigation(x)){capture(x);found=true;}if(!found){new NavStore(this).liveOff();NavWidget.publish(this);}}
    private boolean isNavigation(StatusBarNotification s){Notification n=s.getNotification();return Notification.CATEGORY_NAVIGATION.equals(n.category)||n.extras.getBoolean("com.autonavi.minimap.navigating",false)||"ROUTE_CHANNEL_ID".equals(n.getChannelId());}
    private void capture(StatusBarNotification s){Notification n=s.getNotification();String title=value(n.extras.getCharSequence(Notification.EXTRA_TITLE));String text=value(n.extras.getCharSequence(Notification.EXTRA_TEXT));String big=value(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT));String sub=value(n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT));if(big.length()>text.length())text=big;new NavStore(this).live(title,text,sub);NavWidget.publish(this);}
    private String value(CharSequence c){return c==null?"":c.toString();}
}
