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
// Idle Node Refreshers (Parent application)
//
// Version 1.0.0    Initial release
//

definition(
    name: "Idle Node Refreshers",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Refresh idle nodes in the network",
    category: "Convenience",
    singleInstance: true,
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
    dynamicPage(name: "", title: "Refresh idle nodes in the network", install: true, uninstall: true, refreshInterval: 0)
    {
        if (app.getInstallationState() == 'COMPLETE')
        {
            section
            {
                app(name: "childApps", appName: "Idle Node Refresher", namespace: "cococafe", title: "Create an Idle Node Refresher", multiple: true)
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

def installed()
{
    log.info "There are ${childApps.size()} Idle Node Refreshers"
    childApps.each
    {
        child -> log.info "  Idle Node Refresher: ${child.label}"
    }
}

def updated()
{
    installed()
}
