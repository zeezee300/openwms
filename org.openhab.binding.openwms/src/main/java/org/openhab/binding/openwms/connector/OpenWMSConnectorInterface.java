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
package org.openhab.binding.openwms.connector;

import java.io.IOException;

import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;

/**
 * @author zeezee - Initial contribution
 */

public interface OpenWMSConnectorInterface {

    /**
     * Procedure for connecting to OpenWMS controller.
     *
     * @param device
     *            Controller connection parameters (e.g. serial port name or IP
     *            address).
     */
    public void connect(OpenWMSBridgeConfiguration device) throws Exception;

    /**
     * Procedure for disconnecting to OpenWMS controller.
     *
     */
    public void disconnect();

    /**
     * Procedure for send raw data to OpenWMS controller.
     *
     * @param data
     *            raw bytes.
     */
    public void sendMessage(byte[] data) throws IOException;

    /**
     * Procedure for register event listener.
     *
     * @param listener
     *            Event listener instance to handle events.
     */
    public void addEventListener(OpenWMSEventListener listener);

    /**
     * Procedure for remove event listener.
     *
     * @param listener
     *            Event listener instance to remove.
     */
    public void removeEventListener(OpenWMSEventListener listener);
}
