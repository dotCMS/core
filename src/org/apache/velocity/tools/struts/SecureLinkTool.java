/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.struts;

import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.tools.LinkTool;
import org.apache.velocity.tools.struts.StrutsUtils;

import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.SecureActionConfig;
import org.apache.struts.action.SecurePlugInInterface;
import org.apache.struts.Globals;

/**
 * Tool to be able to use Struts SSL Extensions with Velocity.
 * <p>It has the same interface as StrutsLinkTool and can function as a
 * substitute if Struts 1.1 and SSL Ext are installed. </p>
 * <p>Usage:
 * <pre>
 * Template example:
 * &lt;!-- Use just like a regular StrutsLinkTool --&gt;
 * $link.setAction("nameOfAction")
 * $link.setForward("nameOfForward")
 *
 * If the action or forward is marked as secure, or not,
 * in your struts-config then the link will be rendered
 * with https or http accordingly.
 *
 * Toolbox configuration:
 * &lt;tool&gt;
 *   &lt;key&gt;link&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.struts.SecureLinkTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre>
 * </p>
 * @since VelocityTools 1.1
 * @author <a href="mailto:marinoj@centrum.is">Marino A. Jonsson</a>
 * @version $Revision: 154929 $ $Date: 2005-02-22 18:04:46 -0800 (Tue, 22 Feb 2005) $
 */
public class SecureLinkTool extends LinkTool
{

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String STD_HTTP_PORT = "80";
    private static final String STD_HTTPS_PORT = "443";


    /**
     * <p>Returns a copy of the link with the given action name
     * converted into a server-relative URI reference. This method
     * does not check if the specified action really is defined.
     * This method will overwrite any previous URI reference settings
     * but will copy the query string.</p>
     *
     * @param action an action path as defined in struts-config.xml
     *
     * @return a new instance of StrutsLinkTool
     */
    public SecureLinkTool setAction(String action)
    {
        String link = StrutsUtils.getActionMappingURL(application, request, action);
        return (SecureLinkTool)copyWith(computeURL(request, application, link));
    }

    /**
     * <p>Returns a copy of the link with the given global forward name
     * converted into a server-relative URI reference. If the parameter
     * does not map to an existing global forward name, <code>null</code>
     * is returned. This method will overwrite any previous URI reference
     * settings but will copy the query string.</p>
     *
     * @param forward a global forward name as defined in struts-config.xml
     *
     * @return a new instance of StrutsLinkTool
     */
    public SecureLinkTool setForward(String forward)
    {
        String url = StrutsUtils.getForwardURL(request, application, forward);
        if (url == null)
        {
            return null;
        }
        return (SecureLinkTool)copyWith(url);
    }

    /**
     * Compute a hyperlink URL based on the specified action link.
     * The returned URL will have already been passed to
     * <code>response.encodeURL()</code> for adding a session identifier.
     *
     * @param request the current request.
     * @param app the current ServletContext.
     * @param link the action that is to be converted to a hyperlink URL
     * @return the computed hyperlink URL
     */
    public String computeURL(HttpServletRequest request,
                             ServletContext app, String link)
    {
        StringBuffer url = new StringBuffer(link);

        String contextPath = request.getContextPath();

        SecurePlugInInterface securePlugin = (SecurePlugInInterface)app.getAttribute(SecurePlugInInterface.SECURE_PLUGIN);

        if (securePlugin.getSslExtEnable() &&
            url.toString().startsWith(contextPath))
        {
            // Initialize the scheme and ports we are using
            String usingScheme = request.getScheme();
            String usingPort = String.valueOf(request.getServerPort());

            // Get the servlet context relative link URL
            String linkString = url.toString().substring(contextPath.length());

            // See if link references an action somewhere in our app
            SecureActionConfig secureConfig = getActionConfig(request, app, linkString);

            // If link is an action, find the desired port and scheme
            if (secureConfig != null &&
                !SecureActionConfig.ANY.equalsIgnoreCase(secureConfig.getSecure()))
            {
                String desiredScheme = Boolean.valueOf(secureConfig.getSecure()).booleanValue() ?
                    HTTPS : HTTP;
                String desiredPort = Boolean.valueOf(secureConfig.getSecure()).booleanValue() ?
                    securePlugin.getHttpsPort() : securePlugin.getHttpPort();

                // If scheme and port we are using do not match the ones we want
                if (!desiredScheme.equals(usingScheme) ||
                    !desiredPort.equals(usingPort))
                {
                    url.insert(0, startNewUrlString(request, desiredScheme, desiredPort));

                    // This is a hack to help us overcome the problem that some
                    // older browsers do not share sessions between http & https
                    // If this feature is diabled, session ID could still be added
                    // the previous call to the RequestUtils.computeURL() method,
                    // but only if needed due to cookies disabled, etc.
                    if (securePlugin.getSslExtAddSession() && url.toString().indexOf(";jsessionid=") < 0)
                    {
                        // Add the session identifier
                        url = new StringBuffer(toEncoded(url.toString(),
                                               request.getSession().getId()));
                    }
                }
            }
        }
        return url.toString();
    }

