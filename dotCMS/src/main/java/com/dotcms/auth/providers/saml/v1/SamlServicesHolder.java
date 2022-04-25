package com.dotcms.auth.providers.saml.v1;

import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotmarketing.business.APILocator;
import org.apache.felix.framework.OSGIUtil;
import org.apache.felix.framework.RestartOSgiAware;

import java.io.Serializable;

/**
 * This class is mostly to hold the saml osgi components in one place that can be restarted by core
 * when osgi restart, that way we can have the attributes of the {@link DotSamlResource} rewire correctly
 * @author jsanca
 */
public class SamlServicesHolder implements Serializable, RestartOSgiAware {

    public static SamlServicesHolder getInstance() {
        return SamlServicesHolder.SamlResourceServiceHolder.instance;
    }

    private static class SamlResourceServiceHolder{
        private static SamlServicesHolder instance = new SamlServicesHolder();
    }

    private SamlConfigurationService samlConfigurationService;
    private SAMLHelper           				   samlHelper;
    private SamlAuthenticationService samlAuthenticationService;
    private IdentityProviderConfigurationFactory identityProviderConfigurationFactory;

    public SamlServicesHolder() {

        OSGIUtil.getInstance().registerRestartFelixAware(this.getClass().getName(), this);
        this.onRestartOsgi();
    }

    @Override
    public void onRestartOsgi() {

        onRestartOsgi(DotSamlProxyFactory.getInstance().samlConfigurationService(),
                new SAMLHelper(this.samlAuthenticationService, APILocator.getCompanyAPI()),
                DotSamlProxyFactory.getInstance().samlAuthenticationService(),
                DotSamlProxyFactory.getInstance().identityProviderConfigurationFactory());
    }

    public synchronized void onRestartOsgi(final SamlConfigurationService             samlConfigurationService,
                                           final SAMLHelper           				  samlHelper,
                                           final SamlAuthenticationService            samlAuthenticationService,
                                           final IdentityProviderConfigurationFactory identityProviderConfigurationFactory) {

        this.samlConfigurationService			  = samlConfigurationService;
        this.samlAuthenticationService            = samlAuthenticationService;
        this.identityProviderConfigurationFactory = identityProviderConfigurationFactory;
        this.samlHelper                           = samlHelper;
    }

    public SamlConfigurationService getSamlConfigurationService() {
        return samlConfigurationService;
    }

    public SAMLHelper getSamlHelper() {
        return samlHelper;
    }

    public SamlAuthenticationService getSamlAuthenticationService() {
        return samlAuthenticationService;
    }

    public IdentityProviderConfigurationFactory getIdentityProviderConfigurationFactory() {
        return identityProviderConfigurationFactory;
    }
}
