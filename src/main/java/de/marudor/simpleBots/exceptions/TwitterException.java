package de.marudor.simpleBots.exceptions;

/**
 * Created by marudor on 03/08/14.
 */
public class TwitterException extends Exception {
    public TwitterException(Exception e) {
        super(e);
    }

    public TwitterException(String s) {
        super(s);
    }
}
