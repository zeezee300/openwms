# OpenWMS Binding

The OpenWMS binding should be compatible with the Warema WMS Stick (1002775), which contains both receiver and transmitter functions.

For _bidirectional_ actuators ("blinds") it is even possible to update the OpenHab item state if the actuator gets modified outside of OpenHab.

## Supported Things

This binding supports the Warema USB-Stick transceivers as bridges for accessing sensors and actuators. Unfortunately, I only have one blind, so other devices could not be tested. In addition, the log of the WMS stick is not publicly documented. 

First of all you have to configure an Warema USB-Stick. This device has to be added manually to OpenHab and is represented by an _OpenWMS bridge_. 

You just have to set the right serial port. If everything is running fine you should see the _Current firmware version:_ of your USB-Stick in the properties of your bridge.

## Binding Configuration

#### Warema USB-Stick as Bridge
So far the Bridge should be configured manually via the PaperUI. Note that the serial port that is assigned to it may change if you have more than one USB serial device.

#### Warema USB-Stick as Bridge over TCP/IP
You can also use an USB-Stick device over TCP/IP.
To start a TCP server for an WMS device, you can use socat:

```
socat tcp-l:4001,fork,keepalive,nodelay,reuseaddr /dev/ttyUSB2,nonblock,raw

```
A TCP bridge, for use with socat on a remote host, can only be configured manually either through the PaperUI by adding an "OpenWMS WAREMA WMS-Stick Transceiver over TCP/IP" device


## Thing Configuration

### Manually 
Currently only blinds are supported.
If you know the serial number of the motor and the PANID of your WMS-System you can configure the devices manually:

Bridge: Must be provided
DeviceId: 3 byte hex string (E49D08) - should be the Hex of Serial
Serial: The serial-No. of the motor (123456) 
Channel: 17 (default)
PANID: 2byte hex string (FF9D08)
Check-Time: time in seconds (how long is the time until the next state scan - defauld 60)

Note:
The DeviceId OR the serial-No is necessary!
Also the PANID is required.

### Discovery (search for new devices with the remote control)

Tested with: WAREMA WMS Handsender basis (ArtNr. 1002953)

The devices could be automatically discovered by pressing the scan-button of your warema remote control and put in the Inbox (PAPER UI) or may be conigured manually. 

Note:
However, it is a prerequisite that the devices have already been trained in the warema network and are known.

After the bridge is configured in openhab, wake up the remote control (simply press any button on the remote control). Then press the "Learn" button in the battery compartment for about 5 seconds until the green control lamp lights up. 

You have to wait a few seconds until the green light (the right one on the front of the remote control) stops blinking and turns into red or orange.
During the first scan the WMS network ID is determined and the LED light turns red. Then the red stop button must be pressed (this is the round button between the high and low buttons).

The USB-Stick should receives scan response messages from any sensor or actuator. You can find the new devices put in the Inbox (openhab PAPER UI).
You can also find the current WMS network key as a property of the WMS-Stick transceiver

Please note that currently only the preconfigured radio channel 17 is scanned. Other channel settings are supported but must be configured manually.

## Channels
This binding currently supports following channel types for blinds and weather-moduls:

### BLINDS
| Channel Type ID | Item Type     | Description                                                                        |
|-----------------|---------------|------------------------------------------------------------------------------------|
| command         | Switch        | Command channel (ON, OFF)                                                          |
| shutter         | Rollershutter | Shutter/blind channel (UP, DOWN, STOP).                                            |
| dimminglevel    | Dimmer        | Dimming level channel (Percentage 0-100 => 0 = UP, 100 = DOWN) .                   |

### WEATHER 
| Channel Type ID | Item Type     | Description                                                                        |
|-----------------|---------------|------------------------------------------------------------------------------------|
| windspeed       | Number        | Average wind speed in meters per second (read only)                                |
| rain            | Switch        | Stauts rain => ON: rain, OFF: No rain (read only)                                  |
| temperature     | Number        | Current temperature in degree Celsius (read only)                                  |

## Full Example





