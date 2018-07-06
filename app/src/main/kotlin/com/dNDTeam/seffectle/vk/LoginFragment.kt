package com.dNDTeam.seffectle.vk

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.dNDTeam.seffectle.R
import com.vk.sdk.VKScope
import com.vk.sdk.VKSdk
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk25.coroutines.onClick


class LoginFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val v: View? = inflater.inflate(
                R.layout.fragment_login,
                container,
                false
        )
        v?.find<Button>(R.id.sign_in_button)?.onClick {
            VKSdk.login(activity as LoginActivity, VKScope.MESSAGES)
        }
        return v
    }
}
