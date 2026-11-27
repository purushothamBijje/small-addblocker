package com.example.adblockervpn;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;

public class YouTubeClient extends WebViewClient {
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
