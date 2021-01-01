//
// Copyright (c) 2020-2021, Denny Page
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
// Version 1.1.0    Add App Events
//

definition(
    name: "Simple Idle Alert",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Send alerts for a device that is idle",
    category: "Convenience",
    parent: "cococafe:Simple Idle Alerts",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage()
{
    dynamicPage(name: "", title: "Simple Idle Alert", install: true, uninstall: true, refreshInterval: 0)
    {
        // Ensure label is correct in case the device has changed label
        checkLabel()

        section("") {
            paragraph "Choose the notification device, device and the number of minutes before alerts are sent"
        }
        section("") {
            input "configNotification", "capability.notification", title: "Send alerts to these devices", multiple: true, required: true
        }
        section("") {
            input "configDevice", "capability.*", title: "Send alerts for this device", multiple: false, required: true
        }
        section("")
        {
            input "configMinutes", "number", title: "Number of minutes before first alert", required: true
        }
        section("")
        {
            input "configMinutesSubsequent", "number", title: "Number of minutes between subsequent alerts (0 to disable)", required: true
        }
        section("")
        {
            input name: "appEvents", title: "Enable app events", type: "bool", defaultValue: false
        }
    }
}

def checkLabel()
{
    if (configDevice)
    {
        oldLabel = app.getLabel()
        newLabel = "${configDevice} alert after ${configMinutes} minutes"
        if (newLabel != oldLabel)
        {
            if (oldLabel) log.info "Simple Idle Alert changed: ${oldLabel} -> ${newLabel}"
            app.updateLabel(newLabel)
        }
    }
}

def installed()
{
    checkLabel()

    if (configMinutes)
    {
        runIn(1, checkIdle)
    }
}

def updated() {
    unschedule()
    installed()
}

private Long lastActivity(device)
{
    // 2020-11-17 01:56:54+0000
    return Date.parse("yyyy-MM-dd HH:mm:ssZ", "${device.getLastActivity()}").getTime()
}

private Long millisToIdle()
{
    Long now = now()
    Long idle = lastActivity(configDevice) + configMinutes * 60000

    if (idle > now) return idle - now
    return 0
}

private Long idleMillis(device)
{
    return now() - Date.parse("yyyy-MM-dd HH:mm:ssZ", "${configDevice.getLastActivity()}").getTime()
}

def checkIdle()
{
    Long configMillis = configMinutes * 60000
    Long millis = idleMillis()

    if (millis < configMillis)
    {
        runInMillis(configMillis - millis, checkIdle)
        return
    }

    runInMillis(configMillis, checkActive)
    sendAlert()
}

def checkActive()
{
    Long configMillis = configMinutes * 60000
    Long millis = idleMillis()

    if (millis < configMillis)
    {
        unschedule()
        runInMillis(configMillis - millis, checkIdle)
        return
    }

    runInMillis(configMillis, checkActive)
}

def sendAlert()
{
    Long configMillis = configMinutes * 60000
    Long millis = idleMillis()

    if (millis < configMillis)
    {
        unschedule()
        runInMillis(configMillis - millis, checkIdle)
        return
    }

    Long minutes = millis / 60000
    String desc = "Idle Alert: device ${configDevice} has been idle for ${minutes} minutes"
    log.info "${desc}"
    if (appEvents) sendEvent(name: "SSA", value: "Idle", descriptionText: "${desc}")
    configNotification.deviceNotification("${desc}")

    if (configMinutesSubsequent)
    {
        runIn(configMinutesSubsequent.toInteger() * 60, sendAlert)
    }
}
