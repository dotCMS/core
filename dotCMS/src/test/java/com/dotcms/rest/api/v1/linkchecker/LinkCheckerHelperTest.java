package com.dotcms.rest.api.v1.linkchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.enterprise.linkchecker.LinkCheckerJob;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Unit tests for the helper behind the Link Checker REST resource: which rows of the broken-links
 * list a user receives, how each row is built, paging, and queueing a run of the check.
 */
class LinkCheckerHelperTest {

    private static final String TYPE = "type-inode-1";
    private static final String FIELD_ID = "field-id-1";
    private static final String FIELD_NAME = "Body";
    private static final String OTHER_FIELD_ID = "field-id-0";
    private static final String OTHER_FIELD_NAME = "Summary";
    private static final String CONTENT_TYPE_NAME = "Blog";
    private static final String EDITOR_ID = "editor-1";
    private static final String EDITOR_NAME = "Jane Doe";
    private static final String EDITOR_EMAIL = "jane.doe@example.com";
    private static final String LINK_TITLE = "Read more";
    private static final String URL = "https://example.com/page";
    private static final Date MOD_DATE = new Date(1_773_480_600_000L);

    private LinkCheckerAPI linkCheckerAPI;
    private ContentletAPI contentletAPI;
    private UserAPI userAPI;
    private Scheduler scheduler;
    private User user;
    private Map<String, Field> fieldsById;
    private LinkCheckerHelper helper;

    @BeforeEach
    void setUp() throws Exception {
        linkCheckerAPI = mock(LinkCheckerAPI.class);
        contentletAPI = mock(ContentletAPI.class);
        userAPI = mock(UserAPI.class);
        scheduler = mock(Scheduler.class);
        user = mock(User.class);

        final Field field = mock(Field.class);
        when(field.getFieldName()).thenReturn(FIELD_NAME);
        when(field.getStructureInode()).thenReturn(TYPE);
        fieldsById = new HashMap<>();
        fieldsById.put(FIELD_ID, field);
        final Field otherField = mock(Field.class);
        when(otherField.getFieldName()).thenReturn(OTHER_FIELD_NAME);
        when(otherField.getStructureInode()).thenReturn(TYPE);
        fieldsById.put(OTHER_FIELD_ID, otherField);

        final Structure contentType = mock(Structure.class);
        when(contentType.getName()).thenReturn(CONTENT_TYPE_NAME);
        when(contentType.getInode()).thenReturn(TYPE);
        final Map<String, Structure> typesByInode = new HashMap<>();
        typesByInode.put(TYPE, contentType);

        final User editor = mock(User.class);
        when(editor.getFullName()).thenReturn(EDITOR_NAME);
        when(editor.getEmailAddress()).thenReturn(EDITOR_EMAIL);
        when(userAPI.loadUserById(EDITOR_ID)).thenReturn(editor);

        helper = new LinkCheckerHelper(linkCheckerAPI, contentletAPI, userAPI,
                fieldsById::get, typesByInode::get, () -> scheduler);
    }

