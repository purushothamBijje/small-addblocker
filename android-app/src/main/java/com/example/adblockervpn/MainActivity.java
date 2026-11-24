package com.example.adblockervpn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST_CODE = 1001;
    private TextView statusText;
    private Button toggleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);

        statusText = new TextView(this);
        statusText.setTextSize(22);
        statusText.setGravity(Gravity.CENTER);

        toggleBtn = new Button(this);
        toggleBtn.setTextSize(18);

        layout.addView(statusText);
        layout.addView(toggleBtn);
        setContentView(layout);

        updateUI();

        toggleBtn.setOnClickListener(v -> {
            if (!AdBlockerVpnService.isRunning) {
                Intent prepareIntent = VpnService.prepare(MainActivity.this);
                if (prepareIntent != null) {
                    startActivityForResult(prepareIntent, VPN_REQUEST_CODE);
                } else {
                    startVpnService();
                }
            } else {
                stopVpnService();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (AdBlockerVpnService.isRunning) {
            statusText.setText("Status: ACTIVE (Blocking Ads)\n");
            statusText.setTextColor(Color.parseColor("#2E7D32"));
            toggleBtn.setText("Turn OFF Ad Blocker");
        } else {
            statusText.setText("Status: INACTIVE\n");
            statusText.setTextColor(Color.parseColor("#C62828"));
            toggleBtn.setText("Turn ON Ad Blocker");
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, AdBlockerVpnService.class);
        intent.setAction(AdBlockerVpnService.ACTION_CONNECT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        AdBlockerVpnService.isRunning = true;
        updateUI();
        Toast.makeText(this, "Ad Blocker Activated!", Toast.LENGTH_SHORT).show();
    }

    private void stopVpnService() {
        Intent intent = new Intent(this, AdBlockerVpnService.class);
        intent.setAction(AdBlockerVpnService.ACTION_DISCONNECT);
        startService(intent);
        AdBlockerVpnService.isRunning = false;
        updateUI();
        Toast.makeText(this, "Ad Blocker Deactivated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVpnService();
            } else {
                Toast.makeText(this, "VPN permission is required to block ads", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
