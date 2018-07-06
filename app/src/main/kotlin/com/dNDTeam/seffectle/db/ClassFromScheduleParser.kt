package com.dNDTeam.seffectle.db

import com.dNDTeam.seffectle.dataClasses.ClassFromSchedule
import com.dNDTeam.seffectle.vk.VKUser
import org.jetbrains.anko.db.MapRowParser

object ClassFromScheduleParser : MapRowParser<ClassFromSchedule> {
    override fun parseRow(columns: Map<String, Any?>) =
            ClassFromSchedule(
                    "${columns.getValue(ScheduleMSQLOH.KEY_DAY_OF_WEEK)}".toInt(),
                    VKUser.vkAccountOwner.vkId,
                    "${columns.getValue(ScheduleMSQLOH.KEY_CLASS_BEGIN_TIME)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_CLASS_END_TIME)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_CLASS_TYPE)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_CLASS_NAME)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_ROOM_NAME)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_TEACHER_NAME)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_CLASS_SERIAL_NUMBER)}",
                    "${columns.getValue(ScheduleMSQLOH.KEY_SCHEDULE_CREATION_TIME)}")
}