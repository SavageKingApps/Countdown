package com.savageking.countdown

import android.os.Bundle
import android.os.ResultReceiver
import android.os.*

class Result(_callback : ResultCallback ) : ResultReceiver( Handler() )
{
    private val callback = _callback

    override fun onReceiveResult(resultCode: Int, bundle: Bundle)
    {
        super.onReceiveResult(resultCode, bundle)

        when( resultCode )
        {
            CountdownService.SERVICE_RESULT_CODE_ALREADY_RUNNING -> { callback.alreadyRunning( bundle )}
            CountdownService.SERVICE_RESULT_CODE_QUERY -> { callback.queryResult( bundle ) }
            else -> { throw IllegalStateException("Result Code is not ALREADY_RUNNING or QUERY") }
        }
    }
}