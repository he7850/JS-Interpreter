package com.hubinstudio.jsinterpreter;

/**
 * Created by HUBIN on 2016/1/6.
 */
public class StrException extends Exception {

    String errorMessage;

    public StrException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String toString() {
        return errorMessage;
    }

    public String getMessage() {
        return errorMessage;
    }

    public static void TrueOrThrows(Boolean condition, String message) throws StrException {
        if (!condition) {
            throw new StrException(message == null ? "unknown error." : message);
        }
    }
}
