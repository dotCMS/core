package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.velocity.viewtools.SecretTool;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

/**
 * Wraps the SecretTool (dotsecrets) into the JS context.
 * @author jsanca
 */
public class SecretJsViewTool  implements JsViewTool, JsViewContextAware {

    private final SecretTool secretTool = new SecretTool();

    @Override
    public void setViewContext(final ViewContext viewContext) {

        secretTool.init(viewContext);
    }

    @Override
    public String getName() {
        return "dotsecrets";
    }

    @HostAccess.Export
    /**
     * Gets a secret as an object|string, based on the current host (if configured)
     * @param key String
     * @return Object
     */
    public Object get(final String key) {

        return this.secretTool.get(key);
    }

    @HostAccess.Export
    /**
     * Gets a secret as an object|string, based on the system host (if configured)
     * @param key String
     * @return Object
     */
    public Object getSystemSecret (final String key) {

        return this.secretTool.getSystemSecret(key);
    }

    @HostAccess.Export
    /**
     * Gets a secret as an object|string, based on the system host (if configured)
     * If not present, returns the default value
     * @param key String
     * @param defaultValue Object
     * @return Object
     */
    public Object getSystemSecret (final String key,
                                   final Object defaultValue) {

        return this.secretTool.getSystemSecret(key, defaultValue);
    }

    @HostAccess.Export
    public char[] getCharArray(final String key) {

        return this.secretTool.getCharArray(key);
    }

    @HostAccess.Export
    public char[] getCharArraySystemSecret (final String key) {

        return this.secretTool.getCharArraySystemSecret(key);
    }

    @HostAccess.Export
    public char[] getCharArraySystemSecret (final String key,
                                            final char[] defaultValue) {

        return this.secretTool.getCharArraySystemSecret(key, defaultValue);
    }
}
