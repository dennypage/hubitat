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
// Version 1.1.0    Report version information for protocol, hardware and firmware.
//                  Unhandled events logged as warnings.
// Version 1.2.0    Add support for setting the wakeup interval.
// Version 1.3.0    Move to Wakeup interval in minutes and improve validity checks.
// Version 1.4.0    Use zwaveSecureEncap method introduced in Hubitat 2.2.3.
// Version 1.5.0    Normalize logging
// Version 1.5.1    Fix low battery alert
// Version 1.5.2    Low battery value cannot be 0
// Version 1.5.3    Fix battery value again
// Version 1.5.4    Notify if parameter 7 is not factory default
// Version 1.6.0    Set state change to true for temperature/humidity/battery
//                  events to properly handle auto reports
// Version 1.6.1    Fix zero comparison that prevented disabling of various reports
// Version 1.6.2    Revert explicit setting of isStateChange
// Version 2.0.0    Code restructure and cleanup
//

// Supported Z-Wave Classes:
//
//     0x30 COMMAND_CLASS_SENSOR_BINARY_V2 (removed in newer firmware)
//     0x31 COMMAND_CLASS_SENSOR_MULTILEVEL_V11 (older firmware is V5)
//     0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
//     0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
//     0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//     0x70 COMMAND_CLASS_CONFIGURATION
//     0x71 COMMAND_CLASS_NOTIFICATION_V8 (older firmware is V4)
//     0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//     0x73 COMMAND_CLASS_POWERLEVEL
//     0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
//     0x80 COMMAND_CLASS_BATTERY
//     0x84 COMMAND_CLASS_WAKE_UP_V2
//     0x85 COMMAND_CLASS_ASSOCIATION_V2
//     0x86 COMMAND_CLASS_VERSION_V3 (older firmware is V2)
//     0x8F COMMAND_CLASS_MULTI_CMD
//     0x98 COMMAND_CLASS_SECURITY
//     0x9F COMMAND_CLASS_SECURITY_2 (only in newer firmware)

import groovy.transform.Field

metadata
{
    definition(
        name: "Philio PAT02-B", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/pat02-b/pat02-b.groovy"
    )
    {
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Sensor"
        capability "Refresh"
        capability "Configuration"
        capability "Battery"
        capability "TamperAlert"

        command "clearTamper"

        // NB: This fingerprint matches the security enabled version of the device. There is also a
        //     "non secure" version of the device (deviceId 002B) which lacks COMMAND_CLASS_SECURITY
        fingerprint mfr: "013C", prod: "0002", deviceId: "0020" // PAT02-B
        fingerprint mfr: "013C", prod: "0002", deviceId: "002B" // PAT02-B-NS (No Security)
    }
}

@Field static final Map commandClassVersions = [0x30:2, 0x31:11, 0x70:1, 0x71:8, 0x80:1, 0x84:2, 0x86:3, 0x98:1]

@Field static final Map<Integer,Map> deviceParamaters = [
    // Device values noted for reference, but not configurable by this driver
    // Temperature Scale: Parameter 5 bit 3 - 0: Fahrenheit [default], 1: Celsius
    // Notification Type: Parameter 7 bit 4 - 0: Notification Report [default], 1: Sensor Binary Report
    //                                           NB: Newer firmware no longer supports Sensor Binary Report
    // Disable Multi CC:  Parameter 7 bit 5 - 0: Enable Mulit CC in Auto report [default], 1: Disable Multi CC

    // Temperature differential report: Parameter 21, Range 0-127, default 1, units of degrees Fahrenheit
    21: [name: "temperatureDifferential", title: "Temperature differential report",
        type: "number", defaultValue: "1", range: "0..127",
        description: "0 disables differential reporting"],

    // Humidity differential report: Parameter 23, Range 0-60, default 5, units of percent RH%
    23: [name: "humidityDifferential", title: "Humidity differential report",
        type: "number", defaultValue: "5", range: "0..60",
        description: "0 disables differential reporting"],

    // Auto Report Tick interval: Parameter 20, Range 0-255, default 30, units of minutes. 0 disables all auto reporting.
    20: [name: "tickInterval", title: "Auto Report Tick minutes",
       type: "number", defaultValue: "30", range: "0..255",
        description: "0 disables ALL auto reporting"],

    // Auto Report Battery interval: Parameter 10, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    10: [name: "batteryInterval", title: "Battery Auto Report Ticks",
        type: "number", defaultValue: "12", range: "0..127",
        description: "0 disables auto reporting"],

    // Auto Report Temperature interval: Parameter 13, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    13: [name: "temperatureInterval", title: "Temperature Auto Report Ticks",
        type: "number", defaultValue: "12", range: "0..127",
        description: "0 disables auto reporting"],

    // Auto Report Humidity interval: Parameter 14, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    14: [name: "humidityInterval", title: "Humidity Auto Report Ticks",
        type: "number", defaultValue: "12", range: "0..127",
        description: "0 disables auto reporting"],
]

