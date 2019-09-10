package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.sql.FieldSql;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.startup.StartupTask;

/**
 * @author nollymar
 */
public class Task05190UpdateFormsWidgetCodeField implements StartupTask {


  private static final String SELECT_FORM_INODE =
      "select inode from structure where velocity_var_name='" + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + "'";


  private static final String DELETE_OLD_FIELD="delete from field where structure_inode=? and velocity_var_name='" + FormAPI.FORM_WIDGET_CODE_VELOCITY_VAR_NAME + "'";
  private static final String REPLACEMENT_VELOCITY_CODE = "$velutil.mergeTemplate('/static/content/content_form.vtl')";

  @Override
  public boolean forceRun() {
    return true;
  }

  @Override
  public void executeUpgrade() throws DotDataException, DotRuntimeException {

      final DotConnect dotConnect = new DotConnect();

      // find widget type "forms"
      final String inode = dotConnect.setSQL(SELECT_FORM_INODE).getString("inode");
      
      // delete old field from "forms"
      dotConnect.setSQL(DELETE_OLD_FIELD);
      dotConnect.addParam(inode);
      dotConnect.loadResult();

      // insert new field into forms
      Field newField = ImmutableConstantField.builder()
          .id("e5666638-e7f4-4b3a-b6d3-22f8d13188e8")
          .name(WidgetContentType.WIDGET_CODE_FIELD_NAME)
          .variable(WidgetContentType.WIDGET_CODE_FIELD_VAR)
          .sortOrder(3)
          .fixed(true)
          .readOnly(true)
          .searchable(true)
          .contentTypeId(inode)
          .values(REPLACEMENT_VELOCITY_CODE)
          .build();


      insertInodeInDb(newField);
      insertFieldInDb(newField);

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
