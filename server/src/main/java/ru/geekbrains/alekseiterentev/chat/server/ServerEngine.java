package ru.geekbrains.alekseiterentev.chat.server;

import ru.geekbrains.alekseiterentev.chat.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerEngine {

    public static final String STAT = "/stat";
    public static final String LOGIN = "/login ";
    public static final String LOGIN_OK = "/login_ok ";
    public static final String LOGIN_FAILED = "/login_failed ";
    public static final String WHO_AM_I = "/who_am_i";
    public static final String EXIT = "/exit";
    public static final String W = "/w ";

    private int port;
    private List<ClientHandler> clients;

    public ServerEngine(int port) {
        this.port = port;
        this.clients = new ArrayList<>();

        try(ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("Server running on port 8189. Waiting for client logged in...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client logged in!!!");

                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcast(String msg) throws IOException {
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        };
    }

    public void sendPrivateMessage(String clientName, String msg) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(clientName)) {
                client.sendMsg(msg);
            }
        }
    }

    public boolean isNickBusy(String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getNickname().equals(username)) {
                return true;
            }
        }
        return false;
    }
}
