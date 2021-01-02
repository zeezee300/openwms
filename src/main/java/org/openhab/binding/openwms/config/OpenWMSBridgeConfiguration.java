package org.openhab.binding.openwms.config;

/*
 *
* @author zeezee - Initial contribution
*/
public class OpenWMSBridgeConfiguration {

    public static final String SERIAL_PORT = "serialPort";
    public static final String BRIDGE_ID = "bridgeId";

    // Serial port for manual configuration
    public String serialPort;

    // Configuration for discovered bridge devices
    public String bridgeId;

    // Host for using OpenWMS USB over TCP/IP
    public String host;

    // Port for using OpenWMS USB over TCP/IP
    public int port;

    public String chanel;

    public String NetzID;

    // Prevent unknown devices from being added to the inbox
    public boolean disableDiscovery;

}
