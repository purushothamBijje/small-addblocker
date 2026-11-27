package com.example.adblockervpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

public class AdBlockerVpnService extends VpnService {
    public static final String ACTION_CONNECT = "com.example.adblockervpn.CONNECT";
    public static final String ACTION_DISCONNECT = "com.example.adblockervpn.DISCONNECT";
    public static volatile boolean isRunning = false;
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        startForegroundNotification();
        startVpn();
        return START_STICKY;
    }

    private void startForegroundNotification() {
        String channelId = "vpn_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Ad Blocker Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Protects all installed apps from ads");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("Ad Blocker Active (All Apps)")
                .setContentText("Filtering ads across all apps, games, and web traffic")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

    private void startVpn() {
        if (vpnInterface != null) return;
        try {
            Builder builder = new Builder();
            builder.setSession("AdBlocker");
            builder.addAddress("10.0.0.2", 32);

            // AdGuard Ad-Blocking DNS (Primary & Secondary)
            builder.addDnsServer("94.140.14.14");
            builder.addDnsServer("94.140.15.15");

            // Intercept ALL DNS ports system-wide for EVERY app (IPv4)
            builder.addRoute("94.140.14.14", 32);
            builder.addRoute("94.140.15.15", 32);

            // Zero exclusions: Every single app installed on the device is filtered
            builder.setBlocking(false);
            vpnInterface = builder.establish();
            isRunning = true;
        } catch (Exception e) {
            e.printStackTrace();
            isRunning = false;
        }
    }

    private void stopVpn() {
        isRunning = false;
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
        } catch (Exception ignored) {}
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
