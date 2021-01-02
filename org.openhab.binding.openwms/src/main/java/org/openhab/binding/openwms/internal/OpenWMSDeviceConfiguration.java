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
 *
 *
 *
 * @author zeezee - Initial contribution
 */

public class OpenWMSDeviceConfiguration {
    //
    // public static final String DEVICE_ID_LABEL = "deviceId";
    // public static final String SERIAL = "serial";
    // public static final String CHANNEL = "channel";
    // public static final String PANID = "panId";
    // public static final String STATECHECK = "stateCheck";
    // public static final String ON_COMMAND_ID_LABEL = "onCommandId";
    // public static final String OFF_COMMAND_ID_LABEL = "offCommandId";

    public String deviceId;
    public String serial;
    public String channel;
    public String panId;
    public Boolean ignoreConfig;
    public Integer onCommandId;
    public Integer offCommandId;
}
