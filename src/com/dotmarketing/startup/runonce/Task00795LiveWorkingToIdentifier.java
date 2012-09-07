package com.dotmarketing.startup.runonce;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * http://jira.dotmarketing.net/browse/DOTCMS-6383
 *
 * In this task we need to
 *
 * - Drop live,working,deleted,locked attributes
 * from contentlet,container,template,links,htmlpage,file_assets.
 * With associated indexs
 *
 * - Create new records for version_info tables associated
 * with identifiers linking working and live inodes.
 *
 * - Drop old triggers that enforced rules with live, working versions
 * not needed anymore
 *
 * @author Jorge Urdaneta
 */
public class Task00795LiveWorkingToIdentifier implements StartupTask {

    protected void dropOldTriggers() throws DotDataException, SQLException {
        if(DbConnectionFactory.isPostgres()) {
            DotConnect dc = new DotConnect();
            dc.executeStatement(
                "drop trigger if exists content_work_version_trigger ON contentlet");
            dc.executeStatement(
                "drop function if exists content_work_version_check()");
            dc.executeStatement(
                "drop trigger if exists file_asset_live_version_trigger ON file_asset");
            dc.executeStatement(
                "drop function if exists file_asset_live_version_check()");
            dc.executeStatement(
                "drop trigger if exists content_live_version_trigger ON contentlet");
            dc.executeStatement(
                "drop function if exists content_live_version_check()");
        }
    }

    protected void createNewTables() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();

