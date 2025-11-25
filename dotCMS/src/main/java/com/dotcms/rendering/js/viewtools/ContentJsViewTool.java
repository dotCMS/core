package com.dotcms.rendering.js.viewtools;


import com.dotcms.rendering.js.JsHttpServletRequestAware;
import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rendering.velocity.viewtools.content.ContentTool;
import com.dotcms.rendering.velocity.viewtools.content.LazyLoaderContentMap;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.content.ContentTool} (dotcontent) into the JS context.
 *
 * @author jsanca
 */
public class ContentJsViewTool implements JsViewTool, JsViewContextAware, JsHttpServletRequestAware {

    private final ContentTool contentTool = new ContentTool();
    private HttpServletRequest request;

    @Override
    public void setViewContext(final ViewContext viewContext) {

        contentTool.init(viewContext);
    }

    @Override
    public String getName() {
        return "dotcontent";
    }

    protected LazyLoaderContentMap loadInternal(final String inodeOrIdentifier) {

        return this.contentTool.load(inodeOrIdentifier);
    }

    @HostAccess.Export
    /**
     * Will load a lazy version of the content map based on the inode or identifier. It will always
     * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
     * identifier.  If it is an inode this is ignored.
     * Will return NULL if not found
     * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
     * @return NULL if not found
     */
    public Object load(final String inodeOrIdentifier) {

        return JsProxyFactory.createProxy(this.loadInternal(inodeOrIdentifier));
    }


    protected ContentMap findInternal(final String inodeOrIdentifier) {

        return this.contentTool.find(inodeOrIdentifier);
    }

    @HostAccess.Export
    /**
     * Will pull a single piece of content for you based on the inode or identifier. It will always
     * try to retrieve the live content unless in EDIT_MODE in the backend of dotCMS when passing in an
     * identifier.  If it is an inode this is ignored.
     * Will return NULL if not found
     * @param inodeOrIdentifier Can be either an Inode or Identifier of content.
     * @return NULL if not found
     */
    public Object find(final String inodeOrIdentifier) {

        return JsProxyFactory.createProxy(this.findInternal(inodeOrIdentifier));
    }

    @HostAccess.Export
    /**
     * Will retrieve the content type by inode or variable name
     * @param inodeOrVariable Can be either an Inode or variable name of type.
     * @return NULL if not found
     */
    public Object loadType (final String inodeOrVariable) {

        final User user = WebAPILocator.getUserWebAPI().getLoggedInUser(this.request);
        return JsProxyFactory.createProxy(
                Try.of(()->APILocator.getContentTypeAPI(user).find(inodeOrVariable)).getOrNull());
    }

    @HostAccess.Export
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
    public Object pull(final String query, final String limit, final String sort) {
        return JsProxyFactory.createProxy(this.pullInternal(query, limit, sort));
    }

    protected List<ContentMap> pullInternal(final String query, final String limit, final String sort) {
        return contentTool.pull(query, limit, sort);
    }

    @HostAccess.Export
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
    public Object pull(final String query, final int limit, final String sort) {
        return JsProxyFactory.createProxy(this.pullInternal(query, limit, sort));
    }

    protected List<ContentMap> pullInternal(final String query, final int limit, final String sort) {
        return contentTool.pull(query, limit, sort);
    }

    protected PaginatedArrayList<ContentMap> pullInternal(final String query, final int offset, final int limit, final String sort) {

        return contentTool.pull(query, offset, limit, sort);
    }

    @HostAccess.Export
    public Object pull(final String query, final int offset, final int limit, final String sort) {

        return JsProxyFactory.createProxy(this.pullInternal(query, offset, limit, sort));
    }

    /**
     * Will return a ContentMap object which can be used on dotCMS front end.
     * This method is better then the old #pullcontent macro because it doesn't have to
     * parse all the velocity content object that are returned.  If you are building large pulls
     * and depending on the types of fields on the content this can get expensive especially
     * with large data sets.<br />
     * EXAMPLE:<br />
     * #foreach($con in $dotcontent.pullPagenated('+structureName:newsItem',20,20,'modDate desc'))<br />
     * $con.headline<br />
     * #end<br />
     * Returns empty List if no results are found <br />
     * there is a totalResults avialible to you on the returned list. $retList.totalResults
     *
     * @param query  - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
     * @param limit  0 is the dotCMS max limit which is 10000. Becareful when searching for unlimited amount as all content will load into memory
     * @param offset offset to start the results from
     * @param sort   - Velocity variable name to sort by.  this is a string and can contain multiple values "sort1 acs, sort2 desc"
     * @return Returns empty List if no results are found
     */
    protected PaginatedArrayList<ContentMap> pullPaginatedInternal(final String query, final int limit,
                                                                   final int offset, final String sort) {
        return contentTool.pullPagenated(query, limit, offset, sort);
    }

    @HostAccess.Export
    public Object pullPaginated(final String query, final int limit, final int offset, final String sort) {

        return JsProxyFactory.createProxy(this.pullPaginatedInternal(query, offset, limit, sort));
    }

