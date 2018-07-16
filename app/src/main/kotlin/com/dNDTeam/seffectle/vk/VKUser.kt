package com.dNDTeam.seffectle.vk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.dNDTeam.seffectle.dataClasses.User
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_COURSE_NUMBER
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_FACULTY_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_FIRST_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_GRADUATION_YEAR
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_LAST_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_UNIVERSITY_NAME
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_USER_ID
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.KEY_USER_VK_ID
import com.dNDTeam.seffectle.db.ScheduleMSQLOH.Companion.USER_INFO_TABLE_NAME
import com.dNDTeam.seffectle.db.database
import com.vk.sdk.VKSdk
import com.vk.sdk.api.*
import com.vk.sdk.api.model.VKApiModel
import com.vk.sdk.api.model.VKList
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.update
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

internal object VKUser {
    //https://vk.com/dev/errors - все виды ошибок от VK API

    var vkAccountOwner: User = User()

    var userInfoWasReceived = false

    private const val feedback = "FEEDBACK ABOUT SCHEDULE APP:\n"

    private const val secondsNumberForWaiting: Long = 1

    // главный метод для проверки подключения
    private fun internetIsAvailable(context: Context): Boolean {

        //http://code.i-harness.com/ru/docs/android/f4e
        val connectivityManager = context.connectivityManager

        val activeNetwork = connectivityManager.activeNetworkInfo

        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting/*activeNetwork.isConnected*/) {
            try {
                /*// тест доступности внешнего ресурса
                val url = URL("https://www.google.com/")
                val urlc = url.openConnection() as HttpURLConnection
                urlc.setRequestProperty("User-Agent", "test")
                urlc.setRequestProperty("Connection", "close")
                urlc.connectTimeout = 2_000 // Timeout в миллисекундах
                urlc.connect()
                // статус ресурса OK
                return if (urlc.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(App.LOG_TAG, "Интернет-соединение установлено")
                    true
                } else {
                    Log.d(App.LOG_TAG, "Интернет-соединение НЕ установлено")
                    false
                }*/


                /*1)Если смартфон подключен к Wi-Fi, то метод вернет true. Даже если интернет не оплачен или из роутера выдернут шнур, то метод все равно вернет true.
                    2) Если смартфон подключен к мобильной сети, но интернет не оплачен, то метод вернет true.*/
                return true
            } catch (ioException: IOException) {
//                Log.d(App.LOG_TAG, "Ошибка проверки подключения к интернету", ioException)
                return false
            }
        }

