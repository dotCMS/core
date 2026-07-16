package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.util.Mailer;

import java.util.Map;

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
        return sendEmail(to, from, subject, message, html, null);
    }

    /**
     * Sends an email with custom headers
     *
     * Example:
     * <pre>
     * #set($headerMap = {})
     * $!headerMap.put("Reply-To", "reply@yourdomain.com")
     * $!headerMap.put("X-Priority", "1")
     * #set($error = $mailer.sendEmail('them@theirdomain.com', 'you@yourdomain.com',
     *                                'The Subject', 'The Message', false, $headerMap))
     * #if($UtilMethods.isSet($error))
     *   ## Custom Error Handling
     * #else
     *   <p>Your message was sent</p>
     * #end
     * </pre>
     *
     * @param to email address to send to
     * @param from email address to send from
     * @param subject subject of the email
     * @param message the message to send
     * @param html Whether or not to send it in HTML
     * @param customHeaders Map containing custom headers with header names as keys and header values as values
     * @return Empty if Successful, or the error message otherwise
     */
    public String sendEmail(final String to, final String from, final String subject, final String message, final boolean html, final Map<String, String> customHeaders) {
        final Mailer mailer = new Mailer();
        final String[] froms = from.split(" ");
        if (froms.length == 2) {
            mailer.setFromName(froms[0]);
            mailer.setFromEmail(froms[1]);
        } else {
            mailer.setFromEmail(from);
        }
        mailer.setToEmail(to);
        mailer.setSubject(subject);

        if(html) {
            mailer.setHTMLBody(message);
        } else {
            mailer.setTextBody(message);
        }

        // Process headers from map - cleaner approach
        if(customHeaders != null && !customHeaders.isEmpty()) {
            for(Map.Entry<String, String> entry : customHeaders.entrySet()) {
                mailer.addHeader(entry.getKey(), entry.getValue());
            }
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
