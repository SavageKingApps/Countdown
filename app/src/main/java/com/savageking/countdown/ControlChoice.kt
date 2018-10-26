package com.savageking.countdown

import android.view.View

const val UNINITIALIZED_CUSTOM = -1

enum class Choice(private val index : Int, private var seconds : Int  ) {
    SECONDS_10(0, 10), SECONDS_30(1, 30), SECONDS_45(2,45),
    SECONDS_60(3,60), SECONDS_90(4,90), SECONDS_CUSTOM(5, UNINITIALIZED_CUSTOM);

    companion object {

        fun getChoice( position : Int ) : Choice =

            when ( position )
            {
                Choice.SECONDS_10.index -> Choice.SECONDS_10
                Choice.SECONDS_30.index -> Choice.SECONDS_30
                Choice.SECONDS_45.index -> Choice.SECONDS_45
                Choice.SECONDS_60.index -> Choice.SECONDS_60
                Choice.SECONDS_90.index -> Choice.SECONDS_90
                else -> Choice.SECONDS_CUSTOM
            }

        fun isCustomVisible( position : Int ) : Int =
            when( position )
            {
                Choice.SECONDS_CUSTOM.index -> View.VISIBLE
                else -> View.GONE
            }
    }

    fun updateCustom( time : Int )
    {
        seconds = time
    }

    fun isCustomSetup() : Boolean = seconds == UNINITIALIZED_CUSTOM

    fun getIndex() = index

    fun getSeconds() = seconds
}