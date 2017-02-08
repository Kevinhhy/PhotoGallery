package com.example.kevin.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

/**
 * Created by kevin on 2017/2/6.
 */

public class PhotoPageActivity extends SingleFragmentActivity {
    private Callback mCallback;

    public interface Callback {
        WebView getWebview();
    }

    public static Intent newIntent(Context context, Uri uri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(uri);
        return i;
    }

    @Override
    public Fragment createFragment() {
        PhotoPageFragment fragment = PhotoPageFragment.newInstance(getIntent().getData());
        mCallback = fragment;
        return fragment;
    }

    @Override
    public void onBackPressed() {
        WebView webView = mCallback.getWebview();
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
