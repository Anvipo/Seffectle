package com.dNDTeam.seffectle.db

import android.content.Context
import com.dNDTeam.seffectle.dataClasses.User
import com.dNDTeam.seffectle.restClient.ServerInfo
import com.dNDTeam.seffectle.vk.VKUser
import org.jetbrains.anko.db.MapRowParser
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update

internal fun getIPFrom(database: ScheduleMSQLOH) {
    if (ServerInfo.serverIP.isBlank()) {
        val temp = getServerIPFromDB(database)

        if (temp != null)
            ServerInfo.serverIP = temp
    }

    if (ServerInfo.serverIP.isBlank())
        database.use {
            val wasUpdated = update(
                ScheduleMSQLOH.SERVER_INFO_TABLE_NAME,
                ScheduleMSQLOH.KEY_SERVER_IP to ServerInfo.defaultIP
            )
                .exec() > 0

            if (!wasUpdated)
            //эта схема иногда называется upsert
            //т.е. если ничего не было обновлено, то вставим такие данные
                insert(
                    ScheduleMSQLOH.SERVER_INFO_TABLE_NAME,
                    ScheduleMSQLOH.KEY_SERVER_IP to ServerInfo.defaultIP
                )

            ServerInfo.serverIP = ServerInfo.defaultIP
        }
}

internal fun getServerIPFromDB(database: ScheduleMSQLOH): String? {
    return database.use {
        return@use select(ScheduleMSQLOH.SERVER_INFO_TABLE_NAME)
            .parseOpt(object : MapRowParser<String> {
                override fun parseRow(columns: Map<String, Any?>) =
                    columns.getValue(ScheduleMSQLOH.KEY_SERVER_IP).toString()
            })
    }
}

internal fun getVKUserInfoFrom(database: ScheduleMSQLOH, context: Context) {
    if (VKUser.vkAccountOwner.id.isBlank())
        database.use {
            val temp: User? = select(ScheduleMSQLOH.USER_INFO_TABLE_NAME)
                //метод whereArgs защищает (судя по исходникам) от SQL-инъекций
                .whereArgs(
                    "(${ScheduleMSQLOH.KEY_USER_ID} = {${ScheduleMSQLOH.KEY_USER_ID}})",
                    ScheduleMSQLOH.KEY_USER_ID to "0"
                )
                .parseOpt(UserParser)

            if (temp != null)
                VKUser.vkAccountOwner = temp
        }

    if (VKUser.vkAccountOwner.id.isBlank())
    //запускаем асинхронно, не блокируя UI, получать данные о пользователе
        VKUser.asyncGetVkAccountOwnerInfo(context)
}

//получить пары нужного дня
internal fun getClassesList(day: Int, context: Context) = context.database.use {
    select(ScheduleMSQLOH.SCHEDULE_TABLE_NAME)
        //метод whereArgs защищает (судя по исходникам) от SQL-инъекций
        .whereArgs(
            "(${ScheduleMSQLOH.KEY_DAY_OF_WEEK} = {${ScheduleMSQLOH.KEY_DAY_OF_WEEK}})",
            ScheduleMSQLOH.KEY_DAY_OF_WEEK to day
        )
        .parseList(ClassFromScheduleParser)
}