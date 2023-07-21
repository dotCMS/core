package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Optional;

/**
 * This view tool expose the dot velocity secrets app to velocity
 * This allows to get configuration from the dotVelocitySecretApp
 * @author jsanca
 */
public class SecretTool implements ViewTool {

    private InternalContextAdapterImpl internalContextAdapter;
    private Context context;
	private HttpServletRequest request;

	@Override
	public void init(final Object initData) {

        final ViewContext context = (ViewContext) initData;
		this.request = context.getRequest();
        this.context = context.getVelocityContext();
	}

	/**
	 * Gets a secret as an object|string, based on the current host (if configured)
	 * @param key String
	 * @return Object
	 */
	public Object get(final String key) {

        if(!canUserEvaluate()){
            throw new SecurityException("External scripting is disabled in your dotcms instance.");
        }

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

        if(!canUserEvaluate()){
            throw new SecurityException("External scripting is disabled in your dotcms instance.");
        }
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getStringOrNull(key, null!= defaultValue? defaultValue.toString():null) : defaultValue;
	}

	public char[] getCharArray(final String key) {

        if(!canUserEvaluate()){
            throw new SecurityException("External scripting is disabled in your dotcms instance.");
        }
		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(request);
		return config.isPresent()? config.get().getCharArrayOrNull(key) : null;
	}

	public char[] getCharArraySystemSecret (final String key) {

		return getCharArraySystemSecret(key, null);
	}

	public char[] getCharArraySystemSecret (final String key,
								   final char[] defaultValue) {

        if(!canUserEvaluate()){
            throw new SecurityException("External scripting is disabled in your dotcms instance.");
        }
		final Optional<DotVelocitySecretAppConfig> config = DotVelocitySecretAppConfig.config(APILocator.systemHost());
		return config.isPresent()? config.get().getCharArrayOrNull(key, defaultValue) : defaultValue;
	}

    private static final boolean ENABLE_SCRIPTING = Config.getBooleanProperty("ENABLE_SCRIPTING", false);

    /**
     * Test 2 things.
	 * 1) see if the user has the scripting role
	 * 2) otherwise check if the last modified user has the scripting role
     * @return boolean
     */
    protected boolean canUserEvaluate() {

        if(!ENABLE_SCRIPTING) {

            Logger.warn(this.getClass(), "Scripting called and ENABLE_SCRIPTING set to false");
            return false;
        }

        try {

			boolean hasScriptingRole = false;
			final User user = WebAPILocator.getUserWebAPI().getUser(this.request);
			final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
			// try with the current user
			if (null != user) {

				hasScriptingRole = APILocator.getRoleAPI().doesUserHaveRole(user, scripting);
			}

			if (!hasScriptingRole) {

				this.internalContextAdapter    = new InternalContextAdapterImpl(context);
				final String fieldResourceName = this.internalContextAdapter.getCurrentTemplateName();
				final String contentletFileAssetInode = fieldResourceName.substring(fieldResourceName.indexOf("/") + 1, fieldResourceName.indexOf("_"));
				final Contentlet contentlet = APILocator.getContentletAPI().find(contentletFileAssetInode, APILocator.systemUser(), true);
				final User lastModifiedUser = APILocator.getUserAPI().loadUserById(contentlet.getModUser(), APILocator.systemUser(), true);
				hasScriptingRole = APILocator.getRoleAPI().doesUserHaveRole(lastModifiedUser, scripting);
			}

			return hasScriptingRole;
        } catch(Exception e){
            Logger.warn(this.getClass(), "Scripting called with error" + e);
            return false;
        }
    } // canUserEvaluate.
}
