package com.dNDTeam.seffectle

import android.app.Application
import com.dNDTeam.seffectle.db.database
import com.dNDTeam.seffectle.db.getServerIPFromDB
import com.dNDTeam.seffectle.restClient.ServerApiInterface
import com.dNDTeam.seffectle.restClient.ServerInfo
import com.dNDTeam.seffectle.vk.LoginActivity
import com.google.gson.GsonBuilder
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKAccessTokenTracker
import com.vk.sdk.VKSdk
import org.jetbrains.anko.clearTop
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.newTask
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val address = getServerIPFromDB(database)

        val url = if (address != null)
            urlBegin + address
        else
            DEFAULT_BASE_URL

        builder = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(url)

        serverApiInterface = createService(ServerApiInterface::class.java)

        object : VKAccessTokenTracker() {
            override fun onVKAccessTokenChanged(
                oldToken: VKAccessToken?,
                newToken: VKAccessToken?
            ) {
                if (newToken == null) {
                    longToast(R.string.accessToken_invalidated)
                    startActivity(intentFor<LoginActivity>().newTask().clearTop())
                }
            }
        }.startTracking()

        VKSdk.initialize(this)

//        val fingerprints = VKUtil.getCertificateFingerprint(this, this.packageName)
//        Log.d("my", fingerprints.first())
    }

    companion object {
        //        internal const val LOG_TAG = "my"
        val ipRegex =
            "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(?::\\d+)?".toRegex()
        private const val urlBegin = "http://"
        private const val urlEnd = ":8000/"
        private const val DEFAULT_BASE_URL = "$urlBegin${ServerInfo.defaultIP}$urlEnd"
        lateinit var serverApiInterface: ServerApiInterface

        private val gson = GsonBuilder()
            .setLenient()
            .create()

        private lateinit var builder: Retrofit.Builder

        fun changeApiBaseUrl(newApiBaseUrl: String) {
            val url = if (newApiBaseUrl.contains(":"))
                "$urlBegin$newApiBaseUrl/"
            else
                "$urlBegin$newApiBaseUrl$urlEnd"

            builder = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(url)

            serverApiInterface = createService(ServerApiInterface::class.java)
        }

        private fun <S> createService(serviceClass: Class<S>): S {
            val retrofit = builder.build()
            return retrofit.create(serviceClass)
        }

        /*private fun getIpAddress(): String {
            val url = URL("http://checkip.amazonaws.com/")
            val inputStream = url.openStream()
            val reader = BufferedReader(InputStreamReader(inputStream))
            val result = reader.readLine()
            inputStream.close()
            reader.close()
            return result
        }*/
    }
}