package com.example.smartread.fokus.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class GardenColors(
    val glow: Color,
    val air: Color,
    val farHill: Color,
    val nearHill: Color,
    val meadow: Color,
    val grass: Color,
    val potLight: Color,
    val potBody: Color,
    val potShade: Color,
    val soil: Color,
    val moss: Color,
    val bark: Color,
    val barkHighlight: Color,
    val leafDark: Color,
    val leaf: Color,
    val leafLight: Color,
    val blossom: Color,
    val water: Color
)

private val daylight = GardenColors(
    glow = Color(0xFFF0F9D4),
    air = Color(0xFF4C7D63),
    farHill = Color(0xFFC7DCBE),
    nearHill = Color(0xFFB5CFAB),
    meadow = Color(0xFF9CBC91),
    grass = Color(0xFF648A60),
    potLight = Color(0xFFC67B57),
    potBody = Color(0xFFA55E3E),
    potShade = Color(0xFF773E26),
    soil = Color(0xFF4C3A2D),
    moss = Color(0xFF689756),
    bark = Color(0xFF564231),
    barkHighlight = Color(0xFF83684F),
    leafDark = Color(0xFF27563C),
    leaf = Color(0xFF4E844D),
    leafLight = Color(0xFFC1D88A),
    blossom = Color(0xFFF5F8DE),
    water = Color(0xFF237F64)
)

private val moonlight = GardenColors(
    glow = Color(0xFFDAE1A2),
    air = Color(0xFFB7DEC2),
    farHill = Color(0xFF314B3C),
    nearHill = Color(0xFF3C5945),
    meadow = Color(0xFF4F6C51),
    grass = Color(0xFF81A476),
    potLight = Color(0xFF8E6753),
    potBody = Color(0xFF6F4E3E),
    potShade = Color(0xFF463026),
    soil = Color(0xFF2B221B),
    moss = Color(0xFF628D58),
    bark = Color(0xFF8E7A62),
    barkHighlight = Color(0xFFB39F82),
    leafDark = Color(0xFF386847),
    leaf = Color(0xFF6FA069),
    leafLight = Color(0xFFC7E198),
    blossom = Color(0xFFE7F0C5),
    water = Color(0xFF99E6C1)
)

private data class AirStream(
    val y: Float,
    val offset: Float,
    val speed: Float,
    val length: Float,
    val amplitude: Float,
    val hasCurl: Boolean
)

private val airStreams = listOf(
    AirStream(y = 48f, offset = 0.04f, speed = 2f, length = 78f, amplitude = 5.5f, hasCurl = false),
    AirStream(y = 86f, offset = 0.41f, speed = 2f, length = 92f, amplitude = 6.5f, hasCurl = true),
    AirStream(y = 128f, offset = 0.75f, speed = 1f, length = 72f, amplitude = 4.5f, hasCurl = false)
)

@Composable
internal fun TreeIllustration(growth: State<Float>, watering: State<Float>, modifier: Modifier) {
    val dark = LocalDarkTheme.current
    val colors = if (dark) moonlight else daylight
    val breezeTransition = rememberInfiniteTransition(label = "Garden breeze")
    val breezeCycle by breezeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "Breeze cycle"
    )
    val streamPath = remember { Path() }
    val curlPath = remember { Path() }

    Canvas(modifier) {
        val age = (growth.value / 5.85f).coerceIn(0f, 1f)
        val theta = breezeCycle * 2.0 * PI
        val gust = (0.58 * sin(theta) + 0.28 * sin(2.0 * theta + 0.85) + 0.14 * sin(3.0 * theta + 2.1)).toFloat()

        withTransform({ scale(size.width / 320f, size.height / 222f, Offset.Zero) }) {
            drawLandscape(colors, theta)
            drawAir(colors, dark, breezeCycle, theta, streamPath, curlPath)
            drawBonsaiPot(colors)
        }

        val drawingScale = minOf(size.width / 320f, size.height / 222f)
        withTransform({
            translate(size.width / 2, size.height * 176f / 222f)
            scale(drawingScale, drawingScale, Offset.Zero)
        }) {
            drawBonsaiTree(age, colors, gust, theta)
            drawWatering(watering.value, age, colors.water)
        }
    }
}

