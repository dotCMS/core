package com.dotcms.filters.interceptor.saml;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.MockIdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.filters.CMSUrlUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlWebInterceptorTest {

    @Test
    public void test_intercept_by_pass_false_should_be_next() throws IOException {

        final Encryptor encryptor            = mock(Encryptor.class);
        final LoginServiceAPI loginService   = mock(LoginServiceAPI.class);
        final UserAPI userAPI                = mock(UserAPI.class);
        final HostWebAPI hostWebAPI          = mock(HostWebAPI.class);
        final AppsAPI appsAPI                = mock(AppsAPI.class);
        final SamlWebUtils    samlWebUtils   = mock(SamlWebUtils.class);
        final HttpServletRequest request     = mock(HttpServletRequest.class);
        final HttpServletResponse response   = mock(HttpServletResponse.class);
        final CMSUrlUtil cmsUrlUtil          = mock(CMSUrlUtil.class);
        final SamlConfigurationService samlConfigurationService = mock(SamlConfigurationService.class);
        final IdentityProviderConfigurationFactory identityProviderConfigurationFactory = new MockIdentityProviderConfigurationFactory();
        final SamlWebInterceptor interceptor = new SamlWebInterceptor(encryptor, loginService, userAPI, hostWebAPI,
                appsAPI, samlWebUtils, cmsUrlUtil, identityProviderConfigurationFactory);

        interceptor.setSamlConfig(samlConfigurationService);
        when(request.getParameter(SamlWebUtils.BY_PASS_KEY)).thenReturn("true");
        final Result result = interceptor.intercept(request, response);

        Assert.assertEquals(Result.NEXT, result);
    }

    @Test
    public void test_intercept_should_be_next() throws IOException {

        final Encryptor encryptor            = mock(Encryptor.class);
        final LoginServiceAPI loginService   = mock(LoginServiceAPI.class);
        final UserAPI userAPI                = mock(UserAPI.class);
        final HostWebAPI hostWebAPI          = mock(HostWebAPI.class);
        final AppsAPI appsAPI                = mock(AppsAPI.class);
        final SamlWebUtils    samlWebUtils   = mock(SamlWebUtils.class);
        final HttpServletRequest request     = mock(HttpServletRequest.class);
        final HttpServletResponse response   = mock(HttpServletResponse.class);
        final CMSUrlUtil cmsUrlUtil          = mock(CMSUrlUtil.class);
        final SamlConfigurationService samlConfigurationService = mock(SamlConfigurationService.class);
        final IdentityProviderConfigurationFactory identityProviderConfigurationFactory = new MockIdentityProviderConfigurationFactory();
        final SamlWebInterceptor interceptor = new SamlWebInterceptor(encryptor, loginService, userAPI, hostWebAPI,
                appsAPI, samlWebUtils, cmsUrlUtil, identityProviderConfigurationFactory);

        interceptor.setSamlConfig(samlConfigurationService);
        final Result result = interceptor.intercept(request, response);

        Assert.assertEquals(Result.NEXT, result);
    }
}
