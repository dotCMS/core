/**
 *
 */
package com.dotcms.content.elasticsearch.business;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.*;
import com.dotmarketing.util.*;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.io.*;
import java.util.*;

/**
 * A helper for {@link ESContentletAPIImpl}
 *
 * @author jsanca
 * @since 1.5
 *
 */
public class ESContentletAPIHelper implements Serializable {


    public final static ESContentletAPIHelper INSTANCE =
            new ESContentletAPIHelper();

    private ESContentletAPIHelper() {}

    /**
     * Generate the Notification for the can not delete scenario on the {@link ESContentletAPIImpl}
     * @param notificationAPI
     * @param userLocale
     * @param userId
     * @param iNode
     */
    public final void generateNotificationCanNotDelete (final NotificationAPI notificationAPI,
                                                  final Locale userLocale,
                                                  final String userId,
                                                  final String iNode) {

        final Locale locale = (null != userLocale)?
                userLocale: Locale.getDefault();
        I18NMessage titleMsg = null;
        I18NMessage errorMsg = null;
        String i18nErrorMsg  = null;

        try {

            titleMsg = new I18NMessage(
                    "notification.escontentelet.cannotdelete.info.title");

             errorMsg = new I18NMessage(
                    "notification.escontentelet.cannotdelete.info.message", null,
                    iNode); // message = Contentlet with Inode {0} cannot be deleted because it's not archived. Please archive it first before deleting it.

            i18nErrorMsg = LanguageUtil.get(locale,
                    errorMsg.getKey(),
                    errorMsg.getArguments());

            Logger.error(this, i18nErrorMsg);

            notificationAPI.generateNotification(
                    titleMsg, // title = Contentlet Notification
                    errorMsg,
                    null, // no actions
                    NotificationLevel.ERROR,
                    NotificationType.GENERIC,
                    userId,
                    locale
            );
        } catch (LanguageException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }

        throw new DotStateException(i18nErrorMsg);
    } // generateNotificationCanNotDelete.

} // E:O:F:ESContentletAPIHelper.
