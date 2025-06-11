package com.dotcms.contenttype.business;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.exception.DotDataValidationException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable.Builder;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.DbFieldTransformer;
import com.dotcms.contenttype.transform.field.DbFieldVariableTransformer;
import com.dotcms.graphql.business.ContentAPIGraphQLTypesProvider;
import com.dotcms.rendering.velocity.services.FieldLoader;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.lang.time.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dotcms.util.DotPreconditions.checkNotNull;

/**
 * This class provides a SQL-based implementation for the {@link FieldFactory} interface.
 *
 * @author Will Ezell
 * @since Jun 29th, 2016
 */
public class FieldFactoryImpl implements FieldFactory {

  //List of reserved field variables
    public static final Set<String> RESERVED_FIELD_VARS = ImmutableSet.of(
          Contentlet.LANGUAGEID_KEY.toLowerCase(),
          Contentlet.LOCKED_KEY.toLowerCase(),
          Contentlet.LIVE_KEY.toLowerCase(),
          Contentlet.MOD_DATE_KEY.toLowerCase(),
          Contentlet.IDENTIFIER_KEY.toLowerCase(),
          Contentlet.HOST_KEY.toLowerCase(),
          Contentlet.CLASS_NAME_KEY.toLowerCase(),
          Contentlet.CON_FOLDER_KEY.toLowerCase(),
          Contentlet.CON_HOST_KEY.toLowerCase(),
          Contentlet.DELETED_KEY.toLowerCase(),
          Contentlet.FILE_KEY.toLowerCase(),
          Contentlet.FORM_KEY.toLowerCase(),
          Contentlet.INODE_KEY.toLowerCase(),
          Contentlet.MOD_USER_KEY.toLowerCase(),
          Contentlet.OWNER_KEY.toLowerCase(),
          Contentlet.NUMBER_KEY.toLowerCase(),
          Contentlet.STRING_KEY.toLowerCase(),
          Contentlet.OWNER_CAN_PUBLISH_KEY.toLowerCase(),
          Contentlet.OWNER_CAN_WRITE_KEY.toLowerCase(),
          Contentlet.OWNER_CAN_READ_KEY.toLowerCase(),
          Contentlet.PERMISSIONS_KEY.toLowerCase(),
          Contentlet.TYPE_KEY.toLowerCase(),
          Contentlet.WEBSITE_KEY.toLowerCase(),
          Contentlet.WORKING_KEY.toLowerCase(),
          Contentlet.STRUCTURE_INODE_KEY.toLowerCase(),
          Contentlet.DISABLED_WYSIWYG_KEY.toLowerCase(),
          Contentlet.ARCHIVED_KEY.toLowerCase(),
          Contentlet.BASE_TYPE_KEY.toLowerCase(),
          Contentlet.CONTENT_TYPE_KEY.toLowerCase(),
          Contentlet.MOD_USER_NAME_KEY.toLowerCase(),
          Contentlet.OWNER_USER_NAME_KEY.toLowerCase(),
          Contentlet.CREATION_DATE_KEY.toLowerCase(),
          Contentlet.PUBLISH_USER_KEY.toLowerCase(),
          Contentlet.PUBLISH_USER_NAME_KEY.toLowerCase()
  );

  final FieldSql sql;

  public FieldFactoryImpl() {
    sql = FieldSql.getInstance();
  }

  @Override
  public Field byId(String id) throws DotDataException {

    return selectInDb(id);
  }

  @Override
  public Field byContentTypeFieldVar(final ContentType type, final String velocityVarName) throws DotDataException {
    checkNotNull(type, "Content Type cannot be null");
    final Field field = type.fieldMap().get(velocityVarName);

    if(field==null) {
      throw new NotFoundInDbException(String.format("Field Variable '%s' in Content Type " +
              "'%s' [ %s ] was not found", velocityVarName, type.name(), type.id()));
    }

    return field;

  }

  @Override
  public Field byContentTypeIdFieldVar(String id, String var) throws DotDataException {
    return selectByContentTypeFieldVarInDb(id, var);
  }

  @Override
  public Optional<Field> byContentTypeIdFieldRelationTypeInDb(String id, String var) throws DotDataException {
    return selectByContentTypeFieldRelationTypeInDb(id, var);
  }

  @Override
  public List<Field> byContentType(ContentType type) throws DotDataException {
    return type.fields();
  }

  @Override
  public List<Field> byContentTypeId(String id) throws DotDataException {
    return selectByContentTypeInDb(id);
  }

