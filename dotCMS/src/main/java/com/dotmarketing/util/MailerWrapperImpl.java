package com.dotmarketing.util;

public class MailerWrapperImpl implements MailerWrapper {
    private final Mailer mailer;

    public MailerWrapperImpl() {
        this.mailer = new Mailer();
    }

    @Override
    public void setToEmail(String email) {
        mailer.setToEmail(email);
    }

    @Override
    public void setToName(String name) {
        mailer.setToName(name);
    }

    @Override
    public void setSubject(String subject) {
        mailer.setSubject(subject);
    }

    @Override
    public void setHTMLBody(String body) {
        mailer.setHTMLBody(body);
    }

    @Override
    public void setFromEmail(String email) {
        mailer.setFromEmail(email);
    }

    @Override
    public void sendMessage() {
        mailer.sendMessage();
    }

    @Override
    public void setFromName(String s) {
        mailer.setFromName(s);
    }

    @Override
    public void setHTMLAndTextBody(String emailText) {
        mailer.setHTMLAndTextBody(emailText);
    }

    @Override
    public void setTextBody(String emailText) {
        mailer.setTextBody(emailText);
    }
}
