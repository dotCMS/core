package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.startup.StartupTask;

/**
 * @author nollymar
 */
public class Task05190UpdateFormsWidgetCodeField implements StartupTask {



  protected static final String SELECT_FORM_WIDGET_FIELD_INODE =
      "select field.inode as fieldId, structure.inode as contentTypeId "
      + "from field,structure "
      + "where "
      + "field.structure_inode=structure.inode and "
      + "structure.velocity_var_name='" + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + "' and "
      + "field.velocity_var_name = '" + FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME + "'";
  
  private static final String DELETE_FIELD="delete from field where inode=?";
  private static final String DELETE_INODE="delete from inode where inode=?";
  protected static final String REPLACEMENT_VELOCITY_CODE = "$velutil.mergeTemplate('/static/content/content_form.vtl')";

  protected final static String NEW_FIELD_ID="e5666638-e7f4-4b3a-b6d3-22f8d13188e8";
  
  @Override
  public boolean forceRun() {
    return true;
  }

  @Override
  public void executeUpgrade() throws DotDataException, DotRuntimeException {

      final DotConnect dotConnect = new DotConnect().setSQL(SELECT_FORM_WIDGET_FIELD_INODE);
      final String fieldId = dotConnect.getString("fieldId");
      final String contentTypeId = dotConnect.getString("contentTypeId");
      
      if(contentTypeId==null) {
        return;
      }
      
      
      // delete old field from "forms"
      dotConnect.setSQL(DELETE_FIELD);
      dotConnect.addParam(fieldId);
      dotConnect.loadResult();
      
      
      // delete old field from "inode"
      dotConnect.setSQL(DELETE_INODE);
      dotConnect.addParam(fieldId);
      dotConnect.loadResult();
      
      
      // delete new field from "forms" if it exists (idempotent)
      dotConnect.setSQL(DELETE_FIELD);
      dotConnect.addParam(NEW_FIELD_ID);
      dotConnect.loadResult();
      
      // delete new field from "forms" if it exists (idempotent)
      dotConnect.setSQL(DELETE_INODE);
      dotConnect.addParam(NEW_FIELD_ID);
      dotConnect.loadResult();
      
      
      
      // insert new field into forms
      Field newField = ImmutableConstantField.builder()
          .id(NEW_FIELD_ID)
          .name(WidgetContentType.WIDGET_CODE_FIELD_NAME)
          .variable(WidgetContentType.WIDGET_CODE_FIELD_VAR)
          .sortOrder(3)
          .dataType(DataTypes.SYSTEM)
          .dbColumn(DataTypes.SYSTEM.value)
          .fixed(true)
          .readOnly(false)
          .searchable(false)
          .contentTypeId(contentTypeId)
          .values(REPLACEMENT_VELOCITY_CODE)
          .build();


      // insert the new field
      insertInodeInDb(newField);
      insertFieldInDb(newField);
      CacheLocator.getContentTypeCache().clearCache();
      CacheLocator.getContentTypeCache2().clearCache();

  }
  private void insertInodeInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(FieldSql.getInstance().insertFieldInode);
    dc.addParam(field.id());
    dc.addParam(field.iDate());
    dc.addParam(field.owner());
    dc.loadResult();
  }
  
  private void insertFieldInDb(Field field) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL(FieldSql.getInstance().insertField);
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
}
