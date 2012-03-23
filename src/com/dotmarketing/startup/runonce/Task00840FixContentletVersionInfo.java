package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * http://jira.dotmarketing.net/browse/DOTCMS-7291 Move
 * contentlet_lang_version_info to contentlet_version_info
 * 
 * @author Jorge Urdaneta
 */
public class Task00840FixContentletVersionInfo implements StartupTask {
    
    public boolean forceRun() {
        
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            DotConnect dc = new DotConnect();
            dc.setSQL("select count(*) from contentlet_lang_version_info");
            dc.loadResult();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
    
    void addFields() throws SQLException {
        DotConnect dc = new DotConnect();
        String langType = "int8";
        if (DbConnectionFactory.isOracle())
            langType = "number(19,0)";
        else if (DbConnectionFactory.isMsSql())
            langType = "numeric(19,0)";
        String uuidType = "varchar(36)";
        if (DbConnectionFactory.isOracle())
            uuidType = "varchar2(36)";
        
        dc.executeStatement("alter table contentlet_version_info add lang "
                + langType);
        dc.executeStatement("alter table contentlet_version_info add working_inode "
                + uuidType);
        dc.executeStatement("alter table contentlet_version_info add live_inode "
                + uuidType);
        
    }
    
    void migrateData() throws SQLException {
        Connection con = DbConnectionFactory.getConnection();
        PreparedStatement selContVersion = null;
        final int limit=1000;
        if (DbConnectionFactory.isOracle()) {
            selContVersion = con
                    .prepareStatement("select * from ( "
                            + "   select identifier,locked_on,locked_by,deleted, row_number() over (order by identifier) rn "
                            + "   from contentlet_version_info where lang is null "
                            + ") where rn >= ? and rn < ? ");
        } else if (DbConnectionFactory.isMsSql()) {
            selContVersion = con
                    .prepareStatement(" SELECT TOP "
                            + limit
                            + "  * FROM (SELECT identifier,locked_on,locked_by,deleted,ROW_NUMBER() "
                            + " OVER (order by identifier) AS RowNumber FROM "
                            + " contentlet_version_info where lang is null) temp "
                            + " WHERE RowNumber > ? ");
            
        } else {
            selContVersion = con
                    .prepareStatement("select identifier,locked_on,locked_by,deleted from "
                            + " contentlet_version_info "
                            + " where lang is null "
                            + " limit ? offset ?");
        }
        
        PreparedStatement insertVersionInfo = con
                .prepareStatement("insert into "
                        + " contentlet_version_info "
                        + " (identifier,working_inode,locked_on,locked_by,deleted,lang,live_inode) "
                        + " values (?,?,?,?,?,?,?)");
        
        PreparedStatement selLangInfo = con
                .prepareStatement("select lang,working_inode,live_inode from contentlet_lang_version_info where identifier=?");
        
        int offset = 0;
        boolean notDone = true;
        do {
            if (DbConnectionFactory.isMsSql()) {
                selContVersion.setInt(1, offset);
            } else if (DbConnectionFactory.isOracle()) {
                selContVersion.setInt(1, offset);
                selContVersion.setInt(2, offset + limit);
            } else {
                selContVersion.setInt(1, limit);
                selContVersion.setInt(2, offset);
            }
            offset = offset + limit;
            
            notDone=false;
            ResultSet rs=selContVersion.executeQuery();
            while(rs.next()) {
                notDone=true;
                
                String identifier=rs.getString("identifier");
                Date lockedOn=rs.getDate("locked_on");
                String lockedBy=rs.getString("locked_by");
                Boolean deleted=rs.getBoolean("deleted");
                
                selLangInfo.setString(1, identifier);
                ResultSet langinfo=selLangInfo.executeQuery();
                while(langinfo.next()) {
                    long lang=langinfo.getLong("lang");
                    String working_inode=langinfo.getString("working_inode");
                    String live_inode=langinfo.getString("live_inode");
                    if(langinfo.wasNull())
                        live_inode=null;
                    
                    insertVersionInfo.setString(1,identifier);
                    insertVersionInfo.setString(2, working_inode);
                    insertVersionInfo.setDate(3, lockedOn);
                    insertVersionInfo.setString(4, lockedBy);
                    insertVersionInfo.setBoolean(5, deleted);
                    insertVersionInfo.setLong(6, lang);
                    if(live_inode!=null)
                        insertVersionInfo.setString(7, live_inode);
                    else
                        insertVersionInfo.setNull(7, Types.VARCHAR);
                    
                    insertVersionInfo.executeUpdate();
                }
            }
            rs.close();
        } while (notDone);
        selContVersion.close();
        insertVersionInfo.close();
        selLangInfo.close();
    }
    
    void removeOldRecords() throws SQLException {
        // delete those old records in contentlet_version_info with lang and working_inode in null
        Connection con=DbConnectionFactory.getConnection();
        Statement removeOldData=con.createStatement();
        removeOldData.executeUpdate("delete from contentlet_version_info where lang is null and working_inode is null");
        removeOldData.close();
    }
    
    void dropTable() throws SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement("drop table contentlet_lang_version_info");
    }
    
