package com.example.habitflowai.data.model

data class GenerateGoalsRequest(
    val userId: String,
    val dayOfWeek: Int
)

data class GoalTask(
    val description: String,
    val points: Int
)

data class GenerateGoalsResponse(
    val isValid: Boolean,
    val errorReason: String?,
    val personaType: String?,
    val motivationalMessage: String?,
    val coreGoals: List<GoalTask>?,
    val dailyVariations: List<GoalTask>?
)

fun GenerateGoalsResponse.toGoalPairList(): List<Pair<String, Int>> {
    val core = this.coreGoals?.map { it.description to it.points } ?: emptyList()
    val variations = this.dailyVariations?.map { it.description to it.points } ?: emptyList()
    return core + variations
}
