### Please note: This driver does not currently support S2. In order to use this driver on the C-7, the Aeotec must not be securely enrolled.


### Aeotec Range Extender 7

This driver supports the Range Extender 7 manufactured by Aeotec.


### Indicator does not always work as expected
As of this writing (firmware version 1.1), there is an unexpected behavior with the RE7.
Following network inclusion, the indicator remains illuminated as expected. Double clicking
the "action button" enables and disables the indicator as documented. However as soon as the
illuminator status is queried (INDICATOR_GET) by the Refresh command, the device disables
the indicator, and it can no longer be enabled by double clicking the action button. The
indicator can only be re-enabled by an explicit set (INDICATOR_SET) done by the Configure
command.  Even once the indicators been enabled by the Configure command, if the indicator
is subsequently disabled and re-enabled via the action button, the indicator will again be
completely disabled as soon as a Refresh command is executed.

**I have received confirmation from Aeotec that this issue is addressed in firmware 1.2 in September.**
