package com.example.myapplication.api;

/**
 * ApiException class that matches the Flutter app's exception handling
 */
public class ApiException extends Exception {
    private final int statusCode;

    public ApiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "ApiException: " + getMessage() + (statusCode != 0 ? " (Status code: " + statusCode + ")" : "");
    }
}