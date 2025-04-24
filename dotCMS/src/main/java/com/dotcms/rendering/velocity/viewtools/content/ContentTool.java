package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.visitor.domain.Visitor;
import com.dotcms.visitor.domain.Visitor.AccruedTag;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

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

	private UserWebAPI userAPI;

	private HttpServletRequest req;
	private User user = null;

	private boolean EDIT_OR_PREVIEW_MODE=false;
	private String tmDate;
	private Context context;
	private Host currentHost;
	private PageMode mode;
	private Language language;

	public void init(Object initData) {
		this.req = ((ViewContext) initData).getRequest();

		user = getUser(req);

		this.context = ((ViewContext) initData).getVelocityContext();

		tmDate=null;
		language = WebAPILocator.getLanguageWebAPI().getLanguage(req);
		HttpSession session = req.getSession(false);
		mode = PageMode.get(req);
		EDIT_OR_PREVIEW_MODE=!mode.showLive;
		if(session!=null){
			tmDate = (String) session.getAttribute("tm_date");
			boolean tm=tmDate!=null;

		}
		try{
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		}catch(Exception e){
			Logger.error(this, "Error finding current host", e);
		}
	}

	/**
	 * Will load a lazy version of the content map based on the inode or identifier. It will always
	 * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
	 * identifier.  If it is an inode this is ignored.
	 * Will return NULL if not found
	 * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
	 * @return NULL if not found
	 */
	public LazyLoaderContentMap load(final String inodeOrIdentifier) {

		return new LazyLoaderContentMap(()-> find(inodeOrIdentifier));
	}

	/**
	 * Will pull a single piece of content for you based on the inode or identifier. It will always
	 * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
	 * identifier.  If it is an inode this is ignored.
	 * In this case the object will be hydrated with all properties associated to the contentlet
	 * Will return NULL if not found
	 * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
	 * @return NULL if not found
	 */
	public ContentMap findHydrated(String inodeOrIdentifier) {

		return find(inodeOrIdentifier, true);
	}

	/**
	 * Will pull a single piece of content for you based on the inode or identifier. It will always
	 * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
	 * identifier.  If it is an inode this is ignored.
	 * Will return NULL if not found
	 * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
	 * @return NULL if not found
	 */
	public ContentMap find(String inodeOrIdentifier) {

		return find(inodeOrIdentifier, false);
	}

	/**
	 * Will pull a single piece of content for you based on the inode or identifier. It will always
	 * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
	 * identifier.  If it is an inode this is ignored.
	 * Will return NULL if not found
	 * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
	 * @param hydrateRelated Should the content be hydrated with all properties associated to the contentlet
	 * @return NULL if not found
	 */
	public ContentMap find(String inodeOrIdentifier, final boolean hydrateRelated) {
		final long sessionLang = language.getId();
	    try {
    		Contentlet c = ContentUtils.find(inodeOrIdentifier, user, EDIT_OR_PREVIEW_MODE, sessionLang);
    		if(c== null || !InodeUtils.isSet(c.getInode())){
    			return null;
    		}

			if(hydrateRelated) {
				final DotContentletTransformer myTransformer = new DotTransformerBuilder()
						.hydratedContentMapTransformer().content(c).build();
				c = myTransformer.hydrate().get(0);
			}

    		return new ContentMap(c, user, EDIT_OR_PREVIEW_MODE,currentHost,context);
	    }
	    catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.find. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
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
        int l = Integer.valueOf(limit);
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
	public List<ContentMap> pull(String query,int limit, String sort){
	    return pull(query,-1,limit,sort);
	}
	
	public PaginatedArrayList<ContentMap> pull(String query, int offset,int limit, String sort){
	    try {
    	    PaginatedArrayList<ContentMap> ret = new PaginatedArrayList<>();

    	    PaginatedArrayList<Contentlet> cons = ContentUtils.pull(
    	    		ContentUtils.addDefaultsToQuery(query, EDIT_OR_PREVIEW_MODE, req),
					offset, limit, sort, user, tmDate);
    	    for(Contentlet cc : cons) {
    	    	ret.add(new ContentMap(cc,user,EDIT_OR_PREVIEW_MODE,currentHost,context));
    	    }
    	    ret.setQuery(cons.getQuery());
			ret.setTotalResults(cons.getTotalResults());
    		return ret;
	    }
	    catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.pull. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
        }
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
		return pull(query, offset, limit, sort);
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
	public PaginatedContentList<ContentMap> pullPerPage(String query, int currentPage, int contentsPerPage, String sort){
		PaginatedContentList<ContentMap> ret = new PaginatedContentList<>();
		try {
			final PaginatedContentList<Contentlet> cons = ContentUtils.pullPerPage(
					ContentUtils.addDefaultsToQuery(query, EDIT_OR_PREVIEW_MODE, req), currentPage,
					contentsPerPage, sort, user, tmDate);
    	    for(Contentlet cc : cons) {
    	    	ret.add(new ContentMap(cc,user,EDIT_OR_PREVIEW_MODE,currentHost,context));
    	    }

			ret.setQuery(cons.getQuery());
			ret.setLimit(cons.getLimit());
			ret.setOffset(cons.getOffset());
			ret.setCurrentPage(cons.getCurrentPage());
			ret.setTotalResults(cons.getTotalResults());
			ret.setTotalPages(cons.getTotalPages());
			ret.setNextPage(cons.isNextPage());
			ret.setPreviousPage(cons.isPreviousPage());
		}
		catch(Throwable ex) {
		    if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
		        Logger.error(this,"error in ContentTool.pullPerpage. URL: "+req.getRequestURL().toString(),ex);
		    }
		    throw new RuntimeException(ex);
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
	    try {
	        return ContentUtils.query(ContentUtils.addDefaultsToQuery(query, EDIT_OR_PREVIEW_MODE, req), limit, user, sort);
	    }
	    catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.query. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
        }
	}
	
	/**
	 * Use this method to return the number of contents which match a particular query. 
	 * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
	 * @return
	 */
	public long count(String query) {
	    try {
	        return ContentUtils.count(query, user, tmDate);
	    }
	    catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.count. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
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
     * @param limit 0 is the dotCMS max limit which is 10000. Be careful when searching for
     * unlimited amount as all content will load into memory
     * @param sort - Velocity variable name to sort by.  This is a string and can contain multiple
     * values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    public List<ContentMap> pullRelated(final String relationshipName, final String contentletIdentifier,
            final String condition, final boolean pullParents, final int limit, final String sort) {
        try {
            final PaginatedArrayList<ContentMap> ret = new PaginatedArrayList<>();

			long langId = UtilMethods.isSet(condition) && condition.contains("languageId") ? -1 :
					language.getId();

            final List<Contentlet> cons = ContentUtils
                    .pullRelated(relationshipName, contentletIdentifier,
                            condition == null ? condition : ContentUtils.addDefaultsToQuery(condition, EDIT_OR_PREVIEW_MODE, req),
                            pullParents,
                            limit, sort, user, tmDate, langId,
                            EDIT_OR_PREVIEW_MODE ? null : true);

            for (Contentlet cc : cons) {
                ret.add(new ContentMap(cc, user, EDIT_OR_PREVIEW_MODE, currentHost, context));
            }
            return ret;
        } catch (Throwable ex) {
            if (Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,
                        "error in ContentTool.pullRelated. URL: " + req.getRequestURL().toString(),
                        ex);
            }
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns a list of related content given a RelationshipField and additional filtering
     * criteria
     *
     * @param contentletIdentifier - Identifier of the contentlet
     * @param fieldVariable - Full field variable (including the content type variable, ie.:
     * news.youtubes where 'news' is the content type variable and 'youtubes' is the field
     * variable)
     * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be
     * Null
     * @param limit - 0 is the dotCMS max limit which is 10000. Be careful when searching for
     * unlimited amount as all content will load into memory
     * @param offset - Starting position of the resulting list. -1 is the default value and the
     * first results of the pagination are returned
     * @param sort - Velocity variable name to sort by.  This is a string and can contain multiple
     * values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    public List<ContentMap> pullRelatedField(String contentletIdentifier, String fieldVariable,
            String condition, int limit, int offset, String sort) {
        try {
            PaginatedArrayList<ContentMap> ret = new PaginatedArrayList();

            if (!fieldVariable.contains(StringPool.PERIOD)) {
                final String message = "Invalid field variable " + fieldVariable;
                if (Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                    Logger.error(this, message);
                }
                throw new RuntimeException(new DotDataValidationException(message));
            }

            final ContentType contentType = APILocator.getContentTypeAPI(user)
                    .find(fieldVariable.split("\\" + StringPool.PERIOD)[0]);
            final Field field = APILocator.getContentTypeFieldAPI()
                    .byContentTypeAndVar(contentType,
                            fieldVariable.split("\\" + StringPool.PERIOD)[1]);
            final Relationship relationship = APILocator.getRelationshipAPI()
                    .getRelationshipFromField(field, user);

            final boolean pullParents = APILocator.getRelationshipAPI().isParentField(relationship, field);
            List<Contentlet> cons = ContentUtils
                    .pullRelatedField(relationship, contentletIdentifier,
							ContentUtils.addDefaultsToQuery(condition, EDIT_OR_PREVIEW_MODE, req), limit, offset, sort, user, tmDate, pullParents,
                            language.getId(), EDIT_OR_PREVIEW_MODE ? null : true);

            for (Contentlet cc : cons) {
                ret.add(new ContentMap(cc, user, EDIT_OR_PREVIEW_MODE, currentHost, context));
            }
            return ret;
        } catch (Throwable ex) {
            if (Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,
                        "error in ContentTool.pullRelated. URL: " + req.getRequestURL().toString(),
                        ex);
            }
            throw new RuntimeException(ex);
        }
    }

	/**
	 * Returns a list of related content given a RelationshipField and additional filtering criteria
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param fieldVariable - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be Null
	 * @param limit - 0 is the dotCMS max limit which is 10000. Be careful when searching for unlimited amount as all content will load into memory
	 * @param sort - Velocity variable name to sort by.  This is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
	 * @return Returns empty List if no results are found
	 */
	public List<ContentMap> pullRelatedField(String contentletIdentifier, String fieldVariable,
			String condition, int limit, String sort) {
		return pullRelatedField(contentletIdentifier, fieldVariable, condition, limit, -1, sort);
	}

	/**
	 * Returns a list of related content given a RelationshipField and additional filtering criteria
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param fieldVariable - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be Null
	 * @param sort - Velocity variable name to sort by.  This is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
	 * @return Returns empty List if no results are found
	 */
	public List<ContentMap> pullRelatedField(String contentletIdentifier, String fieldVariable,
			String condition, String sort) {
		return pullRelatedField(contentletIdentifier, fieldVariable, condition, 0, sort);
	}

	/**
	 * Returns a list of related content given a RelationshipField and additional filtering criteria
	 * @param contentletIdentifier - Identifier of the contentlet
	 * @param fieldVariable - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
	 * @param condition - Extra conditions to add to the query. like +title:Some Title.  Can be Null
	 * @return Returns empty List if no results are found
	 */
	public List<ContentMap> pullRelatedField(String contentletIdentifier, String fieldVariable,
			String condition) {
		return pullRelatedField(contentletIdentifier, fieldVariable, condition, null);
	}
	
	public List<ContentMap> pullPersonalized(String query, int limit, int offset, String secondarySort) {	
		try {
			
			query=addPersonalizationToQuery(query);
			String sort = secondarySort==null ? "score" : "score " + secondarySort;
			if (offset > 0){
				return pullPerPage(query, offset, limit, sort);
			}else{
				return pull(query, offset, limit, sort);
			}
		}
		catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.pullRelated. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
        }
	
	}
	
	public List<ContentMap> pullPersonalized(String query, int limit) {	
		return pullPersonalized(query, limit, 0, null);
	}
	
	public List<ContentMap> pullPersonalized(String query, int limit, String secondarySort) {	
		return pullPersonalized(query, limit, 0, secondarySort);
	}
	public List<ContentMap> pullPersonalized(String query, String limitStr, String secondarySort) {	
		
		
		int limit = Integer.parseInt(limitStr);
		
		return pullPersonalized(query, limit, 0, secondarySort);
		
	}
	public List<ContentMap> pullPersonalized(String query, String limitStr) {	
		
		int limit = Integer.parseInt(limitStr);
		
		return pullPersonalized(query, limit, 0, null);
	}
	
    private String addPersonalizationToQuery(String query) {
        Optional<Visitor> opt = APILocator.getVisitorAPI().getVisitor(this.req);
        if (opt.isEmpty() || query == null) {
            return query;
        }
        query = query.toLowerCase();

        // if we are already personalized
        if (query.indexOf(" tags:") > -1) {
            return query;
        }

        final StringWriter buff = new StringWriter().append(query);
        final Visitor visitor = opt.get();
        final IPersona p = visitor.getPersona();
        final String keyTag = (p == null) ? null : p.getKeyTag().toLowerCase();
        final Map<String, Float> personas = visitor.getWeightedPersonas();


        final List<AccruedTag> tags = visitor.getAccruedTags();
        if (p == null && (tags == null || tags.size() == 0)) {
            return query;
        }

        int maxBoost = Config.getIntProperty("PULLPERSONALIZED_PERSONA_WEIGHT", 100);

        // make personas more powerful than the most powerful tag
        if (!tags.isEmpty()) {
            maxBoost = tags.get(0).getCount() + maxBoost;
        }


        if (Config.getBooleanProperty("PULLPERSONALIZED_USE_MULTIPLE_PERSONAS", true)) {

            if (personas != null && !personas.isEmpty()) {
                for (Map.Entry<String, Float> map : personas.entrySet()) {
                    int boostMe = Math.round(maxBoost * map.getValue());
                    if (map.getKey().equals(keyTag)) {
                        boostMe = boostMe + Config.getIntProperty("PULLPERSONALIZED_LAST_PERSONA_WEIGHT", 0);
                    }

                    buff.append(" tags:\"" + map.getKey().toLowerCase() + "\"^" + boostMe);
                }
            }


        } else {
            if (p != null) {
                buff.append(" tags:\"" + keyTag + "\"^" + maxBoost);
            }
        }



        for (AccruedTag tag : tags) {
            buff.append(" tags:\"" + tag.getTag().toLowerCase() + "\"^" + (tag.getCount() + 1) + " ");
        }

        return buff.toString();
    }
	
    /**
     * Gets the top viewed contents identifiers and numberOfViews  for a particular structure for a specified date interval
     * 
     * @param structureVariableName
     * @param startDate
     * @param endDate
     * @return
     */
	public List<Map<String, String>> getMostViewedContent(String structureVariableName, String startDate, String endDate) {
	    try {
	        return APILocator.getContentletAPI().getMostViewedContent(structureVariableName, startDate, endDate, user);
	    }
	    catch(Throwable ex) {
            if(Config.getBooleanProperty("ENABLE_FRONTEND_STACKTRACE", false)) {
                Logger.error(this,"error in ContentTool.getMostViewedContent. URL: "+req.getRequestURL().toString(),ex);
            }
            throw new RuntimeException(ex);
        }
	}
}
