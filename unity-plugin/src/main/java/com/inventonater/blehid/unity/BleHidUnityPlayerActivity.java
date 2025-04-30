package com.inventonater.blehid.unity;

import com.unity3d.player.UnityPlayerActivity;
import com.unity3d.player.UnityPlayer;

import android.os.Bundle;
import android.util.Log;
import android.app.PictureInPictureParams;
import android.util.Rational;
import android.view.View;
import android.graphics.Point;
import android.content.res.Configuration;

public class BleHidUnityPlayerActivity extends UnityPlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isInPictureInPictureMode()) {
            mUnityPlayer.resume();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        Log.d("BleHid", "PiP mode changed: " + isInPictureInPictureMode);

        mUnityPlayer.resume();
        BleHidUnityBridge bridge = BleHidUnityBridge.getInstance();
        if(bridge != null) bridge.notifyPipModeChanged(isInPictureInPictureMode);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // UnityPlayerForGameActivity → View
        View unityView = mUnityPlayer.getView();      // <-- still public :contentReference[oaicite:0]{index=0}

        int w = unityView.getWidth();
        int h = unityView.getHeight();

        // If Unity hasn’t been measured yet (returns 0), use the display size instead
        if (w == 0 || h == 0) {
            Point sz = new Point();
            getWindowManager().getDefaultDisplay().getRealSize(sz);
            w = sz.x;
            h = sz.y;
        }

        PictureInPictureParams pipParams = new PictureInPictureParams.Builder().setAspectRatio(new Rational(w, h)).build();

        enterPictureInPictureMode(pipParams);

        mUnityPlayer.resume();
        mUnityPlayer.windowFocusChanged(true); // restore input & let loop tick
    }
}
