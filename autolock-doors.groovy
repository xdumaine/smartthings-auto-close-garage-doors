definition(
    name: "Auto Close Garage Doors",
    namespace: "xdumaine",
    author: "Xander Dumaine",
    description: "Automatically close doors a specific door after X minutes after opened. Optionally disable auto locking when in a specific mode.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/xdumaine-random-junk/garage.png",
    iconX2Url: "https://s3.amazonaws.com/xdumaine-random-junk/garage.png"
)

preferences{
    section("Select the Garage Doors") {
        input "garages", "capability.doorControl", required: true, multiple: true
    }
    section("Automatically close the doors x minutes after opened...") {
        input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section("Disable auto close when...") {
        input "modes", "mode", title: "Select mode(s) (optional)", multiple: true
    }
    section( "Notifications for Success" ) {
        input("recipients", "contact", title: "Send notifications to", required: false, multiple: true) {
            input "sendPushSuccess", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneSuccess", "phone", title: "Send a Text Message?", required: false
        }
    }
    section( "Notifications for Errors" ) {
        input("recipients", "contact", title: "Send notifications to", required: false, mulitple: true) {
            input "sendPushError", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneError", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed () {
    initialize()
}

def updated () {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize () {
    log.debug "Settings: ${settings}"
    try {
        garages.each {
            subscribe(it, "close", doorHandler, [filterEvents: false])
            it.close()
        }
    } catch (all) {
        notifyError("failed to initialize")
    }
}

def notifyError (msg) {
    if (sendPushError == 'Yes') {
        sendPush("Auto Close Garage Doors: ${msg}")
    }
    if (phoneError) {
        sendSms(phoneError, "Auto Close Garage Doors: ${msg}")
    }
}
def notifySuccess (msg) {
  if (sendPushSuccess == 'Yes') {
      sendPush("Auto Close Garage Doors: ${msg}")
  }
  if (phoneSuccess) {
      sendSms(phoneSuccess, "Auto Close Garage Doors: ${msg}")
  }
}

def closeDoors () {
    if (modes.contains(location.mode)) {
        notifyError("doors not closed because mode is ${location.mode}")
    } else {
        log.debug "Closing the doors."
        try {
            garages.each { it.close() }
            notifySuccess("doors closed.")
        } catch (all) {
            notifyError("encountered an error closing the door(s)")
        }
    }
}

def doorHandler (evt) {
    log.debug("Handling event: " + evt.value)
    def open = garages.find { it.latestValue("door") != "closed" }
    if (open && evt.value != "open") {
        log.debug("Scheduling close because ${open} was open")
        try {
            runIn((minutesLater * 60), closeDoors) // ...schedule (in minutes) to close.
            notifySuccess("scheduled doors to close in ${minutesLater} minutes")
        } catch (all) {
            notifyError("failed to schedule the close action. Door(s) will not close.")
        }
    } else {
        log.debug "Not scheduling close because ${evt.value}"
    }
}
