package com.dotcms.cms.login;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.apache.commons.lang.SerializationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

/**
 * Prevent the session fixation if the config property "PREVENT_SESSION_FIXATION_ON_LOGIN" is not in false.
 * @author jsanca
 */
public class PreventSessionFixationUtil {

    private PreventSessionFixationUtil() {
        // singleton
    }

    private static class SingletonHolder {
        private static final PreventSessionFixationUtil INSTANCE = new PreventSessionFixationUtil();
    }

    public static PreventSessionFixationUtil getInstance () {
        return PreventSessionFixationUtil.SingletonHolder.INSTANCE;
    }

    /**
     * Gets the current session (if exists) invalidate it and them created a new one with a
     * copy of the previous session attributes.
     * @param request {@link HttpServletRequest}
     * @param createSessionIfDoesNotExists {@link Boolean} if false and the session on the request.getSession(false) returns null (no session created) returns a null session,
     *                                                    if true will create a new session if does not exists
     * @return HttpSession
     */
    public HttpSession preventSessionFixation(final HttpServletRequest request, final boolean createSessionIfDoesNotExists) {

        HttpSession session = request.getSession(false);

        if(Config.getBooleanProperty("PREVENT_SESSION_FIXATION_ON_LOGIN", true)) {

            Logger.debug(this, ()-> "Preventing the session fixation");
            Logger.info(this, "========== Preventing the session fixation ==========");
            Logger.info(this, "createSessionIfDoesNotExists = " + createSessionIfDoesNotExists);

            final Map<String, Object> sessionMap  = new HashMap<>();
            final HttpSession oldSession          = session;

            if (null != oldSession) {

                final Enumeration<String> keys = oldSession.getAttributeNames();

                Logger.info(this, "-> Original Session params [ " + oldSession.getId() + " ]:");

                while (keys.hasMoreElements()) {

                    final String key = keys.nextElement();
                    final Object value = oldSession.getAttribute(key);
                    sessionMap.put(key, value);

                    Logger.info(this, "- Attr '" + key + "' = " + value);
                }

                final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                        .getSubmitter("map-copy");
                final CompletionService<Map<String, Object>> completionService = new ExecutorCompletionService<>(dotSubmitter);
                completionService.submit(() -> Map.copyOf(sessionMap));
                final Map<String, Object> newSessionMap = Try.of(() -> completionService.take().get()).getOrElse(new HashMap<>());

                //final Map<String, Object> newSessionMap = Map.copyOf(sessionMap);

                Logger.info(this, "-> New Session params size = " + newSessionMap.size());

                /*try {
                    Logger.info(this, "... Sleeping for 3 seconds ...");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }*/

                oldSession.invalidate();

                final HttpSession newSession = request.getSession();

                Logger.info(this, "-> New Session params [ " + newSession.getId() + " ]:");

                //for (final Map.Entry<String, Object> entry : sessionMap.entrySet()) {
                for (final Map.Entry<String, Object> entry : newSessionMap.entrySet()) {
                    newSession.setAttribute(entry.getKey(), entry.getValue());

                    Logger.info(this, "- Added Attr '" + entry.getKey() + "' = " + entry.getValue());

                }

                session = newSession;
            } else {
                Logger.info(this, "This time, there was NO SESSION in the request!!");
                Logger.info(this, "--- Request Data:");
                Logger.info(this, " -> request.getRemoteAddr() = " + request.getRemoteAddr());
                Logger.info(this, " -> request.getContextPath() = " + request.getContextPath());
                Logger.info(this, " -> request.getPathInfo() = " + request.getPathInfo());
                Logger.info(this, " -> request.getRequestURI() = " + request.getRequestURI());
                Logger.info(this, " -> request.getQueryString() = " + request.getQueryString());
                Logger.info(this, " -> request.getRequestedSessionId() = " + request.getRequestedSessionId());
                Logger.info(this, " -> request.getRequestURL() = " + request.getRequestURL());
                Logger.info(this, " -> request.getRemoteHost() = " + request.getRemoteHost());
                Logger.info(this, " -> request.getServletPath() = " + request.getServletPath());
                Logger.info(this, " -> request.getContentType() = " + request.getContentType());
                Logger.info(this, " -> request.getAuthType() = " + request.getAuthType());
                Logger.info(this, "A NEW SESSION IS BEING CREATED AND RETURNED!!");
                Logger.info(this, "--- Current stack trace:");
                Logger.info(this, ExceptionUtil.getCurrentStackTraceAsString());
            }
        }

        //return null == session && createSessionIfDoesNotExists? request.getSession(): session;
        session = null == session && createSessionIfDoesNotExists? request.getSession(): session;
        if (null != session) {
            Logger.info(this, "--- New Session ID = " + session.getId());
            Logger.info(this, "--- Has any attributes? = " + session.getAttributeNames().hasMoreElements());
        }
        return session;
    } // preventSessionFixation.
}
