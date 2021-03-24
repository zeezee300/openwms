package org.openhab.binding.openwms.messages;

import static org.openhab.binding.openwms.config.OpenWMSBindingConstants.*;

import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.openwms.internal.OpenWMSDeviceConfiguration;

/*
* @author zeezee - Initial contribution
*/
public class OpenWMSGetResponse {

    public String paketTyp;
    public String msgTyp; // 8011
    // public String panId;
    public String deviceId;
    public String position;
    public String angle;
    public String valance1;
    public String valance2;
    public String panId = "";
    public String deviceTyp;
    public String windspeed;
    public String rain = ""; // ON - OFF
    public String temp;
    public String brightness;
    public String dusk;
    public String channel;
    public String networkid;
    public String wms_response;

    public Commands command;

    public enum Commands {
        OFF(0),
        ON(1),
        OPEN(0),
        CLOSE(1),
        STOP(2),
        CHANGE_DIRECTON(7);

        private final int command;

        Commands(int command) {
            this.command = command;
        }

    }

    public OpenWMSGetResponse(String data) {
        // noch sind die Daten in raw-Form ->
        // Antwort Status {rE49D0880110100000520FFFFFF00}
        // Antwort Fahrbefehl {rE49D0870710010023F023EFFFFFF0CFFFFFF}
        // Scan {rE18F0670204DE402}
        // Antwort Scan {rE49D0870214DE4258C2F000300000000000000000304000100C1000000000000}
        // Wetter response {rAAAAAA708000WWL1FFFFFFL2FFRRTTFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF}
        // Netzwerkschlüssel {rAAAAAA5018PPPP0123456789ABCDEFFEDCBA9876543210FF11}
        // Netzwerkschlüssel II {rxxxxxx5059kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk}

        setPaketTyp(data);
        String payload = "0";
        if (paketTyp.equals("r")) {
            setDeviceId(data.substring(2, 8));
            setMsgTyp(data.substring(8, 12));
            switch (msgTyp) {
                case "5018": // Join Network request
                    payload = data.substring(12);
                    String s = payload.substring(4, 36);
                    setPanId(data);
                    setNetworkID(s);
                    setChannel(payload.substring(38, 40));
                    setWmsResponse("{R04FFFFFF7020" + panId + "02}");

                    break;
                case "5060": // Switch Channel request
                    payload = data.substring(12);
                    setPanId(data);
                    setChannel(String.valueOf(Integer.parseInt(payload.substring(6, 8), 16)));
                    setDeviceTyp(data);
                    setWmsResponse(OpenWMSMessageFactory.setzenPANID(channel, "FFFF"));

                    break;
                case "5059": // Network Key
                    payload = data.substring(12);
                    String t = payload.substring(0, 32);
                    setNetworkID(t);

                    break;

                case "7020": // Scan request
                    payload = data.substring(12);
                    setPanId(data);
                    setDeviceTyp(data);
                    // setWmsResponse(OpenWMSMessageFactory.sendeSCANRESPONSE(deviceId, "FFFF", deviceTyp));

                    break;
                case "7021": // Scan response
                    payload = data.substring(12);
                    setPanId(data);
                    setDeviceTyp(data);
                    break;
                case "7080": // Wetter
                    payload = data.substring(12);
                    setWindspeed(String.valueOf(Integer.parseInt(payload.substring(2, 4), 16))); // WW 00-25 m/s
                    setBrightness(payload.substring(4, 6));
                    setDusk(payload.substring(12, 14));
                    setRain(payload.substring(16, 18)); // RR 00: No Rain, C8: Rain
                    setTemp(payload.substring(18, 20)); // TT -20 bis +60

                    break;
                case "8011":
                    payload = data.substring(20);
                    switch (data.substring(12, 20)) {
                        case "01000003": // position WMS Motor
                            // TODO
                            setPosition(String.valueOf(Integer.parseInt(payload.substring(0, 2), 16) / 2));
                            setValance1(String.valueOf(Integer.parseInt(payload.substring(4, 6), 16) / 2));
                            setValance2(String.valueOf(Integer.parseInt(payload.substring(6, 8), 16) / 2));
                            break;
                        case "01000005": // position WMS Motor
                            setPosition(String.valueOf(Integer.parseInt(payload.substring(0, 2), 16) / 2));
                            setValance1(String.valueOf(Integer.parseInt(payload.substring(4, 6), 16) / 2));
                            setValance2(String.valueOf(Integer.parseInt(payload.substring(6, 8), 16) / 2));
                            break;
                        case "26000046": // Current clock timer settings
                            // TODO
                            break;
                        case "0C000006": // Current limits
                            // TODO
                            setWindspeed(String.valueOf(Integer.parseInt(payload.substring(0, 2), 16))); // WW 00-25 m/s
                            setBrightness(payload.substring(4, 6));
                            setDusk(payload.substring(6, 8));
                            break;
                    }

                    break;
            }

        }
    }

    public String getPaketTyp() {
        return paketTyp;
    }

    public void setPaketTyp(String paketTyp) {
        String paket = paketTyp.replaceAll("[{}]", "");
        paket = paket.substring(0, 1);
        this.paketTyp = paket;
    }

    public void setMsgTyp(String msgTyp) {
        this.msgTyp = msgTyp;
    }

    public void setWmsResponse(String wms_response) {
        this.wms_response = wms_response;
    }

    public String getMsgTyp() {
        return msgTyp;
    }

    public String getPosition() {
        return position;
    }

    public void setDeviceTyp(String deviceTyp) {
        String paket = deviceTyp.replaceAll("[{}]", "");
        paket = paket.substring(15, 17);
        this.deviceTyp = paket;
    }

    public String getDeviceTyp() {
        return deviceTyp;
    }

