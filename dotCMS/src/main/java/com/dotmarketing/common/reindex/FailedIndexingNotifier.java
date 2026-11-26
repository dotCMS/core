package com.dotmarketing.common.reindex;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotcms.notifications.bean.NotificationAction;
import com.dotcms.notifications.bean.NotificationActionType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * The one place that tells people content failed to index (#37269, review of #37924).
 *
 * <p>Every event goes through two channels with the same words: a pop-up (system message) for
 * whoever is connected, and a bell notification for the CMS administrators that stays. Both
 * point to the Maintenance portlet's index status, where the failed records are listed.</p>
 *
 * <ul>
 *   <li>{@link #reindexFinished(long, boolean)}: a full reindex switched over. With no failed
 *   contents and a switchover the journal reached on its own, the usual "finished successfully"
 *   bell notification; otherwise how many contents were skipped, and whether the operator
 *   stopped the reindex.</li>
 *   <li>{@link #savedContentWithheld(String, String)}: a saved content's index document was
 *   withheld, so the save reported success but the content will not appear in searches.</li>
 * </ul>
 */
public class FailedIndexingNotifier {

    /** A withheld version's title and last editor, read without loading the contentlet. */
    public record WithheldContent(@Nullable String title, @Nullable String modUser) {
    }

    /** Where the failed records are listed. */
    private static final String INDEX_STATUS_LINK = "#/c/maintenance";

    /** How long a pop-up stays, as the failed-reindex pop-up always did. */
    private static final long POPUP_LIFE_MILLIS = 3_600_000L;

    private final NotificationAPI notificationAPI;
    private final RoleAPI roleAPI;
    private final UserAPI userAPI;
    private final BiConsumer<SystemMessage, List<String>> popup;
    private final Function<String, String> messages;
    private final Function<String, Optional<WithheldContent>> contentByInode;

    /** Uses the platform APIs, the system-message channel and the default company's messages. */
    public FailedIndexingNotifier() {
        this(APILocator.getNotificationAPI(), APILocator.getRoleAPI(), APILocator.getUserAPI(),
                (message, users) -> SystemMessageEventUtil.getInstance().pushMessage(message, users),
                FailedIndexingNotifier::message, FailedIndexingNotifier::findWithheldContent);
    }

    @VisibleForTesting
    FailedIndexingNotifier(final NotificationAPI notificationAPI, final RoleAPI roleAPI,
            final UserAPI userAPI, final BiConsumer<SystemMessage, List<String>> popup,
            final Function<String, String> messages,
            final Function<String, Optional<WithheldContent>> contentByInode) {
        this.notificationAPI = notificationAPI;
        this.roleAPI = roleAPI;
        this.userAPI = userAPI;
        this.popup = popup;
        this.messages = messages;
        this.contentByInode = contentByInode;
    }

    /**
     * Reports how a full reindex finished, right after its switchover.
     *
     * <p>"Finished successfully" is only true when the journal drained on its own. The Maintenance
     * portlet's "Stop Reindex and Make Active" deletes every pending record and promotes an index
     * that never received them, so a forced switchover is reported as a warning instead.
     * Otherwise the administrators who did not press the button — the notification goes to the
     * whole CMS Administrator role — read a success message for a reindex that was cut short.</p>
     *
     * <p>The two conditions are independent and both are reported: a forced switchover that also
     * left records parked sends one warning for each, because the parked count says nothing about
     * how much was discarded.</p>
     *
     * @param failedRecords the records parked as failed
     * @param switchForced  {@code true} when the operator forced the switchover, {@code false}
     *                      when the journal drained and the switchover followed on its own
     * @throws DotDataException if the notification cannot be posted
     */
    public void reindexFinished(final long failedRecords, final boolean switchForced)
            throws DotDataException {
        if (failedRecords == 0 && !switchForced) {
            bell(NotificationLevel.INFO, new I18NMessage("notification.reindexing.success"), null);
            return;
        }
        if (switchForced) {
            warnAdmins(messages.apply("Reindex-Stopped-message"),
                    new I18NMessage("Reindex-Stopped-notification"));
        }
        if (failedRecords > 0) {
            warnAdmins(messages.apply("Contents-Failed-Reindex-message")
                            .replace("{0}", String.valueOf(failedRecords)),
                    new I18NMessage("Contents-Failed-Reindex-notification", null, failedRecords));
        }
    }

    /**
     * Sends one warning to the CMS administrators through both channels with the same words: a
     * pop-up for whoever is connected, and a bell notification that stays and links to the index
     * status.
     *
     * @param popupText   the pop-up wording, which carries its own link to the index status
     * @param bellMessage the bell wording, to which the index-status action is attached
     * @throws DotDataException if the notification cannot be posted
     */
    private void warnAdmins(final String popupText, final I18NMessage bellMessage)
            throws DotDataException {
        final Role cmsAdmin = roleAPI.loadCMSAdminRole();
        popup(popupText, roleAPI.findUserIdsForRole(cmsAdmin));
        bell(NotificationLevel.WARNING, bellMessage, indexStatusLink());
    }

    /**
     * Reports a saved content whose index document was withheld: a pop-up to its last editor (or
     * to the CMS administrators when it cannot be resolved) and a bell notification to the CMS
     * administrators.
     *
     * @param identifier the content identifier
     * @param reason     the journal reason written when the document was withheld
     * @throws DotDataException if the notification cannot be posted
     */
    public void savedContentWithheld(final String identifier, final String reason)
            throws DotDataException {
        final Optional<WithheldContent> content = IndexDocumentViolation.violationOf(reason)
                .flatMap(withheld -> contentByInode.apply(withheld.inode()));
        final String name = content.map(WithheldContent::title).filter(UtilMethods::isSet)
                .orElse(identifier);
        final String detail = limitDetail(reason);
        final List<String> recipients = content.map(WithheldContent::modUser)
                .filter(UtilMethods::isSet).map(List::of)
                .orElseGet(() -> Try.of(() -> roleAPI.findUserIdsForRole(
                        roleAPI.loadCMSAdminRole())).getOrElse(List.of()));
        popup(messages.apply("Content-Failed-Index-message")
                .replace("{0}", name).replace("{1}", detail), recipients);
        bell(NotificationLevel.WARNING,
                new I18NMessage("Content-Failed-Index-notification", null, name, detail),
                indexStatusLink());
    }

    /**
     * The part of a withheld document's reason that says which limit it exceeded, e.g.
     * {@code field 'catchall' is 42,000,921 chars; limit 20,000,000}.
     */
    private static String limitDetail(final String reason) {
        final int start = reason.indexOf("field '");
        final int end = reason.lastIndexOf(" — ");
        return start >= 0 && end > start ? reason.substring(start, end) : reason;
    }

    private void popup(final String text, final List<String> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        popup.accept(new SystemMessageBuilder()
                .setMessage(text)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.WARNING)
                .setLife(POPUP_LIFE_MILLIS)
                .create(), users);
    }

    private void bell(final NotificationLevel level, final I18NMessage message,
            @Nullable final List<NotificationAction> actions) throws DotDataException {
        final User systemUser = userAPI.getSystemUser();
        notificationAPI.generateNotification(new I18NMessage("notification.reindex.error.title"),
                message, actions, level, NotificationType.GENERIC, Visibility.ROLE,
                roleAPI.loadCMSAdminRole().getId(), systemUser.getUserId(),
                systemUser.getLocale());
    }

    private static List<NotificationAction> indexStatusLink() {
        return List.of(new NotificationAction(new I18NMessage("Contents-Failed-Reindex-action"),
                INDEX_STATUS_LINK, NotificationActionType.LINK, null));
    }

    /** A message of the default company's language. */
    private static String message(final String key) {
        return Try.of(() -> LanguageUtil.get(APILocator.getCompanyAPI().getDefaultCompany(), key))
                .getOrElse(key);
    }

    /** Reads a version's title and last editor from its row, without loading the contentlet. */
    private static Optional<WithheldContent> findWithheldContent(final String inode) {
        return Try.of(() -> new DotConnect()
                        .setSQL("SELECT title, mod_user FROM contentlet WHERE inode = ?")
                        .addParam(inode)
                        .loadObjectResults())
                .onFailure(e -> Logger.debug(FailedIndexingNotifier.class,
                        "Unable to read withheld content " + inode + ": " + e.getMessage()))
                .toJavaOptional()
                .filter(rows -> !rows.isEmpty())
                .map(rows -> rows.get(0))
                .map((Map<String, Object> row) -> new WithheldContent(
                        (String) row.get("title"), (String) row.get("mod_user")));
    }
}
