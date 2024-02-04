package com.example.terminer

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeUpdater(
    private val onTimeUpdated: (String) -> Unit,
    private val onDateUpdated: (String) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMillis = 60000L // 60 seconds

    init {
        // Start updating immediately
        updateCurrentTime()
    }

    // Function to get current time as a string
    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // Customize the format as needed
        return simpleDateFormat.format(calendar.time)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) // Customize the format as needed
        return simpleDateFormat.format(calendar.time)
    }

    // Function to update the current time and schedule the next update
    private fun updateCurrentTime() {
        val currentTime = getCurrentTime()
        val currentDate = getCurrentDate()

        onTimeUpdated(currentTime)
        onDateUpdated(currentDate)

        // Schedule the next update after the specified interval
        handler.postDelayed(::updateCurrentTime, updateIntervalMillis)
    }

    // Call this method to stop the updates when needed (e.g., when the activity is destroyed)
    fun stopUpdates() {
        handler.removeCallbacksAndMessages(null)
    }
}
