package ru.gb.may_chat.server.error;

public class UserNotFoundException extends IllegalArgumentException {
    public UserNotFoundException() {
    }

    public UserNotFoundException(String s) {
        super(s);
    }
}
