package com.dotmarketing.osgi.custom.spring;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * Copy of the dotCMS ViewResolver {@link com.dotcms.spring.web.DotViewResolver} in order
 * to use our custom spring version and not the one use it by dotCMS.
 *
 * @author Jonathan Gamba
 *         Date: 4/15/13
 * @see com.dotcms.spring.web.DotViewResolver
 */
public class CustomViewResolver implements ViewResolver {

    String prefix;
    String suffix;

    public void setPrefix ( String prefix ) {
        this.prefix = prefix;
    }

    public void setSuffix ( String suffix ) {
        this.suffix = suffix;
    }

    public View resolveViewName ( String path, Locale locale ) throws Exception {

        if ( !path.startsWith( "redirect:" ) ) {
            path = (prefix != null)
                    ? prefix + path
                    : path;

            path = (suffix != null)
                    ? path + suffix
                    : path;
        }

        return new CustomView( path );
    }

}