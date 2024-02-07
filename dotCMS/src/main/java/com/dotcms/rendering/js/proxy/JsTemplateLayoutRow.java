package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyArray;

import java.io.Serializable;
import java.util.stream.Collectors;

/**
 * Encapsulates a {@link TemplateLayoutRow} in a Js context.
 * @author jsanca
 */
public class JsTemplateLayoutRow implements Serializable, JsProxyObject<TemplateLayoutRow> {

    private final TemplateLayoutRow row;

    public JsTemplateLayoutRow(final TemplateLayoutRow row) {
        this.row = row;
    }

    @Override
    public TemplateLayoutRow getWrappedObject() {
        return row;
    }

    @HostAccess.Export
    public JsTemplateLayoutColumn getColumn(final String column) {
        return new JsTemplateLayoutColumn(this.row.getColumn(column));
    }

    @HostAccess.Export
    public JsTemplateLayoutColumn getColumn(final int column) {

        return new JsTemplateLayoutColumn(this.row.getColumn(column));
    }

    @HostAccess.Export
    public int getIdentifier () {
        return this.row.getIdentifier();
    }

    @HostAccess.Export
    public String getValue () {
        return this.row.getValue();
    }

    @HostAccess.Export
    public String getId () {
        return this.row.getId();
    }

    @HostAccess.Export
    public Object getColumns () {
        return null != this.row.getColumns()?
                JsProxyFactory.createProxy(this.row.getColumns().stream().map(JsTemplateLayoutColumn::new).collect(Collectors.toList())):
                ProxyArray.fromArray();
    }

    @HostAccess.Export
    public String getStyleClass() {
        return this.row.getStyleClass();
    }


    @HostAccess.Export
    @Override
    public String toString() {
       return this.row.toString();
    }
}
