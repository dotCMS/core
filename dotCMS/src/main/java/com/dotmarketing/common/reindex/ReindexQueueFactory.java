package com.dotmarketing.common.reindex;

import com.dotmarketing.common.db.Params;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.stream.Collectors;


/**
 * Provides access to all the routines associated to the re-indexation process in dotCMS.
 * 
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 *
 */
public class ReindexQueueFactory {

    private final String REINDEX_JOURNAL_INSERT =
            "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) values (?, ?, ?, ?, ?)";

    // if there are old records in the reindexQueue that have been claimed by a server that is no longer
    // running, tee them back up
    private static final int REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC = Config.getIntProperty("REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC", 120);

    public static int REINDEX_RECORDS_TO_FETCH = Config.getIntProperty("REINDEX_RECORDS_TO_FETCH", 100);

    public static final int REINDEX_MAX_FAILURE_ATTEMPTS = Config.getIntProperty("RETRY_FAILED_INDEX_TIMES", 5);

    private static final ConcurrentLinkedQueue<ReindexEntry> queue = new ConcurrentLinkedQueue<>();
    
    public ConcurrentLinkedQueue<ReindexEntry> getLocalQueue(){
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
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) "
                    + " select distinct identifier,identifier," + Priority.REINDEX.dbValue() + "," + ReindexAction.REINDEX.ordinal() + ", "
                    + timestampSQL() + " from contentlet_version_info where identifier is not null";
            dc.setSQL(sql);
            dc.loadResult();
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    protected void addStructureReindexEntries(String structureInode) throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) "
                    + " select distinct c.identifier,c.identifier," + Priority.STRUCTURE.dbValue() + "," + ReindexAction.REINDEX.ordinal()
                    + "," + timestampSQL() + " from contentlet c " + " where c.structure_inode = ? and c.identifier is not null";
            dc.setSQL(sql);
            dc.addParam(structureInode);
            dc.loadResult();

        } catch (Exception ex) {
            Logger.fatal(this, "Error  unlocking the reindex journal table" + ex);
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

    protected void deleteFailedRecords() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority > ?");
        dc.addParam(Priority.REINDEX.dbValue());
        dc.loadResult();
    }

    @CloseDBIfOpened
    protected List<ReindexEntry> getFailedReindexRecords() throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT id, ident_to_index, priority, index_val, time_entered FROM dist_reindex_journal WHERE priority > ?");
        dc.addParam(ReindexQueueFactory.Priority.REINDEX.dbValue());
        final List<Map<String, Object>> failedRecords = dc.loadObjectResults();
        final List<ReindexEntry> failed = new ArrayList<>();
        long identifier;
        int priority;
        for (final Map<String, Object> map : failedRecords) {
            final String indexVal = UtilMethods.isSet(map.get("index_val")) ? String.class.cast(map.get("index_val"))
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

            final ReindexEntry ridx = new ReindexEntry()
                    .setId(identifier)
                    .setIdentToIndex((String) map.get("ident_to_index"))
                    .setPriority(priority)
                    .setTimeEntered((Date) map.get("time_entered"))
                    .setLastResult(indexVal);
            failed.add(ridx);
        }
        return failed;
    }

    protected void deleteReindexEntry(ReindexEntry iJournal) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where ident_to_index = ? or id= ?");
        dc.addParam(iJournal.getIdentToIndex());
        dc.addParam(iJournal.getId());
        dc.loadResult();
    }

    protected void deleteReindexEntry(String identifier) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where ident_to_index = ? ");
        dc.addParam(identifier);
        dc.loadResult();
    }

    protected void deleteReindexEntry(final List<ReindexEntry> recordsToDelete) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        final int batchSize = REINDEX_RECORDS_TO_FETCH / 5;
        int from = 0;
        while (from <= recordsToDelete.size()) {
            dotConnect.executeBatch(
                    "DELETE FROM dist_reindex_journal where " + (DbConnectionFactory.isMySql()
                            ? "id = ?" : "ident_to_index = ?"),
                    recordsToDelete
                            .subList(from, Math.min(recordsToDelete.size(), batchSize + from))
                            .stream().map(entry -> new Params(
                            DbConnectionFactory.isMySql() ? entry.getId()
                                    : entry.getIdentToIndex())).collect(
                            Collectors.toList()));

            from += batchSize;
        }

    }
    
    
    protected void markAsFailed(ReindexEntry idx, String cause) throws DotDataException {
        final int newPriority =
                (idx.errorCount() >= REINDEX_MAX_FAILURE_ATTEMPTS) ? Priority.ERROR.dbValue() + idx.getPriority() : (1 + idx.getPriority());

        DotConnect dc = new DotConnect();
        dc.setSQL("UPDATE dist_reindex_journal set serverid=null, priority = ? , index_val = ? where id= ?");
        dc.addParam(newPriority);
        dc.addParam(cause);
        dc.addParam(idx.getId());
        dc.loadResult();
    }

    
    
    
  protected Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn) throws DotDataException {
    Map<String, ReindexEntry> contentToIndex = new HashMap<>();

    if (queue.isEmpty()) {
      loadUpLocalQueue();
    }

    for (ReindexEntry entry; (entry = queue.poll()) != null;) {
      contentToIndex.put(entry.getIdentToIndex(), entry);
      if (contentToIndex.size() >= recordsToReturn)
        break;
    }

    return contentToIndex;
  }

    private static long lastIdIndexed=0;

    @VisibleForTesting
    static void resetLastIdReindexed(){
        lastIdIndexed = 0;
    }
    
    @CloseDBIfOpened
    private void loadUpLocalQueue() throws DotDataException {
        List<String> reindexingServers = APILocator.getServerAPI().getReindexingServers();
        if(reindexingServers==null || reindexingServers.size() == 0) {
            Logger.warn(this.getClass(), "There are no servers in cluster - something is wrong with server heartbeat");
            return;
        }
        int myIndex = reindexingServers.indexOf(APILocator.getServerAPI().readServerId());
        final int priorityLevel = Priority.ERROR.dbValue();
        DotConnect db  = new DotConnect();

        if (DbConnectionFactory.isOracle()){
            db.setSQL("select * from (select * from dist_reindex_journal where MOD(id, ?) = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC) where ROWNUM <= 2000");
        } else if (DbConnectionFactory.isMsSql()){
            db.setSQL("select TOP 2000 * from dist_reindex_journal where id % ? = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC");
        } else{
            db.setSQL("select * from dist_reindex_journal where MOD(id, ?) = ?"
                    + " and priority <= ? and id > ? ORDER BY priority ASC LIMIT 2000");
        }

        db.addParam(reindexingServers.size());
        db.addParam(myIndex);
        db.addParam(priorityLevel);
        db.addParam(lastIdIndexed);

        for (Map<String, Object> map : db.loadObjectResults()) {
            final ReindexEntry entry = mapToReindexEntry(map);
            lastIdIndexed=entry.getId();
            queue.add(entry);
        }

        if(queue.isEmpty()) {
            lastIdIndexed=0;
        }
    }

    private ReindexEntry mapToReindexEntry(Map<String,Object> map) {
      final ReindexEntry entry = new ReindexEntry();
      entry.setId(((Number) map.get("id")).longValue());
      String identifier = (String) map.get("ident_to_index");
      entry.setIdentToIndex(identifier);
      entry.setPriority(((Number) (map.get("priority"))).intValue());
      entry.setDelete(((Number) (map.get("dist_action"))).intValue() == ReindexAction.DELETE.ordinal());
      return entry;
      
    }
    
    
    
    

    protected String getServerId() {
        return ConfigUtils.getServerId();
    }

    protected long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }

    protected long recordsInQueue(final Connection conn) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from dist_reindex_journal where priority <= ?");
        dc.addParam(Priority.ERROR.dbValue());
        final List<Map<String, String>> results = dc.loadResults(conn);
        final String c = results.get(0).get("count");
        return Long.parseLong(c);
    }

    protected void refreshContentUnderFolder(Folder folder) throws DotDataException {
        final String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
                + " SELECT distinct identifier.id, identifier.id, ?, ? "
                + " FROM contentlet join identifier ON contentlet.identifier=identifier.id "
                + " WHERE identifier.host_inode=? AND identifier.parent_path LIKE ? ";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.NORMAL.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(folder.getHostId());
        String folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
        dc.addParam(folderPath + "%");
        dc.loadResult();
    }

    protected void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException {
        final String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "
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
    }

    protected void addIdentifierReindex(final String identifier, final int priority) throws DotDataException {

        addIdentifierReindex(ImmutableList.of(identifier), priority);
    } // addIdentifierReindex.

    protected void addIdentifierReindex(final String identifier) throws DotDataException {
        addIdentifierReindex(identifier, Priority.NORMAL.dbValue());

    } // addIdentifierReindex.

    protected void addReindexHighPriority(final String identifier) throws DotDataException {
        addIdentifierReindex(identifier, Priority.ASAP.dbValue());
    } // addReindexHighPriority.

    protected int addIdentifierReindex(final Collection<String> identifiers) throws DotDataException {

        return this.addIdentifierReindex(identifiers, Priority.NORMAL.dbValue());
    }

    protected int addReindexHighPriority(final Collection<String> identifiers) throws DotDataException {

        return this.addIdentifierReindex(identifiers, Priority.ASAP.dbValue());
    }

    private int addIdentifierReindex(final Collection<String> identifiers, final int prority) throws DotDataException {

        if (identifiers == null || identifiers.isEmpty()) {
            return 0;
        }

        final Date date = DbConnectionFactory.now();
        for (final String identifier : identifiers) {

            new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier).addParam(identifier).addParam(prority)
                    .addParam(ReindexAction.REINDEX.ordinal()).addParam(date).loadResult();

        }
        return identifiers.size();
    }
    protected int addIdentifierDelete(final Collection<String> identifiers, final int prority) throws DotDataException {

        if (identifiers == null || identifiers.isEmpty()) {
            return 0;
        }

        final Date date = DbConnectionFactory.now();
        for (final String identifier : identifiers) {

            new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier).addParam(identifier).addParam(prority)
                    .addParam(ReindexAction.DELETE.ordinal()).addParam(date).loadResult();

        }
        return identifiers.size();
    }

    protected void refreshContentUnderHost(final Host host) throws DotDataException {
        String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT id, id, ?, ? "
                + " FROM identifier " + " WHERE asset_type='contentlet' and identifier.host_inode=?";
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT asset_id, asset_id, ?, ? "
                + " FROM permission_reference " + " WHERE reference_id=?";
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(ReindexAction.REINDEX.ordinal());
        dc.addParam(host.getIdentifier());
        dc.loadResult();
    }

    static long lastTimeIRequedRecords = 0;

    @CloseDBIfOpened
    public boolean requeueStaleReindexRecordsTimer() throws DotDataException {
        if (lastTimeIRequedRecords + (REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC / 2 * 1000) < System.currentTimeMillis()) {
            lastTimeIRequedRecords = System.currentTimeMillis();
            requeueStaleReindexRecords();
            return true;
        }
        return false;
    }

    @WrapInTransaction
    public void requeueStaleReindexRecords() throws DotDataException {
        final Date olderThan = new Date(System.currentTimeMillis() - (1000 * REQUEUE_REINDEX_RECORDS_OLDER_THAN_SEC));

        DotConnect dc = new DotConnect()
                .setSQL("UPDATE dist_reindex_journal SET serverid=NULL where time_entered<? and serverid is not null").addParam(olderThan);

        dc.loadResult();

    }

}
