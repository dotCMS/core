package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfig;
import com.dotmarketing.business.APILocator;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * This view tool expose the dot velocity secrets app to velocity
 * This allows to get configuration from the dotVelocitySecretApp
 * @author jsanca
 */
public class SecretTool implements ViewTool {

	@Override
	public void init(Object initData) {
	}

	public Object get(final String key) {

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		Object value = null;

		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(request);
		return config.isPresent() && config.get().extraParameters.containsKey(key)?
			config.get().extraParameters.get(key): null;
	}

	public Object getSystemSecret (final String key) {

		return getSystemSecret(key, null);
	}

	public Object getSystemSecret (final String key,
								   final Object defaultValue) {

		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent() && config.get().extraParameters.containsKey(key)?
				config.get().extraParameters.get(key) : defaultValue;
	}
}
