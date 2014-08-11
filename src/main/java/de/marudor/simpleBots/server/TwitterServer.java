package de.marudor.simpleBots.server;

import de.marudor.simpleBots.account.Account;
import de.marudor.simpleBots.account.AccountStatus;
import de.marudor.simpleBots.authentication.AuthUser;
import de.marudor.simpleBots.authentication.Authenticator;
import de.marudor.simpleBots.authentication.Task;
import de.marudor.simpleBots.exceptions.*;
import de.marudor.simpleBots.twitter.TwitterSession;
import gnu.trove.map.hash.TCharObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.ws.WebServiceContext;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by marudor on 01/08/14.
 */

@WebService()
public class TwitterServer {
    @Resource
    private WebServiceContext wsctx;
    private final Logger logger = LoggerFactory.getLogger(TwitterServer.class);
    private static final ExecutorService es = new ThreadPoolExecutor(0, 20, 30L, TimeUnit.SECONDS,new SynchronousQueue<>());

    @WebMethod
    public Account register(
            @WebParam(name="username") @XmlElement(required=true) String username,
            @WebParam(name="email") String email,
            @WebParam(name="password") String password) throws InvalidLoginException, NotAuthorizedException {
        if (!Authenticator.isAllowed(wsctx, Task.REGISTER)) throw new NotAuthorizedException();
        TCharObjectHashMap<String> accountDetails = new TCharObjectHashMap<>();
        accountDetails.put('u',username);
        if (email!=null)
            accountDetails.put('e',email);
        if (password != null)
            accountDetails.put('p',password);
        logger.info("Creating account with "+accountDetails.toString());
        return TwitterSession.registerTwitter(accountDetails);
    }

    /**
     * Provide either accountName or account. account is preferred.
     * @param accountName accountName of account to use
     * @param account account to use
     * @return true if account can be used
     * @throws MissingArgumentException
     * @throws NotFoundException
     */
    @WebMethod
    public boolean isUseable(
            @WebParam(name="accountName") String accountName,
            @WebParam(name="account") Account account) throws MissingArgumentException, NotFoundException, InvalidLoginException, NotAuthorizedException {
        if (accountName==null && account==null)
            throw new MissingArgumentException("Either username or account has to be provided");
        if (!Authenticator.isAllowed(wsctx, Task.GET_STATUS))
            throw new NotAuthorizedException();
        if (account != null)
            return account.isUseable();
        return Account.getByName(accountName).isUseable();
    }

    @WebMethod
    public AccountStatus getStatus(
            @WebParam(name = "accountName") String accountName,
            @WebParam(name = "account") Account account) throws MissingArgumentException, NotAuthorizedException, NotFoundException, InvalidLoginException {
        if (accountName==null && account==null)
            throw new MissingArgumentException("Either username or account has to be provided");
        if (!Authenticator.isAllowed(wsctx, Task.GET_STATUS)) throw new NotAuthorizedException();
        if (account != null)
            return account.getStatus();
        return Account.getByName(accountName).getStatus();
    }

    @WebMethod
    public Account getAccount(
            @WebParam(name="accountName") @XmlElement(required = true) String accountName) throws InvalidLoginException, NotAuthorizedException, NotFoundException {
        if (!Authenticator.isAllowed(wsctx, Task.GET_ACCOUNT)) throw new NotAuthorizedException();
        return Account.getByName(accountName);
    }

    @WebMethod
    public List<Account> getAccountsByStatus(@WebParam(name="status") @XmlElement(required = true) AccountStatus status) throws NotAuthorizedException, InvalidLoginException {
        if (!Authenticator.isAllowed(wsctx, Task.GET_ACCOUNTS)) throw new NotAuthorizedException();
        return Account.getAllByStatus(status);
    }

    @WebMethod
    public List<Account> getAllAccounts(@WebParam(name="includeBanned") @XmlElement(defaultValue = "false") boolean includeBanned) throws InvalidLoginException, NotAuthorizedException {
        if (!Authenticator.isAllowed(wsctx, Task.GET_ACCOUNTS)) throw new NotAuthorizedException();
        return Account.getAll(includeBanned);
    }

