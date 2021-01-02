package org.openhab.binding.openwms.messages;

import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.openwms.internal.OpenWMSDeviceConfiguration;

public interface OpenWMSMessage {

    /**
     * Procedure for encode raw data.
     *
     * @param data
     *                 Raw data.
     */
    void encodeMessage(byte[] data);

    /**
     * Procedure for decode object to raw data.
     *
     * @return raw data.
     */
    byte[] decodeMessage();

    /**
     * Procedure for converting openHAB state to WMS object.
     *
     */
    void convertFromState(String channelId, Type type);

    /**
     * Procedure to pass configuration to a message
     *
     * @param deviceConfiguration configuration about the device
     *
     */
    void setConfig(OpenWMSDeviceConfiguration deviceConfiguration);
}
