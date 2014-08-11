package de.marudor.simpleBots.authentication;

import com.lambdaworks.crypto.SCryptUtil;
import de.marudor.simpleBots.database.Database;
import de.marudor.simpleBots.exceptions.NotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import twitter4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.util.List;
import java.util.Map;

/**
 * Created by marudor on 03/08/14.
 */

@Entity
public class AuthUser {
    private static final Logger logger = Logger.getLogger(AuthUser.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(unique = true)
    private String username;
    @NotNull
    private String password;
    @NotNull
    private boolean admin = false;

    public AuthUser() {
    }

    private AuthUser(String username, String password, boolean admin) {
        this.username = username;
        int n = (int) Math.pow(2,14);
        logger.debug("Crypting Password using "+n);
        this.password = SCryptUtil.scrypt(password, n, 8, 1);
        this.admin = admin;
    }

    public String getName() {
        return username;
    }

    public void setName(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean correctPassword(String password) {
        return SCryptUtil.check(password, this.password);
    }

    public static boolean isAdmin(String username) {
        AuthUser u = getByUsername(username);
        return (u != null && u.isAdmin());
    }

    public static AuthUser getByUsername(String username) {
        Session s = Database.getSession();
        try {
            return (AuthUser) s.createCriteria(AuthUser.class).add(Restrictions.eq("username", username)).uniqueResult();
        } finally {
            if (s != null && s.isOpen()) s.close();
        }
    }

    public static void createNew(String username, String password, boolean admin) {
        AuthUser user = new AuthUser(username, password, admin);
        logger.debug("Crypted Password, saving");
        Database.save(user);
        logger.debug("Saved AuthUser");
    }

    public static void delete(String username) throws NotFoundException {
        AuthUser user = AuthUser.getByUsername(username);
        if (user == null) throw new NotFoundException();
        Database.delete(user);
    }

    public static boolean isSame(WebServiceContext wsctx, String username) {
        MessageContext mctx = wsctx.getMessageContext();
        Map http_headers = (Map) mctx.get(MessageContext.HTTP_REQUEST_HEADERS);
        List userList = (List) http_headers.get("Username");
        String contextUsername = userList!=null ? userList.get(0).toString() : null;
        return contextUsername.equals(username);
    }

}
