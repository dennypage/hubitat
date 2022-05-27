//
// Copyright (c) 2020-2022, Denny Page
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
// Version 2.0.0    Support flood sensor (PAT02-A & PAT03-C)
// Version 2.0.1    Poll flood sensor on refresh
// Version 2.0.2    Support older firmware that may send SensorBinaryReport rather
//                  than NotificationReport for flood sensor
//

metadata
{
    definition (
        name: "Philio PAT02", namespace: "cococafe", author: "Denny Page"
    )
    {
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Sensor"
        capability "WaterSensor"
        capability "Refresh"
        capability "Configuration"
        capability "Battery"
        capability "TamperAlert"

        command "clearTamper"

        fingerprint mfr: "013C", prod: "0002", deviceId: "001F" // PAT02-A
        fingerprint mfr: "013C", prod: "0002", deviceId: "0020" // PAT02-B
        fingerprint mfr: "013C", prod: "0002", deviceId: "0021" // PAT02-C
        fingerprint mfr: "013C", prod: "0002", deviceId: "002B" // PAT02-B-NS (No Security)

        // 0x30 COMMAND_CLASS_SENSOR_BINARY_V2 (removed in later firmware)
        // 0x31 COMMAND_CLASS_SENSOR_MULTILEVEL_V5 (later firmware uses V11)
        // 0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
        // 0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
        // 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
        // 0x70 COMMAND_CLASS_CONFIGURATION
        // 0x71 COMMAND_CLASS_NOTIFICATION_V4 (later firmware uses V8)
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
        // 0x73 COMMAND_CLASS_POWERLEVEL
        // 0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
        // 0x80 COMMAND_CLASS_BATTERY
        // 0x84 COMMAND_CLASS_WAKE_UP_V2
        // 0x85 COMMAND_CLASS_ASSOCIATION_V2
        // 0x86 COMMAND_CLASS_VERSION_V2 (later firmware uses V3)
        // 0x8F COMMAND_CLASS_MULTI_CMD
        // 0x98 COMMAND_CLASS_SECURITY
        // 0x9F COMMAND_CLASS_SECURITY_2 (only in later firmware)
    }
}

