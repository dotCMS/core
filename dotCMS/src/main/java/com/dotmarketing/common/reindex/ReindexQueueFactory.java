package com.dotmarketing.common.reindex;

import com.dotcms.contenttype.model.type.ContentType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import com.google.common.collect.ImmutableList;


/**
 * Provides access to all the routines associated to the re-indexation process in dotCMS.
 *
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 */
public class ReindexQueueFactory {

    private final String REINDEX_JOURNAL_INSERT =
            "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) values (?, ?, ?, ?, ?)";

    // if there are old records in the reindexQueue that have been claimed by a server that is no longer
    // running, tee them back up
    private static final int REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC = Config.getIntProperty(
            "REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC", 120);

    public static int REINDEX_RECORDS_TO_FETCH = Config.getIntProperty("REINDEX_RECORDS_TO_FETCH",
            100);

    public static final int REINDEX_MAX_FAILURE_ATTEMPTS = Config.getIntProperty(
            "RETRY_FAILED_INDEX_TIMES", 5);

    private static final ConcurrentLinkedQueue<ReindexEntry> queue = new ConcurrentLinkedQueue<>();

    public ConcurrentLinkedQueue<ReindexEntry> getLocalQueue() {
        return queue;
    }

    public enum Priority {
        ASAP, NORMAL, STRUCTURE, REINDEX, ERROR;

        public int dbValue() {
            return this.ordinal() * 100;
        }
    }

    public enum ReindexAction {
        NONE, REINDEX, DELETE;
    }


    public ReindexQueueFactory() {

    }

    private String timestampSQL() {
        if (DbConnectionFactory.isMsSql()) {
            return "GETDATE()";
        } else if (DbConnectionFactory.isOracle()) {
            return "CAST(SYSTIMESTAMP AS TIMESTAMP)";
        } else {
            return "NOW()";
        }
    }

    protected void addAllToReindexQueue() throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql =
                    "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) "
                            + " select distinct identifier,identifier," + Priority.REINDEX.dbValue()
                            + "," + ReindexAction.REINDEX.ordinal() + ", "
                            + timestampSQL()
                            + " from contentlet_version_info where identifier is not null";
            dc.setSQL(sql);
            dc.loadResult();
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

