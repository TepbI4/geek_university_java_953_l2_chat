package ru.geekbrains.alekseiterentev.chat.client;

import ru.geekbrains.alekseiterentev.chat.server.ServerEngine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.CHANGE_NICK_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.EXIT_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN_FAILED_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.LOGIN_OK_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.STAT_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.PRIVATE_MSG_CMD;
import static ru.geekbrains.alekseiterentev.chat.server.ServerEngine.WHO_AM_I_CMD;

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
                    if (msg.startsWith(LOGIN_CMD)) {
                        // login Bob
                        String usernameFromLogin = msg.substring(LOGIN_CMD.length());

                        if (server.isNickBusy(usernameFromLogin)) {
                            sendMsg(LOGIN_FAILED_CMD + "Current nickname is already used");
                            continue;
                        }

                        nickname = usernameFromLogin;
                        sendMsg(LOGIN_OK_CMD + nickname);
                        server.subscribe(this);
                        break;
                    }
                }

                while (true) {
                    String msg = in.readUTF();
                    if (msg.isEmpty()) {
                        continue;
                    }

                    if (msg.startsWith("/")) {
                        if (sendCmd(msg)) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    server.broadcast(nickname + ": " + msg);
                    msgCount++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private boolean sendCmd(String cmd) throws IOException {
        if (cmd.startsWith(STAT_CMD)) {
            sendMsg("Messages count is: " + msgCount);
            return true;
        }

        if (cmd.startsWith(WHO_AM_I_CMD)) {
            sendMsg(nickname);
            return true;
        }

        if (cmd.startsWith(PRIVATE_MSG_CMD)) {
            String privateMsg = cmd.substring(PRIVATE_MSG_CMD.length());
            String recipientNickname = privateMsg.substring(0, privateMsg.indexOf(" "));
            String msgText = privateMsg.substring(privateMsg.indexOf(" ") + 1);
            server.sendPrivateMessage(this, recipientNickname, nickname + ": " + msgText);
            msgCount++;
            return true;
        }

        if (cmd.startsWith(CHANGE_NICK_CMD)) {
            String newNickName = cmd.substring(CHANGE_NICK_CMD.length());
            server.unsubscribe(this);
            nickname = newNickName;
            server.subscribe(this);
            return true;
        }

        if (cmd.startsWith(EXIT_CMD)) {
            sendMsg(cmd);
            return false;
        }

        return true;
    }

    public void sendMsg(String msg) throws IOException {
        System.out.println(msg);
        out.writeUTF(msg);
    }

    public void disconnect() throws IOException {
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
