package com.dotcms.mail;

import java.util.Map;
import java.util.Properties;
import com.google.common.annotations.VisibleForTesting;

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

    final Map<String, String> incomingProperties;

    MailConfig() {
        this(System.getenv());
    }

    @VisibleForTesting
    MailConfig(Map<String, String> properties) {
        this.incomingProperties = properties;
    }

    Properties read() {

        final Properties props = new Properties();

        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.user", "dotCMS");


        for (Map.Entry<String, String> env : incomingProperties.entrySet()) {
            final String origKey = env.getKey().toLowerCase();
            if (!origKey.startsWith("dot_mail")) {
                continue;
            }


            final String origValue = env.getValue();
            final String propName = origKey.replace("dot_", "").replace("_", ".");
            props.put(propName, origValue);


        }
        return props;

    }

}
