package de.marudor.simpleBots.twitter;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.common.util.concurrent.*;
import de.marudor.simpleBots.EmailHandler;
import de.marudor.simpleBots.RandomUserAgent;
import de.marudor.simpleBots.account.Account;
import de.marudor.simpleBots.account.AccountStatus;
import de.marudor.simpleBots.account.AccountType;
import de.marudor.simpleBots.database.Database;
import de.marudor.simpleBots.database.UserAgent;
import de.marudor.simpleBots.exceptions.AccountChangedException;
import de.marudor.simpleBots.exceptions.TwitterCaptchaException;
import de.marudor.simpleBots.exceptions.TwitterException;
import de.marudor.simpleBots.exceptions.TwitterLoginException;
import gnu.trove.map.hash.TCharObjectHashMap;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by marudor on 01/08/14.
 */
public class TwitterSession {
    private final ListeningExecutorService es = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private static final Logger logger = LoggerFactory.getLogger(TwitterSession.class);
    private final Account account;
    private final WebClient client;
    private boolean loggedIn;
    private boolean loggedInMobile;

    public TwitterSession(Account account) {
        this.account = account;
        this.client = new WebClient(new BrowserVersion("","",account.getUserAgent().getUserAgent(),1));
        this.client.getOptions().setCssEnabled(false);
        this.client.getOptions().setJavaScriptEnabled(false);
        this.client.getCookieManager().addCookie(new Cookie("mobile.twitter.com", "images", "false"));
        this.loggedIn = false;
        this.loggedInMobile = false;
    }

    private TwitterSession() {
        account = null;
        this.client = new WebClient(RandomUserAgent.getRandomBrowserVersion());
        this.client.getOptions().setCssEnabled(false);
        this.client.getOptions().setJavaScriptEnabled(false);
        this.client.getCookieManager().addCookie(new Cookie("mobile.twitter.com", "images", "false"));
    }

    private static String generatePassword() {
        return UUID.randomUUID().toString();
    }

