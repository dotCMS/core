package com.dotcms.rendering.velocity.viewtools.secrets;

import com.dotcms.security.apps.Secret;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread local holder for the dot secrets
 * this approach just avoids to load the secrets more than one in the same request
 * @author jsanca
 */
public class DotVelocitySecretAppConfigThreadLocal implements Serializable {

    private static final long serialVersionUID = 1L;

    private static ThreadLocal<Map<String, DotVelocitySecretAppConfig>> configLocal = new ThreadLocal<>();

    public static final DotVelocitySecretAppConfigThreadLocal INSTANCE = new DotVelocitySecretAppConfigThreadLocal();

    /**
     * Get the request from the current thread
     *
     * @return {@link DotVelocitySecretAppConfig}
     */
    public Optional<DotVelocitySecretAppConfig> getConfig(final String siteId) {

        this.init();
        return Optional.ofNullable(configLocal.get().get(siteId));
    }

    public void setConfig(final String siteId, final Optional<DotVelocitySecretAppConfig> config) {

        this.init();
        configLocal.get().put(siteId, null != config && config.isPresent()? config.get(): null);
    }

    private void init () {

        if (null == configLocal.get()) {
            configLocal.set(new ConcurrentHashMap<>());
        }
    }

    public void clearConfig() {

        if (null != configLocal.get()) {

            final Map<String, DotVelocitySecretAppConfig> map = configLocal.get();
            for (final String key : map.keySet()) {

                DotVelocitySecretAppConfig secrets = map.get(key);
                secrets.destroySecrets();
                secrets = null;
                map.remove(key);
            }
            map.clear();
        }
        configLocal.remove();
    }
}