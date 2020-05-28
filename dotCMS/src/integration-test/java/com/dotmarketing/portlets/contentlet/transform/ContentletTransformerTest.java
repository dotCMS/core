package com.dotmarketing.portlets.contentlet.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.struts.ContentletForm;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer.Builder;
import com.dotmarketing.portlets.contentlet.transform.strategy.StrategyResolver;
import com.dotmarketing.portlets.contentlet.transform.strategy.TransformToolBox;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        final List<Map<String, Object>> transformedList = new DotContentletTransformer(list).toMaps();

        assertEquals(list.size(), transformedList.size());
        for(int i=0; i < list.size(); i++){
            final Contentlet original = list.get(i);
            final Map<String,Object> transformed = transformedList.get(i);
            //Basic properties must exist on both
            assertEquals(original.getMap().get(Contentlet.IDENTIFIER_KEY),transformed.get(Contentlet.IDENTIFIER_KEY));
            assertEquals(original.getMap().get(Contentlet.INODE_KEY),transformed.get(Contentlet.INODE_KEY));
            assertEquals(original.getMap().get(Contentlet.LANGUAGEID_KEY),transformed.get(Contentlet.LANGUAGEID_KEY));
            assertEquals(original.getMap().get(Contentlet.MOD_DATE_KEY),transformed.get(Contentlet.MOD_DATE_KEY));
            assertEquals(original.getMap().get(Contentlet.MOD_USER_KEY),transformed.get(Contentlet.MOD_USER_KEY));

            //New Properties expected
            assertNotNull(transformed.get(Contentlet.TITTLE_KEY));
            assertNotNull(transformed.get(Contentlet.CONTENT_TYPE_KEY));
            assertNotNull(transformed.get(HTMLPageAssetAPI.URL_FIELD));

            //Forbidden properties Must Not be part of the result
            for(final String property : TransformToolBox.privateInternalProperties){
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

        final TransformToolBox toolBox = new TransformToolBox(APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(), APILocator.getFileAssetAPI(),
                APILocator.getVersionableAPI(), APILocator.getUserAPI(),
                APILocator.getContentletAPI(), APILocator.getHTMLPageAssetAPI(), contentHelper);

        final StrategyResolver resolver = new StrategyResolver(toolBox);

        final Contentlet newContentlet = new DotContentletTransformer(Collections.singletonList(contentlet), resolver, DotContentletTransformer.defaultOptions).hydrate().get(0);

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

        final TransformToolBox toolBox = new TransformToolBox(APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(), APILocator.getFileAssetAPI(),
                APILocator.getVersionableAPI(), APILocator.getUserAPI(),
                APILocator.getContentletAPI(), APILocator.getHTMLPageAssetAPI(), contentHelper);

        final StrategyResolver resolver = new StrategyResolver(toolBox);

        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);

        when(identifierAPI.find(identifier)).thenReturn(null);

        final Contentlet newContentlet = new DotContentletTransformer(
                  Collections.singletonList(contentlet), resolver,
                  DotContentletTransformer.defaultOptions
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

        final TransformToolBox toolBox = new TransformToolBox(APILocator.getIdentifierAPI(),
                APILocator.getHostAPI(), APILocator.getLanguageAPI(), APILocator.getFileAssetAPI(),
                APILocator.getVersionableAPI(), APILocator.getUserAPI(),
                APILocator.getContentletAPI(), APILocator.getHTMLPageAssetAPI(), contentHelper);

        final StrategyResolver resolver = new StrategyResolver(toolBox);

        final String identifier = "1234";
        Identifier identifierObject = new Identifier();
        identifierObject.setAssetName(urlExpected);
        identifierObject.setId(identifier);
        Contentlet contentlet = new Contentlet();

        contentlet.getMap().put(ContentletForm.IDENTIFIER_KEY, identifier);
        contentlet.getMap().put(Contentlet.LANGUAGEID_KEY, 1L);
        when(identifierAPI.find(identifier)).thenReturn(identifierObject);

        final Contentlet newContentlet = new DotContentletTransformer(
                  Collections.singletonList(contentlet), resolver, DotContentletTransformer.defaultOptions).hydrate().get(0);

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
        Map<String,Object> map = new DotContentletTransformer(newContentlet).toMaps().get(0);
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

        
        map = new DotContentletTransformer(newContentlet).toMaps().get(0);


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

    @Test
    @UseDataProvider("listTestCases")
    public void Transformer_Backwards_Compatibility_Test(final TestCase testCase) {

        final List <Contentlet> list = testCase.contentlets;
        final String baseTypeName = testCase.baseContentType.name();

        if(list.isEmpty()){
           Logger.warn(ContentletTransformerTest.class, String.format("Unable to get test samples of type `%s`",testCase.baseContentType.name()));
           return;
        }

        final ContentletToMapTransformer contentletToMapTransformer = new ContentletToMapTransformer(list);

        final List<Map<String, Object>> transformedList1 = contentletToMapTransformer.toMaps();

        final DotTransformer dotTransformer = new Builder().defaultOptions().content(list).build();

        final List<Map<String, Object>> transformedList2 = dotTransformer.toMaps();

        assertEquals(transformedList1.size(), transformedList2.size());
        for (int i = 0; i < list.size(); i++) {

            final Contentlet original = list.get(i);
            //We compare the transformation results are the same using the default options on the new Transformer
            final Map<String, Object> objectMap1 = transformedList1.get(i);

            final Map<String, Object> objectMap2 = transformedList2.get(i);

            assertTrue(String.format(" baseType `%s` should have same (or more) number of properties " ,baseTypeName),objectMap2.size() >= objectMap1.size());

            for (String key1 : objectMap1.keySet()) {
                final Object object1 = objectMap1.get(key1);
                final Object object2 = objectMap2.get(key1);

                assertNotNull(object2);
                assertEquals(String.format(" contentType:`%s` , id: `%s` ,  key: `%s` ",
                        baseTypeName, original.getIdentifier(), key1), object1,
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

            for (String key1 : contentlet1.getMap().keySet()) {
                final Object object1 = contentlet1.getMap().get(key1);
                final Object object2 = contentlet2.getMap().get(key1);

                assertNotNull(object2);
                assertEquals(String.format(" contentType:`%s` , id: `%s` ,  key: `%s` ",
                        baseTypeName, contentlet1.getIdentifier(), key1), object1,
                        object2);
            }

        }

    }


}
