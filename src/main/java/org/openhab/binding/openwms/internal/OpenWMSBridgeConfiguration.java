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

/**
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
