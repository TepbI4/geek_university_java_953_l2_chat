package ru.geekbrains.alekseiterentev.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {

    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключения клиента...");

            Socket socket = serverSocket.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Клиент подключился!!!");
            int msgCount = 0;

            while (true) {
                String msg = in.readUTF();
                if (msg.isEmpty()) {
                    continue;
                }

                if (msg.startsWith("/stat")) {
                    out.writeUTF("Messages count is: " + msgCount);
                    continue;
                }

                System.out.println(msg);
                out.writeUTF("ECHO: " + msg);
                msgCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