private fun DrawScope.drawLandscape(colors: GardenColors, theta: Double) {
    val sun = Offset(213f, 54f)
    drawCircle(
        Brush.radialGradient(listOf(colors.glow.copy(alpha = 0.48f), Color.Transparent), sun, 90f),
        radius = 90f, center = sun
    )
    drawCircle(colors.glow.copy(alpha = 0.66f), 15f, sun)
    drawCircle(colors.glow.copy(alpha = 0.48f), 23f, sun, style = Stroke(0.8f))
    listOf(Offset(57f, 55f), Offset(95f, 29f), Offset(141f, 53f), Offset(255f, 31f), Offset(270f, 81f))
        .forEach { drawCircle(colors.glow, 1.5f, it) }

    drawPath(Path().apply {
        moveTo(0f, 129f)
        cubicTo(43f, 102f, 78f, 99f, 131f, 132f)
        cubicTo(186f, 159f, 217f, 96f, 276f, 110f)
        quadraticTo(293f, 114f, 320f, 133f)
        lineTo(320f, 222f)
        lineTo(0f, 222f)
        close()
    }, colors.farHill)
    drawPath(Path().apply {
        moveTo(0f, 149f)
        cubicTo(65f, 158f, 79f, 181f, 128f, 152f)
        cubicTo(179f, 120f, 218f, 160f, 256f, 145f)
        quadraticTo(285f, 132f, 320f, 147f)
        lineTo(320f, 222f)
        lineTo(0f, 222f)
        close()
    }, colors.nearHill)
    drawPath(Path().apply {
        moveTo(0f, 194f)
        cubicTo(49f, 178f, 98f, 197f, 146f, 191f)
        cubicTo(212f, 180f, 255f, 203f, 320f, 174f)
        lineTo(320f, 222f)
        lineTo(0f, 222f)
        close()
    }, colors.meadow)

    listOf(Offset(34f, 188f), Offset(60f, 203f), Offset(86f, 211f), Offset(234f, 204f), Offset(267f, 192f), Offset(292f, 205f))
        .forEachIndexed { index, point ->
            val lift = if (index % 2 == 0) 7f else 5f
            val wave = sin(theta * 2.0 - point.x / 85.0).toFloat()
            val bend = 1.6f + wave * 2.4f
            val leftTip = point + Offset(-3.6f + bend * 0.85f, -lift)
            val rightTip = point + Offset(4.0f + bend, -lift - 1f)
            drawLine(colors.grass, point, leftTip, 1.15f, StrokeCap.Round)
            drawLine(colors.grass, point, rightTip, 1.15f, StrokeCap.Round)
            if (index % 2 == 0) drawCircle(colors.blossom, 1.8f, rightTip + Offset(0.3f, -1f))
        }
}

