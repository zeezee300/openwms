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

    public OpenWMSBridgeHandler(@NonNull Bridge br) {
        super(br);
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
                logger.debug("Checking OpenWMS transceiver connection, thing status = {}", thing.getStatus());
                if (thing.getStatus() != ThingStatus.ONLINE) {
                    connect();
                }
            }, 0, 60, TimeUnit.SECONDS);
        }
    }

    private synchronized void connect() {
        logger.debug("Connecting to OpenWMS transceiver");

        try {
            if (configuration.serialPort != null) {
                if (connector == null) {
                    connector = new OpenWMSSerialConnector();
                }

            } else if (configuration.host != null) {
                if (connector == null) {
                    connector = new OpenWMSTcpConnector();
                }
            }

            if (connector != null) {
                connector.disconnect();
                connector.connect(configuration);

                // logger.debug("Reset controller");
                // connector.sendMessage(RFXComMessageFactory.CMD_RESET);
                String messageString = "{G}";
                connector.sendMessage(messageString.getBytes());
                byte[] data = messageString.getBytes();

                // controller does not response immediately after reset,
                // so wait a while
                Thread.sleep(300);

                connector.addEventListener(eventListener);

                logger.debug("Try to send to controller");
                // ToDo
                // connector.sendMessage(RFXComMessageFactory.CMD_GET_STATUS);
                Integer secondsRemaining = 1;
                while (secondsRemaining > 0) {
                    // System.out.println("Sekunden verbleiben: " + secondsRemaining.toString());
                    secondsRemaining--;
                    connector.sendMessage(OpenWMSMessageFactory.CHECK.getBytes());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                // connector.sendMessage(messageString.getBytes());
                // updateStatus(ThingStatus.ONLINE);

            }
        } catch (NoSuchPortException e) {
            logger.error("Connection to OpenWMS transceiver failed", e);
        } catch (IOException e) {
            logger.error("Connection to OpenWMS transceiver failed", e);
            if ("device not opened (3)".equalsIgnoreCase(e.getMessage())) {

            }
        } catch (Exception e) {
            logger.error("Connection to OpenWMS transceiver failed", e);
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
        public void packetReceived(byte[] packet) {
            logger.debug("Message received: {}", packet);
            try {

                // OpenWMSMessage message = RFXComMessageFactory.createMessage(packet);
                logger.debug("Message received: {}", packet);
            } catch (Exception e) {
                // // catch all exceptions give all handlers a fair chance of handling the messages
                logger.error("An exception occurred while calling the DeviceStatusListener", e);
            }

            // if (message instanceof RFXComInterfaceMessage) {
            // RFXComInterfaceMessage msg = (RFXComInterfaceMessage) message;
            // if (msg.subType == SubType.RESPONSE) {
            // if (msg.command == Commands.GET_STATUS) {
            // logger.info("RFXCOM transceiver/receiver type: {}, hw version: {}.{}, fw version: {}",
            // msg.transceiverType, msg.hardwareVersion1, msg.hardwareVersion2,
            // msg.firmwareVersion);
            // thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION,
            // msg.hardwareVersion1 + "." + msg.hardwareVersion2);
            // thing.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, Integer.toString(msg.firmwareVersion));
            //
            // if (configuration.ignoreConfig) {
            // logger.debug("Ignoring transceiver configuration");
            // } else {
            // byte[] setMode = null;
            //
            // if (configuration.setMode != null && !configuration.setMode.isEmpty()) {
            // try {
            // setMode = HexUtils.hexToBytes(configuration.setMode);
            // if (setMode.length != 14) {
            // logger.warn("Invalid RFXCOM transceiver mode configuration");
            // setMode = null;
            // }
            // } catch (IllegalArgumentException ee) {
            // logger.warn("Failed to parse setMode data", ee);
            // }
            // } else {
            // RFXComInterfaceControlMessage modeMsg = new RFXComInterfaceControlMessage(
            // msg.transceiverType, configuration);
            // setMode = modeMsg.decodeMessage();
            // }
            //
            // if (setMode != null) {
            // if (logger.isDebugEnabled()) {
            // logger.debug("Setting RFXCOM mode using: {}", HexUtils.bytesToHex(setMode));
            // }
            // connector.sendMessage(setMode);
            // }
            // }
            //
            // // No need to wait for a response to any set mode. We start
            // // regardless of whether it fails and the RFXCOM's buffer
            // // is big enough to queue up the command.
            // logger.debug("Start receiver");
            // connector.sendMessage(RFXComMessageFactory.CMD_START_RECEIVER);
            // }
            // } else if (msg.subType == SubType.START_RECEIVER) {
            // updateStatus(ThingStatus.ONLINE);
            // logger.debug("Start TX of any queued messages");
            // transmitQueue.send();
            // } else {
            // logger.debug("Interface response received: {}", msg);
            // transmitQueue.sendNext();
            // }
            // } else if (message instanceof RFXComTransmitterMessage) {
            // RFXComTransmitterMessage resp = (RFXComTransmitterMessage) message;
            //
            // logger.debug("Transmitter response received: {}", resp);
            //
            // transmitQueue.sendNext();
            // } else if (message instanceof RFXComDeviceMessage) {
            // for (DeviceMessageListener deviceStatusListener : deviceStatusListeners) {
            // try {
            // deviceStatusListener.onDeviceMessageReceived(getThing().getUID(),
            // (RFXComDeviceMessage) message);
            // } catch (Exception e) {
            // // catch all exceptions give all handlers a fair chance of handling the messages
            // logger.error("An exception occurred while calling the DeviceStatusListener", e);
            // }
            // }
            // } else {
            // logger.warn("The received message cannot be processed, please create an "
            // + "issue at the relevant tracker. Received message: {}", message);
            // }
            // } catch (RFXComMessageNotImplementedException e) {
            // logger.debug("Message not supported, data: {}", HexUtils.bytesToHex(packet));
            // } catch (RFXComException e) {
            // logger.error("Error occurred during packet receiving, data: {}", HexUtils.bytesToHex(packet), e);
            // } catch (IOException e) {
            // errorOccurred("I/O error");
            // }
            // }

        }

        @Override
        public void errorOccurred(String error) {
            logger.error("Error occurred: {}", error);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }

        @Override
        public void packetStrReceived(String data) {
            logger.debug("Message received: {}", data);
            try {
                if (data.replaceAll("[{}]", "").equals("gWMS USB-Stick")) {
                    logger.debug("Get USB-Stick Version");
                    updateStatus(ThingStatus.ONLINE);

                    try {
                        connector.sendMessage(OpenWMSMessageFactory.VERSION.getBytes());
                        thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION, "Version 123456");

                    } catch (IOException e) {

                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } else if (data.replaceAll("[{}]", "").substring(0, 1).equals("v")) {
                    logger.debug("Set USB-Stick Version");
                    thing.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, data.replaceAll("[{}]", ""));

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

                        // Map<String, String> message = new Hashtable();
                        // message = OpenWMSMessageFactory.decode(data);
                        // String id = "";
                        // try {
                        // id = message.get("deviceId").toString();
                        // for (Thing appliance : getThing().getThings()) {
                        // // if (appliance.getStatus() == ThingStatus.ONLINE) {
                        // String UID = (String) appliance.getConfiguration().getProperties().get("deviceId");
                        // if (id.equals(UID)) {
                        // // hier wird der Online-Status des Items gesetzt
                        // appliance.setStatusInfo(
                        // new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null));
                        // OpenWMSGetResponse message2 = new OpenWMSGetResponse(data);
                        // // hier werden die Statuswerte der Channel aktualisert
                        // for (Channel channel : appliance.getChannels()) {
                        // logger.debug("Label of channel: {}", channel.getLabel());
                        // String channelId = channel.getUID().getId();
                        // State state = message2.convertToState(channelId);
                        // ChannelUID channelUID = new ChannelUID(appliance.getUID(), channelId);
                        // updateState(channelUID, state);
                        //
                        // }
                        //
                        // }
                        //
                        // }
                        // } catch (Exception e) {
                        //
                        // // TODO Auto-generated catch block
                        // e.printStackTrace();
                        // }

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
