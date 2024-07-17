package com.dotcms.ai.util;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * The VelocityContextFactory class is responsible for creating a Velocity context for a specific contentlet.
 *
 * @author Daniel Silva
 * @since Mar 27th, 2024
 */
public class VelocityContextFactory {

    private VelocityContextFactory() {}

    public static Context getMockContext(final Contentlet contentlet) {
        return getMockContext(contentlet, APILocator.systemUser());
    }

    /**
     * Creates a mock Velocity Context that will be used to evaluate Velocity code present in
     * Contentlet fields. The generated context will include a property named
     * {@code "contentletToString"} which represents the contentlet's data as a String.
     *
     * @param contentlet The Contentlet to be used in the context.
     * @param user       The User to be used in the context.
     *
     * @return The mock Velocity {@link Context} object.
     */
    public static Context getMockContext(final Contentlet contentlet, final User user) {
        return getMockContext(contentlet, user, true);
    }

    /**
     * Creates a mock Velocity Context that will be used to evaluate Velocity code present in
     * Contentlet fields. The generated context <b>will NOT include</b> the
     * {@code "contentletToString"} property, unlike the other methods in this class.
     *
     * @param contentlet             The Contentlet to be used in the context.
     * @param user                   The User to be used in the context.
     *
     * @return The mock Velocity {@link Context} object.
     */
    public static Context getMockContextNoContentToString(final Contentlet contentlet, final User user) {
        return getMockContext(contentlet, user, false);
    }

    /**
     * Creates a mock Velocity Context that will be used to evaluate Velocity code present in
     * Contentlet fields.
     *
     * @param contentlet             The Contentlet to be used in the context.
     * @param user                   The User to be used in the context.
     * @param includeContentToString If it's necessary to include a property with the contentlet's
     *                               data as a String, set this to {@code true}.
     *
     * @return The mock Velocity {@link Context} object.
     */
    private static Context getMockContext(final Contentlet contentlet, final User user, final boolean includeContentToString) {
        final Host site = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), true)).getOrElse(APILocator.systemHost());
        final String hostName = Host.SYSTEM_HOST.equalsIgnoreCase(site.getIdentifier())
                ? Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElseThrow(DotRuntimeException::new)
                : site.getHostname();
        final HttpServletRequest requestProxy =
                new MockSessionRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest(hostName, null).request(), "referer", "https://" + hostName + "/fakeRefer"
                        ).request()
                );
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        final Context ctx = VelocityUtil.getInstance().getContext(requestProxy, new BaseResponse().response());
        final ContentMap contentMap = new ContentMap(contentlet, user, PageMode.EDIT_MODE, site, ctx);
        ctx.put("contentMap", contentMap);
        ctx.put("dotContentMap", contentMap);
        ctx.put("contentlet", contentMap);
        if (includeContentToString) {
            final Optional<String> contentletToString = ContentToStringUtil.impl.get().turnContentletIntoString(contentlet);
            contentletToString.ifPresent(content -> ctx.put("contentletToString", content));
        }
        return ctx;
    }

}
