package com.inventonater.blehid.descriptor;

import com.inventonater.blehid.HidReportConstants;

/**
 * HID descriptors for mouse, keyboard, and consumer control.
 * 
 * This class contains the report descriptors required by the HID service.
 * These descriptors follow the USB HID specification and define the format
 * and capabilities of the HID reports.
 */
public class HidDescriptors {
    /**
     * Combined report map for mouse, keyboard, and consumer control.
     * This descriptor is based on the USB HID specification and is tested
     * for broad compatibility with various host devices.
     */
    public static final byte[] REPORT_MAP = new byte[] {
        // Mouse descriptor (Report ID 2)
        (byte) 0x05, (byte) 0x01,         // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x02,         // USAGE (Mouse)
        (byte) 0xA1, (byte) 0x01,         // COLLECTION (Application)
        (byte) 0x85, HidReportConstants.REPORT_ID_MOUSE,  // REPORT_ID (2)
        (byte) 0x09, (byte) 0x01,         //   USAGE (Pointer)
        (byte) 0xA1, (byte) 0x00,         //   COLLECTION (Physical)
        
        // Buttons (5 buttons)
        (byte) 0x05, (byte) 0x09,         //     USAGE_PAGE (Button)
        (byte) 0x19, (byte) 0x01,         //     USAGE_MINIMUM (Button 1)
        (byte) 0x29, (byte) 0x05,         //     USAGE_MAXIMUM (Button 5)
        (byte) 0x15, (byte) 0x00,         //     LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,         //     LOGICAL_MAXIMUM (1)
        (byte) 0x95, (byte) 0x05,         //     REPORT_COUNT (5)
        (byte) 0x75, (byte) 0x01,         //     REPORT_SIZE (1)
        (byte) 0x81, (byte) 0x02,         //     INPUT (Data,Var,Abs)
        
        // Button padding (3 bits)
        (byte) 0x95, (byte) 0x01,         //     REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x03,         //     REPORT_SIZE (3)
        (byte) 0x81, (byte) 0x01,         //     INPUT (Cnst)
        
        // X, Y, and wheel movement
        (byte) 0x05, (byte) 0x01,         //     USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x30,         //     USAGE (X)
        (byte) 0x09, (byte) 0x31,         //     USAGE (Y)
        (byte) 0x09, (byte) 0x38,         //     USAGE (Wheel)
        (byte) 0x15, (byte) 0x81,         //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7F,         //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,         //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x03,         //     REPORT_COUNT (3)
        (byte) 0x81, (byte) 0x06,         //     INPUT (Data,Var,Rel)
        (byte) 0xC0,                      //   END_COLLECTION
        (byte) 0xC0,                      // END_COLLECTION
        
        // Keyboard descriptor (Report ID 1)
        (byte) 0x05, (byte) 0x01,         // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x06,         // USAGE (Keyboard)
        (byte) 0xA1, (byte) 0x01,         // COLLECTION (Application)
        (byte) 0x85, HidReportConstants.REPORT_ID_KEYBOARD,  // REPORT_ID (1)
        
        // Modifier keys (ctrl, shift, alt, gui)
        (byte) 0x05, (byte) 0x07,         //   USAGE_PAGE (Keyboard)
        (byte) 0x19, (byte) 0xE0,         //   USAGE_MINIMUM (Keyboard LeftControl)
        (byte) 0x29, (byte) 0xE7,         //   USAGE_MAXIMUM (Keyboard Right GUI)
        (byte) 0x15, (byte) 0x00,         //   LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,         //   LOGICAL_MAXIMUM (1)
        (byte) 0x75, (byte) 0x01,         //   REPORT_SIZE (1)
        (byte) 0x95, (byte) 0x08,         //   REPORT_COUNT (8)
        (byte) 0x81, (byte) 0x02,         //   INPUT (Data,Var,Abs)
        
        // Reserved byte
        (byte) 0x95, (byte) 0x01,         //   REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x08,         //   REPORT_SIZE (8)
        (byte) 0x81, (byte) 0x01,         //   INPUT (Cnst)
        
        // LEDs (Num Lock, Caps Lock, Scroll Lock, Compose, Kana)
        (byte) 0x95, (byte) 0x05,         //   REPORT_COUNT (5)
        (byte) 0x75, (byte) 0x01,         //   REPORT_SIZE (1)
        (byte) 0x05, (byte) 0x08,         //   USAGE_PAGE (LEDs)
        (byte) 0x19, (byte) 0x01,         //   USAGE_MINIMUM (Num Lock)
        (byte) 0x29, (byte) 0x05,         //   USAGE_MAXIMUM (Kana)
        (byte) 0x91, (byte) 0x02,         //   OUTPUT (Data,Var,Abs)
        
        // LED padding (3 bits)
        (byte) 0x95, (byte) 0x01,         //   REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x03,         //   REPORT_SIZE (3)
        (byte) 0x91, (byte) 0x01,         //   OUTPUT (Cnst)
        
        // Key codes (6 keys)
        (byte) 0x95, (byte) 0x06,         //   REPORT_COUNT (6)
        (byte) 0x75, (byte) 0x08,         //   REPORT_SIZE (8)
        (byte) 0x15, (byte) 0x00,         //   LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x65,         //   LOGICAL_MAXIMUM (101)
        (byte) 0x05, (byte) 0x07,         //   USAGE_PAGE (Keyboard)
        (byte) 0x19, (byte) 0x00,         //   USAGE_MINIMUM (Reserved (no event indicated))
        (byte) 0x29, (byte) 0x65,         //   USAGE_MAXIMUM (Keyboard Application)
        (byte) 0x81, (byte) 0x00,         //   INPUT (Data,Ary,Abs)
        (byte) 0xC0,                      // END_COLLECTION
        
        // Consumer Control descriptor (Report ID 3)
        (byte) 0x05, (byte) 0x0C,         // USAGE_PAGE (Consumer Devices)
        (byte) 0x09, (byte) 0x01,         // USAGE (Consumer Control)
        (byte) 0xA1, (byte) 0x01,         // COLLECTION (Application)
        (byte) 0x85, HidReportConstants.REPORT_ID_CONSUMER,  // REPORT_ID (3)
        (byte) 0x15, (byte) 0x00,         //   LOGICAL_MINIMUM (0)
        (byte) 0x26, (byte) 0xFF, (byte) 0x03,  //   LOGICAL_MAXIMUM (1023)
        (byte) 0x19, (byte) 0x00,         //   USAGE_MINIMUM (0)
        (byte) 0x2A, (byte) 0xFF, (byte) 0x03,  //   USAGE_MAXIMUM (1023)
        (byte) 0x75, (byte) 0x10,         //   REPORT_SIZE (16)
        (byte) 0x95, (byte) 0x01,         //   REPORT_COUNT (1)
        (byte) 0x81, (byte) 0x00,         //   INPUT (Data,Ary,Abs)
        (byte) 0xC0                       // END_COLLECTION
    };
    
