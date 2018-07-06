package com.dNDTeam.seffectle.dataClasses

import com.google.gson.annotations.SerializedName

data class ServerResponse(
    //с таким тегом придёт ответ от сервера
    @SerializedName("responseData")
    //в такую переменную
    var responseData: String? = null
)