  @Override
  public List<Field> byContentTypeVar(String var) throws DotDataException {
    return selectByContentTypeVarInDb(var);
  }

  @Override
  public Optional<FieldVariable> byFieldVariableKey(final String fieldId, final String key) throws DotDataException {

    final List<Map<String, Object>> results =
            new DotConnect().setSQL(sql.selectFieldIdVarByKey)
              .addParam(fieldId).addParam(key).loadObjectResults();

    return results.isEmpty()?Optional.empty():Optional.ofNullable(new DbFieldVariableTransformer(results).from());
  }

  @Override
  public List<FieldVariable> byFieldVariableKey(final String key) throws DotDataException {
        final DotConnect dc = new DotConnect();
        dc.setSQL(sql.selectFieldVarByKey);
        dc.addParam(key);

        final List<Map<String, Object>> results = dc.loadObjectResults();

        return UtilMethods.isSet(results) ? new DbFieldVariableTransformer(dc.loadObjectResults())
                .asList() : Collections.emptyList();
  }

  @Override
  public void delete(Field field) throws DotDataException {
      deleteFieldInDb(field);
  }

  @Override
  public List<FieldVariable> loadVariables(Field field) throws DotDataException {
      return selectFieldVarsInDb(field);
  }

  @Override
  public FieldVariable loadVariable(String id) throws DotDataException {
    return selectFieldVarInDb(id);
  }



  @Override
  public FieldVariable save(FieldVariable fieldVar) throws DotDataException {

      if(!UtilMethods.isSet(fieldVar.key())){
        throw new DotDataException("FieldVariable.key cannot be empty");
      }
      
      if(!UtilMethods.isSet(fieldVar.value())){
        throw new DotDataException("FieldVariable.value cannot be empty");
      }

      FieldVariable fv =  upsertFieldVariable(fieldVar);
      Field f = byId(fieldVar.fieldId());
      APILocator.getContentTypeAPI(APILocator.systemUser()).updateModDate(f);
      return fv;
  }

  @Override
  public void delete(FieldVariable fieldVar) throws DotDataException {
      deleteFieldVarInDb(fieldVar);
      Field f = byId(fieldVar.fieldId());
      APILocator.getContentTypeAPI(APILocator.systemUser()).updateModDate(f);
  }

    @Override
    public Field save(final Field fieldToSave) throws DotDataException {
        final Field field = dbSaveUpdate(fieldToSave);
        invalidateField(field);
        return field;
    }

    @Override
    public Field save(final Field fieldToSave, final Field existingField) throws DotDataException {
        final Field field = dbSaveUpdate(fieldToSave, existingField);
        invalidateField(field);
        return field;
    }

    /**
     * Updates the Content Type's mod date and invalidates the field cache
     *
     * @param field the field to invalidate
     * @throws DotDataException if there is an error updating the Content Type's mod date
     */
    private void invalidateField(final Field field) throws DotDataException {
        APILocator.getContentTypeAPI(APILocator.systemUser()).updateModDate(field);
        new FieldLoader().invalidate(field);
    }

  private Field normalizeData(final Field throwAwayField) throws DotDataException {
    FieldBuilder builder = FieldBuilder.builder(throwAwayField);
    Field returnField = throwAwayField;
    
    if("constant".equals(returnField.dbColumn()) && !(returnField instanceof ConstantField)) {
      builder = ImmutableConstantField.builder().from(returnField);
      builder.dbColumn(DataTypes.SYSTEM.value);
      returnField = builder.build();
    }

    if(returnField.acceptedDataTypes().size()==1  && returnField.acceptedDataTypes().get(0) == DataTypes.SYSTEM) {
      builder.dataType( DataTypes.SYSTEM);
      returnField = builder.build();
    }
    // make sure we are setting the db to system
    if(returnField.dataType() == DataTypes.SYSTEM){
      builder.dbColumn(DataTypes.SYSTEM.value);
      returnField = builder.build();
    }
    
    
    // if this is a new column validate and assign a db column
    try{
      validateDbColumn(returnField);
    }
    catch(Throwable e){
      Logger.debug(this.getClass(), "Field db column being updated: " + e.getMessage());
      builder.dbColumn(nextAvailableColumn(returnField));
      returnField = builder.build();
    }
    // make sure we are properly indexed
    if ((returnField.searchable() || returnField.listed()) || returnField.unique()
        || returnField instanceof HostFolderField || returnField instanceof TagField) {
      
      builder.indexed(true);
      returnField = builder.build();
    }
    if (returnField.unique()) {
      builder.required(true);
      returnField = builder.build();
    }
    
    return returnField;
  }

