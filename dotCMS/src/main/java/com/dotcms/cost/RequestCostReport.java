package com.dotcms.cost;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.APILocator;
import com.liferay.util.Xss;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;

public class RequestCostReport {


    private final RequestCostApi requestCostApi = APILocator.getRequestCostAPI();

    String writeAccounting(HttpServletRequest request) {
        List<Map<String, Object>> accounting = requestCostApi.getAccountList(request);

        String url = Xss.escapeHTMLAttrib(request.getRequestURI());

        requestCostApi.endAccounting(request);
        Context context = VelocityUtil.getBasicContext();
        context.put("url", url);
        context.put("accounting", accounting);
        String parse = VelocityUtil.getInstance().merge("/static/request-accounting.vtl", context);

        return parse;

    }


}
