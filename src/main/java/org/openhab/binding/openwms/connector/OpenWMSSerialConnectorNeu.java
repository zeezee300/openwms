package org.openhab.binding.openwms.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.TooManyListenersException;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.util.HexUtils;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.openwms.config.OpenWMSBindingConstants;
import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* @author zeezee - Initial contribution
*/
public class OpenWMSSerialConnectorNeu extends OpenWMSBaseConnector implements SerialPortEventListener {

    private final Logger logger = LoggerFactory.getLogger(OpenWMSSerialConnectorNeu.class);

    private OutputStream out;
    private SerialPort serialPort;

    private Thread readerThread;

    private SerialPortManager serialPortManager;

    public OpenWMSSerialConnectorNeu(SerialPortManager serialPortManager) {
        super();
        this.serialPortManager = serialPortManager;
    }

    // protected byte[] buffer;

    // protected int bufferPos;

    @Override
    public void connect(OpenWMSBridgeConfiguration device)
            throws PortInUseException, UnsupportedCommOperationException, IOException {

        logger.info("Connecting to OpenWMS USB at {}", device.serialPort);
        logger.debug("Serial port #### Jetzt gehts los");

        SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(device.serialPort.toString());
        if (portIdentifier == null) {
            logger.debug("No serial port {}", device.serialPort);
            throw new IOException("Could not find a gateway on given path '" + device.serialPort + "', "
                    + serialPortManager.getIdentifiers().count() + " ports available.");
        }

        logger.debug("Serial port #### Schritt 1 - open Port");
        serialPort = portIdentifier.open(OpenWMSBindingConstants.BINDING_ID, 2000);
        /// SerialPort commPort = portIdentifier.open(this.getClass().getName(), 5000);
        // CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000); // timeout 2 s.

        logger.debug("Serial port #### Schritt 2");
        /// serialPort = commPort;

        logger.debug("Serial port #### Schritt 3 230400");
        serialPort.setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        logger.debug("Serial port #### Schritt 4");
        serialPort.enableReceiveThreshold(1);
        logger.debug("Serial port #### Schritt 5");
        serialPort.enableReceiveTimeout(2000); // In ms. Small values mean faster shutdown but more cpu usage.
        // logger.debug("Serial port #### Schritt 4");
        // serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        // serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT | SerialPort.FLOWCONTROL_RTSCTS_IN);

        logger.debug("Serial port #### Schritt 6");
        in = serialPort.getInputStream();

        logger.debug("Serial port #### Schritt 7");
        out = serialPort.getOutputStream();
        out.flush();
        if (in.markSupported()) {
            in.reset();
        }

        // RXTX serial port library causes high CPU load
        // Start event listener, which will just sleep and slow down event loop
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            logger.debug("Serial port event listener started");
        } catch (TooManyListenersException e) {
        }

        readerThread = new OpenWMSStreamReader(this);
        readerThread.start();

    }

    @Override
    public void disconnect() {
        logger.debug("Disconnecting");

        if (serialPort != null) {
            serialPort.removeEventListener();
            logger.debug("Serial port event listener stopped");
        }

        if (readerThread != null) {
            logger.debug("Interrupt serial listener");
            readerThread.interrupt();
            try {
                readerThread.join();
            } catch (InterruptedException e) {
            }
        }

        if (out != null) {
            logger.debug("Close serial out stream");
            IOUtils.closeQuietly(out);
        }
        if (in != null) {
            logger.debug("Close serial in stream");
            IOUtils.closeQuietly(in);
        }

        if (serialPort != null) {
            logger.debug("Close serial port");
            serialPort.close();
        }

        readerThread = null;
        serialPort = null;
        out = null;
        in = null;

        logger.debug("Closed");
    }

    @Override
    public void sendMessage(byte[] data) throws IOException {
        String str = new String(data, StandardCharsets.UTF_8);
        if (out == null) {
            throw new IOException("Not connected sending messages is not possible");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Send data (data={}, len={} Hex={} data={})", str, data.length, HexUtils.bytesToHex(data),
                    data);
        }

        out.write(data);
        out.flush();
    }

    @Override
    public void serialEvent(SerialPortEvent arg0) {
        try {
            /*
             * See more details from
             * https://github.com/NeuronRobotics/nrjavaserial/issues/22
             */
            logger.trace("RXTX library CPU load workaround, sleep forever");
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignore) {
        }

    }

}
