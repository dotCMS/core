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
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.google.common.collect.ImmutableList;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.ContentTypeUtil;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.*;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.quartz.job.IdentifierDateJob;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchResponse;

import java.util.*;

public class ContentTypeAPIImpl implements ContentTypeAPI {

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


  @WrapInTransaction
  @Override
  public void delete(ContentType type) throws DotSecurityException, DotDataException {
    perms.checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);

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

    HibernateUtil.addCommitListener(()-> {
      localSystemEventsAPI.notify(new ContentTypeDeletedEvent(type.variable()));
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


  @CloseDBIfOpened
  @Override
  public int count(String condition, BaseContentType base) throws DotDataException {
    try {
      return perms.filterCollection(this.contentTypeFactory.search(condition, base, "mod_date", -1, 0), PermissionAPI.PERMISSION_READ,
          respectFrontendRoles, user).size();
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }
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
    public List<ContentType> search(String condition, BaseContentType base, final String orderBy, final int limit, final int offset)
            throws DotDataException {

        final List<ContentType> returnTypes = new ArrayList<>();
        int rollingOffset = offset;
        try {
            while ((limit<0)||(returnTypes.size() < limit)) {
                final List<ContentType> rawContentTypes = this.contentTypeFactory.search(condition, base, orderBy, limit, rollingOffset);
                if (rawContentTypes.isEmpty()) {
                    break;
                }
                returnTypes.addAll(perms.filterCollection(rawContentTypes, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user));
                if(returnTypes.size() >= limit || rawContentTypes.size()<limit) {
                    break;
                }
                rollingOffset += limit;
            }

            final int maxAmount = (limit<0)?returnTypes.size():Math.min(limit, returnTypes.size());
            return returnTypes.subList(0, maxAmount);
        } catch (DotSecurityException e) {
            throw new DotStateException(e);
        }

    }

  @CloseDBIfOpened
  @Override
  public List<ContentType> findUrlMapped() throws DotDataException {
    return contentTypeFactory.findUrlMapped();
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
    // Sets the host:
    try {
      if (contentType.host() == null || contentType.fixed()) {
        final List<Field> existinFields = contentType.fields();
        contentType = ContentTypeBuilder.builder(contentType).host(Host.SYSTEM_HOST).build();
        contentType.constructWithFields(existinFields);
      }
      if (!UUIDUtil.isUUID(contentType.host()) && !Host.SYSTEM_HOST.equalsIgnoreCase(contentType.host())) {
        HostAPI hapi = APILocator.getHostAPI();
        contentType = ContentTypeBuilder.builder(contentType)
            .host(hapi.resolveHostName(contentType.host(), APILocator.systemUser(), true).getIdentifier()).build();
      }
    } catch (DotDataException e) {
      throw new DotDataException("unable to resolve host:" + contentType.host(), e);
    } catch (DotSecurityException es) {
      throw new DotSecurityException("invalid permissions to:" + contentType.host(), es);
    }

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
                                        final ContentType ctype) throws DotDataException, DotSecurityException {

    ContentType contentTypeToSave = ctype;

    // set to system folder if on system host or the host id of the folder it is on
    List<Field> oldFields = fieldAPI.byContentTypeId(contentTypeToSave.id());

    // Checks if the folder has been set, if so checks the host where that folder lives and set
    // it.
    if (UtilMethods.isSet(contentTypeToSave.folder()) && !contentTypeToSave.folder().equals(Folder.SYSTEM_FOLDER)) {
      contentTypeToSave = ContentTypeBuilder.builder(contentTypeToSave)
          .host(APILocator.getFolderAPI().find(contentTypeToSave.folder(), user, false).getHostId()).build();
    } else if (UtilMethods.isSet(contentTypeToSave.host())) {// If there is no folder set, check
                                                             // if the host has been set, if so
                                                             // set the folder to System Folder
      contentTypeToSave = ContentTypeBuilder.builder(contentTypeToSave).folder(Folder.SYSTEM_FOLDER).build();
    }

    if (!ctype.fields().isEmpty()) {
      contentTypeToSave.constructWithFields(ctype.fields());
    }

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
        if (fireUpdateIdentifiers(oldType.expireDateVar(), contentTypeToSave.expireDateVar())) {
            IdentifierDateJob.triggerJobImmediately(oldOldType, user);
        } else if (fireUpdateIdentifiers(oldType.publishDateVar(), contentTypeToSave.publishDateVar())) {
            IdentifierDateJob.triggerJobImmediately(oldOldType, user);
        }
        perms.resetPermissionReferences(contentTypeToSave);
    }
    ActivityLogger.logInfo(getClass(), "Save ContentType Action",
        "User " + user.getUserId() + "/" + user.getFullName() + " added ContentType " + contentTypeToSave.name()
            + " to host id:" + contentTypeToSave.host());
    AdminLogger.log(getClass(), "ContentType", "ContentType saved : " + contentTypeToSave.name(), user);

    // update the existing content type fields
    if (newFields != null) {

      Map<String, Field> varNamesCantDelete = new HashMap();

      for (Field oldField : oldFields) {
        if (!newFields.stream().anyMatch(f -> f.id().equals(oldField.id()))) {
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
          fieldAPI.save(field, APILocator.systemUser());
        } else {
          // We replace the newField-ID with the oldField-ID in order to be able to update the
          // Field
          // instead of creating a new one due the different ID. We need to be sure new field has
          // same variable and DB column.
          Field oldField = varNamesCantDelete.get(field.variable());
          if (oldField.variable().equals(field.variable()) && oldField.dbColumn().equals(field.dbColumn())) {

            // Create a copy of the new Field with the oldField-ID,
            field = FieldBuilder.builder(field).id(oldField.id()).build();
            fieldAPI.save(field, APILocator.systemUser());
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
}
