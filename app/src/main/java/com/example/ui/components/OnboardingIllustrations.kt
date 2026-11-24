package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object OnboardingPalette {
    val CobaltBlue = Color(0xFF0D24BD)
    val CobaltDeep = Color(0xFF08178A)
    val CobaltBright = Color(0xFF2845F5)
    val PureWhite = Color(0xFFFFFFFF)
    val WarmAmber = Color(0xFFF8A32B)
    val WarmAmberDeep = Color(0xFFD9820B)
    val Obsidian = Color(0xFF14161A)
    val SlateInk = Color(0xFF475569)

    val MintAccent = Color(0xFF18B894)
    val MintBright = Color(0xFF34D3B4)
    val MintDeep = Color(0xFF0F766E)
    val RibbonCoral = Color(0xFFFF7A45)
    val RibbonCoralDeep = Color(0xFFD94B1A)
    val RibbonCoralHighlight = Color(0xFFFF9666)
}

/**
 * Slide 1 Illustration: Angled crisp white smartphone outline with 3D extruded
 * cobalt & amber gears symbolizing automated personal finance setup.
 */
@Composable
fun OnboardingIsometricSlide1(
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "slide1_motion")
    val floatY by inf.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide1_float"
    )
    val gearAngle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slide1_gear_rot"
    )

    val pageVisibility = (1f - abs(parallaxOffset).coerceIn(0f, 1f))
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = pageVisibility
                translationX = -parallaxOffset * 48f
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val strokeThin = (unit * 0.0055f).coerceAtLeast(1.8f)

        // Angled white smartphone centered comfortably with top breathing room
        val phoneCenter = Offset(w * 0.40f, h * 0.52f + floatY * unit * 0.014f)
        val phoneW = unit * 0.56f
        val phoneH = unit * 0.86f
        val phoneCorner = unit * 0.095f

        withTransform({
            translate(left = phoneCenter.x, top = phoneCenter.y)
            rotate(degrees = -32f, pivot = Offset.Zero)
        }) {
            val rectLeft = -phoneW / 2f
            val rectTop = -phoneH / 2f

            // Extruded outer wireframe edge (bottom-left thickness)
            drawRoundRect(
                color = OnboardingPalette.PureWhite.copy(alpha = 0.75f),
                topLeft = Offset(rectLeft - unit * 0.022f, rectTop + unit * 0.024f),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneCorner, phoneCorner),
                style = Stroke(width = strokeThin)
            )
            // Main phone white face
            drawRoundRect(
                color = OnboardingPalette.PureWhite,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneCorner, phoneCorner)
            )
            // Outer crisp contour
            drawRoundRect(
                color = OnboardingPalette.CobaltBlue,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(phoneW, phoneH),
                cornerRadius = CornerRadius(phoneCorner, phoneCorner),
                style = Stroke(width = strokeThin * 1.15f)
            )
            // Inner bezel frame line
            val bezelPad = unit * 0.019f
            drawRoundRect(
                color = OnboardingPalette.CobaltBlue.copy(alpha = 0.65f),
                topLeft = Offset(rectLeft + bezelPad, rectTop + bezelPad),
                size = Size(phoneW - bezelPad * 2f, phoneH - bezelPad * 2f),
                cornerRadius = CornerRadius(phoneCorner * 0.85f, phoneCorner * 0.85f),
                style = Stroke(width = strokeThin * 0.75f)
            )
            // Bottom speaker / USB slot
            val slotW = unit * 0.068f
            val slotH = unit * 0.014f
            drawRoundRect(
                color = OnboardingPalette.CobaltBlue,
                topLeft = Offset(-slotW / 2f, rectTop + phoneH - bezelPad * 0.6f - slotH / 2f),
                size = Size(slotW, slotH),
                cornerRadius = CornerRadius(slotH / 2f, slotH / 2f),
                style = Stroke(width = strokeThin * 0.85f)
            )

            // Subtle wallet card lines inside screen bottom
            val barY = rectTop + phoneH * 0.78f
            drawLine(
                color = OnboardingPalette.CobaltBlue.copy(alpha = 0.20f),
                start = Offset(rectLeft + unit * 0.075f, barY),
                end = Offset(rectLeft + phoneW - unit * 0.075f, barY),
                strokeWidth = strokeThin * 2.0f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = OnboardingPalette.WarmAmber.copy(alpha = 0.85f),
                start = Offset(rectLeft + unit * 0.075f, barY + unit * 0.034f),
                end = Offset(rectLeft + phoneW * 0.52f, barY + unit * 0.034f),
                strokeWidth = strokeThin * 2.0f,
                cap = StrokeCap.Round
            )
        }

        // Large 3D extruded isometric Hero Gear overlapping the phone screen
        val bigGearCenter = Offset(w * 0.40f, h * 0.54f + floatY * unit * 0.016f)
        drawExtrudedGear(
            center = bigGearCenter,
            outerRadius = unit * 0.21f,
            innerRadius = unit * 0.16f,
            holeRadius = unit * 0.086f,
            teeth = 12,
            rotationDeg = gearAngle * 0.45f,
            extrudeOffset = Offset(unit * 0.038f, -unit * 0.026f),
            frontColor = OnboardingPalette.PureWhite,
            depthColor = OnboardingPalette.CobaltBlue,
            strokeColor = OnboardingPalette.CobaltBlue,
            strokeWidth = strokeThin * 1.1f
        )
        // Inner cobalt crescent accent inside gear hole
        drawCircle(
            color = OnboardingPalette.CobaltBlue,
            radius = unit * 0.072f,
            center = bigGearCenter + Offset(-unit * 0.015f, unit * 0.010f)
        )
        drawCircle(
            color = OnboardingPalette.PureWhite,
            radius = unit * 0.045f,
            center = bigGearCenter + Offset(-unit * 0.006f, unit * 0.003f)
        )

        // Top-right floating Amber gear
        val amberGearTop = Offset(w * 0.82f, h * 0.52f - floatY * unit * 0.018f)
        drawExtrudedGear(
            center = amberGearTop,
            outerRadius = unit * 0.066f,
            innerRadius = unit * 0.048f,
            holeRadius = unit * 0.022f,
            teeth = 8,
            rotationDeg = -gearAngle,
            extrudeOffset = Offset(-unit * 0.014f, unit * 0.011f),
            frontColor = OnboardingPalette.WarmAmber,
            depthColor = OnboardingPalette.WarmAmberDeep,
            strokeColor = OnboardingPalette.PureWhite,
            strokeWidth = strokeThin * 0.85f
        )

        // Mid-right crisp white gear
        val whiteGearMid = Offset(w * 0.72f, h * 0.78f + floatY * unit * 0.015f)
        drawExtrudedGear(
            center = whiteGearMid,
            outerRadius = unit * 0.050f,
            innerRadius = unit * 0.036f,
            holeRadius = unit * 0.017f,
            teeth = 8,
            rotationDeg = gearAngle * 1.1f + 15f,
            extrudeOffset = Offset(unit * 0.01f, -unit * 0.008f),
            frontColor = OnboardingPalette.PureWhite,
            depthColor = OnboardingPalette.CobaltDeep,
            strokeColor = OnboardingPalette.CobaltBlue,
            strokeWidth = strokeThin * 0.8f
        )

        // Lower-left floating small Amber gear
        val amberGearLow = Offset(w * 0.16f, h * 0.81f - floatY * unit * 0.016f)
        drawExtrudedGear(
            center = amberGearLow,
            outerRadius = unit * 0.046f,
            innerRadius = unit * 0.034f,
            holeRadius = unit * 0.015f,
            teeth = 8,
            rotationDeg = gearAngle * 0.8f,
            extrudeOffset = Offset(-unit * 0.01f, unit * 0.009f),
            frontColor = OnboardingPalette.WarmAmber,
            depthColor = OnboardingPalette.WarmAmberDeep,
            strokeColor = OnboardingPalette.PureWhite,
            strokeWidth = strokeThin * 0.8f
        )
    }
}

