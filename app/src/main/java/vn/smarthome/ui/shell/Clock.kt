package vn.smarthome.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Gio phut ngay thang hien tai, tu cap nhat. */
data class ClockNow(
    val time: String,     // 16:45
    val date: String,     // Th 7, 08/11/2025
    val greeting: String, // Chào buổi chiều!
)

/**
 * Dong ho that cua may - KHONG phai so lieu mau.
 *
 * Nhip cap nhat tuy cho dung: sidebar chi can phut (tietKiem = true), con
 * khung camera can giay cho giong dong ho ghi hinh.
 */
@Composable
fun rememberClock(seconds: Boolean = false): ClockNow {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(seconds) {
        while (true) {
            now = LocalDateTime.now()
            delay(if (seconds) 1000L else 15_000L)
        }
    }

    return ClockNow(
        time = format(now, seconds),
        date = vietnameseDate(now),
        greeting = greeting(now.hour),
    )
}

private fun two(v: Int) = if (v < 10) "0" + v.toString() else v.toString()

private fun format(t: LocalDateTime, seconds: Boolean): String =
    if (seconds) two(t.hour) + ":" + two(t.minute) + ":" + two(t.second)
    else two(t.hour) + ":" + two(t.minute)

/** Chu nhat la "CN", con lai la "Th 2".."Th 7" theo cach noi o Viet Nam. */
private fun vietnameseDate(t: LocalDateTime): String {
    val dow = when (t.dayOfWeek.value) {
        7 -> "CN"
        else -> "Th " + (t.dayOfWeek.value + 1).toString()
    }
    return dow + ", " + two(t.dayOfMonth) + "/" + two(t.monthValue) + "/" + t.year.toString()
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..10 -> "Chào buổi sáng!"
    in 11..12 -> "Chào buổi trưa!"
    in 13..17 -> "Chào buổi chiều!"
    else -> "Chào buổi tối!"
}

/** Dong thoi gian day du cho overlay camera: 16/11/2026 22:30:11 */
@Composable
fun rememberCameraStamp(): String {
    val c = rememberClock(seconds = true)
    val d = c.date.substringAfter(", ")
    return d + " " + c.time
}
