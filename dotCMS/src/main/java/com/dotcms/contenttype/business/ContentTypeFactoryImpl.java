package com.dotcms.contenttype.business;

import static com.dotcms.contenttype.business.ContentTypeAPIImpl.TYPES_AND_FIELDS_VALID_VARIABLE_REGEX;

import com.dotcms.contenttype.business.sql.ContentTypeSql;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.*;
import com.dotcms.contenttype.transform.contenttype.DbContentTypeTransformer;
import com.dotcms.contenttype.transform.contenttype.ImplClassContentTypeTransformer;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.*;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkFlowFactory;
import com.dotmarketing.util.*;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.time.DateUtils;

import java.util.*;
import java.util.Calendar;

public class ContentTypeFactoryImpl implements ContentTypeFactory {

  final ContentTypeSql contentTypeSql;
  final ContentTypeCache2 cache;

  
  final static Set<String> reservedContentTypeVars = ImmutableSet.<String>builder()
                  .add("basetype")
                  .add("categories")
                  .add("conhost")
                  .add("conhost")
                  .add("conhostname")
                  .add("contenttype")
                  .add("deleted")
                  .add("expdate")
                  .add("host")
                  .add("identifier")
                  .add("inode")
                  .add("languageid")
                  .add("live")
                  .add("locked")
                  .add("metadata")
                  .add("moddate")
                  .add("moduser")
                  .add("originalstartdate")
                  .add("owner")
                  .add("ownercanpublish")
                  .add("ownercanread")
                  .add("ownercanwrite")
                  .add("parentpath")
                  .add("path")
                  .add("permissions")
                  .add("pubdate")
                  .add("recurrenceend")
                  .add("recurrencestart")
                  .add("shortid")
                  .add("shortinode")
                  .add("structurename")
                  .add("structuretype")
                  .add("tags")
                  .add("title")
                  .add("type")
                  .add("urlmap")
                  .add("versionts")
                  .add("wfassign")
                  .add("wfcreatedby")
                  .add("wfcurrentstepname")
                  .add("wfmoddate")
                  .add("wfscheme")
                  .add("wfstep")
                  .add("working")
                  .build();
  
  
  
  public ContentTypeFactoryImpl() {
    this.contentTypeSql = ContentTypeSql.getInstance();
    this.cache = CacheLocator.getContentTypeCache2();
  }

    @Override
    public ContentType find(String id) throws DotDataException {

        ContentType type = cache.byVarOrInode(id);

        if (type == null) {

            /*
             1. If the if has UUID format lets try to find the ContentType by id.
             2. If not let's try to find it by var name.
             3. If the id is a really old inode, it will not have the UUID format but still need to catch that case.
             */
            if ( UUIDUtil.isUUID(id) ){
              type = dbById(id);
            } else {
                try {
                  type = dbByVar(id);
                } catch (NotFoundInDbException e){
                  type = dbById(id);
                }

            }

            type.fieldMap();
            Logger.debug(this.getClass(), "found type by db:" + type.name());
            cache.add(type);
        }

        return type;
    }

  @Override
  public List<ContentType> findAll() throws DotDataException {
      return dbAll("structuretype,upper(name)");
  }

  @Override
  public void delete(ContentType type) throws DotDataException {
      dbDelete(type);
      cache.remove(type);
  }

  @Override
  public List<ContentType> findAll(String orderBy) throws DotDataException {
      return dbAll(orderBy);
  }

  @Override
  public List<ContentType> findUrlMapped() throws DotDataException {
      return dbSearch(" url_map_pattern is not null ", BaseContentType.ANY.getType(), "mod_date", -1, 0);
  }

  @Override
  public List<ContentType> search(String search, int baseType, String orderBy, int limit, int offset)
      throws DotDataException {
      return dbSearch(search, baseType, orderBy, limit, offset);
  }

  @Override
  public List<ContentType> search(String search, BaseContentType baseType, String orderBy, int limit, int offset)
      throws DotDataException {
    return dbSearch(search, baseType.getType(), orderBy, limit, offset);
  }

  @Override
  public List<ContentType> search(String search, String orderBy, int limit, int offset) throws DotDataException {
    
    return search(search, BaseContentType.ANY, orderBy, limit, offset);
  }

  @Override
  public List<ContentType> search(String search, String orderBy) throws DotDataException {
    return search(search, BaseContentType.ANY, orderBy, Config.getIntProperty("PER_PAGE", 50), 0);
  }

