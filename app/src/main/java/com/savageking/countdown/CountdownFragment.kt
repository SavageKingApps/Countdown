package com.savageking.countdown

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


class CountdownFragment : Fragment()
{
    companion object {
        fun getInstance() = CountdownFragment()
        fun getInstanceTag() = "COUNTDOWN_FRAGMENT"
    }

    private lateinit var clickToolbar : View.OnClickListener
    private lateinit var clickButton : View.OnClickListener
    private lateinit var clickSpinner : AdapterView.OnItemSelectedListener
    private lateinit var result : Result

    private var choice : Choice = Choice.SECONDS_10
    private val FIFTEEN_MINUTES : Int = 900

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clickToolbar = View.OnClickListener{ view : View -> Unit
            activity?.finish()
        }

        clickButton = View.OnClickListener{ Unit ->

            var startService = true

            if( choice == Choice.SECONDS_CUSTOM )
            {
               startService = processCustom()
            }

            if( startService )
            {
                Intent().apply{
                    putExtra(CountdownService.SERVICE_EXTRA_CONTROL, ControlService.START)
                    putExtra(CountdownService.SERVICE_EXTRA_CHOICE, choice )
                    putExtra(CountdownService.SERVICE_EXTRA_RESULT, result)
                    setClass( context!!, CountdownService::class.java )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        activity?.startForegroundService( this )
                    else
                        activity?.startService( this )
                }

                //show toast
                Toast.makeText( context, R.string.toast_start_countdown, Toast.LENGTH_LONG ).show()

                //end the activity
                activity?.finish()
            }
        }

        clickSpinner = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, _view: View?, position: Int, id: Long) {

                val input = view?.findViewById<TextInputLayout>( R.id.design_edit_text)
                val errorInputLayout = view?.findViewById<TextInputLayout>(R.id.design_edit_text)

                errorInputLayout!!.error = null

                input?.visibility = Choice.isCustomVisible( position )
                choice = Choice.getChoice( position )
            }
        }

        val update = object : ResultCallback {

            override fun alreadyRunning( bundle : Bundle ) {
                val timeRemaining = bundle.getInt(CountdownService.SERVICE_EXTRA_REMAINING_TIME)
                val message = getString( R.string.result_service_is_running , timeRemaining )
                Toast.makeText( context, message, Toast.LENGTH_LONG ).show()
            }

            override fun queryResult( bundle : Bundle ) {
                val serviceControl = bundle.getSerializable( CountdownService.SERVICE_EXTRA_QUERY ) as ControlService
                Log.d("Tiger", "CountdownFragment -> queryResult: ${serviceControl.name}")
            }
        }

       result = Result( update )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.countdown_fragment, container, false)

        val toolbar = view.findViewById<Toolbar>( R.id.toolbar )

        toolbar.apply{
            setTitle( R.string.toolbar_title )
            setSubtitle( R.string.toolbar_subtitle )
            setNavigationIcon(R.drawable.ic_clear_mtrl_alpha)
            setNavigationOnClickListener( clickToolbar )
        }

        val button = view.findViewById<ImageButton>( R.id.imageButton )
        button.setOnClickListener( clickButton )

        val spinner = view.findViewById<Spinner>( R.id.spinner )
        spinner.onItemSelectedListener = clickSpinner

        return view
    }

    private fun processCustom() : Boolean {

        var ready = true

            val errorInputLayout = view!!.findViewById<TextInputLayout>(R.id.design_edit_text)
            val input = view!!.findViewById<EditText>(R.id.editText)
            val inputText = input.text.toString()

            if (inputText.isBlank()) {
                errorInputLayout?.error = getString(R.string.error_custom_is_blank)
                ready = false
            }

            else
            {
                inputText.toIntOrNull()?.let{

                    if ( it > FIFTEEN_MINUTES )
                    {
                        ready = false
                        errorInputLayout?.error = getString(R.string.error_custom_greater_than_nine_hundred)
                    }

                    else
                    {
                        choice.updateCustom(it)
                    }
                }
            }

        return ready
    }

    override fun onResume() {
        super.onResume()

            //queries the status of the service
            Intent().apply{
                putExtra(CountdownService.SERVICE_EXTRA_CONTROL, ControlService.QUERY)
                putExtra(CountdownService.SERVICE_EXTRA_RESULT, result)
                setClass( context!!, CountdownService::class.java )
                activity?.startService( this )
            }
    }
}
