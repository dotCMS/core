package com.dotcms.rest.api.v1.linkchecker;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.enterprise.linkchecker.LinkCheckerJob;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds the broken-links list of the Link Checker tool and queues runs of the link check.
 * <p>
 * Every row's contentlet is read with the requesting user, so a user only receives the rows whose
 * content they can read, and the total counts only those rows. Rows whose contentlet, field or
 * content type no longer exists are left out. The last editor appears by full name only. Rows
 * are sorted by link inode, then field id, then URL, so paging is stable across requests.
 */
public class LinkCheckerHelper {

    /** Largest page size a client may request. */
    public static final int MAX_LIMIT = 100;

    /** Page size used to read every stored link of a content type in one call. */
    static final int READ_ALL_ROWS = Integer.MAX_VALUE;

    private static final String DATE_PATTERN = "MM/dd/yyyy hh:mm a";
    /** Message of the exception thrown when the broken links list cannot be read. */
    static final String READ_FAILURE_MESSAGE = "Unable to read the broken links list";

    private static final String QUEUE_FAILURE_MESSAGE = "Unable to queue the link check";
    private static final String EDITOR_FAILURE_MESSAGE = "Unable to resolve the last editor of a broken link row";

    /** Deepest level of the cause chain inspected when looking for a missing database row. */
    private static final int MAX_CAUSE_DEPTH = 10;

    /** Order of the rows: link inode, then field id, then URL; missing values go last. */
    private static final Comparator<Candidate> ROW_ORDER = Comparator
            .comparing((Candidate candidate) -> candidate.link().getInode(),
                    Comparator.nullsLast(Comparator.<String>naturalOrder()))
            .thenComparing(candidate -> candidate.link().getField(),
                    Comparator.nullsLast(Comparator.<String>naturalOrder()))
            .thenComparing(candidate -> candidate.link().getUrl(),
                    Comparator.nullsLast(Comparator.<String>naturalOrder()));

    private final LinkCheckerAPI linkCheckerAPI;
    private final ContentletAPI contentletAPI;
    private final UserAPI userAPI;
    private final Function<String, Field> fieldLookup;
    private final Function<String, Structure> contentTypeLookup;
    private final Supplier<Scheduler> schedulerSupplier;

    /**
     * Creates the helper with the production services, caches and scheduler.
     */
    public LinkCheckerHelper() {
        this(APILocator.getLinkCheckerAPI(),
                APILocator.getContentletAPI(),
                APILocator.getUserAPI(),
                LinkCheckerHelper::findFieldOrNull,
                inode -> CacheLocator.getContentTypeCache().getStructureByInode(inode),
                QuartzUtils::getScheduler);
    }

    /**
     * Creates the helper with the given collaborators.
     *
     * @param linkCheckerAPI    reads the stored broken links
     * @param contentletAPI     reads each row's contentlet as the requesting user
     * @param userAPI           resolves the last editor of a contentlet
     * @param fieldLookup       resolves a field by id, returning {@code null} when it does not exist
     * @param contentTypeLookup resolves a content type by inode, returning {@code null} or a content
     *                          type without an inode when it does not exist
     * @param schedulerSupplier provides the scheduler used to queue a link check
     */
    @VisibleForTesting
    LinkCheckerHelper(final LinkCheckerAPI linkCheckerAPI,
                      final ContentletAPI contentletAPI,
                      final UserAPI userAPI,
                      final Function<String, Field> fieldLookup,
                      final Function<String, Structure> contentTypeLookup,
                      final Supplier<Scheduler> schedulerSupplier) {
        this.linkCheckerAPI = linkCheckerAPI;
        this.contentletAPI = contentletAPI;
        this.userAPI = userAPI;
        this.fieldLookup = fieldLookup;
        this.contentTypeLookup = contentTypeLookup;
        this.schedulerSupplier = schedulerSupplier;
    }

