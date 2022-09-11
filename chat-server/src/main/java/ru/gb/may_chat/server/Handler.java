package ru.gb.may_chat.server;

import ru.gb.may_chat.enums.Command;
import ru.gb.may_chat.props.PropertyReader;
import ru.gb.may_chat.server.error.NickAlreadyIsBusyException;
import ru.gb.may_chat.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.AUTH_MESSAGE;
import static ru.gb.may_chat.enums.Command.AUTH_OK;
import static ru.gb.may_chat.enums.Command.CHANGE_NICK_OK;
import static ru.gb.may_chat.enums.Command.ERROR_MESSAGE;

public class Handler {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String user;

    private Long authTimeout;

    private final Object mon = new Object();

    public Handler(Socket socket, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.authTimeout = PropertyReader.getInstance().getAuthTimeout();
            System.out.println("Handler created");
        } catch (IOException e) {
            System.err.println("Connection problems with user: " + user);
        }
    }

    public void handle() {
        server.getExecutorService().submit(() -> {
            authorize();
            System.out.println("Auth done");
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    String message = in.readUTF();
                    parseMessage(message);
                } catch (IOException e) {
                    System.out.println("Connection broken with client: " + user);
                    break;
                }
            }
            server.removeHandler(this);
        });
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command) {
            case BROADCAST_MESSAGE -> server.broadcast(user, split[1]);
            case PRIVATE_MESSAGE -> server.sendPrivateMessage(user, split[1], split[2]);
            case CHANGE_NICK -> changeNick(split[1]);
            default -> System.out.println("Unknown message " + message);
        }
    }

    private void changeNick(String newNick) {
       try {
          server.getUserService().changeNick(user, newNick);
          user = newNick;
          server.updateHandlerUsername();
          send(CHANGE_NICK_OK.getCommand() + REGEX + newNick);
        } catch (NickAlreadyIsBusyException e) {
           send(ERROR_MESSAGE.getCommand() + REGEX + "This nickname already in use");
       }
    }

    private void authorize() {
        System.out.println("Authorizing");

        new Thread(() -> {
            try {
                Thread.sleep(authTimeout);
                synchronized (mon) {
                    if (user == null) {
                        socket.close();
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            while (!socket.isClosed()) {
                String msg = in.readUTF();
                if (msg.startsWith(AUTH_MESSAGE.getCommand())) {
                    String[] parsed = msg.split(REGEX);
                    String response = "";
                    String nickname = null;
                    String login = parsed[1];
                    String password = parsed[2];

                    try {
                        nickname = server.getUserService().authenticate(login, password);
                    } catch (WrongCredentialsException e) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + e.getMessage();
                        System.out.println("Wrong credentials: " + login);
                    }
                    
                    if (server.isUserAlreadyOnline(nickname)) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + "This client already connected";
                        System.out.println("Already connected");
                    }
                    
                    if (!response.equals("")) {
                        send(response);
                    } else {
                        System.out.println("Auth ok");
                        synchronized (mon) {
                            this.user = nickname;
                        }
                        send(AUTH_OK.getCommand() + REGEX + login + REGEX + nickname);
                        server.addHandler(this);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUser() {
        return user;
    }
}
