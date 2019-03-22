package com.dotmarketing.common.business.journal;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.common.business.journal.DistributedJournalFactory;
import com.dotmarketing.common.business.journal.IndexJournal;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.primitives.Ints;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oracle.jdbc.OracleTypes;

/**
 * Provides access to all the routines associated to the re-indexation process in dotCMS.
 * 
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 *
 */
public class DistributedJournalFactory {

    private String TIMESTAMPSQL = "NOW()";
    private String REINDEXENTRIESSELECTSQL = "SELECT * FROM load_records_to_index(?, ?, ?)";
    private String ORACLEREINDEXENTRIESSELECTSQL = "SELECT * FROM table(load_records_to_index(?, ?, ?))";
    private String MYSQLREINDEXENTRIESSELECTSQL = "{call load_records_to_index(?,?,?)}";
    private String REINDEX_JOURNAL_INSERT =
            "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) values (?, ?, ?, ?, ?)";

    public static final int JOURNAL_TYPE_CONTENTENTINDEX = 1;
    public static final int JOURNAL_TYPE_CACHE = 2;

    public static final int RETRY_FAILED_INDEX_TIMES = Config.getIntProperty("RETRY_FAILED_INDEX_TIMES", 5);
    public static final int REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT = 40;
    public static final int REINDEX_JOURNAL_PRIORITY_NEWINDEX = 30;
    public static final int REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX = 20;
    public static final int REINDEX_JOURNAL_PRIORITY_CONTENT_CAN_WAIT_REINDEX = 15;
    public static final int REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX = 10;

    public static final int REINDEX_ACTION_REINDEX_OBJECT = 1;
    public static final int REINDEX_ACTION_DELETE_OBJECT = 2;

    public DistributedJournalFactory() {

        if (DbConnectionFactory.isMsSql()) {
            TIMESTAMPSQL = "GETDATE()";
        } else if (DbConnectionFactory.isOracle()) {
            if (DbConnectionFactory.getDbVersion() >= 10)
                REINDEXENTRIESSELECTSQL = ORACLEREINDEXENTRIESSELECTSQL;
            else
                REINDEXENTRIESSELECTSQL = "SELECT * FROM table(CAST(load_records_to_index(?, ?, ?)))";
            TIMESTAMPSQL = "CAST(SYSTIMESTAMP AS TIMESTAMP)";
        } else if (DbConnectionFactory.isMySql()) {
            REINDEXENTRIESSELECTSQL = MYSQLREINDEXENTRIESSELECTSQL;

        }
    }

