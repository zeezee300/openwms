/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openwms.handler;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.openwms.connector.OpenWMSConnectorInterface;
import org.openhab.binding.openwms.connector.OpenWMSEventListener;
import org.openhab.binding.openwms.connector.OpenWMSSerialConnector;
import org.openhab.binding.openwms.connector.OpenWMSTcpConnector;
import org.openhab.binding.openwms.internal.DeviceMessageListener;
import org.openhab.binding.openwms.internal.OpenWMSBindingConstants;
import org.openhab.binding.openwms.internal.OpenWMSBridgeConfiguration;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.openhab.binding.openwms.messages.OpenWMSMessageFactory;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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
                        logger.error("IO Error: message: {}", e1.getMessage());
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
                    logger.debug("Now sending to controller");
                    connector.sendMessage(OpenWMSMessageFactory.CHECK.getBytes());
                    updateStatus(ThingStatus.ONLINE);
                    // connector.sendMessage("{R06E49D08801001000005}".getBytes());
                    // sendMessage("{R06E49D08801001000005}");

                    // try {
                    // Thread.sleep(1000);
                    // } catch (InterruptedException e) {
                    // updateStatus(ThingStatus.ONLINE);
                    // logger.debug("Error sending to controller, message: {} ", e.getMessage());
                    // updateStatus(ThingStatus.OFFLINE); 31.12.2019
                    // }
                }
                // connector.sendMessage(messageString.getBytes());
                // updateStatus(ThingStatus.ONLINE);

            }
        } catch (org.openhab.core.io.transport.serial.PortInUseException e) {
            logger.error("Connection to OpenWMS transceiver failed - Port in Use exception", e);
        } catch (org.openhab.core.io.transport.serial.UnsupportedCommOperationException e) {
            logger.error("Connection to OpenWMS transceiver failed - Unsupported Comm Operation exception", e);
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
                // weiter nur dann, wenn überhaupt Daten vorhanden sind....
                if (data.replaceAll("[{}]", "").length() > 0) {
                    if (data.replaceAll("[{}]", "").equals("gWMS USB-Stick")) {
                        logger.debug("Get USB-Stick Version");
                        updateStatus(ThingStatus.ONLINE);

                        try {
                            connector.sendMessage(OpenWMSMessageFactory.VERSION.getBytes());
                            // thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION, "Version 123456");

                        } catch (IOException e) {

                            // TODO Auto-generated catch block
                            logger.error("IO Error: message: {}", e.getMessage());
                        }

                    } else if (data.replaceAll("[{}]", "").substring(0, 1).equals("v")) {
                        logger.debug("Set USB-Stick Version");
                        // thing.setProperty(Thing.PROPERTY_FIRMWARE_VERSION, data.replaceAll("[{}]", ""));
                        thing.setProperty(Thing.PROPERTY_HARDWARE_VERSION, data.replaceAll("[{}]", ""));

                    } else if (data.replaceAll("[{}]", "").substring(0, 1).equals("f")) {
                        logger.debug("{f} An error occurd with the last command send to the WMS Stick");

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
                }

                // ToDo - auf die jeweiligen Antworten reagieren.....

                transmitQueue.sendNext();

            } catch (Exception e) {
                logger.error("Error occurred: {}", e.getMessage());
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
