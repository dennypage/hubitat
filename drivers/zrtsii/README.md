### Somfy Z-Wave to RTS Interface II (ZRTSII)

These drivers supports the Z-Wave to RTS Interface II (ZRTSII) made by Somfy

There are two drivers that support different components of the ZRTSII

#### Somfy ZRTSII Controller

This driver only serves to recognize the Static Controller Node of the ZRTSII. Other than that, it doesn't do anything worthwhile.

#### Somfy ZRTSII Node

This driver supports the 16 virtual nodes of the ZRTSII. Each of the 16 Virtual Nodes represents an RTS channel, and is presented as a Z-Wave Multiposition Motor Control Class A Device. As a Class A device, there is no ability to report the current motor position or when an endpoint has been reached. In short, it's a one-way interface.

Since there is no position feedback, the driver attempts to maintain position based upon the time and direction of movement. This is not an exact science, and there are a number of things that can when a move command actually beginning: how busy the hub is, how busy the ZRTSII is, how reliable the RTS motor reception is, etc. 

In other words, don't expect positioning to be highly accurate. 10% is a good result. The best results will always be from a fully open or fully closed starting point. If the positioning is off, moving moving to a fully open or fully closed position will reset the calculation.