    /**
     * Returns one page of the broken links of a content type, limited to the rows whose content the
     * user can read.
     *
     * @param contentTypeId the content type whose links are listed; must not be blank
     * @param offset        the index of the first row of the page; must not be negative
     * @param limit         the page size, from 1 to {@link #MAX_LIMIT}
     * @param user          the requesting user; each contentlet is read with this user
     * @return the rows of the page and the number of rows the user can read across all pages; an
     *         empty page with total 0 when the content type is unknown or has no links
     * @throws BadRequestException when the content type is blank, the offset is negative or the
     *                             limit is outside 1..{@link #MAX_LIMIT}
     * @throws DotRuntimeException when the links, their content, fields or content types cannot be
     *                             read; the message is always {@link #READ_FAILURE_MESSAGE}
     */
    public BrokenLinksView findBrokenLinks(final String contentTypeId, final int offset,
                                           final int limit, final User user) {
        validate(contentTypeId, offset, limit);
        try {
            final List<Candidate> candidates = findReadableCandidates(contentTypeId, user);
            final int total = candidates.size();
            final int from = (int) Math.min((long) offset, total);
            final int to = (int) Math.min((long) offset + limit, total);
            final List<BrokenLinkView> page = new ArrayList<>(to - from);
            for (final Candidate candidate : candidates.subList(from, to)) {
                page.add(toView(candidate));
            }
            return new BrokenLinksView(page, total);
        } catch (DotDataException | DotSecurityException | DotRuntimeException e) {
            Logger.error(this, READ_FAILURE_MESSAGE, e);
            throw new DotRuntimeException(READ_FAILURE_MESSAGE);
        }
    }

    /**
     * Queues one run of the link check, starting immediately.
     *
     * @throws DotRuntimeException when the scheduler cannot queue the run
     */
    public void runCheckNow() {
        final String randomId = UUIDGenerator.generateUuid();
        final JobDetail jobDetail = new JobDetail("linkCheckerJob-" + randomId, "dotcms_jobs",
                LinkCheckerJob.class);
        jobDetail.setJobDataMap(new JobDataMap());
        jobDetail.setDurability(false);
        jobDetail.setVolatility(false);
        jobDetail.setRequestsRecovery(true);
        final Trigger trigger = new SimpleTrigger("linkCheckerTrigger-" + randomId, "group20", new Date());
        try {
            schedulerSupplier.get().scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            Logger.error(this, QUEUE_FAILURE_MESSAGE, e);
            throw new DotRuntimeException(QUEUE_FAILURE_MESSAGE);
        }
    }

    /**
     * Rejects a blank content type, a negative offset or a limit outside 1..{@link #MAX_LIMIT}.
     *
     * @param contentTypeId the requested content type
     * @param offset        the requested offset
     * @param limit         the requested page size
     * @throws BadRequestException when any of the values is not accepted
     */
    private void validate(final String contentTypeId, final int offset, final int limit) {
        if (!UtilMethods.isSet(contentTypeId) || contentTypeId.trim().isEmpty()) {
            throw new BadRequestException("A content type is required");
        }
        if (offset < 0) {
            throw new BadRequestException("The offset must not be negative");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("The limit must be between 1 and " + MAX_LIMIT);
        }
    }

    /**
     * Reads every stored link of the content type and keeps those whose contentlet the user can
     * read and whose field and content type still exist; a content type lookup that answers
     * {@code null} or a content type without an inode counts as a missing content type. The rows are sorted by link inode, then
     * field id, then URL (missing values last), so every request cuts the pages the same way
     * whatever order the links are stored in.
     *
     * @param contentTypeId the content type whose links are read
     * @param user          the requesting user
     * @return the rows the user can read, sorted
     * @throws DotDataException  when the links or a contentlet cannot be read
     * @throws DotStateException   when a field lookup fails for a reason other than a missing field
     * @throws DotRuntimeException when a contentlet lookup fails in the data layer
     */
    private List<Candidate> findReadableCandidates(final String contentTypeId, final User user)
            throws DotDataException {
        final List<InvalidLink> links = linkCheckerAPI.findAllByStructure(contentTypeId, 0, READ_ALL_ROWS);
        final List<Candidate> candidates = new ArrayList<>();
        if (links == null) {
            return candidates;
        }
        for (final InvalidLink link : links) {
            final Contentlet contentlet;
            try {
                contentlet = contentletAPI.find(link.getInode(), user, false);
            } catch (DotSecurityException e) {
                continue;
            }
            if (contentlet == null) {
                continue;
            }
            final Field field = fieldLookup.apply(link.getField());
            if (field == null) {
                continue;
            }
            final Structure contentType = contentTypeLookup.apply(field.getStructureInode());
            if (contentType == null || !UtilMethods.isSet(contentType.getInode())) {
                continue;
            }
            candidates.add(new Candidate(link, contentlet, field, contentType));
        }
        candidates.sort(ROW_ORDER);
        return candidates;
    }

