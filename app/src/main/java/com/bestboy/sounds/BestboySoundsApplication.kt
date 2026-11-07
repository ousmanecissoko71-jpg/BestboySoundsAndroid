package com.bestboy.sounds

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds

class BestboySoundsApplication : Application() {
    companion object { const val PLAYBACK_CHANNEL_ID = "bestboy_music_playback" }
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(PLAYBACK_CHANNEL_ID, getString(R.string.playback_channel_name), NotificationManager.IMPORTANCE_LOW)
            channel.description = getString(R.string.playback_channel_description)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        MobileAds.initialize(this)
    }
}
