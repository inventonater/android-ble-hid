package com.example.blehid.tv;

import android.content.Context;
import android.util.Log;

import com.example.blehid.core.BleGattServerManager;
import com.example.blehid.tv.lg.LgTvHidService;

/**
 * Factory class for creating TV-specific HID services.
 * This allows the application to easily switch between different TV implementations.
 */
public class TvHidServiceFactory {
    private static final String TAG = "TvHidServiceFactory";
    
    // TV types
    public static final String TV_TYPE_LG = "lg";
    public static final String TV_TYPE_SAMSUNG = "samsung";
    public static final String TV_TYPE_SONY = "sony";
    public static final String TV_TYPE_GENERIC = "generic";
    
    /**
     * Creates a TV HID service based on the specified TV type.
     * 
     * @param tvType The type of TV (e.g., "lg", "samsung")
     * @param gattServerManager The GATT server manager
     * @return The appropriate TvHidService implementation, or null if not supported
     */
    public static TvHidService createService(String tvType, BleGattServerManager gattServerManager) {
        Log.d(TAG, "Creating TV HID service for " + tvType);
        
        if (tvType == null || gattServerManager == null) {
            Log.e(TAG, "Invalid parameters");
            return null;
        }
        
        switch (tvType.toLowerCase()) {
            case TV_TYPE_LG:
                return new LgTvHidService(gattServerManager);
            
            // Add more cases here as additional TV implementations are added
            // case TV_TYPE_SAMSUNG:
            //     return new SamsungTvHidService(gattServerManager);
            
            // case TV_TYPE_SONY:
            //     return new SonyTvHidService(gattServerManager);
            
            default:
                Log.w(TAG, "Unsupported TV type: " + tvType + ", using LG as default");
                return new LgTvHidService(gattServerManager);
        }
    }
    
    /**
     * Gets a list of supported TV types.
     * 
     * @return Array of supported TV types
     */
    public static String[] getSupportedTvTypes() {
        // Currently only LG is implemented
        // Update this as more TV types are added
        return new String[]{TV_TYPE_LG};
    }
    
    /**
     * Gets a user-friendly name for a TV type.
     * 
     * @param tvType The TV type
     * @return A user-friendly name for the TV type
     */
    public static String getTvTypeName(String tvType) {
        switch (tvType.toLowerCase()) {
            case TV_TYPE_LG:
                return "LG Smart TV";
            case TV_TYPE_SAMSUNG:
                return "Samsung Smart TV";
            case TV_TYPE_SONY:
                return "Sony Smart TV";
            case TV_TYPE_GENERIC:
                return "Generic TV";
            default:
                return "Unknown TV Type";
        }
    }
}
