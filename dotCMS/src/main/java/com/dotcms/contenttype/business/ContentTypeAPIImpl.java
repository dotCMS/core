package com.dotcms.contenttype.business;

import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.event.ContentTypeDeletedEvent;
import com.dotcms.contenttype.model.event.ContentTypeSavedEvent;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.ContentTypeUtil;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.EnterpriseFeature;
import com.dotcms.util.LowerKeyMap;
import com.dotcms.workflow.form.WorkflowSystemActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotCorruptedDataException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.quartz.job.ContentTypeDeleteJob;
import com.dotmarketing.quartz.job.IdentifierDateJob;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.elasticsearch.action.search.SearchResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link ContentTypeAPI}. Each content item in dotCMS is an instance of a Content Type. The
 * Content Type specifies all the following:
 * <ul>
 *     <li>What data (in the form of fields) can be added to a content item of that Content Type.</li>
 *     <li>Which fields are required and which are optional.</li>
 *     <li>The default Permissions applied to items of the Content Type.</li>
 *     <li>The location where items of the Content Type are stored (and thus whether they inherit Permissions from the
 *     Content Type itself, or a dotCMS Site or folder).</li>
 *     <li>What Workflow Schemes can be applied to items of the Content Type.</li>
 *     <li>The default Workflow Action to perform on an item of the Content Type in operations where a Workflow Action
 *     is not specified.</li>
 *     <li>Whether or not items of the Content Type are accessible via a URL Map.</li>
 * </ul>
 * You can create your own Content Types to represent the data you wish to display on your site.
 *
 * @author Will Ezell
 * @since Jun 24th, 2016
 */
public class ContentTypeAPIImpl implements ContentTypeAPI {

  public static final String DELETE_CONTENT_TYPE_ASYNC = "DELETE_CONTENT_TYPE_ASYNC";

  public static final String DELETE_CONTENT_TYPE_ASYNC_WITH_JOB = "DELETE_CONTENT_TYPE_ASYNC_WITH_JOB";

  private final ContentTypeFactory contentTypeFactory;
  private final FieldFactory fieldFactory;
  private final PermissionAPI perms;
  private final User user;
  private final Boolean respectFrontendRoles;
  private final FieldAPI fieldAPI;
  private final LocalSystemEventsAPI localSystemEventsAPI;

  public static final String TYPES_AND_FIELDS_VALID_VARIABLE_REGEX = "[_A-Za-z][_0-9A-Za-z]*";

  public ContentTypeAPIImpl(User user, boolean respectFrontendRoles, ContentTypeFactory fac, FieldFactory ffac,
      PermissionAPI perms, FieldAPI fAPI, final LocalSystemEventsAPI localSystemEventsAPI) {
    super();
    this.contentTypeFactory = fac;
    this.fieldFactory = ffac;
    this.perms = perms;
    this.user = user;
    this.respectFrontendRoles = respectFrontendRoles;
    this.fieldAPI = fAPI;
    this.localSystemEventsAPI = localSystemEventsAPI;
  }

  public ContentTypeAPIImpl(User user, boolean respectFrontendRoles) {
    this(user, respectFrontendRoles, FactoryLocator.getContentTypeFactory(), FactoryLocator.getFieldFactory(),
        APILocator.getPermissionAPI(), APILocator.getContentTypeFieldAPI(), APILocator.getLocalSystemEventsAPI());
  }

  @Override
  public void delete(final ContentType contentType) throws DotSecurityException, DotDataException {
    this.delete(contentType, true);
  }

  @Override
  public void deleteSync(final ContentType contentType) throws DotSecurityException, DotDataException {
    this.delete(contentType, false);
  }

  /**
   * Deletes the specified Content Type, either in the same database transaction or as a separate
   * process.
   *
   * @param contentType The {@link ContentType} being deleted.
   * @param async       If the deletion process should be executed asynchronously -- i.e.; in a
   *                    separate process, set this to {@code true}.
   *
   * @throws DotSecurityException The specified User does not have edition permissions on this
   *                              Content Type.
   * @throws DotDataException     An error occurred when interacting with the database.
   */
  private void delete(final ContentType contentType, final boolean async) throws DotSecurityException, DotDataException {
    if (!contentTypeCanBeDeleted(contentType)) {
      Logger.warn(this, String.format("Content Type '%s' does not exist", contentType.name()));
      return;
    }
    boolean asyncDelete = async;
    boolean asyncDeleteWithJob = Config.getBooleanProperty(DELETE_CONTENT_TYPE_ASYNC_WITH_JOB, true);
    if (async) {
      asyncDelete = Config.getBooleanProperty(DELETE_CONTENT_TYPE_ASYNC, true);
    }

    if (!asyncDelete) {
      Logger.debug(this, () -> String.format("Content Type '%s' will be deleted synchronously", contentType.name()));
      this.transactionalDelete(contentType);
    } else {
      //We make a copy to hold all the contentlets that will be deleted asynchronously and then dispose the original one
      this.triggerAsyncDelete(contentType, asyncDeleteWithJob);
    }
  }

