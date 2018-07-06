package com.dNDTeam.seffectle.dataClasses

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class ClassFromSchedule(
        val dayOfWeek: Int,
        val userID: String = "",
        var classBeginTime: String = "",
        var classEndTime: String = "",
        var classType: String = "",
        var className: String = "",
        var roomName: String = "",
        var teacherName: String = "",
        val classSerialNumber: String = "",
        val scheduleCreationTime: String = "") : Parcelable