preferences
{
    // Configurable device parameters
    deviceParamaters.each { parameter, map ->
        input map
    }

    // Wakeup Interval: Number of minutes between wakeups
    input name: "wakeUpInterval", title: "Wakeup interval minutes",
        type: "number", defaultValue: "1440", range: "30..7200"

    // Temperature offset: Adjustment amount for temperature measurement
    input name: "temperatureOffset", title: "Temperature offset degrees",
        type: "decimal", defaultValue: "0"

    // Humidity offset: Adjustment amount for humidity measurement
    input name: "humidityOffset", title: "Humidity offset percent",
        type: "decimal", defaultValue: "0"

    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable descriptionText logging", type: "bool", defaultValue: true
}

void installed() {
    state.pendingResync = true
    state.pendingRefresh = true
    runIn(1, deviceSync)
    runIn(1800, logsOff)
}

void updated() {
    if (logEnable) log.debug "Updated preferences"

    // Validate parameter numbers
    Integer value
    deviceParamaters.each { parameter, map ->
        if (settings[map.name] != null) {
            value = settings[map.name].toBigDecimal()
            if (value != settings[map.name]) {
                log.warn "${map.title} must be an integer: value changed from ${settings[map.name]} to ${value}"
                device.updateSetting("${map.name}", value)
            }
        }
    }

    // Validate wakeup interval
    if (wakeUpInterval) {
        value = wakeUpInterval.toBigDecimal()
        if (value < 30) {
            value = 30
        }
        else if (value > 7200) {
            value = 7200
        }
        else {
            Integer r = value % 30
            if (r) {
                value += 30 - r
            }
        }
        if (value != wakeUpInterval) {
            log.warn "Wakeup interval must be an integer multiple of 30 between 30 and 7200: ${wakeUpInterval} changed to ${value}"
            device.updateSetting("wakeUpInterval", value)
        }
    }

    log.warn "Debug logging is ${logEnable}"
    log.warn "Description logging is ${txtEnable}"
}

void configure() {
    state.pendingResync = true
    log.warn "Configuration will resync when device wakes up"
}

void refresh() {
    state.pendingRefresh = true
    log.warn "Data will refresh when device wakes up"
}

void clearTamper() {
    logEvent("tamper", "clear", null, "Tamper cleared")
}

void deviceSync() {
    resync = state.pendingResync
    refresh = state.pendingRefresh

    state.pendingResync = false
    state.pendingRefresh = false

    if (logEnable) log.debug "deviceSync: pendingResync ${resync}, pendingRefresh ${refresh}"

    List<hubitat.zwave.Command> cmds = []
    if (resync) {
        cmds.add(zwave.versionV3.versionGet())
        cmds.add(zwave.configurationV1.configurationGet(parameterNumber: 7))
    }

    deviceParamaters.each { parameter, map ->
        value = (settings[map.name] != null) ? settings[map.name].toInteger() : map.defaultValue.toInteger()
        if (resync || state[map.name] != value) {
            log.warn "Updating device ${map.name}: ${value}"
            cmds.add(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: parameter, size: 1))
            cmds.add(zwave.configurationV1.configurationGet(parameterNumber: parameter))
        }
   }

    value = (wakeUpInterval != null) ? wakeUpInterval.toInteger() : 1440
    if (resync || state.wakeUpInterval != value) {
        log.warn "Updating device wakeUpInterval: ${value}"
        cmds.add(zwave.wakeUpV2.wakeUpIntervalSet(seconds: value * 60, nodeid: zwaveHubNodeId))
        cmds.add(zwave.wakeUpV2.wakeUpIntervalGet())
    }

    if (refresh) {
        cmds.add(zwave.batteryV1.batteryGet())
        cmds.add(zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: 1))
        cmds.add(zwave.sensorMultilevelV11.sensorMultilevelGet(sensorType: 5))
    }

    cmds.add(zwave.wakeUpV2.wakeUpNoMoreInformation())
    sendCmds(cmds)
}

