package de.marudor.simpleBots.database;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


/**
 * Created by marudor on 29/07/14.
 */
public class Database {
    private static SessionFactory sessionFactory;
    static {
        Properties connectionProperties = new Properties();
        try {
            PropertiesConfiguration connectionPropertiesConfiguraton = new PropertiesConfiguration("config/hibernate.properties");
            try (FileReader fr = new FileReader(connectionPropertiesConfiguraton.getFile())) {
                connectionProperties.load(fr);
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
            sessionFactory = new Configuration().configure().addProperties(connectionProperties).buildSessionFactory();
        }
        catch (ConfigurationException ignored) {
            ignored.printStackTrace();
        }
        Session s = getSession();
        if (s.createCriteria(UserAgent.class).list().isEmpty())
            UserAgent.initialData();
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    public static void save(Object o) {
        Session s = getSession();
        s.saveOrUpdate(o);
        s.flush();
        s.close();
    }

    public static void delete(Object o) {
        Session s = getSession();
        s.delete(o);
        s.flush();
        s.close();
    }
}
