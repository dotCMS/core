package com.dotmarketing.webdav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bradmcevoy.http.ExistingEntityHandler;
import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HttpExtension;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.ReportableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import java.util.Arrays;
import org.junit.Test;

/**
 * Covers the WebDAV library starting and answering REPORT without jdom 1.x on the classpath.
 *
 * <p>The library builds a REPORT handler whenever it builds its WebDAV protocol, and its own
 * handler is linked against jdom 1.x. dotCMS ships a replacement under the same class name, so
 * these tests fail if the replacement stops being the one loaded or if the old dependency returns.
 */
public class WebdavReportHandlerTest {

    /** jdom 1.x is gone from the classpath, which is the condition the rest of these tests rely on. */
    @Test
    public void jdomOneIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.jdom.JDOMException"));
    }

    /** The servlet builds exactly this manager at startup; this is what failed at runtime before. */
    @Test
    public void theWebDavManagerStarts() {
        assertNotNull(reportHandler());
    }

    /** No dotCMS resource implements REPORT, so the library answers "not implemented" as before. */
    @Test
    public void reportIsNotOfferedForOrdinaryResources() {
        assertFalse(reportHandler().isCompatible(mock(Resource.class)));
    }

    /**
     * A resource that does accept REPORT, which no dotCMS resource does, gets 400 Bad Request and
     * the body is never read.
     */
    @Test
    public void reportOnAReportableResourceIsRefusedWithoutReadingTheBody() throws Exception {
        final Request request = mock(Request.class);
        final Response response = mock(Response.class);

        assertThrows(BadRequestException.class, () -> reportHandler()
                .processExistingResource(null, request, response, mock(ReportableResource.class)));

        verify(request, never()).getInputStream();
        verifyNoInteractions(response);
    }

    private static ExistingEntityHandler reportHandler() {
        final HttpManager manager = new HttpManager((host, path) -> null);
        Handler found = null;
        for (final HttpExtension extension : manager.getHandlers()) {
            for (final Handler handler : extension.getHandlers()) {
                if (Arrays.asList(handler.getMethods()).contains(Method.REPORT.code)) {
                    assertNull("Only one handler should answer REPORT", found);
                    found = handler;
                }
            }
        }
        assertNotNull("The WebDAV protocol registers a REPORT handler", found);
        return (ExistingEntityHandler) found;
    }
}
