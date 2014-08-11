package de.marudor.simpleBots.twitter;

import de.marudor.simpleBots.account.Account;

/**
 * Created by marudor on 03/08/14.
 */
public class Tweet {
    private final String tweet;
    private final Account account;

    public Tweet(String tweet, Account account) {
        this.tweet = tweet;
        this.account = account;
    }

    public String getTweet() {
        return tweet;
    }

    public Account getAccount() {
        return account;
    }
}
