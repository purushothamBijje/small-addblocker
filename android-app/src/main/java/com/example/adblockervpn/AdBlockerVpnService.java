package com.example.adblockervpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

public class AdBlockerVpnService extends VpnService {
    public static final String ACTION_CONNECT = "com.example.adblockervpn.CONNECT";
    public static final String ACTION_DISCONNECT = "com.example.adblockervpn.DISCONNECT";
    public static boolean isRunning = false;
    private ParcelFileDescriptor vpnInterface;

    // Critical media/streaming apps that bundle ads into video streams and freeze if blocked
    private static final String[] BYPASS_PACKAGES = {
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "in.startv.hotstar",
        "com.spotify.music",
        "com.whatsapp",
        "com.facebook.katana",
        "com.instagram.android"
    };

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
                    NotificationManager.IMPORTANCE_LOW
            );
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
                .setContentTitle("Ad Blocker Active")
                .setContentText("Ad blocking enabled. Video & messaging apps protected.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();

        startForeground(1, notification);
    }

    private void startVpn() {
        if (vpnInterface != null) return;
        try {
            Builder builder = new Builder();
            builder.setSession("AdBlocker");
            builder.addAddress("10.0.0.2", 32);

            // Primary: AdGuard Ad-blocking DNS
            builder.addDnsServer("94.140.14.14");
            builder.addDnsServer("94.140.15.15");
            // Reliable fallback so connection NEVER drops or times out
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("8.8.8.8");

            // Only intercept the DNS server addresses
            builder.addRoute("94.140.14.14", 32);
            builder.addRoute("94.140.15.15", 32);

            // Bypass streaming, video, and chat apps to prevent video buffering & frozen connections
            PackageManager pm = getPackageManager();
            for (String pkg : BYPASS_PACKAGES) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    builder.addDisallowedApplication(pkg);
                } catch (PackageManager.NameNotFoundException ignored) {
                    // App not installed on device, ignore
                }
            }

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
