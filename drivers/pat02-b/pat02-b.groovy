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

metadata
{
    definition (
        name: "Philio PAT02-B", namespace: "cococafe", author: "Denny Page"
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
        fingerprint mfr: "013C", prod: "0002", deviceId: "0020",
            inClusters: "0x5E,0x80,0x71,0x85,0x70,0x72,0x86,0x30,0x31,0x84,0x59,0x73,0x5A,0x8F,0x98,0x7A",
            deviceJoinName: "Philio Temperature/Humidity sensor"

        // 0x30 COMMAND_CLASS_SENSOR_BINARY_V2
        // 0x31 COMMAND_CLASS_SENSOR_MULTILEVEL_V5
        // 0x59 COMMAND_CLASS_ASSOCIATION_GRP_INFO
        // 0x5A COMMAND_CLASS_DEVICE_RESET_LOCALLY
        // 0x5E COMMAND_CLASS_ZWAVEPLUS_INFO_V2
        // 0x70 COMMAND_CLASS_CONFIGURATION
        // 0x71 COMMAND_CLASS_NOTIFICATION_V4
        // 0x72 COMMAND_CLASS_MANUFACTURER_SPECIFIC_V2
        // 0x73 COMMAND_CLASS_POWERLEVEL
        // 0x7A COMMAND_CLASS_FIRMWARE_UPDATE_MD_V2
        // 0x80 COMMAND_CLASS_BATTERY
        // 0x84 COMMAND_CLASS_WAKE_UP_V2
        // 0x85 COMMAND_CLASS_ASSOCIATION_V2
        // 0x86 COMMAND_CLASS_VERSION_V2
        // 0x8F COMMAND_CLASS_MULTI_CMD
        // 0x98 COMMAND_CLASS_SECURITY
    }
}

preferences
{
    // Device values noted for reference, but not configurable by this driver
    // Temperature Scale: Parameter 5 bit 3 (0: Fahrenheit [default], 1: Celsius)
    // Notification Type: Parameter 7 bit 4 (0: Notification Report [default], 1: Sensor Binary Report)
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
        cmds.add(secureCmd(zwave.wakeUpV2.wakeUpIntervalGet()))
    }
    
    value = temperatureDifferential ? temperatureDifferential.toInteger() : 1
    if (resync || state.temperatureDifferential != value)
    {
        log.warn "Updating device temperatureDifferential: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 21, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 21)))
    }
    
    value = humidityDifferential ? humidityDifferential.toInteger() : 5
    if (resync || state.humidityDifferential != value)
    {
        log.warn "Updating device humidityDifferential: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 23, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 23)))
    }
   
    value = tickInterval ? tickInterval.toInteger() : 30
    if (resync || state.tickInterval != value)
    {
        log.warn "Updating device tickInterval: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 20, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 20)))
    }

    value = batteryInterval ? batteryInterval.toInteger() : 12
    if (resync || state.batteryInterval != value)
    {   
        log.warn "Updating device batteryInterval: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 10, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 10)))
    }
    
    value = temperatureInterval ? temperatureInterval.toInteger() : 12
    if (resync || state.temperatureInterval != value)
    {   
        log.warn "Updating device temperatureInterval: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 13, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 13)))
    }

    value = humidityInterval ? humidityInterval.toInteger() : 12
    if (resync || state.humidityInterval != value)
    {   
        log.warn "Updating device humidityInterval: ${value}"
        cmds.add(secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 14, size: 1)))
        cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: 14)))
    }

    if (refresh)
    {
        cmds.add(secureCmd(zwave.batteryV1.batteryGet()))
        cmds.add(secureCmd(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1)))
        cmds.add(secureCmd(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5)))
    }

    cmds.add(secureCmd(zwave.wakeUpV2.wakeUpNoMoreInformation()))
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
    runIn(1800,logsOff)
}

void updated()
{
    if (logEnable) log.debug "Updated preferences"
    
    // Validate numbers in preferences
    if (temperatureDifferential)
    {
        value = (temperatureDifferential.toBigDecimal()).toInteger()
        if (value != temperatureDifferential)
        {
            log.warn "temperatureDifferential must be an integer: ${temperatureDifferential} changed to ${value}"
            device.updateSetting("temperatureDifferential", value)
        }
    }
    if (humidityDifferential)
    {
        value = (humidityDifferential.toBigDecimal()).toInteger()
        if (value != humidityDifferential)
        {
            log.warn "humidityDifferential must be an integer: ${humidityDifferential} changed to ${value}"
            device.updateSetting("humidityDifferential", value)
        }
    }
    if (tickInterval)
    {
        value = (tickInterval.toBigDecimal()).toInteger()
        if (value != tickInterval)
        {
            log.warn "tickInterval must be an integer: ${tickInterval} changed to ${value}"
            device.updateSetting("tickInterval", value)
        }
    }
    if (batteryInterval)
    {
        value = (batteryInterval.toBigDecimal()).toInteger()
        if (value != batteryInterval)
        {
            log.warn "batteryInterval must be an integer: ${batteryInterval} changed to ${value}"
            device.updateSetting("batteryInterval", value)
        }
    }
    if (temperatureInterval)
    {
        value = (temperatureInterval.toBigDecimal()).toInteger()
        if (value != temperatureInterval)
        {
            log.warn "temperatureInterval must be an integer: ${temperatureInterval} changed to ${value}"
            device.updateSetting("temperatureInterval", value)
        }
    }
    if (humidityInterval)
    {
        value = (humidityInterval.toBigDecimal()).toInteger()
        if (value != humidityInterval)
        {
            log.warn "humidityInterval must be an integer: ${humidityInterval} changed to ${value}"
            device.updateSetting("humidityInterval", value)
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
  
def clearTamper() {
    def map = [:]
    map.name = "tamper"
    map.value = "clear"
    if (txtEnable) map.descriptionText = "${device.displayName} tamper cleared"

    log.info "${device.displayName} tamper cleared"
    sendEvent(map)
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

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
    def map = [:]

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
            if (txtEnable) map.descriptionText = "${device.displayName} temperature is ${map.value}°${map.unit}"

            log.info "${device.displayName} temperature is ${map.value}°${map.unit}"
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
            if (txtEnable) map.descriptionText = "${device.displayName} humidity is ${map.value}${map.unit}"

            log.info "${device.displayName} humidity is ${map.value}${map.unit}"
            break

        default:
            if (logEnable) log.debug "Unknown SensorMultilevelReport: ${cmd.toString()}"
            return null
            break
    }

    createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd)
{
    def map = [:]

    map.name = "battery"
    map.value = cmd.batteryLevel
    map.unit = "%"
    if (txtEnable) map.descriptionText = "${device.displayName} battery is ${map.value}${map.unit}"

    log.info "${device.displayName} battery is ${map.value}${map.unit}"
    createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd)
{
    if (cmd.notificationType == 7 && cmd.event == 3)
    {
        def map = [:]
        map.name = "tamper"
        map.value = "detected"
        if (txtEnable) map.descriptionText = "${device.displayName} tamper detected"

        log.info "${device.displayName} tamper detected"
        createEvent(map)
    }
    else
    {
        if (logEnable) log.debug "Unhandled NotificationReport: ${cmd.toString()}"
    }
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
    state.wakeUpInterval = cmd.seconds
    if (logEnable) log.debug "${device.displayName} wakup interval ${cmd.seconds}"
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd)
{
    if (logEnable) log.debug "Received WakeUpNotification"
    runInMillis(200, deviceSync)
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
    if (getDataValue("zwaveSecurePairingComplete") == "true")
    {
        return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    }
    else
    {
        return cmd.format()
    }    
}

