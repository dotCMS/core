package com.dotmarketing.util;

public interface MailerWrapper {
    void setToEmail(String email);
    void setToName(String name);
    void setSubject(String subject);
    void setHTMLBody(String htmlBody);
    void setFromEmail(String email);
    void sendMessage();
    void setFromName(String s);
    void setHTMLAndTextBody(String emailText);
    void setTextBody(String emailText);
}

