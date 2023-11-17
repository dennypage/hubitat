//
// Copyright (c) 2023, Denny Page
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
// Version 1.0.0    Initial release.
//

import groovy.transform.Field

// Supported Z-Wave Classes:
//
//     0x22 Application Status V1
//     0x55 Transport Service V2
//     0x59 Association Group Information V1
//     0x5A Device Reset Locally V1
//     0x5E Z-Wave Plus Info V2
//     0x66 Barrier Operator V1
//     0x6C Supervision V1
//     0x71 Notification V8
//     0x72 Manufacturer Specific V2
//     0x73 Powerlevel V1
//     0x7A Firmware Update Meta Data V4
//     0x85 Association V2
//     0x86 Version V2
//     0x98 Security V1
//     0x9F Security 2 V1

@Field static final Map commandClassVersions = [0x22:1, 0x55:2, 0x66:1, 0x6C:1, 0x71:8, 0x73:1, 0x86:2]

@Field static final List powerLevels = ["normal", "-1dBm", "-2dBm", "-3dBm", "-4dBm", "-5dBm", "-6dBm", "-7dBm", "-8dBm", "-9dBm"]

metadata
{
    definition(
        name: "GoControl Garage Door Opener", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/gocontrol/gocongrol.groovy"
    )
    {
        capability "Actuator"
        capability "GarageDoorControl"
        capability "ContactSensor"
        capability "Sensor"
        capability "Battery"
        capability "TamperAlert"
        capability "Refresh"

        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"

        command "resetTiltSensor"

        command "powerTest", [[name: "seconds*", type: "NUMBER", defaultValue: "0",
                               description: "Seconds before returning to normal power"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against (decimal)"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        fingerprint mfr:"014F", prod:"4744", deviceId:"3531",
            inClusters:"0x5E,0x55,0x98,0x9F",
            deviceJoinName: "GoControl GD00Z-8-GC"
    }
}

preferences
{
    input name: "testFrames", title: "Frame count for range testing",
        type: "number", defaultValue: "10", range: "1..255"

    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable descriptionText logging", type: "bool", defaultValue: true
}

void logsOff() {
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "Debug logging disabled"
}

void installed() {
    runIn(1, refresh)
    runIn(1800, logsOff)
}

void updated() {
    if (logEnable) log.debug "Updated preferences"

    log.warn "Debug logging is ${logEnable}"
    log.warn "Description logging is ${txtEnable}"
}

void refresh() {
    if (logEnable) log.debug "Refresh"

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.barrierOperatorV1.barrierOperatorGet())
    cmds.add(zwave.powerlevelV1.powerlevelGet())
    cmds.add(zwave.powerlevelV1.powerlevelTestNodeGet())
    cmds.add(zwave.versionV2.versionGet())
    sendCmds(cmds)
}

void open() {
    if (logEnable) log.debug "open()"
    sendCmd(zwave.barrierOperatorV1.barrierOperatorSet(requestedBarrierState: 255))
}

void close() {
    if (logEnable) log.debug "close()"
    sendCmd(zwave.barrierOperatorV1.barrierOperatorSet(requestedBarrierState: 0))
}

void resetTiltSensor() {
    logEvent("tamper", "clear", null, "Tamper cleared")
    logEvent("battery", "100", "%", "Battery alert reset")
}

static List<String> powerLevelToString(Number power) {
    return power ? ["-${power}", "dBm"] : ["normal", ""]
}

static Integer stringToPowerLevel(String string) {
    def match = (string =~ /-([0-9]+)dBm/)
    if (match.find()) return match.group(1).toInteger()
    return 0
}

void powerTest(Number seconds, String powerString) {
    if (seconds < 0 || seconds > 255) {
        log.error "Invalid powerTest seconds ${seconds}"
        return
    }

    Short power = stringToPowerLevel(powerString)
    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.powerlevelV1.powerlevelSet(powerLevel: power, timeout: seconds))
    cmds.add(zwave.powerlevelV1.powerlevelGet())
    sendCmds(cmds)
}

void rangeTest(Number node, String powerString) {
    if (node < 1) {
        log.error "Invalid test node ${node}"
        return
    }

    Short power = stringToPowerLevel(powerString)
    Integer frames = testFrames ? testFrames.toInteger() : 10
    logEvent("rangeTest", "pending", null, "Range test pending - sending ${frames} frames to node ${node} at power level ${powerString}")

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.powerlevelV1.powerlevelTestNodeSet(powerLevel: power, testFrameCount: frames, testNodeid: node))
    cmds.add(zwave.powerlevelV1.powerlevelTestNodeGet())
    sendCmds(cmds, 100)
}

void requestPowerLevel() {
    sendCmd(zwave.powerlevelV1.powerlevelGet())
}

void requestTestNode() {
    sendCmd(zwave.powerlevelV1.powerlevelTestNodeGet())
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

void sendCmd(hubitat.zwave.Command cmd) {
    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(cmd.format()), hubitat.device.Protocol.ZWAVE))
}

