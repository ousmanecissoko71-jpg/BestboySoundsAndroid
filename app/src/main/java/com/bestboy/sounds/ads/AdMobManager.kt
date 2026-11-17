package com.bestboy.sounds.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.lang.ref.WeakReference

object AdMobManager {

    private const val TAG = "AdMobManager"

    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val PREFS_NAME = "bestboy_sounds_ad_preferences"
    private const val KEY_COMPLETED = "completed_songs"
    private const val KEY_NEXT_AD_AT = "next_ad_at"
    private const val KEY_PENDING = "pending_interstitial"

    private const val FIRST_AD_AT = 10
    private const val AD_INTERVAL = 5

    private val mainHandler = Handler(Looper.getMainLooper())

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var foregroundActivity: WeakReference<Activity>? = null

    fun setForegroundActivity(activity: Activity) {
        foregroundActivity = WeakReference(activity)
        tryShowPendingInterstitial()
    }

    fun clearForegroundActivity(activity: Activity) {
        if (foregroundActivity?.get() === activity) {
            foregroundActivity = null
        }
    }

    fun onSongCompleted(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val completed = prefs.getInt(KEY_COMPLETED, 0) + 1
        val nextAdAt = prefs.getInt(KEY_NEXT_AD_AT, FIRST_AD_AT)
        val pending = prefs.getBoolean(KEY_PENDING, false) || completed >= nextAdAt

        prefs.edit()
            .putInt(KEY_COMPLETED, completed)
            .putBoolean(KEY_PENDING, pending)
            .apply()

        Log.d(TAG, "Chansons terminees : $completed")

        mainHandler.post {
            if (prefs.getBoolean(KEY_PENDING, false)) {
                loadInterstitialAd(appContext)
                tryShowPendingInterstitial()
            }
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true

        InterstitialAd.load(
            context.applicationContext,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    tryShowPendingInterstitial()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "Chargement interstitiel impossible : ${error.message}")
                }
            }
        )
    }

    private fun tryShowPendingInterstitial() {
        val activity = foregroundActivity?.get() ?: return

        if (activity.isFinishing || activity.isDestroyed) return

        val prefs = activity.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean(KEY_PENDING, false)) return

        val ad = interstitialAd

        if (ad == null) {
            loadInterstitialAd(activity)
            return
        }

        interstitialAd = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                finishInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                Log.w(TAG, "Publicite impossible a afficher : ${error.message}")
                // Le compteur reste en attente. On réessaiera au prochain retour.
            }
        }

        ad.show(activity)
    }

    private fun finishInterstitial(activity: Activity) {
        val prefs = activity.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val nextAdAt = prefs.getInt(KEY_NEXT_AD_AT, FIRST_AD_AT) + AD_INTERVAL
        val completed = prefs.getInt(KEY_COMPLETED, 0)

        prefs.edit()
            .putInt(KEY_NEXT_AD_AT, nextAdAt)
            .putBoolean(KEY_PENDING, completed >= nextAdAt)
            .apply()

        loadInterstitialAd(activity.applicationContext)
    }

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true

        RewardedAd.load(
            context.applicationContext,
            TEST_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int, rewardType: String) -> Unit,
        onAdClosed: () -> Unit
    ) {
        val ad = rewardedAd

        if (ad != null) {
            rewardedAd = null

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd(activity)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewardedAd(activity)
                    onAdClosed()
                }
            }

            ad.show(activity) { reward ->
                onRewardEarned(reward.amount, reward.type)
            }
        } else {
            loadRewardedAd(activity)
            onAdClosed()
        }
    }

    fun isRewardedAdLoaded(): Boolean = rewardedAd != null
}
