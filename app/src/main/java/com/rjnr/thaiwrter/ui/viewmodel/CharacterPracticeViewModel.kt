package com.rjnr.thaiwrter.ui.viewmodel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rjnr.thaiwrter.data.models.THAI_CHARACTERS
import com.rjnr.thaiwrter.data.models.ThaiCharacter
import com.rjnr.thaiwrter.data.repository.ThaiLanguageRepository
import com.rjnr.thaiwrter.utils.MLStrokeValidator
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


// Add to your ViewModel or a separate file
enum class PracticeStep {
    INITIAL,                    // Initial state, character loaded
    GUIDE_AND_TRACE,            // NEW: Combines guide animation and user tracing
    MORPHING_TRACE_TO_CORRECT,  // User's trace morphs to green correct version
    AWAITING_BLANK_SLATE,       // Green character shown, tap to proceed to blank slate
    USER_WRITING_BLANK,         // Blank canvas, user draws from memory
    MORPHING_WRITE_TO_CORRECT,  // User's write morphs to green correct version
    AWAITING_NEXT_CHARACTER     // Green character shown, tap for next character/lesson complete
}

// In CharacterPracticeViewModel
class CharacterPracticeViewModel(
    private val repository: ThaiLanguageRepository, // Assuming you have this
    private val mlStrokeValidator: MLStrokeValidator
) : ViewModel() {
    // ...
    private val allCharacters = THAI_CHARACTERS
    private val _currentCharacter = MutableStateFlow<ThaiCharacter?>(null)
    val currentCharacter = _currentCharacter.asStateFlow()

    private val _practiceStep = MutableStateFlow(PracticeStep.INITIAL)
    val practiceStep: StateFlow<PracticeStep> = _practiceStep.asStateFlow()

    private val _guideAnimationProgress = Animatable(0f) // For one-shot guide animation
    val guideAnimationProgress: Float get() = _guideAnimationProgress.value

    private val _userDrawnPath = MutableStateFlow<Path?>(null)
    val userDrawnPath: StateFlow<Path?> = _userDrawnPath.asStateFlow()

    // To trigger clearing the DrawingCanvas from the ViewModel
    private val _clearCanvasSignal = MutableSharedFlow<Unit>(replay = 0)
    val clearCanvasSignal: SharedFlow<Unit> = _clearCanvasSignal.asSharedFlow()

    // NEW: Tracks if user interacted with current guide
    private val _userHasStartedTracing = MutableStateFlow(false)
    val userHasStartedTracing: StateFlow<Boolean> = _userHasStartedTracing.asStateFlow()


    init {
        // Load the first character when ViewModel is created
        initialLoadAndPrepareCharacter()
    }

    private fun setupForNewCharacter() {
        _userDrawnPath.value = null
        _userHasStartedTracing.value = false
        viewModelScope.launch { _guideAnimationProgress.snapTo(0f) }
        _practiceStep.value = PracticeStep.GUIDE_AND_TRACE // Go directly to combined step
    }

    private fun initialLoadAndPrepareCharacter() {
        _currentCharacter.value = allCharacters.randomOrNull()
        _currentCharacter.value?.let {
            setupForNewCharacter()
        }
    }

    fun loadNextCharacterAndPrepareAnimation() {
        _currentCharacter.value = allCharacters.randomOrNull()
        _currentCharacter.value?.let {
            setupForNewCharacter()
        }
    }


    // This will be called by the Composable

    // This will be called by the Composable's LaunchedEffect
    suspend fun executeGuideAnimationLoop() {
        // Ensure we are in the correct step and character is loaded
        if (_practiceStep.value != PracticeStep.GUIDE_AND_TRACE || currentCharacter.value == null) {
            return
        }

        // Loop the guide animation until user starts tracing or the step changes
        while (isActive && // Check if the coroutine is still active
            _practiceStep.value == PracticeStep.GUIDE_AND_TRACE &&
            !_userHasStartedTracing.value && // Stop looping if user starts tracing
            currentCharacter.value != null
        ) {

            _guideAnimationProgress.snapTo(0f) // Reset for loop
            try {
                _guideAnimationProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1500,
                        easing = LinearEasing
                    ) // Your animation speed
                )
                // If animation completes and user hasn't started tracing and still in GUIDE_AND_TRACE step
                if (isActive && !_userHasStartedTracing.value && _practiceStep.value == PracticeStep.GUIDE_AND_TRACE) {
                    delay(500) // Pause before restarting loop
                }
            } catch (e: CancellationException) {
                // Animation was cancelled (e.g., scope closed or user interacted via _userHasStartedTracing)
                break // Exit loop on cancellation
            }
        }

        // If loop broke because user started tracing, ensure progress is at 1f for static guide
        if (isActive && _userHasStartedTracing.value && _practiceStep.value == PracticeStep.GUIDE_AND_TRACE) {
            _guideAnimationProgress.snapTo(1f)
        }
    }

    // Called from onDragStart in DrawingCanvas
    fun userStartedTracing() {
        if (_practiceStep.value == PracticeStep.GUIDE_AND_TRACE) {
            _userHasStartedTracing.value = true
            // The animation loop in executeGuideAnimationLoop will see this flag and stop.
            // We might want to immediately snap the animation to its end if it was mid-animation.
            viewModelScope.launch {
                _guideAnimationProgress.stop() // Stop any ongoing animation
                _guideAnimationProgress.snapTo(1f) // Ensure the animated part is fully "drawn" if we want to show it statically
            }
        }
    }

    fun onUserStrokeFinished(path: Path) {
        _userDrawnPath.value = path
        viewModelScope.launch { _clearCanvasSignal.emit(Unit) }

        when (_practiceStep.value) {
            PracticeStep.GUIDE_AND_TRACE -> {
                _userHasStartedTracing.value = true // Ensure this is set
                _practiceStep.value = PracticeStep.MORPHING_TRACE_TO_CORRECT
            }

            PracticeStep.USER_WRITING_BLANK -> {
                _practiceStep.value = PracticeStep.MORPHING_WRITE_TO_CORRECT
            }

            else -> {}
        }
    }

    fun onMorphAnimationFinished() {
        when (_practiceStep.value) {
            PracticeStep.MORPHING_TRACE_TO_CORRECT -> {
                _practiceStep.value = PracticeStep.AWAITING_BLANK_SLATE
            }

            PracticeStep.MORPHING_WRITE_TO_CORRECT -> {
                _practiceStep.value = PracticeStep.AWAITING_NEXT_CHARACTER
            }

            else -> {}
        }
    }

    // In advanceToNextStep, when going from AWAITING_NEXT_CHARACTER to new char
    fun advanceToNextStep() {
        viewModelScope.launch {
            when (_practiceStep.value) {
                PracticeStep.AWAITING_BLANK_SLATE -> {
                    _userDrawnPath.value = null
                    _clearCanvasSignal.emit(Unit)
                    _practiceStep.value = PracticeStep.USER_WRITING_BLANK
                }
                PracticeStep.AWAITING_NEXT_CHARACTER -> {
                    _userDrawnPath.value = null
                    _clearCanvasSignal.emit(Unit)
                    loadNextCharacterAndPrepareAnimation()
                }
                else -> {}
            }
        }
    }

    // Your existing clearCanvas, checkAnswer, nextCharacter can be refactored or removed
    // if their logic is now handled by practiceStep transitions.
    // The `clearCanvas()` from your code is good for a manual clear button.
    // The `_shouldClearCanvas` can be replaced by `_clearCanvasSignal`.

    fun manualClear() { // For the "Clear" button
        viewModelScope.launch {
            _userDrawnPath.value = null
            _clearCanvasSignal.emit(Unit)
            // Optionally reset to current step's beginning if needed
            // e.g., if (_practiceStep.value == PracticeStep.USER_TRACING_ON_GUIDE) { /* do nothing more */ }
        }
    }

    // When "Next Char" button is pressed
    fun requestNextCharacter() {
        viewModelScope.launch {
            _userDrawnPath.value = null
            _clearCanvasSignal.emit(Unit)
            loadNextCharacterAndPrepareAnimation()
        }
    }

    // ... rest of your ViewModel ...
    // Ensure ThaiCharacter has svgPathData: String
    // data class ThaiCharacter(val id: String, val character: String, val pronunciation: String, val svgPathData: String)
}
//class CharacterPracticeViewModel(
//    private val repository: ThaiLanguageRepository,
//    private val mlStrokeValidator: MLStrokeValidator
//) : ViewModel()
//{
//    private val allCharacters = MLStrokeValidator.CHARACTER_MAP.map { (index, char) ->
//        ThaiCharacter(
//            id = index,
//            character = char,
//            pronunciation = MLStrokeValidator.getPronunciation(index)
//        )
//    }
//    private val _currentCharacter = MutableStateFlow<ThaiCharacter?>(null)
//    val currentCharacter = _currentCharacter.asStateFlow()
//
//    private val _shouldClearCanvas = MutableStateFlow(false)
//    val shouldClearCanvas = _shouldClearCanvas.asStateFlow()
//
//    private val _prediction = MutableStateFlow<CharacterPrediction?>(null)
//    val prediction = _prediction.asStateFlow()
//    //
//    // Add these properties to CharacterPracticeViewModel
//    private val _drawingPaths = MutableStateFlow<List<Path>>(emptyList())
//    val drawingPaths = _drawingPaths.asStateFlow()
//
//    private val _pathColor = MutableStateFlow(Color.Black)
//    val pathColor = _pathColor.asStateFlow()
//
//    private val _isCorrect = MutableStateFlow(false)
//    val isCorrect = _isCorrect.asStateFlow()
//
//    private val _instructionText = MutableStateFlow("trace the character")
//    val instructionText = _instructionText.asStateFlow()
//
//    private val _showGuide = MutableStateFlow(true)
//    val showGuide = _showGuide.asStateFlow()
//
//    init {
//        loadNextCharacter()
//    }
//
//    fun onDrawingComplete(points: List<Point>, width: Int, height: Int) {
//        viewModelScope.launch {
//            val prediction =
//                mlStrokeValidator.predictCharacter(points, width.toFloat(), height.toFloat())
//            prediction?.let {
//                Log.d("Prediction", "Character: ${it.character}")
//                Log.d("Prediction", "Confidence: ${it.confidence * 100}%")
//                Log.d("Prediction", "Alternatives: ${it.alternativeCharacters}")
//            }
//            _prediction.value = prediction
//        }
//    }
//
//    fun clearCanvas() {
//        _prediction.value = null
//        _shouldClearCanvas.value = true
//        // Reset the flag after a brief delay
//        viewModelScope.launch {
//            delay(100)
//            _shouldClearCanvas.value = false
//        }
//    }
//
////    fun checkAnswer() {
////        viewModelScope.launch {
////            // Compare prediction with current character
////            prediction.value?.let { pred ->
////                if (pred.confidence > 0.7f) {  // Adjust threshold as needed
////                    updateProgress()
////                }
////            }
////        }
////    }
//fun checkAnswer() {
//    viewModelScope.launch {
//        // Compare prediction with current character
//        prediction.value?.let { pred ->
//            val isCorrectPrediction = currentCharacter.value?.id == pred.characterIndex &&
//                    pred.confidence > 0.7f
//
//            if (isCorrectPrediction) {
//                // Animate to green color
//                _pathColor.value = Color.Green
//                _isCorrect.value = true
//                _instructionText.value = "tap to advance"
//                updateProgress()
//            } else {
//                // Show feedback
//                _isCorrect.value = false
//            }
//        }
//    }
//}
//    // New method to handle animated correction
//    fun animateCorrection() {
//        viewModelScope.launch {
//            // This would morph the path to the correct shape
//            // For a placeholder effect:
//            delay(500) // Simulate morph animation time
//            _pathColor.value = Color.Green
//            _isCorrect.value = true
//            _instructionText.value = "tap to advance"
//        }
//    }
//
//    // Update next character to progress through modes
////    fun nextCharacter() {
////        clearCanvas()
////        if (_isCorrect.value) {
////            // If completed correctly, load next character
////            _isCorrect.value = false
////            _pathColor.value = Color.Black
////
////            // First time show with guide, second time without
////            _showGuide.value = !_showGuide.value
////            _instructionText.value = if (_showGuide.value) "trace the character" else "write the character"
////
////            loadNextCharacter()
////        } else {
////            // If still practicing, just clear the canvas
////            _instructionText.value = if (_showGuide.value) "trace the character" else "write the character"
////        }
////    }
//
//    fun nextCharacter() {
//        clearCanvas()
//        loadNextCharacter()
//    }
//
//    private fun loadNextCharacter() {
//        _currentCharacter.value = allCharacters.random()
//        clearCanvas()
//    }
//    fun skipCharacter() {
//        loadNextCharacter()
//    }
//    private suspend fun updateProgress() {
//        currentCharacter.value?.let { char ->
//            repository.updateUserProgress(
//                UserProgress(
//                    characterId = char.id,
//                    lastReviewed = System.currentTimeMillis(),
//                    nextReviewDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
//                    correctCount = 1,
//                    incorrectCount = 0,
//                    streak = 1,
//                    easeFactor = 2.5f
//                )
//            )
//        }
//    }
//}



