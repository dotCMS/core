package com.dotcms.contenttype.util;

import java.io.Serializable;
import java.util.List;

import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.SerialWrapper;

public class ContentTypeImportExportHolder implements Serializable {

    private static final long serialVersionUID = 4301018990419103537L;
    List<SerialWrapper<ContentType>> contentTypes;
    List<SerialWrapper<ContentType>> fields;
    List<FieldVariable> fieldVariables;
    public List<SerialWrapper<ContentType>> getContentTypes() {
        return contentTypes;
    }
    public void setContentTypes(List<SerialWrapper<ContentType>> contentTypes) {
        this.contentTypes = contentTypes;
    }
    public List<SerialWrapper<ContentType>> getFields() {
        return fields;
    }
    public void setFields(List<SerialWrapper<ContentType>> fields) {
        this.fields = fields;
    }
    public List<FieldVariable> getFieldVariables() {
        return fieldVariables;
    }
    public void setFieldVariables(List<FieldVariable> fieldVariables) {
        this.fieldVariables = fieldVariables;
    }






}
