package ru.geekbrains.alekseiterentev.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField loginField, msgField;

    @FXML
    TextArea chatField;

    @FXML
    HBox loginPanel, msgPanel;

    @FXML
    ListView<String> clientsList;

    @FXML
    VBox clientsListBox;

    private String nickname;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public static final String STAT_CMD = "/stat";
    public static final String LOGIN_CMD = "/login ";
    public static final String LOGIN_OK_CMD = "/login_ok ";
    public static final String LOGIN_FAILED_CMD = "/login_failed ";
    public static final String WHO_AM_I_CMD = "/who_am_i";
    public static final String EXIT_CMD = "/exit";
    public static final String PRIVATE_MSG_CMD = "/w ";
    public static final String CLIENTS_MSG_CMD = "/clients_list ";

    public void setNickname(String nickname) {
        if (nickname != null) {
            this.nickname = nickname;
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            msgPanel.setVisible(true);
            msgPanel.setManaged(true);
            clientsListBox.setVisible(true);
            clientsListBox.setManaged(true);
        } else {
            loginPanel.setVisible(true);
            loginPanel.setManaged(true);
            msgPanel.setVisible(false);
            msgPanel.setManaged(false);
            clientsListBox.setVisible(false);
            clientsListBox.setManaged(false);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setNickname(null);
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
            out.writeUTF(LOGIN_CMD + loginField.getText());
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
                        if (msg.startsWith(LOGIN_OK_CMD)) {
                            setNickname(msg.substring(LOGIN_OK_CMD.length()));
                            break;
                        }
                        if (msg.startsWith(LOGIN_FAILED_CMD)) {
                            String cause = msg.substring(LOGIN_FAILED_CMD.length());
                            chatField.appendText(cause + "\n");
                        }
                    }

                    while (true) {
                        String msg = in.readUTF();

                        if (msg.startsWith("/")) {
                            if (executeCmd(msg)) {
                                continue;
                            } else {
                                break;
                            }
                        }

                        chatField.appendText(msg + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to server [ localhost:8189 ]");
        }
    }

    private boolean executeCmd(String cmd) {
        if (cmd.startsWith(EXIT_CMD)) {
            return false;
        }

        if (cmd.startsWith(CLIENTS_MSG_CMD)) {
            String clientsStr = cmd.substring(CLIENTS_MSG_CMD.length());
            String[] clients = clientsStr.split("\\s");

            Platform.runLater(() -> {
                System.out.println(Thread.currentThread().getName());
                clientsList.getItems().clear();
                for (String client : clients) {
                    clientsList.getItems().add(client);
                }
            });

            return true;
        }

        return true;
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

    public void onClickLogOut(ActionEvent actionEvent) throws IOException {
        out.writeUTF(EXIT_CMD);
    }
}
