package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PERSONA_KEY_TAG;
import static com.dotcms.contenttype.model.field.LegacyFieldTypes.CUSTOM_FIELD;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_KEY_TAG_FIELD_VAR;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
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
import com.dotmarketing.business.RelationshipAPI;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * Implementation class for the {@link ContentMappingAPI}.
 * <p>
 * This class provides useful methods and mechanisms to map properties that are present in a {@link Contentlet} object,
 * specially for ES indexation purposes.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class ESMappingAPIImpl implements ContentMappingAPI {

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
     * This method takes a mapping string and puts it in a collection of
     * indexes
     * @param indexes
     * @param mapping
     * @return
     * @throws ElasticsearchException
     * @throws IOException
     */
    public boolean putMapping(final List<String> indexes, final String mapping)
            throws ElasticsearchException, IOException {

        final PutMappingRequest request = new PutMappingRequest(
                indexes.stream().map(indexName -> APILocator.getESIndexAPI()
                        .getNameWithClusterIDPrefix(indexName)).toArray(String[]::new));
        request.setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        request.source(mapping, XContentType.JSON);

        final AcknowledgedResponse putMappingResponse = RestHighLevelClientProvider.getInstance()
                .getClient().indices()
                .putMapping(request, RequestOptions.DEFAULT);

        return putMappingResponse.isAcknowledged();
    }

    /**
     * This method takes a mapping string and puts it in the specified index
     * @param indexName
     * @param mapping
     * @return
     * @throws ElasticsearchException
     * @throws IOException
     */
    public boolean putMapping(final String indexName, final String mapping)
            throws ElasticsearchException, IOException {

        return putMapping(CollectionsUtils.list(indexName), mapping);
    }


	/**
	 * Gets the mapping params for an index and type
	 * @param index
	 * @return
	 * @throws ElasticsearchException
	 * @throws IOException
	 */
	public  String getMapping(final String index) throws ElasticsearchException, IOException{

		final GetMappingsRequest request = new GetMappingsRequest();
		request.indices(index);

		final GetMappingsResponse getMappingResponse = RestHighLevelClientProvider.getInstance().getClient()
				.indices().getMapping(request, RequestOptions.DEFAULT);

		return getMappingResponse.mappings().get(index).source().string();
	}

	public Map<String, Object> getFieldMappingAsMap(final String index, final String fieldName) throws IOException {
	    final GetFieldMappingsRequest request = new GetFieldMappingsRequest();
	    request.indices(index).fields(fieldName);
        final GetFieldMappingsResponse getMappingResponse = RestHighLevelClientProvider.getInstance().getClient()
                .indices().getFieldMapping(request, RequestOptions.DEFAULT);

        return getMappingResponse.mappings().get(index).get(fieldName) != null ? getMappingResponse
                .mappings().get(index).get(fieldName).sourceAsMap() : Collections
                .emptyMap();
    }

	@SuppressWarnings("unchecked")
	public String toJson(final Contentlet contentlet) throws DotMappingException {

		try {
			final Map<String,Object> contentletMap = toMap(contentlet);
			return mapper.writeValueAsString(contentletMap);
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
			final StringWriter sw = new StringWriter();
			final User systemUser = APILocator.getUserAPI().getSystemUser();
			final Map<String,Object> contentletMap = new HashMap();
			final Map<String,Object> mlowered	   = new HashMap();
			loadCategories(contentlet, contentletMap);
			loadFields(contentlet, contentletMap);
			loadPermissions(contentlet, contentletMap);
            loadRelationshipFields(contentlet, contentletMap, sw);

			final Identifier contentIdentifier = APILocator.getIdentifierAPI().find(contentlet);
            if (null == contentIdentifier || !UtilMethods.isSet(contentIdentifier.getId())) {
                final String errorMsg = String.format("Identifier '%s' was not found via API.", contentlet
                        .getIdentifier());
                throw new DotDataException(errorMsg);
            }
			final ContentletVersionInfo versionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(contentIdentifier.getId(), contentlet.getLanguageId());
            if (null == versionInfo || !UtilMethods.isSet(versionInfo.getIdentifier())) {
                final String errorMsg = String.format("Version Info for Identifier '%s' and Language '%s' was not" +
                        " found via API.", contentIdentifier.getId(), contentlet.getLanguageId());
                throw new DotDataException(errorMsg);
            }
			final ContentType contentType = CacheLocator.getContentTypeCache2().byVarOrInode(contentlet.getContentTypeId());
            if (null == contentType || !UtilMethods.isSet(contentType.id())) {
                final String errorMsg = String.format("Content Type with ID '%s' was not found via API.",
                        contentlet.getContentTypeId());
                throw new DotDataException(errorMsg);
            }
			final Folder contentFolder = APILocator.getFolderAPI().findFolderByPath(contentIdentifier.getParentPath(), contentIdentifier.getHostId(), systemUser, false);
            if (null == contentFolder || !UtilMethods.isSet(contentFolder.getIdentifier())) {
                final String errorMsg = String.format("Parent folder '%s' in Site '%s' was not found via API. Please " +
                        "check that the specified value points to a valid folder.", contentIdentifier.getParentPath()
                        , contentIdentifier.getHostId());
                throw new DotDataException(errorMsg);
            }
			final Host contentSite = APILocator.getHostAPI().find(contentIdentifier.getHostId(), systemUser, false);
            if (null == contentSite || !UtilMethods.isSet(contentSite.getIdentifier())) {
                final String errorMsg = String.format("Site with ID '%s' was not found via API. Please check that the" +
                        " specified value points to a valid Site.", contentIdentifier.getHostId());
                throw new DotDataException(errorMsg);
            }

			contentletMap.put(ESMappingConstants.TITLE, contentlet.getTitle());
			contentletMap.put(ESMappingConstants.STRUCTURE_NAME, contentType.variable()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.CONTENT_TYPE, contentType.variable());
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE, contentType.baseType().getType()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.BASE_TYPE, contentType.baseType().getType());
			contentletMap.put(ESMappingConstants.TYPE, ESMappingConstants.CONTENT);
			contentletMap.put(ESMappingConstants.INODE, contentlet.getInode());
			contentletMap.put(ESMappingConstants.MOD_DATE, elasticSearchDateTimeFormat.format(contentlet.getModDate()));
			contentletMap.put(ESMappingConstants.OWNER, contentlet.getOwner()==null ? "0" : contentlet.getOwner());
			contentletMap.put(ESMappingConstants.MOD_USER, contentlet.getModUser());
			contentletMap.put(ESMappingConstants.LIVE, contentlet.isLive());
			contentletMap.put(ESMappingConstants.WORKING, contentlet.isWorking());
			contentletMap.put(ESMappingConstants.LOCKED, contentlet.isLocked());
			contentletMap.put(ESMappingConstants.DELETED, contentlet.isArchived());
			contentletMap.put(ESMappingConstants.LANGUAGE_ID, contentlet.getLanguageId());
			contentletMap.put(ESMappingConstants.IDENTIFIER, contentIdentifier.getId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOST, contentIdentifier.getHostId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOSTNAME, contentSite.getHostname());
			contentletMap.put(ESMappingConstants.CONTENTLET_FOLER, contentFolder!=null && InodeUtils.isSet(contentFolder.getInode()) ? contentFolder.getInode() : contentlet.getFolder());
			contentletMap.put(ESMappingConstants.PARENT_PATH, contentIdentifier.getParentPath());
			contentletMap.put(ESMappingConstants.PATH, contentIdentifier.getPath());
			// makes shorties searchable regardless of length
			contentletMap.put(ESMappingConstants.SHORT_ID, contentIdentifier.getId().replace("-", ""));
			contentletMap.put(ESMappingConstants.SHORT_INODE, contentlet.getInode().replace("-", ""));
			//add workflow to map
			contentletMap.putAll(getWorkflowInfoForContentlet(contentlet));

            if (UtilMethods.isSet(contentIdentifier.getSysPublishDate())) {
                contentletMap.put(ESMappingConstants.PUBLISH_DATE,
                        elasticSearchDateTimeFormat.format(contentIdentifier.getSysPublishDate()));
            } else {
                contentletMap.put(ESMappingConstants.PUBLISH_DATE,
                        elasticSearchDateTimeFormat.format(versionInfo.getVersionTs()));
            }

            if (UtilMethods.isSet(contentIdentifier.getSysExpireDate())) {
                contentletMap.put(ESMappingConstants.EXPIRE_DATE,
                        elasticSearchDateTimeFormat.format(contentIdentifier.getSysExpireDate()));
            } else {
                contentletMap.put(ESMappingConstants.EXPIRE_DATE,
                        elasticSearchDateTimeFormat.format(29990101000000L));
            }

			contentletMap.put(ESMappingConstants.VERSION_TS, elasticSearchDateTimeFormat.format(versionInfo.getVersionTs()));

            addTextFieldToMapIfNeeded(contentletMap, contentlet, contentIdentifier, versionInfo, contentType);
            String urlMap;
			try{
				urlMap = APILocator.getContentletAPI().getUrlMapForContentlet(contentlet, APILocator.getUserAPI().getSystemUser(), true);
				if(urlMap != null){
					contentletMap.put(ESMappingConstants.URL_MAP,urlMap );
				}
			} catch (final Exception e) {
				Logger.warn(this.getClass(), "Cannot get URLMap for Content Type: " + contentType.name() + " and " +
						"contentlet.id : " + ((contentIdentifier != null) ? contentIdentifier.getId() : contentlet) +
						" , reason: " + e.getMessage());
			}

			for(final Entry<String,Object> entry : contentletMap.entrySet()){
				final String lowerCaseKey = entry.getKey().toLowerCase();
				Object lowerCaseValue = entry.getValue();

				if (UtilMethods.isSet(lowerCaseValue) && (lowerCaseValue instanceof String || (
						//filters relationships
                        !(lowerCaseValue instanceof List) &&
						 !lowerCaseKey
								.endsWith(ESMappingConstants.TAGS)))) {

					if (lowerCaseValue instanceof String){
						lowerCaseValue = ((String) lowerCaseValue).toLowerCase();
					}

                    if (!lowerCaseKey.endsWith(TEXT)){
						//for example: when lowerCaseValue=moddate, moddate_dotraw must be created from its moddate_text if exists
						//when the moddate_text is evaluated.
						if (!contentletMap.containsKey(entry.getKey() + TEXT)){
							mlowered.put(lowerCaseKey + "_dotraw", lowerCaseValue);
						}
					}else{
						mlowered.put(lowerCaseKey.replace(TEXT, "_dotraw"), lowerCaseValue);
					}
				}

				mlowered.put(lowerCaseKey, lowerCaseValue);

				//exclude null values and relationships because they where appended on the loadRelationships method
				if(lowerCaseValue!=null && !(lowerCaseValue instanceof List)) {
					sw.append(lowerCaseValue.toString()).append(' ');
				}
			}

			final Optional<Field> binaryField = this.findFirstBinaryFieldIndexable(contentlet);
			if (binaryField.isPresent()) {

				this.generateBinaryMetadata(contentlet, sw, mlowered, binaryField.get());

				if (BaseContentType.FILEASSET.equals(contentlet.getContentType().baseType())) {
					// see if we have content metadata
					final File contentMeta = APILocator.getFileAssetAPI().getContentMetadataFile(contentlet.getInode());
					if (contentMeta.exists() && contentMeta.length() > 0) {
						final String contentData = APILocator.getFileAssetAPI().getContentMetadataAsString(contentMeta);
						mlowered.put(FileAssetAPI.META_DATA_FIELD.toLowerCase() + StringPool.PERIOD + "content", contentData);
					}
				}
			}
			//The url is now stored under the identifier for html pages, so we need to index that also.
			if (contentlet.getContentType().baseType().getType() == BaseContentType.HTMLPAGE.getType()) {
				mlowered.put(contentlet.getContentType().variable().toLowerCase() + ".url", contentIdentifier.getAssetName());
				mlowered.put(contentlet.getContentType().variable().toLowerCase() + ".url_dotraw", contentIdentifier.getAssetName());
				sw.append(contentIdentifier.getAssetName());
			}
			mlowered.put("catchall", sw.toString());

			return mlowered;
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when mapping properties of Contentlet with ID " +
                    "'%s': %s", contentlet.getIdentifier(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotMappingException(errorMsg, e);
		}
	}

    /**
     * Fields with suffix _text will be indexed if the property `CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS` is on
     * @param contentletMap
     * @param contentlet
     * @param contentIdentifier
     * @param versionInfo
     * @param contentType
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private void addTextFieldToMapIfNeeded(final Map<String, Object> contentletMap,
            final Contentlet contentlet, final Identifier contentIdentifier,
            final ContentletVersionInfo versionInfo, final ContentType contentType)
            throws DotSecurityException, DotDataException {
        if (Config.getBooleanProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false)) {
            contentletMap.put(ESMappingConstants.STRUCTURE_TYPE + TEXT,
                    Integer.toString(
                            contentType.baseType().getType())); // marked for DEPRECATION
            contentletMap.put(ESMappingConstants.BASE_TYPE + TEXT,
                    Integer.toString(contentType.baseType().getType()));
            contentletMap.put(ESMappingConstants.MOD_DATE + TEXT,
                    datetimeFormat.format(contentlet.getModDate()));
            contentletMap
                    .put(ESMappingConstants.LIVE + TEXT, Boolean.toString(contentlet.isLive()));
            contentletMap.put(ESMappingConstants.WORKING + TEXT,
                    Boolean.toString(contentlet.isWorking()));
            contentletMap
                    .put(ESMappingConstants.LOCKED + TEXT, Boolean.toString(contentlet.isLocked()));
            contentletMap.put(ESMappingConstants.DELETED + TEXT,
                    Boolean.toString(contentlet.isArchived()));
            contentletMap.put(ESMappingConstants.LANGUAGE_ID + TEXT,
                    Long.toString(contentlet.getLanguageId()));

            if (UtilMethods.isSet(contentIdentifier.getSysPublishDate())) {
                contentletMap.put(ESMappingConstants.PUBLISH_DATE + TEXT,
                        datetimeFormat.format(contentIdentifier.getSysPublishDate()));
            } else {
                contentletMap.put(ESMappingConstants.PUBLISH_DATE + TEXT,
                        datetimeFormat.format(versionInfo.getVersionTs()));
            }

            if (UtilMethods.isSet(contentIdentifier.getSysExpireDate())) {
                contentletMap.put(ESMappingConstants.EXPIRE_DATE + TEXT,
                        datetimeFormat.format(contentIdentifier.getSysExpireDate()));
            } else {
                contentletMap.put(ESMappingConstants.EXPIRE_DATE + TEXT, "29990101000000");
            }

            contentletMap.put(ESMappingConstants.VERSION_TS + TEXT,
                    datetimeFormat.format(versionInfo.getVersionTs()));
        }
    }

	private void generateBinaryMetadata(final Contentlet contentlet,
										final StringWriter stringWriter,
										final Map<String, Object> mapLowered,
										final Field field) throws Exception {

		//Verify if it is enabled the option to regenerate missing metadata files on reindex
		final boolean regenerateMissingMetadata = Config
				.getBooleanProperty("regenerate.missing.metadata.on.reindex", true);

		/*
		Verify if it is enabled the option to always regenerate metadata files on reindex,
		enabling this could affect greatly the performance of a reindex process.
		 */
		final boolean alwaysRegenerateMetadata = Config
				.getBooleanProperty("always.regenerate.metadata.on.reindex", false);

		Map<String, Object> metadataMap = null;

		if (contentlet.isLive() || contentlet.isWorking()) {

			final Optional<com.dotcms.contenttype.model.field.FieldVariable> customIndexMetaDataFieldsOpt =
					FactoryLocator.getFieldFactory().byFieldVariableKey(field.getIdentifier(), BinaryField.INDEX_METADATA_FIELDS);
			final TikaUtils tikaUtils = new TikaUtils();

			final Set<String> metadataFields = customIndexMetaDataFieldsOpt.isPresent()?
					new HashSet<>(Arrays.asList(customIndexMetaDataFieldsOpt.get().value().split(StringPool.COMMA))):
					tikaUtils.getConfiguredMetadataFields();  // gets the metadata fields to filter from the tika metadata

			final File binaryField = contentlet.getBinary(field.getVelocityVarName());
			if (null != binaryField && binaryField.exists() && binaryField.canRead()) {

				if (alwaysRegenerateMetadata) {
					metadataMap = tikaUtils.generateMetaDataForce(contentlet, binaryField, field.getVelocityVarName(), metadataFields);
				} else if (regenerateMissingMetadata) {
					metadataMap = tikaUtils.generateMetaData(contentlet, binaryField, field.getVelocityVarName(), metadataFields);
				}
			}

			// see if we have content metadata
			if(null != metadataMap) {

				for (final String metadataKey : metadataFields) {

					final Object metadataValue = metadataMap.get(metadataKey);
					if (null != metadataValue) {
						mapLowered.put(FileAssetAPI.META_DATA_FIELD.toLowerCase() + StringPool.PERIOD + metadataKey.toLowerCase(), metadataValue);
						if (metadataKey.contains(FileAssetAPI.CONTENT_FIELD)) {
							stringWriter.append(metadataValue.toString()).append(' ');
						}
					}
				}
			}
		}
	}

	private Optional<Field> findFirstBinaryFieldIndexable(final Contentlet contentlet) {

		final List<Field> fields  = new ArrayList<>(
				FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode()));

		return fields.stream().filter(Field::isIndexed)
				.filter(field -> field.getFieldType().equals(Field.FieldType.BINARY.toString()))
				.findFirst();
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
                if (Config.getBooleanProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false)) {
                    workflowMap.put(ESMappingConstants.WORKFLOW_MOD_DATE + TEXT,
                            datetimeFormat.format(task.getModDate()));
                }
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
                    final String entryStep = scheme.entryStep();
                    if (entryStep != null) {
                        schemeWriter.add(scheme.getId());
                        stepIds.add(entryStep);
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

	    List<Category> cats = APILocator.getCategoryAPI().getParents(con, APILocator.systemUser(), false);

        List<String> catsVarNames = cats.stream().map(Category::getCategoryVelocityVarName).map(
                String::toLowerCase).collect(Collectors.toList());

        m.put(ESMappingConstants.CATEGORIES, catsVarNames);
        

	    for(final com.dotcms.contenttype.model.field.Field f : catFields){
	        // I don't think we care if we put all the categories in each field
            m.put(type.variable() + "." + f.variable(), catsVarNames);
	    
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
		m.put(ESMappingConstants.OWNER_CAN_WRITE, ownerCanWrite);
		m.put(ESMappingConstants.OWNER_CAN_PUBLISH, ownerCanPub);

        if (Config.getBooleanProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false)) {
            m.put(ESMappingConstants.OWNER_CAN_READ + TEXT, Boolean.toString(ownerCanRead));
            m.put(ESMappingConstants.OWNER_CAN_WRITE + TEXT, Boolean.toString(ownerCanWrite));
            m.put(ESMappingConstants.OWNER_CAN_PUBLISH + TEXT, Boolean.toString(ownerCanPub));
        }
	}

	public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMdd");
	public static final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");

	public static final String elasticSearchDateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss";
	public static final FastDateFormat elasticSearchDateTimeFormat = FastDateFormat.getInstance(elasticSearchDateTimeFormatPattern);

	public static final FastDateFormat timeFormat = FastDateFormat.getInstance("HH:mm:ss");

	protected void loadFields(final Contentlet contentlet, final Map<String, Object> contentletMap) throws DotDataException {

		// https://github.com/dotCMS/dotCMS/issues/6152
		final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		final DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000", otherSymbols);
		final FieldAPI fieldAPI   = APILocator.getFieldAPI();
		final List<Field> fields  = new ArrayList<>(
				FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode()));
		final Structure structure = contentlet.getStructure();
		StringBuilder keyNameBuilder;
		String keyName;
		String keyNameText;
		final TikaUtils tikaUtils = new TikaUtils();

		for (final Field field : fields) {

			keyNameBuilder = new StringBuilder(structure.getVelocityVarName()).append(".")
					.append(field.getVelocityVarName());
			keyName        = keyNameBuilder.toString();
			keyNameText    = keyNameBuilder.append(TEXT).toString();
			
			
			if (field.getFieldType().equals(Field.FieldType.BINARY.toString()) && field.isIndexed()){
			    String fileName = Try.of(()-> contentlet.getBinary(field.getVelocityVarName()).getName()).getOrElse("_unk");
                contentletMap.put(keyName, fileName);
                contentletMap.put(keyNameText, fileName);
                continue;
            }
			
			if (field.getFieldType().equals(Field.FieldType.BINARY.toString())
					|| field.getFieldContentlet() != null && (field.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SYSTEM_FIELD)
					&& !field.getFieldType().equals(Field.FieldType.TAG.toString()))) {

				continue;
			}

			if(!field.isIndexed()) {

				continue;
			}

			try {
				if(fieldAPI.isElementConstant(field)){
					contentletMap.put(keyName, (field.getValues() == null ? "":field.getValues()));
					continue;
				}

				Object valueObj = contentlet.get(field.getVelocityVarName());

				if (field.getFieldContentlet().startsWith(ESMappingConstants.FIELD_TYPE_SECTION_DIVIDER)) {
					valueObj = "";
				}

				if (!UtilMethods.isSet(valueObj) && !field.getFieldType()
						.equals(Field.FieldType.TAG.toString())) {
					contentletMap.put(keyName, null);
				}
				else if(field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_TIME)) {
					try{
						String timeStr=timeFormat.format(valueObj);
						contentletMap.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						contentletMap.put(keyNameText, timeStr);
					}
					catch(Exception e){
						contentletMap.put(keyName, null);
						contentletMap.put(keyNameText, null);
					}
				}
				else if (field.getFieldType().equals(ESMappingConstants.FIELD_ELASTIC_TYPE_DATE)) {
					try {
						String dateString = dateFormat.format(valueObj);
						contentletMap.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						contentletMap.put(keyNameText, dateString);
					}
					catch(Exception ex) {
						contentletMap.put(keyName, null);
						contentletMap.put(keyNameText, null);
					}
				} else if(field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_DATE_TIME)) {
					try {
						String datetimeString = datetimeFormat.format(valueObj);
						contentletMap.put(keyName, elasticSearchDateTimeFormat.format(valueObj));
						contentletMap.put(keyNameText, datetimeString);
					}
					catch(Exception ex) {
						contentletMap.put(keyName, null);
						contentletMap.put(keyNameText, null);
					}
				} else if (field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CATEGORY)) {
					// moved the logic to loadCategories
				} else if (field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_RELATIONSHIP)) {
                    // loadRelationshipFields processes relationship fields
                    continue;
                } else if (field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_CHECKBOX) || field
						.getFieldType().equals(ESMappingConstants.FIELD_TYPE_MULTI_SELECT)) {
					if (field.getFieldContentlet().startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						contentletMap.put(keyName, valueObj);
						contentletMap.put(keyNameText, valueObj.toString());
					} else {
						contentletMap.put(keyName,
								UtilMethods.listToString(valueObj.toString()));
					}
				} else if (field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_KEY_VALUE)){
					final boolean fileMetadata =
							field.getVelocityVarName().equals(FileAssetAPI.META_DATA_FIELD)
									&& structure.getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET;
					if(LicenseUtil.getLevel()>= LicenseLevel.STANDARD.level) {

						this.loadKeyValueField(contentletMap, keyName, tikaUtils, field, (String) valueObj, fileMetadata);
					}
				} else if(field.getFieldType().equals(Field.FieldType.TAG.toString())) {

					if (this.loadTagsField(contentlet, contentletMap, structure, keyName)) {

						continue;
					}
				} else if(field.getFieldType().equals(CUSTOM_FIELD.legacyValue())
						&& field.getVelocityVarName().equals(PERSONA_KEY_TAG_FIELD_VAR)) {
					contentletMap.put(PERSONA_KEY_TAG,valueObj.toString());
					contentletMap.put(keyName, valueObj.toString());
				} else {
					if (field.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_BOOLEAN)) {
						contentletMap.put(keyName, valueObj);
						contentletMap.put(keyNameText,valueObj.toString());
					} else if (field.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_FLOAT) || field
							.getFieldContentlet()
							.startsWith(ESMappingConstants.FIELD_ELASTIC_TYPE_INTEGER)) {
						contentletMap.put(keyName, valueObj);
						contentletMap.put(keyNameText, numFormatter.format(valueObj));
					} else {
						contentletMap.put(keyName, valueObj);
						contentletMap.put(keyNameText, valueObj.toString());
					}
				}

				// Store sha256 hash for unique fields in the index
				if (field.isUnique() && contentletMap.containsKey(keyName)) {
					final Object uniqueValue = contentletMap.get(keyName);
					contentletMap.put(keyName + ESUtils.SHA_256,
							ESUtils.sha256(keyName, uniqueValue, contentlet.getLanguageId()));
				}
			} catch (Exception e) {
				Logger.warn(ESMappingAPIImpl.class, "Error indexing field: " + field.getFieldName()
						+ " of contentlet: " + contentlet.getInode(), e);
				throw new DotDataException(e.getMessage(),e);
			}
		}
	}

	private boolean loadTagsField(final Contentlet contentlet,
								  final Map<String, Object> contentletMap,
								  final Structure structure,
								  final String keyName) throws DotDataException {

		final List<Tag> tagList = APILocator.getTagAPI().getTagsByInode(contentlet.getInode());
		if(tagList ==null || tagList.size()==0) {
			return true;
		}

		final List<String> tagsNames = tagList.stream().map(Tag::getTagName).collect(
				Collectors.toList());

		contentletMap.put(keyName, tagsNames);
		contentletMap.put(ESMappingConstants.TAGS, tagsNames);

		if ( Structure.STRUCTURE_TYPE_PERSONA != contentlet.getStructure().getStructureType() ) {
			final List<String> personaTagsNames = tagList.stream()
					.filter(Tag::isPersona)
					.map(Tag::getTagName)
					.collect(Collectors.toList());

			contentletMap.put(structure.getVelocityVarName() + "."
					+ ESMappingConstants.PERSONAS, personaTagsNames);
			contentletMap.put(ESMappingConstants.PERSONAS, personaTagsNames);
		}

		return false;
	}

	private void loadKeyValueField(final Map<String, Object> contentletMap,
								   final String keyName,
								   final TikaUtils tikaUtils,
								   final Field field,
								   final String valueObj,
								   final boolean fileMetadata) throws DotDataException, DotSecurityException {

		final Map<String,Object> keyValueMap = KeyValueFieldUtil.JSONValueToHashMap(valueObj);
		Set<String> allowedFields = new HashSet<>();

		if(fileMetadata) {
			// http://jira.dotmarketing.net/browse/DOTCMS-7243
			final List<FieldVariable> fieldVariables = APILocator.getFieldAPI().getFieldVariablesForField(
					field.getInode(), APILocator.getUserAPI().getSystemUser(), false);
			for(final FieldVariable fieldVariable : fieldVariables) {
				if(fieldVariable.getKey().equals(ESMappingConstants.DOT_INDEX_PATTERN)) {

					final String[] names = fieldVariable.getValue().split(",");
					allowedFields        = new HashSet<>();
					for(final String name : names) {
						allowedFields.add(name.trim().toLowerCase());
					}
				}
			}

			allowedFields
					.addAll(tikaUtils.getConfiguredMetadataFields());

			tikaUtils.filterMetadataFields(keyValueMap, allowedFields);

            final String keyValuePrefix = FileAssetAPI.META_DATA_FIELD.toLowerCase();
            keyValueMap.forEach((k, v) -> contentletMap.put(keyValuePrefix + StringPool.PERIOD + k, v));
		} else {
			keyValueMap.forEach((k, v) -> {
				((List)contentletMap.computeIfAbsent(keyName, key -> new ArrayList<>())).add(
						ImmutableMap.of( "key", k, "value", v));
			});
		}
	}

	public String toJsonString(Map<String, Object> map) throws IOException{
		return mapper.writeValueAsString(map);
	}

	@CloseDBIfOpened
	public List<String> dependenciesLeftToReindex(final Contentlet contentlet) throws DotStateException, DotDataException, DotSecurityException {
		final List<String> dependenciesToReindex = new ArrayList<>();

		final ContentletAPI conAPI=APILocator.getContentletAPI();

		final String relatedSQL = "select tree.* from tree where child = ? order by tree_order";
		final DotConnect db = new DotConnect();
		db.setSQL(relatedSQL);
		db.addParam(contentlet.getIdentifier());

		final List<HashMap<String, String>> relatedContentlets = db.loadResults();

		if(relatedContentlets.size()>0) {

			final List<Relationship> relationships = FactoryLocator.getRelationshipFactory()
					.byContentType(contentlet.getContentType());

			for(final Relationship relationship : relationships) {

				final List<Contentlet> oldDocs;
				final List<String> oldRelatedIds = new ArrayList<>();
				final List<String> newRelatedIds = new ArrayList<>();

                oldDocs = conAPI.getRelatedContent(contentlet, relationship,
                        APILocator.getUserAPI().getSystemUser(), false);

                if(oldDocs.size() > 0) {
					for(Contentlet oldDoc : oldDocs) {
						oldRelatedIds.add(oldDoc.getIdentifier());
					}
				}

				relatedContentlets.stream().filter(map -> map.get(ESMappingConstants.RELATION_TYPE)
						.equals(relationship.getRelationTypeValue())).forEach(
						entry -> replaceExistingRelatedContent(entry, contentlet,
								oldRelatedIds, newRelatedIds));

				//Taking the disjunction of both collections will give the old list of dependencies that need to be removed from the
				//re-indexation and the list of new dependencies no re-indexed yet
				dependenciesToReindex.addAll(
						CollectionUtils.disjunction(oldRelatedIds, newRelatedIds));
			}
		}
		return dependenciesToReindex;
	}

	/**
	 *
	 * @param relatedEntry
	 * @param con
	 * @param oldRelatedIds
	 * @param newRelatedIds
	 */
	private void replaceExistingRelatedContent(final Map<String, String> relatedEntry,
			final Contentlet con, final List<String> oldRelatedIds,
			final List<String> newRelatedIds) {

		final String childId = relatedEntry.get(ESMappingConstants.CHILD);
		final String parentId = relatedEntry.get(ESMappingConstants.PARENT);
		if (con.getIdentifier().equalsIgnoreCase(childId)) {
			newRelatedIds.add(parentId);
			oldRelatedIds.remove(parentId);
		} else {
			newRelatedIds.add(childId);
			oldRelatedIds.remove(childId);
		}
	}

    /**
     * @deprecated Use {@link ESMappingAPIImpl#loadRelationshipFields(Contentlet, Map, StringWriter)} instead
     * @param contentlet
     * @param esMap
     * @throws DotStateException
     * @throws DotDataException
     */
	@Deprecated
    protected void loadRelationshipFields(final Contentlet contentlet,
            final Map<String, Object> esMap) throws DotStateException, DotDataException {
        loadRelationshipFields(contentlet, esMap, new StringWriter());
    }

    /**
     *
     * @param contentlet Contentlet to be mapped
     * @param esMap Map with fields to be indexed
     * @param catchallWriter StringWriter to save related content identifiers in the catchall field
     * @throws DotStateException
     * @throws DotDataException
     */
    protected void loadRelationshipFields(final Contentlet contentlet,
            final Map<String, Object> esMap, final StringWriter catchallWriter)
            throws DotStateException, DotDataException {

        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        final DotConnect db = new DotConnect();
        db.setSQL(
                "select child, relation_type from tree where parent = ? and relation_type !='child' order by tree_order asc");
        db.addParam(contentlet.getIdentifier());

        for (Map<String, Object> relatedEntry : db.loadObjectResults()) {

            final String childId = relatedEntry.get(ESMappingConstants.CHILD).toString();
            final String relType = relatedEntry.get(ESMappingConstants.RELATION_TYPE).toString();
            final Relationship relationship = relationshipAPI.byTypeValue(relType);

            if (relationship != null && InodeUtils.isSet(relationship.getInode())) {
                List.class.cast(esMap
                        .computeIfAbsent(relType,
                                k -> new ArrayList<>()))
                        .add(childId);

                //add related content to catchall
                catchallWriter.append(childId).append(' ');
            }
        }
    }

}
