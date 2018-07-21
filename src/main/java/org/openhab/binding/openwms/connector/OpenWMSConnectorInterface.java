package org.openhab.binding.openwms.connector;

import java.io.IOException;

import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;

public interface OpenWMSConnectorInterface {

    /**
     * Procedure for connecting to OpenWMS controller.
     *
     * @param device
     *            Controller connection parameters (e.g. serial port name or IP
     *            address).
     */
    public void connect(OpenWMSBridgeConfiguration device) throws Exception;

    /**
     * Procedure for disconnecting to OpenWMS controller.
     *
     */
    public void disconnect();

    /**
     * Procedure for send raw data to OpenWMS controller.
     *
     * @param data
     *            raw bytes.
     */
    public void sendMessage(byte[] data) throws IOException;

    /**
     * Procedure for register event listener.
     *
     * @param listener
     *            Event listener instance to handle events.
     */
    public void addEventListener(OpenWMSEventListener listener);

    /**
     * Procedure for remove event listener.
     *
     * @param listener
     *            Event listener instance to remove.
     */
    public void removeEventListener(OpenWMSEventListener listener);

}