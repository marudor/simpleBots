package de.marudor.simpleBots.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


/**
 * Created by marudor on 29/07/14.
 */
public class Database {
    public static final SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    public static void save(Object o) {
        Session s = getSession();
        s.saveOrUpdate(o);
        s.flush();
        s.close();
    }
}