void logsOff() {
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "Debug logging disabled"
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
    hubitat.zwave.Command cmd = zwave.parse(description, commandClassVersions)
    if (cmd) {
        zwaveEvent(cmd)
    }
    else {
        log.warn "Non Z-Wave parse event: ${description}"
    }
}

void zwaveEvent(hubitat.zwave.commands.multicmdv1.MultiCmdEncap cmd) {
    if (logEnable) log.debug "MultiCmdEncap: ${cmd}"

    cmd.encapsulatedCommands(commandClassVersions).each { encapsulatedCommand ->
        zwaveEvent(encapsulatedCommand)
    }
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport cmd) {
    if (logEnable) log.debug "SensorMultilevelReport: ${cmd}"

    String value, newValue, unit
    switch (cmd.sensorType) {
        case 1: // temperature
            value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
            unit = getTemperatureScale()
            if (temperatureOffset) {
                newValue = (value.toBigDecimal() + temperatureOffset.toBigDecimal())
                if (logEnable) log.debug "Adjusting temperature by ${temperatureOffset}°${unit} from ${value}°${unit} to ${newValue}°${unit}"
                value = newValue
            }
            logEvent("temperature", value, unit, "Temperature is ${value}°${unit}")
            break

        case 5: // humidity
            value = cmd.scaledSensorValue
            if (humidityOffset) {
                newValue = (value.toBigDecimal() + humidityOffset.toBigDecimal())
                if (logEnable) log.debug "Adjusting humidity by ${humidityOffset}% from ${value}% to ${newValue}%"
                value = newValue
            }
            logEvent("humidity", value, "%", "Humidity is ${value}%")
            break

        default:
            log.warn "Unknown SensorMultilevelReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
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

    switch (cmd.notificationType) {
        case 7: // tamper
            logEvent("tamper", "detected", null, "Tamper detected", true)
            break
        default:
            log.warn "Unknown NotificationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
    // NB: Older firmware versions may send SensorBinaryReport instead of NotificationReport
    if (logEnable) log.debug "SensorBinaryReport: ${cmd}"

    switch (cmd.sensorType) {
        case 8: // tamper
            logEvent("tamper", "detected", null, "Tamper detected", true)
            break
        default:
            log.warn "Unknown SensorBinaryReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if (logEnable) log.debug "ConfigurationReport: ${cmd}"

    Integer parameterNumber = cmd.parameterNumber
    Map map = deviceParamaters[parameterNumber]
    if (map) {
        state[map.name] = cmd.configurationValue[0]
    }
    else if (parameterNumber == 7) {
        Short value = cmd.configurationValue[0]
        if (value) log.warn "Parameter 7 (custom functions) set to ${value} (factory default is 0)"
    }
    else {
        log.warn "Unknown Configuration Report Received ConfigurationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    state.wakeUpInterval = cmd.seconds / 60
    if (logEnable) log.debug "Wakup interval ${state.wakeUpInterval} minutes"
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    if (logEnable) log.debug "Received WakeUpNotification"
    runInMillis(200, deviceSync)
}

void zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    encapCmd = cmd.encapsulatedCommand(commandClassVersions)
    if (encapCmd) {
        zwaveEvent(encapCmd)
    }
    else {
        log.warn "Unable to extract encapsulated cmd: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    log.warn "Unhandled cmd: ${cmd}"
}
