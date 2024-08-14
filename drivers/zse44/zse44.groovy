//
// Copyright (c) 2024, Denny Page
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
// Version 1.0.1    Update command classes following clarification from Zooz.
// Version 1.1.0    Put Supervision handling back
//

// Supported Z-Wave Classes:
//
//     0x31 COMMAND_CLASS_SENSOR_MULTILEVEL_V11
//     0x59 COMMAND_CLASS_TRANSPORT_SERVICE_V2
//     0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO_v3
//     0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
//     0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
//     0x6C COMMAND_CLASS_SUPERVISION
//     0x70 COMMAND_CLASS_CONFIGURATION_v4
//     0x71 COMMAND_CLASS_NOTIFICATION_V8
//     0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
//     0x73 COMMAND_CLASS_POWERLEVEL
//     0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V5
//     0x80 COMMAND_CLASS_BATTERY
//     0x84 COMMAND_CLASS_WAKE_UP_V2
//     0x85 COMMAND_CLASS_ASSOCIATION_V3
//     0x86 COMMAND_CLASS_VERSION_V3
//     0x8E COMMAND_CLASS_MULTI_CHANNEL_ASSOCIATION_V4
//     0x8F COMMAND_CLASS_INDICATOR_V3
//     0x9F COMMAND_CLASS_SECURITY_2

import groovy.transform.Field

metadata
{
    definition(
        name: "Zooz ZSE44", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/drivers/zse44/zse44.groovy"
    )
    {
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Sensor"
        capability "Refresh"
        capability "Configuration"
        capability "Battery"

        fingerprint mfr: "027A", prod: "7000", deviceId: "E004" // Zooz Temperature / Humidity XS Sensor ZSE44
    }
}

@Field static final Map commandClassVersions = [0x31:11, 0x6C:1, 0x70:4, 0x71:8, 0x72:2, 0x80:1, 0x84:2, 0x86:3]

//
// Device parameters:
//   Information in https://www.support.getzooz.com/kb/article/853-zse44-temperature-humidity-xs-sensor-advanced-settings/
//   Parameters 5-12 (unused) control temperature and humidity alerts
//
//   NB: Doc as of 2024-08-01 says the range for parameter 2 (low battery alert threshold) is 10-50,
//       however this does not match behavior of firmware version 2.00 (800 series). Zooz support
//       confirmed on 2024-08-02 the published doc is incorrect, and that the doc would be updated
//       in the near future.

@Field static final Map<Integer,Map> deviceParamaters = [
    3: [name: "temperatureReportThreshold", title: "Temperature report threshold (°F)",
        type: "decimal", size: "1", defaultValue: "2.0", range: "1.0..10.0",
        description: "range:&nbsp 1.0 &nbsp—&nbsp 10.0"],

    16: [name: "temperatureReportInterval", title: "Temperature Report Interval (min)",
        type: "number", size: "2", defaultValue: "240", range: "0..480",
        description: "range:&nbsp 0 (disabled), &nbsp 1 &nbsp—&nbsp 480"],

    14: [name: "temperatureSensorOffset", title: "Temperature sensor offset (°F)",
        type: "decimal", size: "1", defaultValue: "0.0", range: "-10.0..10.0",
        description: "range:&nbsp -10.0 &nbsp—&nbsp +10.0", parameterMap: "mapSensorOffsetParameter"],

    4: [name: "humidityReportThreshold", title: "Humidity report threshold (%RH)",
        type: "number", size: "1", defaultValue: "10", range: "1..50",
        description: "range:&nbsp 1 &nbsp—&nbsp 50"],

    17: [name: "humidityReportInterval", title: "Humidity Report Interval (min)",
        type: "number", size: "2", defaultValue: "240", range: "0..480&nbsp ",
        description: "range:&nbsp 0 (disabled) &nbsp—&nbsp 480"],

    15: [name: "humiditySensorOffset", title: "Humidity sensor offset (%RH)",
        type: "decimal", size: "1", defaultValue: "0.0", range: "-10.0..10.0",
        description: "range:&nbsp -10.0 &nbsp—&nbsp +10.0", parameterMap: "mapSensorOffsetParameter"],

    1: [name: "batteryReportThreshold", title: "Battery report threshold (%)",
        type: "number", size: "1", defaultValue: "5", range: "1..10",
        description: "range:&nbsp 1 &nbsp—&nbsp 10"],

    2: [name: "batteryAlertThreshold", title: "Low battery alert (%)",
        type: "number", size: "1", defaultValue: "10", range: "5..20",
        description: "range:&nbsp 5 &nbsp—&nbsp 20"],

    // Parameter 13 controls temperature report unit (Celsius/Fahrenheit) and is hard coded to Fahrenheit
    13: [name: "temperatureReportUnit", size: "1", fixedValue: "1"],
]

