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
// Version 2.0.0    Code restructure and cleanup
//

definition(
    name: "Simple Contact Reminders",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Send reminders for contact sensors (doors/windows/etc.) that have been left open",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/simple-contact-reminders/simple-contact-reminders.groovy",
    singleInstance: true,
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Simple Contact Reminders", install: true, uninstall: true, refreshInterval: 0)
    {
        if (app.getInstallationState() == 'COMPLETE') {
            // Ensure child labels are correct in case a device has changed label
            childApps.each {
                child -> child.checkLabel()
            }

            section("")
            {
                paragraph "Send reminders about contact sensors (doors/windows/etc.) that have been left open."
            }
            section
            {
                app(name: "childApps", appName: "Simple Contact Reminder", namespace: "cococafe", title: "Create A New Simple Contact Reminder", multiple: true)
            }
        }
        else
        {
            section("")
            {
                paragraph "<b>Click Done to complete the installation.</b>"
            }
        }
    }
}

void installed() {
    log.info "There are ${childApps.size()} Simple Contact Reminders"
    childApps.each {
        child -> log.info "  Simple Contact Reminder: ${child.label}"
    }
}

void updated() {
    unsubscribe()
    installed()
}
