// Groovy service implementations for DurHrServices

def submitTimesheet() {
    def timesheetId = context.timesheetId
    
    def timesheet = ec.entity.find("durion.hr.DurHrTimesheet")
            .condition("timesheetId", timesheetId).one()
    
    if (!timesheet) {
        ec.message.addError("Timesheet not found")
        return [success: false]
    }
    
    // Calculate total hours
    def timeEntries = ec.entity.find("durion.hr.DurHrTimeEntry")
            .condition("timesheetId", timesheetId).list()
    
    def totalHours = 0.0
    def regularHours = 0.0
    def overtimeHours = 0.0
    
    timeEntries.each { entry ->
        totalHours += entry.hoursWorked ?: 0.0
        overtimeHours += entry.overtimeHours ?: 0.0
    }
    
    regularHours = totalHours - overtimeHours
    
    // Update timesheet
    timesheet.totalHours = totalHours
    timesheet.regularHours = regularHours
    timesheet.overtimeHours = overtimeHours
    timesheet.status = "submitted"
    timesheet.submittedDate = ec.user.nowTimestamp
    timesheet.update()
    
    return [success: true]
}

def approveTimesheet() {
    def timesheetId = context.timesheetId
    
    def timesheet = ec.entity.find("durion.hr.DurHrTimesheet")
            .condition("timesheetId", timesheetId).one()
    
    if (!timesheet) {
        ec.message.addError("Timesheet not found")
        return [success: false]
    }
    
    if (timesheet.status != "submitted") {
        ec.message.addError("Timesheet must be submitted before approval")
        return [success: false]
    }
    
    // Approve timesheet
    timesheet.status = "approved"
    timesheet.approvedDate = ec.user.nowTimestamp
    timesheet.approvedBy = ec.user.userId
    timesheet.update()
    
    return [success: true]
}

def calculatePersonHoursForPeriod() {
    def personId = context.personId
    def startDate = context.startDate
    def endDate = context.endDate
    
    def timeEntries = ec.entity.find("durion.hr.DurHrTimeEntry")
            .condition("personId", personId)
            .condition("workDate", ">=", startDate)
            .condition("workDate", "<=", endDate)
            .list()
    
    def totalHours = 0.0
    def overtimeHours = 0.0
    
    timeEntries.each { entry ->
        totalHours += entry.hoursWorked ?: 0.0
        overtimeHours += entry.overtimeHours ?: 0.0
    }
    
    return [
        totalHours: totalHours,
        regularHours: totalHours - overtimeHours,
        overtimeHours: overtimeHours
    ]
}