    /**
     * Provide either account or accountName. account is preferred.
     *
     * @param tweetText Text to Tweet
     * @param account account to use
     * @param accountName accountName of account to use
     * @param mobile Should we use Mobile Twitter?
     */
    @WebMethod
    public void tweet(@WebParam(name="tweetText") @XmlElement(required = true) String tweetText,
                         @WebParam(name="account") Account account,
                         @WebParam(name="accountName") String accountName,
                         @WebParam(name="mobile") @XmlElement(defaultValue = "false") boolean mobile) throws InvalidLoginException, NotAuthorizedException, NotFoundException, TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!Authenticator.isAllowed(wsctx, Task.TWEET)) throw new NotAuthorizedException();
        if (account == null)
            account = Account.getByName(accountName);
        TwitterSession s = new TwitterSession(account);
        if (mobile)
            s.tweetMobile(tweetText, null, null);
        else
            s.tweet(tweetText);
    }

    @WebMethod
    public void tweetWithMedia(@WebParam(name="account") Account account,
                                 @WebParam(name="accountName") String accountName,
                                 @WebParam(name="tweetText") @XmlElement(required = true) String tweetText,
                                 @WebParam(name="base64Media") @XmlElement(required = true) String base64Media,
                                 @WebParam(name="mobile") @XmlElement(defaultValue = "false") boolean mobile) throws InvalidLoginException, NotAuthorizedException, NotFoundException, IOException, TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!Authenticator.isAllowed(wsctx, Task.TWEET_WITH_MEDIA)) throw new NotAuthorizedException();
        if (account == null)
            account = Account.getByName(accountName);
        TwitterSession session = new TwitterSession(account);
        String mediaId = session.uploadMedia(base64Media);
        logger.debug("mediaid: "+mediaId);
        if (mobile)
            session.tweetMobile(tweetText, mediaId, null);
        else
            session.tweet(tweetText,mediaId);
    }

    @WebMethod
    public String updateProfile(@WebParam(name="account") Account account,
                                @WebParam(name="accountName") String accountName,
                                @WebParam(name="nickname") String nickname,
                                @WebParam(name="bio") String bio,
                                @WebParam(name="location") String location,
                                @WebParam(name="homepage") String homepage) throws NotAuthorizedException, InvalidLoginException, NotFoundException, TwitterLoginException, TwitterException, AccountChangedException, TwitterCaptchaException {
        if (!Authenticator.isAllowed(wsctx, Task.UPDATE_PROFILE)) throw new NotAuthorizedException();
        if (account == null)
            account = Account.getByName(accountName);
        return new TwitterSession(account).updateProfile(nickname, bio, location, homepage);
    }

    @WebMethod
    public void createNewAPIAccount(@WebParam(name="username") @XmlElement(required = true) String username,
                                      @WebParam(name="password") @XmlElement(required = true) String password,
                                      @WebParam(name="admin") @XmlElement(required=true,defaultValue = "false") boolean admin) throws InvalidLoginException, NotAuthorizedException {
        if (!Authenticator.isAllowed(wsctx, Task.CREATE_API_USER)) throw new NotAuthorizedException();
        AuthUser.createNew(username, password, admin);
    }

    @WebMethod
    public void deleteAPIAccount(@WebParam(name="username") @XmlElement(required = true) String username) throws NotAuthorizedException, InvalidLoginException, NotFoundException, SameAuthUserException {
        if (!Authenticator.isAllowed(wsctx, Task.DELETE_API_USER)) throw new NotAuthorizedException();
        if (AuthUser.isSame(wsctx, username)) throw new SameAuthUserException();
        AuthUser.delete(username);
    }

    @WebMethod
    public void registerMassAccount(@WebParam(name="baseName") String baseName,
                                  @WebParam(name="number") @XmlElement(required = true) int number) throws InvalidLoginException, NotAuthorizedException, AlreadyWorkingException {
        if (!Authenticator.isAllowed(wsctx, Task.REGISTER)) throw new NotAuthorizedException();
        if (baseName == null)
            es.submit(()-> {
                for (int i = 0; i < number; i++) {
                    TCharObjectHashMap<String> data = new TCharObjectHashMap<>();
                    data.put('u', org.apache.commons.lang3.RandomStringUtils.random(10,true,true));
                    TwitterSession.registerTwitter(data);
                }
            });
        else
            es.submit(()-> {
                for (int i = 1; i <= number; i++) {
                    TCharObjectHashMap<String> data = new TCharObjectHashMap<>();
                    data.put('u',baseName+i);
                    TwitterSession.registerTwitter(data);
                }
            });
    }

    @WebMethod
    public void massTweet(@WebParam(name="tweetText") @XmlElement(required = true) String tweetText,
                          @WebParam(name="base64media") String media,
                          @WebParam(name="numberOfTweets") @XmlElement(required = true) int number) throws InvalidLoginException, NotAuthorizedException, AlreadyWorkingException {
        if (!Authenticator.isAllowed(wsctx, Task.MASS_TWEET)) throw new NotAuthorizedException();
        es.submit(()-> {
            es.submit(()-> {
                List<Account> accounts = Account.getRandomX(number);
                for (Account a : Account.getRandomX(number))
                try {
                    if (media == null)
                        new TwitterSession(a).tweet(tweetText);
                    else {
                        TwitterSession s = new TwitterSession(a);
                        String mediaId = s.uploadMedia(media);
                        s.tweet(tweetText,mediaId);
                    }
                } catch (Exception ignored) {}
            });
        });
    }
}
