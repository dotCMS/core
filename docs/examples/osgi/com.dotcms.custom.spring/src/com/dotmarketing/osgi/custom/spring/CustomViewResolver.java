package com.dotmarketing.osgi.custom.spring;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * Created by Jonathan Gamba
 * Date: 4/15/13
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