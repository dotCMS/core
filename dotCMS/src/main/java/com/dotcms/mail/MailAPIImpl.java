package com.dotcms.mail;

import java.util.Optional;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.JNDIUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class MailAPIImpl implements MailAPI {

    private static Lazy<Session> mailSessionHolder = Lazy.of(() ->
            new MailAPIImpl().loadMailSessionFromContext()
                    .orElse(new MailAPIImpl().createNewMailContext()));

    private final Properties properties;

    public MailAPIImpl() {
        this(new MailConfig().properties.get());
    }

    @VisibleForTesting
    MailAPIImpl(final Properties properties) {
        this.properties = properties;
    }

    @Override
    public Session getMailSession() {
        return mailSessionHolder.get();
    }

    @Override
    public boolean hasMailInContext() {
        return loadMailSessionFromContext().isPresent();
    }

    @Override
    public Optional<Session> loadMailSessionFromContext() {

        try {
            return Optional.of(
                    (Session)JNDIUtil.lookup((Context)new InitialContext().lookup("java:comp/env"), MAIL_JNDI_NAME));
        } catch (NamingException ne) {
            try {

                return Optional.of(
                        (Session)JNDIUtil.lookup(new InitialContext(), MAIL_JNDI_NAME));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    private Session createNewMailContext() {

        final Session session = Session.getInstance(this.properties, createAuthenticator());
        // bind session if not already there
        if (!hasMailInContext()) {
            Try.run(() -> new InitialContext().bind(MAIL_JNDI_NAME, session))
                            .onFailure(e -> Logger.warnAndDebug(MailAPIImpl.class, e));
        }

        return session;
    }

    @VisibleForTesting
    Authenticator createAuthenticator() {

        final String protocol = this.properties.containsKey(Keys.TRANSPORT_PROTOCOL.getValue())
                ? this.properties.getProperty(Keys.TRANSPORT_PROTOCOL.getValue()) : "smtp";

        final boolean enabled = "true".equalsIgnoreCase(this.properties.getProperty("mail." + protocol + ".auth"));

        if (!enabled) {

            return null;
        }

        final String userKey = "mail." + protocol + ".user";
        final String passwordKey = "mail." + protocol + ".password";

        final String user = this.properties.containsKey(userKey)?
                                this.properties.getProperty(userKey):
                                this.properties.getProperty(Keys.USER.getValue());

        final String password = this.properties.containsKey(passwordKey)?
                                this.properties.getProperty(passwordKey):
                                this.properties.getProperty(Keys.PASSWORD.getValue());

        if(user==null || password==null) {

            return null;
        }
        
        return new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        };
    }
}
