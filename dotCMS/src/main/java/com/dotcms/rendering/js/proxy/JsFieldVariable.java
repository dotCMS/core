package com.dotcms.rendering.js.proxy;


import com.dotcms.contenttype.model.field.FieldVariable;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * This class is used to expose the {@link com.dotcms.contenttype.model.field.FieldVariable} object to the javascript engine.
 *
 * @author jsanca
 */
public class JsFieldVariable implements Serializable, JsProxyObject<FieldVariable> {

    private final FieldVariable fieldVariable;

    public JsFieldVariable(final FieldVariable fieldVariable) {
        this.fieldVariable = fieldVariable;
    }

    @Override
    public FieldVariable getWrappedObject() {
        return fieldVariable;
    }

    @HostAccess.Export
    public String id() {
        return fieldVariable.id();
    }

    @HostAccess.Export
    public String fieldId() {
        return fieldVariable.fieldId();
    }

    @HostAccess.Export
    public String name() {
        return fieldVariable.name();
    }

    @HostAccess.Export
    public String key() {
        return fieldVariable.key();
    }

    @HostAccess.Export
    public String value() {
        return fieldVariable.value();
    }

    @HostAccess.Export
    public String userId() {
        return fieldVariable.userId();
    }

    @HostAccess.Export
    public Object modDate() {
        return JsProxyFactory.createProxy(this.fieldVariable.modDate());
    }
}
