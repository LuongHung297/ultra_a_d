package gs.ad.gsadsexample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import gs.ad.gsadsexample.MainActivity.Companion.TAG
import gs.ad.gsadsexample.ads.GroupNativeAd
import gs.ad.gsadsexample.databinding.ActivitySplashBinding
import gs.ad.utils.ads.AdmUMP
import gs.ad.utils.ads.format.AdmNativeAd
import gs.ad.utils.google_iab.BillingClientLifecycle
import gs.ad.utils.google_iab.OnBillingListener
import gs.ad.utils.google_iab.enums.ErrorType
import gs.ad.utils.google_iab.models.ProductInfo
import gs.ad.utils.google_iab.models.PurchaseInfo
import gs.ad.utils.utils.GlobalVariables
import gs.ad.utils.utils.NetworkUtil
import gs.ad.utils.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val mBillingClientLifecycle: BillingClientLifecycle?
        get() {
            return (application as AppOwner).mBillingClientLifecycle
        }

    private lateinit var ump: AdmUMP

    private var countLoadAd = 0
    private var totalLoadAd = 0
        set(value) {
            maxProgress = value
            field = value
        }
    private var maxProgress: Int = 0
        set(value) {
            field = value * 100 + 1000
        }
    private var isInitUMP: Boolean = false

    private data class AdPreload(
        val id: Int,
        val isFullScreen: Boolean = false
    )

    private var adPosition: MutableList<AdPreload> = mutableListOf(
        AdPreload(0),
        AdPreload(0),
        AdPreload(0, true),
        AdPreload(0),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ump = AdmUMP(this)
        //reset onboard
        //PreferencesManager.getInstance().saveShowOnBoard(false)

        //reset sub
        //PreferencesManager.getInstance().purchaseFailed()

        //remove lifetime
        //PreferencesManager.getInstance().removeLifetime()

        //remove Ads
        //PreferencesManager.getInstance().removeAds(false)

        GlobalVariables.canShowOpenAd = false
        binding.splashProcessBar.visibility = View.GONE
        binding.splashProcessBar.progress = 0

        mBillingClientLifecycle?.setListener(this, eventListener = object : OnBillingListener {
            override fun onPurchasedProductsFetched(purchaseInfos: List<PurchaseInfo>) {
                super.onPurchasedProductsFetched(purchaseInfos)
                initUMP()
            }

            override fun onBillingError(errorType: ErrorType) {
                super.onBillingError(errorType)
                initUMP()
            }
        })
        isInitUMP = false
        checkNetwork()
    }

    override fun onStart() {
        super.onStart()
        GlobalVariables.canShowOpenAd = false
    }

    private fun checkNetwork() {
        if (NetworkUtil.isNetworkAvailable(this)) {
            initFirstApp()
        } else {
            AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("No Internet connection. Make sure that Wi-Fi or mobile data is turned on, then try again.")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    binding.splashProcessBar.postDelayed({
                        checkNetwork()
                    }, 500)
                }
                .create()
                .show()
        }
    }

    private fun initFirstApp() {
        if (mBillingClientLifecycle == null) {
            initUMP()
        } else {
            mBillingClientLifecycle?.fetchSubPurchasedProducts()
        }
    }

    fun initUMP() {
        if (isInitUMP) return
        isInitUMP = true
        ump.initUMP(gatherConsentFinished = {
            runOnUiThread {
                loadProgress()
            }
        })
    }

    private fun loadProgress() {
        if (!PreferencesManager.getInstance().isSUB()) {
            val progressBar = binding.splashProcessBar
            progressBar.visibility = View.VISIBLE

            if (PreferencesManager.getInstance().isShowOnBoard()) {
                adPosition.clear()
                if (adPosition.isEmpty()) countLoadAd += 1
            }

            var countLoadedAd = 0
            var countProgress = 0
            totalLoadAd = adPosition.count()
            binding.splashProcessBar.max = maxProgress
            adPosition.withIndex()
                .groupBy { it.value.id }
                .forEach { (id, group) ->
                    val positions = group.map { it.index }        // id=0 -> [0,1], id=1 -> [2,3]

                    // holder: 1 cái / position, ko load, chỉ giữ 1 NativeAd -> adapter dùng y như cũ
                    positions.forEach { position ->
                        val holder = AdmNativeAd(id, this, adPosition[position].isFullScreen)
                        holder.tag = position
                        GroupNativeAd.listOnBoardNativeAd[position] = holder
                    }

                    // loader: chỉ để loadAds(count)
                    val loader = AdmNativeAd(id, this, false)
                    loader.tag = id

                    loader.onPreloadAdsFinished = { ads ->
                        positions.forEachIndexed { slot, position ->
                            GroupNativeAd.listOnBoardNativeAd[position]?.nativeAd = ads.getOrNull(slot)
                        }
                        ads.filterNotNull().forEach { ad ->
                            ad.setOnPaidEventListener {
                                Log.d("AdmNative", "NativeAd ad paid: ${it.valueMicros}")
                            }
                        }
                        loader.nativeAds.clear()      // nhường ownership cho holder, tránh destroy 2 lần
                        runOnUiThread { countLoadAd += positions.size }
                    }

                    loader.preloadAd(positions.size)
                }

            lifecycleScope.launch(Dispatchers.Main) {
                val progressJob = async {
                    while (countProgress < maxProgress) {
                        val addProgress =
                            if (PreferencesManager.getInstance().isShowOnBoard()) 2 else 1
                        countProgress += addProgress
                        progressBar.progress = countProgress
                        delay(1)
                    }
                }

                val loadAdsJob = async(Dispatchers.IO) {
                    while (getCountLoadAd() < totalLoadAd) {
                        if (countLoadedAd != getCountLoadAd()) {
                            countLoadedAd = getCountLoadAd()
                            countProgress += 100
                            progressBar.progress = countProgress
                        }
                    }
                }

                progressJob.await()
                loadAdsJob.await()

                startMainActivity()
            }
        } else {
            startMainActivity()
        }
    }

    private suspend fun getCountLoadAd(): Int = withContext(Dispatchers.IO) {
        return@withContext countLoadAd
    }

    private fun startMainActivity() {
        val intent =
            if (PreferencesManager.getInstance().isShowOnBoard()) Intent(
                this,
                MainActivity::class.java
            )
            else Intent(this, OnBoardActivity::class.java)
        MainScope().launch {
            startActivity(intent)
            mBillingClientLifecycle?.removeListener(this@SplashActivity)
            finish()
        }
    }
}