/**
 * Slide 2 Illustration: Crisp White surface with isometric 3D Cobalt "Ai"
 * typography inside a tilted Warm Amber dashed bounding frame with anchor handles.
 */
@Composable
fun OnboardingIsometricSlide2(
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "slide2_motion")
    val floatY by inf.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide2_float"
    )
    val anchorPulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide2_anchor_pulse"
    )

    val pageVisibility = (1f - abs(parallaxOffset).coerceIn(0f, 1f))
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = pageVisibility
                translationX = -parallaxOffset * 44f
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val strokeThin = (unit * 0.005f).coerceAtLeast(1.8f)

        val cx = w * 0.50f
        val cy = h * 0.50f + floatY * unit * 0.016f

        // Dashed Warm Amber vector bounding box rotated ~14 degrees
        val boxW = unit * 0.72f
        val boxH = unit * 0.56f
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(unit * 0.018f, unit * 0.016f), 0f)

        withTransform({
            translate(cx, cy)
            rotate(degrees = 14f, pivot = Offset.Zero)
        }) {
            val left = -boxW / 2f
            val top = -boxH / 2f
            val right = boxW / 2f
            val bottom = boxH / 2f

            // Bezier spline curves intersecting top-left & bottom-right corners
            val curvePath = Path().apply {
                moveTo(left - unit * 0.02f, top + boxH * 0.25f)
                cubicTo(
                    left + boxW * 0.05f, top - unit * 0.04f,
                    left + boxW * 0.30f, top + boxH * 0.08f,
                    left + boxW * 0.42f, top + boxH * 0.34f
                )
                moveTo(right - boxW * 0.40f, bottom - boxH * 0.22f)
                cubicTo(
                    right - boxW * 0.14f, bottom - boxH * 0.02f,
                    right - unit * 0.02f, bottom + unit * 0.05f,
                    right + unit * 0.05f, bottom - boxH * 0.14f
                )
            }
            drawPath(
                path = curvePath,
                color = OnboardingPalette.CobaltBlue.copy(alpha = 0.78f),
                style = Stroke(width = strokeThin * 0.9f, cap = StrokeCap.Round)
            )

            // Amber dashed selection box
            drawRect(
                color = OnboardingPalette.WarmAmber,
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = strokeThin * 1.05f, pathEffect = dashEffect)
            )

            // Corner vector anchor handles
            val handleSize = unit * 0.072f * anchorPulse
            val smallHandle = unit * 0.048f * anchorPulse
            drawAnchorSquare(
                center = Offset(left, top),
                sizePx = handleSize,
                color = OnboardingPalette.CobaltBlue
            )
            drawAnchorSquare(
                center = Offset(right, top),
                sizePx = smallHandle,
                color = OnboardingPalette.WarmAmber
            )
            drawAnchorSquare(
                center = Offset(left, bottom),
                sizePx = smallHandle,
                color = OnboardingPalette.CobaltBlue
            )
            drawAnchorSquare(
                center = Offset(right, bottom),
                sizePx = handleSize,
                color = OnboardingPalette.CobaltBlue
            )
        }

        // 3D Extruded Isometric "Ai" Sculpture centered cleanly
        val extrude = Offset(unit * 0.036f, unit * 0.026f)
        val letterHeight = unit * 0.40f
        val letterBaseY = cy + letterHeight * 0.36f
        val aLeft = cx - unit * 0.29f
        val aWidth = unit * 0.31f

        // Letter "A" outer contour
        val pathA = Path().apply {
            moveTo(aLeft + aWidth * 0.56f, letterBaseY - letterHeight)
            lineTo(aLeft + aWidth * 0.79f, letterBaseY - letterHeight)
            lineTo(aLeft + aWidth * 0.95f, letterBaseY)
            lineTo(aLeft + aWidth * 0.73f, letterBaseY)
            lineTo(aLeft + aWidth * 0.69f, letterBaseY - letterHeight * 0.22f)
            lineTo(aLeft + aWidth * 0.34f, letterBaseY - letterHeight * 0.22f)
            lineTo(aLeft + aWidth * 0.24f, letterBaseY)
            lineTo(aLeft, letterBaseY)
            close()
        }
        // Cutout triangle inside "A" (drawn as path fill with white)
        val holeA = Path().apply {
            moveTo(aLeft + aWidth * 0.63f, letterBaseY - letterHeight * 0.71f)
            lineTo(aLeft + aWidth * 0.42f, letterBaseY - letterHeight * 0.37f)
            lineTo(aLeft + aWidth * 0.66f, letterBaseY - letterHeight * 0.37f)
            close()
        }

        // Extruded 3D shadow depth for "A"
        val pathADepth = Path().apply {
            addPath(pathA)
            translate(extrude)
        }
        drawPath(pathADepth, color = OnboardingPalette.PureWhite)
        drawPath(
            pathADepth,
            color = OnboardingPalette.CobaltBlue,
            style = Stroke(width = strokeThin * 1.1f, join = StrokeJoin.Round)
        )
        // Connect key depth corners on A for crisp 3D extrusion
        val cornersA = listOf(
            Offset(aLeft, letterBaseY),
            Offset(aLeft + aWidth * 0.24f, letterBaseY),
            Offset(aLeft + aWidth * 0.73f, letterBaseY),
            Offset(aLeft + aWidth * 0.95f, letterBaseY),
            Offset(aLeft + aWidth * 0.79f, letterBaseY - letterHeight)
        )
        cornersA.forEach { pt ->
            drawLine(
                color = OnboardingPalette.CobaltBlue,
                start = pt,
                end = pt + extrude,
                strokeWidth = strokeThin
            )
        }
        // Front face of "A"
        drawPath(pathA, color = OnboardingPalette.CobaltBlue)
        drawPath(holeA, color = OnboardingPalette.PureWhite)
        drawPath(
            holeA,
            color = OnboardingPalette.CobaltBlue,
            style = Stroke(width = strokeThin, join = StrokeJoin.Round)
        )

        // Letter "i" stem & floating circular dot
        val iLeft = cx + unit * 0.14f
        val stemW = unit * 0.088f
        val stemTop = letterBaseY - letterHeight * 0.68f
        val stemRect = Path().apply {
            moveTo(iLeft + stemW * 0.14f, stemTop)
            lineTo(iLeft + stemW * 1.14f, stemTop)
            lineTo(iLeft + stemW, letterBaseY)
            lineTo(iLeft, letterBaseY)
            close()
        }
        val stemDepth = Path().apply {
            addPath(stemRect)
            translate(extrude)
        }
        drawPath(stemDepth, color = OnboardingPalette.PureWhite)
        drawPath(
            stemDepth,
            color = OnboardingPalette.CobaltBlue,
            style = Stroke(width = strokeThin * 1.1f, join = StrokeJoin.Round)
        )
        listOf(
            Offset(iLeft, letterBaseY),
            Offset(iLeft + stemW, letterBaseY),
            Offset(iLeft + stemW * 1.14f, stemTop)
        ).forEach { pt ->
            drawLine(
                color = OnboardingPalette.CobaltBlue,
                start = pt,
                end = pt + extrude,
                strokeWidth = strokeThin
            )
        }
        drawPath(stemRect, color = OnboardingPalette.CobaltBlue)

        // Floating 3D extruded dot above "i"
        val dotCenter = Offset(iLeft + stemW * 0.74f, letterBaseY - letterHeight * 0.89f)
        val dotRadius = unit * 0.052f
        drawCircle(
            color = OnboardingPalette.PureWhite,
            radius = dotRadius,
            center = dotCenter + extrude
        )
        drawCircle(
            color = OnboardingPalette.CobaltBlue,
            radius = dotRadius,
            center = dotCenter + extrude,
            style = Stroke(width = strokeThin * 1.1f)
        )
        drawCircle(
            color = OnboardingPalette.CobaltBlue,
            radius = dotRadius,
            center = dotCenter
        )
    }
}

