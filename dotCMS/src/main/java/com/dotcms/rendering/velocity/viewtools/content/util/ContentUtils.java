package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.util.PaginationUtil;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.TimeMachineUtil;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.RecurrenceUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to abstract the methods called from the ContentTool Viewtool
 * so they can be called from other viewtools or external plugins.
 * other viewtools can return a ContentMap back to the page
 * @author Jason Tesser
 * @since 1.9.3
 */
public class ContentUtils {

	// it is true, even if the pattern is false because the client has to include the depth parameter to activate it
	private static final boolean addRelationshipsOnPage = Config.getBooleanProperty("ADD_RELATIONSHIPS_ON_PAGE", true);

	private static ContentletAPI conAPI;
	   public static final ContentUtils INSTANCE = new ContentUtils();

	    private ContentUtils() {
	    	conAPI = APILocator.getContentletAPI();
	    }

	    public ContentUtils getInstance()
	    {
	        return INSTANCE;
	    }


	/**
	 * Pulls a single piece of content based on its Inode or Identifier. It will always try to retrieve the live
	 * content unless the process calling this method is in EDIT or PREVIEW Mode in the back-end of dotCMS when
	 * passing in an Identifier. If it is an Inode, this is ignored. This method will return {@code null} if the
	 * Contentlet is not found.
	 *
	 * @param inodeOrIdentifier    The Inode or Identifier of the {@link Contentlet} being requested.
	 * @param user                 The {@link User} used to retrieve the Contentlet's data.
	 * @param EDIT_OR_PREVIEW_MODE If the current access mode is EDIT or PREVIEW, set this to {@code true}.
	 * @param sessionLang          The language ID of the current session.
	 *
	 * @return The requested {@link Contentlet} object.
	 */
		public static Contentlet find(final String inodeOrIdentifier, final User user, final boolean EDIT_OR_PREVIEW_MODE, final long sessionLang){
			final Optional<String> timeMachineDate = TimeMachineUtil.getTimeMachineDate();
			return find(inodeOrIdentifier, user, timeMachineDate.orElse(null), EDIT_OR_PREVIEW_MODE, sessionLang);
		}

	    private static Contentlet fixRecurringDates(Contentlet contentlet, String[] recDates) {
	        if( null == contentlet || !contentlet.isCalendarEvent() || recDates==null || recDates.length!=2) {
	            return contentlet;
	        }
            if(UtilMethods.isSet(recDates[0]) && UtilMethods.isSet(recDates[1])){
                contentlet.setDateProperty("startDate", new Date(Long.parseLong(recDates[0])));
                contentlet.setDateProperty("endDate", new Date(Long.parseLong(recDates[1])));
            }
            
            return contentlet;
	    }

