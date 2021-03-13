package ru.geekbrains.alekseiterentev.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {

    @FXML
    TextField loginField, msgField;

    @FXML
    TextArea chatField;

    @FXML
    HBox loginPanel, msgPanel;

    private String nickname;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public static final String STAT = "/stat";
    public static final String LOGIN = "/login ";
    public static final String LOGIN_OK = "/login_ok ";
    public static final String LOGIN_FAILED = "/login_failed ";
    public static final String WHO_AM_I = "/who_am_i";
    public static final String EXIT = "/exit";

    public void setNickname(String nickname) {
        if (nickname != null) {
            this.nickname = nickname;
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
        } else {
            loginPanel.setVisible(true);
            loginPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
        }
    }

    public void sendMsgAction(Event actionEvent) {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to send your message!", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void sendMsgOnKeyPressedAction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            sendMsgAction(keyEvent);
        }
    }

    public void login(Event actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        if (loginField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "User Log in cannot be empty!", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            out.writeUTF("/login " + loginField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loginOnKeyPressedAction(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            login(keyEvent);
        }
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith(LOGIN_OK)) {
                            setNickname(msg.substring(LOGIN_OK.length()));
                            break;
                        }
                        if (msg.startsWith(LOGIN_FAILED)) {
                            String cause = msg.substring(LOGIN_FAILED.length());
                            chatField.appendText(cause + "\n");
                        }
                    }

                    while (true) {
                        String msg = in.readUTF();

                        if (msg.startsWith(EXIT)) {
                            disconnect();
                            break;
                        }

                        chatField.appendText(msg + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to server [ localhost:8189 ]");
        }
    }

    private void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setNickname(null);
    }
}