    /**
     * Finds the configuration definition for the specified action link
     *
     * @param request the current request.
     * @param app the current ServletContext.
     * @param linkString The action we are searching for, specified as a
     *        link. (i.e. may include "..")
     * @return The SecureActionConfig object entry for this action,
     *         or null if not found
     */
    private static SecureActionConfig getActionConfig(HttpServletRequest
            request,
            ServletContext app,
            String linkString)
    {
        ModuleConfig moduleConfig = StrutsUtils.selectModule(linkString, app);

        // Strip off the module path, if any
        linkString = linkString.substring(moduleConfig.getPrefix().length());

        // Use our servlet mapping, if one is specified
        //String servletMapping = (String)app.getAttribute(Globals.SERVLET_KEY);

        SecurePlugInInterface spi = (SecurePlugInInterface)app.getAttribute(
                SecurePlugInInterface.SECURE_PLUGIN);
        Iterator mappingItr = spi.getServletMappings().iterator();
        while (mappingItr.hasNext())
        {
            String servletMapping = (String)mappingItr.next();

            int starIndex = servletMapping != null ? servletMapping.indexOf('*')
                            : -1;
            if (starIndex == -1)
            {
                continue;
            } // No servlet mapping or no usable pattern defined, short circuit

            String prefix = servletMapping.substring(0, starIndex);
            String suffix = servletMapping.substring(starIndex + 1);

            // Strip off the jsessionid, if any
            int jsession = linkString.indexOf(";jsessionid=");
            if (jsession >= 0)
            {
                linkString = linkString.substring(0, jsession);
            }

            // Strip off the query string, if any
            // (differs from the SSL Ext. version - query string before anchor)
            int question = linkString.indexOf("?");
            if (question >= 0)
            {
                linkString = linkString.substring(0, question);
            }

            // Strip off the anchor, if any
            int anchor = linkString.indexOf("#");
            if (anchor >= 0)
            {
                linkString = linkString.substring(0, anchor);
            }


            // Unable to establish this link as an action, short circuit
            if (!(linkString.startsWith(prefix) && linkString.endsWith(suffix)))
            {
                continue;
            }

            // Chop off prefix and suffix
            linkString = linkString.substring(prefix.length());
            linkString = linkString.substring(0,
                                              linkString.length()
                                              - suffix.length());
            if (!linkString.startsWith("/"))
            {
                linkString = "/" + linkString;
            }

            SecureActionConfig secureConfig = (SecureActionConfig)moduleConfig.
                                              findActionConfig(linkString);

            return secureConfig;
        }
        return null;

    }

    /**
     * Builds the protocol, server name, and port portion of the new URL
     * @param request The current request
     * @param desiredScheme  The scheme (http or https) to be used in the new URL
     * @param desiredPort The port number to be used in th enew URL
     * @return The new URL as a StringBuffer
     */
    private static StringBuffer startNewUrlString(HttpServletRequest request,
                                                  String desiredScheme,
                                                  String desiredPort)
    {
        StringBuffer url = new StringBuffer();
        String serverName = request.getServerName();
        url.append(desiredScheme).append("://").append(serverName);

        if ((HTTP.equals(desiredScheme) && !STD_HTTP_PORT.equals(desiredPort)) ||
            (HTTPS.equals(desiredScheme) && !STD_HTTPS_PORT.equals(desiredPort)))
        {
            url.append(":").append(desiredPort);
        }
        return url;
    }

    /**
     * Return the specified URL with the specified session identifier
     * suitably encoded.
     *
     * @param url URL to be encoded with the session id
     * @param sessionId Session id to be included in the encoded URL
     * @return the specified URL with the specified session identifier suitably encoded
     */
    public String toEncoded(String url, String sessionId)
    {
        if (url == null || sessionId == null)
        {
            return (url);
        }

        String path = url;
        String query = "";
        String anchor = "";

        // (differs from the SSL Ext. version - anchor before query string)
        int pound = url.indexOf('#');
        if (pound >= 0)
        {
            path = url.substring(0, pound);
            anchor = url.substring(pound);
        }
        int question = path.indexOf('?');
        if (question >= 0)
        {
            query = path.substring(question);
            path = path.substring(0, question);
        }
        StringBuffer sb = new StringBuffer(path);
        // jsessionid can't be first.
        if (sb.length() > 0)
        {
            sb.append(";jsessionid=");
            sb.append(sessionId);
        }
        sb.append(query);
        sb.append(anchor);
        return sb.toString();
    }

}
