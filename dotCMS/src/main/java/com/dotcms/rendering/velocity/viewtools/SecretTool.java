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

	/**
	 * Gets a secret as an object|string, based on the current host (if configured)
	 * @param key String
	 * @return Object
	 */
	public Object get(final String key) {

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(request);
		return config.isPresent()? config.get().getStringOrNull(key) : null;
	}

	/**
	 * Gets a secret as an object|string, based on the system host (if configured)
	 * @param key String
	 * @return Object
	 */
	public Object getSystemSecret (final String key) {

		return getSystemSecret(key, null);
	}

	/**
	 * Gets a secret as an object|string, based on the system host (if configured)
	 * If not present, returns the default value
	 * @param key String
	 * @param defaultValue Object
	 * @return Object
	 */
	public Object getSystemSecret (final String key,
								   final Object defaultValue) {

		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getStringOrNull(key, null!= defaultValue? defaultValue.toString():null) : defaultValue;
	}

	public char[] getCharArray(final String key) {

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(request);
		return config.isPresent()? config.get().getCharArrayOrNull(key) : null;
	}

	public char[] getCharArraySystemSecret (final String key) {

		return getCharArraySystemSecret(key, null);
	}

	public char[] getCharArraySystemSecret (final String key,
								   final char[] defaultValue) {

		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getCharArrayOrNull(key, defaultValue) : defaultValue;
	}
}
