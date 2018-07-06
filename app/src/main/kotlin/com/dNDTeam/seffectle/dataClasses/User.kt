package com.dNDTeam.seffectle.dataClasses

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class User(val id: String = "",
                val vkId: String = "",
                val firstName: String = "",
                val lastName: String = "",
                val universityName: String = "",
                val facultyName: String = "",
                val graduationYear: String = "",
                val courseNumber: String = "") : Parcelable