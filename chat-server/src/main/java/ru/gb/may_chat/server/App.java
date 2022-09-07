package ru.gb.may_chat.server;

import ru.gb.may_chat.server.dao.UserDao;
import ru.gb.may_chat.server.service.impl.DbUserServiceImpl;

public class App {
    public static void main(String[] args) {
        new Server(new DbUserServiceImpl(new UserDao())).start();
    }
}
