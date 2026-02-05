package com.dotcms.exception;

import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_BADTYPE;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_BAD_CARDINALITY;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_CHAR_LIMIT;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_BAD_REL;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_INVALID_REL_CONTENT;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_PATTERN;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_REQUIRED;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_REQUIRED_REL;
import static com.dotmarketing.portlets.contentlet.business.DotContentletValidationException.VALIDATION_FAILED_UNIQUE;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.config.DotServiceLocatorImpl.QuietServiceShutdownException;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.exceptions.DuplicateFileException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.DuplicateRoleKeyException;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.DotBinaryFieldException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.DotDateFieldException;
import com.dotmarketing.portlets.contentlet.business.DotJsonFieldException;
import com.dotmarketing.portlets.contentlet.business.DotNumericFieldException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.portlets.folders.business.AddContentToFolderPermissionException;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowPortletAccessException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.DuplicateGroupException;
import com.liferay.portal.DuplicateRoleException;
import com.liferay.portal.DuplicateUserEmailAddressException;
import com.liferay.portal.DuplicateUserIdException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.jvnet.mimepull.MIMEParsingException;

/**
 * This utility class provides useful methods to extract and process different pieces of information
 * from Java Exceptions that may be thrown by the application.
 *
 * @author andrecurione
 * @since Jan 30th, 2018
 */
public class ExceptionUtil {

    public static final Set<Class<? extends Throwable>> DUPLICATE_EXCEPTIONS = Set
            .of(
                    DotDuplicateDataException.class,
                    DuplicateFileException.class,
                    DuplicateGroupException.class,
                    DuplicateRoleException.class,
                    com.dotmarketing.business.DuplicateRoleException.class,
                    DuplicateRoleKeyException.class,
                    DuplicateUserEmailAddressException.class,
                    DuplicateUserException.class,
                    DuplicateUserIdException.class
            );

    public static final Set<Class<? extends Throwable>> SECURITY_EXCEPTIONS = Set
            .of(
                    DotSecurityException.class,
                    InvalidLicenseException.class,
                    WorkflowPortletAccessException.class,
                    AddContentToFolderPermissionException.class
            );

    public static final Set<Class<? extends Throwable>> NOT_FOUND_EXCEPTIONS = Set
            .of(
                    NotFoundInDbException.class,
                    DoesNotExistException.class
            );

    public static final Set<Class<? extends Throwable>> MALFORMED_MULTIPART_EXCEPTIONS = Set
            .of(
                    MIMEParsingException.class
            );

    public static final Set<Class<? extends Throwable>> BAD_REQUEST_EXCEPTIONS = Set
            .of(
                    AlreadyExistException.class,
                    IllegalArgumentException.class,
                    IllegalStateException.class,
                    DotStateException.class,
                    DotContentletStateException.class,
                    DotDataValidationException.class,
                    DotJsonFieldException.class,
                    DotNumericFieldException.class,
                    DotDateFieldException.class,
                    DotBinaryFieldException.class,
                    ValidationException.class,
                    BadRequestException.class,
                    JsonProcessingException.class,
                    NumberFormatException.class,
                    InvalidFolderNameException.class
            );


