package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
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

	private Context context;
	private HttpServletRequest request;

	@Override
	public void init(final Object initData) {

		if (null != initData && initData instanceof ViewContext) {
			final ViewContext context = (ViewContext) initData;
			this.request = context.getRequest();
			this.context = context.getVelocityContext();
		} else {

			this.request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
			this.context = VelocityUtil.getInstance().getContext(request, HttpServletResponseThreadLocal.INSTANCE.getResponse());
		}
	}

	/**
	 * Gets a secret as an object|string, based on the current host (if configured)
	 * @param key String
	 * @return Object
	 */
	public Object get(final String key) {

		canUserEvaluate();

		final Optional<DotVelocitySecretAppConfig> config = resolveConfig();
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
		final Optional<DotVelocitySecretAppConfig> config = resolveConfig();
		return config.isPresent()? config.get().getCharArrayOrNull(key) : null;
	}

	/**
	 * Resolves the {@link DotVelocitySecretAppConfig} for the current execution context.
	 * <p>
	 * Uses {@code this.request}, which is snapshotted at {@link #init} time — before any VTL
	 * executes — making it immune to {@code #set($host = ...)} mutations.
	 * <ul>
	 *   <li><b>Velocity page rendering:</b> {@code init(ViewContext)} sets {@code this.request}
	 *       from the real HTTP request, so host resolution matches the browser's site.</li>
	 *   <li><b>Workflow actionlet (HTTP or background):</b> the actionlet builds a mock request
	 *       scoped to the contentlet's own site and passes it to {@code engine.eval()}, which
	 *       propagates it here via {@code init(ViewContext)}. This ensures secrets resolve for
	 *       the contentlet's site regardless of which site the triggering browser is on.</li>
	 * </ul>
	 *
	 * @return the resolved config, or {@link java.util.Optional#empty()} if none is configured
	 */
	private Optional<DotVelocitySecretAppConfig> resolveConfig() {

		if (null == this.request) {
			return Optional.empty();
		}
		return DotVelocitySecretAppConfig.config(this.request);
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
	 *     <li>The User that last modified the Contentlet rendering the Secrets has the Scripting
	 *     Role.</li>
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
			boolean hasScriptingRole = checkRoleFromLastModUser(scripting);
			if (!hasScriptingRole) {
				User user = WebAPILocator.getUserWebAPI().getUser(this.request);

				if (null == user || user.isAnonymousUser()) {
					final User contextUser = (User) this.context.get("user");
					if (null != contextUser) {
						user = contextUser;
					}
				}

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
	 * Checks whether the User who last edited the Contentlet rendering the Secrets has the
	 * specified dotCMS Role or not.
	 *
	 * @param role The {@link Role} that will be checked.
	 *
	 * @return If the User has the specified Role, returns {@code true}.
	 */
	private boolean checkRoleFromLastModUser(final Role role) {
		final InternalContextAdapterImpl internalContextAdapter = new InternalContextAdapterImpl(context);
		final String resourcePath = internalContextAdapter.getCurrentTemplateName();
		boolean hasRole = false;
		if (UtilMethods.isSet(resourcePath)) {
			String contentletInode = StringPool.BLANK;
			try {

				contentletInode = CMSUrlUtil.getInstance().getInodeFromUrlPath(resourcePath);
				Versionable versionable = APILocator.getContentletAPI().find(contentletInode, APILocator.systemUser(), true);

				if (null == versionable) {
					versionable = APILocator.getContainerAPI().getLiveContainerById(contentletInode, APILocator.systemUser(), true);
				}

				if (null == versionable) {
					versionable = APILocator.getTemplateAPI().findLiveTemplate(contentletInode, APILocator.systemUser(), true);
				}

				if (null != versionable) {
					final User lastModifiedUser = APILocator.getUserAPI().loadUserById(versionable.getModUser(), APILocator.systemUser(), true);
					hasRole = APILocator.getRoleAPI().doesUserHaveRole(lastModifiedUser, role);
				}
			} catch (final Exception e) {
				Logger.warnAndDebug(SecretTool.class, String.format("Failed to find last " +
						"modification user from Retrieved ID '%s' in URL Path '%s': %s",
						contentletInode, resourcePath,
						ExceptionUtil.getErrorMessage(e)), e);
			}
		}
		return hasRole;
	}

}
