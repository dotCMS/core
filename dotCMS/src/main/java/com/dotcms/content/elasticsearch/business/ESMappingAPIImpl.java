package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.org.apache.commons.collections.CollectionUtils;
import com.dotcms.repackage.org.apache.commons.lang.time.FastDateFormat;

import com.dotcms.util.CollectionsUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.business.FieldAPI;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.KeyValueFieldUtil;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;
import com.dotmarketing.util.UtilMethods;


public class ESMappingAPIImpl implements ContentMappingAPI {

	private static final int UUID_LENGTH = 36;
	static ObjectMapper mapper = null;

	public ESMappingAPIImpl() {
		if (mapper == null) {
			synchronized (this.getClass().getName()) {
				if (mapper == null) {
					mapper = new ObjectMapper();
					ThreadSafeSimpleDateFormat df = new ThreadSafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					mapper.setDateFormat(df);
				}
			}
		}
	}

	
	
	/**
	 * This method takes a mapping string, a type and puts it as the mapping
	 * @param indexName
	 * @param type
	 * @param mapping
	 * @return
	 * @throws ElasticsearchException
	 * @throws IOException
	 */
	public  boolean putMapping(String indexName, String type, String mapping) throws ElasticsearchException, IOException{

		ListenableActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices().preparePutMapping().setIndices(indexName).setType(type).setSource(mapping).execute();
		return lis.actionGet().isAcknowledged();
	}

	/**
	 * This method takes a mapping string, a type and puts it as the mapping
	 * @param indexName
	 * @param type
	 * @param mapping
	 * @return
	 * @throws ElasticsearchException
	 * @throws IOException
	 */
	public  boolean putMapping(String indexName, String type, String mapping, String settings) throws ElasticsearchException, IOException{
		ListenableActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices().preparePutMapping().setIndices(indexName).setType(type).setSource(mapping).execute();
		return lis.actionGet().isAcknowledged();
	}

	public  boolean setSettings(String indexName,   String settings) throws ElasticsearchException, IOException{
		new ESClient().getClient().admin().indices().prepareUpdateSettings().setSettings(settings).setIndices( indexName).execute().actionGet();
		return true;
	}



	/**
	 * Gets the mapping params for an index and type
	 * @param index
	 * @param type
	 * @return
	 * @throws ElasticsearchException
	 * @throws IOException
	 */
	public  String getMapping(String index, String type) throws ElasticsearchException, IOException{

		return new ESClient().getClient().admin().cluster().state(new ClusterStateRequest())
				.actionGet().getState().metaData().indices()
				.get(index).mapping(type).source().string();

	}

	public  String getSettings(String index, String type) throws ElasticsearchException, IOException{

		return new ESClient().getClient().admin().cluster().state(new ClusterStateRequest())
				.actionGet().getState().metaData().indices()

				.get(index).settings().getAsMap().toString();

	}



	private Map<String, Object> getDefaultFieldMap() {

		Map<String, Object> fieldProps = new HashMap<String, Object>();
		fieldProps.put("store", "no");
		fieldProps.put("include_in_all", false);
		return fieldProps;

	}


	@SuppressWarnings("unchecked")
	public String toJson(Contentlet con) throws DotMappingException {

		try {
			Map<String,Object> m = toMap(con);
			return mapper.writeValueAsString(m);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotMappingException(e.getMessage(), e);
		}
	}

	/**
	 * This method is the same of the toJson except that it returns directly the mlowered map.
	 *
	 * It checks first if this contentlet is already into the temporarily memory otherwise it recreate.
	 *
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * Jun 7, 2013 - 3:47:26 PM
	 */
	public Map<String,Object> toMap(Contentlet con) throws DotMappingException {
		try {

			Map<String,String> contentletMap = new HashMap<String,String>();
			Map<String,Object> mlowered=new HashMap<String,Object>();
			loadCategories(con, contentletMap);
			loadFields(con, contentletMap);
			loadPermissions(con, contentletMap);
			loadRelationshipFields(con, contentletMap);

			Identifier ident = APILocator.getIdentifierAPI().find(con);
			ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), con.getLanguageId());
			Structure st=CacheLocator.getContentTypeCache().getStructureByInode(con.getStructureInode());

