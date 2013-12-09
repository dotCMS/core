package com.dotmarketing.portlets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

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
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:36 AM
 */
public class ContentletBaseTest extends TestBase {

    protected static ContentletAPI contentletAPI;
    protected static Host defaultHost;
    protected static Folder testFolder;
    protected static ContentletFactory contentletFactory;
    protected static MenuLinkAPI menuLinkAPI;
    protected static FileAPI fileAPI;
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
    protected static Collection<Identifier> identifiers;
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
        menuLinkAPI = APILocator.getMenuLinkAPI();
        fileAPI = APILocator.getFileAPI();

        defaultHost = hostAPI.findDefaultHost( user, false );

        structures = new ArrayList<Structure>();
        permissions = new ArrayList<Permission>();
        contentlets = new ArrayList<Contentlet>();
        containers = new ArrayList<Container>();
        templates = new ArrayList<Template>();
        htmlPages = new ArrayList<HTMLPage>();
        identifiers = new ArrayList<Identifier>();

        //*******************************************************************************
        //Create the new folder
        testFolder = new Folder();

        testFolder.setFilesMasks( "" );
        testFolder.setIDate( new Date() );
        testFolder.setName( "dotcms_junit_test_folder_" + String.valueOf( new Date().getTime() ) );
        testFolder.setOwner( user.getUserId() );
        testFolder.setShowOnMenu( false );
        testFolder.setSortOrder( 0 );
        testFolder.setTitle( "dotcms_junit_test_folder_" + String.valueOf( new Date().getTime() ) );
        testFolder.setType( "folder" );
        testFolder.setHostId( defaultHost.getIdentifier() );
        //Creates and set an identifier
        Identifier identifier = APILocator.getIdentifierAPI().createNew( testFolder, defaultHost );
        identifiers.add( identifier );
        testFolder.setIdentifier( identifier.getId() );

        //Saving the folder
        folderAPI.save( testFolder, user, false );

        //*******************************************************************************
        //Creating tests structures
        Structure newStructure = createStructure( "Test Structure_0_"+System.currentTimeMillis(), "junit_test_st_0_"+System.currentTimeMillis() );
        structures.add( newStructure );
        newStructure = createStructure( "JUnit Test Structure_1_"+System.currentTimeMillis(), "junit_test_st_1_"+System.currentTimeMillis() );
        structures.add( newStructure );
        newStructure = createStructure( "JUnit Test Structure_2_"+System.currentTimeMillis(), "junit_test_st_2_"+System.currentTimeMillis() );
        structures.add( newStructure );
        newStructure = createStructure( "JUnit Test Structure_3_"+System.currentTimeMillis(), "junit_test_st_3_"+System.currentTimeMillis() );
        structures.add( newStructure );

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
        Contentlet newContentlet = createContentlet( testStructure1, null, true );
        contentlets.add( newContentlet );

