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
import com.liferay.portal.model.User;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public abstract class WebDotcmsException extends RuntimeException{

    public  WebDotcmsException (User user, String propertiesKey, String... arguments){
        this( user, null, propertiesKey, arguments );

    }

    public  WebDotcmsException (String propertiesKey, String... arguments){
        this( null, null, propertiesKey, arguments );

    }

    public  WebDotcmsException (Throwable cause, String propertiesKey, String... arguments){
        super( getFomatedMessage(null, propertiesKey, arguments), cause);

    }

    public  WebDotcmsException (User user, Throwable cause, String propertiesKey, String... arguments){
        super( getFomatedMessage(user, propertiesKey, arguments), cause);

    }

    private static String getFomatedMessage(User user, String propertiesKey, String[] arguments) {
        String message;

        try {
            if (user == null) {
                HttpServletRequest req = WebContextFactory.get().getHttpServletRequest();
                Locale locale = req.getLocale();
                message = LanguageUtil.get(locale, propertiesKey);
            }else{
                message = LanguageUtil.get(user, propertiesKey);
            }

            return MessageFormat.format(message, arguments);
        } catch (LanguageException e) {
            Logger.error(WebDotcmsException.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