    private Field dbSaveUpdate(final Field throwAwayField) throws DotDataException {

        // Search for the field in the database to see if it already exists
        var existingFieldOptional = resolveExistingField(throwAwayField);

        return dbSaveUpdate(throwAwayField, existingFieldOptional.orElse(null));
    }

    private Field dbSaveUpdate(final Field throwAwayField, final Field existingField)
            throws DotDataException {

        FieldBuilder builder = FieldBuilder.builder(throwAwayField);

        Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
        builder.modDate(modDate);

        if (existingField != null) {

            if (!existingField.variable().equalsIgnoreCase(throwAwayField.variable())) {
                Logger.warn(this,
                        String.format(
                                "Field variable can not be modified, ignoring [%s] and using [%s] "
                                        + "instead for field [%s]",
                                throwAwayField.variable(),
                                existingField.variable(),
                                existingField.id()
                        )
                );
            }

            builder.id(existingField.id());
            builder.variable(existingField.variable());
            builder.fixed(existingField.fixed());
            builder.readOnly(existingField.readOnly());
            builder.dataType(existingField.dataType());
            builder.dbColumn(existingField.dbColumn());
        } else {

            List<Field> fieldsAlreadyAdded = byContentTypeId(throwAwayField.contentTypeId());

            if (throwAwayField.sortOrder() < 0) {
                // move to the end of the line
                builder.sortOrder(
                        fieldsAlreadyAdded.stream().map(Field::sortOrder).max(Integer::compare)
                                .orElse(-1) + 1
                );
            }

            // normalize our velocityvar
            final List<String> takenFieldVars = fieldsAlreadyAdded.stream().map(Field::variable)
                    .collect(
                            Collectors.toList());

            String tryVar = getFieldVariable(throwAwayField, takenFieldVars);

            builder.variable(tryVar);

            // assign an inode and db column if needed
            if (throwAwayField.id() == null) {
                builder.id(APILocator.getDeterministicIdentifierAPI()
                        .generateDeterministicIdBestEffort(throwAwayField, () -> tryVar));
            }
        }

        builder = FieldBuilder.builder(normalizeData(builder.build()));

        Field retField = builder.build();

        validateDbColumn(retField);

        if (existingField == null) {
            insertInodeInDb(retField);
            insertFieldInDb(retField);
        } else {
            updateInodeInDb(retField);
            updateFieldInDb(retField);
        }

        return retField;
    }

    public Optional<Field> resolveExistingField(final Field field) throws DotDataException {

        Field oldField = null;

        // The following block of code is used to find the field in the database, if it exists, as
        // the id should not be really required, we should be able to identify the field also by
        // variable.
        if (org.apache.commons.lang.StringUtils.isNotEmpty(field.id())) {

            try {
                oldField = selectInDb(field.id());
            } catch (NotFoundInDbException e) {

                if (org.apache.commons.lang.StringUtils.isNotEmpty(field.variable())) {

                    Logger.debug(this.getClass(), String.format(
                            "Failed to find field by ID [%s]. Falling back to lookup by variable [%s].",
                            field.id(), field.variable()
                    ));

                    // We provided a field id, but it was not found in the database, now let's try
                    // to find it by content type and variable
                    try {
                        oldField = selectByContentTypeFieldVarInDb(
                                field.contentTypeId(), field.variable()
                        );
                    } catch (NotFoundInDbException byVarException) {
                        Logger.debug(this.getClass(),
                                String.format(
                                        "Failed to find field by content type [%s] and variable [%s]",
                                        field.contentTypeId(), field.variable()));
                    }
                } else {
                    Logger.warn(this.getClass(), String.format(
                            "Failed to find field by ID [%s].", field.id()
                    ));
                }
            }
        } else if (org.apache.commons.lang.StringUtils.isNotEmpty(field.variable())) {
            try {
                oldField = selectByContentTypeFieldVarInDb(
                        field.contentTypeId(), field.variable()
                );
            } catch (NotFoundInDbException e) {
                Logger.debug(this.getClass(),
                        String.format("Failed to find field by content type [%s] and variable [%s]",
                                field.contentTypeId(), field.variable()));
            }
        }

        return Optional.ofNullable(oldField);
    }

