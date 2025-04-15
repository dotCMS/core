package com.dotcms.graphql.datafetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class CategoryFieldDataFetcherTest {

    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI  = APILocator.getHostAPI();
        userAPI  = APILocator.getUserAPI();
        user     = userAPI.getSystemUser();
        defaultHost    = hostAPI.findDefaultHost(user, false);
    }

    /**
     * MethodToTest {@link CategoryFieldDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link CategoryFieldDataFetcher} on a content that does not contain categories
     * ExpectedResult: Should return an empty list
     * @throws Exception
     */
    @Test
    public void testGetWithNullVar() throws Exception {
        final CategoryFieldDataFetcher fetcher = new CategoryFieldDataFetcher();
        final DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);
        final Field field = new Field("MyField");

        Mockito.when(environment.getContext()).thenReturn(DotGraphQLContext.createServletContext().with(
                user).build());
        Mockito.when(environment.getSource()).thenReturn(new Contentlet());
        Mockito.when(environment.getField()).thenReturn(field);

        final List result = fetcher.get(environment);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * MethodToTest {@link CategoryFieldDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link CategoryFieldDataFetcher} on a content that contains categories
     * ExpectedResult: Should return the list of categories
     * @throws Exception
     */
    @Test
    public void testGetWithNotNullVar() throws Exception {
        final CategoryFieldDataFetcher fetcher = new CategoryFieldDataFetcher();

        final DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);

        final Field field = new Field("categories");

        //Create Categories
        final Category categoryChild1 = new CategoryDataGen()
                .setCategoryName("RoadBike-" + System.currentTimeMillis()).setKey("RoadBike")
                .setKeywords("RoadBike").setCategoryVelocityVarName("roadBike").next();
        final Category categoryChild2 = new CategoryDataGen()
                .setCategoryName("MTB-" + System.currentTimeMillis()).setKey("MTB")
                .setKeywords("MTB").setCategoryVelocityVarName("mtb").next();
        final Category rootCategory = new CategoryDataGen()
                .setCategoryName("Bikes-" + System.currentTimeMillis())
                .setKey("Bikes").setKeywords("Bikes").setCategoryVelocityVarName("bikes")
                .children(categoryChild1, categoryChild2).next();

        // Get "News" content-type
        final ContentType contentType = TestDataUtils
                .getNewsLikeContentType("newsCategoriesTest" + System.currentTimeMillis(),
                        rootCategory.getInode());

        // Create dummy "News" content
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .host(defaultHost);

        contentletDataGen.setProperty("title", "Bicycle");
        contentletDataGen.setProperty("byline", "Bicycle");
        contentletDataGen.setProperty("story", "BicycleBicycleBicycle");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/bicycle");
        contentletDataGen.setProperty("categories", Map.of("categories", CollectionsUtils.list(categoryChild1, categoryChild2)));

        Mockito.when(environment.getContext())
                .thenReturn(DotGraphQLContext.createServletContext().with(
                        APILocator.systemUser()).build());
        Mockito.when(environment.getSource()).thenReturn(contentletDataGen.next());
        Mockito.when(environment.getField()).thenReturn(field);

        final List result = fetcher.get(environment);
        assertTrue(UtilMethods.isSet(result));
        assertEquals(2, result.size());
        assertEquals(categoryChild1, result.get(0));
        assertEquals(categoryChild2, result.get(1));

    }

}
