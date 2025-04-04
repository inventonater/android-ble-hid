package com.example.blehid.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blehid.app.R;

/**
 * This class is maintained for backward compatibility but is not used in the current app.
 * The app now uses SimpleMediaActivity instead.
 */
public class SimpleMouseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity is no longer used - functionality moved to SimpleMediaActivity
        finish();
    }
}
