### UPS monitor and shutdown controller for use with Network UPS Tools (NUT)

This device driver (NUT upsmon) provides a Hubitat implementation upsmon, which is the UPS monitor and shutdown controller used by NUT.  The NUT upsmon driver is used with a remote NUT server (upsd), running on a general purpose server, NAS, etc.


#### Install the device driver

Install the Hubitat NUT upsmon device driver (nut-upsmon.groovy) contained in this directory on the hub. Installation of the Hubitat driver is pretty simple -- see [How to Install Custom Drivers](https://docs2.hubitat.com/en/how-to/install-custom-drivers) in the Hubitat documentation for detailed instructions.

The driver is also available via Hubitat Package Manager (HPM).

#### Configure the device driver

After installing the driver, create a virtual device of type "NUT upsmon" on the Devices page. Once you have created the device, in the preferences section enter the following:

* NUT server host: The hostname or IP address of the NUT server (upsd). It should look something like this: myhost.mydomain.net or 192.168.100.1

* NUT server port: The port number for the NUT server (upsd), as defined in upsd.conf. This will usually be 3493.

* UPS name: The name of the UPS to monitor, as defined in in ups.conf.

* NUT username: The username for the connection, as defined in upsd.users

* NUT password: The password for the connection, as defined in upsd.users

* Polling frequency: How often to poll for UPS status. This is the equivalent of POLLFREQ found in upsmon.conf. The default is 5 seconds.

* Enable Hub shutdown: If you want to allow NUT upsmon to shut down the hub, you must enable this. If you do not enable the option, the hub will not actually shut down when the ups battery runs out.
