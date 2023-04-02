//
// Copyright (c) 2020-2023, Denny Page
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
// motor reception is, etc. In other words, don't expect positioning
// to be highly accurate. 10% is a good result. Best results will
// always be from a fully open or fully closed starting point.
//
// Some RTS motors seem to frequently miss commands from the ZRTSII.
// The ZRTSII will flash indicating that the RTS command has been
// sent, but the motor will not move. To address this, the cmdCount
// parameter can be increased until the motor operates reliably.
//
// Version 1.0.0    Initial release
// Version 1.1.0    Unhandled events logged as warnings
// Version 1.1.1    Fix issue with null current position
// Version 1.2.0    Add option to support a reversed motor
// Version 1.3.0    Remove unused and confusing Switch capability.
//                  Simplify sendEvent use.
// Version 2.0.0    Support Somfy's My Postion.
//                  Add refresh capability (node ping)
// Version 2.0.1    Use SwitchMultilevelReport for node ping.
//                  Send events on refresh.
// Version 2.1.0    Add Actuator capability to allow device to
//                  appear in selection lists.
// Version 3.0.0    Code restructure and cleanup
// Version 3.1.0    Add startPositionChange and stopPositionChange
//

// Supported Z-Wave Classes:
//
//     0x20 COMMAND_CLASS_BASIC
//     0x25 COMMAND_CLASS_SWITCH_BINARY
//     0x26 COMMAND_CLASS_SWITCH_MULTILEVEL
//     0x2B COMMAND_CLASS_SCENE_ACTIVATION
//     0x2C COMMAND_CLASS_SCENE_ACUTATOR_CONF
//     0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC
//     0x86 COMMAND_CLASS_VERSION

import groovy.transform.Field

metadata
{
    definition(
        name: "Somfy ZRTSII Node", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/zrtsii/somfy-rtsii-node.groovy",
        singleThreaded: true
    )
    {
        capability "Actuator"
        capability "WindowShade"
        capability "Refresh"
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
    }
}

@Field static final Map commandClassVersions = [0x26:1, 0x72:1, 0x86:1]

preferences
{
    input name: "travelTime", title: "Open / Close travel time in seconds",
        description: "(position accuracy depends on this)",
        type: "number", defaultValue: "30", range: "1..127"

    input name: "myPosition", title: "Somfy My Position",
        description: "(leave blank if My Position not set)",
        type: "number", ange: "0..100"

    input name: "cmdCount", title: "Send commands this many times",
        description: "(needed for some rts motor types)",
        type: "enum", defaultValue: "1", options: [[1:"1 [default]"], [2:"2"], [3:"3"], [4:"4"], [5:"5"]]

    input name: "reverseMotor", title: "Reverse motor direction", type: "bool", defaultValue: false

    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable descriptionText logging", type: "bool", defaultValue: true
}

void installed() {
    state.moveBegin = 0
    state.oldPosition = 0
    state.newPosition = 100
}

void refresh() {
    List<hubitat.zwave.Command> cmds = []

    cmds.add(zwave.versionV1.versionGet())
    cmds.add(zwave.manufacturerSpecificV1.manufacturerSpecificGet())
    cmds.add(zwave.switchMultilevelV1.switchMultilevelGet())
    sendCmds(cmds)
}

void open() {
    setPosition(100)
}

void close() {
    setPosition(0)
}

void stop() {
    if (logEnable) log.debug "stop()"

    if (state.moveBegin) {
        unschedule(moveComplete)
        moveComplete([sendStop: true])
        return
    }
    else if (myPosition) {
        setPosition(myPosition)
    }
    else {
        sendStopCommand()
    }
}

void startPositionChange(String direction) {
    if (direction == "open") {
        open()
    }
    else if (direction == "close") {
        close()
    }
}

void stopPositionChange() {
    stop()
}

