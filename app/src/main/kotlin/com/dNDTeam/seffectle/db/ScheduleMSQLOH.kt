package com.dNDTeam.seffectle.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class ScheduleMSQLOH(context: Context) : ManagedSQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
) {
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "ScheduleAppDB.db"

        const val SCHEDULE_TABLE_NAME = "schedules"
        const val KEY_CLASS_ID = "_id"
        const val KEY_DAY_OF_WEEK = "day_of_week"
        const val KEY_CLASS_BEGIN_TIME = "class_begin_time"
        const val KEY_CLASS_END_TIME = "class_end_time"
        const val KEY_CLASS_TYPE = "class_type"
        const val KEY_CLASS_NAME = "class_name"
        const val KEY_TEACHER_NAME = "teacher_name"
        const val KEY_ROOM_NAME = "room_name"
        const val KEY_CLASS_SERIAL_NUMBER = "class_serial_number"
        const val KEY_SCHEDULE_CREATION_TIME = "schedule_creation_time"

        const val USER_INFO_TABLE_NAME = "user_info"
        const val KEY_USER_ID = "_id"
        const val KEY_USER_VK_ID = "vk_ID"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_UNIVERSITY_NAME = "university_name"
        const val KEY_FACULTY_NAME = "faculty_name"
        const val KEY_GRADUATION_YEAR = "graduation_year"
        const val KEY_COURSE_NUMBER = "course_number"

        const val SERVER_INFO_TABLE_NAME = "server_info"
        const val KEY_SERVER_IP = "server_IP"
        const val KEY_SERVER_HAS_USER_INFO = "server_has_user_info"

        private var instance: ScheduleMSQLOH? = null

        @Synchronized
        fun getInstance(ctx: Context): ScheduleMSQLOH {
            if (instance == null)
                instance = ScheduleMSQLOH(ctx.applicationContext)

            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(SCHEDULE_TABLE_NAME, true,
                KEY_CLASS_ID to INTEGER + NOT_NULL + DEFAULT("0") + PRIMARY_KEY + AUTOINCREMENT + UNIQUE,
                KEY_DAY_OF_WEEK to INTEGER + NOT_NULL,
                KEY_CLASS_BEGIN_TIME to TEXT + NOT_NULL,
                KEY_CLASS_END_TIME to TEXT + NOT_NULL,
                KEY_CLASS_TYPE to TEXT + NOT_NULL,
                KEY_CLASS_NAME to TEXT + NOT_NULL,
                KEY_ROOM_NAME to TEXT + NOT_NULL,
                KEY_TEACHER_NAME to TEXT + NOT_NULL,
                KEY_CLASS_SERIAL_NUMBER to TEXT + NOT_NULL,
                KEY_SCHEDULE_CREATION_TIME to TEXT + NOT_NULL)

        db.createTable(USER_INFO_TABLE_NAME, true,
                KEY_USER_ID to INTEGER + NOT_NULL + DEFAULT("0") + PRIMARY_KEY + AUTOINCREMENT + UNIQUE,
                KEY_USER_VK_ID to INTEGER + UNIQUE + NOT_NULL,
                KEY_FIRST_NAME to TEXT + NOT_NULL,
                KEY_LAST_NAME to TEXT + NOT_NULL,
                KEY_UNIVERSITY_NAME to TEXT + NOT_NULL,
                KEY_FACULTY_NAME to TEXT + NOT_NULL,
                KEY_GRADUATION_YEAR to TEXT + NOT_NULL,
                KEY_COURSE_NUMBER to TEXT + NOT_NULL)

        db.createTable(SERVER_INFO_TABLE_NAME, true,
            KEY_SERVER_IP to TEXT + NOT_NULL + UNIQUE,
            KEY_SERVER_HAS_USER_INFO to TEXT + NOT_NULL + UNIQUE
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        if(newVersion>oldVersion)
//            copyDatabase()

        db.dropTable(SCHEDULE_TABLE_NAME, true)
        db.dropTable(USER_INFO_TABLE_NAME, true)
        db.dropTable(SERVER_INFO_TABLE_NAME, true)
        onCreate(db)
    }
}

// Access property for Context
val Context.database: ScheduleMSQLOH
    get() = ScheduleMSQLOH.getInstance(applicationContext)
