package com.rjnr.thaiwrter.ui.drawing

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import kotlin.math.max
import kotlin.math.min
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.graphics.Path as ComposePath

data class StrokeSpec(
    val pathData: String,           // raw SVG "d" string
    val strokeWidth: Float = 14f
)


@Composable
fun FirstPath(modifier: Modifier = Modifier) {
//    val strokePath = remember(testStroke.pathData) {
//        androidx.compose.ui.graphics.vector.PathParser()    // UI-graphics 1.7+
//            .parsePathString(testStroke.pathData)
//            .toPath()
//    }
}

/** Compose → Android path helpers */
fun ComposePath.toAndroid() = this.asAndroidPath()
fun android.graphics.Path.toCompose() = this.asComposePath()


//@Composable
//fun StrokeGuide(
//    svgPathData: String?,
//    animationProgress: Float,       // Current progress of the looping animation (0f to 1f)
//    userHasStartedTracing: Boolean, // From ViewModel
//    marginToApply: Float = 0.15f,   // Adjusted from previous, this is the overall padding
//    staticGuideColor: Color = Color.LightGray.copy(alpha = 0.4f), // Faint static guide
//    animatedSegmentColor: Color = Color(0xFF00579C),
//    finalStaticSegmentColor: Color = Color(0xFF00579C).copy(alpha = 0.6f), // Color of guide once user starts tracing
//    modifier: Modifier = Modifier
//) {
//    if (svgPathData.isNullOrEmpty()) return
//
//    val rawPath = remember(svgPathData) {
//        androidx.compose.ui.graphics.vector.PathParser()
//            .parsePathString(svgPathData)
//            .toPath()
//            .asAndroidPath()
//    }
//    val rawBounds = remember(svgPathData) {
//        RectF().also { rawPath.computeBounds(it, true) }
//    }
//
//    val pm = remember { android.graphics.PathMeasure() }
//    val pathScaledAndTransformed = remember { android.graphics.Path() }
//    val animatedSegmentPath = remember { android.graphics.Path() }
//
//    Canvas(modifier) {
//        // --- Sizing and Centering Logic (ensure this is robust) ---
//        val padX = size.width * marginToApply
//        val padY = size.height * marginToApply
//        val availW = size.width - padX * 2
//        val availH = size.height - padY * 2
//
//        if (rawBounds.width() <= 0 || rawBounds.height() <= 0 || availW <= 0 || availH <= 0) return@Canvas
//
//        val scaleFactor = 0.9f // Small internal factor to prevent touching edges
//        val finalScale = min(availW / rawBounds.width(), availH / rawBounds.height()) * scaleFactor
//        val scaledWidth = rawBounds.width() * finalScale
//        val scaledHeight = rawBounds.height() * finalScale
//        val finalDx = padX + (availW - scaledWidth) / 2f - (rawBounds.left * finalScale)
//        val finalDy = padY + (availH - scaledHeight) / 2f - (rawBounds.top * finalScale)
//
//        val m = android.graphics.Matrix().apply {
//            postScale(finalScale, finalScale)
//            postTranslate(finalDx, finalDy)
//        }
//
//
//        pathScaledAndTransformed.reset()
//        pathScaledAndTransformed.set(rawPath)
//        pathScaledAndTransformed.transform(m)
//        // --- End Sizing ---
//
//        val strokeWidthPx = DrawingConfig.getStrokeWidth(min(size.width, size.height)) // Use config instead of hardcoded
//
//        // 1. Draw the full, faint static guide path underneath
//        drawPath(
//            path = pathScaledAndTransformed.asComposePath(),
//            color = staticGuideColor,
//            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
//        )
//
//        // 2. Draw the animated segment or the final static segment on top
//        pm.setPath(pathScaledAndTransformed, false)
//        if (pm.length > 0) {
//            animatedSegmentPath.reset()
//            val currentDisplayProgress = if (userHasStartedTracing) 1f else animationProgress
//            pm.getSegment(0f, pm.length * currentDisplayProgress, animatedSegmentPath, true)
//
//            val segmentColor = if (userHasStartedTracing) {
//                finalStaticSegmentColor // Guide color after user interaction
//            } else {
//                animatedSegmentColor // Looping animation color
//            }
//
//            drawPath(
//                path = animatedSegmentPath.asComposePath(),
//                color = segmentColor,
//                style = Stroke(
//                    width = strokeWidthPx,
//                    cap = StrokeCap.Round,
//                    join = StrokeJoin.Round
//                )
//            )
//        }
//    }
//}
@Composable
fun StrokeGuide(
    strokes: List<String>,          // ordered list
    animationProgress: Float,
    userHasStartedTracing: Boolean,
    marginToApply: Float = 0.15f,
    staticGuideColor: Color = Color.LightGray.copy(alpha = 0.4f),
    animatedSegmentColor: Color = Color(0xFF00579C),
    finalStaticSegmentColor: Color = Color(0xFF00579C).copy(alpha = 0.6f),
    modifier: Modifier = Modifier
) {
    if (strokes.isEmpty()) return        // nothing to draw

    /* ---------- PARSE & CACHE ---------- */
    val parsedPaths = remember(strokes) {
        strokes.map { d ->
            androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString(d)
                .toPath()
                .asAndroidPath()
        }
    }

    // Combine all raw paths to get a single bounding-box for scaling
    val rawBounds = remember(strokes) {
        val combined = android.graphics.Path().apply {
            parsedPaths.forEach { addPath(it) }
        }
        android.graphics.RectF().also { combined.computeBounds(it, true) }
    }

    /* ---------- RENDER ---------- */
    val pm = remember { android.graphics.PathMeasure() }
    val scaledStroke = remember { android.graphics.Path() }
    val animSegment = remember { android.graphics.Path() }

    // Compute how many strokes should be fully revealed at current progress
    // e.g. progress 0.0-0.33 = stroke1, 0.34-0.66 = stroke2, etc.
    val strokesCount = parsedPaths.size
    val progPerStroke = 1f / strokesCount
    val currentStrokeIdx = (animationProgress / progPerStroke)
        .coerceIn(0f, strokesCount - 1f)
        .toInt()
    val intraStrokeProgress = (animationProgress - currentStrokeIdx * progPerStroke) / progPerStroke

    Canvas(modifier) {
        /* ------ same scaling math you already had (kept verbatim) ------ */
        val padX = size.width * marginToApply
        val padY = size.height * marginToApply
        val availW = size.width - padX * 2
        val availH = size.height - padY * 2
        if (rawBounds.width() <= 0 || rawBounds.height() <= 0 ||
            availW <= 0 || availH <= 0) return@Canvas

        val finalScale = min(availW / rawBounds.width(), availH / rawBounds.height()) * 0.9f
        val finalDx = padX + (availW - rawBounds.width() * finalScale) / 2f -
                (rawBounds.left * finalScale)
        val finalDy = padY + (availH - rawBounds.height() * finalScale) / 2f -
                (rawBounds.top * finalScale)
        val m = android.graphics.Matrix().apply {
            postScale(finalScale, finalScale)
            postTranslate(finalDx, finalDy)
        }

        /* ------ draw faint full guide (all strokes) ------ */
        val guidePath = android.graphics.Path().apply {
            parsedPaths.forEach { addPath(it) }
            transform(m)
        }
        val strokeWidthPx = DrawingConfig
            .getStrokeWidth(min(size.width, size.height))

        drawPath(
            path = guidePath.asComposePath(),
            color = staticGuideColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        /* ------ draw animated or final segment ------ */
        // Iterate over strokes already “completed”
        for (i in 0 until currentStrokeIdx) {
            scaledStroke.reset()
            scaledStroke.set(parsedPaths[i])
            scaledStroke.transform(m)

            drawPath(
                path = scaledStroke.asComposePath(),
                color = finalStaticSegmentColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Animate current stroke unless everything is done or user took over
        if (currentStrokeIdx < strokesCount) {
            scaledStroke.reset()
            scaledStroke.set(parsedPaths[currentStrokeIdx])
            scaledStroke.transform(m)

            pm.setPath(scaledStroke, false)
            animSegment.reset()
            val segLen = pm.length * intraStrokeProgress
            pm.getSegment(0f, segLen, animSegment, true)

            val segmentColor = if (userHasStartedTracing)
                finalStaticSegmentColor else animatedSegmentColor

            drawPath(
                path = animSegment.asComposePath(),
                color = segmentColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}



/* ---------- Cheap similarity check ---------- */
private fun pathSimilarity(user: ComposePath, target: ComposePath): Float {
    val size = 64                 // tiny bitmap for speed
    return toMaskDiff(user.toMask(), target.toMask())
}

private fun toMaskDiff(a: Bitmap, b: Bitmap): Float {
    var matches = 0
    val total = a.width * a.height
    for (y in 0 until a.height) {
        for (x in 0 until a.width) {
            val same = (a[x, y] != 0) == (b[x, y] != 0)
            if (same) matches++
        }
    }
    return matches / total.toFloat()          // 0‒1
}

fun isCloseEnough(user: ComposePath, target: ComposePath) =
    pathSimilarity(user, target) >= 0.8f

private const val MASK_SIZE = 64
private const val PADDING = 0.05f          // 5 %

private fun ComposePath.normalised(): Path {
    val android = this.asAndroidPath()
    val bounds = RectF().also { android.computeBounds(it, true) }

    // leave a 5 % gutter
    val avail = (1f - PADDING * 2)
    val scale = avail * MASK_SIZE / max(bounds.width(), bounds.height())

    val dx = (MASK_SIZE - bounds.width() * scale) / 2f - bounds.left * scale
    val dy = (MASK_SIZE - bounds.height() * scale) / 2f - bounds.top * scale

    return Path().apply {
        transform(Matrix().apply {
            scale(scale, scale)
            translate(dx, dy)
//            postScale(scale, scale)
//            postTranslate(dx, dy)
        })
    }
}

//private fun Path.toMask(): Bitmap {
//    val bmp = Bitmap.createBitmap(MASK_SIZE, MASK_SIZE, Bitmap.Config.ALPHA_8)
//    val c   = Canvas(bmp)
//    val p   = Paint().apply {
//        color = Color.WHITE
//        style = Paint.Style.STROKE
//        strokeWidth = 6f
//        isAntiAlias = true
//        strokeCap = Paint.Cap.ROUND
//        strokeJoin = Paint.Join.ROUND
//    }
//    c.drawPath(this, p)
//    return bmp
//}
fun ComposePath.toMask(): Bitmap {
    val bmp = createBitmap(MASK_SIZE, MASK_SIZE, Bitmap.Config.ALPHA_8)
    val c = AndroidCanvas(bmp)
    c.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    val p = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    c.drawPath(this@toMask.toAndroid(), p)
    return bmp
}

private fun bitmapSimilarity(a: Bitmap, b: Bitmap): Float {
    var same = 0
    val total = MASK_SIZE * MASK_SIZE
    for (y in 0 until MASK_SIZE) {
        for (x in 0 until MASK_SIZE) {
            val hitA = a.getPixel(x, y) != 0
            val hitB = b.getPixel(x, y) != 0
            if (hitA == hitB) same++
        }
    }
    return same / total.toFloat()      // 1 == identical
}

/** Public entry – call from onStrokeFinished */
fun pathsAreClose(user: ComposePath, perfect: ComposePath, threshold: Float = 0.7f): Boolean {
    val a = user.normalised().toMask()
    val b = perfect.normalised().toMask()
    return bitmapSimilarity(a, b) >= threshold
}

@Preview(showBackground = true)
@Composable
private fun FirstPathPreview() {
    FirstPath()
}