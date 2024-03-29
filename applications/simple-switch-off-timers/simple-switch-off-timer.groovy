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
    name: "Simple Switch Off Timer",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Turn a switch off after it has been on for a number of minutes",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/simple-switch-off-timers/simple-switch-off-timer.groovy",
    parent: "cococafe:Simple Switch Off Timers",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Simple Switch Off Timer", install: true, uninstall: true, refreshInterval: 0)
    {
        // Ensure label is correct in case the device has changed label
        checkLabel()

        section("")
        {
            paragraph "Choose the switch and the number of minutes before the switch is automatically turned off"
        }
        section("")
        {
            input "configSwitch", "capability.switch", title: "Automatically turn off this switch", multiple: false, required: true
        }
        section("")
        {
            input "configMinutes", "number", title: "Number of minutes before switch is turned off", required: true
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
            runIn(configMinutes.toInteger() * 60, switchOff)
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
        newLabel = "${configSwitch} off after ${configMinutes} minutes"
        if (newLabel != oldLabel) {
            if (oldLabel) log.info "Simple Switch Off Timer changed: ${oldLabel} -> ${newLabel}"
            app.updateLabel(newLabel)
        }
    }
}

void switchOff() {
    String desc = "Switch Off Timer: ${configSwitch} off after ${configMinutes} minutes"
    log.info "${desc}"
    if (appEvents) sendEvent(name: "SSA", value: "Off", descriptionText: "${desc}")
    configSwitch.off()
}

void switchEvent(evt) {
    if (evt.value == "on") {
        runIn(configMinutes.toInteger() * 60, switchOff)
    }
    else if (evt.value == "off") {
        unschedule()
    }
}
