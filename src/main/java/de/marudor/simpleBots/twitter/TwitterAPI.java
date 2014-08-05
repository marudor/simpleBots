package de.marudor.simpleBots.twitter;


import twitter4j.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by marudor on 01/08/14.
 */
public class TwitterAPI {
    private final static Twitter twitter = TwitterFactory.getSingleton();
    private final static ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
    private static boolean canUse = true;

    static {
        twitter.addRateLimitStatusListener(new RateLimitStatusListener() {
            @Override
            public void onRateLimitStatus(RateLimitStatusEvent event) {
            }

            @Override
            public void onRateLimitReached(RateLimitStatusEvent event) {
                canUse = false;
                es.schedule(()->canUse=true, event.getRateLimitStatus().getSecondsUntilReset(), TimeUnit.SECONDS);
            }
        });
    }

    /**
     * Using Twitter API to get all Follower for this user.
     * @param user
     * @return List of all Follower for this user
     * @throws RateLimitException
     */
    public static List<TwitterUser> getFollowerForUser(TwitterUser user) throws RateLimitException {
        if (!canUse)
            throw new RateLimitException();
        List<TwitterUser> result = user.getFollower();
        try {
            PagableResponseList<User> follower = twitter.getFollowersList(user.getUsername(), -1, 5000, true, false);
            while (follower.hasNext()) {
                follower.forEach((u) -> result.add(new TwitterUser(u.getName(), u.getScreenName())));
                follower = twitter.getFollowersList(user.getUsername(), follower.getNextCursor(), 5000, true, false);
            }
            follower.forEach((u) -> result.add(new TwitterUser(u.getName(), u.getScreenName())));
        } catch (TwitterException e) {
            if (e.exceededRateLimitation()) {
                es.schedule(()->canUse=true, e.getRateLimitStatus().getSecondsUntilReset(), TimeUnit.SECONDS);
                canUse=false;
                throw new RateLimitException();
            }
        }
        return result;
    }

    /**
     * Using Twitter API to get all user this user is Following
     * @param user
     * @return List of all User this User is following
     * @throws RateLimitException
     */
    public static List<TwitterUser> getFollowingForUser(TwitterUser user) throws RateLimitException {
        if (!canUse)
            throw new RateLimitException();
        List<TwitterUser> result = user.getFollowing();
        try {
            PagableResponseList<User> following = twitter.getFriendsList(user.getUsername(), -1, 5000, true, false);
            following.forEach((u) -> result.add(new TwitterUser(u.getName(), u.getScreenName())));
            while (following.hasNext()) {
                following.forEach((u) -> result.add(new TwitterUser(u.getName(), u.getScreenName())));
                following = twitter.getFriendsList(user.getUsername(), -1, 5000, true, false);
            }
        } catch (TwitterException e) {
            if (e.exceededRateLimitation()) {
                es.schedule(()->canUse=true, e.getRateLimitStatus().getSecondsUntilReset(), TimeUnit.SECONDS);
                canUse=false;
                throw new RateLimitException();
            }
        }
        return result;
    }
}

class RateLimitException extends TwitterException {

    public RateLimitException() {
        super("Rate limit reached");
    }
}
