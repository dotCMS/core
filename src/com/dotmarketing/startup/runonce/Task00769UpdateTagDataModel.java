package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.startup.AbstractJDBCStartupTask;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

public class Task00769UpdateTagDataModel extends AbstractJDBCStartupTask implements StartupTask  {

	public Task00769UpdateTagDataModel() {
		setRebuildForeignKeys(false);
		setRebuildIndices(false);
		setRebuildPrimaryKeys(false);
	}

	public boolean forceRun() {
		return true;
	}

	private String query1 = "ALTER TABLE tag ADD tag_id varchar(100);";
	private String query2 = "ALTER TABLE tag_inode add tag_id varchar(100);";
	private String query3 = "ALTER TABLE tag ADD host_id varchar(255);";
	private String query4 = "UPDATE tag set host_id = '"+Host.SYSTEM_HOST+"' ;";
	private String query5 = "DELETE FROM tag_inode where tagname not in (select tagname from tag);" ;
	private String query6 ="DELETE FROM tag where tagname not in (select tagname from tag_inode);";
	private String queryPRE8MSSQL = "alter table tag alter column tag_id varchar(100) not null;";
	private String query8 = "alter table tag add constraint tag_pkey primary key (tag_id);";
	private String query8MySQL ="ALTER TABLE tag ADD PRIMARY KEY (tag_id);";
	private String queryPRE10MSSQL = "alter table tag_inode alter column tag_id varchar(100) not null; alter table tag_inode alter column inode varchar(100) not null;";
	private String query10 = "alter table tag_inode add constraint pk_tag_inode primary key (tag_id, inode);";
	private String query10MySQL = "alter table tag_inode ADD PRIMARY KEY (tag_id, inode);";
	private String query11 = "alter table tag_inode drop column tagname;";
	private String query12 = "alter table tag_inode add constraint fk_tag_inode_tagid foreign key (tag_id) references tag (tag_id);";
	private String pullTagNames = "SELECT tagname as tagname from tag";
	private String uniqueKeyTag = "alter table tag add constraint tag_tagname_host unique (tagname, host_id);";
	private String findLayouts = "select id, layout_name from cms_layout";


	@SuppressWarnings("unchecked")
	private String getUpdateTagsId() {

		StringBuilder sb = new StringBuilder("");

		try {
			DotConnect dc = new DotConnect();
			dc.executeStatement("UPDATE tag set tagname = replace(tagname,'\\\\r',' ')");
			try {
			dc.executeStatement("UPDATE tag_inode set tagname = replace(tagname,'\\\\r',' ')");
			} catch (Exception e) {
				e.printStackTrace();
			}
			//update tag and tag_inode ids
			dc.setSQL(pullTagNames);
			List<HashMap<String,String>> tagNames = dc.loadResults();
			if(UtilMethods.isSet(tagNames)){
				for(HashMap<String,String> tagName : tagNames){
					String tag = tagName.get("tagname").toString();
					String uuid = UUIDGenerator.generateUuid();

					tag = tag.replace("'", "''");

					if(DbConnectionFactory.isMySql() || DbConnectionFactory.isPostgres()) {
						tag = tag.replace("\\", "\\\\");
					}

					if(DbConnectionFactory.isPostgres()) {
						sb.append("UPDATE tag set tag_id = '").append(uuid).append("' where tagname = E'").append(tag).append("'; ");
						sb.append("UPDATE tag_inode set tag_id = '").append(uuid).append("' where tagname in(select tagname from tag where tagname = E'").append(tag).append("'); ");
					} else {
						sb.append("UPDATE tag set tag_id = '").append(uuid).append("' where tagname = '").append(tag).append("'; ");
						sb.append("UPDATE tag_inode set tag_id = '").append(uuid).append("' where tagname in(select tagname from tag where tagname = '").append(tag).append("'); ");
					}
				}
			}

		} catch(Exception e) {
		    throw new RuntimeException(e);
		}

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private String getInsertLayouts() {

		StringBuilder sb = new StringBuilder("");

		try {

			DotConnect dc = new DotConnect();
			dc.setSQL(findLayouts);
			List<HashMap<String,String>>  layouts = dc.loadResults();

			if(UtilMethods.isSet(layouts)){
				for(HashMap<String,String> layout : layouts){
					String layout_name = layout.get("layout_name").toString();
					if(layout_name.contains("CMS Admin")||layout_name.equals("CMS_Admin")){

						sb.append("insert into cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) ");
						sb.append(" values('").append(UUIDGenerator.generateUuid()).append("', ");
						sb.append(" '").append(layout.get("id")).append("', ");
						sb.append(" UPPER('").append("EXT_TAG_MANAGER").append("'), ");
						sb.append(" '").append(11).append("') ; ");
					}
				}
				MaintenanceUtil.flushCache();
				MaintenanceUtil.deleteMenuCache();

			}


		} catch(Exception e) {
			e.printStackTrace();
		}

		return sb.toString();
	}


	private String getSQLs() {
	    try {
            HibernateUtil.startTransaction();
        } catch (DotHibernateException e2) {
            throw new RuntimeException(e2);
        }
	    
		StringBuilder sb = new StringBuilder();
		sb.append(query1);
		sb.append(query2);
		sb.append(query3);
		sb.append(query4);
		sb.append(query5);
		sb.append(query6);
		sb.append(getUpdateTagsId());

		if(DbConnectionFactory.isMsSql()) {
			sb.append(queryPRE8MSSQL);
		}

		sb.append(DbConnectionFactory.isMySql()?query8MySQL:query8);

		sb.append(uniqueKeyTag);

		if(DbConnectionFactory.isMsSql()) {
			sb.append(queryPRE10MSSQL);
		}

		sb.append(DbConnectionFactory.isMySql()?query10MySQL:query10);

		sb.append(query11);
		sb.append(query12);
		sb.append(getInsertLayouts());
		
		try {
		    // we  need to commit to release locks in
		    // the threadlocal connection
            HibernateUtil.commitTransaction();
        } catch (DotHibernateException e) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {}
            throw new RuntimeException(e);
        }
		
		return sb.toString();
	}

	@Override
	public String getPostgresScript() {
		return getSQLs();
	}

	@Override
	public String getMySQLScript() {
		return getSQLs();
	}

	@Override
	public String getOracleScript() {
		return getSQLs();
	}

	@Override
	public String getMSSQLScript() {
		return getSQLs();
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		List<String> tables = new ArrayList<String>();
		tables.add("tag");
		tables.add("tag_inode");
		return tables;
	}
}
