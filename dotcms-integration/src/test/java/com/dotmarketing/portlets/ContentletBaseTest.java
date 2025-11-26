package com.dotmarketing.portlets;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.business.MenuLinkAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Created by Jonathan Gamba.
 * Date: 3/19/12
 * Time: 11:36 AM
 */
public class ContentletBaseTest extends IntegrationTestBase {

    protected static ContentletAPI contentletAPI;
    protected static Host defaultHost;
    protected static Folder testFolder;
    protected static ContentletFactory contentletFactory;
    protected static MenuLinkAPI menuLinkAPI;
    protected static TagAPI tagAPI;
    protected static LanguageAPI languageAPI;
    protected static FieldAPI fieldAPI;
    protected static ContentTypeAPI contentTypeAPI;
    protected static RelationshipAPI relationshipAPI;
    private static RoleAPI roleAPI;
    protected static PermissionAPI permissionAPI;
    protected static HostAPI hostAPI;
    private static CategoryAPI categoryAPI;
    protected static ContainerAPI containerAPI;
    private static TemplateAPI templateAPI;
    protected static FolderAPI folderAPI;

    protected static User user;
    protected static List<Contentlet> contentlets;
    protected static Collection<Container> containers;
    protected static Collection<Structure> structures;
    protected static Collection<Template> templates;
    protected static Collection<Permission> permissions;
    protected static Collection<Identifier> identifiers;
    protected static int FIELDS_SIZE = 14;

    protected static Map<String, Long> uniqueIdentifier = new HashMap<>();

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder getTemporaryFolder(){
       return temporaryFolder;
    }

    private static String wysiwygValue = "<p>Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. " +
            "Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem.</p>" +
            "<p>Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer " +
            "tincidunt. Cras dapibus. Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a.</p>";

    protected static ContentType blogContentType;
    protected static ContentType commentsContentType;
    protected static ContentType newsContentType;
    protected static ContentType wikiContentType;
    protected static ContentType simpleWidgetContentType;

