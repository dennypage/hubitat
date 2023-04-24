/* groovylint-disable LineLength, MethodSize, UnnecessaryGString */

/*
 * Copyright (c) 2023, Denny Page
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Network UPS Tools (NUT) monitor and shutdown controller (upsmon) for Hubitat
 *
 * Version 1.0.0    Initial release
 * Version 1.1.0    Use 0 for unknown numerical values rather than a string
 *                  Set the unit for numerical values
 */

metadata {
    definition(
        name: "NUT upsmon driver", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/nut-upsmon/nut-upsmon.groovy",
        singleThreaded: true
    )
    {
        capability "Initialize"
        capability "Refresh"
        capability "Battery"

        attribute "status",   "String"
        attribute "load",     "number"
        attribute "battery'", "number"
        attribute "runtime",  "number"
    }
}

import groovy.transform.Field

// Variable name -> attribute map
@Field static final Map<String,Map> variableMap = [
    'battery.charge':  [name: 'battery', unit: '%', unknownValue: '0'],
    'battery.runtime': [name: 'runtime', unit: 's', unknownValue: '0'],
    'ups.load':        [name: 'load',    unit: '%', unknownValue: '0'],
    'ups.status':      [name: 'status',  unit: '',  unknownValue: 'unknown']
]
@Field static final String statusName = 'status'
@Field static final String statusShutdownRequested = 'Shutdown Requested'

// UPSD statword -> status map
//   There are additional statwords that might be seen,
//   but this is the list that upsmon cares about.
@Field static final Map<String,String> statwordMap = [
    'OL':      'Online',
    'OB':      'On Battery',
    'LB':      'Low Battery',
    'RB':      'Battery Needs Replaced',
    'CAL':     'Runtime Calibration',
    'FSD':     'Forced Shutdown'
]
@Field static final String statwordOL = 'OL'
@Field static final String statwordOB = 'OB'
@Field static final String statwordLB = 'LB'
@Field static final String statwordFSD = 'FSD'

// UPSD error -> status map
@Field static final Map<String,String> errorMap = [
    'ACCESS-DENIED': 'Access denied',
    'UNKNOWN-UPS': 'Unknown ups',
    'DATA-STALE': 'Stale data',
    'DRIVER-NOT-CONNECTED': 'Driver not connected'
]
@Field static final String errAccessDenied = 'ACCESS-DENIED'
@Field static final String errUnknownUps = 'UNKNOWN-UPS'
@Field static final String errDataStale = 'DATA-STALE'
@Field static final String errDriverNotConnected = 'DRIVER-NOT-CONNECTED'
@Field static final String errVarNotSupported = 'VAR-NOT-SUPPORTED'

preferences {
    input name: "serverHost", type: "text", title: "NUT server host", required: true
    input name: "serverPort", type: "number", title: "NUT server port", defaultValue: 3493, range: "1..65535", required: true
    input name: "upsName", type: "text", title: "UPS name", required: true
    input name: "username", type: "text", title: "NUT username", required: true
    input name: "password", type: "password", title: "NUT password", required: true
    input name: "pollFreq", type: "number", title: "Polling frequency", defaultValue: 5, range: "1..30", required: true

    input name: "shutdownEnable", title: "<b>Enable Hub shutdown</b>", description: "Note that if you do not enable this, the hub will not actually shut down when the ups battery runs out", type: "bool", defaultValue: false
    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: false
}

void installed() {
    variableMap.each { variable, attribute ->
        sendEvent(name: attribute.name, unit: attribute.unit, value: attribute.unknownValue)
    }
}

void uninstalled() {
    upsdDisconnect()
}

void updated() {
    log.info("updated: ups ${upsName} on host ${serverHost}:${serverPort}")
    upsdDisconnect()
    runIn(1, upsdConnect)
}

void initialize() {
    upsdDisconnect()
    runIn(1, upsdConnect)
}

void refresh() {
    upsdPoll()
}

void upsdConnect() {
    if (logEnable) {
        log.debug("attempting to connect to upsd on ${serverHost}:${serverPort}...")
    }

    try {
        telnetConnect([termChars:[10]], serverHost, serverPort.toInteger(), null, null)
        state.upsdConnected = true
        log.info("connected to upsd on ${serverHost}:${serverPort} - monitoring ${upsName} every ${pollFreq} seconds")

        telnetSend("USERNAME ${username}")
        telnetSend("PASSWORD ${password}")
        telnetSend("LOGIN ${upsName}")

        upsdPoll()
        schedule("0/${pollFreq} * * * * ? *", upsdPoll)
    }
    catch (e) {
        log.error("telnet connect error: ${e}")
        runIn(pollFreq, upsdConnect)
    }
}

