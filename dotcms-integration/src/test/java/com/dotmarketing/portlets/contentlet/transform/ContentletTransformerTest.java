package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.api.APIProvider;
import com.dotcms.api.APIProvider.Builder;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.google.common.io.Files;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.strategy.AbstractTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.DefaultTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.FileAssetViewStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolverImpl;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.workflows.actionlet.copy.AssertionStrategy;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.control.Try;
import javax.enterprise.context.ApplicationScoped;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.MIMETYPE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.TITLE_FIELD;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.UNDERLYING_FILENAME;
import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletTransformerTest extends IntegrationTestBase {

    static String serializePath;
    static long langId;
    static File directory;
    static Host site;

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();

        final ContentType employeeLikeContentType = TestDataUtils.getEmployeeLikeContentType();
        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();
        final ContentType newsLikeContentType = TestDataUtils.getNewsLikeContentType();

        site = new SiteDataGen().nextPersisted();

        // Creating the contentlets for they will be pulled-out from the index
        for (int i = 0; i <= 10; i++) {
            TestDataUtils.getEmployeeContent(true, 1, employeeLikeContentType.id(),
                    site);
            TestDataUtils.getBannerLikeContent(true, 1, bannerLikeContentType.id(),
                    site);
            TestDataUtils.getNewsContent(true, 1, newsLikeContentType.id(), site);
        }

        serializePath = Files.createTempDir().getCanonicalPath();
        langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        directory = new File(serializePath);
        directory.mkdirs();

    }

    @Test
    public void Transformer_Simple_Test() throws DotDataException, DotSecurityException {

        List<Contentlet> list = APILocator.getContentletAPI().findContentletsByHost(site,
                APILocator.systemUser(), false);
        list = list.stream().filter(Objects::nonNull).collect(Collectors.toList());
        assertFalse("I was expecting at least 20 contentlets returned from the index",list.isEmpty());
        final List<Map<String, Object>> transformedList = new DotTransformerBuilder().defaultOptions().content(list).build().toMaps();

        assertEquals(list.size(), transformedList.size());
        for(int i=0; i < list.size(); i++){
            final Contentlet original = list.get(i);
            final Map<String,Object> transformed = transformedList.get(i);
            //Basic properties must exist on both
            assertEquals(original.getMap().get(IDENTIFIER_KEY),transformed.get(IDENTIFIER_KEY));
            assertEquals(original.getMap().get(Contentlet.INODE_KEY),transformed.get(Contentlet.INODE_KEY));
            assertEquals(original.getMap().get(Contentlet.LANGUAGEID_KEY),transformed.get(Contentlet.LANGUAGEID_KEY));
            assertEquals(original.getMap().get(Contentlet.MOD_DATE_KEY),transformed.get(Contentlet.MOD_DATE_KEY));
            assertEquals(original.getMap().get(Contentlet.MOD_USER_KEY),transformed.get(Contentlet.MOD_USER_KEY));

            //New Properties expected
            assertNotNull(transformed.get(Contentlet.TITTLE_KEY));
            assertNotNull(transformed.get(Contentlet.CONTENT_TYPE_KEY));
            assertNotNull(transformed.get(HTMLPageAssetAPI.URL_FIELD));

            //Forbidden properties Must Not be part of the result
            for(final String property : AbstractTransformStrategy.privateInternalProperties){
                assertFalse("found private property:" + property,transformed.containsKey(property));
            }
        }

    }

    /**
     * Method to test {@link DotContentletTransformer#toMaps()} {@link com.dotmarketing.portlets.contentlet.transform.strategy.CategoryViewStrategy#transform(Contentlet, Map, Set, User)}
     * Given Scenario: We create categories. One of the categories has a null value preset on the keywords. Simply we want to test that the Transformers can handle null values on the Categories map.
     * Expected Result: We should be able to retrieve categories set them through the Transformer and validate that the null values remain there.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Null_Value_On_Category() throws DotDataException, DotSecurityException {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),false);

        //Create Categories setting Null values on the keywords
        final CategoryDataGen rootCategoryDataGen = new CategoryDataGen().setCategoryName("Bikes-"+System.currentTimeMillis()).setKey("Bikes").setKeywords(null).setCategoryVelocityVarName("bikes");
        final Category child1 = new CategoryDataGen().setCategoryName("RoadBike-"+System.currentTimeMillis()).setKey("RoadBike").setKeywords(null).setCategoryVelocityVarName("roadBike").next();

        final Category rootCategory = rootCategoryDataGen.children(child1).nextPersisted();

        // Get "News" content-type
        final ContentType contentType = TestDataUtils
                .getNewsLikeContentType("newsCategoriesTest" + System.currentTimeMillis(),
                        rootCategory.getInode());

        // Create dummy "News" content
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .host(defaultHost).setProperty("title", "Bicycle").setProperty("byline", "Bicycle")
                .setProperty("story", "BicycleBicycleBicycle")
                .setProperty("sysPublishDate", new Date()).setProperty("urlTitle", "/news/bicycle")
                //Set the categories
                .addCategory(child1);

            final Contentlet original = contentletDataGen.nextPersisted();
            //Create a Transformer set to process Categories
            final List<Map<String, Object>> transformedList = new DotTransformerBuilder().categoryToMapTransformer().content(original).build().toMaps();
            final Map<String,Object> transformed = transformedList.get(0);
            final Map<String,Map<String,Object>>  map = (Map<String,Map<String,Object>>)transformed.get("categories");
            final List<Map<String,Object>> categories = (List<Map<String,Object>>)map.get("categories");
            final Map<String,Object>  category = categories.get(0);
            assertNull(category.get("keywords"));
    }

    @Test
    public void Test_Hydrate_Contentlet_WithUrl() throws DotDataException {
        final ContentType testContentType = TestDataUtils.getBannerLikeContentType();
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = "1234";
        final Identifier identifierObject = new Identifier();
        final String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(testContentType);
        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(HTMLPageAssetAPI.URL_FIELD, urlExpected);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);
        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final APIProvider toolbox = new Builder().withContentHelper(contentHelper).build();

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolbox);

        final Contentlet newContentlet = new DotContentletTransformerImpl(singletonList(contentlet), resolver, DotContentletTransformerImpl.defaultOptions, null).hydrate().get(0);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet); //This method now returns a new instance. A copy of the original contentlet
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertEquals(newContentlet.getMap().get(ContentletForm.IDENTIFIER_KEY), identifier);
        assertEquals(newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD), urlExpected);
        assertTrue(newContentlet.getMap().containsKey(DefaultTransformStrategy.SHORTY_ID));
    }

    @Test
    public void Test_Hydrate_Contentlet_Without_Url_And_AssetName_Does_Not_Exist() throws DotDataException {
        final ContentType testContentType = TestDataUtils.getBannerLikeContentType();
        final String  anyUrl = "anyUrl";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return anyUrl;
            }
        };
        final String identifier = "1234";

        final APIProvider toolbox = new Builder().withContentHelper(contentHelper).build();

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolbox);

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(testContentType);
        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);

        when(identifierAPI.find(identifier)).thenReturn(null);

        final Contentlet newContentlet = new DotContentletTransformerImpl(
                  singletonList(contentlet), resolver,
                  DotContentletTransformerImpl.defaultOptions, null
        ).hydrate().get(0);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet); //This method now returns a new instance. A copy of the original contentlet
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertEquals(identifier, newContentlet.getMap().get(ContentletForm.IDENTIFIER_KEY));
        assertEquals(anyUrl, newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD));
    }

    @Test
    public void Test_Hydrate_Contentlet_Without_Url() throws DotDataException {
        final ContentType testContentType = TestDataUtils.getBannerLikeContentType();
        final String urlExpected = "home_page";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return urlExpected;
            }
        };

        final APIProvider toolBox = new Builder().withContentHelper(contentHelper).build();

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolBox);

        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(testContentType);
        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);
        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final Contentlet newContentlet = new DotContentletTransformerImpl(
                singletonList(contentlet), resolver,
                DotContentletTransformerImpl.defaultOptions, null).hydrate().get(0);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet);
        assertFalse(contentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertTrue(newContentlet.getMap().containsKey(HTMLPageAssetAPI.URL_FIELD));
        assertEquals(urlExpected, newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD));
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertTrue(newContentlet.getMap().containsKey(DefaultTransformStrategy.SHORTY_ID));
    }


    /**
     * this test creates a widget type that has constant fields and a content of this type.
     * It updates the constant field values and insures that the new constant field values are both
     * in the content object and the newly transformed map
     * @throws Exception
     */
    @Test
    public void Test_Constant_fields_are_in_map() throws Exception {

        // create a widget and a content of that widget;
        final ContentType widgetLikeContenType = TestDataUtils.getWidgetLikeContentType();
        final Contentlet newContentlet = TestDataUtils.getWidgetContent(true, 1, widgetLikeContenType.id());
        assertNotNull(newContentlet);

        final List<Field> constants = widgetLikeContenType.fields(ConstantField.class);
        
        final String constantVar1 = constants.get(0).variable();
        final String constantVar2 = constants.get(1).variable();
        
        // assert our content type has no constant values in it
        assertTrue(constants.size()>1);
        assertNull(constants.get(0).values());
        assertNull(constants.get(1).values());
        
        // assert our content Map has no constant values in it
        Map<String,Object> map = new DotTransformerBuilder().defaultOptions().content(newContentlet).build().toMaps().get(0);
        assertNull(map.get(constantVar1));
        assertNull(map.get(constantVar2));
        
        
        // update our Content Type constants to have values
        final List<Field> allFields = new ArrayList<>(widgetLikeContenType.fields());
        allFields.removeIf(constants::contains);
        for(final Field field : constants) {
            final String value = UUIDGenerator.generateUuid();
            Field newField = FieldBuilder.builder(field).values(value).build();
            allFields.add(newField);
        }
        ContentType updatedType =APILocator.getContentTypeAPI(APILocator.systemUser()).save(widgetLikeContenType, allFields);
        
        // contentlet.constantValue now have values
        assertNotNull(newContentlet.get(constantVar1));  
        assertNotNull(newContentlet.get(constantVar2));  
        
        // field.value == contentlet.constantValue 
        assertEquals(updatedType.fieldMap().get(constantVar1).values(), newContentlet.get(constantVar1));  
        assertEquals(updatedType.fieldMap().get(constantVar2).values(), newContentlet.get(constantVar2));  

        
        map = new DotTransformerBuilder().defaultOptions().content(newContentlet).build().toMaps().get(0);


        // contentlet.constantValue ==  map.values
        assertEquals(newContentlet.get(constantVar1), map.get(constantVar1));  
        assertEquals(newContentlet.get(constantVar2), map.get(constantVar2));  
        
    }

    /**
     * Given scenario: We create a dotAsset then we transform it and then we call getContentPrintableMap which internally does call the transformers again.
     * Meaning we're pushing the contentlet twice into the transformers pipeline.
     * Expected results: We should still get all the file related properties with the expected values.
     * @throws Exception
     */
    @Test
    public void Test_DotAsset_FileAsset_Pushed_Back_into_Transformer() throws Exception {
        final User systemUser = APILocator.systemUser();
        when(Config.CONTEXT.getMimeType(Mockito.endsWith(".jpg"))).thenReturn("image/jpeg");
        when(Config.CONTEXT.getMimeType(Mockito.endsWith(".jpeg"))).thenReturn("image/jpeg");
        final Contentlet dotAssetLikeContentlet = TestDataUtils.getDotAssetLikeContentlet();
        final Contentlet transformedDotAsset = new DotTransformerBuilder().defaultOptions().content(dotAssetLikeContentlet).build().hydrate().get(0);
        final Map<String, Object> map1 = ContentletUtil.getContentPrintableMap(
                systemUser, transformedDotAsset, true);
        Assert.assertTrue(map1.get("asset") instanceof String);
        Assert.assertNotEquals(map1.get("title"),"unknown");
        Assert.assertTrue((long)map1.get("size") > 0);
        Assert.assertNotEquals(map1.get("mimeType"),"unknown");

        final Contentlet fileAssetLikeContentlet = TestDataUtils.getFileAssetContent(true, langId, TestFile.GIF);
        final Contentlet transformedFileAsset = new DotTransformerBuilder().defaultOptions().content(fileAssetLikeContentlet).build().hydrate().get(0);
        final Map<String, Object> map2 = ContentletUtil.getContentPrintableMap(systemUser, transformedFileAsset, true);
        Assert.assertTrue(map2.get("fileAsset") instanceof String);
        Assert.assertNotEquals(map2.get("title"),"unknown");
        Assert.assertNotEquals(map2.get("mimeType"),"unknown");
        Assert.assertTrue((long)map2.get("size") > 0);

    }

    /**
     * Given scenario: We create a dotAsset-like contentlet, set a "title" property,
     * and then call getContentPrintableMap, which should return a map of the contentlet's properties.
     * Expected result: The "title" property in the resulting map should match the custom title set
     * in the original contentlet.
     * @throws Exception in case of errors during the mapping process
     */
    @Test
    public void Test_DotAsset_With_Title_Property_Pushed_into_Transformer() throws Exception {
        final User systemUser = APILocator.systemUser();
        final Contentlet dotAssetLikeContentlet = TestDataUtils.getDotAssetLikeContentlet();
        dotAssetLikeContentlet.setProperty("title", "my custom title");

        final Map<String, Object> map1 = ContentletUtil.getContentPrintableMap(
                systemUser, dotAssetLikeContentlet, true);
        Assert.assertEquals("my custom title", map1.get("title"));
    }

    /**
     * Given scenario: We create a dotAsset-like contentlet without setting a "title" property
     * and call getContentPrintableMap, which should return a map of the contentlet's properties.
     * Expected result: The "title" property in the resulting map should match the asset title.
     * @throws Exception in case of errors during the mapping process
     */
    @Test
    public void Test_DotAsset_Without_Title_Property_Pushed_into_Transformer() throws Exception {
        final User systemUser = APILocator.systemUser();
        final Contentlet dotAssetLikeContentlet = TestDataUtils.getDotAssetLikeContentlet();

        final Map<String, Object> map1 = ContentletUtil.getContentPrintableMap(
                systemUser, dotAssetLikeContentlet, true);
        Assert.assertEquals("test.jpg", map1.get("title"));
    }

    /**
     * Given Scenario: We have a contentlet with a dotAsset field, and we push it into the transformer prepared with the FILEASSET_VIEW strategy.
     * Expected Result: We should get the dotAsset field with the expected properties.
     * @throws Exception
     */
    @Test
    public void Test_DotAsset_Field_Pushed_Into_Transformer() throws Exception {

        final Contentlet dotAssetLikeContentlet = TestDataUtils.getDotAssetLikeContentlet();
        ContentletDataGen.publish(dotAssetLikeContentlet);

        final List<Field> fields = List.of(
                new FieldDataGen()
                        .name("title")
                        .velocityVarName("title")
                        .next(),
                new FieldDataGen()
                        .type(FileField.class)
                        .name("dotAsset")
                        .velocityVarName("dotAsset")
                        .next()
        );

        final String contentTypeName =  "withDotAssetType"+System.currentTimeMillis();

        final ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeName)
                .fields(fields)
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "Test")
                .setProperty("dotAsset", dotAssetLikeContentlet.getIdentifier())
                .nextPersistedAndPublish();

        final Contentlet transformed = new DotTransformerBuilder().hydratedContentMapTransformer().content(contentlet).build().hydrate().get(0);
        Map<?,?> asset = (Map)transformed.getMap().get("dotAsset");
        Assert.assertNotNull(asset);
        Assert.assertFalse(asset.isEmpty());
        Assert.assertEquals("dot_asset", asset.get("type"));
    }


    @DataProvider
    public static Object[] listTestCases() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final User user = APILocator.systemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final int limit = 300;
        final boolean respectFrontEndUsers = false;
        return new Object[]{
                new CompatibilityTestCase(BaseContentType.getBaseContentType(0),
                        contentletAPI.search("+basetype:0", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(1),
                        contentletAPI.search("+basetype:1", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(2),
                        contentletAPI.search("+basetype:2", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(3),
                        contentletAPI.search("+basetype:3", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(4),
                        contentletAPI.search("+basetype:4", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(5),
                        contentletAPI.search("+basetype:5", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(6),
                        contentletAPI.search("+basetype:6", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(7),
                        contentletAPI.search("+basetype:7", limit, 0, null, user, respectFrontEndUsers)),
                new CompatibilityTestCase(BaseContentType.getBaseContentType(8),
                        contentletAPI.search("+basetype:8", limit, 0, null, user, respectFrontEndUsers))
        };
    }

    private static class CompatibilityTestCase {
        final BaseContentType baseContentType;
        final List<Contentlet> contentlets;

        CompatibilityTestCase(final BaseContentType baseContentType,
                final List<Contentlet> contentlets) {
            this.baseContentType = baseContentType;
            this.contentlets = contentlets;
        }

        @Override
        public String toString() {
            return "CompatibilityTestCase{" + "baseContentType=" + baseContentType +'}';
        }
    }

    /**
     * Given Scenario: We have the old and deprecated implementation vs the new one
     * Expected Result: Both results must match
     * @param testCase
     */
    @Test
    @UseDataProvider("listTestCases")
    public void Transformer_Backwards_Compatibility_Test(final CompatibilityTestCase testCase)
            throws DotDataException {

        final List <Contentlet> list = testCase.contentlets;

        if(list.isEmpty()){
           Logger.warn(ContentletTransformerTest.class, String.format("Unable to get test samples of type `%s`",testCase.baseContentType.name()));
           return;
        }

        final Set<String> privateInternalProperties = AbstractTransformStrategy.privateInternalProperties;

        final String baseTypeName = testCase.baseContentType.name();

        final ContentletToMapTransformer contentletToMapTransformer = new ContentletToMapTransformer(list);

        final List<Map<String, Object>> transformedList1 = contentletToMapTransformer.toMaps();

        final DotContentletTransformer dotTransformer = new DotTransformerBuilder().defaultOptions().content(list).build();

        final List<Map<String, Object>> transformedList2 = dotTransformer.toMaps();

        assertEquals(transformedList1.size(), transformedList2.size());
        for (int i = 0; i < list.size(); i++) {

            final Contentlet original = list.get(i);
            final ContentType type = original.getContentType();
            final List<Field> storyBlockFields = type.fields(StoryBlockField.class);
            //We compare the transformation results are the same using the default options on the new Transformer
            final Map<String, Object> sourceMap = transformedList1.get(i);
            final Map<String, Object> copyMap = transformedList2.get(i);

            final String missingKeys = sourceMap.keySet().stream()
                    .filter(key -> !copyMap.containsKey(key)).collect(Collectors.joining(","));

            assertTrue(String.format(" baseType `%s` should have same (or more) number of properties. Missing properties %s" ,baseTypeName, missingKeys),copyMap.size() >= sourceMap.size());
            final String assertMessage =  "Base contentType: `%s` , content: `%s` ,  key: `%s` ";

            for (final String propertyName : sourceMap.keySet()) {

                final Object object1 = sourceMap.get(propertyName);
                final Object object2 = copyMap.get(propertyName);

                if(null != object1 && object2 == null &&  privateInternalProperties.contains(propertyName)){
                   //We're looking at private property that exists in the source but was removed on the copy. which is fine.
                   continue;
                }

                if(object1 instanceof File){
                    //Binaries are now formatted to their /dA/ path form.
                    final String dAPath = "/dA/%s/%s/";
                    final String binaryPath = String.format(dAPath, sourceMap.get("identifier"),propertyName);
                    assertTrue(String.format(assertMessage, baseTypeName, original, propertyName), object2.toString().contains(binaryPath));
                    continue;
                }

                assertNotNull(String.format("Object with key `%s` is null why??",object2), object2);
                final boolean isStoryBlockField =
                        storyBlockFields.stream().anyMatch(field -> propertyName.equals(field.variable()));
                if (isStoryBlockField) {
                    final LinkedHashMap<String, Object> jsonMap =
                            Try.of(() -> APILocator.getStoryBlockAPI().toMap(object1)).getOrElse(new LinkedHashMap<>());
                    assertEquals(String.format(assertMessage, baseTypeName, original, propertyName), jsonMap, object2);
                } else {
                    assertEquals(String.format(assertMessage, baseTypeName, original.getIdentifier(), propertyName), object1, object2);
                }
            }
        }
        Logger.warn(ContentletTransformerTest.class, String.format("Test using samples of type `%s` successfully completed. ",baseTypeName));

        final List<Contentlet> hydrated1 = contentletToMapTransformer.hydrate();
        final List<Contentlet> hydrated2 = dotTransformer.hydrate();

        assertEquals(hydrated1.size(), hydrated2.size());

        for (int i = 0; i < list.size(); i++) {
            final Contentlet contentlet1 = hydrated1.get(i);
            final Contentlet contentlet2 = hydrated2.get(i);
            assertEquals(contentlet1.getIdentifier(),contentlet2.getIdentifier());
            final ContentType type = contentlet1.getContentType();
            final List<Field> storyBlockFields = type.fields(StoryBlockField.class);

            for (final String propertyName : contentlet1.getMap().keySet()) {
                final Object object1 = contentlet1.getMap().get(propertyName);
                final Object object2 = contentlet2.getMap().get(propertyName);

                if(null != object1 && object2 == null &&  privateInternalProperties.contains(propertyName)){
                    //We're looking at private property that exists in the source but was removed on the copy. which is fine.
                    continue;
                }

                if(object1 instanceof File){
                    //Binaries are now formatted to their /dA/ path form.
                    final String dAPath = "/dA/%s/%s/";
                    final String binaryPath = String.format(dAPath, contentlet1.getMap().get("identifier"),propertyName);
                    assertTrue(String.format("Base contentType: `%s` , content: `%s` ,  key: `%s` ", baseTypeName, contentlet1, propertyName), object2.toString().contains(binaryPath));
                    continue;
                }

                assertNotNull(String.format("Object with key `%s` is null why??", object2), object2);
                final boolean isStoryBlockField =
                        storyBlockFields.stream().anyMatch(field -> propertyName.equals(field.variable()));
                if (isStoryBlockField) {
                    final LinkedHashMap<String, Object> jsonMap =
                            Try.of(() -> APILocator.getStoryBlockAPI().toMap(object1)).getOrElse(new LinkedHashMap<>());
                    assertEquals(String.format("Base contentType: `%s` , content: `%s` ,  key: `%s` ", baseTypeName, contentlet1, propertyName), jsonMap, object2);
                } else {
                    assertEquals(String.format("Base contentType: `%s` , content: `%s` ,  key: `%s` ", baseTypeName, contentlet1, propertyName), object1, object2);
                }
            }
        }
    }

    private ContentType mockFileAssetContentType(){
        return new FileAssetContentType(){

            @Override
            public String name() {
                return "File Asset";
            }

            @Override
            public String id() {
                return "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";
            }

            @Override
            public String description() {
                return "Default structure for all uploaded files";
            }

            @Override
            public String variable() {
                return "FileAsset";
            }
        };
    }

    /**
     * Given scenario: This Test simply does a verification of the mechanism that instantiates strategies given a Content Type
     * Expected results: The additional info is present on the resulting view
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Contentlet_To_Map_View_For_FileAsset() throws DotDataException, DotSecurityException {

        final String systemUserId =  APILocator.systemUser().getUserId();

        final String urlExpected = "/index";
        final String identifier = "identifier";
        final String modeUser = "modUser";
        final String inode = "inode";
        final String mimeType = "any";
        final String fileName = "image";
        final String underlyingFileName = "image.png";
        final long fileSize = 100;
        final int width = 40;
        final int height = 50;

        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final Identifier identifierObject = mock(Identifier.class);

        when(identifierAPI.find(identifier)).thenReturn(identifierObject);
        when(identifierObject.getAssetName()).thenReturn(urlExpected);
        when(identifierObject.getId()).thenReturn(identifier);
        when(identifierObject.getPath()).thenReturn("/path/");

        final UserAPI userAPI = mock(UserAPI.class);

        final User user = mock(User.class);

        when(userAPI.loadUserById(modeUser)).thenReturn(user);

        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return urlExpected;
            }
        };

        final APIProvider toolBox = new Builder().withIdentifierAPI(identifierAPI)
                .withFileAssetAPI(fileAssetAPI).withUserAPI(APILocator.getUserAPI())
                .withContentHelper(contentHelper).build();

        final Map<BaseContentType, Supplier<AbstractTransformStrategy>> strategyTriggeredByBaseType = of(
                BaseContentType.FILEASSET, () -> new FileAssetViewStrategy(toolBox)
        );

        final Supplier<DefaultTransformStrategy>  supplier = ()-> new DefaultTransformStrategy(toolBox);

        final StrategyResolverImpl resolver = new StrategyResolverImpl(strategyTriggeredByBaseType, of(), supplier);

        final FileAssetContentType fileAssetContentType = mock(FileAssetContentType.class);
        when(fileAssetContentType.baseType()).thenReturn(BaseContentType.FILEASSET);
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getModUser()).thenReturn(modeUser);

        final Map<String, Object> map = new HashMap<>();

        final List<ContentType> byType = APILocator.getContentTypeAPI(APILocator.systemUser()).findByType(BaseContentType.FILEASSET);
        final String anyFileAssetSubType = byType.get(0).id();

        map.put(Contentlet.STRUCTURE_INODE_KEY, anyFileAssetSubType);
        map.put(IDENTIFIER_KEY, identifier);
        map.put(Contentlet.LANGUAGEID_KEY, 1L);
        map.put(Contentlet.INODE_KEY, inode);
        map.put(Contentlet.CONTENT_TYPE_KEY, fileAssetContentType);
        map.put(Contentlet.BASE_TYPE_KEY, BaseContentType.FILEASSET);

        final ImageField field = mock(ImageField.class);
        when(field.variable()).thenReturn("imageVar");
        final Optional<Field> titleImage = Optional.of(field);
        map.put(Contentlet.TITLE_IMAGE_KEY, titleImage);
        map.put(Contentlet.HAS_TITLE_IMAGE_KEY, true);

        when(contentlet.getMap()).thenReturn(map);

        when(contentlet.getModUser()).thenReturn(systemUserId);

        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        when(contentlet.getTitleImage()).thenReturn(titleImage);

        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());

        when(contentlet.getIdentifier()).thenReturn(identifier);

        final Set<TransformOptions> options = EnumSet.of(
                COMMON_PROPS, VERSION_INFO
        );

        final FileAsset fileAsset = mock(FileAsset.class);
        when(fileAsset.getIdentifier()).thenReturn(identifier);
        when(fileAsset.getContentType()).thenReturn(fileAssetContentType);
        when(fileAsset.getContentTypeId()).thenReturn(anyFileAssetSubType);
        when(fileAsset.getMimeType()).thenReturn(mimeType);
        when(fileAsset.getFileName()).thenReturn(fileName);
        when(fileAsset.getFileSize()).thenReturn(fileSize);
        when(fileAsset.getUnderlyingFileName()).thenReturn(underlyingFileName);
        when(fileAsset.getWidth()).thenReturn(width);
        when(fileAsset.getHeight()).thenReturn(height);
        when(fileAsset.isImage()).thenReturn(true);

        when(fileAssetAPI.fromContentlet(any(Contentlet.class))).thenReturn(fileAsset);

        final List<Map<String, Object>> maps = new DotContentletTransformerImpl(
                singletonList(contentlet),
                resolver,
                options, user).toMaps();

        final Map<String, Object> mapView = maps.get(0);

        final String returnedMimeType = (String)mapView.get(MIMETYPE_FIELD);
        final String title = (String)mapView.get(TITLE_FIELD);
        final String returnedIdentifier = (String)mapView.get(IDENTIFIER_KEY);
        final String parent = (String)mapView.get("parent");
        final String extension = (String)mapView.get("extension");
        final Integer returnedWidth = (Integer)mapView.get("width");
        final Integer returnedHeight = (Integer)mapView.get("height");
        final String returnedUnderlyingFileName = (String)mapView.get(UNDERLYING_FILENAME);

        //FLAG Alias is OFF
        assertFalse(mapView.containsKey("fileName"));
        assertFalse(mapView.containsKey("fileSize"));
        //Flag Metadata is OFF

        assertEquals(identifier, returnedIdentifier);
        assertEquals(urlExpected, title);
        assertEquals(mimeType, returnedMimeType);
        assertEquals("png",extension );
        assertEquals(StringPool.BLANK, parent);
        assertEquals(height, returnedHeight.intValue());
        assertEquals(width, returnedWidth.intValue());
        assertEquals(underlyingFileName, returnedUnderlyingFileName);

    }

    /**
     * Given Scenario: This takes samples of content transforms the original then serialize it the recovers the serialized content and transforms the result then compares the two contentlet
     * Expected Result: Both results must match
     * @param serializationTestCase
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("listSerializeTestCases")
    public void Test_Serialize_Contentlet_Then_Recover_Then_Transform(final SerializationTestCase serializationTestCase)
            throws IOException, ClassNotFoundException, DotDataException {

        final File file = new File(directory,String.format("%s.serialized",System.currentTimeMillis()));
        file.createNewFile();

        final Contentlet original = serializationTestCase.contentlet;
        serializeContentlet(serializationTestCase.contentlet, file);
        final Contentlet copy = readSerializedContentlet(file);
        validateTransformation(original, copy);
        final AssertionStrategy assertionStrategy = serializationTestCase.assertionStrategy;
        if(null != assertionStrategy) {
            assertionStrategy.apply(original, copy);
        }
    }

    private static class SerializationTestCase {

        final Contentlet contentlet;
        final AssertionStrategy assertionStrategy;

        SerializationTestCase(final Contentlet contentlet,
                final AssertionStrategy assertionStrategy) {
            this.contentlet = contentlet;
            this.assertionStrategy = assertionStrategy;
        }
    }

    @DataProvider
    public static Object[] listSerializeTestCases() throws Exception {

        IntegrationTestInitService.getInstance().init();

        final FileAssetAPI assetAPI = APILocator.getFileAssetAPI();
        final HTMLPageAssetAPI pageAssetAPI = APILocator.getHTMLPageAssetAPI();

        return new Object[]{
                new SerializationTestCase(((Supplier<Contentlet>) () -> {
                    final Contentlet contentlet = TestDataUtils.getFileAssetSVGContent(true, langId);
                    return assetAPI.fromContentlet(contentlet);
                }).get(), fileAssetValidation),
                new SerializationTestCase(((Supplier<Contentlet>) () -> {
                    final Contentlet contentlet = TestDataUtils.getFileAssetContent(true, langId);
                    return assetAPI.fromContentlet(contentlet);
                }).get(), fileAssetValidation),
                new SerializationTestCase(((Supplier<Contentlet>) () -> {
                    final Contentlet contentlet = TestDataUtils.getPageContent(true, langId);
                    return pageAssetAPI.fromContentlet(contentlet);
                }).get(), pageValidation),
                new SerializationTestCase(TestDataUtils.getDotAssetLikeContentlet(), null),
                new SerializationTestCase(TestDataUtils.getBlogContent(true, langId), null)
        };
    }

    private void validateTransformation(final Contentlet original, final Contentlet copy){
        final DotContentletTransformer transformer1 = new DotTransformerBuilder().defaultOptions().content(original).build();
        final Map<String, Object> preSerializationMap = transformer1.toMaps().get(0);

        final DotContentletTransformer transformer2 = new DotTransformerBuilder().defaultOptions().content(copy).build();
        final Map<String, Object> postSerializationMap = transformer2.toMaps().get(0);
        Assert.assertEquals(preSerializationMap, postSerializationMap);

        Assert.assertEquals(transformer1.hydrate().get(0), transformer2.hydrate().get(0));
    }

    private static final AssertionStrategy fileAssetValidation = (original, copy) -> {
        final FileAsset fileAsset1 = (FileAsset) original;
        final int width = fileAsset1.getWidth();
        final int height = fileAsset1.getHeight();
        final FileAsset fileAsset2 = (FileAsset) copy;
        final int width2 = fileAsset2.getWidth();
        final int height2 = fileAsset2.getHeight();
        Assert.assertEquals(width, width2);
        Assert.assertEquals(height, height2);
    };

    private static final AssertionStrategy pageValidation = (original, copy) -> {
        final HTMLPageAsset htmlPageAsset1 = (HTMLPageAsset) original;
        final HTMLPageAsset htmlPageAsset2 = (HTMLPageAsset) copy;
        Assert.assertEquals(htmlPageAsset1.getPageUrl(),htmlPageAsset2.getPageUrl());
        Assert.assertEquals(htmlPageAsset1.getRedirect(),htmlPageAsset2.getRedirect());
        Assert.assertEquals(htmlPageAsset1.getURI(),htmlPageAsset2.getURI());
    };


    /**
     * This will serialize a contentlet to disk
     * @param contentlet
     * @param file
     * @throws IOException
     */
    private void serializeContentlet(final Contentlet contentlet, final File file)
            throws IOException {

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                objectOutputStream.writeObject(contentlet);
            }
        }
    }

    /**
     * This will deserialize a contentlet from disk
     * @param file
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Contentlet readSerializedContentlet(final File file) throws IOException, ClassNotFoundException{
        try(FileInputStream fileInputStream = new FileInputStream(file)){
            try(ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)){
                return (Contentlet) inputStream.readObject();
            }
        }
    }

    /**
     * Given Scenario: This tests that the transformer used to handle serialization for the legacy content-resource is configured properly
     * to handle the date formats returned from the regular database columns and also the fields loaded from the contentlet-as-json column
     * Expected Result: The transformer instantiated through contentResourceOptions method should be able to convert from Date to Timestamp which the expected datatype used to feed JSONObject
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Transformer_content_Resource_Date_Formats_Test()
            throws Exception {

        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .setProperty("title", "Bicycle")
                .setProperty("timeField", new Date())
                .setProperty("dateField", new Date())
                .setProperty("dateTimeField", new Date());
        final Contentlet contentlet = contentletDataGen.nextPersisted();

        Assert.assertTrue(contentlet.getMap().get("timeField") instanceof Date);
        Assert.assertTrue(contentlet.getMap().get("dateField") instanceof Date);
        Assert.assertTrue(contentlet.getMap().get("dateTimeField") instanceof Date);

        final DotContentletTransformer transformer = new DotTransformerBuilder()
                .contentResourceOptions(true)
                .content(contentlet).build();

        final Map<String, Object> map = transformer.toMaps().get(0);

        Assert.assertTrue(map.get("timeField") instanceof Timestamp);
        Assert.assertTrue(map.get("dateField") instanceof Timestamp);
        Assert.assertTrue(map.get("dateTimeField") instanceof Timestamp);

        final Map<String, Object> printableMap = ContentletUtil.getContentPrintableMap(
                APILocator.systemUser(), contentlet);

        Assert.assertTrue(printableMap.get("timeField") instanceof Timestamp);
        Assert.assertTrue(printableMap.get("dateField") instanceof Timestamp);
        Assert.assertTrue(printableMap.get("dateTimeField") instanceof Timestamp);

        //This part simulates the JSON rendering that takes place in the ContentResource

        final JSONObject object = new JSONObject()
                .put("timeField", map.get("timeField"))
                .put("dateField", map.get("dateField"))
                .put("dateTimeField", map.get("dateTimeField")
        );

        Assert.assertTrue(isValidStringDateISO8601(object.get("timeField").toString()));
        Assert.assertTrue(isValidStringDateISO8601(object.get("dateField").toString()));
        Assert.assertTrue(isValidStringDateISO8601(object.get("dateTimeField").toString()));

    }

    /**
     * Given Scenario: This tests that the transformer used to transform content from the DB decode colons and commas
     * Expected Result: Colons and commas shouldn't be HTML encoded when transform them from the DB.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Transformer_content_Decode_JSON()
            throws Exception {

        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore();
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .setProperty("title", "test_KeyValueFieldDecode" + System.currentTimeMillis())
                .setProperty("keyValueField", "{\"origin\":\"https&#58;//test.com &#44; http&#58;//test2.com\"}");

        final Contentlet contentlet = contentletDataGen.nextPersisted();

        final Contentlet findContentlet = APILocator.getContentletAPI().find(contentlet.getInode(),APILocator.systemUser(),false);

        final Map<String, Object> keyValueField = findContentlet.getKeyValueProperty("keyValueField");

        Assert.assertFalse(keyValueField.get("origin").toString().contains("&#58;"));
        Assert.assertFalse(keyValueField.get("origin").toString().contains("&#44;"));

    }

    /**
     * Given Scenario: A Site is hydrated through the same options the new Edit Content editor's
     * endpoint uses ({@code GET /api/v1/content/{id}} → {@code contentResourceOptions}).
     * <p>
     * Expected Result: {@code hostName} holds the Site's OWN name. It is a real field on the Host
     * Content Type — the required Text field labelled "Site Key" — and must not be replaced by the
     * derived name of the Site the contentlet lives on. Every Site lives on the System Host, so
     * before the fix for <a href="https://github.com/dotCMS/core/issues/37584">#37584</a> this
     * returned the literal "System Host" for every Site.
     */
    @Test
    public void Test_Transform_Site_Keeps_Its_Own_HostName() {

        final Host newSite = new SiteDataGen().nextPersisted();

        final Map<String, Object> map = new DotTransformerBuilder()
                .contentResourceOptions(false)
                .content(newSite).build().toMaps().get(0);

        assertEquals("A Site's hostName must be its own name, not the parent Site's",
                newSite.getHostname(), map.get(Contentlet.HOST_NAME));
        assertEquals("hostName and title must agree for a Site",
                map.get(Contentlet.TITTLE_KEY), map.get(Contentlet.HOST_NAME));
        Assert.assertNotEquals("The derived parent-Site name must not leak into the Site Key field",
                Host.SYSTEM_HOST_SITENAME, map.get(Contentlet.HOST_NAME));

        // The parent reference itself is untouched: a Site still lives on the System Host.
        assertEquals(Host.SYSTEM_HOST, map.get(Contentlet.HOST_KEY));
    }

    /**
     * Given Scenario: The System Host itself is hydrated through the editor's options.
     * <p>
     * Expected Result: its {@code hostName} is "System Host" — which is the correct answer here,
     * because that IS its own name. This is the case that made #37584 look plausible: the System
     * Host was the one Site the broken derived value happened to get right, and the fix must not
     * regress it.
     */
    @Test
    public void Test_Transform_System_Host_Still_Reports_Its_Own_Name()
            throws DotDataException, DotSecurityException {

        final Host systemHost = APILocator.getHostAPI().findSystemHost();

        final Map<String, Object> map = new DotTransformerBuilder()
                .contentResourceOptions(false)
                .content(systemHost).build().toMaps().get(0);

        assertEquals("The System Host's hostName is its own name",
                systemHost.getHostname(), map.get(Contentlet.HOST_NAME));
        assertEquals(Host.SYSTEM_HOST_SITENAME, map.get(Contentlet.HOST_NAME));
    }

    /**
     * Given Scenario: A Site is read through the new Edit Content editor's path
     * ({@code contentResourceOptions}) and the values it hands back are fed straight into a Site
     * save — the round trip a save from that editor performs — changing only an unrelated field.
     * <p>
     * Expected Result: the save goes through WITHOUT the {@code forceExecution} escape hatch.
     * {@code HostAPIImpl.save} refuses any save whose incoming {@code hostName} differs from the
     * stored one, so before the fix for
     * <a href="https://github.com/dotCMS/core/issues/37584">#37584</a> this round trip carried
     * "System Host" back into the save and tripped that guard with an
     * {@link IllegalArgumentException}. This is AC-003 of the issue: what the editor reads back
     * must be safe to save.
     */
    @Test
    public void Test_Site_Read_Through_Editor_Path_Saves_Without_ForceExecution()
            throws DotDataException, DotSecurityException {

        final Host newSite = new SiteDataGen().nextPersisted();
        final String originalName = newSite.getHostname();
        final User systemUser = APILocator.systemUser();

        final Map<String, Object> editorView = new DotTransformerBuilder()
                .contentResourceOptions(false)
                .content(newSite).build().toMaps().get(0);

        final Contentlet stored = APILocator.getContentletAPI()
                .find(newSite.getInode(), systemUser, false);
        // Guards against a vacuous pass: the check in HostAPIImpl.save only fires when the stored
        // Site carries a hostName to compare the incoming one against.
        assertEquals("The stored Site must carry its name for this round trip to mean anything",
                originalName, stored.get(Host.HOST_NAME_KEY));

        final Host toSave = new Host(stored);
        // Exactly what the editor posts back: the hostName it was served, untouched by the user.
        toSave.setHostname((String) editorView.get(Contentlet.HOST_NAME));
        toSave.setProperty("description", "Edited without touching the Site Key");

        final Host saved = APILocator.getHostAPI().save(toSave, systemUser, false);

        assertEquals("A Site read through the editor path must save without forceExecution, "
                        + "and without its name changing",
                originalName, saved.getHostname());
    }

    /**
     * Given Scenario: A custom Content Type declares its own {@code hostName} field but a
     * Contentlet of that type leaves it unset, and is hydrated through the editor's options.
     * <p>
     * Expected Result: the key is still present, carrying the {@code N/A} sentinel. The transform
     * map has always guaranteed a non-null {@code hostName}, so the collision guard must not turn
     * a declared-but-empty field into a missing key — raised in review on
     * <a href="https://github.com/dotCMS/core/pull/37589">#37589</a>.
     */
    @Test
    public void Test_Transform_Declared_But_Unset_HostName_Falls_Back_To_Sentinel() {

        final ContentType customType = new ContentTypeDataGen()
                // FieldDataGen seeds a defaultValue by default; null it out so the field really is
                // unset on the contentlet, which is the case under test.
                .field(new FieldDataGen().name("Site Key").velocityVarName(Contentlet.HOST_NAME)
                        .type(TextField.class).defaultValue(null).next())
                .field(new FieldDataGen().name("Title").velocityVarName("title")
                        .type(TextField.class).next())
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(customType.id())
                .setProperty("title", "declared-but-unset-" + System.currentTimeMillis())
                .host(site)
                .nextPersisted();

        final Map<String, Object> map = new DotTransformerBuilder()
                .contentResourceOptions(false)
                .content(contentlet).build().toMaps().get(0);

        assertTrue("A declared but unset hostName must not drop the key",
                map.containsKey(Contentlet.HOST_NAME));
        // AbstractTransformStrategy.NOT_APPLICABLE is package-private; assert its value rather
        // than widening its visibility for a test.
        assertEquals("An unset declared field falls back to the sentinel, not to the Site name",
                "N/A", map.get(Contentlet.HOST_NAME));
    }

    /**
     * Given Scenario: An ordinary contentlet — whose Content Type does NOT declare a
     * {@code hostName} field — is hydrated through the same options.
     * <p>
     * Expected Result: {@code hostName} is still the name of the Site the contentlet lives on.
     * This is the regression guard for #37584: the fix must narrow the derived property only where
     * a real field would be overwritten, and leave every other Content Type exactly as it was.
     */
    @Test
    public void Test_Transform_Regular_Content_Still_Reports_Its_Parent_Site_Name() {

        final ContentType newsLikeContentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet news = TestDataUtils.getNewsContent(true, langId,
                newsLikeContentType.id(), site);

        final Map<String, Object> map = new DotTransformerBuilder()
                .contentResourceOptions(false)
                .content(news).build().toMaps().get(0);

        assertEquals("A regular contentlet's hostName must be the Site it lives on",
                site.getHostname(), map.get(Contentlet.HOST_NAME));
        assertEquals(site.getIdentifier(), map.get(Contentlet.HOST_KEY));
    }

    /**
     * Utitlity method to validate a string date against the ISO8601 format
     * @param dateString
     * @return
     */
    public static boolean isValidStringDateISO8601(final String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setLenient(false); // Strict date parsing
        return null != Try.of(()-> dateFormat.parse(dateString)).getOrElse((Date)null);
    }

}
