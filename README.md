# Note
This fork ports the orginal OpenWMS Binding by zeezee300 to OpenHab 3.0, kudos to him for preparing this binding.
Non OSGI packages have been removed, therefore external opening of the USB port is still required even when connecting the Warema stick directly to your device.

1.) download 'serialport.jar' from https://github.com/zeezee300/openwms/blob/master/serialport.jar
2.) copy it where you want (f.e. /opt/warema) and make it excecutable
3.) excecute it with your device as an argument (f.e./dev/ttyUSB2):

It works fine on my OpenHabian 3.0.0 installation, but the code still requires quite a bit of cleanup.

The following is the original readme from zeezee300:


# OpenWMS Binding

The OpenWMS binding should be compatible with the Warema WMS Stick (1002775), which contains both receiver and transmitter functions.

For _bidirectional_ actuators ("blinds") it is even possible to update the OpenHab item state if the actuator gets modified outside of OpenHab.

## Supported Things

This binding supports the Warema USB-Stick transceivers as bridges for accessing sensors and actuators. Unfortunately, I only have one blind, so other devices could not be tested. In addition, the log of the WMS stick is not publicly documented. 

First of all you have to configure an Warema USB-Stick. This device has to be added manually to OpenHab and is represented by an _OpenWMS bridge_. 

You just have to set the right serial port. If everything is running fine you should see the _Current firmware version:_ of your USB-Stick in the properties of your bridge.

## Installation notes
To use this binding, the feature openhab-transport-serial has to be installed with karaf:

   * open karaf shell (`` ssh -p 8101 openhab@localhost ``, Standard password 'habopen')
 
   * ``feature:install openhab-transport-serial``
    
    

## Binding Configuration

#### Warema USB-Stick as Bridge
So far the Bridge should be configured manually via the PaperUI. Note that the serial port that is assigned to it may change if you have more than one USB serial device.

To solve this, you can define a udev rule to create a symlink so that the port name for the WMS stick is always /dev/warema.

1.) Check which is the ATTRS{serial} of your WMS stick:

```
udevadm info --name=/dev/ttyUSB0 --attribute-walk
```

2.) Edit /lib/udev/rules.d/99-usbsticks.rules and add the following line (pls. replace ABC12345 with the ATTRS{serial} of your WMS stick)

```
SUBSYSTEM==“tty”, ATTRS{serial}==“ABC12345”, SYMLINK+=“warema”
```

If necessary, an initialization script can be executed at the same time:

```
SUBSYSTEM==“tty”, ATTRS{serial}==“ABC12345”, SYMLINK+=“warema”, RUN+="/usr/bin/at -M -f /opt/warema/yourscript.sh now + 1 minutes"
```

3.) Do not forget to restart the udev rules after the changes:

```
udevadm control --reload-rules && udevadm trigger
```


#### Warema USB-Stick as Bridge over TCP/IP
You can also use an USB-Stick device over TCP/IP.
To start a TCP server for an WMS device, you can use socat:

```
socat tcp-l:4001,fork,keepalive,nodelay,reuseaddr /dev/ttyUSB2,nonblock,raw

```
A TCP bridge, for use with socat on a remote host, can only be configured manually through the PaperUI by adding an "OpenWMS WAREMA WMS-Stick Transceiver over TCP/IP" device.

Sometimes the serial port on the server side is not initialized correctly. For testing purposes I have provided a small java program to fix this problem as a workaround:

1.) download 'serialport.jar'

2.) copy it where you want (f.e. /opt/warema) and make it excecutable

3.) excecute it with your device as an argument (f.e./dev/ttyUSB2):

```
java -jar /opt/warema/serialport.jar /dev/ttyUSB2
```

The script opens the port and everything should work as expected until the wms-stick is unpluged.
Therefor a udev-definition wich runs a script could be helpfull (see above).

```
SUBSYSTEM==“tty”, ATTRS{serial}==“ABC12345”, SYMLINK+=“warema”, RUN+="/usr/bin/at -M -f /opt/warema/yourscript.sh now + 1 minutes"
```

## Thing Configuration

### Manually 
Currently only blinds are supported.
If you know the serial number of the motor and the PANID of your WMS-System you can configure the devices manually:

Bridge: Must be provided

DeviceId: 3 byte hex string (E49D08) - should be the Hex of Serial

Serial: The serial-No. of the motor (123456) 

Channel: 17 (default)

PANID: 2byte hex string (9D08). If you don't know the PANID try to use 'FFFF' as default.

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
During the first scan the WMS network ID is determined and the LED light turns red. Then the stop button must be pressed (this is the round button between the high and low buttons).

The USB-Stick should receives scan response messages from any sensor or actuator. You can find the new devices put in the Inbox (openhab PAPER UI).
For your information the current WMS network key will be stored as a property of the WMS-Stick transceiver.

Please note that currently only the preconfigured radio channel 17 is scanned. Other channel settings are supported but must be configured manually.
It is suggested to repeat the process 2-3 times.

## Channels
This binding currently supports following channel types for blinds and weather-moduls:

### BLINDS
| Channel Type ID | Item Type     | Description                                                                        |
|-----------------|---------------|------------------------------------------------------------------------------------|
| command         | Switch        | Command channel (ON, OFF)                                                          |
| shutter         | Rollershutter | Shutter/blind channel (UP, DOWN, STOP).                                            |
| dimminglevel    | Dimmer        | Dimming level channel (Percentage 0-100 => 0 = UP, 100 = DOWN) .                   |

If you use a weather station, you can set the the preconfigured limits for wind, rain, brightness and dusk. 
But attention: This will overwrite the default values.
| Parameter                |  Description                                                                        |
|--------------------------|-------------------------------------------------------------------------------------|
| Skip Limit configuration | Fully skip and ignore the limit configuration. When this is enabled, the set mode command and individual message configurations are ignored.|
| Rain                     | Note or ignore rain                                                                 |
| Limit Wind               | Speed in meters per second: min = 5 m/s, max = 13 m/s                               |
| Limit Sun                | Brightness: min = 10 klx, max = 50 klx                                              |
| Limit Dusk               | Dusk: min = 16 klx, max = 400 klx                                                   |

### WEATHER
| Channel Type ID | Item Type     | Description                                                                        |
|-----------------|---------------|------------------------------------------------------------------------------------|
| windspeed       | Number        | Average wind speed in meters per second: min = 5 m/s, max = 13 m/s (read only)     |
| rain            | Switch        | Status rain => ON: rain, OFF: No rain (read only)                                  |
| brightness      | Number        | (read only)                                                                        |
| dusk            | Number        | (read only)                                                                        |
| temperature     | Number        | Current temperature in degree Celsius (read only)                                  ||

## Full Example