preferences
{
    // Configurable device parameters
    deviceParamaters.each { parameter, map ->
        if (map.fixedValue == null) {
            input map
        }
    }

    // Wakeup Interval
    input name: "wakeUpInterval", title: "Wakeup interval (hours)",
        type: "number", defaultValue: "12", range: "1..24",
        description: "range: &nbsp 1 &nbsp—&nbsp 24"

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

    // Validate number settings
    deviceParamaters.each { parameter, map ->
        if (settings[map.name] != null && map.type == "number") {
            Integer value = settings[map.name].toBigDecimal()
            if (value != settings[map.name]) {
                log.warn "${map.title} must be an integer: value changed from ${settings[map.name]} to ${value}"
                device.updateSetting("${map.name}", value)
            }
        }
    }

    // Validate wakeup interval
    if (wakeUpInterval) {
        Integer value = wakeUpInterval.toBigDecimal()
        if (value != settings["wakeUpInterval"]) {
            log.warn "wakeUpInterval must be an integer: value changed from ${settings["wakeUpInterval"]} to ${value}"
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

// Lack of unsigned in Java is a serious annoyance
Integer forwardBiasParameterValue(Integer size, Integer value) {
    Integer bias = 1
    while (size--) { bias <<= 8 }
    if (value >= (bias >> 1)) {
        value -= bias
    }
    return value
}

Integer reverseBiasParameterValue(Integer size, Integer value) {
    Integer bias = 1
    while (size--) { bias <<= 8 }
    if (value < 0) {
        value += bias
    }
    return value
}

Integer mapSensorOffsetParameter(BigDecimal offset) {
    Integer parameterValue = (offset * 10.0) + 100
    return parameterValue
}

void deviceSync() {
    resync = state.pendingResync
    refresh = state.pendingRefresh

    state.pendingResync = false
    state.pendingRefresh = false

    if (logEnable) log.debug "deviceSync: pendingResync ${resync}, pendingRefresh ${refresh}"

    List<hubitat.zwave.Command> cmds = []
    if (resync) {
        cmds.add(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
        cmds.add(zwave.versionV3.versionGet())
    }

    deviceParamaters.each { parameter, map ->
        BigDecimal value
        if (map.fixedValue) {
            value = map.fixedValue.toBigDecimal()
        }
        else {
            value = (settings[map.name] != null) ? settings[map.name].toBigDecimal() : map.defaultValue.toBigDecimal()
            if (map.parameterMap) {
                // Map setting to device parameter value
                value = "${map.parameterMap}"(value)
            }
        }

        Integer parameterValue = value.toInteger()
        if (resync || state[map.name] != parameterValue) {
            log.warn "Updating device ${map.name}: ${parameterValue}"
            parameterValue = forwardBiasParameterValue(map.size.toInteger(), parameterValue)
            cmds.add(zwave.configurationV4.configurationSet(scaledConfigurationValue: parameterValue, parameterNumber: parameter, size: map.size))
            cmds.add(zwave.configurationV4.configurationGet(parameterNumber: parameter))
        }
    }

    value = (wakeUpInterval != null) ? wakeUpInterval.toInteger() : 12
    if (resync || state.wakeUpInterval != value) {
        log.warn "Updating device wakeUpInterval: ${value}"
        cmds.add(zwave.wakeUpV2.wakeUpIntervalSet(seconds: value * 3600, nodeid: zwaveHubNodeId))
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

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv11.SensorMultilevelReport cmd) {
    if (logEnable) log.debug "SensorMultilevelReport: ${cmd}"

    String value, unit
    switch (cmd.sensorType) {
        case 1: // temperature
            value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
            unit = getTemperatureScale()
            logEvent("temperature", value, unit, "Temperature is ${value}°${unit}")
            break

        case 5: // humidity
            value = cmd.scaledSensorValue
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
        default:
            log.warn "Unknown NotificationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.configurationv4.ConfigurationReport cmd) {
    if (logEnable) log.debug "ConfigurationReport: ${cmd}"

    Integer parameterNumber = cmd.parameterNumber
    Map map
    map = deviceParamaters[parameterNumber]
    if (map) {
        state[map.name] = reverseBiasParameterValue(cmd.size, cmd.scaledConfigurationValue)
    }
    else {
        log.warn "Unknown Configuration Report Received ConfigurationReport: ${cmd}"
    }
}

void zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    state.wakeUpInterval = cmd.seconds / 3600
    if (logEnable) log.debug "Wakup interval ${state.wakeUpInterval} hours"
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

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    if (logEnable) log.debug "ManufacturerSpecificReport: ${cmd}"
    device.updateDataValue("manufacturer", "${cmd.manufacturerId}")
    device.updateDataValue("deviceType", "${cmd.productTypeId}")
    device.updateDataValue("deviceId", "${cmd.productId}")
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
