package com.dotmarketing.portlets;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:36 AM
 */
public class ContentletBaseTest extends TestBase {

    protected static ContentletAPI contentletAPI;
    protected static Host defaultHost;
    protected static ContentletFactory contentletFactory;
    private static RoleAPI roleAPI;
    private static PermissionAPI permissionAPI;
    private static LanguageAPI languageAPI;
    private static HostAPI hostAPI;
    private static CategoryAPI categoryAPI;

    protected static User user;
    protected static List<Contentlet> contentlets;
    protected static Collection<Structure> structures;
    protected static Collection<Permission> permissions;
    protected static int FIELDS_SIZE = 14;

    private static String wysiwygValue = "<p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. " +
            "Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem.</p>" +
            "<p>Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer " +
            "tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a.</p>";

    @BeforeClass
    public static void prepare () throws DotSecurityException, DotDataException {

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();

        //Getting the APIs references
        roleAPI = APILocator.getRoleAPI();
        permissionAPI = APILocator.getPermissionAPI();
        contentletFactory = FactoryLocator.getContentletFactory();
        languageAPI = APILocator.getLanguageAPI();
        contentletAPI = APILocator.getContentletAPI();
        hostAPI = APILocator.getHostAPI();
        categoryAPI = APILocator.getCategoryAPI();

        defaultHost = hostAPI.findDefaultHost( user, false );

        structures = new ArrayList<Structure>();
        permissions = new ArrayList<Permission>();
        contentlets = new ArrayList<Contentlet>();

        //*******************************************************************************
        //Creating tests structures

        addStructure( "JUnit Test Structure_0", "junit_test_structure_0" );
        addStructure( "JUnit Test Structure_1", "junit_test_structure_1" );
        addStructure( "JUnit Test Structure_2", "junit_test_structure_2" );
        addStructure( "JUnit Test Structure_3", "junit_test_structure_3" );

        //Adding the fields to the structures
        for ( Structure structure : structures ) {
            addFields( structure );
        }

        //*******************************************************************************
        //Creating the test contentlets

        Iterator<Structure> structureIterator = structures.iterator();

        //NO set the language
        addContentlet( structureIterator.next(), null );

        //Set the language to default value
        Language language = languageAPI.getDefaultLanguage();
        List<Language> languages = languageAPI.getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }
        addContentlet( structureIterator.next(), language );
    }

    @AfterClass
    public static void afterClass () throws DotDataException, DotSecurityException {

        //Delete the contentles
        for ( Contentlet contentlet : contentlets ) {
            contentletAPI.delete( contentlet, user, false );
        }

        //Delete the structures
        for ( Structure structure : structures ) {
            StructureFactory.deleteStructure( structure );
        }
    }

    /**
     * Creates and add an structure to a collection for a later use in the tests
     *
     * @param name
     * @param structureVelocityVarName
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    private static void addStructure ( String name, String structureVelocityVarName ) throws DotDataException, DotSecurityException {

        //Create the structure
        Structure testStructure = new Structure();

        testStructure.setDefaultStructure( false );
        testStructure.setDescription( "JUnit Test Structure Description." );
        testStructure.setFixed( false );
        testStructure.setIDate( new Date() );
        testStructure.setName( name );
        testStructure.setOwner( user.getUserId() );
        testStructure.setDetailPage( "" );
        testStructure.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure.setSystem( true );
        testStructure.setType( "structure" );
        testStructure.setVelocityVarName( structureVelocityVarName );

        //Saving the structure
        StructureFactory.saveStructure( testStructure );

        //Creating and adding permissions
        Permission permissionRead = new Permission( testStructure.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ );
        Permission permissionEdit = new Permission( testStructure.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_EDIT );
        Permission permissionWrite = new Permission( testStructure.getInode(), roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_WRITE );

        permissionAPI.save( permissionRead, testStructure, user, false );
        permissionAPI.save( permissionEdit, testStructure, user, false );
        permissionAPI.save( permissionWrite, testStructure, user, false );

        permissions.add( permissionRead );
        permissions.add( permissionEdit );
        permissions.add( permissionWrite );

        //Finally add it to the test collection
        structures.add( testStructure );
    }

    /**
     * Creating fields and add them to a given structure for testing
     *
     * @param jUnitTestStructure
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     */
    private static void addFields ( Structure jUnitTestStructure ) throws DotHibernateException {

        //Create the fields
        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, jUnitTestStructure, false, true, false, 1, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Text Area", Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 2, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Wysiwyg", Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 3, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Date", Field.FieldType.DATE, Field.DataType.DATE, jUnitTestStructure, false, false, false, 4, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Time", Field.FieldType.TIME, Field.DataType.DATE, jUnitTestStructure, false, false, false, 5, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Date Time", Field.FieldType.DATE_TIME, Field.DataType.DATE, jUnitTestStructure, false, false, false, 6, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Integer", Field.FieldType.TEXT, Field.DataType.INTEGER, jUnitTestStructure, false, false, false, 7, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Float", Field.FieldType.TEXT, Field.DataType.FLOAT, jUnitTestStructure, false, false, false, 8, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Boolean", Field.FieldType.RADIO, Field.DataType.BOOL, jUnitTestStructure, false, false, false, 9, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test File", Field.FieldType.FILE, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 10, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Image", Field.FieldType.IMAGE, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 11, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Binary", Field.FieldType.BINARY, Field.DataType.BINARY, jUnitTestStructure, false, false, false, 12, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Host Folder", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 12, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Tag", Field.FieldType.TAG, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 12, false, false, false );
        FieldFactory.saveField( field );
    }

    /**
     * Creates and add a Contentlet to a collection for a later use in the tests
     *
     * @param structure
     * @param language
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static void addContentlet ( Structure structure, Language language ) throws DotDataException, DotSecurityException {

        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        //contentlet.setLive( true );
        //contentlet.setWorking( true );
        contentlet.setStructureInode( structure.getInode() );
        contentlet.setHost( defaultHost.getIdentifier() );
        if ( UtilMethods.isSet( language ) ) {
            contentlet.setLanguageId( language.getId() );
        }

        //Get all the fields for the structure
        List<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Fill the new contentlet with test data
        for ( Field field : fields ) {
            Object value = null;
            if ( field.getVelocityVarName().equals( "comments" ) ) {
                value = "off";
            } else if ( field.getFieldType().equals( Field.FieldType.TEXT.toString() ) ) {
                if ( field.getFieldContentlet().startsWith( "text" ) ) {
                    value = language != null? language.getCountry() + " Language" : "No Language";
                } else if ( field.getFieldContentlet().startsWith( "float" ) ) {
                    value = 0;
                }
            } else if ( field.getFieldType().equals( Field.FieldType.WYSIWYG.toString() ) ) {
                value = wysiwygValue;
            } else if ( field.getFieldType().equals( Field.FieldType.TAG.toString() ) ) {
                value = "Test Tag";
            } else if ( field.getFieldType().equals( Field.FieldType.DATE.toString() ) || field.getFieldType().equals( Field.FieldType.DATE_TIME.toString() ) ) {
                value = new Date();
            }
            if ( UtilMethods.isSet( value ) ) {
                contentletAPI.setContentletProperty( contentlet, field, value );
            }
        }

        //Getting some categories to test
        List<Category> categories = categoryAPI.findTopLevelCategories( user, false );
        //Get The permissions of the structure
        List<Permission> structurePermissions = permissionAPI.getPermissions( structure );

        //Validate if the contenlet is OK
        contentletAPI.validateContentlet( contentlet, categories );

        //Save the contentlet
        contentlet = contentletAPI.checkin( contentlet, categories, structurePermissions, user, true );

        //Adding it to the test collection
        contentlets.add( contentlet );
    }

}