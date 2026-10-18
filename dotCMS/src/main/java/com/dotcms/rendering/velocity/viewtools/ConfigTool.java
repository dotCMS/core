package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.Arrays;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Exposes configuration properties to Velocity as {@code $config}.
 * <p>
 * Properties whose names look like credentials, keys or internal locations (see
 * {@link #RESTRICTED_KEYS_PROPERTY}) are only returned when the author of the code being rendered
 * or the current user is a CMS Administrator or has the Scripting Developer role; otherwise the
 * default value is returned. See {@link RestrictedViewToolAccess}.
 */
public class ConfigTool implements ViewTool {

    /**
     * Comma-separated list of name fragments. A property whose normalized name (upper case,
     * {@code .} and {@code -} replaced with {@code _}) contains any of them is restricted.
     */
    public static final String RESTRICTED_KEYS_PROPERTY = "VELOCITY_CONFIG_TOOL_RESTRICTED_KEYS";

    private static final String[] DEFAULT_RESTRICTED_KEYS = {
            "PASSWORD", "PASSWD", "PWD", "_PW", "SECRET", "TOKEN", "CREDENTIAL", "PRIVATE", "SALT",
            "KEY", "AUTH", "CERT", "USER", "JWT", "DB_", "DATASOURCE", "CLUSTER_ID", "REAL_PATH",
            "DOTSECURE", "_PATH", "_DIR", "FOLDER", "SMTP", "MAIL_", "ENDPOINT", "URL", "HOST"};

    private Context context;
    private HttpServletRequest request;

    @Override
    public void init(final Object initData) {
        if (initData instanceof ViewContext) {
            final ViewContext viewContext = (ViewContext) initData;
            this.context = viewContext.getVelocityContext();
            this.request = viewContext.getRequest();
        }
    }

    /**
     * @param name The property name.
     *
     * @return {@code true} if the property name matches one of the restricted name fragments.
     */
    public static boolean isRestrictedKey(final String name) {
        if (null == name) {
            return true;
        }
        final String normalized = normalizeKey(name);
        final String[] fragments = Config.getStringArrayProperty(RESTRICTED_KEYS_PROPERTY,
                DEFAULT_RESTRICTED_KEYS);
        return null != fragments && Arrays.stream(fragments)
                .map(ConfigTool::normalizeKey)
                .filter(fragment -> !fragment.isEmpty())
                .anyMatch(normalized::contains);
    }

    static String normalizeKey(final String key) {
        return null == key ? "" : key.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }

    /**
     * @return {@code true} if the given property can be read in the current rendering context.
     */
    protected boolean canRead(final String name) {
        if (!isRestrictedKey(name)) {
            return true;
        }
        final HttpServletRequest currentRequest = null != this.request
                ? this.request : HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (RestrictedViewToolAccess.hasAccess(this.context, currentRequest)) {
            return true;
        }
        Logger.debug(ConfigTool.class.getName(),
                () -> "Property '" + name + "' is not available in this rendering context");
        return false;
    }

    public String getStringProperty(String name, String defValue) {
        return canRead(name) ? Config.getStringProperty(name, defValue) : defValue;
    }

    public String getStringProperty(String name) {
        return this.getStringProperty(name, null);
    }

    public String[] getStringArrayProperty(String name) {
        return canRead(name) ? Config.getStringArrayProperty(name) : null;
    }

    public int getIntProperty(String name) {
        return this.getIntProperty(name, 0);
    }

    public int getIntProperty(String name, int defValue) {
        return canRead(name) ? Config.getIntProperty(name, defValue) : defValue;
    }

    public long getLongProperty(String name) {
        return this.getLongProperty(name, 0);
    }

    public long getLongProperty(String name, int defValue) {
        return canRead(name) ? Config.getLongProperty(name, defValue) : defValue;
    }

    public float getFloatProperty(String name) {
        return this.getFloatProperty(name, 0f);
    }


    public float getFloatProperty(String name, float defaultVal) {
        return canRead(name) ? Config.getFloatProperty(name, defaultVal) : defaultVal;
    }


    public boolean getBooleanProperty(String name) {
        return this.getBooleanProperty(name, false);
    }

    public boolean getBooleanProperty(String name, boolean defaultVal) {
        return canRead(name) ? Config.getBooleanProperty(name, defaultVal) : defaultVal;
    }
}
