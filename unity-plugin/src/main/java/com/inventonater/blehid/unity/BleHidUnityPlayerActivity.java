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
        // (Optional) any setup after UnityPlayer is created
    }

    @Override 
    protected void onResume() {
        super.onResume();

        // Unity will handle resuming the player here (if it was paused)
        // No additional code needed in many cases, but ensure Unity is not paused:
        if (!isInPictureInPictureMode()) {
            mUnityPlayer.resume();
        }
    }

    @Override 
    public void onBackPressed()
    {
      // Instead of calling UnityPlayerGameActivity.onBackPressed(), this example ignores the back button event
      // super.onBackPressed();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        
        Log.d("BleHid", "PiP mode changed: " + isInPictureInPictureMode);
        
        if (isInPictureInPictureMode) {
            // We're entering PiP mode
            // Make sure Unity keeps running
            mUnityPlayer.resume();
        } else {
            // We're exiting PiP mode
            mUnityPlayer.resume();
        }
        
        // Use the bridge to notify Unity about PiP mode changes
        // This ensures we use the same callback mechanism as other events
        BleHidUnityBridge.getInstance().notifyPipModeChanged(isInPictureInPictureMode);
    }

    // @Override
    // public void onUserLeaveHint() {
    //     // Trigger PiP when the user presses Home/Swipe‑up
    //     if (!isInPictureInPictureMode()) {
    
    //         // Build params right here ──────────────────────────────────────
    //         // Use current view size; fall back to display size if 0×0.
    //         View v = mUnityPlayer.getView();
    //         int w = v.getWidth();
    //         int h = v.getHeight();
    //         if (w == 0 || h == 0) {
    //             Point p = new Point();
    //             getWindowManager().getDefaultDisplay().getRealSize(p);
    //             w = p.x; h = p.y;
    //         }
    
    //         PictureInPictureParams params =
    //                 new PictureInPictureParams.Builder()
    //                         .setAspectRatio(new Rational(w, h))
    //                         .build();
    //         // ──────────────────────────────────────────────────────────────
    
    //         enterPictureInPictureMode(params);      // now compiles
    //         return;                                 // skip default Home behaviour
    //     }
    //     super.onUserLeaveHint();
    // }

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

        PictureInPictureParams pipParams =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(w, h))
                        .build();

        enterPictureInPictureMode(pipParams);

        mUnityPlayer.resume();
        mUnityPlayer.windowFocusChanged(true); // restore input & let loop tick
    }
}
