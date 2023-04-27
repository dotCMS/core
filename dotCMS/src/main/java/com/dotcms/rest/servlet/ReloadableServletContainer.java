package com.dotcms.rest.servlet;

import com.dotcms.rest.annotation.HeaderFilter;
import com.dotcms.rest.annotation.RequestFilter;
import com.dotcms.rest.api.CorsFilter;
import com.dotcms.rest.api.MyObjectMapperProvider;
import com.dotcms.rest.config.DotRestApplication;
import com.dotcms.rest.exception.mapper.DefaultDotBadRequestExceptionMapper;
import com.dotcms.rest.exception.mapper.DoesNotExistExceptionMapper;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.dotcms.rest.exception.mapper.DotDataExceptionMapper;
import com.dotcms.rest.exception.mapper.DotSecurityExceptionMapper;
import com.dotcms.rest.exception.mapper.ElasticsearchStatusExceptionMapper;
import com.dotcms.rest.exception.mapper.HttpStatusCodeExceptionMapper;
import com.dotcms.rest.exception.mapper.InvalidFormatExceptionMapper;
import com.dotcms.rest.exception.mapper.InvalidLicenseExceptionMapper;
import com.dotcms.rest.exception.mapper.JsonMappingExceptionMapper;
import com.dotcms.rest.exception.mapper.JsonParseExceptionMapper;
import com.dotcms.rest.exception.mapper.NotFoundInDbExceptionMapper;
import com.dotcms.rest.exception.mapper.ParamExceptionMapper;
import com.dotcms.rest.exception.mapper.ResourceNotFoundExceptionMapper;
import com.dotcms.rest.exception.mapper.RuntimeExceptionMapper;
import com.dotcms.rest.exception.mapper.UnrecognizedPropertyExceptionMapper;
import com.dotcms.rest.exception.mapper.WorkflowPortletAccessExceptionMapper;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class ReloadableServletContainer extends ServletContainer  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(new ServletConfigWrapper(config, Map.of("javax.ws.rs.Application", DotRestApplication.class.getName())));
    }

    private static class ServletConfigWrapper extends HashMap<String,Object> implements ServletConfig {

        private final ServletConfig config;
        private final Map<String, String> initParams=new HashMap<>();

        public ServletConfigWrapper(ServletConfig config, Map<String, String> defaultInitParams) {
            this.config = config;
            this.initParams.putAll(defaultInitParams);
            for (Enumeration<String> e = config.getInitParameterNames(); e.hasMoreElements();) {
                String name = e.nextElement();
                initParams.put(name, config.getInitParameter(name));
            }
        }

        @Override
        public String getServletName() {
            return config.getServletName();
        }

        @Override
        public ServletContext getServletContext() {
            return config.getServletContext();
        }

        @Override
        public String getInitParameter(String name) {
            return initParams.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(initParams.keySet());
        }
    }
}