  @Override
  public List<ContentType> search(String search) throws DotDataException {
    return search(search, BaseContentType.ANY, "mod_date desc", Config.getIntProperty("PER_PAGE", 50), 0);
  }

  @Override
  public List<ContentType> search(String search, int limit) throws DotDataException {
    return search(search, BaseContentType.ANY, "mod_date desc", limit, 0);
  }

  @Override
  public List<ContentType> search(String search, String orderBy, int limit) throws DotDataException {
    return search(search, BaseContentType.ANY, orderBy, limit, 0);
  }

  @Override
  public int searchCount(String search) throws DotDataException {
      return dbCount(search, BaseContentType.ANY.getType());
  }

  @Override
  public int searchCount(String search, int baseType) throws DotDataException {
      return dbCount(search, baseType);
  }

  @Override
  public int searchCount(String search, BaseContentType baseType) throws DotDataException {
      return dbCount(search, baseType.getType());
  }

  @Override
  public List<ContentType> findByBaseType(BaseContentType type) throws DotDataException {
      return dbByType(type.getType());
  }


  @Override
  public ContentType findDefaultType() throws DotDataException {
      return dbSelectDefaultType();
  }

  @Override
  public List<ContentType> findByBaseType(int type) throws DotDataException {
      return dbByType(type);
  }

  @Override
  public ContentType save(ContentType type) throws DotDataException {

      ContentType returnType = dbSaveUpdate(type);
      cache.remove(returnType);
      if (type instanceof UrlMapable) {
        cache.clearURLMasterPattern();
      }
      validateFields(returnType);

      return find(returnType.id());
  }

  @Override
  public ContentType setAsDefault(ContentType type) throws DotDataException {
    final ContentType oldDefault = findDefaultType();
    if (!type.equals(oldDefault)) {
        ContentType returnType = dbUpdateDefaultToTrue(type);
        cache.remove(type);
        cache.remove(oldDefault);
        return returnType;
    }

    return type;
  }



  private ContentType dbSelectDefaultType() throws DotDataException {
    DotConnect dc = new DotConnect().setSQL(this.contentTypeSql.SELECT_DEFAULT_TYPE);

    return new DbContentTypeTransformer(dc.loadObjectResults()).from();
  }

  private ContentType dbUpdateDefaultToTrue(ContentType type) throws DotDataException {

    new DotConnect().setSQL(this.contentTypeSql.UPDATE_ALL_DEFAULT).addParam(false).loadResult();
    type = ContentTypeBuilder.builder(type).defaultType(true).build();
    return save(type);


  }

  private List<ContentType> dbByType(int type) throws DotDataException {
    DotConnect dc = new DotConnect();
    String sql = this.contentTypeSql.SELECT_BY_TYPE;
    dc.setSQL(String.format(sql, "mod_date desc")).addParam(type);

    return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

  }

  private List<ContentType> dbAll(String orderBy) throws DotDataException {
    DotConnect dc = new DotConnect();
    String sql = this.contentTypeSql.SELECT_ALL;
    orderBy = SQLUtil.sanitizeSortBy(orderBy);
    dc.setSQL(String.format(sql, orderBy));

    return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

  }

