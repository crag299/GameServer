package com.gamedev.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 4;
    private final List<ClientHandler> clients = new ArrayList<>();
    private boolean running = true;
    private static int playerCounter = 0;
    private final SimpleGame game = new SimpleGame();

    public void start() {
        // Start game update loop in separate thread
        Thread gameLoop = new Thread(this::gameLoop);
        gameLoop.setDaemon(true);
        gameLoop.start();
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);
            System.out.println("Game loop started - enemies will spawn and attack!");
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player connected: " + clientSocket.getInetAddress());
                int playerId = ++playerCounter;
                ClientHandler handler = new ClientHandler(clientSocket, this, playerId);
                clients.add(handler);
                game.addPlayer(playerId);
                new Thread(handler).start();
                
                // Send welcome message with controls
                handler.sendMessage("WELCOME:Connected as Player " + playerId);
                handler.sendMessage("INFO:Controls: WASD to move, IJKL to shoot, Q to quit");
                handler.sendMessage("INFO:Survive against the enemies! Kill them for points!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void gameLoop() {
        final int FPS = 20; // 20 updates per second
        final long FRAME_TIME = 1000 / FPS;
        
        while (running) {
            long startTime = System.currentTimeMillis();
            
            // Update game state
            game.update();
            
            // Broadcast game state to all clients
            String gameState = game.getGameState();
            if (!gameState.isEmpty()) {
                broadcast("GAMESTATE:" + gameState);
            }
            
            // Sleep to maintain frame rate
            long elapsedTime = System.currentTimeMillis() - startTime;
            long sleepTime = FRAME_TIME - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public synchronized void broadcast(String message) {
        clients.removeIf(client -> !client.isConnected());
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        game.removePlayer(client.getPlayerId());
        System.out.println("Player " + client.getPlayerId() + " disconnected");
    }

    public SimpleGame getGame() {
        return game;
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
