package ru.gb.may_chat.server.dao;

import ru.gb.may_chat.server.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    private Connection connection;

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chat-db");
    }

    public void close() throws SQLException {
        connection.close();
    }

    public Optional<String> getNickByLoginAndPassword(
            String login,
            String password
    ) throws SQLException {
        var statement = connection.prepareStatement(
                "SELECT * FROM user WHERE login = ? AND password = ?");
        statement.setString(1, login);
        statement.setString(2, password);
        var result = statement.executeQuery();
        if (result.next()) {
            return Optional.of(result.getString("nick"));
        } else {
            return Optional.empty();
        }
    }

    public void updateNick(String oldNick, String newNick) throws SQLException {
        var statement = connection.prepareStatement(
                "UPDATE user SET nick = ? WHERE nick = ?");
        statement.setString(1, newNick);
        statement.setString(2, oldNick);
        statement.execute();
    }

    public boolean isNickExists(String nick) throws SQLException {
        return findUserByNickname(nick).isPresent();
    }

    public Optional<User> findUserByNickname(String nick) throws SQLException {
        var statement = connection.prepareStatement(
                "SELECT * FROM user WHERE nick = ?");
        statement.setString(1, nick);
        var result = statement.executeQuery();
        if (result.next()) {
            return Optional.of(buildUserFromResultSet(result));
        } else {
            return Optional.empty();
        }
    }

    private User buildUserFromResultSet(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("login"),
                resultSet.getString("password"),
                resultSet.getString("nick")
        );
    }
}
