package com.dotcms.rest.api.v1.authentication;

import static com.dotcms.exception.ExceptionUtil.BAD_REQUEST_EXCEPTIONS;
import static com.dotcms.exception.ExceptionUtil.DUPLICATE_EXCEPTIONS;
import static com.dotcms.exception.ExceptionUtil.MALFORMED_MULTIPART_EXCEPTIONS;
import static com.dotcms.exception.ExceptionUtil.NOT_FOUND_EXCEPTIONS;
import static com.dotcms.exception.ExceptionUtil.SECURITY_EXCEPTIONS;
import static com.dotcms.exception.ExceptionUtil.causedBy;
import static com.dotcms.exception.ExceptionUtil.getRootCause;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

/**
 * Just a helper to encapsulate AuthenticationResource functionality.
 * @author jsanca
 */
public class ResponseUtil implements Serializable {

    public static ResponseUtil INSTANCE =
            new ResponseUtil();


    private ResponseUtil() {

    }

    private final static byte [] BEGIN_RESPONSE_ENTITY_VIEW = ("{\"entity\":").getBytes(StandardCharsets.UTF_8);
    private final static byte [] START_OBJECT    = StringPool.OPEN_CURLY_BRACE.getBytes(StandardCharsets.UTF_8);
    private final static String  BEGIN_PROPERTY  = "\"{0}\":";
    private final static byte [] END_PROPERTY    = StringPool.CLOSE_CURLY_BRACE.getBytes(StandardCharsets.UTF_8);
    private final static byte [] EMPTY_END_RESPONSE_ENTITY_VIEW    =
            ("\"errors\": [],\n" +
            "    \"i18nMessagesMap\": {},\n" +
            "    \"messages\": [],\n" +
            "    \"permissions\": []").getBytes(StandardCharsets.UTF_8);


    public static void beginWrapResponseEntityView(final OutputStream outputStream, final boolean startObject) throws IOException {

        outputStream.write(BEGIN_RESPONSE_ENTITY_VIEW);
        if (startObject) {
            outputStream.write(START_OBJECT);
        }
    }

    public static void beginWrapProperty(final OutputStream outputStream, final String propertyName, final boolean startObject) throws IOException {

        outputStream.write(MessageFormat.format(BEGIN_PROPERTY, propertyName).getBytes(StandardCharsets.UTF_8));
        if (startObject) {
            outputStream.write(START_OBJECT);
        }
    }

    public static void endWrapProperty(final OutputStream outputStream) throws IOException {

        outputStream.write(END_PROPERTY);
    }

    public static void wrapProperty(final OutputStream outputStream, final String propertyName,
                                    final String valueOnJson) throws IOException {

        outputStream.write(MessageFormat.format("\"{0}\":", propertyName).getBytes(StandardCharsets.UTF_8));
        outputStream.write(valueOnJson.getBytes(StandardCharsets.UTF_8));
        outputStream.write(END_PROPERTY);
    }

    public static void endWrapResponseEntityView(final OutputStream outputStream, final boolean endObject) throws IOException {

        outputStream.write(EMPTY_END_RESPONSE_ENTITY_VIEW);
        if (endObject) {
            outputStream.write(END_PROPERTY);
        }
    }

    /**
     * Get Error response based on a status and message key
     * This support is a single message
     *
     * @param request
     * @param status
     * @param userId
     * @param messageKey
     * @return Response
     */
    public Response getErrorResponse(final HttpServletRequest request,
                                     final Response.Status status,
                                     final Locale locale,
                                     final String userId,
                                     final String messageKey,
                                     final Object... arguments) {

        return ErrorResponseHelper.INSTANCE.getErrorResponse(status, locale, messageKey, arguments);
    }

