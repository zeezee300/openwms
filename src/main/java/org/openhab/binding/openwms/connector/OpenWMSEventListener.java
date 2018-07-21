package org.openhab.binding.openwms.connector;

public interface OpenWMSEventListener {

    /**
     * Procedure for receive raw data from controller.
     *
     * @param data
     *            Received raw data.
     */
    void packetReceived(byte[] data);

    void packetStrReceived(String data);

    /**
     * Procedure for receiving information fatal error.
     *
     * @param error
     *            Error occurred.
     */
    void errorOccurred(String error);

}
