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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.openwms.config.OpenWMSBindingConstants;
import org.openhab.binding.openwms.handler.OpenWMSBridgeHandler;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

//import com.google.common.collect.Sets;

/**
 * The {@link OpenWMSHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 *
 * @author zeezee - Initial contribution
 */
@NonNullByDefault
// @Component(service = ThingHandlerFactory.class, configurationPid = "binding.openwms")
@Component(configurationPid = "binding.openwms", service = ThingHandlerFactory.class)
public class OpenWMSHandlerFactory extends BaseThingHandlerFactory {

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    // private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(
    // OpenWMSBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS,
    // OpenWMSBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream
            .concat(OpenWMSBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS.stream(),
                    OpenWMSBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS.stream())
            .collect(Collectors.toSet());

    private @NonNullByDefault({}) SerialPortManager serialPortManager;

    @Reference
    protected void setSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    protected void unsetSerialPortManager(final SerialPortManager serialPortManager) {
        this.serialPortManager = null;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        // protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (OpenWMSBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            OpenWMSBridgeHandler handler = new OpenWMSBridgeHandler((Bridge) thing, serialPortManager);
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
