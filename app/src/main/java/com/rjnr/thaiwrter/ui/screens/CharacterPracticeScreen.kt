package com.rjnr.thaiwrter.ui.screens

import MorphOverlay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rjnr.thaiwrter.ui.drawing.DrawingConfig
import com.rjnr.thaiwrter.ui.drawing.OptimizedDrawingCanvas
import com.rjnr.thaiwrter.ui.drawing.StrokeGuide
import com.rjnr.thaiwrter.ui.viewmodel.CharacterPracticeViewModel
import com.rjnr.thaiwrter.ui.viewmodel.PracticeStep
import org.koin.androidx.compose.koinViewModel

//@Composable
//fun CharacterPracticeScreen(
//    viewModel: CharacterPracticeViewModel = koinViewModel(),
//    navController: NavController
//) {
//    //
//    val prediction by viewModel.prediction.collectAsState()
//    val currentCharacter by viewModel.currentCharacter.collectAsState()
//    val shouldClearCanvas by viewModel.shouldClearCanvas.collectAsState()
//    val pathColor by viewModel.pathColor.collectAsState()
//    val isCorrect by viewModel.isCorrect.collectAsState()
//    val instructionText by viewModel.instructionText.collectAsState()
//
//    // System metrics for proper scaling
//    val metrics = LocalDensity.current.density
//    val strokePath = remember(testStroke.pathData) {
//        androidx.compose.ui.graphics.vector.PathParser()    // UI-graphics 1.7+
//            .parsePathString(testStroke.pathData)
//            .toPath()
//    }
//    val perfectStroke =
//        "M14 127C11.6 38.2 7 55 28 33L1 15C1 15 26.9941 0.0325775 45 1C60.4269 1.82886 82 5.00001 82 15C82 25 82 127 82 127"
//
//    val perfectPath = remember(perfectStroke) {
//        PathParser()
//            .parsePathString(perfectStroke)
//            .toPath()
//    }
//    /* --- state --- */
//    var showGuide by remember { mutableStateOf(true) }
//    var showMorph by remember { mutableStateOf(false) }
//    var userPathForMorph by remember { mutableStateOf<Path?>(null) }
//
//
//    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(16.dp)
//                .background(MaterialTheme.colorScheme.background)
//
//        ) {
//            // Character display
//            Text(
//                text = currentCharacter?.character ?: "",
//                style = MaterialTheme.typography.displayLarge,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//
//            // Pronunciation guide
//            Text(
//                text = currentCharacter?.pronunciation ?: "",
//                style = MaterialTheme.typography.bodyLarge,
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//
//            // Drawing canvas
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .aspectRatio(1f)
//                    .padding(vertical = 16.dp)
//            ) {
//                Card(
//                    modifier = Modifier.fillMaxSize(),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .aspectRatio(1f)
//                            .padding(16.dp)
//                    ) {
//                        // Main drawing canvas
//                        if (showGuide) {
//                            StrokeGuide(
//                                svgPathData = perfectStroke,
//                                marginRatio = 0.15f,            // shrink the guide
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                        DrawingCanvas(
//                            modifier = Modifier.fillMaxSize(),
//                            shouldClear = shouldClearCanvas,
//                            onStrokeFinished = { userPath ->
//                                if (pathsAreClose(userPath, perfectPath)) {
//                                    userPathForMorph = userPath
//                                    showGuide = false            // ① hide purple loop
//                                    showMorph = true
//                                }
//                            }
//                        )
//                        // ③ Morph overlay – only when showMorph == true
//                        if (showMorph && userPathForMorph != null) {
//                            MorphOverlay(
//                                userPath = userPathForMorph!!,
//                                perfectSvg = perfectStroke,
//                                onFinished = {
//                                    showMorph = false            // reset
//                                    viewModel::clearCanvas
//                                    showGuide = true             // ready for next try
//                                },
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                    }
//                }
//            }
//            // Prediction results
//            prediction?.let { pred ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp)
//                ) {
//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Text(
//                            text = "Prediction: ${pred.character}",
//                            style = MaterialTheme.typography.headlineMedium
//                        )
//                        Text(
//                            text = "Confidence: ${(pred.confidence * 100).toInt()}%",
//                            style = MaterialTheme.typography.bodyLarge
//                        )
//
//                        // Pronunciation
//                        Text(
//                            text = pred.pronunciation,
//                            style = MaterialTheme.typography.bodyLarge,
//                            modifier = Modifier.padding(vertical = 8.dp)
//                        )
//
//                        // Show top alternative predictions
////                        pred.alternativePredictions.take(3).forEach { (index, confidence) ->
////                            Text(
////                                text = "Alternative: ${index} (${(confidence * 100).toInt()}%)",
////                                style = MaterialTheme.typography.bodyMedium
////                            )
////                        }
//                        if (pred.alternativeCharacters.isNotEmpty()) {
//                            Text(
//                                text = "Alternative predictions:",
//                                style = MaterialTheme.typography.titleMedium,
//                                modifier = Modifier.padding(top = 8.dp)
//                            )
//                            pred.alternativeCharacters.take(3).forEach { (char, conf) ->
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(vertical = 4.dp),
//                                    horizontalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    Text(char, style = MaterialTheme.typography.bodyLarge)
//                                    Text(
//                                        "${(conf * 100).toInt()}%",
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        color = MaterialTheme.colorScheme.secondary
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 16.dp),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(onClick = viewModel::clearCanvas) {
//                    Text("Clear")
//                }
//                Button(onClick = viewModel::checkAnswer) {
//                    Text("Check")
//                }
//                Button(onClick = viewModel::nextCharacter) {
//                    Text("Next")
//                }
//            }
//        }
//    }
//}
@Composable
fun CharacterPracticeScreen(
    viewModel: CharacterPracticeViewModel = koinViewModel(), // or viewModel()
    navController: NavController // If used for back navigation
) {
    val currentCharacter by viewModel.currentCharacter.collectAsState()
    val practiceStep by viewModel.practiceStep.collectAsState()
    val guideAnimationProgress = viewModel.guideAnimationProgress // Direct float
    val userHasStartedTracing by viewModel.userHasStartedTracing.collectAsState() // Collect new state
    val userDrawnPath by viewModel.userDrawnPath.collectAsState()

    // For the "tap to advance" functionality
    val interactionSource = remember { MutableInteractionSource() }
    // Enabled only when user is supposed to draw and not during morphing/guide animation
    val drawingEnabled = practiceStep == PracticeStep.GUIDE_AND_TRACE ||
            practiceStep == PracticeStep.USER_WRITING_BLANK

    // Trigger the guide animation loop
    LaunchedEffect(practiceStep, currentCharacter, userHasStartedTracing) {
        // Add userHasStartedTracing key
        if (practiceStep == PracticeStep.GUIDE_AND_TRACE && currentCharacter != null && !userHasStartedTracing) {
            viewModel.executeGuideAnimationLoop()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background) // Use theme colors
                .clickable( // Global click for "tap to advance"
                    interactionSource = interactionSource,
                    indication = null, // No ripple for the whole screen
                    enabled = practiceStep == PracticeStep.AWAITING_BLANK_SLATE ||
                            practiceStep == PracticeStep.AWAITING_NEXT_CHARACTER
                ) {
                    viewModel.advanceToNextStep()
                }
        ) {
            // Top Bar (Character Info, Progress - Placeholder)
            currentCharacter?.let { char ->
                Text(
                    text = char.character,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = char.pronunciation,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            Spacer(Modifier.height(8.dp)) // Reduced spacer

            // Drawing Area
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp) // Reduced padding
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    ) // Theme color
            ) {
                // Grid lines
                Canvas(Modifier.fillMaxSize()) {
                    val thirdHeight = size.height / 3
                    val thirdWidth = size.width / 3
                    drawLine(
                        Color.Gray,
                        Offset(0f, thirdHeight),
                        Offset(size.width, thirdHeight),
                        alpha = 0.5f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(0f, 2 * thirdHeight),
                        Offset(size.width, 2 * thirdHeight),
                        alpha = 0.5f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(thirdWidth, 0f),
                        Offset(thirdWidth, size.height),
                        alpha = 0.5f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(2 * thirdWidth, 0f),
                        Offset(2 * thirdWidth, size.height),
                        alpha = 0.5f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(0f, size.height / 2),
                        Offset(size.width, size.height / 2),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        alpha = 0.5f
                    )
                }

                // Stroke Guide
//                if (practiceStep == PracticeStep.ANIMATING_GUIDE || practiceStep == PracticeStep.USER_TRACING_ON_GUIDE) {
//                    StrokeGuide(
//                        svgPathData = currentCharacter?.svgPathData,
//                        practiceStep = practiceStep, // Pass the practice step
//                        animationProgress = guideAnimationProgress,
//                        userHasStartedTracing = userHasStartedTracing, // Pass this new state
//                        marginRatio = 0.25f, // Adjusted for issue 4
//                        modifier = Modifier.fillMaxSize()
//                    )
//                }
                if (practiceStep == PracticeStep.GUIDE_AND_TRACE && currentCharacter != null) {
                    currentCharacter?.let { char ->
                        StrokeGuide(
                            strokes = char.strokes,
                            animationProgress = guideAnimationProgress,
                            userHasStartedTracing = userHasStartedTracing,
                            marginToApply = 0.2f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // User Drawing Canvas
                OptimizedDrawingCanvas(
                    modifier = Modifier.fillMaxSize(),
                    clearSignal = viewModel.clearCanvasSignal,
                    onStrokeFinished = viewModel::onUserStrokeFinished,
                    onDragStartAction = viewModel::userStartedTracing,
                    enabled = drawingEnabled,
                    strokeColor = Color.Black,
                    strokeWidthRatio = DrawingConfig.DEFAULT_STROKE_WIDTH_RATIO
                )

                // Morph Overlay
                // Morph Overlay or Static Green Correct Character
                if ((practiceStep == PracticeStep.MORPHING_TRACE_TO_CORRECT || practiceStep == PracticeStep.MORPHING_WRITE_TO_CORRECT) && userDrawnPath != null) {
                    MorphOverlay(
                        userPath = userDrawnPath!!,
                        perfectStrokes = currentCharacter?.strokes ?: listOf(),
                        onFinished = viewModel::onMorphAnimationFinished,
                        marginToApply = 0.2f, // Consistent margin
                        modifier = Modifier.fillMaxSize()
                    )
                } else if ((practiceStep == PracticeStep.AWAITING_BLANK_SLATE || practiceStep == PracticeStep.AWAITING_NEXT_CHARACTER) && currentCharacter?.strokes != null) {
                    // Re-use StrokeGuide to draw the static perfect character in green
                    currentCharacter?.let { char ->
                        StrokeGuide(
                            strokes = char.strokes,
                            animationProgress = 1f, // Fully drawn
                            userHasStartedTracing = true, // Treat as if user interacted for static display
                            staticGuideColor = Color.Green.copy(alpha = 0.0f), // Make underlying static guide invisible
                            animatedSegmentColor = Color.Green, // This will be the color used for the "segment"
                            finalStaticSegmentColor = Color.Green, // Ensure it's green
                            marginToApply = 0.2f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }


            // Instruction Text
            Text(
                text = when (practiceStep) {
                    PracticeStep.INITIAL -> "Loading..."
                    PracticeStep.GUIDE_AND_TRACE -> if (!userHasStartedTracing) "Trace the character" else "Finish tracing"
                    PracticeStep.MORPHING_TRACE_TO_CORRECT, PracticeStep.MORPHING_WRITE_TO_CORRECT -> ""
                    PracticeStep.AWAITING_BLANK_SLATE -> "Tap to try from memory"
                    PracticeStep.USER_WRITING_BLANK -> "Write the character"
                    PracticeStep.AWAITING_NEXT_CHARACTER -> "Tap for next"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White, // White text for blue background
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp) // More space for instruction
                    .clickable( // Make "tap to advance" apply here too
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = practiceStep == PracticeStep.AWAITING_BLANK_SLATE ||
                                practiceStep == PracticeStep.AWAITING_NEXT_CHARACTER
                    ) {
                        viewModel.advanceToNextStep()
                    }
            )

            Spacer(Modifier.weight(1f))

            // ML Prediction display (your existing code, can be kept)
            // viewModel.prediction.collectAsState().value?.let { pred -> ... }


            // Bottom Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = viewModel::manualClear, enabled = drawingEnabled) { Text("Clear") }
                // "Check" button might be redundant if morphing happens automatically
                // Button(onClick = viewModel::checkAnswer) { Text("Check") }
                Button(onClick = viewModel::requestNextCharacter) { Text("Next Char") } // Or "Skip"
            }

            // Icons (as in video) - Placeholder for functionality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Practice Mode",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Pronunciation",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Guide (if applicable)",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Practice Complete Dialog (from video) - This would be triggered from ViewModel
        // when practiceStep becomes, e.g., PracticeStep.LESSON_COMPLETE
        if (practiceStep == PracticeStep.AWAITING_NEXT_CHARACTER /* && isLastCharacterInLesson */) {
            // Show your "Practice Complete" dialog here with stars, Retry, Continue
            // For simplicity, this is handled by "Tap for next" now.
            // You could navigate to a summary screen or show an AlertDialog.
        }
    }
}
