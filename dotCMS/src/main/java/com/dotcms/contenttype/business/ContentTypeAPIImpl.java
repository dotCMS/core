package com.dotcms.contenttype.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;

import com.dotcms.api.system.event.ContentTypePayloadDataWrapper;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.UrlMapable;
import com.dotcms.exception.BaseRuntimeInternationalizationException;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.quartz.job.IdentifierDateJob;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

public class ContentTypeAPIImpl implements ContentTypeAPI {

  final ContentTypeFactory fac;
  final FieldFactory ffac;
  final PermissionAPI perms;
  final User user;
  final Boolean respectFrontendRoles;
  final FieldAPI fAPI;



  public ContentTypeAPIImpl(User user, boolean respectFrontendRoles, ContentTypeFactory fac, FieldFactory ffac,
                            PermissionAPI perms, FieldAPI fAPI) {
    super();
    this.fac = fac;
    this.ffac = ffac;
    this.perms = perms;
    this.user = user;
    this.respectFrontendRoles = respectFrontendRoles;
    this.fAPI = fAPI;
  }



  public ContentTypeAPIImpl(User user, boolean respectFrontendRoles) {
    this(user, respectFrontendRoles, FactoryLocator.getContentTypeFactory(), FactoryLocator.getFieldFactory(),
        APILocator.getPermissionAPI(), APILocator.getContentTypeFieldAPI());
  }


  @Override
  public void delete(ContentType type) throws DotSecurityException, DotDataException {
    perms.checkPermission(type, PermissionLevel.PUBLISH, user);

    // permission check delete related contentlets
    DotConnect dc = new DotConnect();
    List<Map<String, Object>> ids = null;

    // test permissions over content
    int maxRows = 100;
    int offset = 0;
    do {
      ids = dc.setSQL(ContentTypeSql.getInstance().SELECT_CONTENTLET_IDS_BY_TYPE).setMaxRows(100).setStartRow(offset)
          .addParam(type.id()).loadObjectResults();
      for (Map<String, Object> id : ids) {
        PermissionableProxy proxy = new PermissionableProxy();
        proxy.setIdentifier((String) id.get("identifier"));
        proxy.setInode((String) id.get("identifier"));
        proxy.setOwner(null);
        proxy.setType(new Contentlet().getType());
        APILocator.getPermissionAPI().checkPermission(proxy, PermissionLevel.PUBLISH, user);
      }
      offset += maxRows;
    } while (ids.size() > 0);



    try {
      fac.delete(type);
    } catch (DotStateException | DotDataException e) {
      Logger.error(ContentType.class, e.getMessage(), e);
      throw new BaseRuntimeInternationalizationException(e);
    }
    try {
      String actionUrl = ContentTypeUtil.getInstance().getActionUrl(type, user);
      ContentTypePayloadDataWrapper contentTypePayloadDataWrapper = new ContentTypePayloadDataWrapper(actionUrl, type);
      APILocator.getSystemEventsAPI().push(SystemEventType.DELETE_BASE_CONTENT_TYPE, new Payload(
          contentTypePayloadDataWrapper, Visibility.PERMISSION, String.valueOf(PermissionAPI.PERMISSION_READ)));
    } catch (DotStateException | DotDataException e) {
      Logger.error(ContentType.class, e.getMessage(), e);
      throw new BaseRuntimeInternationalizationException(e);
    }

  }

