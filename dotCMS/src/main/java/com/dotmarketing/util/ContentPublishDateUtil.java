package com.dotmarketing.util;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.Debouncer;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Identifier;
import com.liferay.portal.language.LanguageUtil;

import io.vavr.control.Try;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling content publish date related operations
 */
public class ContentPublishDateUtil {

    private static Debouncer debouncer = new Debouncer();

    @VisibleForTesting
    static void setDebouncer(Debouncer testDebouncer) {
        debouncer = testDebouncer;
    }

    @VisibleForTesting
    static Debouncer getDebouncer() {
        return debouncer;
    }

    /**
     * Checks whether the given versionable has a publish date set in the future and, if so,
     * sends a user notification message indicating that the content cannot be published yet.
     *
     * <p>This method verifies that the contentType defines a publish date field and that the
     * identifier's publish date is later than the current time. If both conditions are met,
     * a success-type system message is sent to the user, and {@code true} is returned.</p>
     *
     * @param contentType   the contentType, used to determine if a publish date field exists
     * @param identifier  the identifier containing publish date metadata
     * @param modUser the user who last modified the version
     * @return {@code true} if a future publish date was detected and a message was sent;
     *         {@code false} otherwise
     */
    public static boolean notifyIfFuturePublishDate(final ContentType contentType, final Identifier identifier, final String modUser) {
        if (UtilMethods.isSet(contentType.publishDateVar()) &&
                UtilMethods.isSet(identifier.getSysPublishDate()) &&
                identifier.getSysPublishDate().after(new Date())) {

            final Runnable futurePublishDateRunnable = () -> futurePublishDateMessage(modUser);
            debouncer.debounce("contentPublishDateError" + modUser, futurePublishDateRunnable, 5000, TimeUnit.MILLISECONDS);
            return true;
        }
        return false;
    }

    /**
     * Method to encapsulate the logic of a growl message when content has a future publish date
     * @param user user to show the growl
     */
    private static void futurePublishDateMessage(final String user) {
        final String message = Try.of(() -> LanguageUtil.get("message.contentlet.publish.future.date"))
                .getOrElse("The content was saved successfully but cannot be published because"
                        + " it is scheduled to be published on future date.");
        final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder()
                .setMessage(message).setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.SUCCESS).setLife(5000);

        SystemMessageEventUtil.getInstance().pushMessage(systemMessageBuilder.create(),
                java.util.List.of(user));
        Logger.debug(ContentPublishDateUtil.class, message);
    }
}