private fun DrawScope.drawBonsaiPot(colors: GardenColors) {
    // Soft ground shadow beneath the ceramic planter feet
    drawOval(colors.potShade.copy(alpha = 0.28f), Offset(96f, 204f), Size(128f, 11f))

    // Sculpted tokoname bonsai pot feet
    drawRoundRect(colors.potShade, Offset(115f, 200f), Size(14f, 8f), CornerRadius(3f, 3f))
    drawRoundRect(colors.potShade, Offset(191f, 200f), Size(14f, 8f), CornerRadius(3f, 3f))

    // Rich dark bonsai soil inside the pot opening
    drawOval(colors.soil, Offset(104f, 170f), Size(112f, 12f))

    // Soft cushion moss mounds flanking the root base (nebari)
    drawOval(colors.moss, Offset(116f, 171f), Size(46f, 8f))
    drawOval(colors.moss.copy(alpha = 0.9f), Offset(154f, 171.5f), Size(48f, 7.5f))
    drawOval(colors.leafLight.copy(alpha = 0.45f), Offset(132f, 172f), Size(22f, 4f))

    // Glazed trapezoidal ceramic pot body
    val body = Path().apply {
        moveTo(103f, 180f)
        lineTo(217f, 180f)
        lineTo(209f, 202f)
        quadraticTo(160f, 205f, 111f, 202f)
        close()
    }
    drawPath(
        path = body,
        brush = Brush.horizontalGradient(
            listOf(colors.potLight, colors.potBody, colors.potShade),
            startX = 103f,
            endX = 217f
        )
    )

    // Subtle decorative horizontal groove & glazed ceramic sheen on pot face
    drawLine(
        colors.potShade.copy(alpha = 0.38f),
        Offset(112f, 194f),
        Offset(208f, 194f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
    drawLine(
        colors.potLight.copy(alpha = 0.42f),
        Offset(113f, 184f),
        Offset(145f, 184f),
        strokeWidth = 1.3f,
        cap = StrokeCap.Round
    )

    // Rolled ceramic rim / lip of the planter
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(colors.potLight, colors.potBody, colors.potShade),
            startX = 98f,
            endX = 222f
        ),
        topLeft = Offset(98f, 174.5f),
        size = Size(124f, 6.8f),
        cornerRadius = CornerRadius(3.4f, 3.4f)
    )
    drawLine(
        colors.glow.copy(alpha = 0.22f),
        Offset(103f, 176.2f),
        Offset(172f, 176.2f),
        strokeWidth = 0.9f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawAir(
    colors: GardenColors,
    dark: Boolean,
    breezeCycle: Float,
    theta: Double,
    streamPath: Path,
    curlPath: Path
) {
    val peakAlpha = if (dark) 0.28f else 0.30f
    airStreams.forEachIndexed { index, stream ->
        val progress = (breezeCycle * stream.speed + stream.offset) % 1f
        val xStart = -96f + progress * 440f
        val xEnd = xStart + stream.length
        val fade = sin(progress * PI).toFloat().coerceIn(0f, 1f)
        val alpha = peakAlpha * fade
        if (alpha > 0.01f) {
            val ripple = sin(theta * 2.0 + index * 1.4).toFloat() * stream.amplitude
            streamPath.reset()
            streamPath.moveTo(xStart, stream.y)
            streamPath.cubicTo(
                xStart + stream.length * 0.34f, stream.y - stream.amplitude + ripple * 0.45f,
                xStart + stream.length * 0.66f, stream.y + stream.amplitude * 0.7f - ripple * 0.45f,
                xEnd, stream.y - 1.5f
            )
            drawPath(
                path = streamPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        colors.air.copy(alpha = alpha),
                        colors.air.copy(alpha = alpha * 0.72f),
                        Color.Transparent
                    ),
                    startX = xStart,
                    endX = xEnd
                ),
                style = Stroke(width = 1.6f, cap = StrokeCap.Round)
            )
            if (stream.hasCurl) {
                val curlStart = xStart + stream.length * 0.45f
                val curlEnd = curlStart + 28f
                val curlY = stream.y + 8f
                curlPath.reset()
                curlPath.moveTo(curlStart, curlY)
                curlPath.cubicTo(
                    curlStart + 11f, curlY + 5f,
                    curlEnd - 4f, curlY - 6f,
                    curlEnd - 9f, curlY - 1.5f
                )
                drawPath(
                    path = curlPath,
                    color = colors.air.copy(alpha = alpha * 0.65f),
                    style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                )
            }
        }
    }

    repeat(5) { index ->
        val speed = if (index % 2 == 0) 1f else 2f
        val p = (breezeCycle * speed + index * 0.19f) % 1f
        val x = -18f + p * 356f
        val baseY = 42f + (index * 23f)
        val y = baseY + sin(theta * 2.0 + index * 1.3).toFloat() * 7.5f
        val moteAlpha = sin(p * PI).toFloat().coerceIn(0f, 1f) * if (dark) 0.48f else 0.42f
        if (moteAlpha > 0.01f) {
            drawCircle(
                color = if (index % 2 == 0) colors.glow.copy(alpha = moteAlpha) else colors.air.copy(alpha = moteAlpha),
                radius = if (index % 2 == 0) 1.8f else 1.4f,
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawBonsaiTree(age: Float, colors: GardenColors, gust: Float, theta: Double) {
    val height = 0.34f + age * 0.70f
    val width = 0.44f + age * 0.60f
    val crown = ((age - 0.20f) / 0.35f).coerceIn(0f, 1f)
    val youngLeaves = (1f - crown).coerceIn(0f, 1f)
    val trunkColor = lerp(colors.leafDark, colors.bark, crown)
    val baseSway = 1.1f + gust * 2.0f
    val stemAngle = baseSway * (1.2f - 0.62f * crown)

    withTransform({ scale(width, height, Offset.Zero) }) {
        rotate(degrees = stemAngle, pivot = Offset.Zero) {
            // Exposed surface roots (nebari) gripping the bonsai moss
            val rootSpread = 6f + 16f * crown
            drawPath(
                Path().apply {
                    moveTo(-rootSpread, 1.5f)
                    quadraticTo(-7f, -4f, -4f, -12f)
                    lineTo(5f, -12f)
                    quadraticTo(9f, -4f, rootSpread, 1.5f)
                    quadraticTo(0f, 3.2f, -rootSpread, 1.5f)
                    close()
                },
                trunkColor
            )

            // Winding S-curved bonsai trunk (Moyogi style)
            val trunk = Path().apply {
                moveTo(-7.5f - 5.5f * crown, 0f)
                cubicTo(-13f, -24f, 16f, -44f, 13f, -68f)
                cubicTo(10f, -88f, -15f, -98f, -10f, -118f)
                cubicTo(-6f, -106f, 20f, -90f, 20f, -66f)
                cubicTo(20f, -40f, -1f, -22f, 6.5f + 5.5f * crown, 0f)
                close()
            }
            drawPath(
                path = trunk,
                brush = Brush.horizontalGradient(
                    listOf(trunkColor, colors.barkHighlight),
                    startX = -14f,
                    endX = 22f
                )
            )

            // Winding bonsai branches reaching out for asymmetric foliage pads
            if (crown > 0f) {
                drawPath(
                    Path().apply {
                        // Lower left overhanging branch
                        moveTo(-2f, -34f)
                        cubicTo(-26f, -36f, -46f, -42f, -66f, -54f)
                        // Mid right balancing branch
                        moveTo(15f, -58f)
                        cubicTo(36f, -58f, 52f, -64f, 68f, -74f)
                        // Upper left accent branch
                        moveTo(2f, -86f)
                        cubicTo(-18f, -90f, -36f, -96f, -48f, -102f)
                        // Apex limb
                        moveTo(-10f, -112f)
                        cubicTo(-4f, -122f, 2f, -126f, 6f, -132f)
                    },
                    color = trunkColor.copy(alpha = crown),
                    style = Stroke(width = 4.6f, cap = StrokeCap.Round)
                )
            }

            // Young seedling/sprout foliage emerging from the curved shoot
            if (youngLeaves > 0f) {
                val flutter = { idx: Int -> gust * 2.4f + sin(theta * 2.0 + idx * 1.3).toFloat() * 1.7f }
                rotate(flutter(0), Offset(-10f, -116f)) {
                    leaf(Offset(-10f, -116f), Offset(-56f, -136f), 22f, colors.leaf.copy(alpha = youngLeaves))
                }
                rotate(flutter(1), Offset(13f, -68f)) {
                    leaf(Offset(13f, -68f), Offset(62f, -94f), 24f, colors.leafDark.copy(alpha = youngLeaves))
                }
                rotate(flutter(2), Offset(-8f, -118f)) {
                    leaf(Offset(-8f, -118f), Offset(18f, -152f), 17f, colors.leafLight.copy(alpha = youngLeaves))
                }
                if (age > 0.08f) {
                    rotate(flutter(3), Offset(-4f, -44f)) {
                        leaf(Offset(-4f, -44f), Offset(-48f, -62f), 16f, colors.leafLight.copy(alpha = youngLeaves))
                    }
                }
            }

            // Tiered cloud-like bonsai foliage pads + blossoms
            if (crown > 0f) {
                val bloom = ((age - 0.78f) / 0.18f).coerceIn(0f, 1f)
                drawBonsaiPad(
                    center = Offset(-64f, -58f),
                    width = 58f,
                    height = 24f,
                    coverage = crown,
                    colors = colors,
                    swayDeg = gust * 1.6f + sin(theta * 2.0 + 0.4).toFloat() * 1.8f,
                    bloom = bloom,
                    blossomSeed = 1,
                    theta = theta
                )
                drawBonsaiPad(
                    center = Offset(66f, -77f),
                    width = 62f,
                    height = 25f,
                    coverage = crown,
                    colors = colors,
                    swayDeg = gust * 1.7f + sin(theta * 2.0 + 1.5).toFloat() * 1.9f,
                    bloom = bloom,
                    blossomSeed = 2,
                    theta = theta
                )
                drawBonsaiPad(
                    center = Offset(-44f, -104f),
                    width = 52f,
                    height = 22f,
                    coverage = crown,
                    colors = colors,
                    swayDeg = gust * 1.9f + sin(theta * 2.0 + 2.4).toFloat() * 2.0f,
                    bloom = bloom,
                    blossomSeed = 3,
                    theta = theta
                )
                drawBonsaiPad(
                    center = Offset(4f, -133f),
                    width = 70f,
                    height = 28f,
                    coverage = crown,
                    colors = colors,
                    swayDeg = gust * 2.1f + sin(theta * 2.0 + 3.2).toFloat() * 2.1f,
                    bloom = bloom,
                    blossomSeed = 4,
                    theta = theta
                )
            }
        }
    }
}

private fun DrawScope.drawBonsaiPad(
    center: Offset,
    width: Float,
    height: Float,
    coverage: Float,
    colors: GardenColors,
    swayDeg: Float,
    bloom: Float,
    blossomSeed: Int,
    theta: Double
) {
    rotate(degrees = swayDeg, pivot = center + Offset(0f, height * 0.35f)) {
        // Shaded flat-bottomed bonsai foliage cushion (layered cloud silhouette)
        drawOval(
            color = colors.leafDark.copy(alpha = coverage),
            topLeft = Offset(center.x - width * 0.50f, center.y - height * 0.38f),
            size = Size(width, height * 0.88f)
        )
        drawOval(
            color = colors.leaf.copy(alpha = coverage),
            topLeft = Offset(center.x - width * 0.44f, center.y - height * 0.54f),
            size = Size(width * 0.88f, height * 0.80f)
        )
        drawOval(
            color = colors.leafLight.copy(alpha = coverage * 0.82f),
            topLeft = Offset(center.x - width * 0.28f, center.y - height * 0.58f),
            size = Size(width * 0.54f, height * 0.46f)
        )
        // Small side puffs for organic cloud contour
        drawCircle(colors.leaf.copy(alpha = coverage), height * 0.32f, center + Offset(-width * 0.32f, -height * 0.08f))
        drawCircle(colors.leaf.copy(alpha = coverage), height * 0.30f, center + Offset(width * 0.32f, -height * 0.05f))

        if (bloom > 0f) {
            val blossoms = listOf(
                center + Offset(-width * 0.22f, -height * 0.22f),
                center + Offset(width * 0.16f, -height * 0.28f),
                center + Offset(0f, -height * 0.06f)
            )
            blossoms.forEachIndexed { idx, spot ->
                val drift = Offset(
                    sin(theta * 2.0 + blossomSeed + idx).toFloat() * 0.8f,
                    cos(theta * 3.0 + idx).toFloat() * 0.4f
                )
                val pos = spot + drift
                repeat(5) { petal ->
                    val angle = petal / 5.0 * 2 * PI + sin(theta * 2.0 + idx) * 0.08
                    drawCircle(
                        colors.blossom.copy(alpha = bloom),
                        1.7f,
                        pos + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * 2.2f
                    )
                }
                drawCircle(colors.potLight.copy(alpha = bloom), 1.25f, pos)
            }
        }
    }
}

private fun DrawScope.leaf(base: Offset, tip: Offset, width: Float, color: Color) {
    val axis = tip - base
    val perpendicular = Offset(-axis.y, axis.x) * (width / axis.getDistance())
    val upper = base + axis * 0.43f - perpendicular
    val lower = base + axis * 0.72f + perpendicular * 0.72f
    drawPath(Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(upper.x, upper.y, tip.x, tip.y)
        quadraticTo(lower.x, lower.y, base.x, base.y)
        close()
    }, color)
    val vein = base + axis * 0.68f
    drawLine(Color.White.copy(alpha = color.alpha * 0.25f), base, vein, 1f, StrokeCap.Round)
}

private fun DrawScope.drawWatering(progress: Float, age: Float, water: Color) {
    if (progress <= 0f || progress >= 1f) return
    val height = 54f + age * 102f
    repeat(7) { index ->
        val fall = (progress - index * 0.035f) / 0.68f
        if (fall in 0f..1f) {
            val x = -46f + index * 15f
            val y = -height + index % 3 * 9f + fall * height
            val alpha = sin(fall * PI).toFloat()
            drawLine(water.copy(alpha = alpha), Offset(x + 3f, y - 8f), Offset(x, y), 2.6f, StrokeCap.Round)
        }
    }
    val ripple = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    if (ripple > 0f) {
        val radius = 6f + 36f * ripple
        drawOval(water.copy(alpha = (1f - ripple) * 0.55f), Offset(-radius, -2f - radius * 0.10f),
            Size(radius * 2f, radius * 0.18f), style = Stroke(1.4f))
    }
}
