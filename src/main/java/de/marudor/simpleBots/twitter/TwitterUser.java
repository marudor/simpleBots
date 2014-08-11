package de.marudor.simpleBots.twitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marudor on 01/08/14.
 */
class TwitterUser {
    private final String username;
    private final String screenName;
    private final List<TwitterUser> follower = new ArrayList<>();
    private final List<TwitterUser> following = new ArrayList<>();

    public String getUsername() {
        return username;
    }

    public String getScreenName() {
        return screenName;
    }

    public List<TwitterUser> getFollower() {
        return follower;
    }

    public List<TwitterUser> getFollowing() {
        return following;
    }

    public TwitterUser(String username, String screenName) {
        this.username = username;
        this.screenName = screenName;
    }

    public void calculateFollower() {
        TwitterSession.getFollowerForUser(this);
    }

    public void calculateFollowing() {
        TwitterSession.getFollowingForUser(this);
    }
}
