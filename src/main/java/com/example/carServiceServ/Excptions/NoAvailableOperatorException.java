package com.example.carServiceServ.Excptions;

public class NoAvailableOperatorException extends RuntimeException {
    public NoAvailableOperatorException(String message) {
        super(message);
    }

    public NoAvailableOperatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