	/**
	 * Pulls a single piece of content based on its Inode or Identifier. It will always try to retrieve the live
	 * content unless the process calling this method is in EDIT or PREVIEW Mode in the back-end of dotCMS when
	 * passing in an Identifier. If it is an Inode, this is ignored. This method will return {@code null} if the
	 * Contentlet is not found.
	 *
	 * @param inodeOrIdentifierIn  The Inode or Identifier of the {@link Contentlet} being requested.
	 * @param user                 The {@link User} used to retrieve the Contentlet's data.
	 * @param tmDate               Optional, the specific date for retrieving a specific Time Machine version.
	 * @param EDIT_OR_PREVIEW_MODE If the current access mode is EDIT or PREVIEW, set this to {@code true}.
	 * @param sessionLang          The language ID of the current session.
	 *
	 * @return The requested {@link Contentlet} object.
	 */
	public static Contentlet find(final String inodeOrIdentifierIn, final User user,
			final String tmDate, final boolean EDIT_OR_PREVIEW_MODE,
			final long sessionLang) {
		final String inodeOrIdentifier = RecurrenceUtil.getBaseEventIdentifier(inodeOrIdentifierIn);
		final String[] recDates = RecurrenceUtil.getRecurrenceDates(inodeOrIdentifier);
		try {
			final PageMode pageMode = PageMode.get();
			// Test by inode
			Contentlet contentlet = conAPI.find(inodeOrIdentifier, user, true);
			if (contentlet != null) {  // Time machine by the identifier extracted from the contentlet we found through the inode
				if (null != tmDate) {
					final Date ffdate = new Date(Long.parseLong(tmDate));
					final Optional<Contentlet> futureContent = conAPI.findContentletByIdentifierOrFallback(
							contentlet.getIdentifier(), sessionLang,
							VariantAPI.DEFAULT_VARIANT.name(),
							ffdate, user, pageMode.respectAnonPerms);
					if (futureContent.isPresent()) {
						return fixRecurringDates(futureContent.get(), recDates);
					}
				}
			}

			// okay if we failed retrieving the contentlet using inode we still need to test by identifier Cuz this method actually takes an identifier
			if (null != tmDate) {
				final Date ffdate = new Date(Long.parseLong(tmDate));
				final Optional<Contentlet> futureContent = conAPI.findContentletByIdentifierOrFallback(
						inodeOrIdentifier, sessionLang, VariantAPI.DEFAULT_VARIANT.name(),
						ffdate, user, pageMode.respectAnonPerms);
				if (futureContent.isPresent()) {
					return fixRecurringDates(futureContent.get(), recDates);
				}
			}

			// if We ran out of possibilities retrieving the contentlet via time machine we fall back on the current or present live contentlet
			// if we already had one found by inode this is the one We should return
			if (null != contentlet) {
				return fixRecurringDates(contentlet, recDates);
			}
            // Fallbacks from here on...
			//If not we try to get the contentlet by the identifier
			final ContentletVersionInfo contentletVersionInfoByFallback = WebAPILocator.getVariantWebAPI()
					.getContentletVersionInfoByFallback(sessionLang, inodeOrIdentifier,
							EDIT_OR_PREVIEW_MODE ? PageMode.PREVIEW_MODE : PageMode.LIVE, user);
			// If content is being viewed in EDIT_OR_PREVIEW_MODE, we need to get the working version. Otherwise, we
			// need the live version. That's why we're negating it when calling the API
			final String contentletInode =
					EDIT_OR_PREVIEW_MODE ? contentletVersionInfoByFallback.getWorkingInode()
							: contentletVersionInfoByFallback.getLiveInode();

			contentlet = conAPI.find(contentletInode, user, true);
			return fixRecurringDates(contentlet, recDates);
		} catch (final Exception e) {
			String msg = e.getMessage();
			msg = (msg.contains("\n")) ? msg.substring(0, msg.indexOf("\n")) : msg;
			final String errorMsg = String.format(
					"An error occurred when User '%s' attempted to find Contentlet " +
							"with Inode/ID '%s' [lang=%s, tmDate=%s]: %s",
					user.getUserId(), inodeOrIdentifier, sessionLang, tmDate, msg);
			Logger.warn(ContentUtils.class, errorMsg);
			Logger.debug(ContentUtils.class, errorMsg, e);
			return null;
		}
	}
		
		/**
		 * Returns empty List if no results are found
		 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
		 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
		 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
		 * @return  Returns empty List if no results are found
		 */
		public static List<Contentlet> pull(String query, String limit, String sort,User user, String tmDate){
				int l = Integer.valueOf(limit);
				return pull(query, l, sort, user, tmDate);
		}
		
		/**
		 * Returns empty List if no results are found
		 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
		 * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
		 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
		 * @return Returns empty List if no results are found
		 */
		public static List<Contentlet> pull(String query,int limit, String sort, User user, String tmDate){
		    return pull(query,-1,limit,sort, user, tmDate);
		}

		/**
		 * Pull content using query if a Time Machine Date is set into session then it ios take account when the query is
		 * ran.
		 * 
		 * @param query
		 * @param offset
		 * @param limit
		 * @param sort
		 * @param user
		 * @param tmDate
		 * @return
		 */
		public static PaginatedArrayList<Contentlet> pull(
				final String query,
				final int offset,
				final int limit,
				final String sort,
				final User user,
				final String tmDate){
			return pull(query, offset, limit, sort, user, tmDate, PageMode.get().respectAnonPerms);
		}