    protected static Language spanishLanguage;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

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
        folderAPI = APILocator.getFolderAPI();
        menuLinkAPI = APILocator.getMenuLinkAPI();
        tagAPI = APILocator.getTagAPI();
        fieldAPI = APILocator.getContentTypeFieldAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user, false);
        relationshipAPI = APILocator.getRelationshipAPI();

        defaultHost = hostAPI.findDefaultHost( user, false );

        structures = new ArrayList<>();
        permissions = new ArrayList<>();
        contentlets = new ArrayList<>();
        containers = new ArrayList<>();
        templates = new ArrayList<>();
        identifiers = new ArrayList<>();

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
            long l = addFields(structure);
            uniqueIdentifier.put( structure.getName(), l );
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
        
        IntegrationTestInitService.getInstance().mockStrutsActionModule();

        //Test content types
        blogContentType = TestDataUtils.getBlogLikeContentType();
        commentsContentType = TestDataUtils.getCommentsLikeContentType();
        newsContentType = TestDataUtils.getNewsLikeContentType();
        wikiContentType = TestDataUtils.getWikiLikeContentType();
        simpleWidgetContentType = TestDataUtils.getWidgetLikeContentType();

        //Search for the Spanish language, if does not exist we need to create it
        spanishLanguage = TestDataUtils.getSpanishLanguage();
    }

    @AfterClass
    public static void afterClass () throws Exception {

        //Delete the contentles
        for ( Contentlet contentlet : contentlets ) {

            contentletAPI.destroy(contentlet, user, false);
            contentlets.remove(contentlet);
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

                contentletAPI.destroy(contentlet, user, false);
            }
            contentTypeAPI.delete(new StructureTransformer(structure).from());
        }
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
        testStructure.setSystem( false );
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
    protected static long addFields ( Structure jUnitTestStructure ) throws DotHibernateException {

        Random random = new Random();
        long uniqueIdentifier = Math.abs(random.nextLong());

        //Create the fields
        Field field = new Field( "JUnit Test Text-" + uniqueIdentifier, Field.FieldType.TEXT, Field.DataType.TEXT, jUnitTestStructure, false, true, false, 1, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Text Area-" + uniqueIdentifier, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 2, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Wysiwyg-" + uniqueIdentifier, Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, jUnitTestStructure, false, false, false, 3, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Date-" + uniqueIdentifier, Field.FieldType.DATE, Field.DataType.DATE, jUnitTestStructure, false, false, false, 4, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Time-" + uniqueIdentifier, Field.FieldType.TIME, Field.DataType.DATE, jUnitTestStructure, false, false, false, 5, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Date Time-" + uniqueIdentifier, Field.FieldType.DATE_TIME, Field.DataType.DATE, jUnitTestStructure, false, false, false, 6, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Integer-"+ uniqueIdentifier, Field.FieldType.TEXT, Field.DataType.INTEGER, jUnitTestStructure, false, false, false, 7, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Float-" + uniqueIdentifier, Field.FieldType.TEXT, Field.DataType.FLOAT, jUnitTestStructure, false, false, false, 8, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Boolean-" + uniqueIdentifier, Field.FieldType.RADIO, Field.DataType.BOOL, jUnitTestStructure, false, false, false, 9, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test File-" + uniqueIdentifier, Field.FieldType.FILE, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 10, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Image-" + uniqueIdentifier, Field.FieldType.IMAGE, Field.DataType.TEXT, jUnitTestStructure, false, false, false, 11, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Binary-" + uniqueIdentifier, Field.FieldType.BINARY, Field.DataType.BINARY, jUnitTestStructure, false, false, false, 12, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Host Folder-" + uniqueIdentifier, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, jUnitTestStructure, false, false, true, 12, false, false, false );
        FieldFactory.saveField( field );

        field = new Field( "JUnit Test Tag-" + uniqueIdentifier, Field.FieldType.TAG, Field.DataType.SYSTEM, jUnitTestStructure, false, false, true, 12, false, false, false );
        FieldFactory.saveField( field );

        return uniqueIdentifier;
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
            } else if ( field.getFieldType().equals( Field.FieldType.DATE.toString())
                || field.getFieldType().equals( Field.FieldType.TIME.toString())
                || field.getFieldType().equals( Field.FieldType.DATE_TIME.toString() ) ) {
                value = new Date();
            } else if ( field.getFieldType().equals( Field.FieldType.BINARY.toString() ) ) {
            	try {
                	java.io.File file = java.io.File.createTempFile("testFile"+field.getVelocityVarName(), ".txt");
            		FileUtil.write(file, "Test Binary");
            		value = file;
            	} catch (Exception e){}
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
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet = contentletAPI.checkin( contentlet, categories, structurePermissions, user, true );

        if ( createWithContainer ) {
            //Create a container
            addContainer( contentlet );
        }

        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
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
        container.setType(Inode.Type.CONTAINERS.getValue());
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

        //Adding it to the test collection
        templates.add( template );
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
        menuLink.setUrl( "dotcms.com" );
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
     * Creates a Relationship object for a later use in the tests
     *
     * @param structure
     * @param required
     * @return
     * @throws DotDataException 
     */
    protected static Relationship createRelationShip ( Structure structure, boolean required ) throws DotDataException {

        return createRelationShip( structure.getInode(), structure.getInode(), required );
    }

    protected static Relationship createRelationShip ( String parentStructureInode, String childStrunctureInode, boolean required) throws DotDataException {
        return createRelationShip(parentStructureInode, childStrunctureInode, required, 0);
    }


        /**
         * Creates a Relationship object for a later use in the tests
         *
         * @param parentStructureInode
         * @param childStrunctureInode
         * @param required
         * @return
         * @throws DotDataException
         */
    protected static Relationship createRelationShip ( String parentStructureInode, String childStrunctureInode,
            boolean required, final int cardinality) throws DotDataException {

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
        relationship.setCardinality(cardinality);

        //Save it
        APILocator.getRelationshipAPI().save( relationship );

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
    protected static ContentletRelationships createContentletRelationships ( final Relationship relationship,
         final Contentlet contentlet, final Structure structure, final List<Contentlet> contentRelationships ) {
        return createContentletRelationships(relationship, contentlet, structure, contentRelationships, null);
    }



    /**
     * Creates a ContentletRelationships object for a later use in the tests
     *
     * @param relationship
     * @param contentlet
     * @param structure
     * @return
     */
    protected static ContentletRelationships createContentletRelationships ( final Relationship relationship,
            final Contentlet contentlet, final Structure structure, final List<Contentlet> contentRelationships,
                                                                             final Boolean hasParent ) {

        //Create the contentlet relationships
        final ContentletRelationships contentletRelationships = new ContentletRelationships( contentlet );

        final boolean internalHasParent = hasParent!=null
            ? hasParent
            : APILocator.getRelationshipAPI().isParent( relationship, structure );

        //Adding the relationships records
        final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords =
            contentletRelationships.new ContentletRelationshipRecords( relationship, internalHasParent );
        contentletRelationshipRecords.setRecords( contentRelationships );

        List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<>();
        relationshipsRecords.add( contentletRelationshipRecords );
        contentletRelationships.setRelationshipsRecords( relationshipsRecords );

        return contentletRelationships;
    }

}