preferences
{
    // Device values noted for reference, but not configurable by this driver
    // Temperature Scale: Parameter 5 bit 3 (0: Fahrenheit [default], 1: Celsius)
    // Notification Type: Parameter 7 bit 4 (0: Notification Report [default], 1: Sensor Binary Report)
    //                                      (NB: Newer firmware no longer supports Sensor Binary Report)
    // Disable Multi CC: Parameter 7 bit 5 (0: Enable Mulit CC in Auto report [default], 1: Disable Multi CC)

    // Temperature differential report: Parameter 21, Range 0-127, default 1, units of degrees Fahrenheit
    input name: "temperatureDifferential", title: "Temperature differential report",
        description: "0 disables differential reporting",
        type: "number", defaultValue: "1", range: "0..127"

    // Humidity differential report: Parameter 23, Range 0-60, default 5, units of percent RH%
    input name: "humidityDifferential", title: "Humidity differential report",
        description: "0 disables differential reporting",
        type: "number", defaultValue: "5", range: "0..60"

    // Auto Report Tick interval: Parameter 20, Range 0-255, default 30, units of minutes. 0 disables all auto reporting.
    input name: "tickInterval", title: "Auto Report Tick minutes",
        description: "0 disables ALL auto reporting",
        type: "number", defaultValue: "30", range: "0..255"

    // Auto Report Battery interval: Parameter 10, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    input name: "batteryInterval", title: "Battery Auto Report Ticks",
        description: "0 disables auto reporting",
        type: "number", defaultValue: "12", range: "0..127"

    // Auto Report Temperature interval: Parameter 13, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    input name: "temperatureInterval", title: "Temperature Auto Report Ticks",
        description: "0 disables auto reporting",
        type: "number", defaultValue: "12", range: "0..127"

    // Auto Report Humidity interval: Parameter 14, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    input name: "humidityInterval", title: "Humidity Auto Report Ticks",
        description: "0 disables auto reporting",
        type: "number", defaultValue: "12", range: "0..127"

    // Auto Report Water interval: Parameter 15, Range 0-127, default 12, units of Ticks. 0 disables auto reporting.
    input name: "waterInterval", title: "Water Auto Report Ticks",
        description: "0 disables auto reporting",
        type: "number", defaultValue: "12", range: "0..127"

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

def deviceSync()
{
    resync = state.pendingResync
    refresh = state.pendingRefresh

    state.pendingResync = false
    state.pendingRefresh = false

    if (logEnable) log.debug "deviceSync: pendingResync ${resync}, pendingRefresh ${refresh}"

    def cmds = []
    if (resync)
    {
        cmds.add(zwaveSecureEncap(zwave.versionV2.versionGet()))
    }

    value = temperatureDifferential ? temperatureDifferential.toInteger() : 1
    if (resync || state.temperatureDifferential != value)
    {
        log.warn "Updating device temperatureDifferential: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 21, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 21)))
    }

    value = humidityDifferential ? humidityDifferential.toInteger() : 5
    if (resync || state.humidityDifferential != value)
    {
        log.warn "Updating device humidityDifferential: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 23, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 23)))
    }

    value = tickInterval ? tickInterval.toInteger() : 30
    if (resync || state.tickInterval != value)
    {
        log.warn "Updating device tickInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 20, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 20)))
    }

    value = batteryInterval ? batteryInterval.toInteger() : 12
    if (resync || state.batteryInterval != value)
    {
        log.warn "Updating device batteryInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 10, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 10)))
    }

    value = temperatureInterval ? temperatureInterval.toInteger() : 12
    if (resync || state.temperatureInterval != value)
    {
        log.warn "Updating device temperatureInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 13, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 13)))
    }

    value = humidityInterval ? humidityInterval.toInteger() : 12
    if (resync || state.humidityInterval != value)
    {
        log.warn "Updating device humidityInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 14, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 14)))
    }

    value = waterInterval ? waterInterval.toInteger() : 12
    if (resync || state.waterInterval != value)
    {
        log.warn "Updating device waterInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 15, size: 1)))
        cmds.add(zwaveSecureEncap(zwave.configurationV1.configurationGet(parameterNumber: 15)))
    }

    value = wakeUpInterval ? wakeUpInterval.toInteger() : 1440
    if (resync || state.wakeUpInterval != value)
    {
        log.warn "Updating device wakeUpInterval: ${value}"
        cmds.add(zwaveSecureEncap(zwave.wakeUpV2.wakeUpIntervalSet(seconds: value * 60, nodeid: zwaveHubNodeId)))
        cmds.add(zwaveSecureEncap(zwave.wakeUpV2.wakeUpIntervalGet()))
    }

    if (refresh)
    {
        cmds.add(zwaveSecureEncap(zwave.batteryV1.batteryGet()))
        cmds.add(zwaveSecureEncap(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1)))
        cmds.add(zwaveSecureEncap(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5)))
        cmds.add(zwaveSecureEncap(zwave.notificationV4.notificationGet(notificationType: 5, v1AlarmType: 0, event: 0)))
        cmds.add(zwaveSecureEncap(zwave.notificationV4.notificationGet(notificationType: 5, v1AlarmType: 0, event: 2)))
        cmds.add(zwaveSecureEncap(zwave.sensorBinaryV2.sensorBinaryGet(sensorType: 6)))
    }

    cmds.add(zwaveSecureEncap(zwave.wakeUpV2.wakeUpNoMoreInformation()))
    delayBetween(cmds, 250)
}

void logsOff()
{
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "debug logging disabled"
}

void installed()
{
    state.pendingResync = true
    state.pendingRefresh = true
    runIn(1, deviceSync)
    runIn(1800, logsOff)
}

