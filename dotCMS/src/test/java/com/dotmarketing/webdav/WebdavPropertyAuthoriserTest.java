package com.dotmarketing.webdav;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.property.PropertyAuthoriser.CheckResult;
import com.bradmcevoy.property.PropertyAuthoriser.PropertyPermission;
import java.util.Set;
import javax.xml.namespace.QName;
import org.junit.Test;

/**
 * Covers the property permission check for the WebDAV endpoints.
 *
 * <p>The decision itself is the resource's, and these tests only pin what is done with the answer.
 * The case that matters is a request that names no property: the library default builds its result
 * by iterating the names, so with none to iterate a refusal comes back as an empty result, and the
 * caller reads empty as "nothing wrong" and carries on.
 */
public class WebdavPropertyAuthoriserTest {

    private static final QName DISPLAY_NAME = new QName("DAV:", "displayname");
    private static final QName CREATION_DATE = new QName("DAV:", "creationdate");

    @Test
    public void aRefusalIsReportedEvenWhenNoPropertyWasNamed() {
        final Resource resource = refusing();

        final Set<CheckResult> result = new WebdavPropertyAuthoriser()
                .checkPermissions(request(), Method.PROPPATCH, PropertyPermission.WRITE,
                        Set.of(), resource);

        assertFalse("A refusal reported as an empty result reads to the caller as no refusal",
                result == null || result.isEmpty());
        assertEquals(Status.SC_UNAUTHORIZED, result.iterator().next().getStatus());
    }

    /** Same case, reached the other way: the caller hands in a null set for some bodies. */
    @Test
    public void aRefusalIsReportedWhenTheNameSetIsNull() {
        final Set<CheckResult> result = new WebdavPropertyAuthoriser()
                .checkPermissions(request(), Method.PROPPATCH, PropertyPermission.WRITE,
                        null, refusing());

        assertFalse("A null name set must not turn a refusal into a pass",
                result == null || result.isEmpty());
    }

    @Test
    public void aRefusalNamesEveryPropertyTheRequestAskedFor() {
        final Resource resource = refusing();

        final Set<CheckResult> result = new WebdavPropertyAuthoriser()
                .checkPermissions(request(), Method.PROPPATCH, PropertyPermission.WRITE,
                        Set.of(DISPLAY_NAME, CREATION_DATE), resource);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == Status.SC_UNAUTHORIZED));
        assertTrue(result.stream().allMatch(r -> r.getResource() == resource));
        assertEquals(Set.of(DISPLAY_NAME, CREATION_DATE),
                result.stream().map(CheckResult::getField).collect(toSet()));
    }

    @Test
    public void nothingIsReportedWhenTheResourceAllowsIt() {
        final Resource resource = mock(Resource.class);
        when(resource.authorise(any(), any(), any())).thenReturn(true);

        final Set<CheckResult> result = new WebdavPropertyAuthoriser()
                .checkPermissions(request(), Method.PROPPATCH, PropertyPermission.WRITE,
                        Set.of(DISPLAY_NAME), resource);

        assertTrue("An allowed request must report nothing", result == null || result.isEmpty());
    }

    /**
     * The resource is what holds the permission logic, so it has to be asked with the request's own
     * method and credentials rather than anything reconstructed here.
     */
    @Test
    public void theResourceIsAskedWithTheRequestsOwnMethodAndCredentials() {
        final Request request = request();
        final Auth auth = request.getAuthorization();
        final Resource resource = refusing();

        new WebdavPropertyAuthoriser().checkPermissions(request, Method.PROPPATCH,
                PropertyPermission.WRITE, Set.of(DISPLAY_NAME), resource);

        verify(resource).authorise(request, Method.PROPPATCH, auth);
    }

    private static Request request() {
        final Request request = mock(Request.class);
        final Auth auth = mock(Auth.class);
        when(request.getAuthorization()).thenReturn(auth);
        when(request.getMethod()).thenReturn(Method.PROPPATCH);
        return request;
    }

    private static Resource refusing() {
        final Resource resource = mock(Resource.class);
        when(resource.authorise(any(), any(), any())).thenReturn(false);
        return resource;
    }
}
