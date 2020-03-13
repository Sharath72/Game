package com.adeliosys.keybout.model

/**
 * A player score during a game.
 * Some attributes are for the current round, while others are for the whole game.
 */
class Score(val userName: String) {

    // Number of words caught in a round
    var points = 0

    // Timestamp of the latest word won
    private var latestWordTimestamp = getTimestamp()

    // Number of words/min for the round, used in capture games only
    var speed = 0.0f

    // Awards for the current round, is a bitwise "or" of all the awards
    var awards = 0

    // Number of rounds won
    var victories = 0

    // Timestamp of the latest victory
    var latestVictoryTimestamp = getTimestamp()

    // Best number of words/min so far in the game
    var bestSpeed = 0.0f

    // Rank position, 0 means not ranked (used only in race games)
    var topRank = 0

    // Top speed, if ranked (used only in race games)
    var topSpeed = 0.0f

    constructor(userName: String, speed: Float): this(userName) {
        this.speed = speed
    }

    constructor(rank: Int, speed: Float): this("") {
        topRank = rank
        topSpeed = speed
    }

    fun resetPoints() {
        points = 0
        awards = 0
    }

    fun incrementPoints() {
        points++
        latestWordTimestamp = getTimestamp()
    }

    fun addAward(award: Int) {
        awards = awards or award
    }

    fun incrementVictories() {
        victories++
        latestVictoryTimestamp = getTimestamp()
    }

    /**
     * Update words/min and durations once all words are caught.
     */
    fun updateSpeeds(roundStart: Long) {
        speed = if (points > 0) 60000.0f * points / (latestWordTimestamp - roundStart) else 0.0f

        if (bestSpeed <= 0 || bestSpeed < speed) {
            bestSpeed = speed
        }
    }

    /**
     * Update the top rank and top speed.
     */
    fun updateTops(rank: Int, speed: Float) {
        topRank = rank;
        topSpeed = speed;
    }

    private fun getTimestamp() = System.currentTimeMillis()
}

/**
 * DTO used for notifications sent to the frontend.
 */
class ScoreDto(val userName: String, val points: Int, val speed: Float, val awards: Int?)

/**
 * Score of a ranked player.
 */
data class TopScore(val userName: String = "-", val speed: Float = 0.0f)

/**
 * DTO used to send all the top scores for a given category.
 */
class TopScoresDto(val style: GameStyle, val language: Language, val difficulty: Difficulty, scores: List<TopScore>) {

    // Duplicate the lists to prevent concurrency issues
    val scores = scores.toList()
}
