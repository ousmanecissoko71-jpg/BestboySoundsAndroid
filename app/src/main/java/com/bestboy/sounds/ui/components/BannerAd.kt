package com.bestboy.sounds.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bestboy.sounds.ads.AdMobManager
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdRequest

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context ->
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdMobManager.TEST_BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    })
}
