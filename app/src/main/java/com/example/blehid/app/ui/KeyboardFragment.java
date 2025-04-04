package com.example.blehid.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.blehid.app.R;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.BleHidManager;

/**
 * Fragment that handles keyboard input controls
 */
public class KeyboardFragment extends Fragment {
    private static final String TAG = "KeyboardFragment";
    
    // Modifier key buttons
    private Button keyCtrl;
    private Button keyShift;
    private Button keyAlt;
    private Button keyMeta;
    
    // Alpha keys
    private Button buttonA, buttonB, buttonC, buttonD, buttonE, buttonF, buttonG;
    private Button buttonH, buttonI, buttonJ, buttonK, buttonL, buttonM, buttonN;
    private Button buttonO, buttonP, buttonQ, buttonR, buttonS, buttonT, buttonU;
    private Button buttonV, buttonW, buttonX, buttonY, buttonZ;
    
    // Special keys
    private Button buttonSpace, buttonEnter, buttonEsc, buttonTab, buttonBackspace;
    
    private BleHidManager bleHidManager;
    private MouseFragment.HidEventListener eventListener;
    
    public void setBleHidManager(BleHidManager manager) {
        this.bleHidManager = manager;
    }
    
    public void setEventListener(MouseFragment.HidEventListener listener) {
        this.eventListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_keyboard, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize modifier keys
        keyCtrl = view.findViewById(R.id.keyCtrl);
        keyShift = view.findViewById(R.id.keyShift);
        keyAlt = view.findViewById(R.id.keyAlt);
        keyMeta = view.findViewById(R.id.keyMeta);
        
        // Initialize alpha keys
        buttonA = view.findViewById(R.id.buttonA);
        buttonB = view.findViewById(R.id.buttonB);
        buttonC = view.findViewById(R.id.buttonC);
        buttonD = view.findViewById(R.id.buttonD);
        buttonE = view.findViewById(R.id.buttonE);
        buttonF = view.findViewById(R.id.buttonF);
        buttonG = view.findViewById(R.id.buttonG);
        buttonH = view.findViewById(R.id.buttonH);
        buttonI = view.findViewById(R.id.buttonI);
        buttonJ = view.findViewById(R.id.buttonJ);
        buttonK = view.findViewById(R.id.buttonK);
        buttonL = view.findViewById(R.id.buttonL);
        buttonM = view.findViewById(R.id.buttonM);
        buttonN = view.findViewById(R.id.buttonN);
        buttonO = view.findViewById(R.id.buttonO);
        buttonP = view.findViewById(R.id.buttonP);
        buttonQ = view.findViewById(R.id.buttonQ);
        buttonR = view.findViewById(R.id.buttonR);
        buttonS = view.findViewById(R.id.buttonS);
        buttonT = view.findViewById(R.id.buttonT);
        buttonU = view.findViewById(R.id.buttonU);
        buttonV = view.findViewById(R.id.buttonV);
        buttonW = view.findViewById(R.id.buttonW);
        buttonX = view.findViewById(R.id.buttonX);
        buttonY = view.findViewById(R.id.buttonY);
        buttonZ = view.findViewById(R.id.buttonZ);
        
        // Initialize special keys
        buttonSpace = view.findViewById(R.id.buttonSpace);
        buttonEnter = view.findViewById(R.id.buttonEnter);
        buttonEsc = view.findViewById(R.id.buttonEsc);
        buttonTab = view.findViewById(R.id.buttonTab);
        buttonBackspace = view.findViewById(R.id.buttonBackspace);
        
        setupControls();
    }
    
