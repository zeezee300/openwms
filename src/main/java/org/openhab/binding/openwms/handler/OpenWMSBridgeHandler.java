package org.openhab.binding.openwms.handler;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.openwms.config.OpenWMSBindingConstants;
import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;
import org.openhab.binding.openwms.connector.OpenWMSConnectorInterface;
import org.openhab.binding.openwms.connector.OpenWMSEventListener;
import org.openhab.binding.openwms.connector.OpenWMSSerialConnector;
import org.openhab.binding.openwms.connector.OpenWMSTcpConnector;
import org.openhab.binding.openwms.internal.DeviceMessageListener;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.openhab.binding.openwms.messages.OpenWMSMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NoSuchPortException;
/*
* @author zeezee - Initial contribution
*/

public class OpenWMSBridgeHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(OpenWMSBridgeHandler.class);

    private OpenWMSBridgeConfiguration configuration = null;
    private ScheduledFuture<?> connectorTask;
    private OpenWMSConnectorInterface connector = null;
    private MessageListener eventListener = new MessageListener();
    private List<DeviceMessageListener> deviceStatusListeners = new CopyOnWriteArrayList<>();

    private SerialPortManager serialPortManager;

    public OpenWMSBridgeHandler(@NonNull Bridge br, SerialPortManager serialPortManager) {
        super(br);
        this.serialPortManager = serialPortManager;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Bridge commands not supported.");
    }

    @Override
    public synchronized void dispose() {
        logger.debug("Handler disposed.");

        for (DeviceMessageListener deviceStatusListener : deviceStatusListeners) {
            unregisterDeviceStatusListener(deviceStatusListener);
        }

        if (connector != null) {
            connector.removeEventListener(eventListener);
            connector.disconnect();
            connector = null;
        }

        if (connectorTask != null && !connectorTask.isCancelled()) {
            connectorTask.cancel(true);
            connectorTask = null;
        }

        super.dispose();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing WMS bridge handler");
        updateStatus(ThingStatus.OFFLINE);

        configuration = getConfigAs(OpenWMSBridgeConfiguration.class);

        if (connectorTask == null || connectorTask.isCancelled()) {
            connectorTask = scheduler.scheduleWithFixedDelay(() -> {
                logger.debug("Checking OpenWMS transceiver connection, thing status should be = {}", thing.getStatus());
                if (thing.getStatus() != ThingStatus.ONLINE) {
                    connect();
                } else {
                    logger.debug("Checke ob WMS bridge handler tatsächlich online ist");
                    try {
                        connector.sendMessage(OpenWMSMessageFactory.CHECK.getBytes());
                        // Thread.sleep(1000);
                    } catch (IOException e1) {
                        logger.debug("WMS bridge handler ist offline");
                        updateStatus(ThingStatus.OFFLINE);
                        e1.printStackTrace();
                    }

                }
            }, 0, 60, TimeUnit.SECONDS);
        }
    }

    private synchronized void connect() {
        logger.debug("Connecting to OpenWMS transceiver");

        try {
            if (configuration.serialPort != null) {
                if (connector == null) {
                    connector = new OpenWMSSerialConnector(serialPortManager);

                }

            } else if (configuration.host != null) {
                if (connector == null) {
                    connector = new OpenWMSTcpConnector();
                }
            }

            if (connector != null) {
                connector.disconnect();
                connector.connect(configuration);

                // connector.addEventListener(eventListener);

                // controller does not response immediately after reset,
                // so wait a while
                // Thread.sleep(600);
                // String messageString = "{G}";
                // connector.sendMessage(messageString.getBytes());
                // byte[] data = messageString.getBytes();
                // Thread.sleep(600);
                connector.addEventListener(eventListener);

                logger.debug("Try to send to controller");
                // ToDo
                // connector.sendMessage(RFXComMessageFactory.CMD_GET_STATUS);
                Integer secondsRemaining = 1;
                while (secondsRemaining > 0) {
                    // System.out.println("Sekunden verbleiben: " + secondsRemaining.toString());
                    secondsRemaining--;
                    connector.sendMessage(OpenWMSMessageFactory.CHECK.getBytes());

                    // connector.sendMessage("{R06E49D08801001000005}".getBytes());
                    // sendMessage("{R06E49D08801001000005}");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        updateStatus(ThingStatus.ONLINE);
                        // updateStatus(ThingStatus.OFFLINE); 31.12.2019
                    }
                }
                // connector.sendMessage(messageString.getBytes());
                // updateStatus(ThingStatus.ONLINE);

            }
        } catch (NoSuchPortException e) {
            logger.error("Connection to OpenWMS transceiver failed - NoSuchPortException", e);
        } catch (IOException e) {
            logger.error("Connection to OpenWMS transceiver failed - IOException", e);
            if ("device not opened (3)".equalsIgnoreCase(e.getMessage())) {

            }
        } catch (Exception e) {
            logger.error("Connection to OpenWMS transceiver failed - Exception", e);
        } catch (UnsatisfiedLinkError e) {
            logger.error("Error occurred when trying to load native library for OS '{}' version '{}', processor '{}'",
                    System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"), e);
        }
    }

    public OpenWMSBridgeConfiguration getConfiguration() {
        return configuration;
    }

    private class MessageListener implements OpenWMSEventListener {

        @Override
        public void errorOccurred(String error) {
            logger.error("Error occurred: {}", error);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }

        @Override
        public void packetStrReceived(String data) {
            logger.debug("Message received packet: {}", data);
            try {
                if (data.replaceAll("[{}]", "").equals("gWMS USB-Stick")) {
                    logger.debug("Get USB-Stick Version");
                    updateStatus(ThingStatus.ONLINE);

                    try {
                        connector.sendMessage(OpenWMSMessageFactory.VERSION.getBytes());
                        // thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION, "Version 123456");

                    } catch (IOException e) {

                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } else if (data.replaceAll("[{}]", "").substring(0, 1).equals("v")) {
                    logger.debug("Set USB-Stick Version");
                    // thing.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, data.replaceAll("[{}]", ""));
                    thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION, data.replaceAll("[{}]", ""));

                } else {

                    if (data.replaceAll("[{}]", "").substring(0, 1).equals("r")) {
                        // String txt = getThing().getThingTypeUID().getId().toUpperCase();

                        OpenWMSGetResponse wmsMsg = new OpenWMSGetResponse(data);

                        for (DeviceMessageListener deviceStatusListener : deviceStatusListeners) {
                            try {
                                deviceStatusListener.onDeviceMessageReceived(getThing().getUID(), wmsMsg);
                            } catch (Exception e) {
                                // catch all exceptions give all handlers a fair chance of handling the messages
                                logger.error("An exception occurred while calling the DeviceStatusListener", e);
                            }
                        }
                        // auf die jeweiligen empfangenen "r"-Messages reagieren und Antwort senden .....
                        if (wmsMsg.wms_response != null) {
                            sendMessage(wmsMsg.wms_response);
                        }
                        if (wmsMsg.networkid != null) {
                            logger.debug("Set NetworkId {}", wmsMsg.networkid);
                            thing.setProperty(OpenWMSBindingConstants.PROPERTY_NETWORKKEY, wmsMsg.networkid);

                        }
                        if (wmsMsg.panId != null && wmsMsg.panId != "") {
                            logger.debug("Set PANID {}", wmsMsg.panId);
                            thing.setProperty(OpenWMSBindingConstants.PROPERTY_PANID, wmsMsg.panId);

                        }

                    }
                }

                // ToDo - auf die jeweiligen Antworten reagieren.....

                transmitQueue.sendNext();

            } catch (Exception e) {
                logger.error("Error occurred: {}", e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }

        }

    }

    public void sendMessage(String msg) {
        try {

            transmitQueue.enqueue(msg);
        } catch (IOException e) {
            logger.error("I/O Error", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private class TransmitQueue {
        private Queue<String> queue = new LinkedBlockingQueue<>();

        public synchronized void enqueue(String msg) throws IOException {
            boolean wasEmpty = queue.isEmpty();
            if (queue.offer(msg)) {
                if (wasEmpty) {

                    send();
                }
            } else {
                logger.error("Transmit queue overflow. Lost message: {}", msg);
            }
        }

        public synchronized void sendNext() throws IOException {
            queue.poll();
            send();
        }

        public synchronized void send() throws IOException {
            while (!queue.isEmpty()) {
                String msg = queue.peek();

                try {
                    logger.debug("Transmitting message '{}'", msg);
                    connector.sendMessage(msg.getBytes());
                    break;
                } catch (IOException e) {
                    logger.error("Error during send of {}", msg, e);
                    queue.poll();
                }
            }
        }
    }

    private TransmitQueue transmitQueue = new TransmitQueue();

    public boolean registerDeviceStatusListener(DeviceMessageListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null deviceStatusListener.");
        }
        return deviceStatusListeners.contains(deviceStatusListener) ? false
                : deviceStatusListeners.add(deviceStatusListener);
    }

    public boolean unregisterDeviceStatusListener(DeviceMessageListener deviceStatusListener) {
        if (deviceStatusListener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null deviceStatusListener.");
        }
        return deviceStatusListeners.remove(deviceStatusListener);
    }

}
