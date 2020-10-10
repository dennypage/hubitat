### Multi-Relay Door/Shade Controller

This application creates and manages a single child device that controls a door or window shade
that uses descrete relay inputs (Open, Close, Stop) to manage the physical device. An example of
this is automated door controllers from companies like Doors In Motion (Caldwell/INMOTION).

There are three relays supported:

* Open: Start opening the door or shade.
* Close: Start closing the door or shade.
* Stop: Stop the door or shade at the current postion.

The Open and Close relay are requred. With two relays, the controller will support positions of
fully closed (0) and fully open (100). With the optional third relay, Stop, the controller will
support setting any arbitrary position from 0 to 100.

The individual Open/Close/Stop relays are expected to provide their own automatic off timers with
appropriate values for the device being operated. An reasonable example of this would be a Zooz
ZEN16 with "Switch Type" set to "Toggle Switch" and "Auto Turn-off Timer" enabled for 1 second.

As you would expect, position accuracy depends upon the accuracy of the Open/Close travel time
setting. Make a first attempt with a stop-watch, then confirm/adjust based on the door status
changing in the driver.

Note that there are two components to the controller:

* The [Parent Application](multi-relay-door-shade-controller-app.groovy) which is installed in Apps Code
* The [Child Driver](multi-relay-door-shade-controller-driver.groovy) which is installed in Drivers Code

Be sure to install both!
