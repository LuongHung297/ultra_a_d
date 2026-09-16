package com.compose.gsxgoogleadscompose.interstitial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compose.gsxgoogleadscompose.common.PremiumStatusText
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.gsx.googleadcompose.GoogleAds.AdmInterstitial
import com.gsx.googleadcompose.GoogleAds.nativead.AdmNativeAd
import com.gsx.googleadcompose.GoogleAds.nativead.AdmNativeFullStory

/**
 * Màn test AdmInterstitial, 2 kiểu:
 * 1. Inter thường: đóng inter là xong.
 * 2. Inter + native full: đóng inter -> native full 2 trang (mỗi trang 5s -> skip, trang cuối 5s -> X).
 */
@Composable
fun InterstitialScreen() {
    var status by remember { mutableStateOf("Chưa load") }
    var withNativeFull by remember { mutableStateOf(false) }   // kiểu của lần show đang chạy
    var showStory by remember { mutableStateOf(false) }

    // 2 native preload cùng lúc load inter -> inter đóng là có sẵn để show
    val storyAds = remember { mutableStateListOf<NativeAd?>(null, null) }
    DisposableEffect(Unit) { onDispose { storyAds.forEach { it?.destroy() } } }

    val inter = AdmInterstitial {
        onPaid = {
            type -> status = "Paid:${type.valueMicros} ${type.currencyCode}"
        }
        onDismissed = { if (withNativeFull) showStory = true }
    }

    fun preloadStoryAds() {
        storyAds.indices.forEach { i ->
            if (storyAds[i] == null) AdmNativeAd.preload { storyAds[i] = it }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF120726))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Interstitial", color = Color(0xFF52DE11), fontSize = 32.sp, fontWeight = FontWeight.W700)
            PremiumStatusText()
            Text(
                text = status,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            )

            Button(
                onClick = {
                    status = "Đang load..."
                    inter.load(1)
                    preloadStoryAds()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Load") }

            OutlinedButton(
                onClick = { withNativeFull = false; inter.show() },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text("1. Show inter thường") }

            OutlinedButton(
                onClick = { withNativeFull = true; preloadStoryAds(); inter.show() },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text("2. Show inter + native full") }
        }

        if (showStory) {
            AdmNativeFullStory(
                modifier = Modifier.fillMaxSize(),
                nativeAds = storyAds,
                onClose = {
                    showStory = false
                    storyAds.indices.forEach { i -> storyAds[i]?.destroy(); storyAds[i] = null }
                },
            ) {
                ctaColor = Color(0xFF5B8DEF)
                ctaTextColor = Color.White
            }
        }
    }
}
