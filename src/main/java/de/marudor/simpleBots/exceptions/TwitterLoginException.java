package de.marudor.simpleBots.exceptions;

/**
 * Created by marudor on 03/08/14.
 */
public class TwitterLoginException extends Exception {
    public TwitterLoginException() {
    }

    public TwitterLoginException(Exception e) {
        super(e);
    }
}
