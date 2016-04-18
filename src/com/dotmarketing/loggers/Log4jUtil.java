package com.dotmarketing.loggers;

import com.dotcms.repackage.org.apache.logging.log4j.Level;
import com.dotcms.repackage.org.apache.logging.log4j.LogManager;
import com.dotcms.repackage.org.apache.logging.log4j.Logger;
import com.dotcms.repackage.org.apache.logging.log4j.core.Appender;
import com.dotcms.repackage.org.apache.logging.log4j.core.Layout;
import com.dotcms.repackage.org.apache.logging.log4j.core.LoggerContext;
import com.dotcms.repackage.org.apache.logging.log4j.core.appender.ConsoleAppender;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.Configuration;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.ConfigurationSource;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.Configurator;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.LoggerConfig;
import com.dotcms.repackage.org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import com.dotcms.repackage.org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 8/5/15
 */
public class Log4jUtil {

    /**
     * Creates a ConsoleAppender in order to add it to the root logger
     */
    public static void createAndAddConsoleAppender () {

        //Getting the current log4j appenders
        Logger logger = LogManager.getRootLogger();
        //Getting all the appenders for this logger
        Map<String, Appender> appenderMap = ((com.dotcms.repackage.org.apache.logging.log4j.core.Logger) logger).getAppenders();

        //Getting the log4j configuration
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
        Configuration configuration = loggerContext.getConfiguration();

        //Init log4j to see the messages in ant's output
        if ( !appenderMap.isEmpty() ) {

            //Create a simple layout for our appender
            Layout simpleLayout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, configuration, null, null, true, false, null, null);

            //Create and add a console appender to the configuration
            ConsoleAppender consoleAppender = ConsoleAppender.createDefaultAppenderForLayout(simpleLayout);
            configuration.addAppender(consoleAppender);
        }
    }

    /**
     * Returns the context registered Loggers
     *
     * @return
     */
    public static Collection<com.dotcms.repackage.org.apache.logging.log4j.core.Logger> getLoggers () {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
        return loggerContext.getLoggers();
    }

    /**
     * Sets the logging level of the log4j configuration
     *
     * @param level
     */
    public static void setLevel ( Level level ) {

        //Getting the log4j configuration
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
        Configuration configuration = loggerContext.getConfiguration();

        LoggerConfig loggerConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        //Set the level
        loggerConfig.setLevel(level);
        loggerContext.updateLoggers();// This causes all Loggers to re-fetch information from their LoggerConfig.
    }

    /**
     * Sets the logging level of the log4j configuration of a given Logger
     *
     * @param logger
     * @param level
     */
    public static void setLevel ( com.dotcms.repackage.org.apache.logging.log4j.core.Logger logger, Level level ) {
        logger.setLevel(level);
    }

    /**
     * Normally there is no need to do this manually.
     * Each LoggerContext registers a shutdown hook that takes care of releasing resources when the JVM exits (unless system property log4j.shutdownHookEnabled is set to false).
     * Web applications should include the log4j-web module in their classpath which disables the shutdown
     * hook but instead cleans up log4j resources when the web application is stopped.
     */
    public static void shutdown () {

        // get the current context
        LoggerContext context = (LoggerContext) LogManager.getContext();

        //Shutting down log4j in order to avoid memory leaks
        shutdown(context);
    }

    /**
     * Normally there is no need to do this manually.
     * Each LoggerContext registers a shutdown hook that takes care of releasing resources when the JVM exits (unless system property log4j.shutdownHookEnabled is set to false).
     * Web applications should include the log4j-web module in their classpath which disables the shutdown
     * hook but instead cleans up log4j resources when the web application is stopped.
     */
    public static void shutdown(LoggerContext context) {
        //Shutting down log4j in order to avoid memory leaks
        Configurator.shutdown(context);
    }

    /**
     * Initialises/reconfigures log4j based on a given log4j configuration file
     *
     * @param log4jConfigFilePath
     */
    public static void initializeFromPath ( String log4jConfigFilePath ) {

        if ( log4jConfigFilePath != null ) {

            try {

                LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);

                if ( !loggerContext.isInitialized() || loggerContext.isStopped() ) {

                    /*ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFilePath));
                    XmlConfiguration xmlConfig = new XmlConfiguration(source);

                    loggerContext.start(xmlConfig);*/
                	Configurator.initialize(null, log4jConfigFilePath);
                } else {
                    loggerContext.setConfigLocation(URI.create(log4jConfigFilePath));
                    loggerContext.reconfigure();
                }
            } catch ( Exception e ) {
                LogManager.getLogger().error("Error initializing log for " + log4jConfigFilePath + " configuration file.", e);
            }

        }
    }

    /**
     * Returns the current dotCMS logger context
     *
     * @return current logger context
     */
    public static LoggerContext getLoggerContext () {
        return (LoggerContext) LogManager.getContext(false);
    }

}