package com.dotcms.auth.providers.saml.v1;

import com.dotcms.business.SystemCache;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * On previous SAML versions, the SAML Id was an uuid, on this version the SAML Id is the site identifier, 
 * due to this change, we need to map the old SAML Id to the new one, this interface is used to do that mapping.
 * @author jsanca 
 */
public class OldSAMLConfigToSiteSAMLConfigMapping implements SiteConfigSAMLMapping {

    private static class SingletonHolder {
        private static final OldSAMLConfigToSiteSAMLConfigMapping INSTANCE = new OldSAMLConfigToSiteSAMLConfigMapping();
    }
    /**
     * Get the instance.
     * @return OldSAMLConfigToSiteSAMLConfigMapping
     */
    public static OldSAMLConfigToSiteSAMLConfigMapping getInstance() {

        return OldSAMLConfigToSiteSAMLConfigMapping.SingletonHolder.INSTANCE;
    } // getInstance.


    private SystemCache systemCache = CacheLocator.getSystemCache();
    @Override
    public String apply(final String otherConfigId, final IdentityProviderConfigurationFactory identityProviderConfigurationFactory) {

        String siteId = otherConfigId;
        final Map<String, String> samlConfigMap = getConfigMap(identityProviderConfigurationFactory);
        if (samlConfigMap.containsKey(otherConfigId)) {
            siteId = samlConfigMap.get(otherConfigId);
        }
        return siteId;
    }
    
    private Map<String, String> getConfigMap(final IdentityProviderConfigurationFactory identityProviderConfigurationFactory) {
        Map<String, String> configMap = (Map<String, String>) systemCache.get("SAML_CONFIG_MAP");
        if (null == configMap) {
            configMap = Try.of(()->initSAMLConfigMap(identityProviderConfigurationFactory)).getOrElse(Collections.emptyMap()) ;
        }
        return configMap;
    }

    /**
     * Look for on all available configurations the oldSAMLConfigId
     * @param identityProviderConfigurationFactory
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private synchronized Map<String, String> initSAMLConfigMap(final IdentityProviderConfigurationFactory identityProviderConfigurationFactory) throws DotDataException, DotSecurityException {

        final Map<String, String> configMap = new HashMap<>();
        APILocator.getHostAPI().findAllFromDB(APILocator.systemUser(), false).forEach(host -> {
            final String hostIdentifier = host.getIdentifier();
            IdentityProviderConfiguration identityProviderConfiguration = identityProviderConfigurationFactory
                    .findIdentityProviderConfigurationById(hostIdentifier);
            if (null != identityProviderConfiguration && identityProviderConfiguration.isEnabled() &&
                    identityProviderConfiguration.containsOptionalProperty("oldSAMLConfigId")) {

                configMap.put(hostIdentifier, identityProviderConfiguration.getOptionalProperty(
                        "oldSAMLConfigId").toString());
            }
        });

        if (!configMap.isEmpty()) {
            systemCache.put("SAML_CONFIG_MAP", configMap);
        }

        return configMap;
    }
}
