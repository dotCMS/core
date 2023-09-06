package com.dotcms.config;

import static io.smallrye.config.SecretKeys.doLocked;

import com.liferay.util.StringPool;
import io.smallrye.config.ConfigLogging;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import javax.annotation.Priority;

/**
 * If the direct property cannot be found this interceptor will look for a property with the name "DOT_" + name.toUpperCase().replace(".", "_")
 * and if it exists will return it.
 *
 * @author nolly
 */
@Priority(io.smallrye.config.Priorities.APPLICATION)
public class DotEnvConfigSourceInterceptor implements ConfigSourceInterceptor {

    public static final String ENV_PREFIX = "DOT_";

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {

        ConfigValue configValue = doLocked(() -> context.proceed(name));
        if (configValue != null) {
            ConfigLogging.log.lookup(configValue.getName(), configValue.getLocation(), configValue.getValue());
        } else {
            ConfigLogging.log.notFound(name);
        }
        if (configValue==null) {
            configValue = context.proceed(ENV_PREFIX + name.toUpperCase().replace(StringPool.PERIOD, StringPool.UNDERLINE));
        }
        return configValue;
    }
}
