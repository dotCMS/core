package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link S3VanityAliasSupport}.
 */
public class S3VanityAliasSupportTest {

    private final S3VanityAliasSupport support = new S3VanityAliasSupport();

    @Test
    public void normalizeVanityPathShouldReturnOptionalWithStaticSafePath() {
        final String vanityPath = "  /promo\\area//landing  ";

        final Optional<String> result = support.normalizeVanityPath(vanityPath);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("/promo/area/landing", result.get());
    }

    @Test
    public void normalizeVanityPathShouldReturnEmptyOptionalForRegexLikePath() {
        final String vanityPath = "/promo/(.*)";

        final Optional<String> result = support.normalizeVanityPath(vanityPath);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void normalizeCanonicalPathShouldReturnOptionalWithInternalPath() {
        final String forwardTo = "  /content\\pages//home  ";

        final Optional<String> result = support.normalizeCanonicalPath(forwardTo);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("/content/pages/home", result.get());
    }

    @Test
    public void normalizeCanonicalPathShouldReturnEmptyOptionalForExternalUrl() {
        final String forwardTo = "https://example.com/content/pages/home";

        final Optional<String> result = support.normalizeCanonicalPath(forwardTo);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void isForwardActionShouldReturnTrueForHttpOkVanityAction() {
        final Contentlet vanityContentlet = vanityContentlet("/promo", "/home", HttpStatus.SC_OK, true);

        final boolean result = support.isForwardAction(vanityContentlet);

        Assert.assertTrue(result);
    }

    @Test
    public void isSupportedVanityUrlShouldReturnFalseForExternalForwardTarget() {
        final Contentlet vanityContentlet = vanityContentlet("/promo", "https://example.com/home",
                HttpStatus.SC_OK, true);

        final boolean result = support.isSupportedVanityUrl(vanityContentlet);

        Assert.assertFalse(result);
    }

    @Test
    public void toAliasMappingsShouldReturnDeduplicatedListByMaterializedVanityPath() {
        final S3VanityAliasContext context = context("/home");
        final List<CachedVanityUrl> vanityUrls = List.of(
                vanityUrl("first", "/promo", "/home"),
                vanityUrl("second", "/promo", "/home"));

        final List<S3VanityAlias> result = support.toAliasMappings(context, vanityUrls);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("first", result.get(0).vanityUrlId);
        Assert.assertEquals("/promo", result.get(0).vanityPath);
    }

    @Test
    public void toAliasMappingsShouldReturnOnlyAliasesWithSupportedStaticVanityPath() {
        final S3VanityAliasContext context = context("/home");
        final List<CachedVanityUrl> vanityUrls = List.of(
                vanityUrl("unsupported", "/promo/(.*)", "/home"),
                vanityUrl("supported", "/promo", "/home"));

        final List<S3VanityAlias> result = support.toAliasMappings(context, vanityUrls);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("supported", result.get(0).vanityUrlId);
        Assert.assertEquals("/promo", result.get(0).vanityPath);
    }

    private Contentlet vanityContentlet(final String uri, final String forwardTo,
                                        final int action, final boolean vanityUrl) {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.isVanityUrl()).thenReturn(vanityUrl);
        when(contentlet.getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR)).thenReturn((long) action);
        when(contentlet.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)).thenReturn(uri);
        when(contentlet.getStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR)).thenReturn(forwardTo);
        return contentlet;
    }

    private S3VanityAliasContext context(final String canonicalPath) {
        return new S3VanityAliasContext(
                new S3VanityAliasLookup("endpoint", "host", 1L, canonicalPath),
                "bucket",
                "region",
                "prefix",
                mock(Host.class),
                mock(Language.class),
                new File("page.html"),
                mock(AWSS3EndPointPublisher.class));
    }

    private CachedVanityUrl vanityUrl(final String id, final String uri, final String forwardTo) {
        return new CachedVanityUrl(id, uri, 1L, "host", forwardTo, HttpStatus.SC_OK, 0);
    }
}
