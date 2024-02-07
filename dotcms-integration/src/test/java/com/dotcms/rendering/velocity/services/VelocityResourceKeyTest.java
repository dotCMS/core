package com.dotcms.rendering.velocity.services;

import com.dotcms.rendering.velocity.directive.RenderParams;
import com.dotcms.rendering.velocity.directive.TemplatePathStrategy;
import com.dotcms.rendering.velocity.directive.TemplatePathStrategyResolver;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;

/**
 * Test of {@link VelocityResourceKey}
 */
public class VelocityResourceKeyTest {

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void velocityResourceKey_constructor_fs_container_path_test() throws DotDataException {
        final String path = "//demo.dotcms.com/application/containers/large-column";

        final Host host = mock(Host.class);
        final Language language = mock(Language.class);

        final Context context = mock(Context.class);
        final RenderParams params = new RenderParams(APILocator.systemUser(), language, host, PageMode.EDIT_MODE);
        final String[] arguments = new String[]{path, "1551200983126"};

        final TemplatePathStrategyResolver templatePathResolver = TemplatePathStrategyResolver.getInstance();
        final Optional<TemplatePathStrategy> strategy           = templatePathResolver.get(context, params, arguments);

        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(strategy.get().apply(context, params, arguments));

        Assert.assertEquals("/EDIT_MODE/%%demo#dotcms#com%application%containers%large-column/1551200983126.container", velocityResourceKey.path);
        Assert.assertEquals("%%demo#dotcms#com%application%containers%large-column", velocityResourceKey.id1);
        Assert.assertEquals("1551200983126", velocityResourceKey.id2);
        Assert.assertEquals(PageMode.EDIT_MODE, velocityResourceKey.mode);
        Assert.assertEquals(VelocityType.CONTAINER, velocityResourceKey.type);
        Assert.assertEquals("1", velocityResourceKey.language);
        Assert.assertEquals("EDIT_MODE-%%demo#dotcms#com%application%containers%large-column-1.container", velocityResourceKey.cacheKey);
    }


    @Test
    public void velocityResourceKey_constructor_db_container_test() throws DotDataException {

        final String path = "/PREVIEW_MODE/a050073a-a31e-4aab-9307-86bfb248096a/1551388637913.container";
        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(path);

        Assert.assertEquals("/PREVIEW_MODE/a050073a-a31e-4aab-9307-86bfb248096a/1551388637913.container", velocityResourceKey.path);
        Assert.assertEquals("a050073a-a31e-4aab-9307-86bfb248096a", velocityResourceKey.id1);
        Assert.assertEquals("1551388637913", velocityResourceKey.id2);
        Assert.assertEquals(PageMode.PREVIEW_MODE, velocityResourceKey.mode);
        Assert.assertEquals(VelocityType.CONTAINER, velocityResourceKey.type);
        Assert.assertEquals("1", velocityResourceKey.language);
        Assert.assertEquals("PREVIEW_MODE-a050073a-a31e-4aab-9307-86bfb248096a-1.container", velocityResourceKey.cacheKey);
    }

    @Test
    public void velocityResourceKey_constructor_page_lang2_test() throws DotDataException {

        final String path = "1EDIT_MODE/3bac9454-ccb1-497a-bf8d-4c3094abd3eb_2.dothtmlpage";
        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(path);

        Assert.assertEquals("/EDIT_MODE/3bac9454-ccb1-497a-bf8d-4c3094abd3eb_2.dothtmlpage", velocityResourceKey.path);
        Assert.assertEquals("3bac9454-ccb1-497a-bf8d-4c3094abd3eb", velocityResourceKey.id1);
        Assert.assertNull(velocityResourceKey.id2);
        Assert.assertEquals(PageMode.EDIT_MODE, velocityResourceKey.mode);
        Assert.assertEquals(VelocityType.HTMLPAGE, velocityResourceKey.type);
        Assert.assertEquals("2", velocityResourceKey.language);
        Assert.assertEquals("/EDIT_MODE/3bac9454-ccb1-497a-bf8d-4c3094abd3eb_2.dothtmlpage", velocityResourceKey.cacheKey);
    }

    @Test
    public void velocityResourceKey_constructor_contentlet_lang2_test() throws DotDataException {

        final String path = "1/PREVIEW_MODE/5a125bb6-4950-429a-9cfc-f1a9c8d90aa8_2.contentlet";
        final VelocityResourceKey velocityResourceKey =
                new VelocityResourceKey(path);

        Assert.assertEquals("/PREVIEW_MODE/5a125bb6-4950-429a-9cfc-f1a9c8d90aa8_2.contentlet", velocityResourceKey.path);
        Assert.assertEquals("5a125bb6-4950-429a-9cfc-f1a9c8d90aa8", velocityResourceKey.id1);
        Assert.assertNull(velocityResourceKey.id2);
        Assert.assertEquals(PageMode.PREVIEW_MODE, velocityResourceKey.mode);
        Assert.assertEquals(VelocityType.CONTENT, velocityResourceKey.type);
        Assert.assertEquals("2", velocityResourceKey.language);
        Assert.assertEquals("/PREVIEW_MODE/5a125bb6-4950-429a-9cfc-f1a9c8d90aa8_2.contentlet", velocityResourceKey.cacheKey);
    }

}
