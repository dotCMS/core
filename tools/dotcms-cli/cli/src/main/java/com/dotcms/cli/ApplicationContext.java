package com.dotcms.cli;

import com.starxg.keytar.Keytar;
import com.starxg.keytar.KeytarException;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ApplicationContext {

    private static final Logger logger = Logger.getLogger(ApplicationContext.class);
    private static final String CURRENT_USER = "cli-current-user";
    private static final String NONE = "none";

    @ConfigProperty(name = CURRENT_USER, defaultValue = NONE)
    String user;

    public Optional<String> getUser() {
        return NONE.equals(user) ? Optional.empty() : Optional.ofNullable(user);
    }

    @ConfigProperty(name = "password-store-name")
    String passwordStoreName;
    // Make thread safe

    @ConfigProperty(name = "dotcms.api.url")
    String dotCMSAPIHost;

    public String getDotCMSAPIHost() {
        return dotCMSAPIHost;
    }

    private final Keytar keytar = Keytar.getInstance();

    public Optional<String> getToken() {
        final Optional<String> currentUser = getUser();
        if(currentUser.isPresent()){
        try {
            return Optional.of(keytar.getPassword(passwordStoreName, currentUser.get()));
        } catch (KeytarException e) {
            logger.error(String.format("Error recovering Token from %s",passwordStoreName), e);
        }
        }
        return Optional.empty();
    }

    public void setToken(final String token, final String user) {
        if (token == null) {
            try {
                keytar.deletePassword(passwordStoreName, user);
            } catch (KeytarException e) {
                logger.error(String.format("Error setting Token from %s",passwordStoreName), e);
            }
        } else if (user != null) {
            try {
                keytar.setPassword(passwordStoreName, user, token);
                this.user = user;
            } catch (KeytarException e) {
                logger.error(String.format("Error setting Token from %s",passwordStoreName), e);
            }
        }
    }

    private final Properties currentUserProperties = new Properties() ;
    private boolean preloadedUserProperties = false;
    private static final String DOT_CMS_CLI_PROPERTIES = "dotCms-cli.properties";

    void preloadUserPropertiesIfNeeded() {
        if(preloadedUserProperties){
            return;
        }
        final File file = getUserPropertiesFile();
        if (file.exists()) {
            try {
                final FileInputStream fileInputStream = new FileInputStream(file);
                currentUserProperties.load(fileInputStream);
                this.user = currentUserProperties.getProperty(CURRENT_USER);
                //any other prop we decide to add to this class  must be set here
                preloadedUserProperties = true;
            } catch (IOException e) {
                logger.error("Error loading user stored properties", e);
            }
        }
    }

    void flushUserProperties()  {
        currentUserProperties.setProperty(CURRENT_USER, user);
        //Other relevant properties must be set here too
        final File file = getUserPropertiesFile();
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            currentUserProperties.store(fileOutputStream, "user stored properties should list here.");
        }catch (IOException e){
            logger.error("Error flushing user stored properties", e);
        }
    }

    /**
     *
     * /user-home/dotCms-cli.properties
     * This method gets you such file
     * @return
     */
    private File getUserPropertiesFile() {
        final Path currentWorkingDir = getUserHomeDir();
        return Path.of(currentWorkingDir.toString(), DOT_CMS_CLI_PROPERTIES)
                .normalize().toFile();
    }

    /**
     * Application execution current working directly
     * @return
     */
    private Path getUserHomeDir() {
        return Paths.get(System.getProperty("user.home")).toAbsolutePath();
    }

    void onStart(@Observes StartupEvent ev) {
        preloadUserPropertiesIfNeeded();
    }

    void onStop(@Observes ShutdownEvent ev) {
        flushUserProperties();
    }

}
