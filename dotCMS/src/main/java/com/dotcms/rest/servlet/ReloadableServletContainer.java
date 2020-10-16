/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.dotcms.rest.servlet;

import com.dotcms.rest.annotation.HeaderFilter;
import com.dotcms.rest.annotation.RequestFilter;
import com.dotcms.rest.api.CorsFilter;
import com.dotcms.rest.api.MyObjectMapperProvider;
import com.dotcms.rest.config.DotRestApplication;
import com.dotcms.rest.exception.mapper.*;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Throwables;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.util.List;

public class ReloadableServletContainer extends HttpServlet  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static ServletContainer container = null;

    private static ServletConfig servletConfig;

    public ReloadableServletContainer() {
        this(new DotRestApplication());
    }

    public ReloadableServletContainer(Class<? extends Application> appClass) {
        container = new ServletContainer(createResourceConfig(appClass));
    }

    public ReloadableServletContainer(Application app) {
        container = new ServletContainer(createResourceConfig(app));
    }

    // GenericServlet


    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            container.service(req, res);
        } catch (ServletException e) {

            List<Throwable> chain = Throwables.getCausalChain(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            if(chain.get(chain.size() - 1) instanceof UnrecognizedPropertyException){
                // Log the exception at trace level only, since we handled it, and thus (presumably) understand what caused it.
                Logger.getLogger(this.getClass()).warn("Bad request: " + e.getMessage());
                Logger.getLogger(this.getClass()).trace("Bad request:", e);

            } else{
                Logger.getLogger(this.getClass()).error("Unhandled error during request processing: ", e);
                throw e;
            }
        } catch (IOException e) {
            Logger.getLogger(this.getClass()).error("Unhandled error during request processing: ", e);
            throw e;
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        servletConfig = config;
        container.init(config);
    }

    public static void reload(final Application app) {
       
        try {
            final ServletContainer testContainer = new ServletContainer(createResourceConfig(app));
            testContainer.init(servletConfig);
            container = testContainer; // todo: do a thread-safe switch
        } catch (ServletException e) {
            throw new DotStateException(e.getMessage(), e);
        }
    }

    /**
     * Destroy this Servlet or Filter.
     */
    @Override
    public void destroy() {
        if(container != null) {
            container.destroy();
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        container.init(filterConfig);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        container.service(req, res);
    }

    private static ResourceConfig createResourceConfig(Application app) {
        return configureResourceConfig(ResourceConfig.forApplication(app));
    }

    private static ResourceConfig createResourceConfig(Class<? extends Application> appClass) {
        return configureResourceConfig(ResourceConfig.forApplicationClass(appClass));
    }

    private static ResourceConfig configureResourceConfig(ResourceConfig config) {
        return config
                .register(RequestFilter.class)
                .register(HeaderFilter.class)
                .register(CorsFilter.class)
                .register(MyObjectMapperProvider.class)
                .register(JacksonJaxbJsonProvider.class)
                .register(HttpStatusCodeExceptionMapper.class)
                .register(ResourceNotFoundExceptionMapper.class)
                .register(InvalidFormatExceptionMapper.class)
                .register(JsonParseExceptionMapper.class)
                .register(ParamExceptionMapper.class)
                .register(JsonMappingExceptionMapper.class)
                .register(UnrecognizedPropertyExceptionMapper.class)
                .register(InvalidLicenseExceptionMapper.class)
                .register(WorkflowPortletAccessExceptionMapper.class)
                .register(NotFoundInDbExceptionMapper.class)
                .register(DoesNotExistExceptionMapper.class)
                .register((new DotBadRequestExceptionMapper<AlreadyExistException>(){}).getClass())
                .register((new DotBadRequestExceptionMapper<IllegalArgumentException>(){}).getClass())
                .register((new DotBadRequestExceptionMapper<DotStateException>(){}).getClass())
                .register(DefaultDotBadRequestExceptionMapper.class)
                .register((new DotBadRequestExceptionMapper<JsonProcessingException>(){}).getClass())
                .register((new DotBadRequestExceptionMapper<NumberFormatException>(){}).getClass())
                .register(DotSecurityExceptionMapper.class)
                .register(DotDataExceptionMapper.class)
                .register(ElasticsearchStatusExceptionMapper.class)
                .register((new DotBadRequestExceptionMapper<InvalidFolderNameException>(){}).getClass())
                .register(RuntimeExceptionMapper.class);
                //.register(ExceptionMapper.class); // temporaly unregister since some services are expecting just a plain message as an error instead of a json, so to keep the compatibility we won't apply this change yet.
    }
}
