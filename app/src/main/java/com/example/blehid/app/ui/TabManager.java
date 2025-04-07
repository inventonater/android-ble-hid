package com.example.blehid.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.Button;

import com.example.blehid.app.R;

/**
 * Manages the tab switching functionality.
 * Handles UI state for media, mouse, and keyboard panels.
 */
public class TabManager {

    private final Context context;
    private final Button mediaTabButton;
    private final Button mouseTabButton;
    private final Button keyboardTabButton;
    private final View mediaPanel;
    private final View mousePanel;
    private final View keyboardPanel;

    /**
     * Creates a new TabManager.
     *
     * @param context The activity context
     * @param mediaTabButton Button for the media tab
     * @param mouseTabButton Button for the mouse tab
     * @param keyboardTabButton Button for the keyboard tab
     * @param mediaPanel View containing media controls
     * @param mousePanel View containing mouse controls
     * @param keyboardPanel View containing keyboard controls
     */
    public TabManager(Context context, 
                      Button mediaTabButton, 
                      Button mouseTabButton, 
                      Button keyboardTabButton,
                      View mediaPanel,
                      View mousePanel,
                      View keyboardPanel) {
        this.context = context;
        this.mediaTabButton = mediaTabButton;
        this.mouseTabButton = mouseTabButton;
        this.keyboardTabButton = keyboardTabButton;
        this.mediaPanel = mediaPanel;
        this.mousePanel = mousePanel;
        this.keyboardPanel = keyboardPanel;

        setupTabSwitching();
    }

    /**
     * Sets up tab switching functionality.
     */
    private void setupTabSwitching() {
        // Media tab click listener
        mediaTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMediaTab();
            }
        });
        
        // Mouse tab click listener
        mouseTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMouseTab();
            }
        });
        
        // Keyboard tab click listener
        keyboardTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboardTab();
            }
        });
        
        // Set media tab as default
        showMediaTab();
    }

    /**
     * Shows the media control tab.
     */
    public void showMediaTab() {
        // Show media panel, hide others
        mediaPanel.setVisibility(View.VISIBLE);
        mousePanel.setVisibility(View.GONE);
        keyboardPanel.setVisibility(View.GONE);
        
        // Update tab button styling
        mediaTabButton.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
        mouseTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        keyboardTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
    }

    /**
     * Shows the mouse control tab.
     */
    public void showMouseTab() {
        // Show mouse panel, hide others
        mousePanel.setVisibility(View.VISIBLE);
        mediaPanel.setVisibility(View.GONE);
        keyboardPanel.setVisibility(View.GONE);
        
        // Update tab button styling
        mouseTabButton.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
        mediaTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        keyboardTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
    }

    /**
     * Shows the keyboard control tab.
     */
    public void showKeyboardTab() {
        // Show keyboard panel, hide others
        keyboardPanel.setVisibility(View.VISIBLE);
        mediaPanel.setVisibility(View.GONE);
        mousePanel.setVisibility(View.GONE);
        
        // Update tab button styling
        keyboardTabButton.setBackgroundTintList(getColorStateList(android.R.color.holo_blue_dark));
        mediaTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        mouseTabButton.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
    }

    /**
     * Helper method to get color state list from a resource.
     */
    private ColorStateList getColorStateList(int colorResource) {
        return context.getColorStateList(colorResource);
    }
}
