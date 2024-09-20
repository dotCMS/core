package com.dotcms.e2e.logging;

import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class Logger {

    private Logger() {
    }

    public static void info(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.info(message);
    }

    public static void info(final Class<?> clazz, final Supplier<String> message) {
        info(clazz, message.get());
    }

    public static void warn(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.warn(message);
    }

    public static void warn(final Class<?> clazz, final Supplier<String> message) {
        warn(clazz, message.get());
    }

    public static void warn(final Class<?> clazz, final String message, final Throwable throwable) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.warn(message, throwable);
    }

    public static void warn(final Class<?> clazz, final Supplier<String> message, final Throwable throwable) {
        warn(clazz, message.get(), throwable);
    }

    public static void error(final Class<?> clazz, final String message) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message);
    }

    public static void error(final Class<?> clazz, final Supplier<String> message) {
        error(clazz, message.get());
    }

    public static void error(final Class<?> clazz, final String message, final Throwable throwable) {
        final org.slf4j.Logger logger = LoggerFactory.getLogger(clazz);
        logger.error(message, throwable);
    }

    public static void error(final Class<?> clazz, final Supplier<String> message, final Throwable throwable) {
        error(clazz, message.get(), throwable);
    }

}
