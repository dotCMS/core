package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.publishing.DotPublishingException;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link S3VanityAliasService}.
 */
public class S3VanityAliasServiceTest {

    private static final String ENDPOINT_ID = "endpoint";
    private static final String HOST_ID = "host";
    private static final long LANGUAGE_ID = 1L;
    private static final String CANONICAL_PATH = "/canonical";
    private static final String BUCKET = "bucket";
    private static final String REGION = "region";
    private static final String PREFIX = "prefix";

    private final VanityUrlAPI vanityUrlAPI = mock(VanityUrlAPI.class);
    private final S3VanityAliasRepository repository = mock(S3VanityAliasRepository.class);
    private final AWSS3EndPointPublisher endpointPublisher = mock(AWSS3EndPointPublisher.class);
    private final Host host = mock(Host.class);
    private final Language language = mock(Language.class);
    private final File canonicalFile = new File("canonical.html");
    private final S3VanityAliasLookup lookup =
            new S3VanityAliasLookup(ENDPOINT_ID, HOST_ID, LANGUAGE_ID, CANONICAL_PATH);
    private final S3VanityAliasContext context =
            new S3VanityAliasContext(lookup, BUCKET, REGION, PREFIX, host, language,
                    canonicalFile, endpointPublisher);
    private final S3VanityAliasService service =
            new S3VanityAliasService(vanityUrlAPI, new S3VanityAliasSupport(), repository,
                    mock(HTMLPageAssetAPI.class), mock(S3VanityTargetResolver.class));

    @Test
    public void publishAliasesShouldReturnVoidAndRefreshOnlyPersistedAliases() throws Exception {
        final S3VanityAlias persistedAlias = alias("/promo", "vanity-1");
        whenCurrentVanities(vanityUrl("/promo", "vanity-1"));
        when(repository.findByLookup(lookup)).thenReturn(List.of(persistedAlias));

        final Void result = invokeVoid(() -> service.publishAliases(context));

        Assert.assertNull(result);
        verify(endpointPublisher).pushFileToEndpoint(BUCKET, REGION, PREFIX, "/promo", canonicalFile);
        final List<S3VanityAlias> mappings = replacedMappings();
        Assert.assertEquals(1, mappings.size());
        Assert.assertEquals("/promo", mappings.get(0).vanityPath);
    }

    @Test
    public void publishAliasesShouldReturnVoidAndAvoidCreatingNewAliasesDuringCanonicalRepublish()
            throws Exception {
        whenCurrentVanities(vanityUrl("/new-promo", "vanity-1"));
        when(repository.findByLookup(lookup)).thenReturn(List.of());

        final Void result = invokeVoid(() -> service.publishAliases(context));

        Assert.assertNull(result);
        verify(endpointPublisher, never()).pushFileToEndpoint(any(), any(), any(), any(), any());
        final List<S3VanityAlias> mappings = replacedMappings();
        Assert.assertTrue(mappings.isEmpty());
    }

    @Test
    public void publishAliasesShouldReturnVoidAndDeleteObsoletePersistedAliases() throws Exception {
        final S3VanityAlias obsoleteAlias = alias("/old-promo", "vanity-1");
        whenCurrentVanities();
        when(repository.findByLookup(lookup)).thenReturn(List.of(obsoleteAlias));

        final Void result = invokeVoid(() -> service.publishAliases(context));

        Assert.assertNull(result);
        verify(endpointPublisher).deleteFilesFromEndpoint(BUCKET, PREFIX, "/old-promo");
        final List<S3VanityAlias> mappings = replacedMappings();
        Assert.assertTrue(mappings.isEmpty());
    }

    @Test
    public void publishAliasesShouldReturnDotDataExceptionAndRestoreDeletedAliasWhenRepositoryFails()
            throws Exception {
        final S3VanityAlias obsoleteAlias = alias("/old-promo", "vanity-1");
        whenCurrentVanities();
        when(repository.findByLookup(lookup)).thenReturn(List.of(obsoleteAlias));
        doThrow(new DotDataException("replace failed")).when(repository).replaceMappings(eq(lookup), any());

        final DotDataException result = Assert.assertThrows(DotDataException.class,
                () -> service.publishAliases(context));

        Assert.assertNotNull(result);
        verify(endpointPublisher).deleteFilesFromEndpoint(BUCKET, PREFIX, "/old-promo");
        verify(endpointPublisher).pushFileToEndpoint(BUCKET, REGION, PREFIX, "/old-promo", canonicalFile);
    }

    @Test
    public void unpublishAliasesShouldReturnVoidAndDeletePersistedAliases() throws Exception {
        final S3VanityAlias firstAlias = alias("/promo", "vanity-1");
        final S3VanityAlias secondAlias = alias("/landing", "vanity-2");
        when(repository.findByLookup(lookup)).thenReturn(List.of(firstAlias, secondAlias));

        final Void result = invokeVoid(() -> service.unpublishAliases(context));

        Assert.assertNull(result);
        verify(endpointPublisher).deleteFilesFromEndpoint(BUCKET, PREFIX, "/promo");
        verify(endpointPublisher).deleteFilesFromEndpoint(BUCKET, PREFIX, "/landing");
        verify(repository).deleteByLookup(lookup);
    }

    @Test
    public void unpublishAliasesShouldReturnDotDataExceptionAndKeepMappingWhenS3DeleteFails()
            throws Exception {
        final S3VanityAlias persistedAlias = alias("/promo", "vanity-1");
        when(repository.findByLookup(lookup)).thenReturn(List.of(persistedAlias));
        doThrow(new DotPublishingException("delete failed"))
                .when(endpointPublisher).deleteFilesFromEndpoint(BUCKET, PREFIX, "/promo");

        final DotDataException result = Assert.assertThrows(DotDataException.class,
                () -> service.unpublishAliases(context));

        Assert.assertNotNull(result);
        verify(repository, never()).deleteByLookup(lookup);
    }

    private Void invokeVoid(final VoidMethod method) throws Exception {
        method.invoke();
        return null;
    }

    private void whenCurrentVanities(final CachedVanityUrl... vanityUrls) {
        when(vanityUrlAPI.findByForward(host, language, CANONICAL_PATH, HttpStatus.SC_OK, true))
                .thenReturn(List.of(vanityUrls));
    }

    private CachedVanityUrl vanityUrl(final String uri, final String vanityUrlId) {
        return new CachedVanityUrl(vanityUrlId, uri, LANGUAGE_ID, HOST_ID, CANONICAL_PATH,
                HttpStatus.SC_OK, 0);
    }

    private S3VanityAlias alias(final String vanityPath, final String vanityUrlId) {
        return new S3VanityAlias(ENDPOINT_ID, HOST_ID, LANGUAGE_ID, CANONICAL_PATH, vanityPath,
                vanityUrlId, BUCKET, REGION, PREFIX);
    }

    private List<S3VanityAlias> replacedMappings() throws DotDataException {
        final ArgumentCaptor<List<S3VanityAlias>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).replaceMappings(eq(lookup), captor.capture());
        return captor.getValue();
    }

    @FunctionalInterface
    private interface VoidMethod {
        void invoke() throws Exception;
    }
}
