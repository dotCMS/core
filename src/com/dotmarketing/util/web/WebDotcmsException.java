package com.dotmarketing.util.web;

import com.dotcms.repackage.uk.ltd.getahead.dwr.WebContextFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import org.apache.velocity.runtime.resource.Resource;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public abstract class WebDotcmsException extends RuntimeException{

    public  WebDotcmsException (String propertiesKey, String... arguments){
        this( null, propertiesKey, arguments );

    }

    public  WebDotcmsException (Throwable cause, String propertiesKey, String... arguments){
        super( getFomatedMessage(propertiesKey, arguments), cause);

    }

    private static String getFomatedMessage(String propertiesKey, String[] arguments) {
        HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
        Locale locale = req.getLocale();

        try {
            String message = LanguageUtil.get(locale, propertiesKey);
            return MessageFormat.format(message, arguments);

        } catch (LanguageException e) {
            Logger.error(WebDotcmsException.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
