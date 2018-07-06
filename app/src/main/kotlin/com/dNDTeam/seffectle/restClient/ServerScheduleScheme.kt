package com.dNDTeam.seffectle.restClient

import com.google.gson.annotations.SerializedName

data class ServerScheduleScheme(
        @SerializedName("_id")
        val id: String,

        @SerializedName("roomName")
        val roomName: String,

        @SerializedName("classBeginTime")
        val classBeginTime: String,

        @SerializedName("classEndTime")
        val classEndTime: String,

        @SerializedName("className")
        val className: String,

        @SerializedName("classSerialNumber")
        val classSerialNumber: String,

        @SerializedName("classType")
        val classType: String,

        @SerializedName("dayOfWeek")
        val dayOfWeek: Int,

        @SerializedName("teacherName")
        val teacherName: String,

        @SerializedName("userID")
        val userID: String,

        @SerializedName("__v")
        val v: Int,

        @SerializedName("scheduleCreationTime")
        val scheduleCreationTime: String
)
