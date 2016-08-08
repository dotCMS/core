package com.dotmarketing.portlets.dashboard.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
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
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;



public abstract class DashboardFactory {

    protected String getSummaryPagesQuery(){
    	StringBuilder queryBuilder = new StringBuilder("");
    	
    	if(DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle() || DbConnectionFactory.isH2()) {
			//Find legacy html pages
			queryBuilder.append("SELECT COUNT(*) AS hits, htmlpage_version_info.live_inode AS inode, ")
			.append("(identifier.parent_path || identifier.asset_name) AS uri ")
			.append("FROM clickstream_request cr ")
			.append("JOIN identifier ON (identifier.id = cr.associated_identifier) ")
			.append("JOIN htmlpage_version_info ON (htmlpage_version_info.identifier = identifier.id) ")
			.append("WHERE EXTRACT(DAY FROM cr.timestampper) = ? AND EXTRACT(MONTH FROM cr.timestampper) = ? AND EXTRACT(YEAR FROM cr.timestampper) = ? AND cr.host_id = ? ")
			.append("GROUP BY associated_identifier, (identifier.parent_path || identifier.asset_name), htmlpage_version_info.live_inode")
			.append(" UNION ALL ")
			// Find contentlets type 'html page'
			.append("SELECT COUNT(*) AS hits, contentlet_version_info.live_inode AS inode, ")
			.append("(identifier.parent_path || identifier.asset_name) AS uri ")
			.append("FROM clickstream_request cr ")
			.append("JOIN contentlet ON (contentlet.identifier = cr.associated_identifier) ")
			.append("JOIN structure ON (structure.inode = contentlet.structure_inode) ")
			.append("JOIN identifier ON (identifier.id = contentlet.identifier) ")
			.append("JOIN contentlet_version_info ON (contentlet_version_info.identifier = identifier.id) ")
			.append("WHERE EXTRACT(DAY FROM cr.timestampper) = ? AND EXTRACT(MONTH FROM cr.timestampper) = ? AND EXTRACT(YEAR FROM cr.timestampper) = ? ")
			.append("AND cr.host_id = ? AND structure.structuretype = ").append(Structure.STRUCTURE_TYPE_HTMLPAGE).append(" ")
			.append("GROUP BY associated_identifier, (identifier.parent_path || identifier.asset_name), contentlet_version_info.live_inode");
    	}
    	else if(DbConnectionFactory.isMySql()) { // MySQL Query Builder
    		// Find legacy html pages
    		queryBuilder.append("SELECT COUNT(*) AS hits, htmlpage_version_info.live_inode AS inode, ")
    		.append("CONCAT(identifier.parent_path, identifier.asset_name) AS uri ")
    		.append("FROM clickstream_request cr ")
    		.append("JOIN identifier ON (identifier.id = cr.associated_identifier) ")
    		.append("JOIN htmlpage_version_info ON (htmlpage_version_info.identifier = identifier.id) ")
    		.append("WHERE DAY(cr.timestampper) = ? AND MONTH(cr.timestampper) = ? AND YEAR(cr.timestampper) = ? AND cr.host_id = ? ")
    		.append("GROUP BY associated_identifier, CONCAT(identifier.parent_path, identifier.asset_name), htmlpage_version_info.live_inode")
    		.append(" UNION ALL ")
    		// Find contentlets type 'html page'
    		.append("SELECT COUNT(*) AS hits, contentlet_version_info.live_inode AS inode, ")
    		.append("CONCAT(identifier.parent_path, identifier.asset_name) AS uri ")
    		.append("FROM clickstream_request cr ")
    		.append("JOIN contentlet ON (contentlet.identifier = cr.associated_identifier) ")
    		.append("JOIN structure ON (structure.inode = contentlet.structure_inode) ")
    		.append("JOIN identifier ON (identifier.id = contentlet.identifier) ")
    		.append("JOIN contentlet_version_info ON (contentlet_version_info.identifier = identifier.id) ")
    		.append("WHERE DAY(cr.timestampper) = ? AND MONTH(cr.timestampper) = ? AND YEAR(cr.timestampper) = ? ")
    		.append("AND cr.host_id = ? AND structure.structuretype = ").append(Structure.STRUCTURE_TYPE_HTMLPAGE).append(" ")
    		.append("GROUP BY associated_identifier, CONCAT(identifier.parent_path, identifier.asset_name), contentlet_version_info.live_inode");
    	} else if(DbConnectionFactory.isMsSql()) { // MsSQL Query Builder
    		//Find legacy html pages
			queryBuilder.append("SELECT COUNT(*) AS hits, htmlpage_version_info.live_inode AS inode, ")
			.append("(identifier.parent_path + identifier.asset_name) AS uri ")
			.append("FROM clickstream_request cr ")
			.append("JOIN identifier ON (identifier.id = cr.associated_identifier) ")
			.append("JOIN htmlpage_version_info ON (htmlpage_version_info.identifier = identifier.id) ")
			.append("WHERE DATEPART(DAY, cr.timestampper) = ? AND DATEPART(MONTH, cr.timestampper) = ? AND DATEPART(YEAR, cr.timestampper) = ? AND cr.host_id = ? ")
			.append("GROUP BY associated_identifier, (identifier.parent_path + identifier.asset_name), htmlpage_version_info.live_inode")
			.append(" UNION ALL ")
			// Find contentlets type 'html page'
			.append("SELECT COUNT(*) AS hits, contentlet_version_info.live_inode AS inode, ")
			.append("(identifier.parent_path + identifier.asset_name) AS uri ")
			.append("FROM clickstream_request cr ")
			.append("JOIN contentlet ON (contentlet.identifier = cr.associated_identifier) ")
			.append("JOIN structure ON (structure.inode = contentlet.structure_inode) ")
			.append("JOIN identifier ON (identifier.id = contentlet.identifier) ")
			.append("JOIN contentlet_version_info ON (contentlet_version_info.identifier = identifier.id) ")
			.append("WHERE DATEPART(DAY, cr.timestampper) = ? AND DATEPART(MONTH, cr.timestampper) = ? AND DATEPART(YEAR, cr.timestampper) = ? ")
			.append("AND cr.host_id = ? AND structure.structuretype = ").append(Structure.STRUCTURE_TYPE_HTMLPAGE).append(" ")
			.append("GROUP BY associated_identifier, (identifier.parent_path + identifier.asset_name), contentlet_version_info.live_inode");
    	}
    	
    	return queryBuilder.toString();
    };

