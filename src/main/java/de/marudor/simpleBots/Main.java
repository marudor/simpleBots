package de.marudor.simpleBots;


import de.marudor.simpleBots.server.TwitterServer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;

/**
 * Created by marudor on 29/07/14.
 */
public class Main {
    private static final EmailHandler emailHandler = new EmailHandler();
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private static Configuration config;

    static {
        try {
            config = new PropertiesConfiguration("jaxws.properties");
        } catch (ConfigurationException e) {
            config = null;
        }
    }

    public static void main(final String[] args) throws Exception {
        if (config == null) {
            logger.error("Couldn't find jaxws.properties config file.");
            emailHandler.quit();
            return;
        }
        Object implementor = new TwitterServer();

        Endpoint.publish(config.getString("TwitterServer.address", "localhost:61182"), implementor);
        logger.info("Application started");

        //The EmailHandler.idlemanager blocks quitting of application
    }
}
