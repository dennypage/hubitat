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
    name: "Simple Contact Reminder",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Send reminders for a contact sensor (door/window/etc.) that has been left open",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/simple-contact-reminders/simple-contact-reminder.groovy",
    parent: "cococafe:Simple Contact Reminders",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Simple Contact Reminder", install: true, uninstall: true, refreshInterval: 0)
    {
        // Ensure label is correct in case the device has changed label
        checkLabel()

        section("")
        {
            paragraph "Choose the notification device, contact sensor (door/window/etc.) and the number of minutes before reminders are sent"
        }
        section("")
        {
            input "configNotification", "capability.notification", title: "Send reminders to these devices", multiple: true, required: true
        }
        section("")
        {
            input "configContact", "capability.contactSensor", title: "Send reminders for this contact sensor", multiple: false, required: true
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
        subscribe(configContact, "contact", contactEvent)
        if (configContact.currentState("contact").value == "open") {
            runIn(configMinutes.toInteger() * 60, contactReminder)
        }
    }
}

void updated() {
    unsubscribe()
    unschedule()
    installed()
}

void checkLabel() {
    if (configContact) {
        oldLabel = app.getLabel()
        newLabel = "${configContact} reminder after ${configMinutes} minutes"
        if (newLabel != oldLabel) {
            if (oldLabel) log.info "Simple Contact Reminder changed: ${oldLabel} -> ${newLabel}"
            app.updateLabel(newLabel)
        }
    }
}

void contactReminder() {
    String desc = "Contact Reminder: ${configContact} left open"
    log.info "${desc}"
    if (appEvents) sendEvent(name: "SSA", value: "Open", descriptionText: "${desc}")
    configNotification.deviceNotification("${desc}")

    if (configMinutesSubsequent) {
        runIn(configMinutesSubsequent.toInteger() * 60, contactReminder)
    }
}

void contactEvent(evt) {
    if (evt.value == "open") {
        runIn(configMinutes.toInteger() * 60, contactReminder)
    }
    else if (evt.value == "closed") {
        unschedule()
    }
}
