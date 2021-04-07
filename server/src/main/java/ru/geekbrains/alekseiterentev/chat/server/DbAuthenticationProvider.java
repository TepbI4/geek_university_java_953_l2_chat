package ru.geekbrains.alekseiterentev.chat.server;

import org.sqlite.JDBC;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbAuthenticationProvider implements AuthenticationProvider {

    private final Connection connection;
    private final PreparedStatement getNicknameByLoginAndPassword;
    private final PreparedStatement updateNickname;

    public DbAuthenticationProvider() {
        try {
            Class.forName(JDBC.class.getName());
            connection = DriverManager.getConnection(JDBC.PREFIX + "chatdb.db");
            getNicknameByLoginAndPassword = connection.prepareStatement("select nick_name from user where login = ? and password = ?");
            updateNickname = connection.prepareStatement("update user set nick_name = ? where nick_name = ?");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to connect to DB");
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        getNicknameByLoginAndPassword.setString(1, login);
        getNicknameByLoginAndPassword.setString(2, password);

        ResultSet resultSet = getNicknameByLoginAndPassword.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString(1);
        }
        return null;
    }

    @Override
    public void changeNickname(String oldNickname, String newNickname) throws SQLException {
        updateNickname.setString(2, oldNickname);
        updateNickname.setString(1, newNickname);
        updateNickname.executeUpdate();
    }
}
