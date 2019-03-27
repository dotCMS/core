package com.dotcms.util;

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * Message Service Factory
 * @author jsanca
 */
public class MessageAPIFactory implements Serializable {

    private final MessageAPI messageService = new MessageServiceImpl();

    private static class SingletonHolder {
        private static final MessageAPIFactory INSTANCE = new MessageAPIFactory();
    }

    /**
     * Get the instance.
     *
     * @return MessageAPIFactory
     */
    public static MessageAPIFactory getInstance() {

        return MessageAPIFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    public MessageAPI getMessageService() {
        return messageService;
    }

    private final class MessageServiceImpl implements MessageAPI {

        @Override
        public void sendMail(final User user,
                             final Company company,
                             final String subject,
                             final String body) {

            if (Logger.isDebugEnabled(MessageAPIFactory.class)) {

                Logger.debug(MessageAPIFactory.class, "Sending an email with a subject: " +
                    subject + ", to the user email: " + user.getEmailAddress());
            }

            MessageAPI.super.sendMail(user, company, subject, body);
        } // sendMail.
    } // MessageServiceImpl.

} // E:O:F:MessageAPIFactory.
