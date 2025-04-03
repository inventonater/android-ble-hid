package com.example.blehid.diagnostic.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blehid.diagnostic.R;

/**
 * Activity for debugging BLE connection parameters and state.
 * In a complete implementation, this would show detailed connection information.
 */
public class ConnectionDebugActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We're not implementing this fully for the diagnostic app prototype
        // This is just a placeholder activity to satisfy the manifest
        setContentView(android.R.layout.simple_list_item_1);
        setTitle(R.string.title_connection_debug);
    }
}
