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

import org.openhab.binding.openwms.messages.OpenWMSGetResponse;
import org.openhab.core.thing.ThingUID;

/**
 *
 * @author zeezee - Initial contribution
 */

public interface DeviceMessageListener {

    /**
     * This method is called whenever the message is received from the bridge.
     *
     * @param bridge
     *            The OpenWMS bridge where message is received.
     * @param message
     *            The message which received.
     */
    void onDeviceMessageReceived(ThingUID bridge, OpenWMSGetResponse message);
}
