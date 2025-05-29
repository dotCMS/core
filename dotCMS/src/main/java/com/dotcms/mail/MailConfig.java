package com.dotcms.mail;

import static com.dotcms.mail.MailAPI.DEFAULT_MAIL_PROTOCOL;

import com.dotcms.mail.MailAPI.Keys;
import java.util.Properties;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;

/**
 * Converts a <String,String> key/value to a <String,String> , <String,Bool> or <String,Int> as is
 * expected by the given property.
 *
 * It expect to kind of properties:
 * 1) properties that starts with "mail." {@link com.dotcms.mail.MailAPI.Keys}
 * 2) properties that starts with "DOT_MAIL_." This second option usually came from env properties, for instance:
 *
 * DOT_MAIL_SMTP_user=testUser
 * DOT_MAIL_SMTP_host=testHost
 * DOT_MAIL_SMTP_port=12345
 * DOT_MAIL_SMTP_connectiontimeout=54321
 * DOT_MAIL_SMTP_auth=true
 * DOT_MAIL_SMTP_ehlo=false
 *
 * @author will
 */
class MailConfig {

    Lazy<Properties> properties = Lazy.of(()->{

        final Properties properties = new Properties();

        final String protocol = properties.containsKey(Keys.TRANSPORT_PROTOCOL.getValue())
                ? properties.getProperty(Keys.TRANSPORT_PROTOCOL.getValue()) : DEFAULT_MAIL_PROTOCOL;

        properties.setProperty("mail." + protocol + ".host", "localhost");
        properties.setProperty("mail." + protocol + ".user", "dotCMS");

        Config.getKeys().forEachRemaining(origKey -> {
            final String lowerKey = origKey.toLowerCase();
            if (lowerKey.startsWith("dot_mail") || lowerKey.startsWith("mail.")) {
                final String value = Config.getStringProperty(origKey);
                final String propName = lowerKey.replace("dot_", "").replace("_", ".");
                properties.put(propName, value);
            }

        });

        return properties;
    });
}
