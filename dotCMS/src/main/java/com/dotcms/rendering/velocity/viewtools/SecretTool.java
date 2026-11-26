package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * This view tool expose the dot velocity secrets app to velocity
 * This allows to get configuration from the dotVelocitySecretApp
 * @author jsanca
 */
public class SecretTool implements ViewTool {

	private HttpServletRequest request;

	@Override
	public void init(final Object initData) {

		if (null != initData && initData instanceof ViewContext) {
			final ViewContext context = (ViewContext) initData;
			this.request = context.getRequest();
		} else {

			this.request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		}
	}

	/**
	 * Gets a secret as an object|string, based on the current host (if configured)
	 * @param key String
	 * @return Object
	 */
	public Object get(final String key) {

		canUserEvaluate();

		final HttpServletRequest requestFromThreadLocal = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final Optional<DotVelocitySecretAppConfig> config = resolveConfig(requestFromThreadLocal);
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

		canUserEvaluate();
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getStringOrNull(key, null!= defaultValue? defaultValue.toString():null) : defaultValue;
	}

	public char[] getCharArray(final String key) {

		canUserEvaluate();
		final HttpServletRequest requestFromThreadLocal = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final Optional<DotVelocitySecretAppConfig> config = resolveConfig(requestFromThreadLocal);
		return config.isPresent()? config.get().getCharArrayOrNull(key) : null;
	}

	public char[] getCharArraySystemSecret (final String key) {

		return getCharArraySystemSecret(key, null);
	}

	public char[] getCharArraySystemSecret (final String key,
								   final char[] defaultValue) {

		canUserEvaluate();
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getCharArrayOrNull(key, defaultValue) : defaultValue;
	}

    private static final boolean ENABLE_SCRIPTING = Config.getBooleanProperty("secrets.scripting.enabled", true);

	/**
	 * Checks for the existence of the mandatory Scripting Role based on the following criteria:
	 * <ol>
	 *     <li>The User who authored the code being rendered -- announced on the request by the
	 *     renderer, never derived from the Velocity resource name -- has the Scripting Role.</li>
	 *     <li>The User present in the HTTP Request has the Scripting Role assigned to it.</li>
	 * </ol>
	 *
	 * @throws SecurityException The User that either added the Secrets ViewTool code or the User
	 *                           present in the HTTP Request does not have the required Role.
	 */
    protected void canUserEvaluate() {
		final String disabledScriptingErrorMsg = "External scripting is disabled in your dotcms instance";
        if (!ENABLE_SCRIPTING) {
            Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
			throw new SecurityException(disabledScriptingErrorMsg);
        }

        try {
			final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
			boolean hasScriptingRole = RestrictedViewToolAccess.renderingAuthorHasAccess(this.request);
			if (!hasScriptingRole) {
				final User user = WebAPILocator.getUserWebAPI().getUser(this.request);
				// try with the current user
				if (null != user) {
					hasScriptingRole = APILocator.getRoleAPI().doesUserHaveRole(user, scripting);
				}
			}

			if (!hasScriptingRole) {
				throw new SecurityException(disabledScriptingErrorMsg);
			}
        } catch (final Exception e) {
			Logger.warnAndDebug(this.getClass(), String.format("Failed to evaluate Scripting Role " +
					"presence: %s", ExceptionUtil.getErrorMessage(e)), e);
			throw new SecurityException(disabledScriptingErrorMsg, e);
        }
    } // canUserEvaluate.

	/**
	 * Resolves the {@link DotVelocitySecretAppConfig} for the current execution context. File-based
	 * scripting endpoints use the site captured with the scripting file, ignoring caller-selected
	 * host parameters; other contexts use the given request.
	 *
	 * @param requestFromThreadLocal the current request
	 * @return the resolved config, or {@link java.util.Optional#empty()} if none is configured
	 */
	private Optional<DotVelocitySecretAppConfig> resolveConfig(final HttpServletRequest requestFromThreadLocal) {
		if (null != requestFromThreadLocal) {
			final String siteId = ScriptingFileAttribute.getSiteId(requestFromThreadLocal);
			if (siteId != null) {
				try {
					final Host site = APILocator.getHostAPI().find(siteId, APILocator.systemUser(), false);
					if (site == null) {
						throw new SecurityException("Scripting site not found");
					}
					return DotVelocitySecretAppConfig.config(site);
				} catch (final DotDataException | DotSecurityException e) {
					throw new SecurityException("Unable to resolve scripting site", e);
				}
			}
		}
		return DotVelocitySecretAppConfig.config(requestFromThreadLocal);
	}

}