			Folder conFolder=APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false);

			contentletMap.put(ESMappingConstants.TITLE, con.getTitle());
			contentletMap.put(ESMappingConstants.STRUCTURE_NAME, st.getVelocityVarName()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.CONTENT_TYPE, st.getVelocityVarName());
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE, st.getStructureType() + ""); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.BASE_TYPE, st.getStructureType() + "");
			contentletMap.put(ESMappingConstants.TYPE, ESMappingConstants.CONTENT);
			contentletMap.put(ESMappingConstants.INODE, con.getInode());
			contentletMap.put(ESMappingConstants.MOD_DATE, datetimeFormat.format(con.getModDate()));
			contentletMap.put(ESMappingConstants.OWNER, con.getOwner()==null ? "0" : con.getOwner());
			contentletMap.put(ESMappingConstants.MOD_USER, con.getModUser());
			contentletMap.put(ESMappingConstants.LIVE, Boolean.toString(con.isLive()));
			contentletMap.put(ESMappingConstants.WORKING, Boolean.toString(con.isWorking()));
			contentletMap.put(ESMappingConstants.LOCKED, Boolean.toString(con.isLocked()));
			contentletMap.put(ESMappingConstants.DELETED, Boolean.toString(con.isArchived()));
			contentletMap.put(ESMappingConstants.LANGUAGE_ID, Long.toString(con.getLanguageId()));
			contentletMap.put(ESMappingConstants.IDENTIFIER, ident.getId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOST, ident.getHostId());
			contentletMap.put(ESMappingConstants.CONTENTLET_FOLER, conFolder!=null && InodeUtils.isSet(conFolder.getInode()) ? conFolder.getInode() : con.getFolder());
			contentletMap.put(ESMappingConstants.PARENT_PATH, ident.getParentPath());
			contentletMap.put(ESMappingConstants.PATH, ident.getPath());
			// makes shorties searchable regardless of length
			contentletMap.put(ESMappingConstants.SHORT_ID, ident.getId().replace("-", ""));
			contentletMap.put(ESMappingConstants.SHORT_INODE, con.getInode().replace("-", ""));
			try{
				WorkflowTask task = APILocator.getWorkflowAPI().findTaskByContentlet(con);
				if(task!=null && task.getId()!=null){
					contentletMap.put(ESMappingConstants.WORKFLOW_CREATED_BY, task.getCreatedBy());
					contentletMap.put(ESMappingConstants.WORKFLOW_ASSIGN, task.getAssignedTo());
					contentletMap.put(ESMappingConstants.WORKFLOW_STEP, task.getStatus());
					contentletMap.put(ESMappingConstants.WORKFLOW_MOD_DATE, datetimeFormat.format(task.getModDate()));
				}

			}
			catch(DotDataException e){
				Logger.error(this.getClass(), "unable to add workflow info to index:" + e, e);
			}



			if(UtilMethods.isSet(ident.getSysPublishDate()))
				contentletMap.put(ESMappingConstants.PUBLISH_DATE, datetimeFormat.format(ident.getSysPublishDate()));
			else
				contentletMap.put(ESMappingConstants.PUBLISH_DATE, datetimeFormat.format(cvi.getVersionTs()));

			if(UtilMethods.isSet(ident.getSysExpireDate()))
				contentletMap.put(ESMappingConstants.EXPIRE_DATE, datetimeFormat.format(ident.getSysExpireDate()));
			else
				contentletMap.put(ESMappingConstants.EXPIRE_DATE, "29990101000000");

			contentletMap.put(ESMappingConstants.VERSION_TS, datetimeFormat.format(cvi.getVersionTs()));

			String urlMap = null;
			try{
				urlMap = APILocator.getContentletAPI().getUrlMapForContentlet(con, APILocator.getUserAPI().getSystemUser(), true);
				if(urlMap != null){
					contentletMap.put(ESMappingConstants.URL_MAP,urlMap );
				}
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Cannot get URLMap for contentlet.id : " + ((ident != null) ? ident.getId() : con) + " , reason: "+e.getMessage());
				throw new DotRuntimeException(urlMap, e);
			}

			for(Entry<String,String> entry : contentletMap.entrySet()){
				final String lcasek=entry.getKey().toLowerCase();
				final String lcasev = UtilMethods.isSet(entry.getValue()) ? entry.getValue().toLowerCase() : null;
				mlowered.put(lcasek, lcasev);
				mlowered.put(lcasek + "_dotraw", lcasev);
			}

			if(con.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
				// see if we have content metadata
				File contentMeta=APILocator.getFileAssetAPI().getContentMetadataFile(con.getInode());
				if(contentMeta.exists() && contentMeta.length()>0) {

					String contentData=APILocator.getFileAssetAPI().getContentMetadataAsString(contentMeta);

					String lvar=con.getStructure().getVelocityVarName().toLowerCase();

					mlowered.put(lvar+".metadata.content", contentData);
				}
			}

			//The url is now stored under the identifier for html pages, so we need to index that also.
			if(con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE){
				mlowered.put(con.getStructure().getVelocityVarName().toLowerCase() + ".url", ident.getAssetName());
				mlowered.put(con.getStructure().getVelocityVarName().toLowerCase() + ".url_dotraw", ident.getAssetName());
			}

			return mlowered;
		} catch (Exception e) {
			//Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotMappingException(e.getMessage(), e);
		}
	}

	public Object toMappedObj(Contentlet con) throws DotMappingException {
		return toJson(con);
	}

	@SuppressWarnings("unchecked")
	protected void loadCategories(final Contentlet con, final Map<String,String> m)
			throws DotDataException, DotSecurityException {
	    // first we check if there is a category field in the structure. We don't hit db if not needed

	    final ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find(con.getContentTypeId());
	    List<com.dotcms.contenttype.model.field.Field> catFields = type.fields().stream()
				.filter(field -> field instanceof CategoryField).collect(CollectionsUtils.toImmutableList());

        if(catFields.isEmpty()) {
        	return;
		}

	    List<Category> myCats = APILocator.getCategoryAPI().getParents(con, APILocator.systemUser(), false);
	    final StringWriter myCatsString=new StringWriter();
	    for(final Category me : myCats){
	        myCatsString.append(me.getCategoryVelocityVarName()).append(" ");
	    }

        m.put(ESMappingConstants.CATEGORIES, myCatsString.toString());
        

	    for(final com.dotcms.contenttype.model.field.Field f : catFields){/*
	        StringWriter fieldCatString=new StringWriter();
	        Category parent = APILocator.getCategoryAPI().find(f.values(), APILocator.systemUser(), false);
            List<Category> childrens=APILocator.getCategoryAPI().getAllChildren(
                            parent, APILocator.systemUser(), false);
            for(Category me : myCats){
                if(childrens.contains(me)){
                    fieldCatString.append(me.getCategoryVelocityVarName()).append(" ");
                }
            }
            */
	        // I don't think we care if we put all the categories in each field
            m.put(type.variable() + "." + f.variable(), myCatsString.toString());
	    
	    }
	}

	@SuppressWarnings("unchecked")
	protected void loadPermissions(Contentlet con, Map<String,String> m) throws DotDataException {
		PermissionAPI permissionAPI = APILocator.getPermissionAPI();
		List<Permission> permissions = permissionAPI.getPermissions(con, false, false, false);
		StringBuilder permissionsSt = new StringBuilder();
		boolean ownerCanRead = false;
		boolean ownerCanWrite = false;
		boolean ownerCanPub = false;
		for (Permission permission : permissions) {
			String str = "P" + permission.getRoleId() + "." + permission.getPermission() + "P ";
			if (permissionsSt.toString().indexOf(str) < 0) {
				permissionsSt.append(str);
			}
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
		}
		m.put(ESMappingConstants.PERMISSIONS, permissionsSt.toString());
		m.put(ESMappingConstants.OWNER_CAN_READ, Boolean.toString(ownerCanRead));
		m.put(ESMappingConstants.OWNER_CAN_WRITE, Boolean.toString(ownerCanWrite));
		m.put(ESMappingConstants.OWNER_CAN_PUBLISH, Boolean.toString(ownerCanPub));
	}

	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
	public static final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");

	public static final String elasticSearchDateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss'Z'";
	public static final FastDateFormat elasticSearchDateTimeFormat = FastDateFormat.getInstance(elasticSearchDateTimeFormatPattern);

	public static final FastDateFormat timeFormat = FastDateFormat.getInstance("HHmmss");

	protected void loadFields(Contentlet con, Map<String, String> m) throws DotDataException {

		// https://github.com/dotCMS/dotCMS/issues/6152
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000", otherSymbols);

		FieldAPI fAPI=APILocator.getFieldAPI();
		List<Field> fields = new ArrayList<Field>(FieldsCache.getFieldsByStructureInode(con.getStructureInode()));

		Structure st=con.getStructure();
		for (Field f : fields) {
			if (f.getFieldType().equals(Field.FieldType.BINARY.toString())
					|| f.getFieldContentlet() != null && (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SYSTEM_FIELD) && !f.getFieldType().equals(Field.FieldType.TAG.toString()))) {
				continue;
			}
			if(!f.isIndexed()){
				continue;
			}
			try {
				if(fAPI.isElementConstant(f)){
					m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), (f.getValues() == null ? "":f.getValues().toString()));
					continue;
				}

				Object valueObj = con.get(f.getVelocityVarName());
				if(valueObj == null){
					valueObj = "";
				}
				if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SECTION_DIVIDER)) {
					valueObj = "";
				}

				if(!UtilMethods.isSet(valueObj)) {
					m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), "");
				}
				else if(f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_TIME)) {
					try{
						String timeStr=timeFormat.format(valueObj);
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), timeStr);
					}
					catch(Exception e){
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
					}
				}
				else if (f.getFieldType().equals(ESMappingConstants.FIELD_ELASTIC_TYPE_DATE)) {
					try {
						String dateString = dateFormat.format(valueObj);
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), dateString);
					}
					catch(Exception ex) {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
					}
				} else if(f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_DATE_TIME)) {
					try {
						String datetimeString = datetimeFormat.format(valueObj);
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), datetimeString);
					}
					catch(Exception ex) {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(),"");
					}
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CATEGORY)) {
					// moved the logic to loadCategories
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CHECKBOX) || f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_MULTI_SELECT)) {
					if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
					} else {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), UtilMethods.listToString(valueObj.toString()));
					}
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_KEY_VALUE)){
					boolean fileMetadata=f.getVelocityVarName().equals(FileAssetAPI.META_DATA_FIELD) && st.getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET;
					if(!fileMetadata || LicenseUtil.getLevel()>= LicenseLevel.STANDARD.level) {

						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), (String)valueObj);
						Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap((String)valueObj);

						Set<String> allowedFields=null;
						if(fileMetadata) {
							// http://jira.dotmarketing.net/browse/DOTCMS-7243
							List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(
									f.getInode(), APILocator.getUserAPI().getSystemUser(), false);
							for(FieldVariable fv : fieldVariables) {
								if(fv.getKey().equals(ESMappingConstants.DOT_INDEX_PATTERN)) {
									String[] names=fv.getValue().split(",");
									allowedFields=new HashSet<String>();
									for(String n : names)
										allowedFields.add(n.trim().toLowerCase());
								}
							}
							// aditional fields from the configuration file
							String configFields=Config.getStringProperty("INDEX_METADATA_FIELDS", "");
							if(configFields.trim().length()>0) {
								String[] names=configFields.split(",");
								if(names.length>0 && allowedFields==null)
									allowedFields=new HashSet<String>();
								for(String n : names)
									allowedFields.add(n.trim().toLowerCase());
							}
						}

						if(keyValueMap!=null && !keyValueMap.isEmpty())
							for(String key : keyValueMap.keySet())
								if(allowedFields==null || allowedFields.contains(key.toLowerCase()))
									m.put(st.getVelocityVarName() + "." + f.getVelocityVarName() + "." + key, (String)keyValueMap.get(key));
					}
				} else if(f.getFieldType().equals(Field.FieldType.TAG.toString())) {

					StringBuilder personaTags = new StringBuilder();
					StringBuilder tagg = new StringBuilder();
					List<Tag> tagList = APILocator.getTagAPI().getTagsByInode(con.getInode());
					if(tagList ==null || tagList.size()==0) continue;

					final String tagDelimit = Config.getStringProperty("ES_TAG_DELIMITER_PATTERN", ",,");


					for ( Tag t : tagList ) {
						if(t.getTagName() ==null) continue;
						String myTag = t.getTagName().trim();
						tagg.append(myTag).append(tagDelimit);
						if ( t.isPersona() ) {
							personaTags.append(myTag).append(' ');
						}
					}
					if(tagg.length() >tagDelimit.length()){
						String taggStr = tagg.substring(0, tagg.length()-tagDelimit.length());
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), taggStr.replaceAll(",,", " "));
						m.put(ESMappingConstants.TAGS, taggStr);
					}


					if ( Structure.STRUCTURE_TYPE_PERSONA != con.getStructure().getStructureType() ) {
						if ( personaTags.length() > tagDelimit.length() ) {
							String personaStr = personaTags.substring(0, personaTags.length()-tagDelimit.length());
							m.put(st.getVelocityVarName() + "."+ESMappingConstants.PERSONAS, personaStr);
							m.put(ESMappingConstants.PERSONAS, personaStr);
						}
					}

				} else {
					if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
					} else if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_FLOAT) || f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_INTEGER)) {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), numFormatter.format(valueObj));
					} else {
						m.put(st.getVelocityVarName() + "." + f.getVelocityVarName(), valueObj.toString());
					}
				}
			} catch (Exception e) {
				Logger.warn(ESMappingAPIImpl.class, "Error indexing field: " + f.getFieldName()
						+ " of contentlet: " + con.getInode(), e);
				throw new DotDataException(e.getMessage(),e);
			}
		}
	}
	public String toJsonString(Map<String, Object> map) throws IOException{
		return mapper.writeValueAsString(map);
	}
	public List<String> dependenciesLeftToReindex(Contentlet con) throws DotStateException, DotDataException, DotSecurityException {
		List<String> dependenciesToReindex = new ArrayList<String>();

		ContentletAPI conAPI=APILocator.getContentletAPI();

		String relatedSQL = "select tree.* from tree where parent = ? or child = ? order by tree_order";
		DotConnect db = new DotConnect();
		db.setSQL(relatedSQL);
		db.addParam(con.getIdentifier());
		db.addParam(con.getIdentifier());
		ArrayList<HashMap<String, String>> relatedContentlets = db.loadResults();

		if(relatedContentlets.size()>0) {

			List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType(con.getStructure());

			for(Relationship rel : relationships) {

				List<Contentlet> oldDocs = new ArrayList <Contentlet>();

				String q = "";
				boolean isSameStructRelationship = rel.getParentStructureInode().equalsIgnoreCase(rel.getChildStructureInode());

				if(isSameStructRelationship)
					q = "+type:content +(" + rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_PARENT+":" + con.getIdentifier() + " " +
							rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_CHILD+":" + con.getIdentifier() + ") ";
				else
					q = "+type:content +" + rel.getRelationTypeValue() + ":" + con.getIdentifier();

				oldDocs  = conAPI.search(q, -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);

				List<String> oldRelatedIds = new ArrayList<String>();
				if(oldDocs.size() > 0) {
					for(Contentlet oldDoc : oldDocs) {
						oldRelatedIds.add(oldDoc.getIdentifier());
					}
				}

				List<String> newRelatedIds = new ArrayList<String>();
				for(HashMap<String, String> relatedEntry : relatedContentlets) {
					String childId = relatedEntry.get(ESMappingConstants.CHILD);
					String parentId = relatedEntry.get(ESMappingConstants.PARENT);
					if(relatedEntry.get(ESMappingConstants.RELATION_TYPE).equals(rel.getRelationTypeValue())) {
						if(con.getIdentifier().equalsIgnoreCase(childId)) {
							newRelatedIds.add(parentId);
							oldRelatedIds.remove(parentId);
						} else {
							newRelatedIds.add(childId);
							oldRelatedIds.remove(childId);
						}
					}
				}

				//Taking the disjunction of both collections will give the old list of dependencies that need to be removed from the
				//re-indexation and the list of new dependencies no re-indexed yet
				dependenciesToReindex.addAll(CollectionUtils.disjunction(oldRelatedIds, newRelatedIds));
			}
		}
		return dependenciesToReindex;
	}

	protected void loadRelationshipFields(Contentlet con, Map<String,String> m) throws DotStateException, DotDataException {
		DotConnect db = new DotConnect();
		db.setSQL("select * from tree where parent = ? or child = ? order by tree_order asc");
		db.addParam(con.getIdentifier());
		db.addParam(con.getIdentifier());

		for(Map<String, Object> relatedEntry : db.loadObjectResults()) {

			String childId = relatedEntry.get(ESMappingConstants.CHILD).toString();
			String parentId = relatedEntry.get(ESMappingConstants.PARENT).toString();
			String relType=relatedEntry.get(ESMappingConstants.RELATION_TYPE).toString();
			String order = relatedEntry.get(ESMappingConstants.TREE_ORDER).toString();

			if("child".equals(relType)) {
				continue;
			}

			Relationship rel = FactoryLocator.getRelationshipFactory().byTypeValue(relType);

			if(rel!=null && InodeUtils.isSet(rel.getInode())) {
				boolean isSameStructRelationship = rel.getParentStructureInode().equals(rel.getChildStructureInode());

				String propName = isSameStructRelationship ?
						(con.getIdentifier().equals(parentId)?rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_CHILD:rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_PARENT)
						: rel.getRelationTypeValue();

				String orderKey = rel.getRelationTypeValue()+ESMappingConstants.SUFFIX_ORDER;


                if(relType.equals(rel.getRelationTypeValue())) {
                    String me = con.getIdentifier();
                    String related = me.equals(childId)? parentId : childId;

					String previousPropNameValue = m.get(propName);
					int previousPropNameValueLength = previousPropNameValue!=null?previousPropNameValue.length():0;

					StringBuilder propNameValue = new StringBuilder(previousPropNameValueLength + UUID_LENGTH + 1);

					// put a pointer to the related content
					m.put(propName, propNameValue.append(previousPropNameValue != null ? previousPropNameValue : "")
							.append(related).append(" ").toString() );

					String previousOrderKeyValue = m.get(orderKey);
					int previousOrderKeyValueLength = previousOrderKeyValue!=null?previousOrderKeyValue.length():0;
					int orderLength = order!=null?order.length():0;

					StringBuilder orderKeyValue = new StringBuilder(previousOrderKeyValueLength + UUID_LENGTH + 1 + orderLength + 1);

					// make a way to sort
					m.put(orderKey, orderKeyValue.append(previousOrderKeyValue!=null ? previousOrderKeyValue : "")
							.append(related).append("_").append(order).append(" ").toString());
				}
			}
		}

	}


}