    @HostAccess.Export
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
    public Object pullPerPage(final String query, final int currentPage,
                              final int contentsPerPage, final String sort) {

        return JsProxyFactory.createProxy(this.pullPerPageInternal(query, currentPage, contentsPerPage, sort));
    }

    protected PaginatedContentList<ContentMap> pullPerPageInternal(final String query, final int currentPage,
                                                                   final int contentsPerPage, final String sort) {

        return this.contentTool.pullPerPage(query, currentPage, contentsPerPage, sort);
    }

    @HostAccess.Export
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
    public Object query(final String query, final int limit) {
        return JsProxyFactory.createProxy(this.queryInternal(query, limit));
    }

    protected List<ContentletSearch> queryInternal(final String query, final int limit) {
        return contentTool.query(query, limit);
    }

    @HostAccess.Export
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
    public Object query(final String query, final int limit, final String sort) {
        return JsProxyFactory.createProxy(this.queryInternal(query, limit, sort));
    }

    protected List<ContentletSearch> queryInternal(final String query, final int limit, final String sort) {
        return contentTool.query(query, limit, sort);
    }

    @HostAccess.Export
    /**
     * Use this method to return the number of contents which match a particular query.
     * @param query - Lucene Query used to search for content - Will append live, working, deleted, and language if not passed
     * @return
     */
    public long count(final String query) {
        return contentTool.count(query);
    }

    @HostAccess.Export
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
    public Object pullRelated(final String relationshipName, final String contentletIdentifier,
                              final boolean pullParents, final int limit) {
        return JsProxyFactory.createProxy(this.pullRelatedInternal(relationshipName, contentletIdentifier, pullParents, limit));
    }

    protected List<ContentMap> pullRelatedInternal(final String relationshipName, final String contentletIdentifier,
                                                   final boolean pullParents, final int limit) {

        return this.contentTool.pullRelated(relationshipName, contentletIdentifier, pullParents, limit);
    }

    @HostAccess.Export
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
    public Object pullRelated(final String relationshipName, final String contentletIdentifier,
                              final boolean pullParents, final int limit, final String sort) {
        return JsProxyFactory.createProxy(pullRelatedInternal(relationshipName, contentletIdentifier, pullParents, limit, sort));
    }

    public List<ContentMap> pullRelatedInternal(final String relationshipName, final String contentletIdentifier,
                                                final boolean pullParents, final int limit, final String sort) {
        return contentTool.pullRelated(relationshipName, contentletIdentifier, pullParents, limit, sort);
    }

    @HostAccess.Export
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
    public Object pullRelated(final String relationshipName, final String contentletIdentifier,
                              final String condition, final boolean pullParents, final int limit, final String sort) {
        return JsProxyFactory.createProxy(this.pullRelatedInternal(relationshipName, contentletIdentifier, condition, pullParents, limit, sort));
    }

    protected List<ContentMap> pullRelatedInternal(final String relationshipName, final String contentletIdentifier,
                                                   final String condition, final boolean pullParents, final int limit, final String sort) {
        return contentTool.pullRelated(relationshipName, contentletIdentifier, condition, pullParents, limit, sort);
    }

    @HostAccess.Export
    /**
     * Returns a list of related content given a RelationshipField and additional filtering
     * criteria
     *
     * @param contentletIdentifier - Identifier of the contentlet
     * @param fieldVariable        - Full field variable (including the content type variable, ie.:
     *                             news.youtubes where 'news' is the content type variable and 'youtubes' is the field
     *                             variable)
     * @param condition            - Extra conditions to add to the query. like +title:Some Title.  Can be
     *                             Null
     * @param limit                - 0 is the dotCMS max limit which is 10000. Be careful when searching for
     *                             unlimited amount as all content will load into memory
     * @param offset               - Starting position of the resulting list. -1 is the default value and the
     *                             first results of the pagination are returned
     * @param sort                 - Velocity variable name to sort by.  This is a string and can contain multiple
     *                             values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    public Object pullRelatedField(final String contentletIdentifier, final String fieldVariable,
                                   final String condition, final int limit, final int offset, final String sort) {

        return JsProxyFactory.createProxy(this.pullRelatedFieldInternal(contentletIdentifier, fieldVariable, condition, limit, offset, sort));
    }

    protected List<ContentMap> pullRelatedFieldInternal(final String contentletIdentifier, final String fieldVariable,
                                                        final String condition, final int limit, final int offset, final String sort) {
        return this.contentTool.pullRelatedField(contentletIdentifier, fieldVariable, condition, limit, offset, sort);
    }

    @HostAccess.Export
    /**
     * Returns a list of related content given a RelationshipField and additional filtering criteria
     *
     * @param contentletIdentifier - Identifier of the contentlet
     * @param fieldVariable        - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
     * @param condition            - Extra conditions to add to the query. like +title:Some Title.  Can be Null
     * @param limit                - 0 is the dotCMS max limit which is 10000. Be careful when searching for unlimited amount as all content will load into memory
     * @param sort                 - Velocity variable name to sort by.  This is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    public Object pullRelatedField(final String contentletIdentifier, final String fieldVariable,
                                   final String condition, final int limit, final String sort) {
        return JsProxyFactory.createProxy(this.pullRelatedFieldInternal(contentletIdentifier, fieldVariable, condition, limit, sort));
    }

    public List<ContentMap> pullRelatedFieldInternal(final String contentletIdentifier, final String fieldVariable,
                                                     final String condition, final int limit, final String sort) {
        return contentTool.pullRelatedField(contentletIdentifier, fieldVariable, condition, limit, sort);
    }

    @HostAccess.Export
    /**
     * Returns a list of related content given a RelationshipField and additional filtering criteria
     *
     * @param contentletIdentifier - Identifier of the contentlet
     * @param fieldVariable        - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
     * @param condition            - Extra conditions to add to the query. like +title:Some Title.  Can be Null
     * @param sort                 - Velocity variable name to sort by.  This is a string and can contain multiple values "sort1 acs, sort2 desc". Can be Null
     * @return Returns empty List if no results are found
     */
    public Object pullRelatedField(final String contentletIdentifier, final String fieldVariable,
                                             final String condition, final String sort) {
        return JsProxyFactory.createProxy(this.pullRelatedFieldInternal(contentletIdentifier, fieldVariable, condition, sort));
    }

