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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zeezee - Initial contribution
 */
public abstract class OpenWMSBaseConnector implements OpenWMSConnectorInterface {
    private final Logger logger = LoggerFactory.getLogger(OpenWMSBaseConnector.class);

    private List<OpenWMSEventListener> listeners = new ArrayList<>();
    protected InputStream in;

    @Override
    public synchronized void addEventListener(OpenWMSEventListener OpenWMSEventListener) {
        if (!listeners.contains(OpenWMSEventListener)) {
            listeners.add(OpenWMSEventListener);
        }
    }

    @Override
    public synchronized void removeEventListener(OpenWMSEventListener listener) {
        listeners.remove(listener);
    }

    // void sendMsgToListeners(byte[] msg) {
    // try {
    // for (OpenWMSEventListener listener : listeners) {
    // listener.packetReceived(msg);
    // }
    // } catch (Exception e) {
    // logger.error("Event listener invoking error", e);
    // }
    // }

    void sendStrToListeners(String msg) {
        try {
            for (OpenWMSEventListener listener : listeners) {
                listener.packetStrReceived(msg);
            }
        } catch (Exception e) {
            logger.error("Event listener invoking error", e);
        }
    }

    void sendErrorToListeners(String error) {
        try {
            for (OpenWMSEventListener listener : listeners) {
                listener.errorOccurred(error);
            }
        } catch (Exception e) {
            logger.error("Event listener invoking error", e);
        }
    }

    int read(byte[] buffer, int offset, int length) throws IOException {
        return in.read(buffer, offset, length);
    }
}
