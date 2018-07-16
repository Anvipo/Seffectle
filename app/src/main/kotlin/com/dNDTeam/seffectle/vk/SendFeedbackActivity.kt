package com.dNDTeam.seffectle.vk

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.dNDTeam.seffectle.R
import com.dNDTeam.seffectle.R.string.you_are_not_logged_in_via_VK
import com.dNDTeam.seffectle.R.string.you_did_not_enter_the_message
import com.dNDTeam.seffectle.vk.VKUser.asyncSendFeedback
import com.vk.sdk.VKSdk
import kotlinx.android.synthetic.main.activity_send_feedback.feedback_text_ET_ASF
import kotlinx.android.synthetic.main.activity_send_feedback.send_feedback_button
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast

class SendFeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_feedback)

        send_feedback_button.onClick {
            if (VKUser.vkAccountOwner.id.isBlank())
                VKUser.getVkAccountOwnerInfoWrapper(this@SendFeedbackActivity)

            val feedbackText = feedback_text_ET_ASF.text.toString().trim()

            if (feedbackText.isNotBlank()) {
                if (!VKSdk.isLoggedIn()) {
                    longToast(you_are_not_logged_in_via_VK)
                    finish()
                } else if (asyncSendFeedback(this@SendFeedbackActivity, feedbackText).await()) {
                    setResult(RESULT_OK)
                    finish()
                }
            } else
                toast(getString(you_did_not_enter_the_message))
        }
    }
}
