package org.openhab.binding.openwms.internal.discovery;

import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryServiceCallback;
import org.eclipse.smarthome.config.discovery.ExtendedDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.openwms.config.OpenWMSBindingConstants;
import org.openhab.binding.openwms.handler.OpenWMSBridgeHandler;
import org.openhab.binding.openwms.internal.DeviceMessageListener;
import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 *
 * @author zeezee - Initial contribution
 */
public class OpenWMSDeviceDiscovery extends AbstractDiscoveryService
        implements ExtendedDiscoveryService, DeviceMessageListener {
    private final Logger logger = LoggerFactory.getLogger(OpenWMSDeviceDiscovery.class);

    private OpenWMSBridgeHandler bridgeHandler;
    private DiscoveryServiceCallback callback;

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

    }

    @Override
    public void setDiscoveryServiceCallback(DiscoveryServiceCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onDeviceMessageReceived(ThingUID bridge, OpenWMSGetResponse message) {
        logger.trace("Received: bridge: {} message: {}", bridge, message);
        if (!message.getPanId().toString().equals("") && message.getMsgTyp().equals("7020")) {
            // String messageString = "{M%17" + message.getPanId().toString() + "}";
            // bridgeHandler.sendMessage(messageString);
            String messageString = "{R04FFFFFF7020" + message.getPanId().toString() + "02}";
            bridgeHandler.sendMessage(messageString);
        }
        String id = message.getDeviceId();
        ThingTypeUID uid = OpenWMSBindingConstants.PACKET_TYPE_THING_TYPE_UID_MAP.get(message.getDeviceTyp());
        ThingUID thingUID = new ThingUID(uid, bridge, id);

        if (callback.getExistingThing(thingUID) == null) {
            if (!bridgeHandler.getConfiguration().disableDiscovery) {
                logger.trace("Adding new OpenWMS Device {} with id '{}' to smarthome inbox", thingUID, id);
                DiscoveryResultBuilder discoveryResultBuilder = DiscoveryResultBuilder.create(thingUID)
                        .withBridge(bridge);

                message.addDevicePropertiesTo(discoveryResultBuilder);

                thingDiscovered(discoveryResultBuilder.build());
            } else {
                logger.trace("Ignoring OpenWMS {} with id '{}' - discovery disabled", thingUID, id);
            }
        } else {
            logger.trace("Ignoring already known OpenWMS {} with id '{}'", thingUID, id);
        }
    }

}
