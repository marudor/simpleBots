package de.marudor.simpleBots;


import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import de.marudor.simpleBots.server.TwitterServer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.net.ssl.*;
import javax.xml.ws.Endpoint;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by marudor on 29/07/14.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {
        SLF4JBridgeHandler.install();
        final Configuration config;
        try {
            config = new PropertiesConfiguration("config/jaxws.properties");
        } catch (ConfigurationException e) {
            logger.error("Couldn't find jaxws.properties config file.");
            System.exit(1);
            return;
        }
        ExecutorService tp = Executors.newFixedThreadPool(1);
        tp.submit(()->startWS(config));
        tp.submit(Main::startEmailHandler);

    }

    private static void startEmailHandler() {
        logger.info("starting EmailHandler");
        new EmailHandler();
        logger.info("started EmailHandler");
    }

    private static void startWS(Configuration config) {
        logger.info("Starting Webserver");
        try {
            Endpoint endpoint = Endpoint.create(new TwitterServer());
            SSLContext ssl = SSLContext.getInstance("SSLv3");


            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());


            String password = config.getString("TwitterServer.keystorePassword");
            try (FileInputStream fr = new FileInputStream(new PropertiesConfiguration("config/keystore").getFile())) {
                store.load(fr, password.toCharArray());
            }

            //init the key store, along with the password 'test'
            kmf.init(store, password.toCharArray());
            KeyManager[] keyManagers;
            keyManagers = kmf.getKeyManagers();


            //Init the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            //It will reference the same key store as the key managers
            tmf.init(store);

            TrustManager[] trustManagers = tmf.getTrustManagers();


            ssl.init(keyManagers, trustManagers, new SecureRandom());

            //Init a configuration with our SSL context
            HttpsConfigurator configurator = new HttpsConfigurator(ssl);


            //Create a server on localhost, port 443 (https port)
            int port = config.getInt("TwitterServer.port");
            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(config.getString("TwitterServer.address"), port), port);
            httpsServer.setHttpsConfigurator(configurator);


            //Create a context so our service will be available under this context
            HttpContext context = httpsServer.createContext("/twitter");
            httpsServer.start();


            //Finally, use the created context to publish the service
            endpoint.publish(context);
            logger.info("Started Webserver");
        } catch (Exception ex) {
            logger.error("Can't start Webserver");
            logger.error(ex.getMessage());
        }

    }
}
