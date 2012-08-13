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

import com.dotcms.enterprise.ClusterThreadProxy;
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

import edu.emory.mathcs.backport.java.util.Arrays;

public class ESDistributedJournalFactoryImpl<T> extends DistributedJournalFactory<T> {
    
    private String[] serversIds = ClusterThreadProxy.getClusteredServerIds();
    private String serverId ;
    
    private boolean indexationEnabled = Config.getBooleanProperty("DIST_INDEXATION_ENABLED");
    
    private String TIMESTAMPSQL = "NOW()";
    private String REINDEXENTRIESSELECTSQL = "SELECT * FROM load_records_to_index(?, ?)";
    private String ORACLEREINDEXENTRIESSELECTSQL = "SELECT * FROM table(load_records_to_index(?, ?))";
    private String MYSQLREINDEXENTRIESSELECTSQL = "{call load_records_to_index(?,?)}";
    
    public ESDistributedJournalFactoryImpl(T newIndexValue) {
        super(newIndexValue);

        Logger.info(this, "Server IDs configured: " + Arrays.toString(serversIds));
        
        serverId = ConfigUtils.getServerId();
        
        if (serversIds.length < 1) {
            serversIds = new String[] { serverId };
        }
        
        if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
            TIMESTAMPSQL = "GETDATE()";
        } else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
            if(DbConnectionFactory.getDbVersion() >= 10)
               REINDEXENTRIESSELECTSQL = ORACLEREINDEXENTRIESSELECTSQL;
            else
                REINDEXENTRIESSELECTSQL = "SELECT * FROM table(CAST(load_records_to_index(?, ?)))";
            TIMESTAMPSQL = "CAST(SYSTIMESTAMP AS TIMESTAMP)"; 
        } else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)) {
            REINDEXENTRIESSELECTSQL = MYSQLREINDEXENTRIESSELECTSQL;
            
        }
    }
    
    @Override
    protected void addBuildNewIndexEntries() throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index, priority, dist_action, time_entered) " +
                  " select distinct identifier,identifier," + REINDEX_JOURNAL_PRIORITY_NEWINDEX +"," + REINDEX_ACTION_REINDEX_OBJECT + ", " + TIMESTAMPSQL +  
                  " from contentlet where inode is not null and identifier is not null";
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
        try {
            if (indexationEnabled) {
                con = DbConnectionFactory.getConnection();
                con.setAutoCommit(false);
                java.sql.Timestamp timestamp = new java.sql.Timestamp(new java.util.Date().getTime());
                for (String serversId : serversIds) {
                    if (!serverId.equals(serversId)) {
                        DotConnect dc = new DotConnect();
                        dc.setSQL("INSERT INTO dist_process(object_to_index, time_entered, serverid, journal_type)VALUES (?, ?, ?, ?)");
                        dc.addParam(key + ":" + group);
                        dc.addParam(timestamp);
                        dc.addParam(serversId);
                        dc.addParam(JOURNAL_TYPE_CACHE);
                        try {
                            dc.getResult(con);
                        } catch (Exception e) {
                            Logger
                                    .warn(this,
                                            "Usually not a problem but a cache entry failed to insert in the table.");
                            Logger.debug(this, e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (SQLException e1) {
            throw new DotDataException(e1.getMessage(), e1);
        } finally {
            try {
                if(con!=null){
                    con.commit();
                }
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            } finally {
                try {
                    if(con!=null){
                        con.close();
                    }
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected void addStructureReindexEntries(T structureInode)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        Connection conn = null;
        try {
            conn = DbConnectionFactory.getConnection();
            conn.setAutoCommit(false);
            String sql = "insert into dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action, time_entered) " +
            		" select distinct c.identifier,c.identifier,"+REINDEX_JOURNAL_PRIORITY_STRUCTURE_REINDEX+"," + 
                    REINDEX_ACTION_REINDEX_OBJECT + "," + TIMESTAMPSQL  + " from contentlet c " +
                    		" where c.structure_inode = ?";
            dc.setSQL(sql);
            dc.addParam(structureInode);
            dc.loadResult(conn);
            
        } catch (SQLException ex) {
            Logger.fatal(this,"Error  unlocking the reindex journal table" +  ex);
        } finally {
            try {
                conn.commit();
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            } finally {
                try {
                    conn.close();
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected boolean areRecordsLeftToIndex() throws DotDataException {
        DotConnect dc = new DotConnect();
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
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
        return count > 0;
    }

    @Override
    protected void cleanDistReindexJournal() throws DotDataException {
        DotConnect dc = new DotConnect();
        dc.setSQL("DELETE From dist_reindex_journal where priority =? or priority=?");
        dc.addParam(REINDEX_JOURNAL_PRIORITY_NEWINDEX);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_NEWINDEX-10);
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
    protected void deleteReindexEntryForServer(List<IndexJournal<T>> recordsToDelete) throws DotDataException {
        DotConnect dc = new DotConnect();
        StringBuilder sql=new StringBuilder().append("DELETE FROM dist_reindex_journal where id in (");
        boolean first=true;
        for(IndexJournal<T> idx : recordsToDelete) {
            if(!first) sql.append(','); else first=false;
            sql.append(idx.getId());
        }
        sql.append(')');
        
        dc.setSQL(sql.toString());
        dc.loadResult();
    }

    @Override
    protected void distReindexJournalCleanup(int time, boolean add, boolean includeInodeCheck, DateType type) throws DotDataException {
        StringBuilder reindexJournalCleanupSql = new StringBuilder();

        String sign = "+";
        if(!add)
            sign = "-";

        if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < DATEADD("+ type.toString() +", "+ sign + "" + time +", GETDATE()) ");
        else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < DATE_ADD(NOW(), INTERVAL "+ sign + "" + time +" " + type.toString()+") ");
        else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL))
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE time_entered < NOW() "+ sign + " INTERVAL '"+ time +" " + type.toString()  +"' ");
        else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))
            reindexJournalCleanupSql.append("DELETE FROM dist_reindex_journal " +
            		" WHERE CAST(time_entered AS TIMESTAMP) <  CAST(SYSTIMESTAMP "+ sign + 
            		     "  INTERVAL '"+time+"' "+ type.toString() + " AS TIMESTAMP)");
        
        if(includeInodeCheck)
            reindexJournalCleanupSql.append(" AND inode_to_index NOT IN " +
            		" (SELECT i.inode FROM inode i,contentlet c " +
            		"  WHERE type = 'contentlet' AND i.inode=c.inode AND c.identifier = ident_to_index)");
        
        reindexJournalCleanupSql.append(" AND serverid = ?");

        Connection conn = null;
        DotConnect dc = new DotConnect();
        dc.setSQL(reindexJournalCleanupSql.toString());
        dc.addParam(serverId);
        dc.loadResult();
    }

    private void deleteCacheEntries(String serverId, long id, Connection con)
            throws DotDataException {
        DotConnect dc = new DotConnect();
        try {
            dc.setSQL("DELETE FROM dist_journal where serverid = ? and journal_type = ? and id < ?");
            dc.addParam(serverId);
            dc.addParam(JOURNAL_TYPE_CACHE);
            dc.addParam(id + 1);
            dc.loadResult(con);
        } catch (Exception e1) {
            throw new DotDataException(e1.getMessage(), e1);
        }
    }
    
    @Override
    protected List<String> findCacheEntriesToRemove() throws DotDataException {
        DotConnect dc = new DotConnect();
        List<String> x = new ArrayList<String>();
        Connection con = null;
        try {
            con = DbConnectionFactory.getConnection();
            con.setAutoCommit(false);
            dc.setSQL("SELECT object_to_index , max(id) as id from dist_journal " +
            		" where journal_type = ? and serverid = ? GROUP BY id, object_to_index,time_entered ORDER BY time_entered ASC");
            dc.addParam(JOURNAL_TYPE_CACHE);
            dc.addParam(serverId);

            List<Map<String, String>> results = dc.loadResults(con);
            long id = 0;
            for (Map<String, String> r : results) {
                x.add(r.get("object_to_index"));
                id = new Long(r.get("id"));
            }
            deleteCacheEntries(serverId, id, con);
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
    protected List<IndexJournal<T>> findContentReindexEntriesToReindex()
            throws DotDataException {
        DotConnect dc = new DotConnect();
        List<IndexJournal<T>> x = new ArrayList<IndexJournal<T>>();
        List<Map<String, Object>> results;
        Connection con = null;
        try {
            con = DbConnectionFactory.getConnection();
            con.setAutoCommit(false);
            if(DbConnectionFactory.isOracle()) {
                CallableStatement call = con.prepareCall("{ ? = call load_records_to_index(?,?) }");
                call.registerOutParameter(1, OracleTypes.CURSOR);
                call.setString(2, serverId);
                call.setInt(3, 50);
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
        
                dc.setSQL("load_records_to_index @server_id='"+serverId+"', @records_to_fetch=50");
                dc.setForceQuery(true);
                results = dc.loadObjectResults(con);
            } else {
                dc.setSQL(REINDEXENTRIESSELECTSQL);
                dc.addParam(serverId);
                dc.addParam(50);
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
        return serverId;
    }

    @Override
    protected boolean isIndexationEnabled() {
        return indexationEnabled;
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
    protected void refreshContentUnderHost(Host host) throws DotDataException {
        final String sql = " INSERT INTO dist_reindex_journal(inode_to_index,ident_to_index,priority,dist_action) "+ 
                " SELECT distinct identifier.id, identifier.id, ?, ? " +
                " FROM contentlet join identifier ON contentlet.identifier=identifier.id "+ 
                " WHERE identifier.host_inode=?";
        DotConnect dc = new DotConnect();
        dc.setSQL(sql);
        dc.addParam(REINDEX_JOURNAL_PRIORITY_CONTENT_REINDEX);
        dc.addParam(REINDEX_ACTION_REINDEX_OBJECT); 
        dc.addParam(host.getIdentifier());
        dc.loadResult();
    }

    @Override
    protected void setIndexationEnabled(boolean indexationEnabled) {
        this.indexationEnabled = indexationEnabled;
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

        
        private String oraClusterLock = "LOCK TABLE DIST_JOURNAL IN EXCLUSIVE MODE";
        private String pgLock = "lock table DIST_JOURNAL;";
        
        public ClusterMutex(Connection conn) {
            conn1 = conn;
        }

        Connection conn1 = null;

        public void lockTable() throws SQLException {
            if (DbConnectionFactory.getDBType().equals(
                    DbConnectionFactory.MYSQL)) {
                // We need another connection, to get around mysqls limitations
                // with locks (the need to lock all or no tables in a query)
                conn1 = DbConnectionFactory.getDataSource().getConnection();
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(myLock);
            }
            
            
            if (DbConnectionFactory.getDBType().equals(
                    DbConnectionFactory.ORACLE)) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(oraClusterLock);
            }
            
            if (DbConnectionFactory.getDBType().equals(
                    DbConnectionFactory.MSSQL)) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(msLock);
            }
            
            if (DbConnectionFactory.getDBType().equals(
                    DbConnectionFactory.POSTGRESQL)) {
                conn1.setAutoCommit(false);
                Statement s = conn1.createStatement();
                s.execute(pgLock);
            }
            
        }

        public void unlockTable() throws SQLException {
            if (DbConnectionFactory.getDBType().equals(
                    DbConnectionFactory.MYSQL)) {
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
