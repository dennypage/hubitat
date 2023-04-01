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
// This driver recognizes the Static Controller Node of the ZRTSII
//
// The Static Controller driver doesn't offer any usable functions.
// All usable functions are in the Virtual Node driver.
//
// Version 1.0.0    Initial release
// Version 1.1.0    Unhandled events logged as warnings
// Version 2.0.0    Add a refresh capability (node ping)
// Version 2.0.1    Don't log on node ping
// Version 3.0.0    Code restructure and cleanup
//

// Supported Z-Wave Classes:
//
//     0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC
//     0x86 COMMAND_CLASS_VERSION

import groovy.transform.Field

metadata
{
    definition(
        name: "Somfy ZRTSII Controller", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/zrtsii/somfy-rtsii-controller.groovy",
    )
    {
        capability "Refresh"

        fingerprint mfr: "0047", prod: "5A52", deviceId: "5400", inClusters: "0x86,0x72"
    }
}

@Field static final Map commandClassVersions = [0x72:1, 0x86:1]

preferences
{
    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
}

void refresh() {
    List<hubitat.zwave.Command> cmds = []

    cmds.add(zwave.versionV1.versionGet())
    cmds.add(zwave.manufacturerSpecificV1.manufacturerSpecificGet())
    sendCmds(cmds)
}

void sendCmds(List<hubitat.zwave.Command> cmds, Long interval = 200) {
    sendHubCommand(new hubitat.device.HubMultiAction(delayBetween(cmds*.format(), interval), hubitat.device.Protocol.ZWAVE))
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

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "Unhandled cmd: ${cmd}"
}
