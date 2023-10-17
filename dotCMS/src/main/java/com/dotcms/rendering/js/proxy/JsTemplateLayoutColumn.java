package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link TemplateLayoutColumn} in a Js context.
 * @author jsanca
 */
public class JsTemplateLayoutColumn implements Serializable, JsProxyObject<TemplateLayoutColumn> {

    private final TemplateLayoutColumn column;

    public JsTemplateLayoutColumn(final TemplateLayoutColumn column) {
        this.column = column;
    }

    @Override
    public TemplateLayoutColumn getWrappedObject() {
        return column;
    }

    @HostAccess.Export
    public Integer getWidthPercent () {

        return column.getWidthPercent();
    }

    @HostAccess.Export
    public Integer getWidth () {

        return column.getWidth();
    }

    @HostAccess.Export
    public int getLeftOffset() {
        return column.getLeftOffset();
    }

    @HostAccess.Export
    // zero based left offset
    public int getLeft() {
        return column.getLeft();
    }

    @HostAccess.Export
    @Override
    public String toString() {
        return column.toString();
    }

    @HostAccess.Export
    public String getStyleClass() {
        return column.getStyleClass();
    }
}
