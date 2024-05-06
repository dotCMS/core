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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.util.json.JSONObject;

import java.io.File;

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

            final String template = this.existsInFileSystem(this.defaultPath + type + ".vtl")?
                    this.defaultPath + type + ".vtl": this.defaultPath + defaultTemplateName;
            return VelocityUtil.getInstance().mergeTemplate(template, context);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private boolean existsInFileSystem(final String filePath) {
        return Files.exists(Paths.get(VelocityUtil.getVelocityRootPath(),filePath));
    }

    private Tuple2<Boolean, String> existsFile (final String path, final Host host, final User user)  {

        final Tuple2<Boolean, String> notFoundResult =  Tuple.of(false, null);

        Logger.debug(this, ()-> "host: " + host.getHostname() + ", type: " + type);
        Logger.debug(this, ()-> "item: " + item.toString());
        try {

            Folder folder = APILocator.getFolderAPI().findFolderByPath(path, host, user, false);
            String contentType = StringPool.BLANK;

            if ("dotContent".equalsIgnoreCase(this.type) && this.item instanceof JSONObject) {

                if (JSONObject.class.cast(this.item).has("attrs")) {

                    final JSONObject attrs = JSONObject.class.cast(this.item).getJSONObject("attrs");
                    if (attrs.has("data")) {

                        final JSONObject data = attrs.getJSONObject("data");
                        if (data.has("contentType")) {

                            contentType = StringPool.DASH + data.getString("contentType");
                        }
                    }
                }
            }

            // this one is just the type.vtl
            final String fileNameType = type + ".vtl";
            // this one would be when there is a cotentlet aka dotContent to check if exists the content type implement
            final String fileNameContentType = type + contentType + ".vtl";

            Logger.debug(this, ()-> "fileNameType: " + fileNameType + ", fileNameContentType: " + fileNameContentType);
             if(null != folder && null != folder.getIdentifier()) {

                 if(APILocator.getFileAssetAPI().fileNameExists(host, folder, fileNameContentType)) {

                     return Tuple.of(true, fileNameContentType);
                 }

                 if (APILocator.getFileAssetAPI().fileNameExists(host, folder, fileNameType)){

                     return Tuple.of(true, fileNameType);
                 }
             }
        } catch (Exception  e) {

            return notFoundResult;
        }

        return notFoundResult;
    }

    @Override
    public String toHtml(final String baseTemplatePath) {

        try {

            final User user = APILocator.systemUser();
            final Tuple2<String, Host> hostPathTuple = resolveHost(baseTemplatePath, user);
            final Host host = hostPathTuple._2();
            final String relativePath = hostPathTuple._1();
            final Tuple2<Boolean, String> existFileResult = existsFile(relativePath, host, user);
            Logger.debug(this, ()-> "Checking if exists the path: " + relativePath + ", existFileResult: "
                    + existFileResult._1() + ", " + existFileResult._2());
            if (existFileResult._1()) {

                Context context = externalContext;
                if (externalContext == null) {

                    final HttpServletRequest requestProxy   = new FakeHttpRequest(host.getHostname(), null).request();
                    final HttpServletResponse responseProxy = new BaseResponse().response();
                    context = VelocityUtil.getInstance().getContext(requestProxy, responseProxy);
                }

                context.put("item", item);

                final String customTemplate = String.format("#dotParse(\"%s\")", baseTemplatePath + existFileResult._2());

                Logger.debug(this, ()-> "Using the customTemplate: " + customTemplate);
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