    void fixNotNull() throws SQLException {
        // contentlet_version_info => working_inode & lang
        DotConnect dc=new DotConnect();
        
        if(DbConnectionFactory.isMsSql()) {
            // MS SQL
            dc.executeStatement("ALTER TABLE contentlet_version_info ALTER COLUMN lang numeric(19,0) NOT NULL");
            dc.executeStatement("ALTER TABLE contentlet_version_info ALTER COLUMN working_inode varchar(36) NOT NULL");
        }
        else if(DbConnectionFactory.isOracle()) {
            // ORACLE
            dc.executeStatement("ALTER TABLE contentlet_version_info MODIFY (lang NOT NULL)");
            dc.executeStatement("ALTER TABLE contentlet_version_info MODIFY (working_inode NOT NULL)");
        }
        else if(DbConnectionFactory.isMySql()) {
            // MySQL
            dc.executeStatement("ALTER TABLE contentlet_version_info MODIFY lang int8 NOT NULL");
            dc.executeStatement("ALTER TABLE contentlet_version_info MODIFY working_inode varchar(36) NOT NULL");
        }
        else if(DbConnectionFactory.isPostgres()) {
            // PostgreSQL
            dc.executeStatement("ALTER TABLE contentlet_version_info ALTER COLUMN lang SET NOT NULL");
            dc.executeStatement("ALTER TABLE contentlet_version_info ALTER COLUMN working_inode SET NOT NULL");
        }
    }
    
    void addForeignKeys() throws SQLException {
        DotConnect dc=new DotConnect();
        dc.executeStatement("alter table contentlet_version_info add constraint fk_cont_version_info_working  foreign key (working_inode) references contentlet(inode)");
        dc.executeStatement("alter table contentlet_version_info add constraint fk_cont_version_info_live     foreign key (live_inode)    references contentlet(inode)");
    }
    
    void dropVersionInfoPK() throws SQLException {
        DotConnect dc=new DotConnect();
        String t="contentlet_version_info";
        Connection conn=DbConnectionFactory.getConnection();
        DatabaseMetaData dbmd=conn.getMetaData();
        String schema=null;
        if(DbConnectionFactory.isOracle()){
            t = t.toUpperCase();
            schema=dbmd.getUserName();
        }
        ResultSet rs=dbmd.getPrimaryKeys(conn.getCatalog(), schema, t);
        rs.next();
        String pkName=rs.getString("PK_NAME");
        if(DbConnectionFactory.isMySql()) 
            dc.executeStatement("ALTER TABLE " + t + " DROP PRIMARY KEY ");
        else
            dc.executeStatement("ALTER TABLE " + t + " DROP CONSTRAINT "+pkName);
            
    }
    
    void addPrimaryKey() throws Exception {
        DotConnect dc=new DotConnect();
        dc.executeStatement("ALTER TABLE contentlet_version_info add constraint contentlet_version_info_pkey PRIMARY KEY (identifier,lang)");
    }
    
