package com.dotmarketing.portlets.contentlet.util;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.repackage.org.apache.hadoop.mapred.lib.Arrays;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by Oscar Arrieta on 6/13/17.
 */
public class ContentletUtilTest extends IntegrationTestBase {

    final User user = APILocator.systemUser();
    final Language language = APILocator.getLanguageAPI().getDefaultLanguage();

    final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI( user, false );
    final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment.
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * https://github.com/dotCMS/core/issues/11751
     */
    @Test
    public void validateContentPrintableMapMethodReturnProperCategories() throws Exception{

        Contentlet contentlet = null;
        Field textField = null;
        Field categoryField1 = null;
        Field categoryField2 = null;
        ContentType contentType = null;
        Category contentCategory = null;
        Category popularCategory = null;
        Category contentBeltsCategory = null;
        Category flightsCategory = null;
        Category homeCategory = null;

        try {

            final Host defaultHost = APILocator.getHostAPI().findDefaultHost( user, false );

            //Creating Categories.
            //Create Parent Content Category.
            contentCategory = new Category();
            contentCategory.setCategoryName( "Content" );
            contentCategory.setKey( "content" );
            contentCategory.setCategoryVelocityVarName( "content" );
            contentCategory.setSortOrder( (String) null );
            contentCategory.setKeywords( null );

            categoryAPI.save( null, contentCategory, user, false );

            Category foundCategory = categoryAPI.find( contentCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Child Popular Category.
            popularCategory = new Category();
            popularCategory.setCategoryName( "Popular" );
            popularCategory.setKey( "popular" );
            popularCategory.setCategoryVelocityVarName( "popular" );
            popularCategory.setSortOrder( 1 );
            popularCategory.setKeywords( null );

            categoryAPI.save( contentCategory, popularCategory, user, false );

            foundCategory = categoryAPI.find( popularCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Parent Content belts Category.
            contentBeltsCategory = new Category();
            contentBeltsCategory.setCategoryName( "Content Belts" );
            contentBeltsCategory.setKey( "contentbelts" );
            contentBeltsCategory.setCategoryVelocityVarName( "contentbelts" );
            contentBeltsCategory.setSortOrder( (String) null );
            contentBeltsCategory.setKeywords( null );

            categoryAPI.save( null, contentBeltsCategory, user, false );

            foundCategory = categoryAPI.find( contentBeltsCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Child Flights Category.
            flightsCategory = new Category();
            flightsCategory.setCategoryName( "Popular" );
            flightsCategory.setKey( "popular" );
            flightsCategory.setCategoryVelocityVarName( "popular" );
            flightsCategory.setSortOrder( 1 );
            flightsCategory.setKeywords( null );

            categoryAPI.save( contentBeltsCategory, flightsCategory, user, false );

            foundCategory = categoryAPI.find( flightsCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Child Flights Category.
            homeCategory = new Category();
            homeCategory.setCategoryName( "Home" );
            homeCategory.setKey( "home" );
            homeCategory.setCategoryVelocityVarName( "home" );
            homeCategory.setSortOrder( 1 );
            homeCategory.setKeywords( null );

            categoryAPI.save( contentBeltsCategory, homeCategory, user, false );

            foundCategory = categoryAPI.find( homeCategory.getCategoryId(), user, false );
            assertNotNull( foundCategory );

            //Create Content Type.
            contentType = ContentTypeBuilder.builder( BaseContentType.CONTENT.immutableClass() )
                    .description( "Test ContentType" )
                    .host( defaultHost.getIdentifier() )
                    .name( "Test ContentType" )
                    .owner( "owner" )
                    .variable( "testVelocityVarName" )
                    .build();

            contentType = contentTypeAPI.save( contentType );

            ContentType foundContentType = contentTypeAPI.find( contentType.inode() );
            assertNotNull( foundContentType );

            //Save Fields. 1. Text, 2. Category, 3. Category.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId( contentType.id() )
                    .dataType( DataTypes.TEXT)
                    .build();

            final String CATEGORY_NAME_CONTENT = "content";

            categoryField1 = ImmutableCategoryField.builder()
                    .name( CATEGORY_NAME_CONTENT )
                    .variable( CATEGORY_NAME_CONTENT )
                    .contentTypeId( contentType.id() )
                    .values( contentCategory.getInode() )
                    .build();

            final String CATEGORY_NAME_CONTENT_BELTS = "contentBelts";

            categoryField2 = ImmutableCategoryField.builder()
                    .name( CATEGORY_NAME_CONTENT_BELTS )
                    .variable( CATEGORY_NAME_CONTENT_BELTS )
                    .contentTypeId( contentType.id() )
                    .values( contentBeltsCategory.getInode() )
                    .build();

            textField = fieldAPI.save( textField, user );
            categoryField1  = fieldAPI.save( categoryField1, user );
            categoryField2 = fieldAPI.save( categoryField2, user );

            textField = fieldAPI.find( textField.id() );
            categoryField1 = fieldAPI.find( categoryField1.id() );
            categoryField2 = fieldAPI.find( categoryField2.id() );

            assertNotNull( textField );
            assertNotNull( categoryField1 );
            assertNotNull( categoryField2 );

            //Creating content.
            contentlet = new Contentlet();
            contentlet.setStructureInode( contentType.inode() );
            contentlet.setLanguageId( language.getId() );
            contentlet.setStringProperty( textField.variable(), "Test Contentlet" );

            List<Category> contentletCategories = Arrays.asList( popularCategory, homeCategory );

            contentletAPI.validateContentlet( contentlet, contentletCategories  );
            contentlet = contentletAPI.checkin( contentlet, null, contentletCategories, user, false );
            contentletAPI.isInodeIndexed( contentlet.getInode() );

            assertTrue( UtilMethods.isSet( contentlet.getInode() ) );

            final Map<String, Object> contentPrintableMap = ContentletUtil.getContentPrintableMap( user, contentlet );
            assertTrue( contentPrintableMap.containsKey( CATEGORY_NAME_CONTENT ) );
            assertTrue( contentPrintableMap.containsKey( CATEGORY_NAME_CONTENT_BELTS ) );
            assertEquals( popularCategory.getCategoryName(), contentPrintableMap.get( CATEGORY_NAME_CONTENT ) );
            assertEquals( homeCategory.getCategoryName(), contentPrintableMap.get( CATEGORY_NAME_CONTENT_BELTS ) );

        } catch ( Exception e ){

            fail(e.getMessage());

        } finally {

            //Delete Contentlet.
            if ( contentlet != null ){
                contentletAPI.archive( contentlet, user, false );
                contentletAPI.delete( contentlet, user, false );
            }

            //Cleaning.
            //Delete Content Type and Fields.
            if ( textField != null ){
                fieldAPI.delete( textField );
                assertTrue( checkNotFoundInDbException( textField ) );
            }
            if ( categoryField1 != null ){
                fieldAPI.delete( categoryField1 );
                assertTrue( checkNotFoundInDbException( categoryField1 ) );
            }
            if ( categoryField2 != null ){
                fieldAPI.delete( categoryField2 );
                assertTrue( checkNotFoundInDbException( categoryField2 ) );
            }
            if ( contentType != null ){
                contentTypeAPI.delete( contentType );
                assertTrue( checkNotFoundInDbException( contentType ) );
            }

            //Delete Categories.
            if ( contentCategory != null ){
                categoryAPI.delete( contentCategory, user, false );
            }
            if ( popularCategory != null ){
                categoryAPI.delete( popularCategory, user, false );
            }
            if ( contentBeltsCategory != null ){
                categoryAPI.delete( contentBeltsCategory, user, false );
            }
            if ( flightsCategory != null ){
                categoryAPI.delete( flightsCategory, user, false );
            }
            if ( homeCategory != null ){
                categoryAPI.delete( homeCategory, user, false );
            }

        }

    }

    /**
     * Util method to check if NotFoundInDbException is returned when trying to find a Field or ContentType.
     * @param o ContentType or Field to find.
     * @return true is NotFoundInDbException is returned, false if else.
     */
    private boolean checkNotFoundInDbException (Object o){
        if ( o instanceof ContentType ){
            try {
                contentTypeAPI.find( ((ContentType)o).inode() );
            } catch ( Exception e ){
                return true;
            }
        }
        if ( o instanceof Field ){
            try {
                fieldAPI.find( ((Field)o).id() );
            } catch ( Exception e ){
                return true;
            }
        }
        return false;
    }
}
