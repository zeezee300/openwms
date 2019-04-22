/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openwms.internal;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.openwms.handler.OpenWMSBridgeHandler;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.openhab.binding.openwms.messages.OpenWMSMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OpenWMSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 *
 * @author zeezee - Initial contribution
 */
// @NonNullByDefault
public class OpenWMSHandler extends BaseThingHandler implements DeviceMessageListener {

    private final Logger logger = LoggerFactory.getLogger(OpenWMSHandler.class);
    private ScheduledFuture<?> connectorTask;
    private OpenWMSDeviceConfiguration config;
    private OpenWMSBridgeHandler bridgeHandler;

    public OpenWMSHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        if (bridgeHandler != null) {
            if (command instanceof RefreshType) {
                logger.trace("Received unsupported Refresh command");
            } else {
                String t = getThing().getThingTypeUID().getId().toUpperCase();
                logger.debug("Thing: ", t);

                Map<String, String> msg = OpenWMSMessageFactory.createMessage(command.toString(), thing);
                // String tt = getThing().g.getThingTypeUID().getId().toUpperCase();
                // .convertPacketType(getThing().getThingTypeUID().getId().toUpperCase());

                // OpenWMSMessage msg = OpenWMSMessageFactory.createMessage(packetType);
                //
                // msg.setConfig(config);
                // msg.convertFromState(channelUID.getId(), command);

                for (Entry<String, String> entry : msg.entrySet()) {
                    System.out.println(entry.getValue());
                    bridgeHandler.sendMessage(entry.getValue());
                }

            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing thing {}", getThing().getUID());
        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(),
                (getBridge() == null) ? null : getBridge().getStatus());
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(), bridgeStatusInfo.getStatus());
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus bridgeStatus) {
        logger.debug("initializeBridge {} for thing {}", bridgeStatus, getThing().getUID());

        config = getConfigAs(OpenWMSDeviceConfiguration.class);

        // Umwandeln der Seriennummer in HEX => deviceId
        if (config.deviceId == null && config.serial != null) {
            Configuration conf = editConfiguration();
            config.deviceId = Diverses.stringToEndian(config.serial);
            conf.put("deviceId", config.deviceId);
            updateConfiguration(conf);
        }

        if (config.deviceId == null || config.panId == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "OpenWMS device missing deviceId or PANID");
        } else if (thingHandler != null && bridgeStatus != null) {
            bridgeHandler = (OpenWMSBridgeHandler) thingHandler;
            bridgeHandler.registerDeviceStatusListener(this);

            if (bridgeStatus == ThingStatus.ONLINE) {

                String timer = (String) thing.getConfiguration().getProperties().get("stateCheck");
                if (!(Integer.valueOf(timer) > 0)) {
                    timer = "60";
                }
                if (connectorTask == null || connectorTask.isCancelled()) {
                    connectorTask = scheduler.scheduleWithFixedDelay(() -> {
                        logger.debug("Checking OpenWMS BLIND connection, thing status = {}", thing.getStatus());
                        Map<String, String> msg = OpenWMSMessageFactory.createMessage("GETSTATUS", thing);
                        for (Entry<String, String> entry : msg.entrySet()) {
                            // System.out.println(entry.getValue());
                            bridgeHandler.sendMessage(entry.getValue());
                        }
                    }, 0, Integer.valueOf(timer), TimeUnit.SECONDS);
                }

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Thing {} disposed.", getThing().getUID());
        if (bridgeHandler != null) {
            bridgeHandler.unregisterDeviceStatusListener(this);
        }
        if (connectorTask != null && !connectorTask.isCancelled()) {
            connectorTask.cancel(true);
            connectorTask = null;
        }
        bridgeHandler = null;
        super.dispose();
    }

    @Override
    public void onDeviceMessageReceived(ThingUID bridge, OpenWMSGetResponse message) {
        try {
            String id = message.getDeviceId();
            if (config.deviceId.equals(id)) {
                // String receivedId = PACKET_TYPE_THING_TYPE_UID_MAP.get(message.getPacketType()).getId();
                String receivedId = message.getDeviceId();
                logger.debug("Received message from bridge: {} message: {}", bridge, message);

                if (receivedId.equals(getThing().getConfiguration().getProperties().get("deviceId"))) {
                    updateStatus(ThingStatus.ONLINE);

                    for (Channel channel : getThing().getChannels()) {
                        String channelId = channel.getUID().getId();
                        if (message.convertToState(channelId) != null) {
                            updateState(channelId, message.convertToState(channelId));
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred during message receiving", e);
        }
    }

}
