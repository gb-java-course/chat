package ru.gb.may_chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gb.may_chat.props.PropertyReader;
import ru.gb.may_chat.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.BROADCAST_MESSAGE;
import static ru.gb.may_chat.enums.Command.LIST_USERS;
import static ru.gb.may_chat.enums.Command.PRIVATE_MESSAGE;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final int port;

    private List<Handler> handlers;
    private UserService userService;

    private ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Server(UserService userService) {
        this.userService = userService;
        this.handlers = new ArrayList<>();
        port = PropertyReader.getInstance().getPort();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Server start!");
            userService.start();
            while (true) {
                log.info("Waiting for connection......");
                Socket socket = serverSocket.accept();
                log.info("Client connected");
                Handler handler = new Handler(socket, this);
                handler.handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String from, String message) {
        String msg = BROADCAST_MESSAGE.getCommand() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }

    public void sendPrivateMessage(String from, String to, String message) {
        String msg = PRIVATE_MESSAGE.getCommand() + REGEX + String.format("[%s]: %s", from, message);
        handlers.stream()
                .filter(it -> it.getUser().equals(to))
                .findFirst()
                .orElseThrow()
                .send(msg);
    }

    public UserService getUserService() {
        return userService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    public synchronized boolean isUserAlreadyOnline(String nick) {
        for (Handler handler : handlers) {
            if (handler.getUser().equals(nick)) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized void addHandler(Handler handler) {
        this.handlers.add(handler);
        sendContacts();
    }

    public synchronized void removeHandler(Handler handler) {
        this.handlers.remove(handler);
        sendContacts();
    }

    public synchronized void updateHandlerUsername() {
        sendContacts();
    }

    private void shutdown() {
        userService.stop();
        executorService.shutdown();
    }

    private void sendContacts() {
       String contacts = handlers.stream()
                .map(Handler::getUser)
                .collect(Collectors.joining(REGEX));
       String msg = LIST_USERS.getCommand() + REGEX + contacts;

        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }
}
