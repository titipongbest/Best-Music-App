package com.bestmusic.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestmusic.app.BuildConfig

const val APP_CREDIT = "By NorthXbar"
val APP_VERSION: String get() = "v${BuildConfig.VERSION_NAME}"

/** โลโก้เล็กในแอป: สี่เหลี่ยมมุมโค้งสีเหลืองไล่เฉด + โน้ตเพลง (เข้าชุดกับไอคอนแอป) */
@Composable
fun AppLogo(size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(Color(0xFFFFE45C), Color(0xFFFFB300)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = Color(0xFF1B1B1D), modifier = Modifier.size(size * 0.62f))
    }
}

/** บรรทัดเล็ก "v1.0.x · By NorthXbar" */
@Composable
fun AppCredit(modifier: Modifier = Modifier) {
    Text(
        "$APP_VERSION · $APP_CREDIT",
        color = TextSecondary,
        fontSize = 12.sp,
        modifier = modifier,
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("ปิด", color = AppAccent) } },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppLogo(72.dp)
                Spacer(Modifier.height(14.dp))
                Text("Best music", color = AppAccent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("เวอร์ชัน ${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "เล่นเพลงตามโฟลเดอร์ และ MV ในเครื่อง",
                    color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(APP_CREDIT, color = AppAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
    )
}