  private ContentType dbById(@NotNull String id) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.SELECT_BY_INODE);
    dc.addParam(id);
    List<Map<String, Object>> results;

    results = dc.loadObjectResults();
    if (results.size() == 0) {
      throw new NotFoundInDbException("Content Type with id:'" + id + "' not found");
    }
    return new DbContentTypeTransformer(results.get(0)).from();

  }

  @Override
  public String suggestVelocityVar(final String tryVar) throws DotDataException {

    DotConnect dc = new DotConnect();
    final String suggestedVarName = VelocityUtil.convertToVelocityVariable(tryVar, true);
    String varName = suggestedVarName;
    for (int i = 1; i < 100000; i++) {
      dc.setSQL(this.contentTypeSql.SELECT_COUNT_VAR);
      dc.addParam(varName.toLowerCase());
      if (dc.getInt("test") == 0 && !reservedContentTypeVars.contains(varName)) {
        return varName;
      }
      varName = suggestedVarName + i;
    }
    throw new DotDataException("Unable to suggest a variable name.  Got to:" + varName);

  }

  private ContentType dbByVar(String var) throws DotDataException {

    if (var == null) {
      throw new NotFoundInDbException("Content Type with var:" + var + " not found");
    }
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.SELECT_BY_VAR);
    dc.addParam(var.toLowerCase());
    List<Map<String, Object>> results;

    results = dc.loadObjectResults();
    if (results.size() == 0) {
      throw new NotFoundInDbException("Content Type with var:" + var + " not found");
    }
    return new DbContentTypeTransformer(results.get(0)).from();

  }

  private ContentType dbSaveUpdate(final ContentType saveType) throws DotDataException {


    ContentTypeBuilder builder =
        ContentTypeBuilder.builder(saveType).modDate(DateUtils.round(new Date(), Calendar.SECOND));

    boolean isNew = false;
    if (saveType.id() == null) {
      isNew = true;
      builder.id(UUID.randomUUID().toString()).build();
    }

    if (!(saveType instanceof UrlMapable)) {
      builder.urlMapPattern(null);
      builder.detailPage(null);
    }
    if (!(saveType instanceof Expireable)) {
      builder.expireDateVar(null);
      builder.publishDateVar(null);
    }

    ContentType oldContentType = null;
    try {
      oldContentType = dbById(saveType.id());
    } catch (NotFoundInDbException notThere) {
      Logger.debug(getClass(), "structure inode not found in db:" + saveType.id());
    }

    if (oldContentType == null) {
    	if (UtilMethods.isSet(saveType.variable())) {
    		builder.variable(saveType.variable());
    	} else {
    		final String generatedVar = suggestVelocityVar(saveType.name());

    		if(!generatedVar.matches(
                  TYPES_AND_FIELDS_VALID_VARIABLE_REGEX)) {
              throw new IllegalArgumentException("Invalid content type variable: " + generatedVar);
            }
    		builder.variable(generatedVar);
    	}
    } else {
    	builder.variable(oldContentType.variable());
    }

    ContentType retType = builder.build();

    if (oldContentType == null) {
    	dbInodeInsert(retType);
    	dbInsert(retType);

    	if (isNew) {
    		if (ContentTypeAPI.reservedStructureNames.contains(retType.name().toLowerCase())) {
    			throw new DotDataException("cannot save a structure with name:" + retType.name());
    		}
    		if (ContentTypeAPI.reservedStructureVars.contains(retType.variable().toLowerCase())) {
    			throw new DotDataException("cannot save a structure with name:" + retType.name());
    		}
    	}

    } else {

    	dbInodeUpdate(retType);
    	dbUpdate(retType);
    	retType = new ImplClassContentTypeTransformer(retType).from();
    }

    // set up default fields
    if (oldContentType == null) {
    	List<Field> fields = new ArrayList<Field>(saveType.fields());

        for (Field ff : retType.requiredFields()) {
          Optional<Field> optional = fields.stream().filter(x -> ff.variable().equalsIgnoreCase(x.variable())).findFirst();
          if (!optional.isPresent()) {
            fields.add(ff);
          }
        }

        FieldAPI fapi = APILocator.getContentTypeFieldAPI();
        for (Field f : fields) {
          f = FieldBuilder.builder(f).contentTypeId(retType.id()).build();
          try {
            fapi.save(f, APILocator.systemUser());
          } catch (DotSecurityException e) {
            throw new DotStateException(e);
          }
        }
    }

    return retType;
  }

  private void dbInodeUpdate(final ContentType type) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.UPDATE_TYPE_INODE);
    dc.addParam(type.owner());
    dc.addParam(type.id());
    dc.loadResult();
  }

  private void dbInodeInsert(final ContentType type) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.INSERT_TYPE_INODE);
    dc.addParam(type.id());
    dc.addParam(type.iDate());
    dc.addParam(type.owner());
    dc.loadResult();
  }

  private void dbUpdate(ContentType type) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.UPDATE_TYPE);
    dc.addParam(type.name());
    dc.addParam(type.description());
    dc.addParam(type.defaultType());
    dc.addParam(type.detailPage());
    dc.addParam(type.baseType().getType());
    dc.addParam(type.system());
    dc.addParam(type.fixed());
    dc.addParam(type.variable());
    dc.addParam(new CleanURLMap(type.urlMapPattern()).toString());
    dc.addParam(type.host());
    dc.addParam(type.folder());
    dc.addParam(type.expireDateVar());
    dc.addParam(type.publishDateVar());
    dc.addParam(type.modDate());
    dc.addParam(type.id());
    dc.loadResult();
  }

  private void dbInsert(ContentType type) throws DotDataException {



    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.INSERT_TYPE);
    dc.addParam(type.id());
    dc.addParam(type.name());
    dc.addParam(type.description());
    dc.addParam(type.defaultType());
    dc.addParam(type.detailPage());
    dc.addParam(type.baseType().getType());
    dc.addParam(type.system());
    dc.addParam(type.fixed());
    dc.addParam(type.variable());
    dc.addParam(new CleanURLMap(type.urlMapPattern()).toString());
    dc.addParam(type.host());
    dc.addParam(type.folder());
    dc.addParam(type.expireDateVar());
    dc.addParam(type.publishDateVar());
    dc.addParam(type.modDate());
    dc.loadResult();
  }

  private boolean dbDelete(ContentType type) throws DotDataException {

    // default structure can't be deleted
    if (type.defaultType()) {
      throw new DotDataException("contenttype.delete.cannot.delete.default.type");
    }
    if (type.system()) {
      throw new DotDataException("contenttype.delete.cannot.delete.system.type");
    }

    // deleting fields
    FactoryLocator.getFieldFactory().deleteByContentType(type);

    // make sure folders don't refer to this structure as default fileasset structure

    updateFolderFileAssetReferences(type);


    // delete container structures
    APILocator.getContainerAPI().deleteContainerStructureByContentType(type);

    // delete contentlets
    deleteContentletsByType(type);

    // delete workflow schema references
    deleteWorkflowSchemeReference(type);

    // remove structure permissions
    APILocator.getPermissionAPI().removePermissions(type);


    // delete relationships
    deleteRelationships(type);



    // remove structure itself
    DotConnect dc = new DotConnect();
    dc.setSQL(this.contentTypeSql.DELETE_TYPE_BY_INODE).addParam(type.id()).loadResult();
    dc.setSQL(this.contentTypeSql.DELETE_INODE_BY_INODE).addParam(type.id()).loadResult();
    return true;
  }

  private List<ContentType> dbSearch(String search, int baseType, String orderBy, int limit, int offset)
      throws DotDataException {
    int bottom = (baseType == 0) ? 0 : baseType;
    int top = (baseType == 0) ? 100000 : baseType;
    if (limit == 0)
      throw new DotDataException("limit param must be more than 0");
    limit = (limit < 0) ? 10000 : limit;
    // our legacy code passes in raw sql conditions and so we need to detect
    // and handle those
    SearchCondition searchCondition = new SearchCondition(search);
    //check if order by is set, if not set it to mod_date
    if(SQLUtil.sanitizeSortBy(orderBy).isEmpty()){
    	orderBy = "mod_date";
    }
    DotConnect dc = new DotConnect();
    dc.setSQL( String.format( this.contentTypeSql.SELECT_QUERY_CONDITION, SQLUtil.sanitizeCondition( searchCondition.condition ), orderBy ) );
    dc.setMaxRows(limit);
    dc.setStartRow(offset);
    dc.addParam( searchCondition.search );
    dc.addParam(searchCondition.search.toLowerCase());
    dc.addParam( searchCondition.search );
    dc.addParam(bottom);
    dc.addParam(top);
    
    Logger.debug(this, "QUERY " + dc.getSQL());

    return new DbContentTypeTransformer(dc.loadObjectResults()).asList();

  }

  private int dbCount(String search, int baseType) throws DotDataException {
    int bottom = (baseType == 0) ? 0 : baseType;
    int top = (baseType == 0) ? 100000 : baseType;

    SearchCondition searchCondition = new SearchCondition(search);

    DotConnect dc = new DotConnect();
    dc.setSQL( String.format( this.contentTypeSql.SELECT_COUNT_CONDITION, SQLUtil.sanitizeCondition( searchCondition.condition ) ) );
    dc.addParam( searchCondition.search );
    dc.addParam( searchCondition.search.toLowerCase());
    dc.addParam( searchCondition.search );
    dc.addParam(bottom);
    dc.addParam(top);
    return dc.getInt("test");
  }

  private void updateFolderFileAssetReferences(ContentType type) throws DotDataException {
    if (!(type instanceof FileAssetContentType))
      return;
    ContentType defaultFileAssetStructure = find(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
    DotConnect dc = new DotConnect();
    dc.setSQL("update folder set default_file_type = ? where default_file_type = ?");
    dc.addParam(defaultFileAssetStructure.id());
    dc.addParam(type.id());
    dc.loadResult();
  }

  private void deleteWorkflowSchemeReference(ContentType type) throws DotDataException {
    final WorkFlowFactory workFlowFactory = FactoryLocator.getWorkFlowFactory();
    workFlowFactory.forceDeleteSchemeForContentType(type.id());
  }

  private void deleteContentletsByType(ContentType type) throws DotDataException {
    // permissions have already been checked at this point
    int limit = 200;
    ContentletAPI conAPI = APILocator.getContentletAPI();
    List<Contentlet> contentlets;

    try {
      contentlets = conAPI.findByStructure(type.id(), APILocator.systemUser(), false, limit, 0);

      while (contentlets.size() > 0) {
        conAPI.destroy(contentlets, APILocator.systemUser(), false);
        contentlets = conAPI.findByStructure(type.id(), APILocator.systemUser(), false, limit, 0);
      }
    } catch (DotSecurityException e) {
      throw new DotDataException(e);
    }
  }

  private void deleteRelationships(ContentType type) throws DotDataException {
    List<Relationship> relationships;
    //Deletes the child relationship field (if exists) if the parent is deleted.
    relationships = FactoryLocator.getRelationshipFactory().byParent(type);
    for (final Relationship rel : relationships) {
      if(UtilMethods.isSet(rel.getParentRelationName()) && rel.isRelationshipField()) {
        final Field fieldToDelete = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(rel.getChildStructureInode(), rel.getParentRelationName());
        APILocator.getContentTypeFieldAPI().delete(fieldToDelete);
      }
      FactoryLocator.getRelationshipFactory().delete(rel);
    }

    //Deletes the parent relationship field if the child is deleted.
    relationships = FactoryLocator.getRelationshipFactory().byChild(type);
    for (final Relationship rel : relationships) {
      if(UtilMethods.isSet(rel.getChildRelationName()) && rel.isRelationshipField()) {
        final Field fieldToDelete = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(rel.getParentStructureInode(), rel.getChildRelationName());
        APILocator.getContentTypeFieldAPI().delete(fieldToDelete);
      }
      FactoryLocator.getRelationshipFactory().delete(rel);
    }

  }



  /**
   * parses legacy conditions passed in as raw sql
   * 
   * @author root
   *
   */
  class SearchCondition {
    final String search;
    final String condition;

    SearchCondition(final String searchOrCondition) {
      if (!UtilMethods.isSet(searchOrCondition) || searchOrCondition.equals("%")) {
        this.condition = "";
        this.search = "%";
      } else if (searchOrCondition.contains("<") || searchOrCondition.contains("=") || searchOrCondition.contains("<")
          || searchOrCondition.contains(" like ") || searchOrCondition.contains(" is ")) {
        this.search = "%";
        this.condition =
            (searchOrCondition.toLowerCase().trim().startsWith("and")) ? searchOrCondition : "and " + searchOrCondition;

      } else {
        this.condition = "";
        this.search = "%" + searchOrCondition + "%";

      }
    }

    @Override
    public String toString() {
      return "SearchCondition [search=" + search + ", condition=" + condition + "]";
    }
  }


  class CleanURLMap {
    final String urlMap;

    public CleanURLMap(String url) {
      this.urlMap = url;
    }

    @Override
    public String toString() {
      String ret = null;
      if (UtilMethods.isSet(urlMap)) {
        ret = this.urlMap.trim();
        if (!ret.startsWith("/")) {
          ret = "/" + ret;
        }
      }
      return ret;
    }
  }


  @Override
  public void validateFields(ContentType type) {
    List<Field> testFields = type.fields();
    if (!"forms".equals(type.variable())) {
      for (Field test : type.requiredFields()) {
        Optional<Field> optional =
            testFields.stream().filter(x -> test.variable().equalsIgnoreCase(x.variable())).findFirst();
        if (!optional.isPresent()) {
          if (test instanceof HostFolderField) {
            optional = testFields.stream().filter(x -> x instanceof HostFolderField).findFirst();
          }
        }

          if (!optional.isPresent()) {
              if (Config.getBooleanProperty("THROW_REQUIRED_FIELD_EXCEPTION", false)){
                  throw new DotValidationException("ContentType does not have the required Fields: " + test);
              } else {
                  Logger.warn(this, "ContentType: " + type.name() +" does not have the required Fields: " + test);
              }

          }

      }
    }
    
  }

 @Override
 public void updateModDate(ContentType type) throws DotDataException {
	 ContentTypeBuilder builder =
		        ContentTypeBuilder.builder(type).modDate(DateUtils.round(new Date(), Calendar.SECOND));
	 type = builder.build();
	 dbUpdateModDate(type);
	 cache.remove(type);
 }
 
 private void dbUpdateModDate(ContentType type) throws DotDataException{
	 DotConnect dc = new DotConnect();
	 dc.setSQL(this.contentTypeSql.UPDATE_TYPE_MOD_DATE_BY_INODE);
	 dc.addParam(type.modDate());
	 dc.addParam(type.id());
	 dc.loadResult();
 }

}
