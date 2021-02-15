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

package org.openhab.binding.openwms.messages;

import org.openhab.binding.openwms.internal.OpenWMSDeviceConfiguration;
import org.openhab.core.types.Type;

/**
 * @author zeezee - Initial contribution
 */

public interface OpenWMSMessage {

    /**
     * Procedure for encode raw data.
     *
     * @param data
     *            Raw data.
     */
    void encodeMessage(byte[] data);

    /**
     * Procedure for decode object to raw data.
     *
     * @return raw data.
     */
    byte[] decodeMessage();

    /**
     * Procedure for converting openHAB state to WMS object.
     *
     */
    void convertFromState(String channelId, Type type);

    /**
     * Procedure to pass configuration to a message
     *
     * @param deviceConfiguration configuration about the device
     *
     */
    void setConfig(OpenWMSDeviceConfiguration deviceConfiguration);
}