    /**
     *
     * @param accountDetails Map containing the details for registration.
     *                       Needs at least a 'u' (username)
     *                       'e' (email) is optional otherwise defaults to tw-{username}@marudor.de
     *                       'p' (password) is optional otherwise defaults to a random one
     * @return null if unsuccessfull, otherwise the newly registred Account.
     */
    public static Account registerTwitter(TCharObjectHashMap<String> accountDetails) {
        UserAgent userAgent = RandomUserAgent.getRandomUserAgent();
        WebClient client = new WebClient(RandomUserAgent.getBrowserVersion(userAgent));
        HtmlPage registerPage;
        String username = accountDetails.get('u');
        String email = accountDetails.get('e');
        if (email == null) email = EmailHandler.DefaultEmail(username);
        String password = accountDetails.get('p');
        if (password == null) password = generatePassword();
        logger.info("Registering "+username);
        try {
            registerPage = client.getPage("https://mobile.twitter.com/signup");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        HtmlForm form = registerPage.getFirstByXPath("//div[@class=\"body\"]/form");

        form.getInputByName("oauth_signup_client[fullname]").setValueAttribute(username);
        form.getInputByName("oauth_signup_client[phone_number]").setValueAttribute(email);
        form.getInputByName("oauth_signup_client[password]").setValueAttribute(password);

        try {
            registerPage = form.getInputByName("commit").click();
        } catch (IOException e) {
            return null;
        }
        form = registerPage.getForms().get(0);
        if (form.getFirstByXPath("//div[contains(@class,\"invalid-field\")]") != null) return null;
        username = form.getInputByName("settings[screen_name]").getValueAttribute();
        try {
            form.getInputByName("commit").click();
            logger.info("Completed Register");
        } catch (IOException e) {
            return null;
        }

        Account account  = new Account(null, username, password, email, userAgent, AccountType.Twitter);
        account.setStatus(AccountStatus.REGISTRED);
        Database.save(account);
        return account;
    }

    /**
     * Tries to verify the Account attached to this Session using the code provided
     * @param code The code to verify the account
     * @return true if we succeed. false otherwise.
     * @throws TwitterLoginException
     */
    public boolean verifyMail(String code) throws TwitterLoginException, AccountChangedException, TwitterCaptchaException, TwitterException {
        logger.debug("Verifying "+account.getUsername()+" with "+code);
        if (account.getStatus().lessThan(AccountStatus.APPROVED)) {
            logger.warn(account.getUsername()+" is already verified.");
            return true;
        }
        try {
            Page verifyPage = client.getPage("https://twitter.com/account/confirm_email/"+account.getUsername()+"/"+code);
            if (verifyPage.getUrl().getPath().equals("/login"))
                this.login((HtmlPage)verifyPage);
            if (this.loggedIn) {
                account.setStatus(AccountStatus.APPROVED);
                Database.save(account);
                return true;
            }
            logger.warn("Coudn't login to verify?.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lets login!
     * @throws TwitterLoginException Well we failed.
     */
    boolean login() throws TwitterLoginException, AccountChangedException, TwitterCaptchaException, TwitterException { return login(null); }
    boolean login(HtmlPage page) throws TwitterLoginException, AccountChangedException, TwitterCaptchaException, TwitterException {
        try {
            if (page == null)
                page = client.getPage("https://twitter.com/login");
            HtmlForm loginForm = page.getForms().get(1);
            loginForm.getInputByName("session[username_or_email]").setValueAttribute(account.getUsername());
            loginForm.getInputByName("session[password]").setValueAttribute(account.getPassword());
            page = loginForm.getElementsByAttribute("button","type","submit").get(0).click();
            if (page.getUrl().getHost().contains("mobile")) {
                logger.error("UserAgent ("+client.getBrowserVersion().getUserAgent()+") is old and incompatible. Trying new");
                UserAgent newAgent = this.account.getUserAgent();
                while (newAgent == this.account.getUserAgent())
                    newAgent=RandomUserAgent.getRandomUserAgent();
                this.account.setUserAgent(newAgent);
                Database.save(this.account);
                UserAgent.deleteByUserAgent(client.getBrowserVersion().getUserAgent());
                throw new AccountChangedException(this.account);
            }
            if (page.getElementById("captcha-challenge-form") != null) {
                loginMobile();
                return false;
            }
            //if (!page.getUrl().getPath().equals("/"))
            //    throw new IOException();
            logger.debug("Logged in");

        } catch (IOException e) {
            logger.error("Couldn't login " + account.getUsername());
            throw new TwitterLoginException(e);
        }
        loggedIn = true;
        return true;
    }

    public String tweetMobile(String message, String mediaId, HtmlPage page) throws TwitterException, TwitterLoginException {
        try {
            if (!loggedInMobile)
                loginMobile();
            if (page == null)
                page = client.getPage("https://mobile.twitter.com/compose/tweet");
            String authToken = page.getElementByName("authenticity_token").getAttribute("value");
            List<NameValuePair> data = new ArrayList<>();
            data.add(new NameValuePair("tweet[text]",message));
            data.add(new NameValuePair("authenticity_token", authToken));
            data.add(new NameValuePair("commit","Tweet"));
            WebRequest request = new WebRequest(new URL("https://mobile.twitter.com/"), HttpMethod.POST);
            request.setRequestParameters(data);
            Map<String, String> header = new THashMap<>();
            header.put("Referer","https://mobile.twitter.com/compose/tweet");
            header.put("Origin","https://mobile.twitter.com/");
            page = client.getPage(request);
            logger.error(page.getWebResponse().getContentAsString());
            return page.getWebResponse().getContentAsString();
        } catch (IOException e) {
            throw new TwitterException(e);
        }

    }

    private void loginMobile() throws TwitterLoginException {
        try {
            HtmlPage page = client.getPage("https://mobile.twitter.com/i/guest");
            String authToken = page.getElementByName("authenticity_token").getAttribute("value");
            WebRequest request = new WebRequest(new URL("https://mobile.twitter.com/session"));
            List<NameValuePair> data = new ArrayList<>();
            data.add(new NameValuePair("username",this.account.getUsername()));
            data.add(new NameValuePair("password",this.account.getPassword()));
            data.add(new NameValuePair("authenticity_token", authToken));
            data.add(new NameValuePair("commit","Sign in"));
            request.setRequestParameters(data);
            page = client.getPage(request);
            loggedInMobile = true;
            //logger.error(page.getWebResponse().getContentAsString());
        } catch (IOException e) {
            logger.error("Couldn't login " + account.getUsername());
            throw new TwitterLoginException(e);
        }
    }


    public void tweet(String message) throws TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        tweet(message, null);
    }
    public void tweet(String message, String mediaId) throws TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!loggedIn)
            if (!login()) {
                tweetMobile(message, mediaId, null);
                return;
            }
        HtmlPage page;
        try {
            page = client.getPage("https://twitter.com");
        } catch (IOException e) {
            throw new TwitterException(e);
        }
        tweet(message, mediaId, page, true);
    }
    /**
     * Directly address the internal Twitter API.
     * This API is not RateLimited - you have to be signed in though.
     *
     * @param message Message to Tweet
     * @param mediaId mediaId of a media already uploaded to twitter.
     * @param page Page to use - should be logged in!
     * @param tryAgain Should we try again if twitter refuses=
     * @return Response of the Post Request
     * @throws TwitterLoginException
     * @throws TwitterException
     */
    public void tweet(String message, String mediaId, HtmlPage page, boolean tryAgain) throws TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!loggedIn)
            if (!login()) {
                tweetMobile(message, mediaId, null);
                return;
            }
        try {
            logger.debug(MessageFormat.format("Tweeting ({0}, {1})", message, mediaId));
            String authToken = page.getElementByName("authenticity_token").getAttribute("value");
            WebRequest request = new WebRequest(new URL("https://twitter.com/i/tweet/create"), HttpMethod.POST);
            Map<String, String> header = new THashMap<>();
            header.put("Origin", "https://twitter.com");
            header.put("Referer", "https://twitter.com");
            header.put("Accept", "application/json, text/javascript, */*; q=0.01");
            header.put("X-Requested-With", "XMLHttpRequest");
            header.put("DNT", "1");
            header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            request.setAdditionalHeaders(header);
            request.setCharset("UTF-8");
            List<NameValuePair> data = new ArrayList<>();
            logger.warn("authToken: " + authToken);
            data.add(new NameValuePair("authenticity_token", authToken));
            data.add(new NameValuePair("place_id", ""));
            data.add(new NameValuePair("status", message));
            data.add(new NameValuePair("tagged_users", ""));
            if (mediaId != null)
                data.add(new NameValuePair("media_ids", mediaId));
            request.setRequestParameters(data);
            Page p = client.getPage(request);
            for (NameValuePair rh : p.getWebResponse().getResponseHeaders())
                logger.warn(rh.getName() + " : " + rh.getValue());
            return;
        } catch (FailingHttpStatusCodeException e) {
            if (!tryAgain)
                throw new TwitterException(e);
            logger.error(e.getResponse().getContentAsString());
            if (e.getStatusCode()==403) {
                try {
                    page = (HtmlPage) page.refresh();
                } catch (IOException e1) {
                    throw new TwitterException(e);
                }
                tweet(message,mediaId,page,false);
                return;
            }
            throw new TwitterException(e);
        } catch (IOException e) {
            throw new TwitterException(e);
        }
    }

    public String updateProfile(String nickname, String bio, String location, String homepage) throws TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!loggedIn)
            this.login();
        try {
            HtmlPage page = client.getPage("https://twitter.com/marujavafoo");
            String authToken = page.getElementByName("authenticity_token").getAttribute("value");
            WebRequest request = new WebRequest(new URL("https://twitter.com/i/profiles/update"), HttpMethod.POST);
            Map<String,String> header = new THashMap<>();
            header.put("origin","https://twitter.com");
            header.put("x-requested-with","XMLHttpRequest");
            request.setAdditionalHeaders(header);
            request.setCharset("UTF-8");
            List<NameValuePair> data = new ArrayList<>();
            data.add(new NameValuePair("authenticity_token",authToken));
            data.add(new NameValuePair("page_context","me"));
            data.add(new NameValuePair("section_context","section"));
            if (nickname != null)
                data.add(new NameValuePair("user[name]", nickname));
            if (bio != null)
                data.add(new NameValuePair("user[description]", bio));
            if (location != null)
                data.add(new NameValuePair("user[location]", location));
            if (homepage != null)
                data.add(new NameValuePair("user[url]", homepage));
            request.setRequestParameters(data);
            Page p = client.getPage(request);
            return p.getWebResponse().getContentAsString();
        } catch (IOException e) {
            throw new TwitterException(e);
        }

    }

    private boolean addTwitterUserToList(HtmlPage page, WebClient client, List<TwitterUser> follower, FutureCallback<HtmlPage> callback) {
        HtmlAnchor next = page.getFirstByXPath("//div[@class=\"w-button-more\"]/a");
        if (next != null) {
            ListenableFuture<HtmlPage> ft = es.submit(() -> client.getPage("https://mobile.twitter.com" + next.getHrefAttribute()));
            Futures.addCallback(ft, callback);
        }
        List<HtmlAnchor> rawFollower = (List<HtmlAnchor>) page.getByXPath("//a[@data-scribe-action=\"profile_click\"]");
        for (HtmlAnchor a : rawFollower) {
            String href = a.getHrefAttribute().substring(1);
            String username = href.substring(0, href.length() - 4);
            String screenname = a.getFirstElementChild().getTextContent();
            follower.add(new TwitterUser(username, screenname));
        }
        return next == null;
    }

    private List<TwitterUser> getFollowerForUserInt(TwitterUser user) {
        Object mon = new Object();
        FutureCallback<HtmlPage> callback =new FutureCallback<HtmlPage>() {
            @Override
            public void onSuccess(HtmlPage page) {
                if (addTwitterUserToList(page, client, user.getFollower(), this))
                    synchronized (mon) {
                        mon.notify();
                    }
            }
            @Override
            public void onFailure(Throwable t) {

            }
        };
        ListenableFuture<HtmlPage> ft = es.submit(()->client.getPage("https://mobile.twitter.com/"+user.getUsername()+"/followers"));
        Futures.addCallback(ft, callback);
        synchronized (mon) {
            try {
                mon.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return user.getFollower();
    }

    private List<TwitterUser> getFollowingForUserInt(TwitterUser user) {
        Object mon = new Object();
        FutureCallback<HtmlPage> callback = new FutureCallback<HtmlPage>() {
            @Override
            public void onSuccess(HtmlPage page) {
                if (addTwitterUserToList(page,client,user.getFollowing(),this))
                    synchronized (mon) {
                        mon.notify();
                    }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        };
        ListenableFuture<HtmlPage> ft = es.submit(()->client.getPage("https://mobile.twitter.com/"+user.getUsername()+"/following"));
        Futures.addCallback(ft,callback);
        synchronized (mon) {
            try {
                mon.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return user.getFollowing();
    }

    public static List<TwitterUser> getFollowingForUser(TwitterUser user) {
        try {
            return TwitterAPI.getFollowingForUser(user);
        } catch (RateLimitException e) {
            return new TwitterSession().getFollowingForUserInt(user);
        }
    }

    /**
     *
     * @param user A TwitterUser, needs username.
     * @return List of Follower for specific User
     * Uses TwitterAPI if possible. Otherwise slow webcrawlin gmethod
     */
    public static List<TwitterUser> getFollowerForUser(TwitterUser user) {
        try {
            return TwitterAPI.getFollowerForUser(user);
        } catch (RateLimitException e) {
            return new TwitterSession().getFollowerForUserInt(user);
        }
    }


    public String uploadMedia(String base64Media) throws IOException, TwitterLoginException, AccountChangedException, TwitterCaptchaException, TwitterException {
        if (!loggedIn)
            this.login();
        WebRequest request = new WebRequest(new URL("https://upload.twitter.com/i/media/upload.iframe"), HttpMethod.POST);
        List<NameValuePair> data = new ArrayList<>();
        data.add(new NameValuePair("media", base64Media));
        request.setRequestParameters(data);
        Page page = client.getPage(request);
        String responseBody = page.getWebResponse().getContentAsString();
        Pattern p = Pattern.compile(".*snf:(\\d+).*",Pattern.DOTALL);
        Matcher m = p.matcher(responseBody);
        m.find();
        return m.group(1);
    }
}