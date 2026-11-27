package com.example.adblockervpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST_CODE = 1001;
    private TextView statusText;
    private Button toggleBtn;
    private Button batteryBtn;
    private Button openYtBtn;
    private WebView ytWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(40, 40, 40, 40);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);

        toggleBtn = new Button(this);
        toggleBtn.setTextSize(16);

        batteryBtn = new Button(this);
        batteryBtn.setText("Disable Battery Saver (Prevent Auto-Kill)");
        batteryBtn.setTextSize(13);

        openYtBtn = new Button(this);
        openYtBtn.setText("Watch Ad-Free YouTube");
        openYtBtn.setTextSize(15);
        openYtBtn.setBackgroundColor(Color.parseColor("#CC0000"));
        openYtBtn.setTextColor(Color.WHITE);

        layout.addView(statusText);
        layout.addView(toggleBtn);
        layout.addView(batteryBtn);
        layout.addView(openYtBtn);

        ytWebView = new WebView(this);
        ytWebView.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        ytWebView.setLayoutParams(params);

        WebSettings settings = ytWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        ytWebView.setWebChromeClient(new WebChromeClient());
        ytWebView.setWebViewClient(new AdBlockWebViewClient());

        layout.addView(ytWebView);
        setContentView(layout);

        updateUI();

        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        batteryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBatteryOptimizationExemption();
            }
        });

        openYtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ytWebView.getVisibility() == View.GONE) {
                    statusText.setVisibility(View.GONE);
                    toggleBtn.setVisibility(View.GONE);
                    batteryBtn.setVisibility(View.GONE);
                    openYtBtn.setText("Close YouTube Player");
                    ytWebView.setVisibility(View.VISIBLE);
                    ytWebView.loadUrl("https://m.youtube.com");
                } else {
                    ytWebView.stopLoading();
                    ytWebView.setVisibility(View.GONE);
                    statusText.setVisibility(View.VISIBLE);
                    toggleBtn.setVisibility(View.VISIBLE);
                    batteryBtn.setVisibility(View.VISIBLE);
                    openYtBtn.setText("Watch Ad-Free YouTube");
                }
            }
        });
    }

    private static class AdBlockWebViewClient extends WebViewClient {
        private static final String SCRIPT =
                "(function() {" +
                "  function kill() {" +
                "    var ads = document.querySelectorAll('.ad-showing, .ad-interrupting, .ytp-ad-overlay-container, ytd-promoted-video-renderer, #player-ads');" +
                "    for (var i = 0; i < ads.length; i++) ads[i].remove();" +
                "    var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');" +
                "    if (skip) skip.click();" +
                "    var vid = document.querySelector('video');" +
                "    if (vid && document.querySelector('.ad-showing')) vid.currentTime = vid.duration || 9999;" +
                "  }" +
                "  setInterval(kill, 400);" +
                "})();";

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.contains("doubleclick.net") || url.contains("/pagead/") || url.contains("googleads")) {
                return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.evaluateJavascript(SCRIPT, null);
            super.onPageFinished(view, url);
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Battery optimization already disabled!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (ytWebView.getVisibility() == View.VISIBLE && ytWebView.canGoBack()) {
            ytWebView.goBack();
        } else if (ytWebView.getVisibility() == View.VISIBLE) {
            ytWebView.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            toggleBtn.setVisibility(View.VISIBLE);
            batteryBtn.setVisibility(View.VISIBLE);
            openYtBtn.setText("Watch Ad-Free YouTube");
        } else {
            super.onBackPressed();
        }
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
            toggleBtn.setText("Turn OFF System Ad Blocker");
        } else {
            statusText.setText("Status: INACTIVE\n");
            statusText.setTextColor(Color.parseColor("#C62828"));
            toggleBtn.setText("Turn ON System Ad Blocker");
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
                Toast.makeText(this, "VPN permission required", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
