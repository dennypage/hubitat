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
// Version 1.0.0    Initial release.
// Version 1.1.0    Clean up log messages. Handle button push.
// Version 1.1.1    Mark seconds as a required input for power test.
// Version 1.2.0    Normalize logging.
// Version 1.2.1    Minor capitalization fix.
// Version 1.3.0    Add Initialize capability to cover PowerSource changes while hub not running.
// Version 2.0.0    Code restructure and cleanup
//

import groovy.transform.Field

// Supported Z-Wave Classes:
//
//     0x55 Transport Service V2
//     0x59 Association Group Information V3
//     0x5A Device Reset Locally V1
//     0x5E Z-Wave Plus Info V2
//     0x6C Supervision V1
//     0x70 Configuration V4
//     0x71 Notification V8
//     0x72 Manufacturer V2
//     0x73 Powerlevel V1
//     0x7A Firmware Update Meta Data V5
//     0x80 Battery V2
//     0x85 Association V2
//     0x86 Version V3
//     0x87 Indicator V3
//     0x8E Multi-Channel Association V3
//     0x9F Security 2 V1

@Field static final Map commandClassVersions = [0x6C:1, 0x70:4, 0x71:8, 0x73:1, 0x80:2, 0x86:3, 0x87:3]

@Field static final List powerLevels = ["normal", "-1dBm", "-2dBm", "-3dBm", "-4dBm", "-5dBm", "-6dBm", "-7dBm", "-8dBm", "-9dBm"]

metadata
{
    definition(
        name: "Ring Alarm Range Extender gen2", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/ring-re2/ring-range-extender-gen2.groovy"
    )
    {
        capability "Configuration"
        capability "Initialize"
        capability "Refresh"
        capability "Battery"
        capability "PowerSource"

        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"

        command "powerTest", [[name: "seconds*", type: "NUMBER", defaultValue: "0",
                               description: "Seconds before returning to normal power"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against (decimal)"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        fingerprint mfr:"0346", prod:"0401", deviceId:"0301",
            inClusters:"0x5E,0x59,0x85,0x80,0x70,0x5A,0x7A,0x87,0x72,0x8E,0x71,0x73,0x9F,0x6C,0x55,0x86",
            deviceJoinName: "Ring Alarm Range Extender gen2"
    }
}

preferences
{
    input name: "heartbeatInterval", title: "Heartbeat interval in minutes",
        type: "number", defaultValue: "70", range: "1..70"

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

void refresh() {
    if (logEnable) log.debug "Refresh"

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 8, v1AlarmType: 0, event: 2))
    cmds.add(zwave.notificationV8.notificationGet(notificationType: 8, v1AlarmType: 0, event: 3))
    cmds.add(zwave.batteryV2.batteryGet())
    cmds.add(zwave.powerlevelV1.powerlevelGet())
    cmds.add(zwave.powerlevelV1.powerlevelTestNodeGet())
    cmds.add(zwave.configurationV4.configurationGet(parameterNumber: 1))
    sendCmds(cmds)
}

void initialize() {
    if (logEnable) log.debug "Initialize"

    runIn(1, refresh)
}

void configure() {
    if (logEnable) log.debug "Configure"

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.versionV3.versionGet())
    cmds.add(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1))

    BigInteger interval = heartbeatInterval ? heartbeatInterval.toInteger() : 70
    cmds.add(zwave.configurationV4.configurationSet(parameterNumber: 1, scaledConfigurationValue: interval, size: 1))
    cmds.add(zwave.configurationV4.configurationGet(parameterNumber: 1))
    sendCmds(cmds)
}

void updated() {
    if (logEnable) log.debug "Updated preferences"

    log.warn "Debug logging is ${logEnable}"
    log.warn "Description logging is ${txtEnable}"

    runIn(1, configure)
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

void zwaveEvent(hubitat.zwave.commands.batteryv2.BatteryReport cmd) {
    if (logEnable) log.debug "BatteryReport: ${cmd}"

    if (cmd.batteryLevel == 0xFF) {
        logEvent("battery", "0", "%", "Battery is critically low", true)
    }
    else {
        logEvent("battery", "${cmd.batteryLevel}", "%", "Battery is ${cmd.batteryLevel}%")
    }
}

void zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd) {
    if (logEnable) log.debug "NotificationReport: ${cmd}"

    if (cmd.notificationType == 8) {
        // Power management notification
        switch (cmd.event) {
            case 1:
                log.info "Power on"
                break
            case 2:
                logEvent("powerSource", "battery", null, "On battery", true)
                break
            case 3:
                logEvent("powerSource", "mains", null, "On mains")
                break
            case 5:
                log.warn "Voltage drop/drift"
                break
             default:
                if (cmd.event) log.warn "Unhandled power notifcation event: ${cmd.event}"
                return
        }
    }
    else if (cmd.notificationType == 9) {
        // System notification
        if (cmd.event == 4) {
            // System Software Failure
            value = cmd.eventParameter[0]
            if (value) {
                switch (value) {
                    case 0x55:
                        log.warn "Watchdog"
                        break
                    case 0xA9:
                        log.warn "Software fault (SDK)"
                        break
                    case 0xAA:
                        log.warn "Software fault (Ring)"
                        break
                    case 0xAB:
                        log.warn "Pin reset"
                        break
                    case 0xAC:
                        log.warn "Software reset"
                        break
                    case 0xAD:
                        log.warn "Dropped frame"
                        break
                    default:
                        log.warn "Unknown system software failure (${value})"
                }
            }
        }
        else if (cmd.event == 5) {
            // Heartbeat notification
            log.info "Heartbeat (button)"
        }
        else {
            log.info "Unknown system notification (${cmd.event})"
        }
    }
    else {
        log.warn "Unhandled NotificationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Manufacturer Specific Report: ${cmd}"

    switch (cmd.deviceIdType) {
        case 1:
            String serialNumber = ""
            if (cmd.deviceIdDataFormat == 1) {
                // Data format binary
                cmd.deviceIdData.each { c ->
                    serialNumber += hubitat.helper.HexUtils.integerToHexString(c & 0xff, 1).padLeft(2, '0')
                }
            }
            else {
                cmd.deviceIdData.each { c ->
                    serialNumber += (char) c
                }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break

        default:
            log.warn "Unhandled Manufacturer Specific Report: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
    if (logEnable) log.debug "ConfigurationReport: ${cmd}"

    switch (cmd.parameterNumber) {
        case 1: // Heartbeat interval
            state.heartbeatInterval = cmd.configurationValue[0]
            break
        default:
            log.warn "Unknown Configuration Report Received ConfigurationReport: ${cmd}"
    }
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
