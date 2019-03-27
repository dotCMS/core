package com.dotcms.util;

import com.dotmarketing.util.EmailUtils;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.io.Serializable;

/**
 * This class encapsulates all the kind of messages possible on dotCMS.
 * Such as email.
 * @author jsanca
 */
public interface MessageAPI extends Serializable {

    /**
     * Sends an email.
     * @param user
     * @param company
     * @param subject
     * @param body
     */
    public default void sendMail(final User user,
                                 final Company company,
                                 final String subject,
                                 final String body) {

        EmailUtils.sendMail(user, company, subject, body);
    } // sendMail.
} // E:O:F:MessageAPI.