void setPosition(BigDecimal position) {
    Integer newPosition = position.toInteger()
    Boolean sendStop = false
    BigDecimal delta

    if (logEnable) log.debug "setPosition(${newPosition})"

    if (newPosition > 100) {
        newPosition = 100
    }
    else if (newPosition < 0) {
        newPosition = 0
    }

    if (state.moveBegin) {
        if (newPosition == state.newPosition) {
            return
        }

        // Stop and allow state update
        stop()
        pauseExecution(250)
    }

    Integer oldPosition = device.currentValue("position") ?: 0
    if (newPosition == oldPosition) {
        // There is  the possibility that the shade has been changed
        // has been changed without our knowledge. For example if
        // we think the shade is closed but someone opened the shade
        // using a hand controller. So to help reduce confusion, even
        // if we think we are already fully open, closed, or at
        // myPosition, we send a command anyway.
        if (newPosition == 0) {
            sendStartCommand(false)
        }
        else if (newPosition >= 100) {
            sendStartCommand(true)
        }
        else if (myPosition && newPosition == myPosition) {
            sendStopCommand()
        }
        return
    }

    if (newPosition > oldPosition) {
        delta = newPosition - oldPosition
    }
    else {
        delta = oldPosition - newPosition
    }
    Long millis = travelTime.toBigDecimal() * 10.0 * delta

    if (logEnable) log.debug "Moving position from ${oldPosition} to ${newPosition} (${millis}ms)"
    String status = newPosition > oldPosition ? "opening" : "closing"
    logEvent("windowShade", status, null, "Shade is ${status}")

    state.oldPosition = oldPosition
    state.newPosition = newPosition

    if (myPosition && newPosition == myPosition) {
        sendStopCommand()
    }
    else {
        Boolean upDown = newPosition > oldPosition
        if (newPosition > 0 && newPosition < 100) sendStop = true
        sendStartCommand(upDown)
    }
    state.moveBegin = now()
    runInMillis(millis.toLong(), moveComplete, [data: [sendStop: sendStop]])
}

void moveComplete(Map args) {
    if (logEnable) log.debug "moveComplete(${args})"

    if (!state.moveBegin) return

    // Note that we do not send a stop command if we are moving to either
    // fully open or fully closed. This helps address the natural errors
    // that occur when using time based estimates of position.
    if (args.sendStop) sendStopCommand()

    BigDecimal elapsed = now() - state.moveBegin
    Integer delta = (elapsed / (travelTime.toBigDecimal() * 10.0))
    state.moveBegin = 0

    String status, position, description
    if (state.newPosition > state.oldPosition) {
        position = Math.min(state.oldPosition.toInteger() + delta, (Integer) 100)
    }
    else {
        position = Math.max(state.oldPosition.toInteger() - delta, (Integer) 0)
    }

    if (position == "0") {
        status = "closed"
        description = "Shade is closed"
    }
    else if (position == "100") {
        status = "open"
        description = "Shade is open"
    }
    else {
        status = "partially open"
        description = "Shade is partially open (${position}%)"
    }
    logEvent("windowShade", status, null, description)
    logEvent("position", position, "%")
}

void logEvent(String name, String value, String unit = null, String description = null, Boolean warn = false) {
    Map map = [name: name, value: value]
    if (unit) {
        map.unit = unit
    }
    if (description) {
        map.descriptionText = description
    }
    sendEvent(map)

    if (description) {
        if (warn) {
            log.warn description
        }
        else if (txtEnable) {
            log.info description
        }
    }
}

void sendCmds(List<hubitat.zwave.Command> cmds, Long interval = 200) {
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds*.format(), interval), hubitat.device.Protocol.ZWAVE))
}

void sendCmdCount(hubitat.zwave.Command cmd, Integer count = 1, Long interval = 1) {
    List<hubitat.zwave.Command> cmds = []
    count.times {
        cmds.add(cmd)
    }
    sendCmds(cmds, interval)
}

void parse(String description) {
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    }
    else {
        log.warn "Non Z-Wave parse event: ${description}"
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
    if (logEnable) log.debug "VersionReport: ${cmd}"
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
    if (logEnable) log.debug "Manufacturer Specific Report: ${cmd}"
}

void zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
    if (logEnable) log.debug "Switch multilevel report: ${cmd}"
    // NB: The value in the multilevel report is only 0 or 255, so we ignore it
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "Unhandled cmd: ${cmd}"
}

private void sendStartCommand(Boolean upDown) {
    if (reverseMotor) upDown = !upDown
    sendCmdCount(zwave.switchMultilevelV1.switchMultilevelStartLevelChange(upDown: upDown), cmdCount.toInteger())
}

private void sendStopCommand() {
    sendCmdCount(zwave.switchMultilevelV1.switchMultilevelStopLevelChange(), cmdCount.toInteger())
}
