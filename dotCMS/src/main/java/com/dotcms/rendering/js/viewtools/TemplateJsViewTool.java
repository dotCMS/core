package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.DotTemplateTool} (tags) into the JS context.
 * @author jsanca
 */
public class TemplateJsViewTool implements JsViewTool, JsViewContextAware {

    private final DotTemplateTool dotTemplateTool = new DotTemplateTool();
    @Override
    public String getName() {
        return "templatetool";
    }

    @Override
    public void setViewContext(final ViewContext viewContext) {
        this.dotTemplateTool.init(viewContext);
    }

    @HostAccess.Export
    /**
     * Given a theme id we will parse it and return the Layout for the given template
     *
     * @param themeInode
     * @param isPreview
     * @return
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    public Object themeLayout (final String themeInode, final Boolean isPreview) throws DotDataException, DotSecurityException {

        return JsProxyFactory.createProxy(this.themeLayoutInternal(themeInode, isPreview));
    }

    protected TemplateLayout themeLayoutInternal (final String themeInode, final Boolean isPreview) throws DotDataException, DotSecurityException {
       return this.dotTemplateTool.themeLayout(themeInode, isPreview);
    }

    @HostAccess.Export
    /**
     * Method that will create a map of required data for the Layout template, basically paths
     * where the different elements of the theme can be found.
     *
     * @param themeFolderInode
     * @param hostId
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public  Object theme (final String themeFolderInode, final String siteid)
            throws DotDataException, DotSecurityException {

        return JsProxyFactory.createProxy(DotTemplateTool.theme(themeFolderInode, siteid));
    }
}
