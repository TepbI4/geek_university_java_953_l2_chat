package ru.geekbrains.alekseiterentev.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField loginField, msgField;

    @FXML
    PasswordField passwordField;

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
    private File logFile;
    private BufferedWriter logWriter;

    public static final String STAT_CMD = "/stat";
    public static final String LOGIN_CMD = "/login ";
    public static final String LOGIN_OK_CMD = "/login_ok ";
    public static final String LOGIN_FAILED_CMD = "/login_failed ";
    public static final String WHO_AM_I_CMD = "/who_am_i";
    public static final String EXIT_CMD = "/exit";
    public static final String PRIVATE_MSG_CMD = "/w ";
    public static final String CLIENTS_MSG_CMD = "/clients_list ";

    public void setNickname(String nickname) {
        this.nickname = nickname;
        boolean nicknameIsNull = nickname == null;
        loginPanel.setVisible(nicknameIsNull);
        loginPanel.setManaged(nicknameIsNull);
        msgPanel.setVisible(!nicknameIsNull);
        msgPanel.setManaged(!nicknameIsNull);
        clientsListBox.setVisible(!nicknameIsNull);
        clientsListBox.setManaged(!nicknameIsNull);
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
            out.writeUTF(LOGIN_CMD + loginField.getText() + " " + passwordField.getText());
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
            logFile = new File("log.txt");

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith(LOGIN_OK_CMD)) {
                            setNickname(msg.substring(LOGIN_OK_CMD.length()));
                            fetchLog();
                            createLogger();
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
                        logMessage(msg);
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

        try {
            if(in != null) {
                in.close();
            }

            if(out != null) {
                out.close();
            }

            if (socket != null) {
                socket.close();
            }

            if(logWriter != null) {
                logWriter.flush();
                logWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        setNickname(null);
    }

    public void onClickLogOut(ActionEvent actionEvent) throws IOException {
        out.writeUTF(EXIT_CMD);
    }

    private void fetchLog() {
        try (BufferedReader dataInputStream = new BufferedReader(new FileReader(logFile))) {
            String logLine = dataInputStream.readLine();
            while (logLine != null) {
                chatField.appendText(logLine + "\n");
                logLine = dataInputStream.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createLogger() throws IOException {
        logWriter = new BufferedWriter(new FileWriter(logFile));
    }

    private void logMessage(String msg) throws IOException {
        logWriter.write(msg + "\n");
    }
}
