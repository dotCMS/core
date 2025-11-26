package com.dotcms.e2e.logging;

import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Logger utility class for logging messages with different levels of severity.
 *
 * This class provides static methods to log info, warn, and error messages.
 *
 * @author vico
 */
public class Logger {

    private Logger() {
    }

    /**
     * Logs an info message.
     *
     * @param clazz the class from which the log is made
     * @param message the message to log
     */
    public static void info(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.info(message);
    }

    /**
     * Logs an info message.
     *
     * @param clazz the class from which the log is made
     * @param message the supplier of the message to log
     */
    public static void info(final Class<?> clazz, final Supplier<String> message) {
        info(clazz, message.get());
    }

    /**
     * Logs a warning message.
     *
     * @param clazz the class from which the log is made
     * @param message the message to log
     */
    public static void warn(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.warn(message);
    }

    /**
     * Logs a warning message.
     *
     * @param clazz the class from which the log is made
     * @param message the supplier of the message to log
     */
    public static void warn(final Class<?> clazz, final Supplier<String> message) {
        warn(clazz, message.get());
    }

    /**
     * Logs a warning message with a throwable.
     *
     * @param clazz the class from which the log is made
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public static void warn(final Class<?> clazz, final String message, final Throwable throwable) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.warn(message, throwable);
    }

    /**
     * Logs a warning message with a throwable.
     *
     * @param clazz the class from which the log is made
     * @param message the supplier of the message to log
     * @param throwable the throwable to log
     */
    public static void warn(final Class<?> clazz, final Supplier<String> message, final Throwable throwable) {
        warn(clazz, message.get(), throwable);
    }

    /**
     * Logs an error message.
     *
     * @param clazz the class from which the log is made
     * @param message the message to log
     */
    public static void error(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message);
    }

    /**
     * Logs an error message.
     *
     * @param clazz the class from which the log is made
     * @param message the supplier of the message to log
     */
    public static void error(final Class<?> clazz, final Supplier<String> message) {
        error(clazz, message.get());
    }

    /**
     * Logs an error message with a throwable.
     *
     * @param clazz the class from which the log is made
     * @param message the message to log
     * @param throwable the throwable to log
     */
    public static void error(final Class<?> clazz, final String message, final Throwable throwable) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message, throwable);
    }

    /**
     * Logs an error message with a throwable.
     *
     * @param clazz the class from which the log is made
     * @param message the supplier of the message to log
     * @param throwable the throwable to log
     */
    public static void error(final Class<?> clazz, final Supplier<String> message, final Throwable throwable) {
        error(clazz, message.get(), throwable);
    }

}