    protected void addAllToReindexQueue() throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) "
                    + " select distinct identifier,identifier," + REINDEX_JOURNAL_PRIORITY_NEWINDEX + "," + REINDEX_ACTION_REINDEX_OBJECT
                    + ", " + TIMESTAMPSQL + " from contentlet_version_info where identifier is not null";
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
                    + " select distinct c.identifier,c.identifier," + REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX + ","
                    + REINDEX_ACTION_REINDEX_OBJECT + "," + TIMESTAMPSQL + " from contentlet c "
                    + " where c.structure_inode = ? and c.identifier is not null";
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
            Logger.fatal(this, "Error unlocking the reindex journal table" + ex);
        }
        return false;
    }

    protected void cleanDistReindexJournal() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority >= ?");
        dc.addParam(REINDEX_JOURNAL_PRIORITY_NEWINDEX - 10);
        dc.loadResult();
    }


    protected void deleteReindexEntry(IndexJournal iJournal) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where ident_to_index = ? or id= ?");
        dc.addParam(iJournal.getIdentToIndex());
        dc.addParam(iJournal.getId());
        dc.loadResult();
    }

    protected void resetServerForReindexEntry(Collection<IndexJournal> recordsToModify) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final int totalAttempts = REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT + RETRY_FAILED_INDEX_TIMES;
        final StringBuilder sql =
                new StringBuilder().append("UPDATE dist_reindex_journal SET serverid=NULL, priority = CASE WHEN priority < ")
                        .append(REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT).append(" THEN ")
                        .append(REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT).append(" WHEN priority = ").append(totalAttempts)
                        .append(" THEN priority ").append(" ELSE priority + 1 END where id in (");
        boolean first = true;

        for (IndexJournal idx : recordsToModify) {
            if (!first)
                sql.append(',');
            else
                first = false;
            sql.append(idx.getId());
        }

        sql.append(") AND priority <= " + totalAttempts);
        dotConnect.setSQL(sql.toString());
        dotConnect.loadResult();

    }

    protected void deleteReindexEntry(final Collection<IndexJournal> recordsToDelete) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder sql = new StringBuilder().append("DELETE FROM dist_reindex_journal where ident_to_index in (");
        boolean first = true;

        for (IndexJournal idx : recordsToDelete) {
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

    protected void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException {
        StringBuilder reindexJournalCleanupSql = new StringBuilder();

        String sign = "+";
        if (!add)
            sign = "-";

        if (DbConnectionFactory.isMsSql() || DbConnectionFactory.isH2())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " + " WHERE time_entered < DATEADD(" + type.toString() + ", "
                    + sign + "" + time + ", " + DbConnectionFactory.getDBDateTimeFunction() + ") ");
        else if (DbConnectionFactory.isMySql())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " + " WHERE time_entered < DATE_ADD(NOW(), INTERVAL " + sign
                    + "" + time + " " + type.toString() + ") ");
        else if (DbConnectionFactory.isPostgres())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " + " WHERE time_entered < NOW() " + sign + " INTERVAL '"
                    + time + " " + type.toString() + "' ");
        else if (DbConnectionFactory.isOracle())
            reindexJournalCleanupSql
                    .append("DELETE FROM dist_reindex_journal " + " WHERE CAST(time_entered AS TIMESTAMP) <  CAST(SYSTIMESTAMP " + sign
                            + "  INTERVAL '" + time + "' " + type.toString() + " AS TIMESTAMP)");

        if (includeInodeCheck)
            reindexJournalCleanupSql.append(" AND inode_to_index NOT IN " + " (SELECT i.inode FROM inode i,contentlet c "
                    + "  WHERE type = 'contentlet' AND i.inode=c.inode AND c.identifier = ident_to_index)");

        reindexJournalCleanupSql.append(" AND serverid = ?");

        DotConnect dc = new DotConnect();
        String serverId = ConfigUtils.getServerId();
        dc.setSQL(reindexJournalCleanupSql.toString());
        dc.addParam(serverId);
        dc.loadResult();
    }

    protected List<IndexJournal> findContentReindexEntriesToReindex() throws DotDataException {
        return findContentReindexEntriesToReindex(false);
    }

    protected List<IndexJournal> findContentReindexEntriesToReindex(boolean includeFailedRecords) throws DotDataException {
        DotConnect dc = new DotConnect();
        List<IndexJournal> x = new ArrayList<IndexJournal>();
        List<Map<String, Object>> results;
        Connection con = null;
        String serverId = ConfigUtils.getServerId();

        try {

            // Get the number of records to fetch
            int recordsToFetch = Config.getIntProperty("REINDEX_RECORDS_TO_FETCH", 100);
            int priorityLevel = REINDEX_JOURNAL_PRIORITY_NEWINDEX;
            if (includeFailedRecords) {
                priorityLevel = REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT + (RETRY_FAILED_INDEX_TIMES);
            }

            con = DbConnectionFactory.getConnection();
            con.setAutoCommit(false);
            if (DbConnectionFactory.isOracle()) {
                CallableStatement call = con.prepareCall("{ ? = call load_records_to_index(?,?,?) }");
                call.registerOutParameter(1, OracleTypes.CURSOR);
                call.setString(2, serverId);
                call.setInt(3, recordsToFetch);
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

                dc.setSQL("load_records_to_index @server_id='" + serverId + "', @records_to_fetch=" + String.valueOf(recordsToFetch)
                        + ", @priority_level=" + String.valueOf(priorityLevel));
                dc.setForceQuery(true);
                results = dc.loadObjectResults(con);
            } else if (DbConnectionFactory.isH2()) {
                dc.setSQL("call load_records_to_index(?,?,?)");
                dc.addParam(serverId);
                dc.addParam(recordsToFetch);
                dc.addParam(priorityLevel);
                results = dc.loadObjectResults();
            } else {
                dc.setSQL(REINDEXENTRIESSELECTSQL);
                dc.addParam(serverId);
                dc.addParam(recordsToFetch);
                dc.addParam(priorityLevel);
                results = dc.loadObjectResults(con);
            }

            for (Map<String, Object> r : results) {
                IndexJournal ij = new IndexJournal();
                ij.setId(((Number) r.get("id")).longValue());
                String o1 = (String) r.get("ident_to_index");
                ij.setIdentToIndex(o1);
                ij.setPriority(((Number) (r.get("priority"))).intValue());
                x.add(ij);
            }
        } catch (SQLException e1) {
            throw new DotDataException(e1.getMessage(), e1);
        } finally {
            try {
                con.commit();
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            } finally {
                try {
                    con.close();
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
        }
        return x;
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
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
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
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(hostId);
        dc.addParam(folderPath + "%");
        dc.loadResult();
    }

    protected void addIdentifierReindex(final String identifier) throws DotDataException {

        new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier).addParam(identifier)
                .addParam(REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX).addParam(REINDEX_ACTION_REINDEX_OBJECT).addParam(new Date())
                .loadResult();
    } // addIdentifierReindex.

    protected void addReindexHighPriority(final String identifier) throws DotDataException {

        new DotConnect().setSQL(REINDEX_JOURNAL_INSERT).addParam(identifier).addParam(identifier)
                .addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX).addParam(REINDEX_ACTION_REINDEX_OBJECT).addParam(new Date())
                .loadResult();
    } // addReindexHighPriority.

    protected int addIdentifierReindex(final Collection<String> identifiers) throws DotDataException {

        return this.addIdentifierReindex(identifiers, REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX);
    }

    protected int addReindexHighPriority(final Collection<String> identifiers) throws DotDataException {

        return this.addIdentifierReindex(identifiers, REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
    }

    private int addIdentifierReindex(final Collection<String> identifiers, final int prority) throws DotDataException {

        if (identifiers.isEmpty()) {
            return 0;
        }

        final List<Params> insertParams = new ArrayList<>(identifiers.size());
        final Date date = DbConnectionFactory.now();
        for (final String identifier : identifiers) {
            insertParams.add(new Params(identifier, identifier, prority, REINDEX_ACTION_REINDEX_OBJECT, date));
        }

        final List<Integer> batchResult =
                Ints.asList(new DotConnect().executeBatch(REINDEX_JOURNAL_INSERT, insertParams, (preparedStatement, params) -> {
                    preparedStatement.setString(1, String.class.cast(params.get(0)));
                    preparedStatement.setString(2, String.class.cast(params.get(1)));
                    preparedStatement.setInt(3, Integer.class.cast(params.get(2)));
                    preparedStatement.setInt(4, Integer.class.cast(params.get(3)));
                    preparedStatement.setObject(5, Date.class.cast(params.get(4)));
                }));

        final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
        Logger.info(this, "Batch rows inserted for reindex: " + rowsAffected);
        return rowsAffected;
    }

    protected void refreshContentUnderHost(Host host) throws DotDataException {
        String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT id, id, ?, ? "
                + " FROM identifier " + " WHERE asset_type='contentlet' and identifier.host_inode=?";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) " + " SELECT asset_id, asset_id, ?, ? "
                + " FROM permission_reference " + " WHERE reference_id=?";
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();
    }

    @WrapInTransaction
    public void requeStaleReindexRecords(final int secondsOld) throws DotDataException {

        final Date olderThan = new Date(System.currentTimeMillis() - (1000 * secondsOld));

        new DotConnect().setSQL("UPDATE dist_reindex_journal SET serverid=NULL where time_entered<? and serverid is not null")
                .addParam(olderThan).loadResult();

    }

}
