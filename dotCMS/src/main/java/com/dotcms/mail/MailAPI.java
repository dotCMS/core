package com.dotcms.mail;

import javax.mail.Session;
import java.util.Optional;

/**
 * This class encapsulate email features of dotCMS
 * Note: To resolve the Mail Session see {@link MailConfig} and {@link Keys}
 * You can configure the session on:
 * - JNDI: mail/MailSession
 * - dotmarketing-config.properties see {@link Keys}
 * - from evn properties see {@link MailConfig}
 */
public interface MailAPI {

    /**
     * Basic properties  for email session configuration
     */
    enum Keys {

        TRANSPORT_PROTOCOL("mail.transport.protocol"),
        USER("mail.user"),
        PASSWORD("mail.password");

        private final String value;

        Keys(final String value){
            this.value = value;
        }

        @Override
        public java.lang.String toString() {
            return value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Default JNDI dir for the mail session
     */
    String MAIL_JNDI_NAME="mail/MailSession";

    /**
     * Tries to load the session from the JNDI
     * @return
     */
    Optional<Session> loadMailSessionFromContext();

    int getConnectionPort();

    String getConnectionHost();

    /**
     * True if has a mail session in the context
     * @return Boolean
     */
    boolean hasMailInContext();

    /**
     * Get the Mail Session
     * @return Session
     */
    Session getMailSession();
}
