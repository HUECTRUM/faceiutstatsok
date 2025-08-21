package com.nothing.http

import spock.lang.Specification

class FaceitDataApiTest extends Specification {

    def "should find common matches where all players participated"() {
        given: "player histories for 5 players"
        def playerHistories = [
            // Player 1 history
            [
                [match_id: 'match1', started_at: 1000, teams: [faction1: [players: [player_id: 'player1']], faction2: [players: []]]],
                [match_id: 'match2', started_at: 2000, teams: [faction1: [players: [player_id: 'player1']], faction2: [players: []]]],
                [match_id: 'match3', started_at: 3000, teams: [faction1: [players: [player_id: 'player1']], faction2: [players: []]]]
            ],
            // Player 2 history
            [
                [match_id: 'match1', started_at: 1000, teams: [faction1: [players: [player_id: 'player2']], faction2: [players: []]]],
                [match_id: 'match2', started_at: 2000, teams: [faction1: [players: [player_id: 'player2']], faction2: [players: []]]],
                [match_id: 'match4', started_at: 4000, teams: [faction1: [players: [player_id: 'player2']], faction2: [players: []]]]
            ],
            // Player 3 history  
            [
                [match_id: 'match1', started_at: 1000, teams: [faction1: [players: [player_id: 'player3']], faction2: [players: []]]],
                [match_id: 'match5', started_at: 5000, teams: [faction1: [players: [player_id: 'player3']], faction2: [players: []]]]
            ],
            // Player 4 history
            [
                [match_id: 'match1', started_at: 1000, teams: [faction1: [players: [player_id: 'player4']], faction2: [players: []]]],
                [match_id: 'match2', started_at: 2000, teams: [faction1: [players: [player_id: 'player4']], faction2: [players: []]]]
            ],
            // Player 5 history
            [
                [match_id: 'match1', started_at: 1000, teams: [faction1: [players: [player_id: 'player5']], faction2: [players: []]]],
                [match_id: 'match6', started_at: 6000, teams: [faction1: [players: [player_id: 'player5']], faction2: [players: []]]]
            ]
        ]
        def playerIds = ['player1', 'player2', 'player3', 'player4', 'player5']

        when: "finding common matches"
        def commonMatches = findCommonMatches(playerHistories, playerIds)

        then: "should return only matches where all 5 players participated"
        commonMatches.size() == 1
        commonMatches[0].match_id == 'match1'
    }

    private List findCommonMatches(List<List> playerHistories, List<String> playerIds) {
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
}