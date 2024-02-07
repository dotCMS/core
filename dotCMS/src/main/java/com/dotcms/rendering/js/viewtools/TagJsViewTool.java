package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.js.proxy.JsUser;
import com.dotcms.rendering.velocity.viewtools.TagsWebAPI;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.TagsWebAPI} (tags) into the JS context.
 * @author jsanca
 */
public class TagJsViewTool implements JsViewTool, JsViewContextAware {

    private final TagsWebAPI tagsWebAPI = new TagsWebAPI();
    @Override
    public void setViewContext(final ViewContext viewContext) {

        tagsWebAPI.init(viewContext);
    }

    @Override
    public String getName() {
        return "tags";
    }

    @HostAccess.Export
    public Object getTagsByUser(final JsUser user) throws DotDataException {

        final List tagsUser = this.getTagsByUserInternal(user.getWrappedObject());
        return null != tagsUser?
                JsProxyFactory.createProxy(tagsUser.stream().map(JsProxyFactory::createProxy).collect(Collectors.toList())): List.of();

    }

    protected List getTagsByUserInternal(final User user) throws DotDataException {

            return this.tagsWebAPI.getTagsByUser(user);
    }

    @HostAccess.Export
    public Object getTagsByNonLoggedUser() {

        final List tagsUser = this.getTagsByNonLoggedUserInternal();
        return null != tagsUser?
                JsProxyFactory.createProxy(tagsUser.stream().map(JsProxyFactory::createProxy).collect(Collectors.toList())): List.of();

    }

    protected List getTagsByNonLoggedUserInternal() {
        return this.tagsWebAPI.getTagsByNonLoggedUser();
    }

    @HostAccess.Export
    /**
     * Method that accrues a given String of tag names with a CSV format to the current {@link Visitor}
     *
     * @param tags String of tag names with a CSV format to accrue
     */
    public void accrueTags(final String tags) {
        this.tagsWebAPI.accrueTags(tags);
    }
}