        return false
    }

    private fun sendFeedback(feedbackText: String) = async {
        //Suspend function 'delay' should be called only from a coroutine or another suspend function
        //поэтому используем async

        var isComplete = false

        VKRequest(
            "messages.send",
            VKParameters.from(
                VKApiConst.USER_ID,
                "81695100",
                VKApiConst.MESSAGE,
                feedback + feedbackText,
                //для предотвращения спама одинаковыми сообщениями (оно вообще работает?)
//                            "random_id", //можно использовать начиная с версии 5.45
                "guid", //альтернатива random_id для старых версий
                "0"
            )
        )
            .executeWithListener(object : VKRequest.VKRequestListener() {
                @SuppressLint("SetTextI18n")
                override fun onComplete(response: VKResponse?) {
                    isComplete = true
//                        context.toast("Сообщение успешно отправлено")
                }

                override fun onError(error: VKError?) {
                    /*
        900 Нельзя отправлять сообщение пользователю из черного списка
        901 Нельзя первым писать пользователю от имени сообщества.
        902 Нельзя отправлять сообщения этому пользователю в связи с настройками приватности
        913 Слишком много пересланных сообщений
        914 Сообщение слишком длинное
        921 Невозможно переслать выбранные сообщения
        */

//                        context.toast("$error")
                }

                override fun onProgress(
                    progressType: VKRequest.VKProgressType?, bytesLoaded: Long,
                    bytesTotal: Long
                ) {
//                        context.toast("onProgress")
                }

                @SuppressLint("SetTextI18n")
                override fun attemptFailed(
                    request: VKRequest?,
                    attemptNumber: Int,
                    totalAttempts: Int
                ) {
//                        context.toast("Ошибка в методе: asyncSendFeedbackWithReturnBool()\n" +
//                                "Attempt $attemptNumber/$totalAttempts failed\n")
                }
            })

        delay(secondsNumberForWaiting, TimeUnit.SECONDS)

        return@async isComplete
    }

    fun asyncSendFeedback(context: Context, feedbackText: String): Deferred<Boolean> = async {
        var wasSent = false

//        if (!VKSdk.isLoggedIn()) {
//            return@async false
//        }

        if (internetIsAvailable(context)) {
            var num = 1

            wasSent = sendFeedback(feedbackText).await()

            //пытаемся отправить сообщение
            while (!wasSent && num != 5) {
//            delay(1, TimeUnit.SECONDS)
                ++num
                wasSent = sendFeedback(feedbackText).await()
            }

//            Log.d(LOG_TAG, "Количество попыток отправить feedback: $num")

            if (!wasSent)
                context.longToast("Feedback не был отправлен.\nВозможно отсутствует соединение с сервером")
        } else {
            context.runOnUiThread {
                //                longToast("У вас отсутствует подключение к интернету\n" +
//                        "Необходимая информация о Вас НЕ была получена от сервера ВК")
            }
        }
        return@async wasSent
    }

    private suspend fun getVkAccountOwnerInfo(context: Context): Boolean {
        //TODO переделать на VKRequest
        val vkRequest = VKApi.users()
            .get(VKParameters.from(VKApiConst.FIELDS, "id,firstName,lastName,education"))

        vkRequest.executeSyncWithListener(vkRequestListener(context))

        delay(secondsNumberForWaiting, TimeUnit.SECONDS)

        return userInfoWasReceived
    }

    private fun calculateCourseNumber(userInfo: VKApiModel): Int =
        if (userInfo.fields.has("graduation") &&
            userInfo.fields.get("graduation").toString().toInt() > 0
        ) {
            fun currentMonthLETJuly(): Boolean =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                //LocalDate since Java 8 and Android 8.0
                    LocalDate.now().monthValue <= Month.JULY.value
                else
                    GregorianCalendar().get(Calendar.MONTH) <= Calendar.JULY

            fun calculateCourseNumberWhenCMLETJuly(gradYear: Int): Int =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    when (gradYear - LocalDate.now().year) {
                        0 -> 4  //Если Год выпуска - текущий год == 1, то человек на 4-ом курсе
                        1 -> 3
                        2 -> 2
                        3 -> 1
                        else -> 0
                    }
                else when (gradYear - GregorianCalendar().get(Calendar.YEAR)) {
                    0 -> 4
                    1 -> 3
                    2 -> 2
                    3 -> 1
                    else -> 0
                }

            fun calculateCourseNumberWhenCMGTJuly(gradYear: Int): Int =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    when (gradYear - LocalDate.now().year) {
                        1 -> 4  //Если Год выпуска - текущий год == 0,
                    // то человек на 4-ом курсе
                        2 -> 3
                        3 -> 2
                        4 -> 1
                        else -> 0
                    }
                else
                    when (gradYear - GregorianCalendar().get(Calendar.YEAR)) {
                        1 -> 4
                        2 -> 3
                        3 -> 2
                        4 -> 1
                        else -> 0
                    }

            val gradYear = userInfo.fields.get("graduation") as Int

            if (currentMonthLETJuly())
            //Если Текущий месяц <= Июля, то
                calculateCourseNumberWhenCMLETJuly(gradYear)
            else
            //Если Текущий месяц > Июля, то
                calculateCourseNumberWhenCMGTJuly(gradYear)

        } else 0

    private fun vkRequestListener(context: Context): VKRequest.VKRequestListener =
        object : VKRequest.VKRequestListener() {
            @SuppressLint("SetTextI18n")
            override fun onComplete(response: VKResponse?) {
                val userInfoList = response?.parsedModel as VKList<*>

                if (userInfoList.isEmpty())
                    return

                val userInfo = userInfoList[0]

                val courseNumber = calculateCourseNumber(userInfo)

                val universityName = if (userInfo.fields.has("university_name"))
                    userInfo.fields.get("university_name").toString().trim()
                else
                    ""

                val facultyName = if (userInfo.fields.has("faculty_name"))
                    userInfo.fields.get("faculty_name").toString().trim()
                else
                    ""

                val graduation = if (userInfo.fields.has("graduation"))
                    userInfo.fields.get("graduation").toString().trim()
                else
                    ""

                val userInfoArray = arrayOf(
                    KEY_USER_ID to "0",
                    KEY_USER_VK_ID to userInfo.fields.get("id"),
                    KEY_FIRST_NAME to userInfo.fields.get("first_name"),
                    KEY_LAST_NAME to userInfo.fields.get("last_name"),
                    KEY_UNIVERSITY_NAME to universityName,
                    KEY_FACULTY_NAME to facultyName,
                    KEY_GRADUATION_YEAR to graduation,
                    KEY_COURSE_NUMBER to courseNumber
                )

                vkAccountOwner = User(
                    "0",
                    userInfo.fields.get("id").toString(),
                    userInfo.fields.get("first_name").toString(),
                    userInfo.fields.get("last_name").toString(),
                    universityName,
                    facultyName,
                    graduation,
                    courseNumber.toString()
                )

                context.database.use {
                    val wasUpdated = update(USER_INFO_TABLE_NAME, *userInfoArray)
                        //метод whereArgs защищает (судя по исходникам) от SQL-инъекций
                        .whereArgs(
                            "($KEY_USER_ID = {$KEY_USER_ID})",
                            KEY_USER_ID to "0"
                        )
                        .exec() > 0

                    if (!wasUpdated) {
                        //эта схема иногда называется upsert
                        //т.е. если ничего не было обновлено, то вставим такие данные
                        insert(USER_INFO_TABLE_NAME, *userInfoArray)
                    }
                }

                userInfoWasReceived = true
                //context.toast("Необходимая о Вас информация от ВК была получена")
            }

            override fun onError(error: VKError?) {
                /*val text = "Данные от VK о юзере НЕ были получены\n" +
                        "Возможно отсутствует интернет соединение\n" +
                        "$error"*/

                //context.longToast(text)
            }

            override fun onProgress(
                progressType: VKRequest.VKProgressType?, bytesLoaded: Long,
                bytesTotal: Long
            ) {
                //val text = "onProgress"
                //context.toast(text)
            }

            @SuppressLint("SetTextI18n")
            override fun attemptFailed(
                request: VKRequest?,
                attemptNumber: Int,
                totalAttempts: Int
            ) {
                //val text =
                //"Ошибка в методе: getVKUserInfo()\nAttempt $attemptNumber/$totalAttempts failed\n"

                //context.toast(text)
            }
        }

    suspend fun getVkAccountOwnerInfoWrapper(context: Context): Boolean {
        if (!VKSdk.isLoggedIn())
            return userInfoWasReceived

        if (userInfoWasReceived)
            return userInfoWasReceived

        if (internetIsAvailable(context)) {
//            val b = context.wifiManager.isWifiEnabled

//            Log.d(App.LOG_TAG, "Интернет-соединение установлено")

            var num = 1

            var wasReceived = getVkAccountOwnerInfo(context)

            //пытаемся получить информацию о пользователе
            while (!wasReceived && num != 5) {
//            delay(1, TimeUnit.SECONDS)
                wasReceived = getVkAccountOwnerInfo(context)
                ++num
            }

//            Log.d(LOG_TAG, "Количество попыток получить с сервера ВК информацию о юзере: $num")

            if (!userInfoWasReceived) {
                context.runOnUiThread {
                    //                    longToast("Необходимая информация о Вас НЕ была получена от сервера ВК" +
//                            "\nВозможно отсутствует интернет соединение")
                }
            }
        } else {
            context.runOnUiThread {
                //                longToast("У вас отсутствует подключение к интернету\n" +
//                        "Необходимая информация о Вас НЕ была получена от сервера ВК")
            }
        }

        return userInfoWasReceived
    }
}