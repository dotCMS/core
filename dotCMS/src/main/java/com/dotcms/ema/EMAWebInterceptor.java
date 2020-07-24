package com.dotcms.ema;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.Header;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.ema.proxy.ProxyResponse;
import com.dotcms.ema.proxy.ProxyTool;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

public class EMAWebInterceptor  implements WebInterceptor{

    public  static final String      PROXY_EDIT_MODE_URL_VAR = "proxyEditModeUrl";
    private static final String      API_CALL                = "/api/v1/page/render";
    private static final ProxyTool   proxy                   = new ProxyTool();


    @Override
    public String[] getFilters() {
        return new String[] {
                API_CALL + "*"
        };
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final PageMode mode             = PageMode.get(request);
        final Optional<String> proxyUrl = this.proxyUrl(request);

        if (!proxyUrl.isPresent() || mode == PageMode.LIVE) {
            return Result.NEXT;
        }

        Logger.info(this.getClass(), "GOT AN EMA Call -->" + request.getRequestURI());

        return new Result.Builder().wrap(new MockHttpCaptureResponse(response)).next().build();
    }


    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        try {

            if (response instanceof MockHttpCaptureResponse) {
                
                final Optional<String> proxyUrl            = proxyUrl(request);
                final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse)response;
                final String postJson                      = new String(mockResponse.getBytes());
                final JSONObject json                      = new JSONObject(postJson);
                final Map<String, String> params           = ImmutableMap.of("dotPageData", postJson);
                
                Logger.info(this.getClass(), "Proxying Request -->" + proxyUrl.get());
                
                String responseStr = new String();
                final ProxyResponse pResponse = proxy.sendPost(proxyUrl.get(), params);

                if (pResponse.getResponseCode() == 200) {
                    responseStr = new String(pResponse.getResponse());
                }else {
                    responseStr+="<html><body>";
                    responseStr+="<h3>Unable to connect with the rendering engine</h3>";
                    responseStr+="<br><div style='display:inline-block;width:80px'>Trying: </div><b>" + proxyUrl.get()  + "</b>";
                    responseStr+="<br><div style='display:inline-block;width:80px'>Got:</div><b>" + pResponse.getStatus() + "</b>";
                    responseStr+="<hr>";
                    responseStr+="<h4>Headers</h4>";
                    responseStr+="<table border=1 style='min-width:500px'>";

                    for(Header header : pResponse.getHeaders()) {
                      responseStr+="<tr><td style='font-weight:bold;padding:5px;'><pre>" + header.getName() + "</pre></td><td><pre>" + header.getValue() + "</td></tr>";
                    }
                    responseStr+="</table>";
                    
                    responseStr+="<p>The Json Payload, POSTing as Content-Type:'application/x-www-form-urlencoded' with form param <b>dotPageData</b>, has been printed in the logs.</p>";
                    responseStr+="</body></html>";

                }

                json.getJSONObject("entity").getJSONObject("page").put("rendered", responseStr);
                json.getJSONObject("entity").getJSONObject("page").put("remoteRendered", true);
                response.setContentType("application/json");

                response.getWriter().write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    private Optional<String> proxyUrl(HttpServletRequest request) {


        final String uri = request.getRequestURI();

        if (!uri.startsWith(API_CALL)) {
            return Optional.empty();
        }


        Host host = this.getCurrentHost(request);
        if(host==null) {
            return Optional.empty();
        }
        String proxyUrl = host.getStringProperty(PROXY_EDIT_MODE_URL_VAR);


        return Optional.ofNullable(proxyUrl);


    }


    @CloseDBIfOpened
    public Host getCurrentHost(HttpServletRequest request) {
        try {
            Host host = null;
            HttpSession session = request.getSession(false);
            UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
            User systemUser = APILocator.systemUser();
            boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(request);

            PageMode mode = PageMode.get(request);

            String pageHostId = request.getParameter("host_id");
            if (pageHostId != null && mode.isAdmin) {
                host = APILocator.getHostAPI().find(pageHostId, systemUser, respectFrontendRoles);
            } else {
                if (session != null && session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID) != null && mode.isAdmin) {
                    host = APILocator.getHostAPI().find((String) session.getAttribute(WebKeys.CMS_SELECTED_HOST_ID), systemUser, false);
                } else if (session != null && mode.isAdmin && session.getAttribute(WebKeys.CURRENT_HOST) != null) {
                    host = (Host) session.getAttribute(WebKeys.CURRENT_HOST);
                } else if (request.getAttribute(WebKeys.CURRENT_HOST) != null) {
                    host = (Host) request.getAttribute(WebKeys.CURRENT_HOST);
                } else {
                    String serverName = request.getServerName();
                    if (UtilMethods.isSet(serverName)) {
                        host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
                    }
                }
            }

            request.setAttribute(WebKeys.CURRENT_HOST, host);
            if (session != null && mode.isAdmin) {
                session.setAttribute(WebKeys.CMS_SELECTED_HOST_ID, host.getIdentifier());
                session.setAttribute(WebKeys.CURRENT_HOST, host);
            }
            return host;
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }



}
