package com.dotcms.mail;

import java.util.Optional;
import javax.mail.Session;

public interface MailAPI {

    final static String MAIL_JNDI_NAME="mail/MailSession";

    Optional<Session> loadMailSessionFromContext();

    boolean hasMailInContext();

    Session getMailSession();
    
    
    
    
}
