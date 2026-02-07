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
    
    // Collect time entry IDs for this timesheet and call pos-people to execute approval
    def timeEntries = ec.entity.find("durion.hr.DurHrTimeEntry")
            .condition("timesheetId", timesheetId).list()

    if (!timeEntries) {
        ec.message.addError("No time entries found for timesheet")
        return [success: false]
    }

    def entryIds = timeEntries.collect { it.timeEntryId }

    def baseUrl = System.getenv('POS_PEOPLE_BASE_URL') ?: 'http://pos-people:8080'
    def approveUrl = "${baseUrl}/v1/people/timeEntries/approve"

    def requestBody = [decisions: entryIds.collect { [timeEntryId: it] }]
    def jsonBody = groovy.json.JsonOutput.toJson(requestBody)

    try {
        def url = new URL(approveUrl)
        def conn = url.openConnection()
        conn.setRequestMethod('POST')
        conn.setDoOutput(true)
        conn.setRequestProperty('Content-Type', 'application/json')

        // propagate or create a correlation id
        def corr = null
        try { corr = ec.web.request.getHeader('X-Correlation-Id') } catch (e) { corr = null }
        if (!corr) corr = java.util.UUID.randomUUID().toString()
        conn.setRequestProperty('X-Correlation-Id', corr)

        conn.getOutputStream().withWriter('UTF-8') { it << jsonBody }

        def respCode = conn.getResponseCode()
        if (respCode >= 200 && respCode < 300) {
            // Do not perform local approval logic here; pos-people is authoritative.
            return [success: true]
        } else {
            def errStream = conn.getErrorStream()
            def errText = errStream ? errStream.getText('UTF-8') : "HTTP ${respCode}"
            ec.message.addError("Approval failed: ${errText}")
            return [success: false]
        }
    } catch (Exception e) {
        ec.message.addError("Approval request failed: ${e.message}")
        return [success: false]
    }
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
