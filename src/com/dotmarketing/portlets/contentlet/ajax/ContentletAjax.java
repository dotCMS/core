package com.dotmarketing.portlets.contentlet.ajax;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.queryParser.ParseException;
import org.directwebremoting.WebContextFactory;

import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.enterprise.FormAJAXProxy;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.portlets.hostadmin.business.CopyHostContentUtil;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author David
 */
public class ContentletAjax {

	private java.text.DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
			java.text.DateFormat.SHORT);

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private ContentletWebAPI contentletWebAPI = WebAPILocator.getContentletWebAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private FormAPI formAPI = APILocator.getFormAPI();

	public List<Map<String, String>> getContentletsData(String inodesStr) {
		List<Map<String,String>> rows = new ArrayList<Map<String, String>>();

		if(inodesStr == null || !UtilMethods.isSet(inodesStr)) {
			return rows;
		}

		String[] inodes =  inodesStr.split(",");
		for (String inode : inodes) {
			rows.addAll(getContentletData(inode));
		}

		return rows;
	}

	public List<Map<String, String>> getContentletData(String inode) {

		List<Map<String,String>> rows = new ArrayList<Map<String, String>>();

		try {

			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
			User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			Contentlet firstContentlet = conAPI.find(inode, currentUser, true);
			List<Contentlet> contentletList = conAPI.getAllLanguages(firstContentlet, firstContentlet.isLive(), currentUser, true);
			Structure targetStructure = firstContentlet.getStructure();
			List<Field> targetFields = FieldsCache.getFieldsByStructureInode(targetStructure.getInode());

			String identifier = String.valueOf(firstContentlet.getIdentifier());

			for( Contentlet cont : contentletList ) {
				Map<String, String> map = new HashMap<String, String>();

				boolean hasListedFields = false;

				for (Field f : targetFields) {
					if (f.isIndexed() || f.isListed()) {
						hasListedFields = true;
						String fieldName = f.getFieldName();
						Object fieldValueObj = "";
						try{
							fieldValueObj = conAPI.getFieldValue(cont, f);
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
						map.put(fieldName, fieldValue);
					}
				}
				if( !hasListedFields ) {
					map.put("identifier", identifier);
				}

				map.put("inode", String.valueOf(cont.getInode()));
				map.put("working", String.valueOf(cont.isWorking()));
				map.put("live", String.valueOf(cont.isLive()));
				map.put("deleted", String.valueOf(cont.isArchived()));
				map.put("locked", String.valueOf(cont.isLocked()));
				map.put("id", identifier); // Duplicates value for identifier key in map so that UI does not get broken
				Language language = langAPI.getLanguage(cont.getLanguageId());
				String languageCode = langAPI.getLanguageCodeAndCountry(cont.getLanguageId(),null);
				String languageName =  language.getLanguage();
				map.put("langCode", languageCode);
				map.put("langName", languageName);
				map.put("hasListedFields", Boolean.toString(hasListedFields) );

				rows.add(map);
			}


		} catch (DotDataException e) {
			Logger.error(this, "Error trying to obtain the contentlets from the relationship.", e);
		} catch (PortalException e) {
			Logger.error(this, "Portal exception.", e);
		} catch (SystemException e) {
			Logger.error(this, "System exception.", e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Security exception.", e);
		}

		return rows;
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
	public List searchContentlet(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, int perPage, String orderBy) throws DotStateException, DotDataException, DotSecurityException {

		PermissionAPI perAPI = APILocator.getPermissionAPI();
		HttpSession sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		String userId = "";
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			userId = currentUser.getUserId();
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(structureInode, fields, categories, showDeleted, filterSystemHost, false, false, page, orderBy, perPage, currentUser, sess, null, null);
	}

	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, String orderBy, String modDateFrom, String modDateTo) throws DotStateException, DotDataException, DotSecurityException {
	    return searchContentlets(structureInode, fields, categories, showDeleted, filterSystemHost, page, orderBy, modDateFrom, modDateTo, true);
	}

	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, int page, String orderBy, String modDateFrom, String modDateTo, boolean saveLastSearch) throws DotStateException, DotDataException, DotSecurityException {
	    HttpSession sess = null;
        if(saveLastSearch)
            sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		String userId = "";
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			userId = currentUser.getUserId();
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(structureInode, fields, categories, showDeleted, filterSystemHost, false, false, page, orderBy, 0,currentUser, sess, modDateFrom, modDateTo);
	}

	public List searchContentlets(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost,  boolean filterUnpublish, boolean filterLocked, int page, String orderBy, String modDateFrom, String modDateTo) throws DotStateException, DotDataException, DotSecurityException {

		PermissionAPI perAPI = APILocator.getPermissionAPI();
		HttpSession sess = WebContextFactory.get().getSession();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		// User info
		User currentUser = null;
		String userId = "";
		try {
			currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
			userId = currentUser.getUserId();
		} catch (Exception e) {
			Logger.error(this, "Error trying to obtain the current liferay user from the request.", e);
		}

		return searchContentletsByUser(structureInode, fields, categories, showDeleted, filterSystemHost, filterUnpublish, filterLocked, page, orderBy, 0,currentUser, sess, modDateFrom, modDateTo);
	}

	/**
	 * This method is used by the backend to pull from lucene index the form widgets
	 * if the widget doesn't exist then is created and also checks the user
	 * permissions to see the content
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
	 * @param page
	 *            The page number to show (starting with 1)
	 *            If page is 0, this will return all possible contentlets
	 * @param perPage
	 * @param orderBy
	 *            The field name to be used to sort the content
	 * @return The list of contents that match the parameters at the position 0
	 *         the result included a hashmap with some useful information like
	 *         the total number of results, ...
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws ParseException
	 * @throws DotSecurityException
	 * @throws IllegalArgumentException
	 * @throws DotContentletStateException
	 * @throws DotContentletValidationException
	 * @throws ParseException
	 */
	public Map<String, Object> searchFormWidget(String formStructureInode) throws DotDataException, DotSecurityException, ParseException {
		FormAJAXProxy fp = new FormAJAXProxy();
		return fp.searchFormWidget(formStructureInode);
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
	 *            If page is 0, this will return all posible contentlets
	 * @param perPage
	 * 				Number of contents to display per page
	 * @param orderBy
	 *            The field name to be used to sort the content
	 * @param currentUser
	 *            The user needed to check the permissions
	 * @param sess
	 *            HttpSession to save some values if is set
	 * @return The list of contents that match the parameters at the position 0
	 *         the result included a hashmap with some useful information like
	 *         the total number of results, ...
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	@SuppressWarnings("unchecked")
	public List searchContentletsByUser(String structureInode, List<String> fields, List<String> categories, boolean showDeleted, boolean filterSystemHost, boolean filterUnpublish, boolean filterLocked, int page, String orderBy,int perPage, User currentUser, HttpSession sess,String  modDateFrom, String modDateTo) throws DotStateException, DotDataException, DotSecurityException {




		if(perPage < 1){
			perPage = Config.getIntProperty("PER_PAGE");
		}
		if(!InodeUtils.isSet(structureInode)) {
			Logger.error(this, "An invalid structure inode =  \"" + structureInode + "\" was passed");
			throw new DotRuntimeException("a valid structureInode need to be passed");
		}

		// Building search params and lucene query
		StringBuffer luceneQuery = new StringBuffer();

		Map<String, Object> lastSearchMap = new HashMap<String, Object>();

		if (UtilMethods.isSet(sess))
			sess.setAttribute(WebKeys.CONTENTLET_LAST_SEARCH, lastSearchMap);

		Structure st = StructureCache.getStructureByInode(structureInode);


		WorkflowScheme wfScheme = APILocator.getWorkflowAPI().findSchemeForStruct(st);



		lastSearchMap.put("structure", st);
		luceneQuery.append("+structureName:" + st.getVelocityVarName() + " ");
		Map<String, String> fieldsSearch = new HashMap<String, String>();

		// Stores (database name,type description) pairs to catch certain field types.
		List<Field> targetFields = FieldsCache.getFieldsByStructureInode(st.getInode());
		Map<String,String> fieldContentletNames = new HashMap<String,String>();
		Map<String,Field> decimalFields = new HashMap<String,Field>();//DOTCMS-5478

		boolean hasHostFolderField = false;

		for( Field f : targetFields ) {
			fieldContentletNames.put(f.getFieldContentlet(), f.getFieldType());
			if(f.getFieldType().toString().equals(Field.FieldType.HOST_OR_FOLDER.toString())){
				hasHostFolderField = true;
			}
			if(f.getFieldContentlet().startsWith("float")){
				decimalFields.put(st.getVelocityVarName()+"."+f.getVelocityVarName(), f);
			}
		}
		CategoryAPI catAPI = APILocator.getCategoryAPI();
		Category category=null;
		String categoriesvalues="";
		boolean first = true;
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
				if(fieldsSearch.containsKey(fieldName)){//DOTCMS-5987, To handle lastSearch for multi-select fields.
					fieldsSearch.put(fieldName, fieldsSearch.get(fieldName)+","+fieldValue);
				}else{
					fieldsSearch.put(fieldName, fieldValue);
				}

				if(fieldName.equalsIgnoreCase("conhost")){
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
				}else {
						List <Field> fieldsl = FieldsCache.getFieldsByStructureInode(st.getInode());
						String fieldbcontentname="";
						String fieldVelocityVarName = "";
						Boolean isStructField=false;
						String fieldVelName = "";
						if(fieldName.startsWith(st.getVelocityVarName() + ".")){
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

						for (Field fd : fieldsl) {
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



									metakey = VelocityUtil.convertToVelocityVariable(metakey);
									String metaVal = splitter[splitter.length-1];
									fieldValue = metakey + ":" + metaVal;
									luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName +  "." +  fieldValue.toString().replaceAll("\"", "\\\"") + " ");


								}
								catch(Exception e){

								}





							}else if( FieldFactory.isTagField(fieldbcontentname,st)== false){
//								String quotes = fieldValue.contains(" ") ? "\"" : "";
								fieldValue = fieldValue.trim();
								boolean hasQuotes = fieldValue != null && fieldValue.length() > 1 && fieldValue.endsWith("\"") && fieldValue.startsWith("\"");
								if(hasQuotes){
									fieldValue = fieldValue.replaceFirst("\"", "");
									fieldValue = fieldValue.substring(0, fieldValue.length()-1);
								}
								luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName + ":" + (hasQuotes ? "\"":wildCard) + ESUtils.escape(fieldValue.toString()) + (hasQuotes ? "\"":wildCard) + " ");
							}
							else{
								String[] splitValues = fieldValue.split(",");
								for(String splitValue : splitValues)
								{
									splitValue = splitValue.trim();
									String quotes = splitValue.contains(" ") ? "\"" : "";
									luceneQuery.append("+" + st.getVelocityVarName() + "." + fieldVelocityVarName+ ":" + quotes + ESUtils.escape(splitValue.toString()) + quotes + " ");
								}
							}
						}
						else if( fieldbcontentname.startsWith("date") ){
							luceneQuery.append("+" + st.getVelocityVarName() +"."+ fieldVelocityVarName + ":" + fieldValue + " ");
						} else {
							if(isStructField==false){
								luceneQuery.append("+" + fieldName + ":" + fieldValue.toString() + wildCard + " ");
							}
							else {
								luceneQuery.append("+" + st.getVelocityVarName() +"."+ fieldVelocityVarName + ":" + fieldValue.toString() + wildCard + " ");
							}
						}

				}
			}
		}

		if(UtilMethods.isSet(categoriesvalues)){
			luceneQuery.append("+(" + categoriesvalues + ") " );
		}

		lastSearchMap.put("fieldsSearch", fieldsSearch);

		//for (String cat : categories) {
		//	luceneQuery.append("+c" + cat + "c:on ");
		//}
		lastSearchMap.put("categories", categories);

		if (!UtilMethods.isSet(orderBy)){
			orderBy = "modDate desc";
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


		int offset = 0;
		if (page != 0)
			offset = perPage * (page - 1);

		lastSearchMap.put("orderBy", orderBy);

		luceneQuery.append(" +working:true");

		//Executing the query
		long before = System.currentTimeMillis();
		PaginatedArrayList <Contentlet> hits = new PaginatedArrayList <Contentlet>();
		long totalHits=0;
		try{
			hits = (PaginatedArrayList<Contentlet>) conAPI.search(luceneQuery.toString(), perPage + 1, offset, orderBy, currentUser, false);
			totalHits = hits.getTotalResults();
		}catch (Exception pe) {
			Logger.error(ContentletAjax.class, "Unable to execute Lucene Query", pe);
		}
		long after = System.currentTimeMillis();
		Logger.debug(ContentletAjax.class, "searchContentletsByUser: Time to search on lucene =" + (after - before) + " ms.");


		before = System.currentTimeMillis();

		//The reesults list returned to the page
		List<Object> results = new ArrayList<Object>();

		//Adding the result counters as the first row of the results
		Map<String, Object> counters = new HashMap<String, Object>();
		results.add(counters);

		//Adding the headers as the second row of the results
		List<Object> headers = new ArrayList<Object>();
		Map<String, Field> fieldsMapping = new HashMap<String, Field>();
		List<Field> stFields = FieldsCache.getFieldsByStructureInode(st.getInode());
		for (Field f : stFields) {
			if (f.isListed()) {
				fieldsMapping.put(f.getVelocityVarName(), f);
				headers.add(f.getMap());
			}
		}
		if (headers.size() == 0) {
			Map<String, String> fieldMap = new HashMap<String, String> ();
			fieldMap.put("fieldVelocityVarName", "identifier");
			fieldMap.put("fieldName", "identifier");
			headers.add(fieldMap);
		}
		results.add(headers);

		// we add the total hists for the query
		results.add(totalHits);

		//Adding the query results
		Contentlet con;
		for (int i = 0; ((i < perPage) && (i < hits.size())); ++i) {
			con = hits.get(i);

			Map<String, String> searchResult = new HashMap<String, String>();

			for (String fieldContentlet : fieldsMapping.keySet()) {
				String fieldValue = null;
				if(con.getMap() != null && con.getMap().get(fieldContentlet) !=null){
					fieldValue =(con.getMap().get(fieldContentlet)).toString();
				}
				Field field = (Field) fieldsMapping.get(fieldContentlet);
				if (UtilMethods.isSet(fieldValue) && field.getFieldType().equals(Field.FieldType.DATE.toString()) ||
						UtilMethods.isSet(fieldValue) && field.getFieldType().equals(Field.FieldType.TIME.toString()) ||
						UtilMethods.isSet(fieldValue) && field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
					try {
						Date date = DateUtil.convertDate(fieldValue, new String[] { "yyyy-MM-dd HH:mm:ss", "E MMM dd HH:mm:ss z yyyy" });
						if (field.getFieldType().equals(Field.FieldType.DATE.toString()))
							fieldValue = UtilMethods.dateToHTMLDate(date);
						if (field.getFieldType().equals(Field.FieldType.TIME.toString()))
							fieldValue = UtilMethods.dateToHTMLTime(date);
						if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()))
							fieldValue = UtilMethods.dateToHTMLDate(date) + " " + UtilMethods.dateToHTMLTime(date);
					} catch (java.text.ParseException e) {
						Logger.error(ContentletAjax.class, e.getMessage(), e);
						throw new DotRuntimeException(e.getMessage(), e);
					}
				}else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) || field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())) {
					if (UtilMethods.isSet(fieldValue))
						fieldValue = fieldValue.replaceAll("# #",",").replaceAll("#","");
				}
				searchResult.put(fieldContentlet, fieldValue);
			}
			searchResult.put("inode", con.getInode());
			searchResult.put("Identifier",con.getIdentifier());
			searchResult.put("identifier", con.getIdentifier());
			String fieldValue = UtilMethods.dateToHTMLDate(con.getModDate()) + " " + UtilMethods.dateToHTMLTime(con.getModDate());

			searchResult.put("modDate", fieldValue);
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
				user =con.getModUser();
			} else {
				user = contentEditor.getFullName();
			}
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
				String str = "P" + permission.getRoleId() + "." + permission.getPermission() + "P ";
				if (permissionsSt.toString().indexOf(str) < 0) {
					permissionsSt.append(str);
				}
				try {
					if(APILocator.getRoleAPI().loadCMSOwnerRole().getId().equals(String.valueOf(permission.getRoleId()))){
						if(permission.getPermission() == PERMISSION_READ){
							ownerCanRead = true;
						}else if(permission.getPermission() == PERMISSION_WRITE){
							ownerCanRead = true;
							ownerCanWrite = true;
						}else if(permission.getPermission() == PERMISSION_PUBLISH){
							ownerCanRead = true;
							ownerCanWrite = true;
							ownerCanPub = true;
						}
					}
				} catch (DotDataException e) {

				}
			}
			searchResult.put("modUser", user);
			searchResult.put("owner", con.getOwner());
			searchResult.put("ownerCanRead", ownerCanRead.toString());
			searchResult.put("ownerCanWrite", ownerCanWrite.toString());
			searchResult.put("ownerCanPublish", ownerCanPub.toString());
			Boolean working=con.isWorking();
			searchResult.put("working", working.toString());
			Boolean live=con.isLive();
			searchResult.put("statusIcons", UtilHTML.getStatusIcons(con));

			searchResult.put("live", live.toString());
			Boolean isdeleted=con.isArchived();
			searchResult.put("deleted", isdeleted.toString());
			Boolean locked=con.isLocked();
			searchResult.put("locked", locked.toString());
			searchResult.put("structureInode", con.getStructureInode());
			searchResult.put("workflowMandatory", String.valueOf(APILocator.getWorkflowAPI().findSchemeForStruct(con.getStructure()).isMandatory()));
			//searchResult.put("structureName", st.getVelocityVarName());
			Long LanguageId=con.getLanguageId();
			searchResult.put("languageId", LanguageId.toString());
			searchResult.put("permissions", permissionsSt.toString());

			results.add(searchResult);
		}

		long total = hits.getTotalResults();
		counters.put("total", total);

		if (page == 0)
			counters.put("hasPrevious", false);
		else
			counters.put("hasPrevious", page != 1);

		if (page == 0)
			counters.put("hasNext", false);
		else
			counters.put("hasNext", perPage < hits.size());

		// Data to show in the bottom content listing page
		String luceneQueryToShow2= luceneQuery.toString();
		luceneQueryToShow2=luceneQueryToShow2.replaceAll("\\+languageId:[0-9]*\\*?","").replaceAll("\\+deleted:[a-zA-Z]*","")
			.replaceAll("\\+working:[a-zA-Z]*","").trim();
		String luceneQueryToShow= luceneQuery.toString();
		counters.put("luceneQueryRaw", luceneQueryToShow);
		counters.put("luceneQueryFrontend", luceneQueryToShow2);
		counters.put("sortByUF", orderBy);

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

	public ArrayList<String[]> doSearchGlossaryTerm(String valueToComplete, String language) throws Exception {
		ArrayList<String[]> list = new ArrayList<String[]>(15);

		List<LanguageKey> props = retrieveProperties(Long.parseLong(language));

		String[] term;
		valueToComplete = valueToComplete.toLowerCase();

		for (LanguageKey prop : props) {
			if (prop.getKey().toLowerCase().startsWith(valueToComplete)) {
				term = new String[] { prop.getKey(),
						(70 < prop.getValue().length() ? prop.getValue().substring(0, 69) : prop.getValue())};
				list.add(term);
			}
		}

		return list;
	}

	private String getGlobalVariablesPath() {
		String globalVarsPath = Config.getStringProperty("GLOBAL_VARIABLES_PATH");
		if (!UtilMethods.isSet(globalVarsPath)) {
			globalVarsPath = Config.CONTEXT.getRealPath(File.separator + ".." + File.separator + "common"
					+ File.separator + "ext-ejb" + File.separator + "content" + File.separator);
		}
		if (!globalVarsPath.endsWith(File.separator))
			globalVarsPath = globalVarsPath + File.separator;
		return globalVarsPath;
	}

	private List<LanguageKey> retrieveProperties(long langId) throws Exception {
		Language lang = langAPI.getLanguage(langId);
		return langAPI.getLanguageKeys(lang);
	}

	/**
	 * Publishes or unpublishes contentlets from a given list of identifiers.  You can have to publish within
	 * a specific language or all languages.  Set the languageId = 0 for all languages.
	 * @param identifiersList
	 * @param isPublish whether it should publish or unpublish the contentlets
	 * @param languageId if set to 0 will publish for all languages
	 * @return
	 */
	public List<Map<String, Object>> publishContentlets(List<String> identifiersList, boolean isPublish, long languageId) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();

		for (int x = 0; x < identifiersList.size(); x++) {

			String id = identifiersList.get(x);

			try {
				User currentUser = com.liferay.portal.util.PortalUtil.getUser(req);
				Contentlet contentlet = new Contentlet();
				List<Contentlet> contentletList = new ArrayList<Contentlet>();
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
			} catch (PortalException e) {
				Logger.error(this, "Portal exception.", e);
			} catch (SystemException e) {
				Logger.error(this, "System exception.", e);
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
	public Map<String,Object> saveContent(List<String> formData, boolean isAutoSave,boolean isCheckin, boolean publish) throws LanguageException, PortalException, SystemException {

	    try {
            HibernateUtil.startTransaction();
        } catch (DotHibernateException e1) {
            Logger.warn(this, e1.getMessage(),e1);
        }
	    

		int tempCount = 0;// To store multiple values opposite to a name. Ex: selected permissions & categories
		String newInode = "";

		String referer = "";
		String language = "";
		String strutsAction = "";
		String recurrenceDaysOfWeek="";

		Map<String,Object> contentletFormData = new HashMap<String,Object>();
		Map<String,Object> callbackData = new HashMap<String,Object>();
		List<String> saveContentErrors = new ArrayList<String>();

		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		Config.CONTEXT.setAttribute("WEB_SERVER_HTTP_PORT", Integer.toString(req.getServerPort()));
		User user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest)req);

		// get the struts_action from the form data
		for (Iterator iterator = formData.iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();
			if(element!=null) {
    			String elementName = element.substring(0, element.indexOf(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR));

    			if (elementName.startsWith("_EXT") && elementName.endsWith("cmd")) {
    				strutsAction = elementName.substring(0, elementName.indexOf("cmd"));
    				break;
    			}
			}
		}

		// Storing form data into map.
		for (Iterator iterator = formData.iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();

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
			//http://jira.dotmarketing.net/browse/DOTCMS-3463
			if(elementName.startsWith("binary")){
				String binaryFileValue = (String) elementValue;
				File binaryFile = null;
				if(UtilMethods.isSet(binaryFileValue) && !binaryFileValue.equals("---removed---")){
					binaryFileValue = ContentletUtil.sanitizeFileName(binaryFileValue);
					binaryFile = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary()  
							+ File.separator + user.getUserId() + File.separator + elementName
							+ File.separator + binaryFileValue);
					if(binaryFile.exists()) {
    					try {
    					    // https://github.com/dotCMS/dotCMS/issues/35
    					    // making a copy just in case the transaction fails so
    					    // we can have the file for possible next attempts
                            File acopyFolder=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
                                    + File.separator + user.getUserId() + File.separator + elementName
                                    + File.separator + UUIDGenerator.generateUuid());
                            if(!acopyFolder.exists())
                                acopyFolder.mkdir();
                            File acopy=new File(acopyFolder, binaryFileValue);
                            FileUtils.copyFile(binaryFile, acopy);
                            elementValue = acopy;
                        } catch (IOException e) {
                            Logger.warn(this, "can't make a copy of the uploaded file");
                            throw new SystemException(e);
                        }
					}
				}else{
					elementValue = null;
				}
				//http://jira.dotmarketing.net/browse/DOTCMS-5802
				boolean populate = req.getSession().getAttribute("populateAccept")!=null?
						((Boolean)req.getSession().getAttribute("populateAccept")).booleanValue():false;
						if(populate && elementValue!=null){
							String siblingData = req.getSession().getAttribute(elementName+"-sibling")!=null?
									(String)req.getSession().getAttribute(elementName+"-sibling"):null;
									try{
										if(UtilMethods.isSet(siblingData)){
											String[] sessData = siblingData.split(",");
											if(sessData.length>0){
												File binFile = conAPI.getBinaryFile(sessData[0].trim(), sessData[1].trim(), user);
												if(binFile != null) {
													String fieldValue = binFile.getName();
													File destFile = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
															+ java.io.File.separator + user.getUserId()
															+ java.io.File.separator + fieldValue);

														if(!destFile.exists()){
															destFile.createNewFile();
														}
														FileUtils.copyFile(binFile, destFile);
														elementValue = destFile;

												}
											}
										}
									}catch(Exception e){
									}
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

		try {


			newInode = contentletWebAPI.saveContent(contentletFormData,isAutoSave,isCheckin,user);
			Contentlet contentlet = (Contentlet) contentletFormData.get(WebKeys.CONTENTLET_EDIT);
			if(contentlet != null){
				callbackData.put("contentletIdentifier", contentlet.getIdentifier());
				callbackData.put("contentletInode", contentlet.getInode());
				callbackData.put("contentletLocked", contentlet.isLocked());

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
					copyHostContentUtil.checkHostCopy(contentlet, user, copyOptionsStr);
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
						Map<String,Integer> vars = new HashMap<String, Integer>();
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
								String contVar = contentlet.get(var)!=null?(String)contentlet.get(var):"";
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

			// everything Ok? then commit
			HibernateUtil.commitTransaction();

		}
		catch (DotContentletValidationException ve) {

			if(ve instanceof FileAssetValidationException){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE);
				for (Field field : reqs) {
					String errorString = LanguageUtil.get(user, ve.getMessage());
					errorString = errorString.replace("{0}", field.getFieldName());
					saveContentErrors.add(errorString);
				}

			}else{

				if(ve.hasRequiredErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.required");
						errorString = errorString.replace("{0}", field.getFieldName());
						saveContentErrors.add(errorString);
					}
				}

				if(ve.hasLengthErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_MAXLENGTH);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.maxlength");
						errorString = errorString.replace("{0}", field.getFieldName());
						errorString = errorString.replace("{1}", "225");
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


				if(ve.hasBadTypeErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.type");
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

		}
		catch(DotLockException dse){
			String errorString = LanguageUtil.get(user,"message.content.locked");
			saveContentErrors.add(errorString);

		}
		catch(DotSecurityException dse){
			String errorString = LanguageUtil.get(user,"message.insufficient.permissions.to.save");
			saveContentErrors.add(errorString);
		}

		catch (Exception e) {
			saveContentErrors.add(e.toString());
			callbackData.put("saveContentErrors", saveContentErrors);
			callbackData.put("referer", referer);
			return callbackData;
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
		}

		if(!isAutoSave
				&&(saveContentErrors == null
						|| saveContentErrors.size() == 0)){

			Logger.debug(this, "AFTER PUBLISH LANGUAGE=" + language);

			if (UtilMethods.isSet(language) && referer.indexOf("language") > -1) {
				Logger.debug(this, "Replacing referer language=" + referer);
				referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
				Logger.debug(this, "Referer after being replaced=" + referer);
			}
		}
		if(!isAutoSave){
			if(InodeUtils.isSet(newInode) && !conAPI.isInodeIndexed(newInode)){
				Logger.error(this, "Timed Out waiting for index to return");
			}
		}
		callbackData.put("referer", referer);
		return callbackData;
	}

	//http://jira.dotmarketing.net/browse/DOTCMS-2273
	public String cancelContentEdit(String workingContentletInode,String currentContentletInode,String referer,String language){

		try{
			HttpServletRequest req =WebContextFactory.get().getHttpServletRequest();
			User user = com.liferay.portal.util.PortalUtil.getUser(req);
			//contentletWebAPI.cancelContentEdit(workingContentletInode,currentContentletInode,user);
		}
		catch(Exception ae){
			Logger.debug(this, "Error trying to cancelContentEdit");
		}

		referer = referer.replaceAll("language=([0-9])*", com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE+"=" + language);
		return referer;
	}


	public Map<String,Object> saveContentProperties(String inode, List<String> formData, boolean isAutoSave,boolean isCheckin,boolean isPublish) throws PortalException, SystemException, DotDataException, DotSecurityException{
		HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
		User user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest)req);
		Contentlet cont  = conAPI.find(inode, user, false);
		Map<String,Object> contentletFormData = new HashMap<String,Object>();
		Map<String,Object> callbackData = new HashMap<String,Object>();
		List<String> saveContentErrors = new ArrayList<String>();
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
			Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();
			List<Relationship> rels = RelationshipFactory
			.getAllRelationshipsByStructure(structure);
			for (Relationship r : rels) {
				if (!contentRelationships.containsKey(r)) {
					contentRelationships
					.put(r, new ArrayList<Contentlet>());
				}
				List<Contentlet> cons = conAPI.getRelatedContent(
						cont, r, user, true);
				for (Contentlet co : cons) {
					List<Contentlet> l2 = contentRelationships.get(r);
					l2.add(co);
				}
			}

			conAPI.validateContentlet(cont, contentRelationships, APILocator.getCategoryAPI().getParents(cont, user, false));
			if(isPublish){//DOTCMS-5514
				conAPI.checkin(cont, contentRelationships,
						APILocator.getCategoryAPI().getParents(cont, user, false),
						APILocator.getPermissionAPI().getPermissions(cont, false, true), user, false);
				APILocator.getVersionableAPI().setLive(cont);
			}else{
				//cont.setLive(false);
				conAPI.saveDraft(cont, contentRelationships,
					APILocator.getCategoryAPI().getParents(cont, user, false),
					APILocator.getPermissionAPI().getPermissions(cont, false, true), user, false);
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

				if(ve.hasLengthErrors()){
					List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_MAXLENGTH);
					for (Field field : reqs) {
						String errorString = LanguageUtil.get(user,"message.contentlet.maxlength");
						errorString = errorString.replace("{0}", field.getFieldName());
						errorString = errorString.replace("{1}", "225");
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
					HibernateUtil.commitTransaction();
					callbackData.put("saveContentSuccess",LanguageUtil.get(user,"message.contentlet.save"));
				}
			}



		return callbackData;

	}

	public void removeSiblingBinaryFromSession(String fieldContentlet){
		//http://jira.dotmarketing.net/browse/DOTCMS-5802
		if(UtilMethods.isSet(fieldContentlet)){
			HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
			req.getSession().removeAttribute(fieldContentlet+"-sibling");
		}
	}

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
		List<Contentlet> conList = new ArrayList<Contentlet>();
		String resultStr = "Content Unrelated";
		try {
			currentContentlet = conAPI.find(contentletIdentifier, currentUser, false);
			contentletToUnrelate = conAPI.find(identifierToUnrelate, currentUser, false);

			relationship = CacheLocator.getRelationshipCache().getRelationshipByInode(relationshipInode);
			if(relationship == null)
				relationship = RelationshipFactory.getRelationshipByInode(relationshipInode);

			conList.add(contentletToUnrelate);
			RelationshipFactory.deleteRelationships(currentContentlet, relationship, conList);

			//if contentletToUnrelate is related as new content, there exists the below relation which also needs to be deleted.
			conList.clear();
			conList.add(currentContentlet);
			RelationshipFactory.deleteRelationships(contentletToUnrelate, relationship, conList);

			conAPI.refresh(currentContentlet);
			conAPI.refresh(contentletToUnrelate);

			resultStr = LanguageUtil.get(currentUser,"Content-Unrelated");
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage());
		} catch (DotCacheException e) {
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


		Map<String, String> ret = new HashMap<String, String>();
		ret.put("lockedIdent", contentletInode );
		try{
			conAPI.lock(c, currentUser, false);

			ret.put("lockedOn", UtilMethods.capitalize(DateUtil.prettyDateSince(APILocator.getVersionableAPI().getLockedOn(c), currentUser.getLocale()) ));
			ret.put("lockedBy", currentUser.getFullName() );

		}
		catch(Exception ex){
			ret.put("Error", LanguageUtil.get(currentUser, "message.cannot.lock.content") );

		}





		return ret;
	}



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
		Map<String, String> ret = new HashMap<String, String>();
		ret.put("lockedIdent", contentletInode );
		return ret;
	}


}

