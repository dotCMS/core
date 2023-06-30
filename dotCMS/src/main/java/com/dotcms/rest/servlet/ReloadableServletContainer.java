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

import com.dotcms.rest.config.DotRestApplication;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.WebConfig;
import org.glassfish.jersey.servlet.WebServletConfig;

/**
 * This class is a wrapper around the Jersey ServletContainer that prevents needing
 * to reconfigure the web.xml file.   The base DotRestApplication now handles the
 * servlet reloading.  This class is only needed to add the additional init parameters
 *
 */
public class ReloadableServletContainer extends ServletContainer  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Map<String,String> ADDITIONAL_INIT_PARAMS = Map.of("javax.ws.rs.Application", DotRestApplication.class.getName());

    public ReloadableServletContainer() {
    }

    @Override
    public void init() throws ServletException {
        // Cannot extend WebServletConfig because it is final so we will decorate it to modify
        // the init parameters
        init(new WebServletConfigDecorator(new WebServletConfig(this)));
    }


    private class WebServletConfigDecorator implements WebConfig {

        private final WebServletConfig webServletConfig;

        public WebServletConfigDecorator(WebServletConfig webServletConfig) {
            this.webServletConfig = webServletConfig;
        }

        @Override
        public ConfigType getConfigType() {
            return webServletConfig.getConfigType();
        }

        @Override
        public ServletConfig getServletConfig() {
            return webServletConfig.getServletConfig();
        }

        @Override
        public FilterConfig getFilterConfig() {
            return webServletConfig.getFilterConfig();
        }

        @Override
        public String getName() {
            return webServletConfig.getName();
        }

        @Override
        public String getInitParameter(String name) {
            if (ADDITIONAL_INIT_PARAMS.containsKey(name)) {
                return ADDITIONAL_INIT_PARAMS.get(name);
            }
            return webServletConfig.getInitParameter(name);
        }

        @Override
        public Enumeration getInitParameterNames() {
            // Add the "javax.ws.rs.Application" init parameter to the list of init parameters
            // so that the Jersey application can be configured via web.xml

            @SuppressWarnings("unchecked")
            HashSet<String> paramNamesSet = new HashSet<String>(Collections.list(webServletConfig.getInitParameterNames()));

            paramNamesSet.addAll(ADDITIONAL_INIT_PARAMS.keySet());
            return Collections.enumeration(paramNamesSet);
        }

        @Override
        public ServletContext getServletContext() {
            return webServletConfig.getServletContext();
        }
    }
}
