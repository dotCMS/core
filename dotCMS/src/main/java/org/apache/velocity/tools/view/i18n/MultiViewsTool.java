/*
 * Copyright 2003 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.velocity.tools.view.i18n;


import java.util.Locale;
import javax.servlet.ServletContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Allows for transparent content negotiation in a manner mimicking
 * Apache httpd's <a
 * href="http://httpd.apache.org/docs-2.0/content-negotiation.html">MultiViews</a>.
 *
 * <p>Reads the default language out of the ViewContext as
 * <code>org.apache.velocity.tools.view.i18n.defaultLanguage</code>.
 * See {@link #findLocalizedResource(String, String)} and {@link
 * #findLocalizedResource(String, Locale)} for usage.</p>
 *
 * @version $Id: MultiViewsTool.java 72113 2004-11-11 06:22:45Z nbubna $
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 */
public class MultiViewsTool implements ViewTool
{
    /**
     * The key used to search initialization, context, and JVM
     * parameters for the default language to use.
     */
    protected static final String DEFAULT_LANGUAGE_KEY =
        "org.apache.velocity.tools.view.i18n.defaultLanguage";

    /**
     * The two character abbreviation for the request's default
     * language.
     */
    protected String defaultLanguage;

    protected VelocityEngine engine;

    /**
     * Creates a new uninitialized instance.  Call {@link #init} 
     * to initialize it.
     */
    public MultiViewsTool()
    {
    }

    /**
     * Extracts the default language from the specified
     * <code>ViewContext</code>, looking first at the Velocity
     * context, then the servlet context, then lastly at the JVM
     * default.  This "narrow scope to wide scope" pattern makes it
     * easy to setup language overrides at different levels within
     * your application.
     *
     * @param obj the current ViewContext
     * @throws IllegalArgumentException if the param is not a ViewContext
     */
    public void init(Object obj)
    {
        if (!(obj instanceof ViewContext))
        {
            throw new IllegalArgumentException("Tool can only be initialized with a ViewContext");
        }

        ViewContext context = (ViewContext)obj;
        Context vc = context.getVelocityContext();
        this.engine = context.getVelocityEngine();

        defaultLanguage = (String) vc.get(DEFAULT_LANGUAGE_KEY);
        if (defaultLanguage == null || defaultLanguage.trim().equals(""))
        {
            ServletContext sc = context.getServletContext();
            defaultLanguage = (String) sc.getAttribute(DEFAULT_LANGUAGE_KEY);
            if (defaultLanguage == null || defaultLanguage.trim().equals(""))
            {
                // Use JVM default.
                defaultLanguage = Locale.getDefault().getLanguage();
            }
        }
    }

    /**
     * Calls {@link #findLocalizedResource(String, String)} using the
     * language extracted from <code>locale</code>.
     *
     * @see #findLocalizedResource(String, String)
     */
    public String findLocalizedResource(String name, Locale locale)
    {
        return findLocalizedResource(name, locale.getLanguage());
    }

    /**
     * Calls {@link #findLocalizedResource(String, String)} using the
     * default language.
     *
     * @see #findLocalizedResource(String, String)
     */
    public String findLocalizedResource(String name)
    {
        return findLocalizedResource(defaultLanguage);
    }

    /**
     * <p>Finds the a localized version of the requested Velocity
     * resource (such as a file or template) which is most appropriate
     * for the locale of the current request.  Use in conjuction with
     * Apache httpd's <code>MultiViews</code>, or by itself.</p>
     *
     * <p>Usage from a template would be something like the following:
     * <blockquote><code><pre>
     * #parse ($multiviews.findLocalizedResource("header.vm", "en"))
     * #include ($multiviews.findLocalizedResource("my_page.html", "en"))
     * #parse ($multiviews.findLocalizedResource("footer.vm", "en"))
     * </pre></code></blockquote>
     *
     * You might also wrap this method using another pull/view tool
     * which does internationalization/localization/content negation
     * for a single point of access.</p>
     *
     * @param name The unlocalized name of the file to find.
     * @param language The language to find localized context for.
     * @return The localized file name, or <code>name</code> if it is
     * not localizable.
     */
    public String findLocalizedResource(String name, String language)
    {
        String localizedName = name + '.' + language;
        // templateExists() checks for static content as well
        if (!engine.templateExists(localizedName))
        {
            // Fall back to the default lanaguage.
            String defaultLangSuffix = '.' + defaultLanguage;
            if (localizedName.endsWith(defaultLangSuffix))
            {
                // Assume no localized version of the resource.
                localizedName = name;
            }
            else
            {
                localizedName = name + defaultLangSuffix;
                if (!engine.templateExists(localizedName))
                {
                    localizedName = name;
                }
            }
        }
        return localizedName;
    }

}
