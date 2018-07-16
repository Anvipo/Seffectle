package com.dNDTeam.seffectle.dataClasses

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class ClassFromSchedule(
    val day_of_week: Int,
    val user_ID: String = "",
    var class_begin_time: String = "",
    var class_end_time: String = "",
    var class_type: String = "",
    var class_name: String = "",
    var room_name: String = "",
    var teacher_name: String = "",
    val class_serial_number: String = "",
    val schedule_creation_time: String = ""
) : Parcelable