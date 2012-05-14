package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.lucene.queryParser.ParseException;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.search.SearchHits;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.LuceneHits;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContentsWebAPI implements ViewTool {

	private HttpServletRequest request;
	private User user = null;
	private User backuser = null;
	private UserWebAPI userAPI;
	private PermissionAPI perAPI;
	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private static int MAX_LIMIT = 100;

	// private HttpServletRequest request;
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		userAPI = WebAPILocator.getUserWebAPI();
		perAPI = APILocator.getPermissionAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(request);
			backuser = userAPI.getLoggedInUser(request);
		} catch (Exception e) {
			Logger.error(this, "Error finding the logged in user", e);
		}
	}

	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public ContentletAPI getContentletAPI() {
		return conAPI;
	}

	public void setContentletAPI(ContentletAPI conAPI) {
		this.conAPI = conAPI;
	}


	/**
	 *
	 * @param cont
	 * @return
	 * @throws DotDataException
	 * @throws DotStateException
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public Identifier getContentIdentifier(Contentlet cont) throws DotStateException, DotDataException {
		return (Identifier) APILocator.getIdentifierAPI().find(cont);
	}

	/**
	 *
	 * @param inode
	 * @return
	 * @deprecated
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Contentlet getContentByInode(long inode) throws DotDataException, DotSecurityException {
		return getContentByInode(String.valueOf(inode));
	}

	/**
	 *
	 * @param inode
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public Contentlet getContentByInode(String inode) throws DotDataException, DotSecurityException {
		return conAPI.find(inode, user, true);
	}

	/**
	 *
	 * @param structureType
	 * @return
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public Structure getStructureByType(String structureType) {
		return StructureCache.getStructureByType(structureType);
	}

	/**
	 * Get the Structure from cache by inode
	 * @param structureInode
	 * @return Structure
	 * @author Oswaldo Gallango
	 * @version 1.0
	 * @since 1.5
	 */
	public Structure getStructureByInode(String structureInode) {
		return StructureCache.getStructureByInode(structureInode);
	}

	/**
	 *
	 * @param structure
	 * @param category
	 * @param maxResults
	 * @return
	 * @throws ParseException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public List<Contentlet> getLastestContents(Structure structure, Category category, int maxResults) throws DotDataException, DotSecurityException, ParseException {
		StringBuffer buffy = new StringBuffer();
		buffy.append("+live:true +deleted:false +structureInode:" + structure.getInode() + " +c" + category.getInode() + "c:on");
		return conAPI.search(buffy.toString(), maxResults, -1, "mod_date desc", user, true);
	}

	/**
	 *
	 * @param structureType
	 * @param categoryName
	 * @param maxResults
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public List<Contentlet> getLastestContents(String structureType, String categoryName, int maxResults) throws DotDataException, DotSecurityException, ParseException {
		Category category = categoryAPI.findByName(categoryName, user, true);
		Structure structure = StructureCache.getStructureByType(structureType);
		return getLastestContents(structure, category, maxResults);
	}

	/**
	 *
	 * @param structure
	 * @param category
	 * @return
	 * @throws ParseException
	 * @throws DotSecurityException
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public List<Contentlet> getLastestContents(Structure structure, Category category)throws DotDataException, DotSecurityException, ParseException {
		StringBuffer buffy = new StringBuffer();
		buffy.append("+live:true +deleted:false +structureInode:" + structure.getInode() + " +c" + category.getInode() + "c:on");
		return conAPI.search(buffy.toString(), 0, -1, "mod_date desc", user, true);
	}

	/**
	 *
	 * @param structureType
	 * @param categoryName
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 * @deprecated this methods was deprecated because it hits the database, try to use the lucene search methods instead.
	 */
	public List<Contentlet> getLastestContents(String structureType, String categoryName) throws DotDataException, DotSecurityException, ParseException {
		Category category = categoryAPI.findByName(categoryName, user, true);
		Structure structure = StructureCache.getStructureByType(structureType);
		return getLastestContents(structure, category);
	}

	/**
	 * This methods retrieves the field of an structure based on his presentation name
	 * @param st Structure owner of the field
	 * @param fieldName The presentation name of the field
	 * @return The field found
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead
	 */
	public Field getFieldByName(Structure st, String fieldName) {
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for (Field f : fields) {
			if (f.getFieldName().equals(fieldName))
				return f;
		}
		return new Field();
	}

	/**
	 * This methods retrieves the field of an structure based on his presentation name
	 * @param structureType The structure type name
	 * @param fieldName  The presentation name of the field
	 * @return The field found
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public Field getFieldByName(String structureType, String fieldName) {
		Structure st = StructureCache.getStructureByType(structureType);
		return getFieldByName(st, fieldName);
	}

	/**
	 * This methods retrieves the field of an structure based on his presentation name
	 * @param structureInode The structure inode
	 * @param fieldName  The presentation name of the field
	 * @return The field found
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public Field getFieldByInode(long structureInode, String fieldName) {
		return getFieldByInode(String.valueOf(structureInode), fieldName);
	}
	/**
	 * This methods retrieves the field of an structure based on his presentation name
	 * @param structureInode The structure inode
	 * @param fieldName  The presentation name of the field
	 * @return The field found
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public Field getFieldByInode(String structureInode, String fieldName) {
		Structure st = StructureCache.getStructureByInode(structureInode);
		return getFieldByName(st, fieldName);
	}

	/**
	 * This methods retrieves the field of an structure based on his logical name. E.G. Last_Name
	 * @param st Structure owner of the field
	 * @param fieldName The presentation name of the field
	 * @return The field found, an empty field if it wasn't found
	 */
	public Field getFieldByLogicalName(Structure st, String fieldName) {
		List<Field> fields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for (Field f : fields) {
			if (f.getFieldContentlet().equals(fieldName))
				return f;
		}
		return new Field();
	}

	/**
	 * This methods retrieves the field of an structure based on his logical name. E.G. Last_Name
	 * @param structureType The structure type name
	 * @param fieldName  The presentation name of the field
	 * @return The field found, an empty field if it wasn't found
	 */
	public Field getFieldByLogicalName(String structureType, String fieldName) {
		@SuppressWarnings("deprecation")
		Structure st = StructureCache.getStructureByType(structureType);
		return getFieldByLogicalName(st, fieldName);
	}

	/**
	 * This methods retrieves the field of an structure based on his logical name. E.G. Last_Name
	 * @param structureInode The structure inode
	 * @param fieldName  The presentation name of the field
	 * @return The field found, an empty field if it wasn't found
	 */
	public Field getFieldByLogicalNameAndInode(long structureInode, String fieldName) {
		return getFieldByLogicalNameAndInode(String.valueOf(structureInode),fieldName);
	}
	/**
	 * This methods retrieves the field of an structure based on his logical name. E.G. Last_Name
	 * @param structureInode The structure inode
	 * @param fieldName  The presentation name of the field
	 * @return The field found, an empty field if it wasn't found
	 */
	public Field getFieldByLogicalNameAndInode(String structureInode, String fieldName) {
		Structure st = StructureCache.getStructureByInode(structureInode);
		return getFieldByLogicalName(st, fieldName);
	}

	/**
	 *
	 * @param fieldName
	 * @param content
	 * @return
	 * @deprecated Try using the #getContentDetail or #getContentDetailByIdentifier macros to retrieve the fields of a content.
	 */
	public Object getFieldValue(String fieldName, Contentlet content) {
		Structure structure = StructureCache.getStructureByInode(content.getStructureInode());
		Field theField = null;
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		for (Field field : fields) {
			if (field.getFieldName().equals(fieldName)) {
				theField = field;
				break;
			}
		}
		if (theField == null)
			return null;
		try {
			return PropertyUtils.getProperty(content, theField.getFieldContentlet());
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}


	/**
	 *
	 * @param structure
	 * @param fieldName
	 * @param fieldValue
	 * @return
	 * @throws ParseException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public List<Contentlet> getContentsByStructureAndFieldValue(Structure structure, String fieldName, String fieldValue) throws DotDataException, DotSecurityException, ParseException {
		Field field = structure.getField(fieldName);
		StringBuffer buffy = new StringBuffer();
		buffy.append("+live:true +deleted:false +structureInode:" + structure.getInode() + " +" + field.getFieldName() + ":" + fieldValue);
		return conAPI.search(buffy.toString(), 0, -1, "mod_date", user, true);
	}

	/**
	 *
	 * @param structure
	 * @param orderFieldName
	 * @param direction
	 * @param rowNumber
	 * @return
	 * @throws ParseException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public List<Contentlet> getContentletsByStructureAndOrder(Structure structure, String orderFieldName,
			String direction, int rowNumber) throws DotDataException, DotSecurityException, ParseException {
		StringBuffer buffy = new StringBuffer();
		buffy.append("+live:true +deleted:false +structureInode:" + structure.getInode());
		return conAPI.search(buffy.toString(), rowNumber, -1, orderFieldName + " " + direction, user, true);
	}

	/**
	 *
	 * @param structureType
	 * @param categoryName
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws ParseException
	 * @deprecated This method was deprecated because it uses the presentation name of the field
	 *              we encourage the use of the logical name of the field instead @see getFieldByLogicalName
	 */
	public List<Contentlet> getContents(String structureType, String categoryName) throws DotDataException, DotSecurityException, ParseException {
		Category category = categoryAPI.findByName(categoryName, user, true);
		Structure structure = StructureCache.getStructureByType(structureType);
		StringBuffer buffy = new StringBuffer();
		buffy.append("+live:true +deleted:false +structureInode:" + structure.getInode() + " +c" + category.getInode() + "c:on");
		return conAPI.search(buffy.toString(), 0, -1, "mod_date", user, true);
	}

	/**
	 * This method searches inside the lucene index for contents, this methods
	 * uses the contentlet database names of the fields in the condition and the
	 * sort by field so you should convert the display names of the fields to
	 * the contentlet fields names before call this method
	 *
	 * @param structureType
	 *            The name or inode of the structure, E.G: "Web Page Content" or
	 *            12354
	 * @param luceneCondition
	 *            The lucene query E.G. +text1:test text2:you
	 * @param sortBy
	 *            The field used to sort you can also use the desc or asc
	 *            suffix, E.G: text1 desc
	 * @param pageStr
	 *            The page you want to display
	 * @param rowsPerPage
	 *            The number of records you want to show per page.
	 * @return
	 * @throws ParseException
	 */
	@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
	public HashMap searchWithLuceneQuery(String structureType, String luceneCondition, String sortBy, String pageStr,
			String rowsPerPage) throws ParseException,DotSecurityException {

		String structInode = "";
		Structure structure = null;
		try {
			structInode = structureType;
			structure = new Structure();
			structure.setInode(structInode);
		} catch (Exception e) {
			structure = StructureCache.getStructureByType(structureType);
		}

		Logger.debug(ContentsWebAPI.class, "search: luceneCondition: " + luceneCondition + ", sortBy: " + sortBy
				+ ", page: " + pageStr);
		int perPage = Integer.parseInt(rowsPerPage);
		int page = Integer.parseInt(pageStr);
		int offset = (page - 1) * perPage;

		List <ContentletSearch> assets = null;
		HashMap retMap = new HashMap();

		StringBuffer buffy = new StringBuffer();
		buffy.append("structureInode:" + structInode + " deleted:false ");
		buffy.append(luceneCondition);

		try {
			assets = conAPI.searchIndex(buffy.toString(), perPage, offset, sortBy,user,true);
		} catch (DotDataException e) {
			Logger.error(ContentsWebAPI.class,e.getMessage(),e);
		}

		int totalRecords = assets.size();

		retMap.put("assets", assets);
		int totalPages = (int) Math.ceil((double) totalRecords / (double) perPage);
		retMap.put("total_records", String.valueOf(totalRecords));
		retMap.put("total_pages", String.valueOf(totalPages));
		retMap.put("total_records_int", totalRecords);
		retMap.put("total_pages_int", totalPages);
		retMap.put("has_next_page", page < totalPages);
		retMap.put("has_previous_page", page > 1);

		return retMap;

	}

	@SuppressWarnings("rawtypes")
	public HashMap searchWithLuceneQuery(String structureType, String luceneCondition, String sortBy, int maxResults) throws ParseException, DotSecurityException {
		int page = 1;
		int pageSize = -1;
		return searchWithLuceneQuery(structureType, luceneCondition, sortBy, maxResults, page, pageSize);
	}

	@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
	public HashMap searchWithLuceneQuery(String structureType, String luceneCondition, String sortBy, int maxResults,
			int page, int pageSize) throws ParseException, DotSecurityException {
		/*
		 * We avoid a db hit if we pass the structure inode
		 */

		String structInode = "";
		Structure structure = null;
		try {
			structInode = structureType;
			structure = new Structure();
			structure.setInode(structInode);
		} catch (Exception e) {
			structure = StructureCache.getStructureByType(structureType);
		}
		//luceneCondition = LuceneUtils.findAndReplaceQueryDatesLegacy(luceneCondition);
		int offSet = 0;
		if (pageSize > 0) {
			offSet = (page - 1) * pageSize;
		}
		Logger.debug(ContentsWebAPI.class, "search: luceneCondition: " + luceneCondition + ", sortBy: " + sortBy
				+ ", max results: " + maxResults);
		List <ContentletSearch> assets = null;
		HashMap retMap = new HashMap();

		StringBuffer buffy = new StringBuffer();
		buffy.append("structureInode:" + structInode + " deleted:false ");
		buffy.append(luceneCondition);

		try {
			assets = conAPI.searchIndex(buffy.toString(), maxResults, offSet, sortBy, user, true);
		} catch (DotDataException e) {
			Logger.error(ContentsWebAPI.class,e.getMessage(),e);
		}

		int totalRecords = assets.size();

		retMap.put("assets", assets);
		retMap.put("total_records", String.valueOf(totalRecords));

		return retMap;

	}


	/**
	 * This methods retrieves the disk paths for a list of contents hits retrieved from lucene
	 * this method is used in the dynamic containers code to get the list of contents paths
	 * @param assets The assets hits
	 * @param working True if you want the working path to the content file, false if you the live path to the file
	 * @return A list of paths to the contents
	 */
	public List<String> getContentsPathsFromLuceneHits(LuceneHits assets, boolean working) {
		ArrayList<String> paths = new ArrayList<String>();
		for (int i = 0; i < assets.length(); i++) {
			Logger.debug(this, "getContentsPathsFromLuceneHits: Adding asset path: "
					+ (working ? "/working/" : "/live/") + assets.doc(i).get("identifier") + "_"
					+ assets.doc(i).get("languageId") + "." + Config.getStringProperty("VELOCITY_CONTENT_EXTENSION"));
			paths.add((working ? "/working/" : "/live/") + assets.doc(i).get("identifier") + "_"
					+ assets.doc(i).get("languageId") + "." + Config.getStringProperty("VELOCITY_CONTENT_EXTENSION"));

		}
		return paths;
	}

	/**
	 * This methods retrieves the identifier for a list of contents hits retrieved from lucene
	 * this method is used in the dynamic containers code to get the list of contents
	 * @param assets The assets hits
	 * @return A list of identifier to the contents
	 */
	public List<String> getContentIdentifiersFromLuceneHits(LuceneHits assets) {
		ArrayList<String> identifiers = new ArrayList<String>();
		for (int i = 0; i < assets.length(); i++) {
			Logger.debug(this, "getContentIdentifiersFromLuceneHits: Adding asset identifier: "
					+ assets.doc(i).get("identifier"));
			identifiers.add(assets.doc(i).get("identifier"));
		}
		return identifiers;
	}

	public List<Category> getContentletCategories(String inode) throws DotDataException, DotSecurityException {
		Contentlet content = conAPI.find(inode, user, true);
		return categoryAPI.getChildren(content, user, true);
	}

	// Relationships methods
	// We need to make the relationship not database dependant

	/**
	 * This method retrieves the relationship by relation type value =
	 * relationship name
	 */
	public Relationship getRelationshipByName(String relationshipName) {
		return RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
	}

	/**
	 * This methods gets the list of all the relationship objects associated to
	 * the structure of the contentlet as a parent or a child
	 *
	 * @param cont
	 *            The contentlet
	 * @return A list of relationship objects
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Relationship> getRelationshipsOfContentlet(Contentlet cont) throws DotDataException, DotSecurityException {
		return getRelationshipsOfContentlet(cont.getInode());
	}
    @Deprecated
	public List<Relationship> getRelationshipsOfContentlet(long contentletInode) throws DotDataException, DotSecurityException {
		return getRelationshipsOfContentlet(((Long) contentletInode).toString());
	}

	public List<Relationship> getRelationshipsOfContentlet(String contentletInode) throws DotDataException, DotSecurityException {
		Contentlet cont = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipsByStructure(cont.getStructure());
	}

	/**
	 * This gets the list of all the relationship objects associated to the
	 * structure of the contentlet
	 *
	 * @param cont
	 *            The contentlet
	 * @param hasParent
	 *            true If you find the relations where the contentlet is parent,
	 *            false If you find the relations where the contentlet is child
	 * @return A list of relationship objects
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Relationship> getRelationshipsOfContentlet(Contentlet cont, boolean hasParent) throws DotDataException, DotSecurityException {
		return getRelationshipsOfContentlet(cont.getInode(), hasParent);
	}
    @Deprecated
	public List<Relationship> getRelationshipsOfContentlet(long contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		return getRelationshipsOfContentlet(((Long) contentletInode).toString(), hasParent);
	}

	public List<Relationship> getRelationshipsOfContentlet(String contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		Contentlet cont = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipsByStructure(cont.getStructure(), hasParent);
	}

	/**
	 * This gets the list of all the relationship objects associated to the
	 * structure of the contentlet
	 *
	 * @param cont
	 *            The contentlet
	 * @param hasParent
	 *            true If you find the relations where the contentlet is parent,
	 *            false If you find the relations where the contentlet is child
	 * @return A list of relationship objects
	 * @deprecated this methods could be found on StructuresWebAPI
	 */
	@Deprecated
	public List<Relationship> getRelationshipsOfStructure(Structure st, boolean hasParent) {
		return getRelationshipsOfStructure(st.getInode(), hasParent);
	}
	@Deprecated
	public List<Relationship> getRelationshipsOfStructure(long structureInode, boolean hasParent) {
		return getRelationshipsOfStructure(((Long) structureInode).toString(), hasParent);
	}
	@Deprecated
	public List<Relationship> getRelationshipsOfStructure(String structureInode, boolean hasParent) {
		Structure st = (Structure) InodeFactory.getInode(structureInode, Structure.class);
		return RelationshipFactory.getAllRelationshipsByStructure(st, hasParent);
	}

	/**
	 * This methods retrieve the list of related contentlets in a relationship
	 * of a given contentlet
	 *
	 * @param relationship
	 *            The relationship
	 * @param cont
	 *            The contentlet
	 * @param hasParent
	 *            true Find the related children contentlets where the given
	 *            contentlet is the parent, false Find the related parents
	 *            contentlets where the given contentlet is a child
	 * @return The list of related contentlets
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public static List<Contentlet> getRelatedContentlets(Relationship relationship, Contentlet contentlet, boolean hasParent) throws DotStateException, DotDataException {
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, hasParent, true,"");
	}
    @Deprecated
	public List<Contentlet> getRelatedContentlets(long relationshipInode, long contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		return getRelatedContentlets(String.valueOf(relationshipInode), String.valueOf(contentletInode),hasParent);
	}

	public List<Contentlet> getRelatedContentlets(String relationshipInode, String contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		Relationship relationship = (Relationship) InodeFactory.getInode(relationshipInode, Relationship.class);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, hasParent, true,"");
	}

	public List<Contentlet> getRelatedChildContent(String relationshipName, String contentletInode) throws DotDataException, DotSecurityException {
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, true, true,"");
	}

	public List<Contentlet> getRelatedParentContent(String relationshipName, String contentletInode) throws DotDataException, DotSecurityException {
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, false, true,"");
	}

	public List<Contentlet> getAllRelatedContent(String relationshipName, String contentletInode) throws DotDataException, DotSecurityException {
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Set<Contentlet> contSet = new HashSet();
		contSet.addAll(RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, false));
		contSet.addAll(RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, true));
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<Contentlet> conts = new ArrayList();
		conts.addAll(contSet);
		return conts;
	}

	public List<Contentlet> getRelatedContentletsByCondition(String relationshipInode, String contentletInode, boolean hasParent, String condition) throws DotDataException, DotSecurityException {
		Relationship relationship = (Relationship) InodeFactory.getInode(relationshipInode, Relationship.class);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, hasParent, true, condition);
	}

	/**
	 * This method is used by the pullRelatedContent macro to retrieve related content and display it in a page
	 * @param relationshipName
	 * @param contentletInode
	 * @param limit
	 * @param orderBy
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Contentlet> pullRelatedContent (String relationshipName, String contentletInode, String limit, String orderBy) throws DotDataException, DotSecurityException  {

		int limitI = 0;
		try { limitI = Integer.parseInt(limit); } catch (Exception e) { }

		return pullRelatedContent(relationshipName, contentletInode, limitI, orderBy);
	}

	/**
	 * This method is used by the pullRelatedContent macro to retrieve related content and display it in a page
	 * @param relationshipName
	 * @param contentletInode
	 * @param limit
	 * @param orderBy
	 * @deprecated
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Contentlet> pullRelatedContent (String relationshipName, long contentletInode, int limit, String orderBy) {
		return pullRelatedContent (relationshipName, String.valueOf(contentletInode), limit, orderBy);
	}

	/**
	 * This method is used by the pullRelatedContent macro to retrieve related content and display it in a page
	 * @param relationshipName
	 * @param contentletInode
	 * @param limit
	 * @param orderBy
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public List<Contentlet> pullRelatedContent (String relationshipName, String contentletInode, int limit, String orderBy) {
		ContentletAPI conAPI = APILocator.getContentletAPI();

		Contentlet contentlet = null;
		try {
			contentlet = conAPI.find(contentletInode, APILocator.getUserAPI().getSystemUser(), true);
		} catch (DotSecurityException e) {
			Logger.info(this, "Unable to look up content because of a problem getting the system user");
		} catch (DotDataException de) {
			Logger.info(this, "Unable to retrieve content with inode = " + contentletInode);
		}
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		if(contentlet == null || !InodeUtils.isSet(contentlet.getInode()) || relationship == null || !InodeUtils.isSet(relationship.getInode()))
			return new ArrayList<Contentlet>();

		if(UtilMethods.isSet(orderBy)) {
			String[] orderBySplitted = orderBy != null?orderBy.split("[,\\s]+"):new String[0];
			for (String orderBySeg : orderBySplitted) {
				orderBySeg = orderBySeg.trim();
				if(orderBySeg.toLowerCase().equals("desc") || orderBySeg.toLowerCase().equals("asc"))
					continue;
				if(orderBySeg.toLowerCase().equals("moddate")) {
					orderBy = orderBy.replaceAll("(?i)moddate", "mod_date");
					continue;
				}
				Field field = FieldFactory.getFieldByVariableName(contentlet.getStructureInode(), orderBySeg);
				if (field != null && InodeUtils.isSet(field.getInode()))
					orderBy = orderBy.replaceAll(orderBySeg, field.getFieldContentlet());
			}
		}
		return RelationshipFactory.getRelatedContentlets(relationship, contentlet, orderBy, null, true, limit);

	}

	/**
	 * This methods checks if the given contentlet has the role of parent of the
	 * given relationship
	 *
	 * @param contentlet
	 *            The contentlet
	 * @param relationship
	 *            The relationship
	 * @return true If the contentlet has the role of parent, false otherwise
	 */
	public static boolean isParentOfTheRelationship(Contentlet contentlet, Relationship relationship) {
		Structure contStructure = contentlet.getStructure();
		return relationship.getParentStructure().getInode().equalsIgnoreCase(contStructure.getInode());
	}

    @Deprecated
	public boolean isParentOfTheRelationship(long contentletInode, long relationshipInode) throws DotDataException, DotSecurityException {
		return isParentOfTheRelationship(String.valueOf(contentletInode), String.valueOf(relationshipInode));
	}

	public boolean isParentOfTheRelationship(String contentletInode, String relationshipInode) throws DotDataException, DotSecurityException {
		Relationship relationship = (Relationship) InodeFactory.getInode(relationshipInode, Relationship.class);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return isParentOfTheRelationship(contentlet, relationship);
	}

	/**
	 * This methods checks if the given contentlet has the role of child of the
	 * given relationship
	 *
	 * @param contentlet
	 *            The contentlet
	 * @param relationship
	 *            The relationship
	 * @return true If the contentlet has the role of child, false otherwise
	 */
	public static boolean isChildOfTheRelationship(Contentlet contentlet, Relationship relationship) {
		Structure contStructure = contentlet.getStructure();
		return relationship.getChildStructure().getInode().equalsIgnoreCase(contStructure.getInode());
	}

    @Deprecated
	public boolean isChildOfTheRelationship(long contentletInode, long relationshipInode) throws DotDataException, DotSecurityException {
		return isChildOfTheRelationship(String.valueOf(contentletInode), String.valueOf(relationshipInode));
	}

	public boolean isChildOfTheRelationship(String contentletInode, String relationshipInode) throws DotDataException, DotSecurityException {
		Relationship relationship = (Relationship) InodeFactory.getInode(relationshipInode, Relationship.class);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return isChildOfTheRelationship(contentlet, relationship);
	}
	/*
	 * Used to pull dynamic lists of content in the form of maps
	 * for the front end of the web site.  This is used by the
	 * #pullContent macro.
	 */
	@SuppressWarnings("rawtypes")
	public List pullContent(String query, String lim, String sortBy) throws ParseException, DotSecurityException, DotDataException {
		return pullContent(query, lim, sortBy, false);
	}

	/*
	 * Used to pull dynamic lists of content in the form of maps
	 * for the front end of the web site.  This is used by the
	 * #pullContent macro.
	 */

	@SuppressWarnings("rawtypes")
	public List pullContent(String query, String lim, String sortBy, Boolean editMode) throws ParseException, DotSecurityException, DotDataException {
		boolean eMode;
		if(editMode == null){
			eMode = false;
		}else{
			eMode = editMode;
		}
		User u = null;
		if(eMode){
			u = backuser;
		}else{
			u = user;
		}
		int limit = 0;
		List<Map> l = new ArrayList<Map>();
		//LuceneHits hits = new LuceneHits();
		List <ContentletSearch> hits = new ArrayList<ContentletSearch>();


		int offset = 0;

		try {
			limit = Integer.parseInt(lim);
			if(limit == 0){
				offset = -1;
			}

		} catch (Exception e) {
			return l;
		}

		if(UtilMethods.isSet(sortBy) && sortBy.equalsIgnoreCase("random")){
			sortBy="";
			if(limit>=(MAX_LIMIT-10)){
				limit += MAX_LIMIT;
			}else{
				limit = MAX_LIMIT;
			}

		}



		hits = conAPI.searchIndex(query, limit, offset, sortBy, u, true);


		/**
		 * when the limit is 0, set the limit to the size of the lucene result search
		 */

		for (ContentletSearch conwrap: hits) {



				Map<String, Object> hm = new HashMap<String, Object>();
				hm.put("inode", conwrap.getInode());
				hm.put("identifier", conwrap.getIdentifier());
				l.add(hm);


		}
		return l;

	}

	/*
	 * Used to pull dynamic lists of content in the form of maps
	 * for the front end of the web site.  This is used by the
	 * #pullPersonalizedContentByCategories macro.
	 */
	@SuppressWarnings("rawtypes")
	public List pullPersonalizedContentByCategories(String query, String lim, String sortBy, List categoryList) throws DotSecurityException, DotDataException, ParseException {
		ContentletAPI conAPI = APILocator.getContentletAPI();
		@SuppressWarnings("unchecked")
		List<HashMap> contents = pullContent(query, lim, sortBy);
		List<HashMap> returnList = new ArrayList<HashMap>();

		for(HashMap content: contents) {
			String inode = (String) content.get("inode");

			Contentlet contentlet = new Contentlet();
			try{
				contentlet = conAPI.find(inode, user, true);
			}catch (DotDataException ex){
				Logger.error(this, "Unable to find contentlet with inode " + inode);
			}

			List<Category> categoryContentlet = categoryAPI.getChildren(contentlet, user, true);
			Iterator it = categoryContentlet.iterator();
			if (UtilMethods.isSet(categoryList)) {
				while (it.hasNext()) {
					Category cat = (Category)it.next();
					if (categoryList.contains(cat)) {
						returnList.add(content);
						break;
					}
				}
			}
		}

		return returnList;
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	private static boolean isTagOntagInodeList (List tagInodeList, Tag tag) {
		for (int i=0; i<tagInodeList.size(); i++) {
			HashMap tagInode = (HashMap) tagInodeList.get(i);

			if (tag.getTagName().equalsIgnoreCase(String.valueOf(tagInode.get("tagname")))) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	public List randomizeList(List list, String limit)
	{
		try
		{
			int i = 0;
			try
			{
				i = Integer.parseInt(limit);
			}
			catch(Exception e)
			{
				Logger.warn(ContentsWebAPI.class,"Problems with the randomizeList limit");
				i = list.size();
			}

			if(!list.isEmpty())
			{
				Collections.shuffle(list);
				return (list.size() > i ? list.subList(0,i):list);
			}
			else
			{
				return list;
			}
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			Logger.debug(ContentsWebAPI.class,message);
			return list;
		}
	}

	/**
	 * This method pulls dynamic lists of content in the form of maps
	 * for the front end of the web site using pagination.
	 * This is used by the #pageContent macro.
	 * @param query the lucene query E.G. +text1:test text2:you
	 * @param sortBy the field used to sort you can also use the desc or asc suffix, E.G: text1 desc
	 * @param perPage the number of records showed per page, the number of contents to return
	 * @param currentPageNumber number of the page currently displayed
	 * @return HashMap, with two keys, _inodeList: the list of contents, and _total: total number of records
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HashMap pageContent(String query, String sortBy, String perPage, String currentPageNumber) {

		HashMap retMap = new HashMap();
		int pageNumber = 1;
		try {
			pageNumber = Integer.parseInt(currentPageNumber);
		}catch(Exception e) {}

		int displayPerPage = Config.getIntProperty("PER_PAGE");
		try {
			displayPerPage = Integer.parseInt(perPage);
		}catch(Exception e) {}

		int minIndex = (pageNumber - 1) * displayPerPage;
		int maxIndex = displayPerPage * pageNumber;

		int limit = 0;

		List<Map> l = new ArrayList<Map>();
		SearchHits hits = null;
		List<Contentlet> c = new ArrayList<Contentlet>();

		try {
		    c = APILocator.getContentletAPI().search(query, limit, -1, sortBy, user, true);

		} catch (Exception ex) {
			Logger.error(this.getClass(), "indexSearch: Error Searching Contentlets - lucene query: " + query, ex);
		}

		for (int i = minIndex; i < c.size(); i++) {
			if(i==maxIndex){
				break;
			}
			try{

				Map<String, Object> hm = new HashMap<String, Object>();
				hm.put("inode", c.get(i).getInode());
				hm.put("identifier", c.get(i).getIdentifier());
				l.add(hm);

			}
			catch(Exception e){}
		}

		retMap.put("_inodeList", l);
		retMap.put("_total", String.valueOf(c.size()));

		return retMap;
	}

	@SuppressWarnings("rawtypes")
	public Map getEmptyMap() {
		return new HashMap();
	}

	@SuppressWarnings("rawtypes")
	public List getEmptyList() {
		return new ArrayList();
	}


	/**
	 * This method return if a identifier or inode is from a
	 * contentlet object. is use in the permalink macro
	 * @param inode
	 * @return boolean
	 * @author Oswaldo Gallango
	 * @version 1.5
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws NumberFormatException
	 * @since 1.5
	 */
	public boolean isContentletIdentifierOrInode(String id) throws NumberFormatException{
		Contentlet contentlet = null;
		User user;
		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.debug(this, "Unable to look up system user", e);
			return false;
		}
		try{
			contentlet = conAPI.find(id,user,true);
		}catch(Exception de){
			Logger.debug(this, "Unable to find contentlet by inode", de);
		}
		if(contentlet != null && InodeUtils.isSet(contentlet.getInode())){
			return true;
		}else{
			try {
				contentlet = conAPI.findContentletByIdentifier(id, false, langAPI.getDefaultLanguage().getId(), user, true);
			} catch (Exception e) {
				Logger.debug(this, "Unable to find contentlet by identifier", e);
			}
			if(contentlet != null && InodeUtils.isSet(contentlet.getInode())){
				return true;
			}else{
				return false;
			}
		}
	}
	/**
	 * This methods matches the browsers URL to a contentlet field and
	 * returns the contentlet with the closest match.  For example, let's say the
	 * user is viewing a page called /departments/accounting
	 * and there is a structure wi
	 *
	 * @param request
	 *            The HTTPServletRequest, from velocity: $request
	 * @param structureName
	 *            The name of the Structure, e.g. "Departments"
	 * @param fieldName
	 *            The full name of the field, e.g. "The Department Url"
	 * @return The identifier of the top matching contentlet
	 */
	public String getContentletByUrl(HttpServletRequest request, String structureName, String fieldName) {
		long x = System.currentTimeMillis();
		// get the default language
		long languageId = langAPI.getDefaultLanguage().getId();
		try {
			languageId = ((Language) request.getSession(false).getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).getId();
		} catch (Exception e) {

		}

		ArrayList<String> al = new ArrayList<String>();
		String url = request.getRequestURI();
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(url, "/");
		int boost = 1;
		while (st.hasMoreTokens()) {
			sb.append("/");
			sb.append(st.nextToken());

			if (sb.toString().indexOf("?") > -1)
				break;

			//DOTCMS-1992 Match the exact url without the trailing / like /departments/sciences
			al.add(sb.toString()+ "^" + boost);

			//DOTCMS-1992 Match the exact url with the trailing / like /departments/sciences/
			al.add(sb.toString()+ "/^" + boost);

			//DOTCMS-1992 Also added term boosting to the query to be able to have a more exact match
			//Let say you have a structure with a field url like /departments and other with /departments/hispanic
			//Without the boost logic hitting a url like /departments/hispanic/index.dot could be matching the structure with
			//url field like /departments when it's more accurate to return the content with /departments/hispanic value on the
			//matching field


			boost++;

		}
		Collections.reverse(al);


		// get the structure and field
		@SuppressWarnings("deprecation")
		Structure structure = StructureCache.getStructureByName(structureName);
        if(structure ==null){
            Logger.error(this.getClass(), "getContentletByUrl unable to find structure " +structureName + "." +fieldName );
            return null;
        }


		List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
		Field field = null;
		for (Field f : fields) {
			try {
				if (f.getFieldName().equals(fieldName)) {
					field = f;
					break;
				}
			} catch (Exception e) {
			}
		}
        if(field ==null){
            Logger.error(this.getClass(), "getContentletByUrl unable to find field " +structureName + "." +fieldName );
            return null;
        }


		StringBuffer luceneQuery = new StringBuffer();
		luceneQuery.append(" +structureName:");
		luceneQuery.append(structure.getVelocityVarName());
		luceneQuery.append(" +(");
		int i = 0;
		for (String s : al) {
			luceneQuery.append("");
			luceneQuery.append(structure.getVelocityVarName()+"."+field.getVelocityVarName());
			luceneQuery.append(":");
			luceneQuery.append(s);

			if(++i < al.size()){
				luceneQuery.append(" ");
			}
		}
		luceneQuery.append(")");
		luceneQuery.append(" +languageId:");
		luceneQuery.append(languageId);
		luceneQuery.append(" +deleted:false");
		luceneQuery.append(" +live:true ");


		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Map<String, Object> map = (Map) pullContent(luceneQuery.toString(), "1", null).get(0);
			return (String) map.get("identifier");
		} catch (Exception e) {
			return null;
		}
		finally{

			Logger.debug(this.getClass(), "getContentByUrl time:" + (System.currentTimeMillis()  - x));
		}

	}

	//Permission related methods

	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 * @deprecated
	 */
	public boolean doesUserHasPermission (long contentInode, int permission) throws DotDataException {
		return doesUserHasPermission(String.valueOf(contentInode), permission, user, true);
	}



	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 */
	public boolean doesUserHasPermission (String contentInode, int permission) throws DotDataException {
		return doesUserHasPermission(contentInode, permission, user, true);
	}

	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 * @deprecated
	 */
	public boolean doesUserHasPermission (long contentInode, int permission, boolean editMode) throws DotDataException {
		if(editMode)
			return doesUserHasPermission(String.valueOf(contentInode), permission, backuser, true);
		return doesUserHasPermission(String.valueOf(contentInode), permission, user, true);
	}



	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 */
	public boolean doesUserHasPermission (String contentInode, int permission, boolean editMode) throws DotDataException {
		if(editMode)
			return doesUserHasPermission(contentInode, permission, backuser, true);
		return doesUserHasPermission(contentInode, permission, user, true);
	}

	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 * @deprecated
	 */
	public boolean doesUserHasPermission (long contentInode, int permission, User user, boolean respectFrontendRoles) throws DotDataException {
		return doesUserHasPermission (String.valueOf(contentInode),permission, user, respectFrontendRoles);
	}


	/**
	 * This method checks if the logged in user (frontend) has the required permission over
	 * the passed contentlet id
	 */
	public boolean doesUserHasPermission (String contentInode, int permission, User user, boolean respectFrontendRoles) throws DotDataException {
		try {
			if(!InodeUtils.isSet(contentInode))
				return false;
			Contentlet cont = conAPI.find(contentInode, user, respectFrontendRoles);
			return perAPI.doesUserHavePermission(cont, permission, user, respectFrontendRoles);
		} catch (DotSecurityException e) {
			return false;
		}
	}

	/**
	 * This method return the possible values in a field of all the contents of the specified structure
	 * @param structureName type of structure of the contentlets
	 * @param fieldName field name of the specified structure
	 * @param user User
	 * @return List<String> list of possible values
	 */
	public List<String> findFieldValues(String structureName, String fieldName, User user) throws DotDataException {
		List<String> result = new ArrayList<String>();

		try {
			@SuppressWarnings("deprecation")
			Structure structure = StructureCache.getStructureByName(structureName);
			if ((structure == null) || !InodeUtils.isSet(structure.getInode()))
				return result;

			Field field = FieldFactory.getFieldByName(structure.getInode(), fieldName);
			if ((field == null) || !InodeUtils.isSet(field.getInode()))
				return result;

			result = conAPI.findFieldValues(structure.getInode(), field, user, true);
		} catch (Exception e) {
			Logger.debug(this, e.toString());
			HibernateUtil.closeSession();
		}

		return result;
	}

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 *
	 * @param contentlet
	 * @return String with the URL Map. Null if the structure of the content doesn't have the URL Map Pattern set.
	 */
	public String getUrlMapForContentlet(Contentlet contentlet) {
		String result = null;
		try {
			result = conAPI.getUrlMapForContentlet(contentlet, user, true);
		} catch (Exception e) {
			Logger.warn(ContentsWebAPI.class, e.toString());
		}

		return result;
	}

	/**
	 * Return the URL Map for the specified content identifier if the structure associated to the content has the URL Map Pattern set.
	 *
	 * @param identifier
	 * @return String with the URL Map. Null if the structure of the content doesn't have the URL Map Pattern set.
	 */
	public String getUrlMapForContentlet(String identifier) {
		String result = null;
		try {
			Contentlet contentlet = conAPI.findContentletByIdentifier(identifier, true, langAPI.getDefaultLanguage().getId(), user, true);
			result = getUrlMapForContentlet(contentlet);
		} catch (Exception e) {
			Logger.warn(ContentsWebAPI.class, e.toString());
		}

		return result;
	}

	/**
	 * Return the URL Map for the specified content if the structure associated to the content has the URL Map Pattern set.
	 *
	 * @param contentlet
	 * @return String with the URL Map. Null if the structure of the content doesn't have the URL Map Pattern set.
	 */
	public String getUrlMapForContentlet(Map<String, Object> contentlet) {
		String result = null;
		try {
			result = getUrlMapForContentlet((String) contentlet.get("identifier"));
		} catch (Exception e) {
			Logger.warn(ContentsWebAPI.class, e.toString());
		}

		return result;
	}
}