package org.openhab.binding.openwms.messages;

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.smarthome.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* @author zeezee - Initial contribution
*/
public class OpenWMSMessageFactory {

    // @SuppressWarnings("serial")
    // private static final Map<String, Class<? extends OpenWMSMessage>> MESSAGE_CLASSES = Collections
    // .unmodifiableMap(new HashMap<String, Class<? extends OpenWMSMessage>>() {
    // {
    // put("8011", OpenWMSBlindMessage.class);
    //
    //
    // }
    // });

    private static Logger logger = LoggerFactory.getLogger(OpenWMSMessageFactory.class);
    public static final String CHECK = "{G}";
    public static final String VERSION = "{V}";

    public static Map<String, String> decode(String paket) {
        String paket_type = "";
        String message = "";
        Map<String, String> message_payload = new Hashtable();
        if (!paket.equals("")) {

            paket = paket.replaceAll("[{}]", "");
            // System.out.println(paket.substring(0, 1));
            switch (paket.substring(0, 1)) {
                case "g":

                    break;

                case "r": // receive Data
                    paket_type = "message";
                    message = paket.substring(1);
                    break;

            }

            if (paket_type == "message") {
                String type = message.substring(6, 10);
                String payload = message.substring(10);
                // Map<String, String> message_payload = new Hashtable();

                switch (type) {
                    case "5018":
                        StringBuilder input1 = new StringBuilder();
                        String input = payload.substring(4, 36);
                        input1.append(input);
                        input1 = input1.reverse();
                        message_payload.put("type", "joinNetworkRequest");
                        message_payload.put("panId", payload.substring(0, 4));
                        message_payload.put("networkKey", input1.toString());
                        message_payload.put("unknown", payload.substring(36, 38));
                        message_payload.put("channel", parseInt(payload.substring(38, 40), 16).toString());
                        break;

                    case "7021":
                        message_payload.put("type", "scanResponse");
                        message_payload.put("panId", payload.substring(0, 4));
                        message_payload.put("deviceType", payload.substring(4, 6)); // 63: wetterstation, 06:
                                                                                    // webcontrol,
                                                                                    // 02: stick/software, 20:
                                                                                    // zwischenstecker
                        message_payload.put("unknown", payload.substring(6));
                        break;

                    case "7071":

                        message_payload.put("type", "controlResponse");
                        message_payload.put("deviceId", message.substring(0, 6));

                        break;

                    case "8011":
                        message_payload.put("type", "parameterGetResponse");
                        message_payload.put("deviceId", message.substring(0, 6));
                        switch (payload.substring(0, 8)) {
                            case "01000003": // position
                            case "01000005": // position
                                message_payload.put("Position",
                                        String.valueOf(Integer.parseInt(payload.substring(8, 10), 16) / 2));
                                message_payload.put("Angle",
                                        String.valueOf(Integer.parseInt(payload.substring(10, 12), 16) - 127));
                                message_payload.put("Valance_1", payload.substring(12, 14));
                                message_payload.put("Valance_2", payload.substring(14, 16));

                        }
                        message_payload.put("art", "Position");

                        break;

                }

                for (String key : message_payload.keySet()) {
                    System.out.println(key + " - " + message_payload.get(key));
                }
                System.out.println();
            }
        }
        return message_payload;
    }

