package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end companion of {@link SecretToolScriptingFileForgeryTest}: a template evaluated
 * through the real dotCMS engine and toolbox context must not be able to re-bind the
 * scripting-file marker that names the file asset whose last editor authorizes secret reads.
 * If it could, a published script file could borrow a developer-edited content as its author
 * and read secrets to whoever can call the endpoint — including anonymous callers on sites
 * that opted into {@code SCRIPTING_APIS_ALLOW_ANONYMOUS}.
 */
public class ScriptingFileForgeryIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * The forge template is evaluated against the same {@link VelocityUtil#getEngine()} and
     * toolbox-backed context that {@code /api/v1/vtl} uses. It must leave the marker that
     * Java bound untouched, while Java's own binding still resolves for the secrets check.
     */
    @Test
    public void evaluatedTemplateCannotRebindTheScriptingFileMarker() throws Exception {

        // Request that really stores attributes, so a re-bind is observable.
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        doAnswer(invocation -> attributes.put(
                invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString()))
                .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        // What Java binds when the endpoint loads the file asset.
        final Contentlet javaBoundFile = new Contentlet();
        ScriptingFileAttribute.set(request, javaBoundFile);
        final Object javaBoundMarker = attributes.get(ScriptingFileAttribute.ATTRIBUTE);

        // The production context: toolbox tools ($dotsecrets), $context with getRequest(),
        // and a Contentlet a template could obtain (e.g. $contents.getContentByInode).
        final Context context = VelocityUtil.getInstance().getContext(
                request, new MockHttpResponse());
        context.put("file", new Contentlet());

        final String forgeTemplate = "$dotsecrets.setScriptingFile($context.getRequest(), $file)"
                + "$context.getRequest().setAttribute('" + ScriptingFileAttribute.ATTRIBUTE + "', $file)";
        VelocityUtil.getEngine().evaluate(context, new StringWriter(),
                "scripting-file-forgery-integration", new StringReader(forgeTemplate));

        assertSame("A template re-bound the trusted scripting-file marker",
                javaBoundMarker, attributes.get(ScriptingFileAttribute.ATTRIBUTE));
        assertSame("Java's own binding must keep resolving for the secrets check",
                javaBoundFile, ScriptingFileAttribute.get(request));
    }
}