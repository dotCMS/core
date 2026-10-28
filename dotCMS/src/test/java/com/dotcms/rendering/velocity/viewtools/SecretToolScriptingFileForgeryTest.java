package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.junit.Test;

/**
 * Guards the trust boundary around the scripting-file marker that names the file asset whose
 * last editor authorizes secret reads (see {@link SecretTool} and
 * {@link ScriptingFileAttribute}): a template evaluated on a file-based scripting endpoint
 * ({@code /api/vtl} / {@code /api/js}) must not be able to re-bind it. If it could, any published
 * script file could borrow a developer-edited content as its "last editor" and read secrets to
 * whoever can call the endpoint — including anonymous callers on sites that opted into
 * {@code SCRIPTING_APIS_ALLOW_ANONYMOUS}.
 *
 * <p>The marker's setter must therefore not be a method on {@link SecretTool} (or any other
 * object a template can reach through the context or toolbox): the production sandbox is
 * class-based, so it cannot distinguish this one method from the tool's legitimate read API.
 * It lives on {@link ScriptingFileAttribute}, which no template can obtain a reference to.</p>
 */
public class SecretToolScriptingFileForgeryTest {

    private static final String[] BAD_CLASSES = {
            "java.lang.Class", "java.lang.ClassLoader", "java.lang.Runtime", "java.lang.Process",
            "java.lang.System", "java.lang.Thread", "java.net.Socket",
            "org.apache.velocity.app.VelocityEngine"
    };
    private static final String[] BAD_PACKAGES = { "java.lang.reflect" };

    /**
     * The production {@link SecureIntrospectorImpl} — the same resolution path Velocity uses for
     * {@code $dotsecrets.someMethod(...)} — must not resolve a {@code setScriptingFile} method
     * on the tool. The sandbox is class-based, so the only reliable guard is that the method
     * does not exist on any reachable object.
     */
    @Test
    public void sandboxMustNotResolveAScriptingFileSetterOnTheTool() {

        final SecureIntrospectorImpl sandbox = new SecureIntrospectorImpl(BAD_CLASSES, BAD_PACKAGES);
        final HttpServletRequest request = mock(HttpServletRequest.class);

        // Sanity: the tool's own read API still resolves, so a null below means the setter is
        // gone, not a broken sandbox setup.
        assertNotNull("The tool's read API must keep resolving for templates",
                sandbox.getMethod(SecretTool.class, "getSystemSecret", new Object[] { "any" }));

        assertNull("A template must not be able to re-bind the scripting-file marker: keep the "
                        + "setter off SecretTool (and every other reachable object)",
                sandbox.getMethod(SecretTool.class, "setScriptingFile",
                        new Object[] { request, new Contentlet() }));
    }
}