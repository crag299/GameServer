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
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Game server started on port " + PORT);
            while (running && clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Player connected: " + clientSocket.getInetAddress());
                int playerId = ++playerCounter;
                ClientHandler handler = new ClientHandler(clientSocket, this, playerId);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public SimpleGame getGame() {
        return game;
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}
