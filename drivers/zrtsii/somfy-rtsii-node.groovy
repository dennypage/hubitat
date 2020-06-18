//
// Copyright (c) 2020, Denny Page
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

//
// The Somfy Z-Wave to RTS Interface II (ZRTSII)
//
// This driver supports the Virtual Nodes of the ZRTSII Controller
//
// Each of the 16 Virtual Nodes represents an RTS channel, and is
// presented as a Z-Wave Multiposition Motor Control Class A Device.
// As a Class A device, there is no ability to report the current
// motor position or when an endpoint has been reached. In short,
// it's a one-way interface.
//
// Since there is no position feedback available, we must attempt
// to guess position based upon the amount of time since a command
// was sent. There are a number of things that can affect this, 
// how busy the hub is, how busy the ZRTSII is, how reliable the RTS
// motor receiption is, etc. In other words, don't expect positioning
// to be highly accurate. 10% is a good result. Best results will
// always be from a fully open or fully closed starting point.
//
// Some RTS motors seem to frequently miss commands from the ZRTSII.
// The ZRTSII will flash indicating that the RTS command has been
// sent, but the motor will not move. To address this, the cmdCount
// parameter can be increased until the motor operates reliably.
//

metadata
{
    definition (
        name: "Somfy ZRTSII Node", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Switch"
        capability "WindowShade"

        command "stop"

        fingerprint mfr: "0047", prod: "5A52", deviceId: "5401",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "54025",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5403",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5404",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5405",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5406",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5407",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5408",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5409",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540A",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540B",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540C",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540D",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540E",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "540F",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5410",
            inClusters: "0x2C,0x72,0x26,0x20,0x25,0x2B,0x86"

        // All command classes are version 1
        // 0x20 COMMAND_CLASS_BASIC
        // 0x25 COMMAND_CLASS_SWITCH_BINARY
        // 0x26 COMMAND_CLASS_SWITCH_MULTILEVEL
        // 0x2B COMMAND_CLASS_SCENE_ACTIVATION
        // 0x2C COMMAND_CLASS_SCENE_ACUTATOR_CONF
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC
        // 0x86 COMMAND_CLASS_VERSION
    }
}

preferences
{
    input name: "travelTime", title: "Open / Close travel time in seconds",
        description: "(position accuracy depends on this)",
        type: "number", defaultValue: "30", range: "1..127"

    input name: "cmdCount", title: "Send commands this many times",
        description: "(needed for some rts motor types)",
        type: "enum", defaultValue: "1", options: [[1:"1 [default]"], [2:"2"], [3:"3"], [4:"4"], [5:"5"]]

    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable descriptionText logging", type: "bool", defaultValue: true
}

def installed()
{
    state.moveBegin = 0
    state.oldPosition = 0
    state.newPosition = 100
}

def moveComplete(args)
{
    if (logEnable) log.debug "moveComplete(${args})"

    if (!state.moveBegin) return

    // Note that we do not send a stop command if we are moving to either
    // fully open or fully closed. This helps address the natural errors
    // that occur when using time based estimates of position.
    if (args.sendStop)
    {
        def hubAction = new hubitat.device.HubAction(new hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange().format(), hubitat.device.Protocol.ZWAVE)
        Integer limit = cmdCount.toInteger()
        pauseExecution(1) // Yield
        for (Integer i = 0; i < limit; i++) sendHubCommand(hubAction)
    }
    
    BigDecimal elapsed = now() - state.moveBegin
    state.moveBegin = 0

    Integer delta = (elapsed / (travelTime.toBigDecimal() * 10.0))
    if (state.newPosition > state.oldPosition)
    {
        newPosition = Math.min(state.oldPosition.toInteger() + delta, (Integer) 100)
        status = newPosition < 99 ? "partially open" : "open"
    }
    else
    {
        newPosition = Math.max(state.oldPosition.toInteger() - delta, (Integer) 0)
        status = newPosition > 0 ? "partially open" : "closed"
    }
    
    log.info "${device.displayName} is ${status} (${newPosition})"

    def map = [:]
    map.name = "switch"
    map.value = newPosition > 0 ? "on" : "off"
    sendEvent(map)
    
    map.name = "windowShade"
    map.value = status
    if (txtEnable) map.descriptionText = "${device.displayName} is ${status}"
    sendEvent(map)

    map.name = "position"
    map.value = newPosition
    if (txtEnable) map.descriptionText = "${device.displayName} position ${newPosition}"
    sendEvent(map)
}

def stop()
{
    if (logEnable) log.debug "stop()"

    unschedule(moveComplete)
    moveComplete([sendStop: true])
}

def setPosition(BigDecimal newPosition)
{
    if (logEnable) log.debug "setPosition(${newPosition})"
    if (state.moveBegin)
    {
        if (newPosition == state.newPosition) return
        // Stop and allow state update
        stop()
        pauseExecution(100)
    }
    
    BigDecimal oldPosition = device.currentValue("position")
    if (newPosition == oldPosition) return

    Boolean upDown
    Boolean sendStop
    BigDecimal delta
    if (newPosition > oldPosition)
    {
        upDown = true
        newPosition = Math.min(newPosition.toInteger(), (Integer) 100)
        delta = newPosition - oldPosition
        sendStop = newPosition < 99 ? true : false
    }
    else
    {
        upDown = false
        newPosition = Math.max(newPosition.toInteger(), (Integer) 0)
        delta = oldPosition - newPosition
        sendStop = newPosition > 0 ? true : false
    }
    Long millis = travelTime.toBigDecimal() * 10.0 * delta
    
    if (logEnable) log.debug "moving position from ${oldPosition} to ${newPosition} (${millis}ms)"
    
    def map = [:]
    status = upDown ? "opening" : "closing"
    map.name = "windowShade"
    map.value = status
    if (txtEnable) map.descriptionText = "${device.displayName} is ${status}"
    sendEvent(map)

    def hubAction = new hubitat.device.HubAction(new hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange(upDown: upDown).format(), hubitat.device.Protocol.ZWAVE)
    Integer limit = cmdCount.toInteger()
    pauseExecution(1) // Yield
    for (Integer i = 0; i < limit; i++) sendHubCommand(hubAction)

    state.moveBegin = now()
    state.oldPosition = oldPosition
    state.newPosition = newPosition

    runInMillis(millis.toLong(), moveComplete, [data: [sendStop: sendStop]])
}

def open()
{
    setPosition(100)
}

def close()
{
    setPosition(0)
}

def on()
{
    open()
}

def off()
{
    close()
}

// Effectively unused...
def parse(String description)
{
    hubitat.zwave.Command cmd = zwave.parse(description,commandClassVersions)
    if (cmd)
    {
        return zwaveEvent(cmd)
    }

    if (logEnable) log.debug "Non Z-Wave parse event: ${description}"
    return null
}

def zwaveEvent(hubitat.zwave.Command cmd)
{
    if (logEnable) log.debug "Unhandled cmd: ${cmd.toString()}"
    return null
}

