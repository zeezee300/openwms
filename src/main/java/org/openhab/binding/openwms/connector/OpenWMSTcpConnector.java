package org.openhab.binding.openwms.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenWMS USB connector for TCP/IP communication.
 *
 *
 */
public class OpenWMSTcpConnector extends OpenWMSBaseConnector {
    private final Logger logger = LoggerFactory.getLogger(OpenWMSTcpConnector.class);

    private OutputStream out;
    private Socket socket;

    private Thread readerThread;

    @Override
    public void connect(OpenWMSBridgeConfiguration device) throws IOException {
        final int MAX_CONNECTION = 10;
        int reconnections = 0;
        boolean scanning = true;

        logger.info("Connecting to OpenWMS USB at {}:{} over TCP/IP", device.host, device.port);
        try {
            String ipAddress = device.host;
            InetAddress inet = InetAddress.getByName(ipAddress);
            System.out.println("Sending Ping Request to " + ipAddress);
            if (inet.isReachable(5000)) {
                System.out.println(ipAddress + " is reachable.");
            } else {
                System.out.println(ipAddress + " NOT reachable.");
            }
        } catch (Exception e) {
            System.out.println("Exception:" + e.getMessage());
        }

        while (scanning && reconnections < MAX_CONNECTION) {
            reconnections++;
            logger.info("Try open Socket: {}", reconnections);
            try {
                socket = new Socket(device.host, device.port);
                socket.setSoTimeout(2000); // In ms. Small values mean faster shutdown but more cpu usage.

                // socket..setKeepAlive(true);
                // socket.setReuseAddress(true);
                // socket.setTcpNoDelay(true);

                in = socket.getInputStream();
                out = socket.getOutputStream();

                out.flush();
                if (in.markSupported()) {
                    in.reset();
                }
                scanning = false;
            } catch (UnknownHostException e) {
                System.out.println("Unknown Host...");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("IOProbleme...");
                e.printStackTrace();
            }
        }
        readerThread = new OpenWMSStreamReader(this);
        readerThread.start();
    }

    @Override
    public void disconnect() {
        logger.debug("Disconnecting");

        if (readerThread != null) {
            logger.debug("Interrupt stream listener");
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
            }
        }

        if (out != null) {
            logger.debug("Close tcp out stream");
            IOUtils.closeQuietly(out);

        }
        if (in != null) {
            logger.debug("Close tcp in stream");
            IOUtils.closeQuietly(in);
        }

        if (socket != null) {
            logger.debug("Close socket");
            IOUtils.closeQuietly(socket);
        }

        readerThread = null;
        socket = null;
        out = null;
        in = null;

        logger.debug("Closed");
    }

    @Override
    public void sendMessage(byte[] data) throws IOException {
        String str = new String(data, StandardCharsets.UTF_8);
        if (logger.isTraceEnabled()) {
            logger.trace("Send data (len={}): {}", data.length, HexUtils.bytesToHex(data));
        }
        out.write(data);
        out.flush();

    }

    @Override
    int read(byte[] buffer, int offset, int length) throws IOException {
        try {
            return super.read(buffer, offset, length);
        } catch (SocketTimeoutException ignore) {
            // ignore this exception, instead return 0 to behave like the serial read
            return 0;
        }
    }
}
