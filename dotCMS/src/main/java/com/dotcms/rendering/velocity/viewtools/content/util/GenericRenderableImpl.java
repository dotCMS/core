package com.dotcms.rendering.velocity.viewtools.content.util;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.viewtools.content.Renderable;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Generic render for items based on a type.
 * It is controlled by convention, basically the component has base default template, it concat the type as a vtl in order to find the resource
 * Also, users can pass their own base path, in case the item render velocity template does not exists will fallbacks to the default one.
 * @author jsanca
 */
class GenericRenderableImpl implements Renderable { 

    private final String defaultPath;
    private final String defaultTemplateName;
    private final Object item;
    private final String type;
    private final Context externalContext;

    public GenericRenderableImpl(final String defaultPath, final String templateName,
                                 final Object item, final String type, final Context context) {

        this.defaultPath     = defaultPath;
        this.defaultTemplateName = templateName;
        this.item            = item;
        this.type            = type;
        this.externalContext = context;
    }

    @Override
    public String toHtml() {

        try {

            final Host   host     = Try.of(()->APILocator.getHostAPI().findDefaultHost(
                    APILocator.systemUser(), false)).getOrNull();
            final String hostname = null == host? host.getHostname():"dotcms.com"; // fake host

            Context context = externalContext;
            if (externalContext == null) {

                final HttpServletRequest requestProxy = new FakeHttpRequest(hostname, null).request();
                final HttpServletResponse responseProxy = new BaseResponse().response();
                context = VelocityUtil.getInstance().getContext(requestProxy, responseProxy);
            }

            context.put("item", item);

            return VelocityUtil.getInstance().mergeTemplate(this.existsFile(this.defaultPath, host, new User())?
                    this.defaultPath + type + ".vtl": this.defaultPath + defaultTemplateName, context);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private boolean existsFile (final String path, final Host host, final User user)  {

        try {

            final Folder folder = APILocator.getFolderAPI().findFolderByPath(path, host, user, false);
            return null != folder && null != folder.getIdentifier() && APILocator.getFileAssetAPI().fileNameExists(host, folder, type + ".vtl");
        } catch (Exception  e) {
            return false;
        }
    }

    @Override
    public String toHtml(final String baseTemplatePath) {

        try {

            final User user = APILocator.systemUser();
            final Tuple2<String, Host> hostPathTuple = resolveHost(baseTemplatePath, user);
            final Host host = hostPathTuple._2();
            final String relativePath = hostPathTuple._1();

            if (existsFile(relativePath, host, user)) {

                Context context = externalContext;
                if (externalContext == null) {

                    final HttpServletRequest requestProxy   = new FakeHttpRequest(host.getHostname(), null).request();
                    final HttpServletResponse responseProxy = new BaseResponse().response();
                    context = VelocityUtil.getInstance().getContext(requestProxy, responseProxy);
                }

                context.put("item", item);

                final String customTemplate = String.format("#dotParse(\"%s\")", baseTemplatePath + type + ".vtl");
                return VelocityUtil.getInstance().parseVelocity(customTemplate, context);
            }

            return this.toHtml();
        } catch (DotStateException | DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage(), e);
            return this.toHtml();
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private Tuple2<String, Host>  resolveHost(final String baseTemplatePath, final User user) throws DotSecurityException, DotDataException {

        final Tuple2<String, Host> hostPathTuple = Try.of(()-> HostUtil.splitPathHost(baseTemplatePath, user,
                StringPool.FORWARD_SLASH)).get();
        return hostPathTuple._2() == null?
                Tuple.of(hostPathTuple._1(), APILocator.getHostAPI().findDefaultHost(user, false)):
                hostPathTuple;
    }
}
