package com.dNDTeam.seffectle.db

import com.dNDTeam.seffectle.dataClasses.User
import org.jetbrains.anko.db.MapRowParser

object UserParser : MapRowParser<User> {
    override fun parseRow(columns: Map<String, Any?>): User {
        return User("${columns.getValue(ScheduleMSQLOH.KEY_USER_ID)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_USER_VK_ID)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_FIRST_NAME)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_LAST_NAME)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_UNIVERSITY_NAME)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_FACULTY_NAME)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_GRADUATION_YEAR)}",
                "${columns.getValue(ScheduleMSQLOH.KEY_COURSE_NUMBER)}")
    }
}