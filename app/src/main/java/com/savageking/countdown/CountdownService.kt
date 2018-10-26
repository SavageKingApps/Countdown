package com.savageking.countdown

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log

class CountdownService : Service() {

    private lateinit var handler: Handler
    private lateinit var bundle: Bundle
    private lateinit var result: ResultReceiver
    private lateinit var notification : NotificationCompat.Builder

    private val NOTIFICATION_ID = 953
    private val WHAT_UPDATE_COUNTDOWN_CONTINUES = 1
    private val WHAT_UPDATE_COUNTDOWN_FINISHED = 2
    private val DELAY_ONE_SECOND : Long = 1000

    private lateinit var countdown : Countdown
    private lateinit var control : ControlService

    companion object {
        val SERVICE_EXTRA_RESULT = "EXTRA_RESULT"
        val SERVICE_EXTRA_CONTROL = "EXTRA_CONTROL"
        val SERVICE_EXTRA_CHOICE = "EXTRA_CHOICE"
        val SERVICE_EXTRA_QUERY = "EXTRA_QUERY"
        val SERVICE_EXTRA_REMAINING_TIME = "EXTRA_TIME"
        val SERVICE_RESULT_CODE_QUERY = 935
        val SERVICE_RESULT_CODE_ALREADY_RUNNING = 936
    }

