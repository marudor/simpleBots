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
    public static Properties connectionProperties;
    static {
        try {
            PropertiesConfiguration connectionPropertiesConfiguraton = new PropertiesConfiguration("config/hibernate.properties");
            connectionProperties = new Properties();
            try (FileReader fr = new FileReader(connectionPropertiesConfiguraton.getFile())) {
                connectionProperties.load(fr);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ConfigurationException e) {
            connectionProperties = null;
        }
    }
    public static final SessionFactory sessionFactory = new Configuration().configure().addProperties(connectionProperties).buildSessionFactory();

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