	protected String getSummaryContentQuery(){
		return (DbConnectionFactory.isPostgres() || DbConnectionFactory.isOracle() || DbConnectionFactory.isH2()) ?
			" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
			" join identifier on identifier.id = associated_identifier join multi_tree on associated_identifier = parent1 join contentlet on contentlet.identifier = multi_tree.child  "+
			" where extract(day from timestampper) = ? and extract(month from timestampper) = ? and "+
			" extract(year from timestampper) = ? and host_id = ?"+
			" group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title "
			:DbConnectionFactory.isMySql() ?
					" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
					" join identifier on identifier.id = associated_identifier join multi_tree on associated_identifier = parent1 join contentlet on contentlet.identifier = multi_tree.child  "+
					" where DAY(timestampper) = ? and MONTH(timestampper) = ? and YEAR(timestampper) = ? "+
					" and host_id = ? group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title "
					:DbConnectionFactory.isMsSql() ?
							" select count(*) as hits, identifier.parent_path as uri ,contentlet.identifier as inode, contentlet.title as title  from clickstream_request "+
							" join identifier on identifier.id = associated_identifier join multi_tree on associated_identifier = parent1 join contentlet on contentlet.identifier = multi_tree.child  "+
							" where DATEPART(day, timestampper) = ? and DATEPART(month, timestampper) = ? and DATEPART(year, timestampper) = ? "+
							" and host_id = ? group by associated_identifier, identifier.parent_path,contentlet.identifier,contentlet.title ":"";
	}

