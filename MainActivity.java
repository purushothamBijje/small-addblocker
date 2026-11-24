package com.example.adblockervpn;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST_CODE = 1001;
    private boolean isBlocking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);

        TextView statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setText("Status: Inactive");

        Button toggleBtn = new Button(this);
        toggleBtn.setText("Turn ON Ad Blocker");

        toggleBtn.setOnClickListener(v -> {
            if (!isBlocking) {
                Intent vpnIntent = VpnService.prepare(MainActivity.this);
                if (vpnIntent != null) {
                    startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
                } else {
                    onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
                }
            } else {
                Intent stopIntent = new Intent(this, AdBlockerVpnService.class);
                stopService(stopIntent);
                isBlocking = false;
                statusText.setText("Status: Inactive");
                toggleBtn.setText("Turn ON Ad Blocker");
            }
        });

        layout.addView(statusText);
        layout.addView(toggleBtn);
        setContentView(layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, AdBlockerVpnService.class);
            startService(serviceIntent);
            isBlocking = true;
            // Updates UI
            recreate();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
