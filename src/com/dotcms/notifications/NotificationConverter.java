package com.dotcms.notifications;

import com.dotcms.notifications.bean.Notification;
import com.dotcms.notifications.bean.NotificationAction;
import com.dotcms.notifications.bean.NotificationData;
import com.dotcms.notifications.bean.UserNotificationPair;
import com.dotcms.notifications.view.NotificationActionView;
import com.dotcms.notifications.view.NotificationDataView;
import com.dotcms.notifications.view.NotificationView;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.Converter;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Locale;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.util.DateUtil.prettyDateSince;

/**
 * This converter basically tooks a {@link UserNotificationPair} and convert to notification but doing i18n support with the user {@link java.util.Locale} info
 * in addition it is also return the pretty date with the time sent.
 * @author jsanca
 */
public class NotificationConverter implements Converter<UserNotificationPair, NotificationView> {

    private String getMessage (final Locale locale, final I18NMessage i18NMessage) throws LanguageException {

        String message = StringUtils.EMPTY;

        message = (null == i18NMessage.getArguments())?
            LanguageUtil.get(locale, i18NMessage.getKey()):
                LanguageUtil.format(locale, i18NMessage.getKey(), i18NMessage.getArguments(), false);

        if (!UtilMethods.isSet(message) && UtilMethods.isSet(i18NMessage.getDefaultMessage())) {

            message = i18NMessage.getDefaultMessage();
        }

        return message;
    } // getMessage.

    @Override
    public NotificationView convert(final UserNotificationPair userNotificationPair) {

        NotificationDataView data = null;
        List<NotificationActionView> actions = null;
        final Notification notification = userNotificationPair.getNotification();
        final User user = userNotificationPair.getUser();
        final Locale locale = (null != user)?user.getLocale():Locale.getDefault();

        if (null != notification.getNotificationData()) {

            String title = StringUtils.EMPTY;

            try {

                title = this.getMessage(locale, notification.getNotificationData().getTitle());
            } catch (LanguageException e) {

                title =  (UtilMethods.isSet(notification.getNotificationData().getTitle().getDefaultMessage()))?
                        notification.getNotificationData().getTitle().getDefaultMessage():
                        notification.getNotificationData().getTitle().getKey();
            }

            String message = StringUtils.EMPTY;

            try {

                message = this.getMessage(locale, notification.getNotificationData().getMessage());
            } catch (LanguageException e) {

                message =  (UtilMethods.isSet(notification.getNotificationData().getMessage().getDefaultMessage()))?
                        notification.getNotificationData().getMessage().getDefaultMessage():
                        notification.getNotificationData().getMessage().getKey();
            }

            if (null != notification.getNotificationData().getActions()) {

                actions =  list();
                for (NotificationAction action : notification.getNotificationData().getActions()) {

                    String text = StringUtils.EMPTY;

                    try {

                        text = this.getMessage(locale, action.getText());
                    } catch (LanguageException e) {

                        text =  (UtilMethods.isSet(action.getText().getDefaultMessage()))?
                                action.getText().getDefaultMessage():
                                action.getText().getKey();
                    }

                    actions.add(new NotificationActionView(text, action.getAction(),
                            action.getActionType(), action.getAttributes()));
                }
            }

            data = new NotificationDataView(title, message, actions);
        }

        final NotificationView notificationResult = new NotificationView(notification.getId(),
                notification.getType(), notification.getLevel(), notification.getUserId(),
                notification.getTimeSent(), notification.getWasRead(),
                data, prettyDateSince(notification.getTimeSent(), locale));

        return notificationResult;
    }
} // E:O:F:NotificationConverter.
