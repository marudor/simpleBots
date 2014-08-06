package de.marudor.simpleBots.exceptions;

/**
 * Created by marudor on 03/08/14.
 */
public class TwitterException extends Exception {
    public TwitterException(String message, Exception e) { super(message,e); }
    public TwitterException(Exception e) {
        super(e);
    }
}
