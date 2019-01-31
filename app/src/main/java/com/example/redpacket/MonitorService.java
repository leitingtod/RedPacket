package com.example.redpacket;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.util.Timer;
import java.util.TimerTask;

public class MonitorService extends AccessibilityService {
    public static int openDelayTime;
    public static int loadingTime;

    private NotificationManager notificationManager;
    private NotificationChannel notificationChannel;

    private Timer timer;
    private int eventCount = 0;
    private int lastEventCount = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        eventCount++;

        Logger.w("EventType: %s, EventTime: %s, ToIndex: %s\nClassName: %s, Package: %s",
                AccessibilityEvent.eventTypeToString(event.getEventType()),
                event.getEventTime(),
                event.getToIndex(),
                event.getClassName(),
                event.getPackageName());

        WeChat.process(this, event);
    }

    @Override
    protected void onServiceConnected() {
        String s = getResources().getString(R.string.app_name) +
                getResources().getString(R.string.seperator) +
                getResources().getString(R.string.service_opened) +
                getResources().getString(R.string.seperator) +
                getResources().getString(R.string.close_service);

        notify(s);
        setTimerTask();
        showConfig("onConnected");

        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String s = getResources().getString(R.string.app_name) +
                getResources().getString(R.string.seperator) +
                getResources().getString(R.string.service_closed) +
                getResources().getString(R.string.seperator) +
                getResources().getString(R.string.open_service);

        notify(s);
        timer.cancel();
        showConfig("onUnbind");

        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
        String s = getResources().getString(R.string.app_name) +
                getResources().getString(R.string.seperator) +
                getResources().getString(R.string.service_feedback_interrupt);

        notify(s);
    }

    @Override
    public void onCreate() {
        String id = "channel-redpacket";
        String description = getResources().getString(R.string.app_name);
        try {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationChannel = new NotificationChannel(id, description, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(description);

            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);

            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{200, 300});

            notificationManager.createNotificationChannel(notificationChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLogger();

        super.onCreate();
    }

    public void notify(String message) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

            RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
            views.setTextViewText(R.id.textView, message);
            views.setTextColor(R.id.textView, Color.BLACK);
            views.setImageViewResource(R.id.imageView, R.mipmap.ic_launcher_round);
            views.setOnClickPendingIntent(R.id.textView, pi);

            Notification notification = new Notification.Builder(this, notificationChannel.getId())
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setCustomContentView(new RemoteViews(getPackageName(), R.layout.notification))
                    .setCustomBigContentView(views)
                    .setCustomHeadsUpContentView(views)
                    .setFullScreenIntent(pi, true)
                    .build();

            notificationManager.notify(1, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toast(String message) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }, 100);
    }

    public void toast(int resId) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
        }, 100);
    }

    private void showConfig(String title) {
        Logger.i("%s : openDelayTime=%s, loadingTime=%s", title, openDelayTime, loadingTime);
    }

    private void setLogger() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                //.logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
                .tag("")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
    }

    private void setTimerTask() {
        MonitorService service = this;

        if (timer == null) {
            timer = new Timer();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (lastEventCount == eventCount) {
                    eventCount = 0;

                    if (WeChat.eventStart) {
                        WeChat.eventStart = false;
                        WeChat.eventStop = true;
                        if (WeChat.scrollStart) {
                            WeChat.scrollStart = false;
                            WeChat.scrollStop = true;
                        }
                        WeChat.handleEventStop(service);
                    }
                }
                lastEventCount = eventCount;
            }
            // 间隔为100时，易出现滚动开始前检测出TYPE_WINDOW_CONTENT_CHANGED事件已停止,造成
            // 在同一页面搜索2次(主要发生在一个页面加载时，这两个事件都发生的情况)
        }, 0, 150);
    }

    private void showConfig() {
        Logger.d("onConnected: openDelayTime=%s, loadingTime=%s", openDelayTime, loadingTime);
    }
}
