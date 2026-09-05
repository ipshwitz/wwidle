package com.wyrmwhelp.idlehoard.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.wyrmwhelp.idlehoard.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Google Mobile Ads' rewarded-ad API. App-scoped
 * singleton (like `GameEngine`) since a loaded ad should survive across
 * screens/configuration changes rather than being reloaded per-composable.
 * Initializes the Mobile Ads SDK and starts loading the first ad as soon as
 * Hilt constructs this (once, app-wide) — no separate `initialize()` call
 * needed from a ViewModel or `Application` class.
 *
 * Currently wired to one specific rewarded placement — the Welcome Back
 * dialog's "double your offline earnings" (see
 * `GameViewModel.watchAdToDoubleOfflineEarnings`). A second placement
 * (Shop's still-disabled "Watch an Ad" for Platinum) would need its own ad
 * unit id and its own `RewardedAd?` slot — this class isn't written to
 * juggle multiple concurrent placements yet, since only one exists.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        // This is the real, live ad unit — Google's own policy on invalid
        // traffic means it must never actually serve/load a real ad on a
        // dev device or emulator. Registering test devices makes the SDK
        // return Google's test creative instead through this same real
        // unit id, with no revenue or policy risk. DEVICE_ID_EMULATOR
        // covers any AVD; a physical dev device would need its own hashed
        // id added here too (Logcat prints the exact line once it makes a
        // request without being registered).
        if (BuildConfig.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build(),
            )
        }
        MobileAds.initialize(context)
        loadAd()
    }

    /** True once a rewarded ad has finished loading and is ready for [showAd]. */
    fun isAdReady(): Boolean = rewardedAd != null

    private fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            OFFLINE_DOUBLE_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the loaded rewarded ad on [activity]. [onRewardEarned] fires only
     * if the player watches to completion — Google's own reward-eligibility
     * rules, closing early doesn't call it. [onUnavailable] fires immediately
     * if no ad is ready yet (e.g. still loading, or the last load failed)
     * rather than waiting on a dismiss/fail callback that would never come.
     * Always kicks off loading the next ad afterward — on reward, on
     * unavailability, on a mid-ad failure, and on a plain dismissal alike.
     */
    fun showAd(activity: Activity, onRewardEarned: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            loadAd()
            return
        }
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadAd()

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${error.message}")
                loadAd()
            }
        }
        ad.show(activity) { onRewardEarned() }
    }

    private companion object {
        const val TAG = "AdManager"

        // Rewarded placement: doubling the Welcome Back dialog's offline
        // earnings — see GameViewModel.watchAdToDoubleOfflineEarnings.
        const val OFFLINE_DOUBLE_AD_UNIT_ID = "ca-app-pub-1913393601233746/1494731799"
    }
}