    public List<ContentMap> pullRelatedFieldInternal(final String contentletIdentifier, final String fieldVariable,
                                                     final String condition, final String sort) {

        return contentTool.pullRelatedField(contentletIdentifier, fieldVariable, condition, sort);
    }

    @HostAccess.Export
    /**
     * Returns a list of related content given a RelationshipField and additional filtering criteria
     *
     * @param contentletIdentifier - Identifier of the contentlet
     * @param fieldVariable        - Full field variable (including the content type variable, ie.: news.youtubes where 'news' is the content type variable and 'youtubes' is the field variable)
     * @param condition            - Extra conditions to add to the query. like +title:Some Title.  Can be Null
     * @return Returns empty List if no results are found
     */
    public Object pullRelatedField(final String contentletIdentifier, final String fieldVariable,
                                             final String condition) {

        return JsProxyFactory.createProxy(pullRelatedFieldInternal(contentletIdentifier, fieldVariable, condition));
    }

    protected List<ContentMap> pullRelatedFieldInternal(final String contentletIdentifier, final String fieldVariable,
                                             final String condition) {
        return contentTool.pullRelatedField(contentletIdentifier, fieldVariable, condition);
    }

    @HostAccess.Export
    public Object pullPersonalized(final String query, final int limit, final int offset, final String secondarySort) {
        return JsProxyFactory.createProxy(this.pullPersonalizedInternal(query, limit, offset, secondarySort));
    }

    protected List<ContentMap> pullPersonalizedInternal(final String query, final int limit, final int offset, final String secondarySort) {

        return contentTool.pullPersonalized(query, limit, offset, secondarySort);
    }

    @HostAccess.Export
    public Object pullPersonalized(final String query, final int limit) {
        return JsProxyFactory.createProxy(this.pullPersonalizedInternal(query, limit));
    }

    protected List<ContentMap> pullPersonalizedInternal(final String query, final int limit) {
        return contentTool.pullPersonalized(query, limit);
    }

    @HostAccess.Export
    public Object pullPersonalized(final String query, final int limit, final String secondarySort) {
        return JsProxyFactory.createProxy(pullPersonalizedInternal(query, limit, secondarySort));
    }

    protected List<ContentMap> pullPersonalizedInternal(final String query, final int limit, final String secondarySort) {
        return contentTool.pullPersonalized(query, limit, secondarySort);
    }

    @HostAccess.Export
    public Object pullPersonalized(final String query, final String limitStr, final String secondarySort) {

        return JsProxyFactory.createProxy(this.pullPersonalizedInternal(query, limitStr, secondarySort));
    }

    protected List<ContentMap> pullPersonalizedInternal(final String query, final String limitStr, final String secondarySort) {

        return contentTool.pullPersonalized(query, limitStr, secondarySort);
    }

    @HostAccess.Export
    public Object pullPersonalized(final String query, final String limitStr) {

        return JsProxyFactory.createProxy(this.pullPersonalizedInternal(query, limitStr));
    }

    protected List<ContentMap> pullPersonalizedInternal(final String query, final String limitStr) {
        return this.contentTool.pullPersonalized(query, limitStr);
    }

    @HostAccess.Export
    /**
     * Gets the top viewed contents identifiers and numberOfViews  for a particular structure for a specified date interval
     *
     * @param structureVariableName
     * @param startDate
     * @param endDate
     * @return
     */
    public Object getMostViewedContent(String structureVariableName, String startDate, String endDate) {
        return JsProxyFactory.createProxy(getMostViewedContentInternal(structureVariableName, startDate, endDate));
    }

    protected List<Map<String, String>> getMostViewedContentInternal(final String structureVariableName,
                                                                  final String startDate,
                                                                  final String endDate) {
        return this.contentTool.getMostViewedContent( structureVariableName,  startDate,  endDate);
    }

    @Override
    public void setRequest(final HttpServletRequest request) {
        this.request = request;
    }
}
