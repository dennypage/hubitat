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
//

metadata
{
    definition (
        name: "WeeWX", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "Configuration"

        attribute "temperature", "number"
        attribute "humidity", "number"

        attribute "windSpeed", "number"
        attribute "windDirection", "number"
        attribute "windGustSpeed", "number"
        attribute "windGustDirection", "number"

        attribute "apptemp", "number"
        attribute "heatindex", "number"
        attribute "humidex", "number"
        attribute "windchill", "number"
        
        attribute "rain", "number"
        attribute "rainRate", "number"
        attribute "hourRain", "number"
        attribute "dayRain", "number"
        attribute "rain24", "number"

        attribute "barometer", "number"
        attribute "dewpoint", "number"
        attribute "cloudbase", "number"

        attribute "uv", "number"
        attribute "radiation", "number"
        attribute "THSW", "number"

        attribute "usUnits", "number"
    }
}

preferences
{
    input name: "weewxIpAddress", title: "IPv4 address of WeeWX server", type: "string"
    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
}
  
def configure()
{
    state.hubURL = "http://${device.hub.getDataValue("localIP")}:${device.hub.getDataValue("localSrvPortTCP")}"
    log.info "Hub URL is ${state.hubURL}"
}
  
void logsOff()
{
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "debug logging disabled"
}

void installed()
{
    configure()
    runIn(1800, logsOff)
}

void updated()
{
    if (logEnable) log.debug "Updated preferences"
    log.warn "debug logging is ${logEnable}"
        
    boolean validIPv4 = weewxIpAddress.matches("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")
    if (! validIPv4)
    {
        log.error "Invalid IPv4 address for WeeWX server"
        return
    }

    weewxNetID = weewxIpAddress.tokenize( '.' ).collect { String.format('%02X', it.toInteger()) }.join()
    log.info "WeeWX Device Network ID is ${weewxNetID}"
    device.setDeviceNetworkId("${weewxNetID}")

    configure()    
}

void parse(String description)
{
    if (logEnable) log.debug "parse: ${description}"

    def msg = parseLanMessage(description)
    if (logEnable) log.debug "msg: ${msg}"

    json = parseJson(msg.body)
    if (json)
    {
        json.each
        {
            key, value -> sendEvent(name: "${key}", value: "${value}")
        }
    }
}

