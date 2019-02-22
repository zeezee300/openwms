package org.openhab.binding.openwms.internal;

import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;

public interface DeviceMessageListener {

    /**
     * This method is called whenever the message is received from the bridge.
     *
     * @param bridge
     *                    The OpenWMS bridge where message is received.
     * @param message
     *                    The message which received.
     */
    void onDeviceMessageReceived(ThingUID bridge, OpenWMSGetResponse message);

}
