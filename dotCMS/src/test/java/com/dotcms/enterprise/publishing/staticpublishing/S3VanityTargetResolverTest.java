package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotmarketing.beans.Host;
import com.dotmarketing.cms.urlmap.URLMapAPI;
import com.dotmarketing.cms.urlmap.URLMapInfo;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.filters.CMSFilter.IAmSubType;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link S3VanityTargetResolver}.
 */
public class S3VanityTargetResolverTest {

    private static final String CANONICAL_PATH = "/home";
    private static final long LANGUAGE_ID = 1L;

    private final CMSUrlUtil cmsUrlUtil = mock(CMSUrlUtil.class);
    private final HTMLPageAssetAPI htmlPageAssetAPI = mock(HTMLPageAssetAPI.class);
    private final URLMapAPI urlMapAPI = mock(URLMapAPI.class);
    private final Host host = mock(Host.class);
    private final Language language = mock(Language.class);
    private final User user = null;
    private final S3VanityAliasPublishContext context = new S3VanityAliasPublishContext(
            "endpoint", "bucket", "region", "prefix", host, language, mock(AWSS3EndPointPublisher.class));
    private final S3VanityTargetResolver resolver = new S3VanityTargetResolver(cmsUrlUtil,
            htmlPageAssetAPI, urlMapAPI, mock(ContentletAPI.class), mock(FileAssetAPI.class));

    @Test
    public void resolveShouldReturnOptionalWithPageTargetWhenPathResolvesDirectlyToHtmlPage()
            throws Exception {
        final IHTMLPage htmlPage = mock(IHTMLPage.class);
        when(language.getId()).thenReturn(LANGUAGE_ID);
        when(htmlPageAssetAPI.getPageByPath(CANONICAL_PATH, host, LANGUAGE_ID, true)).thenReturn(htmlPage);

        final Optional<S3VanityResolvedTarget> result = resolver.resolve(CANONICAL_PATH, context, user);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(DotAsset.PAGE, result.get().type);
        Assert.assertEquals(CANONICAL_PATH, result.get().canonicalPath);
        Assert.assertEquals(htmlPage, result.get().htmlPage);
    }

    @Test
    public void resolveShouldReturnOptionalWithIndexPageTargetWhenDotCmsClassifiesPathAsPageIndex()
            throws Exception {
        final IHTMLPage indexPage = mock(IHTMLPage.class);
        when(language.getId()).thenReturn(LANGUAGE_ID);
        when(htmlPageAssetAPI.getPageByPath(CANONICAL_PATH, host, LANGUAGE_ID, true))
                .thenReturn(null, indexPage);
        when(cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS, CANONICAL_PATH, host, LANGUAGE_ID))
                .thenReturn(Tuple.of(IAm.PAGE, IAmSubType.PAGE_INDEX));

        final Optional<S3VanityResolvedTarget> result = resolver.resolve(CANONICAL_PATH, context, user);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(DotAsset.PAGE_INDEX, result.get().type);
        Assert.assertEquals(CANONICAL_PATH, result.get().canonicalPath);
        Assert.assertEquals(indexPage, result.get().htmlPage);
    }

    @Test
    public void resolveShouldReturnOptionalWithUrlMapTargetWhenDotCmsClassifiesPathAsUrlMap()
            throws Exception {
        final URLMapInfo urlMapInfo = mock(URLMapInfo.class);
        final IHTMLPage detailPage = mock(IHTMLPage.class);
        when(language.getId()).thenReturn(LANGUAGE_ID);
        when(htmlPageAssetAPI.getPageByPath(CANONICAL_PATH, host, LANGUAGE_ID, true)).thenReturn(null);
        when(cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS, CANONICAL_PATH, host, LANGUAGE_ID))
                .thenReturn(Tuple.of(IAm.PAGE, IAmSubType.PAGE_URL_MAP));
        when(urlMapInfo.getDetailtPageUri()).thenReturn("/detail");
        when(urlMapAPI.processURLMap(any(UrlMapContext.class))).thenReturn(Optional.of(urlMapInfo));
        when(htmlPageAssetAPI.getPageByPath("/detail", host, LANGUAGE_ID, true)).thenReturn(detailPage);

        final Optional<S3VanityResolvedTarget> result = resolver.resolve(CANONICAL_PATH, context, user);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(DotAsset.PAGE_URL_MAP, result.get().type);
        Assert.assertEquals(CANONICAL_PATH, result.get().canonicalPath);
        Assert.assertEquals(detailPage, result.get().htmlPage);
        Assert.assertEquals(urlMapInfo, result.get().urlMapInfo);
    }

    @Test
    public void resolveShouldReturnEmptyOptionalWhenDotCmsCannotResolveStaticTarget()
            throws Exception {
        when(language.getId()).thenReturn(LANGUAGE_ID);
        when(htmlPageAssetAPI.getPageByPath(CANONICAL_PATH, host, LANGUAGE_ID, true)).thenReturn(null);
        when(cmsUrlUtil.resolveResourceType(IAm.NOTHING_IN_THE_CMS, CANONICAL_PATH, host, LANGUAGE_ID))
                .thenReturn(Tuple.of(IAm.NOTHING_IN_THE_CMS, IAmSubType.NONE));

        final Optional<S3VanityResolvedTarget> result = resolver.resolve(CANONICAL_PATH, context, user);

        Assert.assertFalse(result.isPresent());
    }
}
