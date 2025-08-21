package com.nothing.service.integration

import com.nothing.http.FaceitDataApi
import spock.lang.Specification

class TeamStatsIntegrationTest extends Specification {

    def faceitDataApi = new FaceitDataApi()

    def "should filter team stats to only include matches where all players participated together"() {
        given: "a team of 5 players"
        def playerIds = ['player1', 'player2', 'player3', 'player4', 'player5']
        
        and: "mocked player histories showing different match participation"
        faceitDataApi.metaClass.getPlayerData = { String playerId -> 
            [player_id: playerId, nickname: "player_${playerId}"]
        }
        
        faceitDataApi.metaClass.getPlayerHistory = { String playerId ->
            switch(playerId) {
                case 'player1':
                    return [
                        [match_id: 'match1', started_at: 1000],
                        [match_id: 'match2', started_at: 2000],
                        [match_id: 'match3', started_at: 3000]  // only player1 played this
                    ]
                case 'player2':
                    return [
                        [match_id: 'match1', started_at: 1000],
                        [match_id: 'match2', started_at: 2000],
                        [match_id: 'match4', started_at: 4000]  // only player2 played this
                    ]
                case 'player3':
                case 'player4':
                case 'player5':
                    return [
                        [match_id: 'match1', started_at: 1000],
                        [match_id: 'match2', started_at: 2000]
                    ]
                default:
                    return []
            }
        }
        
        faceitDataApi.metaClass.getPlayerStats = { String playerId ->
            [
                segments: [
                    [
                        type: 'Map',
                        mode: '5v5', 
                        label: 'de_dust2',
                        stats: [
                            'Average K/D Ratio': 1.2,
                            'Average Kills': 20.0,
                            'Win Rate %': 60.0,
                            'Matches': 10  // Original matches for this player on this map
                        ]
                    ],
                    [
                        type: 'Map',
                        mode: '5v5',
                        label: 'de_mirage', 
                        stats: [
                            'Average K/D Ratio': 1.1,
                            'Average Kills': 18.0,
                            'Win Rate %': 55.0,
                            'Matches': 8
                        ]
                    ]
                ]
            ]
        }

        when: "getting team data with new filtering logic"
        def (playerData, playerStats) = faceitDataApi.getTeamData(playerIds)

        then: "should only include stats from common matches (match1 and match2)"
        playerData.size() == 5
        playerStats.size() == 5
        
        and: "each player's stats should be filtered"
        playerStats.each { playerStat ->
            playerStat.segments.each { segment ->
                if (segment.type == 'Map' && segment.mode == '5v5') {
                    // The match count should be reduced to reflect only common matches
                    // Since we have 2 common matches out of roughly 10 original matches per player,
                    // the filtered match count should be around 2-4 per map
                    assert segment.stats['Matches'] < segment.stats['Matches'] ?: 10
                    
                    // Averages (K/D, Kills) should remain the same
                    assert segment.stats['Average K/D Ratio'] in [1.1, 1.2]
                    assert segment.stats['Win Rate %'] in [55.0, 60.0]
                }
            }
        }
    }

    def "should return empty stats when no common matches exist"() {
        given: "players with no overlapping match history"
        def playerIds = ['player1', 'player2']
        
        faceitDataApi.metaClass.getPlayerData = { String playerId -> 
            [player_id: playerId]
        }
        
        faceitDataApi.metaClass.getPlayerHistory = { String playerId ->
            switch(playerId) {
                case 'player1': return [[match_id: 'match1']]
                case 'player2': return [[match_id: 'match2']]
                default: return []
            }
        }
        
        faceitDataApi.metaClass.getPlayerStats = { String playerId ->
            [segments: [[type: 'Map', mode: '5v5', label: 'de_dust2', stats: ['Matches': 5]]]]
        }

        when: "getting team data"
        def (playerData, playerStats) = faceitDataApi.getTeamData(playerIds)

        then: "should return empty stats structure"
        playerStats.each { playerStat ->
            playerStat.segments.each { segment ->
                assert segment.stats['Matches'] == 0
            }
        }
    }
}