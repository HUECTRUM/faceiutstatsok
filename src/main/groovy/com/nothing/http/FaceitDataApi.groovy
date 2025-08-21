package com.nothing.http

import com.nothing.annotations.springcomponents.InjectableService

import org.springframework.web.reactive.function.client.WebClient

import static java.lang.System.currentTimeMillis

@InjectableService
class FaceitDataApi {
    public final Map<String, WebClient> clients

    def getPlayerBySteamId(String steamId) {
        return clients.v1Client.executeBlockingGet("/?limit=1&query=$steamId")
    }

    def getMatch(String matchId) {
        return clients.dataClient.executeBlockingGet("matches/$matchId")
    }

    def getPlayerByName(String playername) {
        return clients.dataClient.executeBlockingGet("/players?nickname=$playername&game=csgo")
    }

    def getPlayerData(String playerId) {
        return clients.dataClient.executeBlockingGet("players/$playerId")
    }

    def getPlayerStats(String playerId) {
        return clients.dataClient.executeBlockingGet("players/$playerId/stats/csgo")
    }

    def getTeamData(List<String> playerIds) {
        def playerData = playerIds.collect { getPlayerData(it) }
        
        // Get player histories to find common matches where ALL players participated together
        def playerHistories = playerIds.collect { getPlayerHistory(it) }
        def commonMatches = findCommonMatches(playerHistories, playerIds)
        
        // Get original player stats for structure reference
        def originalPlayerStats = playerIds.collect { getPlayerStats(it) }
        
        // Filter stats to only include data from common matches
        def filteredPlayerStats = filterStatsToCommonMatches(originalPlayerStats, commonMatches)
        
        return [playerData, filteredPlayerStats]
    }

    def getPlayerHistory(String playerId) {
        long maxTimestamp = currentTimeMillis() / 1000L

        return [].generateUntilEmpty {
            def batch = clients.dataClient.executeBlockingGet("/players/$playerId/history?game=csgo&from=1325376000&to=$maxTimestamp&limit=100")
                    .items
                    .sort { it.started_at }

            maxTimestamp = batch[0]?.started_at ?: 0
            return batch
        }
    }

    private List findCommonMatches(List<List> playerHistories, List<String> playerIds) {
        if (playerHistories.empty || playerHistories.any { it.empty }) {
            return []
        }

        // Get all match IDs for each player
        def playerMatchIds = playerHistories.collect { history ->
            history.collect { match -> match.match_id }.toSet()
        }

        // Find intersection - matches where ALL players participated
        def commonMatchIds = playerMatchIds.inject { result, playerMatches ->
            result.intersect(playerMatches)
        }

        // Return the actual match data for common matches
        return playerHistories[0].findAll { match ->
            match.match_id in commonMatchIds
        }
    }

    private List filterStatsToCommonMatches(List originalPlayerStats, List commonMatches) {
        if (commonMatches.empty) {
            // If no common matches, return empty stats with same structure but zero values
            return originalPlayerStats.collect { playerStat ->
                def emptySegments = playerStat.segments?.collect { segment ->
                    def emptyStats = segment.stats?.collectEntries { key, value ->
                        [key, key == 'Matches' ? 0 : 0.0]
                    } ?: [:]
                    [
                        type: segment.type,
                        mode: segment.mode,
                        label: segment.label,
                        stats: emptyStats
                    ]
                } ?: []
                [segments: emptySegments]
            }
        }

        // For the first implementation, we need to estimate the filtering ratio
        // based on the percentage of common matches vs total matches for the team
        def totalMatches = originalPlayerStats.sum { playerStat ->
            playerStat.segments?.findAll { it.type == 'Map' && it.mode == '5v5' }
                ?.sum { it.stats?.Matches ?: 0 } ?: 0
        } ?: 0

        def averageMatchesPerPlayer = totalMatches / originalPlayerStats.size()
        def commonMatchCount = commonMatches.size()
        
        // Calculate filtering ratio (what portion of matches should be included)
        def filterRatio = averageMatchesPerPlayer > 0 ? 
            Math.min(1.0, commonMatchCount / averageMatchesPerPlayer) : 0.0

        // Apply the filtering ratio to scale down the stats
        return originalPlayerStats.collect { playerStat ->
            def filteredSegments = playerStat.segments?.collect { segment ->
                if (segment.type == 'Map' && segment.mode == '5v5') {
                    def filteredStats = segment.stats?.collectEntries { key, value ->
                        if (key == 'Matches') {
                            [key, Math.round((value as double) * filterRatio)]
                        } else if (key in ['Win Rate %']) {
                            // Win rate stays the same (percentage)
                            [key, value]
                        } else {
                            // Other stats (K/D, kills, etc.) also stay the same as they're averages
                            [key, value]
                        }
                    } ?: [:]
                    [
                        type: segment.type,
                        mode: segment.mode,
                        label: segment.label,
                        stats: filteredStats
                    ]
                } else {
                    // Keep non-Map segments unchanged
                    segment
                }
            } ?: []
            [segments: filteredSegments]
        }
    }
}
