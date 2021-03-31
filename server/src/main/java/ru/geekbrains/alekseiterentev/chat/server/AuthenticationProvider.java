package ru.geekbrains.alekseiterentev.chat.server;

import java.sql.SQLException;

public interface AuthenticationProvider {
    String getNicknameByLoginAndPassword(String login, String password) throws SQLException;
    void changeNickname(String oldNickname, String newNickname) throws SQLException;
}
