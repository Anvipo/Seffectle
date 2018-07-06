package com.dNDTeam.seffectle.vk

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.dNDTeam.seffectle.R
import com.vk.sdk.VKAccessToken
import com.vk.sdk.VKCallback
import com.vk.sdk.VKSdk
import com.vk.sdk.api.VKError
import org.jetbrains.anko.toast

class LoginActivity : FragmentActivity() {
    private var isResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        VKSdk.wakeUpSession(this, object : VKCallback<VKSdk.LoginState> {
            override fun onResult(res: VKSdk.LoginState) {
                if (isResumed)
                    when (res) {
                        VKSdk.LoginState.LoggedOut -> showLogin()
                        VKSdk.LoginState.LoggedIn -> showLogout()
                        VKSdk.LoginState.Pending -> toast("Pending")
                        VKSdk.LoginState.Unknown -> toast("Unknown")
                    }
            }

            override fun onError(error: VKError) = Unit
        })
    }

    override fun onResume() {
        super.onResume()

        isResumed = true
        if (VKSdk.isLoggedIn())
            showLogout()
        else
            showLogin()
    }

    override fun onPause() {
        super.onPause()
        isResumed = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKCallback<VKAccessToken> {
            override fun onResult(res: VKAccessToken) {
                toast(R.string.successful_authorization)
                exit()
            }

            override fun onError(error: VKError) {
                toast(R.string.unsuccessful_authorization)
            }
        }

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, callback))
            super.onActivityResult(requestCode, resultCode, data)
    }

    internal fun exit() {
        setResult(AppCompatActivity.RESULT_OK)
        finish()
    }

    internal fun showLogin() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, LoginFragment())
                .commitAllowingStateLoss()
    }

    internal fun showLogout() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, LogoutFragment())
                .commitAllowingStateLoss()
    }
}
