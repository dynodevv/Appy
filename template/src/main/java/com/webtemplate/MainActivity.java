package com.webtemplate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Ultra-minimal WebView activity with zero external dependencies.
 * Reads URL from assets/config.json at startup.
 */
public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private String targetUrl = "https://example.com";
    private boolean statusBarDark = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Load configuration from config.json
            loadConfig();
            
            // Setup status bar
            setupStatusBar();
            
            // Create UI
            createUI();
            
            // Setup WebView
            setupWebView();
            
            // Load URL
            webView.loadUrl(targetUrl);
        } catch (Exception e) {
            // Show error as toast and show a fallback URL
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (webView != null) {
                webView.loadUrl("https://example.com");
            }
        }
    }
    
    private void loadConfig() {
        try {
            InputStream inputStream = getAssets().open("config.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            inputStream.close();
            
            String jsonString = stringBuilder.toString();
            
            // Simple JSON parsing without using JSONObject to avoid any potential issues
            // Parse "url" field
            int urlIndex = jsonString.indexOf("\"url\"");
            if (urlIndex >= 0) {
                int colonIndex = jsonString.indexOf(":", urlIndex);
                int startQuote = jsonString.indexOf("\"", colonIndex);
                int endQuote = jsonString.indexOf("\"", startQuote + 1);
                if (startQuote >= 0 && endQuote > startQuote) {
                    targetUrl = jsonString.substring(startQuote + 1, endQuote);
                }
            }
            
            // Parse "statusBarDark" field
            int statusIndex = jsonString.indexOf("\"statusBarDark\"");
            if (statusIndex >= 0) {
                int colonIndex = jsonString.indexOf(":", statusIndex);
                String afterColon = jsonString.substring(colonIndex + 1).trim();
                statusBarDark = afterColon.startsWith("true");
            }
        } catch (Exception e) {
            // Use defaults if config loading fails
            targetUrl = "https://example.com";
            statusBarDark = false;
        }
    }

    private void setupStatusBar() {
        // Set status bar color
        int statusBarColor = statusBarDark ? Color.parseColor("#1C1B1F") : Color.parseColor("#F5F5F5");
        getWindow().setStatusBarColor(statusBarColor);
        
        // Set status bar icon color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (statusBarDark) {
                    // Dark background, light icons
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    // Light background, dark icons
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            }
        } else {
            View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (statusBarDark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }
    }
    
    private void createUI() {
        // Create root FrameLayout
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        
        // Set background color matching status bar
        int backgroundColor = statusBarDark ? Color.parseColor("#1C1B1F") : Color.parseColor("#F5F5F5");
        rootLayout.setBackgroundColor(backgroundColor);
        rootLayout.setFitsSystemWindows(true);
        
        // Create WebView
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        // Create ProgressBar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            8
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        
        // Add views
        rootLayout.addView(webView);
        rootLayout.addView(progressBar);
        
        setContentView(rootLayout);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
