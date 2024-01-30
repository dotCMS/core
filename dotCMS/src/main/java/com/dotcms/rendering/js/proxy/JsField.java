package com.dotcms.rendering.js.proxy;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.stream.Collectors;


/**
 * This class is used to expose the Field object to the javascript engine.
 *
 * @author jsanca
 */
public class JsField implements Serializable, JsProxyObject<Field> {

    private final Field field;

    public JsField(final Field field) {
        this.field = field;
    }
    @Override
    public Field getWrappedObject() {
        return field;
    }

    @HostAccess.Export
    public boolean searchable() {
        return field.searchable();
    }

    @HostAccess.Export
    public boolean unique() {
        return field.unique();
    }

    @HostAccess.Export
    public boolean indexed() {
        return field.indexed();
    }

    @HostAccess.Export
    public boolean listed() {
        return field.listed();
    }

    @HostAccess.Export
    public boolean readOnly() {
        return field.readOnly();
    }

    @HostAccess.Export
    public  String owner() {
        return field.owner();
    }
    @HostAccess.Export
    public  String id() {
        return field.id();
    }

    @HostAccess.Export
    public String inode() {
        return field.inode();
    }

    @HostAccess.Export
    public Object modDate() {
        return JsProxyFactory.createProxy(field.modDate());
    }

    @HostAccess.Export
    public String name() {
        return field.name();
    }

    @HostAccess.Export
    public String typeName() {
        return field.typeName();
    }

    @HostAccess.Export
    public String relationType() {
        return field.relationType();
    }

    @HostAccess.Export
    public boolean required() {
        return field.required();
    }

    @HostAccess.Export
    public  String variable() {
        return field.variable();
    }

    @HostAccess.Export
    public int sortOrder() {
        return field.sortOrder();
    }

    @HostAccess.Export
    public String values() {
        return field.values();
    }

    @HostAccess.Export
    public  String regexCheck() {
        return field.regexCheck();
    }

    @HostAccess.Export
    public String hint() {
        return field.hint();
    }

    @HostAccess.Export
    public  String defaultValue() {
        return field.defaultValue();
    }


    @HostAccess.Export
    public boolean fixed() {
        return field.fixed();
    }

    @HostAccess.Export
    public boolean legacyField() {
        return field.legacyField();
    }

    @HostAccess.Export
    public Object fieldVariables() {

        return JsProxyFactory.createProxy(field.fieldVariables());
    }

    @HostAccess.Export
    public Object fieldVariablesMap() {

        return JsProxyFactory.createProxy(field.fieldVariablesMap());
    }

    @HostAccess.Export
    public Object acceptedDataTypes() {
     
        return null != field.acceptedDataTypes()? 
                JsProxyFactory.createProxy(field.acceptedDataTypes().stream()
                        .map(DataTypes::toString).collect(Collectors.toList())):null;
    }

    @HostAccess.Export
    public  String dataType() {
        return field.dataType().value;
    }

    @HostAccess.Export
    public String contentTypeId() {
        return field.contentTypeId();
    }

    @HostAccess.Export
    public String dbColumn() {
        return field.dbColumn();
    }

    @HostAccess.Export
    public Object iDate() {
        return JsProxyFactory.createProxy(field.iDate());

    }

    @HostAccess.Export
    public String getContentTypeFieldHelpTextKey(){
        return field.getContentTypeFieldHelpTextKey();
    }

    @HostAccess.Export
    public String getContentTypeFieldLabelKey(){
        return field.getContentTypeFieldLabelKey();
    }

    @HostAccess.Export
    public Object fieldVariableKeys() {
        return JsProxyFactory.createProxy(field.fieldVariableKeys());
    }
}
