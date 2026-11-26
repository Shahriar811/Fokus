package com.example.smartread.fokus.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ScreenLockLandscape
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class ClockFace(val label: String) { DIGITAL("Digital"), ANALOG("Analog") }

private val clockCanvas = Color(0xFF101713)
private val clockInk = Color(0xFFEDEBE4)

private val clockColors = darkColorScheme(
    primary = clockInk, onPrimary = clockCanvas,
    primaryContainer = Color(0xFF233229), onPrimaryContainer = clockInk,
    surface = clockCanvas, onSurface = clockInk,
    surfaceVariant = Color(0xFF1A251F), onSurfaceVariant = clockInk,
    background = clockCanvas, onBackground = clockInk
)

@Composable
internal fun ClockScreen(
    face: ClockFace,
    time: State<Long>,
    landscape: Boolean,
    onFace: (ClockFace) -> Unit,
    onRotate: (() -> Unit)?,
    onClose: () -> Unit
) {
    MaterialTheme(colorScheme = clockColors, typography = MaterialTheme.typography) {
        Column(
            Modifier.fillMaxSize().background(clockCanvas).safeDrawingPadding().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FokusIconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close clock", tint = clockInk) }
                Selector(ClockFace.entries, face, { it.label }, onFace, Modifier.weight(1f))
                if (onRotate != null) FokusIconButton(onClick = onRotate) {
                    Icon(
                        if (landscape) Icons.Outlined.ScreenLockLandscape else Icons.Outlined.ScreenRotation,
                        if (landscape) "Use device rotation" else "Lock clock to landscape",
                        tint = clockInk
                    )
                }
            }
            AnimatedContent(
                face, Modifier.fillMaxWidth().weight(1f).padding(horizontal = 13.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
                transitionSpec = { fadeIn(tween(240, 65)) togetherWith fadeOut(tween(95)) },
                label = "Clock style"
            ) { style ->
                if (style == ClockFace.DIGITAL) DigitalClock(time) else AnalogClock(time)
            }
        }
    }
}

@Composable
private fun DigitalClock(wall: State<Long>) {
    val context = LocalContext.current
    val minute by remember(wall) { derivedStateOf { wall.value / 60_000L } }
    val twentyFourHours = DateFormat.is24HourFormat(context)
    val time = Instant.ofEpochMilli(minute * 60_000L).atZone(ZoneId.systemDefault())
    val digits = time.format(DateTimeFormatter.ofPattern(if (twentyFourHours) "HH:mm" else "h:mm"))
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val widthPerDigit = maxWidth / if (digits.length == 5) 3.3f else 2.68f
        val font = with(LocalDensity.current) { minOf(widthPerDigit, maxHeight * 0.60f).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digits, color = clockInk, style = numberStyle.copy(fontSize = font, lineHeight = font * 1.05f),
                maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
            if (!twentyFourHours) Text(time.format(DateTimeFormatter.ofPattern("a")), color = clockInk,
                style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AnalogClock(wall: State<Long>) {
    val context = LocalContext.current
    val minute by remember(wall) { derivedStateOf { wall.value / 60_000L } }
    val description = DateFormat.getTimeFormat(context).format(Date(minute * 60_000L))
    val measurer = rememberTextMeasurer(cacheSize = 12)
    val style = MaterialTheme.typography.bodyMedium
    val zone = ZoneId.systemDefault()
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(minOf(maxWidth, maxHeight) * 0.96f)
                .semantics { contentDescription = description }
                .drawWithCache {
                    val radius = size.minDimension / 2f
                    val hub = Offset(size.width / 2f, size.height / 2f)
                    val numeralStyle = style.copy(color = clockInk, fontSize = (radius * 0.18f).toSp())
                    val numerals = (1..12).map { hour ->
                        val text = measurer.measure(hour.toString(), numeralStyle)
                        val position = hub + direction(hour / 12.0) * (radius * 0.69f)
                        text to (position - Offset(text.size.width / 2f, text.size.height / 2f))
                    }
                    val ticks = List(60) { index ->
                        val major = index % 5 == 0
                        val ray = direction(index / 60.0)
                        Triple(hub + ray * radius * if (major) 0.84f else 0.89f, hub + ray * radius * 0.94f, major)
                    }
                    onDrawBehind {
                        drawCircle(clockInk.copy(alpha = 0.45f), radius * 0.99f, style = Stroke(radius * 0.009f))
                        ticks.forEach { (start, end, major) ->
                            drawLine(clockInk.copy(alpha = if (major) 0.96f else 0.48f), start, end,
                                radius * if (major) 0.016f else 0.008f, StrokeCap.Round)
                        }
                        numerals.forEach { (numeral, position) -> drawText(numeral, topLeft = position) }
                        val local = Instant.ofEpochMilli(wall.value).atZone(zone)
                        val seconds = local.second + local.nano / 1_000_000_000.0
                        val minutes = local.minute + seconds / 60
                        val hours = local.hour % 12 + minutes / 60
                        drawLine(clockInk, center, center + direction(hours / 12) * radius * 0.48f,
                            radius * 0.045f, StrokeCap.Round)
                        drawLine(clockInk, center, center + direction(minutes / 60) * radius * 0.58f,
                            radius * 0.029f, StrokeCap.Round)
                        val secondHand = direction(seconds / 60)
                        drawLine(clockInk, center - secondHand * radius * 0.12f, center + secondHand * radius * 0.60f,
                            radius * 0.010f, StrokeCap.Round)
                        drawCircle(clockInk, radius * 0.038f, center)
                        drawCircle(clockCanvas, radius * 0.014f, center)
                    }
                }
        )
    }
}

private fun direction(turn: Double): Offset {
    val angle = (turn - 0.25) * PI * 2
    return Offset(cos(angle).toFloat(), sin(angle).toFloat())
}

@Composable
internal fun liveClock(sweepSeconds: Boolean): State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(System.currentTimeMillis(), lifecycle, sweepSeconds) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = System.currentTimeMillis()
                delay(if (sweepSeconds) 32L else 60_000L - value % 60_000L)
            }
        }
    }
}
