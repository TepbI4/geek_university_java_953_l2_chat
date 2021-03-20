package ru.geekbrains.alekseiterentev.chat.client;

import ru.geekbrains.alekseiterentev.chat.server.ServerEngine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.EXIT;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN_FAILED;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN_OK;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.STAT;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.W;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.WHO_AM_I;

public class ClientHandler {

    private ServerEngine server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int msgCount = 0;
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(ServerEngine server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith(LOGIN)) {
                        // login Bob
                        String usernameFromLogin = msg.substring(LOGIN.length());

                        if (server.isNickBusy(usernameFromLogin)) {
                            sendMsg(LOGIN_FAILED + "Current nickname is already used");
                            continue;
                        }

                        nickname = usernameFromLogin;
                        sendMsg(LOGIN_OK + nickname);
                        server.subscribe(this);
                        break;
                    }
                }

                while (true) {
                    String msg = in.readUTF();
                    if (msg.isEmpty()) {
                        continue;
                    }

                    if (msg.startsWith(STAT)) {
                        sendMsg("Messages count is: " + msgCount);
                        continue;
                    }

                    if (msg.startsWith(WHO_AM_I)) {
                        sendMsg(nickname);
                        continue;
                    }

                    if (msg.startsWith(W)) {
                        String privateMsg = msg.substring(W.length());
                        String recipientNickname = privateMsg.substring(0, privateMsg.indexOf(" "));
                        String msgText = privateMsg.substring(privateMsg.indexOf(" ") + 1);
                        server.sendPrivateMessage(recipientNickname,
                                nickname + ": " + msgText);
                        sendMsg(nickname + ": " + msgText);
                        continue;
                    }

                    if (msg.startsWith(EXIT)) {
                        sendMsg(msg);
                        disconnect();
                        break;
                    }

                    server.broadcast(nickname + ": " + msg);
                    msgCount++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMsg(String msg) throws IOException {
        System.out.println(msg);
        out.writeUTF(msg);
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(this.getNickname() + " disconnected");
    }
}
