### Idle Node Refresher

This application monitors the last activity time for a set of devices, and
performs a refresh on devices if they have not been used for an extended
period of time.

Two key values control the behavior of the application:

* **Inactivity Idle Hours**: The number of hours of inactivity before a
node is considered idle. A reasonable value for this is 24 (1 day) or
greater. The default value is 25 hours.

* **Refresh Interval Minutes**: The minimum number of minutes between idle
node refreshes. Only one node will be refreshed per interval. A reasonable
value for this is 5 or greater. The default value is 10 minutes.

The application is designed to take a slow and steady approach to refreshing
devices. It will refresh only one device per Refresh Interval, regardless
of how many devices are considered idle. The purpose of this is to minimize
unnecessary traffic and ensure that networks such as Z-Wave are not
overwhelmed.

In addition to a refresh() call, the application also has the ability to
refresh the state of a switch by calling on() or off(). See the configuration
page for more information. Use this option with care.

There are two components to the Idle Node Refresher:

* The [Parent Application](idle-node-refreshers.groovy)
* The [Child Application](idle-node-refresher.groovy)

Both of these should be installed in Apps Code--be sure to install both!

Once installed, create a refresher using *+Add User App* in the Apps panel.
