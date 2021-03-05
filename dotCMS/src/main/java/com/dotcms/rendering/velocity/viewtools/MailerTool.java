package com.dotcms.rendering.velocity.viewtools;

import com.liferay.util.StringPool;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.util.Mailer;

/**
 * Simple Email Sender ViewTool for DotCMS
 * 
 * @author Christopher Falzone <cfalzone@edinboro.edu>
 * @author Jason Tesser
 * @version 1.9.1
 */
public class MailerTool implements ViewTool {

    /**
     * Sends an email
     * 
     * Example: #set($error = $mailer.sendEmail( 'them@theirdomain.com', 'you@yourdomain.com', 'The
     * Subject', 'The Message', false)) #if($UtilMethods.isSet($error)) ## Custom Error Handling #else
     * <p>
     * Your message was sent
     * </p>
     * #end
     * 
     * @param to email address to send to
     * @param from email address to send from
     * @param subject subject of the email
     * @param message the message to send
     * @param html Whether or not to send it in HTML
     * @return Empty if Successful, or the error message otherwise
     */
    public String sendEmail(final String to, final String from, final String subject, final String message,
                    final boolean html) {

        final Mailer mailer  = new Mailer();
        final String[] froms = from.split(" ");
        if (froms.length == 2) {
            mailer.setFromName(froms[0]);
            mailer.setFromEmail(froms[1]);
        } else {
            mailer.setFromEmail(from);
        }

        mailer.setToEmail(to);
        mailer.setSubject(subject);

        if (html) {
            mailer.setHTMLAndTextBody(message);
        } else {
            mailer.setTextBody(message);
        }

        if(mailer.sendMessage()) {
            return StringPool.BLANK;
        }

        return mailer.getErrorMessage();

    }

    /**
     * Init Method for the viewtool
     */
    public void init(Object obj) {

    }
    
    public Mailer getMailer() {
        
        return new Mailer();
    }
}
