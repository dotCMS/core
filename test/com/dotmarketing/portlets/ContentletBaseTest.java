package com.dotmarketing.portlets;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
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
    protected static Folder testFolder;
    //protected static Host testHost;
    protected static ContentletFactory contentletFactory;
    private static RoleAPI roleAPI;
    private static PermissionAPI permissionAPI;
    private static LanguageAPI languageAPI;
    private static HostAPI hostAPI;
    private static CategoryAPI categoryAPI;
    private static ContainerAPI containerAPI;
    private static TemplateAPI templateAPI;
    private static HTMLPageAPI htmlPageAPI;
    private static FolderAPI folderAPI;

    protected static User user;
    protected static List<Contentlet> contentlets;
    protected static Collection<Container> containers;
    protected static Collection<HTMLPage> htmlPages;
    protected static Collection<Structure> structures;
    protected static Collection<Template> templates;
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
        containerAPI = APILocator.getContainerAPI();
        templateAPI = APILocator.getTemplateAPI();
        htmlPageAPI = APILocator.getHTMLPageAPI();
        folderAPI = APILocator.getFolderAPI();

        defaultHost = hostAPI.findDefaultHost( user, false );
        //Create a new test host
        /*testHost = new Host();
        testHost.setHostname( "dotcms_junit_test_host" );
        testHost.setModDate( new Date() );
        testHost.setModUser( user.getUserId() );
        testHost.setOwner( user.getUserId() );
        testHost.setProperty( "theme", "default" );
        testHost = hostAPI.save( testHost, user, false );*/

        structures = new ArrayList<Structure>();
        permissions = new ArrayList<Permission>();
        contentlets = new ArrayList<Contentlet>();
        containers = new ArrayList<Container>();
        templates = new ArrayList<Template>();
        htmlPages = new ArrayList<HTMLPage>();

        //*******************************************************************************
        //Create the new folder
        testFolder = new Folder();

        testFolder.setFilesMasks( "" );
        testFolder.setIDate( new Date() );
        testFolder.setName( "dotcms_junit_test_folder" );
        testFolder.setOwner( user.getUserId() );
        testFolder.setShowOnMenu( false );
        testFolder.setSortOrder( 0 );
        testFolder.setTitle( "dotcms_junit_test_folder" );
        testFolder.setType( "folder" );
        testFolder.setHostId( defaultHost.getIdentifier() );
        //Creates and set an identifier
        Identifier identifier = APILocator.getIdentifierAPI().createNew( testFolder, defaultHost );
        testFolder.setIdentifier( identifier.getId() );

        //Saving the folder
        folderAPI.save( testFolder, user, false );

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
        //Creating the test contentlets and containers
        Iterator<Structure> structureIterator = structures.iterator();
        Structure testStructure1 = structureIterator.next();
        Structure testStructure2 = structureIterator.next();

        //NO set the language
        addContentlet( testStructure1, null );

        //Set the language to default value
        Language language = languageAPI.getDefaultLanguage();
        List<Language> languages = languageAPI.getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }
        addContentlet( testStructure2, language );
    }

    @AfterClass
    public static void afterClass () throws Exception {

        //Delete html pages
        for ( HTMLPage htmlPage : htmlPages ) {
            WebAssetFactory.deleteAsset( htmlPage, user );
        }

        //Delete the contentles
        for ( Contentlet contentlet : contentlets ) {
            contentletAPI.delete( contentlet, user, false );
        }

        //Delete the Templates
        DotConnect dotConnect = new DotConnect();
        for ( Template template : templates ) {
            //Delete the relationship between templates and containers
            dotConnect.setSQL( "delete from template_containers where template_id = ?" );
            dotConnect.addParam( template.getIdentifier() );
            dotConnect.loadResult();

            templateAPI.delete( template, user, false );
        }

        //Delete the containers
        for ( Container container : containers ) {
            WebAssetFactory.deleteAsset( container, user );
        }

        //Delete the structures
        for ( Structure structure : structures ) {
            List<Contentlet> structContent = contentletAPI.findByStructure( structure, user, false, 0, 0 );
            for ( Contentlet contentlet : structContent ) {
                contentletAPI.delete( contentlet, user, false );
            }
            StructureFactory.deleteStructure( structure );
        }

        //Delete the folder
        folderAPI.delete( testFolder, user, false );
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

        //Create a container
        addContainer( contentlet );

        //Adding it to the test collection
        contentlets.add( contentlet );
    }

    /**
     * Creates and add a Container to a collection for a later use in the tests
     *
     * @param contentlet
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private static void addContainer ( Contentlet contentlet ) throws DotSecurityException, DotDataException {

        //Create the new container
        Container container = new Container();

        container.setCode( "$!{body}" );
        container.setFriendlyName( "JUnit Test Container 1 Friendly Name" );
        container.setIDate( new Date() );
        container.setLuceneQuery( "" );
        container.setMaxContentlets( 1 );
        container.setModDate( new Date() );
        container.setModUser( user.getUserId() );
        container.setNotes( "JUnit Test Container 1 Note" );
        container.setOwner( user.getUserId() );
        container.setPostLoop( "" );
        container.setPreLoop( "" );
        container.setShowOnMenu( true );
        container.setSortContentletsBy( "" );
        container.setSortOrder( 2 );
        container.setStaticify( true );
        container.setTitle( "JUnit Test Container 1" );
        container.setType( "containers" );
        container.setUseDiv( true );

        //Saving the new container
        WebAssetFactory.createAsset( container, user.getUserId(), defaultHost );

        //Create a template
        addTemplate( contentlet, container );

        //Adding it to the test collection
        containers.add( container );
    }

    /**
     * Creates and add a Template to a collection for a later use in the tests
     *
     * @param contentlet
     * @param container
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private static void addTemplate ( Contentlet contentlet, Container container ) throws DotSecurityException, DotDataException {

        //Create the new template
        Template template = new Template();

        String body = "<html>\n<head>\n</head>\n<body>\n</body>\n#parseContainer('" + container.getIdentifier() + "')\n<br>\n<br>\n#parseContainer('" + container.getIdentifier() + "')\n</html>";
        template.setBody( body );
        template.setFooter( "" );
        template.setFriendlyName( "JUnit Test Template Friendly Name" );
        template.setHeader( "" );
        template.setIDate( new Date() );
        template.setImage( "" );
        template.setModDate( new Date() );
        template.setModUser( user.getUserId() );
        template.setOwner( user.getUserId() );
        template.setSelectedimage( "" );
        template.setShowOnMenu( true );
        template.setSortOrder( 2 );
        template.setTitle( "JUnit Test Template" );
        template.setType( "template" );

        //Saving the template
        template = templateAPI.saveTemplate( template, defaultHost, user, false );

        //Create an htmlPage for this template
        addHTMLPage( contentlet, container, template );

        //Adding it to the test collection
        templates.add( template );
    }

    /**
     * Creates and add an HTMLPage to a collection for a later use in the tests
     *
     * @param contentlet
     * @param container
     * @param template
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    private static void addHTMLPage ( Contentlet contentlet, Container container, Template template ) throws DotSecurityException, DotDataException {

        //Create the new html page
        HTMLPage htmlPage = new HTMLPage();

        htmlPage.setEndDate( new Date() );
        htmlPage.setFriendlyName( "JUnit HTML Page Test Friendly Name" );
        htmlPage.setHttpsRequired( true );
        htmlPage.setIDate( new Date() );
        htmlPage.setMetadata( "" );
        htmlPage.setModDate( new Date() );
        htmlPage.setModUser( user.getUserId() );
        htmlPage.setOwner( user.getUserId() );
        htmlPage.setPageUrl( "junit_htmlpage_test_" + contentlet.getInode() + ".dot" );
        htmlPage.setRedirect( "" );
        htmlPage.setShowOnMenu( true );
        htmlPage.setSortOrder( 2 );
        htmlPage.setStartDate( new Date() );
        htmlPage.setTitle( "JUnit HTML Page Test" );
        htmlPage.setType( "htmlpage" );
        htmlPage.setWebEndDate( "" );
        htmlPage.setWebStartDate( "" );

        //Saving the htmlPage
        htmlPage = htmlPageAPI.saveHTMLPage( htmlPage, template, testFolder, user, false );

        //Creating and adding permissions
        Collection<Permission> permissions = new ArrayList<Permission>();
        permissions.add( new Permission( "", roleAPI.loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ ) );

        Permission newPermission;
        for ( Permission permission : permissions ) {
            newPermission = new Permission( htmlPage.getPermissionId(), permission.getRoleId(), permission.getPermission(), true );
            permissionAPI.save( newPermission, htmlPage, user, false );
        }

        //Save the multi tree
        MultiTreeFactory.saveMultiTree( new MultiTree( htmlPage.getIdentifier(), container.getIdentifier(), contentlet.getIdentifier() ) );

        //Adding it to the test collection
        htmlPages.add( htmlPage );
    }

}