package com.gamedev.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.List;
import java.util.Random;

public class SimpleGame {
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private final Random random = new Random();
    private long lastUpdate = System.currentTimeMillis();
    private long lastEnemySpawn = System.currentTimeMillis();
    private int enemySpawnRate = 2000; // milliseconds
    
    public static class PlayerState {
        public int x, y;
        public int playerId;
        public int health;
        public int score;
        public long lastShot;
        public boolean alive;
        
        public PlayerState(int playerId) {
            this.playerId = playerId;
            this.x = 0; // Starting position
            this.y = 0;
            this.health = 100;
            this.score = 0;
            this.lastShot = 0;
            this.alive = true;
        }
        
        // Legacy constructor for compatibility
        public PlayerState(int id, boolean legacy) {
            this(id);
        }
        
        public int id() { return playerId; }
    }
    
    public static class Enemy {
        public int x, y;
        public int id;
        public int health;
        public int targetPlayerId;
        
        public Enemy(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.health = 20;
            this.targetPlayerId = -1;
        }
    }
    
    public static class Projectile {
        public int x, y;
        public int dx, dy;
        public int playerId;
        public int id;
        
        public Projectile(int id, int x, int y, int dx, int dy, int playerId) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.playerId = playerId;
        }
    }
    
    public void addPlayer(int playerId) {
        players.put(playerId, new PlayerState(playerId));
    }
    
    public void removePlayer(int playerId) {
        players.remove(playerId);
    }
    
    public void updatePlayer(int playerId, String command) {
        PlayerState player = players.computeIfAbsent(playerId, PlayerState::new);
        if (!player.alive) return;
        
        int speed = 2;
        switch (command) {
            case "UP": player.y -= speed; break;
            case "DOWN": player.y += speed; break;
            case "LEFT": player.x -= speed; break;
            case "RIGHT": player.x += speed; break;
            case "SHOOT_UP": shootProjectile(playerId, "UP"); break;
            case "SHOOT_DOWN": shootProjectile(playerId, "DOWN"); break;
            case "SHOOT_LEFT": shootProjectile(playerId, "LEFT"); break;
            case "SHOOT_RIGHT": shootProjectile(playerId, "RIGHT"); break;
        }
        
        // Keep player in bounds
        player.x = Math.max(-50, Math.min(50, player.x));
        player.y = Math.max(-50, Math.min(50, player.y));
    }
    
    public boolean movePlayer(int playerId, String direction) {
        updatePlayer(playerId, direction);
        return true;
    }
    
    public void shootProjectile(int playerId, String direction) {
        PlayerState player = players.get(playerId);
        if (player == null || !player.alive) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - player.lastShot < 300) return; // Rate limit shooting
        
        player.lastShot = currentTime;
        
        int dx = 0, dy = 0, speed = 5;
        switch (direction.toUpperCase()) {
            case "UP": dy = -speed; break;
            case "DOWN": dy = speed; break;
            case "LEFT": dx = -speed; break;
            case "RIGHT": dx = speed; break;
            default: return;
        }
        
        int projectileId = projectiles.size();
        projectiles.add(new Projectile(projectileId, player.x, player.y, dx, dy, playerId));
    }
    
    public Map<Integer, PlayerState> getPlayers() {
        return players;
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdate;
        lastUpdate = currentTime;
        
        // Spawn enemies
        if (currentTime - lastEnemySpawn > enemySpawnRate) {
            spawnEnemy();
            lastEnemySpawn = currentTime;
        }
        
        // Update projectiles
        projectiles.removeIf(projectile -> {
            projectile.x += projectile.dx;
            projectile.y += projectile.dy;
            
            // Remove if out of bounds
            return Math.abs(projectile.x) > 100 || Math.abs(projectile.y) > 100;
        });
        
        // Update enemies
        for (Enemy enemy : enemies) {
            // Find nearest player
            PlayerState nearestPlayer = null;
            double nearestDistance = Double.MAX_VALUE;
            
            for (PlayerState player : players.values()) {
                if (!player.alive) continue;
                double distance = Math.sqrt(Math.pow(enemy.x - player.x, 2) + Math.pow(enemy.y - player.y, 2));
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
            
            // Move towards nearest player
            if (nearestPlayer != null) {
                int dx = Integer.compare(nearestPlayer.x, enemy.x);
                int dy = Integer.compare(nearestPlayer.y, enemy.y);
                enemy.x += dx;
                enemy.y += dy;
                enemy.targetPlayerId = nearestPlayer.playerId;
            }
        }
        
        // Check collisions
        checkCollisions();
    }
    
    private void spawnEnemy() {
        if (players.isEmpty()) return;
        
        // Spawn at edge of map
        int x, y;
        if (random.nextBoolean()) {
            x = random.nextBoolean() ? -60 : 60;
            y = random.nextInt(120) - 60;
        } else {
            x = random.nextInt(120) - 60;
            y = random.nextBoolean() ? -60 : 60;
        }
        
        enemies.add(new Enemy(enemies.size(), x, y));
    }
    
    private void checkCollisions() {
        // Projectile vs Enemy collisions
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                
                if (Math.abs(projectile.x - enemy.x) <= 2 && Math.abs(projectile.y - enemy.y) <= 2) {
                    enemy.health -= 10;
                    projectiles.remove(i);
                    
                    if (enemy.health <= 0) {
                        enemies.remove(j);
                        // Award points to player
                        PlayerState player = players.get(projectile.playerId);
                        if (player != null) {
                            player.score += 10;
                        }
                    }
                    break;
                }
            }
        }
        
        // Enemy vs Player collisions
        for (Enemy enemy : enemies) {
            for (PlayerState player : players.values()) {
                if (!player.alive) continue;
                
                if (Math.abs(enemy.x - player.x) <= 2 && Math.abs(enemy.y - player.y) <= 2) {
                    player.health -= 1;
                    if (player.health <= 0) {
                        player.alive = false;
                    }
                }
            }
        }
    }
    
    public String getGameState() {
        StringBuilder state = new StringBuilder();
        
        // Players
        for (PlayerState player : players.values()) {
            state.append(String.format("PLAYER:%d:%d:%d:%d:%d:%s|", 
                player.playerId, player.x, player.y, player.health, player.score, player.alive));
        }
        
        // Enemies
        for (Enemy enemy : enemies) {
            state.append(String.format("ENEMY:%d:%d:%d:%d|", 
                enemy.id, enemy.x, enemy.y, enemy.health));
        }
        
        // Projectiles
        for (Projectile projectile : projectiles) {
            state.append(String.format("PROJECTILE:%d:%d:%d:%d|", 
                projectile.id, projectile.x, projectile.y, projectile.playerId));
        }
        
        return state.toString();
    }
}