    /**
     * Mouse report descriptor (for use in isolation).
     */
    public static final byte[] MOUSE_REPORT_MAP = new byte[] {
        (byte) 0x05, (byte) 0x01,         // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x02,         // USAGE (Mouse)
        (byte) 0xA1, (byte) 0x01,         // COLLECTION (Application)
        (byte) 0x85, HidReportConstants.REPORT_ID_MOUSE,  // REPORT_ID (2)
        (byte) 0x09, (byte) 0x01,         //   USAGE (Pointer)
        (byte) 0xA1, (byte) 0x00,         //   COLLECTION (Physical)
        
        // Buttons (5 buttons)
        (byte) 0x05, (byte) 0x09,         //     USAGE_PAGE (Button)
        (byte) 0x19, (byte) 0x01,         //     USAGE_MINIMUM (Button 1)
        (byte) 0x29, (byte) 0x05,         //     USAGE_MAXIMUM (Button 5)
        (byte) 0x15, (byte) 0x00,         //     LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,         //     LOGICAL_MAXIMUM (1)
        (byte) 0x95, (byte) 0x05,         //     REPORT_COUNT (5)
        (byte) 0x75, (byte) 0x01,         //     REPORT_SIZE (1)
        (byte) 0x81, (byte) 0x02,         //     INPUT (Data,Var,Abs)
        
        // Button padding (3 bits)
        (byte) 0x95, (byte) 0x01,         //     REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x03,         //     REPORT_SIZE (3)
        (byte) 0x81, (byte) 0x01,         //     INPUT (Cnst)
        
        // X, Y, and wheel movement
        (byte) 0x05, (byte) 0x01,         //     USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x30,         //     USAGE (X)
        (byte) 0x09, (byte) 0x31,         //     USAGE (Y)
        (byte) 0x09, (byte) 0x38,         //     USAGE (Wheel)
        (byte) 0x15, (byte) 0x81,         //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7F,         //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,         //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x03,         //     REPORT_COUNT (3)
        (byte) 0x81, (byte) 0x06,         //     INPUT (Data,Var,Rel)
        (byte) 0xC0,                      //   END_COLLECTION
        (byte) 0xC0                       // END_COLLECTION
    };
    
    /**
     * Creates a report descriptor with a different device name.
     * This allows for customization of the device name in the HID descriptor.
     *
     * @param deviceName The device name to use
     * @param manufacturerName The manufacturer name to use
     * @return The customized report descriptor
     */
    public static byte[] createCustomizedReportMap(String deviceName, String manufacturerName) {
        // This would implement a more complex logic to create a customized
        // report descriptor, but for now we'll simply return the standard one.
        // A full implementation would insert string descriptors for the device name.
        return REPORT_MAP;
    }
}
