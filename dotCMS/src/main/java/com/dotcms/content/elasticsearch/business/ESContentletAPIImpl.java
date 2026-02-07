package com.dotcms.content.elasticsearch.business;

import static com.dotcms.exception.ExceptionUtil.bubbleUpException;
import static com.dotcms.exception.ExceptionUtil.getLocalizedMessageOrDefault;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.URL_MAP_FOR_CONTENT_KEY;
import static com.dotmarketing.portlets.personas.business.PersonaAPI.DEFAULT_PERSONA_NAME_KEY;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.api.system.event.ContentletSystemEventUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.content.elasticsearch.business.event.ContentletArchiveEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletCheckinEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletDeletedEvent;
import com.dotcms.content.elasticsearch.business.event.ContentletPublishEvent;
import com.dotcms.content.elasticsearch.business.field.FieldHandlerStrategyFactory;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.content.elasticsearch.util.PaginationUtil;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategy;
import com.dotcms.contenttype.business.BaseTypeToContentTypeStrategyResolver;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.DotAssetValidationException;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategyResolver;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeIf;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.cost.RequestCost;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.contenttype.util.StoryBlockUtil;
import com.dotcms.util.JsonUtil;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.content.CommitListenerEvent;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.FunctionUtils;
import com.dotcms.util.ThreadContextUtil;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.query.GenericQueryFactory.Query;
import com.dotmarketing.business.query.QueryUtil;
import com.dotmarketing.business.query.ValidationException;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.comparators.ContentMapComparator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.Builder;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformer;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.TrashUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.contentet.pagination.PaginatedContentletBuilder;
import com.dotmarketing.util.contentet.pagination.PaginatedContentlets;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.thoughtworks.xstream.XStream;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.MimeType;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;


/**
 * Implementation class for the {@link ContentletAPI} interface.
 *
 * @author Jason Tesser
 * @author David Torres
 * @since 1.5
 */
public class ESContentletAPIImpl implements ContentletAPI {

    private static Lazy<Boolean> FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION = Lazy.of(() ->
            Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION, true));
    private static final String CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT = "Can't change state of checked out content or where inode is not set. Use Search or Find then use method";
    private static final String CANT_GET_LOCK_ON_CONTENT = "Only the CMS Admin or the user who locked the contentlet can lock/unlock it";
    private static final String FAILED_TO_DELETE_UNARCHIVED_CONTENT = "Failed to delete unarchived content. Content must be archived first before it can be deleted.";
    private static final String NEVER_EXPIRE = "NeverExpire";
    private static final String CHECKIN_IN_PROGRESS = "__checkin_in_progress__";

    private static final String IS_NEW_CONTENT = "__IS_NEW_CONTENT__";

    private final ContentletIndexAPIImpl indexAPI;
    private final ESContentFactoryImpl contentFactory;
    private final PermissionAPI permissionAPI;
    private final CategoryAPI categoryAPI;
    private final RelationshipAPI relationshipAPI;
    private final LanguageAPI languageAPI;
    private final ReindexQueueAPI reindexQueueAPI;
    private final TagAPI tagAPI;
    private final IdentifierStripedLock lockManager;
    private final TempFileAPI tempApi;
    private final FileMetadataAPI fileMetadataAPI;
    private final Lazy<MultiTreeAPI> multiTreeAPI = Lazy.of(APILocator::getMultiTreeAPI);

    public static final int MAX_LIMIT = 10000;
    private static final boolean INCLUDE_DEPENDENCIES = true;

    private static final String backupPath =
            ConfigUtils.getBackupPath() + File.separator + "contentlets";

    public static final String UNIQUE_PER_SITE_FIELD_VARIABLE_NAME = "uniquePerSite";

    /**
     * Property to fetch related content from database (only applies for relationship fields)
     * Related content for legacy relationship will always be pulled from the index
     */
    private static final boolean GET_RELATED_CONTENT_FROM_DB = Config
            .getBooleanProperty("GET_RELATED_CONTENT_FROM_DB", true);

    private final ContentletSystemEventUtil contentletSystemEventUtil;
    private final LocalSystemEventsAPI localSystemEventsAPI;
    private final BaseTypeToContentTypeStrategyResolver baseTypeToContentTypeStrategyResolver =
            BaseTypeToContentTypeStrategyResolver.getInstance();

    private final static Lazy<Boolean> SET_DEFAULT_VALUES = Lazy.of(()-> Config.getBooleanProperty("CONTENT_API_SET_DEFAULT_VALUES", true));

    /**
     * Configuration property to control whether to skip throwing FileAssetValidationException
     * when a file asset is missing its binary field. When set to true, a debug log message
     * will be written instead of throwing an exception. This can be useful during migration
     * or bulk operations where some file assets may have missing binaries that need to be
     * handled gracefully. Default: false (throw exception)
     */
    public static final String SKIP_FILE_ASSET_BINARY_VALIDATION = "SKIP_FILE_ASSET_BINARY_VALIDATION";

    private static final Lazy<Boolean> SKIP_FILE_ASSET_BINARY_VALIDATION_FLAG = Lazy.of(() ->
            Config.getBooleanProperty(SKIP_FILE_ASSET_BINARY_VALIDATION, false));

    private  final Lazy<UniqueFieldValidationStrategyResolver> uniqueFieldValidationStrategyResolver;

    public enum QueryType {
        search, suggest, moreLike, Facets
    }

    ;

    private static final Supplier<String> ND_SUPPLIER = () -> "N/D";

    public static boolean getFeatureFlagDbUniqueFieldValidation() {
        return FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION.get();
    }

    @VisibleForTesting
    public static void setFeatureFlagDbUniqueFieldValidation(final boolean newValue) {
        FEATURE_FLAG_DB_UNIQUE_FIELD_VALIDATION = Lazy.of(() -> newValue);
    }

    /**
     * Default class constructor.
     */
    public ESContentletAPIImpl() {
        this.uniqueFieldValidationStrategyResolver = Lazy.of(ESContentletAPIImpl::getUniqueFieldValidationStrategyResolver);
        indexAPI = new ContentletIndexAPIImpl();
        contentFactory = new ESContentFactoryImpl();
        permissionAPI = APILocator.getPermissionAPI();
        categoryAPI = APILocator.getCategoryAPI();
        relationshipAPI = APILocator.getRelationshipAPI();
        languageAPI = APILocator.getLanguageAPI();
        reindexQueueAPI = APILocator.getReindexQueueAPI();
        tagAPI = APILocator.getTagAPI();
        contentletSystemEventUtil = ContentletSystemEventUtil.getInstance();
        localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();
        lockManager = DotConcurrentFactory.getInstance().getIdentifierStripedLock();
        tempApi = APILocator.getTempFileAPI();
        fileMetadataAPI = APILocator.getFileMetadataAPI();
    }

    private static UniqueFieldValidationStrategyResolver getUniqueFieldValidationStrategyResolver() {
        return CDIUtils.getBeanThrows(UniqueFieldValidationStrategyResolver.class);
    }

    @Override
    public SearchResponse esSearchRaw(String esQuery, boolean live, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        return APILocator.getEsSearchAPI().esSearchRaw(esQuery, live, user, respectFrontendRoles);
    }

    @Override
    public ESSearchResults esSearch(String esQuery, boolean live, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        return APILocator.getEsSearchAPI().esSearch(esQuery, live, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public Object loadField(String inode, Field f) throws DotDataException {
        return contentFactory.loadField(inode, f.getFieldContentlet());
    }

    @CloseDBIfOpened
    public Object loadField(final String inode,
            final com.dotcms.contenttype.model.field.Field field) throws DotDataException {
        if (APILocator.getContentletJsonAPI().isPersistContentAsJson()) {
            final Object value = contentFactory.loadJsonField(inode, field);
            if (null != value) {
                return value;
            }
        }
        return contentFactory.loadField(inode, field.dbColumn());
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllContent(int offset, int limit) throws DotDataException {
        return contentFactory.findAllCurrent(offset, limit);
    }

    @Override
    public boolean isContentlet(String inode) throws DotDataException, DotRuntimeException {
        Contentlet contentlet = new Contentlet();
        try {
            contentlet = find(inode, APILocator.getUserAPI().getSystemUser(), true);
        } catch (DotSecurityException dse) {
            throw new DotRuntimeException("Unable to use system user : ", dse);
        } catch (Exception e) {
            Logger.debug(this, "Inode unable to load as contentlet.  Asssuming it is not content");
            return false;
        }
        if (contentlet != null) {
            if (InodeUtils.isSet(contentlet.getInode())) {
                return true;
            }
        }
        return false;
    }

    @CloseDBIfOpened
    @Override
    public Contentlet find(final String inode, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return find (inode, user, respectFrontendRoles, false);

    }

    /**
     * Find a {@link Contentlet} first looks it in the cache if it is there then return it from there, if it is not in the cache
     * then get it directly from Database.
     * Also check permission.
     *
     * @param inode {@link Contentlet}'s inode
     * @param user User to check Permission
     * @param respectFrontendRoles if it true then Frontend rules are respected
     * @param ignoreBlockEditor if it is true and the {@link Contentlet} is loaded from cache then the Story Blocks are not refresh
     *                          if it is loaded from Database then then the Story Blocks are not hydrated
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @RequestCost(Price.CONTENT_FROM_CACHE)
    @CloseDBIfOpened
    @Override
    public Contentlet find(final String inode, final User user, final boolean respectFrontendRoles, boolean ignoreBlockEditor)
            throws DotDataException, DotSecurityException {
        final Contentlet contentlet = contentFactory.find(inode, ignoreBlockEditor);
        if (contentlet == null) {
            return null;
        }

        if (this.permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                user,
                respectFrontendRoles)) {
            
            if (contentlet.isHTMLPage()) {
                try {
                    final ContentType contentType = contentlet.getContentType();
                    final com.dotcms.contenttype.model.field.Field urlField = contentType != null 
                            ? contentType.fieldMap().get(HTMLPageAssetAPI.URL_FIELD) : null;
                    
                    if (urlField != null && UtilMethods.isSet(urlField.defaultValue())) {
                        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
                        if (identifier != null && UtilMethods.isSet(identifier.getAssetName())) {
                            contentlet.setStringProperty(HTMLPageAssetAPI.URL_FIELD, identifier.getAssetName());
                        }
                    }
                } catch (Exception e) {
                    Logger.debug(this, "Could not populate URL for HTML Page: " + e.getMessage());
                }
            }
            
            return contentlet;
        } else {
            final String userId = (user == null) ? "Unknown" : user.getUserId();
            throw new DotSecurityException(
                    String.format("User '%s' does not have READ permissions on %s", userId,
                            ContentletUtil
                                    .toShortString(contentlet)));
        }
    }

    @Override
    public Contentlet move(final Contentlet contentlet, final User user,
            final String hostAndFolderPath,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        Logger.debug(this, () -> "Moving contentlet: " + contentlet.getIdentifier() + " to: "
                + hostAndFolderPath);

        if (UtilMethods.isNotSet(hostAndFolderPath) || !hostAndFolderPath.startsWith(
                HostUtil.HOST_INDICATOR)) {

            throw new IllegalArgumentException("The host path is not valid: " + hostAndFolderPath);
        }

        final Tuple2<String, Host> hostPathTuple = Try.of(
                () -> HostUtil.splitPathHost(hostAndFolderPath, user,
                        StringPool.FORWARD_SLASH)).getOrElseThrow(e -> new DotRuntimeException(e));

        return this.move(contentlet, user, hostPathTuple._2(), hostPathTuple._1(),
                respectFrontendRoles);
    }

    @Override
    public Contentlet move(final Contentlet contentlet, final User user, final Host host,
            final String folderPathParam,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        Logger.debug(this, () -> "Moving contentlet: " + contentlet.getIdentifier() + " to: "
                + folderPathParam);

        if (UtilMethods.isNotSet(folderPathParam) || !folderPathParam.startsWith(
                StringPool.SLASH)) {

            throw new IllegalArgumentException("The folder is not valid: " + folderPathParam);
        }

        // we need a / at the end to check if exits
        final String folderPath = folderPathParam.endsWith(StringPool.SLASH) ? folderPathParam
                : folderPathParam + StringPool.SLASH;

        //Check if the folder exists via Admin user, b/c user couldn't have VIEW Permissions over the folder
        Folder folder = Try.of(() -> APILocator.getFolderAPI()
                        .findFolderByPath(folderPath, host, APILocator.systemUser(), respectFrontendRoles))
                .getOrNull();

        if (null == folder || !UtilMethods.isSet(folder.getInode())) {

            // if the folder does not exists try, let's see if the current user can create it.
            Logger.debug(this, () -> "On Moving Contentlet, creating the Folders: " + folderPath);

            try {
                // multiple contentlets on a bulk action may require to create the same folder
                // if after release to block, the folder does not exists so create it
                final String lockKey = folderPath;
                folder = lockManager.tryLock(lockKey,
                        () -> {

                            Folder testFolder = Try.of(() -> APILocator.getFolderAPI()
                                            .findFolderByPath(folderPath, host, user, respectFrontendRoles))
                                    .getOrNull();

                            if (null == testFolder || !UtilMethods.isSet(testFolder.getInode())) {

                                Logger.debug(this,
                                        () -> "Creating folders: " + folderPath + ", contentlet: "
                                                + contentlet.getIdentifier());
                                testFolder = DotConcurrentFactory.getInstance()
                                        .getSingleSubmitter() // we need to run this in a separated thread, to use a diff conn.
                                        .submit(() -> this.createFolder(folderPath, contentlet,
                                                host, user, respectFrontendRoles))
                                        .get(); // b.c the folder is a pre-requisites
                            }

                            return testFolder;
                        });
            } catch (final Throwable t) {
                Logger.warn(getClass(), t.getMessage(), t);
                folder = null;
            }

            if (null == folder || !UtilMethods.isSet(folder.getInode())) {
                throw new IllegalArgumentException(
                        "The folder does not exists: " + folderPath + " and could not be created");
            }
        }

        return this.move(contentlet, user, host, folder, respectFrontendRoles);
    }

    @WrapInTransaction
    private Folder createFolder(final String folderPath, final Contentlet contentlet,
            final Host host,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        Logger.debug(this, () -> "Creating folders: " + folderPath + ", contentlet: "
                + contentlet.getIdentifier());
        return APILocator.getFolderAPI()
                .createFolders(folderPath, host, user, respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet move(final Contentlet contentlet, final User incomingUser, final Host host,
            final Folder folder,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        Logger.debug(this, () -> "Moving contentlet: " + contentlet.getIdentifier()
                + " to host: " + host.getHostname() + " and path: " + folder.getPath() + ", id: "
                + folder.getIdentifier());

        final User user =
                incomingUser != null ? incomingUser : APILocator.getUserAPI().getAnonymousUser();

        if (user.isAnonymousUser() && AnonymousAccess.systemSetting() != AnonymousAccess.WRITE) {
            throw new DotSecurityException(
                    "CONTENT_APIS_ALLOW_ANONYMOUS setting does not allow anonymous content WRITEs");
        }

        // if the user can write and add a children to the folder
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE, user,
                respectFrontendRoles) ||
                !permissionAPI.doesUserHavePermission(folder, PERMISSION_CAN_ADD_CHILDREN, user)) {

            this.throwSecurityException(contentlet, user);
        }

        final Identifier identifier = APILocator.getIdentifierAPI()
                .loadFromDb(contentlet.getIdentifier());

        // if id exists
        if (null == identifier || !UtilMethods.isSet(identifier.getId())) {

            throw new DoesNotExistException(
                    "The identifier does not exists: " + contentlet.getIdentifier());
        }

        //Check if another content with the same name already exists in the new folder
        if (APILocator.getFileAssetAPI().fileNameExists(host, folder, identifier.getAssetName(),
                contentlet.getIdentifier())) {
            throw new IllegalArgumentException(
                    "Content with the same name: '" + identifier.getAssetName()
                            + "' already exists at the new path: " + host.getHostname()
                            + folder.getPath());
        }

        // update with the new host and path
        identifier.setHostId(host.getIdentifier());
        identifier.setParentPath(folder.getPath());

        Logger.debug(this, () -> "Updating the identifier: " + identifier);
        // changing the host and path will move the contentlet
        APILocator.getIdentifierAPI().save(identifier);

        // update the version ts in order to be repushed
        final Optional<ContentletVersionInfo> versionInfoOpt = APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier.getId(), contentlet.getLanguageId());
        if (versionInfoOpt.isPresent()) {
            versionInfoOpt.get().setVersionTs(new Date());
            APILocator.getVersionableAPI().saveContentletVersionInfo(versionInfoOpt.get());
        }

        // update the content host + folder
        contentlet.setHost(host.getIdentifier());
        contentlet.setFolder(folder.getInode());

        // clean cache
        HibernateUtil.addCommitListener(identifier.getId(), new FlushCacheRunnable() {
            @Override
            public void run() {
                CacheLocator.getContentletCache().remove(contentlet.getInode());
            }
        });

        // refresh the index based on the index policy
        this.indexAPI.addContentToIndex(contentlet, false);

        return contentlet;
    }

    @CloseDBIfOpened
    @Override
    public Optional<Contentlet> findInDb(final String inode) {

        return contentFactory.findInDb(inode);

    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findByStructure(String structureInode, User user,
            boolean respectFrontendRoles, int limit, int offset)
            throws DotDataException, DotSecurityException {
        List<Contentlet> contentlets = contentFactory.findByStructure(structureInode, limit,
                offset);
        return permissionAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ,
                respectFrontendRoles, user);
    }

    @Override
    public List<Contentlet> findByStructure(Structure structure, User user,
            boolean respectFrontendRoles, int limit, int offset)
            throws DotDataException, DotSecurityException {
        return findByStructure(structure.getInode(), user, respectFrontendRoles, limit, offset);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletForLanguage(long languageId, Identifier contentletId)
            throws DotDataException, DotSecurityException {
        try {
            return findContentletByIdentifier(contentletId.getId(), false, languageId,
                    APILocator.systemUser(), false);
        } catch (DotContentletStateException dcs) {
            Logger.debug(this, () -> String.format(
                    "No working contentlet found for language: %d and identifier: %s ", languageId,
                    contentletId.getId()));
        }
        return null;
    }


    public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId,
            String variantId, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        if (languageId <= 0) {
            languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        }

        try {
            Optional<ContentletVersionInfo> clvi = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(identifier, languageId, variantId);
            String liveInode = null;
            String workingInode = null;

            if (clvi.isPresent()) {
                liveInode = clvi.get().getLiveInode();
                workingInode = clvi.get().getWorkingInode();
            }

            if (live) {
                return find(liveInode, user, respectFrontendRoles);
            } else {
                return find(workingInode, user, respectFrontendRoles);
            }
        } catch (DotSecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new DotContentletStateException(
                    "Can't find contentlet: " + identifier + " lang:" + languageId + " live:"
                            + live, e);
        }
    }

    /**
     * @inheritDoc
     */
    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifier(final String identifier, final long languageId, final String variantId,
            final Date timeMachineDate, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException{
        final Contentlet contentlet = contentFactory.findContentletByIdentifier(identifier, languageId, variantId, timeMachineDate);
        if (contentlet == null) {
            Logger.debug(this, "Contentlet not found for identifier: " + identifier + " lang:" + languageId + " variant:" + variantId + " date:" + timeMachineDate);
            return null;
        }
        if (permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            return contentlet;
        } else {
            final String userId = (user == null) ? "Unknown" : user.getUserId();
            throw new DotSecurityException(
                    String.format("User '%s' does not have READ permissions on %s", userId,
                            ContentletUtil
                                    .toShortString(contentlet)));
        }
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifier(String identifier, boolean live, long languageId,
            User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return findContentletByIdentifier(identifier, live, languageId,
                VariantAPI.DEFAULT_VARIANT.name(), user, respectFrontendRoles);

    }


    @CloseDBIfOpened
    @Override
    public Optional<Contentlet> findContentletByIdentifierOrFallback(final String identifier,
            final boolean live,
            final long incomingLangId, final User user, final boolean respectFrontendRoles) {

        final long defaultLanguageId = this.languageAPI.getDefaultLanguage().getId();
        final long tryLanguage = incomingLangId <= 0 ? defaultLanguageId : incomingLangId;
        boolean fallback = false;

        try {

            // try the user language
            Optional<ContentletVersionInfo> contentletVersionInfo =
                    APILocator.getVersionableAPI()
                            .getContentletVersionInfo(identifier, tryLanguage);

            // try the fallback if does not exists
            if (tryLanguage != defaultLanguageId && (contentletVersionInfo.isEmpty()
                    || (live && contentletVersionInfo.get().getLiveInode() == null))) {
                fallback = true;  // using the fallback
                contentletVersionInfo = APILocator.getVersionableAPI()
                        .getContentletVersionInfo(identifier, defaultLanguageId);
            }

            if (contentletVersionInfo.isEmpty()) {
                return Optional.empty();
            }

            final Contentlet contentlet = live ?
                    this.find(contentletVersionInfo.get().getLiveInode(), user,
                            respectFrontendRoles) :
                    this.find(contentletVersionInfo.get().getWorkingInode(), user,
                            respectFrontendRoles);

            if (null == contentlet) {
                return Optional.empty();
            }

            // if we are using the fallback, and it is not allowed, return empty
            if (fallback && tryLanguage != defaultLanguageId && !contentlet.getContentType()
                    .languageFallback()) {
                return Optional.empty();
            }

            return Optional.of(contentlet);
        } catch (Exception e) {
            throw new DotContentletStateException(
                    "Can't find contentlet: " + identifier + " lang:" + incomingLangId + " live:"
                            + live, e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Optional<Contentlet> findContentletByIdentifierOrFallback(final String identifier,
                                                                     final boolean live,
                                                                     final long incomingLangId, final User user,
                                                                     final boolean respectFrontendRoles,
                                                                     final String variantName) {

        final long defaultLanguageId = this.languageAPI.getDefaultLanguage().getId();
        final long tryLanguage = incomingLangId <= 0 ? defaultLanguageId : incomingLangId;
        boolean fallback = false;

        try {

            // try the user language
            Optional<ContentletVersionInfo> contentletVersionInfo =
                    Objects.nonNull(variantName)?
                            APILocator.getVersionableAPI().getContentletVersionInfo(identifier, tryLanguage, variantName):
                            APILocator.getVersionableAPI().getContentletVersionInfo(identifier, tryLanguage);

            // try the fallback if does not exists
            if (tryLanguage != defaultLanguageId && (contentletVersionInfo.isEmpty()
                    || (live && contentletVersionInfo.get().getLiveInode() == null))) {
                fallback = true;  // using the fallback
                contentletVersionInfo = Objects.nonNull(variantName)?
                        APILocator.getVersionableAPI().getContentletVersionInfo(identifier, defaultLanguageId, variantName):
                        APILocator.getVersionableAPI().getContentletVersionInfo(identifier, defaultLanguageId);
            }

            if (contentletVersionInfo.isEmpty()) {
                return Optional.empty();
            }

            final Contentlet contentlet = live ?
                    this.find(contentletVersionInfo.get().getLiveInode(), user,
                            respectFrontendRoles) :
                    this.find(contentletVersionInfo.get().getWorkingInode(), user,
                            respectFrontendRoles);

            if (null == contentlet) {
                return Optional.empty();
            }

            // if we are using the fallback, and it is not allowed, return empty
            if (fallback && tryLanguage != defaultLanguageId && !contentlet.getContentType()
                    .languageFallback()) {
                return Optional.empty();
            }

            return Optional.of(contentlet);
        } catch (Exception e) {
            throw new DotContentletStateException(
                    "Can't find contentlet: " + identifier + " lang:" + incomingLangId + " live:"
                            + live, e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Optional<Contentlet> findContentletByIdentifierOrFallback(final String identifier,
            final long incomingLangId, String variantId, final Date timeMachine, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final long defaultLanguageId = this.languageAPI.getDefaultLanguage().getId();
        final long tryLanguage = incomingLangId <= 0 ? defaultLanguageId : incomingLangId;

        Contentlet contentlet = findContentletByIdentifier(identifier, tryLanguage,
                variantId, timeMachine, user, respectFrontendRoles);

        if (contentlet == null && tryLanguage != defaultLanguageId) {
            contentlet =  findContentletByIdentifier(identifier, defaultLanguageId,
                variantId, timeMachine, user, respectFrontendRoles);
        }
        return Optional.ofNullable(contentlet);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifierAnyLanguage(final String identifier,
            final boolean includeDeleted) throws DotDataException {
        try {
            return contentFactory.findContentletByIdentifierAnyLanguage(identifier, includeDeleted);

        } catch (Exception e) {
            throw new DotContentletStateException("Can't find contentlet: " + identifier, e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifierAnyLanguage(final String identifier)
            throws DotDataException {
        try {
            return contentFactory.findContentletByIdentifierAnyLanguage(identifier);

        } catch (Exception e) {
            throw new DotContentletStateException("Can't find contentlet: " + identifier, e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifierAnyLanguageAnyVariant(final String identifier) throws DotDataException{

        try {
            final ContentletVersionInfo anyContentletVersionInfoAnyVariant = FactoryLocator.getVersionableFactory()
                    .findAnyContentletVersionInfoAnyVariant(identifier, false)
                    .orElseThrow(() -> new NotFoundInDbException("Contentlet does not exists"));

            return contentFactory.find(anyContentletVersionInfoAnyVariant.getWorkingInode());
        } catch (Exception e) {
            throw new DotContentletStateException("Can't find contentlet: " + identifier, e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifierAnyLanguage(final String identifier, final String variant) throws DotDataException {
        return findContentletByIdentifierAnyLanguage(identifier, variant, false);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet findContentletByIdentifierAnyLanguage(final String identifier, final String variant,
            final boolean includeDeleted) throws DotDataException{
        try {
            return contentFactory.findContentletByIdentifierAnyLanguage(identifier, variant, includeDeleted);
        } catch (Exception e) {
            throw new DotContentletStateException("Can't find contentlet: " + identifier, e);
        }
    }


    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentletsByIdentifiers(final String[] identifiers,
            final boolean live, final long languageId, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        final List<Contentlet> contentlets = new ArrayList<>();

        for (final String identifier : identifiers) {

            final Contentlet contentlet = findContentletByIdentifier(identifier.trim(), live,
                    languageId, user, respectFrontendRoles);
            contentlets.add(contentlet);
        }

        return contentlets;
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentlets(List<String> inodes)
            throws DotDataException, DotSecurityException {
        return contentFactory.findContentlets(inodes);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentletsByFolder(Folder parentFolder, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        try {
            return permissionAPI.filterCollection(
                    search("+conFolder:" + parentFolder.getInode(), -1, 0, null, user,
                            respectFrontendRoles), PermissionAPI.PERMISSION_READ,
                    respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }

    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentletsByHost(Host parentHost, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        try {
            return permissionAPI.filterCollection(
                    search("+conHost:" + parentHost.getIdentifier() + " +working:true", -1, 0, null,
                            user, respectFrontendRoles), PermissionAPI.PERMISSION_READ,
                    respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @CloseDBIfOpened
    @Override
    public PaginatedContentlets findContentletsPaginatedByHost(Host parentHost, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return findContentletsPaginatedByHost(parentHost, null, null, user, respectFrontendRoles);
    }


    public PaginatedContentlets findContentletsPaginatedByHost(final Host parentHost,
            final List<Integer> includingContentTypes,
            final List<Integer> excludingContentTypes,
            final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final String query = getContentletByHostQuery(parentHost, includingContentTypes,
                excludingContentTypes);

        return new PaginatedContentletBuilder()
                .setLuceneQuery(query)
                .setUser(user)
                .setRespectFrontendRoles(respectFrontendRoles)
                .build();
    }

    @Override
    public ESContentletScroll createScrollQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles, final int batchSize, final String sortBy)
            throws DotSecurityException, DotDataException {

        // Apply permissions to query
        final String queryWithPermissions = applyPermissionsToQuery(luceneQuery, user, respectFrontendRoles);

        // Delegate to factory with the permission-filtered query
        return contentFactory.createScrollQuery(queryWithPermissions, user, respectFrontendRoles, batchSize, sortBy);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentletsByHost(Host parentHost,
            List<Integer> includingContentTypes, List<Integer> excludingContentTypes, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        try {
            final String query = getContentletByHostQuery(parentHost, includingContentTypes,
                    excludingContentTypes);

            return permissionAPI.filterCollection(
                    search(query, -1, 0, null, user, respectFrontendRoles),
                    PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    private String getContentletByHostQuery(Host parentHost, List<Integer> includingContentTypes,
            List<Integer> excludingContentTypes) {
        final StringBuilder query = new StringBuilder();
        query.append("+conHost:").append(parentHost.getIdentifier()).append(" +working:true");

        // Including content types
        if (includingContentTypes != null && !includingContentTypes.isEmpty()) {
            query.append(" +structureType:(").append(StringUtils.join(includingContentTypes, " "))
                    .append(")");
        }

        // Excluding content types
        if (excludingContentTypes != null && !excludingContentTypes.isEmpty()) {
            query.append(" -structureType:(").append(StringUtils.join(excludingContentTypes, " "))
                    .append(")");
        }
        return query.toString();
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findContentletsByHostBaseType(Host parentHost,
            List<Integer> includingBaseTypes, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        try {
            StringBuilder query = new StringBuilder();
            query.append("+conHost:").append(parentHost.getIdentifier()).append(" +working:true");

            // Including content types
            if (includingBaseTypes != null && !includingBaseTypes.isEmpty()) {
                query.append(" +baseType:(").append(StringUtils.join(includingBaseTypes, " "))
                        .append(")");
            }

            return permissionAPI.filterCollection(
                    search(query.toString(), -1, 0, null, user, respectFrontendRoles),
                    PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    // note: is not annotated with WrapInTransaction b/c it handles his own transaction locally in the method
    @WrapInTransaction
    @Override
    public void publish(final Contentlet contentlet, final User userIn,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotStateException {
        final User user = (userIn != null) ? userIn : APILocator.getUserAPI().getAnonymousUser();
        String contentPushPublishDate = contentlet.getStringProperty(
                Contentlet.WORKFLOW_PUBLISH_DATE);
        String contentPushExpireDate = contentlet.getStringProperty("wfExpireDate");

        contentPushPublishDate =
                UtilMethods.isSet(contentPushPublishDate) ? contentPushPublishDate : "N/D";
        contentPushExpireDate =
                UtilMethods.isSet(contentPushExpireDate) ? contentPushExpireDate : "N/D";

        ActivityLogger.logInfo(getClass(), "Publishing Content",
                "StartDate: " + contentPushPublishDate + "; "
                        + "EndDate: " + contentPushExpireDate + "; User:" + (user != null
                        ? user.getUserId() : "Unknown")
                        + "; ContentIdentifier: " + (contentlet != null ? contentlet.getIdentifier()
                        : "Unknown"), contentlet.getHost());

        try {

            final Optional<Contentlet> contentletOpt = this.checkAndRunPublishAsWorkflow(contentlet,
                    user, respectFrontendRoles);
            if (contentletOpt.isPresent()) {

                Logger.debug(this, "A Workflow has been run instead of a simple publish: " +
                        contentlet.getIdentifier());
                if (!contentlet.getInode().equals(contentletOpt.get().getInode())) {
                    this.copyProperties(contentlet, contentletOpt.get().getMap());
                }
                return;
            }

            this.internalPublish(contentlet, user, respectFrontendRoles);
        } catch (DotDataException | DotStateException | DotSecurityException e) {
            logInfo("Error Publishing Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);
            throw e;
        }

        logInfo("Content Published", contentlet, user, contentPushPublishDate, contentPushExpireDate);

        //Generate a System Event for this publish operation
        HibernateUtil.addCommitListener(
                () -> this.contentletSystemEventUtil.pushPublishEvent(contentlet), 1000);
    }

    private void internalPublish(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        if (BLANK.equals(contentlet.getInode())) {

            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH,
                user, respectFrontendRoles)) {

            Logger.debug(PublishFactory.class,
                    () -> "publishAsset: user = " + (user != null ? user.getEmailAddress()
                            : "Unknown")
                            + ", don't have permissions to publish: " + (contentlet != null
                            ? contentlet.getInode() : "Unknown"));

            //If the contentlet has CMS Owner Publish permission on it, the user creating the new contentlet is allowed to publish
            List<Role> roles = permissionAPI.getRoles(contentlet.getPermissionId(),
                    PermissionAPI.PERMISSION_PUBLISH, Role.CMS_OWNER_ROLE, 0, -1);
            if (roles.isEmpty() &&
                    isNewContentlet(contentlet)) {

                roles = permissionAPI.getRoles(contentlet.getContentType().getPermissionId(),
                        PermissionAPI.PERMISSION_PUBLISH, Role.CMS_OWNER_ROLE, 0, -1);
            }
            final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();

            if (roles.size() > 0) {
                final boolean isCMSOwner = roles.stream().anyMatch(cmsOwner::equals);

                if (!isCMSOwner) {

                    throw new DotSecurityException(
                            "User " + (user != null ? user.getUserId() : "Unknown")
                                    + "does not have permission to publish contentlet with inode "
                                    + (contentlet != null ? contentlet.getInode() : "Unknown"));
                }
            } else {
                throw new DotSecurityException(
                        "User " + (user != null ? user.getUserId() : "Unknown")
                                + "does not have permission to publish contentlet with inode "
                                + (contentlet != null ? contentlet.getInode() : "Unknown"));
            }
        }

        WorkflowProcessor workflow = null;
        // to run a workflow we need an action id set, not be part of a workflow already and do not desired disable it
        if (contentlet.getMap().get(Contentlet.DISABLE_WORKFLOW) == null &&
                UtilMethods.isSet(contentlet.getActionId()) &&
                (null == contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS) ||
                        Boolean.FALSE.equals(
                                contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS))
                )) {
            workflow = APILocator.getWorkflowAPI().fireWorkflowPreCheckin(contentlet, user);
        }

        canLock(contentlet, user, respectFrontendRoles);

        //Set contentlet to live and unlocked
        APILocator.getVersionableAPI().setLive(contentlet);

        publishAssociated(contentlet, false);

        if (null != workflow) {

            workflow.setContentlet(contentlet);
            APILocator.getWorkflowAPI().fireWorkflowPostCheckin(workflow);
        }

        if (contentlet.isFileAsset()) {

            cleanFileAssetCache(contentlet, user, respectFrontendRoles);
        }

        //"Enable" and/or create a tag for this Persona key tag
        if (Structure.STRUCTURE_TYPE_PERSONA == contentlet.getStructure().getStructureType()) {
            //If not exist create a tag based on this persona key tag
            APILocator.getPersonaAPI().enableDisablePersonaTag(contentlet, true);
        }

        if (contentlet.isVanityUrl()) {
            APILocator.getVanityUrlAPI().invalidateVanityUrl(contentlet);
        }

        invalidateLanguageVariableCache(contentlet);

        /*
        Triggers a local system event when this contentlet commit listener is executed,
        anyone who need it can subscribed to this commit listener event, on this case will be
        mostly use it in order to invalidate this contentlet cache.
         */
        triggerCommitListenerEvent(contentlet, user, true);

        // by now, the publish event is making a duplicate reload events on the site browser
        // so we decided to comment it out by now, and
        //contentletSystemEventUtil.pushPublishEvent(contentlet);
    }

    private boolean isNewContentlet(final Contentlet contentlet) throws DotDataException {
        return contentlet.isNew() || null == contentlet.getIdentifier()
                || ConversionUtils.toBooleanFromDb(contentlet.getMap().getOrDefault(IS_NEW_CONTENT, false))
                || hasOnlyOneVersion(contentlet);
    }

    private boolean hasOnlyOneVersion(final Contentlet contentlet) throws DotDataException {

        final int versionCount = new DotConnect().setSQL("SELECT count(*) as count FROM (SELECT 1 FROM contentlet WHERE identifier =? LIMIT 2) AS t")
                .addParam(contentlet.getIdentifier())
                .loadInt("count");
        return versionCount <= 1;
    }

    @Override
    public void publishAssociated(Contentlet contentlet, boolean isNew)
            throws DotSecurityException, DotDataException,
            DotContentletStateException, DotStateException {
        publishAssociated(contentlet, isNew, true);

    }

    @WrapInTransaction
    @Override
    public void publishAssociated(final Contentlet contentlet, final boolean isNew,
            final boolean isNewVersion) throws
            DotSecurityException, DotDataException, DotStateException {

        if (!contentlet.isWorking()) {

            throw new DotContentletStateException("Only the working version can be published");
        }

        ThreadContextUtil.ifReindex(
                () -> indexAPI.addContentToIndex(contentlet, INCLUDE_DEPENDENCIES),
                INCLUDE_DEPENDENCIES);

        // Publishes the files associated with the Contentlet
        final List<Field> fields = FieldsCache.getFieldsByStructureInode(
                contentlet.getStructureInode());
        final Language defaultLang = languageAPI.getDefaultLanguage();
        final User systemUser = APILocator.getUserAPI().getSystemUser();

        for (final Field field : fields) {
            if (Field.FieldType.IMAGE.toString().equals(field.getFieldType()) ||
                    Field.FieldType.FILE.toString().equals(field.getFieldType())) {

                // NOTE: Keep in mind that at this moment the FILE ASSET could be in the same language or
                // default lang (DEFAULT_FILE_TO_DEFAULT_LANGUAGE=true)
                try {
                    // We need to get the Identifier from the field. (Image or File)
                    final String fieldValue = UtilMethods.isSet(getFieldValue(contentlet, field)) ?
                            getFieldValue(contentlet, field).toString() : StringUtils.EMPTY;
                    Identifier id = APILocator.getIdentifierAPI().find(fieldValue);

                    // If this is a new File Asset (Contentlet).
                    if (InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")) {
                        Contentlet fileAssetCont;

                        // First we want to find the LIVE File Asset with same language of the parent Contentlet.
                        try {
                            findContentletByIdentifier(id.getId(), true, contentlet.getLanguageId(),
                                    systemUser, false);
                        } catch (DotContentletStateException seLive) {
                            // If we don't have results, we try to find the WORKING File Asset
                            // with same language of the parent Contentlet.
                            try {
                                fileAssetCont = findContentletByIdentifier(id.getId(), false,
                                        contentlet.getLanguageId(),
                                        systemUser, false);
                                publish(fileAssetCont, systemUser, false);
                            } catch (DotContentletStateException seWorking) {
                                // Now, if we still don't have resutls we should try to find de LIVE File Asset but
                                // with DEFAULT language. Note: no need to do repeat this is the language previously
                                // serched was already the default.
                                if (defaultLang.getId() != contentlet.getLanguageId()) {
                                    try {
                                        findContentletByIdentifier(id.getId(), true,
                                                defaultLang.getId(),
                                                systemUser, false);
                                    } catch (DotContentletStateException se) {
                                        // Again, if we don't find anything LIVE, we try WORKING File Asset + DEFAULT lang.
                                        fileAssetCont = findContentletByIdentifier(id.getId(),
                                                false, defaultLang.getId(),
                                                systemUser, false);
                                        publish(fileAssetCont, systemUser, false);
                                    }
                                } else {
                                    // If we already were using the default language we need to throw
                                    // the DotContentletStateException.
                                    throw seWorking;
                                }
                            }

                        }
                    }
                } catch (Exception ex) {
                    Logger.debug(this, ex.getMessage(), ex);
                    throw new DotStateException("Problem occurred while publishing file", ex);
                }
            }
        }

        this.publishRelatedLinks(contentlet, isNewVersion, systemUser);

        if (!isNew) {
            // writes the contentlet to a live directory under velocity folder

            new ContentletLoader().invalidate(contentlet);
            CacheLocator.getContentletCache().remove(contentlet.getInode());

            // Need to refresh the live pages that reference this piece of
            // content
            publishRelatedHtmlPages(contentlet);
        }

    }

    @CloseDBIfOpened
    private void publishRelatedLinks(final Contentlet contentlet,
            final boolean isNewVersion,
            final User systemUser) throws DotDataException, DotSecurityException {
        // gets all not live link children
        Logger.debug(this, "IM HERE BEFORE PUBLISHING LINKS FOR A CONTENTLET!!!!!!!");
        final List<Link> links = getRelatedLinks(contentlet, systemUser, false);

        for (Link link : links) {

            Logger.debug(this,
                    "*****I'm a Contentlet -- Publishing my Link Child=" + link.getInode());
            try {

                PublishFactory.publishAsset(link, systemUser, false, isNewVersion);
            } catch (DotSecurityException e) {
                Logger.debug(this, "User has permissions to publish the content = "
                        + contentlet.getIdentifier()
                        + " but not the related link = " + link.getIdentifier());
                throw new DotStateException(
                        "Problem occured while publishing link: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new DotStateException(
                        "Problem occured while publishing file: " + e.getMessage(), e);
            }
        }
    } // publishRelatedLinks.

    @Override
    public PaginatedContentList<Contentlet> searchPaginatedByPage(final String luceneQuery,
            final int contentsPerPage, final int page, final String sortBy, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        // Calculate the offset
        final int currentPage = Math.max(page, 1);
        var offset = contentsPerPage * (currentPage - 1);

        return searchPaginated(
                luceneQuery, contentsPerPage, offset, sortBy, user, respectFrontendRoles
        );
    }

    @Override
    public PaginatedContentList<Contentlet> searchPaginated(final String luceneQuery,
            final int limit, int offset, final String sortBy, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        // Perform the search
        PaginatedArrayList<Contentlet> contents = (PaginatedArrayList<Contentlet>) search(
                luceneQuery, limit, offset, sortBy, user, respectFrontendRoles
        );

        return PaginationUtil.paginatedArrayListToPaginatedContentList(contents, limit, offset);
    }

    @Override
    public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy,
            User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return search(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,
                PermissionAPI.PERMISSION_READ);
    }

    @Override
    public List<Contentlet> search(String luceneQuery, int limit, int offset, String sortBy,
            User user, boolean respectFrontendRoles, int requiredPermission)
            throws DotDataException, DotSecurityException {
        Logger.debug(this, "search - search: " + luceneQuery);
        Logger.debug(this, "search - limit: " + limit);
        Logger.debug(this, "search - offset: " + offset);
        Logger.debug(this, "search - sortBy: " + sortBy);
        Logger.debug(this, "search - user: " + user);
        Logger.debug(this, "search - respectFrontendRoles: " + respectFrontendRoles);
        Logger.debug(this, "search - requiredPermission: " + requiredPermission);

        PaginatedArrayList<Contentlet> contents = new PaginatedArrayList<>();
        ArrayList<String> inodes = new ArrayList<>();

        PaginatedArrayList<ContentletSearch> list = (PaginatedArrayList) searchIndex(luceneQuery,
                limit, offset, sortBy, user, respectFrontendRoles);
        contents.setTotalResults(list.getTotalResults());
        Logger.debug(this, "search - list size: " + contents.getTotalResults());
        for (ContentletSearch conwrap : list) {
            inodes.add(conwrap.getInode());
            Logger.debug(this, "search - inode Added: " + conwrap.getInode());
        }

        Logger.debug(this, "search - inodes: " + inodes);
        List<Contentlet> contentlets = findContentlets(inodes);
        Logger.debug(this, "search - contentlets size: " + contentlets.size());
        Logger.debug(this, "search - contentlets: " + contentlets);
        Map<String, Contentlet> map = new HashMap<>(contentlets.size());
        for (Contentlet contentlet : contentlets) {
            Logger.debug(this, "search - contentlet: " + contentlet);
            Logger.debug(this, "search - contentlet inode: " + contentlet.getInode());
            map.put(contentlet.getInode(), contentlet);
            Logger.debug(this, "search - map size: " + map.size());
        }
        Logger.debug(this, "search - map: " + map);
        for (String inode : inodes) {
            Logger.debug(this, "search - inode: " + inode);
            if (map.get(inode) != null) {
                Logger.debug(this, "search - map.get(inode): " + map.get(inode));
                contents.add(map.get(inode));
                Logger.debug(this, "search - contents size: " + contents.size());
            }
        }
        Logger.debug(this, "search - contents: " + contents);
        return contents;
    }

    @Override
    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset,
            String sortBy, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,
                PermissionAPI.PERMISSION_READ);
    }

    @Override
    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset,
            String sortBy, User user, boolean respectFrontendRoles, int requiredPermission)
            throws DotDataException, DotSecurityException {
        return searchByIdentifier(luceneQuery, limit, offset, sortBy, user, respectFrontendRoles,
                requiredPermission, false);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> searchByIdentifier(String luceneQuery, int limit, int offset,
            String sortBy, User user, boolean respectFrontendRoles, int requiredPermission,
            boolean anyLanguage) throws DotDataException, DotSecurityException {
        PaginatedArrayList<Contentlet> contents = new PaginatedArrayList<>();
        PaginatedArrayList<ContentletSearch> list = (PaginatedArrayList) searchIndex(luceneQuery,
                limit, offset, sortBy, user, respectFrontendRoles);
        contents.setTotalResults(list.getTotalResults());

        List<String> identifierList = new ArrayList<>();
        for (ContentletSearch conwrap : list) {
            String ident = conwrap.getIdentifier();
            Identifier ii = APILocator.getIdentifierAPI().find(ident);
            if (ii != null && UtilMethods.isSet(ii.getId())) {
                identifierList.add(ident);
            }
        }
        String[] identifiers = new String[identifierList.size()];
        identifiers = identifierList.toArray(identifiers);

        List<Contentlet> contentlets = new ArrayList<>();
        if (anyLanguage) {
            for (String identifier : identifierList) {
                for (Language lang : APILocator.getLanguageAPI().getLanguages()) {
                    try {
                        Contentlet languageContentlet = null;
                        try {
                            languageContentlet = findContentletByIdentifier(identifier, false,
                                    lang.getId(), user, respectFrontendRoles);
                        } catch (DotContentletStateException e) {
                            Logger.debug(this, e.getMessage(), e);
                        }
                        if (languageContentlet != null && UtilMethods.isSet(
                                languageContentlet.getInode())) {
                            contentlets.add(languageContentlet);
                            break;
                        }
                    } catch (DotContentletStateException se) {
                        Logger.debug(this, se.getMessage());
                    }
                }
            }
        } else {
            contentlets = findContentletsByIdentifiers(identifiers, false,
                    APILocator.getLanguageAPI().getDefaultLanguage().getId(), user,
                    respectFrontendRoles);
        }

        Map<String, Contentlet> map = new HashMap<>(contentlets.size());
        for (Contentlet contentlet : contentlets) {
            map.put(contentlet.getIdentifier(), contentlet);
        }
        for (String identifier : identifiers) {
            if (map.get(identifier) != null && !contents.contains(map.get(identifier))) {
                contents.add(map.get(identifier));
            }
        }

        return contents;
    }

    @Override
    public List<Contentlet> getAllContentByVariants(final User user,
            final boolean respectFrontendRoles, final String... variantNames)
            throws DotDataException, DotStateException,
            DotSecurityException {

        Logger.debug(this, "Experiments - getAllContentByVariants: " + Arrays.toString(variantNames));

        final String queryWithoutParenthesis = Arrays.stream(variantNames)
                .map((variant) -> "variant:" + variant)
                .collect(Collectors.joining(" OR "));

        final String query = "+(" + queryWithoutParenthesis + ")";

        Logger.debug(this, "Experiments - getAllContentByVariants: query: " + query);

        final List<Contentlet> search = search(query, -1, 0, null,
                user, respectFrontendRoles);

        Logger.debug(this, "Experiments - getAllContentByVariants: search: " + search);

        return search;
    }

    @Override
    public void addPermissionsToQuery(StringBuffer buffy, User user, List<Role> roles,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        if (user != null) {
            buffy.append(" +((+owner:" + user.getUserId() + " +ownerCanRead:true) ");
        } else {
            buffy.append(" +(");
        }
        if (0 < roles.size()) {
            buffy.append(" (");
            for (Role role : roles) {
                buffy.append("permissions:p" + role.getId() + ".1p* ");
            }
            buffy.append(") ");
        }
        if (respectFrontendRoles) {
            buffy.append("(permissions:p" + APILocator.getRoleAPI().loadCMSAnonymousRole().getId()
                    + ".1p*) ");
            if (user != null && user.isFrontendUser()) {
                buffy.append(
                        "(permissions:p" + APILocator.getRoleAPI().loadLoggedinSiteRole().getId()
                                + ".1p*)");
            }
        }
        buffy.append(")");

        if (user == null || !user.isBackendUser()) {
            buffy.append(" +live:true ");
        }


    }

    /**
     * Applies permissions and category permissions to a lucene query string.
     * This method handles admin checks, role loading, and permission clauses.
     * <p>
     * <strong>SECURITY NOTE:</strong> This method MUST be called on any query before
     * passing it to Elasticsearch to ensure proper permission filtering.
     * </p>
     *
     * @param luceneQuery The original lucene query string
     * @param user The user making the request (required if not respecting frontend roles)
     * @param respectFrontendRoles Whether to respect frontend roles
     * @return The query with permissions clauses added
     * @throws DotSecurityException If user is null and not respecting frontend roles
     * @throws DotDataException If there's an error loading roles
     */
    protected String applyPermissionsToQuery(final String luceneQuery, final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // Validate user requirement
        if (user == null && !respectFrontendRoles) {
            throw new DotSecurityException(
                    "You must specify a user if you are not respecting frontend roles");
        }

        // Check if user is admin
        boolean isAdmin = false;
        List<Role> roles = new ArrayList<>();
        if (user != null) {
            if (!APILocator.getRoleAPI()
                    .doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            } else {
                isAdmin = true;
            }
        }

        // If admin, return query unchanged
        if (isAdmin) {
            return luceneQuery;
        }

        // Apply permissions
        final StringBuffer buffy = new StringBuffer(luceneQuery);
        addPermissionsToQuery(buffy, user, roles, respectFrontendRoles);
        addCategoryPermissionsToQuery(buffy, user, roles, respectFrontendRoles);

        return buffy.toString();
    }

    @Override
    public List<ContentletSearch> searchIndex(String luceneQuery, int limit, int offset,
            String sortBy, User user, boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        // Apply permissions to query
        final String queryWithPermissions = applyPermissionsToQuery(luceneQuery, user, respectFrontendRoles);

        if (UtilMethods.isSet(sortBy) && sortBy.trim().equalsIgnoreCase("random")) {
            sortBy = "random";
        }

        if (limit <= 0) {
            limit = MAX_LIMIT;
        }

        if (limit <= MAX_LIMIT) {
            final SearchHits searchHits = contentFactory.indexSearch(queryWithPermissions, limit,
                    offset, sortBy);
            final PaginatedArrayList<ContentletSearch> list = new PaginatedArrayList<>();
            list.setTotalResults(searchHits.getTotalHits().value);

            for (final SearchHit searchHit : searchHits.getHits()) {
                try {
                    final Map<String, Object> sourceMap = searchHit.getSourceAsMap();
                    final ContentletSearch conWrapper = new ContentletSearch();
                    conWrapper.setId(searchHit.getId());
                    conWrapper.setIndex(searchHit.getIndex());
                    conWrapper.setIdentifier(sourceMap.get("identifier").toString());
                    conWrapper.setInode(sourceMap.get("inode").toString());
                    conWrapper.setScore(searchHit.getScore());

                    list.add(conWrapper);
                } catch (Exception e) {
                    Logger.error(this, e.getMessage(), e);
                }
            }
            return list;
        } else {
            return contentFactory.indexSearchScroll(queryWithPermissions, sortBy);
        }

    }

    void addCategoryPermissionsToQuery(StringBuffer buffy, User user, List<Role> roles, boolean respectFrontendRoles) {
        if (!Config.getBooleanProperty("PERMISSION_SECONDARY_CATEGORY_CHECK", true)) {
            return;
        }
        if (user != null && user.isAdmin()) {
            return;
        }

        buffy.append(" +(");

        for (Role role : roles) {
            buffy.append(ESMappingConstants.CATEGORY_PERMISSIONS + ":" + role.getId() + " ");
        }

        if (respectFrontendRoles) {
            buffy.append(ESMappingConstants.CATEGORY_PERMISSIONS + ":"
                    + ESMappingConstants.MAPPED_PERMISSIONS.cms_anon_role.name() + " ");
        }

        // always add NONE
        buffy.append(ESMappingConstants.CATEGORY_PERMISSIONS + ":" + ESMappingConstants.MAPPED_PERMISSIONS.none.name()
                + " ");

        buffy.append(") ");

    }

    @CloseDBIfOpened
    @Override
    public void publishRelatedHtmlPages(final Contentlet contentlet)
            throws DotStateException, DotDataException {
        if (StringUtils.EMPTY.equals(contentlet.getInode())) {
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }
        //Get the contentlet Identifier to gather the related pages
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        //Get the identifier's number of the related pages
        final List<MultiTree> multitrees = APILocator.getMultiTreeAPI()
                .getMultiTreesByChild(identifier.getId());

        for (MultiTree multitree : multitrees) {
            //Get the Identifiers of the related pages
            final Identifier htmlPageIdentifier = APILocator.getIdentifierAPI()
                    .find(multitree.getParent1());
            Long languageId = -1L;
            IHTMLPage page = null;
            //Get the pages
            try {

                //Get the contenlet language in order to find the proper language page to invalidate
                languageId = contentlet.getLanguageId();
                //Search for the page with a given identifier and for a given language (in case of Pages as content)
                page = loadPageByIdentifier(htmlPageIdentifier.getId(), true, languageId,
                        APILocator.getUserAPI().getSystemUser(), false);

                if (null != page && page.isLive()) {
                    //Rebuild the pages' files
                    new PageLoader().invalidate(page);

                }
            } catch (Exception e) {
                final String htmlPageIdentifierId =
                        null != htmlPageIdentifier ? htmlPageIdentifier.getId() : null;
                final String pageInode = null != page ? page.getInode() : null;
                Logger.warn(this.getClass(),
                        "Cannot publish related HTML Pages" + ". htmlPageIdentifier.getId() = ["
                                + htmlPageIdentifierId
                                + "], languageId = [" + languageId + "], pageInode = [" + pageInode
                                + "]. This might happen if contents are being imported in batch.");
            }

        }

        // if it showOnMenu is checked changing publish status should remove nav on that folder
        if ((contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET
                || contentlet.getStructure().getStructureType()
                == Structure.STRUCTURE_TYPE_HTMLPAGE)
                && contentlet.getStringProperty("showOnMenu") != null
                && contentlet.getStringProperty("showOnMenu").contains("true")) {

            CacheLocator.getNavToolCache()
                    .removeNavByPath(identifier.getHostId(), identifier.getParentPath());
        }
    }

    @WrapInTransaction
    @Override
    public void cleanField(final Structure structure, final Date deletionDate, final Field oldField,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {
        if (!permissionAPI.doesUserHavePermission(structure, PermissionAPI.PERMISSION_PUBLISH, user,
                respectFrontendRoles)) {
            throw new DotSecurityException(
                    "Must be able to publish structure to clean all the fields with user: "
                            + (user != null ? user.getUserId() : "Unknown"));
        }

        com.dotcms.contenttype.model.field.Field field = new LegacyFieldTransformer(
                oldField).from();

        // Binary fields have nothing to do with database.
        if (field instanceof BinaryField) {
            int batchSize = 500;
            int offset = 0;
            final List<Contentlet> contentlets = new ArrayList<>();
            contentlets.addAll(
                    contentFactory.findByStructure(structure.getInode(), deletionDate, batchSize,
                            offset));
            while (!contentlets.isEmpty()) {
                final List<Contentlet> finalList = new ArrayList<>();
                finalList.addAll(contentlets);
                HibernateUtil.addCommitListener(() -> moveBinaryFilesToTrash(finalList, oldField));
                offset += batchSize;
                contentlets.clear();
                contentlets.addAll(
                        contentFactory.findByStructure(structure.getInode(), deletionDate,
                                batchSize, offset));
            }
        } else if (field instanceof TagField) {
            int batchSize = 500;
            int offset = 0;
            final List<Contentlet> contentlets = new ArrayList<>();
            contentlets.addAll(
                    contentFactory.findByStructure(structure.getInode(), deletionDate, batchSize,
                            offset));
            while (!contentlets.isEmpty()) {
                for (Contentlet contentlet : contentlets) {
                    tagAPI.deleteTagInodesByInodeAndFieldVarName(contentlet.getInode(),
                            field.variable());
                }
                offset += batchSize;
                contentlets.clear();
                contentlets.addAll(
                        contentFactory.findByStructure(structure.getInode(), deletionDate,
                                batchSize, offset));
            }
        } else if (field.dataType() == DataTypes.SYSTEM || field.dataType() == DataTypes.NONE) {
            return;
        } else {

            contentFactory.clearField(structure.getInode(), deletionDate, oldField);
        }
    }

    @Override
    @WrapInTransaction
    public void cleanField(Structure structure, Field oldField, User user,
            boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {
        cleanField(structure, null, oldField, user, respectFrontendRoles);
    }

    /**
     * Searches for a HTML Page with a given identifier and a given languageId, the languageId will
     * be use only the new HTML Pages (pages as content).
     *
     * @param ident
     * @param live
     * @param languageId
     * @param user
     * @param frontRoles
     * @return
     * @throws DotDataException
     * @throws DotContentletStateException
     * @throws DotSecurityException
     */
    private IHTMLPage loadPageByIdentifier(String ident, boolean live, Long languageId, User user,
            boolean frontRoles)
            throws DotDataException, DotContentletStateException, DotSecurityException {

        return APILocator.getHTMLPageAssetAPI().fromContentlet(
                this.findContentletByIdentifier(ident, live, languageId, user, frontRoles));

    }

    /**
     * @deprecated As of 2016-05-16, replaced by
     * {@link #loadPageByIdentifier(String, boolean, Long, User, boolean)}
     */
    private IHTMLPage loadPageByIdentifier(String ident, boolean live, User user,
            boolean frontRoles)
            throws DotDataException, DotContentletStateException, DotSecurityException {
        return loadPageByIdentifier(ident, live, 0L, user, frontRoles);
    }

    @RequestCost(Price.CONTENT_GET_REFERENCES)
    @CloseDBIfOpened
    @Override
    public List<Map<String, Object>> getContentletReferences(final Contentlet contentlet,
            final User user, final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotContentletStateException {

        final List<Map<String, Object>> results = new ArrayList<>();
        if (contentlet == null || !InodeUtils.isSet(contentlet.getInode())) {
            throw new DotContentletStateException("Contentlet must exist");
        }
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet");
        }

        final Identifier id = APILocator.getIdentifierAPI().find(contentlet);
        if (!InodeUtils.isSet(id.getId())) {
            return results;
        }

        final List<MultiTree> trees = APILocator.getMultiTreeAPI().getMultiTreesByChild(id.getId());
        for (final MultiTree tree : trees) {

            try {
                final IHTMLPage page = APILocator.getHTMLPageAssetAPI()
                        .findByIdLanguageFallback(tree.getHtmlPage(), contentlet.getLanguageId(),
                                false,
                                APILocator.getUserAPI().getSystemUser(), false);

                if (InodeUtils.isSet(page.getInode())) {

                    final Container container = APILocator.getContainerAPI()
                            .getWorkingContainerById(tree.getContainer(),
                                    APILocator.getUserAPI().getSystemUser(), false);

                    if (InodeUtils.isSet(container.getInode())) {
                        final String personaName = getPersonaNameByMultitree(tree);
                        final Map<String, Object> map = new HashMap<>();
                        map.put("page", page);
                        map.put("container", container);
                        map.put("persona", personaName);
                        results.add(map);
                    }
                }
            } catch (DoesNotExistException e) {
                Logger.debug(this, "Page not available in the requested language. This is ok");
            }
        }
        return results;
    }

    @Override
    public Optional<Integer> getAllContentletReferencesCount(final String contentletId)
            throws DotDataException {
        return Optional.of(this.multiTreeAPI.get().getAllContentletReferencesCount(contentletId));
    }

    private String getPersonaNameByMultitree(final MultiTree tree)
            throws DotSecurityException, DotDataException {
        final Supplier<String> defaultPersonaSupplier = () -> Try.of(() ->
                LanguageUtil.get(DEFAULT_PERSONA_NAME_KEY)).getOrElse("Default Visitor");

        String personaTag = Try.of(() -> tree.getPersonalization()
                        .substring((Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON).length()))
                .getOrElse("");
        Optional<Persona> personaOpt = APILocator.getPersonaAPI()
                .findPersonaByTag(personaTag,
                        APILocator.systemUser(), false);

        return personaOpt.isPresent() ? personaOpt.get().getName() : defaultPersonaSupplier.get();
    }

    @CloseDBIfOpened
    @Override
    public Object getFieldValue(final Contentlet contentlet,
            final com.dotcms.contenttype.model.field.Field theField) {
        return getFieldValue(contentlet, theField, null, false);
    }

    @CloseDBIfOpened
    @Override
    public Object getFieldValue(final Contentlet contentlet,
            final com.dotcms.contenttype.model.field.Field theField, final User user,
            final boolean respectFrontEndRoles) {
        try {
            User currentUser = user;
            if (currentUser == null) {
                currentUser = APILocator.getUserAPI().getSystemUser();
            }
            if (theField instanceof ConstantField) {
                contentlet.getMap().put(theField.variable(), theField.values());
                return theField.values();
            }
            if (theField instanceof HostFolderField) {
                if (FolderAPI.SYSTEM_FOLDER.equals(contentlet.getFolder())) {
                    return contentlet.getHost();
                } else {
                    return contentlet.getFolder();
                }
            } else if (theField instanceof CategoryField) {
                final Category categoryField = this.categoryAPI.find(theField.values(), currentUser,
                        respectFrontEndRoles);
                // Get all the Contentlets Categories
                final List<Category> selectedCategories = this.categoryAPI
                        .getParents(contentlet, currentUser, respectFrontEndRoles);
                if (selectedCategories.isEmpty()) {
                    return List.of();
                }
                final List<Category> availableCategories = this.categoryAPI
                        .getAllChildren(categoryField, APILocator.systemUser(), false);
                if (availableCategories.isEmpty()) {
                    return List.of();
                }
                selectedCategories.retainAll(availableCategories);

                return selectedCategories;


            } else if (theField instanceof RelationshipField) {
                final ContentletRelationships contentletRelationships = new ContentletRelationships(
                        contentlet);
                final Relationship relationship = this.relationshipAPI
                        .getRelationshipFromField(theField, currentUser);
                final boolean isChildField =
                        relationshipAPI.isChildField(relationship, theField);
                final ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                        relationship, isChildField);
                records.setRecords(contentlet
                        .getRelated(theField.variable(), currentUser, respectFrontEndRoles,
                                isChildField,
                                contentlet.getLanguageId(), null));
                contentletRelationships.setRelationshipsRecords(CollectionsUtils.list(records));
                return contentletRelationships;
            } else {
                return contentlet.get(theField.variable());
            }
        } catch (final DotDataException | DotSecurityException e) {
            throw new DotStateException(e);
        }
    }

    /**
     * @param theField a legacy field from the contentlet's parent Content Type
     * @deprecated Use
     * {@link ESContentletAPIImpl#getFieldValue(Contentlet,
     * com.dotcms.contenttype.model.field.Field)} instead
     */
    @CloseDBIfOpened
    @Override
    @Deprecated
    public Object getFieldValue(final Contentlet contentlet, final Field theField) {
        try {
            return getFieldValue(contentlet, new LegacyFieldTransformer(theField).from());
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    @WrapInTransaction
    @Override
    public void addLinkToContentlet(Contentlet contentlet, String linkInode, String relationName,
            User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        if (contentlet.getInode().equals("")) {
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }
        if (InodeUtils.isSet(linkInode)) {
            Link link = (Link) InodeFactory.getInode(linkInode, Link.class);
            Identifier identifier = APILocator.getIdentifierAPI().find(link);
            relationshipAPI.addRelationship(contentlet.getInode(), identifier.getInode(),
                    relationName);
            new ContentletLoader().invalidate(contentlet);
        }
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findPageContentlets(String HTMLPageIdentifier,
            String containerIdentifier, String orderby, boolean working, long languageId, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        List<Contentlet> contentlets = contentFactory.findPageContentlets(HTMLPageIdentifier,
                containerIdentifier, orderby, working, languageId);
        return permissionAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ,
                respectFrontendRoles, user);
    }

    /**
     * Get all relationships in a contentlet given its inode
     *
     * @param contentletInode
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public ContentletRelationships getAllRelationships(String contentletInode, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        return getAllRelationships(find(contentletInode, user, respectFrontendRoles));
    }

    /**
     * Get all relationships in a contentlet
     *
     * @param contentlet
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    @Override
    public ContentletRelationships getAllRelationships(final Contentlet contentlet)
            throws DotDataException {
        return getAllRelationships(contentlet, null);
    }

    /**
     * Get all relationships in a contentlet
     *
     * @param contentlet
     * @param cRelationships If this param is set, all relationships on this object will be ignored
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    private ContentletRelationships getAllRelationships(final Contentlet contentlet,
            ContentletRelationships cRelationships) throws DotDataException {

        if (cRelationships == null) {
            cRelationships = new ContentletRelationships(contentlet);
        }

        final ContentType contentType = contentlet.getContentType();
        final List<Relationship> relationships = APILocator.getRelationshipAPI()
                .byContentType(contentType);

        for (Relationship relationship : relationships) {

            final List relatedByRelationship = cRelationships.getRelationshipsRecords().stream()
                    .filter(record -> record.getRelationship().getInode()
                            .equals(relationship.getInode())).collect(
                            Collectors.toList());

            if (relatedByRelationship.size() == 0) {
                if (APILocator.getRelationshipAPI().sameParentAndChild(relationship)) {

                    //If it's a same structure kind of relationship we need to pull all related content
                    //on both roles as parent and a child of the relationship

                    //Pulling as child
                    pullRelated(contentlet, cRelationships, relationship, false);

                    //Pulling as parent
                    pullRelated(contentlet, cRelationships, relationship, true);

                } else {
                    pullRelated(contentlet, cRelationships, relationship,
                            APILocator.getRelationshipAPI()
                                    .isParent(relationship, contentType));
                }
            }
        }

        return cRelationships;
    }

    private void pullRelated(Contentlet contentlet, ContentletRelationships cRelationships,
            Relationship relationship, boolean hasParent)
            throws DotDataException {

        final boolean selfRelated = APILocator.getRelationshipAPI()
                .sameParentAndChild(relationship);

        final List<Contentlet> contentletList = new ArrayList<>();
        final ContentletRelationshipRecords records = cRelationships.new ContentletRelationshipRecords(
                relationship, hasParent);

        try {
            if (selfRelated) {
                contentletList.addAll(getRelatedContent(contentlet, relationship, hasParent,
                        APILocator.getUserAPI().getSystemUser(), true));
            } else {
                contentletList.addAll(getRelatedContent(contentlet, relationship,
                        APILocator.getUserAPI().getSystemUser(), true));
            }
        } catch (DotSecurityException e) {
            Logger.error(this, "Unable to get system user", e);
        }
        records.setRecords(contentletList);
        cRelationships.getRelationshipsRecords().add(records);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> getAllLanguages(Contentlet contentlet, Boolean isLiveContent, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet");
        }

        List<Contentlet> contentletList = null;

        if (isLiveContent != null) {
            contentletList = contentFactory.getContentletsByIdentifier(contentlet.getIdentifier(),
                    isLiveContent);
        } else {
            contentletList = contentFactory.getContentletsByIdentifier(contentlet.getIdentifier(),
                    null);
        }
        return contentletList;
    }

    @WrapInTransaction
    @Override
    public void unlock(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        if (contentlet == null) {

            throw new DotContentletStateException("The contentlet cannot Be null");
        }

        final String contentPushPublishDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE), ND_SUPPLIER);
        final String contentPushExpireDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE), ND_SUPPLIER);

        logInfo("Unlocking Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);

        try {
            canLock(contentlet, user, respectFrontendRoles);

            if (contentlet.isLocked()) {
                // persists the webasset
                APILocator.getVersionableAPI().setLocked(contentlet, false, user);
                ThreadContextUtil.ifReindex(() -> indexAPI.addContentToIndex(contentlet, false));
            }

        } catch (DotDataException | DotStateException | DotSecurityException e) {

            logInfo("Error Unlocking Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);
            throw e;
        }

        logInfo("Content Unlocked", contentlet, user, contentPushPublishDate, contentPushExpireDate);
    }

    @CloseDBIfOpened
    @Override
    public Identifier getRelatedIdentifier(Contentlet contentlet, String relationshipType,
            User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet");
        }
        return contentFactory.getRelatedIdentifier(contentlet, relationshipType);
    }

    @CloseDBIfOpened
    @Override
    public List<Link> getRelatedLinks(Contentlet contentlet, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return permissionAPI.filterCollection(contentFactory.getRelatedLinks(contentlet),
                PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    }


    @Deprecated
    @Override
    public List<Contentlet> filterRelatedContent(Contentlet contentlet, Relationship rel,
            User user, boolean respectFrontendRoles, Boolean pullByParent, int limit, int offset,
            String sortBy)
            throws DotDataException, DotSecurityException {
        Logger.warn(this,
                "This implementation of ContentletAPI.filterRelatedContent is deprecated and no longer respects the sort. "
                        + "Please, use any of the getRelatedContent implementations existing in the ContentletAPI instead ");
        return filterRelatedContent(contentlet, rel, user, respectFrontendRoles, pullByParent,
                limit, offset);
    }

    @VisibleForTesting
    @CloseDBIfOpened
    List<Contentlet> filterRelatedContent(Contentlet contentlet, Relationship rel,
            User user, boolean respectFrontendRoles, Boolean pullByParent, int limit, int offset)
            throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(contentlet.getIdentifier())) {
            return Collections.emptyList();
        }

        final boolean isSameStructureRelationship = APILocator.getRelationshipAPI()
                .sameParentAndChild(rel);

        if (isSameStructureRelationship) {
            if (pullByParent == null) {
                return (List<Contentlet>) CollectionsUtils
                        .join(getRelatedChildren(contentlet, rel, user, respectFrontendRoles, limit,
                                        offset),
                                getRelatedParents(contentlet, rel, user, respectFrontendRoles,
                                        limit, offset)).stream()
                        .filter(Objects::nonNull)
                        .collect(CollectionsUtils.toImmutableList());
            }
            if (pullByParent) {
                return getRelatedChildren(contentlet, rel, user, respectFrontendRoles, limit,
                        offset).stream()
                        .filter(Objects::nonNull)
                        .collect(CollectionsUtils.toImmutableList());
            } else {
                return getRelatedParents(contentlet, rel, user, respectFrontendRoles, limit, offset)
                        .stream().filter(Objects::nonNull)
                        .collect(CollectionsUtils.toImmutableList());
            }
        } else {
            if (rel.getChildStructureInode().equals(contentlet.getContentTypeId())) {
                return getRelatedParents(contentlet, rel, user, respectFrontendRoles, limit,
                        offset);
            } else {
                return getRelatedChildren(contentlet, rel, user, respectFrontendRoles, limit,
                        offset);
            }
        }
    }

    @RequestCost(Price.CONTENT_GET_RELATED)
    @Override
    public List<Contentlet> getRelatedContent(final Contentlet contentlet, final Relationship rel,
            final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        try {
            return getRelatedContent(contentlet, rel, null, user, respectFrontendRoles);
        } catch (final Exception e) {
            final String errorMessage = String.format("Related content for contentlet with " +
                            "identifier '%s', title '%s', Relationship Name '%s' was not found: %s",
                    contentlet.getIdentifier(), contentlet
                            .getTitle(), rel.getRelationTypeValue(), e.getMessage());
            if (e instanceof SearchPhaseExecutionException || e
                    .getCause() instanceof SearchPhaseExecutionException) {
                Logger.warnAndDebug(ESContentletAPIImpl.class,
                        errorMessage + ". An empty list will be returned", e);
                return Collections.emptyList();
            }
            throw new DotDataException(errorMessage, e);
        }
    }

    /**
     * Returns the list of child Contentlets related to the specified Contentlet based on a given
     * Relationship.
     *
     * @param contentlet           The {@link Contentlet} whose child Contentlets will be
     *                             retrieved.
     * @param rel                  The {@link Relationship} used to get the child Contentlets.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User executing this action has the front-end role, or if
     *                             front-end roles must be validated against this user, set to
     *                             {@code true}. Otherwise, set to {@code false}.
     * @param limitParam           The limit to the amount of results that will be returned.
     * @param offset               The offset applied to the result, mostly for pagination
     *                             purposes.
     * @return The list of {@link Contentlet} objects that are the children of the specified
     * Contentlet.
     * @throws DotSecurityException The user executing this action does not have the required
     *                              permissions.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    @RequestCost(Price.CONTENT_GET_RELATED)
    private List<Contentlet> getRelatedChildren(final Contentlet contentlet, final Relationship rel,
            final User user, final boolean respectFrontendRoles, final int limitParam,
            final int offset)
            throws DotSecurityException, DotDataException {
        final boolean HAS_PARENT = Boolean.TRUE;
        final boolean WORKING_VERSION = Boolean.FALSE;
        if (rel.isRelationshipField() && GET_RELATED_CONTENT_FROM_DB) {
            return FactoryLocator.getRelationshipFactory()
                    .dbRelatedContent(rel, contentlet, HAS_PARENT, WORKING_VERSION, "tree_order",
                            limitParam, offset);
        } else {

            final List<Contentlet> result = new ArrayList<>();
            final String relationshipName = rel.getRelationTypeValue().toLowerCase();
            final int limit = limitParam <= 0 ? MAX_LIMIT : limitParam;

            SearchResponse response;
            final boolean DONT_PULL_PARENTS = Boolean.FALSE;

            //Search for related content in existing contentlet
            if (UtilMethods.isSet(contentlet.getInode())) {
                response = APILocator.getEsSearchAPI()
                        .esSearchRelated(contentlet, relationshipName, DONT_PULL_PARENTS,
                                WORKING_VERSION, user,
                                respectFrontendRoles, limit, offset, null);
            } else {
                //Search for related content in other versions of the same contentlet
                response = APILocator.getEsSearchAPI()
                        .esSearchRelated(contentlet.getIdentifier(), relationshipName,
                                DONT_PULL_PARENTS, WORKING_VERSION, user,
                                respectFrontendRoles, limit, offset, null);
            }

            if (response.getHits() == null) {
                return result;
            }

            for (final SearchHit sh : response.getHits()) {
                final Map<String, Object> sourceMap = sh.getSourceAsMap();
                if (sourceMap.get(relationshipName) != null) {
                    List<String> relatedIdentifiers = ((ArrayList<String>) sourceMap.get(
                            relationshipName));

                    if (limit > 0 && offset >= 0 && (offset + limit) <= relatedIdentifiers.size()) {
                        relatedIdentifiers = ((ArrayList<String>) sourceMap.get(relationshipName))
                                .subList(offset, offset + limit);
                    }

                    relatedIdentifiers.stream().forEach(child -> {
                        try {
                            final Contentlet mappedContentlet = findContentletByIdentifierAnyLanguage(
                                    child, true);
                            if (null != mappedContentlet && UtilMethods.isSet(
                                    mappedContentlet.getIdentifier())) {
                                result.add(mappedContentlet);
                            } else {
                                Logger.warn(this, String.format(
                                        "Child Contentlet with ID '%s' for relationship '%s' " +
                                                "was not found. Verify that it exists in the database",
                                        child,
                                        rel.getRelationTypeValue()));
                            }
                        } catch (final Exception e) {
                            throw new RuntimeException(
                                    String.format("An error occurred when mapping Child related " +
                                            "content '%s': %s", child, e.getMessage()), e);
                        }
                    });
                }
            }
            return result;
        }

    }

    /**
     * Returns the list of parent Contentlets related to the specified Contentlet based on a given
     * Relationship.
     *
     * @param contentlet           The {@link Contentlet} whose parent Contentlets will be
     *                             retrieved.
     * @param rel                  The {@link Relationship} used to get the parent Contentlets.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User executing this action has the front-end role, or if
     *                             front-end roles must be validated against this user, set to
     *                             {@code true}. Otherwise, set to {@code false}.
     * @param limitParam           The limit to the amount of results that will be returned.
     * @param offset               The offset applied to the result, mostly for pagination
     *                             purposes.
     * @return The list of {@link Contentlet} objects that are the parents of the specified
     * Contentlet.
     * @throws DotSecurityException The user executing this action does not have the required
     *                              permissions.
     * @throws DotDataException     An error occurred when interacting with the data source.
     */
    @RequestCost(Price.CONTENT_GET_RELATED)
    private List<Contentlet> getRelatedParents(final Contentlet contentlet, final Relationship rel,
            final User user, final boolean respectFrontendRoles, int limitParam, final int offset)
            throws DotSecurityException, DotDataException {
        final boolean HAS_NO_PARENT = Boolean.FALSE;
        final boolean WORKING_VERSION = Boolean.FALSE;
        if (rel.isRelationshipField() && GET_RELATED_CONTENT_FROM_DB) {
            return FactoryLocator.getRelationshipFactory()
                    .dbRelatedContent(rel, contentlet, HAS_NO_PARENT, WORKING_VERSION, "tree_order",
                            limitParam, offset);
        } else {
            final Map<String, Contentlet> relatedMap = new HashMap<>();
            final String relationshipName = rel.getRelationTypeValue().toLowerCase();
            final int limit = limitParam <= 0 ? MAX_LIMIT : limitParam;

            SearchResponse response;
            final boolean PULL_PARENTS = Boolean.TRUE;

            //Search for related content in existing contentlet
            if (UtilMethods.isSet(contentlet.getInode())) {
                response = APILocator.getEsSearchAPI()
                        .esSearchRelated(contentlet, relationshipName, PULL_PARENTS,
                                WORKING_VERSION, user,
                                respectFrontendRoles, limit, offset, null);
            } else {
                response = APILocator.getEsSearchAPI()
                        .esSearchRelated(contentlet.getIdentifier(), relationshipName, PULL_PARENTS,
                                WORKING_VERSION, user,
                                respectFrontendRoles, limit, offset, null);
            }

            if (response.getHits() != null) {
                for (final SearchHit sh : response.getHits()) {
                    final Map<String, Object> sourceMap = sh.getSourceAsMap();
                    final String identifier = (String) sourceMap.get("identifier");
                    if (identifier != null && !relatedMap.containsKey(identifier)) {
                        final Contentlet mappedContentlet = findContentletByIdentifierAnyLanguage(
                                identifier, true);
                        if (null != mappedContentlet && UtilMethods.isSet(
                                mappedContentlet.getIdentifier())) {
                            relatedMap.put(identifier, mappedContentlet);
                        } else {
                            Logger.warn(this, String.format(
                                    "Parent Contentlet with ID '%s' for relationship '%s' was" +
                                            " not found. Verify that it exists in the database",
                                    identifier,
                                    rel.getRelationTypeValue()));
                        }
                    }
                }
            }

            return new ArrayList<>(relatedMap.values());
        }
    }

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, int limit, int offset,
            String sortBy) throws DotDataException {
        return getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, limit,
                offset, sortBy, -1, null);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> getRelatedContent(final Contentlet contentlet, final Relationship rel,
            final Boolean pullByParent, final User user, final boolean respectFrontendRoles,
            final int limit, final int offset,
            final String sortBy, final long language, final Boolean live)
            throws DotDataException {
        String fieldVariable = rel.getRelationTypeValue();
        try {
            if (rel.isRelationshipField()) {
                if ((relationshipAPI.sameParentAndChild(rel) && pullByParent != null
                        && pullByParent) || (relationshipAPI
                        .isParent(rel, contentlet.getContentType()) && !relationshipAPI
                        .sameParentAndChild(rel))) {
                    if (rel.getChildRelationName() != null) {
                        fieldVariable = rel.getChildRelationName();
                    }
                } else if (rel.getParentRelationName() != null) {
                    fieldVariable = rel.getParentRelationName();
                }
            }

            return getRelatedContent(contentlet, fieldVariable, user, respectFrontendRoles,
                    pullByParent, limit,
                    offset, sortBy, language, live);
        } catch (final Exception e) {
            final String id = contentlet != null ? contentlet.getIdentifier() : "null";
            final String relName = rel != null ? rel.getRelationTypeValue() : "null";
            final String errorMessage = String.format(
                    "Unable to look up related content for contentlet with " +
                            "identifier '%s', Relationship name '%s'(%s): %s", id, relName,
                    fieldVariable, e.getMessage());

            if (e instanceof SearchPhaseExecutionException || e
                    .getCause() instanceof SearchPhaseExecutionException) {
                Logger.warnAndDebug(ESContentletAPIImpl.class,
                        errorMessage + ". An empty list will be returned", e);
                return Collections.emptyList();
            }
            throw new DotDataException(errorMessage, e);
        }
    }


    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles, final long language,
            final Boolean live)
            throws DotDataException, DotSecurityException {
        return getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, -1, -1,
                null, language, live);
    }

    @Override
    public List<Contentlet> getRelatedContent(Contentlet contentlet, Relationship rel,
            Boolean pullByParent, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, -1, -1,
                null);
    }

    /**
     * @param contentlet
     * @param rel
     * @param pullByParent
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @deprecated Use
     * {@link ContentletAPI#getRelatedContent(Contentlet, Relationship, Boolean, User, boolean)}
     * instead
     */
    @Deprecated
    public List<Contentlet> getRelatedContentFromIndex(final Contentlet contentlet,
            final Relationship rel, final boolean pullByParent,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        try {
            return getRelatedContent(contentlet, rel, pullByParent, user, respectFrontendRoles, -1,
                    -1, null);
        } catch (final Exception e) {
            final String errorMessage = String.format(
                    "Unable to look up related content for contentlet from Index " +
                            "with identifier '%s', Relationship Name '%s': %s",
                    contentlet.getIdentifier(), rel.getRelationTypeValue
                            (), e.getMessage());
            if (e instanceof SearchPhaseExecutionException || e
                    .getCause() instanceof SearchPhaseExecutionException) {
                Logger.warnAndDebug(ESContentletAPIImpl.class,
                        errorMessage + ". An empty list will be returned", e);
                return Collections.emptyList();
            }
            throw new DotDataException(errorMessage, e);
        }
    }

    /**
     * check if a workflow may be run instead of the delete api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional boolean, present if the workflow ran with a result of the delete operation.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Boolean> checkAndRunDeleteAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, SystemAction.DELETE, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasDeleteActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has a delete contentlet actionlet"
                                + " so firing a workflow and skipping the current delete for the contentlet: "
                                + contentletIn.getIdentifier());

                contentletIn.setActionId(actionId);
                contentletIn.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);

                final WorkflowProcessor processor = workflowAPI.fireWorkflowPreCheckin(contentletIn,
                        user);
                workflowAPI.fireWorkflowPostCheckin(processor);
                if (processor.getContextMap().containsKey("deleted")) {

                    return Optional.ofNullable((Boolean) processor.getContextMap().get("deleted"));
                }

                return Optional.ofNullable(false);
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a delete contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    @RequestCost(Price.CONTENT_DELETE)
    @Override
    public boolean delete(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final Optional<Boolean> deleteOpt = this.checkAndRunDeleteAsWorkflow(contentlet, user,
                respectFrontendRoles);
        if (deleteOpt.isPresent()) {
            Logger.info(this, String.format("A delete workflow has been used to delete Contentlet ID " +
                    "'%s' instead of calling the APIs", contentlet.getIdentifier()));
            return deleteOpt.get();
        }

        boolean deleted;
        final List<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(contentlet);

        try {
            boolean isSite = contentlet.isHost();
            deleted = this.deleteContentlets(contentlets, user, respectFrontendRoles, isSite);
            HibernateUtil.addCommitListener
                    (() -> this.localSystemEventsAPI.notify(
                            new ContentletDeletedEvent<>(contentlet, user)));
        } catch (final DotDataException | DotSecurityException e) {
            this.logContentletActivity(contentlets, String.format("Error deleting content with ID " +
                    "'%s': %s", contentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e)), user);
            throw e;
        }

        return deleted;
    }

    @Override
    public boolean delete(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles, final boolean allVersions)
            throws DotDataException, DotSecurityException {

        final List<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(contentlet);

        try {

            this.delete(contentlets, user, respectFrontendRoles, allVersions);
            HibernateUtil.addCommitListener
                    (() -> this.localSystemEventsAPI.notify(
                            new ContentletDeletedEvent(contentlet, user)));
        } catch (DotDataException | DotSecurityException e) {
            logContentletActivity(contentlets, "Error Deleting Content", user);
            throw e;
        }

        return true;
    }


    @WrapInTransaction
    @Override
    public boolean deleteByHost(final Host host, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        final DotConnect db = new DotConnect();

        List<String> deleteMe = db.setSQL(
                        "select working_inode  from identifier, contentlet_version_info where identifier.id = contentlet_version_info.identifier and host_inode=? and asset_type='contentlet'")
                .addParam(host.getIdentifier())
                .setMaxRows(200)
                .loadObjectResults()
                .stream()
                .map(map -> (String) map.get("working_inode"))
                .collect(Collectors.toList());

        while (deleteMe.size() > 0) {
            final List<String> ids = deleteMe;
            LocalTransaction.wrap(() -> {

                try {

                    List<Contentlet> cons = findContentlets(ids);

                    destroy(cons, user, respectFrontendRoles);

                } catch (DotSecurityException e1) {
                    throw new DotStateException(e1);
                }
            });

            deleteMe = db.setSQL(
                            "select working_inode  from identifier, contentlet_version_info where identifier.id = contentlet_version_info.identifier and host_inode=? and asset_type='contentlet'")
                    .addParam(host.getIdentifier())
                    .setMaxRows(200)
                    .loadObjectResults()
                    .stream()
                    .map(map -> (String) map.get("working_inode"))
                    .collect(Collectors.toList());
        }
        return true;

    }

    @WrapInTransaction
    @Override
    public boolean delete(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        return deleteContentlets(contentlets, user, respectFrontendRoles, false);
    }

    /**
     * check if a workflow may be run instead of the destroy api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional boolean, present if the workflow ran with a result of the delete operation.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Boolean> checkAndRunDestroyAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, SystemAction.DESTROY, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a destroy action, we skip the current checkin
            if (workflowActionOpt.get().hasDestroyActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has a destroy contentlet actionlet"
                                + " so firing a workflow and skipping the current destroy for the contentlet: "
                                + contentletIn.getIdentifier());

                contentletIn.setActionId(actionId);
                contentletIn.setProperty(Contentlet.WORKFLOW_IN_PROGRESS, Boolean.TRUE);

                final WorkflowProcessor processor = workflowAPI.fireWorkflowPreCheckin(contentletIn,
                        user);
                workflowAPI.fireWorkflowPostCheckin(processor);
                if (processor.getContextMap().containsKey("destroy")) {

                    return Optional.ofNullable((Boolean) processor.getContextMap().get("destroy"));
                }

                return Optional.ofNullable(false);
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a destroy contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    @Override
    public boolean destroy(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final Optional<Boolean> deleteOpt = this.checkAndRunDestroyAsWorkflow(contentlet, user,
                respectFrontendRoles);
        if (deleteOpt.isPresent()) {

            Logger.info(this, "A Workflow has been ran instead of destroy the contentlet: " +
                    contentlet.getIdentifier());

            return deleteOpt.get();
        }

        final List<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(contentlet);
        try {

            return this.destroy(contentlets, user, respectFrontendRoles);
        } catch (DotDataException | DotSecurityException e) {

            this.logContentletActivity(contentlets, "Error Destroying Content", user);
            throw e;
        }
    }

    @RequestCost(Price.CONTENT_DELETE)
    @WrapInTransaction
    @Override
    public boolean destroy(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles) throws DotDataException,
            DotSecurityException {

        boolean destroyed = false;

        if (contentlets == null || contentlets.size() == 0) {

            Logger.info(this, "No contents passed to delete so returning");
            return false;
        }

        final List<Contentlet> contentletsToDelete = new ArrayList<>();
        for (final Contentlet contentlet : contentlets) {

            final Optional<Boolean> deleteOpt = this.checkAndRunDestroyAsWorkflow(contentlet, user,
                    respectFrontendRoles);
            if (deleteOpt.isEmpty()) {

                contentletsToDelete.add(contentlet);
            } else {

                Logger.info(this, "A Workflow has been ran instead of destroy the contentlet: " +
                        contentlet.getIdentifier());
                destroyed |= deleteOpt.get();
            }
        }

        return !contentletsToDelete.isEmpty() ?
                this.internalDestroy(contentletsToDelete, user, respectFrontendRoles) : destroyed;
    }

    private boolean internalDestroy(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        this.logContentletActivity(contentlets, "Destroying Content", user);

        for (final Contentlet contentlet : contentlets) {

            if (BLANK.equals(contentlet.getInode())) {

                this.logContentletActivity(contentlet, "Error Destroying Content", user);
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }

            final boolean bringOldVersions = false;  // we do not want old version in order to be more efficient
            final List<Contentlet> versions = this.findAllVersions(
                    APILocator.getIdentifierAPI().find(contentlet.getIdentifier()),
                    bringOldVersions,
                    user, respectFrontendRoles);

            for (final Contentlet version : versions) {
                this.canLock(version, user);
            }
        }

        final List<Contentlet> filterContentlets = this.permissionAPI.filterCollection(contentlets,
                PermissionAPI.PERMISSION_PUBLISH,
                respectFrontendRoles, user);

        if (filterContentlets.size() != contentlets.size()) {

            this.logContentletActivity(contentlets, "Error Destroying Content", user);
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have permission to destroy some or all of the contentlets");
        }

        return this.destroyContentlets(contentlets, user, respectFrontendRoles);
    }

    private void forceUnpublishArchive(final Contentlet contentlet, final User user)
            throws DotSecurityException, DotDataException {

        // Force unpublishing and archiving the contentlet
        try {
            if (contentlet.isLive()) {
                unpublish(contentlet, user, false, 0);
            }
            if (!contentlet.isArchived()) {
                archive(contentlet, user, false, true);
            }
        }
        // make destroy more robust if we cannot find ContentletVersionInfo
        // keep going
        catch (DotStateException e) {
            Logger.debug(this, e.getMessage());
        }
    }

    private void deleteRelationships(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        final List<Relationship> relationships =
                APILocator.getRelationshipAPI().byContentType(contentlet.getStructure());
        // Remove related contents
        for (final Relationship relationship : relationships) {
            deleteRelatedContent(contentlet, relationship, user, respectFrontendRoles);
        }
    }

    private void deleteMultitrees(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getMultiTreesByChild(contentlet.getIdentifier());

        for (final MultiTree multiTree : multiTrees) {

            final Identifier pageIdentifier = APILocator.getIdentifierAPI()
                    .find(multiTree.getHtmlPage());
            if (pageIdentifier != null && UtilMethods.isSet(pageIdentifier.getInode())) {

                try {

                    final IHTMLPage page = loadPageByIdentifier(pageIdentifier.getId(),
                            false, contentlet.getLanguageId(), user, false);
                    if (page != null && UtilMethods.isSet(page.getIdentifier())) {

                        new PageLoader().invalidate(page);
                    }
                } catch (DotStateException dcse) {

                    Logger.warn(this.getClass(),
                            "Page with id:" + pageIdentifier.getId() + " does not exist");
                }
            }

            APILocator.getMultiTreeAPI().deleteMultiTree(multiTree);
        }
    }

    /**
     * Completely destroys the given list of {@link Contentlet} objects (versions, relationships,
     * associated contents, binary files) in all of their languages.
     *
     * @param contentlets          - The list of contentlets that will be completely destroyed.
     * @param user                 - The {@link User} performing this action.
     * @param respectFrontendRoles -
     * @return If the contentlets were successfully destroyed, returns {@code true}. Otherwise,
     * returns {@code false}.
     * @throws DotDataException     An error occurred when deleting the information from the
     *                              database.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this action.
     */
    private boolean destroyContentlets(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        boolean noErrors = true;
        final List<Contentlet> contentletsVersion = new ArrayList<>();
        // Log contentlet identifiers that we are going to destroy
        AdminLogger.log(this.getClass(), "destroy",
                "User trying to destroy the following contents: " +
                        contentlets.stream().map(Contentlet::getIdentifier)
                                .collect(Collectors.toSet()), user);
        final Iterator<Contentlet> contentletIterator = contentlets.iterator();
        while (contentletIterator.hasNext()) {

            final Contentlet contentlet = contentletIterator.next();
            contentlet.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
            this.forceUnpublishArchiveOnDestroy(user, contentlet);
            APILocator.getWorkflowAPI()
                    .deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, user);

            // Remove Rules with this contentlet as Parent.
            try {

                APILocator.getRulesAPI()
                        .deleteRulesByParent(contentlet, user, respectFrontendRoles);
            } catch (InvalidLicenseException ilexp) {

                Logger.warn(this, "An enterprise license is required to delete rules under pages.");
            }

            // Remove category associations
            this.categoryAPI.removeChildren(contentlet, APILocator.getUserAPI().getSystemUser(),
                    true);
            this.categoryAPI.removeParents(contentlet, APILocator.getUserAPI().getSystemUser(),
                    true);
            this.deleteRelationships(contentlet, user, respectFrontendRoles);

            contentletsVersion.addAll(
                    findAllVersions(APILocator.getIdentifierAPI().find(contentlet.getIdentifier()),
                            user,
                            respectFrontendRoles));
            contentletsVersion.forEach(contentletLanguage -> contentletLanguage.setIndexPolicy(
                    contentlet.getIndexPolicy()));
            // Remove page contents (if the content is a Content Page)
            this.deleteMultitrees(contentlet, user);
            this.logContentletActivity(contentlet, "Content Destroyed", user);
        }

        this.backupDestroyedContentlets(contentlets, user);

        // Delete all the versions of the contentlets to delete
        this.contentFactory.delete(contentletsVersion);
        // Remove the contentlets from the Elastic index and cache
        for (final Contentlet contentlet : contentletsVersion) {

            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        this.deleteBinaryFiles(contentletsVersion, null);
        this.deleteElementFromPublishQueueTable(contentlets);

        return noErrors;
    }

    /**
     * at destroying/deleting time this will take care of removing all metadata entries
     *
     * @param contentlets
     */
    private void destroyMetadata(final List<Contentlet> contentlets) {
        for (final Contentlet contentlet : contentlets) {
            fileMetadataAPI.removeMetadata(contentlet);
            Logger.debug(ESContentletAPIImpl.class,
                    String.format("metadata removed for %s", contentlet.getIdentifier()));
        }
        Logger.debug(ESContentletAPIImpl.class,
                String.format("Done removing metadata for %d elements.", contentlets.size()));
    }

    private void forceUnpublishArchiveOnDestroy(final User user, final Contentlet contentlet)
            throws DotSecurityException, DotDataException {

        // it could be into a step, so we do not want to move.
        final Optional<Boolean> disableWorkflowOpt = this.getDisableWorkflow(contentlet);
        contentlet.getMap().put(Contentlet.DISABLE_WORKFLOW, true);
        this.forceUnpublishArchive(contentlet, user);

        contentlet.getMap().put(Contentlet.DISABLE_WORKFLOW, disableWorkflowOpt.isPresent() ?
                disableWorkflowOpt.get() : null);
    }

    private Optional<Boolean> getDisableWorkflow(final Contentlet contentlet) {

        if (!contentlet.getMap().containsKey(Contentlet.DISABLE_WORKFLOW)) {

            return Optional.empty();
        }

        return Optional.ofNullable((boolean) contentlet.getMap().get(Contentlet.DISABLE_WORKFLOW));
    }

    private void deleteElementFromPublishQueueTable(final List<Contentlet> contentlets) {

        for (final Contentlet contentlet : contentlets) {

            try {

                PublisherAPI.getInstance()
                        .deleteElementFromPublishQueueTable(contentlet.getIdentifier());
            } catch (DotPublisherException e) {
                Logger.error(getClass(),
                        "Error destroying Contentlet from Publishing Queue with Identifier: "
                                + contentlet.getIdentifier());
                Logger.debug(getClass(),
                        "Error destroying Contentlet from Publishing Queue with Identifier: "
                                + contentlet.getIdentifier(),
                        e);
            }
        }
    }

    private void backupDestroyedContentlets(final List<Contentlet> contentlets, final User user) {
        if(!Config.getBooleanProperty("BACKUP_DELETED_CONTENTLETS_TO_DISK", false)){
            return;
        }
        if (contentlets.size() > 0) {

            final XStream xstream = XStreamHandler.newXStreamInstance();
            final File backupFolder = new File(backupPath);
            if (!backupFolder.exists()) {

                backupFolder.mkdirs();
            }

            for (final Contentlet contentlet : contentlets) {

                final Structure structure = contentlet.getStructure();
                final List<Field> fields = structure.getFields();
                final List<File> filelist = new ArrayList<>();

                File file = null;
                for (final Field field : fields) {

                    if (field.getFieldType().equals(FieldType.BINARY.toString())) {
                        try {

                            file = getBinaryFile(contentlet.getInode(), field.getVelocityVarName(),
                                    user);
                        } catch (Exception ex) {
                            Logger.debug(this, ex.getMessage(), ex);
                        }

                        if (file != null) {

                            filelist.add(file);
                        }
                    }
                }

                final File filePath = new File(backupPath + File.separator
                        + contentlet.getIdentifier());
                filePath.mkdirs();

                final File _writingwbin = new File(filePath,
                        contentlet.getIdentifier().toString() + ".xml");

                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream
                        (Files.newOutputStream(_writingwbin.toPath()))) {

                    xstream.toXML(contentlet, bufferedOutputStream);
                    for (final File fileChild : filelist) {

                        final File child = new File(filePath, fileChild.getName());
                        FileUtil.move(fileChild, child);
                    }
                } catch (IOException e) {
                    Logger.error(this,
                            "Error processing the file for contentlet with Identifier: "
                                    + contentlet.getIdentifier(), e);
                }
            }
        }
    } // backupDestroyedContentlets.

    /**
     * Deletes the specified list of {@link Contentlet} objects ONLY in the specified language. If
     * any of the specified contentlets is not archived, an exception will be thrown. If there's
     * only one language for a given contentlet, the object will be destroyed.
     *
     * @param contentlets          The list of contentlets that will be deleted.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User executing this action has the front-end role, or if
     *                             front-end roles must be validated against this user, set to
     *                             {@code true}.
     * @param isDeletingAHost      If the code calling this method is trying to delete a given
     *                             Site (host), set to {@code true}. Otherwise, set to
     *                             {@code false}.
     * @return If the contentlets were successfully deleted, returns {@code true}. Otherwise,
     * returns {@code false}.
     * @throws DotDataException     An error occurred when deleting the information from the
     *                              database.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this action.
     * @throws DotStateException    One of the specified contentlets is not archived.
     */
    private boolean deleteContentlets(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles, final boolean isDeletingAHost)
            throws DotDataException,
            DotSecurityException {

        boolean noErrors = true;

        if (UtilMethods.isNotSet(contentlets)) {
            Logger.info(this, "No contents passed to delete");
            noErrors = false;
            return noErrors;
        }

        this.logContentletActivity(contentlets, "Deleting Contents", user);

        final List<Contentlet> filteredContentlets = this.validateAndFilterContentletsToDelete(
                contentlets, user, respectFrontendRoles);

        if (filteredContentlets.size() != contentlets.size()) {

            this.logContentletActivity(contentlets, "Error Deleting Contents.", user);
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have permission to delete some or all of the contentlets");
        }

        // Log contentlet identifiers that we are going to delete
        final HashSet<String> contentletIdentifiers = new HashSet<>();
        for (final Contentlet contentlet : contentlets) {

            contentletIdentifiers.add(contentlet.getIdentifier());
        }

        AdminLogger.log(this.getClass(), "delete", "User trying to delete the following contents: "
                + contentletIdentifiers, user);

        final HashSet<String> deletedIdentifiers = new HashSet<>();
        for (final Contentlet filteredContentlet : filteredContentlets) {

            this.deleteContentlet(contentlets, user, isDeletingAHost,
                    deletedIdentifiers, filteredContentlet);
        }

        return noErrors;
    }

    /**
     * Deletes the specified list of Contentlets.
     *
     * @param contentlets        The complete list of {@link Contentlet} objects to delete.
     * @param user               The {@link User} that is trying to delete such Contentlets.
     * @param isDeletingAHost    If the Contentlet being deleted is a Site, set this to {@code true}
     *                           so that the "destroy" method is called directly, with no
     *                           validations.
     * @param deletedIdentifiers A list that keeps track of the Identifiers that have already been
     *                           deleted.
     * @param contentletToDelete The actual {@link Contentlet} that is being deleted.
     *
     * @throws DotDataException     The specified {@link User} does not have the required
     *                              permissions to perform this action.
     * @throws DotSecurityException An error occurred when deleting the information from the
     *                              database.
     */
    private void deleteContentlet(final List<Contentlet> contentlets, final User user,
            final boolean isDeletingAHost,
            final HashSet<String> deletedIdentifiers, final Contentlet contentletToDelete)
            throws DotDataException, DotSecurityException {

        //If we are deleting a Site/Host, we can call directly the destroy method.
        //No need to validate anything.
        if (isDeletingAHost) {
            //We need to make sure that we only destroy a identifier once.
            //If the contentlet has several languages we could send same identifier several times.
            if (!deletedIdentifiers.contains(contentletToDelete.getIdentifier())) {

                contentletToDelete.setProperty(Contentlet.DONT_VALIDATE_ME, true);
                this.destroyContentlets(Lists.newArrayList(contentletToDelete), user, false);
            }
        } else {

            if (contentletToDelete.isHTMLPage()) {
                unlinkRelatedContentType(user, contentletToDelete);
            }

            //If we are not deleting a site, the course of action will depend
            // on the amount of languages of each contentlet.

            // Find all multi-language working contentlets
            final List<Contentlet> otherLanguageCons = this.contentFactory.getContentletsByIdentifier(
                    contentletToDelete.getIdentifier());
            if (otherLanguageCons.size() == 1) {

                this.destroyContentlets(Lists.newArrayList(contentletToDelete), user, false);
            } else if (otherLanguageCons.size() > 1) {

                if (!contentletToDelete.isArchived()
                        && contentletToDelete.getMap().get(Contentlet.DONT_VALIDATE_ME) == null) {

                    this.logContentletActivity(contentletToDelete, "Error Deleting Content", user);
                    final String errorMsg = "Contentlet with Inode " + contentletToDelete.getInode()
                            + " cannot be deleted because it's not archived. Please archive it first before deleting it.";
                    Logger.error(this, errorMsg);
                    APILocator
                            .getNotificationAPI()
                            .generateNotification(errorMsg, NotificationLevel.INFO,
                                    user.getUserId());
                    throw new DotStateException(errorMsg);
                }

                //TODO we still have several things that need cleaning here:
                //TODO https://github.com/dotCMS/core/issues/9146
                final Identifier identifier = APILocator.getIdentifierAPI()
                        .find(contentletToDelete.getIdentifier());
                final List<Contentlet> allVersionsList = this.findAllVersions(identifier, user,
                        false);
                final List<Contentlet> contentletsLanguageList = allVersionsList.stream().
                        filter(contentlet -> contentlet.getLanguageId()
                                == contentletToDelete.getLanguageId())
                        .collect(Collectors.toList());
                contentletsLanguageList.forEach(
                        contentletLanguage -> contentletLanguage.setIndexPolicy(
                                contentletToDelete.getIndexPolicy()));
                this.contentFactory.delete(contentletsLanguageList, false);

                for (final Contentlet contentlet : contentlets) {

                    try {

                        PublisherAPI.getInstance()
                                .deleteElementFromPublishQueueTable(contentlet.getIdentifier(),
                                        contentlet.getLanguageId());
                    } catch (DotPublisherException e) {

                        Logger.error(getClass(),
                                "Error deleting Contentlet from Publishing Queue with Identifier: "
                                        + contentlet.getIdentifier());
                        Logger.debug(getClass(),
                                "Error deleting Contentlet from Publishing Queue with Identifier: "
                                        + contentlet.getIdentifier(), e);
                    }
                }
            }
        }

        deletedIdentifiers.add(contentletToDelete.getIdentifier());
        this.sendDeleteEvent(contentletToDelete);
    }

    private List<Contentlet> validateAndFilterContentletsToDelete(
            final List<Contentlet> contentlets,
            final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        for (final Contentlet contentlet : contentlets) {

            if (!contentlet.isArchived() && contentlet.validateMe()) {

                throw new DotContentletStateException(
                        getLocalizedMessageOrDefault(user, "Failed-to-delete-unarchived-content",
                                FAILED_TO_DELETE_UNARCHIVED_CONTENT, getClass())
                );
            }

            if (contentlet.getInode().equals("")) {

                logContentletActivity(contentlet, "Error Deleting Content", user);
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }

            this.canLock(contentlet, user);
        }

        return this.permissionAPI.filterCollection(contentlets,
                PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
    }

    private void sendDeleteEvent(final Contentlet contentlet) throws DotHibernateException {
        HibernateUtil.addCommitListener(
                () -> this.contentletSystemEventUtil.pushDeleteEvent(contentlet), 1000);
    }

    /**
     * Verifies if a page is being used as a detail page for any content type
     *
     * @param user
     * @param c
     * @throws DotDataException
     * @throws LanguageException
     */
    private void unlinkRelatedContentType(User user, Contentlet c)
            throws DotDataException {
        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        List<ContentType> relatedContentTypes = contentTypeAPI.search(
                "page_detail='" + c.getIdentifier() + "'");
        HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
        String uri = htmlPageAssetAPI.fromContentlet(c).getURI();
        //Verifies if the page is related to any content type
        if (UtilMethods.isSet(relatedContentTypes)) {

            //Unlinking url map and detail page
            relatedContentTypes.forEach((ContentType contentType) -> {
                try {
                    contentTypeAPI.unlinkPageFromContentType(contentType);
                } catch (DotSecurityException | DotDataException e) {
                    throw new RuntimeException(e);
                }
            });

            StringBuilder relatedPagesMessage = new StringBuilder();
            try {
                relatedPagesMessage.append(UtilMethods.escapeSingleQuotes(LanguageUtil.get(user,
                        "HTML-Page-related-content-type-delete-warning")));
            } catch (LanguageException e) {
                Logger.warn(this, e.getMessage());
            }

            relatedPagesMessage.append(relatedContentTypes.stream()
                    .map((ContentType t) -> t.name() + " - Detail Page: " + uri)
                    .collect(Collectors.joining("<br/>")));

            Logger.warn(this, relatedPagesMessage.toString());
        }
    }

    @WrapInTransaction
    @Override
    public void deleteAllVersionsandBackup(List<Contentlet> contentlets, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        if (contentlets == null || contentlets.size() == 0) {
            Logger.info(this, "No contents passed to delete so returning");
            return;
        }
        for (Contentlet con : contentlets) {
            if (con.getInode().equals("")) {
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }
        }
        List<Contentlet> perCons = permissionAPI.filterCollection(contentlets,
                PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
        List<Contentlet> contentletsVersion = new ArrayList<>();
        contentletsVersion.addAll(contentlets);

        if (perCons.size() != contentlets.size()) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have permission to delete some or all of the contentlets");
        }
        for (Contentlet con : contentlets) {
            categoryAPI.removeChildren(con, APILocator.getUserAPI().getSystemUser(), true);
            categoryAPI.removeParents(con, APILocator.getUserAPI().getSystemUser(), true);
            List<Relationship> rels = APILocator.getRelationshipAPI()
                    .byContentType(con.getStructure());
            for (Relationship relationship : rels) {
                deleteRelatedContent(con, relationship, user, respectFrontendRoles);
            }

            contentletsVersion.addAll(
                    findAllVersions(APILocator.getIdentifierAPI().find(con.getIdentifier()), user,
                            respectFrontendRoles));
        }

        List<String> contentletInodes = new ArrayList<>();
        for (Iterator<Contentlet> iter = contentletsVersion.iterator(); iter.hasNext(); ) {
            Contentlet element = iter.next();
            contentletInodes.add(element.getInode());
        }

        contentFactory.delete(contentletsVersion);

        for (Contentlet contentlet : perCons) {
            indexAPI.removeContentFromIndex(contentlet);
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        backupDestroyedContentlets(contentlets, APILocator.systemUser());
        deleteBinaryFiles(contentletsVersion, null);

    }

    @WrapInTransaction
    @Override
    public void delete(List<Contentlet> contentlets, User user, boolean respectFrontendRoles,
            boolean allVersions) throws DotDataException, DotSecurityException {
        for (Contentlet con : contentlets) {
            if (con.getInode().equals("")) {
                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }
            if (!canLock(con, user)) {
                throw new DotContentletStateException(
                        "Content Object is locked and cannot be deleted:" + con.getIdentifier());
            }
        }
        List<Contentlet> perCons = permissionAPI.filterCollection(contentlets,
                PermissionAPI.PERMISSION_PUBLISH, respectFrontendRoles, user);
        List<Contentlet> contentletsVersion = new ArrayList<>();
        contentletsVersion.addAll(contentlets);

        if (perCons.size() != contentlets.size()) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have permission to delete some or all of the contentlets");
        }
        for (Contentlet con : contentlets) {
            categoryAPI.removeChildren(con, APILocator.getUserAPI().getSystemUser(), true);
            categoryAPI.removeParents(con, APILocator.getUserAPI().getSystemUser(), true);
            List<Relationship> rels = APILocator.getRelationshipAPI()
                    .byContentType(con.getStructure());
            for (Relationship relationship : rels) {
                deleteRelatedContent(con, relationship, user, respectFrontendRoles);
            }

        }

        List<String> contentletInodes = new ArrayList<>();
        for (Iterator<Contentlet> iter = contentletsVersion.iterator(); iter.hasNext(); ) {
            Contentlet element = iter.next();
            contentletInodes.add(element.getInode());
        }

        contentFactory.delete(contentletsVersion);

        for (Contentlet contentlet : perCons) {
            indexAPI.removeContentFromIndex(contentlet);
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
        }

        deleteBinaryFiles(contentletsVersion, null);

    }

    @WrapInTransaction
    @Override
    public void deleteVersion(Contentlet contentlet, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        if (contentlet == null) {
            Logger.info(this, "No contents passed to delete so returning");
            return;
        }
        if (contentlet.getInode().equals("")) {
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH,
                user)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have permission to delete some or all of the contentlets");
        }

        ArrayList<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(contentlet);
        contentFactory.deleteVersion(contentlet);

        Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI()
                .getContentletVersionInfo(
                        contentlet.getIdentifier(), contentlet.getLanguageId());

        if (cinfo.isPresent() && (cinfo.get().getWorkingInode().equals(contentlet.getInode()) ||
                (InodeUtils.isSet(cinfo.get().getLiveInode())
                        && cinfo.get().getLiveInode().equals(contentlet.getInode())))) {
            // we remove from index if it is the working or live version
            indexAPI.removeContentFromIndex(contentlet);
        }

        CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);

        deleteBinaryFiles(contentlets, null);

        fileMetadataAPI.removeVersionMetadata(contentlet);

    }

    @WrapInTransaction
    @Override
    public void archive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        archive(contentlet, user, respectFrontendRoles, false);
    }

    /**
     * check if a workflow may be run instead of the archive api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional Contentlet, present is the workflow ran and returns the contentlet archived.
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Contentlet> checkAndRunArchiveAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, SystemAction.ARCHIVE, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasArchiveActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has an archive contentlet actionlet"
                                + " so firing a workflow and skipping the current archive for the contentlet: "
                                + contentletIn.getIdentifier());

                return Optional.ofNullable(workflowAPI.fireContentWorkflow(contentletIn,
                        new ContentletDependencies.Builder().workflowActionId(actionId)
                                .modUser(user)
                                .respectAnonymousPermissions(respectFrontendRoles)
                                .build()
                ));
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a archive contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    private void archive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles, final boolean isDestroy)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        this.logContentletActivity(contentlet, "Archiving Content", user);

        try {

            if (contentlet.getInode().equals(BLANK)) {

                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }

            final Optional<Contentlet> contentletOpt = this.checkAndRunArchiveAsWorkflow(contentlet,
                    user, respectFrontendRoles);
            if (contentletOpt.isPresent()) {

                Logger.info(this, "A Workflow has been ran instead of archive the contentlet: " +
                        contentlet.getIdentifier());
                if (!contentlet.getInode().equals(contentletOpt.get().getInode())) {
                    this.copyProperties(contentlet, contentletOpt.get().getMap());
                }
                return;
            }

            internalArchive(contentlet, user, respectFrontendRoles, isDestroy);
        } catch (DotDataException | DotStateException | DotSecurityException e) {

            final String errorMsg =
                    "Error archiving content with Identifier [" + contentlet.getIdentifier() + "]: "
                            + e.getMessage();
            Logger.warn(this, errorMsg);
            logContentletActivity(contentlet, errorMsg, user);
            throw e;
        }

        logContentletActivity(contentlet, "Content Archived", user);
    }

    private void internalArchive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles,
            final boolean isDestroy) throws DotDataException, DotSecurityException {

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user,
                respectFrontendRoles)) {

            throw new DotSecurityException(
                    "User: " + (user != null ? user.getUserId() : "Unknown") + " does not " +
                            "have permission to edit the contentlet with Identifier ["
                            + contentlet.getIdentifier() + "]");
        }

        final IndexPolicy indexPolicy = contentlet.getIndexPolicy();
        final IndexPolicy indexPolicyDependencies = contentlet.getIndexPolicyDependencies();
        final Contentlet workingContentlet = findContentletByIdentifier(contentlet.getIdentifier(),
                false, contentlet.getLanguageId(), contentlet.getVariantId(), user,
                respectFrontendRoles);

        if (workingContentlet == null) {
            return;
        }

        Contentlet liveContentlet = null;

        try {

            liveContentlet = findContentletByIdentifier(contentlet.getIdentifier(), true,
                    contentlet.getLanguageId(), user, respectFrontendRoles);
        } catch (DotContentletStateException ce) {

            Logger.debug(this,
                    "No live contentlet found for identifier = " + contentlet.getIdentifier());
        }

        this.canLock(contentlet, user);
        final User modUser = getModUser(workingContentlet);

        if (modUser != null) {

            workingContentlet.setModUser(modUser.getUserId());
        }

        // If the user calling this method is System, no other condition is required.
        // Note: no need to validate this on DELETE SITE/HOST.
        if (contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME) != null || this.canLock(contentlet,
                user)) {

            this.internalArchive(contentlet, user, respectFrontendRoles, isDestroy, indexPolicy,
                    indexPolicyDependencies, workingContentlet, liveContentlet);
        } else {

            throw new DotContentletStateException(
                    "Contentlet with Identifier '" + contentlet.getIdentifier() +
                            "' must be unlocked before being archived");
        }
    } // internalArchive.

    private void internalArchive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles,
            final boolean isDestroy, final IndexPolicy indexPolicy,
            final IndexPolicy indexPolicyDependencies,
            final Contentlet workingContentlet, final Contentlet liveContentlet)
            throws DotDataException, DotSecurityException {

        if (liveContentlet != null && InodeUtils.isSet(liveContentlet.getInode())) {

            APILocator.getVersionableAPI().removeLive(liveContentlet);

            if (!isDestroy) {

                this.indexAPI.removeContentFromLiveIndex(liveContentlet);
            }
        }


        // sets deleted to true
        APILocator.getVersionableAPI().setDeleted(workingContentlet, true);

        // Updating lucene index
        workingContentlet.setIndexPolicy(indexPolicy);
        workingContentlet.setIndexPolicyDependencies(indexPolicyDependencies);
        if (!isDestroy) {

            this.indexAPI.addContentToIndex(workingContentlet);
        }

        this.archiveFileAsset(contentlet, user, respectFrontendRoles);

        new ContentletLoader().invalidate(contentlet);

        this.publishRelatedHtmlPages(contentlet);
        CacheLocator.getContentletCache().remove(contentlet.getInode());
        if (contentlet.isHTMLPage()) {

            CacheLocator.getHTMLPageCache().remove(contentlet.getInode());
        }
        final Set<String> inodes = Stream.of(contentlet).map(Contentlet::getInode).collect(Collectors.toSet());
        updateModDate(inodes, user);
        invalidateLanguageVariableCache(contentlet);
        HibernateUtil.addCommitListener(
                () -> this.contentletSystemEventUtil.pushArchiveEvent(workingContentlet), 1000);
        HibernateUtil.addCommitListener(() -> localSystemEventsAPI.notify(
                new ContentletArchiveEvent(contentlet, user, true)));
    } // internalArchive.

    private void archiveFileAsset(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {

            final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
            CacheLocator.getCSSCache().remove(identifier.getHostId(), identifier.getPath(), true);
            CacheLocator.getCSSCache().remove(identifier.getHostId(), identifier.getPath(), false);
            //remove from navtoolcache
            final IFileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
            if (fileAsset.isShowOnMenu()) {

                final Folder folder = APILocator.getFolderAPI()
                        .findFolderByPath(identifier.getParentPath(),
                                identifier.getHostId(), user, respectFrontendRoles);
                RefreshMenus.deleteMenu(folder);
                CacheLocator.getNavToolCache().removeNav(identifier.getHostId(), folder.getInode());
            }
        }
    } // archiveFileAsset.

    private User getModUser(final Contentlet workingContentlet) {

        User modUser = null;

        try {

            modUser = APILocator.getUserAPI().loadUserById(workingContentlet.getModUser(),
                    APILocator.systemUser(), false);
        } catch (Exception ex) {

            if (ex instanceof NoSuchUserException) {

                modUser = APILocator.systemUser();
            }
        }
        return modUser;
    } // getModUser.

    // todo: everything should be in a transaction>????
    @Override
    public void archive(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        boolean stateError = false;
        for (final Contentlet contentlet : contentlets) {
            try {

                this.archive(contentlet, user, respectFrontendRoles);
            } catch (DotContentletStateException e) {

                Logger.error(this, e.getMessage(), e);
                stateError = true;
            }
        }

        if (stateError) {

            throw new DotContentletStateException(
                    "Unable to archive contentlets because one or more are locked");
        }

    }

    // Most of operations here are checking if the user can lock or permission.
    // the Transaction will place only on the setLocked, the rest of the method is ok to be just closeable.
    @CloseDBIfOpened
    @Override
    public void lock(final Contentlet contentlet, final User user, boolean respectFrontendRoles)
            throws DotContentletStateException, DotDataException, DotSecurityException {

        if (contentlet == null) {

            throw new DotContentletStateException("The contentlet cannot Be null");
        }

        final String contentPushPublishDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE), ND_SUPPLIER);
        final String contentPushExpireDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE), ND_SUPPLIER);

        logInfo("Locking Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);

        try {

            if (BLANK.equals(contentlet.getInode())) {

                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }

            canLock(contentlet, user);

            // persists the webasset
            APILocator.getVersionableAPI().setLocked(contentlet, true, user);
            ThreadContextUtil.ifReindex(() -> indexAPI.addContentToIndex(contentlet, false));
        } catch (DotDataException | DotStateException | DotSecurityException e) {
            logInfo("Error Locking Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);
            throw e;
        }

        logInfo("Content Locked", contentlet, user, contentPushPublishDate, contentPushExpireDate);
    }

    @Override
    public void reindex() throws DotReindexStateException {
        refreshAllContent();
    }

    @WrapInTransaction
    @Override
    public void reindex(Structure structure) throws DotReindexStateException {
        try {
            final ContentTypeTransformer contentTypeTransformer = new StructureTransformer(
                    structure);
            reindexQueueAPI.addStructureReindexEntries(contentTypeTransformer.from());
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex: " + e.getMessage(), e);
        }
    }

    @Override
    public void reindex(Contentlet contentlet) throws DotReindexStateException, DotDataException {
        indexAPI.addContentToIndex(contentlet);
    }

    @WrapInTransaction
    @Override
    public void refresh(Structure structure) throws DotReindexStateException {
        try {
            final ContentTypeTransformer contentTypeTransformer = new StructureTransformer(
                    structure);
            reindexQueueAPI.addStructureReindexEntries(contentTypeTransformer.from());
            //CacheLocator.getContentletCache().clearCache();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex: " + e.getMessage(), e);
        }

    }

    @WrapInTransaction
    @Override
    public void refresh(ContentType type) throws DotReindexStateException {
        try {
            reindexQueueAPI.addStructureReindexEntries(type);
            //CacheLocator.getContentletCache().clearCache();
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex: " + e.getMessage(), e);
        }

    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentToVariant(final Contentlet contentlet, final String variantName,
            final User user) throws DotDataException, DotSecurityException {

        final Contentlet contentletFromDataBase = find(contentlet.getInode(), user, false);

        final Contentlet checkout = checkout(contentletFromDataBase.getInode(), user, false);
        checkout.setVariantId(variantName);

        return Try.of(() -> checkin(checkout, user, false))
                 .getOrElseThrow(
                        e -> new DotStateException("Unable to create a new version from Contentlet:" + contentlet.getInode(), e));
    }


    /**
     * @param contentlet
     * @throws DotReindexStateException
     * @throws DotDataException
     */
    private void refreshNoDeps(final Contentlet contentlet) throws DotReindexStateException,
            DotDataException {
        indexAPI.addContentToIndex(contentlet, false);

    }

    @CloseDBIfOpened
    @Override
    public void refresh(Contentlet contentlet) throws DotReindexStateException,
            DotDataException {
        indexAPI.addContentToIndex(contentlet);

    }

    @CloseDBIfOpened
    @Override
    public void refreshAllContent() throws DotReindexStateException {
        try {
            if (indexAPI.isInFullReindex()) {
                return;
            }
            // we prepare the new index and aliases to point both old and new
            indexAPI.fullReindexStart();

            // delete failing records
            reindexQueueAPI.deleteFailedRecords();

            // new records to index
            reindexQueueAPI.addAllToReindexQueue();

        } catch (Exception e) {
            throw new DotReindexStateException(e.getMessage(), e);
        }

    }

    @WrapInTransaction
    @Override
    public void refreshContentUnderHost(Host host) throws DotReindexStateException {
        try {
            reindexQueueAPI.refreshContentUnderHost(host);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex: " + e.getMessage(), e);
        }

    }

    @WrapInTransaction
    @Override
    public void refreshContentUnderFolder(Folder folder) throws DotReindexStateException {
        try {
            reindexQueueAPI.refreshContentUnderFolder(folder);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex " + e.getMessage(), e);
        }

    }

    @WrapInTransaction
    @Override
    public void refreshContentUnderFolderPath(String hostId, String folderPath)
            throws DotReindexStateException {
        try {
            reindexQueueAPI.refreshContentUnderFolderPath(hostId, folderPath);
        } catch (DotDataException e) {
            Logger.error(this, e.getMessage(), e);
            throw new DotReindexStateException("Unable to complete reindex " + e.getMessage(), e);
        }
    }

    @WrapInTransaction
    @Override
    public void unpublish(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        if (BLANK.equals(contentlet.getInode())) {

            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }

        if (!this.permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH,
                user, respectFrontendRoles)) {

            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot unpublish Contentlet");
        }

        unpublish(contentlet, user, respectFrontendRoles, ThreadContextUtil.isReindex() ? -1 : 0);
    }

    /**
     * check if a workflow may be run instead of the unpublish api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional contentlet, present if ran the workflow, returns the contentlet unpublish
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Contentlet> checkAndRunUnpublishAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, SystemAction.UNPUBLISH, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasUnpublishActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has an unpublish contentlet actionlet"
                                + " so firing a workflow and skipping the current publish for the contentlet: "
                                + contentletIn.getIdentifier());

                return Optional.ofNullable(workflowAPI.fireContentWorkflow(contentletIn,
                        new ContentletDependencies.Builder().workflowActionId(actionId)
                                .modUser(user)
                                .respectAnonymousPermissions(respectFrontendRoles)
                                .build()
                ));
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a unpublish contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    private void unpublish(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles, final int reindex)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        if (contentlet == null || !UtilMethods.isSet(contentlet.getInode())) {

            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }

        final String contentPushPublishDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE), ND_SUPPLIER);
        final String contentPushExpireDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE), ND_SUPPLIER);

        logInfo("Unpublishing Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);

        try {

            final Optional<Contentlet> contentletOpt = this.checkAndRunUnpublishAsWorkflow(
                    contentlet, user, respectFrontendRoles);
            if (contentletOpt.isPresent()) {

                Logger.info(this, "A Workflow has been ran instead of unpublish the contentlet: " +
                        contentlet.getIdentifier());
                if (!contentlet.getInode().equals(contentletOpt.get().getInode())) {
                    this.copyProperties(contentlet, contentletOpt.get().getMap());
                }
                return;
            }

            this.internalUnpublish(contentlet, user, reindex);

            HibernateUtil.addCommitListener(
                    () -> this.contentletSystemEventUtil.pushUnpublishEvent(contentlet), 1000);
            /*
            Triggers a local system event when this contentlet commit listener is executed,
            anyone who need it can subscribed to this commit listener event, on this case will be
            mostly use it in order to invalidate this contentlet cache.
             */
            triggerCommitListenerEvent(contentlet, user, false);
        } catch (DotDataException | DotStateException | DotSecurityException e) {

            logInfo("Error Unpublishing Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);
            throw e;
        }

        logInfo("Content Unpublished", contentlet, user, contentPushPublishDate, contentPushExpireDate);
    }

    private void internalUnpublish(final Contentlet contentlet, final User user, final int reindex)
            throws DotDataException, DotSecurityException {

        WorkflowProcessor workflow = null;
        // to run a workflow we need an action id set, not be part of a workflow already and do not desired disable it
        if (contentlet.getMap().get(Contentlet.DISABLE_WORKFLOW) == null &&
                UtilMethods.isSet(contentlet.getActionId()) &&
                (null == contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS) ||
                        Boolean.FALSE.equals(
                                contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS))
                )) {
            workflow = APILocator.getWorkflowAPI().fireWorkflowPreCheckin(contentlet, user);
        }

        this.canLock(contentlet, user);

        APILocator.getVersionableAPI().removeLive(contentlet);

        //"Disable" the tag created for this Persona key tag
        if (Structure.STRUCTURE_TYPE_PERSONA == contentlet.getStructure().getStructureType()) {
            //Mark the tag created based in the Persona tag key as a regular tag
            APILocator.getPersonaAPI().enableDisablePersonaTag(contentlet, false);
        }

        if (null != workflow) {

            workflow.setContentlet(contentlet);
            APILocator.getWorkflowAPI().fireWorkflowPostCheckin(workflow);
        }

        if (reindex == -1) {

            this.indexAPI.addContentToIndex(contentlet);
        }

        this.indexAPI.removeContentFromLiveIndex(contentlet);

        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET) {

            this.cleanFileAssetCache(contentlet, user, false);
        }

        final Set<String> inodes = Stream.of(contentlet).map(Contentlet::getInode).collect(Collectors.toSet());
        updateModDate(inodes, user);

        invalidateLanguageVariableCache(contentlet);

        new ContentletLoader().invalidate(contentlet, PageMode.LIVE);
        CacheLocator.getContentletCache().remove(contentlet.getInode());
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        CacheLocator.getCSSCache().remove(identifier.getHostId(), identifier.getPath(), true);
        CacheLocator.getCSSCache().remove(identifier.getHostId(), identifier.getPath(), false);
        if (contentlet.isVanityUrl()) {
            APILocator.getVanityUrlAPI().invalidateVanityUrl(contentlet);
        }
        publishRelatedHtmlPages(contentlet);
        if (contentlet.isHTMLPage()) {
            CacheLocator.getNavToolCache().removeNav(
                    contentlet.getHost(), contentlet.getFolder(),
                    contentlet.getLanguageId());
        }
    }

    /**
     * Invalidate the language variable cache if the contentlet is a language variable.
     * @param contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static void invalidateLanguageVariableCache(final Contentlet contentlet) {
       try {
           if (contentlet.isLanguageVariable()) {
                APILocator.getLanguageVariableAPI().invalidateLanguageVariablesCache(contentlet);
           }
       } catch (Exception e) {
           Logger.error(ESContentletAPIImpl.class, "Error invalidating Lang Variable Cache", e);
       }
    }

    private void cleanFileAssetCache(final Contentlet contentlet, final User user,
            final boolean respectFrontEndPermissions)
            throws DotDataException, DotSecurityException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        CacheLocator.getCSSCache().remove(identifier.getHostId(), identifier.getPath(), true);
        //remove from navCache
        final IFileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(contentlet);
        if (fileAsset.isShowOnMenu()) {
            final Folder folder = APILocator.getFolderAPI()
                    .findFolderByPath(identifier.getParentPath(), identifier.getHostId(), user,
                            respectFrontEndPermissions);
            RefreshMenus.deleteMenu(folder);
            CacheLocator.getNavToolCache().removeNav(identifier.getHostId(), folder.getInode());
        }
    }

    // todo:should be in a transaction?
    @Override
    public void unpublish(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        boolean stateError = false;

        for (final Contentlet contentlet : contentlets) {

            try {

                this.unpublish(contentlet, user, respectFrontendRoles);
            } catch (DotContentletStateException e) {

                Logger.error(this, e.getMessage(), e);
                stateError = true;
            }
        }

        if (stateError) {

            Logger.error(this, "Unable to unpublish one or more contentlets because it is locked");
            throw new DotContentletStateException(
                    "Unable to unpublish one or more contentlets because it is locked");
        }
    }

    /**
     * check if a workflow may be run instead of the unarchive api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional Contentlet, if present means the workflow ran, returns the contentlet
     * unarchived
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Contentlet> checkAndRunUnarchiveAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, SystemAction.UNARCHIVE, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasUnarchiveActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has an unarchive contentlet actionlet"
                                + " so firing a workflow and skipping the current unarchive for the contentlet: "
                                + contentletIn.getIdentifier());

                return Optional.ofNullable(workflowAPI.fireContentWorkflow(contentletIn,
                        new ContentletDependencies.Builder().workflowActionId(actionId)
                                .modUser(user)
                                .respectAnonymousPermissions(respectFrontendRoles)
                                .build()
                ));
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a unarchive contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    @WrapInTransaction
    @Override
    public void unarchive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final String contentPushPublishDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE), ND_SUPPLIER);
        final String contentPushExpireDate = UtilMethods.get(
                contentlet.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE), ND_SUPPLIER);

        logInfo("Unarchiving Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);

        try {

            if (BLANK.equals(contentlet.getInode())) {

                throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
            }

            if (!this.permissionAPI.doesUserHavePermission(contentlet,
                    PermissionAPI.PERMISSION_PUBLISH, user, respectFrontendRoles)) {

                throw new DotSecurityException(
                        "User: " + (user != null ? user.getUserId() : "Unknown")
                                + " cannot unpublish Contentlet");
            }

            final Optional<Contentlet> contentletOpt = this.checkAndRunUnarchiveAsWorkflow(
                    contentlet, user, respectFrontendRoles);
            if (contentletOpt.isPresent()) {

                Logger.info(this, "A Workflow has been ran instead of unarchive the contentlet: " +
                        contentlet.getIdentifier());
                if (!contentlet.getInode().equals(contentletOpt.get().getInode())) {
                    this.copyProperties(contentlet, contentletOpt.get().getMap());
                }
                return;
            }

            this.internalUnarchive(contentlet, user, respectFrontendRoles);
        } catch (DotDataException | DotStateException | DotSecurityException e) {

            logInfo("Error Unarchiving Content", contentlet, user, contentPushPublishDate, contentPushExpireDate);
            throw e;
        }

        logInfo("Content Unarchived", contentlet, user, contentPushPublishDate, contentPushExpireDate);

    }

    private void internalUnarchive(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        final Contentlet workingContentlet = this.findContentletByIdentifier(
                contentlet.getIdentifier(),
                false, contentlet.getLanguageId(), user, respectFrontendRoles);
        Contentlet liveContentlet = null;

        this.canLock(contentlet, user);

        try {

            liveContentlet = this.findContentletByIdentifier(contentlet.getIdentifier(), true,
                    contentlet.getLanguageId(), user, respectFrontendRoles);
        } catch (DotContentletStateException ce) {

            Logger.debug(this, () -> "No live contentlet found for identifier = "
                    + contentlet.getIdentifier());
        }

        if (liveContentlet != null && liveContentlet.getInode()
                .equalsIgnoreCase(workingContentlet.getInode())
                && !workingContentlet.isArchived()) {

            throw new DotContentletStateException("Contentlet is unarchivable");
        }

        APILocator.getVersionableAPI().setDeleted(workingContentlet, false);

        this.indexAPI.addContentToIndex(workingContentlet);

        // we don't want to reindex this twice when it is the same version
        if (liveContentlet != null && UtilMethods.isSet(liveContentlet.getInode())
                && !liveContentlet.getInode().equalsIgnoreCase(workingContentlet.getInode())) {

            this.indexAPI.addContentToIndex(liveContentlet);
        }

        new ContentletLoader().invalidate(contentlet);
        CacheLocator.getContentletCache().remove(contentlet.getInode());
        publishRelatedHtmlPages(contentlet);
        invalidateLanguageVariableCache(contentlet);
        HibernateUtil.addCommitListener(() -> this.sendUnArchiveContentSystemEvent(contentlet),
                1000);
        HibernateUtil.addCommitListener(() -> localSystemEventsAPI.notify(
                new ContentletArchiveEvent(contentlet, user, false)));
    }

    private void sendUnArchiveContentSystemEvent(final Contentlet contentlet) {

        this.contentletSystemEventUtil.pushUnArchiveEvent(contentlet);
    }

    // todo: should be in a transaction.
    @Override
    public void unarchive(final List<Contentlet> contentlets, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        boolean stateError = false;

        for (final Contentlet contentlet : contentlets) {

            try {

                this.unarchive(contentlet, user, respectFrontendRoles);
            } catch (DotContentletStateException e) {

                Logger.error(this, e.getMessage(), e);
                stateError = true;
            }
        }

        if (stateError) {

            throw new DotContentletStateException(
                    "Unable to unarchive one or more contentlets because it is locked");
        }
    } // unarchive.

    @Override
    public void deleteRelatedContent(Contentlet contentlet, Relationship relationship, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        this.deleteRelatedContent(contentlet, relationship, APILocator.getRelationshipAPI()
                .isParent(relationship, contentlet.getStructure()), user, respectFrontendRoles);
    }

    @Override
    public void deleteRelatedContent(final Contentlet contentlet, final Relationship relationship,
            final boolean hasParent, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        deleteRelatedContent(contentlet, relationship, hasParent, user, respectFrontendRoles,
                Collections.emptyList());
    }

    @WrapInTransaction
    @Override
    public void deleteRelatedContent(final Contentlet contentlet, final Relationship relationship,
            final boolean hasParent, final User user, final boolean respectFrontendRoles,
            final List<Contentlet> contentletsToBeRelated)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot edit Contentlet with identifier " + contentlet.getIdentifier());
        }

        List<Relationship> rels = APILocator.getRelationshipAPI()
                .byContentType(contentlet.getContentType());
        if (!rels.contains(relationship)) {
            throw new DotContentletStateException(
                    "Error deleting existing relationships in contentlet: " + (contentlet != null
                            ? contentlet.getInode() : "Unknown"));
        }

        List<Contentlet> cons = relationshipAPI
                .dbRelatedContent(relationship, contentlet, hasParent);
        cons = permissionAPI
                .filterCollection(cons, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);

        for (final Contentlet relatedContent : cons) {
            if (hasParent) {
                TreeFactory.deleteTreesByParentAndChildAndRelationType(contentlet.getIdentifier(),
                        relatedContent.getIdentifier(), relationship.getRelationTypeValue());
            } else {
                TreeFactory.deleteTreesByParentAndChildAndRelationType(
                        relatedContent.getIdentifier(),
                        contentlet.getIdentifier(), relationship.getRelationTypeValue());
            }
        }

        final List<String> identifiersToBeRelated = contentletsToBeRelated.stream().map(
                Contentlet::getIdentifier).collect(Collectors.toList());

        // We need to refresh related parents, because currently the system does not
        // update the contentlets that lost the relationship (when the user remove a relationship).
        if (cons != null) {
            for (final Contentlet relatedContentlet : cons) {
                //Only deleted parents will be reindexed
                if (!hasParent && !identifiersToBeRelated
                        .contains(relatedContentlet.getIdentifier())) {
                    relatedContentlet.setIndexPolicy(contentlet.getIndexPolicyDependencies());
                    relatedContentlet
                            .setIndexPolicyDependencies(
                                    contentlet.getIndexPolicyDependencies());
                    refreshNoDeps(relatedContentlet);
                }
                //If relationship field, related content cache must be invalidated
                invalidateRelatedContentCache(relatedContentlet, relationship, !hasParent);
            }
        }

        // Refresh the parent only if the contentlet is not already in the checkin
        if (!contentlet.getBoolProperty(CHECKIN_IN_PROGRESS)) {
            refreshNoDeps(contentlet);
        }
    }

    @Override
    public void invalidateRelatedContentCache(Contentlet contentlet, Relationship relationship,
            boolean hasParent) {

        //If relationship field, related content cache must be invalidated
        if (relationship.isRelationshipField()) {

            if (relationshipAPI.sameParentAndChild(relationship)) {
                if (relationship.getParentRelationName() != null) {
                    contentlet.setRelated(relationship.getParentRelationName(), null);
                    CacheLocator.getRelationshipCache()
                            .removeRelatedContentMap(contentlet.getIdentifier());
                }

                if (relationship.getChildRelationName() != null) {
                    contentlet.setRelated(relationship.getChildRelationName(), null);
                    CacheLocator.getRelationshipCache()
                            .removeRelatedContentMap(contentlet.getIdentifier());
                }
            } else {
                if (!hasParent && relationship.getParentRelationName() != null) {
                    contentlet.setRelated(relationship.getParentRelationName(), null);
                    CacheLocator.getRelationshipCache()
                            .removeRelatedContentMap(contentlet.getIdentifier());
                } else if (hasParent && relationship.getChildRelationName() != null) {
                    contentlet.setRelated(relationship.getChildRelationName(), null);
                    CacheLocator.getRelationshipCache()
                            .removeRelatedContentMap(contentlet.getIdentifier());
                }
            }
        }
    }

    @Override
    public List<Contentlet> getRelatedContent(final Contentlet contentlet,
            final String variableName,
            final User user,
            final boolean respectFrontendRoles, Boolean pullByParents, final int limit,
            final int offset, final String sortBy) {
        return getRelatedContent(contentlet, variableName, user, respectFrontendRoles,
                pullByParents, limit, offset, sortBy, -1, null);
    }

    @RequestCost(Price.CONTENT_GET_RELATED)
    @CloseDBIfOpened
    @Override
    public List<Contentlet> getRelatedContent(final Contentlet contentlet,
            final String variableName,
            final User user,
            final boolean respectFrontendRoles, Boolean pullByParents, final int limit,
            final int offset, final String sortBy, final long language, final Boolean live) {

        if (variableName == null) {
            return Collections.EMPTY_LIST;
        }

        final String contentletIdentifier = contentlet.getIdentifier();
        Map<String, List<String>> relatedIds = null;
        try {
            if (UtilMethods.isSet(CacheLocator.getRelationshipCache()
                    .getRelatedContentMap(contentletIdentifier))) {

                //Get mutable map
                relatedIds = new ConcurrentHashMap<>(CacheLocator.getRelationshipCache()
                        .getRelatedContentMap(contentletIdentifier));
            }
        } catch (DotCacheException e) {
            Logger.debug(this,
                    String.format("Cache entry with key %s was not found.", contentletIdentifier),
                    e);
        }

        if (relatedIds == null) {
            relatedIds = Maps.newConcurrentMap();
        }

        try {
            User currentUser;

            if (user != null) {
                currentUser = user;
            } else {
                currentUser = APILocator.getUserAPI().getAnonymousUser();
            }

            List<Contentlet> relatedContentlet;

            if (relatedIds.containsKey(variableName)) {
                relatedContentlet = getCachedRelatedContentlets(relatedIds, variableName, language,
                        currentUser.equals(APILocator.getUserAPI().getAnonymousUser())
                                ? Boolean.TRUE
                                : live);
            } else {
                relatedContentlet = getNonCachedRelatedContentlets(contentlet, relatedIds,
                        variableName, pullByParents,
                        limit, offset, language,
                        currentUser.equals(APILocator.getUserAPI().getAnonymousUser())
                                ? Boolean.TRUE
                                : live);
            }

            if (UtilMethods.isSet(sortBy)) {
                Collections.sort(relatedContentlet, new ContentMapComparator(sortBy));
            }

            //Restricts contentlet according to user permissions
            return APILocator.getPermissionAPI()
                    .filterCollection(relatedContentlet, PermissionAPI.PERMISSION_READ,
                            currentUser.equals(APILocator.getUserAPI().getAnonymousUser())
                                    ? true : respectFrontendRoles, currentUser);

        } catch (DotDataException | DotSecurityException e) {
            Logger.warn(this, "Error getting related content for field " + variableName, e);
            throw new DotStateException(e);
        }
    }

    /**
     * Retrieves the Contentlets that are associated to a specific piece of Content through a
     * specific Relationship field. This method is executed for Relationships that are not currently
     * cached by dotCMS, and must be looked up from scratch.
     *
     * @param contentlet   The {@link Contentlet} object whose related Contentlets will be
     *                     retrieved.
     * @param relatedIds   The data structure that will store the related Contentlets.
     * @param variableName The Velocity Variable name of the Relationship field for the Content Type
     *                     that the {@code contentlet} object belongs to.
     * @param pullByParent
     * @param limit        Pagination parameter for the total number of results to return.
     * @param offset       Pagination parameter for the offset.
     * @return The list of related {@link Contentlet} objects.
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException
     */
    @Nullable
    private List<Contentlet> getNonCachedRelatedContentlets(final Contentlet contentlet,
            final Map<String, List<String>> relatedIds, final String variableName,
            final Boolean pullByParent, final int limit, final int offset, final long language,
            final Boolean live)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        com.dotcms.contenttype.model.field.Field field = null;
        Relationship relationship;

        try {
            field = APILocator
                    .getContentTypeFieldAPI()
                    .byContentTypeIdAndVar(contentlet.getContentTypeId(), variableName);
            relationship = this.relationshipAPI.getRelationshipFromField(field, systemUser);
        } catch (final NotFoundInDbException e) {
            // Search for legacy relationships
            relationship = this.relationshipAPI.byTypeValue(variableName);
        }

        if (null == relationship) {
            throw new DotStateException(
                    String.format("Relationship field '%s' in Content Type ID '%s' was not found." +
                                    " Make sure that the relationship table points to the correct field.",
                            variableName, contentlet
                                    .getContentTypeId()));
        }
        final List<Contentlet> relatedList = filterRelatedContent(contentlet, relationship,
                systemUser, false,
                pullByParent, limit, offset);

        // Get unique identifiers to avoid duplicates (used to save on cache and filter the final list if needed
        final List<String> uniqueIdentifiers = relatedList.stream().map(Contentlet::getIdentifier)
                .distinct()
                .collect(CollectionsUtils.toImmutableList());

        //Cache related content only if it is a relationship field and there is no filter
        //In case of self-relationships, we shouldn't cache any value for a particular field when pullByParent==null
        //because in this case all parents and children are returned
        if (field != null && limit == -1 && offset <= 0 &&
                !(this.relationshipAPI.sameParentAndChild(relationship) && pullByParent == null)) {
            if (UtilMethods.isSet(relatedList)) {
                relatedIds.put(variableName, uniqueIdentifiers);
            } else {
                relatedIds.put(variableName, Collections.emptyList());
            }
            //refreshing cache when related content map is updated
            CacheLocator.getRelationshipCache()
                    .putRelatedContentMap(contentlet.getIdentifier(), relatedIds);
        }

        if (live == null && language == -1) {
            return relatedList;
        } else {
            /*Filter by live and/or language if set
              If live=true, for each content, it needs to return the live version.
              Otherwise, it would return the working one*/
            return uniqueIdentifiers.stream()
                    .flatMap(identifier -> filterRelatedContentByLiveAndLanguage(language, live,
                            identifier))
                    .collect(Collectors.toList());
        }
    }

    /**
     * @param relatedIds
     * @param variableName
     * @return
     */
    @NotNull
    private List<Contentlet> getCachedRelatedContentlets(final Map<String, List<String>> relatedIds,
            final String variableName, final long language, final Boolean live) {

        return relatedIds
                .get(variableName).stream()
                .flatMap(identifier -> filterRelatedContentByLiveAndLanguage(language, live,
                        identifier))
                .collect(Collectors.toList());
    }

    /**
     * @param language
     * @param live
     * @param identifier
     * @return
     */
    private Stream<? extends Contentlet> filterRelatedContentByLiveAndLanguage(final long language,
            final Boolean live,
            final String identifier) {

        final List<Contentlet> relatedContentList = new ArrayList<>();
        //If language is set, we must return the content version in that language.
        //Otherwise, we need to return a version for each language if exists
        List<Long> languages = language > 0 ? CollectionsUtils.list(language)
                : languageAPI.getLanguages().stream().map(lang -> lang.getId()).collect(
                        Collectors.toList());

        for (Long currentLanguage : languages) {
            try {
                Contentlet currentContent = findContentletByIdentifier(
                        identifier, live == null ? false : live,
                        currentLanguage, APILocator.getUserAPI().getSystemUser(),
                        false);
                if (currentContent != null) {
                    relatedContentList.add(currentContent);
                }
            } catch (DotDataException | DotSecurityException | DotContentletStateException e) {
                Logger.warnEveryAndDebug(this.getClass(),
                        "No live version for contentlet identifier "
                                + identifier, e, 5000);
            }
        }

        return relatedContentList.stream();
    }

    // it is just close db, since the other relate content runs on transacional mode. So, that we can take advance of the cache.
    @CloseDBIfOpened
    @Override
    public void relateContent(final Contentlet contentlet,
            final Relationship rel,
            final List<Contentlet> records,
            final User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final Structure structure = CacheLocator.getContentTypeCache()
                .getStructureByInode(contentlet.getStructureInode());
        final boolean hasParent = APILocator.getRelationshipAPI().isParent(rel, structure);
        final ContentletRelationshipRecords related = new ContentletRelationships(
                contentlet).new ContentletRelationshipRecords(rel, hasParent);
        related.setRecords(records);
        relateContent(contentlet, related, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    private List<Relationship> getRelationships(final ContentTypeIf type) throws DotDataException {

        return APILocator.getRelationshipAPI().byContentType(type);
    }

    @CloseDBIfOpened
    private List<Tree> getContentParents(final String inode) throws DotDataException {

        return TreeFactory.getTreesByChild(inode);
    }

    // it is just Close db, b.c internally the code is handling their own transaction
    @CloseDBIfOpened
    @Override
    public void relateContent(final Contentlet contentlet,
            final ContentletRelationshipRecords related,
            final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user,
                respectFrontendRoles)) {

            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot edit Contentlet: " + (contentlet != null ? contentlet.getInode()
                    : "Unknown"));
        }

        //do not perform any changes on related records
        if (related.getRecords() == null) {
            return;
        }

        final ContentType contentType = contentlet.getContentType();
        final List<Relationship> relationships = this.getRelationships(contentType);
        final Relationship relationship = related.getRelationship();

        if (!relationships.contains(related.getRelationship())) {

            throw new DotContentletStateException(
                    "Error adding relationships in contentlet:  " + (contentlet != null ? contentlet
                            .getInode() : "Unknown"));
        }

        final boolean child = !related.isHasParent();

        List<Tree> contentParents = null;
        if (child) {
            contentParents = this.getContentParents(contentlet.getIdentifier());
        }

        boolean localTransaction = false;
        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        try {
            try {
                localTransaction = HibernateUtil.startLocalTransactionIfNeeded();
            } catch (Exception e) {
                throw new DotDataException(e.getMessage(), e);
            }

            deleteRelatedContent(contentlet, relationship, related.isHasParent(), user,
                    respectFrontendRoles, related.getRecords());

            Tree newTree;
            Set<Tree> uniqueRelationshipSet = new HashSet<>();

            List<Contentlet> conRels = getRelatedContentFromIndex(contentlet, relationship,
                    related.isHasParent(), user, respectFrontendRoles);

            int treePosition = (conRels != null && conRels.size() != 0) ? conRels.size() : 1;
            int positionInParent = 1;

            for (Contentlet c : related.getRecords()) {
                if (child) {
                    for (Tree currentTree : contentParents) {
                        if (currentTree.getRelationType()
                                .equals(relationship.getRelationTypeValue()) && c.getIdentifier()
                                .equals(currentTree.getParent())) {
                            positionInParent = currentTree.getTreeOrder();
                        }
                    }

                    newTree = new Tree(c.getIdentifier(), contentlet.getIdentifier(),
                            relationship.getRelationTypeValue(), positionInParent);
                } else {
                    newTree = new Tree(contentlet.getIdentifier(), c.getIdentifier(),
                            relationship.getRelationTypeValue(), treePosition);
                }
                positionInParent = positionInParent + 1;

                if (uniqueRelationshipSet.add(newTree)) {
                    final int newTreePosition = newTree.getTreeOrder();
                    final Tree treeToUpdate = TreeFactory.getTree(newTree);
                    treeToUpdate.setTreeOrder(newTreePosition);

                    TreeFactory.saveTree(treeToUpdate != null && UtilMethods.isSet(
                            treeToUpdate.getRelationType()) ? treeToUpdate : newTree);

                    treePosition++;
                }
                invalidateRelatedContentCache(c, relationship, !related.isHasParent());
            }

            //If relationship field, related content cache must be invalidated
            invalidateRelatedContentCache(contentlet, relationship, related.isHasParent());

            if (localTransaction) {
                HibernateUtil.commitTransaction();
            }
        } catch (Exception exception) {
            Logger.debug(this.getClass(), "Failed to relate content. : " + exception.toString(),
                    exception);
            if (localTransaction) {
                HibernateUtil.rollbackTransaction();
            }
            throw new DotDataException(exception.getMessage(), exception);
        } finally {
            if (localTransaction && isNewConnection) {
                HibernateUtil.closeSessionSilently();
            }
        }
    }

    // todo: should be in a transaction.????
    @Override
    public void publish(final List<Contentlet> contentlets, final User user, final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotContentletStateException {
        boolean stateError = false;
        String errorMsgFromException = BLANK;
        for (final Contentlet contentlet : contentlets) {
            try {
                publish(contentlet, user, respectFrontendRoles);
            } catch (final DotContentletStateException e) {
                errorMsgFromException = ExceptionUtil.getErrorMessage(e);
                stateError = true;
            } catch (final Exception e) {
                errorMsgFromException = ExceptionUtil.getErrorMessage(e);
                final String errorMsg = String.format("Unable to publish Contentlet ID '%s': %s", contentlet.getIdentifier(),
                        errorMsgFromException);
                Logger.debug(this.getClass(), errorMsg, e);
                stateError = true;
            }
        }
        if (stateError) {
            throw new DotContentletStateException(errorMsgFromException);
        }
    }

    @CloseDBIfOpened
    @Override
    public boolean isContentEqual(Contentlet contentlet1, Contentlet contentlet2, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        if (!permissionAPI.doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet: " + (contentlet1 != null ? contentlet1.getInode()
                    : "Unknown"));
        }
        if (!permissionAPI.doesUserHavePermission(contentlet2, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet: " + (contentlet2 != null ? contentlet1.getInode()
                    : "Unknown"));
        }
        if (contentlet1.getInode().equalsIgnoreCase(contentlet2.getInode())) {
            return true;
        }
        return false;
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> getSiblings(String identifier)
            throws DotDataException, DotSecurityException {
        List<Contentlet> contentletList = contentFactory.getContentletsByIdentifier(identifier);

        return contentletList;
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet, List<Category> cats,
            List<Permission> permissions, User user,
            boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException {

        return checkin(contentlet, (Map<Relationship, List<Contentlet>>) null, cats, permissions,
                user,
                respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet, List<Permission> permissions, User user,
            boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException {

        return checkin(contentlet, (ContentletRelationships) null, null, permissions, user,
                respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships,
            List<Category> cats, User user, boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException {
        return checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles, false);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships, User user,
            boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException {
        return checkin(contentlet, contentRelationships, null, user, respectFrontendRoles);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet, User user, boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException {

        user = (user == null) ? APILocator.getUserAPI().getAnonymousUser() : user;

        return checkin(contentlet, (ContentletRelationships) null, null, null, user,
                respectFrontendRoles, false);
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkin(Contentlet contentlet, User user, boolean respectFrontendRoles,
            List<Category> cats)
            throws IllegalArgumentException, DotDataException, DotSecurityException {
        return checkin(contentlet, null, cats, user, respectFrontendRoles);
    }

    @Override
    public Contentlet checkin(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships,
            List<Category> cats, List<Permission> permissions, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {

        final Contentlet contentletReturned = checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles, false);

        this.handlePermissions(permissions, user, respectFrontendRoles, contentletReturned);

        return contentletReturned;
    }

    /**
     * @param contentlet
     * @param contentRelationships
     * @param cats
     * @param user
     * @param respectFrontendRoles
     * @param generateSystemEvent
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotContentletStateException
     * @throws DotContentletValidationException
     */
    @CloseDBIfOpened
    private Contentlet checkin(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships,
            List<Category> cats, User user, boolean respectFrontendRoles,
            boolean generateSystemEvent)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        ContentletRelationships relationshipsData = getContentletRelationshipsFromMap(contentlet,
                contentRelationships);

        return checkin(contentlet, relationshipsData, cats, user, respectFrontendRoles, true,
                generateSystemEvent);
    }

    @Override
    public Contentlet checkin(Contentlet contentlet, ContentletRelationships contentRelationships,
            List<Category> cats,
            List<Permission> permissions, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {

        final Contentlet contentletReturned =
                    checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles, true,
                false);

        this.handlePermissions(permissions, user, respectFrontendRoles, contentletReturned);

        return contentletReturned;
    }

    private ContentletRelationships getContentletRelationshipsFromMap(final Contentlet contentlet,
            final Map<Relationship, List<Contentlet>> contentRelationships) {

        return new ContentletRelationshipsTransformer(contentlet, contentRelationships).findFirst();
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkinWithoutVersioning(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats,
            List<Permission> permissions, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {

        final ContentletRelationships relationshipsData = getContentletRelationshipsFromMap(contentlet,
                contentRelationships);
        final Contentlet contentletReturned = checkin(contentlet, relationshipsData, cats, user, respectFrontendRoles, false,
                false);

        this.handlePermissions(permissions, user, respectFrontendRoles, contentletReturned);

        return contentletReturned;
    }

    @CloseDBIfOpened
    @Override
    public Contentlet checkinWithoutVersioning(final Contentlet contentlet,
            final ContentletRelationships contentRelationships, final List<Category> cats,
            final List<Permission> permissions, final User user, final boolean respectFrontendRoles)
            throws DotContentletStateException, DotSecurityException, DotDataException {
        return checkin(contentlet, contentRelationships, cats, user, respectFrontendRoles, false,
                false);
    }

    /**
     * @param contentletIn
     * @param contentRelationships
     * @param categories
     * @param user
     * @param respectFrontendRoles
     * @param createNewVersion
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotContentletStateException
     * @throws DotContentletValidationException
     */
    @WrapInTransaction
    private Contentlet checkin(final Contentlet contentletIn,
            final ContentletRelationships contentRelationships,
            final List<Category> categories, final User user, boolean respectFrontendRoles,
            final boolean createNewVersion, final boolean generateSystemEvent)
            throws DotDataException, DotSecurityException {

        Contentlet contentletOut = null;
        Boolean autoAssign = null;

        try {

            String wfPublishDate = contentletIn.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE);
            String wfExpireDate = contentletIn.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE);
            final boolean isWorkflowInProgress = contentletIn.isWorkflowInProgress();
            final String contentPushPublishDateBefore =
                    UtilMethods.isSet(wfPublishDate) ? wfPublishDate : "N/D";
            final String contentPushExpireDateBefore =
                    UtilMethods.isSet(wfExpireDate) ? wfExpireDate : "N/D";

            logInfo("Contentlet to be saved: ", contentletIn, user, contentPushPublishDateBefore,
                    contentPushExpireDateBefore);

            this.checkOrSetContentType(contentletIn, user);

            final String lockKey =
                    "ContentletIdentifier:" + (UtilMethods.isSet(contentletIn.getIdentifier()) ?
                            contentletIn.getIdentifier() : UUIDGenerator.generateUuid());
            try {

                final Optional<Contentlet> workflowContentletOpt =
                        this.validateWorkflowStateOrRunAsWorkflow(contentletIn,
                                contentRelationships,
                                categories, user, respectFrontendRoles, createNewVersion,
                                generateSystemEvent);

                if (workflowContentletOpt.isPresent()) {
                    Logger.info(this,
                            "A Workflow has been run instead of checking in Contentlet with ID: " +
                                    workflowContentletOpt.get().getIdentifier());
                    return workflowContentletOpt.get();
                }

                autoAssign = (Boolean) contentletIn.getMap().get(Contentlet.AUTO_ASSIGN_WORKFLOW);
                contentletOut = lockManager.tryLock(lockKey,
                        () -> internalCheckin(
                                contentletIn, contentRelationships, categories, user,
                                respectFrontendRoles, createNewVersion
                        )
                );
            } catch (final Throwable t) {
                if (!(t instanceof DotContentletValidationException)) {
                    Logger.warn(this, ExceptionUtil.getErrorMessage(t), t);
                }
                bubbleUpException(t);
            }

            //This way no matter how many times the contentlet reference that we're passing in gets override.
            //On the way back this updates the original contentlet references updating the new inode and identifier
            if (null == contentletOut) {
                throw new DotDataException("Checked in contentlet is null. This must never happen!");
            }
            contentletIn.setIdentifier(contentletOut.getIdentifier());
            contentletIn.setInode(contentletOut.getInode());

            wfPublishDate = contentletOut.getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE);
            wfExpireDate = contentletOut.getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE);

            final String contentPushPublishDateAfter =
                    UtilMethods.isSet(wfPublishDate) ? wfPublishDate : "N/D";
            final String contentPushExpireDateAfter =
                    UtilMethods.isSet(wfExpireDate) ? wfExpireDate : "N/D";

            logInfo("Contentlet saved: " ,contentletOut, user, contentPushPublishDateAfter, contentPushExpireDateAfter);

            if (isWorkflowInProgress) {
                autoAssign = false;
            }

            // Creates the Local System event
            if (null != autoAssign) {
                contentletOut.setBoolProperty(Contentlet.AUTO_ASSIGN_WORKFLOW, autoAssign);
            }

            this.createLocalCheckinEvent(contentletOut, user, createNewVersion);

            //Create a System event for this contentlet
            if (generateSystemEvent) {
                this.pushSaveEvent(contentletOut, createNewVersion);
            }

            if (contentletIn.getBoolProperty(Contentlet.IS_COPY)){
                contentletOut.setBoolProperty(Contentlet.IS_COPY, true);
            }

            return contentletOut;
        } finally {
            this.cleanup(contentletOut);
        }
    }

    /*
     * If the contentletIn is new, has not any content type assigned and has a base type set into the properties
     * will try to figure out a match for the content type
     */
    private void checkOrSetContentType(final Contentlet contentletIn, final User user) {

        final Optional<BaseContentType> baseTypeOpt = contentletIn.getBaseType();
        if (contentletIn.isNew() && null == contentletIn.getContentType() &&
                baseTypeOpt.isPresent()) {

            final BaseContentType baseContentType = baseTypeOpt.get();
            final Optional<BaseTypeToContentTypeStrategy> typeStrategy =
                    this.baseTypeToContentTypeStrategyResolver.get(baseContentType);

            if (typeStrategy.isPresent()) {

                final Host host = Try.of(() -> APILocator.getHostAPI().find(
                        contentletIn.getHost(), user, false)).getOrNull();
                if (null != host) {

                    final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
                    final String sessionId = request != null && request.getSession(false) != null
                            ? request.getSession().getId() : null;
                    final List<String> accessingList = null != request ?
                            Arrays.asList(user.getUserId(),
                                    APILocator.getTempFileAPI().getRequestFingerprint(request),
                                    sessionId) :
                            Arrays.asList(user.getUserId(), sessionId);

                    final Optional<ContentType> contentTypeOpt = typeStrategy.get()
                            .apply(baseContentType,
                                    Map.of("user", user, "host", host,
                                            "contentletMap", contentletIn.getMap(), "accessingList",
                                            accessingList));

                    if (contentTypeOpt.isPresent()) {
                        contentletIn.setContentType(contentTypeOpt.get());
                    }
                }
            }
        }
    }

    /**
     * check if a workflow may be run instead of the publish api call itself.
     *
     * @param contentletIn
     * @param user
     * @param respectFrontendRoles
     * @return Optional Contentlet, present if workflow ran
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Optional<Contentlet> checkAndRunPublishAsWorkflow(final Contentlet contentletIn,
            final User user,
            final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, WorkflowAPI.SystemAction.PUBLISH, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the avalable actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                    + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasPublishActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has a publish contentlet actionlet"
                                + " so firing a workflow and skipping the current publish for the contentlet: "
                                + contentletIn.getIdentifier());

                return Optional.ofNullable(workflowAPI.fireContentWorkflow(contentletIn,
                        new ContentletDependencies.Builder().workflowActionId(actionId)
                                .modUser(user)
                                .respectAnonymousPermissions(respectFrontendRoles)
                                .build()
                ));
            }

            Logger.info(this, () -> "The action: " + contentletIn.getIdentifier()
                    + " hasn't a publish contentlet actionlet"
                    + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    private boolean isDefaultActionOnAvailableActions(final Contentlet contentletIn,
            final User user,
            final WorkflowAPI workflowAPI,
            final String actionId) throws DotDataException, DotSecurityException {

        if (workflowAPI.isActionAvailable(contentletIn, user, actionId)) {

            return true;
        }

        Logger.info(this, () -> "The contentlet: " + contentletIn.getIdentifier()
                + " has the action: " + actionId
                + " but the this is not an available action for it");
        return false;
    }

    private Optional<Contentlet> validateWorkflowStateOrRunAsWorkflow(final Contentlet contentletIn,
            final ContentletRelationships contentRelationships,
            final List<Category> categories, final User userIn,
            final boolean respectFrontendRoles, final boolean createNewVersion,
            boolean generateSystemEvent) throws DotSecurityException, DotDataException {

        final User user = (userIn == null) ? APILocator.getUserAPI().getAnonymousUser() : userIn;

        // if already on workflow or has an actionid skip this method.
        if (this.isDisableWorkflow(contentletIn)
                || this.isWorkflowInProgress(contentletIn)
                || this.isInvalidContentTypeForWorkflow(contentletIn)
                || UtilMethods.isSet(contentletIn.getActionId())) {

            return Optional.empty();
        }

        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        // note: by now we are just using the new system action, even if the contentlet already exists.
        // in the future if the contentlet exist, EDIT should be catch
        final Optional<WorkflowAction> workflowActionOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentletIn, contentletIn.isNew() ? WorkflowAPI.SystemAction.NEW
                                : WorkflowAPI.SystemAction.EDIT, user);

        if (workflowActionOpt.isPresent()) {

            final String title = contentletIn.getTitle();
            final String actionId = workflowActionOpt.get().getId();

            // if the default action is in the available actions for the content.
            if (!isDefaultActionOnAvailableActions(contentletIn, user, workflowAPI, actionId)) {
                return Optional.empty();
            }

            Logger.info(this, () -> "The contentlet: " + title + " hasn't action id set"
                    + " using the default action: " + actionId);

            // if the action has a save action, we skip the current checkin
            if (workflowActionOpt.get().hasSaveActionlet()) {

                Logger.info(this,
                        () -> "The action: " + actionId + " has a save contentlet actionlet"
                                + " so firing a workflow and skipping the current checkin for the contentlet: "
                                + title);

                return Optional.ofNullable(workflowAPI.fireContentWorkflow(contentletIn,
                        new ContentletDependencies.Builder().workflowActionId(actionId)
                                .modUser(user).categories(categories)
                                .relationships(contentRelationships)
                                .respectAnonymousPermissions(respectFrontendRoles)
                                .generateSystemEvent(generateSystemEvent)
                                .build()
                ));
            }

            Logger.info(this,
                    () -> "The action: " + actionId + " hasn't a save contentlet actionlet"
                            + " so including just the action to the contentlet: " + title);

            contentletIn.setActionId(actionId);
        }

        return Optional.empty();
    }

    private boolean isInvalidContentTypeForWorkflow(final Contentlet contentletIn) {

        return Host.HOST_VELOCITY_VAR_NAME.equals(contentletIn.getContentType().variable());
    }

    private boolean isDisableWorkflow(final Contentlet contentlet) {
        return contentlet.isDisableWorkflow();
    }

    private boolean isWorkflowInProgress(final Contentlet contentlet) {
        return contentlet.isWorkflowInProgress();
    }

    private Contentlet internalCheckin(Contentlet contentlet,
            ContentletRelationships contentRelationships, List<Category> categories,
            final User incomingUser,
            final boolean respectFrontendRoles,
            boolean createNewVersion
    ) throws DotDataException, DotSecurityException {

        final User user =
                (incomingUser != null) ? incomingUser : APILocator.getUserAPI().getAnonymousUser();

        if (user.isAnonymousUser() && AnonymousAccess.systemSetting() != AnonymousAccess.WRITE) {
            throw new DotSecurityException(
                    "CONTENT_APIS_ALLOW_ANONYMOUS setting does not allow anonymous content WRITEs");
        }

        if (contentRelationships == null) {

            //Obtain all relationships
            contentRelationships = getContentletRelationships(contentlet, user);

            if (contentRelationships != null) {
                getAllRelationships(contentlet, contentRelationships);
            }
        }

        if (categories == null) {
            categories = getExistingContentCategories(contentlet);
        }
        final ContentType contentType = contentlet.getContentType();

        String existingInode = null, existingIdentifier = null;

        Contentlet workingContentlet = contentlet;
        try {

            // if we have incoming temp files set as binaryFields, lets populate the contentlet with them
            for (com.dotcms.contenttype.model.field.Field field : contentType.fields(
                    BinaryField.class)) {
                final Object fileObject = contentlet.get(field.variable());
                if (fileObject instanceof String && tempApi.isTempResource(
                        contentlet.getStringProperty(field.variable()))) {

                    final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

                    final DotTempFile tempFile = tempApi.getTempFile(request,
                            contentlet.getStringProperty(field.variable())).get();
                    contentlet.setBinary(field, tempFile.file);
                }
            }

            String oldHostId = null;

            if (createNewVersion && contentlet != null && InodeUtils.isSet(contentlet.getInode())) {
                // maybe the user want to save new content with existing inode & identifier comming from somewhere
                // we need to check that the inode doesn't exists
                DotConnect dc = new DotConnect();
                dc.setSQL("select inode from contentlet where inode=?");
                dc.addParam(contentlet.getInode());
                if (dc.loadResults().size() > 0) {
                    if (contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME) != null) {
                        Logger.debug(this,
                                "forcing checking with no version as the _dont_validate_me is set and inode exists");
                        createNewVersion = false;
                    } else {
                        throw new DotContentletStateException("Contentlet must not exist already");
                    }
                } else {

                    existingInode = contentlet.getInode();
                    contentlet.setInode(null);

                    Identifier ident = APILocator.getIdentifierAPI()
                            .find(contentlet.getIdentifier());

                    if (ident == null || !UtilMethods.isSet(ident.getId())) {
                        existingIdentifier = contentlet.getIdentifier();
                        contentlet.setIdentifier(null);
                    }
                }

            }

            if (UtilMethods.isSet(contentlet.getIdentifier())) {
                Identifier ident = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());

                if (UtilMethods.isSet(ident)) {
                    oldHostId = ident.getHostId();
                }
            }

            if (!createNewVersion && contentlet != null && !InodeUtils.isSet(
                    contentlet.getInode())) {
                throw new DotContentletStateException("Contentlet must exist already");
            }
            if (contentlet != null && contentlet.isArchived()
                    && contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME) == null) {
                throw new DotContentletStateException(
                        "Unable to checkin an archived piece of content, please un-archive first");
            }

            checkPermission(contentlet, respectFrontendRoles, user);

            if (createNewVersion && categories == null) {
                throw new IllegalArgumentException(
                        "The categories cannot be null when trying to checkin. The method was called improperly");
            }

            // note: we do this in this way in order to invoke hooks if they are
            APILocator.getContentletAPI()
                    .validateContentlet(contentlet, contentRelationships, categories);

            if (contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME) == null) {
                canLock(contentlet, user, respectFrontendRoles);
            }
            contentlet.setModUser(user.getUserId());
            // start up workflow
            final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
            WorkflowProcessor workflow = null;

            if (contentlet.getMap().get(Contentlet.DISABLE_WORKFLOW) == null &&
                    UtilMethods.isSet(contentlet.getActionId()) &&
                    (null == contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS) ||
                            Boolean.FALSE.equals(
                                    contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS))
                    )) {
                workflow = workflowAPI.fireWorkflowPreCheckin(contentlet, user);
            }

            workingContentlet = contentlet;
            if (createNewVersion) {
                workingContentlet = findWorkingContentlet(contentlet);
            }
            String workingContentletInode =
                    (workingContentlet == null) ? "" : workingContentlet.getInode();

            boolean priority = contentlet.isLowIndexPriority();

            Boolean dontValidateMe = (Boolean) contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME);
            Boolean disableWorkflow = (Boolean) contentlet.getMap()
                    .get(Contentlet.DISABLE_WORKFLOW);

            final boolean isNewContent = !InodeUtils.isSet(workingContentletInode);

            if (contentlet.getLanguageId() == 0) {
                Language defaultLanguage = languageAPI.getDefaultLanguage();
                contentlet.setLanguageId(defaultLanguage.getId());
            }

            contentlet.setModUser(user.getUserId());

            if (contentlet.getOwner() == null || contentlet.getOwner().length() < 1) {
                contentlet.setOwner(user.getUserId());
            }

            final User systemUser = APILocator.getUserAPI().getSystemUser();

            //ContentletRaw keeps a copy with everything that was originally sent from the front-end including binaries etc..
            //While the original contentlet first will get stuff marked for delete and then refreshed after saved in the database.
            final Contentlet contentletRaw = populateHost(contentlet);

            if (contentlet.getMap().get("_use_mod_date") != null) {
                    /*
                     When a content is sent using the remote push publishing we want to respect the modification
                     dates the content already had.
                     */
                contentlet.setModDate((Date) contentlet.getMap().get("_use_mod_date"));
            } else {
                contentlet.setModDate(new Date());
            }

            // Keep the 5 properties BEFORE store the contentlet on DB.
            final String contentPushPublishDate = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_PUBLISH_DATE);
            final String contentPushPublishTime = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_PUBLISH_TIME);
            final String contentPushPublishTimeZoneId = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_TIMEZONE_ID);
            final String contentPushExpireDate = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_EXPIRE_DATE);
            final String contentPushExpireTime = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_EXPIRE_TIME);
            final String contentPushNeverExpire = contentlet.getStringProperty(
                    Contentlet.WORKFLOW_NEVER_EXPIRE);
            final String contentWhereToSend = contentlet.getStringProperty(
                    Contentlet.WHERE_TO_SEND);
            final String filterKey = contentlet.getStringProperty(Contentlet.FILTER_KEY);
            final String iWantTo = contentlet.getStringProperty(Contentlet.I_WANT_TO);
            final String pathToMove = contentlet.getStringProperty(Contentlet.PATH_TO_MOVE);

                /*
                 For HTMLPages get the url of the page sent by the user, we use the Contentlet object to
                 move around that url but we DON'T want what url saved in the contentlet table, the URL
                 for HTMLPages must be retrieve it from the Identifier.
                 */
            String htmlPageURL = null;

            if (contentlet.isHTMLPage()) {
                //Getting the URL saved on the contentlet form
                htmlPageURL = contentletRaw.getStringProperty(HTMLPageAssetAPI.URL_FIELD);
                //Clean-up the contentlet object, we don' want to persist this URL in the db
                removeURLFromContentlet(contentlet);

                try {
                    //Verify if the template needs to be update for all versions of the content page
                    updateTemplateInAllLanguageVersions(contentlet, user);
                } catch (NotFoundInDbException e) {
                    Logger.debug(ESContentletAPIImpl.class, e.getMessage(), e);
                }
            }

            final boolean structureHasAHostField = hasAHostField(contentlet.getStructureInode());

            //Preparing the tags info to be related to this contentlet
            final HashMap<String, String> tagsValues = new HashMap<>();
            final String tagsHost = prepareTags(contentlet, user, contentType,
                    structureHasAHostField,
                    tagsValues);

            final IndexPolicy indexPolicy = contentlet.getIndexPolicy();
            final IndexPolicy indexPolicyDependencies = contentlet.getIndexPolicyDependencies();

            boolean changedURI = addOrUpdateContentletIdentifier(contentlet, contentletRaw,
                    existingIdentifier, existingInode, htmlPageURL);

            if (shouldRemoveOldHostCache(contentlet, oldHostId)) {
                CacheLocator.getVanityURLCache().remove(oldHostId, contentlet.getLanguageId());
            }

            contentlet = applyNullProperties(contentlet);
            contentlet = SET_DEFAULT_VALUES.get() && isNewContent? setDefaultValues(contentlet):contentlet;

            //This is executed first hand to create the inode-contentlet relationship.
            if (InodeUtils.isSet(existingInode)) {
                contentlet = contentFactory.save(contentlet, existingInode);
            } else {
                contentlet = contentFactory.save(contentlet);
            }

           if (hasUniqueField(contentType)) {
               uniqueFieldValidationStrategyResolver.get().get().afterSaved(contentlet, isNewContent);
           }


            contentlet.setIndexPolicy(indexPolicy);
            contentlet.setIndexPolicyDependencies(indexPolicyDependencies);

            contentlet = relateTags(contentlet, tagsValues, tagsHost);

            APILocator.getVersionableAPI().setWorking(contentlet);

            if (workingContentlet == null) {
                workingContentlet = contentlet;
            }

            final boolean movedContentDependencies = (createNewVersion
                    || contentRelationships != null
                    || categories != null);

            if (movedContentDependencies) {
                contentlet.setBoolProperty(CHECKIN_IN_PROGRESS, Boolean.TRUE);
                moveContentDependencies(workingContentlet, contentlet, contentRelationships,
                        categories, user, respectFrontendRoles);
            }

            // Refreshing permissions
            if (structureHasAHostField && !isNewContent) {
                permissionAPI.resetPermissionReferences(contentlet);
            }

            // Publish once if needed and reindex once if needed. The publish
            // method reindexes.
            contentlet.setLowIndexPriority(priority);

            //set again the don't validate me and disable workflow properties
            //if they were set
            if (dontValidateMe != null) {
                contentlet.setProperty(Contentlet.DONT_VALIDATE_ME, dontValidateMe);
            }

            if (disableWorkflow != null) {
                contentlet.setProperty(Contentlet.DISABLE_WORKFLOW, disableWorkflow);
            }

            handleBinaries(contentlet, createNewVersion, contentType, workingContentlet,
                    contentletRaw);

            updatePublishAndExpireDates(contentlet, systemUser, contentletRaw);

            final Structure hostStructure = CacheLocator.getContentTypeCache()
                    .getStructureByVelocityVarName("Host");
            if (InodeUtils.isSet(contentlet.getIdentifier()) && contentlet.getStructureInode()
                    .equals(hostStructure.getInode())) {
                final HostAPI hostAPI = APILocator.getHostAPI();
                hostAPI.updateCache(new Host(contentlet));

                final ContentletCache cc = CacheLocator.getContentletCache();
                final Identifier ident = APILocator.getIdentifierAPI().find(contentlet);
                final List<Contentlet> contentlets = findAllVersions(ident, systemUser,
                        respectFrontendRoles);
                for (final Contentlet c : contentlets) {
                    final Host h = new Host(c);
                    cc.remove(h.getHostname());
                    cc.remove(h.getIdentifier());
                }

                hostAPI.updateMenuLinks(new Host(workingContentlet), new Host(contentlet));

                //update tag references
                String oldTagStorageId = "SYSTEM_HOST";
                if (workingContentlet.getMap().get("tagStorage") != null) {
                    oldTagStorageId = workingContentlet.getMap().get("tagStorage").toString();
                }

                String newTagStorageId = "SYSTEM_HOST";
                if (contentlet.getMap().get("tagStorage") != null) {
                    newTagStorageId = contentlet.getMap().get("tagStorage").toString();
                }
                tagAPI.updateTagReferences(contentlet.getIdentifier(), oldTagStorageId,
                        newTagStorageId);
            }

            invalidateLanguageVariableCache(contentlet);

            flushMenuCacheIfNeeded(contentlet, workingContentlet, isNewContent, systemUser);

            invalidateThenReindex(contentlet, createNewVersion, user, changedURI, isNewContent,
                    structureHasAHostField,
                    movedContentDependencies);

            // Set the properties again after the store on DB and before the fire on an Actionlet.
            contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE, contentPushPublishDate);
            contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME, contentPushPublishTime);
            contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID,
                    contentPushPublishTimeZoneId);
            contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE, contentPushExpireDate);
            contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME, contentPushExpireTime);
            contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE, contentPushNeverExpire);
            contentlet.setStringProperty(Contentlet.WHERE_TO_SEND, contentWhereToSend);
            contentlet.setStringProperty(Contentlet.FILTER_KEY, filterKey);
            contentlet.setStringProperty(Contentlet.I_WANT_TO, iWantTo);
            if (UtilMethods.isSet(pathToMove)) {
                contentlet.setStringProperty(Contentlet.PATH_TO_MOVE, pathToMove);
            }

            //workflowAPI.
            if (workflow != null) {
                workflow.setContentlet(contentlet);
                workflowAPI.fireWorkflowPostCheckin(workflow);
            }

            new ContentletLoader().invalidate(contentlet);
            if (isNewContent) {
                // we mark as new. b/c next actions on the pipe may need to know if it is new or not.
                contentlet.setProperty(IS_NEW_CONTENT, true);
            }
        } catch (Exception e) {
            if (createNewVersion && workingContentlet != null && UtilMethods.isSet(
                    workingContentlet.getInode())) {
                APILocator.getVersionableAPI().setWorking(workingContentlet);
            }

            if (e instanceof DotContentletValidationException) {
                Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
            } else {
                Logger.error(this, e.getMessage(), e);
            }

            bubbleUpException(e);
        }
        return contentlet;
    }

    private Contentlet setDefaultValues(final Contentlet contentlet) {

        final List<com.dotcms.contenttype.model.field.Field> fields = Try.of(()->contentlet.getContentType().fields()).getOrElse(Collections.emptyList());
        final Map<String, Object> map = contentlet.getMap();
        Logger.debug(this, ()-> "Setting default values for the contentlet: " + contentlet.getIdentifier());
        // check default values for fields not coming on the map
        for (final com.dotcms.contenttype.model.field.Field field : fields) {

            if (!map.containsKey(field.variable()) && UtilMethods.isSet(field.defaultValue())) {

                try {
                    this.setContentletProperty(contentlet, field, field.defaultValue());
                } catch (Exception e) {


                    Logger.error(this, "Can not set the default value: " + field.defaultValue() +
                            " to the field: " + field.variable() + ", on the ct: " + Try.of(()->contentlet.getContentType().variable()).getOrElse("Unknown"));
                }
            }
        }

        return contentlet;
    }

    private static boolean hasUniqueField(ContentType contentType) {
        return contentType.fields().stream().anyMatch(field -> field.unique());
    }

    private boolean shouldRemoveOldHostCache(Contentlet contentlet, String oldHostId) {
        return contentlet.getBoolProperty(Contentlet.TO_BE_PUBLISH) &&
                contentlet.isVanityUrl() &&
                UtilMethods.isSet(oldHostId) &&
                !contentlet.getHost().equals(oldHostId);
    }


    /**
     * if we're not provided with an identifier this will attempt generating one. In case an
     * identifier already exists, it will be updated if needed. For example: when a folder path or
     * host is changed The inode is also generated here. For it is used internally to avoid
     * collisions on the identifier table generating a unique asset name. We're taking advantage of
     * the exising logic on {@link ESContentFactoryImpl}'s save method. That always explore the
     * given contentlet trying to find a provided inode. Like this:
     * <pre>
     * {@code
     *     final String inode = getInode(existingInode, contentlet);
     * }
     * </pre>
     *
     * @param contentlet         the modified copy contentlet we want to save. After this method
     *                           execution, this contentlet will contain the new identifier and
     *                           inode (if applies)
     * @param contentletRaw      the incoming copy that holds every value passed from the front-end
     * @param existingIdentifier if internalCheckin decides we need to use an existing identifier we
     *                           will use it
     * @param existingInode      if internalCheckin decides we need to use an existing inode we will
     *                           use it
     * @param htmlPageURL        for pages we use the url to generate a unique asset name wen saving
     *                           into the the contentlet table
     * @return A boolean indicating if the contentlet's URI changed
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private boolean addOrUpdateContentletIdentifier(final Contentlet contentlet,
            final Contentlet contentletRaw,
            final String existingIdentifier, final String existingInode, final String htmlPageURL)
            throws DotSecurityException, DotDataException {

        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User systemUser = APILocator.systemUser();

        Identifier identifier;
        if (!InodeUtils.isSet(contentlet.getIdentifier())) {

            //Adding back temporarily the page URL to the contentlet, is needed in order to create a proper Identifier
            addURLToContentlet(contentlet, htmlPageURL);
            try {

                final Treeable parent;
                if (UtilMethods.isSet(contentletRaw.getFolder()) && !contentletRaw.getFolder()
                        .equals(FolderAPI.SYSTEM_FOLDER)) {
                    parent = folderAPI
                            .find(contentletRaw.getFolder(), systemUser, false);
                } else {
                    parent = hostAPI.find(contentlet.getHost(), systemUser, false);
                }

                //We're gonna need a to assign the inode if we want to generate a valid identifier.
                //Inode is used internally by identifierFactory to avoid collisions on identifier table generating a unique asset name
                //Later this very same inode is grabbed to be used when saving content into the contentlet table
                if (!InodeUtils.isSet(existingInode)) {
                    contentlet.setInode(UUIDGenerator.generateUuid());
                } else {
                    contentlet.setInode(existingInode);
                }

                final Contentlet asset = contentlet.isFileAsset() ? contentletRaw : contentlet;
                identifier = existingIdentifier != null ?
                        identifierAPI.createNew(asset, parent, existingIdentifier) :
                        identifierAPI.createNew(asset, parent);

                contentlet.setIdentifier(identifier.getId());
            } finally {
                //Clean-up the contentlet object again..., we don' want to persist this URL in the db
                removeURLFromContentlet(contentlet);
            }
            return false;
        } else {
            //Existing contentlet getting updated.
            identifier = identifierAPI.find(contentlet);

            if (null == identifier || !UtilMethods.isSet(identifier.getId())) {
                throw new DotDataException(
                        String.format("Contentlet with ID '%s' has not been found.",
                                contentlet.getIdentifier()));
            }

            final String oldURI = identifier.getURI();

            // make sure the identifier is removed from cache
            // because changes here may affect URI then IdentifierCache
            // can't remove it
            CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);
            // Once the contentlet is saved, it gets refresh from the db. for which the incoming data gets lost.
            // Therefore here we need to make sure we use the original contentlet that comes with the info passed from the ui.
            final String hostId =
                    UtilMethods.isSet(contentletRaw.getHost()) ? contentletRaw.getHost()
                            : contentlet.getHost();
            identifier.setHostId(hostId);
            if (contentlet.isFileAsset()) {
                try {
                    if (contentletRaw.getBinary(FileAssetAPI.BINARY_FIELD) == null) {
                        final String binaryIdentifier = contentletRaw.getIdentifier() != null
                                ? contentletRaw.getIdentifier() : BLANK;
                        final String binaryNode =
                                contentletRaw.getInode() != null ? contentletRaw.getInode()
                                        : BLANK;

                        if (SKIP_FILE_ASSET_BINARY_VALIDATION_FLAG.get()) {
                            Logger.debug(this,
                                    "Missing binary field " + FileAssetAPI.BINARY_FIELD
                                            + " for identifier: " + binaryIdentifier
                                            + ", inode: " + binaryNode);
                        } else {
                            throw new FileAssetValidationException(
                                    "Unable to validate field: " + FileAssetAPI.BINARY_FIELD
                                            + " identifier: " + binaryIdentifier
                                            + " inode: " + binaryNode);
                        }
                    } else {
                        //We no longer use the old BinaryField to recover the file name.
                        //From now on we'll recover such value from the field "fileName" presented on the screen.
                        //The physical file asset is just an internal piece that is mapped to the system asset-name.
                        //The file per-se no longer can be renamed. We can only modify the asset-name that refers to it.
                        final String assetName = (String) contentletRaw.getMap()
                                .get(FileAssetAPI.FILE_NAME_FIELD);
                        identifier.setAssetName(UtilMethods.isSet(assetName) ?
                                assetName :
                                contentletRaw.getBinary(FileAssetAPI.BINARY_FIELD).getName()
                        );
                    }
                } catch (IOException e) {
                    Logger.error(this.getClass(), "Error handling Binary Field.", e);
                }
            } else if (contentlet.isHTMLPage()) {
                //For HTML Pages - The asset name maps to the page URL
                identifier.setAssetName(htmlPageURL);
            }
            if (UtilMethods.isSet(contentletRaw.getFolder()) && !contentletRaw.getFolder()
                    .equals(FolderAPI.SYSTEM_FOLDER)) {
                final Folder folder = folderAPI.find(contentletRaw.getFolder(), systemUser, false);
                if (null != folder) {
                    Identifier folderIdent = identifierAPI.find(folder.getIdentifier());
                    identifier.setParentPath(folderIdent.getPath());
                }
            } else {
                identifier.setParentPath(StringPool.FORWARD_SLASH);
            }
            identifier = identifierAPI.save(identifier);

            return UtilMethods.isSet(oldURI) && !oldURI.equals(identifier.getURI());
        }
    }


    /**
     * This takes the incoming contentlet makes a copy out of it and includes all the system fields
     * in it so they can be used to generate a sound json representation of the contentlet System
     * fields are required to generate a json representation of the contentlet
     *
     * @param contentlet
     * @param contentletRaw
     * @param tagsValues
     * @return
     */
    private Contentlet includeSystemFields(final Contentlet contentlet,
            final Contentlet contentletRaw, final Map<String, String> tagsValues,
            final List<Category> categories, final User user) {
        final ContentType contentType = contentlet.getContentType();
        final Contentlet systemFieldsInclusiveContentlet = new Contentlet(contentlet);
        final Map<String, Object> map = systemFieldsInclusiveContentlet.getMap();
        final List<com.dotcms.contenttype.model.field.Field> systemFields = contentType.fields()
                .stream()
                .filter(field -> field.dataType() == DataTypes.SYSTEM).collect(Collectors.toList());

        for (final com.dotcms.contenttype.model.field.Field systemField : systemFields) {
            if (systemField instanceof TagField) {
                final String tagValues = tagsValues.get(systemField.variable());
                if (StringUtils.isNotEmpty(tagValues)) {
                    map.put(systemField.variable(), tagValues);
                }
                continue;
            }
            if (systemField instanceof CategoryField) {
                //This is a bit obscure logic. In order to find if a category belongs into a category-field we need to extract the value stored in the field it self
                final List<String> selectedCategories = new ArrayList<>();
                final Category parent = Try.of(
                        () -> categoryAPI.find(systemField.values(), user, false)).getOrNull();
                if (null != parent) {
                    for (final Category child : categories) {
                        if (categoryAPI.isParent(child, parent, user)) {
                            selectedCategories.add(child.getCategoryVelocityVarName());
                        }
                    }
                }
                map.put(systemField.variable(), selectedCategories);
                continue;
            }
            map.put(systemField.variable(), contentletRaw.get(systemField.variable()));

        }
        return systemFieldsInclusiveContentlet;
    }

    /**
     * Invalidate the respective caches depending on the type of contentlet passed down And call the
     * reindex
     *
     * @param contentlet
     * @param createNewVersion
     * @param user
     * @param changedURI
     * @param isNewContent
     * @param structureHasAHostField
     * @param movedContentDependencies
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void invalidateThenReindex(final Contentlet contentlet, final boolean createNewVersion,
            final User user,
            final boolean changedURI, final boolean isNewContent,
            final boolean structureHasAHostField,
            final boolean movedContentDependencies) throws DotDataException, DotSecurityException {
        boolean isLive = false;
        if (contentlet.isHTMLPage()) {
            try {
                isLive = contentlet.isLive();
            } catch (DotStateException e) {
                // Cache miss, remove HTML page entry
                CacheLocator.getIdentifierCache()
                        .removeFromCacheByIdentifier(
                                contentlet.getIdentifier());
            }

            new PageLoader().invalidate(contentlet);

        } else {
            isLive = contentlet.isLive();
        }
        if (isLive) {
            publishAssociated(contentlet, isNewContent, createNewVersion);
        } else {
            if (!isNewContent) {
                new ContentletLoader().invalidate(contentlet);

            }

            if (movedContentDependencies) {
                ThreadContextUtil.ifReindex(
                        () -> indexAPI.addContentToIndex(contentlet, INCLUDE_DEPENDENCIES),
                        INCLUDE_DEPENDENCIES);
            } else if (ThreadContextUtil.isReindex()) {
                indexAPI.addContentToIndex(contentlet, false);
            }
        }

        if (contentlet.isVanityUrl()) {
            //remove from cache
            APILocator.getVanityUrlAPI().invalidateVanityUrl(contentlet);
        }

        if (contentlet.isKeyValue()) {
            //remove from cache
            CacheLocator.getKeyValueCache().remove(contentlet);
        }

        //If the URI changed and we're looking at FileAsset we need to evict all other language instances
        if (contentlet.isFileAsset() && changedURI) {
            HibernateUtil.addCommitListener(() -> {
                cleanupCacheOnChangedURI(contentlet);
            });
        }

        if (structureHasAHostField && changedURI) {
            final DotConnect dc = new DotConnect();
            dc.setSQL(
                    "select working_inode,live_inode from contentlet_version_info where identifier=? and lang<>?");
            dc.addParam(contentlet.getIdentifier());
            dc.addParam(contentlet.getLanguageId());
            final List<Map<String, Object>> others = dc.loadResults();
            for (final Map<String, Object> other : others) {
                final String workingInode = (String) other.get("working_inode");
                indexAPI.addContentToIndex(find(workingInode, user, false));
                final String liveInode = (String) other.get("live_inode");
                if (UtilMethods.isSet(liveInode) && !liveInode.equals(workingInode)) {
                    indexAPI.addContentToIndex(find(liveInode, user, false));
                }
            }
        }
    }

    /**
     * Prepare Tags Info so they can be related later on We expect tagsValues and tagsHost
     *
     * @param contentlet
     * @param user
     * @param contentType
     * @param structureHasAHostField
     * @param tagsValues
     * @return tagsHost
     */
    private String prepareTags(final Contentlet contentlet, final User user,
            final ContentType contentType,
            final boolean structureHasAHostField, final HashMap<String, String> tagsValues) {
        String tagsHost = Host.SYSTEM_HOST;

        for (com.dotcms.contenttype.model.field.Field field : contentType.fields(TagField.class)) {
            String value = null;
            if (contentlet.getStringProperty(field.variable()) != null) {
                value = contentlet.getStringProperty(field.variable()).trim();
            }

            if (UtilMethods.isSet(value)) {

                if (structureHasAHostField || UtilMethods.isSet(contentlet.getHost())) {
                    Host host = null;
                    try {
                        host = APILocator.getHostAPI().find(contentlet.getHost(), user, true);
                    } catch (Exception e) {
                        Logger.error(this, "Unable to get contentlet host", e);
                    }
                    if ((!UtilMethods.isSet(host) || !UtilMethods.isSet(host.getInode()))
                            || host.getIdentifier().equals(Host.SYSTEM_HOST)) {
                        tagsHost = Host.SYSTEM_HOST;
                    } else {
                        tagsHost = host.getIdentifier();
                    }
                }

                //Add these tags to a temporal list in order to relate them later to this contentlet
                tagsValues.put(field.variable(), value);

                //We should not store the tags inside the field, the relation must only exist on the tag_inode table (Unless we want to save it as json)
                contentlet.setStringProperty(field.variable(), "");
            } else if (value != null && value.equals(
                    "")) { // empty string means wiping out the tags
                tagsValues.put(field.variable(), value);
            }
        }
        return tagsHost;
    }

    /**
     * Pages and file need menu cache flush
     *
     * @param contentlet
     * @param workingContentlet
     * @param isNewContent
     * @param systemUser
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void flushMenuCacheIfNeeded(final Contentlet contentlet,
            final Contentlet workingContentlet,
            final boolean isNewContent, final User systemUser)
            throws DotDataException, DotSecurityException {
        // both file & page as content might trigger a menu cache flush
        if (contentlet.isFileAsset() || contentlet.isHTMLPage()) {
            final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
            final Host host = APILocator.getHostAPI().find(
                    identifier.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
            final Folder folder = APILocator.getFolderAPI()
                    .findFolderByPath(identifier.getParentPath(), host,
                            systemUser, false);

            final boolean shouldRefresh =
                    (contentlet.isFileAsset()
                            && RefreshMenus.shouldRefreshMenus(
                            APILocator.getFileAssetAPI().fromContentlet(
                                    workingContentlet)
                            , APILocator.getFileAssetAPI().fromContentlet(contentlet),
                            isNewContent))
                            ||
                            (contentlet.isHTMLPage()
                                    && RefreshMenus.shouldRefreshMenus(
                                    APILocator.getHTMLPageAssetAPI().fromContentlet(
                                            workingContentlet)
                                    , APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet),
                                    isNewContent));

            if (shouldRefresh) {
                RefreshMenus.deleteMenu(folder);
                CacheLocator.getNavToolCache().removeNav(host.getIdentifier(), folder.getInode());
            }
        }
    }

    /**
     * Update System Publish and Expire Dates
     *
     * @param contentlet
     * @param systemUser
     * @param contentletRaw
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void updatePublishAndExpireDates(final Contentlet contentlet, final User systemUser,
            final Contentlet contentletRaw)
            throws DotDataException, DotSecurityException {
        // lets update identifier's sysPubDate & sysExpireDate
        if ((contentlet != null) && InodeUtils.isSet(contentlet.getIdentifier())) {
            final Structure st = contentlet.getStructure();
            if (UtilMethods.isSet(st.getPublishDateVar()) || UtilMethods.isSet(
                    st.getExpireDateVar())) {
                Identifier ident = APILocator.getIdentifierAPI().find(contentlet);
                boolean save = false;
                if (UtilMethods.isSet(st.getPublishDateVar())) {
                    Date pdate = contentletRaw.getDateProperty(st.getPublishDateVar());
                    contentlet.setDateProperty(st.getPublishDateVar(), pdate);
                    if ((ident.getSysPublishDate() == null && pdate != null) ||
                            // was null and now we have a value
                            (ident.getSysPublishDate() != null &&
                                    //wasn't null and now is null or different
                                    (pdate == null || !pdate.equals(ident.getSysPublishDate())))) {
                        ident.setSysPublishDate(pdate);
                        save = true;
                    }
                }
                if (UtilMethods.isSet(st.getExpireDateVar())) {
                    Date edate = contentletRaw.getDateProperty(st.getExpireDateVar());
                    contentlet.setDateProperty(st.getExpireDateVar(), edate);
                    if ((ident.getSysExpireDate() == null && edate != null) ||
                            // was null and now we have a value
                            (ident.getSysExpireDate() != null &&
                                    //wasn't null and now is null or different
                                    (edate == null || !edate.equals(ident.getSysExpireDate())))) {
                        ident.setSysExpireDate(edate);
                        save = true;
                    }
                }
                if (!contentlet.isLive() && UtilMethods.isSet(
                        st.getExpireDateVar())) {//Verify if the structure have a Expire Date Field set
                    if (contentlet
                            .getMap().get(Contentlet.DONT_VALIDATE_ME) == null
                            || !(Boolean) contentlet
                            .getMap().get(Contentlet.DONT_VALIDATE_ME)) {
                        if (UtilMethods.isSet(ident.getSysExpireDate()) && ident.getSysExpireDate()
                                .before(new Date())) {
                            throw new DotContentletValidationException(
                                    "message.contentlet.expired");
                        }
                    }
                }
                if (save) {

                    // publish/expire dates changed
                    APILocator.getIdentifierAPI().save(ident);

                    // we take all inodes associated with that identifier
                    // remove them from cache and then reindex them
                    final List<ContentletVersionInfo> list = APILocator.getVersionableAPI()
                            .findContentletVersionInfos(ident.getId());

                    final List<String> inodes = new ArrayList<>();
                    for (final ContentletVersionInfo cvi : list) {
                        inodes.add(cvi.getWorkingInode());
                        if (UtilMethods.isSet(cvi.getLiveInode()) && !cvi.getWorkingInode()
                                .equals(cvi.getLiveInode())) {
                            inodes.add(cvi.getLiveInode());
                        }
                    }
                    for (final String inode : inodes) {
                        CacheLocator.getContentletCache().remove(inode);
                        final Contentlet ct = APILocator.getContentletAPI()
                                .find(inode, systemUser, false);
                        APILocator.getContentletIndexAPI().addContentToIndex(ct, false);
                    }
                }
            }
        }
    }

    /**
     * Takes the original contentlet passed down from internalCheckin Relate the tags using the
     * respective api and add the related tags back into the original contentlet that was passed
     * down
     *
     * @param contentlet contetlet reference
     * @param tagsValues prepared tags
     * @param tagsHost   prepared
     * @return mutated contentlet
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Contentlet relateTags(final Contentlet contentlet, final Map<String, String> tagsValues,
            final String tagsHost) throws DotSecurityException, DotDataException {
        //Relate the tags with the saved contentlet
        for (final Entry<String, String> tagEntry : tagsValues.entrySet()) {
            //From the given CSV tags names list search for the tag objects and if does not exist create them
            final List<Tag> tagList = tagAPI.getTagsInText(tagEntry.getValue(), tagsHost);

            // empty string for tag field value wipes out existing tags
            if (UtilMethods.isSet(tagList) || BLANK.equals(tagEntry.getValue())) {
                tagAPI.deleteTagInodesByInodeAndFieldVarName(contentlet.getInode(),
                        tagEntry.getKey());
            }

            for (final Tag tag : tagList) {
                //Relate the found/created tag with this contentlet
                tagAPI.addContentletTagInode(tag, contentlet.getInode(), tagEntry.getKey());
            }
            //Adding tags back as field to be returned
            if (tagEntry.getValue() != null && !BLANK.equals(tagEntry.getValue())) {
                contentlet.setProperty(tagEntry.getKey(), tagEntry.getValue());
            } else {
                contentlet.setProperty(tagEntry.getKey(), null);
            }
        }
        return contentlet;
    }

    /**
     * @param contentlet        Just Saved contentlet it is supposed to have at least an inode
     * @param createNewVersion
     * @param contentType       ContentType
     * @param workingContentlet Working version of our content. If different from the actual then it
     *                          has prior version's inode
     * @param contentletRaw     incoming content loaded from the ui. This one has all the files
     * @return bool that says whether or not there were files to process
     * @throws DotDataException
     */
    private boolean handleBinaries(final Contentlet contentlet, final boolean createNewVersion,
            final ContentType contentType, final Contentlet workingContentlet,
            final Contentlet contentletRaw) throws DotDataException {

        // http://jira.dotmarketing.net/browse/DOTCMS-1073
        // storing binary files in file system.
        Logger.debug(this, "ContentletAPIImpl : storing binary files in file system.");

        final boolean validateEmptyFile = UtilMethods.isSetOrGet(
                contentlet.getBoolProperty(Contentlet.VALIDATE_EMPTY_FILE), true);

        // Binary Files
        final String newInode = contentlet.getInode();
        final String oldInode = workingContentlet.getInode();

        File newDir = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                + newInode.charAt(0)
                + File.separator
                + newInode.charAt(1) + File.separator + newInode);
        newDir.mkdirs();

        File oldDir = null;
        if (UtilMethods.isSet(oldInode)) {
            oldDir = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath()
                    + File.separator + oldInode.charAt(0)
                    + File.separator + oldInode.charAt(1)
                    + File.separator + oldInode);
        }

        File tmpDir = null;
        if (UtilMethods.isSet(oldInode)) {
            tmpDir = new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()
                    + File.separator + oldInode.charAt(0)
                    + File.separator + oldInode.charAt(1)
                    + File.separator + oldInode);
        }

        boolean binaryHandled = false;

        // loop over the new field values
        // if we have a new temp file or a deleted file
        // do it to the new inode directory
        for (com.dotcms.contenttype.model.field.Field field : contentType.fields(
                BinaryField.class)) {
            try {

                final String velocityVarNm = field.variable();
                File incomingFile = contentletRaw.getBinary(velocityVarNm);
                if (validateEmptyFile && incomingFile != null && incomingFile.length() == 0
                        && !Config.getBooleanProperty("CONTENT_ALLOW_ZERO_LENGTH_FILES", false)) {
                    throw new DotContentletStateException(
                            "Cannot checkin 0 length file: " + incomingFile);
                }
                final File binaryFieldFolder = new File(
                        newDir.getAbsolutePath() + File.separator + velocityVarNm);

                // if the user has removed this file via ui
                if (incomingFile == null || incomingFile.getAbsolutePath().contains("-removed-")) {
                    FileUtil.deltree(binaryFieldFolder);
                    contentlet.setBinary(velocityVarNm, null);

                    //For removed files we should cleanup any existing metadata.
                    if (contentlet.isFileAsset()) {
                        fileMetadataAPI.removeMetadata(contentlet);
                    }

                } else { // if we have an incoming file
                    if (incomingFile.exists()) {

                        //If the incoming file is temp resource we need to find out if there is any metadata associated
                        final Optional<String> tempResourceId = tempApi.getTempResourceId(
                                incomingFile);

                        //The physical file name is preserved across versions.
                        //No need to update the name. We will only reference the file through the logical asset-name
                        final String oldFileName = incomingFile.getName();

                        File oldFile = null;
                        if (UtilMethods.isSet(oldInode)) {
                            //get old file
                            oldFile = new File(
                                    oldDir.getAbsolutePath() + File.separator + velocityVarNm
                                            + File.separator + oldFileName);

                            // do we have an inline edited file, if so use that
                            File editedFile = new File(
                                    tmpDir.getAbsolutePath() + File.separator + velocityVarNm
                                            + File.separator + WebKeys.TEMP_FILE_PREFIX
                                            + oldFileName);
                            if (editedFile.exists()) {
                                incomingFile = editedFile;
                            }
                        }

                        //The file name must be preserved so it remains the same across versions.
                        final File newFile = new File(
                                newDir.getAbsolutePath() + File.separator + velocityVarNm
                                        + File.separator + oldFileName);
                        binaryFieldFolder.mkdirs();

                        // we move files that have been newly uploaded or edited
                        if (oldFile == null || !oldFile.equals(incomingFile)) {
                            if (!createNewVersion) {
                                // If we're calling a checkinWithoutVersioning method,
                                // then folder needs to be cleaned up in order to add the new file in it.
                                // Otherwise we will have the old file and incoming file at the same time
                                FileUtil.deltree(binaryFieldFolder);
                                binaryFieldFolder.mkdirs();
                            }
                            // We want to copy (not move) cause the same file could be in
                            // another field and we don't want to delete it in the first time.
                            final boolean contentVersionHardLink = Config
                                    .getBooleanProperty("CONTENT_VERSION_HARD_LINK", true);
                            FileUtil.copyFile(incomingFile, newFile, contentVersionHardLink,
                                    validateEmptyFile);

                        } else if (oldFile.exists()) {
                            // otherwise, we copy the files as hardlinks
                            final boolean contentVersionHardLink = Config
                                    .getBooleanProperty("CONTENT_VERSION_HARD_LINK", true);
                            FileUtil.copyFile(incomingFile, newFile, contentVersionHardLink,
                                    validateEmptyFile);
                        }

                        if (workingContentlet != contentlet) {
                            //This copies the metadata from version to version so we don't lose any any custom attribute previously added
                            fileMetadataAPI.copyCustomMetadata(workingContentlet, contentlet);
                            Logger.debug(ESContentletAPIImpl.class, String.format(
                                    "Metadata copied from inode: `%s` to  inode `%s` ",
                                    workingContentlet
                                            .getInode(), contentlet.getInode()));
                        }

                        contentlet.setBinary(velocityVarNm, newFile);
                        binaryHandled = true;

                        //This copies the metadata associated with the temp resource passed if any.
                        if (tempResourceId.isPresent()) {
                            final Optional<Metadata> optionalMetadata = fileMetadataAPI.getMetadata(
                                    tempResourceId.get());
                            if (optionalMetadata.isPresent()) {
                                final Metadata tempMeta = optionalMetadata.get();
                                fileMetadataAPI.putCustomMetadataAttributes(
                                        contentlet,
                                        ImmutableMap.of(velocityVarNm, tempMeta.getCustomMeta()));
                                Logger.debug(ESContentletAPIImpl.class,
                                        String.format("Metadata copied from temp resource: `%s` ",
                                                tempResourceId.get()));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new DotContentletValidationException(
                        "Error occurred while processing the file:" + e.getMessage(), e);
            }
        }
        return binaryHandled;
    }

    private void checkPermission(
            final Contentlet contentlet,
            final boolean respectFrontendRoles,
            final User user) throws DotDataException, DotSecurityException {

        DotPreconditions.checkNotNull(contentlet);

        if (InodeUtils.isSet(contentlet.getIdentifier())) {
            if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_WRITE,
                    user, respectFrontendRoles)) {
                this.throwSecurityException(contentlet, user);
            }
        } else if (!permissionAPI.doesUserHavePermission(contentlet.getContentType(),
                PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
            this.throwSecurityException(contentlet, user);
        }
    }

    /**
     * Given a ContentletRelationships object, verifies if there is any legacy relationship on it
     *
     * @param relationships
     * @return
     */
    private boolean hasLegacyRelationships(ContentletRelationships relationships) {
        for (ContentletRelationshipRecords records : relationships
                .getRelationshipsRecords()) {
            if (!records.getRelationship().isRelationshipField()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a ContentletRelationships with all relationships found on the contentlet map
     *
     * @param contentlet
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private ContentletRelationships getContentletRelationships(final Contentlet contentlet,
            final User user)
            throws DotDataException, DotSecurityException {
        ContentletRelationships contentRelationships = null;

        //Get all relationship fields
        final List<com.dotcms.contenttype.model.field.Field> relationshipFields = contentlet
                .getContentType().fields().stream()
                .filter(field -> field instanceof RelationshipField)
                .collect(Collectors.toList());

        if (contentlet.getMap() != null) {
            //verify if the contentlet map contains related content for each relationship field
            //and add it to the ContentletRelationships to be persisted
            for (final com.dotcms.contenttype.model.field.Field field : relationshipFields) {
                if (contentlet.getMap().containsKey(field.variable())) {
                    final Relationship relationship = relationshipAPI
                            .getRelationshipFromField(field, user);
                    final boolean hasParent = relationshipAPI.isChildField(relationship, field);

                    if (contentRelationships == null) {
                        contentRelationships = new ContentletRelationships(contentlet);
                    }
                    final ContentletRelationshipRecords relationshipRecords = contentRelationships.new ContentletRelationshipRecords(
                            relationship, hasParent);
                    relationshipRecords.getRecords()
                            .addAll((List<Contentlet>) contentlet.get(field.variable()));

                    contentRelationships.getRelationshipsRecords().add(relationshipRecords);
                }
            }
        }
        return contentRelationships;
    }

    private void cleanupCacheOnChangedURI(final Contentlet contentlet) {
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(contentlet.getIdentifier());
        //Need both live and working contentlets for they all need to be evicted from cache.
        try {
            final Set<Contentlet> allLangContentlets = new ImmutableSet.Builder<Contentlet>()
                    .addAll(getAllLanguages(contentlet, true,
                            APILocator.systemUser(), false))
                    .addAll(getAllLanguages(contentlet, false,
                            APILocator.systemUser(), false)).build();
            final ContentletCache contentletCache = CacheLocator.getContentletCache();
            for (final Contentlet content : allLangContentlets) {
                contentletCache.remove(content);
            }

        } catch (DotDataException | DotSecurityException e) {
            Logger.error(getClass(),
                    "Unable to cleanup cache fo"
                            + "r fileAsset identifier " + contentlet
                            .getIdentifier(), e);
        }
    }

    private void cleanup(final Contentlet contentlet) {
        if (null != contentlet) {
            contentlet.getMap().remove(CHECKIN_IN_PROGRESS);
            contentlet.cleanup();
        }
    }

    private void createLocalCheckinEvent(final Contentlet contentlet, final User user,
            final boolean createNewVersion) throws DotHibernateException {

        HibernateUtil.addCommitListener(
                () -> this.localSystemEventsAPI.notify(
                        new ContentletCheckinEvent<>(contentlet, createNewVersion, user))
        );
    }

    /**
     * Updates the Template of the specified Contentlet - in case it's an HTML Page - in all of its
     * existing language versions, only if its value is different from the current one. This update
     * operation can be overridden by setting the configuration parameter
     * {@code DO_NOT_UPDATE_TEMPLATES} as {@code true}.
     *
     * @param contentlet The {@link Contentlet} whose Template will be updated, if necessary
     * @param user       The {@link User} performing this action.
     * @throws DotDataException     An error occurred when interacting with the data source.
     * @throws DotSecurityException The specified user does not have the required permissions to
     *                              perform this action.
     */
    private void updateTemplateInAllLanguageVersions(final Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {
        final boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;
        final String DO_NOT_UPDATE_TEMPLATES = "DO_NOT_UPDATE_TEMPLATES";

        if (contentlet.getBoolProperty(DO_NOT_UPDATE_TEMPLATES)) {
            return;
        }
        if (UtilMethods.isSet(contentlet.getIdentifier())) {

            final Optional<com.dotcms.contenttype.model.field.Field> templateField = contentlet
                    .getContentType().fields().stream()
                    .filter(field -> HTMLPageAssetAPI.TEMPLATE_FIELD.equals(field.variable()))
                    .findFirst();

            if (templateField.isPresent()) {
                final String identifier = contentlet.getIdentifier();
                final String newTemplate = contentlet.get(HTMLPageAssetAPI.TEMPLATE_FIELD)
                        .toString();
                final Contentlet contentInAnyLang = findContentletByIdentifierAnyLanguage(
                        contentlet.getIdentifier(), contentlet.getVariantId());

                if (null == contentInAnyLang || !UtilMethods.isSet(
                        contentInAnyLang.getIdentifier())) {
                    final Contentlet contentletByIdentifierAnyLanguageArchived = findContentletByIdentifierAnyLanguage(
                            contentlet.getIdentifier(), true);

                    if (null == contentletByIdentifierAnyLanguageArchived || !UtilMethods.isSet(
                            contentletByIdentifierAnyLanguageArchived.getIdentifier())) {
                        throw new NotFoundInDbException(String.format(
                                "Contentlet with ID '%s' has not been found: ",
                                contentlet.getIdentifier()));
                    } else if (contentletByIdentifierAnyLanguageArchived.isArchived()) {
                        throw new DotDataException(String.format(
                                "Contentlet is currently marked as 'Archived'.",
                                contentlet.getIdentifier()));
                    } else {
                        return;
                    }
                }

                final String existingTemplate = loadField(contentInAnyLang.getInode(),
                        templateField.get()).toString();
                if (!existingTemplate.equals(newTemplate)) {
                    final List<ContentletVersionInfo> contentletVersions = APILocator.getVersionableAPI()
                            .findContentletVersionInfos(identifier, contentlet.getVariantId());

                    for (final ContentletVersionInfo version : contentletVersions) {
                        final Contentlet contentVersion = find(version.getWorkingInode(), user,
                                DONT_RESPECT_FRONTEND_ROLES);
                        if (contentlet.getInode().equals(contentVersion.getInode())) {
                            continue;
                        }

                        //Create a new working version with the template when the page version is live and working
                        final Contentlet newPageVersion = checkout(contentVersion.getInode(), user,
                                DONT_RESPECT_FRONTEND_ROLES);
                        newPageVersion.setBoolProperty(DO_NOT_UPDATE_TEMPLATES, true);
                        newPageVersion.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD,
                                newTemplate);
                        newPageVersion.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);

                        if (contentlet.getMap().containsKey(Contentlet.DISABLE_WORKFLOW)) {
                            newPageVersion.getMap().put(Contentlet.DISABLE_WORKFLOW,
                                    contentlet.getMap().get(Contentlet.DISABLE_WORKFLOW));
                        }
                        if (contentlet.getMap().containsKey(Contentlet.WORKFLOW_IN_PROGRESS)) {
                            newPageVersion.getMap().put(Contentlet.WORKFLOW_IN_PROGRESS,
                                    contentlet.getMap().get(Contentlet.WORKFLOW_IN_PROGRESS));
                        }

                        checkin(newPageVersion, user, DONT_RESPECT_FRONTEND_ROLES);
                    }
                }
            }
        }
    }

    private void pushSaveEvent(final Contentlet eventContentlet,
            final boolean eventCreateNewVersion) throws DotHibernateException {

        HibernateUtil.addCommitListener(
                () -> this.contentletSystemEventUtil.pushSaveEvent(eventContentlet,
                        eventCreateNewVersion), 100);
    }

    private List<Category> getExistingContentCategories(Contentlet contentlet)
            throws DotSecurityException, DotDataException {
        List<Category> cats = new ArrayList<>();
        Contentlet workingCon = findWorkingContentlet(contentlet);

        if (workingCon != null) {
            cats = categoryAPI.getParents(workingCon, APILocator.getUserAPI().getSystemUser(),
                    true);
        }
        return cats;
    }

    private void throwSecurityException(final Contentlet contentlet,
            final User user) throws DotSecurityException {

        final String userName = (user != null ? user.getUserId() : "Unknown");
        final String message = UtilMethods.isSet(contentlet.getIdentifier()) ?
                "User: " + userName + " doesn't have write permissions to Contentlet: "
                        + contentlet.getIdentifier() :
                "User: " + userName + " doesn't have write permissions to create the Contentlet";

        throw new DotSecurityException(message);
    }

    // todo: should be this in a transaction???
    @Override
    public List<Contentlet> checkout(List<Contentlet> contentlets, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        List<Contentlet> result = new ArrayList<>();
        for (Contentlet contentlet : contentlets) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    // todo: should be this in a transaction???
    @Override
    public List<Contentlet> checkoutWithQuery(String luceneQuery, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        List<Contentlet> result = new ArrayList<>();
        List<Contentlet> cons = search(luceneQuery, 0, -1, "", user, respectFrontendRoles);
        for (Contentlet contentlet : cons) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    // todo: should be this in a transaction???
    @Override
    public List<Contentlet> checkout(String luceneQuery, User user, boolean respectFrontendRoles,
            int offset, int limit)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        List<Contentlet> result = new ArrayList<>();
        List<Contentlet> cons = search(luceneQuery, limit, offset, "", user, respectFrontendRoles);
        for (Contentlet contentlet : cons) {
            result.add(checkout(contentlet.getInode(), user, respectFrontendRoles));
        }
        return result;
    }

    @WrapInTransaction
    @Override
    public Contentlet checkout(final String contentletInode, final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        //return new version
        final Contentlet contentlet = find(contentletInode, user, respectFrontendRoles);

        canLock(contentlet, user);
        lock(contentlet, user, respectFrontendRoles);
        final Contentlet workingContentlet = new Contentlet();
        Map<String, Object> cmap = contentlet.getMap();
        workingContentlet.setStructureInode(contentlet.getStructureInode());
        workingContentlet.setInode(contentletInode);
        workingContentlet.setVariantId(contentlet.getVariantId());
        copyProperties(workingContentlet, cmap);
        workingContentlet.setInode("");

        /*
        The checkin is assuming that HTMLPages are going to have an URL field as it
        is always sent from the UI, but if we checkout the content and then save (checkin) we
        fail as the checkout is not populating that field.
        We need to remember the URL field in pages is a calculated value and should not be stored.
         */
        if (workingContentlet.getContentType().baseType() == BaseContentType.HTMLPAGE
                && UtilMethods.isSet(workingContentlet.getIdentifier())) {

            final Identifier htmlPageIdentifier = APILocator.getIdentifierAPI()
                    .find(workingContentlet.getIdentifier());
            workingContentlet
                    .setStringProperty(HTMLPageAssetAPI.URL_FIELD,
                            htmlPageIdentifier.getAssetName());
        }

        return workingContentlet;
    }

    /**
     * This method moves categories and relationships dependencies
     *
     * @param fromContentlet
     * @param toContentlet
     * @param contentRelationships
     * @param categories
     * @param user
     * @param respect
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void moveContentDependencies(final Contentlet fromContentlet,
            final Contentlet toContentlet, ContentletRelationships contentRelationships,
            List<Category> categories, final User user, final boolean respect)
            throws DotDataException, DotSecurityException {

        //Handles Categories
        moveContentCategories(fromContentlet, toContentlet, categories, user, respect);

        //Handle Relationships
        moveContentRelationships(fromContentlet, toContentlet, contentRelationships, user);
    }

    /**
     * @param fromContentlet
     * @param toContentlet
     * @param contentRelationships
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void moveContentRelationships(final Contentlet fromContentlet,
            final Contentlet toContentlet,
            ContentletRelationships contentRelationships, final User user)
            throws DotDataException, DotSecurityException {

        if (contentRelationships == null) {
            return;
        }

        final ContentType contentType = fromContentlet.getContentType();
        final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        if (contentRelationships.getRelationshipsRecords().isEmpty()) {
            getWipeOutRelationships(contentRelationships, contentType, relationshipAPI);
        } else {
            //keep relationships as they are, but add related content limited by permissions
            addRestrictedContentForLimitedUser(fromContentlet, contentRelationships, user,
                    contentType,
                    relationshipAPI);
        }

        for (final ContentletRelationshipRecords cr : contentRelationships.getRelationshipsRecords()) {
            relateContent(toContentlet, cr, APILocator.getUserAPI().getSystemUser(), true);
        }
    }

    /**
     * This method keeps relationships as they are but also includes restricted content to the list
     * to avoid deleting it
     *
     * @param fromContentlet
     * @param contentRelationships
     * @param user
     * @param contentType
     * @param relationshipAPI
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void addRestrictedContentForLimitedUser(final Contentlet fromContentlet,
            final ContentletRelationships contentRelationships, final User user,
            final ContentType contentType,
            final RelationshipAPI relationshipAPI) throws DotDataException, DotSecurityException {
        for (ContentletRelationshipRecords contentletRelationshipRecords : contentRelationships
                .getRelationshipsRecords()) {
            if (contentletRelationshipRecords.getRecords() != null) {
                final Relationship relationship = contentletRelationshipRecords
                        .getRelationship();
                final UserAPI userAPI = APILocator.getUserAPI();
                if (relationshipAPI.sameParentAndChild(relationship)) {
                    //from parent
                    addContentLimitedByPermissions(user, contentletRelationshipRecords,
                            getRelatedContentFromIndex(
                                    fromContentlet, relationship, true, userAPI.getSystemUser(),
                                    true));

                    //from child
                    addContentLimitedByPermissions(user, contentletRelationshipRecords,
                            getRelatedContentFromIndex(
                                    fromContentlet, relationship, false,
                                    userAPI.getSystemUser(),
                                    true));

                } else {
                    addContentLimitedByPermissions(user, contentletRelationshipRecords,
                            getRelatedContentFromIndex(
                                    fromContentlet, relationship,
                                    relationshipAPI.isParent(relationship, contentType),
                                    userAPI.getSystemUser(),
                                    true));
                }
            }
        }
    }

    /**
     * This method creates a list of all relationships that will be wiped out
     *
     * @param contentRelationships
     * @param contentType
     * @param relationshipAPI
     * @throws DotDataException
     */
    private void getWipeOutRelationships(final ContentletRelationships contentRelationships,
            final ContentType contentType, final RelationshipAPI relationshipAPI)
            throws DotDataException {
        //wipe out all relationships
        final List<Relationship> relationships = APILocator.getRelationshipAPI()
                .byContentType(contentType);
        relationships.forEach(relationship -> {
            //add empty list to each relationship
            if (relationshipAPI.sameParentAndChild(relationship)) {
                //add empty list as parent
                contentRelationships.getRelationshipsRecords()
                        .add(contentRelationships.new ContentletRelationshipRecords(
                                relationship, true));
                //add empty list as child
                contentRelationships.getRelationshipsRecords()
                        .add(contentRelationships.new ContentletRelationshipRecords(
                                relationship, false));
            } else {
                contentRelationships.getRelationshipsRecords()
                        .add(contentRelationships.new ContentletRelationshipRecords(
                                relationship,
                                relationshipAPI.isParent(relationship, contentType)));
            }
        });
    }

    /**
     * This method adds restricted content to the list of related content to be updated
     *
     * @param user
     * @param contentletRelationshipRecords
     * @param relatedContentlets
     */
    private void addContentLimitedByPermissions(User user,
            ContentletRelationshipRecords contentletRelationshipRecords,
            List<Contentlet> relatedContentlets) {

        //consider immutable collections
        contentletRelationshipRecords.setRecords(
                new ArrayList<>(contentletRelationshipRecords.getRecords()));
        contentletRelationshipRecords.getRecords()
                .addAll(relatedContentlets.stream()
                        .filter(contentlet -> {
                            try {
                                return !permissionAPI
                                        .doesUserHavePermission(contentlet,
                                                PermissionAPI.PERMISSION_READ, user,
                                                false);
                            } catch (DotDataException e) {
                                return false;
                            }
                        }).collect(Collectors.toList()));
    }

    /**
     * @param fromContentlet
     * @param toContentlet
     * @param categories
     * @param user
     * @param respect
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void moveContentCategories(final Contentlet fromContentlet,
            final Contentlet toContentlet,
            List<Category> categories, final User user, final boolean respect)
            throws DotDataException, DotSecurityException {
        final List<Category> categoriesUserCannotRemove = new ArrayList<>();

        // checkin was failing if the passed categories were an immutable list.  this makes a copy
        categories = (categories == null) ? new ArrayList<>() : new ArrayList<>(categories);


        //Find categories which the user can't use.  A user cannot remove a category they cannot use
        final List<Category> cats = categoryAPI.getParents(fromContentlet,
                APILocator.getUserAPI().getSystemUser(), true);
        for (final Category category : cats) {
            if (!categoryAPI.canUseCategory(category, user, false)) {
                if (!categories.contains(category)) {
                    categoriesUserCannotRemove.add(category);
                }
            }
        }

        categories = permissionAPI.filterCollection(categories, PermissionAPI.PERMISSION_USE,
                respect, user);
        categories.addAll(categoriesUserCannotRemove);

        // we have already validated permissions on the content object, no need to do it again
        categoryAPI.setParents(toContentlet, categories, APILocator.systemUser(), respect);
    }

    @WrapInTransaction
    @Override
    public void restoreVersion(Contentlet contentlet, User user, boolean respectFrontendRoles)
            throws DotSecurityException, DotContentletStateException, DotDataException {
        Logger.info(this.getClass(),
                "Restoring version for contentlet: " + contentlet.getIdentifier() + " by user: "
                        + (user != null ? user.getUserId() : "Unknown") + " inode: " + contentlet.getInode());
        if (contentlet.getInode().equals("")) {
            throw new DotContentletStateException(CAN_T_CHANGE_STATE_OF_CHECKED_OUT_CONTENT);
        }
        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user,
                respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot edit Contentlet: " + (contentlet != null ? contentlet.getIdentifier()
                    : "Unknown"));
        }
        if (contentlet == null) {
            throw new DotContentletStateException("The contentlet was null");
        }
        canLock(contentlet, user);
        Contentlet currentWorkingCon = findContentletByIdentifier(contentlet.getIdentifier(), false,
                contentlet.getLanguageId(), user, respectFrontendRoles);
        APILocator.getVersionableAPI().setWorking(contentlet);
        // Updating lucene index
        new ContentletLoader().invalidate(contentlet);
        // Updating lucene index
        indexAPI.addContentToIndex(currentWorkingCon);
        indexAPI.addContentToIndex(contentlet);
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllUserVersions(Identifier identifier, User user,
            boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotStateException {
        List<Contentlet> contentlets = contentFactory.findAllUserVersions(identifier);
        if (contentlets.isEmpty()) {
            return new ArrayList<>();
        }
        if (!permissionAPI.doesUserHavePermission(contentlets.get(0), PermissionAPI.PERMISSION_READ,
                user, respectFrontendRoles)) {
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet: " + (identifier != null ? identifier.getId()
                    : "Unknown")
                    + ".So Unable to View Versions");
        }
        return contentlets;
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllVersions(Identifier identifier, User user,
            boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotStateException {
        return findAllVersions(identifier, true, user, respectFrontendRoles);
    }

    /**
     *  Returns all versions of a contentlet given a set of identifiers
     * @param identifiers
     * @param user
     * @param respectFrontendRoles
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public List<Contentlet> findLiveOrWorkingVersions(Set<String> identifiers, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final List<Contentlet> allVersions = contentFactory.findLiveOrWorkingVersions(identifiers);
        if (allVersions.isEmpty()) {
            return List.of();
        }
        return allVersions.stream().filter(contentlet -> {
            try {
                return permissionAPI.doesUserHavePermission(contentlet,
                        PermissionAPI.PERMISSION_READ, user, respectFrontendRoles);
            } catch (DotDataException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllVersions(final Identifier identifier, final Variant variant,
            final User user, boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {
        final List<Contentlet> allVersions = contentFactory.findAllVersions(identifier, variant);

        if (allVersions.isEmpty()) {
            return new ArrayList<>();
        }

        if (!permissionAPI.doesUserHavePermission(allVersions.get(0), PermissionAPI.PERMISSION_READ,
                user, respectFrontendRoles)) {
            throw new DotSecurityException(
                    "User: " + (identifier != null ? identifier.getId() : "Unknown")
                            + " cannot read Contentlet So Unable to View Versions");
        }
        return allVersions;
    }

    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllVersions(Identifier identifier, boolean bringOldVersions,
            User user, boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException, DotStateException {
        List<Contentlet> contentlets = contentFactory.findAllVersions(identifier, bringOldVersions);
        if (contentlets.isEmpty()) {
            return new ArrayList<>();
        }
        if (!permissionAPI.doesUserHavePermission(contentlets.get(0), PermissionAPI.PERMISSION_READ,
                user, respectFrontendRoles)) {
            throw new DotSecurityException(
                    "User: " + (identifier != null ? identifier.getId() : "Unknown")
                            + " cannot read Contentlet So Unable to View Versions");
        }
        return contentlets;
    }


    @CloseDBIfOpened
    @Override
    public List<Contentlet> findAllVersions(final SearchCriteria searchCriteria) throws DotSecurityException, DotDataException {
        Logger.debug(this.getClass(), String.format("Retrieving all versions for Identifier [ %s ], " +
                        "Language ID [ %s ], bringOldVersions [ %b ], limit [ %d ], offset [ %d ]",
                searchCriteria.identifier(), searchCriteria.languageId(), searchCriteria.bringOldVersions(),
                searchCriteria.limit(), searchCriteria.offset()));
        final List<Contentlet> contentlets = this.contentFactory.findAllVersions(
                searchCriteria.identifier(), searchCriteria.languageId(), searchCriteria.bringOldVersions(),
                searchCriteria.limit(), searchCriteria.offset(), searchCriteria.orderDirection());
        if (contentlets.isEmpty()) {
            return List.of();
        }
        if (!permissionAPI.doesUserHavePermission(contentlets.get(0), PermissionAPI.PERMISSION_READ,
                searchCriteria.user(), searchCriteria.respectFrontendRoles())) {
            throw new DotSecurityException(String.format("User ID '%s' does not have READ permissions to access versions of Contentlet ID " +
                    "'%s'", null != searchCriteria.user() ? searchCriteria.user().getUserId() : "- null -", searchCriteria.identifier().getId()));
        }
        return contentlets;
    }

    @CloseDBIfOpened
    @Override
    public String getName(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotContentletStateException, DotDataException {

        Preconditions.checkNotNull(contentlet, "The contentlet is null");

        if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, user,
                respectFrontendRoles)) {
            Logger.error(this.getClass(), "User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet: " + contentlet.getIdentifier());
            throw new DotSecurityException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " cannot read Contentlet: " + contentlet.getIdentifier());
        }

        return contentlet.getTitle();
    }

    /**
     * This is the original method that copy the properties of one contentlet to another, this is
     * tge original firm and call the overloaded firm with checkIsUnique false
     */
    @Override
    public void copyProperties(Contentlet contentlet, Map<String, Object> properties)
            throws DotContentletStateException, DotSecurityException {
        boolean checkIsUnique = false;
        copyProperties(contentlet, properties, checkIsUnique);
    }

    /**
     * This is the new method of the copyProperties that copy one contentlet to another, the
     * checkIsUnique should be by default false, it check if a String value is unique and add a
     * (Copy) string to the end of the field value, this method is called several times, so is
     * important to call it with checkIsUnique false all the times
     *
     * @param contentlet    the new contentlet to the filled
     * @param properties    the map with the fields and values of the old contentlet
     * @param checkIsUnique the variable that establish if the unique string values should be
     *                      modified or not
     * @throws DotContentletStateException
     * @throws DotSecurityException
     */
    @CloseDBIfOpened
    public void copyProperties(final Contentlet contentlet, final Map<String, Object> properties,
            boolean checkIsUnique) throws DotContentletStateException, DotSecurityException {
        if (!InodeUtils.isSet(contentlet.getStructureInode())) {
            Logger.warn(this,
                    "Cannot copy properties to contentlet where structure inode < 1 : You must set the structure's inode");
            return;
        }
        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentlet.getStructureInode());
        List<String> fieldNames = new ArrayList<>();
        Map<String, Field> velFieldmap = new HashMap<>();

        for (Field field : fields) {
            if (!field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())
                    && !field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())) {
                fieldNames.add(field.getFieldName());
                velFieldmap.put(field.getVelocityVarName(), field);
            }
        }
        for (final Map.Entry<String, Object> property : properties.entrySet()) {

            if (fieldNames.contains(property.getKey())) {

                Logger.debug(this, "The map found a field not within the contentlet's structure");
            }

            if (property.getValue() == null) {
                continue;
            }

            if ((!property.getKey().equals("recurrence")) &&
                    !(
                            property.getValue() instanceof Set || property.getValue() instanceof Map
                                    || property.getValue() instanceof String
                                    || property.getValue() instanceof Boolean
                                    || property.getValue() instanceof File ||
                                    property.getValue() instanceof Float
                                    || property.getValue() instanceof Integer
                                    || property.getValue() instanceof Date
                                    || property.getValue() instanceof Long ||
                                    property.getValue() instanceof List
                                    || property.getValue() instanceof BigDecimal
                                    || property.getValue() instanceof Short
                                    || property.getValue() instanceof Double
                                    || property.getValue() instanceof ContentletRelationships
                    )
            ) {
                throw new DotContentletStateException(
                        "The map contains an invalid value: " + property.getValue().getClass());
            }
        }

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String conVariable = property.getKey();
            Object value = property.getValue();
            try {
                if (conVariable.equals(Contentlet.NULL_PROPERTIES)) {
                    continue;
                }
                if (conVariable.equals(Contentlet.INODE_KEY)) {
                    contentlet.setInode((String) value);
                } else if (conVariable.equals(Contentlet.LANGUAGEID_KEY)) {
                    contentlet.setLanguageId(ConversionUtils.toLong(value, 0L));
                } else if (conVariable.equals(Contentlet.STRUCTURE_INODE_KEY)) {
                    contentlet.setStructureInode((String) value);
                } else if (conVariable.equals(Contentlet.DISABLED_WYSIWYG_KEY)) {
                    contentlet.setDisabledWysiwyg((List<String>) value);
                } else if (conVariable.equals(Contentlet.MOD_DATE_KEY)) {
                    contentlet.setModDate((Date) value);
                } else if (conVariable.equals(Contentlet.MOD_USER_KEY)) {
                    contentlet.setModUser((String) value);
                } else if (conVariable.equals(Contentlet.OWNER_KEY)) {
                    contentlet.setOwner((String) value);
                } else if (conVariable.equals(Contentlet.IDENTIFIER_KEY)) {
                    contentlet.setIdentifier((String) value);
                } else if (conVariable.equals(Contentlet.SORT_ORDER_KEY)) {
                    contentlet.setSortOrder(ConversionUtils.toLong(value, 0L));
                } else if (conVariable.equals(Contentlet.HOST_KEY)) {
                    contentlet.setHost((String) value);
                } else if (conVariable.equals(Contentlet.FOLDER_KEY)) {
                    contentlet.setFolder((String) value);
                } else if (isSetPropertyVariable(conVariable)) {
                    contentlet.setProperty(conVariable, value);
                } else if (velFieldmap.get(conVariable) != null) {
                    Field field = velFieldmap.get(conVariable);
                    if (isFieldTypeString(field)) {
                        if (checkIsUnique && field.isUnique()) {
                            value = value +
                                    new StringBuilder(" (COPY_")
                                            .append(System.currentTimeMillis()).append(')')
                                            .toString();
                        }

                        if (value instanceof CharSequence) {
                            contentlet.setStringProperty(conVariable,
                                    value != null ? value.toString() : null);
                        } else {
                            contentlet.setProperty(conVariable, value);
                        }
                    } else if (isFieldTypeBoolean(field)) {
                        contentlet.setBoolProperty(conVariable,
                                ConversionUtils.toBooleanFromDb(value));
                    } else if (isFieldTypeFloat(field)) {
                        contentlet.setFloatProperty(conVariable, ConversionUtils.toFloat(value, 0));
                    } else if (isFieldTypeDate(field)) {
                        contentlet.setDateProperty(conVariable,
                                value != null ? (Date) value : null);
                    } else if (isFieldTypeLong(field)) {
                        contentlet.setLongProperty(conVariable, ConversionUtils.toLong(value, 0L));
                    } else if (isFieldTypeBinary(field)) {
                        final File myFile =
                                (value instanceof String) ? new File(String.valueOf(value))
                                        : (File) value;
                        contentlet.setBinary(conVariable, myFile);
                    } else {
                        contentlet.setProperty(conVariable, value);
                    }
                } else {
                    Logger.debug(this, "Value " + value + " in map cannot be set to contentlet");
                }
            } catch (ClassCastException cce) {
                Logger.error(this, "Value in map cannot be set to contentlet", cce);
            } catch (IOException ioe) {
                Logger.error(this, "IO Error in copying Binary File object ", ioe);
            }

        }

        //if we have a nullProperties variable, it needs to be the last one set
        if (UtilMethods.isSet(properties.get(Contentlet.NULL_PROPERTIES))) {
            contentlet.setProperty(Contentlet.NULL_PROPERTIES,
                    properties.get(Contentlet.NULL_PROPERTIES));
        }

        // workflow
        copyWorkflowProperties(contentlet, properties);
    }

    private boolean isSetPropertyVariable(String conVariable) {
        return conVariable.equals(Contentlet.NULL_PROPERTIES)
                || conVariable.equals(NEVER_EXPIRE)
                || conVariable.equals(Contentlet.CONTENT_TYPE_KEY)
                || conVariable.equals(Contentlet.BASE_TYPE_KEY)
                || conVariable.equals(Contentlet.LIVE_KEY)
                || conVariable.equals(Contentlet.WORKING_KEY)
                || conVariable.equals(Contentlet.LOCKED_KEY)
                || conVariable.equals(Contentlet.ARCHIVED_KEY)
                || conVariable.equals(ESMappingConstants.URL_MAP);
    }

    private void copyWorkflowProperties(Contentlet contentlet, Map<String, Object> properties) {
        contentlet.setActionId(
                (String) properties.get(Contentlet.WORKFLOW_ACTION_KEY));
        contentlet.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY,
                (String) properties.get(Contentlet.WORKFLOW_COMMENTS_KEY));
        contentlet.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY,
                (String) properties.get(Contentlet.WORKFLOW_ASSIGN_KEY));

        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE,
                (String) properties.get(Contentlet.WORKFLOW_PUBLISH_DATE));
        contentlet.setStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME,
                (String) properties.get(Contentlet.WORKFLOW_PUBLISH_TIME));
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE,
                (String) properties.get(Contentlet.WORKFLOW_EXPIRE_DATE));
        contentlet.setStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME,
                (String) properties.get(Contentlet.WORKFLOW_EXPIRE_TIME));
        contentlet.setStringProperty(Contentlet.WORKFLOW_NEVER_EXPIRE,
                (String) properties.get(Contentlet.WORKFLOW_NEVER_EXPIRE));
        contentlet.setStringProperty(Contentlet.FILTER_KEY,
                (String) properties.get(Contentlet.FILTER_KEY));
        contentlet.setStringProperty(Contentlet.WHERE_TO_SEND,
                (String) properties.get(Contentlet.WHERE_TO_SEND));
        contentlet.setStringProperty(Contentlet.I_WANT_TO,
                (String) properties.get(Contentlet.I_WANT_TO));
        contentlet.setStringProperty(Contentlet.PATH_TO_MOVE,
                (String) properties.get(Contentlet.PATH_TO_MOVE));
        contentlet.setStringProperty(Contentlet.WORKFLOW_TIMEZONE_ID,
                (String) properties.get(Contentlet.WORKFLOW_TIMEZONE_ID));
    }

    @Override
    public List<Contentlet> find(Category category, long languageId, boolean live, String orderBy,
            User user, boolean respectFrontendRoles)
            throws DotDataException, DotContentletStateException, DotSecurityException {
        List<Category> cats = new ArrayList<>();
        return find(cats, languageId, live, orderBy, user, respectFrontendRoles);
    }

    @Override
    public List<Contentlet> find(List<Category> categories, long languageId, boolean live,
            String orderBy, User user, boolean respectFrontendRoles)
            throws DotDataException, DotContentletStateException, DotSecurityException {
        if (categories == null || categories.size() < 1) {
            return new ArrayList<>();
        }
        StringBuffer buffy = new StringBuffer();
        buffy.append("+type:content +deleted:false");
        if (live) {
            buffy.append(" +live:true");
        } else {
            buffy.append(" +working:true");
        }
        if (languageId > 0) {
            buffy.append(" +languageId:" + languageId);
        }
        for (Category category : categories) {
            buffy.append(" +c" + category.getInode() + "c:on");
        }
        try {
            return search(buffy.toString(), 0, -1, orderBy, user, respectFrontendRoles);
        } catch (Exception pe) {
            Logger.error(this, "Unable to search for contentlets", pe);
            throw new DotContentletStateException(
                    "Unable to search for contentlets: " + pe.getMessage(), pe);
        }
    }


    @WrapInTransaction
    @Override
    public void setContentletProperty(Contentlet contentlet, Field field, Object value)
            throws DotContentletStateException {

        if (contentlet == null) {
            throw new DotContentletValidationException("The contentlet must not be null");
        }
        String contentTypeInode = contentlet.getContentTypeId();
        if (!InodeUtils.isSet(contentTypeInode)) {
            throw new DotContentletValidationException(
                    "The contentlet's Content Type Inode must be set");
        }

        if (value == null || !UtilMethods.isSet(value.toString())) {
            contentlet.setProperty(field.getVelocityVarName(), null);
            return;
        }

        final com.dotcms.contenttype.model.field.Field newField = LegacyFieldTransformer.from(
                field);

        FieldHandlerStrategyFactory.getInstance().get(newField).apply(contentlet, newField, value);
    }

    @WrapInTransaction
    @Override
    public void setContentletProperty(final Contentlet contentlet,
                                      final com.dotcms.contenttype.model.field.Field field, final Object value)
            throws DotContentletStateException {

        if (contentlet == null) {
            throw new DotContentletValidationException("The contentlet must not be null");
        }

        final String contentTypeInode = contentlet.getContentTypeId();
        if (!InodeUtils.isSet(contentTypeInode)) {
            throw new DotContentletValidationException("The contentlet's Content Type Inode must be set");
        }

        if (value == null || !UtilMethods.isSet(value.toString())) {
            contentlet.setProperty(field.variable(), null);
            return;
        }

        FieldHandlerStrategyFactory.getInstance().get(field).apply(contentlet, field, value);
    }

    /**
     * This method parse the date value and set it to the contentlet
     * The value could be a number (translated to an unix timestamp as Date)
     * A string (translated to a Date using the dateFormats)
     * Or an actual date (which is just set as it is)
     * Otherwise throws an exception ({@link DotContentletStateException}).
     * @param contentlet
     * @param field
     * @param value
     * @param dateFormats
     */
    public static void parseDate(final Contentlet contentlet,
                                  final Field field,
                                  final Object value,
                                  final String... dateFormats) {
        if (value instanceof Number) { // is a timestamp
            contentlet.setDateProperty(field.getVelocityVarName(),
                    DateUtil.convertDate(Number.class.cast(value).longValue()));
        } else if (value instanceof Date) {
            contentlet.setDateProperty(field.getVelocityVarName(), (Date) value);
        } else if (value instanceof String) {
            if (((String) value).trim().length() > 0) {
                try {
                    final String trimmedValue = ((String) value).trim();
                    contentlet.setDateProperty(field.getVelocityVarName(),
                            DateUtil.convertDate(trimmedValue, dateFormats));
                } catch (Exception e) {

                    throw new DotContentletStateException(
                            "Unable to convert string to date " + value + ", field: " +
                                    field.getVelocityVarName());
                }
            } else {

                contentlet.setDateProperty(field.getVelocityVarName(), null);
            }
        } else if (field.isRequired() && value == null) {

            throw new DotContentletStateException(
                    "Date fields must either be of type String or Date, field: " +
                            field.getVelocityVarName());
        }
    }


    /**
     * This method takes the incoming contentlet and determines if the new fileName we're receiving
     * is the same already stored on the identifier.fileAsset This So we enforce validation only of
     * new incoming files
     *
     * @param contentletIn
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    private boolean hasNewIncomingFile(final Contentlet contentletIn)
            throws DotContentletStateException {
        if (!contentletIn.isFileAsset()) {
            throw new IllegalArgumentException("Contentlet isn't a subtype of FileAsset");
        }
        try {
            final String identifierStr = contentletIn.getIdentifier();
            if (!UtilMethods.isSet(identifierStr)) {
                // if no identifier is set, we're dealing with a brand new contentlet then we enforce validation
                return true;
            }

            final Identifier identifier = APILocator.getIdentifierAPI().find(identifierStr);
            final File incomingFile = contentletIn.getBinary(FileAssetAPI.BINARY_FIELD);
            String incomingFileName =
                    null != incomingFile ? incomingFile.getName() : BLANK;
            if (UtilMethods.isSet(contentletIn.getStringProperty(FileAssetAPI.FILE_NAME_FIELD))) {
                incomingFileName = contentletIn.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
            }
            return !incomingFileName.equals(identifier.getAssetName());
        } catch (Exception e) {
            throw new DotContentletStateException(
                    "Exception trying to determine if there's a new incoming file:"
                            + e.getMessage(), e);
        }
    }

    @CloseDBIfOpened
    @Override
    public void validateContentlet(final Contentlet contentlet, final List<Category> cats)
            throws DotContentletValidationException {
        validateContentlet(contentlet, cats, false);
    }

    @CloseDBIfOpened
    public void validateContentlet(final Contentlet contentlet, final List<Category> cats, final boolean preview)
        throws DotContentletValidationException {
        if (null == contentlet) {
            throw new DotContentletValidationException("The contentlet must not be null.");
        }
        final String contentTypeId = contentlet.getContentTypeId();
        final long languageId = contentlet.getLanguageId();
        final String contentIdentifier = (UtilMethods.isSet(contentlet.getIdentifier())
                ? contentlet.getIdentifier()
                : "Unknown/New");
        if (!InodeUtils.isSet(contentTypeId)) {
            throw new DotContentletValidationException(
                    "Contentlet [" + contentIdentifier + "] is not associated to any Content Type.");
        }
        if (languageId > 0 && !this.languageAPI.hasLanguage(languageId)) {
            throw new DotContentletValidationException(
                    "Contentlet [" + contentIdentifier + "] is associated to an invalid language id: " + languageId);
        }

        final ContentType contentType = Sneaky.sneak(
                () -> APILocator.getContentTypeAPI(APILocator.systemUser()).find
                        (contentTypeId));
        if (BaseContentType.FILEASSET.getType() == contentType.baseType().getType()) {
            this.validateFileAsset(contentlet, contentIdentifier, contentType);
        }

        if(contentlet.isDotAsset()){
            this.validateDotAsset(contentlet);
        }

        if (BaseContentType.HTMLPAGE.getType() == contentType.baseType().getType()) {
            this.validateHtmlPage(contentlet, contentIdentifier, contentType);
        }
        if (contentlet.isHost()) {
            this.validateSite(contentlet);
        }
        boolean hasError = false;
        DotContentletValidationException.Builder<DotContentletValidationException> cveBuilder =
                DotContentletValidationException.builder(
                        String.format("Contentlet with ID '%s' ['%s'] has invalid/missing field(s).",
                                contentIdentifier, contentlet.getTitle()));
        final List<Field> fields = FieldsCache.getFieldsByStructureInode(contentTypeId);
        final Map<String, Object> contentletMap = contentlet.getMap();
        final Set<String> nullValueProperties = contentlet.getNullProperties();
        for (final Field field : fields) {
            final com.dotcms.contenttype.model.field.Field newField = LegacyFieldTransformer.from(
                    field);
            if (isIgnorableField(newField)) {
                continue;
            }
            final Object fieldValue = (nullValueProperties.contains(field.getVelocityVarName())
                    ? null : contentletMap.get(field.getVelocityVarName()));
            // Validate Field Type
            if (fieldValue != null) {
                if (isFieldTypeString(field) && !(newField instanceof JSONField)) {

                    if (fieldValue instanceof Map && FieldType.KEY_VALUE.toString()
                            .equals(field.getFieldType())) {
                        //That's fine we're handling now keyValues directly as maps
                        continue;
                    }

                    if (!(fieldValue instanceof String)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type String", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                } else if (isFieldTypeDate(field)) {
                    if (!(fieldValue instanceof Date)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type Date", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                } else if (isFieldTypeBoolean(field)) {
                    if (!(fieldValue instanceof Boolean)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type Boolean", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                } else if (isFieldTypeFloat(field)) {
                    if (!(fieldValue instanceof Float)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type Float", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                } else if (isFieldTypeLong(field)) {
                    if (!(fieldValue instanceof Long || fieldValue instanceof Integer)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type Long or Integer", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                    //  binary field validation
                } else if (isFieldTypeBinary(field)) {
                    if (!(fieldValue instanceof java.io.File)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type File", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                } else if (newField instanceof JSONField) {
                    if (!(fieldValue instanceof String) || !(JsonUtil.isValidJSON(
                            fieldValue.toString()))) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        Logger.warn(this, String.format("Value of field [%s] must be of type JSON", field.getVelocityVarName()));
                        hasError = true;
                        continue;
                    }
                    //  binary field validation
                } else if (isFieldTypeSystem(field) || isFieldTypeConstant(field)) {
                    // Do not validate system or constant field values
                } else {
                    Logger.warn(this, "Found an unknown field type : This should never happen!!!");
                    throw new DotContentletStateException("Field [" + field.getVelocityVarName() + "] has an unknown type");
                }
            }
            // validate required
            if (field.isRequired()) {

                if (fieldValue instanceof Map) {
                    final Map map = (Map) fieldValue;
                    if (field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString()) && map.isEmpty()) {
                        cveBuilder.addRequiredField(field, "");
                        hasError = true;
                        Logger.warn(this, String.format("String Field [%s] is required", field.getVelocityVarName()));
                        continue;
                    }
                } else if (field.getFieldType().equals(Field.FieldType.STORY_BLOCK_FIELD.toString())) {
                    // Story Block validation - handle both JSON and legacy WYSIWYG content during migration
                    if (fieldValue == null) {
                        cveBuilder.addRequiredField(field, "null");
                        hasError = true;
                        Logger.warn(this, String.format("Story Block Field [%s] is required", field.getVelocityVarName()));
                        continue;
                    } else if (!(fieldValue instanceof String)) {
                        cveBuilder.addBadTypeField(field, fieldValue != null ? fieldValue.toString() : "null");
                        hasError = true;
                        Logger.warn(this, String.format("Story Block Field [%s] must be a String, but got: %s",
                            field.getVelocityVarName(), fieldValue.getClass().getSimpleName()));
                        continue;
                    } else {
                        String stringValue = (String) fieldValue;
                        if (JsonUtil.isValidJSON(stringValue)) {
                            // Valid JSON - validate as Story Block
                            if (StoryBlockUtil.isEmptyStoryBlock(stringValue)) {
                                cveBuilder.addRequiredField(field, fieldValue.toString());
                                hasError = true;
                                Logger.warn(this, String.format("Story Block Field [%s] is required", field.getVelocityVarName()));
                                continue;
                            }
                        } else {
                            // Legacy WYSIWYG content (including malformed JSON) - use simple string validation for backward compatibility
                            if (stringValue.trim().isEmpty()) {
                                cveBuilder.addRequiredField(field, fieldValue.toString());
                                hasError = true;
                                Logger.warn(this, String.format("Story Block Field [%s] is required", field.getVelocityVarName()));
                                continue;
                            }
                            // Otherwise, let legacy WYSIWYG content (including malformed JSON) pass validation during migration
                        }
                    }
                } else if (fieldValue instanceof String) {
                    String s1 = (String) fieldValue;
                    if (!UtilMethods.isSet(s1.trim())
                            || (field.getFieldType().equals(Field.FieldType.KEY_VALUE.toString()))
                            && s1.equals("{}")) {
                        cveBuilder.addRequiredField(field, s1);
                        hasError = true;
                        Logger.warn(this, String.format("String Field [%s] is required", field.getVelocityVarName()));
                        continue;
                    }
                } else if (fieldValue instanceof java.io.File) {
                    String s1 = ((java.io.File) fieldValue).getPath();
                    if (!UtilMethods.isSet(s1.trim()) || s1.trim().contains("-removed-")) {
                        cveBuilder.addRequiredField(field, s1);
                        hasError = true;
                        Logger.warn(this, String.format("File Field [%s] is required", field.getVelocityVarName()));
                        continue;
                    }
                } else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    if (!UtilMethods.isSet(fieldValue)) {
                        if (null != contentType.expireDateVar()) {
                            if (field.getVelocityVarName().equals(contentType.expireDateVar())) {
                                if (NEVER_EXPIRE.equals(contentletMap.get(NEVER_EXPIRE))) {
                                    continue;
                                } else {
                                    cveBuilder.addRequiredField(field, fieldValue != null ? fieldValue.toString() : "null");
                                    hasError = true;
                                    Logger.warn(this, String.format("Date/Time in CT Field [%s] is required", field.getVelocityVarName()));
                                    continue;
                                }
                            } else {
                                cveBuilder.addRequiredField(field, fieldValue != null ? fieldValue.toString() : "null");
                                hasError = true;
                                Logger.warn(this, String.format("Date/Time expire Field [%s] is required", field.getVelocityVarName()));
                                continue;
                            }
                        } else {
                            cveBuilder.addRequiredField(field, fieldValue != null ? fieldValue.toString() : "null");
                            hasError = true;
                            Logger.warn(this, String.format("Date/Time Field [%s] is required", field.getVelocityVarName()));
                            continue;
                        }
                    }
                } else if (field.getFieldType().equals(FieldType.RELATIONSHIP.toString())) {
                    continue;
                } else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
                    if (UtilMethods.isNotSet(cats)) {
                        cveBuilder.addRequiredField(field, "empty");
                        hasError = true;
                        Logger.warn(this, String.format("Category Field [%s] is required (empty)", field.getVelocityVarName()));
                        continue;
                    }
                    try {
                        User systemUser = APILocator.getUserAPI().getSystemUser();
                        if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
                            CategoryAPI catAPI = APILocator.getCategoryAPI();
                            Category baseCat = catAPI.find(field.getValues(), systemUser, false);
                            List<Category> childrenCats = catAPI.getAllChildren(baseCat, systemUser,
                                    false);
                            boolean found = false;
                            for (Category cat : childrenCats) {
                                for (Category passedCat : cats) {
                                    if (passedCat.getInode().equalsIgnoreCase(cat.getInode())) {
                                        found = true;
                                    }
                                }
                            }
                            if (!found) {
                                cveBuilder.addRequiredField(field, "values not found");
                                hasError = true;
                                Logger.warn(this, String.format("Category Field [%s] is required (values not found)", field.getVelocityVarName()));
                                continue;
                            }
                        }
                    } catch (final DotDataException | DotSecurityException e) {
                        cveBuilder.addRequiredField(field, "validation error: " + ExceptionUtil.getErrorMessage(e));
                        hasError = true;
                        Logger.warn(this, String.format("Unable to validate category field [%s]: %s", field.getVelocityVarName(), ExceptionUtil.getErrorMessage(e)), e);
                        continue;
                    }
                } else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                    if (!UtilMethods.isSet(contentlet.getHost()) && !UtilMethods.isSet(contentlet.getFolder())) {
                        cveBuilder.addRequiredField(field, "host and folder both empty");
                        hasError = true;
                        Logger.warn(this, String.format("Site or Folder Field [%s] is required", field.getVelocityVarName()));
                        continue;
                    }
                } else if (!UtilMethods.isSet(fieldValue)) {
                    cveBuilder.addRequiredField(field, fieldValue != null ? fieldValue.toString() : "null");
                    hasError = true;
                    Logger.warn(this, String.format("Field [%s] is required", field.getVelocityVarName()));
                    continue;
                }
                if (field.getFieldType().equals(Field.FieldType.IMAGE.toString())
                        || field.getFieldType().equals(Field.FieldType.FILE.toString())) {
                    if (fieldValue instanceof Number) {
                        Number n = (Number) fieldValue;
                        if (n.longValue() == 0) {
                            cveBuilder.addRequiredField(field, n.toString());
                            hasError = true;
                            Logger.warn(this, String.format("Image Field (as number) [%s] is required", field.getVelocityVarName()));
                            continue;
                        }
                    } else if (fieldValue instanceof String) {
                        String s = (String) fieldValue;
                        if (s.trim().equals("0")) {
                            cveBuilder.addRequiredField(field, s);
                            hasError = true;
                            Logger.warn(this, String.format("Image Field (as String) [%s] is required", field.getVelocityVarName()));
                            continue;
                        }
                    }
                    //WYSIWYG patch for blank content
                } else if (field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
                    if (fieldValue instanceof String) {
                        String s = (String) fieldValue;
                        if (s.trim().equalsIgnoreCase("<br>")) {
                            cveBuilder.addRequiredField(field, s);
                            hasError = true;
                            Logger.warn(this, String.format("WYSIWYG Field [%s] is required", field.getVelocityVarName()));
                            continue;
                        }
                    }
                }
            }

            // validate unique
            if (field.isUnique()) {
                try {
                    if (!UtilMethods.isSet(contentlet.getHost())) {
                        populateHost(contentlet);
                    }

                    if (preview) {
                        uniqueFieldValidationStrategyResolver.get().get().validateInPreview(contentlet,
                                LegacyFieldTransformer.from(field));
                    } else {
                        this.handleUniqueFieldValidation(contentlet, field, fieldValue.toString());
                    }
                } catch (final UniqueFieldValueDuplicatedException e) {
                    final String errorMsg = String.format("%s with ID '%s' ['%s'] has invalid/missing field(s). %s",
                            !VariantAPI.DEFAULT_VARIANT.name().equalsIgnoreCase(contentlet.getVariantId())
                                    ? "Contentlet variant"
                                    : "Contentlet",
                            UtilMethods.isSet(contentlet.getIdentifier())
                                    ? contentlet.getIdentifier()
                                    : "Unknown/New",
                            contentlet.getTitle(), ExceptionUtil.getErrorMessage(e));
                    cveBuilder = DotContentletValidationException.builder(errorMsg);
                    cveBuilder.addUniqueField(field, fieldValue != null ? fieldValue.toString() : "null");
                    Logger.warn(this, getUniqueFieldErrorMessage(field, fieldValue,
                            UtilMethods.isSet(e.getContentlets()) ? e.getContentlets().get(0) : "Unknown/New Contentlet"));
                    throw cveBuilder.build();
                } catch (final DotDataException | DotSecurityException e) {
                    Logger.warn(this, String.format("Unable to validate unique field '%s' in Content Type '%s': %s",
                            field.getVelocityVarName(), contentlet.getContentType().name(), ExceptionUtil.getErrorMessage(e)), e);
                    cveBuilder.addUniqueField(field, fieldValue != null ? fieldValue.toString() : "null");
                    hasError = true;
                }
            }

            // validate text
            String dataType = (field.getFieldContentlet() != null) ? field.getFieldContentlet()
                    .replaceAll("[0-9]*", "") : "";
            if (UtilMethods.isSet(fieldValue) && dataType.equals("text")) {
                try {
                    final String stringValue = (String) fieldValue;
                } catch (final Exception e) {
                    Logger.warn(this, String.format("Unable to get string value from text field [%s] in contentlet", field.getVelocityVarName()), e);
                    continue;
                }
            }

            // validate regex
            String regext = field.getRegexCheck();
            if (UtilMethods.isSet(regext)) {
                if (UtilMethods.isSet(fieldValue)) {
                    if (fieldValue instanceof Number) {
                        Number n = (Number) fieldValue;
                        String s = n.toString();
                        boolean match = Pattern.matches(regext, s);
                        if (!match) {
                            hasError = true;
                            cveBuilder.addPatternField(field, s, regext);
                            Logger.warn(this, String.format("Field with number regex [%s] does not match. Regex: %s", field.getVelocityVarName(), regext));
                            continue;
                        }
                    } else if (fieldValue instanceof String && UtilMethods.isSet(
                            ((String) fieldValue).trim())) {
                        String s = ((String) fieldValue).trim();
                        boolean match = Pattern.matches(regext, s);
                        if (!match) {
                            hasError = true;
                            cveBuilder.addPatternField(field, s, regext);
                            Logger.warn(this, String.format("Field with string regex [%s] does not match. Regex: %s", field.getVelocityVarName(), regext));
                            continue;
                        }
                    }
                }
            }
            // validate binary
            if (isFieldTypeBinary(field)) {
                this.validateBinary((File) fieldValue, field.getVelocityVarName(), field, contentType);
            }
        }
        if (hasError) {throw cveBuilder.build();
        }
    }

    /**
     * Handles the unique field validation process based on the current status of the specific
     * Contentlet. This is based on the following rules:
     * <ul>
     *     <li>All Contentlets in their Default Variant are validated, as usual. No exceptions at
     *     all.</li>
     *     <li>For Contentlets in a specific Variant -- usually related to an Experiment -- their
     *     unique value is validated <b>ONLY when it is different from the one in the Default
     *     Variant</b>. If it is the same, no validation is done as such a value is expected to be
     *     the same as the one in the Default Variant.</li>
     * </ul>
     *
     * @param contentlet The {@link Contentlet} to whose unique value will be validated.
     * @param field      The {@link Field} holding the unique value
     * @param fieldValue The value of the unique field.
     *
     * @throws DotDataException                    An error occurred when interacting with the
     *                                             database.
     * @throws DotSecurityException                A permission error has occurred.
     * @throws UniqueFieldValueDuplicatedException The specified unique value is already in use by
     *                                             another Contentlet.
     */
    private void handleUniqueFieldValidation(final Contentlet contentlet, final Field field,
                                             final String fieldValue) throws DotDataException, DotSecurityException,
            UniqueFieldValueDuplicatedException {
        if (VariantAPI.DEFAULT_VARIANT.name().equalsIgnoreCase(contentlet.getVariantId())) {
            this.uniqueFieldValidationStrategyResolver.get().get().validate(contentlet,
                    LegacyFieldTransformer.from(field));
        } else {
            final Contentlet contentletInDefaultVariant = this.getContentletInDefaultVariant(contentlet);
            if (null != contentletInDefaultVariant && UtilMethods.isSet(contentletInDefaultVariant.getIdentifier())) {
                final String uniqueValue = contentletInDefaultVariant.getStringProperty(field.getVelocityVarName());
                if (UtilMethods.isSet(uniqueValue) && !uniqueValue.equals(fieldValue)) {
                    this.uniqueFieldValidationStrategyResolver.get().get().validate(contentlet, LegacyFieldTransformer.from(field));
                }
            }
        }
    }

    /**
     * Finds the Default Variant version of a Contentlet that belongs to a Variant in an Experiment.
     * If there's no live version of the Default Variant, the working version should be returned.
     *
     * @param contentlet The Contentlet Variant belonging to an Experiment.
     *
     * @return The Default Variant version of the Contentlet
     *
     * @throws DotDataException     A permission error has occurred.
     * @throws DotSecurityException A permission error has occurred.
     */
    private Contentlet getContentletInDefaultVariant(final Contentlet contentlet) throws DotDataException, DotSecurityException {
        Contentlet contentletInDefaultVariant =
                this.findContentletByIdentifier(contentlet.getIdentifier(), true, contentlet.getLanguageId(),
                        VariantAPI.DEFAULT_VARIANT.name(), APILocator.systemUser(), false);
        if (null == contentletInDefaultVariant || UtilMethods.isSet(contentletInDefaultVariant.getIdentifier())) {
            contentletInDefaultVariant =
                    this.findContentletByIdentifier(contentlet.getIdentifier(), false, contentlet.getLanguageId(),
                            VariantAPI.DEFAULT_VARIANT.name(), APILocator.systemUser(), false);
        }
        return contentletInDefaultVariant;
    }

    /**
     * Determines whether Contentlet Validation process can safely ignore the specified field.
     *
     * @param newField The {@link com.dotcms.contenttype.model.field.Field} that might be safely
     *                 ignored.
     *
     * @return If the filed can be ignored, returns {@code true}.
     */
    private boolean isIgnorableField(final com.dotcms.contenttype.model.field.Field newField) {
        return newField instanceof RowField || newField instanceof ColumnField || newField instanceof TabDividerField || newField instanceof LineDividerField;
    }

    private String getUniqueFieldErrorMessage(final Field field, final Object fieldValue,
            final String contentletID) {

        return String.format(
                "Value of Field [%s] must be unique. Contents having the same value (%s): %s",
                field.getVelocityVarName(), fieldValue, contentletID);
    }

    private boolean getUniquePerSiteConfig(final Field field) {
        return getUniquePerSiteConfig(LegacyFieldTransformer.from(field));
    }

    private boolean getUniquePerSiteConfig(final com.dotcms.contenttype.model.field.Field field) {
        return field.fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);
    }

    private void validateBinary(final File binary, final String fieldName, final Field legacyField,
            final ContentType contentType) {

        final Map<String, com.dotcms.contenttype.model.field.Field> fieldMap = contentType.fieldMap();

        if (fieldMap.containsKey(fieldName) && null != binary) {

            final List<FieldVariable> fieldVariables = fieldMap.get(fieldName).fieldVariables();

            if (UtilMethods.isSet(fieldVariables)) {

                for (final FieldVariable fieldVariable : fieldVariables) {

                    final String keyField = fieldVariable.key();

                    if (BinaryField.ALLOWED_FILE_TYPES.equalsIgnoreCase(keyField)) {

                        final String binaryMimeType = APILocator.getFileAssetAPI()
                                .getMimeType(binary);
                        final String allowedFileTypes = fieldVariable.value();
                        if (UtilMethods.isSet(allowedFileTypes) && UtilMethods.isSet(binaryMimeType)
                                &&
                                !FileAsset.UNKNOWN_MIME_TYPE.equals(binaryMimeType)) {

                            boolean allowed = false;
                            final MimeType fileMimeType = Sneaky.sneak(
                                    () -> new MimeType(binaryMimeType));
                            final String[] allowedFileTypeArray = StringUtil.split(
                                    allowedFileTypes);
                            for (final String allowFileType : allowedFileTypeArray) {

                                final MimeType mimeType = Sneaky.sneak(
                                        () -> new MimeType(allowFileType));
                                allowed |= mimeType.match(fileMimeType);
                            }

                            // if the extension of the file is not supported
                            if (!allowed) {

                                final DotContentletValidationException cve = DotContentletValidationException.builder(
                                        Sneaky.sneak(() -> LanguageUtil.get(
                                                "message.contentlet.binary.type.notallowed")))
                                        .addBadTypeField(legacyField, binaryMimeType)
                                        .build();
                                Logger.warn(this, String.format("Name of Binary field [%s] has an not allowed type: %s", fieldName, binaryMimeType));
                                throw cve;
                            }
                        }
                    }

                    if (BinaryField.MAX_FILE_LENGTH.equalsIgnoreCase(keyField)) {

                        final long fileLength = binary.length();
                        final String maxLengthString = fieldVariable.value();
                        final long maxLength = ConversionUtils.toLongFromByteCountHumanDisplaySize(
                                maxLengthString, -1l);

                        if (-1 != maxLength && // if the user sets a valid value
                                fileLength > maxLength) {

                            final DotContentletValidationException cve = DotContentletValidationException.builder(
                                            Sneaky.sneak(() -> LanguageUtil.get(
                                                    "message.contentlet.binary.file.exceeds.size",
                                                    binary.getName(),
                                                    UtilMethods.prettyByteify(maxLength))))
                                    .addBadTypeField(legacyField, String.valueOf(fileLength))
                                    .build();
                            Logger.warn(this, String.format("Name of Binary field [%s] has a length: %d but the max length is: %d",
                                    fieldName, fileLength, maxLength));
                            throw cve;
                        }
                    }
                }
            }
        }
    } // validateBinary.

    private void validateHtmlPage(Contentlet contentlet, String contentIdentifier,
            ContentType contentType) {
        if (contentlet.getHost() != null && contentlet.getHost().equals(Host.SYSTEM_HOST) && (
                !UtilMethods.isSet(contentlet.getFolder()) || contentlet.getFolder()
                        .equals(FolderAPI.SYSTEM_FOLDER))) {
            final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(
                    Sneaky.sneak(() -> LanguageUtil.get(
                            "message.contentlet.fileasset.invalid.hostfolder")))
                    .addBadTypeField(new LegacyFieldTransformer(contentType.fieldMap().get(FileAssetAPI
                            .HOST_FOLDER_FIELD)).asOldField(), "System Host")
                    .build();
            Logger.warn(this, String.format("HTML Page [%s] cannot be created directly under System Host", contentIdentifier));
            throw cve;
        }
        try {
            final Host site = APILocator.getHostAPI()
                    .find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), false);
            Folder folder = null;
            if (UtilMethods.isSet(contentlet.getFolder())) {
                folder = APILocator.getFolderAPI()
                        .find(contentlet.getFolder(), APILocator.getUserAPI().getSystemUser(),
                                false);
            } else {
                folder = APILocator.getFolderAPI().findSystemFolder();
            }

            //Get the URL from Identifier if it is not in Contentlet
            String url = contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD);

            if (!UtilMethods.isSet(url)) {

                if (InodeUtils.isSet(contentlet.getVersionId()) || InodeUtils.isSet(
                        contentlet.getInode())) {
                    Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
                    if (UtilMethods.isSet(identifier) && UtilMethods.isSet(
                            identifier.getAssetName())) {

                        url = identifier.getAssetName();
                    }
                }
            }

            if (UtilMethods.isSet(url)) {
                // Extract only the last part after the last /
                if (url.contains("/")) {
                    url = url.substring(url.lastIndexOf('/') + 1);
                }
                contentlet.setProperty(HTMLPageAssetAPI.URL_FIELD, url);
                Identifier folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
                String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? "/" + url
                        : folderId.getPath() + url;
                Identifier htmlpage = APILocator.getIdentifierAPI().find(site, path);
                if (htmlpage != null && InodeUtils.isSet(htmlpage.getId()) && !htmlpage.getId()
                        .equals(contentlet.getIdentifier())) {
                    final String errorMsg =
                            "Page URL [" + path + "] already exists with content ID [" + htmlpage
                                    .getId() + "]";
                    final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(errorMsg)
                            .addBadTypeField(
                                    new LegacyFieldTransformer(contentType.fieldMap().get(HTMLPageAssetAPI
                                            .URL_FIELD)).asOldField(), path)
                            .build();
                    Logger.warn(this, errorMsg);
                    throw cve;
                }
                UtilMethods.validateFileName(url);
            }

        } catch (final DotDataException | DotSecurityException | IllegalArgumentException e) {
            final String errorMsg =
                    "Contentlet [" + contentIdentifier + "] has an invalid URL: " + contentlet
                            .getStringProperty(HTMLPageAssetAPI.URL_FIELD);
            final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(errorMsg)
                    .addBadTypeField(
                            new LegacyFieldTransformer(contentType.fieldMap().get(HTMLPageAssetAPI
                                    .URL_FIELD)).asOldField(), contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD))
                    .build();
            Logger.warn(this, errorMsg);
            throw cve;
        }
    }

    private void validateFileAsset(final Contentlet contentlet, final String contentIdentifier,
            final ContentType contentType) {

        if (contentlet.getHost() != null && contentlet.getHost().equals(Host.SYSTEM_HOST) && (
                !UtilMethods.isSet(contentlet.getFolder()) || contentlet.getFolder()
                        .equals(FolderAPI.SYSTEM_FOLDER))) {
            final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(
                    Sneaky.sneak(() -> LanguageUtil.get(
                            "message.contentlet.fileasset.invalid.hostfolder")))
                    .addBadTypeField(new LegacyFieldTransformer(contentType.fieldMap().get(FileAssetAPI
                            .HOST_FOLDER_FIELD)).asOldField(), "System Host")
                    .build();
            Logger.warn(this, "File Asset [" + contentIdentifier
                    + "] cannot be created directly under System " +
                    "Host");
            throw cve;
        }

        //Enforce validation only if the file name isn't the same we've already got
        if (hasNewIncomingFile(contentlet)) {
            boolean fileNameExists = false;
            try {
                final Host site = APILocator.getHostAPI()
                        .find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), false);
                final Folder folder = UtilMethods.isSet(contentlet.getFolder()) ?
                        APILocator.getFolderAPI().find(contentlet.getFolder(),
                                APILocator.getUserAPI().getSystemUser(), false) :
                        APILocator.getFolderAPI().findSystemFolder();

                String fileName = contentlet.getBinary(FileAssetAPI.BINARY_FIELD) != null
                        ? contentlet.getBinary(FileAssetAPI.BINARY_FIELD).getName()
                        : BLANK;
                if (UtilMethods.isSet(contentlet.getStringProperty("fileName"))) {
                    fileName = contentlet.getStringProperty("fileName");
                }
                if (UtilMethods.isSet(fileName)) {
                    fileNameExists = APILocator.getFileAssetAPI()
                            .fileNameExists(site, folder, fileName, contentlet.getIdentifier());
                    if (!APILocator.getFolderAPI().matchFilter(folder, fileName)) {
                        final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(
                                Sneaky.sneak(() -> LanguageUtil.get(
                                        "message.file_asset.error.filename.filters")))
                                .addBadTypeField(
                                        new LegacyFieldTransformer(contentType.fieldMap().get(FileAssetAPI
                                                .HOST_FOLDER_FIELD)).asOldField(), fileName)
                                .build();
                        Logger.warn(this, "File Asset [" + contentIdentifier
                                + "] does not match specified folder" +
                                " file filters");
                        throw cve;
                    }
                }

            } catch (final Exception e) {
                if (e instanceof FileAssetValidationException) {
                    throw (FileAssetValidationException) e;
                }
                final String errorMsg =
                        "Unable to validate field: " + FileAssetAPI.BINARY_FIELD + " in " +
                                "contentlet [" + contentIdentifier + "]";
                Logger.warn(this, errorMsg);
                throw new FileAssetValidationException(errorMsg, e);
            }
            if (fileNameExists) {
                final FileAssetValidationException cve = DotContentletValidationException.fileAssetBuilder(
                        Sneaky.sneak(() -> LanguageUtil.get(
                                "message.contentlet.fileasset.filename.already.exists")))
                        .addBadTypeField(
                                new LegacyFieldTransformer(contentType.fieldMap().get(FileAssetAPI
                                        .HOST_FOLDER_FIELD)).asOldField(), "filename already exists")
                        .build();
                Logger.warn(this, "Name of File Asset [" + contentIdentifier + "] already exists");
                throw cve;
            }
        }
    }

    /**
     * Simple dotAssetValidation
     * @param contentlet
     * @throws DotAssetValidationException
     */
    private void validateDotAsset(final Contentlet contentlet) throws DotAssetValidationException {
        try {
            final FolderAPI folderAPI = APILocator.getFolderAPI();
            final User systemUser = APILocator.getUserAPI().getSystemUser();
            final Folder folder = UtilMethods.isSet(contentlet.getFolder()) ?
                    folderAPI.find(contentlet.getFolder(),
                            systemUser, false) :
                    folderAPI.findSystemFolder();
            final String fileName =
                    contentlet.getBinary(DotAssetContentType.ASSET_FIELD_VAR) != null
                            ? contentlet.getBinary(DotAssetContentType.ASSET_FIELD_VAR).getName()
                            : BLANK;
            if (UtilMethods.isNotSet(fileName)) {
                throw new DotAssetValidationException(
                        "Unable to get The Asset From the Given dotAsset Contentlet");
            }
            if (!folderAPI.matchFilter(folder, fileName)) {
                final String fileExtension = UtilMethods.getFileExtension(fileName);
                throw new DotAssetValidationException(String.format(
                        "The target folder %s does not accept the given file extension %s",
                        folder.getPath(), fileExtension));
            }
        } catch (Exception e) {
            if (e instanceof DotAssetValidationException) {
                throw (DotAssetValidationException) e;
            }
            Logger.warn(this, e.getMessage());
        }
    }

    static final Pattern dnsPattern = Pattern.compile(dnsRegEx);

    /**
     * Host validation method shared by HostResource and ContentletWebAPI
     *
     * @param contentlet
     */
    private void validateSite(Contentlet contentlet) {
        if (Config.getBooleanProperty("site.key.dns.validation", true)) {
            final String siteKey = (String) contentlet.get(Host.HOST_NAME_KEY);
            if (!UtilMethods.isSet(siteKey) || !dnsPattern.matcher(siteKey).find()) {
                throw new DotContentletValidationException(
                        String.format("Site key %s doesn't match a valid dns format.", siteKey));
            }
        }
    }



    @CloseDBIfOpened
    @Override
    public void validateContentletNoRels(final Contentlet contentlet,
            final List<Category> cats, final boolean preview) throws DotContentletValidationException {
        if (null != contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME)) {
            return;
        }
        final String contentTypeId = contentlet.getContentTypeId();
        if (!InodeUtils.isSet(contentTypeId)) {
            final String errorMsg =
                    "Contentlet [" + contentlet.getIdentifier() + "] has an empty Content Type ID";
            Logger.error(this, errorMsg);
            throw new DotContentletValidationException(errorMsg);
        }
        try {
            validateContentlet(contentlet, cats, preview);
            if (BaseContentType.PERSONA.getType() == contentlet.getContentType().baseType()
                    .getType()) {
                APILocator.getPersonaAPI().validatePersona(contentlet);
            }
            if (contentlet.isVanityUrl()) {
                APILocator.getVanityUrlAPI().validateVanityUrl(contentlet);
            }
        } catch (final DotContentletValidationException ve) {
            throw ve;
        }
    }

    @Override
    public void validateContentlet(Contentlet contentlet,Map<Relationship,
            List<Contentlet>> contentRelationships,List<Category> cats) throws DotContentletValidationException {

        Structure st = CacheLocator.getContentTypeCache()
                .getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationshipRecords> relationshipsRecords = new ArrayList<>();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for (Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = relEntry.getKey();
            boolean hasParent = APILocator.getRelationshipAPI().isParent(relationship, st);
            ContentletRelationshipRecords records = relationshipsData.new ContentletRelationshipRecords(
                    relationship, hasParent);
            records.setRecords(relEntry.getValue());
        }
        validateContentlet(contentlet, relationshipsData, cats);
    }

    @Override
    public void validateContentletNoRels(Contentlet contentlet,
                                  List<Category> cats) throws DotContentletValidationException {
        validateContentletNoRels(contentlet, cats, false);
    }

    @Override
    public void validateContentlet(final Contentlet contentlet,
                                   final ContentletRelationships contentRelationships,
                                   final List<Category> cats) throws DotContentletValidationException {
        validateContentlet(contentlet,contentRelationships, cats, false );
    }


    @CloseDBIfOpened
    @Override
    public void validateContentlet(final Contentlet contentlet,
            final ContentletRelationships contentRelationships,
            final List<Category> cats, final boolean preview) throws DotContentletValidationException {
        if (null != contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME)) {
            return;
        }
        final String contentTypeId = contentlet.getContentTypeId();
        if (!InodeUtils.isSet(contentTypeId)) {
            final String errorMsg =
                    "Contentlet [" + contentlet.getIdentifier() + "] has an empty Content Type ID";
            Logger.error(this, errorMsg);
            throw new DotContentletValidationException(errorMsg);
        }
        try {
            validateContentlet(contentlet, cats, preview);
            if (BaseContentType.PERSONA.getType() == contentlet.getContentType().baseType()
                    .getType()) {
                APILocator.getPersonaAPI().validatePersona(contentlet);
            }
            if (contentlet.isVanityUrl()) {
                APILocator.getVanityUrlAPI().validateVanityUrl(contentlet);
            }
        } catch (final DotContentletValidationException ve) {
            throw ve;
        }

        if (Try.of(() -> !contentlet.getBoolProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION))
                .getOrElse(true)) {
            validateRelationships(contentlet, contentRelationships);
        }
    }

    /**
     * Validates that the relationships where the specified {@code contentlet} is involved are
     * correct in terms of cardinality and Content Type match.
     *
     * @param contentlet           The {@link Contentlet} object whose relationships will be
     *                             validated, if any.
     * @param contentRelationships The {@link ContentletRelationships} containing the information of
     *                             the Contentlet's relationships and associated Contentlets.
     * @throws DotContentletValidationException An error occurred during the validation. This
     *                                          usually means a problem with the data being sent by
     *                                          the user.
     */
    private void validateRelationships(final Contentlet contentlet,
            final ContentletRelationships contentRelationships)
            throws DotContentletValidationException {

        boolean hasError = false;
        final String contentletId = (UtilMethods.isSet(contentlet.getIdentifier())
                ? contentlet.getIdentifier() :
                "Unknown/New");
        final ContentType contentType = contentlet.getContentType();
        final List<Relationship> relationships = APILocator.getRelationshipAPI()
                .byContentType(contentType);
        final Builder<DotContentletValidationException> builder = DotContentletValidationException.builder(
                "Contentlet [" + contentletId + "] has invalid/missing relationships");

        // Check if any required relationships are missing from contentRelationships
        for (final Relationship rel : relationships) {
            final List<Boolean> checkIfContentIsParent = new ArrayList<>();
            if (rel.isChildRequired() && Objects.equals(contentType.id(), rel.getParentStructureInode())) {
                checkIfContentIsParent.add(true);
            }
            if (rel.isParentRequired() && Objects.equals(contentType.id(), rel.getChildStructureInode())) {
                checkIfContentIsParent.add(false);
            }

            for (final boolean checkParent : checkIfContentIsParent) {
                final boolean foundInRelationships = contentRelationships != null
                        && contentRelationships.getRelationshipsRecords() != null
                        && contentRelationships.getRelationshipsRecords().stream()
                            .anyMatch(records ->
                                    checkParent == records.isHasParent() &&
                                    records.getRelationship().getInode().equals(rel.getInode()));

                boolean hasExistingRelatedContent = false;
                if (!foundInRelationships && UtilMethods.isSet(contentlet.getIdentifier())) {
                    // Check if there are existing related content records for this relationship
                    try {
                        final List<Contentlet> existingRelatedContent = getRelatedContent(contentlet, rel,
                                checkParent, APILocator.systemUser(), false);
                        hasExistingRelatedContent = existingRelatedContent != null && !existingRelatedContent.isEmpty();
                        if (hasExistingRelatedContent) {
                            Logger.debug(this, String.format("Required %s relationship [%s] not present in contentRelationships but found existing related content for contentlet [%s]",
                                    (checkParent ? "child" : "parent"), rel.getRelationTypeValue(), contentletId));
                        }
                    } catch (final DotDataException | DotSecurityException e) {
                        Logger.error(this, String.format("Could not check existing related content for relationship [%s] and contentlet [%s]",
                                rel.getRelationTypeValue(), contentletId), e);
                    }
                }

                if (!foundInRelationships && !hasExistingRelatedContent) {
                    hasError = true;
                    Logger.error(this, String.format("Required %s relationship [%s] is not present for contentlet [%s]",
                            (checkParent ? "child" : "parent"), rel.getRelationTypeValue(), contentletId));
                    builder.addRequiredRelationship(rel, new ArrayList<>());
                }
            }
        }

        if (null != contentRelationships) {
            final List<ContentletRelationshipRecords> records = contentRelationships.getRelationshipsRecords();

            for (final ContentletRelationshipRecords cr : records) {
                final Relationship relationship = cr.getRelationship();
                List<Contentlet> contentsInRelationship = cr.getRecords();
                if (null == contentsInRelationship) {
                    contentsInRelationship = new ArrayList<>();
                }

                if (relationship.getCardinality() == RELATIONSHIP_CARDINALITY.ONE_TO_ONE
                        .ordinal() && contentsInRelationship.size() > 0) {
                    hasError |= !isValidOneToOneRelationship(contentlet, builder, relationship,
                            contentsInRelationship);

                    if (hasError) {
                        continue;
                    }
                }
                //There is a case when the Relationship is between same structures
                //We need to validate that case
                boolean isRelationshipParent = true;
                if (APILocator.getRelationshipAPI().sameParentAndChild(relationship)) {
                    if (contentsInRelationship.stream().anyMatch(
                            con -> contentlet.getIdentifier().equals(con.getIdentifier()))) {
                        Logger.error(this, String.format("Cannot relate content [%s] to itself", contentletId));
                        hasError = true;
                        builder.addInvalidContentRelationship(relationship, contentsInRelationship);
                    }
                    if (!cr.isHasParent()) {
                        isRelationshipParent = false;
                    }
                }

                // if i am the parent
                if (APILocator.getRelationshipAPI().isParent(relationship, contentType)
                        && isRelationshipParent) {
                    if (relationship.isChildRequired() && contentsInRelationship.isEmpty()) {
                        hasError = true;
                        Logger.error(this, String.format("Error in Contentlet [%s]: Child relationship [%s] is required.",
                                contentletId, relationship.getRelationTypeValue()));
                        builder.addRequiredRelationship(relationship, contentsInRelationship);
                    }
                    for (final Contentlet contentInRelationship : contentsInRelationship) {
                        try {
                            // In order to get the related content we should use method getRelatedContent
                            // that has -boolean pullByParent- as parameter so we can pass -false-
                            // to get related content where we are parents.
                            final List<Contentlet> relatedContents = getRelatedContent(
                                    contentInRelationship, relationship, false,
                                    APILocator.getUserAPI()
                                            .getSystemUser(), true, 1, 0, null);
                            // If there's a 1-N relationship and the parent
                            // content is relating to a child that already has
                            // a parent...
                            if (relationship.getCardinality()
                                    == RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()
                                    && !relatedContents.isEmpty()
                                    && !relatedContents.get(0).getIdentifier()
                                    .equals(contentlet.getIdentifier())) {
                                final String errorMessage = String.format("ERROR! Parent content [%s] cannot be related to child content [%s] because it is already related to parent content [%s]",
                                        contentletId, contentInRelationship.getIdentifier(), relatedContents.get(0).getIdentifier());
                                Logger.error(this, errorMessage);
                                hasError = true;
                                builder.addBadCardinalityRelationship(relationship,
                                        contentsInRelationship);
                            }

                            if (!contentInRelationship.getContentTypeId()
                                    .equalsIgnoreCase(relationship.getChildStructureInode())) {
                                hasError = true;
                                Logger.error(this, String.format("Content Type of Contentlet [%s] does not match the Content Type in child relationship [%s]",
                                        contentInRelationship.getIdentifier(), relationship.getRelationTypeValue()));
                                builder.addInvalidContentRelationship(relationship,
                                        contentsInRelationship);
                            }
                        } catch (final DotDataException e) {
                            Logger.error(this, String.format("An error occurred when retrieving information from related Contentlet [%s]",
                                    contentInRelationship.getIdentifier()), e);
                        }
                    }
                } else if (APILocator.getRelationshipAPI().isChild(relationship, contentType)) {
                    if (relationship.isParentRequired() && contentsInRelationship.isEmpty()) {
                        hasError = true;
                        Logger.error(this, String.format("Error in Contentlet [%s]: Parent relationship [%s] is required.",
                                contentletId, relationship.getRelationTypeValue()));
                        builder.addRequiredRelationship(relationship, contentsInRelationship);
                    }
                    //grouping by id to avoid duplicate contents due to different languages
                    List<Contentlet> contentsInRelationshipSameLanguage = groupContentletsByLanguage(contentsInRelationship);

                    // If there's a 1-N relationship and the child content is
                    // trying to relate to one more parent...

                    if (relationship.getCardinality()
                            == RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()
                            && contentsInRelationshipSameLanguage.size() > 1) {
                        final String parentIds = contentsInRelationship.stream()
                                .map(Contentlet::getIdentifier)
                                .collect(java.util.stream.Collectors.joining(", "));
                        final String errorMessage = String.format("ERROR! Child content [%s] is already related to another parent content [%s]",
                                contentletId, parentIds);
                        Logger.error(this, errorMessage);
                        hasError = true;
                        builder.addBadCardinalityRelationship(relationship, contentsInRelationship);
                    }

                    for (final Contentlet contentInRelationship : contentsInRelationship) {
                        if (!UtilMethods.isSet(contentInRelationship.getContentTypeId())) {
                            hasError = true;
                            Logger.error(this, String.format("Contentlet with Identifier [%s] has an empty Content Type Inode", contentletId));
                            builder.addInvalidContentRelationship(relationship, contentsInRelationship);
                            continue;
                        }
                        if (null != relationship.getParentStructureInode()
                                && !contentInRelationship.getContentTypeId().equalsIgnoreCase(
                                relationship.getParentStructureInode())) {
                            hasError = true;
                            Logger.error(this, String.format("Content Type of Contentlet [%s] does not match the Content Type in relationship [%s]",
                                    contentletId, relationship.getRelationTypeValue()));
                            builder.addInvalidContentRelationship(relationship, contentsInRelationship);
                        }
                    }
                } else {
                    hasError = true;
                    Logger.error(this, String.format("Relationship [%s] is neither parent nor child of Contentlet [%s]",
                            relationship.getRelationTypeValue(), contentletId));
                    builder.addBadRelationship(relationship, contentsInRelationship);
                }
            }
        }
        if (hasError) {
            throw builder.build();
        }
    }

    /**
     * @param contentlet
     * @param builder
     * @param relationship
     * @param contentsInRelationship
     * @return
     */
    private boolean isValidOneToOneRelationship(final Contentlet contentlet,
            final DotContentletValidationException.Builder builder, final Relationship relationship,
            final List<Contentlet> contentsInRelationship) {

        List<Contentlet> contentsInRelationshipSameLanguage = groupContentletsByLanguage(contentsInRelationship);
        //Trying to relate more than one piece of content
        if (contentsInRelationshipSameLanguage.size() > 1) {
            Logger.error(this, String.format("Error in Contentlet [%s]: Relationship [%s] has been defined as One to One",
                    contentlet.getIdentifier(), relationship.getRelationTypeValue()));
            builder.addBadCardinalityRelationship(relationship, contentsInRelationship);
            return false;
        }

        //Trying to relate a piece of content that already exists to another relationship
        try {
            List<Contentlet> relatedContents = getRelatedContent(
                    contentsInRelationship.get(0), relationship, null,
                    APILocator.getUserAPI()
                            .getSystemUser(), true, 1, 0, null);
            if (relatedContents.size() > 0 && !relatedContents.get(0).getIdentifier()
                    .equals(contentlet.getIdentifier())) {
                Logger.error(this, String.format("Error in related Contentlet [%s]: Relationship [%s] has been defined as One to One",
                        relatedContents.get(0).getIdentifier(), relationship.getRelationTypeValue()));
                builder.addBadCardinalityRelationship(relationship, contentsInRelationship);
                return false;
            }
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when retrieving information from related Contentlet [%s]",
                    contentsInRelationship.get(0).getIdentifier()), e);
            builder.addInvalidContentRelationship(relationship, contentsInRelationship);
            return false;
        }
        return true;
    }

    private List<Contentlet> groupContentletsByLanguage(List<Contentlet> contentsInRelationship) {

    return new ArrayList<>(contentsInRelationship.stream()
            .collect(Collectors.toMap(
                    Contentlet::getIdentifier,
                    Function.identity(),
                    (existing, replacement) -> existing
            ))
            .values());
    }

    @Override
    public boolean isFieldTypeBoolean(Field field) {
        if (field.getFieldContentlet().startsWith("bool")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFieldTypeDate(Field field) {
        if (field.getFieldContentlet().startsWith("date")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFieldTypeFloat(Field field) {
        if (field.getFieldContentlet().startsWith("float")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFieldTypeLong(Field field) {
        if (field.getFieldContentlet().startsWith("integer")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFieldTypeString(Field field) {
        if (field.getFieldContentlet().startsWith("text")) {
            return true;
        }
        return false;
    }

    /**
     * @param field
     * @return
     */
    public boolean isFieldTypeBinary(Field field) {
        if (Field.FieldType.BINARY.toString().equals(field.getFieldType())) {
            return true;
        }
        return false;
    }

    /**
     * @param field
     * @return
     */
    public boolean isFieldTypeSystem(Field field) {
        if (field.getFieldContentlet().startsWith("system")) {
            return true;
        }
        return false;
    }

    /**
     * @param field
     * @return
     */
    public boolean isFieldTypeConstant(Field field) {
        if (field.getFieldContentlet().startsWith("constant")) {
            return true;
        }
        return false;
    }

    /**
     * @param content
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotContentletStateException
     */
    private Contentlet findWorkingContentlet(Contentlet content)
            throws DotDataException, DotContentletStateException {
        Contentlet workingCons = null;
        if (InodeUtils.isSet(content.getIdentifier())) {
            workingCons = contentFactory.findContentletByIdentifier(content.getIdentifier(), false,
                    content.getLanguageId(), content.getVariantId());

            if (!UtilMethods.isSet(workingCons)) {
                workingCons = contentFactory.findContentletByIdentifier(content.getIdentifier(), false,
                        content.getLanguageId(), VariantAPI.DEFAULT_VARIANT.name());
            }
        }

        return workingCons;
    }

    /**
     * @param contentlet
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Map<Relationship, List<Contentlet>> findContentRelationships(Contentlet contentlet)
            throws DotDataException {
        Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<>();
        if (contentlet == null) {
            return contentRelationships;
        }
        List<Relationship> rels = APILocator.getRelationshipAPI()
                .byContentType(contentlet.getStructure());
        for (Relationship r : rels) {
            if (!contentRelationships.containsKey(r)) {
                contentRelationships.put(r, new ArrayList<>());
            }
            List<Contentlet> cons = relationshipAPI.dbRelatedContent(r, contentlet);

            for (Contentlet c : cons) {
                List<Contentlet> l = contentRelationships.get(r);
                l.add(c);
            }
        }
        return contentRelationships;
    }

    @Override
    public int deleteOldContent(Date deleteFrom) throws DotDataException {
        int results = 0;
        if (deleteFrom == null) {
            throw new DotDataException("Date to delete from must not be null");
        }
        results = contentFactory.deleteOldContent(deleteFrom);
        return results;
    }

    @CloseDBIfOpened
    @Override
    public List<String> findFieldValues(String structureInode, Field field, User user,
            boolean respectFrontEndRoles) throws DotDataException {
        List<String> result = new ArrayList<>();

        List<Contentlet> contentlets;
        if (field.isIndexed()) {
            contentlets = new ArrayList<>();
            List<Contentlet> tempContentlets = new ArrayList<>();
            int limit = 500;

            StringBuilder query = new StringBuilder(
                    "+deleted:false +live:true +structureInode:" + structureInode);

            try {
                tempContentlets = search(query.toString(), limit, 0, field.getFieldContentlet(),
                        user, respectFrontEndRoles, PermissionAPI.PERMISSION_READ);
                if (0 < tempContentlets.size()) {
                    contentlets.addAll(tempContentlets);
                }

                for (int offset = limit; 0 < tempContentlets.size(); offset += limit) {
                    tempContentlets = search(query.toString(), limit, offset,
                            field.getFieldContentlet(), user, respectFrontEndRoles,
                            PermissionAPI.PERMISSION_READ);
                    if (0 < tempContentlets.size()) {
                        contentlets.addAll(tempContentlets);
                    }
                }
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        } else {
            contentlets = contentFactory.findContentletsWithFieldValue(structureInode, field);
            try {
                contentlets = permissionAPI.filterCollection(contentlets,
                        PermissionAPI.PERMISSION_READ, respectFrontEndRoles, user);
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        }

        String value;
        for (Contentlet contentlet : contentlets) {
            try {
                value = null;
                if (field.getFieldType().equals(Field.DataType.BOOL)) {
                    value = "" + contentlet.getBoolProperty(field.getVelocityVarName());
                } else if (field.getFieldType().equals(Field.DataType.DATE)) {
                    value = "" + contentlet.getDateProperty(field.getVelocityVarName());
                } else if (field.getFieldType().equals(Field.DataType.FLOAT)) {
                    value = "" + contentlet.getFloatProperty(field.getVelocityVarName());
                } else if (field.getFieldType().equals(Field.DataType.INTEGER)) {
                    value = "" + contentlet.getLongProperty(field.getVelocityVarName());
                } else if (field.getFieldType().equals(Field.DataType.LONG_TEXT)) {
                    value = contentlet.getStringProperty(field.getVelocityVarName());
                } else {
                    value = contentlet.getStringProperty(field.getVelocityVarName());
                }

                if (UtilMethods.isSet(value)) {
                    result.add(value);
                }
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }
        }

        return result;
    }

    /**
     * @param contentlets
     * @param field
     */
    private void deleteBinaryFiles(List<Contentlet> contentlets, Field field) {
        this.destroyMetadata(contentlets);
        contentlets.forEach(con -> {

            String contentletAssetPath = getContentletAssetPath(con, field);
            String contentletAssetCachePath = getContentletCacheAssetPath(con, field);

            // To delete binary files
            FileUtil.deltree(new File(contentletAssetPath));

            // To delete resized images
            FileUtil.deltree(new File(contentletAssetCachePath));
        });
    }

    /**
     * @param contentlets
     * @param field
     */
    private void moveBinaryFilesToTrash(List<Contentlet> contentlets, Field field) {
        contentlets.stream().forEach(con -> {

            String contentletAssetPath = getContentletAssetPath(con, field);
            String contentletAssetCachePath = getContentletCacheAssetPath(con, field);

            try {
                // To delete binary files
                new TrashUtils().moveFileToTrash(new File(contentletAssetPath),
                        "binaries/asset/" + con.getInode());

                // To delete resized images
                new TrashUtils().moveFileToTrash(new File(contentletAssetCachePath),
                        "binaries/cache/" + con.getInode());

            } catch (IOException e) {
                Logger.error(this, "Error moving files to trash: '" + contentletAssetPath + "', '"
                        + contentletAssetCachePath + "'");
            }
        });
    }

    /**
     * @param con
     * @param field
     * @return
     */
    private String getContentletAssetPath(Contentlet con, Field field) {
        String inode = con.getInode();

        String result = APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator
                + inode.charAt(0)
                + File.separator
                + inode.charAt(1)
                + File.separator
                + inode;

        if (field != null) {
            result += File.separator + field.getVelocityVarName();
        }

        return result;
    }

    /**
     * @param con
     * @param field
     * @return
     */
    private String getContentletCacheAssetPath(Contentlet con, Field field) {
        String inode = con.getInode();

        String result = APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator
                + "cache"
                + File.separator
                + inode.charAt(0)
                + File.separator
                + inode.charAt(1)
                + File.separator
                + inode;

        if (field != null) {
            result += File.separator + field.getVelocityVarName();
        }

        return result;
    }

    @CloseDBIfOpened
    @Override
    public File getBinaryFile(final String contentletInode, final String velocityVariableName,
            final User user) throws DotDataException, DotSecurityException {

        Logger.debug(this, "Retrieving binary file name : getBinaryFileName().");

        Contentlet con = contentFactory.find(contentletInode);

        if (!permissionAPI.doesUserHavePermission(con, PermissionAPI.PERMISSION_READ, user)) {
            if (null != user) {
                throw new DotSecurityException(String.format(
                        "Unauthorized Access user [%s , %s] trying to access contentlet identified by `%s`.",
                        user.getUserId(), user.getEmailAddress(), con.getIdentifier()));
            } else {
                throw new DotSecurityException(
                        "Unauthorized Access null user trying to access contentlet. ");
            }
        }

        File binaryFile = null;
        String binaryFilePath = null;
        /*** THIS LOGIC IS DUPED IN THE CONTENTLET POJO.  IF YOU CHANGE HERE, CHANGE THERE **/
        try {

            binaryFilePath = APILocator.getFileAssetAPI().getRealAssetsRootPath()
                    + File.separator
                    + contentletInode.charAt(0)
                    + File.separator
                    + contentletInode.charAt(1)
                    + File.separator
                    + contentletInode
                    + File.separator
                    + velocityVariableName;
            File binaryFilefolder = new File(binaryFilePath);

            if (binaryFilefolder.exists()) {
                java.io.File[] files = binaryFilefolder.listFiles(new BinaryFileFilter());

                if (files.length > 0) {
                    binaryFile = files[0];
                }
            }
        } catch (Exception e) {
            Logger.error(this,
                    "Error occurred while retrieving binary file name : getBinaryFileName(). ContentletInode : "
                            + contentletInode
                            + "  velocityVaribleName : " + velocityVariableName
                            + "  path : " + binaryFilePath);
            throw new DotDataException("File System error.", e);
        }
        return binaryFile;
    }

    @CloseDBIfOpened
    @Override
    public long contentletCount() throws DotDataException {
        return contentFactory.contentletCount();
    }

    @CloseDBIfOpened
    @Override
    public long contentletIdentifierCount() throws DotDataException {
        return contentFactory.contentletIdentifierCount();
    }

    @CloseDBIfOpened
    @Override
    public List<Map<String, Serializable>> DBSearch(Query query, User user,
            boolean respectFrontendRoles) throws ValidationException, DotDataException {

        List<com.dotcms.contenttype.model.field.Field> fields;
        try {
            fields = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(query.getFromClause()).fields();
        } catch (DotSecurityException e) {
            throw new DotStateException(e);
        }
        if (fields == null || fields.size() < 1) {
            throw new ValidationException("No Fields found for Content");
        }
        Map<String, String> dbColToObjectAttribute = new HashMap<>();
        for (com.dotcms.contenttype.model.field.Field field : fields) {
            dbColToObjectAttribute.put(field.dbColumn(), field.variable());
        }

        String title = "inode";
        for (com.dotcms.contenttype.model.field.Field f : fields) {
            if (f.listed()) {
                title = f.dbColumn();
                break;
            }
        }
        if (UtilMethods.isSet(query.getSelectAttributes())) {

            if (!query.getSelectAttributes().contains(title)) {
                query.getSelectAttributes().add(title);
            }
        } else {
            List<String> atts = new ArrayList<>();
            atts.add("*");
            atts.add(title);
            query.setSelectAttributes(atts);
        }

        return QueryUtil.DBSearch(query, dbColToObjectAttribute,
                "structure_inode = '" + fields.get(0).contentTypeId() + "'", user, true,
                respectFrontendRoles);
    }

    @Override
    public Contentlet copyContentlet(final Contentlet sourceContentlet, final Host host,
            final Folder folder, final User user, final String copySuffix,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(sourceContentlet, null, host, folder, user,
                copySuffix, respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    @SuppressWarnings("unchecked")
    public Contentlet copyContentlet(final Contentlet sourceContentlet,
                                     final ContentType contentType, final Host site, final Folder folder, final User user,
                                     final String copySuffix, final boolean respectFrontendRoles) throws DotDataException,
       DotSecurityException, DotContentletStateException {
        if (user == null || UtilMethods.isNotSet(user.getUserId())) {
            throw new DotSecurityException("A user must be specified.");
        }
        final Map<String, HTMLPageAssetAPI.TemplateContainersReMap> templateMappings = (Map<String, TemplateContainersReMap>) sourceContentlet.get(
                Contentlet.TEMPLATE_MAPPINGS);
        Contentlet copyContentlet = new Contentlet();
        String newIdentifier = BLANK;
        final List<Contentlet> versionsToMarkWorking = new ArrayList<>();
        final Map<String, Map<String, Contentlet>> contentletsToCopyRules = Maps.newHashMap();
        final Identifier sourceContentletIdentifier = APILocator.getIdentifierAPI()
                .find(sourceContentlet.getIdentifier());
        final List<Contentlet> versionsToCopy = new ArrayList<>(
                findAllVersions(sourceContentletIdentifier, false, user, respectFrontendRoles));

        // we need to save the versions from older-to-newer to make sure the last save
        // is the current version
        versionsToCopy.sort(Comparator.comparing(Contentlet::getModDate));

        for (final Contentlet contentlet : versionsToCopy) {
            final boolean isContentletLive = contentlet.isLive();
            final boolean isContentletWorking = contentlet.isWorking();
            final boolean isContentletDeleted = contentlet.isArchived();

            if (!permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ,
                    user, respectFrontendRoles)) {
                throw new DotSecurityException(
                        String.format("User '%s' does not have READ permissions on Contentlet " +
                                "Inode '%s'", user.getUserId(), contentlet.getInode()));
            }

            // gets the new information for the template from the request object
            Contentlet newContentlet = new Contentlet();
            newContentlet.setContentTypeId(contentlet.getContentTypeId());
            copyProperties(newContentlet, contentlet.getMap(), true);
            // Check if the copied contentlet must be associated to a specific Content Type instead
            newContentlet.setContentTypeId(null != contentType ? contentType.id() : contentlet.getContentTypeId());
            newContentlet.setInode(BLANK);
            newContentlet.setIdentifier(BLANK);
            newContentlet.setHost(site != null ? site.getIdentifier()
                    : (folder != null ? folder.getHostId() : contentlet.getHost()));
            newContentlet.setFolder(folder != null ? folder.getInode() : null);
            newContentlet.setLowIndexPriority(contentlet.isLowIndexPriority());
            final boolean copyingSite = (!newContentlet.getHost()
                    .equals(sourceContentlet.getHost()));
            if (contentlet.isFileAsset()) {
                final String newName = generateCopyName(
                        newContentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD), copySuffix);
                newContentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newName);

                final String newIdentifierName;
                if (copyingSite) {
                    //if we're copying a site.. re-using the asset-name is a safe strategy (it's supposed to be unique).
                    newIdentifierName = sourceContentletIdentifier.getAssetName();
                } else {
                    //otherwise we generate a suffixed asset-name out of the original identifier.
                    final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
                    newIdentifierName = generateCopyName(identifier.getAssetName(), copySuffix);
                }
                newContentlet.setStringProperty(Contentlet.CONTENTLET_ASSET_NAME_COPY,
                        newIdentifierName);
            }

            final String temporalFolder =
                    APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + File.separator
                            + UUIDGenerator.generateUuid();

            final List<Field> fields = FieldsCache.getFieldsByStructureInode(
                    contentlet.getStructureInode());
            File srcFile;
            File destFile = new File(temporalFolder);
            if (!destFile.exists()) {
                destFile.mkdirs();
            }
            String fieldValue;
            for (final Field tempField : fields) {
                if (tempField.getFieldType().equals(Field.FieldType.BINARY.toString())) {
                    fieldValue = "";
                    try {
                        srcFile = getBinaryFile(contentlet.getInode(),
                                tempField.getVelocityVarName(), user);
                        if (srcFile != null) {
                            if (BaseContentType.FILEASSET.equals(
                                    contentlet.getContentType().baseType())) {
                                fieldValue = generateCopyName(srcFile.getName(), copySuffix);
                            } else {
                                fieldValue = srcFile.getName();
                            }
                            destFile = new File(temporalFolder + File.separator + fieldValue);
                            if (!destFile.exists()) {
                                destFile.createNewFile();
                            }
                            FileUtils.copyFile(srcFile, destFile);
                            newContentlet.setBinary(tempField.getVelocityVarName(), destFile);
                        }
                    } catch (final Exception e) {
                        throw new DotDataException(
                                "Error copying binary file: '" + fieldValue + "': "
                                        + e.getMessage(), e);
                    }
                }

                //verify if the velocity var name is not Host to avoid override the host
                if (tempField.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())
                        && !Host.HOST_VELOCITY_VAR_NAME.equalsIgnoreCase(tempField.getVelocityVarName())) {
                    if (folder != null || site != null) {
                        newContentlet.setStringProperty(tempField.getVelocityVarName(),
                                folder != null ? folder.getInode() : site.getIdentifier());
                    } else {
                        if (contentlet.getFolder().equals(FolderAPI.SYSTEM_FOLDER)) {
                            newContentlet.setStringProperty(tempField.getVelocityVarName(),
                                    contentlet.getFolder());
                        } else {
                            newContentlet.setStringProperty(tempField.getVelocityVarName(),
                                    contentlet.getHost());
                        }
                    }
                }
            }

            final List<Category> parentCats = categoryAPI.getParents(contentlet, false, user,
                    respectFrontendRoles);
            final Map<Relationship, List<Contentlet>> rels = new HashMap<>();
            String destinationHostId;
            if (site != null && UtilMethods.isSet(site.getIdentifier())) {
                destinationHostId = site.getIdentifier();
            } else if (folder != null) {
                destinationHostId = folder.getHostId();
            } else {
                destinationHostId = contentlet.getHost();
            }
            if (sourceContentlet.getHost().equals(destinationHostId)) {
                final ContentletRelationships cr = getAllRelationships(contentlet);
                final List<ContentletRelationshipRecords> rr = cr.getRelationshipsRecords();
                for (final ContentletRelationshipRecords crr : rr) {
                    if (crr.getRelationship().getCardinality() != RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal()
                            && crr.getRelationship().getCardinality() != RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()) {
                        rels.put(crr.getRelationship(), crr.getRecords());
                    }
                }
            }

            //Set URL in the new contentlet because is needed to create Identifier in EscontentletAPI.
            if (contentlet.isHTMLPage()) {
                final String sourceTemplateId = contentlet.getStringProperty(
                        HTMLPageAssetAPI.TEMPLATE_FIELD);
                if (null != templateMappings && templateMappings.containsKey(sourceTemplateId)) {
                    final Template destinationTemplate = templateMappings.get(sourceTemplateId)
                            .getDestinationTemplate();
                    newContentlet.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD,
                            destinationTemplate.getIdentifier());
                } else {
                    final Template template = APILocator.getTemplateAPI()
                            .findWorkingTemplate(sourceTemplateId, user, false);
                    if (template.isAnonymous()) {//If the Template has a custom layout we need to create a copy of it, so when is modified it does not modify the other pages.
                        final Template copiedTemplate = APILocator.getTemplateAPI()
                                .copy(template, user);
                        newContentlet.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD,
                                copiedTemplate.getIdentifier());
                    }
                }

                final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
                if (UtilMethods.isSet(identifier) && UtilMethods.isSet(identifier.getAssetName())) {
                    final String newAssetName = generateCopyName(identifier.getAssetName(),
                            copySuffix);
                    newContentlet.setProperty(HTMLPageAssetAPI.URL_FIELD, newAssetName);
                } else {
                    Logger.warn(this, "Unable to get URL from Contentlet: " + contentlet);
                }
            }

            newContentlet.getMap().put(Contentlet.DISABLE_WORKFLOW, true);
            newContentlet.getMap().put(Contentlet.DONT_VALIDATE_ME, true);
            newContentlet.getMap().put(Contentlet.IS_COPY_CONTENTLET, true);
            newContentlet.getMap().put(Contentlet.IS_COPY, true);
            newContentlet.setIndexPolicy(sourceContentlet.getIndexPolicy());

            // Use the generated identifier if one version of this contentlet
            // has already been checked in
            if (UtilMethods.isSet(newIdentifier)) {
                newContentlet.setIdentifier(newIdentifier);
            }

            newContentlet = checkin(newContentlet, rels, parentCats,
                    permissionAPI.getPermissions(contentlet), user, respectFrontendRoles);
            if (!UtilMethods.isSet(newIdentifier)) {
                newIdentifier = newContentlet.getIdentifier();
            }

            permissionAPI.copyPermissions(contentlet, newContentlet);

            //Using a map to make sure one identifier per page.
            //Avoiding multi languages pages.
            if (!contentletsToCopyRules.containsKey(contentlet.getIdentifier())) {
                final Map<String, Contentlet> contentletMap = Maps.newHashMap();
                contentletMap.put("contentlet", contentlet);
                contentletMap.put("newContentlet", newContentlet);
                contentletsToCopyRules.put(contentlet.getIdentifier(), contentletMap);
            }

            if (isContentletLive) {
                APILocator.getVersionableAPI().setLive(newContentlet);
            }

            if (isContentletWorking) {
                versionsToMarkWorking.add(newContentlet);
            }
            if (isContentletDeleted) {
                APILocator.getVersionableAPI().setDeleted(newContentlet, true);
            }
            if (contentlet.getInode().equals(sourceContentlet.getInode())) {
                copyContentlet = newContentlet;
            }
        }

        for (final Map<String, Contentlet> stringContentletMap : contentletsToCopyRules.values()) {
            try {
                final Contentlet contentlet = stringContentletMap.get("contentlet");
                final Contentlet newContentlet = stringContentletMap.get("newContentlet");
                APILocator.getRulesAPI()
                        .copyRulesByParent(contentlet, newContentlet, user, respectFrontendRoles);
            } catch (final InvalidLicenseException ilexp) {
                Logger.warn(this, "License is required to copy rules under pages");
            }
        }

        for (final Contentlet con : versionsToMarkWorking) {
            APILocator.getVersionableAPI().setWorking(con);
        }

        if (sourceContentlet.isHTMLPage()) {
            final boolean copyingSite = (!copyContentlet.getHost()
                    .equals(sourceContentlet.getHost()));
            if (!copyingSite) { // We only want to execute this logic if we're not copying a whole site.
                // If the content is an HTML Page then copy page associated contentlets
                final List<MultiTree> pageContents = APILocator.getMultiTreeAPI()
                        .getMultiTrees(sourceContentlet.getIdentifier());
                for (final MultiTree multitree : pageContents) {
                    APILocator.getMultiTreeAPI()
                            .saveMultiTree(new MultiTree(copyContentlet.getIdentifier(),
                                    multitree.getContainer(),
                                    multitree.getContentlet(),
                                    multitree.getRelationType(),
                                    multitree.getTreeOrder(),
                                    multitree.getPersonalization(),
                                    multitree.getVariantId(),
                                    multitree.getStyleProperties()));
                }
            }
        }

        this.copyWorkflowState(sourceContentlet, copyContentlet, user);
        this.sendCopyEvent(copyContentlet);

        return copyContentlet;
    }

    /**
     * Copies the Workflow Task information from the "source" Contentlet to the "destination"
     * Contentlet. This is very important in order to keep the copied Contentlet and its respective
     * language versions in the same Workflow Step as the source Contentlet.
     *
     * @param sourceContentlet The {@link Contentlet} instance that will be copied.
     * @param copyContentlet   The resulting copy of the original Contentlet.
     * @param user             The {@link User} executing this action.
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    private void copyWorkflowState(final Contentlet sourceContentlet,
            final Contentlet copyContentlet, final User user) throws DotDataException {
        final long sourceLangId = sourceContentlet.getLanguageId();
        final List<Language> availableLanguages = APILocator.getLanguageAPI().getLanguages();
        try {
            for (final Language language : availableLanguages) {
                sourceContentlet.setLanguageId(language.getId());
                final WorkflowTask task = APILocator.getWorkflowAPI()
                        .findTaskByContentlet(sourceContentlet);
                if (null != task) {
                    final WorkflowTask newTask = new WorkflowTask();
                    Try.run(() -> BeanUtils.copyProperties(newTask, task));
                    newTask.setId(null);
                    newTask.setWebasset(copyContentlet.getIdentifier());
                    newTask.setLanguageId(language.getId());
                    APILocator.getWorkflowAPI().saveWorkflowTask(newTask);

                    final WorkflowComment newComment = new WorkflowComment();
                    newComment.setPostedBy(user.getUserId());
                    newComment.setComment(
                            "Content copied from content id: " + sourceContentlet.getIdentifier());
                    newComment.setCreationDate(new Date());
                    newComment.setWorkflowtaskId(newTask.getId());
                    APILocator.getWorkflowAPI().saveComment(newComment);
                }
            }
        } finally {
            sourceContentlet.setLanguageId(sourceLangId);
        }
    }

    private String generateCopyName(final String originalName, final String copySuffix) {
        final String copyName;
        if (StringUtils.isBlank(copySuffix)) {
            copyName = originalName;
        } else {
            final String assetNameNoExt = UtilMethods.getFileName(originalName);
            final String assetNameExt = UtilMethods.getFileExtension(originalName);
            if (UtilMethods.isSet(assetNameExt)) {
                copyName = assetNameNoExt + copySuffix.trim() + "." + assetNameExt;
            } else {
                copyName = originalName + copySuffix;
            }
        }
        Logger.debug(this, "new copy file will be named: " + copyName);
        return copyName;
    }

    private void sendCopyEvent(final Contentlet contentlet) throws DotHibernateException {

        HibernateUtil.addCommitListener(
                () -> this.contentletSystemEventUtil.pushCopyEvent(contentlet), 1000);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(Contentlet contentlet, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        HostAPI hostAPI = APILocator.getHostAPI();
        FolderAPI folderAPI = APILocator.getFolderAPI();

        final String hostIdentfier = contentlet.getHost();
        Identifier contIdentifier = APILocator.getIdentifierAPI().find(contentlet);

        Host host = hostAPI.find(hostIdentfier, user, respectFrontendRoles);
        if (host == null) {
            host = new Host();
        }
        Folder folder = folderAPI.findFolderByPath(contIdentifier.getParentPath(), host, user,
                false);

        return copyContentlet(contentlet, host, folder, user,
                generateCopySuffix(contentlet, host, folder), respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(Contentlet contentlet, Host host, User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, host, null, user,
                generateCopySuffix(contentlet, host, null), respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(final Contentlet contentlet, final ContentType contentType, final Host site, final User user,
                                     final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, contentType, site, null, user,
                generateCopySuffix(contentlet, site, null), respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(final Contentlet contentlet, final Folder folder, final User user,
                                     final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, null, folder, user,
                generateCopySuffix(contentlet, null, folder), respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(final Contentlet contentlet, final ContentType contentType, final Folder folder, final User user,
                                     final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        return copyContentlet(contentlet, contentType, null, folder, user,
                generateCopySuffix(contentlet, null, folder), respectFrontendRoles);
    }

    @WrapInTransaction
    @Override
    public Contentlet copyContentlet(Contentlet contentlet, Folder folder, User user,
            boolean appendCopyToFileName, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException, DotContentletStateException {
        // Suffix that we need to apply to append in content name
        final String copySuffix = appendCopyToFileName ? "_copy" : BLANK;

        return copyContentlet(contentlet, null, folder, user, copySuffix, respectFrontendRoles);
    }

    /**
     * This method generates the copy suffix when there is a contentlet that already has the same
     * URL.
     * <ul>
     * <li>if the new contentlet URL is NOT used then returns an empty suffix.</li>
     * <li>if the new contentlet URL without "_copy" is used then returns a
     * "_copy" suffix.</li>
     * <li>if the new contentlet URL with or without "_copy" is used then
     * returns a "_copy" plus timestamp in millis (example: "_copy_2122313123")
     * suffix.</li>
     * </ul>
     *
     * @param contentlet the contentlet that we are going to copy or move
     * @param host
     * @param folder     the destination folder
     * @return the generated contentlet asset name suffix
     * @throws DotDataException
     * @throws DotStateException
     * @throws DotSecurityException
     */
    private String generateCopySuffix(Contentlet contentlet, Host host, Folder folder)
            throws DotDataException, DotStateException, DotSecurityException {
        String assetNameSuffix = BLANK;

        final boolean diffHost = ((host != null && contentlet.getHost() != null)
                && !contentlet.getHost()
                .equalsIgnoreCase(host.getIdentifier()));

        // if different host we really don't need to
        if ((!contentlet.isFileAsset() && !contentlet.isHTMLPage()) && (
                diffHost || (folder != null && contentlet.getHost() != null) && !folder.getHostId()
                        .equalsIgnoreCase(contentlet.getHost()))) {
            return assetNameSuffix;
        }

        final String sourcef = (UtilMethods.isSet(contentlet.getFolder())) ? contentlet.getFolder()
                : APILocator.getFolderAPI().findSystemFolder().getInode();
        final String destf = (UtilMethods.isSet(folder)) ? folder.getInode()
                : APILocator.getFolderAPI().findSystemFolder().getInode();

        if (!diffHost && sourcef.equals(destf)) { // is copying in the same folder and samehost?
            assetNameSuffix = "_copy";

            // We need to verify if already exist a content with suffix "_copy",
            // if already exists we need to append a timestamp
            if (isContentletUrlAlreadyUsed(contentlet, host, folder, assetNameSuffix)) {
                assetNameSuffix += "_" + System.currentTimeMillis();
            }
        } else {
            if (isContentletUrlAlreadyUsed(contentlet, host, folder, assetNameSuffix)) {
                throw new DotDataException(
                        Sneaky.sneak(() -> LanguageUtil.get("error.copy.url.conflict")));
            }
        }

        return assetNameSuffix;
    }

    /**
     * This method verifies if the contentlet that we are going to copy or cut into a folder doesn't
     * have conflict with other contentlet that has the same URL.
     *
     * @param contentlet        the contentlet that we are going to copy or move
     * @param destinationHost   the destination host
     * @param destinationFolder the destination folder
     * @param assetNameSuffix   the suffix string that we will append in the asset name. Sometimes
     *                          you need to know if a asset name with a suffix is used or not
     * @return true if the contentlet URL is already used otherwise returns false
     * @throws DotStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private boolean isContentletUrlAlreadyUsed(Contentlet contentlet, Host destinationHost,
            Folder destinationFolder, final String assetNameSuffix)
            throws DotStateException, DotDataException, DotSecurityException {
        Identifier contentletId = APILocator.getIdentifierAPI().find(contentlet);

        // Create new asset name
        final String contentletIdAssetName = contentletId.getAssetName();
        String fileExtension = BLANK;
        if (contentlet.hasAssetNameExtension()) {
            final String ext = UtilMethods.getFileExtension(contentletIdAssetName);
            if (UtilMethods.isSet(ext)) {
                fileExtension = '.' + ext;
            }
        }
        final String futureAssetNameWithSuffix =
                UtilMethods.getFileName(contentletIdAssetName) + assetNameSuffix + fileExtension;

        // Check if page url already exist
        Identifier identifierWithSameUrl = null;
        if (UtilMethods.isSet(destinationFolder) && InodeUtils.isSet(
                destinationFolder.getInode())) { // Folders
            // Create new path
            Identifier folderId = APILocator.getIdentifierAPI()
                    .find(destinationFolder.getIdentifier());
            final String path = (destinationFolder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? "/"
                    : folderId.getPath()) + futureAssetNameWithSuffix;

            final Host host =
                    destinationFolder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ? destinationHost
                            : APILocator.getHostAPI()
                                    .find(destinationFolder.getHostId(),
                                            APILocator.getUserAPI().getSystemUser(), false);
            identifierWithSameUrl = APILocator.getIdentifierAPI().find(host, path);
        } else if (UtilMethods.isSet(destinationHost) && InodeUtils.isSet(
                destinationHost.getInode())) { // Hosts
            identifierWithSameUrl = APILocator.getIdentifierAPI()
                    .find(destinationHost, "/" + futureAssetNameWithSuffix);
        } else {
            // Host or folder object MUST be define
            Logger.error(this,
                    "Host or folder destination are invalid, please check that one of those values are set propertly.");
            throw new DotDataException(
                    "Host or folder destination are invalid, please check that one of those values are set propertly.");
        }

        return InodeUtils.isSet(identifierWithSameUrl.getId());
    }

    /**
     * @param structureInode
     * @return
     */
    private boolean hasAHostField(String structureInode) {
        List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
        for (Field f : fields) {
            if (f.getFieldType().equals("host or folder")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isInodeIndexed(String inode) {
        return isInodeIndexed(inode, false);
    }

    @Override
    public boolean isInodeIndexed(String inode, boolean live) {
        if (!UtilMethods.isSet(inode)) {
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }

        return isInodeIndexedWithQuery("+inode:" + inode + (live ? " +live:true" : ""));
    }

    @Override
    public boolean isInodeIndexed(String inode, boolean live, int secondsToWait) {
        if (!UtilMethods.isSet(inode)) {
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }

        return isInodeIndexedWithQuery("+inode:" + inode + (live ? " +live:true" : ""),
                secondsToWait);
    }

    @Override
    public boolean isInodeIndexed(String inode, boolean live, boolean working) {
        if (!UtilMethods.isSet(inode)) {
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }

        return isInodeIndexedWithQuery(
                "+inode:" + inode + String.format(" +live:%s +working:%s", live, working));
    }

    @Override
    public boolean isInodeIndexed(String inode, int secondsToWait) {

        if (!UtilMethods.isSet(inode)) {
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }

        return isInodeIndexedWithQuery("+inode:" + inode, secondsToWait);
    }

    @Override
    public boolean isInodeIndexedArchived(String inode) {

        return isInodeIndexedArchived(inode, -1);
    }

    @Override
    public boolean isInodeIndexedArchived(String inode, int secondsToWait) {

        if (!UtilMethods.isSet(inode)) {
            Logger.warn(this, "Requested Inode is not indexed because Inode is not set");
        }

        return isInodeIndexedWithQuery("+inode:" + inode + String.format(" +deleted:%s", true),
                secondsToWait);
    }

    private boolean isInodeIndexedWithQuery(String luceneQuery) {
        return isInodeIndexedWithQuery(luceneQuery + " " + UUIDGenerator.shorty(), -1);
    }

    private final List<Integer> fibonacciMapping = Arrays.asList(1, 2, 3, 5, 8, 13, 21, 34,
            55); // it is around 14 + 9 (by the timeout delay) seconds, enough to wait

    private boolean isInodeIndexedWithQuery(final String luceneQuery,
            final int milliSecondsToWait) {

        final long millistoWait = Config.getLongProperty("IS_NODE_INDEXED_INDEX_MILLIS_WAIT", 100);
        final int limit = -1 != milliSecondsToWait ? milliSecondsToWait : 300;
        boolean found = false;
        int counter = 0;
        int fibonacciIndex = 0;

        if (this.contentFactory.indexCount(luceneQuery) > 0) {

            if (ConfigUtils.isDevMode()) {
                Logger.info(this,
                        () -> "******>>>>>> Index count found in the fist hit for the query: "
                                + luceneQuery);
            }
            found = true;
        } else {

            while (counter < limit && fibonacciIndex < this.fibonacciMapping.size()) {

                counter += millistoWait * this.fibonacciMapping.get(
                        fibonacciIndex++); // 100, 200, 300, 500, 800, 1300, 2100, 3400 ...
                DateUtil.sleep(counter);

                try {

                    found = this.contentFactory.indexCount(luceneQuery) > 0;
                } catch (Exception e) {
                    Logger.error(this.getClass(), e.getMessage(), e);
                    return false;
                }

                if (found) {
                    break;
                }
            }
        }

        return found;
    }

    @CloseDBIfOpened
    @Override
    public void UpdateContentWithSystemHost(String hostIdentifier)
            throws DotDataException, DotSecurityException {
        contentFactory.UpdateContentWithSystemHost(hostIdentifier);
    }

    @CloseDBIfOpened
    @Override
    public void removeUserReferences(String userId) throws DotDataException, DotSecurityException {
        contentFactory.removeUserReferences(userId);
    }

    @WrapInTransaction
    @Override
    public void updateUserReferences(User userToReplace, String replacementUserId, User user)
            throws DotDataException, DotSecurityException {
        contentFactory.updateUserReferences(userToReplace, replacementUserId, user);
    }

    static final String CONTENTLET_URL_MAP_FOR_CONTENT_404 = "URL_MAP_FOR_CONTENT_404";

    @CloseDBIfOpened
    @Override
    public String getUrlMapForContentlet(Contentlet contentlet, User user,
            boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
        // no structure, no inode, no workee
        if (UtilMethods.isEmpty(()->contentlet.getInode())) {//NOSONAR
            return null;
        }

        String result = (String) contentlet.getMap().get(URL_MAP_FOR_CONTENT_KEY);
        if (result != null) {
            if (CONTENTLET_URL_MAP_FOR_CONTENT_404.equals(result)) {
                return null;
            }
            return result;
        }

        // if there is no detail page, return
        ContentType type = Try.of(contentlet::getContentType).getOrNull();
        if (UtilMethods.isEmpty(() -> type.detailPage())) { //NOSONAR
            return null;
        }

        Identifier id = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        Host host = APILocator.getHostAPI().find(id.getHostId(), user, respectFrontendRoles);

        // URL MAPPed
        if (UtilMethods.isSet(type.urlMapPattern())) {
            List<RegExMatch> matches = RegEX.find(type.urlMapPattern(), "({[^{}]+})");
            String urlMapField;
            String urlMapFieldValue;
            result = type.urlMapPattern();
            for (RegExMatch match : matches) {
                urlMapField = match.getMatch();
                urlMapFieldValue = contentlet.getStringProperty(
                        urlMapField.substring(1, (urlMapField.length() - 1)));

                //Clean up the contents before to replace the values
                urlMapFieldValue = sanitizeForURLMap(urlMapFieldValue);
                urlMapField = sanitizeForURLMap(urlMapField);

                if (UtilMethods.isSet(urlMapFieldValue)) {
                    result = result.replaceAll(urlMapField, urlMapFieldValue);
                } else {
                    // urlmap property not found on content
                    result = null;
                    break;
                }
            }
        }

        // or Detail page with id=uuid
        else {
            final Identifier detailPageId = APILocator.getIdentifierAPI().find(type.detailPage());
            if (UtilMethods.isEmpty(() -> detailPageId.getPath())) { //NOSONAR
                contentlet.setStringProperty(URL_MAP_FOR_CONTENT_KEY, CONTENTLET_URL_MAP_FOR_CONTENT_404);
                return null;
            }


            if (UtilMethods.isSet(() -> detailPageId.getId())) { //NOSONAR
                result = detailPageId.getPath() + "?id=" + contentlet.getInode();
            }
        }

        // we send the host of the content, not the detail page (is this right?)
        if ((host != null) && !host.isSystemHost() && !respectFrontendRoles && result != null) {
            if (result.indexOf("?") < 0) {
                result = result + "?host_id=" + host.getIdentifier();
            } else {
                result = result + "&host_id=" + host.getIdentifier();
            }
        }

        if (UtilMethods.isEmpty( result )) {
            contentlet.setStringProperty(URL_MAP_FOR_CONTENT_KEY, CONTENTLET_URL_MAP_FOR_CONTENT_404);
        }else {
            contentlet.setStringProperty(URL_MAP_FOR_CONTENT_KEY, result);
        }
        return result;
    }

    /**
     * Sanitizes a given value in order to be properly use when replacing url mapping values
     *
     * @param value
     * @return
     */
    private String sanitizeForURLMap(String value) {

        if (UtilMethods.isSet(value)) {
            value = value.replaceFirst("\\{", "\\\\{");
            value = value.replaceFirst("\\}", "\\\\}");
            value = value.replaceFirst("\\$", "\\\\\\$");
        }

        return value;
    }

    @WrapInTransaction
    @Override
    public Contentlet saveDraft(final Contentlet contentlet,
            final ContentletRelationships contentletRelationships,
            final List<Category> cats,
            final List<Permission> permissions,
            final User user, boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {

        if (!InodeUtils.isSet(contentlet.getInode())) {
            return checkin(contentlet, contentletRelationships, cats, permissions, user,
                    respectFrontendRoles);
        } else {
            canLock(contentlet, user);
            //get the latest and greatest from db
            final Contentlet working = contentFactory
                    .findContentletByIdentifier(contentlet.getIdentifier(), false,
                            contentlet.getLanguageId(), contentlet.getVariantId());

            /*
             * Only draft if there is a working version that is not live
             * and always create a new version if the user is different
             */
            if (null != working &&
                    !working.isLive() && working.getModUser().equals(contentlet.getModUser())) {

                // if we are the latest and greatest and are a draft
                if (working.getInode().equals(contentlet.getInode())) {

                    final Contentlet contentletReturned = checkin(contentlet, contentletRelationships, cats,
                            user, respectFrontendRoles, false, false);

                    this.handlePermissions(permissions, user, respectFrontendRoles, contentletReturned);
                    return contentletReturned;
                } else {
                    final String workingInode = working.getInode();
                    copyProperties(working, contentlet.getMap());
                    working.setInode(workingInode);
                    working.setModUser(user.getUserId());
                    final Contentlet contentletReturned =  checkin(contentlet, contentletRelationships, cats,
                            user, respectFrontendRoles, false, false);

                    this.handlePermissions(permissions, user, respectFrontendRoles, contentletReturned);
                    return contentletReturned;
                }
            }

            contentlet.setInode(null);
            return checkin(contentlet, contentletRelationships,
                    cats,
                    permissions, user, respectFrontendRoles);
        }
    }

    @WrapInTransaction
    @Override
    public Contentlet saveDraft(Contentlet contentlet,
            Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats,
            List<Permission> permissions, User user, boolean respectFrontendRoles)
            throws IllegalArgumentException, DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {
        if (!InodeUtils.isSet(contentlet.getInode())) {
            return checkin(contentlet, contentRelationships, cats, permissions, user, false);
        } else {
            canLock(contentlet, user);
            //get the latest and greatest from db
            Contentlet working = contentFactory
                    .findContentletByIdentifier(contentlet.getIdentifier(), false,
                            contentlet.getLanguageId());

            /*
             * Only draft if there is a working version that is not live
             * and always create a new version if the user is different
             */
            if (null != working &&
                    !working.isLive() && working.getModUser().equals(contentlet.getModUser())) {

                // if we are the latest and greatest and are a draft
                if (working.getInode().equals(contentlet.getInode())) {

                    return checkinWithoutVersioning(contentlet, contentRelationships,
                            cats,
                            permissions, user, false);

                } else {
                    String workingInode = working.getInode();
                    copyProperties(working, contentlet.getMap());
                    working.setInode(workingInode);
                    working.setModUser(user.getUserId());
                    return checkinWithoutVersioning(working, contentRelationships,
                            cats,
                            permissions, user, false);
                }
            }

            contentlet.setInode(null);
            return checkin(contentlet, contentRelationships,
                    cats,
                    permissions, user, false);
        }
    }

    @WrapInTransaction
    @Override
    public void removeFolderReferences(Folder folder)
            throws DotDataException, DotSecurityException {
        contentFactory.removeFolderReferences(folder);
    }

    @Override
    public boolean canLock(final Contentlet contentlet, final User user)
            throws DotLockException {
        return canLock(contentlet, user, false);
    }

    @CloseDBIfOpened
    @Override
    public boolean canLock(final Contentlet contentlet, final User user,
            final boolean respectFrontendRoles) throws DotLockException {

        if (contentlet == null || !UtilMethods.isSet(contentlet.getIdentifier())) {

            return true;
        }

        if (user == null) {

            throw new DotLockException("null User cannot lock content");
        }

        try {

            if (APILocator.getRoleAPI()
                    .doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {

                return true;
            } else if (!APILocator.getPermissionAPI().doesUserHavePermission(
                        contentlet, PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)
                    && !APILocator.getPermissionAPI().doesUserHavePermission(
                        contentlet.getContentType(), PermissionAPI.PERMISSION_EDIT, user, respectFrontendRoles)) {

                throw new DotLockException("User: " + (user != null ? user.getUserId() : "Unknown")
                        + " does not have Edit Permissions to lock content: " + (contentlet != null
                        ? contentlet.getIdentifier() : "Unknown"));
            }
        } catch (DotDataException dde) {

            throw new DotLockException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have Edit Permissions to lock content: " + (contentlet != null
                    ? contentlet.getIdentifier() : "Unknown"));
        }

        Optional<String> lockedBy = Optional.empty();
        try {
            lockedBy = APILocator.getVersionableAPI().getLockedBy(contentlet);
        } catch (Exception e) {

        }

        if (lockedBy.isPresent() && !user.getUserId().equals(lockedBy.get())) {
            throw new DotLockException(CANT_GET_LOCK_ON_CONTENT);
        }

        return true;
    }

    @CloseDBIfOpened
    @Override
    public Map<Relationship, List<Contentlet>> findContentRelationships(
            Contentlet contentlet, User user) throws DotDataException,
            DotSecurityException {

        if (!APILocator.getPermissionAPI()
                .doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user)) {
            throw new DotLockException("User: " + (user != null ? user.getUserId() : "Unknown")
                    + " does not have Edit Permissions on the content: " + (contentlet != null
                    ? contentlet.getIdentifier() : "Unknown"));
        }

        return findContentRelationships(contentlet);
    }

    @Override
    public long indexCount(String luceneQuery, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        boolean isAdmin = false;
        List<Role> roles = new ArrayList<>();
        if (user == null && !respectFrontendRoles) {
            throw new DotSecurityException(
                    "You must specify a user if you are not respecting frontend roles");
        }
        if (user != null) {
            if (!APILocator.getRoleAPI()
                    .doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            } else {
                isAdmin = true;
            }
        }
        StringBuffer buffy = new StringBuffer(luceneQuery);

        // Permissions in the query
        if (!isAdmin) {
            addPermissionsToQuery(buffy, user, roles, respectFrontendRoles);
        }

        return contentFactory.indexCount(buffy.toString());
    }

    @CloseDBIfOpened
    @Override
    public List<Map<String, String>> getMostViewedContent(String structureVariableName,
            String startDateStr, String endDateStr, User user) {

        String[] dateFormats = new String[]{"yyyy-MM-dd HH:mm", "d-MMM-yy", "MMM-yy", "MMMM-yy",
                "d-MMM", "dd-MMM-yyyy", "MM/dd/yyyy hh:mm aa", "MM/dd/yy HH:mm",
                "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d", "EEEE, MMMM dd, yyyy",
                "MM/dd/yyyy",
                "hh:mm:ss aa", "HH:mm:ss", "yyyy-MM-dd"};

        List<Map<String, String>> result = new ArrayList<>();

        String structureInode = CacheLocator.getContentTypeCache()
                .getStructureByVelocityVarName(structureVariableName).getInode();
        if (!UtilMethods.isSet(structureInode)) {
            return result;
        }

        GregorianCalendar gCal = new GregorianCalendar();
        Date endDate = gCal.getTime();
        gCal.add(2, -3);
        Date startDate = gCal.getTime();// Default interval

        if (!UtilMethods.isSet(startDateStr) && !UtilMethods.isSet(endDateStr)) {
            GregorianCalendar gc = new GregorianCalendar();
            endDate = gc.getTime();
            gc.add(2, -3);
            startDate = gc.getTime();
        } else if (!UtilMethods.isSet(startDateStr)) {
            try {
                endDate = DateUtil.convertDate(endDateStr, dateFormats);
                Calendar gc = new GregorianCalendar();
                gc.setTime(endDate);
                gc.add(2, -3);
                startDate = gc.getTime();
            } catch (java.text.ParseException e) {
                GregorianCalendar gc = new GregorianCalendar();
                endDate = gc.getTime();
                gc.add(2, -3);
                startDate = gc.getTime();
            }
        } else if (!UtilMethods.isSet(endDateStr)) {
            try {
                startDate = DateUtil.convertDate(endDateStr, dateFormats);
                Calendar gc = new GregorianCalendar();
                gc.setTime(startDate);
                gc.add(2, +3);
                endDate = gc.getTime();
            } catch (java.text.ParseException e) {
                GregorianCalendar gc = new GregorianCalendar();
                endDate = gc.getTime();
                gc.add(2, -3);
                startDate = gc.getTime();
            }
        } else {
            try {
                startDate = DateUtil.convertDate(startDateStr, dateFormats);
                endDate = DateUtil.convertDate(endDateStr, dateFormats);
            } catch (java.text.ParseException e) {
            }
        }

        try {
            result = contentFactory.getMostViewedContent(structureInode, startDate, endDate, user);
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * Utility method used to log the different operations performed on a list of {@link Contentlet}
     * objects. The information of the operation will be logged in the Activity Logger file.
     *
     * @param contentlets - List of {@link Contentlet} objects whose information will be logged.
     * @param description - A small description of the operation being performed. E.g., "Deleting
     *                    Content", "Error Publishing Content", etc.
     * @param user        - The currently logged in user.
     */
    private void logContentletActivity(List<Contentlet> contentlets,
            String description, User user) {
        for (Contentlet content : contentlets) {
            logContentletActivity(content, description, user);
        }
    }

    /**
     * Utility method used to log the different operations performed on {@link Contentlet} objects.
     * The information of the operation will be logged in the Activity Logger file.
     *
     * @param contentlet  - The {@link Contentlet} whose information will be logged.
     * @param description - A small description of the operation being performed. E.g., "Deleting
     *                    Content", "Error Publishing Content", etc.
     * @param user        - The currently logged in user.
     */
    private void logContentletActivity(final Contentlet contentlet,
            final String description, final User user) {

        String contentPushPublishDate = contentlet
                .getStringProperty(Contentlet.WORKFLOW_PUBLISH_DATE);
        String contentPushPublishTime = contentlet
                .getStringProperty(Contentlet.WORKFLOW_PUBLISH_TIME);
        String contentPushExpireDate = contentlet
                .getStringProperty(Contentlet.WORKFLOW_EXPIRE_DATE);
        String contentPushExpireTime = contentlet
                .getStringProperty(Contentlet.WORKFLOW_EXPIRE_TIME);
        contentPushPublishDate = UtilMethods.isSet(contentPushPublishDate) ? contentPushPublishDate
                : "N/A";
        contentPushPublishTime = UtilMethods.isSet(contentPushPublishTime) ? contentPushPublishTime
                : "N/A";
        contentPushExpireDate = UtilMethods.isSet(contentPushExpireDate) ? contentPushExpireDate
                : "N/A";
        contentPushExpireTime = UtilMethods.isSet(contentPushExpireTime) ? contentPushExpireTime
                : "N/A";
        ActivityLogger.logInfo(getClass(), description,
                "StartDate: "
                        + contentPushPublishDate
                        + "; "
                        + "EndDate: "
                        + contentPushExpireDate
                        + "; User:"
                        + (user != null ? user.getUserId() : "Unknown")
                        + "; ContentIdentifier: "
                        + (contentlet != null ? contentlet.getIdentifier()
                        : "Unknown"), contentlet.getHost());
    }

    /**
     * Utility method that removes from a given contentlet the URL field as it should never be saved
     * on the DB. The URL of a HTMLPage should always been retrieved from the Identifier.
     *
     * @param contentlet
     */
    private void removeURLFromContentlet(Contentlet contentlet) {

        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
            contentlet.setProperty(HTMLPageAssetAPI.URL_FIELD, null);
        }
    }

    /**
     * Utility method that adds to a given contentlet a given URL, remember that we just use the URL
     * field to move aroung the value but we never save it into the DB for HTMLPages, the URL of a
     * HTMLPage should always been retrieved from the Identifier.
     *
     * @param contentlet
     * @param url
     */
    private void addURLToContentlet(Contentlet contentlet, String url) {

        if (contentlet.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE) {
            contentlet.setProperty(HTMLPageAssetAPI.URL_FIELD, url);
        }
    }

    @Override
    public Contentlet checkin(Contentlet contentlet, ContentletRelationships contentRelationships,
            List<Category> cats, List<Permission> selectedPermissions, User user,
            boolean respectFrontendRoles, boolean generateSystemEvent)
            throws IllegalArgumentException,
            DotDataException, DotSecurityException, DotContentletStateException, DotContentletValidationException {

        final Contentlet contentletReturned = checkin(contentlet, contentRelationships, cats, user,
                respectFrontendRoles, true, generateSystemEvent);

        handlePermissions(selectedPermissions, user, respectFrontendRoles, contentletReturned);

        return contentletReturned;
    }

    private void handlePermissions(final List<Permission> selectedPermissions,
                                   final User user,
                                   final boolean respectFrontendRoles,
                                   final Contentlet contentlet) {

        if (InodeUtils.isSet(contentlet.getInode()) && UtilMethods.isSet(
                selectedPermissions)) {

            final Runnable savePermissions = () -> {

                try {

                    Logger.debug(this,
                            () -> "Removing the permissions for: " + contentlet.getTitle() +
                                    ", id: " + contentlet.getIdentifier());
                    this.permissionAPI.removePermissions(contentlet);
                    Logger.debug(this,
                            () -> "Saving the permissions for: " + contentlet.getTitle() +
                                    ", id: " + contentlet.getIdentifier());
                    this.permissionAPI.save(selectedPermissions.stream()
                                    .map(permission -> new Permission(contentlet.getPermissionId(),
                                            permission.getRoleId(), permission.getPermission()))
                                    .collect(Collectors.toList()), contentlet, user,
                            respectFrontendRoles);
                } catch (Exception e) {

                    Logger.error(this,
                            "Could not save the permissions for: " + contentlet.getTitle() +
                                    ", id: " + contentlet.getIdentifier() + ", msg: "
                                    + e.getMessage(), e);
                }
            };

            FunctionUtils.ifOrElse(DbConnectionFactory.inTransaction(),
                    () -> HibernateUtil.addCommitListener(contentlet.getInode(), savePermissions),
                    () -> savePermissions.run());
        }
    }

    @Override
    public Contentlet checkin(final Contentlet contentlet,
            final ContentletDependencies contentletDependencies)
            throws DotSecurityException, DotDataException {

        if (null != contentletDependencies.getIndexPolicy()) {

            contentlet.setIndexPolicy(contentletDependencies.getIndexPolicy());
        }

        if (null != contentletDependencies.getIndexPolicyDependencies()) {

            contentlet.setIndexPolicyDependencies(
                    contentletDependencies.getIndexPolicyDependencies());
        }

        return checkin(contentlet, contentletDependencies.getRelationships(),
                contentletDependencies.getCategories(), contentletDependencies.getPermissions(),
                contentletDependencies.getModUser(),
                contentletDependencies.isRespectAnonymousPermissions(),
                contentletDependencies.isGenerateSystemEvent());
    }

    /**
     * Triggers a local system event when this contentlet commit listener is executed, anyone who
     * need it can subscribed to this commit listener event, on this case will be mostly use it in
     * order to invalidate this contentlet cache.
     *
     * @param contentlet Contentlet to be processed by the Commit listener event
     * @param user       User that triggered the event
     * @param publish    true if it is publish, false unpublish
     */
    private void triggerCommitListenerEvent(final Contentlet contentlet, final User user,
            final boolean publish) {

        try {
            if (!contentlet.getBoolProperty(Contentlet.IS_TEST_MODE)) {

                HibernateUtil.addCommitListener(new FlushCacheRunnable() {
                    public void run() {
                        //Triggering event listener when this commit listener is executed
                        localSystemEventsAPI
                                .asyncNotify(new CommitListenerEvent(contentlet));
                    }
                });
            } else {

                HibernateUtil.addCommitListener(new FlushCacheRunnable() {
                    public void run() {
                        //Triggering event listener when this commit listener is executed
                        localSystemEventsAPI
                                .notify(new CommitListenerEvent(contentlet));
                    }
                });
            }

            HibernateUtil.addCommitListener(() -> {
                //Triggering event listener when this commit listener is executed
                localSystemEventsAPI
                        .notify(new ContentletPublishEvent(contentlet, user, publish));
            });
        } catch (DotHibernateException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Basically this method updates the mod_date on a piece of content
     *
     * @param inodes
     * @param user
     * @return
     * @throws DotDataException
     */
    @WrapInTransaction
    @Override
    public int updateModDate(final Set<String> inodes, final User user) throws DotDataException {
        return contentFactory.updateModDate(inodes, user);
    }

    /**
     * This method takes the properties that were once set as null an nullify the real properties By
     * doing this right before save. We Will null the field values on the desired entries.
     *
     * @param contentlet
     * @return
     */
    private Contentlet applyNullProperties(final Contentlet contentlet) {
        contentlet.getNullProperties().forEach(s -> {
            contentlet.getMap().put(s, null);
        });
        return contentlet;
    }

    /**
     * sets the host / folder if it is not set to either a sibling's host/folder or the content
     * type's host folder
     *
     * @param contentlet
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    protected Contentlet populateHost(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {
        // check contentlet Host
        // If host and folder are not set yet
        if (!UtilMethods.isSet(contentlet.getHost()) && !UtilMethods.isSet(
                contentlet.getFolder())) {
            // Try to get host from sibling
            final Contentlet crownPrince = findContentletByIdentifierAnyLanguage(
                    contentlet.getIdentifier());
            // if has a viable sibling, take siblings host/folder
            if (UtilMethods.isSet(crownPrince) && UtilMethods.isSet(crownPrince.getHost())
                    && UtilMethods.isSet(crownPrince.getFolder())) {
                contentlet.setHost(crownPrince.getHost());
                contentlet.setFolder(crownPrince.getFolder());
            } else { // Try to get host from Content Type
                contentlet.setHost(contentlet.getContentType().host());
                contentlet.setFolder(contentlet.getContentType().folder());
            }
        }
        if (!UtilMethods.isSet(contentlet.getHost())) {
            contentlet.setHost(APILocator.systemHost().getIdentifier());
        }
        if (!UtilMethods.isSet(contentlet.getFolder())) {
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
        }
        return contentlet;
    }

    /**
     * log info
     * @param action the label of the action
     * @param contentlet the contentlet
     * @param user the user
     * @param contentPushPublishDate the publish date
     * @param contentPushExpireDate the expire date
     */
    private void logInfo(final String action, final Contentlet contentlet, final User user, String contentPushPublishDate, String contentPushExpireDate) {
        ActivityLogger.logInfo(getClass(), action,
                "StartDate: " + contentPushPublishDate + "; "
                        + "EndDate: " + contentPushExpireDate + "; User:" + (user != null
                        ? user.getUserId() : "Unknown")
                        + "; ContentIdentifier: " + (contentlet != null
                        ? contentlet.getIdentifier() : "Unknown"), contentlet != null ? contentlet.getHost() : "Unknown");
    }

}
