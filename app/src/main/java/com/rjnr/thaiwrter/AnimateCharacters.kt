import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rjnr.thaiwrter.ui.drawing.DrawingConfig
import kotlinx.coroutines.delay
import kotlin.math.min


@Composable
fun MorphOverlay(
    userPath: Path,
    perfectStrokes: List<String>,
    durationMs: Int = 800,   // Slightly longer for better visual
    modifier: Modifier = Modifier,
    marginToApply: Float = 0.25f, // Consistent margin
    onFinished: () -> Unit
) {
    if (perfectStrokes.isEmpty()) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    /* -------- fuse strokes -------- */
    val targetPath = remember(perfectStrokes) {
        android.graphics.Path().apply {
            perfectStrokes.forEach { d ->
                addPath(
                    androidx.compose.ui.graphics.vector.PathParser()
                        .parsePathString(d)
                        .toPath()
                        .asAndroidPath()
                )
            }
        }
    }
    // ... rest of your MorphOverlay logic is good ...
    // Make sure the scaling logic (m matrix) inside Canvas is robust

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
        delay(200) // Small delay to let user see the green before it disappears or advances
        onFinished()
    }
    val r = remember(targetPath) {
        android.graphics.RectF().also { targetPath.computeBounds(it, true) }
    }


    Canvas(modifier) {
        val strokePx = DrawingConfig.getStrokeWidth(
            min(
                size.width,
                size.height
            )
        ) // Use config instead of hardcoded


        // Consistent Scaling with StrokeGuide
        val r = android.graphics.RectF().also { targetPath.computeBounds(it, true) }
        val padX = size.width * marginToApply
        val padY = size.height * marginToApply
        val availW = size.width - padX * 2
        val availH = size.height - padY * 2

        if (r.width() <= 0 || r.height() <= 0 || availW <= 0 || availH <= 0) {
            // Handle invalid bounds or available space, perhaps by calling onFinished early
            onFinished()
            return@Canvas
        }


        val scaleFactor = 0.9f // Consistent additional factor
        val finalScale = min(availW / r.width(), availH / r.height()) * scaleFactor


        val scaledWidth = r.width() * finalScale
        val scaledHeight = r.height() * finalScale
        val finalDx = padX + (availW - scaledWidth) / 2f - (r.left * finalScale)
        val finalDy = padY + (availH - scaledHeight) / 2f - (r.top * finalScale)

        val m =
            android.graphics.Matrix().apply {
                postScale(finalScale, finalScale)
                postTranslate(finalDx, finalDy)
            }

        val perfectCanvasPath =
            android.graphics.Path().apply {
                set(targetPath); transform(m)
            }


        // User path (fading out)
        drawPath(
            path = userPath,
            color = Color.Black,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = 1f - progress.value
        )
        // Perfect path (fading in, green)
        drawPath(
            path = perfectCanvasPath.asComposePath(),
            color = Color.Green, // Or your MaterialTheme.colorScheme.tertiary
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = progress.value
        )
    }
}


