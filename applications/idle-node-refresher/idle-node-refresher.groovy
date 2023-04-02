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
// Idle Node Refresher (Child application)
//
// Version 1.0.0    Initial release
// Version 1.0.1    Bug fix - incorrect use of idle interval instead of
//                  refresh interval for min sleep.
// Version 1.1.0    Add initialization function to ensure worker schedule
//                  isn't lost during reboot. Use seconds for calculations
//                  rather than milliseconds. Avoid runInMillis.
// Version 1.1.1    Delay actions on reboot
// Version 1.1.2    Change initialized function to systemStart subscription.
//                  Initialized isn't called for apps.
// Version 2.0.0    Code restructure and cleanup
//

definition(
    name: "Idle Node Refresher",
    namespace: "cococafe",
    author: "Denny Page",
    description: "Refresh idle nodes in the network",
    category: "Utility",
    importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/idle-node-refresher/idle-node-refresher.groovy",
    parent: "cococafe:Idle Node Refreshers",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences
{
    page(name: "configPage")
}

def configPage() {
    dynamicPage(name: "", title: "Idle Node Refresher", install: true, uninstall: true, refreshInterval: 0)
    {
        section("")
        {
            input "name", "text", title: "<b>Refresher name</b>", multiple: false, required: true
        }
        section("")
        {
            paragraph "<b>Nodes to be monitored</b>: The nodes to be monitored and refreshed when idle. " +
                "Selected nodes must have a refresh() command."
            input "nodes", "capability.*", title: "Nodes to be monitored", multiple: true, required: true
        }
        section("")
        {
            paragraph "<b>Inactivity Idle Hours</b>: The number of hours of inactivity before a node " +
                "is considered idle. A reasonable value for this is 24 (1 day) or greater. The " +
                "minimum value is 1."
            input "idleHours", "number", title: "Inactivity Idle Hours", required: true, defaultValue: 25
        }
        section("")
        {
            paragraph "<b>Refresh Interval Minutes</b>: The minimum number of minutes between idle " +
                      "node refreshes. Only one node will be refreshed per interval. A reasonable " +
                      "value for this is 5 or greater. The minimum value is 2."
            input "refreshMinutes", "number", title: "Refresh Interval Minutes", required: true, defaultValue: 10
        }
        section("")
        {
            paragraph "<b>Refresh Switch</b>: If this option is enabled and the device being " +
                "refreshed has Switch capability, one minute after calling refresh() on the device " +
                "we will read the current value of the switch and call on() or off() as appropriate " +
                "to refresh the current state. The advantage of refreshing switch state is that it " +
                "will trigger Z-Wave Plus devices to update their route if appropriate. The " +
                "disadvantage of switch refresh is additional network traffic. Additionally, if the " +
                "state in the driver is stale due to prior communication issues, it could result in " +
                "the switch mysteriously turning on or off."
            input "refreshSwitch", "bool", title: "Refresh switch state (on/off)", defaultValue: false
        }
    }
}

void installed() {
    app.updateLabel(name)
    updateLastCache()
    updateSortedIndex()
    runIn(1, refreshNode)
}

void updated() {
    subscribe(location, "systemStart", hubRestartHandler)
    unschedule()

    if (idleHours < 1) {
        app.updateSetting("idleHours", 1)
        log.warn "Input value for Inactivity Idle Hours too low: value changed to 1 hour"
    }
    if (refreshMinutes < 2) {
        app.updateSetting("refreshMinutes", 2)
        log.warn "Input value for Refresh Interval Minutes too low: value changed to 2 minutes"
    }

    installed()
}

void hubRestartHandler(evt) {
    unschedule()
    runIn(60, installed)
}

private Long lastNodeActivity(Integer node) {
    // 2020-11-17 01:56:54+0000
    return Date.parse("yyyy-MM-dd HH:mm:ssZ", "${nodes[node].getLastActivity()}").getTime() / 1000
}

void updateLastCache() {
    state.lastCache = []
    (0 .. nodes.size() - 1).each { node ->
        state.lastCache[node] = lastNodeActivity(node)
    }
}

void updateSortedIndex() {
    List indexList = []
    (0 .. nodes.size() - 1).each { node ->
        indexList[node] = node
    }
    state.sortedIndex = indexList.sort { a, b -> state.lastCache[a] <=> state.lastCache[b] }
}

void switchRefresh(Map args) {
    node = args.node
    try {
        String state = nodes[node].currentState("switch", true).value
        log.info "Node ${nodes[node].getDisplayName()}: Refreshing switch state (${state})..."

        if (state && state == "on") {
            nodes[node].on()
        }
        else
        {
            nodes[node].off()
        }
    }
    catch (e) {
        log.warn "Node ${nodes[node].getDisplayName()}: ${e}"
    }
}

void refreshNode() {
    Long now = now() / 1000
    Long idleSeconds = idleHours * 3600
    Long refreshSeconds = refreshMinutes * 60
    Long seconds

    for (int i = 0; i < state.sortedIndex.size(); i++) {
        node = state.sortedIndex[i]

        seconds = now - state.lastCache[node]
        if (seconds >= idleSeconds) {
            // Update our cached value
            state.lastCache[node] = lastNodeActivity(node)
            seconds = (now - state.lastCache[node])

            if (seconds >= idleSeconds) {
                // Put the node at the end of the line
                // NB: Even if refresh() does not update lastActivity, we won't refresh
                //     the node again until idleHours has expired
                state.lastCache[node] = now

                try {
                    Integer hours = seconds / 3600
                    log.info "Node ${nodes[node].getDisplayName()}: last activity was ${hours} hours ago. Refreshing..."
                    nodes[node].refresh()
                }
                catch (e) {
                    log.warn "Node ${nodes[node].getDisplayName()}: ${e}"
                }

                // Refresh switch state if requested
                if (refreshSwitch && nodes[node].hasCapability("Switch")) {
                    runIn(60, switchRefresh, [data: [node: node]])
                }

                break
            }
        }
    }

    // Resort to integrate any activity cache changes
    updateSortedIndex()

    // Schedule our next refresh
    seconds = idleSeconds - (now - state.lastCache[state.sortedIndex[0]])
    if (seconds < refreshSeconds) seconds = refreshSeconds
    runIn(seconds, refreshNode)
}
