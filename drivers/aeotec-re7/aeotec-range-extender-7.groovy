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
// Version 1.0.1    Fix incorrect event name when no range test has been run
// Version 1.1.0    Don't indicate range test as in progress until the device
//                  responds. Provide feedback on the count of frames received
//                  as an attribute.
//

metadata
{
    definition (
        name: "Aeotec Range Extender 7", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Configuration"
        capability "Refresh"

        attribute "indicator", "string"
        attribute "powerLevel", "string"
        attribute "rangeTest", "string"
        attribute "rangeTestReceived", "string"

        command "rangeTest", [[name: "node*", type: "NUMBER", description: "Node to test against"],
                              [name: "power", type: "ENUM", constraints: ["normal",
                                                          "-1dBm", "-2dBm", "-3dBm",
                                                          "-4dBm", "-5dBm", "-6dBm",
                                                          "-7dBm", "-8dBm", "-9dBm"]]]

        fingerprint mfr:"0371", prod:"0104", deviceId:"00BD",
            inClusters: "0x5E,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x87,0x73,0x9F,0x6C,0x7A"


        // 0x55 COMMAND_CLASS_TRANSPORT_SERVICE_V2
        // 0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
        // 0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
        // 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
        // 0x6C COMMAND_CLASS_SUPERVISION_V1
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
        // 0x73 COMMAND_CLASS_POWERLEVEL
        // 0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
        // 0x85 COMMAND_CLASS_ASSOCIATION_V2
        // 0x86 COMMAND_CLASS_VERSION_V3
        // 0x87 COMMAND_CLASS_INDICATOR_V3
        // 0x8E COMMAND_CLASS_MULTICHANNEL_ASSOCIATION_V3
        // 0x9F COMMAND_CLASS_SECURITY_2
    }
}

preferences
{
    input name: "indicator", title: "Indicator light",
        type: "bool", defaultValue: "0"

    input name: "powerLevel", title: "Transmit power level",
        type: "enum", defaultValue: "0", options: [[0: "normal [default]"],
                                                   [1: "-1dBm"], [2: "-2dBm"], [3: "-3dBm"],
                                                   [4: "-4dBm"], [5: "-5dBm"], [6: "-6dBm"],
                                                   [7: "-7dBm"], [8: "-8dBm"], [9: "-9dBm"]]

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
    runIn(1800, logsOff)
}

def refresh()
{
    if (logEnable) log.debug "Refresh"

    def cmds = []
    cmds.add(secureCmd(zwave.indicatorV3.indicatorGet()))
    cmds.add(secureCmd(zwave.versionV3.versionGet()))
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelGet()))
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelTestNodeGet()))
    delayBetween(cmds, 200)
}

def configure()
{
    if (logEnable) log.debug "Configure"

    Integer indicatorValue = indicator ? 0xFF : 0
    Integer powerValue = powerLevel ? powerLevel.toInteger() : 0

    def power = powerLevelToString(powerValue)
    log.info "setting power value to ${power} (may have no effect)"

    def cmds = []
    // NB: Setting power value may have no effect.
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelSet(powerLevel: powerValue)))
    cmds.add(secureCmd(zwave.indicatorV3.indicatorSet(value: indicatorValue)))
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelGet()))
    cmds.add(secureCmd(zwave.indicatorV3.indicatorGet()))
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
    map.name = "rangeTest"
    map.value = "pending"
    if (txtEnable) map.descriptionText = "sending ${frames} frames to node ${node} at power level ${powerString}"
    sendEvent(map)
    log.info "Range test pending with node ${node}: sending ${frames} frames at power level ${powerString}"

    def cmds = []
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelTestNodeSet(powerLevel: power,
                                                                testFrameCount: frames,
                                                                testNodeid: node)))
    cmds.add(secureCmd(zwave.powerlevelV1.powerlevelTestNodeGet()))
    delayBetween(cmds, 100)
}

def requestTestNode()
{
    secureCmd(zwave.powerlevelV1.powerlevelTestNodeGet())
}

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

void zwaveEvent(hubitat.zwave.commands.indicatorv3.IndicatorReport cmd)
{
    if (logEnable) log.debug "IndicatorReport: ${cmd.toString()}"

    String status = cmd.value ? "on" : "off"
    log.info "indicator ${status}"

    def map = [:]
    map.name = "indicator"
    map.value = "${status}"
    if (txtEnable) map.descriptionText = "indicator light is ${status}"
    sendEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.powerlevelv1.PowerlevelReport cmd)
{
    if (logEnable) log.debug "PowerLevelReport: ${cmd.toString()}"

    power = powerLevelToString(cmd.powerLevel)
    log.info "Power level ${power}, timeout ${cmd.timeout}"

    def map = [:]
    map.name = "powerLevel"
    map.value = "${power}"
    if (txtEnable) map.descriptionText = "transmit power level"
    sendEvent(map)
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
    log.info "Range test ${status} with node ${cmd.testNodeid}: ${cmd.testFrameCount} frames received"

    def map = [:]
    map.name = "rangeTest"
    map.value = "${status}"
    sendEvent(map)

    map.name = "rangeTestReceived"
    map.value = "${cmd.testFrameCount}"
    if (txtEnable && !inProgress) map.descriptionText = "received ${cmd.testFrameCount} frames from node ${cmd.testNodeid}"
    sendEvent(map)

    if (inProgress)
    {
        runIn(1, requestTestNode)
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd)
{
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    encapCmd = cmd.encapsulatedCommand()
    if (encapCmd)
    {
        return zwaveEvent(encapCmd)
    }

    log.warn "Unable to extract encapsulated cmd: ${cmd.toString()}"
    return null
}

def zwaveEvent(hubitat.zwave.Command cmd)
{
    if (logEnable) log.debug "Unhandled cmd: ${cmd.toString()}"
    return null
}

private secureCmd(cmd) {
    if (getDataValue("zwaveSecurePairingComplete") == "true" && 0)
    {
        if (logEnable) log.debug "Secure"

        return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    }
    else
    {
        return cmd.format()
    }
}
