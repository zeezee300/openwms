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
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.types.State;

/**
 * @author zeezee - Initial contribution
 */

public interface OpenWMSDeviceMessage<T> extends OpenWMSMessage {

    /**
     * Procedure for converting OpenWMS value to openHAB state.
     *
     * @param channelId id of the channel
     * @return openHAB state.
     *
     */
    State convertToState(String channelId);

    /**
     * Procedure to get device id.
     *
     * @return device Id.
     */
    String getDeviceId();

    /**
     * Get the packet type for this device message
     *
     * @return the message its packet type
     */
    // OpenWMSBaseMessage.PacketType getPacketType();

    /**
     * Given a DiscoveryResultBuilder add any new properties to the builder for the given message
     *
     * @param discoveryResultBuilder existing builder containing some early details
     *
     */
    void addDevicePropertiesTo(DiscoveryResultBuilder discoveryResultBuilder);

    /**
     * Procedure for converting sub type as string to sub type object.
     *
     * @param subType
     * @return sub type object.
     *
     */
    // T convertSubType(String subType);

    /**
     * Procedure to set sub type.
     *
     * @param subType
     */
    // void setSubType(T subType);

    /**
     * Procedure to set device id.
     *
     * @param deviceId
     *
     */
    void setDeviceId(String deviceId);

    /**
     * Set the config to be applied to this message
     *
     * @param config
     *
     */
    @Override
    void setConfig(OpenWMSDeviceConfiguration config);
}
