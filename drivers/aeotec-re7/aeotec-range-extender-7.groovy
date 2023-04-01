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
// Version 1.0.0    Initial release
// Version 1.0.1    Fix incorrect event name when no range test has been run
// Version 1.1.0    Don't indicate range test as in progress until the device
//                  responds. Provide feedback on the count of frames received
//                  as an attribute.
// Version 1.2.0    Power level set is a temporary setting for testing and
//                  requires a timeout value.
// Version 1.2.1    Clarify that node for range test needs to be decimal
// Version 1.3.0    Use zwaveSecureEncap method introduced in Hubitat 2.2.3.
// Version 1.3.1    Mark seconds as a required input for power test
// Version 1.4.0    Normalize logging
// Version 2.0.0    Code restructure and cleanup
//

// Supported Z-Wave Classes:
//
//     0x55 COMMAND_CLASS_TRANSPORT_SERVICE_V2
//     0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
//     0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
//     0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//     0x6C COMMAND_CLASS_SUPERVISION_V1
//     0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//     0x73 COMMAND_CLASS_POWERLEVEL
//     0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
//     0x85 COMMAND_CLASS_ASSOCIATION_V2
//     0x86 COMMAND_CLASS_VERSION_V3
//     0x87 COMMAND_CLASS_INDICATOR_V3
//     0x8E COMMAND_CLASS_MULTICHANNEL_ASSOCIATION_V3
//     0x9F COMMAND_CLASS_SECURITY_2

import groovy.transform.Field

@Field static final Map commandClassVersions = [0x73:1, 0x86:3, 0x87:3]

@Field static final List powerLevels = ["normal", "-1dBm", "-2dBm", "-3dBm", "-4dBm", "-5dBm", "-6dBm", "-7dBm", "-8dBm", "-9dBm"]

metadata
{
    definition(
        name: "Aeotec Range Extender 7", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/aeotec-re7/aeotec-range-extender-7.groovy"
    )
    {
        capability "Configuration"
        capability "Refresh"

        attribute "indicator", "string"
        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"

        command "powerTest", [[name: "seconds*", type: "NUMBER", defaultValue: "0",
                               description: "Seconds before returning to normal power"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against (decimal)"],
                              [name: "power", type: "ENUM", constraints: powerLevels]]

        fingerprint mfr:"0371", prod:"0104", deviceId:"00BD",
            inClusters: "0x5E,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A"
    }
}

preferences
{
    input name: "indicator", title: "Indicator light",
        type: "bool", defaultValue: "0"

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
    runIn(1800, logsOff)
}

void refresh() {
    if (logEnable) log.debug "Refresh"

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.indicatorV3.indicatorGet())
    cmds.add(zwave.versionV3.versionGet())
    cmds.add(zwave.powerlevelV1.powerlevelGet())
    cmds.add(zwave.powerlevelV1.powerlevelTestNodeGet())
    sendCmds(cmds)
}

void configure() {
    if (logEnable) log.debug "Configure"

    Integer indicatorValue = indicator ? 0xFF : 0

    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.indicatorV3.indicatorSet(value: indicatorValue))
    cmds.add(zwave.indicatorV3.indicatorGet())
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

void zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd) {
    if (logEnable) log.debug "IndicatorReport: ${cmd}"

    String status = cmd.value ? "on" : "off"
    logEvent("indicator", status, null, "Indicator light is ${status}")
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

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "Unhandled cmd: ${cmd}"
}