  /**
   * This method will delete the content type and all the content associated to it.
   * This has been our traditional way to delete content types. and all its associated pieces of content.
   * @param type
   * @throws DotDataException
   */
  @WrapInTransaction
  private void transactionalDelete(ContentType type) throws DotDataException {
      try {
        contentTypeFactory.delete(type);
      } catch (DotStateException | DotDataException e) {
        Logger.error(ContentType.class, e.getMessage(), e);
        throw new BaseRuntimeInternationalizationException(e);
      }
      try {
        String actionUrl = ContentTypeUtil.getInstance().getActionUrl(type, user);
        ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper(actionUrl, type);
        APILocator.getSystemEventsAPI().pushAsync(SystemEventType.DELETE_BASE_CONTENT_TYPE, new Payload(
                contentTypePayloadDataWrapper, Visibility.PERMISSION, String.valueOf(PermissionAPI.PERMISSION_READ)));
      } catch (DotStateException | DotDataException e) {
        Logger.error(ContentType.class, e.getMessage(), e);
        throw new BaseRuntimeInternationalizationException(e);
      }
      HibernateUtil.addCommitListener(() -> localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type)));
  }

  /**
   * Verifies whether the {@link User} calling this method has the required {@code EDIT}
   * permissions to delete the specified Content Type or not.
   *
   * @param type The {@link ContentType} to be deleted.
   *
   * @return If the {@link User} has the required permissions to delete the specified Content
   * Type, returns {@code true}.
   *
   * @throws DotDataException     An error occurred when accessing the database.
   * @throws DotSecurityException The specified User does not have the necessary permissions to
   *                              perform this action.
   */
  protected boolean contentTypeCanBeDeleted(ContentType type) throws DotDataException, DotSecurityException {
      if (null == type.id()) {
          throw new DotDataException("ContentType must have an id set");
      }

      //sometimes we might get an instance that has been called without having set the id therefore it will be missing certain fields
      //This prevents that scenarios from happening
      final String id = type.id();
      type = Try.of(() -> contentTypeFactory.find(id)).getOrNull();
      if (null == type) {
        Logger.warn(this, "ContentType with id: " + id + " does not exist");
        return false;
      }

      perms.checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);
      return true;
  }

  /**
   * This can a heavy operation. depending on the amount of fields present in the CT we want to copy
   * Therefor it is best if it happens in a separate transaction
   * @param type
   * @return
   * @throws DotDataException
   */
  private ContentType makeDisposableCopy(final ContentType type) throws DotDataException {

    //Now we need a copy of the CT that basically makes it possible relocating content for asynchronous deletion
    final String newName = String.format("%s_disposed_%s", type.variable(), System.currentTimeMillis());

    ContentType copy = ContentTypeBuilder.builder(type)
            .id(null)
            .variable(newName)
            .markedForDeletion(true)
            .build();
    Logger.info(getClass(), String.format("::: CT (%s) with inode:(%s) Will be deleted shortly", type.variable(), type.inode()));
    copy = contentTypeFactory.save(copy);

    //A copy is made. but we need to refresh our var since the copy returned by the method is incomplete
    copy = contentTypeFactory.find(copy.id());
    Logger.info(getClass(), String.format("::: A copy with Var Name '%s' and inode " +
            "'%s' will be used to dispose all contentlets in background. :::", copy.variable(), copy.inode()));
    return copy;
  }


  /**
   * triggers the async delete of the content type
   * @param type content type to delete
   * @param asyncDeleteWithJob this flag allows me to skip the Quartz Job and call the method directly
   * @throws DotDataException
   */
   @WrapInTransaction
   void triggerAsyncDelete(final ContentType type, final boolean asyncDeleteWithJob) throws DotDataException {
      //If we're ok permissions wise, we need to remove the content from the index
      //Then this quickly hides the content from the front end and APIS
     contentTypeFactory.markForDeletion(type);
     final ContentType copy = makeDisposableCopy(type);
     //Once a copy has been made, we need to relocate all the content to the dummy CT and then delete the original
     HibernateUtil.addCommitListener(() -> relocateThenDispose(type, copy, asyncDeleteWithJob));

   }

  /**
   * This method will relocate all the content associated to the content type to the dummy CT and then delete the original
   * @param source
   * @param target*
   */
  @WrapInTransaction
  private void internalRelocateThenDispose(final ContentType source, final ContentType target, final boolean asyncDeleteWithJob) {
    try {
      APILocator.getContentletIndexAPI().removeContentFromIndexByContentType(source);
      final Integer relocated = APILocator.getContentTypeDestroyAPI().relocateContentletsForDeletion(source, target);

      Logger.info(ContentTypeFactoryImpl.class, String.format("::: Relocated %d contentlets for ContentType [%s] to [%s] :::", relocated, source.variable(), target.variable()));

      HibernateUtil.addCommitListener(() -> {
        try {
          disposeSourceThenFireContentDelete(source, target, asyncDeleteWithJob);
        } catch (DotDataException e) {
            Logger.error(ContentTypeFactoryImpl.class, String.format("Error removing content from index for ContentType [%s]", source.variable()), e);
        }
      });
    } catch (DotDataException e) {
      Logger.error(ContentTypeFactoryImpl.class, String.format("Error relocating content for ContentType [%s]", source.variable()), e);
    }
  }

  /**
   * Relocating contentlets allows us to delete the content type without having to wait for the content to be deleted
   * @param source source content type
   * @param target destination content type
   * @param asynchronously if true, the content will be relocated in a separate thread
   */
  void relocateThenDispose(final ContentType source, final ContentType target,
          final boolean asynchronously) {
    if (asynchronously) {
       DotConcurrentFactory.getInstance().getSingleSubmitter("ContentRelocationForDeletion")
              .execute(() -> internalRelocateThenDispose(source, target, true));
    } else {
       //Generally speaking we want to go down this path when we're running integration tests
       internalRelocateThenDispose(source, target, false);
    }
  }

  @WrapInTransaction
  private void disposeSourceThenFireContentDelete( final ContentType source, final ContentType target, final boolean asyncDeleteWithJob) throws DotDataException {

      //Now that all the content is relocated, we're ready to destroy the original structure that held all content
      //But first a few checks to make sure nothing remains outside the relocation process
      final int sourceCount = new DotConnect().setSQL("select count (*) as x from contentlet where structure_inode = ?")
              .addParam(source.inode())
              .getInt("x");

      Logger.debug(getClass(),String.format(" ::: Content still remaining on source sourceCount-type: (%s) is (%d) ::: ", source.name(), sourceCount));

      int targetCount = new DotConnect().setSQL("select count (*) as x from contentlet where structure_inode = ?")
              .addParam(target.inode())
              .getInt("x");

      Logger.debug(getClass(),String.format(" ::: Content moved to target type: (%s) is (%d) ::: ", target.name(), targetCount));

      if(sourceCount > 0){
         Logger.error(getClass(),String.format(" ::: Content still remaining on source sourceCount-type: (%s) is (%d) ::: ", source.name(), sourceCount));
         return;
      }

      //Now that all the content is relocated, we're ready to destroy the original structure that held all content
      //System fields like Categories, Relationships and Containers will be deleted using the original structure
      contentTypeFactory.delete(source);
      //and destroy all associated content under the copy

      HibernateUtil.addCommitListener(() -> {
        //Notify the system events API that the content type has been deleted, so it can take care of the WF clean up
        localSystemEventsAPI.notify(new ContentTypeDeletedEvent(source));
          //By default, the deletion process takes placed within job
          Logger.info(this, String.format(" Content type (%s) will be deleted asynchronously using Quartz Job.", source.name()));
          if(asyncDeleteWithJob) {
            ContentTypeDeleteJob.triggerContentTypeDeletion(target);
          } else {
            try {
              APILocator.getContentTypeDestroyAPI().destroy(target, APILocator.systemUser());
            } catch (DotDataException | DotSecurityException e) {
               Logger.error(ContentTypeFactoryImpl.class, String.format("Error deleting content type [%s]", target.variable()), e);
            }
          }
      });

  }

  @Override
  @CloseDBIfOpened
  public ContentType find(final String inodeOrVar) throws DotSecurityException, DotDataException {
    if(!UtilMethods.isSet(inodeOrVar)) {
        return null;
    }
    final ContentType type = this.contentTypeFactory.find(inodeOrVar);

    if (perms.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)) {

      return type;
    }

    throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> find(final List<String> varNames, final String filter, final int offset, final int limit,
                                          final String orderBy) throws DotSecurityException, DotDataException {
    if (UtilMethods.isNotSet(varNames)) {
      return List.of();
    }
    final int internalOffset = 0;
    final int internalLimit = 500;
    final List<ContentType> contentTypeList;
    final List<String> lowercaseVarNames =
            varNames.stream().map(String::toLowerCase).collect(Collectors.toList());
    if (UtilMethods.isSet(filter)) {
      contentTypeList = this.contentTypeFactory.find(lowercaseVarNames, filter.toLowerCase(), offset, limit, orderBy);
    } else if (offset > 0 || limit > 0) {
      int adjustedLimit = offset + limit;
      adjustedLimit = Math.min(adjustedLimit, lowercaseVarNames.size());
      final List<String> varNamesSubList = lowercaseVarNames.subList(offset, adjustedLimit);
      contentTypeList = this.contentTypeFactory.find(varNamesSubList, null, internalOffset, internalLimit, orderBy);
    } else {
      contentTypeList = this.contentTypeFactory.find(lowercaseVarNames, null, internalOffset, internalLimit, orderBy);
    }
    // Exclude inaccessible Content Types from result list
      return contentTypeList.stream().filter(this::hasReadPermission).collect(Collectors.toList());
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findAll() throws DotDataException {

    try {
      return perms.filterCollection(this.contentTypeFactory.findAll(), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      Logger.warn(this.getClass(), e.getMessage(), e);
      return ImmutableList.of();
    }

  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findAllRespectingLicense() throws DotDataException {
    List<ContentType> allTypes = findAll();
    // exclude ee types when no license
    return LicenseUtil.getLevel() <= LicenseLevel.COMMUNITY.level
            ? allTypes.stream().filter((type) ->!(type instanceof EnterpriseType)).collect(Collectors.toList())
            : allTypes;
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findAll(String orderBy) throws DotDataException {

    try {
      return perms.filterCollection(this.contentTypeFactory.findAll(orderBy), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
          user);
    } catch (DotSecurityException e) {
      Logger.warn(this.getClass(), e.getMessage(), e);
      return ImmutableList.of();
    }

  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> search(String condition) throws DotDataException {
    try {
      return perms.filterCollection(this.contentTypeFactory.search(condition, "mod_date", -1, 0), PermissionAPI.PERMISSION_READ,
          respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }
  }


  @Override
  public int count() throws DotDataException {
    return this.count("1=1");
  }

  @Override
  public int count(String condition) throws DotDataException {
    return search(condition).size();
  }

  @Override
  public int count(String condition, BaseContentType base) throws DotDataException {
    return count(condition,base,null);
  }

  @CloseDBIfOpened
  @Override
  public int count(final String condition, final BaseContentType base, final String siteId) throws DotDataException {
    return countForSites(condition, base, UtilMethods.isSet(siteId) ? List.of(siteId) : null);
  }

  @CloseDBIfOpened
  @Override
  public int countForSites(final String condition, final BaseContentType base, final List<String> siteIds) throws DotDataException {
    try {
      final List<String> resolvedSiteIds = HostUtil.resolveSiteIds(siteIds, this.user, this.respectFrontendRoles);
      return this.perms.filterCollection(this.contentTypeFactory.search(resolvedSiteIds,
              condition, base.getType(), ContentTypeFactory.MOD_DATE_COLUMN, -1, 0),
              PermissionAPI.PERMISSION_READ, this.respectFrontendRoles, this.user).size();
    } catch (final DotSecurityException e) {
      Logger.error(this, String.format("An error occurred when getting the Content Type count for Sites " +
              "[ %s ] with condition [ %s ]: %s", siteIds, condition, ExceptionUtil.getErrorMessage(e)), e);
      throw new DotStateException(e);
    }
  }

  @WrapInTransaction
  @Override
  public ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean) throws DotDataException, DotSecurityException {
    return copyFromAndDependencies(copyContentTypeBean, null, true);
  }

  @WrapInTransaction
  @Override
  public ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite) throws DotDataException, DotSecurityException {
    return copyFromAndDependencies(copyContentTypeBean, destinationSite, true);
  }

  @WrapInTransaction
  @Override
  public ContentType copyFromAndDependencies(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite, final boolean copyRelationshipFields) throws DotDataException, DotSecurityException {
    final ContentType sourceContentType = copyContentTypeBean.getSourceContentType();
    final ContentType copiedContentType = copyFrom(copyContentTypeBean, destinationSite, copyRelationshipFields);
    // saving workflow information
    final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
    final List<WorkflowScheme> workflowSchemes = workflowAPI.findSchemesForContentType(find(sourceContentType.id()));
    final List<SystemActionWorkflowActionMapping> systemActionWorkflowActionMappings = workflowAPI.findSystemActionsByContentType(sourceContentType, user);
    workflowAPI.saveSchemeIdsForContentType(copiedContentType, workflowSchemes.stream().map(WorkflowScheme::getId).collect(Collectors.toSet()));
    final WorkflowHelper workflowHelper = WorkflowHelper.getInstance();
    for (final SystemActionWorkflowActionMapping systemActionWorkflowActionMapping : systemActionWorkflowActionMappings) {
      workflowHelper.mapSystemActionToWorkflowAction(new WorkflowSystemActionForm.Builder()
              .systemAction(systemActionWorkflowActionMapping.getSystemAction())
              .actionId(systemActionWorkflowActionMapping.getWorkflowAction().getId())
              .contentTypeVariable(copiedContentType.variable()).build(), user);
    }
    return copiedContentType;
  }

  @WrapInTransaction
  @Override
  @EnterpriseFeature(licenseLevel = LicenseLevel.PROFESSIONAL, errorMsg = "An enterprise license is required in order to use this feature.")
  public ContentType copyFrom(final CopyContentTypeBean copyContentTypeBean) throws DotDataException, DotSecurityException {
    return copyFrom(copyContentTypeBean, null, true);
  }

  @WrapInTransaction
  @Override
  @EnterpriseFeature(licenseLevel = LicenseLevel.PROFESSIONAL, errorMsg = "An enterprise license is required in order to use this feature.")
  public ContentType copyFrom(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite) throws DotDataException, DotSecurityException {
    return copyFrom(copyContentTypeBean, destinationSite, true);
  }

  @WrapInTransaction
  @Override
  @EnterpriseFeature(licenseLevel = LicenseLevel.PROFESSIONAL, errorMsg = "An enterprise license is required in order to use this feature.")
  public ContentType copyFrom(final CopyContentTypeBean copyContentTypeBean, final Host destinationSite, final boolean copyRelationshipFields) throws DotDataException, DotSecurityException {
    final ContentType sourceContentType = copyContentTypeBean.getSourceContentType();
    final ContentTypeBuilder builder = ContentTypeBuilder.builder(sourceContentType)
            .name(copyContentTypeBean.getName())
            .fixed(false)
            .system(false)
            .id(null)
            .modDate(new Date())
            .variable(null);

    if (UtilMethods.isSet(copyContentTypeBean.getNewVariable())) {
      builder.variable(copyContentTypeBean.getNewVariable());
    }

    if (UtilMethods.isSet(copyContentTypeBean.getFolder())) {
      builder.folder(copyContentTypeBean.getFolder());
    }

    if (UtilMethods.isSet(copyContentTypeBean.getHost())) {
      builder.host(copyContentTypeBean.getHost());
    }

    if (UtilMethods.isSet(copyContentTypeBean.getIcon())) {
      builder.icon(copyContentTypeBean.getIcon());
    }

    if (null != destinationSite && UtilMethods.isSet(destinationSite.getIdentifier())) {
      // If the CT is being copied to another Site, more properties must be copied as well
      builder.siteName(destinationSite.getHostname());
      builder.description(sourceContentType.description());
      builder.detailPage(sourceContentType.detailPage());
      builder.urlMapPattern(sourceContentType.urlMapPattern());
      builder.metadata(sourceContentType.metadata());
    }

    Logger.debug(this, ()->"Creating the content type: " + copyContentTypeBean.getName()
            + ", from: " + copyContentTypeBean.getSourceContentType().variable());

    final ContentType contentType    = builder.build();
    final ContentType newContentType = this.save(contentType);
    final List<Field> sourceFields  = APILocator.getContentTypeFieldAPI().byContentTypeId(sourceContentType.id());
    final Map<String, Field> newFieldMap = newContentType.fieldMap();
    final Map<String, Field> lowerNewFieldMap = new LowerKeyMap<>();
    newFieldMap.entrySet().forEach(entry -> lowerNewFieldMap.put(entry.getKey(), entry.getValue()));

    Logger.debug(this, ()->"Saving the fields for the the content type: " + copyContentTypeBean.getName()
            + ", from: " + copyContentTypeBean.getSourceContentType().variable());

    for (final Field sourceField : sourceFields) {
        DotPreconditions.checkNotEmpty(sourceField.variable(), DotCorruptedDataException.class,
              "Velocity Variable Name in Field ID '%s' cannot be empty", sourceField.id());
        if (sourceField instanceof RelationshipField && !copyRelationshipFields) {
          continue;
        }
        Field newField = lowerNewFieldMap.get(sourceField.variable().toLowerCase());
        if (null == newField) {

            newField = APILocator.getContentTypeFieldAPI()
                    .save(FieldBuilder.builder(sourceField).sortOrder(sourceField.sortOrder()).contentTypeId(newContentType.id()).id(null).build(), user);
        } else {

            // if contains we just need to sort based on the source order
          APILocator.getContentTypeFieldAPI()
                  .save(FieldBuilder.builder(newField).sortOrder(sourceField.sortOrder()).build(), user);
        }

        final List<FieldVariable> currentFieldVariables = sourceField.fieldVariables();
        if (UtilMethods.isSet(currentFieldVariables)) {
          for (final FieldVariable fieldVariable : currentFieldVariables) {

            APILocator.getContentTypeFieldAPI().save(ImmutableFieldVariable.builder().from(fieldVariable).
                    fieldId(newField.id()).id(null).userId(user.getUserId()).build(), user);
          }
        }
    }

    return find(newContentType.id());
  }

  @WrapInTransaction
  @Override
  public ContentType save(ContentType type) throws DotDataException, DotSecurityException {
    return save(type, null, null);
  }

  @CloseDBIfOpened
  @Override
  public synchronized String suggestVelocityVar(final String tryVar) throws DotDataException {
    if (!UtilMethods.isSet(tryVar)) {
      return UUID.randomUUID().toString();
    } else {
      return this.contentTypeFactory.suggestVelocityVar(tryVar);
    }
  }

  @WrapInTransaction
  @Override
  public ContentType setAsDefault(ContentType type) throws DotDataException, DotSecurityException {
    perms.checkPermission(type, PermissionLevel.READ, user);
    return contentTypeFactory.setAsDefault(type);

  }

  @CloseDBIfOpened
  @Override
  public ContentType findDefault() throws DotDataException, DotSecurityException {
    ContentType type = contentTypeFactory.findDefaultType();
    perms.checkPermission(type, PermissionLevel.READ, user);
    return type;

  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset)
      throws DotDataException {
    try {
      return perms.filterCollection(this.contentTypeFactory.search("1=1", type, orderBy, limit, offset), PermissionAPI.PERMISSION_READ,
          respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      return ImmutableList.of();
    }

  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findByType(BaseContentType type) throws DotDataException, DotSecurityException {

    try {
      return perms.filterCollection(this.contentTypeFactory.findByBaseType(type), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
          user);
    } catch (DotSecurityException e) {
      return ImmutableList.of();
    }
  }

  @CloseDBIfOpened
  @Override
  public List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException {
    List<SimpleStructureURLMap> res = new ArrayList<>();

    for (ContentType type : contentTypeFactory.findUrlMapped()) {
      if (type instanceof UrlMapable) {
        res.add(new SimpleStructureURLMap(type.id(), type.urlMapPattern()));
      }

    }

    return ImmutableList.copyOf(res);
  }

  @WrapInTransaction
  @Override
  public void moveToSystemFolder(Folder folder) throws DotDataException {

    final List<ContentType> types = search("folder='" + folder.getInode() + "'", "mod_date", -1, 0);

    for (ContentType type : types) {
      ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
      builder.host(folder.getHostId());
      builder.folder(Folder.SYSTEM_FOLDER);



      type = contentTypeFactory.save(builder.build());
      CacheLocator.getContentTypeCache2().remove(type);
      perms.resetPermissionReferences(type);
    }
  }

  private boolean fireUpdateIdentifiers(String oldVal, String newVal) {
    if (oldVal != null || newVal != null) {
      if (oldVal != null) {
        if (!oldVal.equals(newVal)) {
          return true;
        }
      }
      if (newVal != null) {
        if (!newVal.equals(oldVal)) {
          return true;
        }
      }
    }
    return false;
  }

  public List<ContentType> recentlyUsed(BaseContentType type, int numberToShow) throws DotDataException {

    String query =
        " { \"query\": { \"query_string\": { \"query\": \"+moduser:{0} +working:true +deleted:false +basetype:{1}\" } },  \"aggs\": { \"recent-contents\": { \"terms\": { \"field\": \"contentType_dotraw\", \"size\": {2} }, \"aggs\": { \"top_tag_hits\": { \"top_hits\": { \"sort\": [ { \"moddate\": { \"order\": \"desc\" } } ], \"_source\": { \"include\": [ \"title\" ] }, \"size\" : 1 } } }  }  }, \"size\":0 } ";

    int limit = (numberToShow < 1 || numberToShow > 20) ? 20 : numberToShow;

    query = query.replace("{0}", user.getUserId());
    query = query.replace("{1}", String.valueOf(type.getType()));
    query = query.replace("{2}", String.valueOf(limit));

    try {
      SearchResponse raw = APILocator.getEsSearchAPI().esSearchRaw(query.toLowerCase(), false, user, false);


      JSONObject jo = new JSONObject(raw.toString()).getJSONObject("aggregations").getJSONObject("recent-contents");
      JSONArray ja = jo.getJSONArray("buckets");
      List<ContentType> ret = new ArrayList<>();
      for (int i = 0; i < ja.size(); i++) {
        JSONObject joe = ja.getJSONObject(i);
        String var = joe.getString("key");

        ret.add(find(var));
      }



      return ImmutableList.copyOf(ret);
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }

  public Map<String, Long> getEntriesByContentTypes() throws DotDataException {
    String query = "{" + "  \"aggs\" : {" + "    \"entries\" : {" + "       \"terms\" : { \"field\" : \"contenttype_dotraw\",  \"size\" : " + Integer.MAX_VALUE + "}"
        + "     }" + "   }," + "   \"size\":0}";

    try {
      SearchResponse raw = APILocator.getEsSearchAPI().esSearchRaw(query.toLowerCase(), false, user, false);

      JSONObject jo = new JSONObject(raw.toString()).getJSONObject("aggregations").getJSONObject("sterms#entries");
      JSONArray ja = jo.getJSONArray("buckets");

      Map<String, Long> result = new HashMap<>();

      for (int i = 0; i < ja.size(); i++) {
        JSONObject jsonObject = ja.getJSONObject(i);
        String contentTypeName = jsonObject.getString("key");
        long count = jsonObject.getLong("doc_count");

        result.put(contentTypeName, count);
      }

      return result;
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }


  @CloseDBIfOpened
  @Override
  public List<ContentType> search(String condition, String orderBy, int limit, int offset) throws DotDataException {
      return this.search( condition, BaseContentType.ANY, orderBy,  limit,  offset);
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> search(String condition, String orderBy, int limit, int offset,final String hostId) throws DotDataException {
    return this.search( condition, BaseContentType.ANY, orderBy,  limit,  offset,hostId);
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> search(String condition, BaseContentType base, final String orderBy, final int limit, final int offset) throws DotDataException {
    return this.search( condition, base, orderBy,  limit,  offset,null);
  }

  @CloseDBIfOpened
  @Override
  public List<ContentType> search(final List<String> sites, final String condition, final BaseContentType base, final String orderBy, final int limit, final int offset)
          throws DotDataException {

    final List<ContentType> returnTypes = new ArrayList<>();
    int rollingOffset = offset;
    try {
      while ((limit<0)||(returnTypes.size() < limit)) {
        final List<String> resolvedSiteIds = HostUtil.resolveSiteIds(sites, this.user, this.respectFrontendRoles);
        final List<ContentType> rawContentTypes = this.contentTypeFactory.search(resolvedSiteIds,
                condition, base.getType(), orderBy, limit, rollingOffset);
        if (rawContentTypes.isEmpty()) {
          break;
        }
        returnTypes.addAll(this.perms.filterCollection(rawContentTypes, PermissionAPI.PERMISSION_READ, this.respectFrontendRoles, this.user));
        if(returnTypes.size() >= limit || rawContentTypes.size()<limit) {
          break;
        }
        rollingOffset += limit;
      }

      final int maxAmount = (limit<0)?returnTypes.size():Math.min(limit, returnTypes.size());
      return returnTypes.subList(0, maxAmount);
    } catch (final DotSecurityException e) {
      Logger.error(this, String.format("An error occurred when searching for Content Types: " +
              "%s", ExceptionUtil.getErrorMessage(e)));
      throw new DotStateException(e);
    }
  }

  @CloseDBIfOpened
    @Override
    public List<ContentType> search(final String condition, final BaseContentType base, final String orderBy, final int limit, final int offset, final String siteId)
            throws DotDataException {
        return search(UtilMethods.isSet(siteId) ? List.of(siteId) : List.of(), condition, base, orderBy, limit, offset);
    }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findUrlMapped() throws DotDataException {
    return contentTypeFactory.findUrlMapped();
  }

  @CloseDBIfOpened
  @Override
  public List<String> findUrlMappedPattern(final String pageIdentifier) throws DotDataException{

    DotPreconditions.checkArgument(UtilMethods.isSet(pageIdentifier), "pageIdentifier is required");

    return contentTypeFactory.findUrlMappedPattern(pageIdentifier);
  }

  @WrapInTransaction
  @Override
  public ContentType save(ContentType contentType, List<Field> newFields)
      throws DotDataException, DotSecurityException {
    return save(contentType, newFields, null);
  }

  @WrapInTransaction
  @Override
  public ContentType save(ContentType contentType, List<Field> newFields, List<FieldVariable> newFieldVariables)
      throws DotDataException, DotSecurityException {

    if(UtilMethods.isSet(contentType.variable())) {
      DotPreconditions.checkArgument(contentType.variable().matches(
              TYPES_AND_FIELDS_VALID_VARIABLE_REGEX),
              "Invalid content type variable: " + contentType.variable(),
              IllegalArgumentException.class);
    }

    contentType = SiteAndFolderResolver.newInstance(user).resolveSiteAndFolder(contentType);

    // check perms
    Permissionable parent = contentType.getParentPermissionable();
    if (!perms.doesUserHavePermissions(parent,
        "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_EDIT_PERMISSIONS,
        user)) {
      throw new DotSecurityException(
          "User-does-not-have-add-children-or-structure-permission-on-host-folder:" + parent);
    }

    try {
      if (contentType.id() != null) {
        final ContentType oldType = this.contentTypeFactory.find(contentType.id());
        if (!perms.doesUserHavePermission(oldType, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user, respectFrontendRoles)) {
          throw new DotSecurityException("You don't have permission to edit this content type.");
        }
      }
    } catch (NotFoundInDbException notThere) {
      Logger.info(this, "Content type not found with given id: " + contentType.id() + ". Continuing...");
    }

    return transactionalSave(newFields, newFieldVariables, contentType);
  }

  @WrapInTransaction
  private ContentType transactionalSave(final List<Field> newFields,
                                        final List<FieldVariable> newFieldVariables,
                                         ContentType contentTypeToSave) throws DotDataException, DotSecurityException {


    // set to system folder if on system host or the host id of the folder it is on
    final List<Field> oldFields = fieldAPI.byContentTypeId(contentTypeToSave.id());

    ContentType oldType = null;
    try {
      if (contentTypeToSave.id() != null) {
        oldType = this.contentTypeFactory.find(contentTypeToSave.id());
      }
    } catch (NotFoundInDbException notThere) {
      // not logging, expected when inserting new from separate environment
    }

    contentTypeToSave = this.contentTypeFactory.save(contentTypeToSave);

    if (oldType != null) {

      final ContentType oldOldType = oldType;
      if (fireUpdateIdentifiers(oldType.expireDateVar(), contentTypeToSave.expireDateVar())
              || fireUpdateIdentifiers(oldType.publishDateVar(),
              contentTypeToSave.publishDateVar())) {
        IdentifierDateJob.triggerJobImmediately(oldOldType, user);
      }
      perms.resetPermissionReferences(contentTypeToSave);
    }
    ActivityLogger.logInfo(getClass(), "Save ContentType Action",
        "User " + user.getUserId() + "/" + user.getFullName() + " added ContentType " + contentTypeToSave.name()
            + " to host id:" +  contentTypeToSave.host());
    AdminLogger.log(getClass(), "ContentType", "ContentType saved : " + contentTypeToSave.name(), user);

    // update the existing content type fields
    if (newFields != null) {

      Map<String, Field> varNamesCantDelete = new HashMap<>();

      for (Field oldField : oldFields) {
        if (!newFields.stream().anyMatch(f -> oldField.id().equals(f.id()))) {
          if (!oldField.fixed() && !oldField.readOnly()) {
            Logger.info(this, "Deleting no longer needed Field: " + oldField.name() + " with ID: " + oldField.id()
                + ", from Content Type: " + contentTypeToSave.name());

            fieldAPI.delete(oldField);
          } else {
            Logger.info(this, "Can't delete Field because is fixed: " + oldField.name() + " with ID: " + oldField.id()
                + ", from Content Type: " + contentTypeToSave.name());
            varNamesCantDelete.put(oldField.variable(), oldField);
          }
        }
      }

      // for each field in the content type lets create it if doesn't exists and update its
      // properties if it does
      for (Field field : newFields) {

        field = this.checkContentTypeFields(contentTypeToSave, field);
        if (!varNamesCantDelete.containsKey(field.variable())) {
          fieldAPI.save(field, APILocator.systemUser(), false);
        } else {
          // We replace the newField-ID with the oldField-ID in order to be able to update the
          // Field
          // instead of creating a new one due the different ID. We need to be sure new field has
          // same variable and DB column.
          Field oldField = varNamesCantDelete.get(field.variable());
          if (oldField.variable().equals(field.variable()) && oldField.dbColumn().equals(field.dbColumn())) {

            // Create a copy of the new Field with the oldField-ID,
            field = FieldBuilder.builder(field).id(oldField.id()).build();
            fieldAPI.save(field, APILocator.systemUser(), false);
          } else {
            // If the field don't match on VariableName and DBColumn we log an error.
            Logger.error(this, "Can't save Field with already existing VariableName: " + field.variable() + ", id: "
                + field.id() + ", DBColumn: " + field.dbColumn());
          }
        }

        if (newFieldVariables != null && !newFieldVariables.isEmpty()) {
          for (FieldVariable fieldVariable : newFieldVariables) {
            if (fieldVariable.fieldId().equals(field.inode())) {
              fieldAPI.save(fieldVariable, APILocator.systemUser());
            }
          }
        }
      }
    }

    final ContentType savedContentType = find(contentTypeToSave.id());

    HibernateUtil.addCommitListener(()-> {
      localSystemEventsAPI.notify(new ContentTypeSavedEvent(savedContentType));
    });

    return savedContentType;
  }


  private Field checkContentTypeFields(final ContentType contentType, final Field field) {

      if (UtilMethods.isSet(field) && UtilMethods.isSet(contentType.id())) {

          return  !UtilMethods.isSet(field.contentTypeId()) || !field.contentTypeId().equals(contentType.id()) ?
                FieldBuilder.builder(field).contentTypeId(contentType.id()).build() : field;

      }

      return field;
  }

  @WrapInTransaction
  @Override
  public boolean updateModDate(final ContentType type) throws DotDataException {
    boolean updated = false;

    contentTypeFactory.updateModDate(type);

    updated = true;

    return updated;
  }

  @WrapInTransaction
  @Override
  public boolean updateModDate(Field field) throws DotDataException {
    return this.updateModDate( contentTypeFactory.find( field.contentTypeId() ) );
  }

  @WrapInTransaction
  @Override
  public void unlinkPageFromContentType(ContentType contentType)
          throws DotSecurityException, DotDataException {
    ContentTypeBuilder builder =
            ContentTypeBuilder.builder(contentType).urlMapPattern(null).detailPage(null);
    save(builder.build());
  }


  @Override
  public boolean isContentTypeAllowed(ContentType contentType) {
      return LicenseManager.getInstance().isEnterprise() || !BaseContentType
              .getEnterpriseBaseTypes().contains(contentType.baseType());
  }

  public long countContentTypeAssignedToNotSystemWorkflow() throws DotDataException {
    return contentTypeFactory.countContentTypeAssignedToNotSystemWorkflow();
  }
  /**
   * Utility method which verifies whether the current User has {@link PermissionAPI#PERMISSION_READ} permission on a
   * given Content Type or not.
   *
   * @param type The {@link ContentType} use permission will be checked.
   *
   * @return If the {@link User} used by this API has {@code READ} permission on the specific Content Type, returns
   * {@code true}.
   */
  private boolean hasReadPermission(final ContentType type) {
    try {
      return this.perms.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user);
    } catch (final DotDataException e) {
      Logger.warn(this,
              String.format("READ Permission for user '%s' on Content Type '%s' [%s] could not be checked: %s", user.getUserId(), type.name(),
                      type.id(), e.getMessage()));
      return Boolean.FALSE;
    }
  }

}
