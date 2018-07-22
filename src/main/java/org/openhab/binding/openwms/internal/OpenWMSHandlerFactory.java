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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.openwms.config.OpenWMSBindingConstants;
import org.openhab.binding.openwms.handler.OpenWMSBridgeHandler;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.Sets;

/**
 * The {@link OpenWMSHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 *
 * @author zeezee - Initial contribution
 */
// @NonNullByDefault
// @Component(configurationPid = "binding.openwms", service = ThingHandlerFactory.class)
public class OpenWMSHandlerFactory extends BaseThingHandlerFactory {

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(
            OpenWMSBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS,
            OpenWMSBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    // protected @Nullable ThingHandler createHandler(Thing thing) {
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (OpenWMSBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            OpenWMSBridgeHandler handler = new OpenWMSBridgeHandler((Bridge) thing);
            registerDeviceDiscoveryService(handler);
            return handler;
        } else if (supportsThingType(thingTypeUID)) {
            return new OpenWMSHandler(thing);
        }

        // if (THING_TYPE_SAMPLE.equals(thingTypeUID)) {
        // return new OpenWMSHandler(thing);
        // }

        return null;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (this.discoveryServiceRegs != null) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private void registerDeviceDiscoveryService(OpenWMSBridgeHandler handler) {
        OpenWMSDeviceDiscovery discoveryService = new OpenWMSDeviceDiscovery(handler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(handler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

}
