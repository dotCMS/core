package com.dotmarketing.portlets.contentlet.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Oscar Arrieta on 6/13/17.
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentletUtilTest extends IntegrationTestBase {

    private static User user;
    private static Language language;

    private static CategoryAPI categoryAPI;

    private static Host host;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment.
        IntegrationTestInitService.getInstance().init();

        user = APILocator.getUserAPI().getSystemUser();
        language = APILocator.getLanguageAPI().getDefaultLanguage();

        categoryAPI = APILocator.getCategoryAPI();

        host = new SiteDataGen().nextPersisted();
    }

    private static final String CATEGORY_NAMES = "categoryNames";
    private static final String ALL_CATEGORIES_INFO = "allCategoriesInfo";

    @DataProvider
    public static Object[] categoryTestCases() {
        return new String[] {
                CATEGORY_NAMES, ALL_CATEGORIES_INFO
        };
    }

    /**
     * https://github.com/dotCMS/core/issues/11751
     */
    @Test
    @UseDataProvider("categoryTestCases")
    public void validateContentPrintableMapMethodReturnProperCategories(
            final String categoryInfoType)
            throws IOException, DotDataException, DotSecurityException {

        Contentlet contentlet = null;
        ContentType contentType = null;
        Category contentCategory = null;
        Category contentBeltsCategory = null;

        try {

            //Creating Categories.
            //Create Parent Content Category and Child Popular Category.
            final Category popularCategory = createCategory("Popular", 1).next();
            contentCategory = createCategory("Content", 0)
                    .children(popularCategory).nextPersisted();

            //Create Parent Content belts Category and Child Flights and Home Categories.
            final Category flightsCategory = createCategory("Flights", 1).next();
            final Category homeCategory = createCategory("Home", 2).next();
            contentBeltsCategory = createCategory("Content Belts", 0)
                    .children(flightsCategory, homeCategory).nextPersisted();

            //Create Content Type with a Text Field and two Category Fields.
            final String CATEGORY_NAME_CONTENT = "content";
            final String CATEGORY_NAME_CONTENT_BELTS = "contentBelts";
            final List<Field> fields = new ArrayList<>();
            fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
            fields.add(new FieldDataGen().type(CategoryField.class)
                    .name(CATEGORY_NAME_CONTENT).velocityVarName(CATEGORY_NAME_CONTENT)
                    .values(contentCategory.getInode()).next());
            fields.add(new FieldDataGen().type(CategoryField.class)
                    .name(CATEGORY_NAME_CONTENT_BELTS).velocityVarName(CATEGORY_NAME_CONTENT_BELTS)
                    .values(contentBeltsCategory.getInode()).next());
            contentType = new ContentTypeDataGen().host(host).fields(fields).nextPersisted();

            //Creating content.
            contentlet = new ContentletDataGen(contentType.id())
                    .languageId(language.getId())
                    .setProperty("title", "Test Contentlet")
                    .addCategory(popularCategory)
                    .addCategory(homeCategory).addCategory(flightsCategory)
                    .nextPersisted();

            final Map<String, Object> contentPrintableMap = ContentletUtil
                    .getContentPrintableMap( user, contentlet,
                            categoryInfoType.equals(ALL_CATEGORIES_INFO));

            assertTrue(contentPrintableMap.containsKey(CATEGORY_NAME_CONTENT));
            assertTrue(contentPrintableMap.containsKey(CATEGORY_NAME_CONTENT_BELTS));
            if (categoryInfoType.equals(ALL_CATEGORIES_INFO)) {
                verifyCategoriesForField(contentPrintableMap, CATEGORY_NAME_CONTENT,
                        popularCategory);
                verifyCategoriesForField(contentPrintableMap, CATEGORY_NAME_CONTENT_BELTS,
                        homeCategory, flightsCategory);
            } else {
                final List<Map<String, Object>> list = (List) contentPrintableMap
                        .get(CATEGORY_NAME_CONTENT);
                final Map<String, Object> categoryData = list.get(0);
                final Object categoryValue = categoryData.get(popularCategory.getKey());
                assertEquals(popularCategory.getCategoryName(), categoryValue);
                final List<Map<String, String>> categoryMaps = (List) contentPrintableMap
                        .get(CATEGORY_NAME_CONTENT_BELTS);
                assertEquals(2, categoryMaps.size());

                assertTrue(categoryMaps.stream().anyMatch(map -> homeCategory.getCategoryName()
                        .equals(map.get(homeCategory.getKey()))));
                assertTrue(categoryMaps.stream().anyMatch(map -> flightsCategory.getCategoryName()
                        .equals(map.get(flightsCategory.getKey()))));
            }

        } finally {
            if (UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getInode())) {
                ContentletDataGen.remove(contentlet);
            }
            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.id())) {
                ContentTypeDataGen.remove(contentType);
            }
            //Delete Categories.
            if (UtilMethods.isSet(contentCategory) && UtilMethods.isSet(contentCategory.getInode())){
                categoryAPI.delete( contentCategory, user, false );
            }
            if (UtilMethods.isSet(contentBeltsCategory) && UtilMethods.isSet(contentBeltsCategory.getInode())){
                categoryAPI.delete( contentBeltsCategory, user, false );
            }
        }

    }

    @Test
    public void test_getContentPrintableMap_WhenContentTypeIsNeitherFileAssetNorPage_PathIsNotAddedToTheMap()
            throws DotDataException, IOException {

        ContentType contentType = null;
        Contentlet contentlet = null;

        try {
            contentType = new ContentTypeDataGen().host(host).nextPersisted();

            contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

            final Map<String, Object> contentPrintableMap = ContentletUtil
                    .getContentPrintableMap(user, contentlet);

            assertFalse(contentPrintableMap.containsKey("path"));
        } finally {
            if (UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getInode())) {
                ContentletDataGen.remove(contentlet);
            }
            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.id())) {
                ContentTypeDataGen.remove(contentType);
            }
        }

    }

    @Test
    public void test_getContentPrintableMap_WhenContentTypeIsHTMLPage_PathIsAddedToTheMap()
            throws DotDataException, IOException {

        Folder folder = null;
        HTMLPageAsset page = null;
        Template template = null;

        try {

            template = new TemplateDataGen().nextPersisted();

            folder = new FolderDataGen().nextPersisted();

            page = new HTMLPageDataGen(folder, template).nextPersisted();

            final Map<String, Object> contentPrintableMap = ContentletUtil
                    .getContentPrintableMap(user, page);

            assertTrue(contentPrintableMap.containsKey("path"));
            assertTrue(UtilMethods.isSet(contentPrintableMap.get("path")));
        } finally {

            if (UtilMethods.isSet(page) && UtilMethods.isSet(page.getInode())) {
                HTMLPageDataGen.remove(page);
            }

            if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
                FolderDataGen.remove(folder);
            }

            if (UtilMethods.isSet(template) && UtilMethods.isSet(template.getInode())) {
                TemplateDataGen.remove(template);
            }
        }

    }

    @Test
    public void test_getContentPrintableMap_WhenContentTypeIsFileAsset_PathIsAddedToTheMap()
            throws DotSecurityException, DotDataException, IOException {

        Folder folder = null;
        Contentlet contentlet = null;

        try {

            folder = new FolderDataGen().nextPersisted();
            final File file = File.createTempFile("texto", ".txt");
            FileUtil.write(file, "helloworld");

            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder, file);

            contentlet = fileAssetDataGen.nextPersisted();

            final Map<String, Object> contentPrintableMap = ContentletUtil
                    .getContentPrintableMap(user, contentlet);

            assertTrue(contentPrintableMap.containsKey("path"));
            assertTrue(UtilMethods.isSet(contentPrintableMap.get("path")));
        } finally {
            if (UtilMethods.isSet(contentlet) && UtilMethods.isSet(contentlet.getInode())) {
                FileAssetDataGen.remove(contentlet);
            }
            if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
                FolderDataGen.remove(folder);
            }

        }
    }

    private CategoryDataGen createCategory(final String categoryName, final int sortOrder) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String categoryKey = categoryName.toLowerCase().replaceAll("\\s", "")
                + "-" + simpleDateFormat.format(new Date());
        return new CategoryDataGen().setCategoryName(categoryName)
            .setKey(categoryKey).setCategoryVelocityVarName(categoryKey)
            .setSortOrder(sortOrder);
    }

    private void verifyCategoriesForField(
            final Map<String, Object> contentPrintableMap,
            final String fieldName, final Category ...categories) {

        final List<?> categoryList = (List<?>) contentPrintableMap.get(fieldName);
        assertNotNull(categoryList);
        assertEquals(categories.length, categoryList.size());
        for (final Category category : categories) {
            final Optional<?> matchElement = categoryList.stream()
                    .filter(map -> {
                        final Map<?,?> cMap = (Map<?,?>) map;
                        return cMap.containsKey("key")
                                && cMap.get("key").equals(category.getKey());
                    }).findFirst();
            assertTrue(matchElement.isPresent());
            final Map<?,?> categoryMap = (Map<?, ?>) matchElement.get();
            assertEquals(category.getCategoryName(), categoryMap.get("categoryName"));
        }

    }

}
