package com.compose.gsxgoogleadscompose.pip

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.gsx.googleadcompose.GoogleAds.nativead.AdmNativePip

/** Màn test native PIP: card nổi góc trên phải, kéo thả được, thả hít về góc gần nhất. */
@Composable
fun PipScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF120726))) {
        Text(
            "Native PIP\nkéo card đi thử",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        AdmNativePip(Modifier.fillMaxSize(), onClose = { Log.d("AdmNativePip", "closed") }) {
            onError = { type -> Log.w("AdmNativePip", "lỗi: $type") }
        }
    }
}
