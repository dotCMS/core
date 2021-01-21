package com.dotcms.mail;

import java.util.Properties;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;

/**
 * Converts a <String,String> key/value to a <String,String> , <String,Bool> or <String,Int> as is
 * expected by the given property. See:
 * 
 * 
 * 
 * @author will
 *
 */
class MailConfig {



    static Lazy<Properties> properties = Lazy.of(()->{

        final Properties properties = new Properties();

        properties.setProperty("mail.smtp.host", "localhost");
        properties.setProperty("mail.smtp.user", "dotCMS");


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
