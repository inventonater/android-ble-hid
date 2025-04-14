# Bluetooth Connection Parameters Guide

## Overview

Bluetooth Low Energy (BLE) connection parameters play a critical role in determining the performance, responsiveness, and power consumption of your BLE connection. This guide explains each parameter, its effects, and provides recommendations for different use cases.

## Key Connection Parameters

### Connection Interval

**What it is:** The time between connection events, measured in milliseconds. During a connection event, devices exchange data packets.

**Range:** 7.5ms to 4000ms (typically)

**Effects:**
- **Shorter intervals:** More frequent communication, lower latency, higher throughput, but higher power consumption
- **Longer intervals:** Less frequent communication, higher latency, lower throughput, but lower power consumption

**Notes:** The actual interval used is negotiated between devices. The central device proposes an interval, but the peripheral may not accept it exactly as requested.

### Slave Latency

**What it is:** The number of connection events a peripheral device can skip without disconnecting.

**Range:** 0 to 499 (typically)

**Effects:**
- **Higher latency:** Allows peripheral to save power by skipping connection events when it has no data to send
- **Zero latency:** Peripheral must respond to every connection event, resulting in lower latency but higher power consumption

**Notes:** Higher slave latency values are beneficial when the peripheral mostly listens and rarely sends data.

### Supervision Timeout

**What it is:** The maximum time between successful connection events before the connection is considered lost.

**Range:** 100ms to 32000ms (typically)

**Effects:**
- **Shorter timeout:** Faster detection of lost connections, but may cause premature disconnections in noisy environments
- **Longer timeout:** More tolerant of temporary connection issues, but takes longer to detect when a connection is truly lost

**Notes:** Should always be longer than: Connection Interval × (1 + Slave Latency)

### MTU Size (Maximum Transmission Unit)

**What it is:** The maximum size of an Attribute Protocol (ATT) packet.

**Range:** 23 to 517 bytes

**Effects:**
- **Larger MTU:** More data per packet, higher throughput, fewer round trips
- **Smaller MTU:** Less data per packet, more round trips needed for large transfers

**Notes:** Both devices must support the requested MTU size. Larger MTU values can significantly improve throughput.

### Transmit Power Level

**What it is:** The power level used for transmitting BLE packets.

**Range:** Low, Medium, High (implementation-specific values)

**Effects:**
- **Higher power:** Longer range, more reliable connection, but higher power consumption
- **Lower power:** Shorter range, potentially less reliable connection, but lower power consumption

**Notes:** Adjusting transmit power can help optimize for different physical distances between devices.

### RSSI (Received Signal Strength Indicator)

**What it is:** A measurement of signal strength, in dBm (decibels relative to a milliwatt).

**Range:** Typically -30 dBm (very strong) to -90 dBm (very weak)

**Effects:**
- **Higher RSSI (closer to 0):** Stronger signal, more reliable connection
- **Lower RSSI (more negative):** Weaker signal, less reliable connection

**Notes:** RSSI values around -70 dBm or higher generally indicate a good connection. Values below -80 dBm may experience connectivity issues.

## Default Settings for Maximum Performance

This implementation is optimized for maximum performance, prioritizing lowest possible latency and strongest signal over battery life considerations. The following default settings are applied automatically:

- **Connection Priority:** HIGH (minimum latency)
- **Connection Interval:** 7.5ms (targeting minimum possible value)
- **Slave Latency:** 0 (no skipped connection events)
- **MTU Size:** 512 bytes (maximizing data throughput)
- **Transmit Power Level:** HIGH (maximizing signal strength)
- **RSSI Monitoring Frequency:** 500ms (more frequent updates for better signal tracking)

These settings provide the most responsive and reliable connection possible, suitable for real-time control applications where instantaneous response is critical. Battery consumption on connected devices will be higher with these settings, but reliability and low latency are prioritized.

## Connection Priority Presets

Our application provides three connection priority presets, with HIGH being the default:

### High Priority (Low Latency) - DEFAULT
- **Connection Interval:** 7.5-15ms
- **Slave Latency:** 0
- **Suitable for:** Gaming, real-time controls, immediate response applications
- **Trade-off:** Higher power consumption

### Balanced
- **Connection Interval:** 30-50ms
- **Slave Latency:** 2-4
- **Suitable for:** Most general-purpose applications
- **Trade-off:** Good balance between responsiveness and power usage

### Low Power
- **Connection Interval:** 100-500ms
- **Slave Latency:** 4-6
- **Suitable for:** Infrequent updates, sensor readings, battery-critical applications
- **Trade-off:** Higher latency, lower responsiveness

## Recommended Settings by Use Case

### Gaming/Real-time Controls
- **Connection Priority:** High
- **MTU Size:** Highest supported (typically 185+)
- **Transmit Power:** Medium to High
- **Notes:** Optimize for minimum latency; power consumption is less critical

### Media Control
- **Connection Priority:** Balanced
- **MTU Size:** 100-185
- **Transmit Power:** Medium
- **Notes:** Good responsiveness with reasonable power consumption

### Sensor Monitoring
- **Connection Priority:** Low Power
- **MTU Size:** Default (23) is often sufficient
- **Transmit Power:** Low to Medium depending on distance
- **Notes:** Optimize for battery life when immediate updates aren't critical

### Extended Range Operations
- **Connection Priority:** Balanced or Low Power
- **MTU Size:** Lower values may be more reliable
- **Transmit Power:** High
- **Notes:** Maximize transmit power but use longer connection intervals to save power

## Troubleshooting Connection Issues

### Poor Throughput
- Increase MTU size
- Decrease connection interval
- Check RSSI values and increase transmit power if signal is weak

### High Latency
- Decrease connection interval
- Reduce or eliminate slave latency
- Ensure supervision timeout is appropriate

### Frequent Disconnections
- Increase supervision timeout
- Check RSSI values (should be stronger than -80 dBm)
- Increase transmit power
- Reduce MTU size if packet corruption might be occurring

### Excessive Battery Drain
- Increase connection interval
- Increase slave latency
- Reduce transmit power if RSSI values are good
- Use appropriate connection priority for your use case

## Advanced Considerations

### Environmental Factors
- Physical obstacles between devices can weaken signals (RSSI)
- RF interference can affect optimal parameter choices
- Distance between devices impacts reliable connection intervals

### Device Compatibility
- Not all peripherals support all parameter values
- Some devices may reject parameter change requests
- Legacy devices may have more limited parameter ranges

### Monitoring Tips
- Regularly check RSSI values to assess connection quality
- Monitor actual connection parameters vs. requested parameters
- Adjust based on real-world performance rather than theoretical values

## Conclusion

Finding the optimal connection parameters is often a balance between responsiveness, throughput, and power consumption. Start with one of the recommended profiles based on your primary use case, then fine-tune as needed based on actual performance monitoring.

Remember that the actual parameters used in a connection are negotiated between devices, and the peripheral may not accept all requested changes. Always verify the actual values reported back after making adjustments.
