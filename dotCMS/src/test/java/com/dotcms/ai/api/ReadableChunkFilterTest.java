package com.dotcms.ai.api;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies how {@link ReadableChunkFilter} decides which ranked embedding chunks a user gets back:
 * only chunks of readable contentlets, in rank order, cut to the requested page, without ever
 * skipping the permission check.
 *
 * <p>Chunks are plain strings of the form {@code "<inode>#<rank>"} so the test needs no database.</p>
 *
 * @author hassandotcms
 */
public class ReadableChunkFilterTest {

    private static final Function<String, String> INODE_OF = chunk -> chunk.split("#")[0];

    private ContentletAPI contentletAPI;
    private PermissionAPI permissionAPI;
    private User user;

    @Before
    public void before() {
        contentletAPI = mock(ContentletAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        user = mock(User.class);
    }

    /**
     * Given chunks from readable, unreadable and missing contentlets, plus a cache row,
     * only the readable chunks come back, in their original order (AC-005).
     */
    @Test
    public void test_filter_keepsOnlyReadableChunks_inRankOrder() throws Exception {
        givenContentletsExist("r1", "u1", "r2");
        givenReadable("r1", "r2");
        final List<String> candidates = List.of("r1#0", "u1#1", "r1#2", "missing#3", "cache#4", "r2#5");

        final List<String> result = new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(candidates, INODE_OF, user, 0, 10);

        assertEquals(List.of("r1#0", "r1#2", "r2#5"), result);
        // the batch overload skips the live-only and owner rules (FR-001)
        verify(permissionAPI, never()).filterCollection(anyCollection(), anyInt(), any(User.class), anyBoolean());
    }

    /**
     * A request with no user still goes through the permission check, which decides it as
     * Anonymous; there is no bypass (FR-006, AC-005).
     */
    @Test
    public void test_filter_nullUser_isStillPermissionChecked() throws Exception {
        givenContentletsExist("r1", "u1");
        givenReadable("r1");

        final List<String> result = new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(List.of("r1#0", "u1#1"), INODE_OF, null, 0, 10);

        assertEquals(List.of("r1#0"), result);
        verify(permissionAPI).filterCollection(anyList(), eq(PermissionAPI.PERMISSION_READ), eq(true), isNull());
    }

    /**
     * Checking stops once {@code offset + limit} readable chunks are collected: with more readable
     * contentlets than one batch holds and a page of 2, only the first batch is ever loaded
     * (FR-012, AC-005).
     */
    @Test
    public void test_filter_stopsCheckingOncePageIsFull() throws Exception {
        final int total = ReadableChunkFilter.BATCH_SIZE + 50;
        final String[] inodes = IntStream.range(0, total).mapToObj(i -> "c" + i).toArray(String[]::new);
        givenContentletsExist(inodes);
        givenReadable(inodes);
        final List<String> candidates = IntStream.range(0, total).mapToObj(i -> "c" + i + "#" + i)
                .collect(Collectors.toList());

        final List<String> result = new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(candidates, INODE_OF, user, 0, 2);

        assertEquals(List.of("c0#0", "c1#1"), result);
        verify(contentletAPI, times(1)).findContentlets(anyList());
    }

    /**
     * {@code offset} and {@code limit} count readable chunks, not candidates (FR-008, AC-005).
     */
    @Test
    public void test_filter_slicesOffsetAndLimitOverReadableChunks() throws Exception {
        givenContentletsExist("a", "u", "b", "c", "d");
        givenReadable("a", "b", "c", "d");
        final List<String> candidates = List.of("a#0", "u#1", "b#2", "c#3", "d#4");

        final List<String> result = new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(candidates, INODE_OF, user, 1, 2);

        assertEquals(List.of("b#2", "c#3"), result);
    }

    /**
     * A failure while loading contentlets propagates instead of returning an empty (and
     * misleading "no matching content") result (FR-011).
     */
    @Test
    public void test_filter_loadFailure_propagates() throws Exception {
        when(contentletAPI.findContentlets(anyList())).thenThrow(new DotDataException("db down"));

        assertThrows(DotRuntimeException.class, () -> new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(List.of("r1#0"), INODE_OF, user, 0, 10));
    }

    /**
     * Anonymous callers (no user, or the Anonymous user) read at most 200 rows ahead; logged-in
     * callers read up to the configured cap (FR-012).
     */
    @Test
    public void test_candidatesToFetch_anonymousLooksAheadLessThanLoggedIn() {
        final User anonymous = mock(User.class);
        when(anonymous.isAnonymousUser()).thenReturn(true);

        assertEquals(200, ReadableChunkFilter.candidatesToFetch(null, 0, 50, 1000));
        assertEquals(200, ReadableChunkFilter.candidatesToFetch(anonymous, 0, 50, 1000));
        assertEquals(1000, ReadableChunkFilter.candidatesToFetch(user, 0, 50, 1000));
    }

    /**
     * The read-ahead never cuts the requested page, a site cap below 200 also applies to anonymous
     * callers, and {@code offset} never grows how many rows are read, so a huge offset cannot make
     * a request read the whole table (FR-012).
     */
    @Test
    public void test_candidatesToFetch_neverBelowPage_siteCapApplies_offsetDoesNotGrowFetch() {
        assertEquals(500, ReadableChunkFilter.candidatesToFetch(null, 0, 500, 1000));
        assertEquals(100, ReadableChunkFilter.candidatesToFetch(null, 0, 50, 100));
        assertEquals(1000, ReadableChunkFilter.candidatesToFetch(user, 0, 50, 0));
        assertEquals(200, ReadableChunkFilter.candidatesToFetch(null, 300, 100, 1000));
        assertEquals(1000, ReadableChunkFilter.candidatesToFetch(user, Integer.MAX_VALUE, 50, 1000));
    }

    /**
     * An offset past the readable chunks returns an empty page instead of failing, even when
     * {@code offset + limit} overflows an int.
     */
    @Test
    public void test_filter_hugeOffset_returnsEmptyPage() throws Exception {
        givenContentletsExist("r1");
        givenReadable("r1");

        final List<String> result = new ReadableChunkFilter(contentletAPI, permissionAPI)
                .filter(List.of("r1#0"), INODE_OF, user, Integer.MAX_VALUE, 50);

        assertEquals(List.of(), result);
    }

    private void givenContentletsExist(final String... inodes) throws Exception {
        final Set<String> existing = Set.of(inodes);
        when(contentletAPI.findContentlets(anyList())).thenAnswer(invocation -> {
            final List<String> requested = invocation.getArgument(0);
            return requested.stream().filter(existing::contains).map(ReadableChunkFilterTest::contentlet)
                    .collect(Collectors.toList());
        });
    }

    @SuppressWarnings("unchecked")
    private void givenReadable(final String... inodes) throws Exception {
        final Set<String> readable = Set.of(inodes);
        when(permissionAPI.filterCollection(anyList(), eq(PermissionAPI.PERMISSION_READ), eq(true), any()))
                .thenAnswer(invocation -> ((Collection<Contentlet>) invocation.getArgument(0)).stream()
                        .filter(contentlet -> readable.contains(contentlet.getInode()))
                        .collect(Collectors.toList()));
    }

    private static Contentlet contentlet(final String inode) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setInode(inode);
        return contentlet;
    }

}
