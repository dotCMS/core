package com.dotmarketing.portlets.dashboard.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary;
import com.dotmarketing.portlets.dashboard.model.DashboardSummary404;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryContent;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryPage;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryReferer;
import com.dotmarketing.portlets.dashboard.model.DashboardSummaryVisits;
import com.dotmarketing.portlets.dashboard.model.DashboardWorkStream;
import com.dotmarketing.portlets.dashboard.model.TopAsset;
import com.dotmarketing.portlets.dashboard.model.ViewType;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;



public abstract class DashboardFactory {
	
    protected String getSummaryPagesQuery(){
    	
    	return (DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))?
			" select count(*) as hits, htmlpage.inode as inode, identifier.parent_path || identifier.asset_name as uri " +
			" from clickstream_request join identifier on identifier.id = associated_identifier "+
			" join htmlpage on htmlpage.identifier = identifier.id  where extract(day from timestampper) = ? "+
			" and extract(month from timestampper) = ? and extract(year from timestampper) = ? "+
			" and host_id = ? group by associated_identifier, identifier.parent_path || identifier.asset_name ,htmlpage.inode "
			:DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)?
					" select count(*) as hits, htmlpage.inode as inode, CONCAT(identifier.parent_path,identifier.asset_name) as uri " +
					" from clickstream_request join identifier on identifier.id = associated_identifier "+
					" join htmlpage on htmlpage.identifier = identifier.id where DAY(timestampper) = ? "+
					" and MONTH(timestampper) = ? and YEAR(timestampper) = ? and host_id = ?"+
					" group by associated_identifier, CONCAT(identifier.parent_path,identifier.asset_name) ,htmlpage.inode "
					:DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
							" select count(*) as hits, htmlpage.inode as inode,(identifier.parent_path + identifier.asset_name) as uri " +
							" from clickstream_request join identifier on identifier.id = associated_identifier "+
							" join htmlpage on htmlpage.identifier = identifier.id where DATEPART(day, timestampper) = ? "+
							" and DATEPART(month, timestampper) = ? and DATEPART(year, timestampper) = ? and host_id = ?"+
							" group by associated_identifier, (identifier.parent_path + identifier.asset_name) ,htmlpage.inode ":"";
    };

	protected String getSummaryContentQuery(){
		return (DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))?
			" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
			" join identifier on identifier.id = associated_identifier join contentlet on contentlet.identifier = identifier.id  "+
			" where extract(day from timestampper) = ? and extract(month from timestampper) = ? and "+
			" extract(year from timestampper) = ? and host_id = ?"+
			" group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title "
			:DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)?
					" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
					" join identifier on identifier.id = associated_identifier join contentlet on contentlet.identifier = identifier.id  "+
					" where DAY(timestampper) = ? and MONTH(timestampper) = ? and YEAR(timestampper) = ? "+
					" and host_id = ? group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title "
					:DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?
							" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
							" join identifier on identifier.id = associated_identifier join contentlet on contentlet.identifier = identifier.id  "+
							" where DATEPART(day, timestampper) = ? and DATEPART(month, timestampper) = ? and DATEPART(year, timestampper) = ? "+
							" and host_id = ? group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title ":"";
	}
	
	protected String getWorkstreamQuery(String hostId){  
		return  " inode, asset_type, mod_user_id, host_id, mod_date,case when deleted = 1 then 'Deleted' else case when live_inode IS NOT NULL then 'Published' else 'Saved' end end as action, name from( "+ 
		" select contentlet.inode as inode, 'contentlet' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date,lang_info.live_inode,lang_info.working_inode,lang_info.deleted, coalesce(contentlet.title,contentlet.identifier) as name "+ 
		" from contentlet_version_info lang_info,contentlet join identifier identifier on identifier.id = contentlet.identifier where " +
		" contentlet.identifier = lang_info.identifier "+ 
		" UNION ALL "+ 
		" select htmlpage.inode as inode, 'htmlpage' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, page_info.live_inode, page_info.working_inode, page_info.deleted, " +
		((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
			(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+  
			" from htmlpage_version_info page_info,htmlpage join identifier identifier on identifier.id = htmlpage.identifier where htmlpage.identifier = page_info.identifier "+ 
			" UNION ALL "+ 
			" select template.inode as inode, 'template' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, temp_info.live_inode,temp_info.working_inode,temp_info.deleted, coalesce(template.title,template.identifier) as name "+ 
			" from template_version_info temp_info,template join identifier identifier on identifier.id = template.identifier where template.identifier = temp_info.identifier "+ 
			" UNION ALL "+ 
			" select file_asset.inode as inode, 'file_asset' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, file_info.live_inode,file_info.working_inode,file_info.deleted, "+
			((DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)||(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)))?"identifier.parent_path || identifier.asset_name ":
				(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+ 
				" from fileasset_version_info file_info,file_asset join identifier identifier on identifier.id = file_asset.identifier where file_asset.identifier = file_info.identifier"+ 
				" UNION ALL "+ 
				" select containers.inode as inode, 'container' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, con_info.live_inode,con_info.working_inode,con_info.deleted, coalesce(containers.title,containers.identifier) as name "+ 
				" from container_version_info con_info,containers join identifier identifier on identifier.id = containers.identifier where containers.identifier = con_info.identifier "+ 
				" UNION ALL "+ 
				" select links.inode as inode, 'link' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, links_info.live_inode,links_info.working_inode,links_info.deleted, coalesce(links.title,links.identifier) as name "+ 
				" from link_version_info links_info,links join identifier identifier on identifier.id = links.identifier where links_info.identifier= links.identifier "+ 
				" )assets where mod_date>(select coalesce(max(mod_date),"
				+(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)?"'1970-01-01 00:00:00')"
						:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE))?"TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))"
								:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL))?"STR_TO_DATE('1970-01-01','%Y-%m-%d'))"
										:(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL))?"CAST('1970-01-01' AS DATETIME))":"")+
										" from analytic_summary_workstream) and host_id = '"+hostId+"' order by assets.mod_date,assets.name asc ";
	}

	
	protected String getTopAssetsQuery() {
		return "select identifier.host_inode as host_inode,count(htmlpage.inode) as count, 'htmlpage' as asset_type " 
		    + "from htmlpage_version_info pageinfo,identifier "
			+ "join htmlpage on htmlpage.identifier = identifier.id  "
			+ "where htmlpage.identifier = pageinfo.identifier and identifier.host_inode = ? "
			+ "and pageinfo.live_inode is not null "
			+ "group by identifier.host_inode "
			+ "UNION ALL "
			+ "select identifier.host_inode as host_inode,count(file_asset.inode) as count, 'file_asset' as asset_type " 
			+ "from fileasset_version_info fileinfo,identifier "
			+ "join file_asset on file_asset.identifier = identifier.id  "
			+ "where file_asset.identifier = fileinfo.identifier and identifier.host_inode = ? "
			+ "and fileinfo.live_inode is not null "
			+ "group by identifier.host_inode "
			+ "UNION ALL  "
			+ "select identifier.host_inode as host_inode,count(contentlet.inode) as count, 'contentlet' as asset_type " 
			+ "from contentlet_version_info contentinfo,identifier "
			+ "join contentlet on contentlet.identifier = identifier.id "
			+ "where contentlet.identifier = contentinfo.identifier and identifier.host_inode = ? "
			+ "and contentinfo.live_inode is not null "
			+ "group by identifier.host_inode";
	}
	
	protected String  getIdentifierColumn(){
		return "contentlet.identifier";
	}
	
	protected String getWorkstreamListQuery(){
		return "select distinct {analytic_summary_workstream.*}, user_.firstname as username, contentlet.title as hostname " +
		" from analytic_summary_workstream, user_ , contentlet,contentlet_version_info contentinfo " +
		" where user_.userid = analytic_summary_workstream.mod_user_id and contentlet.identifier = analytic_summary_workstream.host_id " +
		" and contentlet.identifier = contentinfo.identifier and contentinfo.live_inode is not null and analytic_summary_workstream.name is not null ";
	}
	
	protected String getWorkstreamCountQuery(){
		return "select count(distinct analytic_summary_workstream.id) as summaryCount from analytic_summary_workstream, user_, contentlet,contentlet_version_info info where" +
		  " user_.userid = analytic_summary_workstream.mod_user_id and contentlet.identifier = analytic_summary_workstream.host_id " +
		  " and contentlet.identifier = info.identifier and info.live_inode is not null and analytic_summary_workstream.name is not null "; 
	}
	
	
	protected StringBuffer getHostListQuery(boolean hasCategory, String selectedCategories,  String runDashboardFieldContentlet){
		StringBuffer query = new StringBuffer();
		query.append("select "+ (DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?"":" distinct ")+" {contentlet.*}, " +
				"coalesce(d.page_views,0) as totalpageviews,  " +
				"CASE WHEN contentinfo.live_inode is not null THEN 'Live' "+
                " ELSE 'Stopped' "+
                "END AS status "+
				"from contentlet_version_info contentinfo,inode contentlet_1_ , contentlet "+
				"left join " +
				"(" +
				  "select sum(page_views) as page_views, host_id from analytic_summary join "+
				  "analytic_summary_period on analytic_summary.summary_period_id = analytic_summary_period.id "+
				  "and analytic_summary_period.full_date > ? and analytic_summary_period.full_date < ? "+
				  "group by host_id" +
				") "+ (DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE) || DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)?"":" as d") +" on d.host_id = contentlet.identifier " +
				(hasCategory?" join tree on tree.child = contentlet.inode and tree.parent in("+selectedCategories+") ":"") +
				"join structure s on contentlet.structure_inode = s.inode " +
				"where contentlet_1_.type = 'contentlet' and contentlet.inode = contentlet_1_.inode and s.name ='Host' and contentlet.identifier = contentinfo.identifier "+ 
		        "and contentlet.title <> 'System Host' and contentinfo.working_inode = contentlet.inode "+ (UtilMethods.isSet(runDashboardFieldContentlet)?" and contentlet."+runDashboardFieldContentlet+"= "+ DbConnectionFactory.getDBTrue()+"":"")+ " ");
		return query;
	}
	
	protected StringBuffer getHostListCountQuery(boolean hasCategory, String selectedCategories,  String runDashboardFieldContentlet){
		StringBuffer query = new StringBuffer();
		query.append("select count(distinct contentlet.inode) as total " +
				"from contentlet_version_info contentinfo,inode contentlet_1_ , contentlet "+
				(hasCategory?" join tree on tree.child = contentlet.inode and tree.parent in("+selectedCategories+") ":"") +
				"join structure s on contentlet.structure_inode = s.inode " +
				"where contentlet_1_.type = 'contentlet' and contentlet.inode = contentlet_1_.inode and s.name ='Host' "+ 
		        "and contentlet.title <> 'System Host' and contentlet.identifier = contentinfo.identifier and contentinfo.working_inode = contentlet.inode "  +
		        (UtilMethods.isSet(runDashboardFieldContentlet)?" and contentlet."+runDashboardFieldContentlet+"= "+ DbConnectionFactory.getDBTrue()+"":"")+ " ");
		return query;
	}
	
	protected String getHostQueryForClickstream(String runDashboardFieldContentlet){
		String query = " select contentlet.identifier as host_id from contentlet, structure s,contentlet_version_info info "+
		" where contentlet.structure_inode = s.inode and s.name ='Host' and contentlet.identifier = info.identifier "+
	    " and contentlet.title <> 'System Host' and info.working_inode = contentlet.inode " + (UtilMethods.isSet(runDashboardFieldContentlet)?" and contentlet."+runDashboardFieldContentlet+"= "+ DbConnectionFactory.getDBTrue()+"":"")+
	    " group by contentlet.identifier "; 
		return query;
	}
	
	/**
	 * 
	 * @param user
	 * @param includeArchived
	 * @param params
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<Host> getHostList(User user, boolean includeArchived, Map<String, Object> params, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param user
	 * @param includeArchived
	 * @param params
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public long getHostListCount(User user, boolean includeArchived, Map<String, Object> params) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param user
	 * @param hostId
	 * @param userId
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<DashboardWorkStream> getWorkStreamList(User user, String hostId, String userId, Date fromDate, Date toDate, int limit, int offset, String sortBy)throws DotDataException,DotHibernateException;
	
	/**
	 * 
	 * @param user
	 * @param hostId
	 * @param userId
	 * @param fromDate
	 * @param toDate
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public long getWorkStreamListCount(User user, String hostId, String userId, Date fromDate, Date toDate)throws DotDataException,DotHibernateException;
	
	/**
	 * 
	 * @param hostId
	 * @param fromDate
	 * @param toDate
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public DashboardSummary getDashboardSummary(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
    /**
     * 
     * @param hostId
     * @param viewType
     * @param fromDate
     * @param toDate
     * @return
     * @throws DotDataException
     * @throws DotHibernateException
     */
	abstract public List<DashboardSummaryVisits> getDashboardSummaryVisits(String hostId, ViewType viewType, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
	
	/**
	 * 
	 * @param hostId
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<DashboardSummaryReferer> getTopReferers(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
	
	
	/**
	 * 
	 * @param hostId
	 * @param fromDate
	 * @param toDate
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public long getTopReferersCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param hostId
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<DashboardSummaryPage> getTopPages(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
	
    /**
     * 
     * @param hostId
     * @param fromDate
     * @param toDate
     * @return
     * @throws DotDataException
     * @throws DotHibernateException
     */
	abstract public long getTopPagesCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param hostId
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<DashboardSummaryContent> getTopContent(String hostId, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
	
    /**
     * 
     * @param hostId
     * @param fromDate
     * @param toDate
     * @return
     * @throws DotDataException
     * @throws DotHibernateException
     */
	abstract public long getTopContentCount(String hostId, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param user
	 * @param hostId
	 * @param showIgnored
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @param offset
	 * @param sortBy
	 * @return
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public List<DashboardSummary404> get404s(String userId, String hostId, boolean showIgnored, Date fromDate, Date toDate, int limit, int offset, String sortBy) throws DotDataException, DotHibernateException;
	
	
    /**
     * 
     * @param user
     * @param hostId
     * @param showIgnored
     * @param fromDate
     * @param toDate
     * @return
     * @throws DotDataException
     * @throws DotHibernateException
     */
	abstract public long get404Count(String userId, String hostId, boolean showIgnored, Date fromDate, Date toDate) throws DotDataException, DotHibernateException;
	
	/**
	 * 
	 * @param user
	 * @param id
	 * @param ignored
	 * @throws DotDataException
	 * @throws DotHibernateException
	 */
	abstract public void setIgnored(User user, long id, boolean ignored) throws DotDataException, DotHibernateException;
	

	/**
	 * 
	 * @param user
	 * @param hostId
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException 
	 */
	abstract public List<TopAsset> getTopAssets(User user,String hostId) throws DotDataException;
	
	/**
	 * 
	 */
	abstract public void populateAnalyticSummaryTables();
	

	
	/**
	 * 
	 * @param month
	 * @param year
	 * @return
	 */
	abstract public int checkPeriodData(int month, int year);
	
}