void upsdDisconnect() {
    unschedule()

    if (state.upsdConnected) {
        if (logEnable) {
            log.debug("disconnecting from upsd...")
        }

        state.upsdConnected = false
        telnetSend("LOGOUT")
        telnetClose()
        log.info("disconnected from upsd")
    }

    variableMap.each { variable, attribute ->
        sendEvent(name: attribute.name, unit: attribute.unit, value: attribute.unknownValue)
    }
}

void upsdPoll() {
    variableMap.each { variable, attribute ->
        telnetSend("GET VAR ${upsName} ${variable}")
    }
}

void telnetSend(String msg) {
    sendHubCommand(new hubitat.device.HubAction("${msg}", hubitat.device.Protocol.TELNET))
}

void telnetStatus(String message) {
    if (state.upsdConnected) {
        log.error("telnet status: ${message}")
        upsdDisconnect()
        runIn(pollFreq, upsdConnect)
    }
}

void parse(String message) {
    if (logEnable) {
        log.debug("parse: ${message}")
    }

    // If we are disconnecting, ignore the message
    if (!state.upsdConnected) {
        return
    }

    // Incoming message: VAR myups battery.runtime "1689.00"
    //   Split results:
    //     response[0]: VAR
    //     response[1]: myups
    //     response[2]: battery.runtime
    //     response[3]: 1689.00
    String[] response = message.split('"?( |\$)(?=(([^"]*"){2})*[^"]*\$)"?')
    if (response[0] == 'OK') {
        return
    }

    if (response[0] == 'ERR') {
        switch (response[1]) {
            case errVarNotSupported:
                // A variable we asked for, such as load, is not available
                break

            case errAccessDenied:
            case errUnknownUps:
                // Configuration error
                log.error("upsd: ${errorMap[response[1]]}")
                upsdDisconnect()
                runIn(pollFreq, upsdConnect)
                sendEvent(name: statusName, value: errorMap[response[1]])
                break

            case errDataStale:
            case errDriverNotConnected:
                // Connected, but data cannot be trusted
                log.warn("upsd: ${errorMap[response[1]]}")
                variableMap.each { variable, attribute ->
                    if (attribute.name != statusName) {
                        sendEvent(name: attribute.name, unit: attribute.unit, value: attribute.unknownValue)
                    }
                }
                sendEvent(name: statusName, value: errorMap[response[1]])
                break

            default:
                log.error("upsd: unexpected error message: ${response[1]}")
        }
        return
    }

    // Anything else should be a variable response
    if (response[0] != 'VAR') {
        log.error("upsd: unexpected message: ${message}")
        return
    }

    // Get the variable attribute
    Map attribute = variableMap[response[2]]
    if (attribute == null) {
        log.error("upsd: unexpected variable: ${response[2]} = ${response[3]}")
        return
    }

    // If the attribute isn't the status, process it as a simple numeric entry and return
    if (attribute.name != statusName) {
        Number n = response[3].toFloat()
        sendEvent(name: attribute.name, unit: attribute.unit, value: n)
        return
    }

    // Process status
    List statwords = response[3].tokenize(' ')

    // Should we shutdown?
    Boolean shutdown = statwords.contains(statwordFSD) || (statwords.contains(statwordOB) && statwords.contains(statwordLB))

    // Build the status string, ensuring we have a primary status up front
    String status
    if (statwords.contains(statwordOL)) {
        status = statwordMap[statwordOL]
        statwords -= statwordOL
    }
    else if (statwords.contains(statwordOB)) {
        status = statwordMap[statwordOB]
        statwords -= statwordOB
    }
    else if (statwords.contains(statwordFSD)) {
        status = statwordMap[statwordFSD]
        statwords -= statwordFSD
    }

    // If any statwords are left, append them
    statwords.each { statword ->
        status += ',' + (statwordMap[statword] ?: statword)
    }

    // Send the status event
    sendEvent(name: statusName, value: status)

    // Handle a reqest to shut down
    if (shutdown) {
        log.warn("upsd requesting client shutdown")

        // Let the server know we're leaving
        upsdDisconnect()

        // Shut down the hub if enabled
        sendEvent(name: statusName, value: statusShutdownRequested)
        if (shutdownEnable) {
            sendHubShutdownCommand()
        }
        else {
            log.error("hub shutdown is not enabled")
        }

        // If were are still here in 60 seconds, try reconnecting
        runIn(60, upsdConnect)
    }
}

private void sendHubShutdownCommand() {
    String cookie = null
    if (security) {
        cookie = getCookie()
    }

    def postParams = [
        uri: "http://127.0.0.1:8080",
        path: "/hub/shutdown",
        headers:[
            "Cookie": cookie
        ]
    ]

    log.warn("sending hub shutdown command...")
    httpPost(postParams) { response ->
        log.warn("hub shutdown command sent")
    }
}
