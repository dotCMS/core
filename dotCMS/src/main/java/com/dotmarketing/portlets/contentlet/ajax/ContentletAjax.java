package com.dotmarketing.portlets.contentlet.ajax;

import com.dotcms.business.CloseDB;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.enterprise.FormAJAXProxy;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.LogTime;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PublishStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotLanguageException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.contentlet.util.ActionletUtil;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.hostadmin.business.CopyHostContentUtil;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.MAX_LIMIT;
import static com.dotcms.exception.ExceptionUtil.getRootCause;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageAPI.isLocalizationEnhancementsEnabled;

/**
 * This class handles the communication between the view and the back-end
 * service that returns information to the user regarding Contentlets in dotCMS.
 * The information provided by this service is accessed via DWR.
 * <p>
 * For example, the <b>Content Search</b> portlet uses this class to display the
 * Contentlet data to the users, which can be filtered by certain criteria
 * depending on the selected Content Type.
 *
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ContentletAjax {

	private static String VELOCITY_CODE_TEMPLATE = "#foreach($con in $dotcontent.pull(\"%s\",10,\"%s\"))<br/>%s<br/>#end";

	private static final String CONTENT_TYPES_INODE_SEPARATOR = ",";

	private java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
			java.text.DateFormat.SHORT);

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private ContentletWebAPI contentletWebAPI = WebAPILocator.getContentletWebAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

	private LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();

	//Number of children related IDs to be added to a lucene query to get children related to a selected parent
	private static final int RELATIONSHIPS_FILTER_CRITERIA_SIZE = Config
            .getIntProperty("RELATIONSHIPS_FILTER_CRITERIA_SIZE", 500);

	public List<Map<String, Object>> getContentletsData(String inodesStr) {
		List<Map<String,Object>> rows = new ArrayList<>();

		if(inodesStr == null || !UtilMethods.isSet(inodesStr)) {
			return rows;
		}

		String[] inodes =  inodesStr.split(",");
		for (String inode : inodes) {
			Map<String, Object> contenletData = getContentletData(inode);
			if(contenletData != null)
				rows.add(contenletData);
		}

		return rows;
	}

	public Map<String, Object> getContentletData(String inode) {

		Map<String,Object> result = new HashMap<>();

		try {

			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Contentlet contentlet = null;

			try{// This is to avoid non-inode strings from throwing exception
				contentlet = conAPI.find(inode, currentUser, true);
			}catch(Exception e){
				Logger.debug(this, e.getMessage());
			}
			if(contentlet == null || !UtilMethods.isSet(contentlet.getInode())) {
				return null;
			}

			getListedFields(result, contentlet);

			result.put("iconClass", UtilHTML.getIconClass(contentlet));
			result.put("identifier", contentlet.getIdentifier());
			result.put("statusIcons", UtilHTML.getStatusIcons(contentlet));
			
			result.put("hasTitleImage", String.valueOf(contentlet.getTitleImage().isPresent()));
			if(contentlet.getTitleImage().isPresent()) {
			    result.put("titleImage", contentlet.getTitleImage().get());
			}
			result.put("title", String.valueOf(contentlet.getTitle()));
			result.put("inode", String.valueOf(contentlet.getInode()));
			result.put("working", String.valueOf(contentlet.isWorking()));
			result.put("live", String.valueOf(contentlet.isLive()));
			result.put("deleted", String.valueOf(contentlet.isArchived()));
			result.put("locked", String.valueOf(contentlet.isLocked()));
			result.put("id", contentlet.getIdentifier());// Duplicates value for identifier key in map so that UI does not get broken
			Language language = langAPI.getLanguage(contentlet.getLanguageId());
			String languageCode = langAPI.getLanguageCodeAndCountry(contentlet.getLanguageId(),null);
			String languageName =  language.getLanguage();
			result.put("langCode", languageCode);
			result.put("langName", languageName);
			result.put("langId", language.getId()+"");
			result.put("siblings", getContentSiblingsData(inode));
			result.put("hasImageFields",String.valueOf(hasImageFields(inode)));

		} catch (DotDataException e) {
			Logger.error(this, "Error trying to obtain the contentlets from the relationship.", e);

		} catch (DotSecurityException e) {
			Logger.error(this, "Security exception.", e);
		}

		return result;
	}


	/**
	 * Puts into the map all listed fields defined for the contentlet
	 * @param result
	 * @param contentlet
	 */
	private void getListedFields(final Map<String, Object> result, final Contentlet contentlet) {
		for (com.dotcms.contenttype.model.field.Field field : contentlet.getContentType()
                .fields()) {
            if (field.listed() || field.indexed()) {
                final Object fieldValueObj = contentlet.get(field.variable());
                String fieldValue = null;
                if (fieldValueObj != null) {
                    if (fieldValueObj instanceof Date) {
                        fieldValue = modDateFormat.format(fieldValueObj);
                    } else if (fieldValueObj instanceof java.sql.Timestamp) {
                        Date fieldDate = new Date(
                                ((java.sql.Timestamp) fieldValueObj).getTime());
                        fieldValue = modDateFormat.format(fieldDate);
                    } else {
                        fieldValue = fieldValueObj.toString();
                    }
                }
                result.put(field.variable(), fieldValue);
            }
        }
	}

	private boolean hasImageFields(String inode) throws DotDataException, DotSecurityException {

		final HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		final User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		final Contentlet firstContentlet = conAPI.find(inode, currentUser, true);
		final Structure targetStructure = firstContentlet.getStructure();
		final List<Field> targetFields = FieldsCache.getFieldsByStructureInode(targetStructure.getInode());

		//use a for each to iterate targetFields and validate if fieldType contains image
		for(final Field field : targetFields){
			if(field.getFieldType().equals(FieldType.IMAGE.toString()) ){
				return true;
			}
		}

		return false;
	}

	private List<Map<String, String>> getContentSiblingsData(String inode) {//GIT-1057

		List<Map<String, String>> result = new ArrayList<>();

		try {

			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);

			Contentlet firstContentlet = conAPI.find(inode, currentUser, true);

			List<Map<String,String>> contentletList = new ArrayList<>();

			LanguageAPI langAPI = APILocator.getLanguageAPI();
			ContentletAPI contentletAPI = APILocator.getContentletAPI();
			List<Language> langs = langAPI.getLanguages();
			Contentlet languageContentlet = null;

			String identifier = String.valueOf(firstContentlet.getIdentifier());

			Structure targetStructure = firstContentlet.getStructure();
			List<Field> targetFields = FieldsCache.getFieldsByStructureInode(targetStructure.getInode());

			boolean parent = false;
			try{
		        parent = firstContentlet.getBoolProperty("dotCMSParentOnTree") ;
		    }
		    catch(Exception e){

		    }

			for(Language lang : langs){

				Map<String, String> contentDetails = new HashMap<>();
				try{
					languageContentlet = null;
					languageContentlet = contentletAPI.findContentletByIdentifier(firstContentlet.getIdentifier(), true, lang.getId(), currentUser, false);
				}catch (Exception e) {
				}

				//Try to find non-live version
				if (languageContentlet == null){
                    try{
                        languageContentlet = contentletAPI.findContentletByIdentifier(firstContentlet.getIdentifier(), false, lang.getId(), currentUser, false);
                    }catch (Exception e1) {	}
                }

				boolean hasListedFields = false;

				if((languageContentlet == null) || (!UtilMethods.isSet(languageContentlet.getInode()))){

					contentDetails.put( "langCode" , langAPI.getLanguageCodeAndCountry(lang.getId(),null));
					contentDetails.put("langName", lang.getLanguage());
					contentDetails.put("langId", lang.getId()+"");
			    	contentDetails.put("inode", "");
					contentDetails.put("parent", parent+"");
					contentDetails.put("working", "false");
					contentDetails.put("live", "false");
					contentDetails.put("deleted", "true");
					contentDetails.put("locked", "false");
					contentDetails.put("siblingInode", firstContentlet.getInode());

					for (Field f : targetFields) {
						if (f.isIndexed() || f.isListed()) {
							hasListedFields = true;
							String fieldName = f.getFieldName();
							String fieldValue = "";
							contentDetails.put(fieldName, fieldValue);
						}
					}
					if( !hasListedFields ) {
						contentDetails.put("identifier", identifier);
					}


				}else{

					contentDetails.put( "langCode" , langAPI.getLanguageCodeAndCountry(lang.getId(),null));
					contentDetails.put("langName", lang.getLanguage());
					contentDetails.put("langId", lang.getId()+"");
			    	contentDetails.put("inode", languageContentlet.getInode());
					contentDetails.put("parent", parent+"");
					contentDetails.put("working", languageContentlet.isWorking()+"");
					contentDetails.put("live", languageContentlet.isLive()+"");
					contentDetails.put("deleted", languageContentlet.isArchived()+"");
					contentDetails.put("locked", languageContentlet.isLocked()+"");
					contentDetails.put("siblingInode", firstContentlet.getInode());

					for (Field f : targetFields) {
						if (f.isIndexed() || f.isListed()) {
							hasListedFields = true;
							String fieldName = f.getFieldName();
							Object fieldValueObj = "";
							try{
								fieldValueObj = conAPI.getFieldValue(languageContentlet, f);
							}catch (Exception e) {
								Logger.error(ContentletAjax.class, "Unable to get value for field", e);
							}
							String fieldValue = "";
							if (fieldValueObj instanceof java.util.Date) {
								if (fieldValueObj != null)
									fieldValue = modDateFormat.format(fieldValueObj);
							} else if (fieldValueObj instanceof java.sql.Timestamp) {
								if (fieldValueObj != null) {
									java.util.Date fieldDate = new java.util.Date(((java.sql.Timestamp) fieldValueObj).getTime());
									fieldValue = modDateFormat.format(fieldDate);
								}
							} else {
								if (fieldValueObj != null)
									fieldValue = fieldValueObj.toString();
							}
							contentDetails.put(fieldName, fieldValue);
						}
					}
					if( !hasListedFields ) {
						contentDetails.put("identifier", identifier);
					}
				}
				contentletList.add(contentDetails);
			}

			result = contentletList;



		} catch (DotDataException e) {
			Logger.error(this, "Error trying to obtain the contentlets from the relationship.", e);

		} catch (DotSecurityException e) {
			Logger.error(this, "Security exception.", e);
		}

		return result;
	}


	/**
	 * This method is used by the backend to pull the content from the lucene
	 * index and also checks the user permissions to see the content
	 *
	 * @param structureInode
	 *            Inode of the structure content to be listed
	 * @param fields
	 *            Fields to filters, where the position i (where i is odd)
	 *            represent the field name and the position i + 1 represent the
	 *            field value to filter
	 * @param categories
	 *            The categories inodes to filter
	 * @param showDeleted
	 *            If true show the deleted elements only
	 * @param filterSystemHost
	 *            If true filter elements of system host
	 * @param page
	 *            The page number to show (starting with 1)
	 *            If page is 0, this will return all possible contentlets
	 * @param perPage
	 * @param orderBy
	 *            The field name to be used to sort the content
	 * @return The list of contents that match the parameters at the position 0
	 *         the result included a hashmap with some useful information like
	 *         the total number of results, ...
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	@SuppressWarnings("rawtypes")
	public List searchContentlet(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, int perPage, String orderBy) throws DotStateException, DotDataException, DotSecurityException {

		HttpSession sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(ImmutableList.of(BaseContentType.ANY), structureInode, fields, categories, showDeleted, filterSystemHost, false, false, page, orderBy, perPage, currentUser, sess, null, null);
	}
	@CloseDB
	@SuppressWarnings("rawtypes")
	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, String orderBy, String modDateFrom, String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
	    return searchContentlets(structureInode, fields, categories, showDeleted, filterSystemHost, page, orderBy, modDateFrom, modDateTo, true);
	}

	@SuppressWarnings("rawtypes")
	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, String orderBy, String modDateFrom, String modDateTo, boolean saveLastSearch) throws DotStateException, DotDataException, DotSecurityException {
	    HttpSession sess = null;
        if(saveLastSearch)
            sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(ImmutableList.of(BaseContentType.ANY), structureInode, fields, categories, showDeleted, filterSystemHost, false, false, page, orderBy, 0,currentUser, sess, modDateFrom, modDateTo);
	}

	@CloseDB
	@SuppressWarnings("rawtypes")
	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted,
	        boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, String orderBy, String modDateFrom,
	        String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
	    return searchContentlets(structureInode,fields,categories,showDeleted,filterSystemHost,filterUnpublish,filterLocked,page,0,orderBy,modDateFrom,modDateTo);
	}

	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted,
			boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, int perPage,String orderBy, String modDateFrom,
			String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
		return searchContentlets(structureInode, fields, categories, showDeleted, filterSystemHost,
				filterUnpublish, filterLocked, page, perPage, orderBy, modDateFrom, modDateTo,
				VariantAPI.DEFAULT_VARIANT.name());
	}

		@SuppressWarnings("rawtypes")
	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted,
	        boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, int perPage,String orderBy, String modDateFrom,
	        String modDateTo, final String variantName) throws DotStateException, DotDataException, DotSecurityException {

		HttpSession sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(ImmutableList.of(BaseContentType.ANY), structureInode, fields, categories, showDeleted, filterSystemHost, filterUnpublish, filterLocked,
		        page, orderBy, perPage,currentUser, sess, modDateFrom, modDateTo, variantName);
	}

	public List searchContentlets(String[] structureInodes, List<String> fields, List<String> categories, boolean showDeleted,
			boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, int perPage,String orderBy, String modDateFrom,
			String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
		String structureInodesJoined = String.join(CONTENT_TYPES_INODE_SEPARATOR, structureInodes);

		return searchContentlets(structureInodesJoined, fields, categories, showDeleted, filterSystemHost, filterUnpublish, filterLocked,
				page, perPage, orderBy, modDateFrom, modDateTo, VariantAPI.DEFAULT_VARIANT.name());
	}

	public List searchContentlets(String[] structureInodes, List<String> fields, List<String> categories, boolean showDeleted,
								  boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, int perPage,String orderBy, String modDateFrom,
								  String modDateTo, final String variantName) throws DotStateException, DotDataException, DotSecurityException {
		String structureInodesJoined = String.join(CONTENT_TYPES_INODE_SEPARATOR, structureInodes);

		return searchContentlets(structureInodesJoined, fields, categories, showDeleted, filterSystemHost, filterUnpublish, filterLocked,
				page, perPage, orderBy, modDateFrom, modDateTo, variantName);
	}

	/**
	 * This method is used by the backend to pull from lucene index the form widgets
	 * if the widget doesn't exist then is created and also checks the user
	 * permissions to see the content
	 *
	 * @param formStructureInode
	 *            Inode of the structure content to be listed
	 * @return The list of contents that match the parameters at the position 0
	 *         the result included a hashmap with some useful information like
	 *         the total number of results, ...
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	@CloseDB
	public Map<String, Object> searchFormWidget(String formStructureInode) throws DotDataException, DotSecurityException {
		FormAJAXProxy fp = new FormAJAXProxy();
		return fp.searchFormWidget(formStructureInode);
	}

	 public List searchContentletsByUser(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, boolean filterUnpublish, boolean filterLocked, int page, String orderBy,int perPage, final User currentUser, HttpSession sess,String  modDateFrom, String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
	   return searchContentletsByUser(ImmutableList.of(BaseContentType.ANY), structureInode, fields, categories, showDeleted, filterSystemHost, filterUnpublish, filterLocked, page, orderBy, perPage, currentUser, sess, modDateFrom, modDateTo);
	 }

	 private <T> List<T> distinct (final List<T> collection, final Function<T, Object> indexKeyFunction) {

		 final Map<Object, T> collectionIndexMap = collection.stream().collect(
		 		Collectors.toMap(indexKeyFunction, Function.identity(), (existing, replacement) -> existing,
						LinkedHashMap::new));

		return new ArrayList<>(collectionIndexMap.values());
	 }

	public List searchContentletsByUser(List<BaseContentType> types, String structureInode,
			List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost,
			boolean filterUnpublish, boolean filterLocked, int page, String orderBy,int perPage,
			final User currentUser, HttpSession sess,String  modDateFrom, String modDateTo)
			throws DotStateException, DotDataException, DotSecurityException {
		return searchContentletsByUser(types, structureInode, fields, categories, showDeleted, filterSystemHost,
				filterUnpublish, filterLocked, page, orderBy, perPage, currentUser, sess, modDateFrom, modDateTo,
				VariantAPI.DEFAULT_VARIANT.name());
	}

	/**
	 * This method is used by the back-end to pull the content from the Lucene
	 * index and also checks the user permissions to see the content.
	 *
	 * @param structureInode
	 *            - Inode of the structure content to be listed
	 * @param fields
	 *            - Fields to use for filtering, where the position i (where i
	 *            is odd) represent the field name and the position i + 1
	 *            represent the field value to filter
	 * @param categories
	 *            - The categories inodes to filter.
	 * @param showDeleted
	 *            - If true show the deleted elements only.
	 * @param filterSystemHost
	 *            - If true filter elements of system host.
	 * @param page
	 *            - The page number to show (starting with 1). If page is 0,
	 *            this will return all possible contentlets.
	 * @param perPage
	 *            - Number of contents to display per page.
	 * @param orderBy
	 *            - The field name to be used to sort the content.
	 * @param currentUser
	 *            - The user needed to check the permissions.
	 * @param sess
	 *            HttpSession to save some values if is set.
	 * @return The list of contents that match the parameters at the position 0
	 *         the result included a {@link HashMap} with some useful
	 *         information like the total number of results, etc.
	 * @throws DotSecurityException
	 *             The user does not have the permissions to perform this
	 *             action.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the
	 *             database.
	 * @throws DotStateException
	 *             A system error has occurred.
	 */
	@SuppressWarnings("rawtypes")
	@LogTime
	public List searchContentletsByUser(List<BaseContentType> types, String structureInode,
			List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost,
			boolean filterUnpublish, boolean filterLocked, int page, String orderBy,int perPage,
			final User currentUser, HttpSession sess,String  modDateFrom, String modDateTo, final String variantName)
				throws DotStateException, DotDataException, DotSecurityException {

        if (perPage < 1) {
          perPage = Config.getIntProperty("PER_PAGE", 40);
        }

        int offset = 0;
        if (page != 0)
            offset = perPage * (page - 1);

		if(!InodeUtils.isSet(structureInode)) {
			Logger.error(this, "An invalid structure inode =  \"" + structureInode + "\" was passed");
			throw new DotRuntimeException("a valid structureInode need to be passed");
		}

		// Building search params and lucene query
		StringBuffer luceneQuery = new StringBuffer();

		String specialCharsToEscape = "([+\\-!\\(\\){}\\[\\]^\"~*?:\\\\]|[&\\|]{2})";
		String specialCharsToEscapeForMetaData = "([+\\-!\\(\\){}\\[\\]^\"~?:/\\\\]{2})";
		Map<String, Object> lastSearchMap = new HashMap<>();

		List<String> relatedIdentifiers = new ArrayList();
		List<RelationshipFieldData> relationshipFields = new ArrayList();
		final StringBuilder relatedQueryByChild = new StringBuilder();

		if (UtilMethods.isSet(sess)) {
	    sess.removeAttribute("structureSelected");
	    sess.removeAttribute("selectedStructure");
			sess.setAttribute(WebKeys.CONTENTLET_LAST_SEARCH, lastSearchMap);
            sess.setAttribute(ESMappingConstants.WORKFLOW_SCHEME, null);
            sess.setAttribute(ESMappingConstants.WORKFLOW_STEP, null);
		}

		Map<String, String> fieldsSearch = new HashMap<>();
		List<Object> headers = new ArrayList<>();
		Map<String, Field> fieldsMapping = new HashMap<>();
		final String[] structureInodes = structureInode.split(CONTENT_TYPES_INODE_SEPARATOR);
		Structure st = null;
		if(!Structure.STRUCTURE_TYPE_ALL.equals(structureInode) && !hasContentTypesInodeSeparator(structureInode)){
		    st = CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
		    lastSearchMap.put("structure", st);
		    luceneQuery.append("+contentType:" + st.getVelocityVarName() + " ");
		} else if (!Structure.STRUCTURE_TYPE_ALL.equals(structureInode) && hasContentTypesInodeSeparator(structureInode)) {
			luceneQuery.append("+contentType:(");

			for (int i = 0; i < structureInodes.length; i++) {
				st = CacheLocator.getContentTypeCache().getStructureByInode(structureInodes[i]);

				if (i != 0) {
					luceneQuery.append(" OR " + st.getVelocityVarName());
				} else {
					luceneQuery.append(st.getVelocityVarName());
				}
			}
			luceneQuery.append(") ");

		} else {
		    for(int i=0;i<fields.size();i++){
		        String x = fields.get(i);
		        if(Structure.STRUCTURE_TYPE_ALL.equals(x)){
		            String fieldValue =  fields.get(i+1);
					fieldValue = fieldValue.replaceAll("\\*", "");
		            while(fieldValue.contains("  ")){
						fieldValue = fieldValue.replace("  ", " ");
		            }
					fieldValue =fieldValue.replaceAll(specialCharsToEscape, "\\\\$1");
		            luceneQuery.append("title:" + fieldValue + "* ");
		            break;
		        }
		    }

			luceneQuery.append("+" + ESMappingConstants.SYSTEM_TYPE + ":false ");
            luceneQuery.append("-contentType:forms ");
            luceneQuery.append("-contentType:Host ");
		}

        final String finalSort = getFinalSort(fields, orderBy, st, structureInodes);

		// Stores (database name,type description) pairs to catch certain field types.
		List<Field> targetFields = new ArrayList<>();

		if(st!=null  && structureInodes.length == 1){
		    targetFields = FieldsCache.getFieldsByStructureInode(st.getInode());
		}

		Map<String,String> fieldContentletNames = new HashMap<>();
		Map<String,Field> decimalFields = new HashMap<>();//DOTCMS-5478
		for( Field f : targetFields ) {
			fieldContentletNames.put(f.getFieldContentlet(), f.getFieldType());
			if(f.getFieldContentlet().startsWith("float")){
				decimalFields.put(st.getVelocityVarName()+"."+f.getVelocityVarName(), f);
			}
		}
		CategoryAPI catAPI = APILocator.getCategoryAPI();
		Category category=null;
		String categoriesvalues="";
		boolean first = true;
		boolean allLanguages = true;
		for (String cat : categories) {
			 try {
				 category=catAPI.find(cat, currentUser, false);
			} catch (DotDataException e) {
				Logger.error(this, "Error trying to obtain the categories", e);
			} catch (DotSecurityException e) {
				Logger.error(this, " Permission error trying to obtain the categories", e);
			}
			if(!first){
				categoriesvalues+=" ";
			}
			categoriesvalues+="categories:"+ category.getCategoryVelocityVarName();
			first = false;
		}
		categoriesvalues = categoriesvalues.trim();
		for (int i = 0; i < fields.size(); i = i + 2) {
			String fieldName = (String) fields.get(i);
			String fieldValue = "";
			try{
				fieldValue = (String) fields.get(i + 1);
				//http://jira.dotmarketing.net/browse/DOTCMS-2656 add the try catch here in case the value is null.
			}catch (Exception e) {}
			if (UtilMethods.isSet(fieldValue)) {
                Optional<Relationship> childRelationship = getRelationshipFromChildField(st, fieldName);
                if (childRelationship.isPresent()) {
                    //Getting related identifiers from index when filtering by parent
                    final List<String> relatedContent = getRelatedIdentifiers(currentUser, offset,
                            relatedQueryByChild, finalSort, fieldValue,
                            childRelationship);

                    relatedQueryByChild.append(fieldName).append(StringPool.COLON).append(fieldValue);

                    if (!relatedContent.isEmpty()) {
                        //creates an intersection of identifiers when filtering by multiple relationship fields
                        if (relatedIdentifiers.isEmpty()){
                            relatedIdentifiers.addAll(relatedContent);
                        } else{
                            relatedIdentifiers = relatedIdentifiers.stream().filter(relatedContent::contains).collect(
                                    Collectors.toList());
                        }

						relationshipFields.add(new RelationshipFieldData(fieldName, fieldValue));
                        continue;
                    }
                }

				if(fieldsSearch.containsKey(fieldName)){//DOTCMS-5987, To handle lastSearch for multi-select fields.
					fieldsSearch.put(fieldName, fieldsSearch.get(fieldName)+","+fieldValue);
				}else{
					fieldsSearch.put(fieldName, fieldValue);
				}
				if(fieldName.equalsIgnoreCase("languageId")){
					if (UtilMethods.isSet(sess)) {
						sess.setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(fieldValue));
					}
					allLanguages = false;
				}
				if(fieldName.equalsIgnoreCase("conhost")){
					fieldValue = fieldValue.equalsIgnoreCase("current") ?
							Host.class.cast(sess.getAttribute(WebKeys.CURRENT_HOST)).getIdentifier()
							: fieldValue;

					if(!filterSystemHost  && !fieldValue.equals(Host.SYSTEM_HOST)){
						try {
							luceneQuery.append("+(conhost:" + fieldValue + " conhost:" + APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), true).getIdentifier() + ") ");
						} catch (Exception e) {
							Logger.error(ContentletAjax.class,e.getMessage(),e);
						}
					}else{
						try {
							luceneQuery.append("+conhost:" + fieldValue + "* ");
						} catch (Exception e) {
							Logger.error(ContentletAjax.class,e.getMessage(),e);
						}
					}
				} else if (ESMappingConstants.WORKFLOW_SCHEME.equalsIgnoreCase(fieldName)) {

					try {
						luceneQuery.append("+(" + ESMappingConstants.WORKFLOW_SCHEME + ":" + fieldValue + "*) ");
						sess.setAttribute(ESMappingConstants.WORKFLOW_SCHEME, fieldValue);
					} catch (Exception e) {
						Logger.error(ContentletAjax.class,e.getMessage(),e);
					}
				} else if (ESMappingConstants.WORKFLOW_STEP.equalsIgnoreCase(fieldName)) {

					try {

						if (ESMappingConstants.WORKFLOW_CURRENT_STEP_NOT_ASSIGNED_VALUE.equalsIgnoreCase(fieldValue)) { // special case for [Not Assigned]

							luceneQuery.append("+(" + ESMappingConstants.WORKFLOW_CURRENT_STEP + ":" + fieldValue + "*) ");
						} else {

							luceneQuery.append("+(" + ESMappingConstants.WORKFLOW_STEP + ":" + fieldValue + "*) ");
						}
                        sess.setAttribute(ESMappingConstants.WORKFLOW_STEP, fieldValue);
					} catch (Exception e) {
						Logger.error(ContentletAjax.class,e.getMessage(),e);
					}
				} else {
						String fieldbcontentname="";
						String fieldVelocityVarName = "";
						Boolean isStructField=false;
						String fieldVelName = "";
						if(st != null && fieldName.startsWith(st.getVelocityVarName() + ".")){
							fieldVelName = fieldName.substring(fieldName.indexOf(".") + 1, fieldName.length());
						}
						else if(fieldName.startsWith(".")) {
                            // http://jira.dotmarketing.net/browse/DOTCMS-6433
                            // weird case due to rare dwr bug on safari
                            fieldVelName = fieldName.substring(1);
                        }
						else{
							fieldVelName = fieldName;
						}
						Field thisField = null;

						for (Field fd : targetFields) {
							if (fd.getVelocityVarName().equals(fieldVelName) || fd.getFieldContentlet().equals(fieldVelName)) {
								fieldbcontentname=fd.getFieldContentlet();
								fieldVelocityVarName = fd.getVelocityVarName();
								isStructField=true;
								thisField = fd;
								break;
							}
						}

						String wildCard = " ";
						if(!fieldName.equals("languageId") && fieldbcontentname.startsWith("text") ){
							wildCard = ( fieldContentletNames.containsKey(fieldName) && fieldContentletNames.get(fieldName).equals(Field.FieldType.SELECT.toString()) ) ? " " : "*";
						}

						if( fieldbcontentname.startsWith("text") ){

							if(thisField.getFieldType().equals(Field.FieldType.KEY_VALUE.toString())){
								fieldValue = fieldValue.trim();
								boolean hasQuotes = fieldValue != null && fieldValue.length() > 1 && fieldValue.endsWith("\"") && fieldValue.startsWith("\"");
								if(hasQuotes){
									fieldValue = fieldValue.replaceFirst("\"", "");
									fieldValue = fieldValue.substring(0, fieldValue.length()-1);
								}
								try{
									String[] splitter = fieldValue.split(":");
									String metakey = "";
									for(int x=0;x< splitter.length-1;x++){
										metakey+= splitter[x];
									}
									metakey = StringUtils.camelCaseLower(metakey);
									String metaVal = "*" +splitter[splitter.length-1]+"*";
									fieldValue = metakey + ":" + metaVal;

									if (fieldVelocityVarName.equals(FileAssetAPI.META_DATA_FIELD)){
										luceneQuery.append("+" + fieldVelocityVarName + "." + fieldValue.toString().replaceAll(specialCharsToEscapeForMetaData, "\\\\$1") + " ");
									}else{
										luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName + "." + fieldValue.toString().replaceAll(specialCharsToEscapeForMetaData, "\\\\$1") + " ");
									}

								} catch (Exception e) {
									Logger.debug(this, "An error occured when processing field name '" + fieldbcontentname + "'");
								}
							}else {
								fieldValue = fieldValue.trim();
								final boolean hasQuotes = fieldValue != null && fieldValue.length() > 1 && fieldValue.endsWith("\"") && fieldValue.startsWith("\"");
								if(hasQuotes){
									fieldValue = CharMatcher.is('\"').trimFrom(fieldValue);
									fieldValue = fieldValue.trim();
								}

								String valueDelimiter = wildCard;
								if (fieldValue.startsWith("\"") && fieldValue.endsWith("\"")) {
									valueDelimiter = "";
								} else if (hasQuotes) {
									valueDelimiter = "\"";
								}

								// if part of the urlmap pattern, use the raw field to match
                              if(st.getUrlMapPattern()!=null && st.getUrlMapPattern().contains("{" +fieldVelocityVarName + "}" )) {
                                    
                                    for(String x : fieldValue.split("[,|\\s+]")) {
                                        luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName +"_dotraw:")
                                        .append(valueDelimiter + x + valueDelimiter + " ");
                                    }
                              }else {
                                  for(String x : fieldValue.split("[,|\\s+]")) {
                                      luceneQuery.append("+(" + st.getVelocityVarName() + "." + fieldVelocityVarName + ":")
                                      .append(valueDelimiter + x + valueDelimiter + " ");
                                      luceneQuery.append(" " + st.getVelocityVarName() + "." + fieldVelocityVarName + "_dotraw:")
                                      .append(valueDelimiter + x + valueDelimiter + ") ");
                                      
                                      
                                  }
                              }
							}
						}
						else if(fieldbcontentname.startsWith("system")
								&& APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(st.getInode(), fieldVelocityVarName) instanceof TagField) {
							String[] splitValues = fieldValue.split(",");
							for(String splitValue : splitValues)
							{
								splitValue = splitValue.trim();
								final String valueForQuery = ESUtils.escape(splitValue);
								String valueDelimiter = "\"";
								if (valueForQuery.startsWith("\"") && valueForQuery.endsWith("\"")) {
									valueDelimiter = "";
								}
								luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName+ ":"
										+ valueDelimiter + valueForQuery + valueDelimiter + " ");
							}
						}
						else if( fieldbcontentname.startsWith("date") ){
							if (!(fieldValue.contains(StringPool.OPEN_BRACKET)
									&& fieldValue.toLowerCase().contains("to")
									&& fieldValue.contains(StringPool.CLOSE_BRACKET))) {
								final StringBuilder dateRange = new StringBuilder();
								dateRange.append(StringPool.OPEN_BRACKET).append(fieldValue)
										.append(" TO ").append(fieldValue)
										.append(StringPool.CLOSE_BRACKET);

								luceneQuery.append(
										"+" + st.getVelocityVarName() + "." + fieldVelocityVarName
												+ ":" + dateRange + " ");
							} else{
								luceneQuery.append("+" + st.getVelocityVarName() +"."+ fieldVelocityVarName + ":" + fieldValue + " ");
							}

						} else {
							if(!isStructField){
							    String fieldValueStr =  fieldValue.toString();
							    if(!fieldValueStr.contains("'") || fieldValueStr.contains("\"")){
							        fieldValueStr = fieldValueStr.replaceAll("\\*", "");
							        while(fieldValueStr.contains("  ")){
							        	fieldValueStr = fieldValueStr.replace("  ", " ");
							        }

									fieldValueStr = fieldValueStr.replaceAll(specialCharsToEscape, "\\\\$1");

							        if(fieldName.equals("languageId") || fieldValueStr.contains("-")){
										luceneQuery.append("+" + fieldName +":" + fieldValueStr + " ");
									}else{
										luceneQuery.append("+" + fieldName +":" + fieldValueStr + "* ");
									}
							        
							        if("catchall".equals(fieldName)) {
							           
							            luceneQuery.append(" title:'" + fieldValueStr + "'^15 ");
                                        final String[] titleSplit = fieldValueStr.split("[,|\\s+]");
                                        if (titleSplit.length > 1) {
                                            for (final String term : titleSplit) {
                                                luceneQuery.append(" title:" + term + "^5 ");
                                            }
                                        }
							            luceneQuery.append(" title_dotraw:*" + fieldValueStr + "*^5 ");
							        }
							        
							        
							    } else{
							        luceneQuery.append("+" + fieldName +":" + fieldValueStr + " ");
							   }
							}
							else {
								luceneQuery.append("+" + st.getVelocityVarName() +"."+ fieldVelocityVarName + ":" + fieldValue.toString() + wildCard + " ");
							}
						}

				}
			}
		}
		if(allLanguages && (UtilMethods.isSet(sess) && sess.getAttribute(WebKeys.LANGUAGE_SEARCHED) == null)) {
				sess.setAttribute(WebKeys.LANGUAGE_SEARCHED, String.valueOf(0));

		}

		if(UtilMethods.isSet(categoriesvalues)){
			luceneQuery.append("+(" + categoriesvalues + ") " );
		}

		lastSearchMap.put("fieldsSearch", fieldsSearch);
		lastSearchMap.put("categories", categories);

		//Adding the headers as the second row of the results
		for (Field f : targetFields) {
		    if (f.isListed()) {
		        fieldsMapping.put(f.getVelocityVarName(), f);
		        headers.add(f.getMap());
		    }
		}

		lastSearchMap.put("showDeleted", showDeleted);
		lastSearchMap.put("filterSystemHost", filterSystemHost);
		lastSearchMap.put("filterLocked", filterLocked);
		lastSearchMap.put("filterUnpublish", filterUnpublish);


		if(!showDeleted)
			luceneQuery.append("+deleted:false ");
		else
			luceneQuery.append("+deleted:true ");
		lastSearchMap.put("page", page);


		if(filterLocked)
			luceneQuery.append("+locked:true ");

		if(filterUnpublish)
			luceneQuery.append("+live:false ");

		/*if we have a date*/
		if(modDateFrom!=null || modDateTo!=null){
			String dates =" +modDate:[";
			dates+= (modDateFrom!=null) ? modDateFrom : "18000101000000";
			dates+= " TO ";
			dates+= (modDateTo!=null)? modDateTo : "30000101000000";
			dates+="]";
			luceneQuery.append(dates);
		}

		lastSearchMap.put("orderBy", orderBy);

		luceneQuery.append(" +working:true");

		if (!VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
			luceneQuery.append(" +(" + ESMappingConstants.VARIANT + ":" + variantName + " OR " +
					ESMappingConstants.VARIANT + ":default)");
		} else {
			luceneQuery.append(" +" + ESMappingConstants.VARIANT + ":default");
		}

        final String luceneQueryToShow= luceneQuery.toString().replaceAll("\\s+", " ");

		//Executing the query
		long before = System.currentTimeMillis();
		PaginatedArrayList <ContentletSearch> hits = new PaginatedArrayList <>();
		long totalHits=0;
		try{
			final String luceneQueryTOElasticSearch = relatedIdentifiers.isEmpty() ?
					luceneQuery.toString() :
					appendRelatedIdentifierToQuery(luceneQuery.toString(), relatedIdentifiers);

			hits =(PaginatedArrayList)conAPI.searchIndex(luceneQueryTOElasticSearch, perPage, offset, finalSort, currentUser, false);

			totalHits = hits.getTotalResults();
		}catch (Exception pe) {
			Logger.error(ContentletAjax.class, "Unable to execute Lucene Query", pe);
		}
		long after = System.currentTimeMillis();
		Logger.debug(ContentletAjax.class, "searchContentletsByUser: Time to search on lucene =" + (after - before) + " ms.");


		before = System.currentTimeMillis();

		//The results list returned to the page
		List<Object> results = new ArrayList<>();

		//Adding the result counters as the first row of the results
		Map<String, Object> counters = new HashMap<>();
		results.add(counters);


		if (headers.size() == 0) {
			Map<String, String> fieldMap = new HashMap<> ();
			fieldMap.put("fieldVelocityVarName", "__title__");
			fieldMap.put("fieldName", Try.of(() -> LanguageUtil.get(currentUser, "Title")).getOrElse("Title"));
			headers.add(fieldMap);

			// if there is a type selected, does not make sense to show it on the list.
			if (Structure.STRUCTURE_TYPE_ALL.equals(structureInode) || this.hasManyContentTypes(structureInode)) {
				fieldMap = new HashMap<>();
				fieldMap.put("fieldVelocityVarName", "__type__");
				fieldMap.put("fieldName", Try.of(() -> LanguageUtil.get(currentUser, "Type")).getOrElse("Type"));
			}
			headers.add(fieldMap);


		}

		final Map<String, String> fieldMap = new HashMap<> ();
		fieldMap.put("fieldVelocityVarName", "__wfstep__");
		fieldMap.put("fieldName", Try.of(() -> LanguageUtil.get(currentUser, "Step")).getOrElse("Step"));
		headers.add(fieldMap);

		results.add(this.distinct(headers, headerFieldMap -> Map.class.cast(headerFieldMap).get("fieldVelocityVarName")));

		// we add the total hists for the query
		results.add(totalHits);

		List<String> expiredInodes=new ArrayList<>();

		boolean exporting = perPage > MAX_LIMIT;

