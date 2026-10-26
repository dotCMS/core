package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
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

        // The production context: toolbox tools ($dotsecrets), $context with getRequest(),
        // and a Contentlet a template could obtain (e.g. $contents.getContentByInode).
        final Context context = VelocityUtil.getInstance().getContext(
                request, new MockHttpResponse());
        final Contentlet templateFile = new Contentlet();
        context.put("file", templateFile);

        final String forgeTemplate = "$dotsecrets.setScriptingFile($context.getRequest(), $file)"
                + "$context.getRequest().setAttribute('" + ScriptingFileAttribute.ATTRIBUTE + "', $file)";
        VelocityUtil.getEngine().evaluate(context, new StringWriter(),
                "scripting-file-forgery-integration", new StringReader(forgeTemplate));

        // The template can clobber the raw attribute through setAttribute, but the trusted
        // read filters to the wrapper type only Java code creates, so a template-supplied
        // Contentlet never becomes the scripting file: after the attempt the secrets check
        // sees no scripting file at all and falls back to the request user.
        assertNull("A template must not be able to forge the trusted scripting-file marker",
                ScriptingFileAttribute.get(request));
        assertNotSame("The template-supplied Contentlet must never resolve as the scripting file",
                templateFile, ScriptingFileAttribute.get(request));
    }
}