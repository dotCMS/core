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

import com.dotcms.repackage.com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.dotcms.repackage.com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.dotcms.repackage.com.google.common.base.Throwables;
import com.dotcms.repackage.javax.ws.rs.core.Application;
import com.dotcms.repackage.org.glassfish.jersey.server.ResourceConfig;
import com.dotcms.repackage.org.glassfish.jersey.servlet.ServletContainer;
import com.dotcms.rest.annotation.HeaderFilter;
import com.dotcms.rest.annotation.RequestFilter;
import com.dotcms.rest.api.CorsFilter;
import com.dotcms.rest.api.MyObjectMapperProvider;
import com.dotcms.rest.config.DotRestApplication;
import com.dotcms.rest.exception.mapper.*;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;

import java.io.IOException;

import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReloadableServletContainer extends HttpServlet implements Filter {

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
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
            container.doFilter(req, res, chain);
    }

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

    public static void reload(Application app) {
        container = new ServletContainer(createResourceConfig(app));
        try {
            container.init(servletConfig);
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
                .register(InvalidFormatExceptionMapper.class)
                .register(JsonParseExceptionMapper.class)
                .register(UnrecognizedPropertyExceptionMapper.class);
                //.register(ExceptionMapper.class); // temporaly unregister since some services are expecting just a plain message as an error instead of a json, so to keep the compatibility we won't apply this change yet.
    }
}