    //These are exceptions we want to log but not bubble up to the user
    public static final Set<Class<? extends Throwable>> LOUD_MOUTH_EXCEPTIONS = Set.of(
            QuietServiceShutdownException.class
    );

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
     * Iterate through the entire cause chain of the exception until an instance of the specified exceptionClass is found
     *
     * @param exception thrown
     * @param exceptionClass Exception class to look for
     * @return An Optional with the Exception found if it exists otherwise return an empty Optional
     */
    public static Optional<Throwable> get(final Throwable exception, final Class <? extends Throwable> exceptionClass) {

        Throwable t = exception;
        while (t != null) {
            if (t.getClass().equals(exceptionClass)) {
                return Optional.of(t);
            }
            t = t.getCause();
        }
        return Optional.empty();
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

    /**
     *
     * @param t
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotRuntimeException
     */
    public static void bubbleUpException(final Throwable t)
            throws DotDataException, DotSecurityException, DotRuntimeException {

        if (t instanceof DotDataException) {
            throw (DotDataException) t;
        }
        if (t instanceof DotSecurityException) {
            throw (DotSecurityException) t;
        }
        if (t instanceof DotContentletValidationException) {
            throw (DotContentletValidationException) t;
        }
        if (t instanceof DotContentletStateException) {
            throw (DotContentletStateException) t;
        }
        if (t instanceof DotWorkflowException) {
            throw (DotWorkflowException) t;
        }

        throw new DotRuntimeException(t.getMessage(), t);
    }

    /**
     * Returns the error message from the specified Java exception. If it's not present, returns the
     * class name.
     *
     * @param throwable The thrown exception.
     *
     * @return The exception's error message, or its class name if not available.
     */
    public static String getErrorMessage(final Throwable throwable) {
        if (null == throwable) {
            return StringPool.BLANK;
        }
        return UtilMethods.isSet(throwable.getMessage()) ? throwable.getMessage() :
                throwable.getClass().getName();
    }

    /**
     * Return an optional throwable if one of them match exactly the class name of the set exceptions
     * Empty optional if not match any
     * @param e
     * @param exceptionClasses
     * @return
     */
    public static Optional<Throwable> getCause(final Throwable e, final Set<Class<? extends Throwable>> exceptionClasses) {

        Throwable t = e;
        while (t != null) {

            if (exceptionClasses.contains(t.getClass())) {
                return Optional.of(t);
            }
            t = t.getCause();
        }

        return Optional.empty();
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


    /**
     * Extracts the root cause out of a given exception
     * @param throwable
     * @return
     */
    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause;
        while((cause = throwable.getCause()) != null) {
            throwable = cause;
        }

        return throwable;
    }

    /**
     * This code was extracted and refactorerd so it can be re used to transform the info contained
     * in a DotContentletValidationException. The info is grouped by validation category
     */
    public static Map<String, List<ValidationError>> mapValidationException(final User user,
            final DotContentletValidationException ve) throws LanguageException {
        final Map<String, List<ValidationError>> contentValidationErrors = new HashMap<>();
        final String unknown = "unknown";
        final String other = "other";

        if (ve instanceof FileAssetValidationException) {
            final List<Field> reqs = ve.getNotValidFields().get(VALIDATION_FAILED_BADTYPE);
            for (final Field field : reqs) {
                String errorString = LanguageUtil.get(user, ve.getMessage());
                errorString = errorString.replace("{0}", field.getFieldName());
                contentValidationErrors
                        .computeIfAbsent(VALIDATION_FAILED_BADTYPE, k -> new ArrayList<>())
                        .add(new ValidationError(field.getVelocityVarName(), errorString));
            }

        } else {

            if (ve.hasRequiredErrors()) {
                final List<Field> reqs = ve.getNotValidFields().get(VALIDATION_FAILED_REQUIRED);
                for (final Field field : reqs) {
                    String errorString = LanguageUtil.get(user, "message.contentlet.required");
                    errorString = errorString.replace("{0}", field.getFieldName());
                    contentValidationErrors
                            .computeIfAbsent(VALIDATION_FAILED_REQUIRED, k -> new ArrayList<>())
                            .add(new ValidationError(field.getVelocityVarName(), errorString));
                }

            }

            if (ve.hasPatternErrors()) {
                final List<Field> reqs = ve.getNotValidFields()
                        .get(VALIDATION_FAILED_PATTERN);
                for (final Field field : reqs) {
                    String errorString = LanguageUtil.get(user, "message.contentlet.format");
                    errorString = errorString.replace("{0}", field.getFieldName());
                    contentValidationErrors
                            .computeIfAbsent(VALIDATION_FAILED_PATTERN, k -> new ArrayList<>())
                            .add(new ValidationError(field.getVelocityVarName(), errorString));
                }

            }

            if (ve.hasBadTypeErrors()) {
                final List<Field> reqs = ve.getNotValidFields().get(VALIDATION_FAILED_BADTYPE);
                for (final Field field : reqs) {
                    String errorString = UtilMethods.isNotSet(ve.getMessage())?
                            LanguageUtil.get(user, "message.contentlet.type"): ve.getMessage();
                    errorString = errorString.replace("{0}", field.getFieldName());
                    contentValidationErrors
                            .computeIfAbsent(VALIDATION_FAILED_BADTYPE, k -> new ArrayList<>())
                            .add(new ValidationError(field.getVelocityVarName(), errorString));
                }

            }

            if (ve.hasCharLimitErrors()) {
                final List<Field> reqs = ve.getNotValidFields().get(VALIDATION_FAILED_CHAR_LIMIT);
                final Map<String, Integer> charLimitMaxByFieldVar = ve.getCharLimitMaxByFieldVar();
                for (final Field field : reqs) {
                    final Integer maxLimit = charLimitMaxByFieldVar.get(field.getVelocityVarName());
                    String errorString = LanguageUtil.get(user, "dot.edit.content.form.field.charLimitExceeded",
                            maxLimit != null ? maxLimit : 0);
                    contentValidationErrors
                            .computeIfAbsent(VALIDATION_FAILED_CHAR_LIMIT, k -> new ArrayList<>())
                            .add(new ValidationError(field.getVelocityVarName(), errorString));
                }
            }

            if (ve.hasRelationshipErrors()) {
                final StringBuilder sb = new StringBuilder();
                final Map<String, Map<Relationship, List<Contentlet>>> notValidRelationships = ve
                        .getNotValidRelationship();
                final Set<String> auxKeys = notValidRelationships.keySet();
                for (final String key : auxKeys) {
                    String errorMessage = StringPool.BLANK;
                    switch (key) {
                        case VALIDATION_FAILED_REQUIRED_REL:
                            errorMessage = "message.contentlet.relationship.required";
                            break;
                        case VALIDATION_FAILED_INVALID_REL_CONTENT:
                            errorMessage = "message.contentlet.relationship.invalid";
                            break;
                        case VALIDATION_FAILED_BAD_REL:
                            errorMessage = "message.contentlet.relationship.bad";
                            break;
                        case VALIDATION_FAILED_BAD_CARDINALITY:
                            errorMessage = "message.contentlet.relationship.cardinality.bad";
                            break;
                    }

                    sb.append(errorMessage).append(StringPool.COLON);
                    final Map<Relationship, List<Contentlet>> relationshipContentlets = notValidRelationships
                            .get(key);

                    for (final Entry<Relationship, List<Contentlet>> relationship : relationshipContentlets
                            .entrySet()) {
                        sb.append(relationship.getKey().getRelationTypeValue()).append(", ");
                    }
                    contentValidationErrors.computeIfAbsent(key, k -> new ArrayList<>())

                    .add(new ValidationError(sb.toString()));
                }
            }

            if (ve.hasUniqueErrors()) {
                final List<Field> reqs = ve.getNotValidFields()
                        .get(VALIDATION_FAILED_UNIQUE);
                for (final Field field : reqs) {
                    String errorString = LanguageUtil.get(user, "message.contentlet.unique");
                    errorString = errorString.replace("{0}", field.getFieldName());
                    contentValidationErrors
                            .computeIfAbsent(VALIDATION_FAILED_UNIQUE, k -> new ArrayList<>())
                            .add(new ValidationError(field.getVelocityVarName(), errorString));
                }
            }

            if (ve.getMessage().contains(
                    "The content form submission data id different from the content which is trying to be edited")) {
                String errorString = LanguageUtil.get(user, "message.contentlet.invalid.form");
                contentValidationErrors.computeIfAbsent(other, k -> new ArrayList<>())
                        .add(new ValidationError(errorString));
            }

            if (ve.getMessage().contains("message.contentlet.expired")) {
                String errorString = LanguageUtil.get(user, "message.contentlet.expired");
                contentValidationErrors.computeIfAbsent(other, k -> new ArrayList<>())
                        .add(new ValidationError(errorString));
            }
        }
        if (contentValidationErrors.size() == 0) {
            contentValidationErrors.computeIfAbsent(unknown, k -> new ArrayList<>())
                    .add(new ValidationError(ve.getMessage()));
        }

        return contentValidationErrors;
    }

    public static class ValidationError {
        private final String field;
        private final String message;

        private ValidationError(final String field, final String message) {
            this.field = field;
            this.message = message;
        }

        public ValidationError(final String message) {
            this(null, message);
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }



    public static String getStackTraceAsString (final StackTraceElement... traces) {
        final StringBuilder builder = new StringBuilder();
        for (final StackTraceElement traceElement : traces) {
            builder.append("\tat " + traceElement + "\n");
        }
        return builder.toString();
    }

    /**
     * Get the current thread stack trace as a simple string
     * @return String
     */
    public static String getCurrentStackTraceAsString () {
        final StackTraceElement [] traces = Thread.currentThread().getStackTrace();
        return getStackTraceAsString(traces);
    }

    public static String exceptionAsString(final Exception exception, final int limit){
        final String exceptionName = exception.toString();
        final StringBuilder stringBuilder = new StringBuilder(exceptionName).append("\n");
        final StackTraceElement[] elements = exception.getStackTrace();
        if(UtilMethods.isSet(elements)){
          stringBuilder.append( getStackTraceAsString(limit, elements) );
        }
        return stringBuilder.toString();
    }


    public static String getStackTraceAsString (final int limit, final StackTraceElement... traces) {
        final StringBuilder builder = new StringBuilder();
        int count = 1;
        for (final StackTraceElement traceElement : traces) {
            if(count <= limit) {
                builder.append("\tat " + traceElement + "\n");
            }
            count++;
        }
        return builder.toString();
    }

    /**
     * Get the current thread stack trace as a simple string
     * @param limit {@link Integer} limit for the stack trace to attach
     * @return String
     */
    public static String getCurrentStackTraceAsString (final int limit) {
        final StackTraceElement [] traces = Thread.currentThread().getStackTrace();
        return getStackTraceAsString(limit, traces);
    }

    /**
     * Get the current thread stack trace as a simple string
     * @param offset {@link Integer} offset to start to attach stack trace to attach
     * @param limit  {@link Integer} limit for the stack trace to attach
     * @return String
     */
    public static String getCurrentStackTraceAsString (final int offset, final int limit) {
        final StringBuilder builder = new StringBuilder();
        final StackTraceElement [] traces = Thread.currentThread().getStackTrace();
        int countItem  = 0;
        int countLimit = limit;
        for (final StackTraceElement traceElement : traces) {

            if (countItem++ > offset) {

                builder.append("\tat " + traceElement + "\n");
                if (countLimit-- < 0) {
                    break;
                }
            }

        }
        return builder.toString();
    }

    /**
     * Determine if the exception happens on edit or preview mode, if the request is not null will use it to figure out
     * @param exception {@link Exception}
     * @param request {@link HttpServletRequest}
     * @return boolean
     */
    public static boolean isPreviewOrEditMode(final Exception exception, final HttpServletRequest request) {

        boolean isPreviewOrEdit          = false;

        if (null != request) {

            final PageMode pageMode = PageMode.get(request);
            isPreviewOrEdit = PageMode.EDIT_MODE == pageMode || PageMode.PREVIEW_MODE == pageMode;
        } else {


            for (final StackTraceElement stackTraceElement : exception.getStackTrace()) {
                if (stackTraceElement.getClassName().indexOf("EditMode") > -1 ||
                        stackTraceElement.getMethodName().indexOf("EditMode") > -1 ||
                        stackTraceElement.getClassName().indexOf("PreviewMode") > -1 ||
                        stackTraceElement.getMethodName().indexOf("PreviewMode") > -1) {

                    isPreviewOrEdit = true;
                    break;
                }
            }
        }

        return isPreviewOrEdit;
    }

    /**
     * Get the cause by exception
     * @param exception {@link Throwable}
     * @param exceptionClasses {@link Set}
     * @return Throwable
     */
    public static Throwable getCauseBy (final Throwable exception,
                                        final Set<Class<? extends Throwable>> exceptionClasses) {

        Throwable throwable = exception;
        while (throwable != null) {
            if (exceptionClasses.contains(throwable.getClass())) {
                return throwable;
            }
            throwable = throwable.getCause();
        }
        return exception;
    }

}
