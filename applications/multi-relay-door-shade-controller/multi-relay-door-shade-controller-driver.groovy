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
// Virtual Multi-relay Door / Shade Controller (Child driver)
//
// Version 1.0.0    Initial release
// Version 1.1.0    When asked to fully open or close, fire the
//                  relay even if we think we are already there.
//                  This helps reduce confusion if the device has
//                  been operated with a different controller.
// Version 1.1.1    Normalize logging
// Version 1.2.0    Add Actuator capability to allow device to
//                  appear in selection lists.
// Version 1.2.1    Disable DoorControl to allow operation with
//                  percentage reporting open in Apple Home.
// Version 2.0.0    Code restructure and cleanup
// Version 3.0.0    Allow reporting of external position changes
//

metadata
{
    definition(
        name: "Multi-Relay Door/Shade Controller", namespace: "cococafe", author: "Denny Page",
        importUrl: "https://raw.githubusercontent.com/dennypage/hubitat/master/applications/multi-relay-door-shade-controller/multi-relay-door-shade-controller-driver.groovy"
    )
    {
        capability "Actuator"
        capability "WindowShade"
        //capability "DoorControl"
        command "stop"
        command "positionChangedExternally", [[name: "newPosition*", type: "NUMBER", desription: "New position"]]
    }
}

preferences
{
    input name: "logEnable", title: "Enable debug logging", type: "bool", defaultValue: true
    input name: "txtEnable", title: "Enable description logging", type: "bool", defaultValue: true
}

void logsOff() {
    device.updateSetting("logEnable", [value:"false", type:"bool"])
    log.warn "debug logging disabled"
}

void installed() {
    state.moveBegin = 0
    state.oldPosition = 0
    state.newPosition = 100

    sendEvent(name: "door", value: "unknown")
    sendEvent(name: "windowShade", value: "unknown")
    sendEvent(name: "position", value: "50")

    runIn(1800, logsOff)
}

void open() {
    if (logEnable) log.debug "open()"
    setPosition(100)
}

void close() {
    if (logEnable) log.debug "close()"
    setPosition(0)
}

void stop() {
    if (logEnable) log.debug "stop()"
    unschedule(moveComplete)
    moveComplete([sendStop: true])
}

void startPositionChange(String direction) {
    if (direction == "open") {
        open()
    }
    else if (direction == "close") {
        close()
    }
}

void stopPositionChange() {
    stop()
}

void setPosition(BigDecimal newPosition) {
    if (logEnable) log.debug "setPosition(${newPosition})"

    if (newPosition < 0) {
        newPosition = 0
    }
    else if (newPostion > 100) {
        newPostion = 100
    }

    if (state.moveBegin) {
        if (newPosition == state.newPosition) {
            return
        }
        // Stop and allow state update
        stop()
        pauseExecution(250)
    }

    if (getDataValue("stopRelay") == null) {
        // If we don't have a stop relay, it's either fully open or fully closed
        if (newPosition > 0) {
            newPosition = 100
        }
    }

    BigDecimal oldPosition = device.currentValue("position") ?: 0
    if (newPosition == oldPosition) {
        // There is the possibility that the door or shade has
        // been changed without our knowledge. For example if we
        // think the door is closed but someone opened the door
        // using a wall switch. So to help reduce confusion, even
        // if we think we are already fully closed or fully open
        // we fire the appropriate relay anyway.
        if (newPosition == 0 || newPosition >= 100) {
            getParent().move(newPosition ? true : false)
        }
        return
    }

    Boolean moveOpen
    Boolean sendStop
    BigDecimal delta
    if (newPosition > oldPosition) {
        moveOpen = true
        newPosition = Math.min(newPosition.toInteger(), (Integer) 100)
        delta = newPosition - oldPosition
        sendStop = newPosition < 99 ? true : false
    }
    else {
        moveOpen = false
        newPosition = Math.max(newPosition.toInteger(), (Integer) 0)
        delta = oldPosition - newPosition
        sendStop = newPosition > 0 ? true : false
    }
    Long millis = getDataValue("travelTime").toBigDecimal() * 10.0 * delta

    if (logEnable) log.debug "moving position from ${oldPosition} to ${newPosition} (${millis}ms)"

    status = moveOpen ? "opening" : "closing"
    sendEvent(name: "door", value: status)
    sendEvent(name: "windowShade", value: status)
    if (txtEnable) log.info "${device.displayName}: ${status}"

    getParent().move(moveOpen)
    state.moveBegin = now()
    state.oldPosition = oldPosition
    state.newPosition = newPosition

    runInMillis(millis.toLong(), moveComplete, [data: [sendStop: sendStop]])
}

void moveComplete(Map args) {
    if (logEnable) log.debug "moveComplete(${args})"

    if (!state.moveBegin) {
        return
    }

    // Note that we do not send a stop command if we are moving to either
    // fully open or fully closed. This helps address the natural errors
    // that occur when using time based estimates of position.
    if (args.sendStop) getParent().stop()

    BigDecimal elapsed = now() - state.moveBegin
    state.moveBegin = 0

    Integer delta = (elapsed / (getDataValue("travelTime").toBigDecimal() * 10.0))
    if (state.newPosition > state.oldPosition) {
        newPosition = Math.min(state.oldPosition.toInteger() + delta, (Integer) 100)
    }
    else {
        newPosition = Math.max(state.oldPosition.toInteger() - delta, (Integer) 0)
    }

    sendEvent(name: "door", value: newPosition > 0 ? "open" : "closed")
    if (newPosition == 0) {
        status = "closed"
    }
    else if (newPosition == 100) {
        status = "open"
    }
    else {
        status = "partially open"
    }
    sendEvent(name: "windowShade", value: status)
    sendEvent(name: "position", value: newPosition)
    if (txtEnable) log.info "${device.displayName}: ${status} (${newPosition})"
}

void positionChangedExternally(Number newPosition) {
    unschedule(moveComplete)
    state.moveBegin = 0

    if (newPosition < 0) {
        newPosition = 0
    }
    else if (newPosition > 100) {
        newPosition = 100
    }

    sendEvent(name: "door", value: newPosition > 0 ? "open" : "closed")
    if (newPosition == 0) {
        status = "closed"
    }
    else if (newPosition == 100) {
        status = "open"
    }
    else {
        status = "partially open"
    }

    sendEvent(name: "windowShade", value: status)
    sendEvent(name: "position", value: newPosition)
    if (txtEnable) log.info "${device.displayName} changed manually: ${status} (${newPosition})"
}
