package com.nothing.service.matchstats

import spock.lang.Specification

class StatsCollectorTest extends Specification {

    def statsCollector = new StatsCollector()

    def "should understand current segment structure"() {
        given: "sample team stats data structure"
        def mockTeamStats = [
            [
                segments: [
                    [type: 'Map', mode: '5v5', label: 'de_dust2', stats: ['Average K/D Ratio': 1.2, 'Matches': 10]],
                    [type: 'Map', mode: '5v5', label: 'de_mirage', stats: ['Average K/D Ratio': 1.1, 'Matches': 5]],
                    [type: 'Overall', mode: '5v5', stats: ['Average K/D Ratio': 1.15, 'Matches': 15]]
                ]
            ],
            [
                segments: [
                    [type: 'Map', mode: '5v5', label: 'de_dust2', stats: ['Average K/D Ratio': 0.9, 'Matches': 8]],
                    [type: 'Map', mode: '5v5', label: 'de_mirage', stats: ['Average K/D Ratio': 1.3, 'Matches': 3]]
                ]
            ]
        ]

        when: "processing segments"
        def result = statsCollector.getSegments(mockTeamStats)

        then: "should filter only Map segments with 5v5 mode"
        result.size() == 4
        result.every { it.type == 'Map' && it.mode == '5v5' }
        result.collect { it.label }.sort() == ['de_dust2', 'de_dust2', 'de_mirage', 'de_mirage']
    }

    def "should understand team data collection structure"() {
        given: "mock team info with player data and stats"
        def team1Info = [
            [/* player data */], 
            [/* player stats with segments */]
        ]
        def team2Info = [
            [/* player data */], 
            [/* player stats with segments */]
        ]

        when: "collecting stats by map"
        // This shows the current structure - team info contains [playerData, playerStats]
        def statsResponse = [team1Info, team2Info].collect { List<?> info -> info[1] }

        then: "should extract stats portion"
        statsResponse.size() == 2
        // Each element is the player stats portion (index 1) from team info
    }
}