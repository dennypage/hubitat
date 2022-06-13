### WeeWX to Hubitat

This package provides a direct local connection for posting data from the WeeWX server to the Hubitat

There are two components required:

* The Hubitat device driver, located in this directory.

* The WeeWX user extension, located [here](https://github.com/dennypage/weewx-hubitat).


#### Hubitat device driver

The Hubitat device driver receives messages from WeeWX. Installation of the Hubitat driver is pretty simple:

* Install the WeeWX device driver (weewx.groovy) from this directory. See [How to Install Custom Drivers](https://docs.hubitat.com/index.php?title=How_to_Install_Custom_Drivers) in the Hubitat documentation for detailed instructions.

* Create a WeeWX virtual device using the Add Virtual Device button on the Devices page.

* Enter and save the IPv4 address of the WeeWX server in the preferences section. It should look something like this: 192.168.100.1

* Take note of the hubURL state variable. You will need this for configuring the WeeWX extension below.

#### WeeWX user extension

The WeeWX extension can be found [here](https://github.com/dennypage/weewx-hubitat). Installation of the WeeWX extension is also pretty simple:

* Download the extenstion as a [zip file](https://github.com/dennypage/weewx-hubitat/archive/master.zip). If your system doesn't support zip files, you can use a [tar.gz file](https://github.com/dennypage/weewx-hubitat/archive/master.tar.gz) instead.

* Do not unzip or untar the file.

* Run the WeeWX extension installer (documentation [here](https://weewx.com/docs/utilities.htm#wee_extension_utility)) with the downloaded file: wee_extension --install=master.zip

* Edit the [[Hubitat]] section of the weewx.conf file:

  - Change the server_url paramater to match the value of hubURL show in the Hubitat WeeWX driver. It should look something like this: http://192.168.100.2:39501

  - If you want to change the interval at which data is posted to the Hubitat, you can change the post_interval parameter. The default is 60 seconds.

  - If the Hubitat needs different units (US/METRIC) than WeeWX is natively configured for for, you can add a target_unit option to the [[Hubitat]] section. Avalable options can be found in the [StdConvert] section of the config file.
  
* Confirm that "user.hubitat.Hubitat" has been added to the list of restful_services in the [[Services]] section of the config file.

* Restart the WeeWX server.


### Attributes of the Hubitat WeeWX device

The hubitat device driver provides the following attributes (if available in WeeWX):

| Hubitat device attribute  | WeeWX Internal name      | Description              |
| :------------------------ | :------------------------| :------------------------|
| temperature | outTemp | Temperature | 
| humidity | outHumidity | Humidity |
| windSpeed | windSpeed | Wind speed |
| windDirection | windDir | Wind direction |
| windGustSpeed | windGust | Wind gust speed |
| windGustDirection | windGustDir | Wind gust direction |
| apptemp | appTemp | Apparent temperature |
| heatindex | heatindex | Heat index |
| humidex | humidex | Humidity Index |
| windchill | windchill | Windchill |
| rain | rain | Rain |
| rainRate | rainRate | Rain rate |
| hourRain | hourRain | Rain in the last hour |
| dayRain | dayRain | Rain today (since midnight) |
| rain24 | rain24 | Rain in the last 24 hours |
| barometer | barometer | Barometric pressure |
| dewpoint | dewpoint | Dew point |
| cloudbase | cloudbase | Cloud base |
| uv | UV | Ultraviolet radiation |
| radiation | radiation | Solar irradiance |
| THSW | THSW | Temperature Humidity Solar Wind |

Note that some attributes may not be available for your weather station.
