package com.savageking.countdown

import android.os.Bundle

interface ResultCallback {
    fun alreadyRunning( bundle : Bundle )
    fun queryResult( bundle : Bundle)
}