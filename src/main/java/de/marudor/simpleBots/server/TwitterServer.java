package de.marudor.simpleBots.server;

import de.marudor.simpleBots.account.Account;
import de.marudor.simpleBots.account.AccountStatus;
import de.marudor.simpleBots.authentication.Authenticator;
import de.marudor.simpleBots.authentication.Task;
import de.marudor.simpleBots.exceptions.*;
import de.marudor.simpleBots.twitter.TwitterSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.ws.WebServiceContext;
import java.util.HashMap;
import java.util.List;

/**
 * Created by marudor on 01/08/14.
 */

@WebService()
public class TwitterServer {
    @Resource
    private WebServiceContext wsctx;
    private final Logger logger = LoggerFactory.getLogger(TwitterServer.class);

    @WebMethod
    public Account register(
            @WebParam(name="username") @XmlElement(required=true) String username,
            @WebParam(name="email") String email,
            @WebParam(name="password") String password) throws InvalidLoginException, NotAuthorizedException {
        if (!Authenticator.isAllowed(wsctx, Task.REGISTER)) throw new NotAuthorizedException();
        HashMap<String,String> accountDetails = new HashMap<String, String>();
        accountDetails.put("username",username);
        logger.debug(username);
        logger.debug(email);
        logger.debug(password);
        if (email!=null)
            accountDetails.put("email",email);
        if (password != null)
            accountDetails.put("password",password);
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
        if (!Authenticator.isAllowed(wsctx, Task.GET_STATUS)) throw new NotAuthorizedException();
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
     */
    @WebMethod
    public String tweet(@WebParam(name="tweetText") String tweetText,
                         @WebParam(name="account") Account account,
                         @WebParam(name="accountName") String accountName) throws InvalidLoginException, NotAuthorizedException, NotFoundException, TwitterLoginException, TwitterException {
        if (!Authenticator.isAllowed(wsctx, Task.REGISTER)) throw new NotAuthorizedException();
        if (account == null)
            account = Account.getByName(accountName);
        return new TwitterSession(account).tweet(tweetText);
    }

    @WebMethod
    public String updateProfile(@WebParam(name="account") Account account,
                                @WebParam(name="accountName") String accountName,
                                @WebParam(name="nickname") String nickname,
                                @WebParam(name="bio") String bio,
                                @WebParam(name="location") String location,
                                @WebParam(name="homepage") String homepage) throws NotAuthorizedException, InvalidLoginException, NotFoundException, TwitterLoginException, TwitterException {
        if (!Authenticator.isAllowed(wsctx, Task.REGISTER)) throw new NotAuthorizedException();
        if (account == null)
            account = Account.getByName(accountName);
        return new TwitterSession(account).updateProfile(nickname, bio, location, homepage);
    }
}
