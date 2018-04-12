package com.dotcms.exception;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotcms.rest.exception.ValidationException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.business.NotAllowedUserWorkflowException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exception Utils
 * @author andrecurione
 */
public class ExceptionUtil {

    public static final Set<Class<? extends Throwable>> SECURITY_EXCEPTIONS = ImmutableSet
            .of(SecurityException.class, DotSecurityException.class,
                    NotAllowedUserWorkflowException.class);

    public static final Set<Class<? extends Throwable>> NOT_FOUND_EXCEPTIONS = ImmutableSet
            .of(NotFoundInDbException.class, DoesNotExistException.class);

    public static final Set<Class<? extends Throwable>> BAD_REQUEST_EXCEPTIONS = ImmutableSet
            .of(AlreadyExistException.class, IllegalArgumentException.class, ValidationException.class);


    private ExceptionUtil () {}

    /**
     * Returns true if the Throwable is instance or contains a cause of the specified ExceptionClass
     * @param e
     * @param exceptionClass
     * @return boolean
     */
    public static boolean causedBy(final Throwable e, final Class <? extends Throwable> exceptionClass) {

        Throwable t = e;
        while (t != null) {
            if (t.getClass().equals(exceptionClass)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * Returns true if the Throwable is instance or contains a cause of the specified on any of the exceptionClasses
     * @param e
     * @param exceptionClasses
     * @return boolean
     */

    public static boolean causedBy(final Throwable e, final Set<Class<? extends Throwable>> exceptionClasses) {

        Throwable t = e;
        while (t != null) {
            if (exceptionClasses.contains(t.getClass())) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     *
     * @param e
     * @param exceptionClasses
     * @return
     */
    public static boolean causedBy(final Throwable e, final Class<? extends Throwable>... exceptionClasses) {

        return causedBy(e, new HashSet<>(Arrays.asList(exceptionClasses)));
    }


    public static String getLocalizedMessageOrDefault(final User user, final String key, final String defaultMessage, final Class clazz){
        String message = defaultMessage;
        try {
            message = LanguageUtil.get(user, key);
        }catch(Exception e){
            if(clazz != null){
              Logger.error(clazz, e.toString());
            }
        }
        return message;
    }


    public static String toString(final DotContentletValidationException ex) {
        final Map<String, List<Field>> errors = ex.getNotValidFields();
        final Set<String> keys = errors.keySet();
        final StringBuilder sb = new StringBuilder();
        if(!errors.isEmpty()) {
            String title = getLocalizedMessageOrDefault(null,
                    "message.contentlet.fields.validation",
                    "Content validatating exception fields: ", null);
            sb.append(title).
                    append("[");
            for (final String key : keys) {
                final Iterator<Field> fields = errors.get(key).iterator();
                if (fields.hasNext()) {
                    while (fields.hasNext()) {
                        final Field field = fields.next();
                        sb.append(field.getFieldName());
                        if (fields.hasNext()) {
                            sb.append(", ");
                        }
                    }
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

}