    public void setPosition(String position) {
        if (!position.toUpperCase().equals("FF"))
            this.position = position;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setPanId(String panId) {
        String paket = panId.replaceAll("[{}]", "");
        paket = paket.substring(11, 15);
        this.panId = paket;
    }

    public String getPanId() {
        return panId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAngle() {
        return angle;
    }

    public void setAngle(String angle) {
        this.angle = angle;
    }

    public String getValance1() {
        return valance1;
    }

    public void setValance1(String valance1) {
        if (!valance1.toUpperCase().equals("FF"))
            this.valance1 = valance1;
    }

    public String getValance2() {
        return valance2;
    }

    public void setValance2(String valance2) {
        if (!valance2.toUpperCase().equals("FF"))
            this.valance2 = valance2;
    }

    public String getWindspeed() {
        return windspeed;
    }

    public void setWindspeed(String windspeed) {
        // int i = Integer.parseInt(windspeed);
        // if (i > 0) {
        // i = i + 4;
        // }
        // this.windspeed = String.valueOf(i);
        this.windspeed = windspeed;
    }

    public String getBrightness() {
        return brightness;
    }

    public void setBrightness(String brightness) {
        // int i = Integer.parseInt(brightness);
        // i = (i * 5);
        // if (i > 0) {
        // i = i + 5;
        // }
        brightness = String.valueOf(Integer.parseInt(brightness, 16) * 2);
        this.brightness = String.valueOf(brightness);
    }

    public String getDusk() {
        return dusk;
    }

    public void setDusk(String s) {
        String dusk;
        dusk = String.valueOf(Integer.parseInt(s, 16) * 2);
        this.dusk = dusk;
    }

    public void setRain(String rain) {
        if (rain.equals("00")) {
            this.rain = "OFF";
        }
        if (rain.equals("C8")) {
            this.rain = "ON";
        }

    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public void setNetworkID(String networkid) {
        String s = networkid;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= s.length() - 2; i = i + 2) {
            result.append(new StringBuilder(s.substring(i, i + 2)).reverse());
        }
        result = result.reverse();
        this.networkid = result.toString();
    }

    public void setTemp(String temp) {
        // setTemp(String.valueOf(Integer.parseInt(payload.substring(18, 20), 16) / 2 - 35));
        double val = Integer.parseInt(temp, 16) / 2 - 35;
        this.temp = String.valueOf(val);
    }

    public State convertToState(String channelId) {
        switch (channelId) {
            case CHANNEL_DIMMINGLEVEL:
                if (position != null) {
                    return new PercentType(position);
                } else {
                    return null;
                }

            case CHANNEL_COMMAND:
                if (position != null) {
                    int wert = Integer.valueOf(position);
                    if (wert == 0) {
                        return OnOffType.OFF;
                    } else {
                        return OnOffType.ON;
                    }
                } else {
                    // return (command == Commands.OFF ? OnOffType.OFF : OnOffType.ON);
                    return null;
                }

            case CHANNEL_SHUTTER:
                if (position != null) {
                    try {
                        return new PercentType(position);
                    } catch(IllegalArgumentException e) {
                        return null;
                    }
                } else {
                    return null;
                }

            case CHANNEL_VALANCE1:
                if (valance1 != null) {
                    try {
                        return new PercentType(valance1);
                    } catch(IllegalArgumentException e) {
                        return null;
                    }
                } else {
                    return null;
                }

            case CHANNEL_VALANCE2:
                if (valance2 != null) {
                    try {
                        return new PercentType(valance2);
                    } catch(IllegalArgumentException e) {
                        return null;
                    }
                } else {
                    return null;
                }

            case CHANNEL_WINDSPEED:
                if (windspeed != null) {
                    return new DecimalType(windspeed);
                } else {
                    return null;
                }

            case CHANNEL_RAIN:
                if (rain != null) {
                    if (rain.equals("OFF")) {
                        return OnOffType.OFF;
                    } else {
                        return OnOffType.ON;
                    }
                } else {
                    return null;
                }

            case CHANNEL_TEMPERATURE:
                if (temp != null) {
                    return new DecimalType(temp);
                } else {
                    return null;
                }

            case CHANNEL_BRIGHTNESS:
                if (brightness != null) {
                    return new DecimalType(brightness);
                } else {
                    return null;
                }

            case CHANNEL_DUSK:
                if (dusk != null) {
                    return new DecimalType(dusk);
                } else {
                    return null;
                }

            default:
                return null;
            // return super.convertToState(channelId);
        }
    }

    public void convertFromState(String channelId, Type type) {
        if (CHANNEL_SHUTTER.equals(channelId) || CHANNEL_VALANCE1.equals(channelId) || CHANNEL_VALANCE2.equals(channelId)) {
            if (type instanceof OpenClosedType) {
                command = (type == OpenClosedType.CLOSED ? Commands.CLOSE : Commands.OPEN);
            } else if (type instanceof UpDownType) {
                command = (type == UpDownType.UP ? Commands.OPEN : Commands.CLOSE);
            } else if (type instanceof StopMoveType) {
                command = Commands.STOP;
            }
        }

    }

    public void setConfig(OpenWMSDeviceConfiguration deviceConfiguration) {
        // TODO Auto-generated method stub

    }

    public void encodeMessage(byte[] data) {
        // TODO Auto-generated method stub

    }

    public byte[] decodeMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addDevicePropertiesTo(DiscoveryResultBuilder discoveryResultBuilder) {
        // super.addDevicePropertiesTo(discoveryResultBuilder);
        discoveryResultBuilder.withProperty(PROPERTY_DEVICEID, deviceId);
        discoveryResultBuilder.withProperty(PROPERTY_PANID, panId);

    }

}
