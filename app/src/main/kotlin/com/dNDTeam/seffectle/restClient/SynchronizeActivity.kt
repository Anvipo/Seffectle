package com.dNDTeam.seffectle.restClient

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.dNDTeam.seffectle.App
import com.dNDTeam.seffectle.App.Companion.changeApiBaseUrl
import com.dNDTeam.seffectle.App.Companion.ipRegex
import com.dNDTeam.seffectle.R.layout
import com.dNDTeam.seffectle.R.string.*
import com.dNDTeam.seffectle.dataClasses.ServerResponse
import com.dNDTeam.seffectle.db.*
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_BEGIN_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_END_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_SERIAL_NUMBER
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_CLASS_TYPE
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_DAY_OF_WEEK
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_ROOM_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_SCHEDULE_CREATION_TIME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_TEACHER_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.SCHEDULE_TABLE_NAME
import com.dNDTeam.seffectle.vk.VKUser
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_synchronize.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SynchronizeActivity : AppCompatActivity() {
    private fun asyncSendVkUserInfoToServer(gson: Gson) = async {
        //если данные от VK не были получены, то получаем их в другом потоке
        if (VKUser.vkAccountOwner.id.isBlank())
            VKUser.asyncGetVkAccountOwnerInfo(this@SynchronizeActivity).await()

        if (VKUser.vkAccountOwner.id.isNotBlank()) {
            val jsonFromAllParams = gson.toJson(VKUser.vkAccountOwner)

            //делаем Json из данных о авторизовавшемся юзере

            //отправляем json на серв в таблицу users
            App.serverApiInterface.sendUser(jsonFromAllParams)
                .enqueue(object : Callback<ServerResponse> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(
                        call: Call<ServerResponse>?,
                        serverResponse: Response<ServerResponse>?
                    ) {
                        /*response_TV.text = when {
                            response == null -> "${getString(server_response_string)}${getString(weird_answer_string)}"
                            response.isSuccessful -> response.body()?.responseData.toString()
                            else -> "${response.errorBody()}"
                        }*/
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onFailure(call: Call<ServerResponse>?, throwable: Throwable?) {
//                            response_TV.text = "${getString(server_response_string)} ${throwable?.message}"
                    }
                })
        }
    }

    private fun saveScheduleInDB(classesListFromServer: MutableList<ServerScheduleScheme>) {
        database.use {
            for (classFromSchedule in classesListFromServer) {
                val arrToDB = arrayOf(
                    KEY_DAY_OF_WEEK to classFromSchedule.dayOfWeek,
                    KEY_CLASS_BEGIN_TIME to classFromSchedule.classBeginTime,
                    KEY_CLASS_END_TIME to classFromSchedule.classEndTime,
                    KEY_CLASS_TYPE to classFromSchedule.classType,
                    KEY_CLASS_NAME to classFromSchedule.className,
                    KEY_TEACHER_NAME to classFromSchedule.teacherName,
                    KEY_ROOM_NAME to classFromSchedule.roomName,
                    KEY_CLASS_SERIAL_NUMBER to classFromSchedule.classSerialNumber,
                    KEY_SCHEDULE_CREATION_TIME to classFromSchedule.scheduleCreationTime
                )

                val wasUpdated =
                    update(SCHEDULE_TABLE_NAME, *arrToDB)
                        //метод whereArgs защищает (судя по исходникам) от SQL-инъекций
                        .whereArgs(
                            "($KEY_DAY_OF_WEEK = {$KEY_DAY_OF_WEEK}) AND " +
                                    "($KEY_CLASS_SERIAL_NUMBER = {$KEY_CLASS_SERIAL_NUMBER})",
                            KEY_DAY_OF_WEEK to classFromSchedule.dayOfWeek,
                            KEY_CLASS_SERIAL_NUMBER to classFromSchedule.classSerialNumber
                        )
                        .exec() > 0

                if (!wasUpdated)
                    insert(SCHEDULE_TABLE_NAME, *arrToDB)
            }
        }
    }

    private var msgSB = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_synchronize)

        getIPFrom(database)

        getVKUserInfoFrom(database, applicationContext)

        serverIP_ET.setText(ServerInfo.serverIP)

        //инициализаций билдера
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        //TODO отправку и получение расписания лучше всего наверно придётся где-то делать в другом месте
        //TODO т.е. эта активити возможно лишняя и исчезнет когда-нибудь
        sendYourScheduleToServer_button.onClick {
            //всё делается в асинхронном потоке UI
            response_TV.text = ""

            async {
                if (!ServerInfo.userInfoWasSentToServer)
                    asyncSendVkUserInfoToServer(gson).await()

                if (VKUser.vkAccountOwner.id.isBlank()) {
                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                }

                //получаем данные из БД в classesListFromDB
                val classesListFromDB = database.use {
                    return@use select(SCHEDULE_TABLE_NAME)
                        .parseList(ClassFromScheduleParser)
                }

                //делаем json из данных о расписании юзера
                val jsonFromSchedule = gson.toJson(classesListFromDB)

                //отправляем в таблицу schedules classesListFromDB
                App.serverApiInterface.sendClasses(jsonFromSchedule)
                    .enqueue(object : Callback<ServerResponse> {
                        @SuppressLint("SetTextI18n")
                        override fun onResponse(
                            call: Call<ServerResponse>?,
                            serverResponse: Response<ServerResponse>?
                        ) {
                            msgSB.append(
                                when {
                                    serverResponse == null -> "${getString(server_response)}${getString(
                                        weird_answer
                                    )}"
                                    serverResponse.isSuccessful -> serverResponse.body()?.responseData.toString()
                                    else -> "${serverResponse.errorBody()}"
                                }
                            )

                            response_TV.text = "$msgSB"
                        }

                        @SuppressLint("SetTextI18n")
                        override fun onFailure(
                            call: Call<ServerResponse>?,
                            throwable: Throwable?
                        ) {
                            response_TV.text =
                                    "${getString(server_response)} ${throwable?.message}"
                        }
                    }
                    )
            }.await()
        }

        getYourScheduleFromServer_button.onClick {
            //всё делается в асинхронном потоке UI
            response_TV.text = ""

            async {
                if (!ServerInfo.userInfoWasSentToServer)
                    asyncSendVkUserInfoToServer(gson).await()

                if (VKUser.vkAccountOwner.id.isBlank()) {
                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                }

                val userID = VKUser.vkAccountOwner.vkId

                //делаем json из данных о расписании юзера
                val jsonFromVkAccountOwner = gson.toJson(mapOf("id" to userID))

                //отправляем в таблицу schedules classesList
                App.serverApiInterface.getClasses(jsonFromVkAccountOwner).enqueue(
                    object : Callback<ServerResponse> {
                        @SuppressLint("SetTextI18n")
                        override fun onResponse(
                            call: Call<ServerResponse>?,
                            serverResponse: Response<ServerResponse>?
                        ) {
                            if (serverResponse == null) {
                                msgSB.setLength(0)
                                msgSB.append("${getString(server_response)}${getString(weird_answer)}")
                                response_TV.text = "$msgSB"
                            } else {
                                if (serverResponse.isSuccessful) {
                                    val responseData =
                                        serverResponse.body()?.responseData.toString()

                                    try {
                                        if (responseData[0] != '[' && responseData[1] != '{') {
                                            //Если пришёл не массив json'ов,
                                            //а ответ типа: пользователь не добавлен в таблицу users
                                            //или в таблице schedules для него нет расписаний (он их не добавил
                                            //или ещё что-то)
                                            msgSB.setLength(0)
                                            msgSB.append(responseData)
                                            response_TV.text = "$msgSB"
                                        } else {
                                            val listType = object :
                                                TypeToken<List<ServerScheduleScheme>>() {}.type
                                            val classesListFromServer: MutableList<ServerScheduleScheme> =
                                                gson.fromJson(responseData, listType)

                                            classesListFromServer.sortBy { it.classBeginTime }

                                            saveScheduleInDB(classesListFromServer)

                                            msgSB.setLength(0)
                                            msgSB.append("Расписание успешно получено и сохранено")

                                            response_TV.text = "$msgSB"

                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    } catch (exception: Exception) {
                                        Log.d(
                                            "my",
                                            "На сервере изменилась структура полей schedules"
                                        )
                                    }
                                } else {
                                    msgSB.setLength(0)
                                    msgSB.append("${serverResponse.errorBody()}")
                                    response_TV.text = "$msgSB"
                                }
                            }
                        }

                        @SuppressLint("SetTextI18n")
                        override fun onFailure(
                            call: Call<ServerResponse>?,
                            throwable: Throwable?
                        ) {
                            msgSB.setLength(0)
                            msgSB.append("${getString(server_response)} ${throwable?.message}")
                            response_TV.text = "$msgSB"
                        }
                    }
                )
            }.await()
        }

        saveServerIP_button.onClick {
            val temp = ipRegex.find(serverIP_ET.text.toString())?.value ?: ""

            if (temp.isBlank()) {
                runOnUiThread { toast(getString(entered_ip_is_not_correct)) }
                return@onClick
            }

            val ip = if (!temp.contains(":")) "$temp:8080" else temp

            async {
                database.use {
                    val wasUpdated = update(
                        ScheduleMSQLOH.SERVER_INFO_TABLE_NAME,
                        ScheduleMSQLOH.KEY_SERVER_IP to ip
                    )
                        .exec() > 0

                    if (!wasUpdated)
                    //эта схема иногда называется upsert
                    //т.е. если ничего не было обновлено, то вставим такие данные
                        insert(
                            ScheduleMSQLOH.SERVER_INFO_TABLE_NAME,
                            ScheduleMSQLOH.KEY_SERVER_IP to ip
                        )

                    changeApiBaseUrl(ip)

                    runOnUiThread { toast("$ip ${getString(saved)}") }
                }
            }.await()
        }
    }

    override fun onResume() {
        super.onResume()
        response_TV.text = "$msgSB"
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString("toastWasShown", "$msgSB")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        msgSB.setLength(0)
        msgSB.append(savedInstanceState!!.getString("toastWasShown"))
    }
}