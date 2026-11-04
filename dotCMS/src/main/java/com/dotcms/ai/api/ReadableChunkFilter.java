package com.dotcms.ai.api;

import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps only the embedding chunks whose source contentlet a user can READ, and cuts the
 * requested page out of what is left.
 *
 * <p>Chunks are checked in the order they were ranked, a batch of distinct inodes at a time, and
 * checking stops as soon as {@code offset + limit} readable chunks have been collected. A chunk
 * whose inode has no contentlet behind it (deleted content, cache rows) is never returned. A
 * {@code null} user is checked as the Anonymous user.</p>
 *
 * <p>The permission check is the per-item
 * {@link PermissionAPI#filterCollection(List, int, boolean, User)}, the same decision
 * {@code ContentletAPI.find(inode, user, true)} makes. The batch
 * {@code filterCollection(Collection, int, User, boolean)} overload must not be used here: it skips
 * the live-only rule for anonymous and front-end users and the owner rule, so it would hand draft
 * text to anonymous visitors.</p>
 *
 * @author hassandotcms
 */
final class ReadableChunkFilter {

    /** Distinct contentlets loaded and permission-checked per round trip. */
    static final int BATCH_SIZE = 200;

    /** Default for {@code embeddingsSearchCandidateCap}: rows read ahead for logged-in callers. */
    static final int DEFAULT_CANDIDATE_CAP = 1000;

    /** Rows read ahead for anonymous callers, who reach retrieval on every public page render. */
    static final int ANONYMOUS_CANDIDATE_CAP = 200;

    private final ContentletAPI contentletAPI;
    private final PermissionAPI permissionAPI;

    ReadableChunkFilter(final ContentletAPI contentletAPI, final PermissionAPI permissionAPI) {
        this.contentletAPI = contentletAPI;
        this.permissionAPI = permissionAPI;
    }

    /**
     * How many ranked rows to read before filtering, so that readable matches ranked below
     * restricted ones can still fill the page. Never less than {@code limit}. Anonymous callers
     * ({@code null} or the Anonymous user) look ahead at most {@link #ANONYMOUS_CANDIDATE_CAP}
     * rows; everyone else up to the configured cap. {@code offset} deliberately does not grow
     * the read: the page is taken from within these rows, so a huge offset cannot make one
     * request read the whole table, and a page beyond them comes back short or empty.
     *
     * @param user          the caller
     * @param offset        readable chunks to skip
     * @param limit         readable chunks requested
     * @param configuredCap the {@code embeddingsSearchCandidateCap} setting; 0 or less means the default
     * @return the number of rows to read
     */
    static int candidatesToFetch(final User user, final int offset, final int limit, final int configuredCap) {
        final int configured = configuredCap > 0 ? configuredCap : DEFAULT_CANDIDATE_CAP;
        final boolean anonymous = user == null || user.isAnonymousUser();
        final int cap = anonymous ? Math.min(configured, ANONYMOUS_CANDIDATE_CAP) : configured;
        return Math.max(Math.max(0, limit), cap);
    }

    /**
     * Returns the readable chunks in positions {@code [offset, offset + limit)}, counting
     * readable chunks only, in the order the candidates were given.
     *
     * @param candidates the ranked chunks returned by the embeddings query
     * @param inodeOf    reads the source contentlet inode of a chunk
     * @param user       the caller; {@code null} is checked as Anonymous
     * @param offset     readable chunks to skip
     * @param limit      readable chunks to return at most
     * @param <T>        the chunk type
     * @return the readable page, never {@code null}
     * @throws DotRuntimeException if the contentlets cannot be loaded or their permissions checked
     */
    <T> List<T> filter(final List<T> candidates, final Function<T, String> inodeOf, final User user,
                       final int offset, final int limit) {
        // long math: offset + limit can overflow an int
        final int wanted = (int) Math.min((long) Math.max(0, offset) + Math.max(0, limit), Integer.MAX_VALUE);
        final Map<String, Boolean> readable = new HashMap<>();
        final List<T> collected = new ArrayList<>();

        for (int i = 0; i < candidates.size() && collected.size() < wanted; i++) {
            final T chunk = candidates.get(i);
            final String inode = inodeOf.apply(chunk);
            if (!UtilMethods.isSet(inode)) {
                continue;
            }
            if (!readable.containsKey(inode)) {
                decideNextBatch(candidates, inodeOf, i, readable, user);
            }
            if (readable.get(inode)) {
                collected.add(chunk);
            }
        }

        final int from = Math.min(Math.max(0, offset), collected.size());
        return new ArrayList<>(collected.subList(from, Math.min(wanted, collected.size())));
    }

    /**
     * Decides READ for the next batch of undecided distinct inodes, starting at {@code start}.
     */
    private <T> void decideNextBatch(final List<T> candidates, final Function<T, String> inodeOf, final int start,
                                     final Map<String, Boolean> readable, final User user) {
        final Set<String> batch = new LinkedHashSet<>();
        for (int i = start; i < candidates.size() && batch.size() < BATCH_SIZE; i++) {
            final String inode = inodeOf.apply(candidates.get(i));
            if (UtilMethods.isSet(inode) && !readable.containsKey(inode)) {
                batch.add(inode);
            }
        }

        try {
            final List<Contentlet> loaded = contentletAPI.findContentlets(new ArrayList<>(batch));
            final Set<String> permitted = permissionAPI
                    .filterCollection(loaded, PermissionAPI.PERMISSION_READ, true, user)
                    .stream()
                    .map(Contentlet::getInode)
                    .collect(Collectors.toSet());
            batch.forEach(inode -> readable.put(inode, permitted.contains(inode)));
        } catch (final DotDataException | DotSecurityException e) {
            throw new DotRuntimeException("Unable to check READ permission on embedded content", e);
        }
    }

}
