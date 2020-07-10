### WeeWX to Hubitat

There are two components required:

* The Hubitat device driver, located in this directory.

* The WeeWX user extension, located [here](https://github.com/dennypage/weewx-hubitat)


#### Hubitat device driver

The Hubitat device driver receives messages from WeeWX. Installation of the Hubitat driver is pretty simple:

* Install the WeeWX device driver (weewx.groovy) from this directory. See [How to Install Custom Drivers](https://docs.hubitat.com/index.php?title=How_to_Install_Custom_Drivers) in the Hubitat documentation for detailed instructions.

* Create a WeeWX virtual device using the Add Virtual Device button on the Devices page.

* Enter and save the IPv4 address of the WeeWX server in the preferences section. It should look something like this: 192.168.100.1

* Take note of the hubURL state variable. You will need this for configuring the WeeWX extension below.

#### WeeWX user extention

The WeeWX extension can be found [here](https://github.com/dennypage/weewx-hubitat). Installation of the WeeWX extension is also pretty simple:

* Download the extenstion as a [zip file](https://github.com/dennypage/weewx-hubitat/archive/master.zip). If your system doesn't support zip files, you can use a [tar.gz file](https://github.com/dennypage/weewx-hubitat/archive/master.tar.gz) instead.

* Run the WeeWX extenstion installer with the downloaded file: wee_extension --install=master.zip

* Edit the [[Hubitat]] section of the weewx.conf file:

  - Change the server_url paramater to match the value of hubURL show in the Hubitat WeeWX driver. It should look something like this: http://192.168.100.2:39501

  - If you want to change the interval at which data is posted to the Hubitat, you can change the post_interval parameter. The default is 60 seconds.

  - If the Hubitat needs different units (US/METRIC) than WeeWX is natively configured for for, you can add a target_unit option to the [[Hubitat]] section. Avalable options can be found in the [StdConvert] section of the config file.

* Restart the WeeWX server.
