package de.marudor.simpleBots;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import de.marudor.simpleBots.database.UserAgent;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * Created by marudor on 28/07/14.
 */
public class RandomUserAgent {
    private static final TObjectDoubleHashMap<UserAgent.BrowserType> freqMap = new TObjectDoubleHashMap<>();
    private static final TCharObjectHashMap<String[]> uaMap = new TCharObjectHashMap<>();

    static {

        freqMap.put(UserAgent.BrowserType.IE, 11.8);
        freqMap.put(UserAgent.BrowserType.FIREFOX, 40.0);
        freqMap.put(UserAgent.BrowserType.CHROME, 92.9);
        freqMap.put(UserAgent.BrowserType.SAFARI, 96.8);
        freqMap.put(UserAgent.BrowserType.OPERA, 98.6);
    }

    public static UserAgent getRandomUserAgent() {

        double rand = Math.random() * 100;
        for (UserAgent.BrowserType key : freqMap.keySet()) {
            if (rand <= freqMap.get(key))
                return UserAgent.getRandomByType(key);
        }
        return null;
    }

    public static BrowserVersion getBrowserVersion(UserAgent userAgent) {
        return new BrowserVersion("","",userAgent.getUserAgent(),0);
    }

    public static BrowserVersion getRandomBrowserVersion() {
        return getBrowserVersion(getRandomUserAgent());
    }
}
