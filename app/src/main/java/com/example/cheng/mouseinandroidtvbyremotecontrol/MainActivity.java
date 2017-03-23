package com.example.cheng.mouseinandroidtvbyremotecontrol;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static WebView webView;
    private int displayHeight, displayWidth;
    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(WebViewClient);
        webView.loadUrl("http://www.google.com");
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        webView.measure(w, h);
        Log.e("webview", String.valueOf(webView.getMeasuredHeight()));
        DisplayMetrics dm = new DisplayMetrics();
        // 取得裝置的資訊
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        displayHeight = dm.heightPixels;
        displayWidth = dm.widthPixels;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + MainActivity.this.getPackageName()));
                startActivityForResult(intent, 87);
            } else {
                startService(new Intent(MainActivity.this, MouseAccessibilityService.class));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mouseAccessibilityService.setServiceInfo(new AccessibilityServiceInfo());
        MouseAccessibilityService.windowManager.removeView(MouseAccessibilityService.cursorView);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (i >= 2) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
                i = 0;
            }
            i++;
//            webView.scrollTo(webviewWidth,webviewHeight);
//            Toast.makeText(MainActivity.this,"返回",Toast.LENGTH_SHORT).show();
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (i >= 2) {
                if (MouseAccessibilityService.cursorLayout.x > 0) {
                    Intent intent = new Intent();
                    intent.putExtra("move", 2);
                    intent.setAction(MouseAccessibilityService.TAG);
                    sendBroadcast(intent);
                } else if (webView.getScrollX() > 0) {
                    webView.scrollTo( webView.getScrollX()-10, webView.getScrollY());
                }
                i = 0;
            }
            i++;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (i >= 2) {
                if (MouseAccessibilityService.cursorLayout.x < displayWidth) {
                    Intent intent = new Intent();
                    intent.putExtra("move", 3);
                    intent.setAction(MouseAccessibilityService.TAG);
                    sendBroadcast(intent);
                } else if (!(webView.getWidth() * webView.getScale() - (webView.getWidth() + webView.getScrollX()) <= 0)) {
                    webView.scrollTo( webView.getScrollX()+10, webView.getScrollY());
                }
                i = 0;
            }
            i++;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (i >= 2) {
                if (MouseAccessibilityService.cursorLayout.y > 0) {
                    Intent intent = new Intent();
                    intent.putExtra("move", 0);
                    intent.setAction(MouseAccessibilityService.TAG);
                    sendBroadcast(intent);
                } else if (webView.getScrollY() > 0) {
                    webView.scrollTo( webView.getScrollX(), webView.getScrollY()-10);
                }
                i = 0;
            }
            i++;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (i >= 2) {
                if (MouseAccessibilityService.cursorLayout.y < displayHeight) {
                    Intent intent = new Intent();
                    intent.putExtra("move", 1);
                    intent.setAction(MouseAccessibilityService.TAG);
                    sendBroadcast(intent);
                } else if (!(webView.getContentHeight() * webView.getScale() - (webView.getHeight() + webView.getScrollY()) <= 0)) {
//                    已经处于底端
                    webView.scrollTo( webView.getScrollX(), webView.getScrollY()+10);
                }
                i = 0;
            }
            i++;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (i >= 2) {
                Intent intent = new Intent();
                intent.putExtra("move", 4);
                intent.setAction(MouseAccessibilityService.TAG);
                sendBroadcast(intent);
                i = 0;
            }
            i++;
        }
        return false;
    }

    android.webkit.WebViewClient WebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    };
}
