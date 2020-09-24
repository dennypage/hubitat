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
// Version 0.0.1    Testing
//

// MIMO2+ endpoints
// Endpoint 1:    SIG1 (input 1)
// Endpoint 2:    SIG2 (input 2)
// Endpoint 3:    Relay 1
// Endpoint 4:    Relay 2


metadata
{
    definition (
        name: "Fortrezz MIMO2+", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Refresh"
        capability "Configuration"

        command "relayOneOn"
        command "relayOneOff"

        command "relayTwoOn"
        command "relayTwoOff"

        fingerprint mfr: "0084", prod: "0463", deviceId: "0208",
            inClusters: "0x5E,0x85,0x86,0x8E,0x73,0x20,0x71,0x25,0x31,0x70,0x60,0x7A,0x72,0x5A,0x59,0x98"

        // inClusters: 0x5E,0x86,0x72,0x5A,0x59,0x98
        // secureInClusters: 0x85,0x8E,0x73,0x20,0x71,0x25,0x31,0x70,0x60,0x7A

        // 0x20 COMMAND_CLASS_BASIC
        // 0x25 COMMAND_CLASS_SWITCH_BINARY
        // 0x31 COMMAND_CLASS_SENSOR_MULTILEVEL_V9
        // 0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
        // 0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
        // 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
        // 0x60 COMMAND_CLASS_MULTI_CHANNEL_V4
        // 0x70 COMMAND_CLASS_CONFIGURATION
        // 0x71 COMMAND_CLASS_ALARM_V2
        // 0x71 COMMAND_CLASS_NOTIFICATION_V4
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
        // 0x73 COMMAND_CLASS_POWERLEVEL
        // 0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
        // 0x85 COMMAND_CLASS_ASSOCIATION_V2
        // 0x86 COMMAND_CLASS_VERSION_V2
        // 0x8E COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V3
        // 0x98 COMMAND_CLASS_SECURITY
    }
}

preferences
{
    // REL1: Parameter 1, default 0x05
    //        Bits 7-5 relay mapping:
    //            b7: trigger requires both SIG1 and SIG2
    //            b6: enable trigger by SIG2
    //            b5: enable trigger by SIG1
    //        Bits 4-0 automatic relay release time in 100ms increments (0 to disable)
    //
    input name: "rel1", title: "Relay 1 configuration",
        description: "see manual for bit values",
        type: "number", defaultValue: "5", range: "0..255"

    // REL2: Parameter 2, default 0x05
    //       (see REL1 for details)
    input name: "rel1", title: "Relay 2 configuration",
        description: "see manual for bit values",
        type: "number", defaultValue: "5", range: "0..255"

    // SIG1: Parameter 3, default 0xA8
    input name: "sig1multilevel", title: "Signal 1 Multilevel Trigger",
        description: "see manual for bit values",
        type: "number", defaultValue: "168", range: "0..255"

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
}

void updated()
{
    if (logEnable) log.debug "Updated preferences"

    // FIXME

    log.warn "debug logging is ${logEnable}"
    log.warn "description logging is ${txtEnable}"
}

def relayOneOn()
{
    log.debug "Relay one on"
    def cmds = []
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.basicV1.basicSet(value: 0xff), 3)))
    delayBetween(cmds, 200)
}

def relayOneOff()
{
    log.debug "Relay one off"
    def cmds = []
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.basicV1.basicSet(value: 0x00), 3)))
    delayBetween(cmds, 200)
}

def relayTwoOn()
{
    log.debug "Relay two on"
    def cmds = []
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.basicV1.basicSet(value: 0xff), 4)))
    delayBetween(cmds, 200)
}

def relayTwoOff()
{
    log.debug "Relay two off"
    def cmds = []
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.basicV1.basicSet(value: 0x00), 4)))
    delayBetween(cmds, 200)
}


def configure()
{
    if (logEnable) log.debug "Configure"

}

