package com.savageking.countdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReceiverStopService : BroadcastReceiver() {

    companion object {
        val ACTION_RECEIVER_STOP_SERVICE = "com.savageking.countdown.ACTION_STOP_SERVICE"
    }

    override fun onReceive(context: Context, intent: Intent?)
    {
        Intent().apply {
            putExtra(CountdownService.SERVICE_EXTRA_CONTROL, ControlService.STOP)
            setClass( context, CountdownService::class.java)
            context.startService( this )
        }
    }
}