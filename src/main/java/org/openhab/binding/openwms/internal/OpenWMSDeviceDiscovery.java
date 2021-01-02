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

package org.openhab.binding.openwms.internal;

import java.util.Set;

import org.openhab.binding.openwms.handler.OpenWMSBridgeHandler;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 *
 * @author zeezee - Initial contribution
 */

public class OpenWMSDeviceDiscovery extends AbstractDiscoveryService implements DeviceMessageListener {
    private final Logger logger = LoggerFactory.getLogger(OpenWMSDeviceDiscovery.class);

    private OpenWMSBridgeHandler bridgeHandler;

    public OpenWMSDeviceDiscovery(OpenWMSBridgeHandler openwmsBridgeHandler) {
        super(null, 1, false);
        this.bridgeHandler = openwmsBridgeHandler;
    }

    public void activate() {
        bridgeHandler.registerDeviceStatusListener(this);
    }

    @Override
    public void deactivate() {
        bridgeHandler.unregisterDeviceStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return OpenWMSBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS;
    }

    @Override
    protected void startScan() {
        // this can be ignored here as we discover devices from received messages

        // jetzt versuchen wir es doch einmal....
        logger.debug("Start scan");
        String messageString = "";
        // zuerst prüfen, ob ein Netzwerkschlüssel vorhanden ist
        String netId = bridgeHandler.getThing().getProperties().get(OpenWMSBindingConstants.PROPERTY_NETWORKKEY);
        String panId = bridgeHandler.getThing().getProperties().get(OpenWMSBindingConstants.PROPERTY_PANID);

        // wenn Netzwerkschlüsssel und PANID vorhanden sind, dann weiter, sonst wird gesucht,
        if (netId != null && !netId.isEmpty() && !panId.isEmpty() && !panId.equals("")) {
            logger.debug("Info: {}", "Scan - found Networkkey: " + netId);
            logger.debug("Info: {}", "Scan - found PANID: " + panId);
            messageString = "{K401" + netId + "}";
            logger.debug("Info: {}", "Scan - Transmitting message: " + messageString);
            bridgeHandler.sendMessage(messageString);
            int i = 17;
            messageString = "{M#" + Integer.toString(i) + panId + "}";
            logger.debug("Info: {}", "Scan - Transmitting message: " + messageString);
            bridgeHandler.sendMessage(messageString);
            messageString = "{R04FFFFFF5060" + panId + "021100}"; // chanel request
            logger.debug("Info: {}", "Scan - Transmitting message: " + messageString);
            bridgeHandler.sendMessage(messageString);

        } else {
            logger.debug("Info: {}", "Scan - found Networkkey: ");
            int i = 17;
            messageString = "{M%" + Integer.toString(i) + "FFFF}";
            bridgeHandler.sendMessage(messageString);
            messageString = "{R04FFFFFF5058}";
            bridgeHandler.sendMessage(messageString);

            // ToDo - suchen nach der PANID

        }
    }

    @Override
    public void onDeviceMessageReceived(ThingUID bridge, OpenWMSGetResponse message) {
        // logger.trace("Received: bridge: {} message: {}", bridge, message);
        if (!message.getPanId().toString().equals("") && message.getMsgTyp().equals("7020")) {
            String messageString = "{M%17" + message.getPanId().toString() + "}";
            bridgeHandler.sendMessage(messageString);

            // wenn noch kein Netzwerkschlüssel vorhanden ist, dann soll versucht werden diesen zu ermitteln
            String netId = bridgeHandler.getThing().getProperties().get(OpenWMSBindingConstants.PROPERTY_NETWORKKEY);
            if (netId != null && !netId.isEmpty()) {
                messageString = "{R04FFFFFF7020" + message.getPanId().toString() + "02}";
            } else {
                messageString = "{R01" + message.getDeviceId().toString() + "7021FFFF02}"; // scan für den
                                                                                           // Netzwerkschlüssel
            }
            bridgeHandler.sendMessage(messageString);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                logger.error("Interrupted Exception Error: {}", e.getMessage());
            }
        }
        String id = message.getDeviceId();
        ThingTypeUID uid = OpenWMSBindingConstants.PACKET_TYPE_THING_TYPE_UID_MAP.get(message.getDeviceTyp());

        if (uid != null) {
            ThingUID thingUID = new ThingUID(uid, bridge, id);
            // TODO prüfen, ob das Device bereits vorhanden ist (gibt es die Device-ID schon?)
            if (!bridgeHandler.getConfiguration().disableDiscovery) {
                logger.trace("Adding new OpenWMS Device {} with id '{}' to smarthome inbox", thingUID, id);
                DiscoveryResultBuilder discoveryResultBuilder = DiscoveryResultBuilder.create(thingUID)
                        .withBridge(bridge);

                message.addDevicePropertiesTo(discoveryResultBuilder);

                thingDiscovered(discoveryResultBuilder.build());
            } else {
                logger.trace("Ignoring OpenWMS {} with id '{}' - discovery disabled", thingUID, id);
            }
        }
    }
}
