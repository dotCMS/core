package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.content.index.IndexDocumentConstraints;
import com.dotcms.content.index.IndexDocumentViolation;
import com.dotcms.notifications.bean.NotificationAction;
import com.dotcms.notifications.bean.NotificationActionType;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.reindex.FailedIndexingNotifier.WithheldContent;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the one place that tells people content failed to index (review of #37924).
 *
 * <h2>What is at stake</h2>
 * <p>Before, a full reindex that left contents out sent two contradicting messages at the same
 * moment — a pop-up "N contents(s) failed to reindex and were skipped" and a bell notification
 * "Reindex process finished successfully" — and a saved content whose document was withheld sent
 * nothing at all. Every event now goes through both channels with the same words: a pop-up for
 * whoever is connected, and a bell notification that stays.</p>
 */
public class FailedIndexingNotifierTest {

    private static final Map<String, String> MESSAGES = Map.of(
            "Contents-Failed-Reindex-message",
            "{0} contents(s) failed to reindex and were skipped.  Please review your"
                    + " <a href=\"#/c/maintenance\">index status</a>.",
            "Reindex-Stopped-message",
            "The reindex was stopped and the new index was made active.  Contents that had not"
                    + " been reindexed yet were discarded.  Please review your"
                    + " <a href=\"#/c/maintenance\">index status</a>.",
            "Content-Failed-Index-message",
            "Content \"{0}\" failed to index and was skipped: {1}.  Please review your"
                    + " <a href=\"#/c/maintenance\">index status</a>.");

    private static final String REASON = new IndexDocumentViolation("id-1", "inode-1", 1L,
            new IndexDocumentConstraints.Violation(IndexDocumentConstraints.Kind.STRING_LENGTH,
                    "catchall", 42_000_921L, 20_000_000L)).toFailureReason();

    private NotificationAPI notificationAPI;
    private final List<SystemMessage> pushed = new ArrayList<>();
    private final List<List<String>> pushedTo = new ArrayList<>();
    private Optional<WithheldContent> content = Optional.empty();
    private FailedIndexingNotifier notifier;

    @Before
    public void setUp() throws Exception {
        notificationAPI = mock(NotificationAPI.class);
        final Role cmsAdmin = mock(Role.class);
        when(cmsAdmin.getId()).thenReturn("cms-admin-role");
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadCMSAdminRole()).thenReturn(cmsAdmin);
        when(roleAPI.findUserIdsForRole(cmsAdmin)).thenReturn(List.of("admin-1", "admin-2"));
        final User system = mock(User.class);
        when(system.getUserId()).thenReturn("system");
        when(system.getLocale()).thenReturn(Locale.US);
        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.getSystemUser()).thenReturn(system);
        notifier = new FailedIndexingNotifier(notificationAPI, roleAPI, userAPI,
                (message, users) -> {
                    pushed.add(message);
                    pushedTo.add(users);
                }, MESSAGES::get, inode -> content);
    }

    /**
     * Given Scenario: A full reindex finished and no content failed.
     * Expected Result: The usual "finished successfully" INFO bell notification, and no pop-up.
     */
    @Test
    public void test_reindexFinished_noFailures_successOnly() throws Exception {
        notifier.reindexFinished(0, false);

        final I18NMessage message = bellMessage(NotificationLevel.INFO, false);
        assertEquals("notification.reindexing.success", message.getKey());
        assertTrue("no pop-up when nothing failed", pushed.isEmpty());
    }

    /**
     * Given Scenario: A full reindex finished with 11 failed contents.
     * Expected Result: The existing pop-up to the CMS administrators, and a WARNING bell
     * notification with the same words and an "index status" link — no "finished successfully".
     */
    @Test
    public void test_reindexFinished_withFailures_sameWordsInBothChannels() throws Exception {
        notifier.reindexFinished(11, false);

        assertEquals(1, pushed.size());
        assertEquals("11 contents(s) failed to reindex and were skipped.  Please review your"
                + " <a href=\"#/c/maintenance\">index status</a>.", pushed.get(0).getMessage());
        assertEquals(MessageSeverity.WARNING, pushed.get(0).getSeverity());
        assertEquals(List.of("admin-1", "admin-2"), pushedTo.get(0));

        final I18NMessage message = bellMessage(NotificationLevel.WARNING, true);
        assertEquals("Contents-Failed-Reindex-notification", message.getKey());
        assertArrayEquals(new Object[]{11L}, message.getArguments());
    }

    /**
     * Given Scenario: The operator pressed "Stop Reindex and Make Active", so the switchover was
     * forced; the pending journal records were deleted and nothing was parked as failed.
     * Expected Result: No "finished successfully" message. A pop-up and a WARNING bell
     * notification saying the reindex was stopped and the contents not yet reindexed were
     * discarded, both pointing at the index status.
     */
    @Test
    public void test_reindexFinished_switchForced_noFailures_reportsStoppedNotSuccess()
            throws Exception {
        notifier.reindexFinished(0, true);

        assertEquals(1, pushed.size());
        assertEquals("The reindex was stopped and the new index was made active.  Contents that"
                + " had not been reindexed yet were discarded.  Please review your"
                + " <a href=\"#/c/maintenance\">index status</a>.", pushed.get(0).getMessage());
        assertEquals(MessageSeverity.WARNING, pushed.get(0).getSeverity());
        assertEquals(List.of("admin-1", "admin-2"), pushedTo.get(0));

        final I18NMessage message = bellMessage(NotificationLevel.WARNING, true);
        assertEquals("Reindex-Stopped-notification", message.getKey());
    }

    /**
     * Given Scenario: The switchover was forced and 11 contents had already been parked as
     * failed.
     * Expected Result: Both warnings, each in both channels — the parked count says nothing
     * about how much the stop discarded, so neither replaces the other — and no "finished
     * successfully".
     */
    @Test
    public void test_reindexFinished_switchForced_withFailures_reportsBoth() throws Exception {
        notifier.reindexFinished(11, true);

        assertEquals(2, pushed.size());
        assertTrue("the stop is reported", String.valueOf(pushed.get(0).getMessage())
                .startsWith("The reindex was stopped"));
        assertTrue("the parked contents are reported", String.valueOf(pushed.get(1).getMessage())
                .startsWith("11 contents(s) failed to reindex"));

        final ArgumentCaptor<I18NMessage> message = ArgumentCaptor.forClass(I18NMessage.class);
        verify(notificationAPI, times(2)).generateNotification(any(I18NMessage.class),
                message.capture(), any(), eq(NotificationLevel.WARNING),
                eq(NotificationType.GENERIC), eq(Visibility.ROLE), eq("cms-admin-role"),
                eq("system"), eq(Locale.US));
        assertEquals(List.of("Reindex-Stopped-notification", "Contents-Failed-Reindex-notification"),
                message.getAllValues().stream().map(I18NMessage::getKey).collect(
                        java.util.stream.Collectors.toList()));
        verify(notificationAPI, never()).generateNotification(any(I18NMessage.class),
                any(I18NMessage.class), any(), eq(NotificationLevel.INFO), any(), any(),
                anyString(), anyString(), any());
    }

    /**
     * Given Scenario: A saved content's document was withheld; the content is found, titled
     * "UI Big", last modified by user dotcms.org.1.
     * Expected Result: A pop-up to that user and a WARNING bell notification to the CMS
     * administrators, both naming the content by title and the exceeded limit, with the
     * "index status" link.
     */
    @Test
    public void test_savedContentWithheld_popupToEditor_bellToAdmins() throws Exception {
        content = Optional.of(new WithheldContent("UI Big", "dotcms.org.1"));

        notifier.savedContentWithheld("id-1", REASON);

        assertEquals(1, pushed.size());
        assertEquals("Content \"UI Big\" failed to index and was skipped: field 'catchall' is"
                + " 42,000,921 chars; limit 20,000,000.  Please review your"
                + " <a href=\"#/c/maintenance\">index status</a>.", pushed.get(0).getMessage());
        assertEquals(MessageSeverity.WARNING, pushed.get(0).getSeverity());
        assertEquals(List.of("dotcms.org.1"), pushedTo.get(0));

        final I18NMessage message = bellMessage(NotificationLevel.WARNING, true);
        assertEquals("Content-Failed-Index-notification", message.getKey());
        assertArrayEquals(new Object[]{"UI Big",
                "field 'catchall' is 42,000,921 chars; limit 20,000,000"}, message.getArguments());
    }

    /**
     * Given Scenario: A saved content's document was withheld, but its version can no longer be
     * found (no title, no editor).
     * Expected Result: The content is named by its identifier and the pop-up goes to the CMS
     * administrators.
     */
    @Test
    public void test_savedContentWithheld_contentNotFound_identifierAndAdmins() throws Exception {
        notifier.savedContentWithheld("id-1", REASON);

        assertTrue(String.valueOf(pushed.get(0).getMessage()).startsWith("Content \"id-1\" failed to index"));
        assertEquals(List.of("admin-1", "admin-2"), pushedTo.get(0));
    }

    /**
     * Captures the bell notification sent to the CMS administrators at the given level and
     * checks whether it carries the "index status" link.
     */
    private I18NMessage bellMessage(final NotificationLevel level, final boolean withLink)
            throws Exception {
        final ArgumentCaptor<I18NMessage> message = ArgumentCaptor.forClass(I18NMessage.class);
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<NotificationAction>> actions =
                ArgumentCaptor.forClass(List.class);
        verify(notificationAPI).generateNotification(any(I18NMessage.class), message.capture(),
                actions.capture(), eq(level), eq(NotificationType.GENERIC), eq(Visibility.ROLE),
                eq("cms-admin-role"), eq("system"), eq(Locale.US));
        if (withLink) {
            final NotificationAction link = actions.getValue().get(0);
            assertEquals(NotificationActionType.LINK, link.getActionType());
            assertEquals("#/c/maintenance", link.getAction());
            assertEquals("Contents-Failed-Reindex-action", link.getText().getKey());
        } else {
            assertTrue(actions.getValue() == null || actions.getValue().isEmpty());
        }
        return message.getValue();
    }
}
