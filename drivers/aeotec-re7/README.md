### Aeotec Range Extender 7

This driver supports the Range Extender 7 manufactured by Aeotec.

Please note that as of this writing (firmware version 1.1), there are a couple of
unexpected behaviors with the RE7:

### Indicator does not always work as expected
Following network inclusion, the indicator remains illuminated as expected. Double clicking
the "action button" enables and disables the indicator as documented. However as soon as the
illuminator status is queried (INDICATOR_GET) by the Refresh command, the device disables
the indicator, and it can no longer be enabled by double clicking the action button. The
indicator can only be re-enabled by an explicit set (INDICATOR_SET) done by the Configure
command.  Even once the indicators been enabled by the Configure command, if the indicator
is subsequently disabled and re-enabled via the action button, the indicator will again be
completely disabled as soon as a Refresh command is executed.

**I have received confirmation from Aeotec that this issue is being addressed.**

### Setting transmit power level does not work
The RE7 appears to ignore POWERLEVEL_SET. Regardless of what value is sent with POWERLEVEL_SET,
POWERLEVEL_GET always returns 0 (NORMAL). I've also not been able to detect a difference in
transmission power during normal operation. In sort, "Transmit power level" in the Preferences
section currently has no effect.

The RE7 does however pay attention to the power level specified with POWERLEVEL_TEST_NODE_SET,
so the Range Test command works as expected.

**This issue is still pending with Aeotec.**
