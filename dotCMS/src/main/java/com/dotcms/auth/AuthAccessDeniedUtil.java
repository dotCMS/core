package com.dotcms.auth;

import com.dotmarketing.util.UtilHTML;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class AuthAccessDeniedUtil {

    private AuthAccessDeniedUtil() {
    }

    public static boolean hasRequiredRole(final User user, final boolean frontEndLogin) {
        if (user == null) {
            return false;
        }
        return frontEndLogin
                ? user.isFrontendUser()
                : user.isBackendUser() || user.isAdmin();
    }

    /**
     * Sends the 403 "no access" page shown when an SSO user signs in but lacks the required access. The page is
     * flushed before returning, so it is committed as a 403 even when sent from a JAX-RS resource method that
     * returns {@code void} (the SAML callback), which would otherwise answer an empty 204.
     *
     * @param response the response to write the page to
     * @param user     the user who signed in, shown by email; may be {@code null}
     * @throws IOException when the page cannot be written
     */
    public static void sendNoAccessPage(final HttpServletResponse response,
                                        final User user) throws IOException {
        setNoCacheHeaders(response);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/html;charset=UTF-8");
        final String email = user != null && user.getEmailAddress() != null
                ? UtilHTML.escapeHTMLSpecialChars(user.getEmailAddress())
                : "";
        response.getWriter().write(
                "<!DOCTYPE html><html><head><title>Access denied</title>"
                        + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                        + "<style>"
                        + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;"
                        + "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;"
                        + "background:#f8fafc;color:#0f172a}"
                        + ".card{text-align:center;max-width:440px;padding:2.5rem;background:#fff;"
                        + "border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.1)}"
                        + "h1{font-size:1.25rem;margin:0 0 .75rem}"
                        + "p{font-size:.875rem;color:#475569;line-height:1.6;margin:0}"
                        + ".email{font-weight:600}"
                        + "</style></head><body>"
                        + "<div class=\"card\">"
                        + "<h1>Your account does not have access to dotCMS</h1>"
                        + "<p>You signed in as <span class=\"email\">" + email + "</span> "
                        + "but this account has not been granted the required permissions. "
                        + "Contact your administrator to request access.</p>"
                        + "</div></body></html>");
        // Commit the page now: the SAML callback is a JAX-RS resource method returning void, and if the
        // response is still only buffered when it returns, Jersey answers 204 No Content and drops the page.
        response.flushBuffer();
    }

    public static void setNoCacheHeaders(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}