    /**
     * Given one link on content the user can read.
     * When findBrokenLinks(type, 0, 25, user).
     * Then one row with inode, title, field name, content type name, mod date, link title and url.
     */
    @Test
    void findBrokenLinks_readableContent_returnsRow() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals(1, result.list().size());
        final BrokenLinkView row = result.list().get(0);
        assertEquals("inode-1", row.inode());
        assertEquals("Sample title inode-1", row.contentTitle());
        assertEquals(FIELD_NAME, row.field());
        assertEquals(CONTENT_TYPE_NAME, row.contentType());
        assertEquals(new SimpleDateFormat("MM/dd/yyyy hh:mm a").format(MOD_DATE), row.modDate());
        assertEquals(LINK_TITLE, row.linkTitle());
        assertEquals(URL, row.url());
    }

    /**
     * Given two links; the user can read only one of the two contentlets.
     * When findBrokenLinks(type, 0, 25, user).
     * Then only the row of the readable contentlet is returned.
     */
    @Test
    void findBrokenLinks_contentUserCannotRead_isLeftOut() throws Exception {
        givenLinks(link("inode-1"), link("inode-2"));
        unreadableContent("inode-1");
        readableContent("inode-2");

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.list().size());
        assertEquals("inode-2", result.list().get(0).inode());
    }

    /**
     * Given three links; the user can read two of the contentlets.
     * When findBrokenLinks(type, 0, 25, user).
     * Then total is 2.
     */
    @Test
    void findBrokenLinks_total_countsOnlyReadableRows() throws Exception {
        givenLinks(link("inode-1"), link("inode-2"), link("inode-3"));
        readableContent("inode-1");
        unreadableContent("inode-2");
        readableContent("inode-3");

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(2, result.total());
        assertEquals(2, result.list().size());
    }

    /**
     * Given one link.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the contentlet is looked up with the requesting user and respectFrontendRoles=false.
     */
    @Test
    void findBrokenLinks_looksUpContentAsRequestingUser() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        helper.findBrokenLinks(TYPE, 0, 25, user);

        verify(contentletAPI).find("inode-1", user, false);
        verify(userAPI, never()).getSystemUser();
    }

    /**
     * Given a contentlet whose editor has a full name and an email address.
     * When findBrokenLinks(type, 0, 25, user).
     * Then modUserName is the full name.
     */
    @Test
    void findBrokenLinks_editor_isFullNameOnly() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        final BrokenLinkView row = helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0);

        assertEquals(EDITOR_NAME, row.modUserName());
    }

    /**
     * Given a contentlet whose editor lookup returns null.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is returned with an empty modUserName.
     */
    @Test
    void findBrokenLinks_editorLookupReturnsNull_keepsRowWithEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");
        when(userAPI.loadUserById(EDITOR_ID)).thenReturn(null);

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("", result.list().get(0).modUserName());
    }

    /**
     * Given a contentlet whose editor lookup is refused with a DotSecurityException.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is kept with an empty modUserName.
     */
    @Test
    void findBrokenLinks_editorLookupRefused_keepsRowWithEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");
        when(userAPI.loadUserById(EDITOR_ID)).thenThrow(new DotSecurityException("Not allowed"));

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("", result.list().get(0).modUserName());
    }

    /**
     * Given a contentlet whose editor resolves but has no full name.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is returned with an empty modUserName, not null.
     */
    @Test
    void findBrokenLinks_editorWithoutFullName_hasEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");
        final User editorWithoutName = mock(User.class);
        when(editorWithoutName.getFullName()).thenReturn(null);
        when(userAPI.loadUserById(EDITOR_ID)).thenReturn(editorWithoutName);

        final BrokenLinkView row = helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0);

        assertEquals("", row.modUserName());
    }

    /**
     * Given five links in sorted order where the 2nd and 4th contentlets are not readable by the user.
     * When findBrokenLinks(type, 0, 2, user) and findBrokenLinks(type, 2, 2, user).
     * Then the first page holds rows 1 and 3, the second page holds row 5, and total is 3 on both.
     */
    @Test
    void findBrokenLinks_pagingSkipsRowsUserCannotRead() throws Exception {
        givenLinks(link("inode-1"), link("inode-2"), link("inode-3"), link("inode-4"), link("inode-5"));
        readableContent("inode-1");
        unreadableContent("inode-2");
        readableContent("inode-3");
        unreadableContent("inode-4");
        readableContent("inode-5");

        final BrokenLinksView firstPage = helper.findBrokenLinks(TYPE, 0, 2, user);
        final BrokenLinksView secondPage = helper.findBrokenLinks(TYPE, 2, 2, user);

        assertEquals(3, firstPage.total());
        assertEquals(2, firstPage.list().size());
        assertEquals("inode-1", firstPage.list().get(0).inode());
        assertEquals("inode-3", firstPage.list().get(1).inode());
        assertEquals(3, secondPage.total());
        assertEquals(1, secondPage.list().size());
        assertEquals("inode-5", secondPage.list().get(0).inode());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, 0, 1, user).
     * Then one row and total 3.
     */
    @Test
    void findBrokenLinks_limitOfOne_returnsOneRow() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 1, user);

        assertEquals(3, result.total());
        assertEquals(1, result.list().size());
        assertEquals("inode-1", result.list().get(0).inode());
    }

    /**
     * Given a contentlet with no modUser.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is returned with an empty modUserName and the editor is not looked up.
     */
    @Test
    void findBrokenLinks_noModUser_returnsEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        final Contentlet contentlet = readableContent("inode-1");
        when(contentlet.getModUser()).thenReturn(null);

        final BrokenLinkView row = helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0);

        assertEquals("", row.modUserName());
        verify(userAPI, never()).loadUserById(any());
    }

    /**
     * Given a contentlet with a null modDate.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is returned with an empty modDate.
     */
    @Test
    void findBrokenLinks_nullModDate_returnsEmptyDate() throws Exception {
        givenLinks(link("inode-1"));
        final Contentlet contentlet = readableContent("inode-1");
        when(contentlet.getModDate()).thenReturn(null);

        final BrokenLinkView row = helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0);

        assertEquals("", row.modDate());
    }

    /**
     * Given a link whose field's content type lookup returns null.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is left out and the total is 0.
     */
    @Test
    void findBrokenLinks_contentTypeNotFound_isLeftOut() throws Exception {
        final LinkCheckerHelper helperWithoutTypes = new LinkCheckerHelper(linkCheckerAPI, contentletAPI,
                userAPI, fieldsById::get, inode -> null, () -> scheduler);
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        final BrokenLinksView result = helperWithoutTypes.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(0, result.total());
        assertTrue(result.list().isEmpty());
    }

    /**
     * Given a field lookup that fails with a DotStateException.
     * When findBrokenLinks(type, 0, 25, user).
     * Then DotRuntimeException with the fixed message, not carrying the lookup's text.
     */
    @Test
    void findBrokenLinks_fieldLookupFails_hasFixedMessage() throws Exception {
        final LinkCheckerHelper failingHelper = new LinkCheckerHelper(linkCheckerAPI, contentletAPI, userAPI,
                fieldId -> {
                    throw new DotStateException("field-lookup-detail");
                },
                inode -> null, () -> scheduler);
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> failingHelper.findBrokenLinks(TYPE, 0, 25, user));

        assertEquals(LinkCheckerHelper.READ_FAILURE_MESSAGE, thrown.getMessage());
        assertFalse(thrown.getMessage().contains("field-lookup-detail"));
    }

    /**
     * Given a lookup failure wrapping a missing database row, and one wrapping another data failure.
     * When isCausedByMissingRow is asked about each.
     * Then only the first one is reported as a missing row.
     */
    @Test
    void isCausedByMissingRow_onlyForMissingRowCause() {
        assertTrue(LinkCheckerHelper.isCausedByMissingRow(
                new DotStateException(new NotFoundInDbException("Field not found"))));
        assertFalse(LinkCheckerHelper.isCausedByMissingRow(
                new DotStateException(new DotDataException("Database unavailable"))));
    }

    /**
     * Given a link whose content type lookup returns a content type without an inode.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is left out and the total is 0.
     */
    @Test
    void findBrokenLinks_contentTypeWithoutInode_isLeftOut() throws Exception {
        final Structure emptyContentType = mock(Structure.class);
        when(emptyContentType.getInode()).thenReturn("");
        final LinkCheckerHelper helperWithEmptyType = new LinkCheckerHelper(linkCheckerAPI, contentletAPI,
                userAPI, fieldsById::get, inode -> emptyContentType, () -> scheduler);
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        final BrokenLinksView result = helperWithEmptyType.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(0, result.total());
        assertTrue(result.list().isEmpty());
    }

    /**
     * Given the contentlet lookup fails with a DotRuntimeException from the data layer.
     * When findBrokenLinks(type, 0, 25, user).
     * Then DotRuntimeException with the fixed message, not carrying the original text.
     */
    @Test
    void findBrokenLinks_contentLookupFails_hasFixedMessage() throws Exception {
        givenLinks(link("inode-1"));
        when(contentletAPI.find("inode-1", user, false))
                .thenThrow(new DotRuntimeException("some data-layer text"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> helper.findBrokenLinks(TYPE, 0, 25, user));

        assertEquals(LinkCheckerHelper.READ_FAILURE_MESSAGE, thrown.getMessage());
        assertFalse(thrown.getMessage().contains("some data-layer text"));
    }

    /**
     * Given a lookup failure whose missing-row cause sits two levels below it.
     * When isCausedByMissingRow is asked about it.
     * Then it is reported as a missing row.
     */
    @Test
    void isCausedByMissingRow_twoLevelsDeep_isTrue() {
        final Throwable failure = new DotStateException(
                new DotDataException("Wrapped", new NotFoundInDbException("Field not found")));

        assertTrue(LinkCheckerHelper.isCausedByMissingRow(failure));
    }

    /**
     * Given a lookup failure with no cause.
     * When isCausedByMissingRow is asked about it.
     * Then it is not reported as a missing row.
     */
    @Test
    void isCausedByMissingRow_noCause_isFalse() {
        assertFalse(LinkCheckerHelper.isCausedByMissingRow(new DotStateException("No cause")));
    }

    /**
     * Given a contentlet whose editor lookup fails with a DotStateException.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is kept with an empty modUserName.
     */
    @Test
    void findBrokenLinks_editorLookupFails_keepsRowWithEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");
        when(userAPI.loadUserById(EDITOR_ID)).thenThrow(new DotStateException("Lookup failed"));

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("", result.list().get(0).modUserName());
    }

    /**
     * Given two readable links on the same contentlet with different field ids, in reverse order.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the rows are sorted by field id.
     */
    @Test
    void findBrokenLinks_sameInode_areSortedByFieldId() throws Exception {
        final InvalidLink onOtherField = link("inode-1");
        onOtherField.setField(OTHER_FIELD_ID);
        givenLinks(link("inode-1"), onOtherField);
        readableContent("inode-1");

        final List<BrokenLinkView> rows = helper.findBrokenLinks(TYPE, 0, 25, user).list();

        assertEquals(2, rows.size());
        assertEquals(OTHER_FIELD_NAME, rows.get(0).field());
        assertEquals(FIELD_NAME, rows.get(1).field());
    }

    /**
     * Given the stored links cannot be read.
     * When findBrokenLinks(type, 0, 25, user).
     * Then DotRuntimeException with the fixed message, not carrying the original text.
     */
    @Test
    void findBrokenLinks_linksCannotBeRead_hasFixedMessage() throws Exception {
        when(linkCheckerAPI.findAllByStructure(eq(TYPE), eq(0), eq(LinkCheckerHelper.READ_ALL_ROWS)))
                .thenThrow(new DotDataException("read-detail"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> helper.findBrokenLinks(TYPE, 0, 25, user));

        assertEquals(LinkCheckerHelper.READ_FAILURE_MESSAGE, thrown.getMessage());
        assertFalse(thrown.getMessage().contains("read-detail"));
    }

    /**
     * Given the stored links API returns a null list.
     * When findBrokenLinks(type, 0, 25, user).
     * Then empty list and total 0.
     */
    @Test
    void findBrokenLinks_nullLinkList_returnsEmpty() throws Exception {
        when(linkCheckerAPI.findAllByStructure(eq(TYPE), eq(0), eq(LinkCheckerHelper.READ_ALL_ROWS)))
                .thenReturn(null);

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.total());
    }

    /**
     * Given readable links returned by the API in a shuffled order.
     * When findBrokenLinks(type, 0, 2, user) and findBrokenLinks(type, 2, 2, user).
     * Then the rows come back sorted by inode and the two pages together hold every row exactly once.
     */
    @Test
    void findBrokenLinks_shuffledLinks_areSortedAndPagedOnce() throws Exception {
        givenLinks(link("inode-3"), link("inode-1"), link("inode-4"), link("inode-2"));
        readableContent("inode-1");
        readableContent("inode-2");
        readableContent("inode-3");
        readableContent("inode-4");

        final List<BrokenLinkView> firstPage = helper.findBrokenLinks(TYPE, 0, 2, user).list();
        final List<BrokenLinkView> secondPage = helper.findBrokenLinks(TYPE, 2, 2, user).list();

        final List<String> inodes = new ArrayList<>();
        firstPage.forEach(row -> inodes.add(row.inode()));
        secondPage.forEach(row -> inodes.add(row.inode()));
        assertEquals(List.of("inode-1", "inode-2", "inode-3", "inode-4"), inodes);
    }

    /**
     * Given two readable links on the same contentlet and field, with different URLs, in reverse order.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the rows are sorted by URL, a null URL last.
     */
    @Test
    void findBrokenLinks_sameInodeAndField_areSortedByUrl() throws Exception {
        final InvalidLink second = link("inode-1");
        second.setUrl("https://example.com/b");
        final InvalidLink first = link("inode-1");
        first.setUrl("https://example.com/a");
        final InvalidLink noUrl = link("inode-1");
        noUrl.setUrl(null);
        givenLinks(noUrl, second, first);
        readableContent("inode-1");

        final List<BrokenLinkView> rows = helper.findBrokenLinks(TYPE, 0, 25, user).list();

        assertEquals("https://example.com/a", rows.get(0).url());
        assertEquals("https://example.com/b", rows.get(1).url());
        assertNull(rows.get(2).url());
    }

    /**
     * Given a contentlet whose editor id no longer resolves.
     * When findBrokenLinks(type, 0, 25, user).
     * Then the row is returned with an empty modUserName.
     */
    @Test
    void findBrokenLinks_editorNotFound_keepsRowWithEmptyName() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");
        when(userAPI.loadUserById(EDITOR_ID)).thenThrow(new NoSuchUserException("User not found"));

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("", result.list().get(0).modUserName());
    }

    /**
     * Given two links; the lookup returns null for one inode.
     * When findBrokenLinks(type, 0, 25, user).
     * Then only the other row is returned.
     */
    @Test
    void findBrokenLinks_contentNotFound_isLeftOut() throws Exception {
        givenLinks(link("inode-1"), link("inode-2"));
        when(contentletAPI.find("inode-1", user, false)).thenReturn(null);
        readableContent("inode-2");

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("inode-2", result.list().get(0).inode());
    }

    /**
     * Given a field that can no longer be resolved.
     * When findBrokenLinks(type, 0, 25, user).
     * Then that row is left out and the total does not count it.
     */
    @Test
    void findBrokenLinks_fieldNotResolvable_isLeftOut() throws Exception {
        final InvalidLink unresolvable = link("inode-1");
        unresolvable.setField("missing-field");
        givenLinks(unresolvable, link("inode-2"));
        readableContent("inode-1");
        readableContent("inode-2");

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertEquals(1, result.total());
        assertEquals("inode-2", result.list().get(0).inode());
    }

    /**
     * Given a content type with no links.
     * When findBrokenLinks(type, 0, 25, user).
     * Then empty list and total 0.
     */
    @Test
    void findBrokenLinks_noLinks_returnsEmpty() throws Exception {
        givenLinks();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 25, user);

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.total());
    }

    /**
     * Given a blank content type id.
     * When findBrokenLinks(" ", 0, 25, user).
     * Then BadRequestException.
     */
    @Test
    void findBrokenLinks_blankContentType_isBadRequest() {
        assertThrows(BadRequestException.class, () -> helper.findBrokenLinks(" ", 0, 25, user));
    }

    /**
     * Given an archived contentlet.
     * When findBrokenLinks(type, 0, 25, user).
     * Then status is archived.
     */
    @Test
    void findBrokenLinks_status_archived() throws Exception {
        givenLinks(link("inode-1"));
        final Contentlet contentlet = readableContent("inode-1");
        when(contentlet.isArchived()).thenReturn(true);
        when(contentlet.isLive()).thenReturn(true);

        assertEquals("archived", helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0).status());
    }

    /**
     * Given a live, not archived contentlet.
     * When findBrokenLinks(type, 0, 25, user).
     * Then status is live.
     */
    @Test
    void findBrokenLinks_status_live() throws Exception {
        givenLinks(link("inode-1"));
        final Contentlet contentlet = readableContent("inode-1");
        when(contentlet.isLive()).thenReturn(true);
        when(contentlet.isWorking()).thenReturn(true);

        assertEquals("live", helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0).status());
    }

    /**
     * Given a working, not live, not archived contentlet.
     * When findBrokenLinks(type, 0, 25, user).
     * Then status is working.
     */
    @Test
    void findBrokenLinks_status_working() throws Exception {
        givenLinks(link("inode-1"));
        final Contentlet contentlet = readableContent("inode-1");
        when(contentlet.isWorking()).thenReturn(true);

        assertEquals("working", helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0).status());
    }

    /**
     * Given a contentlet that is neither archived, live nor working.
     * When findBrokenLinks(type, 0, 25, user).
     * Then status is empty.
     */
    @Test
    void findBrokenLinks_status_none() throws Exception {
        givenLinks(link("inode-1"));
        readableContent("inode-1");

        assertEquals("", helper.findBrokenLinks(TYPE, 0, 25, user).list().get(0).status());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, 0, 2, user).
     * Then the first two rows and total 3.
     */
    @Test
    void findBrokenLinks_firstPage() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, 2, user);

        assertEquals(3, result.total());
        assertEquals(2, result.list().size());
        assertEquals("inode-1", result.list().get(0).inode());
        assertEquals("inode-2", result.list().get(1).inode());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, 2, 2, user).
     * Then the last row and total 3.
     */
    @Test
    void findBrokenLinks_lastPartialPage() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 2, 2, user);

        assertEquals(3, result.total());
        assertEquals(1, result.list().size());
        assertEquals("inode-3", result.list().get(0).inode());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, 3, 2, user).
     * Then empty list and total 3.
     */
    @Test
    void findBrokenLinks_offsetAtEnd_returnsEmptyPage() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 3, 2, user);

        assertEquals(3, result.total());
        assertTrue(result.list().isEmpty());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, 10, 2, user).
     * Then empty list and total 3.
     */
    @Test
    void findBrokenLinks_offsetBeyondReadableRows_returnsEmptyPage() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 10, 2, user);

        assertEquals(3, result.total());
        assertTrue(result.list().isEmpty());
    }

    /**
     * Given three readable rows.
     * When findBrokenLinks(type, Integer.MAX_VALUE, 100, user).
     * Then empty list and total 3, with no exception.
     */
    @Test
    void findBrokenLinks_maxOffset_returnsEmptyPage() throws Exception {
        givenThreeReadableRows();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, Integer.MAX_VALUE, 100, user);

        assertEquals(3, result.total());
        assertTrue(result.list().isEmpty());
    }

    /**
     * Given any content type.
     * When findBrokenLinks(type, -1, 25, user).
     * Then BadRequestException.
     */
    @Test
    void findBrokenLinks_negativeOffset_isBadRequest() {
        assertThrows(BadRequestException.class, () -> helper.findBrokenLinks(TYPE, -1, 25, user));
    }

    /**
     * Given any content type.
     * When findBrokenLinks(type, 0, 0, user).
     * Then BadRequestException.
     */
    @Test
    void findBrokenLinks_zeroLimit_isBadRequest() {
        assertThrows(BadRequestException.class, () -> helper.findBrokenLinks(TYPE, 0, 0, user));
    }

    /**
     * Given any content type.
     * When findBrokenLinks with limit 100, then with limit 101.
     * Then 100 is accepted and 101 is a BadRequestException.
     */
    @Test
    void findBrokenLinks_limitUpperBound() throws Exception {
        givenLinks();

        final BrokenLinksView result = helper.findBrokenLinks(TYPE, 0, LinkCheckerHelper.MAX_LIMIT, user);

        assertEquals(0, result.total());
        assertThrows(BadRequestException.class,
                () -> helper.findBrokenLinks(TYPE, 0, LinkCheckerHelper.MAX_LIMIT + 1, user));
    }

    /**
     * Given an available scheduler.
     * When runCheckNow().
     * Then exactly one link-check job is scheduled, named linkCheckerJob-* in the dotcms_jobs group.
     */
    @Test
    void runCheckNow_schedulesOneJob() throws Exception {
        helper.runCheckNow();

        final ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler, times(1)).scheduleJob(jobCaptor.capture(), any(Trigger.class));
        final JobDetail jobDetail = jobCaptor.getValue();
        assertSame(LinkCheckerJob.class, jobDetail.getJobClass());
        assertTrue(jobDetail.getName().startsWith("linkCheckerJob-"));
        assertEquals("dotcms_jobs", jobDetail.getGroup());
    }

    /**
     * Given a scheduler that fails.
     * When runCheckNow().
     * Then DotRuntimeException whose message does not carry the scheduler's text.
     */
    @Test
    void runCheckNow_schedulerFailure_hasFixedMessage() throws Exception {
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenThrow(new SchedulerException("secret-detail"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class, () -> helper.runCheckNow());

        assertFalse(String.valueOf(thrown.getMessage()).contains("secret-detail"));
    }

    private void givenLinks(final InvalidLink... links) throws Exception {
        when(linkCheckerAPI.findAllByStructure(eq(TYPE), eq(0), eq(LinkCheckerHelper.READ_ALL_ROWS)))
                .thenReturn(List.of(links));
    }

    private void givenThreeReadableRows() throws Exception {
        givenLinks(link("inode-1"), link("inode-2"), link("inode-3"));
        readableContent("inode-1");
        readableContent("inode-2");
        readableContent("inode-3");
    }

    private static InvalidLink link(final String inode) {
        final InvalidLink link = new InvalidLink();
        link.setInode(inode);
        link.setField(FIELD_ID);
        link.setTitle(LINK_TITLE);
        link.setUrl(URL);
        return link;
    }

    private Contentlet readableContent(final String inode) throws Exception {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getTitle()).thenReturn("Sample title " + inode);
        when(contentlet.getModDate()).thenReturn(MOD_DATE);
        when(contentlet.getModUser()).thenReturn(EDITOR_ID);
        when(contentlet.isArchived()).thenReturn(false);
        when(contentlet.isLive()).thenReturn(false);
        when(contentlet.isWorking()).thenReturn(false);
        when(contentletAPI.find(inode, user, false)).thenReturn(contentlet);
        return contentlet;
    }

    private void unreadableContent(final String inode) throws Exception {
        when(contentletAPI.find(inode, user, false))
                .thenThrow(new DotSecurityException("User cannot read this content"));
    }
}