//		Adding the query results
		addContentMapsToResults(structureInode, perPage, currentUser, fieldsMapping, hits, results,
				expiredInodes, exporting);

		long total = hits.getTotalResults();
		counters.put("total", total);

		if (page == 0)
			counters.put("hasPrevious", false);
		else
			counters.put("hasPrevious", page != 1);

		if (page == 0)
			counters.put("hasNext", false);
		else
			counters.put("hasNext", perPage * page < total);

		// Data to show in the bottom content listing page
		String luceneQueryToShow2= luceneQuery.toString();
		luceneQueryToShow2=luceneQueryToShow2.replaceAll("\\+languageId:[0-9]*\\*?","").replaceAll("\\+deleted:[a-zA-Z]*","")
			.replaceAll("\\+working:[a-zA-Z]*","").replaceAll("\\s+", " ").trim();

		counters.put("luceneQueryRaw", luceneQueryToShow);

		final String luceneQueryFormatted = luceneQueryToShow2.replace("\"","\\${esc.quote}");
		final String velocityCode = String.format(VELOCITY_CODE_TEMPLATE,
				luceneQueryFormatted,
				finalSort,
				UtilMethods.isSet(relationshipFields) ? getRelationshipVelocityCode(relationshipFields) : "...");

		counters.put("velocityCode", velocityCode);
        counters.put("relatedQueryByChild",
                relatedQueryByChild.length() > 0 ? relatedQueryByChild.toString() : null);
		counters.put("sortByUF", finalSort);
		counters.put("expiredInodes", expiredInodes);

		long end = total;
		if (page != 0)
			end = page * perPage;

		end = (end < total ? end : total);

		int begin = 1;
		if (page != 0)
			begin = (page == 0 ? 0 : (page - 1) * perPage);

		begin = (end != 0 ? begin + 1: begin);

		int totalPages = 1;
		if (page != 0)
			totalPages = (int) Math.ceil((float) total / (float) perPage);

		counters.put("begin", begin);
		counters.put("end", end);
		counters.put("totalPages", totalPages);

		after = System.currentTimeMillis();
		Logger.debug(ContentletAjax.class, "searchContentletsByUser: Time to process results= " + (after - before) + " ms.");

		return results;
	}

	private String appendRelatedIdentifierToQuery(final String luceneQuery,
			final List<String> relatedIdentifiers) {

		Preconditions.checkArgument(UtilMethods.isSet(relatedIdentifiers), "relatedIdentifiers can not be empty or null");
		final StringBuffer result = new StringBuffer(luceneQuery);

		return result.append(" +identifier:(")
				.append(String.join(" OR ", relatedIdentifiers)).append(") ")
				.toString();
	}

	private String getRelationshipVelocityCode(final List<RelationshipFieldData> relationshipFields) {
		final String setCodeTemplate = "<span style='margin-left: 20px'>"
				+ "#set( $related_%s = $dotcontent.pullRelatedField( \"$con.identifier\", \"%s\", \"+identifier:%s\") )"
				+ "</span>";
		final String conditionCodeTemplate = "!$related_%s.isEmpty()";
		final String velocityCodeTemplate = "<span style='margin-left: 20px'>#if (%s)</span><br>"
				+ "<span style='margin-left: 40px'>...</span><br>"
				+ "<span style='margin-left: 20px'>#end</span>";

		final String setCode = relationshipFields.stream()
				.map(relationshipField -> String.format(setCodeTemplate,
						relationshipField.fieldName.replaceAll("\\.", StringPool.BLANK),
						relationshipField.fieldName,
						relationshipField.fieldValue))
				.collect(Collectors.joining("\n"));

		final String conditionCode = relationshipFields.stream()
				.map(relationshipField -> String.format(conditionCodeTemplate, relationshipField.fieldName.replaceAll("\\.", StringPool.BLANK)))
				.collect(Collectors.joining(" && "));

		return setCode + "<br>" + String.format(velocityCodeTemplate, conditionCode);
	}

	private void addContentMapsToResults(String structureInode, int perPage, User currentUser,
			Map<String, Field> fieldsMapping, PaginatedArrayList<ContentletSearch> hits,
			List<Object> results, List<String> expiredInodes, final boolean exporting) {

		final Map<String, Map<String, String>> searchResults = new LinkedHashMap<>();

		for (int i = 0; ((i < perPage) && (i < hits.size())); ++i) {

			Map<String, String> searchResult = null;

			try {
				final ContentletSearch contentletSearch = hits.get(i);
				final Contentlet con = APILocator
						.getContentletAPI().find(contentletSearch.getInode(), currentUser, false);

				Identifier ident = APILocator.getIdentifierAPI().find(con);
				if (!con.isLive()) {
					if (UtilMethods
							.isSet(ident.getSysExpireDate()) && ident.getSysExpireDate().before(new Date()))
						expiredInodes.add(con.getInode()); // it is unpublished and can't be manualy published
				}

				searchResult = new HashMap<>();

				final Map<String, String> searchResultFromMap = searchResults.get(con.getInode());

				if (UtilMethods.isSet(searchResultFromMap)) {
					final String variantFromMap = searchResultFromMap.get("variant");

					if (VariantAPI.DEFAULT_VARIANT.name().equals(variantFromMap)) {
						searchResults.put(con.getInode(), searchResult);
					} else {
						continue;
					}

				}

				ContentType type = con.getContentType();
				searchResult.put("typeVariable", type.variable());
				searchResult.put("baseType",type.baseType().name());
				searchResult.put("contentTypeIcon",type.icon());

				for (final String fieldContentlet : fieldsMapping.keySet()) {
					String fieldValue = null;
					if (con.getMap() != null && con.getMap().get(fieldContentlet) != null) {
						fieldValue = (con.getMap().get(fieldContentlet)).toString();
					}

					final Field field = fieldsMapping.get(fieldContentlet);
					if (UtilMethods.isSet(fieldValue) && field.getFieldType().equals(FieldType.DATE.toString()) ||
							UtilMethods.isSet(fieldValue) && field.getFieldType().equals(FieldType.TIME.toString()) ||
							UtilMethods.isSet(fieldValue) && field.getFieldType().equals(FieldType.DATE_TIME.toString())) {
						try {
						    
							Date date = con.getDateProperty(fieldContentlet);
							if (field.getFieldType().equals(FieldType.DATE.toString()))
								fieldValue = UtilMethods.dateToHTMLDate(date);
							if (field.getFieldType().equals(FieldType.TIME.toString()))
								fieldValue = UtilMethods.dateToHTMLTime(date);
							if (field.getFieldType().equals(FieldType.DATE_TIME.toString()))
								fieldValue = UtilMethods.dateToHTMLDate(date) + " " + UtilMethods.dateToHTMLTime(date);
						} catch (Exception e) {
							Logger.error(ContentletAjax.class, e.getMessage(), e);
							throw new DotRuntimeException(e.getMessage(), e);
						}
					} else if (field.getFieldType().equals(FieldType.CHECKBOX.toString()) || field.getFieldType().equals(
							FieldType.MULTI_SELECT.toString())) {
						if (UtilMethods.isSet(fieldValue))
							fieldValue = fieldValue.replaceAll("# #", ",").replaceAll("#", "");
					}

					//We need to replace the URL value from the contentlet with the one in the Identifier only for pages.
					if (("url").equals(fieldContentlet) &&
							type != null &&
							type  instanceof PageContentType &&
							UtilMethods.isSet(ident) &&
							UtilMethods.isSet(ident.getAssetName())) {
						fieldValue = ident.getAssetName();
					}

					// when a content type is selected and the field is listed and binary, instead of displaying the path, must display just the name
					if (!Structure.STRUCTURE_TYPE_ALL.equals(structureInode) && field.isListed() && field.getFieldType().equals(
							FieldType.BINARY.toString())) {

						final String binaryName = con.getBinary(fieldContentlet)!=null ?
								con.getBinary(fieldContentlet).getName() : "NOT_FOUND";

						searchResult.put(fieldContentlet+"_title_", binaryName);
					}

					searchResult.put(fieldContentlet, fieldValue);
				}

				searchResult.put("inode", con.getInode());
				searchResult.put("Identifier",con.getIdentifier());
				searchResult.put("identifier", con.getIdentifier());
				searchResult.put("variant", con.getVariantId());

				final Contentlet contentlet = con;
				searchResult.put("__title__", conAPI.getName(contentlet, currentUser, false));

				String spanClass = UtilHTML.getIconClass(contentlet);

				String typeStringToShow = type.name();
				searchResult.put("__type__", "<div class='typeCCol'><span class='" + spanClass +"'></span>&nbsp;" + typeStringToShow +"</div>");

				String fieldValue = UtilMethods.dateToHTMLDate(con.getModDate()) + " " + UtilMethods.dateToHTMLTime(con.getModDate());
				searchResult.put("hasTitleImage", String.valueOf(con.getTitleImage().isPresent()));
	            if(contentlet.getTitleImage().isPresent()) {
	                searchResult.put("titleImage", contentlet.getTitleImage().get().variable());
	            }
				searchResult.put("modDate", fieldValue);
				searchResult.put("modDateMilis", String.valueOf(con.getModDate().getTime()));
				String user = "";
				User contentEditor = null;
				try {
					contentEditor = APILocator.getUserAPI().loadUserById(con.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
				} catch (Exception e1) {
					Logger.error(ContentletAjax.class,e1.getMessage() + " no such user.  did mod_user get deleted?");
					Logger.debug(ContentletAjax.class,e1.getMessage(), e1);
					contentEditor = new User();
				}

				if (contentEditor.getFirstName() == null || contentEditor.getLastName() == null) {
					user = con.getModUser();
				} else {
					user = contentEditor.getFullName();
				}

				if(!exporting) {

					PermissionAPI permissionAPI = APILocator.getPermissionAPI();
					List<Permission> permissions = null;
					try {
						permissions = permissionAPI.getPermissions(con);
					} catch (DotDataException e) {
					}

					StringBuffer permissionsSt = new StringBuffer();
					Boolean ownerCanRead = false;
					Boolean ownerCanWrite = false;
					Boolean ownerCanPub = false;
					for (Permission permission : permissions) {
						String str = "P" + permission.getRoleId() + "." + permission.getPermission()
								+ "P ";
						if (permissionsSt.toString().indexOf(str) < 0) {
							permissionsSt.append(str);
						}
						try {
							if (APILocator.getRoleAPI().loadCMSOwnerRole().getId()
									.equals(String.valueOf(permission.getRoleId()))) {
								if (permission.getPermission() == PERMISSION_READ) {
									ownerCanRead = true;
								} else if (permission.getPermission() == PERMISSION_WRITE) {
									ownerCanRead = true;
									ownerCanWrite = true;
								} else if (permission.getPermission() == PERMISSION_PUBLISH) {
									ownerCanRead = true;
									ownerCanWrite = true;
									ownerCanPub = true;
								}
							}
						} catch (DotDataException e) {

						}
					}

					searchResult.put("ownerCanRead", ownerCanRead.toString());
					searchResult.put("ownerCanWrite", ownerCanWrite.toString());
					searchResult.put("ownerCanPublish", ownerCanPub.toString());
					searchResult.put("permissions", permissionsSt.toString());

				}

				searchResult.put("owner", con.getOwner());
				searchResult.put("modUser", user);
				Boolean working = con.isWorking();
				searchResult.put("working", working.toString());
				Boolean live = con.isLive();
				searchResult.put("statusIcons", UtilHTML.getStatusIcons(con));

				searchResult.put("hasLiveVersion", "false");

				final boolean hasLiveVersion = APILocator.getVersionableAPI().hasLiveVersion(con);
				if (live && hasLiveVersion) {

					searchResult.put("hasLiveVersion", "true");
				}
				if (!live && working && !con.isArchived()) {
					if (hasLiveVersion) {
						searchResult.put("hasLiveVersion", "true");
						searchResult.put("allowUnpublishOfLiveVersion", "true");

						Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
								.getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());

						if(cvi.isPresent()) {
							searchResult.put("inodeOfLiveVersion",
									cvi.get().getLiveInode());
						}
					}
				}

				searchResult.put("live", live.toString());
				Boolean isdeleted = con.isArchived();
				searchResult.put("deleted", isdeleted.toString());
				Boolean locked = con.isLocked();
				searchResult.put("locked", locked.toString());
				searchResult.put("structureInode", con.getStructureInode());

				if(!exporting) {
					setCurrentStep(currentUser, searchResult, contentlet);
				}

				searchResult.put("contentStructureType", "" + con.getStructure().getStructureType());

				if(!exporting) {
					// Workflow Actions
					final JSONArray wfActionMapList = this
							.getAvailableWorkflowActionsListingJson(currentUser, con);
					searchResult.put("wfActionMapList", wfActionMapList.toString());
				}
				// End Workflow Actions

				//searchResult.put("structureName", st.getVelocityVarName());
				Long languageId = con.getLanguageId();
				searchResult.put("languageId", languageId.toString());
				final Language language = APILocator.getLanguageAPI().getLanguage(languageId);
				searchResult.put("language", language.toString());

				//Add mimeType
				if(type.baseType().getType() == BaseContentType.FILEASSET.getType()){
					searchResult.put("mimeType", APILocator.getFileAssetAPI()
							.getMimeType(APILocator.getFileAssetAPI().fromContentlet(con).getUnderlyingFileName()));
				} else if(type.baseType().getType() == BaseContentType.HTMLPAGE.getType()){
					searchResult.put("mimeType", "application/dotpage");
				} else if(type.baseType().getType() == BaseContentType.DOTASSET.getType()){
					searchResult.put("mimeType", APILocator.getFileAssetAPI()
							.getMimeType(con.getBinary(DotAssetContentType.ASSET_FIELD_VAR)));
				} else {
					searchResult.put("mimeType", "");
				}

				searchResult.put("__icon__",UtilHTML.getIconClass(con ));
			} catch (DotSecurityException e) {

				Logger.debug(this, "Does not have permissions to read the content: " + searchResult, e);
				searchResult = null;

			} catch (Exception e) {

				Logger.error(this, "Couldn't read the content: " + searchResult, e);
				searchResult = null;

			}

			if (UtilMethods.isSet(searchResult)) {
				searchResults.put(searchResult.get("inode"), searchResult);
			}
		}

		results.addAll(searchResults.values());
	}

	List<String> getRelatedIdentifiers(User currentUser, int offset,
            StringBuilder relatedQueryByChild, String finalSort, String fieldValue,
            Optional<Relationship> childRelationship) throws DotDataException {
        final Contentlet relatedParent = conAPI
                .findContentletByIdentifierAnyLanguage(fieldValue);
        final List<String> relatedContent = conAPI
                .getRelatedContent(relatedParent, childRelationship.get(), true,
                        currentUser, false, RELATIONSHIPS_FILTER_CRITERIA_SIZE,
                        offset / RELATIONSHIPS_FILTER_CRITERIA_SIZE, finalSort).stream()
                .map(cont -> cont.getIdentifier()).collect(Collectors.toList());

        if (relatedQueryByChild.length() > 0) {
            relatedQueryByChild.append(StringPool.COMMA);
        }
        return relatedContent;
    }

    private boolean hasManyContentTypes(final String structureInode) {

		return null != structureInode && structureInode.split(StringPool.COMMA).length > 1;
	}

	/**
     *
     * @param fields
     * @param orderBy
     * @param st
     * @param structureInodes
     * @return
     */
    private String getFinalSort(final List<String> fields, final String orderBy,
            final Structure st, final String[] structureInodes) {

	    final String finalSort;

        if (!UtilMethods.isSet(orderBy)) {
            finalSort = "modDate desc";
        }else if(orderBy.equalsIgnoreCase("score,modDate desc") && !hasCustomFields(fields)){
            finalSort = "modDate desc";
        }else if (orderBy.endsWith("__wfstep__")){
            finalSort = "wfCurrentStepName";
        }else if (orderBy.endsWith("__wfstep__ desc")){
            finalSort = "wfCurrentStepName desc";
        }else{
            if(orderBy.charAt(0)=='.'){
                if (structureInodes.length > 1) {
                    finalSort = orderBy.substring(1);
                } else {
                    finalSort = st.getVelocityVarName() + orderBy;
                }
            } else{
                finalSort = orderBy;
            }
        }

        return finalSort;
    }

    /**
     *
     * @param fields
     * @return
     */
    private boolean hasCustomFields(final List<String> fields) {
        //Current default fields: conHost, languageId
        final String[] defaultFields = {"conHost", "languageId"};

        //Verify all the default fields are contained. Otherwise, included fields are empty
        for (int i = 0; i < fields.size(); i += 2) {
            final int currentIndex = i;
            if (Arrays.stream(defaultFields)
                    .noneMatch(field -> field.equalsIgnoreCase(fields.get(currentIndex)))
                    && i + 1 < fields.size() && UtilMethods.isSet(fields.get(i+1))) {
                //Case when it is not a default field and has a value set (ie. a user searchable field)
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a relationship field from the child side
     * @param st
     * @param fieldName
     * @return
     */
    private Optional<Relationship> getRelationshipFromChildField(final Structure st,
            final String fieldName) {

        if (st != null) {
            final String fieldVar =
                    fieldName.split("\\.").length > 1 ? fieldName.split("\\.")[1] : fieldName;
            final Field field = st.getFieldVar(fieldVar);

            if (field != null && field.getFieldType().equals(FieldType.RELATIONSHIP.toString())) {
                Relationship relationship = APILocator.getRelationshipAPI()
                        .byTypeValue(field.getFieldRelationType());

                //Considers Many to One relationships where the fieldName might not contain the relation type value
                if (null == relationship && !field.getFieldRelationType().contains(".")) {
                    relationship = APILocator.getRelationshipAPI()
                            .byTypeValue(st.getVelocityVarName() + StringPool.PERIOD + fieldVar);
                }
                return relationship != null?Optional.of(relationship):Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void setCurrentStep(final User currentUser,
								final Map<String, String> searchResult,
								final Contentlet contentlet) throws DotDataException, LanguageException {
		try {
			final Optional<WorkflowStep> step = APILocator.getWorkflowAPI().findCurrentStep(contentlet);
			final String currentStep = (step.isPresent()) ?
					step.get().getName() :
					LanguageUtil.get(currentUser, "workflow.notassigned");
			searchResult.put("__wfstep__", currentStep);
		} catch (InvalidLicenseException e) {
			searchResult.put("__wfstep__", LanguageUtil.get(currentUser, "workflow.licenserequired"));
		}
	}

	private boolean hasContentTypesInodeSeparator(String structureInode) {
		return structureInode.contains(CONTENT_TYPES_INODE_SEPARATOR);
	}

	@CloseDB
	@NotNull
	private JSONArray getAvailableWorkflowActionsListingJson(final User currentUser,
													  final Contentlet contentlet) throws DotDataException, DotSecurityException {

		final List<WorkflowAction> workflowActions = new ArrayList<>();

		try {
            workflowActions.addAll(APILocator.getWorkflowAPI()
					.findAvailableActionsListing(contentlet, currentUser)) ;
        } catch (Exception e) {
            Logger.error(this, "Could not load workflow actions : ", e);
        }

		final JSONArray wfActionMapList = new JSONArray();
        final boolean showScheme = (workflowActions!=null) ?  workflowActions.stream().collect(Collectors.groupingBy(WorkflowAction::getSchemeId)).size()>1 : false;
		for (WorkflowAction action : workflowActions) {

            boolean hasPushPublishActionlet = false;

            final JSONObject wfActionMap = new JSONObject();
            try {
				WorkflowScheme wfScheme = APILocator.getWorkflowAPI().findScheme(action.getSchemeId());
                wfActionMap.put("name", action.getName());
                wfActionMap.put("id", action.getId());
                wfActionMap.put("icon", action.getIcon());
                wfActionMap.put("assignable", action.isAssignable());
                wfActionMap.put("commentable", action.isCommentable() || UtilMethods.isSet(action.getCondition()));
                wfActionMap.put("requiresCheckout", action.requiresCheckout());
				if (action.hasMoveActionletActionlet() && !action.hasMoveActionletHasPathActionlet()) {

					wfActionMap.put("moveable", "true");
				}

                final List<WorkflowActionClass> actionlets =
						APILocator.getWorkflowAPI().findActionClasses(action);
                for (WorkflowActionClass actionlet : actionlets) {
                    if (actionlet.getActionlet() != null
                            && actionlet.getActionlet().getClass().getCanonicalName()
								.equals(PushPublishActionlet.class.getCanonicalName())) {

                        hasPushPublishActionlet = true;
                    }
                }

                wfActionMap.put("hasPushPublishActionlet", hasPushPublishActionlet);

                try {
                    final String actionNameStr = (showScheme) ? LanguageUtil.get(currentUser, action.getName()) +" ( "+LanguageUtil.get(currentUser,wfScheme.getName())+" )" : LanguageUtil.get(currentUser, action.getName());

                    wfActionMap.put("wfActionNameStr", actionNameStr);
                } catch (LanguageException e) {
                    Logger.error(this, "Could not load language key : " + action.getName());
                }

                wfActionMapList.add(wfActionMap);
            } catch (JSONException e1) {
                Logger.error(this, "Could not put property in JSONObject");
            }
        }

		return wfActionMapList;
	}

	@CloseDB
	public List<String[]> doSearchGlossaryTerm(String valueToComplete, String language)
			throws DotDataException, DotSecurityException {
		final int limit = Config.getIntProperty("glossary.term.max.limit", 15);
		List<String[]> list = new ArrayList<>(limit);
		final User systemUser = APILocator.systemUser();
		final long languageId = Long.parseLong(language);

		valueToComplete = valueToComplete.toLowerCase();

		final List<String> listAddedKeys =
				isLocalizationEnhancementsEnabled() ? collectLanguageVariables(valueToComplete,
						list, languageId) : collectLanguageKeys(valueToComplete, list, languageId);

		if (list.size() < limit) {
			List<KeyValue> languageVariables = languageVariableAPI.getAllLanguageVariablesKeyStartsWith(
					valueToComplete, languageId, systemUser, limit);
			for (KeyValue languageVariable : languageVariables) {
				if (!listAddedKeys.contains(languageVariable.getKey())) {
					final String[] term = new String[]{languageVariable.getKey(),
							(70 < languageVariable.getValue().length() ? languageVariable.getValue()
									.substring(0, 69) : languageVariable.getValue())};
					list.add(term);
				}
				if (list.size() == limit) {
					break;
				}
			}
		}

		return list;
	}

	/**
	 * Collects the language keys that start with the valueToComplete from the properties file
	 * @param valueToComplete the value to complete
	 * @param list the list to add the terms
	 * @param languageId the language id
	 * @return the list of added keys
	 */
	private List<String> collectLanguageKeys(final String valueToComplete, final List<String[]> list, final long languageId) {
		final Language lang = langAPI.getLanguage(languageId);
		final List<String> listAddedKeys = new ArrayList<>();
		List<LanguageKey> props = langAPI.getLanguageKeys(lang);
		for (LanguageKey prop : props) {
			if (prop.getKey().toLowerCase().startsWith(valueToComplete)) {
				final String[] term = new String[]{prop.getKey(),
						(70 < prop.getValue().length() ? prop.getValue().substring(0, 69)
								: prop.getValue())};
				list.add(term);
				listAddedKeys.add(prop.getKey());
			}
		}
		return listAddedKeys;
	}

	/**
	 * Collects the language variables that start with the valueToComplete from the properties file
	 * @param valueToComplete the value to complete
	 * @param list the list to add the terms
	 * @param languageId the language id
	 * @return the list of added keys
	 * @throws DotDataException if an error occurs
	 */
	private List<String> collectLanguageVariables(final String valueToComplete, final List<String[]> list, final long languageId)
			throws DotDataException {
		final List<String> listAddedKeys = new ArrayList<>();
		final List<LanguageVariable> variables = languageVariableAPI.findVariables(languageId);
		for (LanguageVariable variable : variables) {
			if (variable.key().toLowerCase().startsWith(valueToComplete)) {
				final String[] term = new String[]{variable.key(),
						(70 < variable.value().length() ? variable.value().substring(0, 69)
								: variable.value())};
				list.add(term);
				listAddedKeys.add(variable.key());
			}
		}
		return listAddedKeys;
	}


	/**
	 * Publishes or unpublishes contentlets from a given list of identifiers.  You can have to publish within
	 * a specific language or all languages.  Set the languageId = 0 for all languages.
	 * @param identifiersList
	 * @param isPublish whether it should publish or unpublish the contentlets
	 * @param languageId if set to 0 will publish for all languages
	 * @return
	 */
	@CloseDB
	public List<Map<String, Object>> publishContentlets(List<String> identifiersList, boolean isPublish, long languageId) {
		List<Map<String, Object>> rows = new ArrayList<>();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		for (int x = 0; x < identifiersList.size(); x++) {

			String id = identifiersList.get(x);

			try {
				User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
				Contentlet contentlet = new Contentlet();
				List<Contentlet> contentletList = new ArrayList<>();
				if(languageId == 0){
					contentlet = conAPI.findContentletByIdentifier(id, false, langAPI.getDefaultLanguage().getId(), currentUser, false);
					contentletList = conAPI.getAllLanguages(contentlet, false, currentUser, false);
				}else{
					contentlet = conAPI.findContentletByIdentifier(id, false, languageId, currentUser, false);
					contentletList.add(contentlet);
				}


				for (Contentlet cont : contentletList) {

					if (isPublish) {

						if(!cont.isLive()){
							conAPI.publish(cont, currentUser, false);
						}

					} else {

						if (cont.isLive()) {
							conAPI.unpublish(cont, currentUser, false);
						}
					}
					if(languageId == 0){//DOTCMS-5182
						cont = conAPI.findContentletByIdentifier(id, false, langAPI.getDefaultLanguage().getId(), currentUser, false);
					}else{
						cont = conAPI.findContentletByIdentifier(id, false, languageId, currentUser, false);
					}
					rows.add(cont.getMap());
				}

			} catch (DotDataException e) {
				Logger.error(this, "Error trying to obtain the contentlets from the relationship.", e);

			}catch (DotSecurityException e) {
				Logger.error(this, "Security exception.", e);
			}
		}
		return rows;
	}

	/**
	 *
	 * @param
	 * @param
	 * @param
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 * @throws LanguageException
	 */
	//http://jira.dotmarketing.net/browse/DOTCMS-2273
	@CloseDB
	public Map<String,Object> saveContent(List<String> formData, boolean isAutoSave,boolean isCheckin, boolean publish) throws LanguageException, PortalException, SystemException {
	  Map<String,Object> contentletFormData = new HashMap<>();
	  Map<String,Object> callbackData = new HashMap<>();
	  List<String> saveContentErrors = new ArrayList<>();
	  User user = null;
      boolean clearBinary = true;//flag to check if the binary field needs to be cleared or not
      HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
      String newInode = "";

      String referer = "";
      String language = "";
      String strutsAction = "";
      String recurrenceDaysOfWeek="";

	  try {
            HibernateUtil.startTransaction();


	    int tempCount = 0;// To store multiple values opposite to a name. Ex: selected permissions & categories



        user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest)req);

		// get the struts_action from the form data
		for (String element:formData) {
			if(element!=null) {
    			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));
    			if (elementName.startsWith("_") && elementName.endsWith("cmd")) {
    				strutsAction = elementName.substring(0, elementName.indexOf("cmd"));
    				break;
    			}
			}
		}

		// Storing form data into map.
		for (String element:formData) {
			if (!com.dotmarketing.util.UtilMethods.isSet(element))
				continue;

			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));
			Object elementValue = element.substring(element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR) + WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR.length());

			if (element.startsWith(strutsAction))
				elementName = elementName.substring(elementName
						.indexOf(strutsAction)
						+ strutsAction.length());

			// Placed increments as Map holds unique keys.
			if(elementName.equals("read")
					||elementName.equals("write")
					||elementName.equals("publish")){

				tempCount++;
				elementName = "selected_permission_"+tempCount+elementName;
			}

			if(elementName.equals("categories")){
				tempCount++;
				elementName = elementName+tempCount+"_";
			}

			if(!UtilMethods.isSet(elementName))
				continue;

			if(!UtilMethods.isSet(elementValue))
				elementValue="";

			if(elementName.equals("referer"))
				referer = (String) elementValue;

			if(elementName.equals("languageId"))
				language = (String) elementValue;

			if ( elementName.equals("recurrenceDaysOfWeek")) {
				recurrenceDaysOfWeek= recurrenceDaysOfWeek + elementValue+ ",";
			}
			//http://jira.dotmarketing.net/browse/DOTCMS-3232
			if(elementName.equalsIgnoreCase("hostId")){
				callbackData.put("hostOrFolder",true);
			}

			if(elementName.startsWith("text")){
				elementValue = elementValue.toString().trim();
			}

			//http://jira.dotmarketing.net/browse/DOTCMS-3463
			if(elementName.startsWith("binary")){
				String binaryFileValue = (String) elementValue;
				File binaryFile = null;
				if(UtilMethods.isSet(binaryFileValue) && !binaryFileValue.equals("---removed---")){
					Contentlet binaryContentlet =  new Contentlet();
					try{
						binaryContentlet = conAPI.find(binaryFileValue, APILocator.getUserAPI().getSystemUser(), false);
					}catch(Exception e){
						Logger.error(this.getClass(), "Problems finding binary content " + binaryFileValue, e);
					}
					if(UtilMethods.isSet(binaryContentlet) && UtilMethods.isSet(binaryContentlet.getInode())){
						try {
							elementValue = binaryContentlet.getBinary(FileAssetAPI.BINARY_FIELD);
							binaryFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
									+ File.separator + user.getUserId() + File.separator + "binary1"
									+ File.separator + ((File)elementValue).getName());
							if(binaryFile.exists())
								elementValue = binaryFile;
						} catch (IOException e) {}
					}else{
						binaryFileValue = ContentletUtil.sanitizeFileName(binaryFileValue);
						binaryFile = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
								+ File.separator + user.getUserId() + File.separator + elementName
								+ File.separator + binaryFileValue);
						if(binaryFile.exists()) {
	    					try {
	    					    // https://github.com/dotCMS/dotCMS/issues/35
	    					    // making a copy just in case the transaction fails so
	    					    // we can have the file for possible next attempts
	                            File acopyFolder=new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
	                                    + File.separator + user.getUserId() + File.separator + elementName
	                                    + File.separator + UUIDGenerator.generateUuid());
	                            if(!acopyFolder.exists())
	                                acopyFolder.mkdir();
	                            File acopy=new File(acopyFolder, binaryFileValue);
	                            FileUtil.copyFile(binaryFile, acopy);
	                            elementValue = acopy;
	                        } catch (Exception e) {
                                Logger.error(this, "can't make a copy of the uploaded file:" + e, e);
                                String errorString = LanguageUtil.get(user,"message.event.recurrence.can.not.copy.uploaded.file");
                                saveContentErrors.add(errorString);
                                callbackData.put("saveContentErrors", saveContentErrors);
                                return callbackData;
                            }
						}
					}
				}else{
					elementValue = new File(binaryFileValue);
				}
			}
			contentletFormData.put(elementName, elementValue);
		}

		contentletFormData.put("recurrenceDaysOfWeek", recurrenceDaysOfWeek);

		if(contentletFormData.get("recurrenceOccurs")!=null &&
		        contentletFormData.get("recurrenceOccurs").toString().equals("annually")){

			if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) &&
					!UtilMethods.isSet((String)contentletFormData.get("specificDayOfMonthRecY")) &&
					!UtilMethods.isSet((String)contentletFormData.get("specificMonthOfYearRecY"))){
				String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.date");
				saveContentErrors.add(errorString);
			}

			if(Boolean.parseBoolean(contentletFormData.get("isSpecificDate").toString()) &&
					UtilMethods.isSet((String)contentletFormData.get("specificDayOfMonthRecY"))
					&& UtilMethods.isSet((String)contentletFormData.get("specificMonthOfYearRecY"))){
				try{
					Long.valueOf((String)contentletFormData.get("specificDayOfMonthRecY"));
					contentletFormData.put("recurrenceDayOfMonth", (String)contentletFormData.get("specificDayOfMonthRecY"));
				}catch (Exception e) {
					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.dayofmonth");
					saveContentErrors.add(errorString);
				}
				try{
					Long.valueOf((String)contentletFormData.get("specificMonthOfYearRecY"));
					contentletFormData.put("recurrenceMonthOfYear", (String)contentletFormData.get("specificMonthOfYearRecY"));
				}catch (Exception e) {
					String errorString = LanguageUtil.get(user,"message.event.recurrence.invalid.monthofyear");
					saveContentErrors.add(errorString);
				}
			}else{
				contentletFormData.put("recurrenceDayOfMonth", "0");
			}
		}

		  // if it is save and publish, the save event must be not generated
		  newInode = contentletWebAPI
				  .saveContent(contentletFormData, isAutoSave, isCheckin, user, !publish);
		
		final String workflowActionId = (String)contentletFormData.get("wfActionId");
		callbackData.put("isMoveAction", false);
		if(UtilMethods.isSet(workflowActionId)){
			final WorkflowAction workflowAction = APILocator.getWorkflowAPI().findAction(workflowActionId, APILocator.systemUser());
			if(null != workflowAction){
				callbackData.put("isMoveAction", ActionletUtil.isMoveableActionlet(workflowAction));
			}
		}

		  Contentlet contentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
		  if (null != contentlet) {
			  callbackData.put("isHtmlPage", contentlet.isHTMLPage());
			  callbackData.put("contentletType", contentlet.getContentType().variable());
			  callbackData.put("contentletBaseType", contentlet.getContentType().baseType().name());

			  //Cleaning up as we don't need more calculations as the Contenlet was deleted
			  if (contentletFormData.containsKey(WebKeys.CONTENTLET_DELETED)
					  && (Boolean) contentletFormData.get(WebKeys.CONTENTLET_DELETED)) {
				  contentlet = null;
			  }
		  }

            if(contentlet != null){
				callbackData.put("contentletIdentifier", contentlet.getIdentifier());
				callbackData.put("contentletInode", contentlet.getInode());
				callbackData.put("contentletLocked", contentlet.isLocked());

                if(contentlet.isHTMLPage()) {
                    HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
                    callbackData.put("htmlPageReferer", page.getURI() + "?" + WebKeys.HTMLPAGE_LANGUAGE + "=" + page.getLanguageId() + "&host_id=" + page.getHost());
                    boolean contentLocked = false;
                    boolean iCanLock = false;

                    try{
                    	contentLocked = page.isLocked();
                        iCanLock = APILocator.getContentletAPI().canLock(contentlet, user);
                     }catch(DotLockException e){
                        iCanLock=false;
                        contentLocked = false;
                     }
                    PageMode.setPageMode(req, contentLocked, iCanLock);

					final String previousTemplateId = (String) contentletFormData.get("currentTemplateId");

					if (UtilMethods.isSet(previousTemplateId) && !previousTemplateId.equals(page.getTemplateId())) {
						final User systemUser = APILocator.systemUser();
						final Template previousTemplate = APILocator.getTemplateAPI().findWorkingTemplate(previousTemplateId,
								systemUser, false);

						if (UtilMethods.isSet(previousTemplate) && previousTemplate.isAnonymous()) {
							APILocator.getTemplateAPI().delete(previousTemplate, systemUser, false);
						}

						CacheLocator.getMultiTreeCache().removePageMultiTrees(contentlet.getIdentifier(), contentlet.getVariantId());
					}
                }

			}

			if(publish && contentlet!=null){
				ContentletAPI capi = APILocator.getContentletAPI();
				capi.publish(contentlet, user, true);
				capi.unlock(contentlet, user, true);
				callbackData.put("contentletLocked", contentlet.isLocked());
			}

			if (contentlet!=null && contentlet.getStructure().getVelocityVarName().equalsIgnoreCase("host")) {
				String copyOptionsStr = (String)contentletFormData.get("copyOptions");
				CopyHostContentUtil copyHostContentUtil = new CopyHostContentUtil();
				if (UtilMethods.isSet(copyOptionsStr)) {
					final User finalUser = user;
					final Contentlet finalContentlet = contentlet;
					HibernateUtil.addCommitListener(()->{
					                  copyHostContentUtil.checkHostCopy(finalContentlet, finalUser, copyOptionsStr);
					       });
				}

			}

			String urlMap = null;
			if(contentlet!=null)
			    urlMap=contentlet.getStructure().getUrlMapPattern();
			if(UtilMethods.isSet(urlMap) && !urlMap.equals("/")){
				if(UtilMethods.isSet(referer)){
					List<RegExMatch> matches = RegEX.find(referer, StructureUtil.generateRegExForURLMap(urlMap));
					if (matches != null && matches.size() > 0) {
						String[] urlFrags = urlMap.split("/");
						Map<String,Integer> vars = new HashMap<>();
						int index = 0;
						for (String frag : urlFrags) {
							if(UtilMethods.isSet(frag)){
								if(frag.startsWith("{")){
									vars.put(frag.substring(frag.indexOf("{")+1, frag.indexOf("}")), index);
								}
							}
							index++;
						}
						if(!vars.isEmpty()){
							String[] refererVars =referer.split("/");
							for(String var: vars.keySet()){
								String contVar = contentlet.get(var)!=null?contentlet.get(var).toString():"";
								String refererVar = refererVars[vars.get(var)];
								if(UtilMethods.isSet(contVar) && !contVar.equals(refererVar)){
									refererVars[vars.get(var)] = contVar;
								}
							}
							if(refererVars.length>0){
								StringBuilder refererPattern = new StringBuilder();
								for(String ref: refererVars){
									if(UtilMethods.isSet(ref)){
										refererPattern.append("/");
										refererPattern.append(ref);
									}
								}
								if(UtilMethods.isSet(refererPattern.toString())){
									refererPattern.append("/");
									referer = refererPattern.toString();
								}

							}
						}
					}
				}
			}


		  if(UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getIdentifier())){
		    callbackData.put("allLangContentlets",
					findAllLangContentlets(contentlet.getIdentifier())
		    );
		  }

			// everything Ok? then commit
			HibernateUtil.closeAndCommitTransaction();

			// clean up tmp_binary
			// https://github.com/dotCMS/dotCMS/issues/2921
			if(contentlet!=null) {
			    for(Field ff : FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode())) {
			        if(ff.getFieldType().equals(FieldType.BINARY.toString())) {
			            File tmp=new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
			                    +File.separator+user.getUserId()+File.separator+ff.getFieldContentlet());
			            FileUtil.deltree(tmp);
			        }
			    }
			}
	  }
	  catch(DotLockException dse){
		  String errorString = LanguageUtil.get(user,"message.content.locked");
		  saveContentErrors.add(errorString);
		  clearBinary = false;
	  }
	  catch(DotSecurityException dse){
		  String errorString = LanguageUtil.get(user,"message.insufficient.permissions.to.save") + ". " + dse.getMessage();
		  saveContentErrors.add(errorString);
		  clearBinary = false;
	  }
	  catch ( PublishStateException pe ) {
		  String errorString = LanguageUtil.get( user, pe.getMessage() );
		  saveContentErrors.add( errorString );
		  clearBinary = false;
	  }
	  catch ( DotLanguageException e ) {
		  saveContentErrors.add( e.getMessage() );
		  callbackData.put( "saveContentErrors", saveContentErrors );
		  callbackData.put( "referer", referer );
		  clearBinary = false;
		  return callbackData;
	  }
	  catch(final Exception e){

		  final
		  Optional<Throwable> optionalThrowable = ExceptionUtil.get(e, DotContentletValidationException.class);

		  if (optionalThrowable.isPresent()) {
			  final DotContentletValidationException ve = (DotContentletValidationException) optionalThrowable.get();
			  clearBinary = handleValidationException(user, ve, saveContentErrors);
		  } else {
			  final Throwable rootCause = getRootCause(e);
			  if (rootCause instanceof DotContentletValidationException) {
				  final DotContentletValidationException ve = DotContentletValidationException.class
						  .cast(rootCause);
				  clearBinary = handleValidationException(user, ve, saveContentErrors);
			  } else {
				  Logger.debug(this, e.getMessage(), e);
				  Logger.error(this, e.getMessage());
				  saveContentErrors.add(e.getMessage());
				  callbackData.put("saveContentErrors", saveContentErrors);
				  callbackData.put("referer", referer);
				  clearBinary = false;
				  return callbackData;
			  }
		  }
		}
		finally{

		    if(saveContentErrors.size()>0) {
                try {
                    HibernateUtil.rollbackTransaction();

                    Contentlet contentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
                    if(contentlet!=null) {
                        callbackData.remove("contentletIdentifier");
                        callbackData.remove("contentletInode");
                        callbackData.remove("contentletLocked");
                        newInode=null;
                    }
                } catch (DotHibernateException e) {
                    Logger.warn(this, e.getMessage(),e);
                }
            }

		    if(!isAutoSave
					&&(saveContentErrors != null
							&& saveContentErrors.size() > 0)){
				callbackData.put("saveContentErrors", saveContentErrors);
				SessionMessages.clear(req.getSession());

			}
		    if(clearBinary){//if an error occur with any other field (was unique, required, length, pattern or bad type) when saving the contentlet, do not clear the binary field
			// If an error occurred, manually delete all other uploaded binary
		    // files since they were not included in the Hibernate transaction
			try {
				HttpSession ses = req.getSession();
				List<String> tempBinaryImageInodes = (List<String>) ses
						.getAttribute(Contentlet.TEMP_BINARY_IMAGE_INODES_LIST);
				if (UtilMethods.isSet(tempBinaryImageInodes)
						&& tempBinaryImageInodes.size() > 0) {
					for (String inode : tempBinaryImageInodes) {
						Contentlet contentlet = conAPI.find(inode, APILocator
								.getUserAPI().getSystemUser(), false);
						if (contentlet != null) {
							conAPI.archive(contentlet, user, false);
							conAPI.delete(contentlet, APILocator.getUserAPI()
									.getSystemUser(), false, true);
						}
					}
					tempBinaryImageInodes.clear();
				}
			} catch (DotContentletStateException e1) {
				Logger.warn(this, "Could not delete temporary image inode", e1);
			} catch (DotDataException e1) {
				Logger.warn(this, "Could not delete temporary image inode", e1);
			} catch (DotSecurityException e1) {
				Logger.warn(this, "Could not delete temporary image inode", e1);
			}
		    }
		}

		if(!isAutoSave
				&&(saveContentErrors == null
						|| saveContentErrors.size() == 0)){

			if(referer.contains("referer")){
				String ref = "referer=";
                String sourceReferer = referer.substring(referer.indexOf(ref)+ref.length(),referer.length());
                referer = referer.substring(0,referer.indexOf(ref));
                callbackData.put("sourceReferer", sourceReferer);
			 }

			Logger.debug(this, "AFTER PUBLISH LANGUAGE=" + language);

			if (UtilMethods.isSet(language) && referer.indexOf("language") > -1) {
				Logger.debug(this, "Replacing referer language=" + referer);
				referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
				Logger.debug(this, "Referer after being replaced=" + referer);
			}else{
		          	referer = referer+"&language="+language;
		    }

		}

		callbackData.put("referer", referer);

		return callbackData;
	}

	/**
	 * This code has been extracted into a separate method so we can use it when original exception has been wrapped within a Runtime Exception
	 * @param user
	 * @param ve
	 * @param saveContentErrors
	 * @return
	 * @throws LanguageException
	 */
	private boolean handleValidationException(final User user,
			final DotContentletValidationException ve, final List<String> saveContentErrors)
			throws LanguageException {
		boolean clearBinary = true;
		if (ve instanceof FileAssetValidationException) {
			final List<Field> reqs = ve.getNotValidFields()
					.get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE);
			for (final Field field : reqs) {
				String errorString = LanguageUtil.get(user, ve.getMessage());
				errorString = errorString.replace("{0}", field.getFieldName());
				saveContentErrors.add(errorString);
			}

		} else {

			if (ve.hasRequiredErrors()) {
				final List<Field> reqs = ve.getNotValidFields()
						.get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
				for (final Field field : reqs) {
					String errorString = LanguageUtil.get(user, "message.contentlet.required");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
				clearBinary = false;
			}

			if (ve.hasPatternErrors()) {
				List<Field> reqs = ve.getNotValidFields()
						.get(DotContentletValidationException.VALIDATION_FAILED_PATTERN);
				for (final Field field : reqs) {
					String errorString = LanguageUtil.get(user, "message.contentlet.format");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
				clearBinary = false;
			}

			if (ve.hasBadTypeErrors()) {
				final List<Field> reqs = ve.getNotValidFields()
						.get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE);
				for (final Field field : reqs) {
					String errorString = LanguageUtil.get(user, "message.contentlet.type");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
				clearBinary = false;
			}

			if (ve.hasCharLimitErrors()) {
				final List<Field> reqs = ve.getNotValidFields()
						.get(DotContentletValidationException.VALIDATION_FAILED_CHAR_LIMIT);
				final Map<String, Integer> charLimitMaxByFieldVar = ve.getCharLimitMaxByFieldVar();
				for (final Field field : reqs) {
					final Integer maxLimit = charLimitMaxByFieldVar.get(field.getVelocityVarName());
					String errorString = LanguageUtil.get(user, "dot.edit.content.form.field.charLimitExceeded",
							maxLimit != null ? maxLimit : 0);
					saveContentErrors.add(errorString);
				}
				clearBinary = false;
			}

			if (ve.hasRelationshipErrors()) {
				StringBuilder sb = new StringBuilder("<br>");
				final Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships = ve
						.getNotValidRelationship();
				final Set<String> auxKeys = notValidRelationships.keySet();
				for (final String key : auxKeys) {
					StringBuilder errorMessage = new StringBuilder();
					if (key.equals(
							DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL)) {
						errorMessage.append("<b>").append(LanguageUtil
								.get(user, "message.contentlet.relationship.required"))
								.append("</b>");
					} else if (key
							.equals(DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT)) {
						errorMessage.append("<b>").append(LanguageUtil
								.get(user, "message.contentlet.relationship.invalid"))
								.append("</b>");
					} else if (key
							.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_REL)) {
						errorMessage.append("<b>").append(LanguageUtil
								.get(user, "message.contentlet.relationship.bad"))
								.append("</b>");
					} else if (key
							.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_CARDINALITY)) {
						errorMessage.append("<b>").append(LanguageUtil
								.get(user, "message.contentlet.relationship.cardinality.bad"))
								.append("</b>");
					}

					sb.append(errorMessage).append(":<br>");
					final Map<Relationship, List<Contentlet>> relationshipContentlets = notValidRelationships
							.get(key);

					for (final Entry<Relationship, List<Contentlet>> relationship : relationshipContentlets
							.entrySet()) {
						sb.append(relationship.getKey().getRelationTypeValue()).append(", ");
					}
					sb.append("<br>");
				}
				sb.append("<br>");

				//need to update message to support multiple relationship validation errors
				String errorString = LanguageUtil.get(user, "message.relationship.required_ext");
				errorString = errorString.replace("{0}", sb.toString());
				saveContentErrors.add(errorString);
				clearBinary = false;
			}

			if (ve.hasUniqueErrors()) {
				final List<Field> reqs = ve.getNotValidFields()
						.get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE);
				for (final Field field : reqs) {
					String errorString = LanguageUtil.get(user, "message.contentlet.unique");
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}
				clearBinary = false;

                try {
                    HibernateUtil.rollbackTransaction();
                } catch (DotHibernateException e) {
                    Logger.error(ContentletAjax.class, e.getMessage());
                }
            }

			if (ve.getMessage().contains(
					"The content form submission data id different from the content which is trying to be edited")) {
				String errorString = LanguageUtil.get(user, "message.contentlet.invalid.form");
				saveContentErrors.add(errorString);
			}

			if (ve.getMessage().contains("message.contentlet.expired")) {
				String errorString = LanguageUtil.get(user, "message.contentlet.expired");
				saveContentErrors.add(errorString);
			}
		}
		if (saveContentErrors.size() == 0) {
			saveContentErrors.add(ve.getMessage());
		}
		return clearBinary;
	}


	@CloseDB
	//http://jira.dotmarketing.net/browse/DOTCMS-2273
	public String cancelContentEdit(String workingContentletInode,String currentContentletInode,String referer,String language){

		try{
			HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();
			User user = com.liferay.portal.util.PortalUtil.getUser(req);
			//contentletWebAPI.cancelContentEdit(workingContentletInode,currentContentletInode,user);
			HttpSession ses = req.getSession();
			List<String> tempBinaryImageInodes = (List<String>) ses.getAttribute(Contentlet.TEMP_BINARY_IMAGE_INODES_LIST);

			if(UtilMethods.isSet(tempBinaryImageInodes) && tempBinaryImageInodes.size() > 0){
				for(String inode : tempBinaryImageInodes){
					conAPI.delete(conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false, true);
				}
				tempBinaryImageInodes.clear();
			}

			// if we are canceling the edition of a host, let's restore the default one for the site browser

			Contentlet content = conAPI.find(workingContentletInode, user, false);
			Structure structure = content.getStructure();

			if(structure!= null && UtilMethods.isSet(structure.getInode()) && structure.getVelocityVarName().equals("Host")) {
				Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, false);
				ses.setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,defaultHost.getIdentifier() );
			}

		}
		catch(Exception ae){
			Logger.debug(this, "Error trying to cancelContentEdit");
		}

		if(referer.contains("language")){
			referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
		}else{
             referer = referer+"&language="+language;
		}
		return referer;
	}

	@CloseDB
	public Map<String,Object> saveContentProperties(String inode, List<String> formData, boolean isAutoSave,boolean isCheckin,boolean isPublish) throws PortalException, SystemException, DotDataException, DotSecurityException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest)req);
		Contentlet cont  = conAPI.find(inode, user, false);
		Map<String,Object> contentletFormData = new HashMap<>();
		Map<String,Object> callbackData = new HashMap<>();
		List<String> saveContentErrors = new ArrayList<>();
		callbackData.put("contentletInode",inode);
		// Storing form data into map.
		for (Iterator iterator = formData.iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();

			if (!com.dotmarketing.util.UtilMethods.isSet(element))
				continue;

			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));
			Object elementValue = element.substring(element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR) + WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR.length());

			if(!UtilMethods.isSet(elementName))
				continue;

			if(!UtilMethods.isSet(elementValue))
				elementValue="";

			if(elementValue.toString().trim().equals("<p><br></p>"))
				elementValue="";

			contentletFormData.put(elementName, elementValue);
		}

		Structure structure = null;
		if(!contentletFormData.isEmpty()){
			structure = cont.getStructure();
			for(Field f: FieldsCache.getFieldsByStructureInode(structure.getInode())){
				for(String key : contentletFormData.keySet()){
					if(f.getVelocityVarName().toString().equals(key)){
						conAPI.setContentletProperty(cont, f, contentletFormData.get(key));
					}
				}
			}
		}
		try{
			HibernateUtil.startTransaction();
			Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<>();
			List<Relationship> rels =  APILocator.getRelationshipAPI().byContentType(structure);
			for (Relationship r : rels) {
				if (!contentRelationships.containsKey(r)) {
					contentRelationships
					.put(r, new ArrayList<>());
				}
				List<Contentlet> cons = conAPI.getRelatedContent(
						cont, r, user, true);
				for (Contentlet co : cons) {
					List<Contentlet> l2 = contentRelationships.get(r);
					l2.add(co);
				}
			}

			conAPI.validateContentlet(cont, contentRelationships, APILocator.getCategoryAPI().getParents(cont, user, false));

			cont.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

			if(isPublish){//DOTCMS-5514
				conAPI.checkin(cont, contentRelationships,
						APILocator.getCategoryAPI().getParents(cont, user, false),
						APILocator.getPermissionAPI().getPermissions(cont, false, true), user, false);
				APILocator.getVersionableAPI().setLive(cont);
			}else{
				//cont.setLive(false);
				Contentlet draftContentlet = conAPI.saveDraft(cont, contentRelationships,
					APILocator.getCategoryAPI().getParents(cont, user, false),
					APILocator.getPermissionAPI().getPermissions(cont, false, true), user, false);

                callbackData.put("isNewContentletInodeHtmlPage", draftContentlet.isHTMLPage());
				callbackData.put("newContentletInode", draftContentlet.getInode());
			}
		}catch (DotContentletValidationException ve) {

				if(ve.hasRequiredErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.required");
						errorString = errorString.replace("{0}", field.getFieldName());
						saveContentErrors.add(errorString);
					}
				}

				if(ve.hasPatternErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_PATTERN);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.format");
						errorString = errorString.replace("{0}", field.getFieldName());
						saveContentErrors.add(errorString);
					}
				}

				if(ve.hasRelationshipErrors()){
					StringBuffer sb = new StringBuffer("<br>");
					Map<String,Map<Relationship,List<Contentlet>>> notValidRelationships = ve.getNotValidRelationship();
					Set<String> auxKeys = notValidRelationships.keySet();
					for(String key : auxKeys)
					{
						String errorMessage = "";
						if(key.equals(DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL))
						{
							errorMessage = "<b>Required Relationship</b>";
						}
						else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT))
						{
							errorMessage = "<b>Invalid Relationship-Contentlet</b>";
						}
						else if(key.equals(DotContentletValidationException.VALIDATION_FAILED_BAD_REL))
						{
							errorMessage = "<b>Bad Relationship</b>";
						}

						sb.append(errorMessage + ":<br>");
						Map<Relationship,List<Contentlet>> relationshipContentlets = notValidRelationships.get(key);

						for(Entry<Relationship,List<Contentlet>> relationship : relationshipContentlets.entrySet())
						{
							sb.append(relationship.getKey().getRelationTypeValue() + ", ");
						}
						sb.append("<br>");
					}
					sb.append("<br>");

					//need to update message to support multiple relationship validation errors
					String errorString = LanguageUtil.get(user,"message.relationship.required_ext");
					errorString = errorString.replace("{0}", sb.toString());
					saveContentErrors.add(errorString);
				}

				if(ve.hasUniqueErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.unique");
						errorString = errorString.replace("{0}", field.getFieldName());
						saveContentErrors.add(errorString);
					}
				}

				if(ve.getMessage().contains("The content form submission data id different from the content which is trying to be edited")){
					String errorString = LanguageUtil.get(user,"message.contentlet.invalid.form");
					saveContentErrors.add(errorString);
				}

			}

			catch(DotSecurityException dse){
				String errorString = LanguageUtil.get(user,"message.insufficient.permissions.to.save");
				saveContentErrors.add(errorString);

			}

			catch (Exception e) {
				String errorString = LanguageUtil.get(user,"message.contentlet.save.error");




				saveContentErrors.add(errorString + "<div style='color:silver;width:300px'>" + e.getMessage() + "</div>");
				SessionMessages.clear(req.getSession());

			}

			finally{
				if(!isAutoSave
						&&(saveContentErrors != null
								&& saveContentErrors.size() > 0)){
					callbackData.put("saveContentErrors", saveContentErrors);
					SessionMessages.clear(req.getSession());
					HibernateUtil.rollbackTransaction();
				}else{
					HibernateUtil.closeAndCommitTransaction();
					callbackData.put("saveContentSuccess",LanguageUtil.get(user,"message.contentlet.save"));
				}
			}



		return callbackData;

	}
	@CloseDB
	public void removeSiblingBinaryFromSession(String fieldContentlet){
		//http://jira.dotmarketing.net/browse/DOTCMS-5802
		if(UtilMethods.isSet(fieldContentlet)){
			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
			req.getSession().removeAttribute(fieldContentlet+"-sibling");
		}
	}
	@CloseDB
	public String unrelateContent(String contentletIdentifier,  String identifierToUnrelate, String relationshipInode){

		// User info
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		Contentlet currentContentlet;
		Contentlet contentletToUnrelate;
		Relationship relationship;
		List<Contentlet> conList = new ArrayList<>();
		String resultStr = "Content Unrelated";
		try {
			currentContentlet = conAPI.find(contentletIdentifier, currentUser, false);
			contentletToUnrelate = conAPI.find(identifierToUnrelate, currentUser, false);

			relationship =  APILocator.getRelationshipAPI().byInode(relationshipInode);

			conList.add(contentletToUnrelate);
			APILocator.getRelationshipAPI().deleteByContent(currentContentlet, relationship, conList);

			//if contentletToUnrelate is related as new content, there exists the below relation which also needs to be deleted.
			conList.clear();
			conList.add(currentContentlet);
			APILocator.getRelationshipAPI().deleteByContent(contentletToUnrelate, relationship, conList);

			conAPI.refresh(currentContentlet);
			conAPI.refresh(contentletToUnrelate);

			resultStr = LanguageUtil.get(currentUser,"Content-Unrelated");
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage());
		} catch (LanguageException e) {
			Logger.error(this, e.getMessage());
		}
		return resultStr;
	}


	public Map<String, String> lockContent(String contentletInode) throws DotContentletStateException, DotDataException, DotSecurityException, LanguageException{
		// User info
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}
		Contentlet c = conAPI.find(contentletInode, currentUser, false);


		Map<String, String> ret = new HashMap<>();
		ret.put("lockedIdent", contentletInode );
		try{
			conAPI.lock(c, currentUser, false);
			Optional<Date> lockedOn = APILocator.getVersionableAPI().getLockedOn(c);

			if(lockedOn.isPresent()) {
				ret.put("lockedOn", UtilMethods
						.capitalize(DateUtil.prettyDateSince(lockedOn.get(), currentUser.getLocale())));
			}
			ret.put("lockedBy", currentUser.getFullName() );

		}
		catch(Exception ex){
			ret.put("Error", LanguageUtil.get(currentUser, "message.cannot.lock.content") );

		}

		return ret;
	}


	@CloseDB
	public Map<String, String> unlockContent(String contentletInode) throws DotContentletStateException, DotDataException, DotSecurityException, LanguageException{
		// User info
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User currentUser = null;
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}
		Contentlet c = conAPI.find(contentletInode, currentUser, false);
		conAPI.unlock(c, currentUser, false);
		Map<String, String> ret = new HashMap<>();
		ret.put("lockedIdent", contentletInode );
		return ret;
	}



	private List<Map<String, String>> findAllLangContentlets(final String contentletIdentifier) {

	    final Identifier identifier = new Identifier(contentletIdentifier);

		final ImmutableList.Builder<Map<String, String>> builder = new ImmutableList.Builder<>();

		final List<Language> allLanguages = langAPI.getLanguages();
		allLanguages.forEach(language -> {

			try {
				final Contentlet contentlet = conAPI.findContentletForLanguage(language.getId(), identifier);
				if (null != contentlet) {
					builder.add(
							new HashMap<>(Map.of("inode", contentlet.getInode(),
									"identifier", contentletIdentifier,
									"languageId", language.getId() + ""))
					);
				} else {
					builder.add(
							new HashMap<>(Map.of("inode", "",
									"identifier", contentletIdentifier,
									"languageId", language.getId() + ""))
					);
				}
			} catch (DotDataException | DotSecurityException e) {
			    Logger.error(ContentletAjax.class, String.format("Unable to get contentlet for identifier %s and language %s", contentletIdentifier, language), e);
			}

		});

		return builder.build();
	}

	private class RelationshipFieldData {
		String fieldName;
		String fieldValue;

		public RelationshipFieldData(String fieldName, String fieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
		}
	}
}

