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
// Version 2.0.0    Code restructure and cleanup
//

definition(
    name: "Simple Switch Reminder",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Send reminders for a switch that has been left on",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/simple-switch-reminders/simple-switch-reminder.groovy",
    parent: "cococafe:Simple Switch Reminders",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Simple Switch Reminder", install: true, uninstall: true, refreshInterval: 0)
    {
        // Ensure label is correct in case the device has changed label
        checkLabel()

        section("")
        {
            paragraph "Choose the notification device, switch and the number of minutes before reminders are sent"
        }
        section("")
        {
            input "configNotification", "capability.notification", title: "Send reminders to these devices", multiple: true, required: true
        }
        section("")
        {
            input "configSwitch", "capability.switch", title: "Send reminders for this switch", multiple: false, required: true
        }
        section("")
        {
            input "configMinutes", "number", title: "Number of minutes before first reminder", required: true
        }
        section("")
        {
            input "configMinutesSubsequent", "number", title: "Number of minutes between subsequent reminders (0 to disable)", required: true
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
        subscribe(configSwitch, "switch", switchEvent)
        if (configSwitch.currentState("switch").value == "on") {
            runIn(configMinutes.toInteger() * 60, switchReminder)
        }
    }
}

void updated() {
    unsubscribe()
    unschedule()
    installed()
}

void checkLabel() {
    if (configSwitch) {
        oldLabel = app.getLabel()
        newLabel = "${configSwitch} reminder after ${configMinutes} minutes"
        if (newLabel != oldLabel) {
            if (oldLabel) log.info "Simple Switch Reminder changed: ${oldLabel} -> ${newLabel}"
            app.updateLabel(newLabel)
        }
    }
}

void switchReminder() {
    String desc = "Switch Reminder: ${configSwitch} left on"
    log.info "${desc}"
    if (appEvents) sendEvent(name: "SSA", value: "On", descriptionText: "${desc}")
    configNotification.deviceNotification("${desc}")

    if (configMinutesSubsequent) {
        runIn(configMinutesSubsequent.toInteger() * 60, switchReminder)
    }
}

void switchEvent(evt) {
    if (evt.value == "on") {
        runIn(configMinutes.toInteger() * 60, switchReminder)
    }
    else if (evt.value == "off") {
        unschedule()
    }
}
