package org.openhab.binding.openwms.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortException;

public class OpenWMSSerialWorkaround {
    private static SerialPort serialPort = null;
    private final static Logger logger = LoggerFactory.getLogger(OpenWMSSerialConnector.class);

    public static void TestSerial(String port) throws SerialPortException {
        logger.debug("Workaround ## start Port: {}", port);

        serialPort = new SerialPort(port);
        final int MAX_CONNECTION = 10;
        int reconnections = 0;

        boolean scanning = true;
        while (scanning && reconnections < MAX_CONNECTION) { // 10x versuchen den Port zu öffnen, danach weiter
            {
                reconnections++;
                try {
                    System.out.println("Versuch " + reconnections);
                    logger.debug("Workaround ## Versuch {}", reconnections);
                    // serialPort.openPort();
                    // logger.debug("Workaround #### Pkt 1");
                    serialPort.setParams(SerialPort.BAUDRATE_128000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                            SerialPort.PARITY_NONE);
                    // logger.debug("Workaround #### Pkt 2");
                    // serialPort.writeString("{G}");

                    // serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
                    // logger.debug("Workaround #### Pkt 3 {} ", scanning);
                    scanning = false;
                    // logger.debug("Workaround #### Pkt 4 {} ", scanning);
                } catch (Exception e) {
                    System.out.println("Connect failed, waiting and trying again. Versuch " + reconnections);
                    e.printStackTrace();
                    try {
                        Thread.sleep(2000);// 2 seconds
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

        }
        // serialPort.closePort();
        serialPort = null;

        logger.debug("Workaround ## ende");
    }

}
