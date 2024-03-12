package com.dotmarketing.filters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.filters.interceptor.FilterWebInterceptorProvider;
import com.dotcms.filters.interceptor.SimpleWebInterceptorDelegateImpl;
import com.dotcms.listeners.SessionMonitor;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import org.junit.BeforeClass;
import org.junit.Test;

public class AutoLoginFilterTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link AutoLoginFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     * When: Send a request to /dotAdmin/logout?r=${new Date().getTime()
     * Should: return 200
     */
    @Test
    public void shouldReturnOkWithLogoutRequest() throws Exception {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURI()).thenReturn("/dotAdmin/logout?r=" + System.currentTimeMillis());

        final ServletResponse servletResponse = mock(HttpServletResponse.class);
        final FilterChain filterChain = mock(FilterChain.class);

        final FilterConfig filterConfig = mock(FilterConfig.class);

        try {
            Config.CONTEXT = mock(ServletContext.class);
            when(servletRequest.getServletContext()).thenReturn(Config.CONTEXT);
            when(filterConfig.getServletContext()).thenReturn(Config.CONTEXT);

            final FilterWebInterceptorProvider filterWebInterceptorProvider =
                    mock(FilterWebInterceptorProvider.class);
            when(Config.CONTEXT.getAttribute(FilterWebInterceptorProvider.class.getName()))
                    .thenReturn(filterWebInterceptorProvider);

            SimpleWebInterceptorDelegateImpl simpleWebInterceptorDelegate = new SimpleWebInterceptorDelegateImpl();
            when(filterWebInterceptorProvider.getDelegate(AutoLoginFilter.class))
                    .thenReturn(simpleWebInterceptorDelegate);

            final RequestDispatcher requestDispatcher = mock(RequestDispatcher.class);

            when(Config.CONTEXT.getRequestDispatcher("/html/portal/show-logout.jsp"))
                    .thenReturn(requestDispatcher);

            final AutoLoginFilter autoLoginFilter = new AutoLoginFilter();
            autoLoginFilter.init(filterConfig);
            autoLoginFilter.doFilter(servletRequest, servletResponse, filterChain);

            verify(requestDispatcher).forward(servletRequest, servletResponse);
        } finally {
            Config.CONTEXT = null;
            ConfigTestHelper._setupFakeTestingContext();
        }
    }
}
