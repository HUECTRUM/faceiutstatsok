package com.nothing.service.matchstats

import com.nothing.annotations.springcomponents.InjectableService

@InjectableService
class StatsCollector {
    def collectStatsByMap(List t1Info, List t2Info) {
        def statsResponse = [t1Info, t2Info].collect { List<?> info -> info[1] }
        def teamPlayerData = [t1Info, t2Info].collect { List<?> info -> info[0] }

        return statsResponse
                .withIndex()
                .collect { teamStats, index -> getSegmentsForFullTeam(teamStats, teamPlayerData[index]) }
                .collect { it.groupBy({ Map<String, ?> segment -> segment.label }) }
    }

    def getSegmentsForFullTeam(List<Map<String, ?>> teamStats, List<Map<String, ?>> teamPlayerData) {
        // Verify we have exactly 5 players (full team)
        if (teamPlayerData.size() != 5) {
            return []
        }
        
        // Check if all players have sufficient match data
        // This is a heuristic to ensure we're dealing with active players who likely play together
        def allPlayersHaveGames = teamStats.every { playerStats ->
            def segments = playerStats.segments?.findAll { it.type == 'Map' && it.mode == '5v5' }
            return segments && !segments.isEmpty() && segments.any { it.stats?.Matches as Integer > 5 }
        }
        
        if (!allPlayersHaveGames) {
            return [] // Not enough data to determine team cohesion
        }
        
        // Apply additional filtering: only include segments where players have similar activity levels
        // This helps ensure we're aggregating stats from when the team played together regularly
        def filteredSegments = getSegments(teamStats)
        
        return applyTeamCohesionFilter(filteredSegments, teamStats)
    }

    def getSegments(List<Map<String, ?>> teamStats) {
        return teamStats
                .collect { it.segments }
                .flattenOnce()
                .findAll { it.type == 'Map' && it.mode == '5v5' }
    }

    private def applyTeamCohesionFilter(List<Map<String, ?>> segments, List<Map<String, ?>> teamStats) {
        // Calculate average matches per player to identify consistent team play
        def playerMatchCounts = teamStats.collect { playerStats ->
            def playerSegments = playerStats.segments?.findAll { it.type == 'Map' && it.mode == '5v5' } ?: []
            return playerSegments.sum { (it.stats?.Matches as Integer) ?: 0 } ?: 0
        }
        
        if (playerMatchCounts.isEmpty() || playerMatchCounts.any { it == 0 }) {
            return []
        }
        
        // Check for team consistency: if match counts are very different, 
        // it suggests players haven't been playing together consistently
        def avgMatches = playerMatchCounts.sum() / playerMatchCounts.size()
        def maxDeviation = avgMatches * 0.5 // Allow 50% deviation
        
        def isConsistentTeam = playerMatchCounts.every { count ->
            Math.abs(count - avgMatches) <= maxDeviation
        }
        
        // Only return segments if team shows consistent play patterns
        return isConsistentTeam ? segments : []
    }
}
