var logger = log4javascript.getRootLogger();
var appender = new log4javascript.BrowserConsoleAppender();
appender.setThreshold(log4javascript.Level.INFO);
var popUpLayout = new log4javascript.PatternLayout("%d{HH:mm:ss} %-5p - %m%n");
appender.setLayout(popUpLayout);
logger.addAppender(appender);

logger.debug("logger started");