    private void setupControls() {
        // Modifier keys
        keyCtrl.setOnClickListener(v -> sendKeyWithModifiers(HidConstants.Keyboard.MODIFIER_LEFT_CTRL, (byte)0));
        keyShift.setOnClickListener(v -> sendKeyWithModifiers(HidConstants.Keyboard.MODIFIER_LEFT_SHIFT, (byte)0));
        keyAlt.setOnClickListener(v -> sendKeyWithModifiers(HidConstants.Keyboard.MODIFIER_LEFT_ALT, (byte)0));
        keyMeta.setOnClickListener(v -> sendKeyWithModifiers(HidConstants.Keyboard.MODIFIER_LEFT_GUI, (byte)0));
        
        // Alpha keys
        buttonA.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_A));
        buttonB.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_B));
        buttonC.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_C));
        buttonD.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_D));
        buttonE.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_E));
        buttonF.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_F));
        buttonG.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_G));
        buttonH.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_H));
        buttonI.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_I));
        buttonJ.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_J));
        buttonK.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_K));
        buttonL.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_L));
        buttonM.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_M));
        buttonN.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_N));
        buttonO.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_O));
        buttonP.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_P));
        buttonQ.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_Q));
        buttonR.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_R));
        buttonS.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_S));
        buttonT.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_T));
        buttonU.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_U));
        buttonV.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_V));
        buttonW.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_W));
        buttonX.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_X));
        buttonY.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_Y));
        buttonZ.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_Z));
        
        // Special keys
        buttonSpace.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_SPACE));
        buttonEnter.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_RETURN));
        buttonEsc.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_ESCAPE));
        buttonTab.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_TAB));
        buttonBackspace.setOnClickListener(v -> sendKeyWithModifiers((byte)0, HidConstants.Keyboard.KEY_DELETE));
    }
    
    private void sendKeyWithModifiers(byte modifiers, byte key) {
        if (bleHidManager == null || !bleHidManager.isConnected()) {
            logEvent("KEYBOARD KEY IGNORED: No connected device");
            return;
        }
        
        String keyName = getKeyName(modifiers, key);
        
        // Press the key
        boolean pressResult = bleHidManager.sendKeyWithModifiers(key, modifiers);
        
        // Add a delay for the key press to register
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Release the key (send empty report)
        boolean releaseResult = bleHidManager.releaseAllKeys();
        boolean result = pressResult && releaseResult;
        
        logEvent("KEYBOARD KEY: " + keyName + (result ? " pressed" : " FAILED"));
    }
    
    private String getKeyName(byte modifiers, byte key) {
        StringBuilder name = new StringBuilder();
        
        // Add modifiers
        if ((modifiers & HidConstants.Keyboard.MODIFIER_LEFT_CTRL) != 0) {
            name.append("CTRL+");
        }
        if ((modifiers & HidConstants.Keyboard.MODIFIER_LEFT_SHIFT) != 0) {
            name.append("SHIFT+");
        }
        if ((modifiers & HidConstants.Keyboard.MODIFIER_LEFT_ALT) != 0) {
            name.append("ALT+");
        }
        if ((modifiers & HidConstants.Keyboard.MODIFIER_LEFT_GUI) != 0) {
            name.append("META+");
        }
        
        // Add the key name
        if (key >= HidConstants.Keyboard.KEY_A && key <= HidConstants.Keyboard.KEY_Z) {
            // Alphabet keys
            name.append((char) ('A' + (key - HidConstants.Keyboard.KEY_A)));
        } else {
            // Special keys
            switch (key) {
                case HidConstants.Keyboard.KEY_SPACE:
                    name.append("SPACE");
                    break;
                case HidConstants.Keyboard.KEY_ESCAPE:
                    name.append("ESC");
                    break;
                case HidConstants.Keyboard.KEY_RETURN:
                    name.append("ENTER");
                    break;
                case HidConstants.Keyboard.KEY_DELETE:
                    name.append("DELETE");
                    break;
                case HidConstants.Keyboard.KEY_TAB:
                    name.append("TAB");
                    break;
                case 0:
                    // Just a modifier
                    if (name.length() > 0) {
                        name.setLength(name.length() - 1); // Remove the trailing '+'
                    } else {
                        name.append("NONE");
                    }
                    break;
                default:
                    name.append("KEY_0x").append(Integer.toHexString(key & 0xFF));
            }
        }
        
        return name.toString();
    }
    
    private void logEvent(String event) {
        if (eventListener != null) {
            eventListener.onHidEvent(event);
        }
    }
}
