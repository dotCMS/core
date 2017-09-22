package com.dotcms.journal.business;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.jdbc.OracleTypes;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.business.journal.DistributedJournalAPI.DateType;
import com.dotmarketing.common.business.journal.DistributedJournalFactory;
import com.dotmarketing.common.business.journal.IndexJournal;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Provides access to all the routines associated to the re-indexation process 
 * in dotCMS.  
 * 
 * @author root
 * @version 3.3
 * @since Mar 22, 2012
 *
 */
public class ESDistributedJournalFactoryImpl<T> extends DistributedJournalFactory<T> {

    private String TIMESTAMPSQL = "NOW()";
    private String REINDEXENTRIESSELECTSQL = "SELECT * FROM load_records_to_index(?, ?, ?)";
    private String ORACLEREINDEXENTRIESSELECTSQL = "SELECT * FROM table(load_records_to_index(?, ?, ?))";
    private String MYSQLREINDEXENTRIESSELECTSQL = "{call load_records_to_index(?,?,?)}";

    public ESDistributedJournalFactoryImpl(T newIndexValue) {
        super(newIndexValue);

        if (DbConnectionFactory.isMsSql()) {
            TIMESTAMPSQL = "GETDATE()";
        } else if (DbConnectionFactory.isOracle()) {
            if(DbConnectionFactory.getDbVersion() >= 10)
               REINDEXENTRIESSELECTSQL = ORACLEREINDEXENTRIESSELECTSQL;
            else
                REINDEXENTRIESSELECTSQL = "SELECT * FROM table(CAST(load_records_to_index(?, ?, ?)))";
            TIMESTAMPSQL = "CAST(SYSTIMESTAMP AS TIMESTAMP)";
        } else if (DbConnectionFactory.isMySql()) {
            REINDEXENTRIESSELECTSQL = MYSQLREINDEXENTRIESSELECTSQL;

        }
    }

