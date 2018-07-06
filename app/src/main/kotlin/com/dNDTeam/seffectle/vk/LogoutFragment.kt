package com.dNDTeam.seffectle.vk

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.dNDTeam.seffectle.R
import com.vk.sdk.VKSdk
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

class LogoutFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_logout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view?.find<Button>(R.id.logout)?.onClick {
            VKSdk.logout()
            if (!VKSdk.isLoggedIn()) {
                toast(getString(R.string.successful_logout))
                (activity as LoginActivity).exit()
//                (activity as LoginActivity).showLogin()
            }
        }
    }
}