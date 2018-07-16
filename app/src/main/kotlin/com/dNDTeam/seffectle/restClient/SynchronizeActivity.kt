package com.dNDTeam.seffectle.restClient

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dNDTeam.seffectle.App
import com.dNDTeam.seffectle.App.Companion.changeApiBaseUrl
import com.dNDTeam.seffectle.App.Companion.ipRegex
import com.dNDTeam.seffectle.R.layout
import com.dNDTeam.seffectle.R.string.*
import com.dNDTeam.seffectle.dataClasses.ClassFromSchedule
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
import kotlinx.android.synthetic.main.activity_synchronize.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

typealias GetScheduleResponseModel = List<ServerScheduleModel>
typealias SendResponseModel = String

@Suppress("RedundantAsync")
class SynchronizeActivity : AppCompatActivity() {
    private suspend fun sendVkUserInfoToServer(gson: Gson) {
        //если данные от VK не были получены, то получаем их в другом потоке
        if (VKUser.vkAccountOwner.id.isBlank())
            VKUser.getVkAccountOwnerInfoWrapper(this)

        if (VKUser.vkAccountOwner.id.isNotBlank()) {
            val vkUserInfoJson = gson.toJson(VKUser.vkAccountOwner)

            App.serverApiInterface.sendUserInfo(vkUserInfoJson).enqueue(sendUserInfoCallback())
        }
    }

    private fun sendUserInfoCallback(): Callback<SendResponseModel> =
        object : Callback<SendResponseModel> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: Call<SendResponseModel>?,
                serverResponse: Response<SendResponseModel>?
            ) {
                val text = when {
                    serverResponse == null -> "${getString(server_response)}${getString(
                        weird_answer
                    )}"
                    serverResponse.isSuccessful -> serverResponse.body() ?: ""
                    else -> "${serverResponse.errorBody()}"
                }

                toast(text)

                ServerInfo.userInfoWasSentToServer = true
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(
                call: Call<SendResponseModel>?,
                throwable: Throwable?
            ) {
                val text = "${getString(server_response)} ${throwable?.message}"
                toast(text)
            }
        }

    private fun saveScheduleInDB(classesListFromServer: List<ServerScheduleModel>) {
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

    //функция для загрузки нужных данных
    @SuppressLint("NewApi")
    private fun loadDataAsync() = launch {
        getOrSetDefaultIPFrom(database)

        serverIP_ET.setText(ServerInfo.serverIP)

        getOrSetDefaultUserInfoWasSentToServerFrom(database)

        getVKUserInfoFrom(database, applicationContext)
    }

    //инициализация билдера
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .create()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_synchronize)

        loadDataAsync()

        //TODO отправку и получение расписания лучше всего наверно придётся где-то делать в другом месте
        //TODO т.е. эта активити возможно лишняя и исчезнет когда-нибудь
        sendYourScheduleToServer_button.onClick {
            sendSchedule()
        }

        getYourScheduleFromServer_button.onClick {
            getSchedule()
        }

        saveServerIP_button.onClick {
            saveServerIP()
        }
    }

    private fun saveServerIP() {
        launch {
            val temp = ipRegex.find(serverIP_ET.text.toString())?.value ?: ""

            if (temp.isBlank())
                return@launch runOnUiThread { toast(getString(entered_ip_is_not_correct)) }

            val ip = if (temp.contains(":")) temp else "$temp:8080"

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
        }
    }

    private fun outputText(text: String) {
        msgSB.setLength(0)
        msgSB.append(text)
        response_TV.text = "$msgSB"
    }

    private suspend fun getSchedule() {
        response_TV.text = ""

        if (VKUser.vkAccountOwner.id.isBlank()) {
            setResult(Activity.RESULT_FIRST_USER)
            finish()
        } else {
            async {
                if (!ServerInfo.userInfoWasSentToServer)
                    sendVkUserInfoToServer(gson)

                val userID = VKUser.vkAccountOwner.vkId

                //делаем json из данных о расписании юзера
                val jsonFromVkAccountOwner = gson.toJson(mapOf("id" to userID))

                //отправляем в таблицу schedules classesList
                val getClassesCall = App.serverApiInterface.getClasses(jsonFromVkAccountOwner)

                getClassesCall.enqueue(getClassesCallback())
            }.await()
        }
    }

    private fun getClassesCallback(): Callback<GetScheduleResponseModel> =
        object : Callback<GetScheduleResponseModel> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: Call<GetScheduleResponseModel>?,
                response: Response<GetScheduleResponseModel>?
            ) {
                if (response == null)
                    return outputText("${getString(server_response)}${getString(weird_answer)}")

                if (!response.isSuccessful) {
                    val s: String = response.errorBody()?.string()
                            ?: response.raw().toString()

                    return outputText(s)
                }

                val responseData = response.body()

                if (responseData == null || responseData.isEmpty())
                    outputText(getString(server_has_not_your_schedule))
                else {
                    saveScheduleInDB(responseData.sortedBy { it.classBeginTime })

                    outputText(getString(your_schedule_was_received_and_saved))

                    setResult(RESULT_OK)
                    finish()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(
                call: Call<GetScheduleResponseModel>?,
                throwable: Throwable?
            ) = outputText("${getString(server_response)} ${throwable?.message}")
        }

    private suspend fun sendSchedule() {
        //всё делается в асинхронном потоке UI
        response_TV.text = ""

        if (VKUser.vkAccountOwner.id.isBlank()) {
            setResult(Activity.RESULT_FIRST_USER)
            finish()
        } else {
            if (!ServerInfo.userInfoWasSentToServer)
                sendVkUserInfoToServer(gson)

            async {
                //получаем данные из БД в classesListFromDB
                val classesListFromDB: List<ClassFromSchedule> = database.use {
                    select(SCHEDULE_TABLE_NAME).parseList(ClassFromScheduleParser)
                }

                //делаем json из данных о расписании юзера
                val jsonFromSchedule = gson.toJson(classesListFromDB)

                //отправляем в таблицу schedules classesListFromDB
                App.serverApiInterface.sendClasses(jsonFromSchedule)
                    .enqueue(sendClassesCallback())
            }.await()
        }
    }

    private fun sendClassesCallback(): Callback<SendResponseModel> =
        object : Callback<SendResponseModel> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                call: Call<SendResponseModel>?,
                response: Response<SendResponseModel>?
            ) {
                val text = when {
                    response == null -> "${getString(server_response)}${getString(
                        weird_answer
                    )}"
                    response.isSuccessful -> response.body() ?: ""
                    else -> "${response.errorBody()}"
                }

                outputText(text)
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(
                call: Call<SendResponseModel>?,
                throwable: Throwable?
            ) {
                val text = "${getString(server_response)} ${throwable?.message}\n" +
                        "Возможно на сервере поменялась структура response или сервер не включён"
                outputText(text)
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