void updated()
{
    if (logEnable) log.debug "Updated preferences"

    Integer value

    // Validate numbers in preferences
    if (temperatureDifferential)
    {
        value = temperatureDifferential.toBigDecimal()
        if (value != temperatureDifferential)
        {
            log.warn "temperatureDifferential must be an integer: ${temperatureDifferential} changed to ${value}"
            device.updateSetting("temperatureDifferential", value)
        }
    }
    if (humidityDifferential)
    {
        value = humidityDifferential.toBigDecimal()
        if (value != humidityDifferential)
        {
            log.warn "humidityDifferential must be an integer: ${humidityDifferential} changed to ${value}"
            device.updateSetting("humidityDifferential", value)
        }
    }
    if (tickInterval)
    {
        value = tickInterval.toBigDecimal()
        if (value != tickInterval)
        {
            log.warn "tickInterval must be an integer: ${tickInterval} changed to ${value}"
            device.updateSetting("tickInterval", value)
        }
    }
    if (batteryInterval)
    {
        value = batteryInterval.toBigDecimal()
        if (value != batteryInterval)
        {
            log.warn "batteryInterval must be an integer: ${batteryInterval} changed to ${value}"
            device.updateSetting("batteryInterval", value)
        }
    }
    if (temperatureInterval)
    {
        value = temperatureInterval.toBigDecimal()
        if (value != temperatureInterval)
        {
            log.warn "temperatureInterval must be an integer: ${temperatureInterval} changed to ${value}"
            device.updateSetting("temperatureInterval", value)
        }
    }
    if (humidityInterval)
    {
        value = humidityInterval.toBigDecimal()
        if (value != humidityInterval)
        {
            log.warn "humidityInterval must be an integer: ${humidityInterval} changed to ${value}"
            device.updateSetting("humidityInterval", value)
        }
    }
    if (waterInterval)
    {
        value = waterInterval.toBigDecimal()
        if (value != waterInterval)
        {
            log.warn "waterInterval must be an integer: ${waterInterval} changed to ${value}"
            device.updateSetting("waterInterval", value)
        }
    }
    if (wakeUpInterval)
    {
        value = wakeUpInterval.toBigDecimal()
        if (value < 30)
        {
            value = 30
        }
        else if (value > 7200)
        {
            value = 7200
        }
        else
        {
            Integer r = value % 30
            if (r)
            {
                value += 30 - r
            }
        }
        if (value != wakeUpInterval)
        {
            log.warn "wakeUpInterval must be an integer multiple of 30 between 30 and 7200: ${wakeUpInterval} changed to ${value}"
            device.updateSetting("wakeUpInterval", value)
        }
    }

    log.warn "debug logging is ${logEnable}"
    log.warn "description logging is ${txtEnable}"
}

def configure()
{
    state.pendingResync = true
    log.warn "Configuration will resync when device wakes up"
}

def refresh()
{
    state.pendingRefresh = true
    log.warn "Data will refresh when device wakes up"
}