void sendCmds(List<hubitat.zwave.Command> cmds, Long interval = 200) {
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds.collect { cmd -> zwaveSecureEncap(cmd) }, interval), hubitat.device.Protocol.ZWAVE))
}

void parse(String description) {
    if (logEnable) log.debug "parse: ${description}"

    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    }
    else {
        log.warn "Non Z-Wave parse event: ${description}"
    }
}

void zwaveEvent(hubitat.zwave.commands.barrieroperatorv1.BarrierOperatorReport cmd) {
    if (logEnable) log.debug "BarrierOperatorReport: ${cmd}"

    switch (cmd.barrierState) {
        case 0:
            logEvent("door", "closed", null, "${device.displayName} is closed", false)
            logEvent("contact", "closed", null, null, false)
            break
        case 252:
            logEvent("door", "closing", null, "${device.displayName} is closing", false)
            break
        case 253:
            logEvent("door", "unknown", null, "${device.displayName} position unknown", true)
            logEvent("contact", "open", null, null, false)
            break
        case 254:
            logEvent("door", "opening", null, "${device.displayName} is opening", false)
            break
        case 255:
            logEvent("door", "open", null, "${device.displayName} is open", false)
            logEvent("contact", "open", null, null, false)
            break
        default:
            log.warn "Unhandled Barrier Operator Report: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
    if (logEnable) log.debug "ApplicationStatus: ${cmd}"

    switch (cmd.status) {
        case 0:
            log.warn "${device.displayName} is busy: Try again later"
            break
        case 1:
            log.warn "${device.displayName} is busy: Try again in ${cmd.waitTime} seconds"
            break
        case 2:
            log.warn "${device.displayName} is busy: Request will be executed later"
            break
        default:
            log.warn "Unknown ApplicationStatus: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
    if (logEnable) log.debug "ApplicationStatus: ${cmd}"

    log.error "Request rejected"
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    if (logEnable) log.debug "NotificationReport: ${cmd}"

    if (cmd.notificationType == 0x06) {
        // Access Control notification
        switch (cmd.event) {
            case 0x44:
                log.error "Hardware Failure"
                logEvent("door", "unknown", null, "${device.displayName} position unknown", true)
                break
            case 0x45:
                log.warn "UL Lockout"
                break
            case 0x46:
                log.warn "Obstruction"
                break
            case 0x49:
                log.error "Loss of Tilt Sensor"
                logEvent("door", "unknown", null, "${device.displayName} position unknown", true)
                break
            case 0x4A:
                logEvent("battery", "1", "%", "${device.displayName} tilt sensor low battery", true)
                break
            default:
                if (cmd.event) log.warn "Unhandled Access notifcation event: ${cmd.event}"
                return
        }
    }
    else if (cmd.notificationType == 0x07) {
        // Home Security
        switch (cmd.event) {
            case 0x03:
                logEvent("tamper", "detected", null, "Tamper detected", true)
                break
            default:
                if (cmd.event) log.warn "Unhandled Home Security notifcation event: ${cmd.event}"
                return
        }
    }
    else {
        log.warn "Unhandled NotificationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelReport cmd) {
    unschedule(requestPowerLevel)

    if (logEnable) log.debug "PowerLevelReport: ${cmd}"

    String power, unit, description
    (power, unit) = powerLevelToString(cmd.powerLevel)
    description = "Transmit power level is ${power}${unit}"
    if (cmd.timeout) {
        description += ", timeout in ${cmd.timeout} seconds"
    }
    logEvent("powerLevel", power, unit, description)

    if (cmd.timeout) {
        runIn(cmd.timeout, requestPowerLevel)
    }
}

void zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd) {
    unschedule(requestTestNode)

    if (logEnable) log.debug "PowerLevelTestNodeReport: ${cmd}"

    // Check test validity
    if (cmd.testNodeid == 0) {
        logEvent("rangeTest", "none")
        return
    }

    Boolean complete = false
    switch (cmd.statusOfOperation) {
        case 0:
            // Test failed
            complete = true
            status = "failed"
            break
        case 1:
            // Test succeeeded
            complete = true
            status = "succeeded"
            break
        case 2:
            // Test in progress
            status = "in progress"
            break
    }

    if (complete) {
        logEvent("rangeTestReceived", "${cmd.testFrameCount}", "frames", "Received ${cmd.testFrameCount} frames from node ${cmd.testNodeid}")
        logEvent("rangeTest", status, null, "range test ${status}")
    }
    else {
        logEvent("rangeTest", status, null, "range test ${status}")
        logEvent("rangeTestReceived", "${cmd.testFrameCount}", "frames", "Received ${cmd.testFrameCount} frames from node ${cmd.testNodeid}")
        runIn(2, requestTestNode)
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    if (logEnable) log.debug "SupervisionGet: ${cmd}"

    encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }

    sendCmd(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0))
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "Unhandled cmd: ${cmd}"
}
