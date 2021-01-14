package com.dotcms.mail;

import java.util.Optional;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class MailAPIImpl implements MailAPI {


    
    
    
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
                            (Session) ((Context) new InitialContext().lookup("java:comp/env")).lookup(MAIL_JNDI_NAME));
        } catch (Exception e1) {
            try {

                return Optional.of((Session) new InitialContext().lookup(MAIL_JNDI_NAME));
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    private static Lazy<Session> mailSessionHolder = Lazy.of(() -> {
        return new MailAPIImpl().loadMailSessionFromContext()
                    .orElse(new MailAPIImpl().createNewMailContext());

    });


    private Session createNewMailContext() {

        final Properties properties = new MailConfig().read();
        Session session = Session.getInstance(properties, createAuthenticator(properties));

        
        // bind session if not already there
        if (!hasMailInContext()) {
            Try.run(() -> new InitialContext().bind(MAIL_JNDI_NAME, session))
                            .onFailure(e -> Logger.warnAndDebug(MailAPIImpl.class, e));
        }
        return session;


    }
    
    private Authenticator createAuthenticator(final Properties properties) {
        
     
        boolean enabled = "true".equals(properties.getProperty("mail.smtp.auth"));
        if(!enabled) {
            return null;
        }
        
        
        String user =  properties.contains("mail.smtp.user") ? properties.getProperty("mail.smtp.user") : properties.getProperty("mail.user");
        String password = properties.contains("mail.smtp.password") ? properties.getProperty("mail.smtp.password") : properties.getProperty("mail.password");
        

        
        return new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication( user, password);
            }
         };
        
    }
    


}