  /**
   * Returns the field variable to use in the new field being saved
   *
   * If the field variable comes null, a new one will be generated based on the name
   *
   * If the field variable comes already set, via {@link Field#variable()}, it needs to be GraphQL-compatible
   * otherwise a {@link DotDataException} is thrown.
   *
   * @param throwAwayField the field whose variable will be returned
   * @param takenFieldVars a list of taken field vars
   * @return the field variable for the field to be saved
   * @throws DotDataException in case that the field variable is not valid
   */
  private String getFieldVariable(Field throwAwayField, List<String> takenFieldVars)
          throws DotDataException {

    String variable;

    if(throwAwayField.variable() == null) {
      variable = suggestVelocityVar(throwAwayField.name(), throwAwayField, takenFieldVars);
    } else {
       variable = throwAwayField.variable();
    }
    return variable;
  }

  private void validateDbColumn(Field field) throws DotDataException {
    

    if (field.contentTypeId() == null) {
      throw new DotDataValidationException(
          "Field Type:" + field.type() + " does not have a contenttype.inode set",
          "field.validation.contenttype.not.set");
    }
    
    List<Field> fieldsAlreadyAdded = byContentTypeId(field.contentTypeId());
    for (Field f : fieldsAlreadyAdded) {
      if (f instanceof CategoryField) {
        if (f.values() != null) {
          if (f.values().equals(field.values())) {
            if (!f.id().equals(field.id())) {
              final String exceptionString = String.format("[f1 %s , f2 %s]", field.name(), f.name() );
              throw new DotDataValidationException("This category field already exists on this content type " + exceptionString ,
                  "message.category.existing.field");
            }
          }
        }
      }
      if (field instanceof OnePerContentType) {
        if (f.id().equals(field.id())) {
          continue;
        }
        if (f.type().equals(field.type())) {
          throw new DotDataValidationException("A content type cannot have two:" + field.type() + " fields",
              "contenttype.validation.cannot.have.two.of.fieldtype");
        }
      }
    }
    
    
    if (!field.acceptedDataTypes().contains(field.dataType())){
      throw new DotDataValidationException("Field Type:" + field.type() + " does not accept datatype "
                + field.dataType() + ":" + field.variable(), "field.validation.incorrect.datatype");
    
    }
    if(field.dbColumn()==null){
      throw new DotDataValidationException("Unable to save field with a null dbColumn field.field_contentlet:" + field, "message.field.dbcolumn.incorrect");
    }
    
    if( !field.dbColumn().matches("(system_field|(text|float|bool|date|text_area|integer)[0-9]+)")){
      throw new DotDataValidationException("Unable to save field with DB Column " + field.dbColumn()+ " - must match (system_field|(text|float|bool|date|text_area|integer)[0-9]+) "  + field.name() + " " + field.variable(), "message.field.dbcolumn.incorrect");
    }
  }
  
