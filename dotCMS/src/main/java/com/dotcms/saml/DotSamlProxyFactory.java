package com.dotcms.saml;

import com.dotcms.auth.providers.saml.v1.DotSamlResource;
import com.dotcms.osgi.OSGIConstants;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecretSavedEvent;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.model.KeyFilterable;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.filters.DotUrlRewriteFilter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import org.apache.felix.framework.OSGISystem;
import org.tuckey.web.filters.urlrewrite.NormalRule;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the proxy to provides the object to interact with the Saml Osgi Bundle
 *
 * @author jsanca
 */
public class DotSamlProxyFactory implements EventSubscriber<AppSecretSavedEvent>, KeyFilterable {

    public static final String SAML_APP_CONFIG_KEY = "dotsaml-config";
    public static final String PROPERTIES_PATH     = File.separator + "saml" + File.separator + "dotcms-saml-default.properties";

    private static final String ASSETS_PATH   = Config.getStringProperty("ASSET_REAL_PATH",
            FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH", "/assets")));
    private static final String IDP_FILE_PATH = ASSETS_PATH + PROPERTIES_PATH;

    private final MessageObserver    messageObserver    = new DotLoggerMessageObserver();
    private final AppsAPI            appsAPI            = APILocator.getAppsAPI();
    private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory =
            new DotIdentityProviderConfigurationFactoryImpl(this.appsAPI, APILocator.getHostAPI());

    private SamlServiceBuilder        samlServiceBuilder;
    private SamlConfigurationService  samlConfigurationService;
    private SamlAuthenticationService samlAuthenticationService;

    private static final AtomicBoolean redirectsDone = new AtomicBoolean(false);

    private static class SingletonHolder {

        private static final DotSamlProxyFactory INSTANCE = new DotSamlProxyFactory();
    }
    /**
     * Get the instance.
     * @return DotSamlFactory
     */
    public static DotSamlProxyFactory getInstance() {

        addRedirects();
        return DotSamlProxyFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Key for the discard non-SAML AppSecretSavedEvent
     * @return Comparable
     */
    @Override
    public Comparable getKey() {
        return SAML_APP_CONFIG_KEY;
    }

    /**
     * When
     * @param event
     */
    @Override
    public void notify(final AppSecretSavedEvent event) {

        final  Map<String, Secret> secretMap = event.getAppSecrets().getSecrets();
        if (null != secretMap) {

            SamlValidator.validateURL("sPEndpointHostname", secretMap.get("sPEndpointHostname").getString(), event.getUserId());
            SamlValidator.validateXML("idPMetadataFile",    secretMap.get("idPMetadataFile").getString(),    event.getUserId());
        }
    }

    /**
     * Returns the dotCMS implementation of the identity provider config.
     * This one basically returns (if exists) the configuration for a idp for a host
     * @return IdentityProviderConfigurationFactory
     */
    public IdentityProviderConfigurationFactory identityProviderConfigurationFactory() {

        return identityProviderConfigurationFactory;
    }

    private static void addRedirects() {

        if (!redirectsDone.get()) {
            final NormalRule rule = new NormalRule();
            rule.setFrom("^\\/dotsaml\\/(" + String.join("|", DotSamlResource.dotsamlPathSegments) + ")\\/(.+)$");
            rule.setToType("forward");
            rule.setTo("/api/v1/dotsaml/$1/$2");
            rule.setName("Dotsaml REST Service Redirect");
            DotUrlRewriteFilter urlRewriteFilter = DotUrlRewriteFilter.getUrlRewriteFilter();
            try {
                if (urlRewriteFilter != null) {
                    urlRewriteFilter.addRule(rule);
                    redirectsDone.set(true);
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                Logger.error(DotSamlProxyFactory.class, "Could not add the Dotsaml REST Service Redirect Rule. Requests to " +
                        "/dotsaml/login/{UUID} will fail!");
            }
        }
    }

    private SamlServiceBuilder samlServiceBuilder() {

        if (null == this.samlServiceBuilder) {

            synchronized (this) {

                if (null == this.samlServiceBuilder) {


                   OSGISystem.getInstance().initializeFramework();
                       try {
                            this.samlServiceBuilder = OSGISystem.getInstance().getService(SamlServiceBuilder.class,
                                    OSGIConstants.BUNDLE_NAME_DOTCMS_SAML);

                            Logger.info(this, "SAML Osgi Bundle has been started");
                        } catch (Exception e) {
                            Logger.warnAndDebug(this.getClass(),
                                    String.format("Failure retrieving OSGI Service [%s] in bundle [%s]",
                                            SamlServiceBuilder.class,
                                            OSGIConstants.BUNDLE_NAME_DOTCMS_SAML), e);
                        }

                }
            }
        }

        return this.samlServiceBuilder;
    }

    /**
     * Returns the dotCMS implementation of the {@link MessageObserver} for the saml osgi bundle
     * @return MessageObserver
     */
    private MessageObserver messageObserver() {

        return this.messageObserver;
    }

    /**
     * Returns the service that helps to retrieve the actual values or default values from the {@link com.dotcms.saml.IdentityProviderConfiguration}
     *
     * @return SamlConfigurationService
     */
    public SamlConfigurationService samlConfigurationService() {

        if (null == this.samlConfigurationService) {

            final SamlServiceBuilder samlServiceBuilder = this.samlServiceBuilder();
            if (null != samlServiceBuilder) {

                synchronized (this) {

                    if (null == this.samlConfigurationService) {

                        this.samlConfigurationService = samlServiceBuilder.buildSamlConfigurationService();
                        this.samlConfigurationService.initService(
                                Map.of(SamlConfigurationService.DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY, IDP_FILE_PATH));
                    }
                }
            } else {

                Logger.error(this.getClass(), "OSGI Framework may be not initialized, couldn't get the Saml Configuration");
            }
        }

        return this.samlConfigurationService;
    }

    /**
     * Retrieve the authentication service, this is the proxy with the SAML Osgi bundle and must exists at least one host configurated with SAML in order to init this service.
     * @return SamlAuthenticationService
     */
    public SamlAuthenticationService samlAuthenticationService() {

        if (this.isAnyHostConfiguredAsSAML()) {

            if (null == this.samlAuthenticationService) {

                final SamlServiceBuilder samlServiceBuilder = this.samlServiceBuilder();

                if (null != samlServiceBuilder) {

                    synchronized (this) {

                        if (null == this.samlAuthenticationService) {

                            this.samlAuthenticationService =
                                    this.samlServiceBuilder.buildAuthenticationService(this.identityProviderConfigurationFactory(),
                                            VelocityUtil.getEngine(), this.messageObserver(), this.samlConfigurationService());

                            Logger.info(this, "Initing SAML Authentication");
                            samlAuthenticationService.initService(Collections.emptyMap());
                        }
                    }
                } else {

                    Logger.error(this.getClass(), "OSGI Framework may be not initialized, couldn't get the Saml Configuration");
                }
            }

            return this.samlAuthenticationService;
        }

        throw new DotSamlException("There is no SAML Configuration for this host");
    }

    /**
     * Returns true is any host is configured as a SAML
     * @return boolean
     */
    public boolean isAnyHostConfiguredAsSAML () {

        boolean isAnyConfigured = false;
        final User user         = APILocator.systemUser();

        final Optional<AppDescriptor> appDescriptorOptional = Try.of(
                ()-> this.appsAPI
                        .getAppDescriptor(SAML_APP_CONFIG_KEY, user)).getOrElseGet(e-> Optional.empty());
        if (appDescriptorOptional.isPresent()) {

            final AppDescriptor appDescriptor = appDescriptorOptional.get();

            final Map<String, Set<String>>  appKeysByHost = Try.of(()-> this.appsAPI.appKeysByHost())
                    .getOrElseGet(e -> Collections.emptyMap());
            final Set<String> sitesWithConfigurations     = this.appsAPI
                    .filterSitesForAppKey(appDescriptor.getKey(), appKeysByHost.keySet(), user);

            isAnyConfigured                   = !sitesWithConfigurations.isEmpty();
        }

        return isAnyConfigured;
    }

}
