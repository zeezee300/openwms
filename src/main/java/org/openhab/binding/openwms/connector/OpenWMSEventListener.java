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

/**
 * @author zeezee - Initial contribution
 */

public interface OpenWMSEventListener {

    /**
     * Procedure for receive raw data from controller.
     *
     * @param data
     *            Received raw data.
     */
    // void packetReceived(byte[] data);

    void packetStrReceived(String data);

    /**
     * Procedure for receiving information fatal error.
     *
     * @param error
     *            Error occurred.
     */
    void errorOccurred(String error);
}