        //Set the language to default value
        Language language = languageAPI.getDefaultLanguage();
        List<Language> languages = languageAPI.getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }
        newContentlet = createContentlet( testStructure2, language, true );
        contentlets.add( newContentlet );
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

        //Delete the identifiers
        /*for ( Identifier identifier : identifiers ) {
            APILocator.getIdentifierAPI().delete( identifier );
        }*/

        //hostAPI.delete( defaultHost, user, false );

        //Delete the folder
        //folderAPI.delete( testFolder, user, false );
    }

    /**
     * Creates an Structure object for a later use in the tests
     *
     * @param name
     * @param structureVelocityVarName
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     */
    protected static Structure createStructure ( String name, String structureVelocityVarName ) throws DotDataException, DotSecurityException {

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

        return testStructure;
    }

    /**
     * Creating fields and add them to a given structure for testing
     *
     * @param jUnitTestStructure
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     */
    protected static void addFields ( Structure jUnitTestStructure ) throws DotHibernateException {

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
     * Creates a Contentlet object for a later use in the tests
     *
     * @param structure
     * @param language
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    protected static Contentlet createContentlet ( Structure structure, Language language, Boolean createWithContainer ) throws DotDataException, DotSecurityException {

        //Create the new Contentlet
        Contentlet contentlet = new Contentlet();
        contentlet.setReviewInterval( "1m" );
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
        contentletAPI.isInodeIndexed(contentlet.getInode());
        
        if ( createWithContainer ) {
            //Create a container
            addContainer( contentlet );
        }

        return contentlet;
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
        htmlPage.setPageUrl( "junit_htmlpage_test_" + contentlet.getInode() + "." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION") );
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

        //Make it working and live
        APILocator.getVersionableAPI().setWorking( htmlPage );
        APILocator.getVersionableAPI().setLive( htmlPage );

        //Adding it to the test collection
        htmlPages.add( htmlPage );
    }

    /**
     * Creates a Link object for a later use in the tests
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    protected static Link createMenuLink () throws DotSecurityException, DotDataException {

        //Creating the menu link
        Link menuLink = new Link();
        menuLink.setModUser( user.getUserId() );
        menuLink.setOwner( user.getUserId() );
        menuLink.setProtocal( "" );
        menuLink.setShowOnMenu( true );
        menuLink.setSortOrder( 2 );
        menuLink.setTarget( "_blank" );
        menuLink.setTitle( "JUnit MenuLink Test" );
        menuLink.setType( "links" );
        menuLink.setUrl( "www.dotcms.org" );
        menuLink.setFriendlyName( "JUnit Test Menu Link" );
        menuLink.setIDate( new Date() );
        menuLink.setInternalLinkIdentifier( "" );
        menuLink.setLinkCode( "" );
        menuLink.setLinkType( Link.LinkType.EXTERNAL.toString() );
        menuLink.setModDate( new Date() );

        //Saving it and adding it permissions
        menuLinkAPI.save( menuLink, testFolder, user, false );
        permissionAPI.copyPermissions( testFolder, menuLink );

        //Make it working and live
        /*APILocator.getVersionableAPI().setLocked( menuLink, false, user );
        APILocator.getVersionableAPI().setWorking( menuLink );
        APILocator.getVersionableAPI().setLive( menuLink );*/

        return menuLink;
    }

    /**
     * Creates a File object for a later use in the tests
     *
     * @param fileName
     * @return savedFile
     * @throws Exception
     * @see File
     */
    protected static File createFile ( String fileName ) throws Exception {

        String testFilesPath = ".." + java.io.File.separator +
                "test" + java.io.File.separator +
                "com" + java.io.File.separator +
                "dotmarketing" + java.io.File.separator +
                "portlets" + java.io.File.separator +
                "contentlet" + java.io.File.separator +
                "business" + java.io.File.separator +
                "test_files" + java.io.File.separator;

        String copyTestFilesPath = ".." + java.io.File.separator +
                "test" + java.io.File.separator +
                "com" + java.io.File.separator +
                "dotmarketing" + java.io.File.separator +
                "portlets" + java.io.File.separator +
                "contentlet" + java.io.File.separator +
                "business" + java.io.File.separator +
                "test_files" + java.io.File.separator +
                "copy" + java.io.File.separator;

        //Reading the file
        String testFilePath = FileUtil.getRealPath( testFilesPath + fileName );
        java.io.File tempTestFile = new java.io.File( testFilePath );
        if ( !tempTestFile.exists() ) {
            String message = "File does not exist: '" + testFilePath + "'";
            throw new Exception( message );
        }

        //Copying the file
        String copyTestFilePath = FileUtil.getRealPath( copyTestFilesPath + fileName );
        java.io.File copyTempTestFile = new java.io.File( copyTestFilePath );
        if ( !copyTempTestFile.exists() ) {
            if ( !copyTempTestFile.createNewFile() ) {
                String message = "Cannot create copy of the test file: '" + copyTestFilePath + "'";
                throw new Exception( message );
            }
        }

        InputStream in = new FileInputStream( tempTestFile );
        OutputStream out = new FileOutputStream( copyTestFilePath );

        byte[] buf = new byte[1024];
        int len;
        while ( 0 < ( len = in.read( buf ) ) ) {
            out.write( buf, 0, len );
        }

        //Creating a test file
        File testFile = new File();
        testFile.setAuthor( user.getUserId() );
        testFile.setFileName( "junit_test_file.txt" );
        testFile.setFriendlyName( "JUnit Test File Friendly Name" );
        testFile.setIDate( new Date() );
        testFile.setMaxSize( 1024 );
        testFile.setMimeType( "text/plain" );
        testFile.setModDate( new Date() );
        testFile.setModUser( user.getUserId() );
        testFile.setOwner( user.getUserId() );
        testFile.setPublishDate( new Date() );
        testFile.setShowOnMenu( true );
        testFile.setSize( ( int ) tempTestFile.length() );
        testFile.setSortOrder( 2 );
        testFile.setTitle( "JUnit Test File" );
        testFile.setType( "file_asset" );

        //Storing the file
        File savedFile = fileAPI.saveFile( testFile, copyTempTestFile, testFolder, user, false );
        //Adding permissions
        permissionAPI.copyPermissions( testFolder, savedFile );

        if ( copyTempTestFile.exists() ) {
            copyTempTestFile.delete();
        }

        return savedFile;
    }

    /**
     * Creates a Relationship object for a later use in the tests
     *
     * @param structure
     * @param required
     * @return
     * @throws DotHibernateException
     */
    protected static Relationship createRelationShip ( Structure structure, boolean required ) throws DotHibernateException {

        return createRelationShip( structure.getInode(), structure.getInode(), required );
    }

    /**
     * Creates a Relationship object for a later use in the tests
     *
     * @param parentStructureInode
     * @param childStrunctureInode
     * @param required
     * @return
     * @throws DotHibernateException
     */
    protected static Relationship createRelationShip ( String parentStructureInode, String childStrunctureInode, boolean required ) throws DotHibernateException {

        Relationship relationship = new Relationship();
        //Set Parent Info
        relationship.setParentStructureInode( parentStructureInode );
        relationship.setParentRelationName( "parent" );
        relationship.setParentRequired( required );
        //Set Child Info
        relationship.setChildStructureInode( childStrunctureInode );
        relationship.setChildRelationName( "child" );
        relationship.setChildRequired( required );
        //Set general info
        relationship.setRelationTypeValue( "parent-child" );
        relationship.setCardinality( 0 );

        //Save it
        RelationshipFactory.saveRelationship( relationship );

        return relationship;
    }

    /**
     * Creates a ContentletRelationships object for a later use in the tests
     *
     * @param relationship
     * @param contentlet
     * @param structure
     * @return
     */
    protected static ContentletRelationships createContentletRelationships ( Relationship relationship, Contentlet contentlet, Structure structure, List<Contentlet> contentRelationships ) {

        //Create the contentlet relationships
        ContentletRelationships contentletRelationships = new ContentletRelationships( contentlet );

        boolean hasParent = RelationshipFactory.isParentOfTheRelationship( relationship, structure );

        //Adding the relationships records
        ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords = contentletRelationships.new ContentletRelationshipRecords( relationship, hasParent );
        contentletRelationshipRecords.setRecords( contentRelationships );

        List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
        relationshipsRecords.add( contentletRelationshipRecords );
        contentletRelationships.setRelationshipsRecords( relationshipsRecords );

        return contentletRelationships;
    }

}