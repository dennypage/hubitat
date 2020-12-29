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
// Version 1.0.0    Initial release
// Version 1.1.0    Clean up log messages. Handle button push.
// Version 1.1.1    Mark seconds as a required input for power test.
// Version 1.2.0    Normalize logging
// Version 1.2.1    Minor capitalization fix
//

metadata
{
    definition (
        name: "Ring Alarm Range Extender gen2", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Configuration"
        capability "Refresh"
        capability "Battery"
        capability "PowerSource"

        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"

        command "powerTest", [[name: "seconds*", type: "NUMBER", defaultValue: "0",
                               description: "Seconds before returning to normal power"],
                              [name: "power", type: "ENUM", constraints: ["normal",
                                                          "-1dBm", "-2dBm", "-3dBm",
                                                          "-4dBm", "-5dBm", "-6dBm",
                                                          "-7dBm", "-8dBm", "-9dBm"]]]

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against (decimal)"],
                              [name: "power", type: "ENUM", constraints: ["normal",
                                                          "-1dBm", "-2dBm", "-3dBm",
                                                          "-4dBm", "-5dBm", "-6dBm",
                                                          "-7dBm", "-8dBm", "-9dBm"]]]

        fingerprint mfr:"0346", prod:"0401", deviceId:"0301",
            inClusters:"0x5E,0x59,0x85,0x80,0x70,0x5A,0x7A,0x87,0x72,0x8E,0x71,0x73,0x9F,0x6C,0x55,0x86",
            deviceJoinName: "Ring Alarm Range Extender gen2"

        // 0x55 Transport Service V2
        // 0x59 Association Group Information V3
        // 0x5A Device Reset Locally V1
        // 0x5E Z-Wave Plus Info V2
        // 0x6C Supervision V1
        // 0x70 Configuration V4
        // 0x71 Notification V8
        // 0x72 Manufacturer V2
        // 0x73 Powerlevel V1
        // 0x7A Firmware Update Meta Data V5
        // 0x80 Battery V2
        // 0x85 Association V2
        // 0x86 Version V3
        // 0x87 Indicator V3
        // 0x8E Multi-Channel Association V3
        // 0x9F Security 2 V1
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

void logsOff()
{
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "debug logging disabled"
}

void installed()
{
    runIn(1, refresh)
    runIn(1800, logsOff)
}

def refresh()
{
    if (logEnable) log.debug "Refresh"

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.notificationV8.notificationGet(notificationType: 8, v1AlarmType: 0, event: 2)))
    cmds.add(zwaveSecureEncap(zwave.notificationV8.notificationGet(notificationType: 8, v1AlarmType: 0, event: 3)))
    cmds.add(zwaveSecureEncap(zwave.batteryV1.batteryGet()))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet()))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet()))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 1)))
    delayBetween(cmds, 200)
}

def configure()
{
    if (logEnable) log.debug "Configure"

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.versionV3.versionGet()))
    cmds.add(zwaveSecureEncap(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1)))

    def interval = heartbeatInterval ? heartbeatInterval.toInteger() : 70
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(parameterNumber: 1, scaledConfigurationValue: interval, size: 1)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 1)))
    delayBetween(cmds, 200)
}

def updated()
{
    if (logEnable) log.debug "Updated preferences"

    log.warn "debug logging is ${logEnable}"
    log.warn "description logging is ${txtEnable}"

    runIn(1, configure)
}

static String powerLevelToString(Number power)
{
    return power ? "-${power}dBm" : "normal"
}

static Integer stringToPowerLevel(String string)
{
    def match = (string =~ /-([0-9]+)dBm/)
    if (match.find()) return match.group(1).toInteger()
    return 0
}

def powerTest(Number seconds, String powerString)
{
    if (seconds < 0 || seconds > 255)
    {
        log.error "Invalid powerTest seconds ${seconds}"
        return null
    }

    def power = stringToPowerLevel(powerString)

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelSet(powerLevel: power, timeout: seconds)))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet()))
    delayBetween(cmds, 200)
}

def rangeTest(Number node, String powerString)
{
    if (node < 1)
    {
        log.error "Invalid test node ${node}"
        return null
    }

    def power = stringToPowerLevel(powerString)
    def frames = testFrames ? testFrames.toInteger() : 10

    def map = [:]
    map.name = "rangeTestReceived"
    map.value = "0"
    sendEvent(map)

    map.name = "rangeTest"
    map.value = "pending"
    map.descriptionText = "${device.displayName}: range test pending - sending ${frames} frames to node ${node} at power level ${powerString}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeSet(powerLevel: power,
                                                                testFrameCount: frames,
                                                                testNodeid: node)))
    cmds.add(zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet()))
    delayBetween(cmds, 100)
}

def requestPowerLevel()
{
    zwaveSecureEncap(zwave.powerlevelV1.powerlevelGet())
}

def requestTestNode()
{
    zwaveSecureEncap(zwave.powerlevelV1.powerlevelTestNodeGet())
}

def parse(String description)
{
    if (logEnable) log.debug "parse: ${description}"

    hubitat.zwave.Command cmd = zwave.parse(description,commandClassVersions)
    if (cmd)
    {
        return zwaveEvent(cmd)
    }

    if (logEnable) log.debug "Non Z-Wave parse event: ${description}"
    return null
}

def zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelReport cmd)
{
    unschedule(requestPowerLevel)

    if (logEnable) log.debug "PowerLevelReport: ${cmd.toString()}"

    power = powerLevelToString(cmd.powerLevel)
    def map = [:]
    map.name = "powerLevel"
    map.value = "${power}"
    map.descriptionText = "${device.displayName}: transmit power level is ${power}, timeout ${cmd.timeout} seconds"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    if (cmd.timeout)
    {
        runIn(cmd.timeout, requestPowerLevel)
    }
}

def zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelTestNodeReport cmd)
{
    unschedule(requestTestNode)

    if (logEnable) log.debug "PowerLevelTestNodeReport: ${cmd.toString()}"

    // Check test validity
    if (cmd.testNodeid == 0)
    {
        sendEvent(name: "rangeTest", value: "none")
        return
    }

    def Boolean inProgress = false
    switch (cmd.statusOfOperation)
    {
        case 0:    // ZW_TEST_FAILED
            status = "failed"
            break
        case 1:    // ZW_TEST_SUCCES
            status = "succeeded"
            break
        case 2:    // ZW_TEST_INPROGRESS
            inProgress = true
            status = "in progress"
            break
    }

    def map = [:]
    map.name = "rangeTest"
    map.value = "${status}"
    map.descriptionText = "${device.displayName}: range test ${status}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    map.name = "rangeTestReceived"
    map.value = "${cmd.testFrameCount}"
    map.descriptionText = "${device.displayName}: received ${cmd.testFrameCount} frames from node ${cmd.testNodeid}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"

    if (inProgress)
    {
        runIn(2, requestTestNode)
    }
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd)
{
    def map = [:]

    map.name = "battery"
    map.unit = "%"
    if (cmd.batteryLevel == 0xFF)
    {
        map.value = 0
        map.descriptionText = "${device.displayName}: battery is critically low"
        sendEvent(map)
        log.warn "${map.descriptionText}"
    }
    else
    {
        map.value = cmd.batteryLevel
        map.descriptionText = "${device.displayName}: battery is ${map.value}${map.unit}"
        sendEvent(map)
        if (txtEnable) log.info "${map.descriptionText}"
    }
}

def zwaveEvent(hubitat.zwave.commands.notificationv8.NotificationReport cmd)
{
    if (logEnable) log.debug "NotificationReport: ${cmd.toString()}"

    def map = [:]

    if (cmd.notificationType == 8)
    {
        switch (cmd.event)
        {
            case 1:
                log.info "${device.displayName}: power on"
                break
            case 2:
                map.name = "powerSource"
                map.value = "battery"
                map.descriptionText = "${device.displayName}: on battery"
                sendEvent(map)
                log.warn "${map.descriptionText}"
                break
            case 3:
                map.name = "powerSource"
                map.value = "mains"
                map.descriptionText = "${device.displayName}: on mains"
                sendEvent(map)
            log.info "${map.descriptionText}"
                break
            case 5:
                log.warn "${device.displayName}: voltage drop/drift"
                break
             default:
                if (cmd.event) log.warn "${device.displayName}: unhandled power notifcation event: ${cmd.event}"
                return null
        }

    }
    else if (cmd.notificationType == 9 && cmd.event == 4)
    {
        value = cmd.eventParameter[0]
        if (value)
        {
            def desc = ""
            switch (value)
            {
                case 0x55:
                    desc = "watchdog"
                    break
                case 0xA9:
                    desc = "software fault (SDK)"
                    break
                case 0xAA:
                    desc = "software fault (Ring)"
                    break
                case 0xAB:
                    desc = "pin reset"
                    break
                case 0xAC:
                    desc = "software reset"
                    break
                case 0xAD:
                    desc = "dropped frame"
                    break
                default:
                    desc = "unknown (${value})"
            }

            log.warn "${device.displayName}: ${desc}"
        }
    }
    else if (cmd.notificationType == 9 && cmd.event == 5)
    {
        log.info "${device.displayName}: heartbeat (button)"
    }
    else
    {
        log.warn "Unhandled NotificationReport: ${cmd.toString()}"
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd)
{
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd)
{
    if (logEnable) log.debug "Manufacturer Specific Report: ${cmd}"

    switch (cmd.deviceIdType)
    {
        case 1:
            def serialNumber = ""
            if (cmd.deviceIdDataFormat == 1)
            {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff, 1).padLeft(2, '0')}
            }
            else
            {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break

        default:
            log.warn "Unhandled Manufacturer Specific Report: ${cmd}"
    }
}

    def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)
{
    if (logEnable) log.debug "ConfigurationReport: ${cmd.toString()}"

    switch (cmd.parameterNumber)
    {
        case 1: // Heartbeat interval
            state.heartbeatInterval = cmd.configurationValue[0]
            break
        default:
            if (logEnable) log.debug "Unknown Configuration Report Received ConfigurationReport: ${cmd.toString()}"
    }
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd)
{
    encapCmd = cmd.encapsulatedCommand()
    if (encapCmd)
    {
        zwaveEvent(encapCmd)
    }
}

def zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd)
{
    if (logEnable) log.debug "SupervisionGet: ${cmd.toString()}"

    encapCmd = cmd.encapsulatedCommand()
    if (encapCmd)
    {
        zwaveEvent(encapCmd)
    }

    sendHubCommand(new hubitat.device.HubAction(zwaveSecureEncap(zwave.supervisionV1.supervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0)), hubitat.device.Protocol.ZWAVE))
}

def zwaveEvent(hubitat.zwave.Command cmd)
{
    if (logEnable) log.debug "Unhandled cmd: ${cmd.toString()}"
    return null
}
