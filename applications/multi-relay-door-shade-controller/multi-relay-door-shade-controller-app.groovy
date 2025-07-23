//
// Copyright (c) 2020-2025, Denny Page
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
// Virtual Multi-relay Door / Shade Controller (Parent application)
//
// Version 1.0.0    Initial release
// Version 1.0.1    Child device should be a component device
// Version 2.0.0    Code restructure and cleanup
// Version 3.0.0    Allow reporting of external position changes
//

definition(
    name: "Multi-Relay Door/Shade Controller",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Create a virtual door or shade device using multiple relays",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/multi-relay-door-shade-controller/multi-relay-door-shade-controller-app.groovy",
    singleInstance: false,
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Multi-relay Door/Shade Controller", install: true, uninstall: true, refreshInterval: 0)
    {
        section("")
        {
            input "deviceName", "text", title: "Device name (label)", multiple: false, required: true
            input name: "travelTime", title: "Open/Close travel time in seconds (position accuracy depends on this)",
                type: "decimal", defaultValue: "30.0", range: "1..300", required: true
        }
        section("")
        {
            paragraph "Choose the relays used to control the door or shade. The Open and Close relays are required. The Stop relay is optional."
            paragraph "If the Stop relay is defined, the device will support any arbitrary position setting from 0 to 100. If not defined, the device will treat all position requests as either fully closed (0) or fully open (100)."
            paragraph "<b>All relays should have automatic off timers</b>"
            input "openRelay", "capability.switch", title: "Open relay", multiple: false, required: true
            input "closeRelay", "capability.switch", title: "Close relay", multiple: false, required: true
            input "stopRelay", "capability.switch", title: "Stop relay (optional)", multiple: false, required: false
        }
    }
}

void updated() {
    log.info "Updated"

    app.updateLabel("Multi-Relay Door/Shade Controller - ${deviceName}")

    String childId = "mrdsc-${app.id}"
    child = getChildDevice(childId)
    if (child == null) {
        child = addChildDevice("cococafe", "Multi-Relay Door/Shade Controller", childId, [isComponent: true])
    }
    child.setLabel(deviceName)

    child.updateDataValue("travelTime", travelTime.toString())
    child.updateDataValue("openRelay", openRelay.toString())
    child.updateDataValue("closeRelay", closeRelay.toString())
    if (stopRelay) {
        child.updateDataValue("stopRelay", stopRelay.toString())
    }
    else {
        child.removeDataValue("stopRelay")
    }
}

void installed() {
    updated()
}

void uninstalled() {
    deleteChildDevice("mrdsc-${app.id}")
}

void move(Boolean moveOpen) {
    if (stopRelay) {
        // Safety check
        if (stopRelay.currentValue("switch", true) == "on") {
            log.warn "${deviceName} - Stop Relay did not auto-off"
            stopRelay.off()
        }
    }

    if (moveOpen) {
        // Safety check
        if (closeRelay.currentValue("switch", true) == "on") {
            log.warn "${deviceName} - Close Relay did not auto-off"
            closeRelay.off()
        }

        openRelay.on()
    }
    else {
        // Safety check
        if (openRelay.currentValue("switch", true) == "on") {
            log.warn "${deviceName} - Open Relay did not auto-off"
            openRelay.off()
        }

        closeRelay.on()
    }
}

void stop() {
    if (stopRelay) {
        // Safety check
        if (openRelay.currentValue("switch", true) == "on") {
            log.warn "${deviceName} - Open Relay did not auto-off"
            openRelay.off()
        }
        if (closeRelay.currentValue("switch", true) == "on") {
            log.warn "${deviceName} - Close Relay did not auto-off"
            closeRelay.off()
        }

        stopRelay.on()
    }
}
