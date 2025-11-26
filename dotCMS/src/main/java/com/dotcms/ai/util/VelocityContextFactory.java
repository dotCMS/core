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
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class VelocityContextFactory {

    private VelocityContextFactory() {}

    public static Context getMockContext(Contentlet contentlet) {
        return getMockContext(contentlet, APILocator.systemUser());
    }

    public static Context getMockContext(final Contentlet contentlet, final User user) {
        final Host host = UtilMethods.isSet(contentlet.getIdentifier()) ?
                Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(),
                        APILocator.systemUser(), true)).getOrElse(APILocator.systemHost()) :
                APILocator.systemHost();
        String hostName = "SYSTEM_HOST".equalsIgnoreCase(host.getIdentifier())
                ? Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElseThrow(DotRuntimeException::new)
                : host.getHostname();


        HttpServletRequest requestProxy =
                new MockSessionRequest(
                        new MockHeaderRequest(
                                new FakeHttpRequest(hostName, null).request(), "referer", "https://" + hostName + "/fakeRefer"
                        ).request()
                );
        requestProxy.setAttribute(WebKeys.CMS_USER, user);
        requestProxy.getSession().setAttribute(WebKeys.CMS_USER, user);
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());




        Context ctx = VelocityUtil.getWebContext(requestProxy, new BaseResponse().response());
        ContentMap contentMap = new ContentMap(contentlet, user, PageMode.EDIT_MODE, host, ctx);
        ctx.put("contentMap", contentMap);
        ctx.put("dotContentMap", contentMap);
        ctx.put("contentlet", contentMap);
        Optional<String> contentletToString = ContentToStringUtil.impl.get().turnContentletIntoString(contentlet);
        if(contentletToString.isPresent()) {
            ctx.put("contentletToString", contentletToString.get());
        }
        return ctx;
    }

    public static Context getMockContext() {

        String hostName = Try.of(() -> APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false).getHostname()).getOrElse("localhost");
        HttpServletRequest requestProxy =
                new MockSessionRequest(new MockHeaderRequest(new FakeHttpRequest(hostName, null).request(), "referer", "https://" + hostName + "/fakeRefer").request());
        HttpServletResponse responseProxy = new BaseResponse().response();
        return VelocityUtil.getWebContext(requestProxy, responseProxy);
    }




}