    private static Object parseInt(String substring, int i) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * mit createMessage werden die Kommandos erzeugt, die an den USB Stickgesendet werden
     * Es soll sichergestellt werden, dass verschiedene Kanäle und PANIDs verwendet werden können, daher werden jeweils
     * zwei Messages erzeugt:
     * 1. setzen von Kanal und PANID (Daten aus dem Thingitem)
     * 2. der eigentliche Sendebefehl
     * -> Command (aus dem openhab Item)
     * -> Thing (Daten des jeweiligen Thing)
     * <- MAP(Zähler für Messages, Message)
     */
    public static Map<String, String> createMessage(String command, Thing thing) {
        String ret = "";
        Map<String, String> messagesToSend = new Hashtable();
        String packetType = thing.getThingTypeUID().getId().toUpperCase();
        Map<String, Object> cfg = null;
        String pos = "";

        switch (packetType) {
            case "BLIND":
                cfg = thing.getConfiguration().getProperties();
                String channel = (String) cfg.get("channel");
                String panID = (String) cfg.get("panID");
                String dest = (String) cfg.get("deviceId");

                // zuerst muß der Channel und die panID gesetzt werden
                ret = setzenPANID(channel, panID);
                messagesToSend.put("1", ret);

                // dann kann der eigentlich Befehl gesetzt werden
                if (command.equals("ON") || command.equals("UP")) {
                    pos = "0";
                    ret = sendePOSITION(dest, pos);
                } else if (command.equals("OFF") || command.equals("DOWN")) {
                    pos = "100";
                    ret = sendePOSITION(dest, pos);
                } else if (command.equals("STOP")) {
                    ret = "{R06" + dest + "7070" + "01" + "FF" + "FF" + "FFFF00}";
                } else if (command.equals("WINKEN")) {
                    ret = sendeWINKEN(dest);
                } else if (command.equals("GETSTATUS")) {
                    ret = sendeGETSTATUS(dest);
                } else if (Integer.valueOf(command) > 0 && Integer.valueOf(command) <= 100) {
                    pos = command;
                    ret = sendePOSITION(dest, pos);
                } else {
                    pos = "0";
                    ret = sendePOSITION(dest, pos);
                }

                messagesToSend.put("2", ret);

                break;

        }
        Map<String, String> sortedMessages = new TreeMap<>(messagesToSend);
        return sortedMessages;
    }

    private static String sendeWINKEN(String dest) {
        String ret = "{R06" + dest + "7050" + "}";
        return ret;
    }

    private static String sendeACK(String dest) {
        String ret = "{R21" + dest + "50AC" + "}";
        return ret;
    }

    private static String sendeGETSTATUS(String dest) {
        String ret = "{R06" + dest + "801001000005" + "}";
        return ret;
    }

    private static String sendePOSITION(String dest, String position) {
        String pos = Integer.toHexString(Math.min(Math.max(Integer.valueOf(position), 0), 100) * 2).toUpperCase();
        pos = ("00" + pos).substring(pos.length()); // mit führenden Nullen auf 2 Stelle auffüllen
        String ret = "{R06" + dest + "7070" + "03" + pos

        // + ("0" + (Math.min(Math.max(parameter.angle, 0), 90) +
        // 127).toString(16)).substr(-2).toUpperCase()
                + "FF" // angle
                + "FFFF00}"; // no idea how valance works
        return ret;
    }

    private static String setzenPANID(String channel, String panID) {
        String ret = "{M%" + channel + panID + "}";
        return ret;
    }

    public static OpenWMSMessage createMessage(String packet) {
        String packetType = packet.substring(7, 11);

        // try {
        //
        // Class<? extends OpenWMSMessage> cl = (Class<? extends OpenWMSMessage>) OpenWMSBlindMessage.class;
        // // Class<? extends OpenWMSMessage> cl = MESSAGE_CLASSES.get(packetType);
        // if (cl == null) {
        // logger.debug("Message " + packetType + " not implemented");
        // }
        // Constructor<?> c = cl.getConstructor(byte[].class);
        // return (OpenWMSMessage) c.newInstance(packet);
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        return null;

        // try {
        // Class<? extends OpenWMSMessage> cl = MESSAGE_CLASSES.get(packetType);
        // if (cl == null) {
        // throw new RFXComMessageNotImplementedException("Message " + packetType + " not implemented");
        // }
        // Constructor<?> c = cl.getConstructor(byte[].class);
        // return (RFXComMessage) c.newInstance(packet);
        // } catch (InvocationTargetException e) {
        // if (e.getCause() instanceof RFXComException) {
        // throw (RFXComException) e.getCause();
        // } else {
        // throw new RFXComException(e);
        // }
        // } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
        // throw new RFXComException(e);
        // }
    }

    // public static PacketType convertPacketType(String packetType) throws IllegalArgumentException {
    //
    // for (PacketType p : PacketType.values()) {
    // if (p.toString().replace("_", "").equals(packetType.replace("_", ""))) {
    // return p;
    // }
    // }
    //
    // throw new IllegalArgumentException("Unknown packet type " + packetType);
    // }

}
