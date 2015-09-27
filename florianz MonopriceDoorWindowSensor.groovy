/**
 *  Monoprice Door/Window Sensor
 *
 *  Capabilities: Contact, Battery
 *
 *  Author: FlorianZ
 *  Date: 2014-02-24
 */

metadata {
    definition (name: "Monoprice Door/Window Sensor", author: "florianz") {
        capability "Battery"
        capability "Contact Sensor"
        capability "Sensor"
        capability "Button"
    }

    simulator {
        status "open":  "command: 2001, payload: FF"
        status "closed": "command: 2001, payload: 00"
    }

    tiles {
        standardTile("contact", "device.contact", width: 2, height: 2) {
            state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
            state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
        }
        standardTile("button", "device.button", inactiveLabel: false, width: 2, height: 2) {
			state "released", label: '', icon: "st.secondary.off", action: "push"
			state "pushed", label: '', icon: "st.custom.buttons.easy", backgroundColor: "#b82121", action: "push"
		}
        valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
            state "battery", label:'${currentValue}% battery', unit:""
        }


        main "contact"
        details(["contact", "button", "battery"])
    }
}

def getTimestamp() {
    new Date().time
}

def shouldRequestBattery() {
    if (!state.lastBatteryRequested) {
        return true
    }
    return (getTimestamp() - state.lastBatteryRequested) > 53*60*60*1000
}

def markLastBatteryRequested() {
    state.lastBatteryRequested = getTimestamp()
}

def parse(String description) {
    def result = []
    def cmd = zwave.parse(description, [0x20: 1, 0x80: 1, 0x84: 1])
    if (cmd) {
        // Did the sensor just wake up?
        if (cmd.CMD == "8407") {
            // Request the battery level?
            if (shouldRequestBattery()) {
                result << response(zwave.batteryV1.batteryGet())
                result << response("delay 1200")
            }
            result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
        }
        result << createEvent(zwaveEvent(cmd))
    }

    log.debug "Parse returned ${result}"
    return result
}

def sensorValueEvent(value) {
	if (value) {
		createEvent(name: "button", value: "released", descriptionText: "$device.displayName is released")
	} else {
		createEvent(name: "button", value: "pushed", descriptionText: "$device.displayName is pushed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    def map = [:]
    map.name = "contact"
    map.value = cmd.value ? "open" : "closed"
    map.descriptionText = cmd.value ? "${device.displayName} is open" : "${device.displayName} is closed"
    return map
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    def map = [:]
    map.value = "";
    map.descriptionText = "${device.displayName} woke up"
    return map
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    markLastBatteryRequested()
    
    def map = [:]
    map.name = "battery"
    map.unit = "%"
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} has a low battery"
        map.isStateChange = true
    } else {
        map.value = cmd.batteryLevel
    }
    return map
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd)
{
	def result = []
	if (cmd.notificationType == 0x07) {
		if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice door/window sensors
			result << sensorValueEvent(cmd.v1AlarmLevel)
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, displayed: false)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // Catch-all handler. The sensor does return some alarm values, which
    // could be useful if handled correctly (tamper alarm, etc.)
    [descriptionText: "Unhandled: ${device.displayName}: ${cmd}", displayed: false]
}

def push() {
    log.debug "pressed"

    sendEvent(name: "button", value: "pushed", displayed: true, isStateChange: true)
    sendEvent(name: "button", value: "released", displayed: true, isStateChange: true)
}