	protected String getWorkstreamQuery(String hostId){
		return  " inode, asset_type, mod_user_id, host_id, mod_date,case when deleted = 1 then 'Deleted' else case when live_inode IS NOT NULL then 'Published' else 'Saved' end end as action, name from( "+
		" select contentlet.inode as inode, case when st.structuretype="+Structure.STRUCTURE_TYPE_FILEASSET+" then 'contentlet' else 'file_asset' end as asset_type, " +
		" mod_user as mod_user_id, identifier.host_inode as host_id, contentlet.mod_date,lang_info.live_inode,lang_info.working_inode,lang_info.deleted, coalesce(contentlet.title,contentlet.identifier) as name "+
		" from contentlet_version_info lang_info join contentlet on (contentlet.identifier = lang_info.identifier) join identifier identifier on (identifier.id = contentlet.identifier) "+
		" join structure st on (contentlet.structure_inode=st.inode) "+
		" UNION ALL "+
		" select htmlpage.inode as inode, 'htmlpage' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, page_info.live_inode, page_info.working_inode, page_info.deleted, " +
		((DbConnectionFactory.isPostgres()||(DbConnectionFactory.isOracle())||DbConnectionFactory.isH2())?"identifier.parent_path || identifier.asset_name ":
			(DbConnectionFactory.isMySql())?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+
			" from htmlpage_version_info page_info join htmlpage on(htmlpage.identifier = page_info.identifier) join identifier identifier on (identifier.id = htmlpage.identifier) "+
			" UNION ALL "+
			" select template.inode as inode, 'template' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, temp_info.live_inode,temp_info.working_inode,temp_info.deleted, coalesce(template.title,template.identifier) as name "+
			" from template_version_info temp_info join template on(template.identifier = temp_info.identifier) join identifier identifier on identifier.id = template.identifier "+
			" UNION ALL "+
			" select file_asset.inode as inode, 'file_asset' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, file_info.live_inode,file_info.working_inode,file_info.deleted, "+
			((DbConnectionFactory.isPostgres()||(DbConnectionFactory.isOracle())||DbConnectionFactory.isH2())?"identifier.parent_path || identifier.asset_name ":
				(DbConnectionFactory.isMySql())?" CONCAT(identifier.parent_path,identifier.asset_name) ": " identifier.parent_path + identifier.asset_name ")+" as name "+
				" from fileasset_version_info file_info join file_asset on(file_asset.identifier = file_info.identifier) join identifier identifier on identifier.id = file_asset.identifier "+
				" UNION ALL "+
				" select " + Inode.Type.CONTAINERS.getTableName() + ".inode as inode, 'container' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, con_info.live_inode,con_info.working_inode,con_info.deleted, coalesce(" + Inode.Type.CONTAINERS.getTableName() + ".title," + Inode.Type.CONTAINERS.getTableName() + ".identifier) as name "+
				" from container_version_info con_info join " + Inode.Type.CONTAINERS.getTableName() + " on(" + Inode.Type.CONTAINERS.getTableName() + ".identifier = con_info.identifier) join identifier identifier on (identifier.id = " + Inode.Type.CONTAINERS.getTableName() + ".identifier) "+
				" UNION ALL "+
				" select links.inode as inode, 'link' as asset_type, mod_user as mod_user_id, identifier.host_inode as host_id, mod_date, links_info.live_inode,links_info.working_inode,links_info.deleted, coalesce(links.title,links.identifier) as name "+
				" from link_version_info links_info join links on(links_info.identifier= links.identifier) join identifier identifier on (identifier.id = links.identifier) "+
				" )assets where mod_date>(select coalesce(max(mod_date),"
				+(DbConnectionFactory.isPostgres()||DbConnectionFactory.isH2()?"'1970-01-01 00:00:00')"
						:(DbConnectionFactory.isOracle())?"TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS'))"
								:(DbConnectionFactory.isMySql())?"STR_TO_DATE('1970-01-01','%Y-%m-%d'))"
										:(DbConnectionFactory.isMsSql())?"CAST('1970-01-01' AS DATETIME))":"")+
										" from analytic_summary_workstream) and host_id = '"+hostId+"' order by assets.mod_date,assets.name asc ";
	}


	protected String getTopAssetsQuery() {
		// This query counts legacy html pages
		StringBuilder sbCountLegacyHtmlPages = new StringBuilder("(SELECT COUNT(*) ")
		.append("FROM htmlpage_version_info pageinfo JOIN identifier ON (identifier.id = pageinfo.identifier) ") 
		.append("WHERE identifier.host_inode = ? AND pageinfo.live_inode IS NOT NULL GROUP BY identifier.host_inode)");
		
		// This query counts html pages by combine 'html page contentlets' and 'legacy html pages'
		StringBuilder sbCountAllHtmlPages = new StringBuilder("SELECT identifier.host_inode AS host_inode, ")
		.append("(COUNT(contentlet.inode) + ").append(sbCountLegacyHtmlPages.toString()).append(") AS count, 'htmlpage' as asset_type ")
		.append("FROM contentlet_version_info contentinfo ")
		.append("JOIN identifier ON (identifier.id = contentinfo.identifier) ") 
		.append("JOIN contentlet ON (contentlet.identifier = identifier.id) ")
		.append("JOIN structure ON (contentlet.structure_inode=structure.inode) ") 
		.append("WHERE identifier.host_inode = ? AND structure.structuretype = ")
		.append(Structure.STRUCTURE_TYPE_HTMLPAGE).append(" ")
		.append("AND contentinfo.live_inode IS NOT NULL ")
		.append("GROUP BY identifier.host_inode");
		
		// This query counts files assets
		StringBuilder sbCountFileAssets = new StringBuilder("SELECT identifier.host_inode AS host_inode, ")
		.append("count(*) AS count, 'file_asset' AS asset_type ")
		.append("FROM ((SELECT identifier, live_inode FROM fileasset_version_info) UNION ALL ")
		.append("      (SELECT identifier, live_inode FROM contentlet_version_info WHERE EXISTS ")
		.append("      (SELECT * FROM contentlet cc JOIN structure st ON (cc.structure_inode = st.inode) ")
		.append("         WHERE contentlet_version_info.identifier=cc.identifier AND ")
		.append("               st.structuretype=").append(Structure.STRUCTURE_TYPE_FILEASSET).append(" ))) ainfo ")
		.append("JOIN identifier ON (identifier.id = ainfo.identifier) ")
		.append("WHERE identifier.host_inode = ? AND ainfo.live_inode IS NOT NULL ")
		.append("GROUP BY identifier.host_inode");
		
		// This query counts contentlets excluding Structure.STRUCTURE_TYPE_FILEASSET and Structure.STRUCTURE_TYPE_HTMLPAGE
		StringBuilder sbCountContentlets  = new StringBuilder("SELECT identifier.host_inode as host_inode, ")
		.append("COUNT(contentlet.inode) AS count, 'contentlet' AS asset_type ")
		.append("FROM contentlet_version_info contentinfo JOIN identifier ON (identifier.id = contentinfo.identifier) ")
		.append("JOIN contentlet ON (contentlet.identifier = identifier.id) JOIN structure ON (contentlet.structure_inode = structure.inode) ")
		.append("WHERE identifier.host_inode = ? AND structure.structuretype<>").append(Structure.STRUCTURE_TYPE_FILEASSET).append(" AND ")
		.append("structure.structuretype<>").append(Structure.STRUCTURE_TYPE_HTMLPAGE).append(" AND contentinfo.live_inode IS NOT NULL ")
		.append("GROUP BY identifier.host_inode");
		
		return sbCountAllHtmlPages.append(" UNION ALL ")
				.append(sbCountFileAssets).append(" UNION ALL ")
				.append(sbCountContentlets).toString();
	}

	protected String  getIdentifierColumn(){
		return "contentlet.identifier";
	}

	protected String getWorkstreamListQuery(){
		return new StringBuilder("select distinct {analytic_summary_workstream.*}, user_.firstname as username, contentlet.title as hostname ").append(
		" from analytic_summary_workstream, user_ , contentlet,contentlet_version_info contentinfo ").append(
		" where user_.userid = analytic_summary_workstream.mod_user_id and contentlet.identifier = analytic_summary_workstream.host_id ").append(
		" and contentlet.identifier = contentinfo.identifier and contentinfo.live_inode is not null and analytic_summary_workstream.name is not null ").append(
			" and user_.delete_in_progress = ").append(DbConnectionFactory.getDBFalse()).toString();
	}

	protected String getWorkstreamCountQuery(){
		return new StringBuilder("select count(distinct analytic_summary_workstream.id) as summaryCount from analytic_summary_workstream, user_, contentlet,contentlet_version_info info where").append(
		  " user_.userid = analytic_summary_workstream.mod_user_id and contentlet.identifier = analytic_summary_workstream.host_id ").append(
		  " and contentlet.identifier = info.identifier and info.live_inode is not null and analytic_summary_workstream.name is not null ").append(
		  	" and user_.delete_in_progress = ").append(DbConnectionFactory.getDBFalse()).toString();
	}


	protected StringBuffer getHostListQuery(boolean hasCategory, String selectedCategories,  String runDashboardFieldContentlet){
		StringBuffer query = new StringBuffer();
		query.append("select "+ (DbConnectionFactory.isOracle() || DbConnectionFactory.isMsSql()?"":" distinct ")+ (" {contentlet.*}, ")    +
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
				") "+ (DbConnectionFactory.isMsSql()?" as d": DbConnectionFactory.isOracle()? " d " : " as d") +" on d.host_id = contentlet.identifier " +
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