def clearTamper()
{
    def map = [:]
    map.name = "tamper"
    map.value = "clear"
    map.descriptionText = "${device.displayName}: tamper cleared"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
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

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
    def map = [:]

    if (logEnable) log.debug "SensorMultilevelReport: ${cmd.toString()}"

    switch (cmd.sensorType)
    {
        case 1: // temperature
            def value = cmd.scaledSensorValue
            def precision = cmd.precision
            def unit = cmd.scale == 1 ? "F" : "C"

            map.name = "temperature"
            map.value = convertTemperatureIfNeeded(value, unit, precision)
            map.unit = getTemperatureScale()
            if (logEnable) log.debug "${device.displayName} temperature sensor value is ${value}°${unit} (${map.value}°${map.unit})"

            if (temperatureOffset)
            {
                map.value = (map.value.toBigDecimal() + temperatureOffset.toBigDecimal()).toString()
                if (logEnable) log.debug "${device.displayName} adjusted temperature by ${temperatureOffset} to ${map.value}°${map.unit}"
            }
            map.descriptionText = "${device.displayName}: temperature is ${map.value}°${map.unit}"
            break

        case 5: // humidity
            value = cmd.scaledSensorValue

            map.name = "humidity"
            map.value = value
            map.unit = "%"
            if (logEnable) log.debug "${device.displayName} humidity sensor value is ${map.value}${map.unit}"

            if (humidityOffset)
            {
                map.value = (map.value.toBigDecimal() + humidityOffset.toBigDecimal()).toString()
                if (logEnable) log.debug "${device.displayName} adjusted humidity by ${humidityOffset} to ${map.value}${map.unit}"
            }
            map.descriptionText = "${device.displayName}: humidity is ${map.value}${map.unit}"
            break

        default:
            if (logEnable) log.debug "Unknown SensorMultilevelReport: ${cmd.toString()}"
            return null
            break
    }

    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd)
{
    def map = [:]

    if (logEnable) log.debug "BatteryReport: ${cmd.toString()}"

    def batteryLevel = cmd.batteryLevel
    if (batteryLevel == 0xFF)
    {
        log.warn "${device.displayName} low battery"
        batteryLevel = 1
    }

    map.name = "battery"
    map.value = batteryLevel
    map.unit = "%"
    map.descriptionText = "${device.displayName}: battery is ${map.value}${map.unit}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
}

def zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd)
{
    def map = [:]

    if (logEnable) log.debug "NotificationReport: ${cmd.toString()}"

    switch (cmd.notificationType)
    {
        case 5: // water
            map.name = "water"
            map.value = cmd.event ? "wet" : "dry"
            map.descriptionText = "${device.displayName}: sensor is ${map.value}"
        case 7: // tamper
            map.name = "tamper"
            map.value = "detected"
            map.descriptionText = "${device.displayName}: tamper detected"
        default:
            if (logEnable) log.debug "Unknown NotificationReport: ${cmd.toString()}"
            return null
            break
    }

    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
}

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd)
{
    // NB: Older firmware versions may send SensorBinaryReport instead of NotificationReport

    def map = [:]

    if (logEnable) log.debug "SensorBinaryReport: ${cmd.toString()}"

    if (cmd.sensorType != 6)
    {
        if (logEnable) log.debug "Unknown SensorBinaryReport: ${cmd.toString()}"
        return null
    }

    map.name = "water"
    map.value = cmd.sensorValue ? "wet" : "dry"
    map.descriptionText = "${device.displayName}: sensor is ${map.value}"
    sendEvent(map)
    if (txtEnable) log.info "${map.descriptionText}"
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)
{
    if (logEnable) log.debug "ConfigurationReport: ${cmd.toString()}"

    switch (cmd.parameterNumber)
    {
        case 10: // Auto Report Battery interval
            state.batteryInterval = cmd.configurationValue[0]
            break
        case 13: // Auto Report Temperature interval
            state.temperatureInterval = cmd.configurationValue[0]
            break
        case 14: // Auto Report Humidity interval
            state.humidityInterval = cmd.configurationValue[0]
            break
        case 15: // Auto Report Water interval
            state.waterInterval = cmd.configurationValue[0]
            break
        case 20: // Auto Report tick interval
            state.tickInterval = cmd.configurationValue[0]
            break
        case 21: // Temperature Differential Report
            state.temperatureDifferential = cmd.configurationValue[0]
            break
        case 23: // Humidity Differential Report
            state.humidityDifferential = cmd.configurationValue[0]
            break
        default:
            if (logEnable) log.debug "Unknown Configuration Report Received ConfigurationReport: ${cmd.toString()}"
    }
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd)
{
    state.wakeUpInterval = cmd.seconds / 60
    if (logEnable) log.debug "${device.displayName} wakup interval ${state.wakeUpInterval} minutes"
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd)
{
    if (logEnable) log.debug "Received WakeUpNotification"
    runInMillis(200, deviceSync)
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd)
{
    if (logEnable) log.debug "VersionReport: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd)
{
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
    log.warn "Unhandled cmd: ${cmd.toString()}"
    return null
}
