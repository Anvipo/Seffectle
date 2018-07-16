package com.dNDTeam.seffectle.restClient

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ServerScheduleModel(
    @SerializedName("id")
    @Expose
    val id: String,

    @SerializedName("class_begin_time")
    @Expose
    val classBeginTime: String,

    @SerializedName("class_end_time")
    @Expose
    val classEndTime: String,

    @SerializedName("class_name")
    @Expose
    val className: String,

    @SerializedName("class_serial_number")
    @Expose
    val classSerialNumber: String,

    @SerializedName("class_type")
    @Expose
    val classType: String,

    @SerializedName("day_of_week")
    @Expose
    val dayOfWeek: Int,

    @SerializedName("room_name")
    @Expose
    val roomName: String,

    @SerializedName("schedule_creation_time")
    @Expose
    val scheduleCreationTime: String,

    @SerializedName("teacher_name")
    @Expose
    val teacherName: String,

    @SerializedName("user_ID")
    @Expose
    val userID: String
)