    /**
     * Builds the response row of a readable link.
     *
     * @param candidate the link with its contentlet, field and content type
     * @return the row
     * @throws DotDataException     when the contentlet status cannot be read
     * @throws DotSecurityException when the contentlet status cannot be read
     */
    private BrokenLinkView toView(final Candidate candidate) throws DotDataException, DotSecurityException {
        final Contentlet contentlet = candidate.contentlet();
        final Date modDate = contentlet.getModDate();
        final String formattedModDate = modDate == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(modDate);
        return new BrokenLinkView(
                candidate.link().getInode(),
                contentlet.getTitle(),
                statusOf(contentlet),
                candidate.field().getFieldName(),
                candidate.contentType().getName(),
                formattedModDate,
                editorNameOf(contentlet),
                candidate.link().getTitle(),
                candidate.link().getUrl());
    }

    /**
     * Returns the contentlet status as shown in the list.
     *
     * @param contentlet the contentlet
     * @return "archived", "live", "working" or empty when none applies
     * @throws DotDataException     when the status cannot be read
     * @throws DotSecurityException when the status cannot be read
     */
    private static String statusOf(final Contentlet contentlet) throws DotDataException, DotSecurityException {
        if (contentlet.isArchived()) {
            return "archived";
        }
        if (contentlet.isLive()) {
            return "live";
        }
        if (contentlet.isWorking()) {
            return "working";
        }
        return "";
    }

    /**
     * Returns the full name of the contentlet's last editor.
     *
     * @param contentlet the contentlet
     * @return the editor's full name, or empty when the editor is not set, has no full name, no
     *         longer exists or cannot be resolved; an unexpected failure to resolve it is logged as a warning
     * @throws DotDataException when the editor cannot be read
     */
    private String editorNameOf(final Contentlet contentlet) throws DotDataException {
        final String modUserId = contentlet.getModUser();
        if (!UtilMethods.isSet(modUserId)) {
            return "";
        }
        try {
            final User editor = userAPI.loadUserById(modUserId);
            return editor == null || editor.getFullName() == null ? "" : editor.getFullName();
        } catch (NoSuchUserException e) {
            return "";
        } catch (DotStateException | DotSecurityException e) {
            Logger.warn(this, EDITOR_FAILURE_MESSAGE + ": " + e.getClass().getSimpleName());
            return "";
        }
    }

    /**
     * Resolves a field by id.
     *
     * @param fieldId the field id
     * @return the field, or {@code null} when no field with that id exists
     * @throws DotStateException when the field cannot be read for any other reason
     */
    private static Field findFieldOrNull(final String fieldId) {
        try {
            return FieldsCache.getField(fieldId);
        } catch (DotStateException e) {
            if (isCausedByMissingRow(e)) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Tells whether a failure was caused by a row that does not exist in the database.
     *
     * @param failure the failure to inspect
     * @return {@code true} when the first {@link #MAX_CAUSE_DEPTH} levels of the cause chain contain
     *         a {@link NotFoundInDbException}
     */
    @VisibleForTesting
    static boolean isCausedByMissingRow(final Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof NotFoundInDbException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /** A link the user can read, with its contentlet, field and content type. */
    private record Candidate(InvalidLink link, Contentlet contentlet, Field field, Structure contentType) {
    }
}
