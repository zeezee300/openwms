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
package org.openhab.binding.openwms.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

//import jersey.repackaged.com.google.common.collect.ImmutableMap;

//import com.google.common.collect.ImmutableMap;
//import com.google.common.collect.ImmutableSet;

/**
 * The {@link OpenWMSBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 *
 *
 * @author zeezee - Initial contribution
 */
@NonNullByDefault
public class OpenWMSBindingConstants {

    public static final String BINDING_ID = "openwms";

    // List of all Bridge Type UIDs
    public static final String BRIDGE_TYPE_MANUAL_BRIDGE = "bridge";
    public static final String BRIDGE_TYPE_TCP_BRIDGE = "tcpbridge";

    public static final ThingTypeUID BRIDGE_MANUAL = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_MANUAL_BRIDGE);
    public static final ThingTypeUID BRIDGE_TCP = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_TCP_BRIDGE);

    /**
     * Presents all supported Bridge types by openwms binding.
     */
    // public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = ImmutableSet.of(BRIDGE_MANUAL,
    // BRIDGE_TCP);
    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(BRIDGE_MANUAL, BRIDGE_TCP).collect(Collectors.toSet()));

    /**
     * Presents all discoverable Bridge types by openwms binding.
     */
    // public static final Set<ThingTypeUID> DISCOVERABLE_BRIDGE_THING_TYPES_UIDS = ImmutableSet.of(BRIDGE_MANUAL,
    // BRIDGE_TCP);
    public static final Set<ThingTypeUID> DISCOVERABLE_BRIDGE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(BRIDGE_MANUAL, BRIDGE_TCP).collect(Collectors.toSet()));

    /**
     * Presents all discoverable Bridge types by openwms binding.
     */
    // public static final Set<ThingTypeUID> DISCOVERABLE_BRIDGE_THING_TYPES_UIDS = Collections.unmodifiableSet(
    // Stream.of(BRIDGE_RFXTRX443, BRIDGE_RFXTRX315, BRIDGE_RFXREC443).collect(Collectors.toSet()));

    // List of all Thing Type UIDs
    // private static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "sample");
    // private static final ThingTypeUID THING_TYPE_WIND = new ThingTypeUID(BINDING_ID, "wind");
    private static final ThingTypeUID THING_TYPE_BLIND = new ThingTypeUID(BINDING_ID, "blind");
    private static final ThingTypeUID THING_TYPE_WEATHER = new ThingTypeUID(BINDING_ID, "weather");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
    public static final String CHANNEL_SHUTTER = "shutter";
    public static final String CHANNEL_VALANCE1 = "valance1";
    public static final String CHANNEL_VALANCE2 = "valance2";
    public static final String CHANNEL_DIMMINGLEVEL = "dimminglevel";
    public static final String CHANNEL_BRIGHTNESS = "brightness";
    public static final String CHANNEL_DUSK = "dusk";
    public static final String CHANNEL_COMMAND_ID = "commandId";
    public static final String CHANNEL_COMMAND = "command";
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_MOTION = "motion";
    public static final String CHANNEL_CONTACT = "contact";
    public static final String CHANNEL_WINDSPEED = "windspeed";
    public static final String CHANNEL_RAIN = "rain";
    public static final String CHANNEL_TEMPERATURE = "temperature";

    // List of all Properties
    public static final String PROPERTY_DEVICEID = "deviceId";
    public static final String PROPERTY_PANID = "panId";
    public static final String PROPERTY_STATECHECK = "stateCheck";
    public static final String PROPERTY_CHANNEL = "channel";
    public static final String PROPERTY_NETWORKKEY = "Current WMS network key";
    public static final String PROPERTY_WIND = "wind";
    public static final String PROPERTY_SUN = "sun";
    public static final String PROPERTY_DUSK = "dusk";
    public static final String PROPERTY_RAIN = "rain";
    public static final String PROPERTY_OPMODE = "opmode";

    // public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_BLIND,
    // THING_TYPE_WEATHER);
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_BLIND, THING_TYPE_WEATHER).collect(Collectors.toSet()));

    /*
     * 02: Stick/Software
     * 06: Webcontrol
     * 20: Zwischenstecker - berücksichtigt
     * 25: Zwischenstecker - berücksichtigt
     * 63: Wetterstation - berücksichtigt
     */
    // public static final Map<String, ThingTypeUID> PACKET_TYPE_THING_TYPE_UID_MAP = ImmutableMap
    // .<String, ThingTypeUID> builder().put("20", OpenWMSBindingConstants.THING_TYPE_BLIND)
    // .put("25", OpenWMSBindingConstants.THING_TYPE_BLIND).put("63", OpenWMSBindingConstants.THING_TYPE_WEATHER)
    // .build();

    // public static final Map<String, ThingTypeUID> PACKET_TYPE_THING_TYPE_UID_MAP = ImmutableMap
    // .<String, ThingTypeUID> builder().put("20", OpenWMSBindingConstants.THING_TYPE_BLIND)
    // .put("25", OpenWMSBindingConstants.THING_TYPE_BLIND).put("63", OpenWMSBindingConstants.THING_TYPE_WEATHER)
    // .build();
    //
    public static final Map<String, ThingTypeUID> PACKET_TYPE_THING_TYPE_UID_MAP = Collections
            .unmodifiableMap(new HashMap<String, ThingTypeUID>() {
                {
                    put("20", OpenWMSBindingConstants.THING_TYPE_BLIND);
                    put("25", OpenWMSBindingConstants.THING_TYPE_BLIND);
                    put("63", OpenWMSBindingConstants.THING_TYPE_WEATHER);
                }
            });

}