    @Override
    protected void addBuildNewIndexEntries() throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) " +
                  " select distinct identifier,identifier," + REINDEX_JOURNAL_PRIORITY_NEWINDEX +"," + REINDEX_ACTION_REINDEX_OBJECT + ", " + TIMESTAMPSQL +
                  " from contentlet_version_info where identifier is not null";
            dc.setSQL(sql);
            dc.loadResult();
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @Override
    protected void addCacheEntry(String key, String group)
            throws DotDataException {
        Connection con = null;


    }

    @Override
    protected void addStructureReindexEntries(T structureInode)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) " +
            		" select distinct c.identifier,c.identifier,"+REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX+"," +
                    REINDEX_ACTION_REINDEX_OBJECT + "," + TIMESTAMPSQL  + " from contentlet c " +
                    		" where c.structure_inode = ? and c.identifier is not null";
            dc.setSQL(sql);
            dc.addParam(structureInode);
            dc.loadResult();

        } catch (Exception ex) {
            Logger.fatal(this,"Error  unlocking the reindex journal table" +  ex);
        }
    }

    @Override
    protected boolean areRecordsLeftToIndex() throws DotDataException {

        DotConnect dc = new DotConnect();
        String serverId = ConfigUtils.getServerId();
        long count = 0;
        try {
            dc.setSQL("select count(*) as count from dist_reindex_journal");
            dc.addParam(serverId);
            List<Map<String, String>> results = dc.loadResults();
            String c = results.get(0).get("count");
            count = Long.parseLong(c);
        } catch (Exception ex) {
            Logger
            .fatal(this,"Error  unlocking the reindex journal table" +  ex);
        }

        return count > 0;
    }

    @Override
    protected void cleanDistReindexJournal() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority >= ?");
        dc.addParam(REINDEX_JOURNAL_PRIORITY_NEWINDEX - 10);
        dc.loadResult();
    }

    @Override
    protected void deleteContentIndexEntries(String serverId, long id)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            dc.setSQL("DELETE FROM dist_journal where serverid = ? and journal_type = ? and id < ?");
            dc.addParam(serverId);
            dc.addParam(JOURNAL_TYPE_CONTENTENTINDEX);
            dc.addParam(id + 1);
            dc.loadResult();
        } catch (Exception e1) {
            throw new DotDataException(e1.getMessage(), e1);
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    @Override
    protected void deleteLikeJournalRecords(IndexJournal<T> ijournal) throws DotDataException {
        String deleteLikeReindexRecords = "DELETE FROM dist_reindex_journal where serverid = ? AND ident_to_index = ? AND id <> ? ";
        String serverId = ConfigUtils.getServerId();
        DotConnect dc = new DotConnect();
        dc.setSQL(deleteLikeReindexRecords);
        dc.addParam(serverId);
        dc.addParam(ijournal.getIdentToIndex());
        dc.addParam(ijournal.getId());
        dc.loadResult();
    }

    @Override
    protected void deleteReindexEntryForServer(IndexJournal<T> iJournal) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_reindex_journal where id = ?");
        dc.addParam(iJournal.getId());
        dc.loadResult();
    }

    @Override
    protected void resetServerForReindexEntry ( List<IndexJournal<T>> recordsToModify ) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final int totalAttempts = REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT + RETRY_FAILED_INDEX_TIMES;
		final StringBuilder sql = new StringBuilder()
				.append("UPDATE dist_reindex_journal SET serverid=NULL, priority = CASE WHEN priority < ")
				.append(REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT).append(" THEN ")
				.append(REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT).append(" WHEN priority = ").append(totalAttempts)
				.append(" THEN priority ").append(" ELSE priority + 1 END where id in (");
        boolean first = true;

        for ( IndexJournal<T> idx : recordsToModify ) {
            if ( !first ) sql.append(',');
            else first = false;
            sql.append(idx.getId());
        }

        sql.append(") AND priority <= " + totalAttempts);
        dotConnect.setSQL(sql.toString());
        dotConnect.loadResult();

    }

    @Override
    protected void deleteReindexEntryForServer(final List<IndexJournal<T>> recordsToDelete) throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final StringBuilder sql = new StringBuilder().append("DELETE FROM dist_reindex_journal where ident_to_index in (");
        boolean first = true;

        for(IndexJournal<T> idx : recordsToDelete) {
            if(!first) sql.append(','); else first=false;
            sql.append("'" + idx.getIdentToIndex() + "'");
        }
        sql.append(')');

        dotConnect.setSQL(sql.toString());
        dotConnect.loadResult();
    }

    @Override
    protected void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException {
        StringBuilder reindexJournalCleanupSql = new StringBuilder();

        String sign = "+";
        if(!add)
            sign = "-";

        if (DbConnectionFactory.isMsSql() || DbConnectionFactory.isH2())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < DATEADD("+ type.toString() +", "+ sign + "" + time +", "+DbConnectionFactory.getDBDateTimeFunction()+") ");
        else if(DbConnectionFactory.isMySql())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < DATE_ADD(NOW(), INTERVAL "+ sign + "" + time +" " + type.toString()+") ");
        else if(DbConnectionFactory.isPostgres())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < NOW() "+ sign + " INTERVAL '"+ time +" " + type.toString()  +"' ");
        else if(DbConnectionFactory.isOracle())
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE CAST(time_entered AS TIMESTAMP) <  CAST(SYSTIMESTAMP "+ sign +
            		     "  INTERVAL '"+time+"' "+ type.toString() + " AS TIMESTAMP)");

        if(includeInodeCheck)
            reindexJournalCleanupSql.append(" AND inode_to_index NOT IN " +
            		" (SELECT i.inode FROM inode i,contentlet c " +
            		"  WHERE type = 'contentlet' AND i.inode=c.inode AND c.identifier = ident_to_index)");

        reindexJournalCleanupSql.append(" AND serverid = ?");

        DotConnect dc = new DotConnect();
        String serverId = ConfigUtils.getServerId();
        dc.setSQL(reindexJournalCleanupSql.toString());
        dc.addParam(serverId);
        dc.loadResult();
    }

    private void deleteCacheEntries(String serverId, long id)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE FROM dist_journal where serverid = ? and journal_type = ? and id < ?");
        dc.addParam(serverId);
        dc.addParam(JOURNAL_TYPE_CACHE);
        dc.addParam(id + 1);
        dc.loadResult();

    }

    @Override
    protected List<String> findCacheEntriesToRemove() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        final List<String> cacheEntries = new ArrayList<>();
        final String serverId = ConfigUtils.getServerId();

        dotConnect.setSQL("SELECT object_to_index , max(id) as id from dist_journal " +
                " where journal_type = ? and serverid = ? GROUP BY id, object_to_index,time_entered ORDER BY time_entered ASC");
        dotConnect.addParam(JOURNAL_TYPE_CACHE);
        dotConnect.addParam(serverId);

        final List<Map<String, String>> results = dotConnect.loadResults();
        long id = 0;

        for (Map<String, String> result : results) {
            cacheEntries.add(result.get("object_to_index"));
            id = new Long(result.get("id"));
        }

        deleteCacheEntries(serverId, id);

        return cacheEntries;
    }

    @Override
    protected List<IndexJournal<T>> findContentReindexEntriesToReindex()
            throws DotDataException {
        return findContentReindexEntriesToReindex(false);
    }
    
    @Override
    protected List<IndexJournal<T>> findContentReindexEntriesToReindex(boolean includeFailedRecords)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        List<IndexJournal<T>> x = new ArrayList<IndexJournal<T>>();
        List<Map<String, Object>> results;
        Connection con = null;
        String serverId = ConfigUtils.getServerId();

        try {

            //Get the number of records to fetch
            int recordsToFetch = Config.getIntProperty("REINDEX_RECORDS_TO_FETCH", 50);
            int priorityLevel = REINDEX_JOURNAL_PRIORITY_NEWINDEX;
            if (includeFailedRecords) {
            	priorityLevel = REINDEX_JOURNAL_PRIORITY_FAILED_FIRST_ATTEMPT + (RETRY_FAILED_INDEX_TIMES);
            }

            con = DbConnectionFactory.getConnection();
            con.setAutoCommit(false);
            if(DbConnectionFactory.isOracle()) {
                CallableStatement call = con.prepareCall("{ ? = call load_records_to_index(?,?,?) }");
                call.registerOutParameter(1, OracleTypes.CURSOR);
                call.setString(2, serverId);
                call.setInt(3, recordsToFetch);
                call.setInt(4, priorityLevel);
                call.execute();
                ResultSet r = (ResultSet)call.getObject(1);
                results = new ArrayList<Map<String,Object>>();
                while(r.next()) {
                    Map<String,Object> m=new HashMap<String,Object>();
                    m.put("id", r.getInt("id"));
                    m.put("inode_to_index", r.getString("inode_to_index"));
                    m.put("ident_to_index", r.getString("ident_to_index"));
                    m.put("priority", r.getInt("priority"));
                    results.add(m);
                }
                r.close();
                call.close();
            } else if(DbConnectionFactory.isMsSql()) {
                // we need to make sure this setting is ON because of the READPAST we use
                // in the stored procedure
                dc.setSQL("SET TRANSACTION ISOLATION LEVEL READ COMMITTED;");
                dc.loadResult();

                dc.setSQL("load_records_to_index @server_id='" + serverId + "', @records_to_fetch=" + String.valueOf(recordsToFetch) + ", @priority_level=" + String.valueOf(priorityLevel));
                dc.setForceQuery(true);
                results = dc.loadObjectResults(con);
            } else if(DbConnectionFactory.isH2()) {
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
                IndexJournal<T> ij = new IndexJournal<T>();
                ij.setId(((Number)r.get("id")).longValue());
                T o = (T)r.get("inode_to_index");
                T o1 = (T)r.get("ident_to_index");
                ij.setInodeToIndex(o);
                ij.setIdentToIndex(o1);
                ij.setPriority(((Number)(r.get("priority"))).intValue());
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

    @Override
    protected String getServerId() {
        return ConfigUtils.getServerId();
    }


    @Override
    protected void processJournalEntries() throws DotDataException {
        DotConnect dc = new DotConnect();
        Connection con = null;
        ClusterMutex mutex = null;
        try {
            con = DbConnectionFactory.getConnection();
            con.setAutoCommit(false);
            mutex = new ClusterMutex(con);
            mutex.lockTable();
            dc.setSQL("SELECT max(id) as max FROM dist_process");
            ArrayList<Map<String, String>> ret = dc.loadResults(con);
            if (ret != null && ret.size() > 0
                    && UtilMethods.isSet(ret.get(0).get("max"))) {

                Long max = Long.parseLong(ret.get(0).get("max"));

                dc.setSQL("INSERT INTO dist_journal (object_to_index, time_entered, serverid, journal_type) " +
                		" SELECT object_to_index, min(time_entered),  serverid, journal_type FROM dist_process p1 " +
                		" WHERE NOT EXISTS (SELECT p.id FROM dist_process p, dist_journal j " +
                		                  " WHERE p.object_to_index = j.object_to_index AND  p.serverid=j.serverid AND " +
                		                  " p.journal_type=j.journal_type AND p1.id = p.id) " +
                		       " AND id <=? GROUP BY object_to_index, serverid, journal_type");
                dc.addParam(max);
                dc.loadResult(con);

                dc.setSQL("DELETE FROM dist_process WHERE id<=?");
                dc.addParam(max);
                dc.loadResult(con);
            }

        } catch (SQLException e1) {
            throw new DotDataException(e1.getMessage(), e1);
        } finally {
            try {
                con.commit();
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            } finally {
                try{
                    mutex.unlockTable();
                }catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
                try {
                    con.close();
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
        }
    }

    protected long recordsLeftToIndexForServer() throws DotDataException {
        return recordsLeftToIndexForServer(DbConnectionFactory.getConnection());
    }

    @Override
    protected long recordsLeftToIndexForServer(Connection conn) throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as count from dist_reindex_journal");
        List<Map<String, String>> results = results = dc.loadResults(conn);
        String c = results.get(0).get("count");
        return Long.parseLong(c);
    }

    @Override
    protected void refreshContentUnderFolder(Folder folder) throws DotDataException {
        final String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "+
                           " SELECT distinct identifier.id, identifier.id, ?, ? " +
                           " FROM contentlet join identifier ON contentlet.identifier=identifier.id "+
                           " WHERE identifier.host_inode=? AND identifier.parent_path LIKE ? ";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(folder.getHostId());
        String folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
        dc.addParam(folderPath+"%");
        dc.loadResult();
    }

    @Override
    protected void refreshContentUnderFolderPath(String hostId, String folderPath) throws DotDataException {
        final String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "+
                           " SELECT distinct identifier.id, identifier.id, ?, ? " +
                           " FROM contentlet join identifier ON contentlet.identifier=identifier.id "+
                           " WHERE identifier.host_inode=? AND identifier.parent_path LIKE ? ";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(hostId);
        dc.addParam(folderPath+"%");
        dc.loadResult();
    }

    @Override
    protected void refreshContentUnderHost(Host host) throws DotDataException {
        String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "+
                " SELECT id, id, ?, ? " +
                " FROM identifier "+
                " WHERE asset_type='contentlet' and identifier.host_inode=?";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();

        // https://github.com/dotCMS/dotCMS/issues/2229
        sql =   " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "+
                " SELECT asset_id, asset_id, ?, ? " +
                " FROM permission_reference "+
                " WHERE reference_id=?";
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT);
        dc.addParam(host.getIdentifier());
        dc.loadResult();
    }

    @Override
    protected List<IndexJournal> viewReindexJournalData() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("select count(*) as mycount,serverid,priority from dist_reindex_journal group by serverid,priority order by serverid, priority");
        List<IndexJournal> journalList = new ArrayList<IndexJournal>();
        List<Map<String, String>> results = dc.loadResults();
        for (Map<String, String> r : results) {
            IndexJournal index = new IndexJournal(r.get("serverid"),new Integer(r.get("mycount")),new Long(r.get("priority")));
            journalList.add(index);
        }
        return journalList;
    }

    public class ClusterMutex {
        private String myLock = "lock table dist_lock write";
        private String myCommit = "unlock tables";

        private String msLock = "SELECT * FROM dist_lock WITH (XLOCK)";
        private String h2Lock = "select * from dist_lock for update";

        private String oraClusterLock = "LOCK TABLE DIST_JOURNAL IN EXCLUSIVE MODE";
        private String pgLock = "lock table DIST_JOURNAL;";

        public ClusterMutex(Connection conn) {
            conn1 = conn;
        }

        Connection conn1 = null;

        public void lockTable() throws SQLException {
            if (DbConnectionFactory.isMySql()) {
                // We need another connection, to get around mysqls limitations
                // with locks (the need to lock all or no tables in a query)
                conn1 = DbConnectionFactory.getDataSource().getConnection();
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(myLock);
            }


            if (DbConnectionFactory.isOracle()) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(oraClusterLock);
            }

            if (DbConnectionFactory.isMsSql()) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(msLock);
            }

            if (DbConnectionFactory.isPostgres()) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(pgLock);
            }

            if (DbConnectionFactory.isH2()) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(h2Lock);
            }

        }

        public void unlockTable() throws SQLException {
            if (DbConnectionFactory.isMySql()) {
                Statement s = conn1.createStatement();
                s.execute(myCommit);
                conn1.commit();
                // We requested a new one, this is why we close only in this
                // case
                conn1.close();

            }
            //No need to unlock for oracle, pg, or sql server.  Using passed in connection, the calling method should commit (which is all that's needed to unlock the tables)
        }
    }

}
