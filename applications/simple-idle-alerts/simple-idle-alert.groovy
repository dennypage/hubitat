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
// Version 1.1.0    Add App Events
// Version 1.2.0    Add initialization function to ensure worker schedule
//                  isn't lost during reboot. Use seconds for calculations
//                  rather than milliseconds. Avoid runInMillis.
// Version 1.2.1    Change initialized function to systemStart subscription.
//                  Initialized isn't called for apps.
// Version 2.0.0    Code restructure and cleanup
//

definition(
    name: "Simple Idle Alert",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Send alerts for a device that is idle",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/simple-idle-alerts/simple-idle-alert.groovy",
    parent: "cococafe:Simple Idle Alerts",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Simple Idle Alert", install: true, uninstall: true, refreshInterval: 0)
    {
        // Ensure label is correct in case the device has changed label
        checkLabel()

        section("")
        {
            paragraph "Choose the notification device, device and the number of minutes before alerts are sent"
        }
        section("")
        {
            input "configNotification", "capability.notification", title: "Send alerts to these devices", multiple: true, required: true
        }
        section("")
        {
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

void installed() {
    checkLabel()
    if (configMinutes) {
        runIn(1, checkIdle)
    }
}

void updated() {
    subscribe(location, "systemStart", hubRestartHandler)
    unschedule()
    installed()
}

void hubRestartHandler(evt) {
    updated()
}

void checkLabel() {
    if (configDevice) {
        oldLabel = app.getLabel()
        newLabel = "${configDevice} alert after ${configMinutes} minutes"
        if (newLabel != oldLabel) {
            if (oldLabel) log.info "Simple Idle Alert changed: ${oldLabel} -> ${newLabel}"
            app.updateLabel(newLabel)
        }
    }
}

private Long lastActivity(device) {
    // 2020-11-17 01:56:54+0000
    return Date.parse("yyyy-MM-dd HH:mm:ssZ", "${device.getLastActivity()}").getTime() / 1000
}

private Long idleSeconds() {
    Long now = now() / 1000

    return now - lastActivity(configDevice)
}

void checkIdle() {
    Long configSeconds = configMinutes * 60
    Long seconds = idleSeconds()

    if (seconds < configSeconds) {
        runIn(configSeconds - seconds, checkIdle)
        return
    }

    runIn(configSeconds, checkActive)
    sendAlert()
}

void checkActive() {
    Long configSeconds = configMinutes * 60
    Long seconds = idleSeconds()

    if (seconds < configSeconds) {
        unschedule()
        runIn(configSeconds - seconds, checkIdle)
        return
    }

    runIn(configSeconds, checkActive)
}

void sendAlert() {
    Long configSeconds = configMinutes * 60
    Long seconds = idleSeconds()

    if (seconds < configSeconds) {
        unschedule()
        runIn(configSeconds - seconds, checkIdle)
        return
    }

    Long minutes = seconds / 60
    String desc = "Idle Alert: device ${configDevice} has been idle for ${minutes} minutes"
    log.info "${desc}"
    if (appEvents) sendEvent(name: "SSA", value: "Idle", descriptionText: "${desc}")
    configNotification.deviceNotification("${desc}")

    if (configMinutesSubsequent) {
        runIn(configMinutesSubsequent.toInteger() * 60, sendAlert)
    }
}
