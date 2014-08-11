package de.marudor.simpleBots;


import com.sun.mail.imap.IMAPFolder;
import de.marudor.simpleBots.account.Account;
import de.marudor.simpleBots.account.AccountStatus;
import de.marudor.simpleBots.database.Database;
import de.marudor.simpleBots.exceptions.NotFoundException;
import de.marudor.simpleBots.twitter.TwitterSession;
import javafx.util.Pair;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.search.FlagTerm;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marudor on 29/07/14.
 */
public class EmailHandler {
    private static final Logger logger = LoggerFactory.getLogger(EmailHandler.class);
    private static String DefaultTemplate;
    /**
     * Configured in simpleBots.properties
     * @param username The Default Email for this username.
     * @return Default Email for specific User
     */
    public static String DefaultEmail(String username) {
        return DefaultTemplate.replace("{username}", username);
    }

    private Folder unknown;
    private Folder error;


    public EmailHandler() {
        Session session = Session.getDefaultInstance(new Properties());
        Store imapStore;
        Configuration config = null;
        try {
            config = new PropertiesConfiguration("config/simpleBots.properties").subset("EmailHandler");
        } catch (ConfigurationException e) {
            logger.error("Couldn't find simpleBots.properties");
            System.exit(1);
        }
        DefaultTemplate = config.getString("DefaultEmail");
        try {
            imapStore = session.getStore(config.getString("storeType", "imaps"));
            imapStore.connect(config.getString("server"), config.getString("username"), config.getString("password"));
        } catch (MessagingException e) {
            logger.error("Couldn't connect to imap Server. Quitting Application.");
            logger.error(e.getMessage());
            return;
        }
        final IMAPFolder inbox;
        try {
            inbox = (IMAPFolder) imapStore.getFolder("INBOX");
            unknown = inbox.getFolder("UNKNOWN");
            if (!unknown.exists())
                unknown.create(Folder.HOLDS_MESSAGES);
            error = inbox.getFolder("ERROR");
            if (!error.exists())
                error.create(Folder.HOLDS_MESSAGES);
            error.open(Folder.READ_WRITE);
            unknown.open(Folder.READ_WRITE);
            inbox.open(Folder.READ_WRITE);
            inbox.setSubscribed(true);
            inbox.addMessageCountListener(new MessageCountAdapter() {
                public void messagesAdded(MessageCountEvent e) {
                    logger.debug("New Email found");
                    for (Message m : e.getMessages())
                        processMail(m);
                }
            });
            ScheduledExecutorService es = new ScheduledThreadPoolExecutor(2);
            es.scheduleWithFixedDelay(() -> {
                logger.debug("Manually checking Mail");
                try {
                    for (Message m : inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)))
                        processMail(m);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.MINUTES);
            es.schedule((Runnable) () -> {
                while (true) {
                    try {
                        logger.debug("Idling inbox");
                        inbox.idle();
                    } catch (MessagingException ignored) {
                        logger.error("idling failed");
                    }
                }
            }, 0, TimeUnit.MINUTES);

        } catch (MessagingException e) {
            logger.debug("Failed Emailhandler init");
            logger.debug(e.getMessage());
        }
    }


    /**
     * We process the Email we got
     * @param m The Email to process
     */
    private void processMail(Message m) {
        String subject = null;
        logger.info("Processing new email");
        try {
            subject = m.getSubject().intern();
            logger.debug("Got Subject");
            if (subject.contains("Confirm your Twitter account"))
                verifyMailTwitter(m, subject);
            else if (subject.contains("complete your Twitter profile today!"))
                verifiedMailTwitter(m, subject);
            else if (subject.contains(" one of your Tweets!"))
                deleteMail(m);
            else {
                logger.warn("Unknown subject found. (" + subject + ")");
                moveMessage(m, unknown);
            }
        } catch (MessagingException e) {
            logger.error("Error processingMail (" + subject + ")");
            logger.debug(e.toString());
            moveMessage(m, error);
        }
    }

    private void deleteMail(Message m) throws MessagingException {
        m.setFlag(Flags.Flag.DELETED, true);
    }

    private void verifiedMailTwitter(Message m, String subject) {
        Matcher match = Pattern.compile("(.*), complete your Twitter profile today!").matcher(subject);
        if (!match.find()) {
            logger.error("Unknown Subject? (" + subject + ")");
            moveMessage(m, unknown);
            return;
        }
        String username = match.group(1);
        Account a;
        try {
            a = Account.getByName(username);
        } catch (NotFoundException e) {
            logger.error(e.getMessage());
            return;
        }
        if (a.getStatus().lessThan(AccountStatus.APPROVED)) {
            a.setStatus(AccountStatus.APPROVED);
            Database.save(a);
        }
        try {
            deleteMail(m);
        } catch (MessagingException e) {
            logger.debug("Couldn't delete message "+e.getMessage());
        }
    }

    private void verifyMailTwitter(Message m, String subject) {
        try {
            logger.debug("Trying to verify");
            Multipart multipart = (Multipart) m.getContent();
            logger.debug("Got message content");
            Pair<String, String> code = extractURL(multipart.getBodyPart(0).getContent().toString());
            logger.debug("Might got code");
            if (code == null) {
                logger.error("Couldn't find Code URL for " + subject);
                moveMessage(m, error);
                return;
            }
            Account account;
            logger.debug("Trying to get account");
            try {
                account = Account.getByName(code.getKey());
            } catch (NotFoundException e) {
                logger.error(e.getMessage());
                m.setFlag(Flags.Flag.DELETED, true);
                return;
            }
            logger.debug("Got Account");
            boolean success = new TwitterSession(account).verifyMail(code.getValue());
            if (success)
                m.setFlag(Flags.Flag.DELETED, true);
            else {
                logger.warn("Couldn't verify Account (" + account.getUsername() + ")");
                moveMessage(m, error);
            }
        } catch (Exception e) {
            logger.error("Error initializing verify Twitter Mail");
            moveMessage(m, error);
        }
    }



    private Pair<String, String> extractURL(String body) {
        logger.debug("Got Body.");
        logger.debug(body);
        Pattern p = Pattern.compile(".*/confirm_email/(.*)/(.{5}-.{5}-.{6})");
        Matcher matcher = p.matcher(body);
        logger.debug("Matching");
        if (!matcher.find()) return null;
        logger.debug("Got Match. Returning Match.");
        String user = matcher.group(1);
        logger.debug("User: " + user);
        String code = matcher.group(2);
        logger.debug("code: " + code);
        return new Pair<>(user, code);
    }

    private void moveMessage(Message m, Folder destination) {
        try {
            m.setFlag(Flags.Flag.SEEN, true);
            m.getFolder().copyMessages(new Message[]{m}, destination);
            m.setFlag(Flags.Flag.DELETED, true);
        } catch (MessagingException ignored) {
        }
    }
}