/**
 * Slide 3 Illustration: Warm Amber card with giant 3D extruded Cobalt currency
 * glyph, orbiting Rupiah coin disc, and floating crisp white coin rings.
 */
@Composable
fun OnboardingIsometricSlide3(
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "slide3_motion")
    val floatY by inf.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "slide3_float"
    )

    val pageVisibility = (1f - abs(parallaxOffset).coerceIn(0f, 1f))
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = pageVisibility
                translationX = -parallaxOffset * 48f
            }
    ) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val strokeThin = (unit * 0.0055f).coerceAtLeast(1.8f)

        // Giant 3D Cobalt currency "$" sculpture positioned with clean top clearance
        val extrude = Offset(unit * 0.042f, unit * 0.038f)
        val symbolCenter = Offset(w * 0.40f, h * 0.51f + floatY * unit * 0.014f)

        withTransform({
            translate(symbolCenter.x, symbolCenter.y)
            rotate(degrees = 26f, pivot = Offset.Zero)
            scale(scaleX = 0.84f, scaleY = 0.84f, pivot = Offset.Zero)
        }) {
            val barW = unit * 0.145f
            val barTop = -unit * 0.56f
            val barBottom = unit * 0.54f

            // Vertical spine bar
            val spine = Path().apply {
                moveTo(-barW * 0.5f, barTop)
                lineTo(barW * 0.5f, barTop)
                lineTo(barW * 0.5f, barBottom)
                lineTo(-barW * 0.5f, barBottom)
                close()
            }

            // Bold stylized S-curve strokes built via thick path
            val sShape = Path().apply {
                // Upper curve
                moveTo(unit * 0.30f, -unit * 0.22f)
                cubicTo(
                    unit * 0.28f, -unit * 0.44f,
                    -unit * 0.32f, -unit * 0.44f,
                    -unit * 0.32f, -unit * 0.16f
                )
                cubicTo(
                    -unit * 0.32f, unit * 0.06f,
                    unit * 0.34f, unit * 0.02f,
                    unit * 0.34f, unit * 0.22f
                )
                cubicTo(
                    unit * 0.34f, unit * 0.46f,
                    -unit * 0.34f, unit * 0.46f,
                    -unit * 0.34f, unit * 0.19f
                )
            }

            // Extrude shadow layer
            withTransform({ translate(extrude.x, extrude.y) }) {
                drawPath(spine, color = OnboardingPalette.WarmAmberDeep.copy(alpha = 0.25f))
                drawPath(
                    spine,
                    color = OnboardingPalette.CobaltBlue,
                    style = Stroke(width = strokeThin * 1.1f, join = StrokeJoin.Round)
                )
                drawPath(
                    sShape,
                    color = OnboardingPalette.CobaltBlue,
                    style = Stroke(
                        width = unit * 0.21f,
                        cap = StrokeCap.Butt,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Crisp amber/cobalt outline separation between extruded body and front body
            drawPath(
                sShape,
                color = OnboardingPalette.WarmAmber,
                style = Stroke(
                    width = unit * 0.22f,
                    cap = StrokeCap.Butt,
                    join = StrokeJoin.Round
                )
            )
            // Front Cobalt Blue layer
            drawPath(spine, color = OnboardingPalette.CobaltBlue)
            drawPath(
                sShape,
                color = OnboardingPalette.CobaltBlue,
                style = Stroke(
                    width = unit * 0.185f,
                    cap = StrokeCap.Butt,
                    join = StrokeJoin.Round
                )
            )
        }

        // Isometric Rupiah coin disc overlapping bottom-right of symbol
        val coinCenter = Offset(w * 0.59f, h * 0.70f - floatY * unit * 0.015f)
        val coinRx = unit * 0.125f
        val coinRy = unit * 0.115f

        // 3D rim edge
        drawOval(
            color = OnboardingPalette.CobaltBlue,
            topLeft = Offset(coinCenter.x - coinRx + unit * 0.02f, coinCenter.y - coinRy + unit * 0.018f),
            size = Size(coinRx * 2f, coinRy * 2f),
            style = Stroke(width = strokeThin * 1.3f)
        )
        drawOval(
            color = OnboardingPalette.WarmAmber,
            topLeft = Offset(coinCenter.x - coinRx + unit * 0.014f, coinCenter.y - coinRy + unit * 0.012f),
            size = Size(coinRx * 2f, coinRy * 2f)
        )
        // Front white coin disc
        drawOval(
            color = OnboardingPalette.PureWhite,
            topLeft = Offset(coinCenter.x - coinRx, coinCenter.y - coinRy),
            size = Size(coinRx * 2f, coinRy * 2f)
        )
        drawOval(
            color = OnboardingPalette.CobaltBlue,
            topLeft = Offset(coinCenter.x - coinRx, coinCenter.y - coinRy),
            size = Size(coinRx * 2f, coinRy * 2f),
            style = Stroke(width = strokeThin * 1.15f)
        )
        // Inner coin ring
        drawOval(
            color = OnboardingPalette.CobaltBlue.copy(alpha = 0.72f),
            topLeft = Offset(coinCenter.x - coinRx * 0.78f, coinCenter.y - coinRy * 0.78f),
            size = Size(coinRx * 1.56f, coinRy * 1.56f),
            style = Stroke(width = strokeThin * 0.75f)
        )
        // Stylized "R" inside the coin (for Rupiah)
        withTransform({
            translate(coinCenter.x, coinCenter.y)
            rotate(degrees = -16f, pivot = Offset.Zero)
        }) {
            val rH = unit * 0.088f
            val rW = unit * 0.056f
            val rPath = Path().apply {
                moveTo(-rW * 0.55f, rH * 0.5f)
                lineTo(-rW * 0.55f, -rH * 0.5f)
                lineTo(rW * 0.25f, -rH * 0.5f)
                cubicTo(rW * 0.65f, -rH * 0.5f, rW * 0.65f, 0f, rW * 0.20f, 0f)
                lineTo(-rW * 0.55f, 0f)
                moveTo(0f, 0f)
                lineTo(rW * 0.55f, rH * 0.5f)
            }
            drawPath(
                path = rPath,
                color = OnboardingPalette.CobaltBlue,
                style = Stroke(
                    width = strokeThin * 2.1f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Floating crisp white outline ellipse coin on right
        val ringCenter = Offset(w * 0.84f, h * 0.60f + floatY * unit * 0.022f)
        withTransform({
            translate(ringCenter.x, ringCenter.y)
            rotate(degrees = 14f, pivot = Offset.Zero)
        }) {
            drawOval(
                color = OnboardingPalette.PureWhite,
                topLeft = Offset(-unit * 0.078f, -unit * 0.052f),
                size = Size(unit * 0.156f, unit * 0.104f),
                style = Stroke(width = strokeThin * 1.15f)
            )
            drawOval(
                color = OnboardingPalette.PureWhite.copy(alpha = 0.85f),
                topLeft = Offset(-unit * 0.062f, -unit * 0.036f),
                size = Size(unit * 0.124f, unit * 0.072f),
                style = Stroke(width = strokeThin * 0.85f)
            )
        }

        // Floating striped coin-edge capsule lower-right
        val barCenter = Offset(w * 0.84f, h * 0.82f - floatY * unit * 0.018f)
        withTransform({
            translate(barCenter.x, barCenter.y)
            rotate(degrees = -30f, pivot = Offset.Zero)
        }) {
            val bW = unit * 0.145f
            val bH = unit * 0.036f
            drawRoundRect(
                color = OnboardingPalette.PureWhite,
                topLeft = Offset(-bW / 2f, -bH / 2f),
                size = Size(bW, bH),
                cornerRadius = CornerRadius(bH / 2f, bH / 2f),
                style = Stroke(width = strokeThin * 1.0f)
            )
            for (step in -2..2) {
                val x = step * (bW * 0.16f)
                drawLine(
                    color = OnboardingPalette.PureWhite,
                    start = Offset(x, -bH / 2f),
                    end = Offset(x, bH / 2f),
                    strokeWidth = strokeThin * 0.85f
                )
            }
        }
    }
}

/**
 * Architectural Cloud-to-Device vector hero illustration for the Authentication stage.
 */
@Composable
fun OnboardingAuthHeroStage(
    isSignedIn: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "auth_hero_motion")
    val pulse by inf.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth_pulse"
    )
    val packetFlow by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auth_packet_flow"
    )

    Canvas(
        modifier = modifier
            .width(264.dp)
            .height(126.dp)
    ) {
        val w = size.width
        val h = size.height
        val stroke = (h * 0.022f).coerceAtLeast(2.2f)
        val surfaceCard = if (isDark) Color(0xFF1E2432) else OnboardingPalette.PureWhite
        val cobalt = if (isDark) OnboardingPalette.CobaltBright else OnboardingPalette.CobaltBlue
        val rightAccent = if (isSignedIn) OnboardingPalette.MintAccent else cobalt

        // Subtle dashed architectural blueprint frame
        drawRoundRect(
            color = OnboardingPalette.WarmAmber.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.04f, h * 0.08f),
            size = Size(w * 0.92f, h * 0.84f),
            cornerRadius = CornerRadius(h * 0.16f, h * 0.16f),
            style = Stroke(
                width = stroke * 0.65f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
            )
        )

        // Corner blueprint anchors
        drawAnchorSquare(Offset(w * 0.04f, h * 0.08f), h * 0.075f, cobalt)
        drawAnchorSquare(Offset(w * 0.96f, h * 0.08f), h * 0.065f, OnboardingPalette.WarmAmber)
        drawAnchorSquare(Offset(w * 0.96f, h * 0.92f), h * 0.075f, rightAccent)

        // Animated dashed bridge between Local Device (left) & Cloud Vault (right)
        val bridgeY = h * 0.52f
        val startX = w * 0.34f
        val endX = w * 0.66f
        drawLine(
            color = rightAccent.copy(alpha = 0.42f),
            start = Offset(startX, bridgeY),
            end = Offset(endX, bridgeY),
            strokeWidth = stroke * 0.9f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f),
            cap = StrokeCap.Round
        )

        // Traveling sync pulse particle
        val dotX = startX + (endX - startX) * packetFlow
        drawCircle(
            color = if (isSignedIn) OnboardingPalette.MintBright else OnboardingPalette.WarmAmber,
            radius = stroke * 1.9f,
            center = Offset(dotX, bridgeY)
        )

        // Left node: Local Smartphone Vault card with 3D extruded shadow
        val phoneW = w * 0.21f
        val phoneH = h * 0.56f
        val px = w * 0.21f - phoneW / 2f
        val py = h * 0.50f - phoneH / 2f + pulse * 2.5f

        drawRoundRect(
            color = OnboardingPalette.WarmAmber,
            topLeft = Offset(px + 5.dp.toPx(), py + 5.dp.toPx()),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = surfaceCard,
            topLeft = Offset(px, py),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = cobalt,
            topLeft = Offset(px, py),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = stroke * 0.95f)
        )
        // Mini balance bars inside phone
        drawLine(
            color = OnboardingPalette.WarmAmber,
            start = Offset(px + phoneW * 0.24f, py + phoneH * 0.36f),
            end = Offset(px + phoneW * 0.76f, py + phoneH * 0.36f),
            strokeWidth = stroke * 1.4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cobalt.copy(alpha = 0.35f),
            start = Offset(px + phoneW * 0.24f, py + phoneH * 0.54f),
            end = Offset(px + phoneW * 0.62f, py + phoneH * 0.54f),
            strokeWidth = stroke * 1.15f,
            cap = StrokeCap.Round
        )
        // Bottom home pill on mini phone
        drawLine(
            color = cobalt.copy(alpha = 0.55f),
            start = Offset(px + phoneW * 0.36f, py + phoneH * 0.84f),
            end = Offset(px + phoneW * 0.64f, py + phoneH * 0.84f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )

        // Right node: Cloud Shield Vault with 3D depth
        val shieldW = w * 0.22f
        val shieldH = h * 0.56f
        val sx = w * 0.78f
        val sy = h * 0.50f - pulse * 2.5f
        val shieldPath = Path().apply {
            moveTo(sx, sy - shieldH * 0.5f)
            lineTo(sx + shieldW * 0.5f, sy - shieldH * 0.33f)
            lineTo(sx + shieldW * 0.45f, sy + shieldH * 0.12f)
            cubicTo(
                sx + shieldW * 0.38f, sy + shieldH * 0.36f,
                sx + shieldW * 0.14f, sy + shieldH * 0.48f,
                sx, sy + shieldH * 0.54f
            )
            cubicTo(
                sx - shieldW * 0.14f, sy + shieldH * 0.48f,
                sx - shieldW * 0.38f, sy + shieldH * 0.36f,
                sx - shieldW * 0.45f, sy + shieldH * 0.12f
            )
            lineTo(sx - shieldW * 0.5f, sy - shieldH * 0.33f)
            close()
        }
        val extrudeShield = Path().apply {
            addPath(shieldPath)
            translate(Offset(4.5.dp.toPx(), 4.5.dp.toPx()))
        }
        drawPath(extrudeShield, color = OnboardingPalette.WarmAmber)
        drawPath(shieldPath, color = rightAccent)
        drawPath(
            shieldPath,
            color = OnboardingPalette.PureWhite,
            style = Stroke(width = stroke * 0.85f, join = StrokeJoin.Round)
        )
        // Checkmark inside shield
        val check = Path().apply {
            moveTo(sx - shieldW * 0.18f, sy + shieldH * 0.02f)
            lineTo(sx - shieldW * 0.03f, sy + shieldH * 0.16f)
            lineTo(sx + shieldW * 0.22f, sy - shieldH * 0.12f)
        }
        drawPath(
            path = check,
            color = OnboardingPalette.PureWhite,
            style = Stroke(width = stroke * 1.45f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawAnchorSquare(
    center: Offset,
    sizePx: Float,
    color: Color
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - sizePx / 2f, center.y - sizePx / 2f),
        size = Size(sizePx, sizePx),
        cornerRadius = CornerRadius(sizePx * 0.22f, sizePx * 0.22f)
    )
}

private fun DrawScope.drawExtrudedGear(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    holeRadius: Float,
    teeth: Int,
    rotationDeg: Float,
    extrudeOffset: Offset,
    frontColor: Color,
    depthColor: Color,
    strokeColor: Color,
    strokeWidth: Float
) {
    val gearPath = buildGearPath(center, outerRadius, innerRadius, teeth, rotationDeg)
    val depthPath = Path().apply {
        addPath(gearPath)
        translate(extrudeOffset)
    }
    drawPath(depthPath, color = depthColor)
    drawPath(
        depthPath,
        color = strokeColor,
        style = Stroke(width = strokeWidth * 0.85f, join = StrokeJoin.Round)
    )

    drawPath(gearPath, color = frontColor)
    drawPath(
        gearPath,
        color = strokeColor,
        style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
    )

    // Inner hole
    drawCircle(
        color = depthColor,
        radius = holeRadius,
        center = center + extrudeOffset * 0.35f
    )
    drawCircle(
        color = strokeColor,
        radius = holeRadius,
        center = center,
        style = Stroke(width = strokeWidth * 0.9f)
    )
}

private fun buildGearPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    teeth: Int,
    rotationDeg: Float
): Path {
    val path = Path()
    val step = (2.0 * PI) / teeth
    val rotRad = Math.toRadians(rotationDeg.toDouble())
    for (i in 0 until teeth) {
        val base = rotRad + i * step
        val a0 = base
        val a1 = base + step * 0.18
        val a2 = base + step * 0.44
        val a3 = base + step * 0.62

        val p0 = Offset(
            center.x + (innerRadius * cos(a0)).toFloat(),
            center.y + (innerRadius * sin(a0)).toFloat()
        )
        val p1 = Offset(
            center.x + (outerRadius * cos(a1)).toFloat(),
            center.y + (outerRadius * sin(a1)).toFloat()
        )
        val p2 = Offset(
            center.x + (outerRadius * cos(a2)).toFloat(),
            center.y + (outerRadius * sin(a2)).toFloat()
        )
        val p3 = Offset(
            center.x + (innerRadius * cos(a3)).toFloat(),
            center.y + (innerRadius * sin(a3)).toFloat()
        )

        if (i == 0) path.moveTo(p0.x, p0.y) else path.lineTo(p0.x, p0.y)
        path.lineTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
        path.lineTo(p3.x, p3.y)
    }
    path.close()
    return path
}

