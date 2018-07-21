package org.openhab.binding.openwms.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.apache.commons.io.IOUtils;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.openwms.config.OpenWMSBridgeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/*
* @author zeezee - Initial contribution
*/
public class OpenWMSSerialConnector extends OpenWMSBaseConnector implements SerialPortEventListener {

    private final Logger logger = LoggerFactory.getLogger(OpenWMSSerialConnector.class);

    private OutputStream out;
    private SerialPort serialPort;

    private Thread readerThread;

    protected byte[] buffer;

    protected int bufferPos;

    @Override
    public void connect(OpenWMSBridgeConfiguration device) throws NoSuchPortException, PortInUseException,
            UnsupportedCommOperationException, IOException, TooManyListenersException {

        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(device.serialPort);
        CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

        serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(128000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        // serialPort.enableReceiveThreshold(1);
        // serialPort.enableReceiveTimeout(100); // In ms. Small values mean faster shutdown but more cpu usage.
        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

        in = serialPort.getInputStream();
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
        if (out == null) {
            throw new IOException("Not connected sending messages is not possible");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Send data (len={}): {}", data.length, HexUtils.bytesToHex(data));
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
