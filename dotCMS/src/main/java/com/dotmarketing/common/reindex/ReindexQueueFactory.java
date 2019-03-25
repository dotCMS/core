package com.dotmarketing.common.reindex;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueAPI.DateType;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;

import oracle.jdbc.OracleTypes;

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

    public int REINDEX_RECORDS_TO_FETCH = Config.getIntProperty("REINDEX_RECORDS_TO_FETCH", 100);

    public static final int REINDEX_MAX_FAILURE_ATTEMPTS = Config.getIntProperty("RETRY_FAILED_INDEX_TIMES", 5);

    public enum Priority {
        ASAP, NORMAL, STRUCTURE, REINDEX, ERROR;
        public int dbValue() {
            return this.ordinal() * 100;
        }
    }

    public static final int REINDEX_ACTION_REINDEX_OBJECT = 1;
    public static final int REINDEX_ACTION_DELETE_OBJECT = 2;

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

    private String reindexSelectSQL() {
        if (DbConnectionFactory.isOracle()) {
            return "SELECT * FROM table(load_records_to_index(?, ?, ?))";
        } else if (DbConnectionFactory.isMySql()) {
            return "{call load_records_to_index(?,?,?)}";
        } else {
            return "SELECT * FROM load_records_to_index(?, ?, ?)";
        }

    }

    protected void addAllToReindexQueue() throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) "
                    + " select distinct identifier,identifier," + Priority.REINDEX.dbValue() + "," + REINDEX_ACTION_REINDEX_OBJECT + ", "
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
                    + " select distinct c.identifier,c.identifier," + Priority.STRUCTURE.dbValue() + "," + REINDEX_ACTION_REINDEX_OBJECT
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

    @CloseDBIfOpened
    protected List<ReindexEntry> getFailedReindexRecords() throws DotDataException {
        DotConnect dc = new DotConnect();

        dc.setSQL("SELECT id, ident_to_index, priority, index_val, time_entered FROM dist_reindex_journal WHERE priority >= ?");
        dc.addParam(ReindexQueueFactory.Priority.REINDEX.dbValue());
        List<Map<String, Object>> failedRecords = dc.loadObjectResults();
        List<ReindexEntry> failed = new ArrayList<>();
        for (Map<String, Object> map : failedRecords) {
            ReindexEntry ridx = new ReindexEntry()
                    .setId(Long.parseLong((String) map.get("id")))
                    .setIdentToIndex((String) map.get("ident_to_index"))
                    .setPriority(Integer.parseInt((String) map.get("priority")))
                    .setTimeEntered((Date) map.get("time_entered"))
                    .setLastResult((String) map.get("index_val"));
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

    protected void deleteReindexEntry(final Collection<ReindexEntry> recordsToDelete) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder sql = new StringBuilder().append("DELETE FROM dist_reindex_journal where ident_to_index in (");
        boolean first = true;

        for (ReindexEntry idx : recordsToDelete) {
            if (!first)
                sql.append(',');
            else
                first = false;
            sql.append("'" + idx.getIdentToIndex() + "'");
        }
        sql.append(')');

        dotConnect.setSQL(sql.toString());
        dotConnect.loadResult();
    }

    protected void updateIndexJournalPriority(long id, int priority) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("UPDATE dist_reindex_journal set  priority = ? where id= ?");
        dc.addParam(priority);
        dc.addParam(id);
        dc.loadResult();
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

    @WrapInTransaction
    protected Map<String, ReindexEntry> findContentToReindex(final int recordsToReturn) throws DotDataException {
        Map<String, ReindexEntry> contentToIndex = loadReindexRecordsFromDb(recordsToReturn);
        if (contentToIndex.size() < recordsToReturn) {
            if (requeueStaleReindexRecordsTimer()) {
                contentToIndex.putAll(loadReindexRecordsFromDb(recordsToReturn));
            }
        }
        return contentToIndex;
    }

    private Map<String, ReindexEntry> loadReindexRecordsFromDb(final int recordsToReturn) throws DotDataException {
        DotConnect dc = new DotConnect();
        Map<String, ReindexEntry> contentList = new LinkedHashMap<String, ReindexEntry>();
        List<Map<String, Object>> results;
        Connection con = null;
        String serverId = ConfigUtils.getServerId();

        try {

            // Get the number of records to fetch

            int priorityLevel = Priority.ERROR.dbValue();

            con = DbConnectionFactory.getConnection();
            if (DbConnectionFactory.isOracle()) {
                CallableStatement call = con.prepareCall("{ ? = call load_records_to_index(?,?,?) }");
                call.registerOutParameter(1, OracleTypes.CURSOR);
                call.setString(2, serverId);
                call.setInt(3, recordsToReturn);
                call.setInt(4, priorityLevel);
                call.execute();
                ResultSet r = (ResultSet) call.getObject(1);
                results = new ArrayList<Map<String, Object>>();
                while (r.next()) {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("id", r.getInt("id"));
                    m.put("inode_to_index", r.getString("inode_to_index"));
                    m.put("ident_to_index", r.getString("ident_to_index"));
                    m.put("priority", r.getInt("priority"));
                    results.add(m);
                }
                r.close();
                call.close();
            } else if (DbConnectionFactory.isMsSql()) {
                // we need to make sure this setting is ON because of the READPAST we use
                // in the stored procedure
                dc.setSQL("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;");
                dc.loadResult();

                dc.setSQL("load_records_to_index @server_id='" + serverId + "', @records_to_fetch=" + String.valueOf(recordsToReturn)
                        + ", @priority_level=" + String.valueOf(priorityLevel));
                dc.setForceQuery(true);
                results = dc.loadObjectResults(con);
            } else if (DbConnectionFactory.isH2()) {
                dc.setSQL("call load_records_to_index(?,?,?)");
                dc.addParam(serverId);
                dc.addParam(REINDEX_RECORDS_TO_FETCH);
                dc.addParam(priorityLevel);
                results = dc.loadObjectResults();
            } else {
                dc.setSQL(reindexSelectSQL());
                dc.addParam(serverId);
                dc.addParam(recordsToReturn);
                dc.addParam(priorityLevel);
                results = dc.loadObjectResults(con);
            }

            for (Map<String, Object> r : results) {
                ReindexEntry ij = new ReindexEntry();
                ij.setId(((Number) r.get("id")).longValue());
                String identifier = (String) r.get("ident_to_index");
                ij.setIdentToIndex(identifier);
                ij.setPriority(((Number) (r.get("priority"))).intValue());
                contentList.put(identifier, ij);
            }
        } catch (SQLException e1) {
            throw new DotDataException(e1.getMessage(), e1);
        }
        return contentList;
    }

    protected String getServerId() {
        return ConfigUtils.getServerId();
    }

    protected long recordsInQueue() throws DotDataException {
        return recordsInQueue(DbConnectionFactory.getConnection());
    }

    protected long recordsInQueue(final Connection conn) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from dist_reindex_journal");
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
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
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
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
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
                    .addParam(REINDEX_ACTION_REINDEX_OBJECT).addParam(date).loadResult();

        }
        return identifiers.size();
    }

    protected void refreshContentUnderHost(Host host) throws DotDataException {
        String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT id, id, ?, ? "
                + " FROM identifier " + " WHERE asset_type='contentlet' and identifier.host_inode=?";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT asset_id, asset_id, ?, ? "
                + " FROM permission_reference " + " WHERE reference_id=?";
        dc.setSQL(sql);
        dc.addParam(Priority.STRUCTURE.dbValue());
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();
    }

    static long lastTimeIRequedRecords = 0;

    @WrapInTransaction
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