  @Override
  public ContentType find(String inode) throws DotSecurityException, DotDataException {
    ContentType type = this.fac.find(inode);
    if (perms.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)) {
      return type;
    }
    throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
  }

  @Override
  public List<ContentType> findAll() throws DotDataException {
    // TODO Auto-generated method stub

    try {
      return perms.filterCollection(this.fac.findAll(), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      Logger.warn(this.getClass(), e.getMessage(), e);
      return ImmutableList.of();
    }

  }

  @Override
  public List<ContentType> findAll(String orderBy) throws DotDataException {
    // TODO Auto-generated method stub

    try {
      return perms.filterCollection(this.fac.findAll(orderBy), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
          user);
    } catch (DotSecurityException e) {
      Logger.warn(this.getClass(), e.getMessage(), e);
      return ImmutableList.of();
    }

  }

  @Override
  public List<ContentType> search(String condition) throws DotDataException {
    try {
      return perms.filterCollection(this.fac.search(condition, "mod_date", -1, 0), PermissionAPI.PERMISSION_READ,
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
    try {
      return perms.filterCollection(this.fac.search(condition, base, "mod_date", -1, 0),
          PermissionAPI.PERMISSION_READ, true, user).size();
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }
  }







  @Override
  public ContentType save(ContentType type) throws DotDataException, DotSecurityException {
	  return save(type, null, null);
  }

  @Override
  public synchronized String suggestVelocityVar(final String tryVar) throws DotDataException {
    if (!UtilMethods.isSet(tryVar)) {
      return UUID.randomUUID().toString();
    } else {
      return this.fac.suggestVelocityVar(tryVar);
    }
  }

  @Override
  public ContentType setAsDefault(ContentType type) throws DotDataException, DotSecurityException {
    perms.checkPermission(type, PermissionLevel.READ, user);
    return fac.setAsDefault(type);

  }

  @Override
  public ContentType findDefault() throws DotDataException, DotSecurityException {
    ContentType type = fac.findDefaultType();
    perms.checkPermission(type, PermissionLevel.READ, user);
    return type;

  }

  @Override
  public List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset)
      throws DotDataException {
    try {
      return perms.filterCollection(this.fac.search("1=1", type, orderBy, limit, offset), PermissionAPI.PERMISSION_READ,
          respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      return ImmutableList.of();
    }

  }

  @Override
  public List<ContentType> findByType(BaseContentType type) throws DotDataException, DotSecurityException {

    try {
      return perms.filterCollection(this.fac.findByBaseType(type), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
          user);
    } catch (DotSecurityException e) {
      return ImmutableList.of();
    }
  }

    @Override
    public List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException {
        List<SimpleStructureURLMap> res = new ArrayList<>();

        for ( ContentType type : fac.findUrlMapped() ){
            if (type instanceof UrlMapable){
                res.add(new SimpleStructureURLMap(type.id(), type.urlMapPattern()));
            }

        }

        return ImmutableList.copyOf(res);
    }

  @Override
  public void moveToSystemFolder(Folder folder) throws DotDataException {

    List<ContentType> types = search("folder='" + folder.getIdentifier() + "'", "mod_date", -1, 0);

    for (ContentType type : types) {
      ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
      builder.host(folder.getHostId());
      builder.folder(Folder.SYSTEM_FOLDER);



      type = fac.save(builder.build());
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
        " { \"query\": { \"query_string\": { \"query\": \"+moduser:{0} +working:true +deleted:false +basetype:{1}\" } },  \"aggs\": { \"recent-contents\": { \"terms\": { \"field\": \"contentType\", \"size\": {2} }, \"aggs\": { \"top_tag_hits\": { \"top_hits\": { \"sort\": [ { \"moddate\": { \"order\": \"desc\" } } ], \"_source\": { \"include\": [ \"title\" ] }, \"size\" : 1 } } }  }  }, \"size\":0 } ";

    int limit = (numberToShow < 1 || numberToShow > 20) ? 20 : numberToShow;

    query = query.replace("{0}", user.getUserId());
    query = query.replace("{1}", String.valueOf(type.getType()));
    query = query.replace("{2}", String.valueOf(limit));

    System.out.println("----");

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



  @Override
  public List<ContentType> search(String condition, String orderBy, int limit, int offset) throws DotDataException {
    try {
      return perms.filterCollection(this.fac.search(condition, orderBy, limit, offset), PermissionAPI.PERMISSION_READ,
          respectFrontendRoles, user);

    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }

  }

  @Override
  public List<ContentType> search(String condition, BaseContentType base, String orderBy, int limit, int offset)
      throws DotDataException {

    try {
      return perms.filterCollection(this.fac.search(condition, base, orderBy, limit, offset),
          PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }

  }

  @Override
  public List<ContentType> findUrlMapped() throws DotDataException {
    return fac.findUrlMapped();
  }
  
  @Override
  public ContentType save(ContentType contentType, List<Field> fields) throws DotDataException, DotSecurityException {
	  return save(contentType, fields, null);
  }

  @Override
  public ContentType save(ContentType contentType, List<Field> fields, List<FieldVariable> fieldVariables) throws DotDataException, DotSecurityException {
	  
	  return LocalTransaction.wrapReturn(() -> {
		  Permissionable parent = contentType.getParentPermissionable();

            ContentType newContentType = contentType;  
		    if (!perms.doesUserHavePermissions(parent,
		        "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_PUBLISH,
		        user)) {
		      throw new DotSecurityException(
		          "User-does-not-have-add-children-or-structure-permission-on-host-folder:" + parent);
		    }

		    // set to system folder if on system host or the host id of the folder it is on
		    List<Field> localFields = newContentType.fields();

		    //Checks if the folder has been set, if so checks the host where that folder lives and set it.
		    if(UtilMethods.isSet(newContentType.folder()) && !newContentType.folder().equals(Folder.SYSTEM_FOLDER)){
		    	newContentType = ContentTypeBuilder.builder(newContentType)
		    	          .host(APILocator.getFolderAPI().find(newContentType.folder(), user, false).getHostId()).build();
		    }else if(UtilMethods.isSet(newContentType.host())){//If there is no folder set, check if the host has been set, if so set the folder to System Folder
		    	newContentType = ContentTypeBuilder.builder(newContentType).folder(Folder.SYSTEM_FOLDER).build();
		    }

		    if (!localFields.isEmpty())
		    	newContentType.constructWithFields(localFields);

          // Sets the host:
          try {
              HostAPI hapi = APILocator.getHostAPI();
              Host host = UUIDUtil.isUUID(newContentType.host()) || "SYSTEM_HOST".equalsIgnoreCase(newContentType.host())
                      ? hapi.find(newContentType.host(), APILocator.systemUser(), true)
                      : hapi.resolveHostName(newContentType.host(), APILocator.systemUser(), true);
              newContentType = ContentTypeBuilder.builder(newContentType).host(host.getIdentifier()).build();
          } catch (DotDataException | DotSecurityException e) {
              throw new DotStateException("unable to resolve host:" + newContentType.host(), e);
          }

		    ContentType oldType = newContentType;
		    try {
		      if (newContentType.id() != null)
		        oldType = this.fac.find(newContentType.id());
		    } catch (NotFoundInDbException notThere) {
		      // not logging, expected when inserting new from separate environment
		    }
		    newContentType = this.fac.save(newContentType);

		    if (oldType != null) {
		      if (fireUpdateIdentifiers(oldType.expireDateVar(), newContentType.expireDateVar())) {
		        IdentifierDateJob.triggerJobImmediately(oldType, user);
		      } else if (fireUpdateIdentifiers(oldType.publishDateVar(), newContentType.publishDateVar())) {
		        IdentifierDateJob.triggerJobImmediately(oldType, user);
		      }
		      perms.resetPermissionReferences(newContentType);
		    }
		    ActivityLogger.logInfo(getClass(), "Save ContentType Action", "User " + user.getUserId() + "/" + user.getFullName()
		        + " added ContentType " + newContentType.name() + " to host id:" + newContentType.host());
		    AdminLogger.log(getClass(), "ContentType", "ContentType saved : " + newContentType.name(), user);

		    
		    //update the existing content type fields
		    if(fields != null){
			  //Fields on current content type version
		      localFields = newContentType.fields();
		      //Create a copy in order to avoid a possible concurrent modification error
		      List<String> localFieldsVarNames = new ArrayList<String>();
		
		      for (Field localField : localFields) {
		      	localFieldsVarNames.add(localField.variable());
		      }
	
	      
		      //for each field in the content type lets create it if doesn't exists and update its properties if it does
		      for( Field field : fields ) {
		    	 fAPI.save(field,APILocator.systemUser());
		    	  
		    	  localFieldsVarNames.remove( field.variable() );
		    	  if(fieldVariables != null){
			    	  for(FieldVariable fieldVariable : fieldVariables) {
			    		  if(fieldVariable.fieldId().equals(field.inode())) {
			    			  fAPI.save(fieldVariable, APILocator.systemUser());
			    		  }
			    	  }
		    	  }
		      }
		      
		      if ( localFieldsVarNames.size() > 0 ) {
		          // we have local fields that didn't came
		          // in the content type. lets remove them
		          for ( String localFieldVarName : localFieldsVarNames ) {
		        	  Field f = fAPI.byContentTypeIdAndVar(newContentType.inode(), localFieldVarName);
		        	  if(!f.fixed()){
		        		  fAPI.delete(f);
		        	  }else{
		        		  Logger.error(this, "Can't delete fixed field:"+f.name()+" from Content Type:"+newContentType.name());
		        	  }
		          }
		      }
	      }
	      
	
		  return newContentType;
	  });
  }


}
