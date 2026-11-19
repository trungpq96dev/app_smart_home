package vn.smarthome.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Cot menu trai co dinh.
 *
 * Chan cot la dong ho + ngay thang that cua may tinh bang (khong phai so mau),
 * dat canh icon wifi - giong anh thiet ke.
 */
@Composable
fun SideNav(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = rememberClock()

    Column(
        modifier = modifier
            .width(Dim.SidebarWidth)
            .fillMaxHeight()
            .background(SH.Sidebar)
            .padding(horizontal = 9.dp, vertical = 10.dp),
    ) {
        // ---- Logo ----
        Image(
            painter = painterResource(vn.smarthome.R.drawable.logo_sh),
            contentDescription = "SmartHome",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .padding(bottom = 8.dp)
                .aspectRatio(1f),
        )

        // ---- Cac muc menu ----
        Dest.entries.forEach { dest ->
            NavItem(dest, dest == current) { onSelect(dest) }
            Spacer(Modifier.height(3.dp))
        }

        Spacer(Modifier.weight(1f))

        // ---- Chan: dong ho that ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Wifi, null, tint = SH.TextDim, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(clock.time, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        }
        Text(clock.date, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
    }
}

@Composable
private fun NavItem(dest: Dest, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SH.Blue else Color.Transparent, label = "navBg")
    val fg = if (selected) Color.White else SH.TextDim

    Row(
        Modifier
            .fillMaxWidth()
            .height(Dim.NavItemH)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(dest.icon, dest.title, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            dest.title,
            style = SHType.Body.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