  @Override
  public List<Field> selectByContentTypeInDb(String id) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.findByContentType);
    dc.addParam(id);
    List<Map<String, Object>> results;
    results = dc.loadObjectResults();
    return new DbFieldTransformer(results).asList();

  }

  private Field selectByContentTypeFieldVarInDb(String id, String var) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.findByContentTypeAndFieldVar).addParam(id).addParam(var);


    List<Map<String, Object>> results;

    results = dc.loadObjectResults();
    if (results.size() == 0) {
      throw new NotFoundInDbException("Field with contentype:" + id + " and var:" + var + " not found");
    }
    return new DbFieldTransformer(results.get(0)).from();

  }

  private List<Field> selectByContentTypeVarInDb(String var) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.findByContentTypeVar);
    dc.addParam(var);
    List<Map<String, Object>> results;
    results = dc.loadObjectResults();
    return new DbFieldTransformer(results).asList();

  }

  private Optional<Field> selectByContentTypeFieldRelationTypeInDb(String id, String fieldRelationType) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.findByContentTypeAndRelationType).addParam(id).addParam(fieldRelationType);

    final List<Map<String, Object>> results = dc.loadObjectResults();
    return results.isEmpty()?
            Optional.empty():Optional.of(new DbFieldTransformer(results.get(0)).from());
  }

  private Field selectInDb(String id) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.findById);
    dc.addParam(id);
    List<Map<String, Object>> results;

    results = dc.loadObjectResults();
    if (results.size() == 0) {
      throw new NotFoundInDbException("Field with id:" + id + " not found");
    }
    return new DbFieldTransformer(results.get(0)).from();
  }

  private boolean deleteFieldInDb(Field field) throws DotDataException {
    deleteFieldVarsInDb(field);
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.deleteById);
    dc.addParam(field.id());
    dc.loadResult();
    dc.setSQL(sql.deleteInodeById);
    dc.addParam(field.id());
    dc.loadResult();
    return true;
  }

  private void updateInodeInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.updateFieldInode);
    dc.addParam(field.id());
    dc.addParam(field.iDate());
    dc.addParam(field.owner());
    dc.addParam(field.id());
    dc.loadResult();
  }

  private void insertInodeInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.insertFieldInode);
    dc.addParam(field.id());
    dc.addParam(field.iDate());
    dc.addParam(field.owner());
    dc.loadResult();
  }

  private List<FieldVariable> selectFieldVarsInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.selectFieldVars);
    dc.addParam(field.id());
    return new DbFieldVariableTransformer(dc.loadObjectResults()).asList();
  }

  private FieldVariable selectFieldVarInDb(String id) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.selectFieldVar);
    dc.addParam(id);

    List<Map<String, Object>> results = dc.loadObjectResults();
    if (results.size() == 0) {
      throw new NotFoundInDbException("Field variable with id:" + id + " not found");
    }

    return new DbFieldVariableTransformer(results).from();
  }

  private FieldVariable upsertFieldVariable(final FieldVariable throwAway) throws DotDataException {
    String value = throwAway.value().trim();


    Builder builder =
        ImmutableFieldVariable.builder().from(throwAway).modDate(DateUtils.round(new Date(), Calendar.SECOND));


    builder.value(value);



    if (!UtilMethods.isSet(throwAway.id())) {
      builder.id(UUID.randomUUID().toString());
    }

    // at least this will order it
    if (!UtilMethods.isSet(throwAway.name())) {
      builder.name(String.valueOf(System.currentTimeMillis()));
    }

    FieldVariable var = builder.build();



    // delete first
    deleteFieldVarInDb(var);



    new DotConnect().setSQL(sql.insertFieldVar).addParam(var.id()).addParam(var.fieldId()).addParam(var.name())
        .addParam(var.key()).addParam(var.value()).addParam(var.userId()).addParam(var.modDate()).loadResult();
    return var;
  }

  private void deleteFieldVarInDb(FieldVariable var) throws DotDataException {

    new DotConnect().setSQL(sql.deleteFieldVar).addParam(var.id()).addParam(var.fieldId()).addParam(var.key())
        .loadResult();



  }

  private void deleteFieldVarsInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.deleteFieldVarsForField);
    dc.addParam(field.id());
    dc.loadResult();
  }

  private void updateFieldInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.updateField);
    dc.addParam(field.contentTypeId());
    dc.addParam(field.name());
    dc.addParam(field.type().getCanonicalName());
    dc.addParam(field.relationType());
    dc.addParam(field.dbColumn());
    dc.addParam(field.required());
    dc.addParam(field.indexed());
    dc.addParam(field.listed());
    dc.addParam(field.variable());
    dc.addParam(field.sortOrder());
    dc.addParam(field.values());
    dc.addParam(field.regexCheck());
    dc.addParam(field.hint());
    dc.addParam(field.defaultValue());
    dc.addParam(field.fixed());
    dc.addParam(field.readOnly());
    dc.addParam(field.searchable());
    dc.addParam(field.unique());
    dc.addParam(field.modDate());
    dc.addParam(field.id());
    dc.loadResult();

  }

  private void insertFieldInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.insertField);
    dc.addParam(field.id());
    dc.addParam(field.contentTypeId());
    dc.addParam(field.name());
    dc.addParam(field.type().getCanonicalName());
    dc.addParam(field.relationType());
    dc.addParam(field.dbColumn());
    dc.addParam(field.required());
    dc.addParam(field.indexed());
    dc.addParam(field.listed());
    dc.addParam(field.variable());
    dc.addParam(field.sortOrder());
    dc.addParam(field.values());
    dc.addParam(field.regexCheck());
    dc.addParam(field.hint());
    dc.addParam(field.defaultValue());
    dc.addParam(field.fixed());
    dc.addParam(field.readOnly());
    dc.addParam(field.searchable());
    dc.addParam(field.unique());
    dc.addParam(field.modDate());

    dc.loadResult();

  }


  @Override
  public String nextAvailableColumn(Field field) throws DotDataException {



    DotConnect dc = new DotConnect();

    if (field instanceof HostFolderField || field instanceof TagField) {
      dc.setSQL(this.sql.selectCountOfType);
      dc.addParam(field.contentTypeId());
      dc.addParam(LegacyFieldTypes.getLegacyName(field.type() + "%"));
      dc.addParam(LegacyFieldTypes.getImplClass(field.type() + "%"));
      int x = dc.getInt("test");
      if (x > 0) {
        throw new OverFieldLimitException("Only one " + field.type() + " per ContentType");
      }
    }

    if (field.dataType() == DataTypes.SYSTEM) {
      return field.dataType().toString();
    }


    String dataType = field.dataType().toString();
    dc.setSQL(this.sql.selectFieldOfDbType);
    dc.addParam(field.contentTypeId());
    dc.addParam(dataType + "%");
    List<Map<String, Object>> rows = dc.loadObjectResults();
    Set<String> columns = new TreeSet<>();
    for (int i = 0; i < rows.size(); i++) {
      columns.add((String) rows.get(i).get("field_contentlet"));
    }

    final int maxSupportedColumn =
            //Json Fields isn't limited to a fix number of columns
            APILocator.getContentletJsonAPI().isPersistContentAsJson() ? Integer.MAX_VALUE
                    : Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25);
    for (int i = 0; i < maxSupportedColumn; i++) {
      if (!columns.contains(dataType + (i + 1))) {
        return dataType + (i + 1);

      }
    }

    throw new OverFieldLimitException("No more columns for datatype:" + dataType);


  }

  @Override
  public void deleteByContentType(ContentType type) throws DotDataException {
    List<Field> fields = byContentType(type);
    for (Field field : fields) {
       //Relationship fields must be handled by ContentTypeFactory#deleteRelationships
       if(!(field instanceof RelationshipField)){
         deleteFieldInDb(field);
       }
    }
  }

    @Override
    public String suggestVelocityVar(String tryVar, final Field field, final List<String> takenFieldsVariables) throws DotDataException {
        final List<String> forbiddenFieldVariables = new ArrayList<>(takenFieldsVariables);
        String var = StringUtils.camelCaseLower(tryVar);
        if (!isFieldVariableValid(var, field)) {
          forbiddenFieldVariables.add(var);
        }
        // if we don't get a var back, we are looking at UTF-8 or worse
        // lets just make a field up
        if (!UtilMethods.isSet(var)) {
            tryVar= "field";
        }
        for (final String fieldVar : forbiddenFieldVariables) {
            if (var.equalsIgnoreCase(fieldVar)) {
                var= null;
                break;
            }
        }

        if (UtilMethods.isSet(var)) {
            return var;
        }

        for (int i = 1; i < 100000; i++) {
            var = StringUtils.camelCaseLower(tryVar) + i;
            for (final String fieldVar : forbiddenFieldVariables) {
                if (var.equalsIgnoreCase(fieldVar)) {
                    var = null;
                    break;
                }
            }

            if (UtilMethods.isSet(var)) {
                return var;
            }
        }
        throw new DotDataValidationException(String.format("Unable to suggest a variable name for '%s'", tryVar),
            "field.validation.variable.already.taken");
    }

    /**
     * Checks if the specified field variable name is valid or not, meaning that it must meet the
     * following conditions:
     * <ul>
     *     <li>It must be GraphQL-compatible.</li>
     *     <li>It must NOT be a reserved field variable name. Please refer to the Set of
     *     {@link #RESERVED_FIELD_VARS}.</li>
     * </ul>
     *
     * @param fieldVarName The field variable name to check.
     * @param field        The {@link Field} to check against.
     *
     * @return If the field variable is valid, returns {@code true}.
     */
    private boolean isFieldVariableValid(final String fieldVarName, final Field field) {
        return ContentAPIGraphQLTypesProvider.INSTANCE.isFieldVariableGraphQLCompatible(fieldVarName, field)
                && !RESERVED_FIELD_VARS.contains(fieldVarName.toLowerCase());
}

  public void moveSortOrderForward(String contentTypeId, int from, int to) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.moveSorOrderForward);
    dc.addParam(contentTypeId);
    dc.addParam(from);
    dc.addParam(to);
    dc.loadResult();
  }

  public void moveSortOrderBackward(String contentTypeId, int from, int to) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(sql.moveSorOrderBackward);
    dc.addParam(contentTypeId);
    dc.addParam(from);
    dc.addParam(to);
    dc.loadResult();
  }

  public void moveSortOrderForward(String contentTypeId, int from) throws DotDataException {
     moveSortOrderForward(contentTypeId, from, Integer.MAX_VALUE);
  }

  public void moveSortOrderBackward(String contentTypeId, int from) throws DotDataException {
    moveSortOrderBackward(contentTypeId, from, Integer.MAX_VALUE);
  }

}