        dc.executeStatement(
        "create table contentlet_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   lang int8 not null,\n"+
        "   working_inode char(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   primary key (identifier, lang)\n"+
        ")");
        dc.executeStatement(
        "create table container_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   working_inode varchar(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   primary key (identifier)\n"+
        ")");
        dc.executeStatement(
        "create table template_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   working_inode varchar(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   primary key (identifier)\n"+
        ")");
        dc.executeStatement(
        "create table htmlpage_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   working_inode varchar(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   primary key (identifier)\n"+
        ")");
        dc.executeStatement(
        "create table fileasset_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   working_inode varchar(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   primary key (identifier)\n"+
        ")");
        dc.executeStatement(
        "create table link_version_info (\n"+
        "   identifier varchar(36) not null,\n"+
        "   working_inode varchar(36) not null,\n"+
        "   live_inode varchar(36),\n"+
        "   locked_by varchar(36),\n"+
        "   locked_on date,\n"+
        "   deleted bool not null,\n"+
        "   primary key (identifier)\n"+
        ")");
    }
    protected void createNewTablesForOracle() throws DotDataException,SQLException{
    	DotConnect dc = new DotConnect();
        
        dc.executeStatement("create table contentlet_version_info (\n"
                + "    identifier varchar2(36) not null,\n"
                + "    deleted number(1,0) not null,\n"
                + "    locked_by varchar2(100),\n" 
                + "    locked_on date,\n"
                + "    lang number(19,0) not null,\n"
                + "    working_inode varchar2(36) not null,\n"
                + "    live_inode varchar2(36),\n"
                + "    primary key (identifier, lang)\n" + ")");

    	dc.executeStatement(
    			"create table container_version_info (\n" +
    			"      identifier varchar2(36) not null,\n" +
    			"	   working_inode varchar2(36) not null,\n" +
    			"	   live_inode varchar2(36),\n" +
 		        "      deleted number(1,0) not null,\n" +
    			"	   locked_by varchar2(100),\n" +
    			"	   locked_on date,\n" +
    			"	   primary key (identifier)\n" +
    			")");

    	dc.executeStatement(
    			"create table htmlpage_version_info (\n" +
    			"     identifier varchar2(36) not null,\n" +
    			"     working_inode varchar2(36) not null,\n" +
    			"     live_inode varchar2(36),\n" +
    			"     deleted number(1,0) not null,\n" +
    			"     locked_by varchar2(100),\n" +
    			"     locked_on date,\n" +
    			"     primary key (identifier)\n" +
    			")");

    	dc.executeStatement(
    			"create table fileasset_version_info (\n" +
    			"     identifier varchar2(36) not null,\n" +
    			"     working_inode varchar2(36) not null,\n" +
    			"     live_inode varchar2(36),\n" +
    			"     deleted number(1,0) not null,\n" +
                "     locked_by varchar2(100),\n" +
                "     locked_on date,\n" +
                "     primary key (identifier)\n" +
                ")");

    	dc.executeStatement(
    			"create table template_version_info (\n" +
    			"     identifier varchar2(36) not null,\n" +
    			"     working_inode varchar2(36) not null,\n" +
    			"     live_inode varchar2(36),\n" +
    			"     deleted number(1,0) not null,\n" +
    			"     locked_by varchar2(100),\n" +
    			"     locked_on date,\n" +
    			"     primary key (identifier)\n" +
    			")");

    	dc.executeStatement(
    			"create table link_version_info (\n" +
    			"	 identifier varchar2(36) not null,\n" +
    			"    working_inode varchar2(36) not null,\n" +
    			"    live_inode varchar2(36),\n" +
                "    deleted number(1,0) not null,\n" +
                "    locked_by varchar2(100),\n" +
                "    locked_on date,\n" +
                "    primary key (identifier)\n" +
    			")");
    }
    protected void createNewTablesForSQLServer() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();
        
        dc.executeStatement("create table contentlet_version_info (\n"
                            + "identifier varchar(36) not null,\n"
                            + "deleted tinyint not null,\n"
                            + "locked_by varchar(100) null,\n"
                            + "locked_on datetime null,\n"
                            + "lang numeric(19,0) not null,\n"
                            + "working_inode varchar(36) not null,\n"
                            + "live_inode varchar(36) null,\n"
                            + "primary key (identifier, lang)\n" 
                            + ")");

    	dc.executeStatement("create table container_version_info (\n" +
    			   			"identifier varchar(36) not null,\n" +
    			   			"working_inode varchar(36) not null,\n" +
    			   			"live_inode varchar(36) null,\n" +
    			   			"deleted tinyint not null,\n" +
    			   			"locked_by varchar(100) null,\n" +
    			   			"locked_on datetime null,\n" +
    			   			"primary key (identifier)\n" +
    						")");
    	dc.executeStatement("create table template_version_info (\n" +
    			   			"identifier varchar(36) not null,\n" +
    			   			"working_inode varchar(36) not null,\n" +
    			   			"live_inode varchar(36) null,\n" +
    			   			"deleted tinyint not null,\n" +
    			   			"locked_by varchar(100) null,\n" +
    			   			"locked_on datetime null,\n" +
    			   			"primary key (identifier)\n" +
    						")");


    	dc.executeStatement("create table htmlpage_version_info (\n" +
    			   			"identifier varchar(36) not null,\n" +
    			   			"working_inode varchar(36) not null,\n" +
    			   			"live_inode varchar(36) null,\n" +
    			   			"deleted tinyint not null,\n" +
    			   			"locked_by varchar(100) null,\n" +
    			   			"locked_on datetime null,\n" +
    			   			"primary key (identifier)\n" +
    						")");

    	dc.executeStatement("create table link_version_info (\n" +
    			   			"identifier varchar(36) not null,\n" +
    			   			"working_inode varchar(36) not null,\n" +
    			   			"live_inode varchar(36) null,\n" +
    			   			"deleted tinyint not null,\n" +
    			   			"locked_by varchar(100) null,\n" +
    			   			"locked_on datetime null,\n" +
    			   			"primary key (identifier)\n" +
    						")");
    	dc.executeStatement("create table fileasset_version_info (\n" +
    			   			"identifier varchar(36) not null,\n" +
    			   			"working_inode varchar(36) not null,\n" +
    			   			"live_inode varchar(36) null,\n" +
    			   			"deleted tinyint not null,\n" +
    			   			"locked_by varchar(100) null,\n" +
    			   			"locked_on datetime not null,\n" +
    			   			"primary key (identifier)\n" +
    			            ")");

    }

    protected void addNewForeignKeys() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();
        dc.executeStatement(
          "alter table contentlet_version_info add constraint fk_con_ver_info_ident         foreign key (identifier) references identifier(id) on delete cascade");
        dc.executeStatement(
          "alter table container_version_info  add constraint fk_container_ver_info_ident   foreign key (identifier) references identifier(id)");
        dc.executeStatement(
          "alter table template_version_info   add constraint fk_template_ver_info_ident    foreign key (identifier) references identifier(id)");
        dc.executeStatement(
          "alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_ident    foreign key (identifier) references identifier(id)");
        dc.executeStatement(
          "alter table fileasset_version_info  add constraint fk_fileasset_ver_info_ident   foreign key (identifier) references identifier(id)");
        dc.executeStatement(
          "alter table link_version_info       add constraint fk_link_ver_info_ident        foreign key (identifier) references identifier(id)");
        dc.executeStatement(
          "alter table container_version_info  add constraint fk_contain_ver_info_working   foreign key (working_inode) references containers(inode)");
        dc.executeStatement(
          "alter table template_version_info   add constraint fk_temp_ver_info_working      foreign key (working_inode) references template(inode)");
        dc.executeStatement(
          "alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_working  foreign key (working_inode) references htmlpage(inode)");
        dc.executeStatement(
          "alter table fileasset_version_info  add constraint fk_fileasset_ver_info_working foreign key (working_inode) references file_asset(inode)");
        dc.executeStatement(
          "alter table link_version_info       add constraint fk_link_version_info_working  foreign key (working_inode) references links(inode)");
        dc.executeStatement(
          "alter table contentlet_version_info add constraint fk_cont_version_info_working  foreign key (working_inode) references contentlet(inode)");
        dc.executeStatement(
          "alter table container_version_info  add constraint fk_container_ver_info_live    foreign key (live_inode) references containers(inode)");
        dc.executeStatement(
          "alter table template_version_info   add constraint fk_template_ver_info_live     foreign key (live_inode) references template(inode)");
        dc.executeStatement(
          "alter table htmlpage_version_info   add constraint fk_htmlpage_ver_info_live     foreign key (live_inode) references htmlpage(inode)");
        dc.executeStatement(
          "alter table fileasset_version_info  add constraint fk_fileasset_ver_info_live    foreign key (live_inode) references file_asset(inode)");
        dc.executeStatement(
          "alter table link_version_info       add constraint fk_link_version_info_live     foreign key (live_inode) references links(inode)");
        dc.executeStatement(
          "alter table contentlet_version_info add constraint fk_cont_version_info_live     foreign key (live_inode) references contentlet(inode)");
        dc.executeStatement(
          "alter table contentlet_version_info add constraint fk_cont_ver_info_lang         foreign key (lang) references language(id)");
        dc.executeStatement(
          "alter table contentlet              add constraint fk_contentlet_lang            foreign key (language_id) references language(id)");
        dc.executeStatement(
          "alter table contentlet_version_info add constraint FK_con_ver_lockedby           foreign key (locked_by) references user_(userid)");
        dc.executeStatement(
          "alter table container_version_info  add constraint FK_tainer_ver_info_lockedby   foreign key (locked_by) references user_(userid)");
        dc.executeStatement(
          "alter table template_version_info   add constraint FK_temp_ver_info_lockedby     foreign key (locked_by) references user_(userid)");
        dc.executeStatement(
          "alter table htmlpage_version_info   add constraint FK_page_ver_info_lockedby     foreign key (locked_by) references user_(userid)");
        dc.executeStatement(
          "alter table fileasset_version_info  add constraint FK_fil_ver_info_lockedby      foreign key (locked_by) references user_(userid)");
        dc.executeStatement(
          "alter table link_version_info       add constraint FK_link_ver_info_lockedby     foreign key (locked_by) references user_(userid)");
    }

    protected void associateWorking(String table) throws DotDataException, SQLException {
        Connection con = DbConnectionFactory.getConnection();
        String versionTable=UtilMethods.getVersionInfoTableName(table);
        PreparedStatement selectContentlets = null;
        final int limit=1000;
        
        Logger.info(this, "creating (working) version info records for "+table+" on "+versionTable);
        
        if(DbConnectionFactory.isOracle()){
        	  selectContentlets = con.prepareStatement(
        	          "select * from ( "+
        	          "   select identifier,inode,live,locked,mod_date,mod_user,deleted, row_number() over (order by inode) rn "+ 
        	          "   from "+table+" where working = 1 "+ 
        	          ") where rn >= ? and rn < ? order by identifier asc, mod_date desc");
        }else if(DbConnectionFactory.isMsSql()){
        	 selectContentlets = con.prepareStatement(
        			 " SELECT TOP " + limit  + "  * FROM (SELECT identifier,inode,live,locked,mod_date,mod_user,deleted,ROW_NUMBER() "
	    		   + " OVER (order by mod_date) AS RowNumber FROM "+ table +" where working = "+DbConnectionFactory.getDBTrue()+") temp "
	    		   + " WHERE RowNumber > ? order by RowNumber, identifier asc, mod_date desc");

        }else{
        	 selectContentlets = con.prepareStatement(
                     "select identifier,inode,live,locked,mod_date,mod_user,deleted from "+ table +
                     " where working="+DbConnectionFactory.getDBTrue()+" order by identifier asc, mod_date desc limit ? offset ? ");
        }

        PreparedStatement insertVersionInfo=con.prepareStatement(
                "insert into "+versionTable+"(identifier,working_inode,locked_on,locked_by,deleted,live_inode) "+
                " values (?,?,?,?,?,?)");


        int offset=0;
        boolean notDone=true;
        String lastId="";
        do {
        	if(DbConnectionFactory.isMsSql()){
        		selectContentlets.setInt(1, offset);
        	} else if(DbConnectionFactory.isOracle()) {
        	    selectContentlets.setInt(1, offset);
        	    selectContentlets.setInt(2, offset+limit);
        	} else{
        		selectContentlets.setInt(1, limit);
                selectContentlets.setInt(2, offset);
        	}
            offset=offset+limit;

            notDone=false;
            ResultSet rs=selectContentlets.executeQuery();
            while(rs.next()) {
                notDone=true;
                final String inode=rs.getString("inode");
                final String identifier=rs.getString("identifier");
                if(UtilMethods.isSet(identifier) && !identifier.equals(lastId)) {
                    lastId=identifier;
                    insertVersionInfo.setString(1, identifier);
                    insertVersionInfo.setString(2, inode);
                    boolean locked=rs.getBoolean("locked");
                    if(locked) {
                        insertVersionInfo.setDate(3, rs.getDate("mod_date"));
                        insertVersionInfo.setString(4, rs.getString("mod_user"));
                    }
                    else {
                        insertVersionInfo.setDate(3, new Date(System.currentTimeMillis()));
                        insertVersionInfo.setNull(4, Types.VARCHAR);
                    }
    
                    insertVersionInfo.setBoolean(5, rs.getBoolean("deleted"));
    
                    boolean live=rs.getBoolean("live");
                    if(live)
                        insertVersionInfo.setString(6, inode);
                    else
                        insertVersionInfo.setNull(6, Types.VARCHAR);
    
                    insertVersionInfo.executeUpdate();
                }
            }
            rs.close();
            //insertVersionInfo.executeBatch();
        } while(notDone);

        selectContentlets.close();
        insertVersionInfo.close();
    }

    protected void associateLiveNotWorking(String table) throws DotDataException {
        DotConnect dc = new DotConnect();
        String versionTable=UtilMethods.getVersionInfoTableName(table);
        String lastId="";
        
        Logger.info(this, "creating (live not working) version info records for "+table+" on "+versionTable);
        
        String contentlets=
            "select identifier,inode, mod_date from " + table +
            " where working="+DbConnectionFactory.getDBFalse()+" and live="+DbConnectionFactory.getDBTrue()+
            " order by identifier asc, mod_date desc";
        dc.setSQL(contentlets);
        List<Map<String,Object>> results=dc.loadObjectResults();
        for(Map<String,Object> rr : results) {
            String identifier=(String)rr.get("identifier");
            String inode=(String)rr.get("inode");
            if(UtilMethods.isSet(identifier) && !identifier.equals(lastId)) {
                lastId=identifier;
                dc.setSQL("update "+versionTable+" set live_inode=? where identifier=?");
                dc.addParam(inode);
                dc.addParam(identifier);
                dc.loadResult();
            }
        }
    }

    protected void dropOldAttributes() throws DotDataException, SQLException {
        DotConnect dc = new DotConnect();
        if(DbConnectionFactory.isMsSql()){
        	dc.executeStatement("drop index idx_contentlet_1 on contentlet");
        	dc.executeStatement("drop index idx_contentlet_2 on contentlet");
        	dc.executeStatement("drop index idx_template4 on template");
        	dc.executeStatement("drop index idx_template5 on template");
        }
        dc.executeStatement(
            "alter table contentlet drop column live");
        dc.executeStatement(
            "alter table contentlet drop column working");
        dc.executeStatement(
            "alter table contentlet drop column deleted");
        dc.executeStatement(
            "alter table contentlet drop column locked");

        dc.executeStatement(
            "alter table containers drop column live");
        dc.executeStatement(
            "alter table containers drop column working");
        dc.executeStatement(
            "alter table containers drop column deleted");
        dc.executeStatement(
            "alter table containers drop column locked");

        dc.executeStatement(
            "alter table template drop column live");
        dc.executeStatement(
            "alter table template drop column working");
        dc.executeStatement(
            "alter table template drop column deleted");
        dc.executeStatement(
            "alter table template drop column locked");

        dc.executeStatement(
            "alter table htmlpage drop column live");
        dc.executeStatement(
            "alter table htmlpage drop column working");
        dc.executeStatement(
            "alter table htmlpage drop column deleted");
        dc.executeStatement(
            "alter table htmlpage drop column locked");

        dc.executeStatement(
            "alter table file_asset drop column live");
        dc.executeStatement(
            "alter table file_asset drop column working");
        dc.executeStatement(
            "alter table file_asset drop column deleted");
        dc.executeStatement(
            "alter table file_asset drop column locked");

        dc.executeStatement(
            "alter table links drop column live");
        dc.executeStatement(
            "alter table links drop column working");
        dc.executeStatement(
            "alter table links drop column deleted");
        dc.executeStatement(
            "alter table links drop column locked");
    }

    protected void associateContentlets() throws DotDataException {
        DotConnect dc = new DotConnect();
        final int limit=1000;
        
        Logger.info(this,"creating version_info records for contentlets");
        
        // first working and (optionally) live
        String contentlets;
        if(DbConnectionFactory.isOracle()){
            contentlets = 
                  "select * from ( "+
                  "  select identifier,inode,live,locked,mod_user,mod_date,deleted,language_id,row_number() over (order by inode) rn "+ 
                  "  from contentlet where working = 1 "+ 
                  ") where rn >= ? and rn < ? order by identifier asc, language_id asc, mod_date desc";
        }else if(DbConnectionFactory.isMsSql()){
            contentlets =  " SELECT TOP "+ limit + " *  FROM (SELECT identifier,inode,live,locked,mod_user,mod_date,deleted,language_id,ROW_NUMBER() "
                         + " OVER (order by mod_date) AS RowNumber FROM contentlet where working="+DbConnectionFactory.getDBTrue()
                         + ") temp WHERE RowNumber > ? order by RowNumber, identifier asc, language_id asc, mod_date desc";
        }else{
            contentlets = "select identifier,inode,live,locked,mod_user,mod_date,deleted,language_id from contentlet "
                       +  " where working="+DbConnectionFactory.getDBTrue()+" order by identifier asc, language_id asc, mod_date desc limit ? offset ? ";
        }
        
        int offset=0;
        boolean notDone;
        String lastId="";
        long lastLang=-1;
        do {

            dc.setSQL(contentlets);
            if(DbConnectionFactory.isMsSql()){
                dc.addParam(offset);
            } else if(DbConnectionFactory.isOracle()) {
                dc.addParam(offset);
                dc.addParam(offset+limit);
            } else{
            	dc.addParam(limit);
                dc.addParam(offset);
            }
            offset=offset+limit;

            List<Map<String,Object>> results=dc.loadObjectResults();
            notDone=results.size()>0;

            for(Map<String,Object> rr : results) {
                String identifier=(String)rr.get("identifier");
                String inode=(String)rr.get("inode");
                long langId=Long.parseLong(rr.get("language_id").toString());
                
                if(UtilMethods.isSet(identifier) && !(langId==lastLang && identifier.equals(lastId)) ) {
                    lastLang=langId;
                    lastId=identifier;
                    
                    boolean live = false;
                    boolean locked = false;
                    boolean deleted = false;
    
                    String liveStr=rr.get("live").toString().trim();
                    String lockedStr=rr.get("locked").toString().trim();
                    String deletedStr=rr.get("deleted").toString().trim();
                    
                    if(liveStr.equalsIgnoreCase("true") || liveStr.equalsIgnoreCase("false"))
                        live = Boolean.parseBoolean(liveStr);
                    else if(liveStr.equals("1") || liveStr.equals("0"))
                        live = Integer.parseInt(liveStr)==1;
                    
                    if(lockedStr.equalsIgnoreCase("true") || lockedStr.equalsIgnoreCase("false"))
                        locked = Boolean.parseBoolean(lockedStr);
                    else if(lockedStr.equals("1") || lockedStr.equals("0"))
                        locked = Integer.parseInt(lockedStr)==1;
                    
                    if(deletedStr.equalsIgnoreCase("true") || deletedStr.equalsIgnoreCase("false"))
                        deleted = Boolean.parseBoolean(deletedStr);
                    else if(deletedStr.equals("1") || deletedStr.equals("0"))
                        deleted = Integer.parseInt(deletedStr)==1;
                    
                    String mod_user=(String)rr.get("mod_user");
                    java.util.Date mod_date=(java.util.Date)rr.get("mod_date");
                    String insert="";
    
                	insert="insert into contentlet_version_info(identifier,locked_on,locked_by,deleted,lang,working_inode"+(live?",live_inode":"")+") values " +
                                                             "(?,?,?,?,?,?"+(live?",?":"")+")";
                    dc.setSQL(insert);
                    dc.addParam(identifier.trim());
                    if(locked) {
                        dc.addParam(mod_date);
                        dc.addParam(mod_user);
                    }
                    else {
                        dc.addParam(new java.util.Date());
                        dc.addObject(null);
                    }
                    dc.addParam(deleted);
                    dc.addParam(langId);
                    dc.addParam(inode);
                    if(live)
                        dc.addParam(inode);
                    
                    try {
                    	dc.loadResult();
                    } catch (DotDataException e) {
                    	e.printStackTrace();
                    }
                }
            }
        } while(notDone);

        // now live not working
        if(DbConnectionFactory.isOracle()){
            contentlets = 
                  "select * from ( "+
                  "  select identifier,inode,language_id,mod_date,row_number() over (order by inode) rn "+ 
                  "  from contentlet where working = 0 and live = 1 "+
                  ") where rn >= ? and rn < ? order by identifier asc, language_id asc, mod_date desc";
        }else if(DbConnectionFactory.isMsSql()){
            contentlets =  " SELECT TOP " + limit + " * FROM (SELECT identifier,inode,language_id,mod_date,ROW_NUMBER() "
                         + " OVER (order by mod_date) AS RowNumber FROM contentlet where working = "+DbConnectionFactory.getDBFalse()
                         + " and live ="+ DbConnectionFactory.getDBTrue()+ ") temp WHERE RowNumber > ? order by RowNumber, identifier asc, language_id asc, mod_date desc";
        }else{
            contentlets = "select identifier,inode,language_id from contentlet "
                        + " where working="+DbConnectionFactory.getDBFalse()+" and live="+DbConnectionFactory.getDBTrue()
                        + " order by identifier asc, language_id asc, mod_date desc limit ? offset ? ";
        }
        lastId="";
        lastLang=-1;
        offset=0;
        do {

            dc.setSQL(contentlets);
            if(DbConnectionFactory.isMsSql()){
            	dc.addParam(offset);
            }else if(DbConnectionFactory.isOracle()) {
                dc.addParam(offset);
                dc.addParam(offset+limit);
            }else{
                dc.addParam(limit);
                dc.addParam(offset);
            }
            offset=offset+limit;
            List<Map<String,Object>> results=dc.loadObjectResults();
            notDone=results.size()>0;
            for(Map<String,Object> rr : results) {
                String identifier=(String)rr.get("identifier");
                String inode=(String)rr.get("inode");
                long langId=Long.parseLong(rr.get("language_id").toString());
                if(UtilMethods.isSet(identifier) && !(langId==lastLang && identifier.equals(lastId))) {
                    lastId=identifier;
                    lastLang=langId;
                    
                    dc.setSQL("update contentlet_version_info set live_inode=? where identifier=? and lang=?");
                    dc.addParam(inode);
                    dc.addParam(identifier);
                    dc.addParam(langId);
                    dc.loadResult();
                }
            }
        } while(notDone);
    }

    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(true);
            dropOldTriggers();
            if(DbConnectionFactory.isOracle()){
            	createNewTablesForOracle();
            }else if(DbConnectionFactory.isMsSql()){
            	createNewTablesForSQLServer();
            }else{
            	createNewTables();
            }
            addNewForeignKeys();
            String[] tables=new String[] {"containers","links","template","file_asset","htmlpage"};
            for(String tt : tables) {
                associateWorking(tt);
                associateLiveNotWorking(tt);
            }
            associateContentlets();
            dropOldAttributes();
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(),e);
        }
        finally {
            try {
                DbConnectionFactory.getConnection().setAutoCommit(false);
            } catch (SQLException e) {
                Logger.warn(this, e.getMessage(), e);
            }
        }
    }

    public boolean forceRun() {
        return true;
    }

}
