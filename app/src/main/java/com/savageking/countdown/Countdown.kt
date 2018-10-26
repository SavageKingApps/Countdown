package com.savageking.countdown

class Countdown( _totalSeconds : Int ) {

    private val totalSeconds = _totalSeconds
    private var  remainingSeconds : Int = totalSeconds

    fun countdown() { remainingSeconds = remainingSeconds.minus(1) }

    fun isAtLastSecond() : Boolean = remainingSeconds == 1

    fun getRemainingSeconds() : Int = remainingSeconds
}