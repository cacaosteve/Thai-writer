package com.rjnr.thaiwrter.data.models

import com.rjnr.thaiwrter.data.local.entities.ThaiCharacterEntity
import com.rjnr.thaiwrter.data.local.entities.UserProgressEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun ThaiCharacterEntity.toDomain(): ThaiCharacter {
    return ThaiCharacter(
        id = id,
        character = character,
        pronunciation = pronunciation,
        strokes = emptyList(),
        difficulty = difficulty,
        category = category
    )
}

fun ThaiCharacter.toEntity(): ThaiCharacterEntity {
    return ThaiCharacterEntity(
        id = id,
        character = character,
        pronunciation = pronunciation,
        strokeData = "",
        difficulty = difficulty,
        category = category
    )
}

fun UserProgressEntity.toDomain(): UserProgress {
    return UserProgress(
        characterId = characterId,
        lastReviewed = lastReviewed,
        nextReviewDate = nextReviewDate,
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        streak = streak,
        easeFactor = easeFactor
    )
}

fun UserProgress.toEntity(): UserProgressEntity {
    return UserProgressEntity(
        characterId = characterId,
        lastReviewed = lastReviewed,
        nextReviewDate = nextReviewDate,
        correctCount = correctCount,
        incorrectCount = incorrectCount,
        streak = streak,
        easeFactor = easeFactor
    )
}