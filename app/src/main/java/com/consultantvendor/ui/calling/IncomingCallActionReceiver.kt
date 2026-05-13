package com.consultantvendor.ui.calling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.data.models.PushData

class IncomingCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, 0)
        val callInvite = intent.getSerializableExtra(Constants.INCOMING_CALL_INVITE) as? PushData

        when (action) {
            Constants.ACTION_ACCEPT -> {
                if (callInvite == null) return
                val activityIntent = Intent(context, CallingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
                    putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    this.action = Constants.ACTION_ACCEPT
                }
                context.startActivity(activityIntent)
            }

            Constants.ACTION_REJECT -> {
                // best-effort: stop ringing if the app is running
                SoundPoolManager.getInstance(context)?.stopRinging()
            }

            Constants.ACTION_CANCEL_CALL -> {
                val cancelIntent = Intent(Constants.ACTION_CANCEL_CALL)
                LocalBroadcastManager.getInstance(context).sendBroadcast(cancelIntent)
            }
        }
    }
}