    /**
     * Get the translation of the message key in the specified locale, if the locale is null
     * then the message is translated into the default user language
     *
     * @param locale           Current user language
     * @param messageKey       Message key to be translated
     * @param messageArguments (Optional) if the message require some argument
     * @return
     */
    public static String getFormattedMessage(Locale locale, String messageKey, Object... messageArguments) {
        String message;
        try {
            message = (UtilMethods.isSet(locale)) ?
                    LanguageUtil.get(locale, messageKey) :
                    LanguageUtil.get((User) null, messageKey);

            return MessageFormat.format(message, messageArguments);
        } catch (LanguageException e) {
            Logger.error(ResponseUtil.class, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * In case of an invalid access attempt this will fulfill the response so it can be interpreted by a front-end consumer
     * Takes an exception and tries to find the most appropriate match code to send an error response.
     * @param e Exception
     * @return Response
     */
    public static Response mapExceptionResponse(final Throwable e){
        // case for non-authenticated users (bad credentials)
        if(causedBy(e, SecurityException.class)){
            return createNonAuthenticatedResponse(e);
        }

        if(causedBy(e, DUPLICATE_EXCEPTIONS)){
            return createDuplicateResponse(e);
        }

        if(causedBy(e, SECURITY_EXCEPTIONS)){
            return createUnAuthorizedResponse(e);
        }

        if(causedBy(e, MALFORMED_MULTIPART_EXCEPTIONS)){
            return createMalformedMultipartResponse(e);
        }

        if(causedBy(e, BAD_REQUEST_EXCEPTIONS)){
            final Throwable causedByException = ExceptionUtil.getCauseBy(e, BAD_REQUEST_EXCEPTIONS);
            return ExceptionMapperUtil.createResponse(null != causedByException? causedByException:e,
                    null != causedByException?causedByException.getMessage():e.getMessage(), Response.Status.BAD_REQUEST);
        }

        if(causedBy(e, NOT_FOUND_EXCEPTIONS)){
            final Throwable causedByException = ExceptionUtil.getCauseBy(e, NOT_FOUND_EXCEPTIONS);
            return ExceptionMapperUtil.createResponse(null != causedByException? causedByException:e,
                    null != causedByException?causedByException.getMessage():e.getMessage(), Response.Status.NOT_FOUND);
        }

        if(e instanceof DotContentletValidationException){
            final DotContentletValidationException ve = DotContentletValidationException.class.cast(e);
            return ExceptionMapperUtil.createResponse(Response.Status.BAD_REQUEST, ve);
        }

        final Throwable rootCause = getRootCause(e);
        if( rootCause instanceof DotContentletValidationException){
            final DotContentletValidationException ve = DotContentletValidationException.class.cast(rootCause);
            return ExceptionMapperUtil.createResponse(Response.Status.BAD_REQUEST, ve);
        }

        return ExceptionMapperUtil.createResponse(rootCause, Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * In case of an duplicate record attempt this will fulfill the response so it can be interpreted by a front-end consumer
     * @param e Exception
     * @return Response
     */
    private static Response createDuplicateResponse(final Throwable e) {
        SecurityLogger.logInfo(ResponseUtil.class, e.getMessage());
        return ExceptionMapperUtil.createResponse(e, Response.Status.PRECONDITION_FAILED);
    }

    /**
     * In case of an invalid access attempt this will fulfill the response so it can be interpreted by a front-end consumer
     * @param e Exception
     * @return Response
     */
    private static Response createUnAuthorizedResponse (final Throwable e) {

        SecurityLogger.logInfo(ResponseUtil.class, e.getMessage());
        return ExceptionMapperUtil.createResponse(e, Response.Status.FORBIDDEN);
    }

    /**
     * Creates a response for a malformed multipart request. This method attempts to process the
     * provided exception, identifying if it matches certain known exception types indicating an
     * issue with the multipart data. If so, it returns a specific error response; otherwise, it
     * defaults to a more general error response.
     *
     * @param e The exception representing the error encountered during the handling of the
     *          multipart request.
     * @return A {@code Response} instance with the appropriate HTTP status and error message based
     * on the exception details.
     */
    private static Response createMalformedMultipartResponse(final Throwable e) {

        Throwable causedByException = ExceptionUtil.getCauseBy(e, MALFORMED_MULTIPART_EXCEPTIONS);
        if (null != causedByException
                && !(causedByException instanceof javax.ws.rs.BadRequestException)) {

            final var message = String.format(
                    "The multipart data is missing or improperly formatted. "
                            + "Please check your request and try again. [%s]",
                    causedByException.getMessage());

            return ExceptionMapperUtil.createResponse(message, Response.Status.BAD_REQUEST);
        }

        return ExceptionMapperUtil.createResponse(e, Response.Status.BAD_REQUEST);
    }

    /**
     * In case of an invalid access attempt this will fulfill the response so it can be interpreted by a front-end consumer
     * @param e Exception
     * @return Response
     */
    private static Response createNonAuthenticatedResponse (final Throwable e) {

        SecurityLogger.logInfo(ResponseUtil.class, e.getMessage());
        return ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
    }

    /**
     * Handles the async response for a future
     * @param future {@link Future}
     * @param asyncResponse {@link AsyncResponse}
     * @param <T>
     */
    public static <T> void  handleAsyncResponse (final Future<T> future, final AsyncResponse asyncResponse) {

        handleAsyncResponse(() -> Response.ok(new ResponseEntityView<>(DotConcurrentFactory.get(future))).build(), asyncResponse);
    } // handleAsyncResponse

    /**
     * Handles the async response for a supplier
     * @param supplier {@link Supplier}
     * @param asyncResponse {@link AsyncResponse}
     * @param <T>
     */
    public static <T> void  handleAsyncResponse (final Supplier<T> supplier, final AsyncResponse asyncResponse) {

        handleAsyncResponse(supplier, ResponseUtil::mapExceptionResponse, asyncResponse);
    } // handleAsyncResponse

    /**
     * Handles the async response for a supplier
     * @param supplier {@link Supplier}
     * @param asyncResponse {@link AsyncResponse}
     * @param <T>
     */
    public static <T> void  handleAsyncResponse (final Supplier<T> supplier, final Function<Throwable, Response> errorHandler, final AsyncResponse asyncResponse) {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                .getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
        final CompletableFuture<T> completableFuture =
                CompletableFuture.supplyAsync(supplier, dotSubmitter);

        completableFuture.thenApply(asyncResponse::resume)
                .exceptionally(e -> asyncResponse.resume(errorHandler.apply(e)));

        Try.run(()->completableFuture.get()).onFailure(e -> Logger.error(ResponseUtil.class, e.getMessage(), e));
    } // handleAsyncResponse


} // E:O:F:ResponseUtil.
