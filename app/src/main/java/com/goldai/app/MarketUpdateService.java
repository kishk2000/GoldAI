package com.goldai.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import com.goldai.app.data.DataEngine;
import com.goldai.app.data.MarketData;
import com.goldai.app.data.XausProvider;

public class MarketUpdateService extends Service {

    private static final String CHANNEL_ID =
            "GOLD_AI_CHANNEL";

    private static final int NOTIFICATION_ID =
            1001;

    private final Handler handler =
            new Handler();

    private DataEngine dataEngine;

    private final Runnable updateTask =
            new Runnable() {

        @Override
        public void run() {

            dataEngine.update(
                    new com.goldai.app.data.MarketDataProvider.Callback() {

                @Override
                public void onSuccess(MarketData data) {

                    updateNotification(
                            String.format(
                                    "XAU/USD: $%.2f",
                                    data.goldUsd
                            )
                    );
                }

                @Override
                public void onError(String error) {

                    updateNotification(
                            "GOLD AI - في انتظار البيانات"
                    );
                }
            });

            handler.postDelayed(
                    this,
                    30000
            );
        }
    };

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        dataEngine =
                new DataEngine(
                        new XausProvider()
                );

        Notification notification =
                createNotification(
                        "GOLD AI",
                        "جاري تحديث سعر الذهب..."
                );

        if (Build.VERSION.SDK_INT >= 29) {

            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );

        } else {

            startForeground(
                    NOTIFICATION_ID,
                    notification
            );
        }

        handler.post(updateTask);
    }

    private void updateNotification(
            String text) {

        Notification notification =
                createNotification(
                        "GOLD AI",
                        text
                );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );

        manager.notify(
                NOTIFICATION_ID,
                notification
        );
    }

    private Notification createNotification(
            String title,
            String text) {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            return new Notification.Builder(
                    this,
                    CHANNEL_ID
            )
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_menu_info_details
                    )
                    .setOngoing(true)
                    .build();

        } else {

            return new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_menu_info_details
                    )
                    .setOngoing(true)
                    .build();
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "GOLD AI",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "تحديث سعر الذهب"
            );

            channel.setSound(
                    null,
                    null
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacks(
                updateTask
        );

        super.onDestroy();
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}
