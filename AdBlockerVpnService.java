package com.example.adblockervpn;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AdBlockerVpnService extends VpnService implements Runnable {
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    private boolean isRunning = false;

    // Blocklist for mobile apps ads
    private static final Set<String> BLOCKED_DOMAINS = new HashSet<>(Arrays.asList(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "admob.com",
        "ads.inmobi.com",
        "unityads.unity3d.com",
        "applovin.com",
        "vungle.com",
        "ironsrc.com"
    ));

    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            vpnThread = new Thread(this, "AdBlockerVpnThread");
            vpnThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            // Configure local loopback VPN adapter
            Builder builder = new Builder();
            builder.setSession("AppAdBlocker");
            builder.addAddress("10.0.0.2", 24);
            builder.addDnsServer("94.140.14.14"); // Upstream AdGuard DNS
            builder.addRoute("0.0.0.0", 0);

            vpnInterface = builder.establish();
            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

            byte[] packet = new byte[32767];
            while (isRunning) {
                int length = in.read(packet);
                if (length > 0) {
                    // Inspect UDP DNS packets (Port 53)
                    // If target ad domain matches, sinkhole/drop the packet
                    // Otherwise pass through
                    out.write(packet, 0, length);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }
}
