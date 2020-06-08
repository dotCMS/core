package com.dotmarketing.portlets.contentlet.transform;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.COMMON_PROPS;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.VERSION_INFO;
import static com.dotmarketing.portlets.fileassets.business.FileAssetAPI.META_DATA_FIELD;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.strategy.AbstractTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.DefaultTransformStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.FileAssetViewStrategy;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolverImpl;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolbox;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ContentletTransformerTest extends BaseWorkflowIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();

        final ContentType employeeLikeContentType = TestDataUtils.getEmployeeLikeContentType();
        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();
        final ContentType newsLikeContentType = TestDataUtils.getNewsLikeContentType();


        // Creating the contentlets for they will be pulled-out from the index
        for (int i = 0; i <= 10; i++) {
            TestDataUtils.getEmployeeContent(true, 1, employeeLikeContentType.id());
            TestDataUtils.getBannerLikeContent(true, 1, bannerLikeContentType.id(), null);
            TestDataUtils.getNewsContent(true, 1, newsLikeContentType.id());
        }

    }

    @Test
    public void Transformer_Simple_Test() throws DotDataException {

        List<Contentlet> list = APILocator.getContentletAPI().findAllContent(0,20);
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

    @Test
    public void Test_Hydrate_Contentlet_WithUrl() throws DotDataException {

        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE);
        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        String urlExpected = "home_page";
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(HTMLPageAssetAPI.URL_FIELD, urlExpected);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);
        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final TransformToolbox toolbox = new TransformToolbox(
                APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(), APILocator.getVersionableAPI(),
                APILocator.getUserAPI(), APILocator.getContentletAPI(),
                APILocator.getHTMLPageAssetAPI(), APILocator.getCategoryAPI(),
                contentHelper);

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolbox);

        final Contentlet newContentlet = new DotContentletTransformerImpl(singletonList(contentlet), resolver, DotContentletTransformerImpl.defaultOptions, null).hydrate().get(0);

        assertNotNull(newContentlet);
        assertNotSame(newContentlet, contentlet); //This method now returns a new instance. A copy of the original contentlet
        assertFalse(newContentlet.getMap().containsKey(Contentlet.NULL_PROPERTIES));
        assertEquals(newContentlet.getMap().get(ContentletForm.IDENTIFIER_KEY), identifier);
        assertEquals(newContentlet.getMap().get(HTMLPageAssetAPI.URL_FIELD), urlExpected);
    }

    @Test
    public void Test_Hydrate_Contentlet_Without_Url_And_AssetName_Does_Not_Exist() throws DotDataException {
        final String  anyUrl = "anyUrl";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return anyUrl;
            }
        };
        final String identifier = "1234";

        final TransformToolbox toolbox = new TransformToolbox(APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(),
                APILocator.getFileAssetAPI(), APILocator.getVersionableAPI(),
                APILocator.getUserAPI(), APILocator.getContentletAPI(),
                APILocator.getHTMLPageAssetAPI(), APILocator.getCategoryAPI(),
                contentHelper);

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolbox);

        Contentlet contentlet = new Contentlet();

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
        String urlExpected = "home_page";
        final IdentifierAPI identifierAPI = mock(IdentifierAPI.class);
        final ContentHelper contentHelper = new ContentHelper(identifierAPI, MapToContentletPopulator.INSTANCE){
            @Override
            public String getUrl(Contentlet contentlet) {
                return urlExpected;
            }
        };

        final TransformToolbox toolBox = new TransformToolbox(APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(), APILocator.getFileAssetAPI(),
                APILocator.getVersionableAPI(), APILocator.getUserAPI(),
                APILocator.getContentletAPI(),
                APILocator.getHTMLPageAssetAPI(),
                APILocator.getCategoryAPI(),
                contentHelper);

        final StrategyResolverImpl resolver = new StrategyResolverImpl(toolBox);

        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

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
        allFields.removeIf(f->constants.contains(f));
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


    @DataProvider
    public static Object[] listTestCases() throws Exception {
        final User user = APILocator.systemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final int limit = 300;
        final boolean respectFrontEndUsers = false;
        return new Object[]{
                new TestCase(BaseContentType.getBaseContentType(0),
                        contentletAPI.search("+basetype:0", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(1),
                        contentletAPI.search("+basetype:1", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(2),
                        contentletAPI.search("+basetype:2", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(3),
                        contentletAPI.search("+basetype:3", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(4),
                        contentletAPI.search("+basetype:4", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(5),
                        contentletAPI.search("+basetype:5", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(6),
                        contentletAPI.search("+basetype:6", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(7),
                        contentletAPI.search("+basetype:7", limit, 0, null, user, respectFrontEndUsers)),
                new TestCase(BaseContentType.getBaseContentType(8),
                        contentletAPI.search("+basetype:8", limit, 0, null, user, respectFrontEndUsers))
        };
    }

    private static class TestCase {
        final BaseContentType baseContentType;
        final List<Contentlet> contentlets;

        TestCase(final BaseContentType baseContentType,
                final List<Contentlet> contentlets) {
            this.baseContentType = baseContentType;
            this.contentlets = contentlets;
        }
    }

    /**
     * Given Scenario: We have the old and deprecated implementation vs the new one
     * Expected Result: Both results must match
     * @param testCase
     */
    @Test
    @UseDataProvider("listTestCases")
    public void Transformer_Backwards_Compatibility_Test(final TestCase testCase) {

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
            //We compare the transformation results are the same using the default options on the new Transformer
            final Map<String, Object> sourceMap = transformedList1.get(i);
            final Map<String, Object> copyMap = transformedList2.get(i);

            assertTrue(String.format(" baseType `%s` should have same (or more) number of properties " ,baseTypeName),copyMap.size() >= sourceMap.size());
            final String assertMessage =  " contentType:`%s` , id: `%s` ,  key: `%s` ";

            for (final String propertyName : sourceMap.keySet()) {

                final Object object1 = sourceMap.get(propertyName);
                final Object object2 = copyMap.get(propertyName);

                if(null != object1 && object2 == null &&  privateInternalProperties.contains(propertyName)){
                   //We're looking at private property that exists in the source but was removed on the copy. which is fine.
                   continue;
                }

                if(object1 instanceof File){
                    //Binaries are now formatted to their /dA/ path form.
                    final File conBinary = (File)object1;
                    final String dAPath = "/dA/%s/%s/%s";
                    final String binaryPath = String.format(dAPath, sourceMap.get("identifier"),propertyName,conBinary.getName());
                    assertEquals(String.format(assertMessage,
                            baseTypeName, original.getIdentifier(), propertyName), binaryPath, object2);
                    continue;
                }

                    assertNotNull(String.format("Object with key `%s` is null why??",object2));
                    assertEquals(String.format(assertMessage,
                            baseTypeName, original.getIdentifier(), propertyName), object1,
                            object2);

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

            for (final String propertyName : contentlet1.getMap().keySet()) {
                final Object object1 = contentlet1.getMap().get(propertyName);
                final Object object2 = contentlet2.getMap().get(propertyName);

                if(null != object1 && object2 == null &&  privateInternalProperties.contains(propertyName)){
                    //We're looking at private property that exists in the source but was removed on the copy. which is fine.
                    continue;
                }

                if(object1 instanceof File){
                    //Binaries are now formatted to their /dA/ path form.
                    final File conBinary = (File)object1;
                    final String dAPath = "/dA/%s/%s/%s";
                    final String binaryPath = String.format(dAPath, contentlet1.getMap().get("identifier"),propertyName,conBinary.getName());
                    assertEquals(String.format(" contentType:`%s` , id: `%s` ,  key: `%s` ",
                            baseTypeName, contentlet1.getIdentifier(), propertyName), binaryPath, object2);
                    continue;
                }

                assertNotNull(object2);
                assertEquals(String.format(" contentType:`%s` , id: `%s` ,  key: `%s` ",
                        baseTypeName, contentlet1.getIdentifier(), propertyName), object1,
                        object2);
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

        final TransformToolbox toolBox = new TransformToolbox(
                identifierAPI,
                APILocator.getHostAPI(),
                APILocator.getLanguageAPI(),
                fileAssetAPI,
                APILocator.getVersionableAPI(),
                APILocator.getUserAPI(),
                APILocator.getContentletAPI(),
                APILocator.getHTMLPageAssetAPI(),
                APILocator.getCategoryAPI(),
                contentHelper
        );

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
        map.put(META_DATA_FIELD, ContentletCache.CACHED_METADATA);

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
        when(fileAsset.getMetaData()).thenReturn("Meta");

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
        final String meta = (String)mapView.get(META_DATA_FIELD);
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
        assertEquals(ContentletCache.CACHED_METADATA, meta);

    }


}