    void dropContLangVersionInfoFK(String t) throws Exception {
        DotConnect dc=new DotConnect();
        Connection conn=DbConnectionFactory.getConnection();
        DatabaseMetaData dbmd=conn.getMetaData();
        String schema=null;
        if(DbConnectionFactory.isOracle()){
            t = t.toUpperCase();
            schema=dbmd.getUserName();
        }
        ResultSet rs=dbmd.getImportedKeys(conn.getCatalog(), schema, t);
        while (rs.next()) {
            String FK_NAME=rs.getString("FK_NAME");
            if(DbConnectionFactory.isMySql()) 
                dc.executeStatement("ALTER TABLE "+t+" DROP FOREIGN KEY "+FK_NAME);
            else
                dc.executeStatement("ALTER TABLE "+t+" DROP CONSTRAINT "+FK_NAME);
        }
    }
    
    void addForeignKeysExtraMySQL() throws Exception {
        DotConnect dc=new DotConnect();
        dc.executeStatement(
           "alter table container_version_info  add constraint fk_con_ver_info_ident " +
           "  foreign key (identifier) references identifier(id)");
        dc.executeStatement(
           "alter table contentlet_version_info add constraint FK_con_ver_lockedby    " +
           "  foreign key (locked_by) references user_(userid)");
    }
    
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        try {
            addFields();
            dropContLangVersionInfoFK("contentlet_lang_version_info");
            if(DbConnectionFactory.isMySql())
                dropContLangVersionInfoFK("contentlet_version_info");
            dropVersionInfoPK();
            migrateData();
            dropTable();
            removeOldRecords();
            fixNotNull();
            addForeignKeys();
            if(DbConnectionFactory.isMySql())
                addForeignKeysExtraMySQL();
            addPrimaryKey();
            
            if(DbConnectionFactory.isMsSql())
                fixMySQLIdentifierTriggers();
        } catch (Exception ex) {
            throw new DotRuntimeException(ex.getMessage(), ex);
        }
    }
    
    void fixMySQLIdentifierTriggers() throws SQLException {
        String parentPathCheckWhenUpdate =  "DROP TRIGGER IF EXISTS check_parent_path_when_update;\n"+
                "CREATE TRIGGER check_parent_path_when_update  BEFORE UPDATE\n"+
                "on identifier\n"+
                "FOR EACH ROW\n"+
                "BEGIN\n"+
                "DECLARE idCount INT;\n"+
                "DECLARE canUpdate boolean default false;\n"+
                " IF @disable_trigger IS NULL THEN\n"+
                "   select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"+
                "   IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"+
                "     SET canUpdate := TRUE;\n"+
                "   END IF;\n"+
                "   IF(canUpdate = FALSE) THEN\n"+
                "     delete from Cannot_update_for_this_path_does_not_exist_for_the_given_host;\n"+
                "   END IF;\n"+
                " END IF;\n"+
                "END\n"+
                "#\n";

        String parentPathCheckWhenInsert =  "DROP TRIGGER IF EXISTS check_parent_path_when_insert;\n"+
                "CREATE TRIGGER check_parent_path_when_insert  BEFORE INSERT\n"+
                "on identifier\n"+
                "FOR EACH ROW\n"+
                "BEGIN\n"+
                "DECLARE idCount INT;\n"+
                "DECLARE canInsert boolean default false;\n"+
                " select count(id)into idCount from identifier where asset_type='folder' and CONCAT(parent_path,asset_name,'/')= NEW.parent_path and host_inode = NEW.host_inode and id <> NEW.id;\n"+
                " IF(idCount > 0 OR NEW.parent_path = '/' OR NEW.parent_path = '/System folder') THEN\n"+
                "   SET canInsert := TRUE;\n"+
                " END IF;\n"+
                " IF(canInsert = FALSE) THEN\n"+
                "  delete from Cannot_insert_for_this_path_does_not_exist_for_the_given_host;\n"+
                " END IF;\n"+
                "END\n"+
                "#\n";
        
        DotConnect dc=new DotConnect();
        dc.executeStatement(parentPathCheckWhenUpdate);
        dc.executeStatement(parentPathCheckWhenInsert);
    }
    
}
