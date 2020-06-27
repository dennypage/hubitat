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
// This driver recognizes the Static Controller Node of the ZRTSII
//
// The Static Controller driver doesn't offer any usable functions.
// All usable functions are in the Virtual Node driver.
//
// Version 1.0.0    Initial release
// Version 1.1.0    Unhandled events logged as warnings
//

metadata
{
    definition (
        name: "Somfy ZRTSII Controller", namespace: "cococafe", author: "Denny Page"
    )
    {
        fingerprint mfr: "0047", prod: "5A52", deviceId: "5400", inClusters: "0x86,0x72"

        // All command classes are version 1
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC
        // 0x86 COMMAND_CLASS_VERSION
    }
}

preferences
{
}

// Effectively unused...
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

def zwaveEvent(hubitat.zwave.Command cmd)
{
    log.warn "Unhandled cmd: ${cmd.toString()}"
    return null
}

