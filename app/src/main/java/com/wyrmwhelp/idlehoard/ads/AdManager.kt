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
 * Every rewarded-ad spot in the app, one per real AdMob ad unit id. Adding a
 * new rewarded placement means adding an entry here — [AdManager] handles
 * the rest (loading, showing, reloading) generically per-placement.
 */
enum class RewardedPlacement(val adUnitId: String) {
    /** Welcome Back dialog's "Watch Ad to Double" — see `GameViewModel.watchAdToDoubleOfflineEarnings`. */
    OFFLINE_EARNINGS_DOUBLE("ca-app-pub-1913393601233746/1494731799"),

    /** Shop's "Watch an Ad" for Platinum Pieces — see `GameViewModel.watchAdForPlatinum`. */
    SHOP_PLATINUM("ca-app-pub-1913393601233746/9425192707"),
}

/**
 * Thin wrapper around Google Mobile Ads' rewarded-ad API. App-scoped
 * singleton (like `GameEngine`) since loaded ads should survive across
 * screens/configuration changes rather than being reloaded per-composable.
 * Initializes the Mobile Ads SDK and starts loading one ad per
 * [RewardedPlacement] as soon as Hilt constructs this (once, app-wide) — no
 * separate `initialize()` call needed from a ViewModel or `Application`
 * class.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val loadedAds = mutableMapOf<RewardedPlacement, RewardedAd>()
    private val loadingPlacements = mutableSetOf<RewardedPlacement>()

    init {
        // These are the real, live ad units — Google's own policy on invalid
        // traffic means they must never actually serve/load a real ad on a
        // dev device or emulator. Registering test devices makes the SDK
        // return Google's test creative instead through these same real
        // unit ids, with no revenue or policy risk. DEVICE_ID_EMULATOR
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
        RewardedPlacement.entries.forEach(::loadAd)
    }

    /** True once [placement]'s ad has finished loading and is ready for [showAd]. */
    fun isAdReady(placement: RewardedPlacement): Boolean = loadedAds.containsKey(placement)

    private fun loadAd(placement: RewardedPlacement) {
        if (placement in loadingPlacements || loadedAds.containsKey(placement)) return
        loadingPlacements += placement
        RewardedAd.load(
            context,
            placement.adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingPlacements -= placement
                    loadedAds[placement] = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingPlacements -= placement
                    loadedAds.remove(placement)
                    Log.w(TAG, "Rewarded ad failed to load ($placement): ${error.message}")
                }
            },
        )
    }

    /**
     * Shows [placement]'s loaded rewarded ad on [activity]. [onRewardEarned]
     * fires only if the player watches to completion — Google's own
     * reward-eligibility rules, closing early doesn't call it.
     * [onUnavailable] fires immediately if no ad is ready yet (e.g. still
     * loading, or the last load failed) rather than waiting on a
     * dismiss/fail callback that would never come. Always kicks off loading
     * the next ad for this placement afterward — on reward, on
     * unavailability, on a mid-ad failure, and on a plain dismissal alike.
     */
    fun showAd(
        placement: RewardedPlacement,
        activity: Activity,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit = {},
    ) {
        val ad = loadedAds[placement]
        if (ad == null) {
            onUnavailable()
            loadAd(placement)
            return
        }
        loadedAds.remove(placement)
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = loadAd(placement)

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded ad failed to show ($placement): ${error.message}")
                loadAd(placement)
            }
        }
        ad.show(activity) { onRewardEarned() }
    }

    private companion object {
        const val TAG = "AdManager"
    }
}
