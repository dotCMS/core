package com.dotmarketing.loggers;

import io.vavr.Lazy;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.BasicAsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import java.util.Collection;
import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 8/5/15
 */
public class Log4jUtil {

    private final static String LOG4J_CONTEXT_SELECTOR = "Log4jContextSelector";
    
    /**
     * Lazily loaded ContextSelector for shutdown optimization
     */
    private static final Lazy<ContextSelector> contextSelector = Lazy.of(() -> {
        LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            // If the factory is a Log4jContextFactory, we can get the selector directly
            return ((Log4jContextFactory) factory).getSelector();
        }
        return null;
    });

    /**
     * Creates a ConsoleAppender in order to add it to the root logger
     */
    public static void createAndAddConsoleAppender () {

        //Getting the current log4j appenders
        org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
        //Getting all the appenders for this logger
        Map<String, Appender> appenderMap = ((Logger) logger).getAppenders();

        //Getting the log4j configuration
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
        Configuration configuration = loggerContext.getConfiguration();

        //Init log4j to see the messages in ant's output
        if ( !appenderMap.isEmpty() ) {

            //Create a simple layout for our appender
            Layout simpleLayout = PatternLayout.newBuilder()
                    .withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                    .withConfiguration(configuration).build();

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
    public static Collection<org.apache.logging.log4j.core.Logger> getLoggers () {
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
    public static void setLevel ( org.apache.logging.log4j.core.Logger logger, Level level ) {
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
     * If the ContextSelector in use is BasicAsyncLoggerContextSelector, we skip the shutdown
     * to avoid shutting down the global async context while still in use (for example, if this method
     * is called from an OSGi plugin).
     * @param context the LoggerContext to shutdown
     */
    public static void shutdown(LoggerContext context) {
        // Get the current ContextSelector and check if it's BasicAsyncLoggerContextSelector
        ContextSelector currentContextSelector = contextSelector.get();
        
        // Skip Configurator.shutdown if using BasicAsyncLoggerContextSelector to avoid issues
        if (!(currentContextSelector instanceof BasicAsyncLoggerContextSelector)) {
            //Shutting down log4j in order to avoid memory leaks
            Configurator.shutdown(context);
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