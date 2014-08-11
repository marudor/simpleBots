package de.marudor.simpleBots.exceptions;

import de.marudor.simpleBots.account.Account;

/**
 * Created by marudor on 10/08/14.
 */
public class AccountChangedException extends Exception {
    private Account account;
    public AccountChangedException(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
