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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zeezee - Initial contribution
 */
public class OpenWMSStreamReader extends Thread {

    private final Logger logger = LoggerFactory.getLogger(OpenWMSStreamReader.class);
    private static final int MAX_READ_TIMEOUTS = 10;

    private OpenWMSBaseConnector connector;

    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            logger.error("Connector died: ", throwable);
        }
    }

    public OpenWMSStreamReader(OpenWMSBaseConnector connector) {
        this.connector = connector;
        setUncaughtExceptionHandler(new ExceptionHandler());
    }

    @Override
    public void run() {
        logger.debug("Data listener started");
        byte[] buf = new byte[Byte.MAX_VALUE];

        // The stream has (or SHOULD have) a read timeout set. Taking a
        // read timeout (read returns 0) between packets gives us a chance
        // to check if we've been interrupted. Read interrupts during a
        // packet are ignored but if too many timeouts occur we take it as
        // meaning the RFXCOM has become missing presumed dead.

        try {
            while (!Thread.interrupted()) {

                int bytesRead = connector.read(buf, 0, 1);
                int packetLength = buf[0];

                // first wait for Start Message '{'
                if (bytesRead > 0 && buf[0] == '{') {
                    processMessage(buf, packetLength);
                    // connector.sendMsgToListeners(Arrays.copyOfRange(buf, 0, packetLength + 1));
                    //// connector.sendMsgToListeners(Arrays.copyOfRange(buf, 0, buf.length));
                    // System.out.println("Empfange: ############## " + new String(buf, 0, packetLength + 1));

                }
            }
        } catch (IOException e) {
            logger.debug("Received exception, will report it to listeners", e);
            connector.sendErrorToListeners(e.getMessage());
        }

        logger.debug("Data listener stopped");
    }

    private void processMessage(byte[] buf, int packetLength) throws IOException {

        int bufferIndex = 0;
        int readTimeoutCount = 1;
        int bytesRead = 0;
        String b = "";
        String retdata = "";
        StringBuilder message = new StringBuilder();
        b = new String(buf, 0, 1); // Beginn der Message '{'
        message.append(b);

        while (!(b.equals("}"))) { // solange bis Ende der Message => '}'

            bytesRead = connector.read(buf, bufferIndex, 1);

            b = new String(buf, bufferIndex, bytesRead);
            message.append(b);
            // }

            if (bytesRead > 0) {
                bufferIndex += bytesRead;
                readTimeoutCount = 1;
            } else if (readTimeoutCount++ == MAX_READ_TIMEOUTS) {
                throw new IOException("Timeout during packet read");

            }
        }
        retdata = message.toString();
        message.setLength(0);
        connector.sendStrToListeners(retdata);
        retdata = "";
    }
}
