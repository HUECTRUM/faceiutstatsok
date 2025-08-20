package com.nothing.service.matchstats

import com.nothing.annotations.springcomponents.InjectableService
import com.nothing.http.FaceitDataApi

@InjectableService
class StatsCollector {
    public final FaceitDataApi faceitDataApi

    def collectStatsByMap(List t1Info, List t2Info) {
        def statsResponse = [t1Info, t2Info].collect { List<?> info -> info[1] }
        def teamPlayerData = [t1Info, t2Info].collect { List<?> info -> info[0] }

        return statsResponse
                .withIndex()
                .collect { teamStats, index -> getSegmentsForFullTeam(teamStats, teamPlayerData[index]) }
                .collect { it.groupBy({ Map<String, ?> segment -> segment.label }) }
    }

    def getSegmentsForFullTeam(List<Map<String, ?>> teamStats, List<Map<String, ?>> teamPlayerData) {
        // Extract player IDs from team data
        def playerIds = teamPlayerData.collect { it.player_id }
        
        // Verify we have exactly 5 players (full team)
        if (playerIds.size() != 5) {
            return []
        }
        
        // Get match histories for all players to check team consistency
        def playerHistories = playerIds.collect { playerId -> 
            faceitDataApi.getPlayerHistory(playerId)
        }
        
        // Find matches where all 5 players participated together
        def commonMatchIds = findCommonMatches(playerHistories)
        
        // Only aggregate stats if the team has played together in multiple matches
        // This ensures we're only getting stats for when this exact 5-person team played
        def minTeamMatches = 3 // Minimum matches required for the full team
        
        if (commonMatchIds.size() < minTeamMatches) {
            return [] // Not enough team matches, return empty stats
        }
        
        // Return segments only if the team has sufficient match history together
        return getSegments(teamStats)
    }

    def getSegments(List<Map<String, ?>> teamStats) {
        return teamStats
                .collect { it.segments }
                .flattenOnce()
                .findAll { it.type == 'Map' && it.mode == '5v5' }
    }

    private def findCommonMatches(List<List<Map<String, ?>>> playerHistories) {
        if (playerHistories.isEmpty() || playerHistories.size() != 5) {
            return []
        }
        
        // Start with first player's match IDs
        def commonMatches = playerHistories[0].collect { it.match_id }.toSet()
        
        // Intersect with each subsequent player's match IDs to find common matches
        playerHistories[1..-1].each { history ->
            def playerMatchIds = history.collect { it.match_id }.toSet()
            commonMatches = commonMatches.intersect(playerMatchIds)
        }
        
        return commonMatches
    }
}
