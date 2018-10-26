package com.savageking.countdown

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast


class CountdownActivity: AppCompatActivity()
{
    override fun onCreate(bundle : Bundle?)
    {
        super.onCreate(bundle)
        setContentView(R.layout.app_container)

        if( bundle == null )
        {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, CountdownFragment.getInstance(), CountdownFragment.getInstanceTag())
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Toast.makeText(this, R.string.toast_end_countdown, Toast.LENGTH_LONG).show()
    }
}
