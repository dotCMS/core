package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.tika.TikaUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
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
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.common.xcontent.XContentType;


public class ESMappingAPIImpl implements ContentMappingAPI {

	private static final int UUID_LENGTH = 36;
	public static final String TEXT = "_text";
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

		final ActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices()
				.preparePutMapping().setIndices(indexName).setType(type)
				.setSource(mapping, XContentType.JSON).execute();
		return lis.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS).isAcknowledged();
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
		final ActionFuture<PutMappingResponse> lis = new ESClient().getClient().admin().indices()
				.preparePutMapping().setIndices(indexName).setType(type)
				.setSource(mapping, XContentType.JSON).execute();
		return lis.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS).isAcknowledged();
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
				.actionGet(INDEX_OPERATIONS_TIMEOUT_IN_MS).getState().metaData().indices()
				.get(index).mapping(type).source().string();

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
	@CloseDBIfOpened
	public Map<String,Object> toMap(final Contentlet contentlet) throws DotMappingException {

		try {

			final Map<String,Object> contentletMap = new HashMap();
			final Map<String,Object> mlowered	   = new HashMap();
			loadCategories(contentlet, contentletMap);
			loadFields(contentlet, contentletMap);
			loadPermissions(contentlet, contentletMap);
			loadRelationshipFields(contentlet, contentletMap);

			Identifier ident = APILocator.getIdentifierAPI().find(contentlet);
			ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), contentlet.getLanguageId());
			Structure st=CacheLocator.getContentTypeCache().getStructureByInode(contentlet.getStructureInode());

			Folder conFolder=APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
			Host conHost = APILocator.getHostAPI().find(ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
			
			contentletMap.put(ESMappingConstants.TITLE, contentlet.getTitle());
			contentletMap.put(ESMappingConstants.STRUCTURE_NAME, st.getVelocityVarName()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.CONTENT_TYPE, st.getVelocityVarName());
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE, st.getStructureType()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE  + TEXT, Integer.toString(st.getStructureType())); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.BASE_TYPE, st.getStructureType());
			contentletMap.put(ESMappingConstants.BASE_TYPE + TEXT, Integer.toString(st.getStructureType()));
			contentletMap.put(ESMappingConstants.TYPE, ESMappingConstants.CONTENT);
			contentletMap.put(ESMappingConstants.INODE, contentlet.getInode());
			contentletMap.put(ESMappingConstants.MOD_DATE, elasticSearchDateTimeFormat.format(contentlet.getModDate()));
			contentletMap.put(ESMappingConstants.MOD_DATE + TEXT, datetimeFormat.format(contentlet.getModDate()));
			contentletMap.put(ESMappingConstants.OWNER, contentlet.getOwner()==null ? "0" : contentlet.getOwner());
			contentletMap.put(ESMappingConstants.MOD_USER, contentlet.getModUser());
			contentletMap.put(ESMappingConstants.LIVE, contentlet.isLive());
			contentletMap.put(ESMappingConstants.LIVE + TEXT, Boolean.toString(contentlet.isLive()));
			contentletMap.put(ESMappingConstants.WORKING, contentlet.isWorking());
			contentletMap.put(ESMappingConstants.WORKING + TEXT, Boolean.toString(contentlet.isWorking()));
			contentletMap.put(ESMappingConstants.LOCKED, contentlet.isLocked());
			contentletMap.put(ESMappingConstants.LOCKED + TEXT, Boolean.toString(contentlet.isLocked()));
			contentletMap.put(ESMappingConstants.DELETED, contentlet.isArchived());
			contentletMap.put(ESMappingConstants.DELETED + TEXT, Boolean.toString(contentlet.isArchived()));
			contentletMap.put(ESMappingConstants.LANGUAGE_ID, contentlet.getLanguageId());
			contentletMap.put(ESMappingConstants.LANGUAGE_ID + TEXT, Long.toString(contentlet.getLanguageId()));
			contentletMap.put(ESMappingConstants.IDENTIFIER, ident.getId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOST, ident.getHostId());
	        contentletMap.put(ESMappingConstants.CONTENTLET_HOSTNAME, conHost.getHostname());
			
			
			
			contentletMap.put(ESMappingConstants.CONTENTLET_FOLER, conFolder!=null && InodeUtils.isSet(conFolder.getInode()) ? conFolder.getInode() : contentlet.getFolder());
			contentletMap.put(ESMappingConstants.PARENT_PATH, ident.getParentPath());
			contentletMap.put(ESMappingConstants.PATH, ident.getPath());
			// makes shorties searchable regardless of length
			contentletMap.put(ESMappingConstants.SHORT_ID, ident.getId().replace("-", ""));
			contentletMap.put(ESMappingConstants.SHORT_INODE, contentlet.getInode().replace("-", ""));
			
			//add workflow to map
			contentletMap.putAll(getWorkflowInfoForContentlet(contentlet));

			if(UtilMethods.isSet(ident.getSysPublishDate())) {
				contentletMap.put(ESMappingConstants.PUBLISH_DATE, elasticSearchDateTimeFormat.format(ident.getSysPublishDate()));
				contentletMap.put(ESMappingConstants.PUBLISH_DATE + TEXT,
						datetimeFormat.format(ident.getSysPublishDate()));
			}else {
				contentletMap.put(ESMappingConstants.PUBLISH_DATE, elasticSearchDateTimeFormat.format(cvi.getVersionTs()));
				contentletMap.put(ESMappingConstants.PUBLISH_DATE + TEXT,
						datetimeFormat.format(cvi.getVersionTs()));
			}

			if(UtilMethods.isSet(ident.getSysExpireDate())) {
				contentletMap.put(ESMappingConstants.EXPIRE_DATE, elasticSearchDateTimeFormat.format(ident.getSysExpireDate()));
				contentletMap.put(ESMappingConstants.EXPIRE_DATE + TEXT,
						datetimeFormat.format(ident.getSysExpireDate()));
			}else {
				contentletMap.put(ESMappingConstants.EXPIRE_DATE, elasticSearchDateTimeFormat.format(29990101000000L));
				contentletMap.put(ESMappingConstants.EXPIRE_DATE + TEXT, "29990101000000");
			}

			contentletMap.put(ESMappingConstants.VERSION_TS, elasticSearchDateTimeFormat.format(cvi.getVersionTs()));
			contentletMap.put(ESMappingConstants.VERSION_TS + TEXT, datetimeFormat.format(cvi.getVersionTs()));

			String urlMap = null;
			try{
				urlMap = APILocator.getContentletAPI().getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), true);
				if(urlMap != null){
					contentletMap.put(ESMappingConstants.URL_MAP,urlMap );
				}
			}
			catch(Exception e){
				Logger.warn(this.getClass(), "Cannot get URLMap for structure : "+ st.getName() + " and contentlet.id : " + ((ident != null) ? ident.getId() : contentlet) + " , reason: "+e.getMessage());
				
			}

			final StringWriter sw = new StringWriter();
			for(final Entry<String,Object> entry : contentletMap.entrySet()){
				final String lcasek=entry.getKey().toLowerCase();
				Object lcasev = entry.getValue();

				if (UtilMethods.isSet(lcasev) && lcasev instanceof String){
					lcasev = ((String) lcasev).toLowerCase();

					if (!lcasek.endsWith(TEXT)){
						//for example: when lcasev=moddate, moddate_dotraw must be created from its moddate_text if exists
						//when the moddate_text is evaluated.
						if (!contentletMap.containsKey(entry.getKey() + TEXT)){
							mlowered.put(lcasek + "_dotraw", lcasev);
						}
					}else{
						mlowered.put(lcasek.replace(TEXT, "_dotraw"), lcasev);
					}
				}

				mlowered.put(lcasek, lcasev);

				if(lcasev!=null) {
					sw.append(lcasev.toString()).append(' ');
				}
			}



			if(contentlet.getStructure().getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
				// see if we have content metadata
				File contentMeta=APILocator.getFileAssetAPI().getContentMetadataFile(contentlet.getInode());
				if(contentMeta.exists() && contentMeta.length()>0) {

					String contentData=APILocator.getFileAssetAPI().getContentMetadataAsString(contentMeta);

					mlowered.put(FileAssetAPI.META_DATA_FIELD.toLowerCase() + StringPool.PERIOD + "content", contentData);
					sw.append(contentData).append(' ');
				}
			}

			//The url is now stored under the identifier for html pages, so we need to index that also.
			if(contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE){
				mlowered.put(contentlet.getStructure().getVelocityVarName().toLowerCase() + ".url", ident.getAssetName());
				mlowered.put(contentlet.getStructure().getVelocityVarName().toLowerCase() + ".url_dotraw", ident.getAssetName());
				sw.append(ident.getAssetName());
			}

			mlowered.put("catchall", sw.toString());

			return mlowered;
		} catch (Exception e) {
			throw new DotMappingException(e.getMessage(), e);
		}
	}

    /**
     * Adds the current workflow task to the contentlet in order to be reindexed.
     * 
     * @param contentlet {@link Contentlet}
     * @return {@link Map}
     */
    protected Map<String, Object> getWorkflowInfoForContentlet(final Contentlet contentlet) {
        
        final Map<String, Object> workflowMap = new HashMap<>();
        final WorkflowAPI workflowAPI 		  = APILocator.getWorkflowAPI();

        try {

            final WorkflowTask task 		  = workflowAPI.findTaskByContentlet(contentlet);

            if(task != null && task.getId() != null && null != task.getStatus()) {

                final WorkflowStep step = workflowAPI.findStep(task.getStatus());
                workflowMap.put(ESMappingConstants.WORKFLOW_SCHEME, step.getSchemeId());
                workflowMap.put(ESMappingConstants.WORKFLOW_STEP, task.getStatus());
				workflowMap.put(ESMappingConstants.WORKFLOW_CURRENT_STEP, step.getName());
                workflowMap.put(ESMappingConstants.WORKFLOW_CREATED_BY, task.getCreatedBy());
                workflowMap.put(ESMappingConstants.WORKFLOW_ASSIGN, task.getAssignedTo());
                workflowMap.put(ESMappingConstants.WORKFLOW_MOD_DATE, elasticSearchDateTimeFormat.format(task.getModDate()));
                workflowMap.put(ESMappingConstants.WORKFLOW_MOD_DATE + TEXT, datetimeFormat.format(task.getModDate()));
            }
                
        } catch (Exception e) {
            Logger.debug(this.getClass(), "No workflow info for contentlet " +  contentlet.getIdentifier());
        }

        if(workflowMap.isEmpty()) {

            try {

                final List<String> stepIds = new ArrayList<>();
                final Set<String> schemeWriter = new HashSet<>();
                final List<WorkflowScheme> schemes = workflowAPI.findSchemesForContentType(contentlet.getContentType());
                for (final WorkflowScheme scheme : schemes) {
                    final List<WorkflowStep> steps = workflowAPI.findSteps(scheme);
                    if (steps != null && !steps.isEmpty()) {
                        schemeWriter.add(scheme.getId());
                        stepIds.add(steps.get(0).getId());
                    }
                }
    
                workflowMap.put(ESMappingConstants.WORKFLOW_SCHEME, String.join(" ", schemeWriter));
                workflowMap.put(ESMappingConstants.WORKFLOW_STEP, stepIds);
				workflowMap.put(ESMappingConstants.WORKFLOW_CURRENT_STEP, ESMappingConstants.WORKFLOW_CURRENT_STEP_NOT_ASSIGNED_VALUE); // multiple steps -> not assigned.
            } catch (Exception e) {
                Logger.error(this.getClass(), "unable to add workflow info to index:" + e, e);
            }
        }

        return workflowMap;
    }

	public Object toMappedObj(Contentlet con) throws DotMappingException {
		return toJson(con);
	}

	@SuppressWarnings("unchecked")
	protected void loadCategories(final Contentlet con, final Map<String,Object> m)
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
        

	    for(final com.dotcms.contenttype.model.field.Field f : catFields){
	        // I don't think we care if we put all the categories in each field
            m.put(type.variable() + "." + f.variable(), myCatsString.toString());
	    
	    }
	}

	@SuppressWarnings("unchecked")
	protected void loadPermissions(final Contentlet con, final Map<String,Object> m) throws DotDataException {
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
		m.put(ESMappingConstants.OWNER_CAN_READ, ownerCanRead);
		m.put(ESMappingConstants.OWNER_CAN_READ + TEXT, Boolean.toString(ownerCanRead));
		m.put(ESMappingConstants.OWNER_CAN_WRITE, ownerCanWrite);
		m.put(ESMappingConstants.OWNER_CAN_WRITE + TEXT, Boolean.toString(ownerCanWrite));
		m.put(ESMappingConstants.OWNER_CAN_PUBLISH, ownerCanPub);
		m.put(ESMappingConstants.OWNER_CAN_PUBLISH + TEXT, Boolean.toString(ownerCanPub));
	}

	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
	public static final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");

	public static final String elasticSearchDateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss";
	public static final FastDateFormat elasticSearchDateTimeFormat = FastDateFormat.getInstance(elasticSearchDateTimeFormatPattern);

	public static final FastDateFormat timeFormat = FastDateFormat.getInstance("HH:mm:ss");

	protected void loadFields(Contentlet con, Map<String, Object> m) throws DotDataException {

		// https://github.com/dotCMS/dotCMS/issues/6152
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000", otherSymbols);

		FieldAPI fAPI=APILocator.getFieldAPI();
		final List<Field> fields = new ArrayList<>(
				FieldsCache.getFieldsByStructureInode(con.getStructureInode()));

		Structure st=con.getStructure();
		StringBuilder keyNameBuilder;
		String keyName;
		String keyNameText;

		final TikaUtils tikaUtils = new TikaUtils();

		for (Field f : fields) {

			keyNameBuilder = new StringBuilder(st.getVelocityVarName()).append(".")
					.append(f.getVelocityVarName());
			keyName        = keyNameBuilder.toString();
			keyNameText    = keyNameBuilder.append(TEXT).toString();
			if (f.getFieldType().equals(Field.FieldType.BINARY.toString())
					|| f.getFieldContentlet() != null && (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SYSTEM_FIELD) && !f.getFieldType().equals(Field.FieldType.TAG.toString()))) {
				continue;
			}
			if(!f.isIndexed()){
				continue;
			}
			try {
				if(fAPI.isElementConstant(f)){
					m.put(keyName, (f.getValues() == null ? "":f.getValues()));
					continue;
				}

				Object valueObj = con.get(f.getVelocityVarName());

				if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SECTION_DIVIDER)) {
					valueObj = "";
				}

				if (!UtilMethods.isSet(valueObj) && !f.getFieldType()
						.equals(Field.FieldType.TAG.toString())) {
					m.put(keyName, null);
				}
				else if(f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_TIME)) {
					try{
						String timeStr=timeFormat.format(valueObj);
						m.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						m.put(keyNameText, timeStr);
					}
					catch(Exception e){
						m.put(keyName, null);
						m.put(keyNameText, null);
					}
				}
				else if (f.getFieldType().equals(ESMappingConstants.FIELD_ELASTIC_TYPE_DATE)) {
					try {
						String dateString = dateFormat.format(valueObj);
						m.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						m.put(keyNameText, dateString);
					}
					catch(Exception ex) {
						m.put(keyName, null);
						m.put(keyNameText, null);
					}
				} else if(f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_DATE_TIME)) {
					try {
						String datetimeString = datetimeFormat.format(valueObj);
						m.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						m.put(keyNameText, datetimeString);
					}
					catch(Exception ex) {
						m.put(keyName, null);
						m.put(keyNameText, null);
					}
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CATEGORY)) {
					// moved the logic to loadCategories
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CHECKBOX) || f
						.getFieldType().equals(ESMappingConstants.FIELD_TYPE_MULTI_SELECT)) {
					if (f.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						m.put(keyName, valueObj);
						m.put(keyNameText, valueObj.toString());
					} else {
						m.put(keyName,
								UtilMethods.listToString(valueObj.toString()));
					}
				} else if (f.getFieldType().equals(ESMappingConstants.FIELD_TYPE_KEY_VALUE)){
					final boolean fileMetadata =
							f.getVelocityVarName().equals(FileAssetAPI.META_DATA_FIELD)
									&& st.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET;
					if(!fileMetadata || LicenseUtil.getLevel()>= LicenseLevel.STANDARD.level) {

						Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap((String)valueObj);

						Set<String> allowedFields = new HashSet<>();
						if(fileMetadata) {
							// http://jira.dotmarketing.net/browse/DOTCMS-7243
							List<FieldVariable> fieldVariables=APILocator.getFieldAPI().getFieldVariablesForField(
									f.getInode(), APILocator.getUserAPI().getSystemUser(), false);
							for(FieldVariable fv : fieldVariables) {
								if(fv.getKey().equals(ESMappingConstants.DOT_INDEX_PATTERN)) {
									String[] names=fv.getValue().split(",");
									allowedFields=new HashSet<>();
									for(String n : names)
										allowedFields.add(n.trim().toLowerCase());
								}
							}

							allowedFields
									.addAll(tikaUtils.getConfiguredMetadataFields());

							tikaUtils.filterMetadataFields(keyValueMap, allowedFields);

							keyValueMap.forEach((k, v) -> m
									.put(FileAssetAPI.META_DATA_FIELD.toLowerCase() + StringPool.PERIOD + k, v));
						}
					}
				} else if(f.getFieldType().equals(Field.FieldType.TAG.toString())) {

					StringBuilder personaTags = new StringBuilder();
					List<String> tagg = new ArrayList<>();
					List<Tag> tagList = APILocator.getTagAPI().getTagsByInode(con.getInode());
					if(tagList ==null || tagList.size()==0) continue;

					final String tagDelimit = Config.getStringProperty("ES_TAG_DELIMITER_PATTERN", ",,");


					for ( Tag t : tagList ) {
						if(t.getTagName() ==null) continue;
						String myTag = t.getTagName().trim();
						tagg.add(myTag);
						if ( t.isPersona() ) {
							personaTags.append(myTag).append(' ');
						}
					}

					m.put(keyName, tagg);
					m.put(ESMappingConstants.TAGS, tagg);

					if ( Structure.STRUCTURE_TYPE_PERSONA != con.getStructure().getStructureType() ) {
						if ( personaTags.length() > tagDelimit.length() ) {
							String personaStr = personaTags.substring(0, personaTags.length()-tagDelimit.length());
							m.put(new StringBuilder(st.getVelocityVarName()).append(".")
									.append(ESMappingConstants.PERSONAS).toString(), personaStr);
							m.put(ESMappingConstants.PERSONAS, personaStr);
						}
					}

				} else {
					if (f.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						m.put(keyName, valueObj);
						m.put(keyNameText,valueObj.toString());
					} else if (f.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_FLOAT) || f
							.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_INTEGER)) {
						m.put(keyName, valueObj);
						m.put(keyNameText, numFormatter.format(valueObj));
					} else {
						m.put(keyName, valueObj);
						m.put(keyNameText, valueObj.toString());
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

	@CloseDBIfOpened
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

				List<Contentlet> oldDocs;

				StringBuilder q = new StringBuilder();
				boolean isSameStructRelationship = FactoryLocator.getRelationshipFactory().sameParentAndChild(rel);

				if(isSameStructRelationship) {
					q.append("+type:content +(").append(rel.getRelationTypeValue())
							.append(ESMappingConstants.SUFFIX_PARENT).append(":")
							.append(con.getIdentifier())
							.append(" ").append(rel.getRelationTypeValue())
							.append(ESMappingConstants.SUFFIX_CHILD).append(":")
							.append(con.getIdentifier()).append(") ");
				}else {
					q.append("+type:content +").append(rel.getRelationTypeValue()).append(":")
							.append(con.getIdentifier());
				}
				oldDocs = conAPI
						.search(q.toString(), -1, 0, null, APILocator.getUserAPI().getSystemUser(),
								false);

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
				dependenciesToReindex.addAll(
						CollectionUtils.disjunction(oldRelatedIds, newRelatedIds));
			}
		}
		return dependenciesToReindex;
	}

	protected void loadRelationshipFields(final Contentlet con, final Map<String, Object> m)
			throws DotStateException, DotDataException {
		String propName;
		final Map<String, List> relationshipsRecords = new HashMap<>();
		String orderKey;

		DotConnect db = new DotConnect();
		db.setSQL("select * from tree where parent = ? or child = ? order by tree_order asc");
		db.addParam(con.getIdentifier());
		db.addParam(con.getIdentifier());

		for (Map<String, Object> relatedEntry : db.loadObjectResults()) {

			String childId = relatedEntry.get(ESMappingConstants.CHILD).toString();
			String parentId = relatedEntry.get(ESMappingConstants.PARENT).toString();
			String relType = relatedEntry.get(ESMappingConstants.RELATION_TYPE).toString();
			String order = relatedEntry.get(ESMappingConstants.TREE_ORDER).toString();

			if ("child".equals(relType)) {
				continue;
			}

			Relationship rel = FactoryLocator.getRelationshipFactory().byTypeValue(relType);

			if (rel != null && InodeUtils.isSet(rel.getInode())) {

				boolean isSameStructRelationship = FactoryLocator.getRelationshipFactory()
						.sameParentAndChild(rel);

				//Support for legacy relationships
				propName = isSameStructRelationship ?
						(con.getIdentifier().equals(parentId) ? rel.getRelationTypeValue()
								+ ESMappingConstants.SUFFIX_CHILD
								: rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_PARENT)
						: rel.getRelationTypeValue();

				orderKey = rel.getRelationTypeValue() + ESMappingConstants.SUFFIX_ORDER;

				if (relType.equals(rel.getRelationTypeValue())) {
					String me = con.getIdentifier();
					String related = me.equals(childId) ? parentId : childId;

					String previousPropNameValue = (String) m.get(propName);
					int previousPropNameValueLength =
							previousPropNameValue != null ? previousPropNameValue.length() : 0;

					StringBuilder propNameValue = new StringBuilder(
							previousPropNameValueLength + UUID_LENGTH + 1);

					// put a pointer to the related content
					m.put(propName, propNameValue
							.append(previousPropNameValue != null ? previousPropNameValue : "")
							.append(related).append(" ").toString());

					String previousOrderKeyValue = (String) m.get(orderKey);
					int previousOrderKeyValueLength =
							previousOrderKeyValue != null ? previousOrderKeyValue.length() : 0;
					int orderLength = order != null ? order.length() : 0;

					StringBuilder orderKeyValue = new StringBuilder(
							previousOrderKeyValueLength + UUID_LENGTH + 1 + orderLength + 1);

					// make a way to sort
					m.put(orderKey, orderKeyValue
							.append(previousOrderKeyValue != null ? previousOrderKeyValue : "")
							.append(related).append("_").append(order).append(" ").toString());

					addRelationshipRecords(con, me.equals(childId) ? rel.getParentRelationName()
							: rel.getChildRelationName(), related, relationshipsRecords, m);
				}
			}
		}

		//Adding new relationships fields to the index map
		m.putAll(relationshipsRecords);

	}

	/**
	 * Groups all relationships records by relationship field
	 */
	private void addRelationshipRecords(final Contentlet contentlet, final String relationName,
			final String related,
			final Map<String, List> relationshipsRecords, final Map<String, Object> mapping) {

		final ContentType contentType = contentlet.getContentType();
		if (relationName != null) {
			final String key = contentType.variable() + StringPool.PERIOD + relationName;

			//this relationship has been already added
			if (mapping.containsKey(key)) {
				return;
			}
			if (!relationshipsRecords.containsKey(key)) {
				try {
					//Search for a relationship field
					final com.dotcms.contenttype.model.field.Field field = APILocator
							.getContentTypeFieldAPI()
							.byContentTypeAndVar(contentType, relationName);
					if (field != null) {
						relationshipsRecords.put(key, new ArrayList());
						relationshipsRecords.get(key).add(related);
					}
				} catch (NotFoundInDbException e) {
					//Do nothing and continue searching for others relationships fields
				} catch (DotDataException e) {
					Logger.warn(this, "Error getting field for relation type " + key, e);
				}

			}
		}
	}
}