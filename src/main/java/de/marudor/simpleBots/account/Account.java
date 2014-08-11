package de.marudor.simpleBots.account;

import de.marudor.simpleBots.database.Database;
import de.marudor.simpleBots.database.UserAgent;
import de.marudor.simpleBots.exceptions.NotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marudor on 28/07/14.
 */

@Entity
@XmlAccessorType(XmlAccessType.FIELD)
public class Account {
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @XmlElement(name="accountType")
    private AccountType type;
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @XmlElement
    private UserAgent userAgent;
    @ManyToOne(fetch = FetchType.LAZY)
    @XmlElement
    private Person person;
    @NotNull
    @XmlElement
    private String username;
    @NotNull
    @XmlElement
    private String password;
    @NotNull
    @XmlElement
    private String email;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @XmlElement(name="accountStatus")
    private AccountStatus status;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    public Account(Person person, String username, String password, String email, UserAgent userAgent, AccountType accountType) {
        this.person = person;
        this.username = username;
        this.password = password;
        this.email = email;
        this.userAgent = userAgent;
        this.status = AccountStatus.NONE;
        this.type = accountType;
    }

    public Account() {
    }

    public Person getPerson() {
        return person;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public boolean isTwitterAccount() {
        return type == AccountType.Twitter;
    }

    public boolean isFacebookAccount() {
        return type == AccountType.Facebook;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isUseable() {
        return status == AccountStatus.READY;
    }

    public static Account getByName(String name) throws NotFoundException {
        Session s = Database.getSession();
        Account a = (Account) s.createCriteria(Account.class).add(Restrictions.eq("username", name)).uniqueResult();
        s.close();
        if (a == null)
            throw new NotFoundException("Account with username "+name+" not found in Database.");
        return a;
    }

    public static List<Account> getAllByStatus(AccountStatus status) {
        Session s = Database.getSession();
        List<Account> accountList = (List<Account>) s.createCriteria(Account.class).add(Restrictions.eq("status",status)).list();
        s.close();
        return accountList;
    }

    public static List<Account> getAll(boolean includeBanned) {
        Session s = Database.getSession();
        List<Account> accountList;
        if (includeBanned)
            accountList = s.createCriteria(Account.class).list();
        else
            accountList = s.createCriteria(Account.class).add(Restrictions.ne("status",AccountStatus.BANNED)).list();
        s.close();
        return accountList;
    }

    public static List<Account> getRandomX(int number) {
        List<Account> r = new ArrayList<>();
        Session s = Database.getSession();
        r = s.createQuery("select a from Account a order by rand()")
                .setMaxResults(number)
                .list();
        return r;
    }
}
