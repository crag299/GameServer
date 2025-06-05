package com.gamedev.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SimpleGame {
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();

    public void updatePlayer(int playerId, String command) {
        PlayerState player = players.computeIfAbsent(playerId, PlayerState::new);
        switch (command) {
            case "UP": player.y--; break;
            case "DOWN": player.y++; break;
            case "LEFT": player.x--; break;
            case "RIGHT": player.x++; break;
        }
    }

    public Map<Integer, PlayerState> getPlayers() {
        return players;
    }

    public static class PlayerState {
        public int id;
        public int x = 0, y = 0;
        public PlayerState(int id) { this.id = id; }
    }
}
