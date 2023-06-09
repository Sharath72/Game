package com.adeliosys.keybout.api

import com.adeliosys.keybout.model.StatsDto
import com.adeliosys.keybout.service.StatsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class StatsController(private val statsService: StatsService) {

    /**
     * Return the application stats.
     */
    @GetMapping("/stats")
    fun getStats(): StatsDto = statsService.getStats()

    /**
     * Force an immediate save of the application stats.
     */
    @GetMapping("/stats/save")
    fun saveStats() {
        statsService.saveStats()
    }
}
