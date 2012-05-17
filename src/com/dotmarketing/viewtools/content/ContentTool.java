package com.dotmarketing.viewtools.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.lucene.queryParser.ParseException;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.RecurrenceUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * The purpose of this class is to provide a way to easily search and interact with content and dotcms
 * objects surrounding content from the front end of dotCMS.  Previously dotCMS relayed on many macros to do 
 * things like search for content but because of the overhead of Velocity this can cause load and
 * performance issues in certain cases. This tool should provide a cleaner way to interact with content without
 * needing to parse tons of objects in Velocity.
 * 
 * The tool is mapped in Velocity as $dotcontent
 * 
 * @author Jason Tesser
 * @since 1.9.3
 */
public class ContentTool implements ViewTool {

	private ContentletAPI conAPI;
	private HttpServletRequest req;
	private UserWebAPI userAPI;
	private User user = null;
	private int MAX_LIMIT = 100;
	private boolean ADMIN_MODE;
	private boolean PREVIEW_MODE;
	private boolean EDIT_MODE;
	private boolean EDIT_OR_PREVIEW_MODE;
	private Context context;
	private Host currentHost;
	
	public void init(Object initData) {
		conAPI = APILocator.getContentletAPI();
		userAPI = WebAPILocator.getUserWebAPI();
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();
		try {
			user = userAPI.getLoggedInFrontendUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
		HttpSession session = req.getSession();
		ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if(EDIT_MODE || PREVIEW_MODE){
			EDIT_OR_PREVIEW_MODE = true;
		}
		try{
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		}catch(Exception e){
			Logger.error(this, "Error finding current host", e);
		}
	}
	
	/**
	 * Will pull a single piece of content for you based on the inode or identifier. It will always
	 * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
	 * identifier.  If it is an inode this is ignored.
	 * Will return NULL if not found
	 * @param inodeOrIdentifier Can be either an Inode or Indentifier of content.
	 * @return NULL if not found
	 */
	public ContentMap find(String inodeOrIdentifier){
		String[] recDates = null;
		try {
			recDates = RecurrenceUtil.getRecurrenceDates(inodeOrIdentifier);
			inodeOrIdentifier = RecurrenceUtil.getBaseEventIdentifier(inodeOrIdentifier);
			Contentlet c = conAPI.find(inodeOrIdentifier, user, true);
			if(c != null){
				if(c.getStructure().getVelocityVarName().equals("calendarEvent")
						&& (recDates!=null && recDates.length==2)){
					String startDate = recDates[0];
					String endDate = recDates[1];
					if(UtilMethods.isSet(startDate) && UtilMethods.isSet(endDate)){
						c.setDateProperty("startDate", new Date(Long.parseLong(startDate)));
						c.setDateProperty("endDate", new Date(Long.parseLong(endDate)));
					}
				}
				return new ContentMap(c, user, EDIT_OR_PREVIEW_MODE,currentHost,context);
			}
		} catch (Exception e) {
			Logger.error(ContentTool.class,e.getMessage(),e);
			return null;
		}
		
		String stateQuery = PREVIEW_MODE || EDIT_MODE ? "+working:true +deleted:false" : "+live:true +deleted:false";
		try {
			List<Contentlet> l = conAPI.search("+identifier:" + inodeOrIdentifier + " " + stateQuery, 0, -1, "modDate", user, true);
			if(l== null || l.size() < 1){
				return null;
			}else{
				if(l.size()>1){
					Logger.warn(this, "More then one live or working content found with identifier = " + inodeOrIdentifier);
				}
				if(l.get(0).getStructure().getVelocityVarName().equals("calendarEvent")
						&& (recDates!=null && recDates.length==2)){
					String startDate = recDates[0];
					String endDate = recDates[1];
					if(UtilMethods.isSet(startDate) && UtilMethods.isSet(endDate)){
						l.get(0).setDateProperty("startDate", new Date(Long.parseLong(startDate)));
						l.get(0).setDateProperty("endDate", new Date(Long.parseLong(endDate)));
					}
				}
				return new ContentMap(l.get(0), user,EDIT_OR_PREVIEW_MODE,currentHost,context);
			}
		} catch (Exception e) {
			Logger.error(ContentTool.class,e.getMessage());
			Logger.debug(ContentTool.class,e.getMessage(),e);
			return null;
		}
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullcontent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pull('+structureName:newsItem',5,'modDate desc'))<br />
	 * 		$con.headline<br />
	 * #end<br />
	 * Returns empty List if no results are found
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
	 * @return  Returns empty List if no results are found
	 */
	public List<ContentMap> pull(String query, String limit, String sort){
			int l = new Integer(limit);
			return pull(query, l, sort);
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullcontent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pull('+structureName:newsItem',5,'modDate desc'))<br />
	 * 		$con.headline<br />
	 * #end<br />
	 * Returns empty List if no results are found
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
	 * @return Returns empty List if no results are found
	 */
	public List<ContentMap> pull(String query, int limit, String sort){
		List<ContentMap> ret = null;
		try {
			for(Contentlet c : conAPI.search(addDefaultsToQuery(query), limit, -1, sort, user, true)){
				if(ret == null){
					ret = new ArrayList<ContentMap>();
				}
				ret.add(new ContentMap(c,user,EDIT_OR_PREVIEW_MODE,currentHost,context));
			}
		} catch (Exception e) {
			Logger.error(ContentTool.class,e.getMessage());
			Logger.debug(ContentTool.class,e.getMessage(),e);
		}
		if(ret == null){
			ret = new ArrayList<ContentMap>();
		}
		return ret;
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullcontent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pullPagenated('+structureName:newsItem',20,20,'modDate desc'))<br />
	 * 		$con.headline<br />
	 * #end<br />
	 * Returns empty List if no results are found <br />
	 * there is a totalResults avialible to you on the returned list. $retList.totalResults
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @param offset offset to start the results from 
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
	 * @return Returns empty List if no results are found
	 */
	public PaginatedArrayList<ContentMap> pullPagenated(String query, int limit, int offset, String sort){
		PaginatedArrayList<ContentMap> ret = new PaginatedArrayList<ContentMap>();
		try {
			PaginatedArrayList<Contentlet> cons = (PaginatedArrayList<Contentlet>)conAPI.search(addDefaultsToQuery(query), limit, offset<0?0:offset, sort, user, true);
			if(cons != null){
				ret.setTotalResults(cons.getTotalResults());
				for(Contentlet c : cons){
					ret.add(new ContentMap(c,user,EDIT_OR_PREVIEW_MODE,currentHost,context));
				}
			}
		} catch (Exception e) {
			Logger.error(ContentTool.class,e.getMessage());
			Logger.debug(ContentTool.class,e.getMessage(),e);
		}
		return ret;
	}
	
	/**
	 * Works just similar to the pullPagenated. Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullcontent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #set($pagedList = $dotcontent.pullPerPage("+structureName:webPageContent" ,20, 3, "modDate desc"))<br/>
	 * <div><br/>
	 * 		$pagedList<br/>
	 * </div><br/>
	 * <h2>$pagedList.previousPage</h2><br/>
	 * <h2>$pagedList.nextPage</h2><br/>
	 * <h2>$pagedList.totalPages</h2><br/>
	 * <h2>$pagedList.totalResults</h2><br/>
	 * #foreach($con in $pagedList)<br/>
	 * 		<h2>$con.inode</h2><br/>
	 * 		<h2>$con.identifier</h2><br/>
	 * 		$con.title<br/>
	 * #end<br />
	 * Returns empty List if no results are found
	 * The returned list is a PaginatedContentList which lets you get at the totalPages, nextPage, and previousPage, and totalResults as helpers ie.. $conlist.previousPage ...
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param currentPage Current page number for pagination the first page would be one.
	 * @param contentsPerPage Number of contentlets you are displaying per page
	 * @param offset offset to start the results from 
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
	 * @return Returns empty List if no results are found
	 * 
	 */
	public PaginatedContentList<ContentMap> pullPerPage(String query, int currentPage, int contentsPerPage, String sort){
		PaginatedArrayList<ContentMap> cmaps = pullPagenated(addDefaultsToQuery(query), contentsPerPage, contentsPerPage * (currentPage - 1), sort);
		PaginatedContentList<ContentMap> ret = new PaginatedContentList<ContentMap>();
		if(cmaps.size()>0){
			long minIndex = (currentPage - 1) * contentsPerPage;
	        long totalCount = cmaps.getTotalResults();
	        long maxIndex = contentsPerPage * currentPage;
	        if((minIndex + contentsPerPage) >= totalCount){
	        	maxIndex = totalCount;
	        }
			ret.addAll(cmaps);
			ret.setTotalResults(cmaps.getTotalResults());
			ret.setTotalPages((long)Math.ceil(((double)cmaps.getTotalResults())/((double)contentsPerPage)));
			ret.setNextPage(maxIndex < totalCount);
			ret.setPreviousPage(minIndex > 0);
			cmaps = null;
		}
		return ret;
	}
	
	/**
	 * Will return a ContentSearch object which can be used on dotCMS front end. 
	 * The method can be used to determine the inodes or identifiers which match a given query.
	 * It does NOT return actual content only the inodes and identifiers.  An example use could be  
	 * you need to know if a query matches at all. Lets say you want to know if a category is used at all within
	 * a given query this is the fastest way to figure it out as dotCMS doesn't have to put together the content itself.
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param limit 0 is the dotCMS max limit which is 10000. 
	 * @return
	 */
	public List<ContentletSearch> query(String query, int limit){
		return query(query, limit, "modDate");
	}
	
	/**
	 * Will return a ContentSearch object which can be used on dotCMS front end. 
	 * The method can be used to determine the inodes or identifiers which match a given query.
	 * It does NOT return actual content only the inodes and identifiers.  An example use could be  
	 * you need to know if a query matches at all. Lets say you want to know if a category is used at all within
	 * a given query this is the fastest way to figure it out as dotCMS doesn't have to put together the content itself.
	 * Returns empty List if no results are found
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @param limit 0 is the dotCMS max limit which is 10000. 
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
	 * @return Returns empty List if no results are found
	 */
	public List<ContentletSearch> query(String query, int limit, String sort){
		List<ContentletSearch> ret = null;
		try {
			 ret = conAPI.searchIndex(addDefaultsToQuery(query), limit, -1, sort, user, true);
		} catch (Exception e) {
			Logger.error(ContentTool.class,e.getMessage(),e);
		}
		if(ret == null){
			ret = new ArrayList<ContentletSearch>();
		}
		return ret;
	}
	
	/**
	 * Use this method to return the number of contents which match a particular query. 
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @return
	 */
	public long count(String query) {
		try {
            return conAPI.indexCount(query, user, true);
        } catch (Exception e) {
            Logger.error(this, "can't get indexCount for query: "+query,e);
            return 0;
        }
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullRelatedContent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd',false,5))<br />
	 * 		$con.title<br />
	 * #end<br />
	 * The method will figure out language, working and live for you. 
	 * Returns empty List if no results are found
	 * @param relationshipName - Name of the relationship as defined in the structure.
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param pullParents Should the related pull be based on Parents or Children
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @return Returns empty List if no results are found
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List<ContentMap> pullRelated(String relationshipName, String contentletIdentifier, boolean pullParents, int limit) {
		return pullRelated(relationshipName, contentletIdentifier, null, pullParents, limit, null);
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullRelatedContent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd',false,5,'modDate'))<br />
	 * 		$con.title<br />
	 * #end<br />
	 * The method will figure out language, working and live for you. 
	 * Returns empty List if no results are found
	 * @param relationshipName - Name of the relationship as defined in the structure.
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param pullParents Should the related pull be based on Parents or Children
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
	 * @return Returns empty List if no results are found
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List<ContentMap> pullRelated(String relationshipName, String contentletIdentifier, boolean pullParents, int limit, String sort) {
		return pullRelated(relationshipName, contentletIdentifier, null, pullParents, limit, sort);
	}
	
	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. 
	 * This method is better then the old #pullRelatedContent macro because it doesn't have to 
	 * parse all the velocity content object that are returned.  If you are building large pulls
	 * and depending on the types of fields on the content this can get expensive especially
	 * with large data sets.<br />
	 * EXAMPLE:<br />
	 * #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd','+myField:someValue',false,5,'modDate desc'))<br />
	 * 		$con.title<br />
	 * #end<br />
	 * The method will figure out language, working and live for you if not passed in with the condition
	 * Returns empty List if no results are found
	 * @param relationshipName - Name of the relationship as defined in the structure.
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be Null
	 * @param pullParents Should the related pull be based on Parents or Children
	 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
	 * @return Returns empty List if no results are found
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @return Returns empty List if no results are found
	 */
	public List<ContentMap> pullRelated(String relationshipName, String contentletIdentifier, String condition, boolean pullParents, int limit, String sort) {	
		Relationship rel = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		String relNameForQuery = "";
		if(rel.getParentStructureInode().equals(rel.getChildStructureInode())){
			if(pullParents){
				relNameForQuery = relationshipName.trim() + "-child";
			}else{
				relNameForQuery = relationshipName.trim() + "-parent";
			}
		}
		
		if(!UtilMethods.isSet(relNameForQuery))//DOTCMS-5328
			relNameForQuery = rel.getRelationTypeValue();
		
		contentletIdentifier = RecurrenceUtil.getBaseEventIdentifier(contentletIdentifier);
		
					
		String pullquery = "+type:content +" + relNameForQuery + ":" + contentletIdentifier;
				
		if(UtilMethods.isSet(condition)){
	           pullquery += " " + condition;
		}

		if(!UtilMethods.isSet(sort)){ 
			sort = relationshipName + "-" + contentletIdentifier + "-order";
		}
		return pull(pullquery, limit, sort);
	}
	
	private String addDefaultsToQuery(String query){
		String q = query;
		if(!query.contains("languageId")){
			if(UtilMethods.isSet(req.getSession().getAttribute("com.dotmarketing.htmlpage.language"))){
				q += " +languageId:" + req.getSession().getAttribute("com.dotmarketing.htmlpage.language");
			} 
		}
	  
	  	if(!(query.contains("live:") || query.contains("working:") )){      
			if(EDIT_OR_PREVIEW_MODE){
				q +=" +working:true ";
			}else{
				q +=" +live:true ";
			}
				
	  	}
		
		  
	  	if(!UtilMethods.contains(query,"deleted:")){      
			q+=" +deleted:false ";
		}
	  	return q;
	}
}