def refresh()
{
    if (logEnable) log.debug "Refresh"

    def cmds = []
    cmds.add(zwaveSecureEncap(zwave.versionV3.versionGet()))

    // Relay configs
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 1)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 2)))
    // Signal 1 configs
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 3)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 4)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 5)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 6)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 7)))
    // Signal 2 configs
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 9)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 10)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 11)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 13)))
    cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 14)))
    // Signal status
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.sensorMultilevelV9.sensorMultilevelGet(sensorType: 1), 1)))
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.sensorMultilevelV9.sensorMultilevelGet(sensorType: 1), 2)))
    // Relay status
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.switchBinaryV1.switchBinaryGet(), 3)))
    cmds.add(zwaveSecureEncap(endpointCmd(zwave.switchBinaryV1.switchBinaryGet(), 4)))
    delayBetween(cmds, 200)
}

def parse(String description)
{
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd)
    {
        return zwaveEvent(cmd)
    }

    log.warn "Non Z-Wave parse event: ${description}"
    return null
}

def zwaveEvent(int endPoint, hubitat.zwave.commands.switchbinaryv2.SwitchBinaryReport cmd)
{
    // FIXME

    log.warn "Unhandled SwitchBinaryReport from endpoint ${endPoint}: ${cmd.toString()}"
}

def zwaveEvent(int endPoint, hubitat.zwave.commands.sensormultilevelv9.SensorMultilevelReport cmd)
{
    // FIXME

    log.warn "Unhandled SensorMultilevelReport from endpoint ${endPoint}: ${cmd.toString()}"
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd)
{
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)
{
    if (logEnable) log.debug "ConfigurationReport: ${cmd.toString()}"

    switch (cmd.parameterNumber)
    {
        case 1:
            state.rel1Config = cmd.configurationValue[0]
            break
        case 2:
            state.rel2Config = cmd.configurationValue[0]
            break
        case 3:
            state.sig1MultiTriggerSettings = cmd.configurationValue[0]
            break
        case 4:
            state.sig1LowerThresholdHigh = cmd.configurationValue[0]
            break
        case 5:
            state.sig1LowerThresholdLow = cmd.configurationValue[0]
            break
        case 6:
            state.sig1UpperThresholdHigh = cmd.configurationValue[0]
            break
        case 7:
            state.sig1UpperThresholdLow = cmd.configurationValue[0]
            break
        case 9:
            state.sig2MultiTriggerSettings = cmd.configurationValue[0]
            break
        case 10:
            state.sig2LowerThresholdHigh = cmd.configurationValue[0]
            break
        case 11:
            state.sig2LowerThresholdLow = cmd.configurationValue[0]
            break
        case 12:
            state.sig2UpperThresholdHigh = cmd.configurationValue[0]
            break
        case 13:
            state.sig2UpperThresholdLow = cmd.configurationValue[0]
            break
        default:
            if (logEnable) log.debug "Unknown Configuration Report Received ConfigurationReport: ${cmd.toString()}"
    }
}


def zwaveEvent(hubitat.zwave.commands.multichannelv4.MultiChannelCmdEncap cmd)
{
    def encapCmd = cmd.encapsulatedCommand()
    log.debug ("Encapsulated endpoint ${cmd.sourceEndPoint}: ${encapCmd}")

    if (encapCmd)
    {
        return zwaveEvent(cmd.sourceEndPoint, encapCmd)
    }

    log.warn "Unable to extract multichannel encapsulated cmd: ${cmd.toString()}"
    return null
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    encapCmd = cmd.encapsulatedCommand()
    if (encapCmd)
    {
        return zwaveEvent(encapCmd)
    }

    log.warn "Unable to extract security encapsulated cmd: ${cmd.toString()}"
    return null
}

def zwaveEvent(hubitat.zwave.Command cmd)
{
    log.warn "Unhandled cmd: ${cmd.toString()}"
    return null
}

private endpointCmd(cmd, endpoint)
{
    zwave.multiChannelV3.multiChannelCmdEncap(bitAddress: false, sourceEndPoint:0, destinationEndPoint: endpoint).encapsulate(cmd)
}
