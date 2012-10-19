package com.dotmarketing.portlets.dashboard.business;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;

public abstract class DashboardDataGenerator {
	
	protected String getWorkstreamQuery(){   
	return " inode, asset_type, mod_user_id, host_id, mod_date,case when deleted = 1 then 'Deleted' else case when live = 1 then 'Published' else 'Saved' end end as action, name from( "+ 
	" select contentlet.inode as inode, 'contentlet' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
	"        case when contentlet.inode=cinfo.live_inode then 1 else 0 end as live, case when contentlet.inode=cinfo.working_inode then 1 else 0 end as working, " +
	"        cinfo.deleted as deleted, contentlet.title as name "+ 
	" from contentlet join identifier identifier on identifier.id = contentlet.identifier join contentlet_version_info cinfo on cinfo.identifier=contentlet.identifier "+ 
	" UNION  "+ 
	" select htmlpage.inode as inode, 'htmlpage' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
	"        case when htmlpage.inode=hinfo.live_inode then 1 else 0 end as live, case when htmlpage.inode=hinfo.working_inode then 1 else 0 end as working, hinfo.deleted as deleted, " +
	((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+  
		" from htmlpage join identifier identifier on identifier.id = htmlpage.identifier join htmlpage_version_info hinfo on hinfo.identifier=htmlpage.identifier "+ 
		" UNION "+ 
		" select template.inode as inode, 'template' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
		"        case when template.inode=tinfo.live_inode then 1 else 0 end as live, case when template.inode=tinfo.live_inode then 1 else 0 end as working, tinfo.deleted, template.title as name "+ 
		" from template join identifier identifier on identifier.id = template.identifier join template_version_info tinfo on tinfo.identifier=template.identifier "+ 
		" UNION "+ 
		" select file_asset.inode as inode, 'file_asset' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
		"        case when file_asset.inode=finfo.live_inode then 1 else 0 end as live, case when file_asset.inode=finfo.live_inode then 1 else 0 end as working, finfo.deleted, "+
		((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
			(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+ 
			" from file_asset join identifier identifier on identifier.id = file_asset.identifier join fileasset_version_info finfo on finfo.identifier=file_asset.identifier "+ 
			" UNION  "+ 
			" select containers.inode as inode, 'container' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
			"        case when containers.inode=ccinfo.live_inode then 1 else 0 end as live, case when containers.inode=ccinfo.live_inode then 1 else 0 end as working, ccinfo.deleted, containers.title as name "+ 
			" from containers join identifier identifier on identifier.id = containers.identifier join container_version_info ccinfo on ccinfo.identifier=containers.identifier "+ 
			" UNION "+ 
			" select links.inode as inode, 'link' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, " +
			"        case when links.inode=linfo.live_inode then 1 else 0 end as live, case when links.inode=linfo.live_inode then 1 else 0 end as working, linfo.deleted, links.title as name "+ 
			" from links join identifier identifier on identifier.id = links.identifier join link_version_info linfo on linfo.identifier=links.identifier "+ 
			" )assets where mod_date>(select coalesce(max(mod_date),"
			+(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)?"'1970-01-01 00:00:00')"
					:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))?"TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))"
							:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?"STR_TO_DATE('1970-01-01','%Y-%m-%d'))"
									:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))?"CAST('1970-01-01' AS DATETIME))":"")+
									" from analytic_summary_workstream) order by assets.mod_date,assets.name asc ";
	}
	
	protected String getSummary404Query() {
		return "select " +((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as uri "+  
		" from htmlpage join identifier identifier on identifier.id = htmlpage.identifier "+   
		" where identifier.host_inode = ? "+
		" UNION ALL  "+
		" select " +((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
			(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as uri  "+
			" from file_asset join identifier identifier on identifier.id = file_asset.identifier "+
			" where identifier.host_inode = ? ";
	}
	
	protected String getPagesQuery() {
		return " select " +((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
		(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as uri , vinfo.live_inode as inode "+
		" from identifier join htmlpage_version_info vinfo on vinfo.identifier = identifier.id "+  
		" where identifier.host_inode = ? and vinfo.live_inode is not null ";
	}
	
	protected String getContentQuery(){
	return "select contentlet.identifier as inode, contentlet.title as title "+
	" from contentlet join identifier identifier on identifier.id = contentlet.identifier " +
	" join contentlet_version_info vinfo on vinfo.live_inode=contentlet.inode "+
	" where identifier.host_inode = ? ";
	}
	
	public abstract void setFlag(boolean flag);//DOTCMS-5511

	public abstract boolean isFinished();

	public abstract double getProgress();

	public abstract List<String> getErrors();

	public abstract long getRowCount();

	public abstract int getMonthFrom();

	public abstract int getYearFrom();

	public abstract int getMonthTo();

	public abstract int getYearTo();
	
	public abstract void start();

}
