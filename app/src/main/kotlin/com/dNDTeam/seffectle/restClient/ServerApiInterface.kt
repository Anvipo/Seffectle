package com.dNDTeam.seffectle.restClient

import com.dNDTeam.seffectle.dataClasses.ServerResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//почему retrofit, а не что-то другое
//http://instructure.github.io/blog/2013/12/09/volley-vs-retrofit/
//циферки посвежее
//https://medium.com/@ali.muzaffar/is-retrofit-faster-than-volley-the-answer-may-surprise-you-4379bc589d7c
//когда какую либу юзать
//https://stackoverflow.com/questions/16902716/comparison-of-android-networking-libraries-okhttp-retrofit-and-volley
//https://ziginsider.github.io/Volley-vs-Retrofit/
interface ServerApiInterface {
    @GET("users")    //посылка GET запроса на Controller.BASE_URL + users/
    fun sendUser(  //?
            @Query("data") data: String //отправляемый параметр "data"
    ): Call<ServerResponse> //приходит ответ типа ServerResponse

    @GET("schedules")    //посылка GET запроса на Controller.BASE_URL + schedules/
    fun sendClasses( //?
            @Query("data") data: String //отправляемый параметр "data"
    ): Call<ServerResponse> //приходит ответ типа ServerResponse

    @GET("schedules")    //посылка GET запроса на Controller.BASE_URL + schedules/
    fun getClasses( //?
            @Query("data") data: String //отправляемый параметр "data"
    ): Call<ServerResponse> //приходит ответ типа ServerResponse
}