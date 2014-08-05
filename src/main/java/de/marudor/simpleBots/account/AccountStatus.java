package de.marudor.simpleBots.account;

/**
 * Created by marudor on 28/07/14.
 */

public enum AccountStatus {
    NONE,
    REGISTRED,
    APPROVED,
    READY,
    BANNED;

    public boolean lessThan(AccountStatus other) {
        return this.ordinal() > other.ordinal();
    }

    public boolean greaterThan(AccountStatus other) {
        return this.ordinal() < other.ordinal();
    }

    public boolean greaterEqualThan(AccountStatus other) { return this.ordinal() <= other.ordinal(); }

    public boolean lessEqualThan(AccountStatus other) { return this.ordinal() >= other.ordinal(); }
}
