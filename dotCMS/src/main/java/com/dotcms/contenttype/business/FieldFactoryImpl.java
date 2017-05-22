package com.dotcms.contenttype.business;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.exception.DotDataValidationException;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.exception.OverFieldLimitException;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable.Builder;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.OnePerContentType;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.DbFieldTransformer;
import com.dotcms.contenttype.transform.field.DbFieldVariableTransformer;
import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;

public class FieldFactoryImpl implements FieldFactory {

  final FieldSql sql;

  public FieldFactoryImpl() {
    sql = FieldSql.getInstance();
  }

  @Override
  public Field byId(String id) throws DotDataException {

    return selectInDb(id);
  }

  @Override
  public Field byContentTypeFieldVar(ContentType type, String var) throws DotDataException {
    
    return type.fieldMap().get(var);

  }

  @Override
  public Field byContentTypeIdFieldVar(String id, String var) throws DotDataException {
    return selectByContentTypeFieldVarInDb(id, var);
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
  public void delete(Field field) throws DotDataException {
    LocalTransaction.wrapReturn(() -> {
      return deleteFieldInDb(field);
    });
  }

  @Override
  public List<FieldVariable> loadVariables(Field field) throws DotDataException {
    // System.err.println("loading field:" + field.variable() + ":" +
    // System.identityHashCode(field));
    List<FieldVariable> l = selectFieldVarsInDb(field);
    return l;
  }

  @Override
  public FieldVariable loadVariable(String id) throws DotDataException {
    return selectFieldVarInDb(id);
  }



  @Override
  public FieldVariable save(FieldVariable fieldVar) throws DotDataException {
    FieldVariable newVar = LocalTransaction.wrapReturn(() -> {
      return upsertFieldVariable(fieldVar);
    });

    Field f = byId(fieldVar.fieldId());
    ContentType t;
    try {
      t = APILocator.getContentTypeAPI(APILocator.systemUser()).find(f.contentTypeId());
      if (t != null) {
        CacheLocator.getContentTypeCache2().remove(t);
      }
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }

    return newVar;

  }

  @Override
  public void delete(FieldVariable fieldVar) throws DotDataException {
    LocalTransaction.wrapReturn(() -> {
      deleteFieldVarInDb(fieldVar);
      return null;
    });

    Field f = byId(fieldVar.fieldId());
    ContentType t;
    try {
      t = APILocator.getContentTypeAPI(APILocator.systemUser()).find(f.contentTypeId());
      if (t != null) {
        CacheLocator.getContentTypeCache2().remove(t);
      }
    } catch (DotSecurityException e) {
      throw new DotStateException(e);
    }
  }

  @Override
  public Field save(final Field throwAwayField) throws DotDataException {
    Field f = LocalTransaction.wrapReturn(() -> {
      return dbSaveUpdate(throwAwayField);
    });
    ContentType t = CacheLocator.getContentTypeCache2().byVarOrInode(f.contentTypeId());
    if (t != null)
      CacheLocator.getContentTypeCache2().remove(t);
    return f;
  }


  private Field dbSaveUpdate(final Field throwAwayField) throws DotDataException {


    // we only validate new fields
    if (throwAwayField.modDate().after(FieldAPI.VALIDATE_AFTER)) {
      if (!throwAwayField.acceptedDataTypes().contains(throwAwayField.dataType())
          && (throwAwayField.acceptedDataTypes().size() > 0
              && throwAwayField.acceptedDataTypes().get(0) != DataTypes.SYSTEM)) {
        throw new DotDataValidationException("Field Type:" + throwAwayField.type() + " does not accept datatype "
            + throwAwayField.dataType() + ":" + throwAwayField, "field.validation.incorrect.datatype");
      }
    }
    if (throwAwayField.contentTypeId() == null) {
      throw new DotDataValidationException(
          "Field Type:" + throwAwayField.type() + " does not have a contenttype.inode set",
          "field.validation.contenttype.not.set");
    }

    List<Field> fieldsAlreadyAdded = byContentTypeId(throwAwayField.contentTypeId());

    Date modDate = DateUtils.round(new Date(), Calendar.SECOND);
    FieldBuilder builder = FieldBuilder.builder(throwAwayField).modDate(modDate);

    if (throwAwayField.acceptedDataTypes().size() == 1
        && throwAwayField.acceptedDataTypes().get(0) == DataTypes.SYSTEM) {
      builder.dataType(DataTypes.SYSTEM);
    }


    Field oldField = null;
    try {
      oldField = selectInDb(throwAwayField.id());
    } catch (NotFoundInDbException e) {

    }



    if (oldField == null) {

      // assign an inode and db column if needed
      if (throwAwayField.id() == null) {
        builder.id(UUID.randomUUID().toString());
        builder.dbColumn(nextAvailableColumn(throwAwayField));
      }

      if (throwAwayField.sortOrder() < 0) {
        // move to the end of the line
    	builder.sortOrder(
    		fieldsAlreadyAdded.stream().map(f -> f.sortOrder()).max(Integer::compare).orElse(-1) + 1
    	);
      }

      // normalize our velocityvar
      String tryVar = (throwAwayField.variable() == null)
          ? suggestVelocityVar(throwAwayField.name(), fieldsAlreadyAdded) : throwAwayField.variable();
      builder.variable(tryVar);

      builder.fixed(false);
      builder.readOnly(false);
    }



    for (Field f : fieldsAlreadyAdded) {
      if (f instanceof CategoryField) {
        if (f.values() != null) {
          if (f.values().equals(throwAwayField.values())) {
            if (!f.id().equals(throwAwayField.id())) {
              throw new DotDataValidationException("This category field already exists on this content type",
                  "message.category.existing.field");
            }
          }
        }
      }
      if (throwAwayField instanceof OnePerContentType) {
        if (f.id().equals(throwAwayField.id())) {
          continue;
        }
        if (f.type().equals(throwAwayField.type())) {
          throw new DotDataValidationException("A content type cannot have two:" + throwAwayField.type() + " fields",
              "contenttype.validation.cannot.have.two.of.fieldtype");
        }
      }
    }

    // make sure we are properly indexed
    if ((throwAwayField.searchable() || throwAwayField.listed()) || throwAwayField.unique()
        || throwAwayField instanceof HostFolderField || throwAwayField instanceof TagField) {
      builder.indexed(true);
    }
    if (throwAwayField.unique()) {
      builder.required(true);
    }



    Field retField = builder.build();


    
    
    
    if (oldField == null) {
      insertInodeInDb(retField);
      insertFieldInDb(retField);
    } else {
      updateInodeInDb(retField);
      updateFieldInDb(retField);
    }



    return retField;
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
    String key = StringUtils.camelCaseLower(throwAway.key());
    String value = throwAway.value().trim();


    Builder builder =
        ImmutableFieldVariable.builder().from(throwAway).modDate(DateUtils.round(new Date(), Calendar.SECOND));


    builder.key(key);
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
    Set<String> columns = new TreeSet<String>();
    for (int i = 0; i < rows.size(); i++) {
      columns.add((String) rows.get(i).get("field_contentlet"));
    }

    for (int i = 0; i < Config.getIntProperty("db.number.of.contentlet.columns.per.datatype", 25); i++) {
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
      deleteFieldInDb(field);
    }
  }


  @Override
  public String suggestVelocityVar(final String tryVar, List<Field> takenFields) throws DotDataException {


    String var = StringUtils.camelCaseLower(tryVar);
    for (Field f : takenFields) {
      if (var.equalsIgnoreCase(f.variable())) {
        var = null;
        break;
      }
    }
    if (var != null)
      return var;

    for (int i = 1; i < 100000; i++) {
      var = StringUtils.camelCaseLower(tryVar) + i;
      for (Field f : takenFields) {
        if (var.equalsIgnoreCase(f.variable())) {
          var = null;
          break;
        }
      }
      if (var != null)
        return var;
    }
    throw new DotDataValidationException("Unable to suggest a variable name.  Got to:" + var,
        "field.validation.variable.already.taken");

  }
}
