package com.company.product;
import com.unity3d.player.UnityPlayerGameActivity;
import android.os.Bundle;
import android.util.Log;
import android.app.PictureInPictureParams;
import android.util.Rational;
import android.view.View;          
import android.graphics.Point;

public class OverrideExample extends UnityPlayerGameActivity {
  
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
    }
}
