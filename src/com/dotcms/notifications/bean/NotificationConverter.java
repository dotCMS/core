package com.dotcms.notifications.bean;

import com.dotcms.util.Converter;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.util.DateUtil.prettyDateSince;

/**
 * This converter basically tooks a {@link UserNotificationPair} and convert to notification but doing i18n support with the user {@link java.util.Locale} info
 * in addition it is also return the pretty date with the time sent.
 * @author jsanca
 */
public class NotificationConverter implements Converter<UserNotificationPair, Notification> {


    @Override
    public Notification convert(final UserNotificationPair userNotificationPair) {

        NotificationData data = null;
        List<NotificationAction> actions = null;
        final Notification notification = userNotificationPair.getNotification();
        final User user = userNotificationPair.getUser();

        if (null != notification.getNotificationData()) {

            String title = notification.getNotificationData().getTitle();

            try {

                title = LanguageUtil.get(user.getLocale(), title);
            } catch (LanguageException e) {
                title = notification.getNotificationData().getTitle();
            }

            String message = notification.getNotificationData().getMessage();

            try {

                message = LanguageUtil.get(user.getLocale(), message);
            } catch (LanguageException e) {
                message = notification.getNotificationData().getMessage();
            }

            if (null != notification.getNotificationData().getActions()) {

                actions =  list();
                for (NotificationAction action : actions) {

                    String text = action.getText();

                    try {

                        text = LanguageUtil.get(user.getLocale(), text);
                    } catch (LanguageException e) {
                        text = action.getText();
                    }

                    actions.add(new NotificationAction(text, action.getAction(),
                            action.getActionType(), action.getAttributes()));
                }
            }

            data = new NotificationData(title, message, actions);
        }

        final Notification notificationResult = new Notification(notification.getId(),
                notification.getType(), notification.getLevel(), notification.getUserId(),
                notification.getTimeSent(), notification.getWasRead(),
                data);

        notificationResult.setPrettyDate
                (prettyDateSince(notification.getTimeSent(), user.getLocale()));

        return notificationResult;
    }
} // E:O:F:NotificationConverter.