    override fun onCreate() {
        super.onCreate()

        //create notifcation channel
        createNotificationChannel()

        bundle = Bundle()
        control = ControlService.NOT_STARTED

        val callback = Handler.Callback { msg: Message ->

            when( msg.what )
            {
                WHAT_UPDATE_COUNTDOWN_CONTINUES -> {

                    countdown.countdown()

                    val remainingSeconds = countdown.getRemainingSeconds()

                    notification.setContentTitle( getString( R.string.notification_seconds_plural, remainingSeconds ))

                    with(NotificationManagerCompat.from(this)) {
                        notify(NOTIFICATION_ID, notification.build())
                    }

                    val handlerWhat = when( countdown.isAtLastSecond() )
                    {
                        true -> WHAT_UPDATE_COUNTDOWN_FINISHED
                        false -> WHAT_UPDATE_COUNTDOWN_CONTINUES
                    }

                    handler.sendEmptyMessageDelayed( handlerWhat, DELAY_ONE_SECOND )
                }

                WHAT_UPDATE_COUNTDOWN_FINISHED ->
                {
                    //set the service control to stop
                    control = ControlService.STOPPED

                    //dismiss the notification
                    with(NotificationManagerCompat.from(this)) {
                        cancel( NOTIFICATION_ID )
                    }

                    //play the notification sound
                    playSound()

                    // start activity unless it is already started
                    Intent().apply {
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setClass(applicationContext, CountdownActivity::class.java)
                        startActivity(this)
                    }

                    //stop the service
                    stopSelf()
                }
            }

            true
        }

        handler = Handler( callback )

        val intent = Intent(this, ReceiverStopService::class.java).apply {
            action = ReceiverStopService.ACTION_RECEIVER_STOP_SERVICE
        }

        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        //create the notification
        notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.perm_group_device_alarms)
            .setContentTitle(getString(R.string.notification_title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce( true )
            .setOngoing( true )
            .addAction( R.drawable.notification_icon_background, getString(R.string.notification_button), pendingIntent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {
        executeIntent( intent )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun executeIntent( intent : Intent? )
    {
        intent?.let {

            val serviceController = it.getSerializableExtra( SERVICE_EXTRA_CONTROL ) as ControlService

            when( serviceController )
            {
                ControlService.START -> {
                    it.run (::initiateCountdown ) }
                ControlService.STOP -> {
                    stopCountdown() }
                ControlService.QUERY -> {
                    it.run( ::queryCountdown ) }
                else -> {
                    stopSelf()
                    throw IllegalStateException("onStartCommand is not START, STOP, or QUERY")
                }
            }
        } ?: handleNullIntent()

    }

    private fun handleNullIntent()
    {
        //if it is running then keep running
        if( control == ControlService.RUNNING )
        {
            val handlerWhat = when( countdown.isAtLastSecond() )
            {
                true -> WHAT_UPDATE_COUNTDOWN_FINISHED
                false -> WHAT_UPDATE_COUNTDOWN_CONTINUES
            }

            handler.sendEmptyMessageDelayed( handlerWhat, DELAY_ONE_SECOND )
        }

        //stop it from running
        else
        {
            handler.sendEmptyMessageDelayed( WHAT_UPDATE_COUNTDOWN_FINISHED, DELAY_ONE_SECOND )
        }
    }

    private fun initiateCountdown( intent : Intent )
    {
        //let user know that the service is running
        if( control == ControlService.RUNNING )
        {
            val remainingTime = countdown.getRemainingSeconds()

            bundle.clear()
            bundle.putSerializable( SERVICE_EXTRA_QUERY, control )
            bundle.putInt( SERVICE_EXTRA_REMAINING_TIME, remainingTime)

            result = intent.getParcelableExtra( SERVICE_EXTRA_RESULT ) as ResultReceiver
            result.send( SERVICE_RESULT_CODE_ALREADY_RUNNING, bundle )

        }

        //initiate the countdown
        else
        {
            //set the controller
            control = ControlService.RUNNING

            // get the choice
            val choice = intent.getSerializableExtra(SERVICE_EXTRA_CHOICE) as Choice

            //get the result receiver
            result = intent.getParcelableExtra( SERVICE_EXTRA_RESULT ) as ResultReceiver

            //get the seconds to countdown
            val secondsToCountdown = choice.getSeconds()

            //initialize countdown object
            countdown = Countdown( secondsToCountdown )

            //show the foreground notification
            notification.setContentTitle( getString( R.string.notification_seconds_plural, secondsToCountdown ))
            startForeground( NOTIFICATION_ID, notification.build() )

            //start the countdown
            handler.sendEmptyMessageDelayed( WHAT_UPDATE_COUNTDOWN_CONTINUES, DELAY_ONE_SECOND )
        }
    }

    private fun stopCountdown()
    {
        if( control == ControlService.RUNNING )
        {
            //stop all handler messages
            handler.removeMessages(WHAT_UPDATE_COUNTDOWN_CONTINUES)
            handler.removeMessages(WHAT_UPDATE_COUNTDOWN_FINISHED)

            playSound()
        }

        //dismiss the notification
        with(NotificationManagerCompat.from(this)) {
            cancel( NOTIFICATION_ID )
        }

        control = ControlService.STOPPED

        //stop the service
        stopSelf()
    }

    //used to query the service
    //to determine the status of the service
    private fun queryCountdown( intent : Intent )
    {
        //get the result receiver
        result = intent.getParcelableExtra( SERVICE_EXTRA_RESULT ) as ResultReceiver
        bundle.clear()

        when( control )
        {
            ControlService.RUNNING -> {

                val remainingTime = countdown.getRemainingSeconds()

                bundle.putSerializable( SERVICE_EXTRA_QUERY, control )
                bundle.putInt( SERVICE_EXTRA_REMAINING_TIME, remainingTime)
                result.send( SERVICE_RESULT_CODE_ALREADY_RUNNING, bundle )
            }

            else ->
            {
                bundle.putSerializable( SERVICE_EXTRA_QUERY, control )
                result.send( SERVICE_RESULT_CODE_QUERY, bundle )
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(getString(R.string.notification_channel_id), name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playSound()
    {
        try
        {
            val notification : Uri = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION )
            val ringtone : Ringtone = RingtoneManager.getRingtone( applicationContext, notification )
            ringtone.play()
        }
        catch( e : Exception )
        {
            Log.d("Tiger", "Error has occurred ${e.message}")
        }
    }
}