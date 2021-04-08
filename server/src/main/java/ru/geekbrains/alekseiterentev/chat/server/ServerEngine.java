package ru.geekbrains.alekseiterentev.chat.server;

import ru.geekbrains.alekseiterentev.chat.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerEngine {

    public static final String STAT_CMD = "/stat";
    public static final String LOGIN_CMD = "/login ";
    public static final String LOGIN_OK_CMD = "/login_ok ";
    public static final String LOGIN_FAILED_CMD = "/login_failed ";
    public static final String WHO_AM_I_CMD = "/who_am_i";
    public static final String EXIT_CMD = "/exit";
    public static final String PRIVATE_MSG_CMD = "/w ";
    public static final String CHANGE_NICK_CMD = "/change_nick ";

    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public ServerEngine(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
//        this.authenticationProvider = new InMemoryAuthenticationProvider();
        this.authenticationProvider = new DbAuthenticationProvider();
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        try(ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("Server running on port 8189. Waiting for client logged in...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client logged in!!!");

                executorService.execute(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    public void subscribe(ClientHandler client) throws IOException {
        clients.add(client);
        broadcast("Client " + client.getNickname() + " entered the chat!");
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler client) throws IOException {
        clients.remove(client);
        broadcast("Client " + client.getNickname() + " left the chat!");
        broadcastClientsList();
    }

    public void broadcast(String msg) throws IOException {
        for (ClientHandler client : clients) {
            client.sendMsg(msg);
        }
    }

    public synchronized void broadcastClientsList() throws IOException {
        StringBuilder stringBuilder = new StringBuilder("/clients_list ");
        for (ClientHandler client : clients) {
            stringBuilder.append(client.getNickname()).append(" ");
        }
        stringBuilder.setLength(stringBuilder.length() - 1);
        String clientsList = stringBuilder.toString();
        broadcast(clientsList);
    }

    public void sendPrivateMessage(ClientHandler sender, String clientName, String msg) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(clientName)) {
                client.sendMsg("From: " + sender.getNickname() + " Message: " + msg);
                sender.sendMsg("To: " + clientName + " Message : " + msg);
                return;
            }
        }
        sender.sendMsg("Unable to send message to user: " + clientName + ". No such user on-line.");
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