    protected void addStructureReindexEntries(final ContentType contentType)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql =
                    "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) "
                            + " select i.id,i.id," + Priority.STRUCTURE.dbValue() + ","
                            + ReindexAction.REINDEX.ordinal()
                            + "," + timestampSQL() + " from identifier i "
                            + " where i.asset_subtype = ? and i.id is not null";
            dc.setSQL(sql);
            dc.addParam(contentType.variable());
            dc.loadResult();

        } catch (Exception ex) {
            Logger.fatal(this, "Error  unlocking the reindex journal table" + ex);
        }
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

    protected boolean areRecordsLeftToIndex() throws DotDataException {

        try {
            return recordsInQueue() > 0;
        } catch (Exception ex) {
            Logger.warn(this, "Error unlocking the reindex journal table" + ex);
        }
        return false;
    }

    protected void deleteReindexAndFailedRecords() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority >= ?");
        dc.addParam(Priority.REINDEX.dbValue());
        dc.loadResult();
    }


    /**
     * deletes reindex records (when a full reindex has been fired) - and excludes stucture or host
     * index records.
     *
     * @throws DotDataException
     */
    protected void deleteReindexRecords() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority >= ? and  priority < ? ");
        dc.addParam(Priority.REINDEX.dbValue());
        dc.addParam(Priority.ERROR.dbValue());
        dc.loadResult();
    }

    /**
     * returns if there are any reindex records in the queue
     *
     * @throws DotDataException
     */
    protected boolean hasReindexRecords() throws DotDataException {
        DotConnect dc = new DotConnect();
        String sql = DbConnectionFactory.isMsSql()
                ? "SELECT TOP 1 id from dist_reindex_journal where priority >= ? and  priority < ?"
                : DbConnectionFactory.isOracle() ?
                        "select 1 from dist_reindex_journal where priority >= ? and  priority < ? and rownum=1"
                        : "select 1 from dist_reindex_journal where priority >= ? and  priority < ? limit 1";
        dc.setSQL(sql);

        dc.addParam(Priority.REINDEX.dbValue());
        dc.addParam(Priority.ERROR.dbValue());
        return !dc.loadResults().isEmpty();
    }

    protected void deleteFailedRecords() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority > ?");
        dc.addParam(Priority.REINDEX.dbValue());
        dc.loadResult();
    }


    protected long failedRecordCount() throws DotDataException {
        DotConnect dc = new DotConnect();
        return dc.setSQL("SELECT count(*) as count from dist_reindex_journal where priority >= ? ")
                .addParam(Priority.ERROR.dbValue()).getInt("count");
    }


    @CloseDBIfOpened
    protected List<ReindexEntry> getFailedReindexRecords() throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL(
                "SELECT id, ident_to_index, priority, dist_action, index_val, time_entered FROM dist_reindex_journal WHERE priority > ?");
        dc.addParam(ReindexQueueFactory.Priority.REINDEX.dbValue());
        final List<Map<String, Object>> failedRecords = dc.loadObjectResults();
        final List<ReindexEntry> failed = new ArrayList<>();
        long identifier;
        int priority;
        for (final Map<String, Object> map : failedRecords) {
            final String indexVal = UtilMethods.isSet(map.get("index_val")) ? String.class.cast(
                    map.get("index_val"))
                    : StringUtils.EMPTY;

            if (DbConnectionFactory.isOracle()) {
                BigDecimal rowVal = (BigDecimal) map.get("id");
                identifier = Long.valueOf(rowVal.toPlainString());
                rowVal = (BigDecimal) map.get("priority");
                priority = Integer.valueOf(rowVal.toPlainString());
            } else {
                identifier = (Long) map.get("id");
                priority = Integer.parseInt(map.get("priority").toString());
            }

            final ReindexEntry ridx = ReindexEntry.builder()
                    .id(identifier)
                    .identToIndex((String) map.get("ident_to_index"))
                    .priority(priority)
                    // A removal that exhausted its retries is still a removal. Reporting it as a
                    // reindex sends whoever reads this list looking for content that no longer
                    // exists, instead of for an index document that should have been removed.
                    .isDelete(isDeleteAction(map))
                    .timeEntered((Date) map.get("time_entered"))
                    .lastResult(indexVal)
                    .build();
            failed.add(ridx);
        }
        return failed;
    }

    /**
     * Acknowledges a single processed entry.
     *
     * <p>Rows for the same identifier up to and including this one are removed together: they are
     * earlier statements about the same content, already superseded by the entry just applied.
     * The {@code id <= ?} bound is what keeps the sweep honest — a row written <em>after</em> this
     * batch was loaded (a delete queued while the reindex was in flight) describes work nobody has
     * done yet, and acknowledging it here would drop it silently (#37276).</p>
     */
    protected void deleteReindexEntry(ReindexEntry iJournal) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where (ident_to_index = ? and id <= ?) or id = ?");
        dc.addParam(iJournal.getIdentToIndex());
        dc.addParam(iJournal.getId());
        dc.addParam(iJournal.getId());
        dc.loadResult();
    }

    protected void deleteReindexEntry(String identifier) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where ident_to_index = ? ");
        dc.addParam(identifier);
        dc.loadResult();
    }

    /**
     * Acknowledges a batch of processed entries.
     *
     * <p>Bounded by row id for the reason given on {@link #deleteReindexEntry(ReindexEntry)}: the
     * batch may have been in flight for as long as the bulk write took, and anything queued for
     * the same identifier in that window has not been applied yet.</p>
     */
    protected void deleteReindexEntry(final List<ReindexEntry> recordsToDelete)
            throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        final int batchSize = REINDEX_RECORDS_TO_FETCH / 5;
        int from = 0;
        while (from <= recordsToDelete.size()) {
            dotConnect.executeBatch(
                    "DELETE FROM dist_reindex_journal where ident_to_index = ? and id <= ?",
                    recordsToDelete
                            .subList(from, Math.min(recordsToDelete.size(), batchSize + from))
                            .stream()
                            .map(entry -> new Params(entry.getIdentToIndex(), entry.getId()))
                            .collect(Collectors.toList()));

            from += batchSize;
        }

    }


    protected void markAsFailed(ReindexEntry idx, String cause) throws DotDataException {
        final int newPriority =
                (idx.errorCount() >= REINDEX_MAX_FAILURE_ATTEMPTS) ? Priority.ERROR.dbValue()
                        + idx.getPriority() : (1 + idx.getPriority());

        DotConnect dc = new DotConnect();
        dc.setSQL(
                "UPDATE dist_reindex_journal set serverid=null, priority = ? , index_val = ? where id= ?");
        dc.addParam(newPriority);
        dc.addParam(cause);
        dc.addParam(idx.getId());
        dc.loadResult();
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }


    protected Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn)
            throws DotDataException {
        Map<String, ReindexEntry> contentToIndex = new HashMap<>();

        if (queue.isEmpty()) {
            loadUpLocalQueue();
        }

        for (ReindexEntry entry; (entry = queue.poll()) != null; ) {
            // One outcome per identifier per batch, resolved by row id rather than by the order
            // the entries happened to be polled in. Two entries for the same identifier are
            // successive statements about what the index should hold, and only the newest is
            // true: a DELETE written after a REINDEX means the content is gone, so applying the
            // REINDEX afterwards would re-add a document for content that no longer exists.
            //
            // The losing row is discarded rather than retried, and that is deliberate — it has
            // been superseded, so re-applying it could only undo the outcome just applied. The
            // ack drops it along with the winner (deleteReindexEntry bounds its sweep by
            // id <= winner), which is also what stops the ack from reaching a row queued after
            // this batch was loaded.
            contentToIndex.merge(entry.getIdentToIndex(), entry,
                    (existing, candidate) -> candidate.getId() > existing.getId()
                            ? candidate : existing);
            if (contentToIndex.size() >= recordsToReturn) {
                while (entry.equals(queue.peek())) {
                    // drain duplicate items
                    queue.poll();
                }
                break;
            }
        }

        return contentToIndex;
    }

    private static long lastIdIndexed = 0;

    @VisibleForTesting
    static void resetLastIdReindexed() {
        lastIdIndexed = 0;
    }

    @CloseDBIfOpened
    private void loadUpLocalQueue() throws DotDataException {
        List<String> reindexingServers = APILocator.getServerAPI().getReindexingServers();
        if (reindexingServers == null || reindexingServers.size() == 0) {
            Logger.warn(this.getClass(),
                    "There are no servers in cluster - something is wrong with server heartbeat");
            return;
        }
        int myIndex = reindexingServers.indexOf(APILocator.getServerAPI().readServerId());
        final int priorityLevel = Priority.ERROR.dbValue();
        DotConnect db = new DotConnect();

        if (DbConnectionFactory.isOracle()) {
            db.setSQL("select * from (select * from dist_reindex_journal where MOD(id, ?) = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC) where ROWNUM <= 2000");
        } else if (DbConnectionFactory.isMsSql()) {
            db.setSQL("select TOP 2000 * from dist_reindex_journal where id % ? = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC");
        } else {
            db.setSQL("select * from dist_reindex_journal where MOD(id, ?) = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC LIMIT 2000");
        }

        db.addParam(reindexingServers.size());
        db.addParam(myIndex);
        db.addParam(priorityLevel);
        db.addParam(lastIdIndexed);

        for (Map<String, Object> map : db.loadObjectResults()) {
            final ReindexEntry entry = mapToReindexEntry(map);
            lastIdIndexed = entry.getId();
            queue.add(entry);
        }

        if (queue.isEmpty()) {
            lastIdIndexed = 0;
            if (reindexingServers.size() > 1) {
                // A safety net must never break what it protects: a failure here is logged and the
                // normal path carries on as it did before this check existed.
                try {
                    takeOverStarvedSlices(reindexingServers, myIndex, priorityLevel);
                } catch (final Exception e) {
                    Logger.warn(this.getClass(), "Could not check the other servers' reindex "
                            + "shares for stalls: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * How long another server's share of the journal may sit unchanged before an idle server takes
     * it over. Read once: it is consulted on every idle poll of the reindex thread.
     */
    private static final Lazy<Duration> STARVED_SLICE_THRESHOLD = Lazy.of(() -> Duration.ofSeconds(
            Config.getIntProperty("REINDEX_STARVED_SLICE_SECONDS", 120)));

    private static StarvedSliceDetector starvedSliceDetector;

    /** Shares whose takeover has already been logged; used only from the reindex thread. */
    private static final Set<Integer> announcedTakeovers = new HashSet<>();

    private static synchronized StarvedSliceDetector starvedSliceDetector() {
        if (null == starvedSliceDetector) {
            starvedSliceDetector = new StarvedSliceDetector(STARVED_SLICE_THRESHOLD.get());
        }
        return starvedSliceDetector;
    }

    /**
     * Called when this server's own share of the journal is empty: loads the rows of any other
     * share that has stopped moving.
     *
     * <p>The journal is split by {@code MOD(id, servers)} across every server that pinged in the
     * last few minutes. A server that keeps pinging but does not index leaves its share untouched
     * for as long as it keeps pinging, and a full reindex then hangs part-way with no error, no
     * failed entries and nothing in the log (issue #36482). An idle server now picks those rows
     * up. The worst case is indexing a document twice, which is harmless; the alternative was
     * content that never got indexed.</p>
     *
     * <p>The check costs one grouped count, and only runs while this server has nothing of its own
     * to do and the journal is split more than one way.</p>
     */
    private void takeOverStarvedSlices(final List<String> reindexingServers, final int myIndex,
            final int priorityLevel) throws DotDataException {
        final int sliceCount = reindexingServers.size();
        final Map<Integer, StarvedSliceDetector.SliceSnapshot> snapshot =
                loadShareSnapshot(sliceCount, priorityLevel);

        final List<Integer> starved = starvedSliceDetector().starvedSlices(snapshot, myIndex,
                sliceCount, Instant.now());
        // One WARN per takeover, not one per poll while it lasts.
        announcedTakeovers.retainAll(starved);
        for (final int slice : starved) {
            if (announcedTakeovers.add(slice)) {
                Logger.warn(this.getClass(), String.format("Reindex share %d of %d, assigned to "
                                + "server %s, has held %d entries without progress for over %d s. "
                                + "That server is listed as alive but is not indexing its share; "
                                + "this server is taking the entries over (issue #36482).", slice,
                        sliceCount, reindexingServers.get(slice), snapshot.get(slice).count(),
                        STARVED_SLICE_THRESHOLD.get().getSeconds()));
            }

            queue.addAll(loadShareEntries(sliceCount, slice, priorityLevel));
        }
    }

    /**
     * Row count and lowest id of every non-empty share of the journal, keyed by slice number.
     *
     * @param sliceCount how many ways the journal is split
     * @param priorityLevel the highest priority still eligible for indexing
     */
    @VisibleForTesting
    @CloseDBIfOpened
    Map<Integer, StarvedSliceDetector.SliceSnapshot> loadShareSnapshot(final int sliceCount,
            final int priorityLevel) throws DotDataException {
        final DotConnect shares = new DotConnect();
        // Grouped by position: PostgreSQL does not treat two bound MOD(id, ?) expressions as the
        // same one, so "group by MOD(id, ?)" is rejected.
        shares.setSQL("select MOD(id, ?) as slice, count(*) as rows_left, min(id) as min_id"
                + " from dist_reindex_journal where priority <= ? group by 1");
        shares.addParam(sliceCount);
        shares.addParam(priorityLevel);

        final Map<Integer, StarvedSliceDetector.SliceSnapshot> snapshot = new HashMap<>();
        for (final Map<String, Object> row : shares.loadObjectResults()) {
            snapshot.put(((Number) row.get("slice")).intValue(),
                    new StarvedSliceDetector.SliceSnapshot(
                            ((Number) row.get("rows_left")).longValue(),
                            ((Number) row.get("min_id")).longValue()));
        }
        return snapshot;
    }

    /**
     * Up to one batch of the entries in one share of the journal, in the order the normal path
     * reads its own share.
     *
     * @param sliceCount how many ways the journal is split
     * @param slice the share to read
     * @param priorityLevel the highest priority still eligible for indexing
     */
    @VisibleForTesting
    @CloseDBIfOpened
    List<ReindexEntry> loadShareEntries(final int sliceCount, final int slice,
            final int priorityLevel) throws DotDataException {
        final DotConnect rows = new DotConnect();
        rows.setSQL("select * from dist_reindex_journal where MOD(id, ?) = ?"
                + " and priority <= ? ORDER BY priority ASC LIMIT 2000");
        rows.addParam(sliceCount);
        rows.addParam(slice);
        rows.addParam(priorityLevel);
        return rows.loadObjectResults().stream()
                .map(this::mapToReindexEntry)
                .collect(Collectors.toList());
    }

    private ReindexEntry mapToReindexEntry(final Map<String, Object> map) {
        return ReindexEntry.builder()
                .id(((Number) map.get("id")).longValue())
                .identToIndex((String) map.get("ident_to_index"))
                .priority(((Number) map.get("priority")).intValue())
                .isDelete(isDeleteAction(map))
                .build();
    }

    /**
     * Decodes the {@code dist_action} column of a journal row into the delete flag.
     *
     * <p>Every read path must go through here: a row whose action is not decoded defaults to
     * {@link ReindexAction#REINDEX}, which turns a pending removal into a no-op reindex — the
     * failure mode #37276 is about. A missing value is treated as a reindex on purpose, matching
     * how rows written before the column carried meaning are interpreted.</p>
     *
     * @param map one row of {@code dist_reindex_journal}
     * @return {@code true} when the row asks for the document to be removed from the index
     */
    private static boolean isDeleteAction(final Map<String, Object> map) {
        final Object action = map.get("dist_action");
        return action instanceof Number
                && ((Number) action).intValue() == ReindexAction.DELETE.ordinal();
    }


    protected String getServerId() {
        return ConfigUtils.getServerId();
    }

    protected long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }

    protected long recordsInQueue(final Connection conn) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from dist_reindex_journal where priority < ?")
                .addParam(Priority.ERROR.dbValue());
        final List<Map<String, String>> results = dc.loadResults(conn);
        final String c = results.get(0).get("count");
        return Long.parseLong(c);
    }

    protected void refreshContentUnderFolder(Folder folder) throws DotDataException {
        final String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
                        + " SELECT distinct identifier.id, identifier.id, ?, ? "
                        + " FROM contentlet join identifier ON contentlet.identifier=identifier.id "
                        + " WHERE identifier.host_inode=? AND identifier.parent_path LIKE ? ";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.NORMAL.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(folder.getHostId());
        String folderPath = APILocator.getIdentifierAPI().find(folder.getIdentifier()).getPath();
        dc.addParam(folderPath + "%");
        dc.loadResult();
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

    protected void refreshContentUnderFolderPath(String hostId, String folderPath)
            throws DotDataException {
        final String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
                        + " SELECT distinct identifier.id, identifier.id, ?, ? "
                        + " FROM contentlet join identifier ON contentlet.identifier=identifier.id "
                        + " WHERE identifier.host_inode=? AND identifier.parent_path LIKE ? ";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.NORMAL.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(hostId);
        dc.addParam(folderPath + "%");
        dc.loadResult();
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

    protected void addIdentifierReindex(final String identifier, final int priority)
            throws DotDataException {

        addIdentifierReindex(ImmutableList.of(identifier), priority);
    } // addIdentifierReindex.

    protected void addIdentifierReindex(final String identifier) throws DotDataException {
        addIdentifierReindex(identifier, Priority.NORMAL.dbValue());

    } // addIdentifierReindex.

    protected void addReindexHighPriority(final String identifier) throws DotDataException {
        addIdentifierReindex(identifier, Priority.ASAP.dbValue());
    } // addReindexHighPriority.

    protected int addIdentifierReindex(final Collection<String> identifiers)
            throws DotDataException {

        return this.addIdentifierReindex(identifiers, Priority.NORMAL.dbValue());
    }

    protected int addReindexHighPriority(final Collection<String> identifiers)
            throws DotDataException {

        return this.addIdentifierReindex(identifiers, Priority.ASAP.dbValue());
    }

    private int addIdentifierReindex(final Collection<String> identifiers, final int prority)
            throws DotDataException {

        if (identifiers == null || identifiers.isEmpty()) {
            return 0;
        }

        final Date date = DbConnectionFactory.now();
        for (final String identifier : identifiers) {

            new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier)
                    .addParam(identifier).addParam(prority)
                    .addParam(ReindexAction.REINDEX.ordinal()).addParam(date).loadResult();

        }
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }

        return identifiers.size();
    }

    protected int addIdentifierDelete(final Collection<String> identifiers, final int prority)
            throws DotDataException {

        if (identifiers == null || identifiers.isEmpty()) {
            return 0;
        }

        final Date date = DbConnectionFactory.now();
        for (final String identifier : identifiers) {

            new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier)
                    .addParam(identifier).addParam(prority)
                    .addParam(ReindexAction.DELETE.ordinal()).addParam(date).loadResult();

        }

        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
        return identifiers.size();
    }

    protected void refreshContentUnderHost(final Host host) throws DotDataException {
        String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
                        + " SELECT id, id, ?, ? "
                        + " FROM identifier "
                        + " WHERE asset_type='contentlet' and identifier.host_inode=?";
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
                + " SELECT asset_id, asset_id, ?, ? "
                + " FROM permission_reference " + " WHERE reference_id=?";
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(host.getIdentifier());
        dc.loadResult();
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

    static long lastTimeIRequedRecords = 0;

    @CloseDBIfOpened
    public boolean requeueStaleReindexRecordsTimer() throws DotDataException {
        if (lastTimeIRequedRecords + (REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC / 2 * 1000)
                < System.currentTimeMillis()) {
            lastTimeIRequedRecords = System.currentTimeMillis();
            requeueStaleReindexRecords();
            return true;
        }
        return false;

    }

    @WrapInTransaction
    public void requeueStaleReindexRecords() throws DotDataException {
        final Date olderThan = new Date(
                System.currentTimeMillis() - (1000 * REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC));

        DotConnect dc = new DotConnect()
                .setSQL("UPDATE dist_reindex_journal SET serverid=NULL where time_entered<? and serverid is not null and priority < ?")
                .addParam(olderThan).addParam(Priority.ERROR.dbValue());

        dc.loadResult();
        if (!Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false)) {
            ReindexThread.unpause();
        }
    }

}