		public static PaginatedArrayList<Contentlet> pull(final String query, final int offset, final int limit,
														  final String sort, final User user, final boolean respectFrontendRoles) {
			final String tmDate = TimeMachineUtil.getTimeMachineDate().orElse(null);
			return pull(query, offset, limit, sort, user, tmDate, respectFrontendRoles);
		}

	public static PaginatedArrayList<Contentlet> pull(String query, final int offset, final int limit, final String sort, final User user, final String tmDate, final boolean respectFrontendRoles){
		    final PaginatedArrayList<Contentlet> ret = new PaginatedArrayList<>();

			try {
				//need to send the query with the defaults --- 
			    List<Contentlet> contentlets=null;
			    if(tmDate!=null && query.contains("+live:true")) {
			        // with time machine on!
                    final Date futureDate = new Date(Long.parseLong(tmDate));
                    query = query.replaceAll("\\+live\\:true", "")
                            .replaceAll("\\+working\\:true", "");
                    final String formatedDate = ESMappingAPIImpl.datetimeFormat.format(futureDate);

                    final String notExpired = " +expdate:[" + formatedDate + " TO 29990101000000] ";
                    final String workingQuery = query + " +working:true " +
                            "+pubdate:[" + ESMappingAPIImpl.datetimeFormat.format(new Date())
                            +
                            " TO " + formatedDate + "] " + notExpired;
                    final String liveQuery = query + " +live:true " + notExpired;
		            
		            final PaginatedArrayList<Contentlet> workingContent = (PaginatedArrayList<Contentlet>)conAPI.search(workingQuery, limit, offset, sort, user, respectFrontendRoles);
                    final PaginatedArrayList<Contentlet> liveContent = (PaginatedArrayList<Contentlet>)conAPI.search(liveQuery, limit, offset, sort, user, respectFrontendRoles);
		            ret.setQuery(liveQuery);

					contentlets = new ArrayList<>(workingContent);
					ret.setTotalResults(Math.max(workingContent.getTotalResults(), liveContent.getTotalResults()));

					// merging both results avoiding repeated inodes
					final Set<String> inodes = workingContent.stream().map(Contentlet::getInode).collect(Collectors.toSet());
                    contentlets.addAll(liveContent.stream().filter(contentlet -> !inodes.contains(contentlet.getInode())).collect(Collectors.toList()));

		            // sorting the result
		            if(UtilMethods.isSet(sort) && !sort.trim().equals("random")) {
	    	            final String[] sorts=sort.split(",");
	    	            Collections.sort(contentlets, new Comparator<Contentlet>() {
	    	                @SuppressWarnings({"unchecked","rawtypes"})
	                        @Override
	    	                public int compare(Contentlet a, Contentlet b) {
	    	                    int comp=0;
	    	                    for(int x=0;x<sorts.length && comp==0;x++) {
	    	                        String[] ss=sorts[x].split(" ");
	    	                        Comparable c1=(Comparable)a.get(ss[0]);
	    	                        Comparable c2=(Comparable)b.get(ss[0]);
	    	                        if(c1!=null && c2!=null) {
	        	                        if(ss.length==1 || ss[1].equals("asc"))
	        	                            comp=c1.compareTo(c2);
	        	                        else
	        	                            comp=c2.compareTo(c1);
	    	                        }
	    	                    }   
	    	                    return comp;
	    	                }
	    	            });
		            }
		            
		            // truncate to respect limit, remember 0 means no limit
		            if(limit > 0 && contentlets.size() > limit){
		                contentlets = contentlets.subList(0, limit);
		            }
			    } else {
			        // normal query
			        PaginatedArrayList<Contentlet> conts=(PaginatedArrayList<Contentlet>)conAPI.search(query, limit, offset, sort, user, respectFrontendRoles);
			        ret.setTotalResults(conts.getTotalResults());
			        ret.setQuery(query);
			        contentlets=conts;
			    }
				ret.addAll(contentlets);
			} 
			catch (Throwable e) {
				String msg = e.getMessage();
				msg=(msg==null && e.getStackTrace().length>0)?e.getStackTrace()[0].toString() : msg;
				msg = (msg!=null && msg.contains("\n")) ? msg.substring(0,msg.indexOf("\n")) : msg;
				Logger.warn(ContentUtils.class,msg);
				Logger.debug(ContentUtils.class,e.getMessage(),e);
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
		public static PaginatedArrayList<Contentlet> pullPagenated(String query, int limit, int offset, String sort,User user, String tmDate){
			return pull(query, offset, limit, sort, user, tmDate);
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
		 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
		 * @return Returns empty List if no results are found
		 * 
		 */
		public static PaginatedContentList<Contentlet> pullPerPage(final String query,
				final int page, final int contentsPerPage, final String sort, final User user,
				final String tmDate) {

            // Calculate the offset
			final int currentPage = Math.max(page, 1);
            var offset = contentsPerPage * (currentPage - 1);

            PaginatedArrayList<Contentlet> cmaps = pullPagenated(
                    query, contentsPerPage, offset, sort, user, tmDate
            );

            return PaginationUtil.paginatedArrayListToPaginatedContentList(
                    cmaps, contentsPerPage, offset
            );
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
		public static List<ContentletSearch> query(String query, int limit, User user, String tmDate){
			return query(query, limit, "modDate", user, tmDate);
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
		public static List<ContentletSearch> query(String query, int limit, String sort, User user, String tmDate){
			List<ContentletSearch> ret = null;
			try {
	            if(tmDate!=null && query.contains("+live:true")) {
	                // with timemachine on!
	                // as we need to load contentlets anyway to sort 
	                // lets just call pull here
	                List<Contentlet> conts=pull(query, limit, sort, user, tmDate);
	                ret = new ArrayList<>(conts.size());
	                for(Contentlet cm : conts) {
	                    ContentletSearch cs=new ContentletSearch();
	                    cs.setInode((String)cm.get("inode"));
	                    cs.setIdentifier((String)cm.get("identifier"));
	                    ret.add(cs);
	                }   
	            }
	            else {
	                // normal query
	                ret = conAPI.searchIndex(query, limit, -1, sort, user, true);
	            }
			} catch (Exception e) {
				Logger.error(ContentUtils.class,e.getMessage(),e);
			}
			if(ret == null){
				ret = new ArrayList<>();
			}
			return ret;
		}
		
		/**
		 * Use this method to return the number of contents which match a particular query. 
		 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
		 * @return
		 */
		public static long count(String query, User user, String tmDate) {
			try {
			    if(tmDate!=null && query.contains("+live:true"))
			        return query(query,0, user, tmDate).size();
			    else
			        return conAPI.indexCount(query, user, true);
	        } catch (Exception e) {
	            Logger.error(ContentUtils.class, "can't get indexCount for query: "+query,e);
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
		public static List<Contentlet> pullRelated(String relationshipName, String contentletIdentifier, boolean pullParents, int limit, User user, String tmDate) {
			return pullRelated(relationshipName, contentletIdentifier, null, pullParents, limit, null, user, tmDate);
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
		public static List<Contentlet> pullRelated(String relationshipName, String contentletIdentifier, boolean pullParents, int limit, String sort, User user, String tmDate) {
			return pullRelated(relationshipName, contentletIdentifier, null, pullParents, limit, sort, user, tmDate);
		}

    /**
     * Will return a ContentMap object which can be used on dotCMS front end. This method is better
     * then the old #pullRelatedContent macro because it doesn't have to parse all the velocity
     * content object that are returned.  If you are building large pulls and depending on the types
     * of fields on the content this can get expensive especially with large data sets.<br />
     * EXAMPLE:<br /> #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd','+myField:someValue',false,5,'modDate
     * desc'))<br /> $con.title<br /> #end<br /> The method will figure out language, working and
     * live for you if not passed in with the condition Returns empty List if no results are found
     *
     * @param relationshipName - Name of the relationship as defined in the structure.
     * @param contentletIdentifier - Identifier of the contentlet
     * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be
     * Null
     * @param pullParents Should the related pull be based on Parents or Children
     * @param limit 0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited
     * amount as all content will load into memory
     * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple
     * values "sort1 acs, sort2 desc". Can be Null
     * @param user
     * @param tmDate Time Machine date
     * @return Returns empty List if no results are found
     */
    public static List<Contentlet> pullRelated(String relationshipName, String contentletIdentifier,
            String condition, boolean pullParents, int limit, String sort, User user,
            String tmDate) {
        final Relationship relationship = APILocator.getRelationshipAPI()
                .byTypeValue(relationshipName);

        return getPullResults(relationship, contentletIdentifier, condition, limit, -1, sort,
                user, tmDate, pullParents, -1, null);
    }

    /**
     * Will return a ContentMap object which can be used on dotCMS front end. This method is better
     * than the old #pullRelatedContent macro because it doesn't have to parse all the velocity
     * content object that are returned.  If you are building large pulls and depending on the types
     * of fields on the content this can get expensive especially with large data sets.<br />
     * EXAMPLE:<br /> #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd','+myField:someValue',false,5,'modDate
     * desc'))<br /> $con.title<br /> #end<br /> The method will figure out language, working and
     * live for you if not passed in with the condition Returns empty List if no results are found
     *
	 * This version of the method will assume no offset is set
	 *
     * @param relationshipName - Name of the relationship as defined in the structure.
     * @param contentletIdentifier - Identifier of the contentlet
     * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be
     * Null
     * @param pullParents Should the related pull be based on Parents or Children
     * @param limit 0 is the dotCMS max limit which is 10000. Be careful when searching for unlimited
     * amount as all content will load into memory
     * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple
     * values "sort1 acs, sort2 desc". Can be Null
     * @param user
     * @param tmDate - Time Machine date
     * @param language - Language ID to be used to filter results
     * @param live - Boolean to filter live/non-live content
     * @return Returns empty List if no results are found
     */
	public static List<Contentlet> pullRelated(String relationshipName, String contentletIdentifier,
			String condition, boolean pullParents, int limit, String sort, User user, String tmDate,
			final long language, final Boolean live) {

		return pullRelated(relationshipName, contentletIdentifier, condition, pullParents, limit,
				-1, sort, user, tmDate, language, live);
	}

	/**
	 * Will return a ContentMap object which can be used on dotCMS front end. This method is better
	 * than the old #pullRelatedContent macro because it doesn't have to parse all the velocity
	 * content object that are returned.  If you are building large pulls and depending on the types
	 * of fields on the content this can get expensive especially with large data sets.<br />
	 * EXAMPLE:<br /> #foreach($con in $dotcontent.pullRelated('myRelationship','asbd-asd-asda-asd','+myField:someValue',false,5,'modDate
	 * desc'))<br /> $con.title<br /> #end<br /> The method will figure out language, working and
	 * live for you if not passed in with the condition Returns empty List if no results are found
	 *
	 * @param relationshipName - Name of the relationship as defined in the structure.
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be
	 * Null
	 * @param pullParents Should the related pull be based on Parents or Children
	 * @param limit 0 is the dotCMS max limit which is 10000. Be careful when searching for unlimited
	 * amount as all content will load into memory
	 * @param offset result slice (how many records from the first one)
	 * @param sort - Velocity variable name to sort by.  this is a string and can contain multiple
	 * values "sort1 acs, sort2 desc". Can be Null
	 * @param user
	 * @param tmDate - Time Machine date
	 * @param language - Language ID to be used to filter results
	 * @param live - Boolean to filter live/non-live content
	 * @return Returns empty List if no results are found
	 */
	public static List<Contentlet> pullRelated(String relationshipName, String contentletIdentifier,
			String condition, boolean pullParents, int limit, int offset, String sort, User user, String tmDate,
			final long language, final Boolean live) {
		final Relationship relationship = APILocator.getRelationshipAPI()
				.byTypeValue(relationshipName);

		return getPullResults(relationship, contentletIdentifier, condition, limit, offset, sort,
				user, tmDate, pullParents, language, live);
	}

    /**
     * Logic used for `pullRelated` and `pullRelatedField` methods
     */
    public static List<Contentlet> getPullResults(final Relationship relationship,
            String contentletIdentifier, final String condition, final int limit, int offset,
            String sort, final User user, final String tmDate, final boolean pullParents,
            final long language, final Boolean live) {


        final boolean selfRelated = APILocator.getRelationshipAPI().sameParentAndChild(relationship);
        final String relationshipName = relationship.getRelationTypeValue();
        contentletIdentifier = RecurrenceUtil.getBaseEventIdentifier(contentletIdentifier);

        try {
            final Contentlet contentlet = conAPI
                .findContentletByIdentifierAnyLanguage(contentletIdentifier);

            if (UtilMethods.isSet(condition)){

                final StringBuilder pullQuery = new StringBuilder();

                if (language != -1 && !condition.contains("languageId")){
                    pullQuery.append(" ").append("+languageId:").append(language).append(" ");
                }
                if (!user.isBackendUser()){
                    pullQuery.append("+live:true ");
                } else if (live !=null){
                    pullQuery.append("+live:").append(live).append(" ");
                }

                pullQuery.append(condition);

                if ((selfRelated && !pullParents) || (!selfRelated && relationship
                        .getParentStructureInode().equals(contentlet.getContentTypeId()))) {
                    //pulling children
                    final List<Contentlet> relatedContent = conAPI
                            .getRelatedContent(contentlet, relationship, !pullParents, user,
                                    true, -1, -1, sort, language, live);

                    if (relatedContent.isEmpty()) {
                        return Collections.emptyList();
                    }

                    pullQuery.append(" +identifier:(").append(String.join(" OR ", relatedContent
                            .stream().map(cont -> cont.getIdentifier()).collect(
                                    Collectors.toList()))).append(")");

                    final List<String> results = conAPI.searchIndex(pullQuery.toString(), -1,-1, sort, user, true)
                                    .stream()
                                    .map(cs-> cs.getIdentifier()).collect(Collectors.toList());

					final List<Contentlet> filteredList = relatedContent.stream().filter(c->results.contains(c.getIdentifier()))
							.collect(Collectors.toList());

					offset = offset >=0 ? offset : 0;
					return filteredList.subList(offset, (limit <= 0 || limit >= filteredList.size() ) ? filteredList.size() : offset+limit);
                } 
                
                //pulling parents
                pullQuery.append(" +" + relationshipName + ":" + contentletIdentifier);
                
                return pull(pullQuery.toString(), offset, limit, sort, user, tmDate, true);

            } else {
                return conAPI
                        .getRelatedContent(contentlet, relationship, !pullParents, user, true, limit, offset, sort, language, live);
            }

        } catch (Exception e) {
            // throw stack when admin
            if(PageMode.get().isAdmin) {
                Logger.warn(ContentUtils.class,
                                "Error pullRelated identifier " + contentletIdentifier
                                        + ". Relationship: " + relationshipName + " : " + e.getMessage(), e);
            }
            else {
            Logger.warnAndDebug(ContentUtils.class,
                    "Error pullRelated identifier " + contentletIdentifier
                            + ". Relationship: " + relationshipName + " : " + e.getMessage(), e);
            }
        }

        return Collections.emptyList();
    }

    /**
	 * @deprecated This method does not work for self related content. Use another pullRelatedField implementation in {@link ContentUtils}
     * Returns a list of related content given a Relationship and additional filtering criteria
	 * @param relationship
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be Null
	 * @param limit - 0 is the dotCMS max limit which is 10000. Be careful when searching for unlimited amount as all content will load into memory
	 * @param offset - Starting position of the resulting list. -1 is the default value and the first results of the pagination are returned
	 * @param sort - Velocity variable name to sort by.  This is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
	 * @param user
	 * @param tmDate
	 * @return Returns empty List if no results are found
	 */
    @Deprecated
	public static List<Contentlet> pullRelatedField(final Relationship relationship,
			final String contentletIdentifier, final String condition, final int limit,
			final int offset, final String sort, final User user, final String tmDate) {
		return getPullResults(relationship, contentletIdentifier, condition, limit, offset, sort,
				user, tmDate, false, -1, null);
	}

    /**
     * Returns a list of related content given a Relationship and additional filtering criteria
     * @param relationship
     * @param contentletIdentifier
     * @param condition
     * @param limit
     * @param offset
     * @param sort
     * @param user
     * @param tmDate
     * @param language
     * @param live
     * @return
     */
    public static List<Contentlet> pullRelatedField(final Relationship relationship,
            final String contentletIdentifier, final String condition, final int limit,
            final int offset, final String sort, final User user, final String tmDate, final boolean pullParents,
            final long language, final Boolean live) {
        return getPullResults(relationship, contentletIdentifier, condition, limit, offset, sort,
                user, tmDate, pullParents, language, live);
    }

    /**
     *
     * @param relationship
     * @param contentletIdentifier
     * @param condition
     * @param limit
     * @param offset
     * @param sort
     * @param user
     * @param tmDate
     * @param pullParents
     * @return
     */
    public static List<Contentlet> pullRelatedField(final Relationship relationship,
            final String contentletIdentifier, final String condition, final int limit,
            final int offset, final String sort, final User user, final String tmDate, final boolean pullParents) {
        return getPullResults(relationship, contentletIdentifier, condition, limit, offset, sort,
                user, tmDate, pullParents, -1, null);
    }

	public static String addDefaultsToQuery(String query, final boolean editOrPreviewMode, final
			HttpServletRequest request){
		String q = "";

		if(query != null)
			q = query;
		else
			query = q;

		if(!query.contains("languageId")){
			q += " +languageId:" + WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
		}

		if(!(query.contains("live:") || query.contains("working:") )){
			if(editOrPreviewMode && !TimeMachineUtil.isRunning()){
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


	/**
	 * Adds the relationships to the contentlet based on the request parameters depth
	 * @param contentlet
	 * @param user
	 * @param mode
	 * @param languageId
	 */
	public static void addRelationships(final Contentlet contentlet, final User user, final PageMode mode, final long languageId) {
		final HttpServletRequest  request  = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();
		if (addRelationshipsOnPage &&
				Objects.nonNull(response) &&
				Objects.nonNull(request) &&
				Objects.nonNull(user)) {

			final String depthParam =
					Objects.nonNull(request.getParameter(WebKeys.HTMLPAGE_DEPTH)) ?
							request.getParameter(WebKeys.HTMLPAGE_DEPTH) :
							(String) request.getAttribute(WebKeys.HTMLPAGE_DEPTH);
			if (Objects.nonNull(depthParam)) {
				final int depth = ConversionUtils.toInt(depthParam, -1);
				addRelationships(contentlet, user, mode, languageId, depth, request, response);
			}
		}
	}
	public static void addRelationships(final Contentlet contentlet, final User user, final PageMode mode,
										final long languageId, final int depth) {

		final HttpServletRequest  request  = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();
		if (addRelationshipsOnPage &&
				Objects.nonNull(response) &&
				Objects.nonNull(request) &&
				Objects.nonNull(user)) {

			addRelationships(contentlet, user, mode, languageId, depth, request, response);
		}
 	}

	/**
	 * Adds the relationships to the contentlet based on depth argument
	 * @param contentlet
	 * @param user
	 * @param mode
	 * @param languageId
	 * @param depth
	 * @param request
	 * @param response
	 */
	public static void addRelationships(final Contentlet contentlet, final User user, final PageMode mode,
										final long languageId, final int depth, final HttpServletRequest  request,
										final HttpServletResponse response) {

		if (depth >= 0 && depth <= 3) {

			try {

				final JSONObject jsonWithRelationShips = ContentHelper.getInstance().addRelationshipsToJSON(request, response,
						request.getParameter("render"), user, depth, mode.respectAnonPerms, contentlet,
						new JSONObject(), null, languageId, mode.showLive, false,
						true);

				final HashMap<String,Object> relationshipsMap = DotObjectMapperProvider.getInstance()
						.getDefaultObjectMapper().readValue(jsonWithRelationShips.toString(), HashMap.class);

				if (UtilMethods.isSet(relationshipsMap)) {
					contentlet.getMap().putAll(relationshipsMap);
				}
			} catch (Exception e) {

				Logger.error(ContentUtils.class, "Error, contentlet id:" +
						contentlet.getIdentifier() + ", msg:" + e.getMessage(), e);
				throw new DotRuntimeException(e);
			}
		} else {

			throw new IllegalArgumentException("Depth must be a number between 0 and 3");
		}

	}
		
}
