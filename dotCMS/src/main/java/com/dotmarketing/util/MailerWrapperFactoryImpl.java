package com.dotmarketing.util;

public class MailerWrapperFactoryImpl implements MailerWrapperFactory {
    @Override
    public MailerWrapper createMailer() {
        return new MailerWrapperImpl();
    }
}
