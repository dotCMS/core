package com.dotcms.api.client;

import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import com.google.common.collect.ImmutableMap;
import io.quarkus.arc.DefaultBean;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryConfig.Mutable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class CredentialsManagerImpl implements CredentialsManager {

    private static final String COM_DOTCMS_API = "com.dotcms.api";

    @Override
    public void persist(final String service, final CredentialsBean credentials)
            throws IOException {

        final RegistriesConfig registriesConfig = RegistriesConfig.resolveConfig();
        final List<RegistryConfig> registries = registriesConfig.getRegistries();
        final Optional<RegistryConfig> registryConfig = registries.stream()
                .filter(cfg -> COM_DOTCMS_API.equals(cfg.getId()))
                .findFirst();
        if (registryConfig.isPresent()) {
            //Updating
            final RegistryConfig existing = registryConfig.get();
            final Map<String, Object> extra = existing.getExtra();
            @SuppressWarnings("unchecked") final Map<String, Object> servicesMap = (Map<String, Object>) extra.get(
                    "services");
            servicesMap.put(service, ImmutableMap.of("credentials", credentials));
            final Map<String, Object> updatedMap = new ImmutableMap.Builder<String, Object>().put(
                    "services", servicesMap).build();

            final Mutable mutable = existing.mutable();
            mutable.setExtra(updatedMap);

            final RegistriesConfig.Mutable registriesConfigMutable = registriesConfig.mutable();
            registriesConfigMutable.addRegistry(mutable);
            registriesConfigMutable.persist();

        } else {
            //Persisting for the first time
            final Mutable mutable = RegistryConfig.builder();
            mutable.setId(COM_DOTCMS_API);
            final Map<String, Object> newServicesMap = ImmutableMap.of("services",
                    ImmutableMap.of(service, ImmutableMap.of("credentials", credentials))
            );
            mutable.setExtra(newServicesMap);
            mutable.setEnabled(false);

            final RegistriesConfig.Mutable registriesConfigMutable = registriesConfig.mutable();
            registriesConfigMutable.addRegistry(mutable);
            registriesConfigMutable.persist();
        }
    }

    @Override
    public Map<String, ServiceBean> services() {
        return null;
    }

    @Override
    public void activate(String profileName) {

    }
}