/**
 * Signature 45-degree rotated rounded-diamond CTA button matching the reference UI.
 */
@Composable
fun OnboardingDiamondNextButton(
    isFinalSlide: Boolean,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "diamond_press_scale"
    )
    val animatedContainer by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(260),
        label = "diamond_bg"
    )
    val animatedIcon by animateColorAsState(
        targetValue = iconColor,
        animationSpec = tween(260),
        label = "diamond_icon"
    )

    Box(
        modifier = modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            color = animatedContainer,
            shadowElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = animatedIcon.copy(alpha = 0.08f)
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(58.dp)
                .graphicsLayer {
                    rotationZ = 45f
                    scaleX = scale
                    scaleY = scale
                }
                .testTag("onboarding_next_squircle_btn")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFinalSlide) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (isFinalSlide) "Lanjut ke autentikasi" else "Slide berikutnya",
                    tint = animatedIcon,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = -45f }
                )
            }
        }
    }
}

/**
 * Reference bottom-right pill + dots indicator (`— • •`).
 */
@Composable
fun OnboardingPillDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 20.dp else 5.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "pill_w_$index"
            )
            val color by animateColorAsState(
                targetValue = if (selected) activeColor else inactiveColor,
                animationSpec = tween(240),
                label = "pill_c_$index"
            )
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
