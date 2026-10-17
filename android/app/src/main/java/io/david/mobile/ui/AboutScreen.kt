package io.david.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Settings › About david.io.
 *
 * Deliberately plain: the name, the version and the two credit lines the
 * licence and the NTL interoperability require. No accounts, links, payment
 * details or contact addresses belong to this app's upstream author here.
 */
@Composable
fun AboutScreen(
    appVersion: String,
    insetTop: Dp,
    insetBottom: Dp,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Dvd.Paper)
            .padding(top = insetTop),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Text(
                text = "Back",
                fontFamily = Dvd.Body,
                fontSize = 16.sp,
                color = Dvd.Link,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = insetBottom + 24.dp),
        ) {
            Text(
                text = "david.io",
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = Dvd.Ink,
            )
            Text(
                text = "Version $appVersion",
                fontFamily = Dvd.Body,
                fontSize = 13.sp,
                color = Dvd.Quiet,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
            )
            Text(
                text = "Based on Wyrm by OM Rajput · modified by david.io (GNU GPL v3)",
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Dvd.Ink,
            )
            Text(
                text = "Team mode is compatible with the NTL MOD for Slither.io by [NTL] Nothing To Lose.",
                fontFamily = Dvd.Body,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Dvd.Ink,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}
