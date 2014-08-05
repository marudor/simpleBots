package de.marudor.simpleBots.authentication;

import de.marudor.simpleBots.database.Database;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Created by marudor on 03/08/14.
 */

@Entity
public class AuthUser {
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

    public AuthUser(String username, String password, boolean admin) {
        this.username = username;
        this.password = password;
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
}
