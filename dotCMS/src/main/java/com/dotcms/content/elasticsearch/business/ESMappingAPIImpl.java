package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CREATION_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.PERSONA_KEY_TAG;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.SYS_PUBLISH_USER;
import static com.dotcms.contenttype.model.field.LegacyFieldTypes.CUSTOM_FIELD;
import static com.dotcms.contenttype.model.type.PersonaContentType.PERSONA_KEY_TAG_FIELD_VAR;
import static com.dotcms.util.DotPreconditions.checkNotEmpty;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.COMMA;
import static com.liferay.util.StringPool.PERIOD;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.business.ContentMappingAPI;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotCorruptedDataException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
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
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;
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
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class ESMappingAPIImpl implements ContentMappingAPI {

    //This property basically tells the Metadata-API whether or not we should generate metadata upon reindexing a piece of content
	public static final String WRITE_METADATA_ON_REINDEX = "write.metadata.on.reindex";

	//If you want to skip indexing metadata dotraw fields set this prop to false
	public static final String INDEX_DOTRAW_METADATA_FIELDS = "index.dotraw.metadata.fields";

	//If you want to override and specify a set of particular fields to be included in the dotRaw generation it can be accomplished through this prop.
	public static final String INCLUDE_DOTRAW_METADATA_FIELDS = "include.dotraw.metadata.fields";

    //These are the fields included by default to be used as  metadata.fieldname_dotraw
	static final String[] defaultIncludedDotRawMetadataFields = {
			"author",
			"contenttype",
			"filesize",
			"height",
			"length",
			"moddate",
			"name",
			"path",
			"title",
			"width"
	};

	// if you want to limit the size of the field `metadata.content`
	// it can be accomplished by setting this property to the number of chars desired
	// by default it'll attempt to include the whole thing returned by the FileMetadataAPI
	public static final String INDEX_METADATA_CONTENT_LENGTH = "index.metadata.content.length";

	public static final String TEXT = "_text";
	public static final String DOTRAW = "_dotraw";
	public static final String NO_METADATA = "NO_METADATA";

	static ObjectMapper mapper = null;

	private UserAPI userAPI;
	private FolderAPI folderAPI;
    private IdentifierAPI identifierAPI;
	private VersionableAPI versionableAPI;
	private PermissionAPI permissionAPI;
	private ContentletAPI contentletAPI;
	private FileMetadataAPI fileMetadataAPI;
	private HostAPI hostAPI;
	private FieldAPI fieldAPI;
	private ESIndexAPI esIndexAPI;
	private RelationshipAPI relationshipAPI;
	private TagAPI tagAPI;
	private CategoryAPI categoryAPI;
	private RoleAPI roleAPI;
	//These two are set as suppliers cuz their instantiation during initialization require of a company to exist.
    //By doing this they will get instantiated once the company users roles haven been settled. After the starter is loaded.
	private Supplier<ContentTypeAPI> contentTypeAPI;
	private Supplier<WorkflowAPI> workflowAPI;

	@VisibleForTesting
	public ESMappingAPIImpl(
			final UserAPI userAPI,
			final FolderAPI folderAPI,
			final IdentifierAPI identifierAPI,
			final VersionableAPI versionableAPI,
			final PermissionAPI permissionAPI,
			final ContentletAPI contentletAPI,
			final FileMetadataAPI fileMetadataAPI,
			final HostAPI hostAPI,
			final FieldAPI fieldAPI,
			final ESIndexAPI esIndexAPI,
			final RelationshipAPI relationshipAPI,
			final TagAPI tagAPI,
			final CategoryAPI categoryAPI,
			final RoleAPI roleAPI,
			final Supplier<ContentTypeAPI> contentTypeAPI,
			final Supplier<WorkflowAPI> workflowAPI) {
		this.userAPI = userAPI;
		this.folderAPI = folderAPI;
		this.identifierAPI = identifierAPI;
		this.versionableAPI = versionableAPI;
		this.permissionAPI = permissionAPI;
		this.contentletAPI = contentletAPI;
		this.fileMetadataAPI = fileMetadataAPI;
		this.hostAPI = hostAPI;
		this.fieldAPI = fieldAPI;
		this.esIndexAPI = esIndexAPI;
		this.relationshipAPI = relationshipAPI;
		this.tagAPI = tagAPI;
		this.categoryAPI = categoryAPI;
		this.roleAPI = roleAPI;
		this.contentTypeAPI = contentTypeAPI;
		this.workflowAPI = workflowAPI;

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

	public ESMappingAPIImpl() {
		this(APILocator.getUserAPI(), APILocator.getFolderAPI(),
			APILocator.getIdentifierAPI(), APILocator.getVersionableAPI(),
			APILocator.getPermissionAPI(), APILocator.getContentletAPI(),
			APILocator.getFileMetadataAPI(), APILocator.getHostAPI(),
			APILocator.getFieldAPI(), APILocator.getESIndexAPI(),
			APILocator.getRelationshipAPI(), APILocator.getTagAPI(),
			APILocator.getCategoryAPI(), APILocator.getRoleAPI(),
			//Use memoized Suppliers to avoid re-instantiating the API on every call
			Lazy.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())),
			Lazy.of(APILocator::getWorkflowAPI));
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
                indexes.stream().map(indexName -> esIndexAPI
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
			final User systemUser = userAPI.getSystemUser();
			final Map<String,Object> contentletMap = new HashMap<>();
			final Map<String,Object> mapLowered	   = new HashMap<>();
			loadCategories(contentlet, contentletMap);
			loadFields(contentlet, contentletMap);
			loadPermissions(contentlet, contentletMap);
            fillCategoryPermissions(contentlet, contentletMap);
            loadRelationshipFields(contentlet, contentletMap, sw);


			final Identifier contentIdentifier = identifierAPI.find(contentlet);
            if (null == contentIdentifier || !UtilMethods.isSet(contentIdentifier.getId())) {
                final String errorMsg = String.format("Identifier '%s' was not found via API.", contentlet
                        .getIdentifier());
                throw new DotDataException(errorMsg);
            }
			final Optional<ContentletVersionInfo> versionInfo = versionableAPI.getContentletVersionInfo(
					contentIdentifier.getId(),
					contentlet.getLanguageId(),
					contentlet.getVariantId());
            if (versionInfo.isEmpty()) {
                final String errorMsg = String.format("Version Info for Identifier '%s' and Language '%s' was not" +
                        " found via API.", contentIdentifier.getId(), contentlet.getLanguageId());
                throw new DotDataException(errorMsg);
            }
			final ContentType contentType = contentTypeAPI.get().find(contentlet.getContentTypeId());
            if (null == contentType || !UtilMethods.isSet(contentType.id())) {
                final String errorMsg = String.format("Content Type with ID '%s' was not found via API.",
                        contentlet.getContentTypeId());
                throw new DotDataException(errorMsg);
            }
            final Host contentSite = hostAPI.find(contentIdentifier.getHostId(), systemUser, DONT_RESPECT_FRONTEND_ROLES);
            if (null == contentSite || !UtilMethods.isSet(contentSite.getIdentifier())) {
                final String errorMsg = String.format("Identifier '%s' is pointing to a Site that is not valid: '%s'." +
                                " Please manually change this record to point to a valid Site, or delete it altogether.",
                        contentIdentifier.getId(), contentIdentifier.getHostId());
                throw new DotDataException(errorMsg);
            }
			final Folder contentFolder = folderAPI.findFolderByPath(contentIdentifier.getParentPath(), contentIdentifier.getHostId(), systemUser, DONT_RESPECT_FRONTEND_ROLES);
            if (null == contentFolder || !UtilMethods.isSet(contentFolder.getIdentifier())) {
                final String errorMsg = String.format("Parent folder '%s' in Site '%s' was not found via API. Please " +
                        "check that the specified value points to a valid folder.", contentIdentifier.getParentPath()
                        , contentIdentifier.getHostId());
                throw new DotDataException(errorMsg);
            }

			contentletMap.put(ESMappingConstants.TITLE, contentlet.getTitle());
			contentletMap.put(ESMappingConstants.SYSTEM_TYPE, contentType.system());
			contentletMap.put(ESMappingConstants.STRUCTURE_NAME, contentType.variable()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.CONTENT_TYPE, contentType.variable());
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE, contentType.baseType().getType()); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.STRUCTURE_TYPE + TEXT, Integer.toString(contentType.baseType().getType())); // marked for DEPRECATION
			contentletMap.put(ESMappingConstants.BASE_TYPE, contentType.baseType().getType());
			contentletMap.put(ESMappingConstants.BASE_TYPE + TEXT, Integer.toString(contentType.baseType().getType()));
			contentletMap.put(ESMappingConstants.TYPE, ESMappingConstants.CONTENT);
			contentletMap.put(ESMappingConstants.INODE, contentlet.getInode());
			contentletMap.put(ESMappingConstants.MOD_DATE, elasticSearchDateTimeFormat.format(contentlet.getModDate()));
			contentletMap.put(ESMappingConstants.MOD_DATE + TEXT, datetimeFormat.format(contentlet.getModDate()));
			contentletMap.put(ESMappingConstants.OWNER, contentlet.getOwner()==null ? "0" : contentlet.getOwner());
			contentletMap.put(ESMappingConstants.MOD_USER, contentlet.getModUser());
			contentletMap.put(CREATION_DATE, elasticSearchDateTimeFormat.format(contentIdentifier.getCreateDate()));
			contentletMap.put(CREATION_DATE + TEXT, datetimeFormat.format(contentIdentifier.getCreateDate()));
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
			contentletMap.put(ESMappingConstants.VARIANT, contentlet.getVariantId());
			contentletMap.put(ESMappingConstants.IDENTIFIER, contentIdentifier.getId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOST, contentIdentifier.getHostId());
			contentletMap.put(ESMappingConstants.CONTENTLET_HOSTNAME, contentSite.getHostname());
			contentletMap.put(ESMappingConstants.CONTENTLET_FOLDER, InodeUtils.isSet(contentFolder.getInode()) ? contentFolder.getInode() : contentlet.getFolder());
			contentletMap.put(ESMappingConstants.PARENT_PATH, contentIdentifier.getParentPath());
			contentletMap.put(ESMappingConstants.PATH, contentIdentifier.getPath());
			// makes shorties searchable regardless of length
			contentletMap.put(ESMappingConstants.SHORT_ID, contentIdentifier.getId().replace("-", ""));
			contentletMap.put(ESMappingConstants.SHORT_INODE, contentlet.getInode().replace("-", ""));
			//add workflow to map
			contentletMap.putAll(getWorkflowInfoForContentlet(contentlet));


			final String publishDateVar = contentType.publishDateVar();
			final Date publishDate = UtilMethods.isSet(publishDateVar) ?
					contentlet.getDateProperty(publishDateVar) : null;
			loadDateTimeFieldValue(contentletMap,
					ESMappingConstants.PUBLISH_DATE, publishDate);

			final String expireDateVar = contentType.expireDateVar();
			final Date expireDate = UtilMethods.isSet(expireDateVar) ?
					contentlet.getDateProperty(expireDateVar) : null;
			loadDateTimeFieldValue(contentletMap,
					ESMappingConstants.EXPIRE_DATE, expireDate);
			if (contentlet.isLive()) {
				contentletMap.put(SYS_PUBLISH_USER, contentlet.getModUser());
			}
			loadDateTimeFieldValue(contentletMap,
					ESMappingConstants.SYS_PUBLISH_DATE, versionInfo.get().getPublishDate());

			contentletMap.put(ESMappingConstants.VERSION_TS, elasticSearchDateTimeFormat.format(versionInfo.get().getVersionTs()));
			contentletMap.put(ESMappingConstants.VERSION_TS + TEXT, datetimeFormat.format(versionInfo.get().getVersionTs()));

			try{
				final String urlMap = contentletAPI.getUrlMapForContentlet(contentlet, systemUser, RESPECT_FRONTEND_ROLES);
				if(urlMap != null){
					contentletMap.put(ESMappingConstants.URL_MAP,urlMap );
				}
			} catch (final Exception e) {
				Logger.warn(this.getClass(), "Cannot get URLMap for Content Type: " + contentType.name() + " and " +
						"contentlet.id : " + contentIdentifier.getId() +
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
							mapLowered.put(lowerCaseKey + DOTRAW, lowerCaseValue);
						}
					}else{
                        mapLowered.put(lowerCaseKey.replace(TEXT, DOTRAW), lowerCaseValue);
					}
				}

				//exclude null values and relationships because they where appended on the loadRelationships method
				if(lowerCaseValue!=null && !(lowerCaseValue instanceof List)) {
					sw.append(lowerCaseValue.toString()).append(' ');
				}

                if (lowerCaseKey.endsWith(TEXT) && !Config
                        .getBooleanProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false)) {
                    continue;
                }

                mapLowered.put(lowerCaseKey, lowerCaseValue);
			}

			//Write Metadata
			writeMetadata(contentlet, sw, mapLowered);
			//Populate any KeyValue named metadata with the written metadata. This couldn't have been done earlier since the metadata just got written
			loadMetadataKeyValueFieldIfAny(contentlet, mapLowered);

			//The url is now stored under the identifier for html pages, so we need to index that also.
			if (contentlet.getContentType().baseType().getType() == BaseContentType.HTMLPAGE.getType()) {
				checkNotEmpty(contentlet.getContentType().variable(),
						DotCorruptedDataException.class, "Contentlet '%s' " +
								"points to a Content Type with an empty Velocity Var Name", contentlet.getIdentifier());
				mapLowered.put(contentlet.getContentType().variable().toLowerCase() + ".url", contentIdentifier.getAssetName());
				mapLowered.put(contentlet.getContentType().variable().toLowerCase() + ".url_dotraw", contentIdentifier.getAssetName());
				sw.append(contentIdentifier.getAssetName());
			}
			mapLowered.put("catchall", sw.toString());

			return mapLowered;
		} catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when mapping properties of Contentlet with ID " +
					//Do not remove the double ':' its expected by a test
                    "'%s':: %s", contentlet.getIdentifier(), e.getMessage());
            Logger.error(this, errorMsg, e);
            throw new DotMappingException(errorMsg, e);
		}
	}


    /**
     * Populates a map with the required roles and permissions associated with the category fields of a given
     * contentlet. If no categories are found, it adds a default "none" permission.
     *
     * @param contentlet The contentlet object containing the category fields to be analyzed and their corresponding
     *                   permissions.
     * @param mapLowered The map where the category-related permissions will be stored, under a specific key.
     */
    void fillCategoryPermissions(final Contentlet contentlet, final Map<String, Object> mapLowered) {
        if (!Config.getBooleanProperty("PERMISSION_SECONDARY_CATEGORY_CHECK", true)) {
            return;
        }
        List<String> requiredRoles = new ArrayList<>();
        requiredRoles.add(ESMappingConstants.MAPPED_PERMISSIONS.cms_admin_role.name());

        // List of fields which have secondaryPermissionCheck=true
        List<com.dotcms.contenttype.model.field.Field> permissionedCategories = contentlet
                .getContentType()
                .fields(CategoryField.class)
                .stream()
                .filter(f -> f.fieldVariables()
                        .stream()
                        .anyMatch(
                                fv -> "secondaryPermissionCheck".equalsIgnoreCase(fv.key()) && "true".equalsIgnoreCase(
                                        fv.value())))
                .collect(Collectors.toList());

        boolean hasCats = false;

        for (com.dotcms.contenttype.model.field.Field field : permissionedCategories) {
            List<Category> myCats = Try.of(
                            () -> (List<Category>) APILocator.getContentletAPI()
                                    .getFieldValue(contentlet, field, APILocator.systemUser(), false))
                    .getOrElse(List.of());
            if (!myCats.isEmpty()) {
                hasCats = true;
            }
            Set<String> permissions = new HashSet<>();
            myCats.forEach(cat -> {
                Try.run(() ->
                {
                    APILocator.getPermissionAPI()
                            .getPermissions(cat)
                            .forEach(
                                    p -> permissions.add(p.getRoleId())
                            );

                }).onFailure(e -> Logger.error(this, "Error getting permissions for category " + cat.getInode(), e));
            });

            requiredRoles.addAll(permissions);
        }
        if (!hasCats) {
            requiredRoles.add(ESMappingConstants.MAPPED_PERMISSIONS.none.name());
        }

        mapLowered.put(ESMappingConstants.CATEGORY_PERMISSIONS, requiredRoles.toArray(new String[0]));


    }




	/**
	 * Metadata generation happens here
	 * @param contentlet
	 * @param stringWriter
	 * @param mapLowered
	 * @throws IOException
	 * @throws DotDataException
	 */
	private void writeMetadata(final Contentlet contentlet, final StringWriter stringWriter,
			final Map<String, Object> mapLowered) throws IOException, DotDataException {
		if (isWriteMetadataOnReindex()) {

			final ContentletMetadata metadata = fileMetadataAPI
					.generateContentletMetadata(contentlet);
			final Map<String, Metadata> fullMetadataMap = metadata
					.getFullMetadataMap();

			//Full metadata map is expected to have one single entry with everything
			fullMetadataMap.forEach((field, metadataValues) -> {
				if (null != metadataValues) {

                    final Set<String> dotRawInclude = getDotRawMetadataFields();

                    metadataValues.getFieldsMeta().forEach((metadataKey, metadataValue) -> {

						final String contentData =
								metadataValue != null ? metadataValue.toString() : BLANK;
						final String compositeKey =
								FileAssetAPI.META_DATA_FIELD.toLowerCase() + PERIOD + metadataKey
										.toLowerCase();
						final Object value = preProcessMetadataValue(compositeKey, metadataValue);
						mapLowered.put(compositeKey, value);

						if (Config.getBooleanProperty(INDEX_DOTRAW_METADATA_FIELDS, true)
								&& dotRawInclude.contains(metadataKey.toLowerCase())) {
							mapLowered.put(compositeKey + DOTRAW, value);
						}

						if (metadataKey.contains(FileAssetAPI.CONTENT_FIELD)) {
							stringWriter.append(contentData).append(' ');
						}

					});
				}
			});

		}
	}

    /**
     * System flag that tells if metadata must be written to the index
     * @return Bool flag
     */
    public static boolean isWriteMetadataOnReindex() {
        return Config.getBooleanProperty(WRITE_METADATA_ON_REINDEX, true);
    }

    /**
     * Return a set with the properties that will be included on the index
     * @return Set of fields
     */
    public static Set<String> getDotRawMetadataFields() {
        return Arrays.stream(Config.getStringArrayProperty(
                INCLUDE_DOTRAW_METADATA_FIELDS,
                defaultIncludedDotRawMetadataFields)).map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
	 * This method takes care of populating any KeyValue Field named metadata that might exist on the ContentType definition
	 * it must be called only once the metadata has been written @see {@link ESMappingAPIImpl#writeMetadata(Contentlet, StringWriter, Map)}
	 * @param contentlet
	 * @param mapLowered
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void loadMetadataKeyValueFieldIfAny(final Contentlet contentlet, final Map<String,Object> mapLowered)
			throws DotDataException, DotSecurityException {
		final Optional<com.dotcms.contenttype.model.field.Field> metadataKeyValueField = contentlet.getContentType()
				.fields(KeyValueField.class).stream()
				.filter(field -> FileAssetAPI.META_DATA_FIELD.equals(field.variable())).findFirst();
        if(metadataKeyValueField.isPresent()  ){

			final com.dotcms.contenttype.model.field.Field field = metadataKeyValueField.get();

			final Map<String, Object> readOnlyMetadata = (Map<String, Object>) contentlet.get(FileAssetAPI.META_DATA_FIELD);
            if(readOnlyMetadata.isEmpty()){
            	Logger.debug(ESMappingAPIImpl.class,String.format("No pre-calculated metadata available to populate keyValue field on contentlet with id `%s`.",contentlet.getIdentifier()));
            	return;
			}

			final String keyName = (contentlet.getContentType().variable() + PERIOD + field.variable()).toLowerCase();

			final Map<String, Object> metadata = new HashMap<>(readOnlyMetadata);

			Set<String> allowedFields = new HashSet<>();
			// http://jira.dotmarketing.net/browse/DOTCMS-7243
			final List<FieldVariable> fieldVariables = fieldAPI.getFieldVariablesForField(
					field.inode(), APILocator.systemUser(), false);
			for(final FieldVariable fieldVariable : fieldVariables) {
				if(fieldVariable.getKey().equals(ESMappingConstants.DOT_INDEX_PATTERN)) {

					final String[] names = fieldVariable.getValue().split(COMMA);
					allowedFields        = new HashSet<>();
					for(final String name : names) {
						allowedFields.add(name.trim().toLowerCase());
					}
				}
			}

			allowedFields.addAll(fileMetadataAPI.getConfiguredMetadataFields());

			fileMetadataAPI.filterMetadataFields(metadata, allowedFields);

			metadata.forEach((k, v) -> {
				((List)mapLowered.computeIfAbsent(keyName, key -> new ArrayList<>())).add(
						ImmutableMap.of( "key", k.toLowerCase(), "value", String.valueOf(v),  "key_value", (k + "_" + v).toLowerCase())
				);
			});

		}
	}

	/**
	 * logic extract to pre-process and ensure proper value added to the index
	 * @param compositeKey
	 * @param value
	 * @return
	 */
	private Object preProcessMetadataValue(final String compositeKey, final Object value) {
        if ("metadata.content".equals(compositeKey)) {
            if (null == value || (value instanceof String && isNotSet((String)value))) {
                //This "NO_METADATA" constant is getting relocated from tika utils
                return NO_METADATA;
            }
            //This is not a dupe of META_DATA_MAX_SIZE since that one is the flag used to set the number of bytes read by tika
            //This one allows me to set a max number of chars on the content field itself
            final int length = Config.getIntProperty(INDEX_METADATA_CONTENT_LENGTH, 0);
            final String string = value.toString().toLowerCase();
            return  length > 0 ? string.substring(0, Math.min(length, string.length())) : string;
        }
        return value;
    }

	/**
     * Adds the current workflow task to the contentlet in order to be reindexed.
     *
     * @param contentlet {@link Contentlet}
     * @return {@link Map}
     */
    protected Map<String, Object> getWorkflowInfoForContentlet(final Contentlet contentlet) {

        final Map<String, Object> workflowMap = new HashMap<>();
        final WorkflowAPI api = workflowAPI.get();
        try {

            final WorkflowTask task = api.findTaskByContentlet(contentlet);

            if(task != null && task.getId() != null && null != task.getStatus()) {

                final WorkflowStep step = api.findStep(task.getStatus());
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
                final List<WorkflowScheme> schemes = api.findSchemesForContentType(contentlet.getContentType());
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

	    final ContentType type = contentTypeAPI.get().find(con.getContentTypeId());
	    List<com.dotcms.contenttype.model.field.Field> catFields = type.fields().stream()
				.filter(field -> field instanceof CategoryField).collect(CollectionsUtils.toImmutableList());

        if(catFields.isEmpty()) {
        	return;
		}

	    final List<Category> cats = categoryAPI.getParents(con, APILocator.systemUser(), false);

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
			if(roleAPI.loadCMSOwnerRole().getId().equals(String.valueOf(permission.getRoleId()))){
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

	public static final Lazy<FastDateFormat> publishExpireESDateTimeFormat = Lazy.of(() -> {
		final TimeZone timeZone = APILocator.systemTimeZone();
		return FastDateFormat.getInstance("yyyy-MM-dd't'HH:mm:ssZ", timeZone);
	});

	public static final FastDateFormat timeFormat = FastDateFormat.getInstance("HH:mm:ss");

	public static final Date dateOufOfRange = Date.from(
			LocalDate.of(2999, Month.JANUARY, 1)
					.atStartOfDay(ZoneId.systemDefault()).toInstant());

	protected void loadFields(final Contentlet contentlet, final Map<String, Object> contentletMap) throws DotDataException {

		// https://github.com/dotCMS/dotCMS/issues/6152
		final DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
		otherSymbols.setDecimalSeparator('.');

		final DecimalFormat numFormatter = new DecimalFormat("0000000000000000000.000000000000000000", otherSymbols);
		final List<Field> fields  = new ArrayList<>(
				FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode()));
		final Structure structure = contentlet.getStructure();
		StringBuilder keyNameBuilder;
		String keyName;
		String keyNameText;

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
						contentletMap.put(keyName, elasticSearchDateTimeFormat.format((valueObj)));
						contentletMap.put(keyNameText, dateString);
					}
					catch(Exception ex) {
						contentletMap.put(keyName, null);
						contentletMap.put(keyNameText, null);
					}
				} else if(field.getFieldType().equals(ESMappingConstants.FIELD_TYPE_DATE_TIME)) {
					try {
						String datetimeString = datetimeFormat.format(valueObj);
						contentletMap.put(keyName, elasticSearchDateTimeFormat.format((valueObj)));
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
					//Load regular key-value fields. Metadata KeyValues are handled once the meta-data gets generated.
					if(LicenseUtil.getLevel()>= LicenseLevel.STANDARD.level) {

						this.loadKeyValueField(contentletMap, keyName, field, valueObj);
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
					    if (valueObj instanceof Date){
                            try {
                                String datetimeString = datetimeFormat.format(valueObj);
                                contentletMap.put(keyName, elasticSearchDateTimeFormat.format((valueObj)));
                                contentletMap.put(keyNameText, datetimeString);
                            } catch(Exception ex) {
                                contentletMap.put(keyName, valueObj);
                                contentletMap.put(keyNameText, valueObj.toString());
                            }
                        } else{
                            contentletMap.put(keyName, valueObj);
                            contentletMap.put(keyNameText, valueObj.toString());
                        }
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

		final List<Tag> tagList = tagAPI.getTagsByInode(contentlet.getInode());
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
								   final Field field,
								   final Object valueObj) {

		if(field.getVelocityVarName().equals(FileAssetAPI.META_DATA_FIELD)){
		  //KeyFields named named Metadata can not be handled here since the metadata hasn't been written yet.
		  //such logic needs to happen right after writeMetadata has done it's thing
		  return;
		}

		final Map<String,Object> keyValueMap = keyValueMap(valueObj);

		keyValueMap.forEach((k, v) -> {
			((List)contentletMap.computeIfAbsent(keyName, key -> new ArrayList<>())).add(
                ImmutableMap.of( "key", k.toLowerCase(), "value", String.valueOf(v),  "key_value", (k + "_" + v).toLowerCase())   
			 );
		});
		
	}

	/**
	 * value object must be resolved to an iterable map so the fields can be extracted
	 * @param valueObj
	 * @return
	 */
	private final Map<String,Object> keyValueMap(final Object valueObj){
	    if(valueObj instanceof Map){
	       return new HashMap<>((Map<String,Object>)valueObj);
	    }

		return KeyValueFieldUtil.JSONValueToHashMap((String) valueObj);
	}

	/**
	 * This method loads a date/time field into the map to be included in the ES index.
	 * @param contentletMap Map to be populated with the date/time field
	 * @param fieldName Name of the field to be populated
	 * @param dateValue Value of the date/time field
	 *                  If the value is null, the field will be populated with a date out of range
	 */
	private void loadDateTimeFieldValue(final Map<String, Object> contentletMap,
			final String fieldName, final Date dateValue) {
		if (UtilMethods.isSet(dateValue)) {
			contentletMap.put(fieldName,
					publishExpireESDateTimeFormat.get().format(dateValue));
			contentletMap.put(fieldName + TEXT,
					datetimeFormat.format(dateValue));
		} else {
			contentletMap.put(fieldName,
					publishExpireESDateTimeFormat.get().format(dateOufOfRange));
			contentletMap.put(fieldName + TEXT,
					datetimeFormat.format(dateOufOfRange));
		}
	}

	public String toJsonString(Map<String, Object> map) throws IOException{
		return mapper.writeValueAsString(map);
	}

	@CloseDBIfOpened
	public List<String> dependenciesLeftToReindex(final Contentlet contentlet) throws DotStateException, DotDataException, DotSecurityException {
		final List<String> dependenciesToReindex = new ArrayList<>();


		final String relatedSQL = "select tree.* from tree where child = ? order by tree_order";
		final DotConnect db = new DotConnect();
		db.setSQL(relatedSQL);
		db.addParam(contentlet.getIdentifier());

		final List<HashMap<String, String>> relatedContentlets = db.loadResults();

		if(relatedContentlets.size()>0) {

			final List<Relationship> relationships = relationshipAPI
					.byContentType(contentlet.getContentType());

			for(final Relationship relationship : relationships) {

				final List<Contentlet> oldDocs;
				final List<String> oldRelatedIds = new ArrayList<>();
				final List<String> newRelatedIds = new ArrayList<>();

                oldDocs = contentletAPI.getRelatedContent(contentlet, relationship,
                        userAPI.getSystemUser(), false);

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
