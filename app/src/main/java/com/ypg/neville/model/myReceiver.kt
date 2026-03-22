package com.ypg.neville.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ypg.neville.services.serviceStreaming

class myReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action")
        when (action) {
            "stop" -> serviceStreaming.mserviseThis?.serviceStop()
            "pause" -> serviceStreaming.mserviseThis?.pauseMediaP()
            "resume" -> serviceStreaming.mserviseThis?.startMediaP()
        }
    }

    companion object {
        const val ACTION_SIGNAL = "com.ypg.neville.action.streaming.signal"
    }
}
