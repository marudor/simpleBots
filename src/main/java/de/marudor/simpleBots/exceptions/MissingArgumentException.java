package de.marudor.simpleBots.exceptions;

/**
 * Created by marudor on 03/08/14.
 */

public class MissingArgumentException extends Exception {
    public MissingArgumentException(String message) {
        super(message);
    }

    public MissingArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
