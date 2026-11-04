package com.dotcms.rendering.velocity.viewtools;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletRequest;

/**
 * Binds the file asset that a file-based scripting endpoint ({@code /api/vtl} / {@code /api/js})
 * is executing to the current request, so checks that need "who wrote this code" (see
 * {@link SecretTool}) use that file's last editor instead of the calling user, and site-scoped
 * secret reads use the file's site instead of a caller-selected site.
 *
 * <p>The binding is trustworthy only if a template evaluated by those endpoints cannot re-bind
 * it: otherwise any published script file could borrow a developer-edited content as its
 * author and read secrets to whoever can call the endpoint. So the value stored in the request
 * is a private wrapper type only this class creates — a raw {@link Contentlet} placed under the
 * attribute by {@code setAttribute} is ignored — and the setter lives here, not on
 * {@link SecretTool} or any other object a template reaches through the context or toolbox
 * (the production sandbox is class-based and cannot wall off a single method).</p>
 */
public final class ScriptingFileAttribute {

    /** Request attribute holding the scripting file. Created only by {@link #set}. */
    public static final String ATTRIBUTE = ScriptingFileAttribute.class.getName() + ".scriptingFile";

    private record ScriptingFile(String modUserId, String siteId) { }

    private ScriptingFileAttribute() {
        // static utility
    }

    /**
     * Captures the editor and site of the file asset whose code the request is about to run,
     * so later content mutations cannot change the secrets authorization or site. Only callers
     * running a stored file asset should use this; the {@code /dynamic} endpoints must not.
     * <p>
     * Code evaluated on the fly inside the file, such as Velocity {@code #evaluate} or JavaScript
     * {@code eval}, has no template name of its own and so also runs with this file's editor.
     * Never evaluate request input in an endpoint whose file was last edited by a Scripting
     * Developer, or an anonymous caller could read any secret.
     *
     * @param request The current HTTP request.
     * @param file    The file asset {@link Contentlet} holding the script.
     */
    public static void set(final HttpServletRequest request, final Contentlet file) {
        request.setAttribute(ATTRIBUTE, new ScriptingFile(file.getModUser(), file.getHost()));
    }

    /**
     * Returns the captured file site, never a caller-supplied host selection.
     *
     * @param request the current request, possibly {@code null}
     * @return the captured site ID, or {@code null} without a Java-bound scripting file
     * @throws SecurityException if a bound scripting file has no site
     */
    public static String getSiteId(final HttpServletRequest request) {
        if (request == null || !(request.getAttribute(ATTRIBUTE) instanceof ScriptingFile file)) {
            return null;
        }
        if (!UtilMethods.isSet(file.siteId())) {
            throw new SecurityException("Scripting file has no site");
        }
        return file.siteId();
    }

    /**
     * Returns the editor ID captured by {@link #set}, or {@code null} when none
     * was bound by Java code. A value that is not the wrapper type (for example a raw
     * {@link Contentlet} a template pushed through {@code setAttribute}) never counts.
     *
     * @param request The current HTTP request, may be {@code null}.
     *
     * @return The captured editor ID, or {@code null}.
     */
    public static String get(final HttpServletRequest request) {
        if (null == request) {
            return null;
        }
        return request.getAttribute(ATTRIBUTE) instanceof ScriptingFile scriptingFile
                ? scriptingFile.modUserId()
                : null;
    }
}