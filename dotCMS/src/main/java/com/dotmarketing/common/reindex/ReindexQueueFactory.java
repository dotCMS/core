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
import org.apache.commons.configuration.ConversionException;
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


    /**
     * Admits one batch using current configuration and membership, including prefetched work.
     * Excluded entries leave only the local queue; their journal rows remain for eligible owners.
     *
     * @param recordsToReturn maximum distinct identifiers in the batch
     * @return eligible entries, retaining the newest journal row for each identifier
     * @throws DotDataException if membership or journal lookup fails
     */
    protected Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn)
            throws DotDataException {
        final ReindexPartition partition = readPartition();
        if (partition == null) {
            return Map.of();
        }
        if (!partition.equals(lastPartition)) {
            resetLastIdReindexed();
            starvedSliceDetector = null;
            bulkStarvedSliceDetector = null;
            announcedTakeovers.clear();
            announcedBulkTakeovers.clear();
            lastPartition = partition;
        }
        final Map<String, ReindexEntry> contentToIndex = new HashMap<>();
        boolean loaded = false;
        for (int pass = 0; pass < 2; pass++) {
            if (queue.isEmpty()) {
                if (loaded) {
                    break;
                }
                loadUpLocalQueue(partition);
                loaded = true;
            }
            for (ReindexEntry entry; (entry = queue.poll()) != null; ) {
                if (!partition.admits(entry)) {
                    continue;
                }
                // Newest id wins regardless of priority/arrival order; acknowledgement removes
                // only superseded rows up to that id, preserving later updates and deletes.
                contentToIndex.merge(entry.getIdentToIndex(), entry,
                        (existing, candidate) -> candidate.getId() > existing.getId()
                                ? candidate : existing);
                if (contentToIndex.size() >= recordsToReturn) {
                    while (entry.equals(queue.peek())) {
                        queue.poll();
                    }
                    break;
                }
            }
            if (!contentToIndex.isEmpty() || loaded) {
                break;
            }
            // Stale prefetch containing only capped work must not hide normal work in the DB.
        }
        return contentToIndex;
    }

    /** One batch's ownership snapshot; provider adapters retain the full server membership. */
    private record ReindexPartition(List<String> servers, int myIndex, int bulkNodes,
                                    int minPriority) {
        /** Whether this node can admit this entry under the current priority cutoff. */
        boolean admits(final ReindexEntry entry) {
            return entry.getPriority() < minPriority || myIndex < bulkNodes;
        }
    }

    private ReindexPartition lastPartition;
    private static long lastIdIndexed = 0;

    /**
     * Resolves both runtime settings once per batch; no setting is latched at JVM startup.
     *
     * @return the current ownership split, or null when this node has no eligible membership
     * @throws DotDataException if cluster membership cannot be read
     */
    private ReindexPartition readPartition() throws DotDataException {
        final int maximum = readWorkerSetting("MAX_REINDEXING_NODES", 2);
        int minimum = readWorkerSetting("REINDEX_NODE_LIMIT_MIN_PRIORITY", 200);
        if (minimum < 0) {
            Logger.warnEvery(this.getClass(), "invalid-reindex-node-priority",
                    "Negative REINDEX_NODE_LIMIT_MIN_PRIORITY; using 200", 60000);
            minimum = 200;
        }
        final List<String> servers = APILocator.getServerAPI().getReindexingServers();
        final int myIndex = servers == null ? -1
                : servers.indexOf(APILocator.getServerAPI().readServerId());
        if (myIndex < 0) {
            Logger.warn(this.getClass(), "This node has no eligible reindex membership; waiting for heartbeat");
            return null;
        }
        final int bulkNodes = maximum <= 0 ? servers.size() : Math.min(maximum, servers.size());
        return new ReindexPartition(List.copyOf(servers), myIndex, bulkNodes, minimum);
    }

    /** Reads an integer setting without letting a malformed live value stop journal consumption. */
    private int readWorkerSetting(final String key, final int defaultValue) {
        try {
            return Config.getIntProperty(key, defaultValue);
        } catch (NumberFormatException | ConversionException e) {
            Logger.warnEvery(this.getClass(), "invalid-reindex-setting-" + key,
                    "Invalid " + key + "; using " + defaultValue, 60000);
            return defaultValue;
        }
    }

    /**
     * Moves the journal cursor back to the start, so the next load reads every row this node owns
     * again, including rows handed out earlier whose batch never finished. {@link ReindexThread}
     * calls it after a batch throws; tests call it to start from a known state.
     */
    static void resetLastIdReindexed() {
        lastIdIndexed = 0;
    }

    /**
     * Refills prefetch with all-node normal ownership and capped ownership for higher priorities.
     *
     * @param partition the configuration/membership snapshot used for this batch
     * @throws DotDataException if the journal query fails
     */
    @CloseDBIfOpened
    private void loadUpLocalQueue(final ReindexPartition partition) throws DotDataException {
        final int priorityLevel = Priority.ERROR.dbValue();
        final DotConnect db = new DotConnect();
        final String modulo = DbConnectionFactory.isMsSql() ? "id % ? = ?" : "MOD(id, ?) = ?";
        final boolean limited = partition.bulkNodes() < partition.servers().size();
        final String ownership = limited
                ? "((priority < ? and " + modulo + ") or (priority >= ? and " + modulo + "))"
                : modulo;
        final String query = "from dist_reindex_journal where " + ownership
                + " and priority <= ? and id > ? ORDER BY priority ASC";
        if (DbConnectionFactory.isOracle()) {
            db.setSQL("select * from (select * " + query + ") where ROWNUM <= 2000");
        } else if (DbConnectionFactory.isMsSql()) {
            db.setSQL("select TOP 2000 * " + query);
        } else {
            db.setSQL("select * " + query + " LIMIT 2000");
        }
        if (limited) {
            db.addParam(partition.minPriority());
        }
        db.addParam(partition.servers().size());
        db.addParam(partition.myIndex());
        if (limited) {
            db.addParam(partition.minPriority());
            db.addParam(partition.bulkNodes());
            db.addParam(partition.myIndex() < partition.bulkNodes() ? partition.myIndex() : -1);
        }
        db.addParam(priorityLevel);
        db.addParam(lastIdIndexed);

        for (Map<String, Object> map : db.loadObjectResults()) {
            final ReindexEntry entry = mapToReindexEntry(map);
            lastIdIndexed = entry.getId();
            queue.add(entry);
        }
        if (queue.isEmpty()) {
            resetLastIdReindexed();
            try {
                if (limited) {
                    takeOverStarvedSlices(partition.servers(), partition.myIndex(),
                            Integer.MIN_VALUE, Math.min(partition.minPriority() - 1, priorityLevel), false);
                    if (partition.myIndex() < partition.bulkNodes()) {
                        takeOverStarvedSlices(partition.servers().subList(0, partition.bulkNodes()),
                                partition.myIndex(), partition.minPriority(), priorityLevel, true);
                    }
                } else {
                    takeOverStarvedSlices(partition.servers(), partition.myIndex(),
                            Integer.MIN_VALUE, priorityLevel, false);
                }
            } catch (final Exception e) {
                Logger.warn(this.getClass(), "Could not check the other servers' reindex "
                        + "shares for stalls: " + e.getMessage(), e);
            }
        }
    }

    /**
     * How long another server's share of the journal may sit unchanged before an idle server takes
     * it over. Read once: it is consulted on every idle poll of the reindex thread.
     */
    private static final Lazy<Duration> STARVED_SLICE_THRESHOLD = Lazy.of(() -> Duration.ofSeconds(
            Config.getIntProperty("REINDEX_STARVED_SLICE_SECONDS", 120)));

    private StarvedSliceDetector starvedSliceDetector;
    private StarvedSliceDetector bulkStarvedSliceDetector;

    /** Normal and capped partitions need independent progress histories and takeover notices. */
    private final Set<Integer> announcedTakeovers = new HashSet<>();
    private final Set<Integer> announcedBulkTakeovers = new HashSet<>();

    /** Returns the stalled-share detector for this priority class. */
    private synchronized StarvedSliceDetector starvedSliceDetector(final boolean bulk) {
        if (bulk) {
            if (bulkStarvedSliceDetector == null) {
                bulkStarvedSliceDetector = new StarvedSliceDetector(STARVED_SLICE_THRESHOLD.get());
            }
            return bulkStarvedSliceDetector;
        }
        if (starvedSliceDetector == null) {
            starvedSliceDetector = new StarvedSliceDetector(STARVED_SLICE_THRESHOLD.get());
        }
        return starvedSliceDetector;
    }

    /**
     * Called when this server's own share of the journal is empty: loads the rows of any other
     * share that has stopped moving.
     *
     * <p>Each priority class is split by {@code MOD(id, servers)} across its eligible participants.
     * Recovery uses the same priority bounds, so normal-work recovery cannot bypass the bulk cap.
     * A server that keeps pinging but does not index leaves its share untouched
     * for as long as it keeps pinging, and a full reindex then hangs part-way with no error, no
     * failed entries and nothing in the log (issue #36482). An idle server now picks those rows
     * up. The worst case is indexing a document twice, which is harmless; the alternative was
     * content that never got indexed.</p>
     *
     * <p>The check costs one grouped count, and only runs while this server has nothing of its own
     * to do and the journal is split more than one way.</p>
     */
    private void takeOverStarvedSlices(final List<String> reindexingServers, final int myIndex,
            final int minimumPriority, final int priorityLevel, final boolean bulk)
            throws DotDataException {
        final int sliceCount = reindexingServers.size();
        if (sliceCount <= 1 || minimumPriority > priorityLevel) {
            return;
        }
        final Map<Integer, StarvedSliceDetector.SliceSnapshot> snapshot =
                loadShareSnapshot(sliceCount, minimumPriority, priorityLevel);
        final List<Integer> starved = starvedSliceDetector(bulk).starvedSlices(snapshot, myIndex,
                sliceCount, Instant.now());
        final Set<Integer> announced = bulk ? announcedBulkTakeovers : announcedTakeovers;
        // One WARN per takeover, not one per poll while it lasts.
        announced.retainAll(starved);
        for (final int slice : starved) {
            if (announced.add(slice)) {
                Logger.warn(this.getClass(), String.format("Reindex share %d of %d, assigned to "
                                + "server %s, has held %d entries without progress for over %d s. "
                                + "That server is listed as alive but is not indexing its share; "
                                + "this server is taking the entries over (issue #36482).", slice,
                        sliceCount, reindexingServers.get(slice), snapshot.get(slice).count(),
                        STARVED_SLICE_THRESHOLD.get().getSeconds()));
            }

            queue.addAll(loadShareEntries(sliceCount, slice, minimumPriority, priorityLevel));
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
        return loadShareSnapshot(sliceCount, Integer.MIN_VALUE, priorityLevel);
    }

    /** Samples stalled shares only within the priority class assigned to this participant set. */
    private Map<Integer, StarvedSliceDetector.SliceSnapshot> loadShareSnapshot(final int sliceCount,
            final int minimumPriority, final int priorityLevel) throws DotDataException {
        final DotConnect shares = new DotConnect();
        // Grouped by position: PostgreSQL does not treat two bound MOD(id, ?) expressions as the
        // same one, so "group by MOD(id, ?)" is rejected.
        shares.setSQL("select MOD(id, ?) as slice, count(*) as rows_left, min(id) as min_id"
                + " from dist_reindex_journal where priority >= ? and priority <= ? group by 1");
        shares.addParam(sliceCount);
        shares.addParam(minimumPriority);
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
        return loadShareEntries(sliceCount, slice, Integer.MIN_VALUE, priorityLevel);
    }

    /** Recovers one stalled share without taking work from the other priority class. */
    private List<ReindexEntry> loadShareEntries(final int sliceCount, final int slice,
            final int minimumPriority, final int priorityLevel) throws DotDataException {
        final DotConnect rows = new DotConnect();
        rows.setSQL("select * from dist_reindex_journal where MOD(id, ?) = ?"
                + " and priority >= ? and priority <= ? ORDER BY priority ASC LIMIT 2000");
        rows.addParam(sliceCount);
        rows.addParam(slice);
        rows.addParam(minimumPriority);
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

    /**
     * Queues every contentlet under the folder's path for reindexing at NORMAL priority, stamped
     * with the current time.
     *
     * @param folder the folder whose content is reindexed
     * @throws DotDataException if the journal rows cannot be written
     */
    protected void refreshContentUnderFolder(Folder folder) throws DotDataException {
        final String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action,time_entered) "
                        + " SELECT distinct identifier.id, identifier.id, ?, ?, " + timestampSQL()
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

    /**
     * Queues every contentlet on the site whose parent path starts with the given path for
     * reindexing at NORMAL priority, stamped with the current time.
     *
     * @param hostId the site identifier
     * @param folderPath the folder path prefix
     * @throws DotDataException if the journal rows cannot be written
     */
    protected void refreshContentUnderFolderPath(String hostId, String folderPath)
            throws DotDataException {
        final String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action,time_entered) "
                        + " SELECT distinct identifier.id, identifier.id, ?, ?, " + timestampSQL()
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

    /**
     * Queues every contentlet on the site, and every asset whose permissions come from the site,
     * for reindexing at STRUCTURE priority, stamped with the current time.
     *
     * @param host the site whose content is reindexed
     * @throws DotDataException if the journal rows cannot be written
     */
    protected void refreshContentUnderHost(final Host host) throws DotDataException {
        String sql =
                " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action,time_entered) "
                        + " SELECT id, id, ?, ?, " + timestampSQL()
                        + " FROM identifier "
                        + " WHERE asset_type='contentlet' and identifier.host_inode=?";
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action,time_entered) "
                + " SELECT asset_id, asset_id, ?, ?, " + timestampSQL()
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
