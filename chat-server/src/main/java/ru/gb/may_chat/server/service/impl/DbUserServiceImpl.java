package ru.gb.may_chat.server.service.impl;

import ru.gb.may_chat.server.dao.UserDao;
import ru.gb.may_chat.server.error.NickAlreadyIsBusyException;
import ru.gb.may_chat.server.error.WrongCredentialsException;
import ru.gb.may_chat.server.model.User;
import ru.gb.may_chat.server.service.UserService;

import java.sql.SQLException;

public class DbUserServiceImpl implements UserService {

    private final UserDao userDao;

    public DbUserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void start() {
        try {
            userDao.connect();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            userDao.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String authenticate(String login, String password) {
        System.out.println("Auth log: " + login + " pass: " + password);

        try {
            return userDao.getNickByLoginAndPassword(login, password)
                    .orElseThrow(() -> new WrongCredentialsException("Wrong login or password"));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("SQL Error", e);
        }
    }

    @Override
    public String changeNick(String oldNick, String newNick) {
        try {
            if (userDao.isNickExists(newNick)) {
                throw new NickAlreadyIsBusyException();
            }
            userDao.updateNick(oldNick, newNick);
            return newNick;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("SQL Error", e);
        }
    }

    @Override
    public User createUser(String login, String password, String nick) {
        return null; //TODO
    }

    @Override
    public void deleteUser(String login, String password) {
        //@TODO
    }

    @Override
    public void changePassword(String login, String oldPassword, String newPassword) {
        //@TODO
    }
}
