package com.gamedev.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private final int playerId;
    private boolean connected = true;

    public ClientHandler(Socket socket, GameServer server, int playerId) {
        this.socket = socket;
        this.server = server;
        this.playerId = playerId;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            String inputLine;
            while (connected && (inputLine = in.readLine()) != null) {
                processCommand(inputLine.trim());
            }
        } catch (IOException e) {
            System.out.println("Player " + playerId + " connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void processCommand(String command) {
        if (command.isEmpty()) return;
        
        System.out.println("Player " + playerId + " command: " + command);
        
        switch (command.toUpperCase()) {
            case "W": 
            case "UP":
                server.getGame().updatePlayer(playerId, "UP");
                break;
            case "S":
            case "DOWN":
                server.getGame().updatePlayer(playerId, "DOWN");
                break;
            case "A":
            case "LEFT":
                server.getGame().updatePlayer(playerId, "LEFT");
                break;
            case "D":
            case "RIGHT":
                server.getGame().updatePlayer(playerId, "RIGHT");
                break;
            case "I":
            case "SHOOT_UP":
                server.getGame().updatePlayer(playerId, "SHOOT_UP");
                break;
            case "K":
            case "SHOOT_DOWN":
                server.getGame().updatePlayer(playerId, "SHOOT_DOWN");
                break;
            case "J":
            case "SHOOT_LEFT":
                server.getGame().updatePlayer(playerId, "SHOOT_LEFT");
                break;
            case "L":
            case "SHOOT_RIGHT":
                server.getGame().updatePlayer(playerId, "SHOOT_RIGHT");
                break;
            case "Q":
            case "QUIT":
            case "EXIT":
                disconnect();
                break;
            default:
                sendMessage("INFO:Unknown command: " + command);
                break;
        }
    }
    
    private void disconnect() {
        connected = false;
        server.removeClient(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket for player " + playerId);
        }
    }

    public void sendMessage(String message) {
        if (out != null && connected) {
            try {
                out.println(message);
            } catch (Exception e) {
                connected = false;
            }
        }
    }
    
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
    
    public int getPlayerId() {
        return playerId;
    }
}
