package eu.devexpert.madhouse.alert

class Alert {
    String alias = "madhouse_core"
    String message
    String description
    AlertPriority priority = AlertPriority.P5
    String note = "Triggered internal alert"

    Alert(AlertPriority priority, String message, String description) {
        this.priority = priority
        this.message = message
        this.description = description
    }
}
