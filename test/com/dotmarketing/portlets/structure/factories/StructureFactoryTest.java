package com.dotmarketing.portlets.structure.factories;

import com.dotmarketing.business.query.GenericQueryFactory;
import com.dotmarketing.business.query.SQLQueryFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/12/12
 * Time: 5:34 PM
 */
public class StructureFactoryTest extends ContentletBaseTest {

    /**
     * Testing {@link StructureFactory#getStructureByInode(String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructureByInode () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the structure
        Structure foundStructure = StructureFactory.getStructureByInode( structure.getInode() );

        //Validations
        assertNotNull( foundStructure );
        assertEquals( foundStructure.getInode(), structure.getInode() );
    }

    /**
     * Testing {@link StructureFactory#getStructureByType(String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructureByType () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the structure
        //TODO: The method is called getStructureByType but it actually search by name...
        Structure foundStructure = StructureFactory.getStructureByType( structure.getName() );

        //Validations
        assertNotNull( foundStructure );
        assertEquals( foundStructure.getInode(), structure.getInode() );
    }

    /**
     * Testing {@link StructureFactory#getStructureByVelocityVarName(String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructureByVelocityVarName () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the structure
        Structure foundStructure = StructureFactory.getStructureByVelocityVarName( structure.getVelocityVarName() );

        //Validations
        assertNotNull( foundStructure );
        assertEquals( foundStructure.getInode(), structure.getInode() );
    }

    /**
     * Testing {@link StructureFactory#getDefaultStructure()}
     *
     * @see StructureFactory
     */
    @Test
    public void getDefaultStructure () {

        //Getting the default structure
        Structure defaultStructure = StructureFactory.getDefaultStructure();

        //Validations
        assertNotNull( defaultStructure );
        assertNotNull( defaultStructure.getInode() );
    }

    /**
     * Testing {@link StructureFactory#getAllStructuresNames()}
     *
     * @see StructureFactory
     */
    @Test
    public void getAllStructuresNames () {

        //Getting all the structures names
        Collection<String> structuresName = StructureFactory.getAllStructuresNames();

        //Validations
        assertTrue( structuresName != null && !structuresName.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getAllVelocityVariablesNames()}
     *
     * @see StructureFactory
     */
    @Test
    public void getAllVelocityVariablesNames () {

        //Getting all the structures velocity variables names
        Collection<String> variablesNames = StructureFactory.getAllVelocityVariablesNames();

        //Validations
        assertTrue( variablesNames != null && !variablesNames.isEmpty() );

        //Validate the variable names
        Structure structure = StructureFactory.getStructureByVelocityVarName( variablesNames.iterator().next() );

        //Validations
        assertTrue( structure != null && structure.getInode() != null );
    }

    /**
     * Testing {@link StructureFactory#findStructureURLMapPatterns()}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void findStructureURLMapPatterns () throws DotDataException {

        //Getting the structures map patterns
        Collection<SimpleStructureURLMap> simpleStructureURLMaps = StructureFactory.findStructureURLMapPatterns();

        //Validations
        assertTrue( simpleStructureURLMaps != null && !simpleStructureURLMaps.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getStructures()}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructures () {

        //Getting all the structures
        Collection<Structure> structuresCollection = StructureFactory.getStructures();

        //Validations
        assertTrue( structuresCollection != null && !structuresCollection.isEmpty() );

        //Validate the integrity of the array
        Structure structure = StructureFactory.getStructureByVelocityVarName( structuresCollection.iterator().next().getVelocityVarName() );

        //Validations
        assertTrue( structure != null && structure.getInode() != null );
    }

    /**
     * Testing {@link StructureFactory#getStructuresByUser(com.liferay.portal.model.User, String, String, int, int, String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructuresByUser () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructuresByUser( user, "structuretype=" + structure.getStructureType(), "upper(name)", 0, 0, "asc" );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getStructuresWithWritePermissions(com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void getStructuresWithWritePermissions () throws DotDataException {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructuresWithWritePermissions( user, false );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getStructuresWithReadPermissions(com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void getStructuresWithReadPermissions () throws DotDataException {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructuresWithReadPermissions( user, false );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getNoSystemStructuresWithReadPermissions(com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void getNoSystemStructuresWithReadPermissions () throws DotDataException {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getNoSystemStructuresWithReadPermissions( user, false );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getStructuresByWFScheme(com.dotmarketing.portlets.workflows.model.WorkflowScheme, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Ignore ( "Not Ready to Run, need to ask about how the relation is between a WorkflowScheme and Structures." )
    @Test
    public void getStructuresByWFScheme () throws DotDataException {

        /*//Search for the structures
        Collection<Structure> structures = StructureFactory.getStructuresByWFScheme( null, user, false );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );*/

        //TODO: Not sure about the relation between a WorkflowScheme and Structures...., ask about it
    }

    /**
     * Testing {@link StructureFactory#getStructures(int)}
     *
     * @see StructureFactory
     */
    @SuppressWarnings ( "unchecked" )
    @Test
    public void getStructuresWithLimit () {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructures( 2 );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
        assertEquals( structures.size(), 2 );
    }

    /**
     * Testing {@link StructureFactory#getStructures(String, int)}
     *
     * @see StructureFactory
     */
    @SuppressWarnings ( "unchecked" )
    @Test
    public void getStructuresWithOrderAndLimit () {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructures( "name", 2 );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
        assertEquals( structures.size(), 2 );
    }

    /**
     * Testing {@link StructureFactory#getStructures(String, int, String)}
     *
     * @see StructureFactory
     */
    @SuppressWarnings ( "unchecked" )
    @Test
    public void getStructuresWithOrderLimitAndDirection () {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructures( "name", 2, "asc" );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
        assertEquals( structures.size(), 2 );
    }

    /**
     * Testing {@link StructureFactory#getStructures(String, String, int, int, String)}
     *
     * @see StructureFactory
     */
    @SuppressWarnings ( "unchecked" )
    @Test
    public void getStructuresAllParameters () {

        //Search for the structures
        Collection<Structure> structures = StructureFactory.getStructures( "structuretype=" + Structure.STRUCTURE_TYPE_CONTENT, "name", 2, 0, "asc" );

        //Validations
        assertTrue( structures != null && !structures.isEmpty() );
        assertEquals( structures.size(), 2 );
    }

    /**
     * Testing the methods {@link StructureFactory#saveStructure(com.dotmarketing.portlets.structure.model.Structure)}, {@link StructureFactory#deleteStructure(String)}
     * and {@link StructureFactory#deleteStructure(com.dotmarketing.portlets.structure.model.Structure)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void structures () throws DotDataException {

        //Create the new structures
        Structure testStructure1 = new Structure();

        testStructure1.setDefaultStructure( false );
        testStructure1.setDescription( "JUnit Test Structure Description." );
        testStructure1.setFixed( false );
        testStructure1.setIDate( new Date() );
        testStructure1.setName( "JUnit Test Structure_4" );
        testStructure1.setOwner( user.getUserId() );
        testStructure1.setDetailPage( "" );
        testStructure1.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure1.setSystem( true );
        testStructure1.setType( "structure" );
        testStructure1.setVelocityVarName( "junit_test_structure_4" );

        Structure testStructure2 = new Structure();

        testStructure2.setDefaultStructure( false );
        testStructure2.setDescription( "JUnit Test Structure Description." );
        testStructure2.setFixed( false );
        testStructure2.setIDate( new Date() );
        testStructure2.setName( "JUnit Test Structure_5" );
        testStructure2.setOwner( user.getUserId() );
        testStructure2.setDetailPage( "" );
        testStructure2.setStructureType( Structure.STRUCTURE_TYPE_CONTENT );
        testStructure2.setSystem( true );
        testStructure2.setType( "structure" );
        testStructure2.setVelocityVarName( "junit_test_structure_5" );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Saving the structures
        StructureFactory.saveStructure( testStructure1 );
        StructureFactory.saveStructure( testStructure2 );

        //Validations
        assertNotNull( testStructure1.getInode() );
        assertNotNull( testStructure2.getInode() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Getting the structure we just saved
        Structure savedStructure = StructureFactory.getStructureByInode( testStructure1.getInode() );

        //Validations
        assertEquals( testStructure1.getInode(), savedStructure.getInode() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Updating the structure
        String updatedName = "UPDATED --- " + savedStructure.getName();
        savedStructure.setName( updatedName );
        StructureFactory.saveStructure( savedStructure );

        //Getting again the saved structure
        testStructure1 = StructureFactory.getStructureByInode( savedStructure.getInode() );

        //Validations
        assertNotNull( testStructure1.getInode() );
        assertEquals( testStructure1.getName(), updatedName );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Delete the structure
        String inode = savedStructure.getInode();
        StructureFactory.deleteStructure( inode );

        //Verify what we just deleted
        Structure tempStructure = StructureFactory.getStructureByInode( inode );

        //validations
        assertTrue( tempStructure == null || tempStructure.getInode() == null || tempStructure.getInode().isEmpty() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Delete the structure
        inode = testStructure2.getInode();
        StructureFactory.deleteStructure( testStructure2 );

        //Verify what we just deleted
        tempStructure = StructureFactory.getStructureByInode( inode );

        //validations
        assertTrue( tempStructure == null || tempStructure.getInode() == null || tempStructure.getInode().isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#disableDefault()}
     *
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     * @see StructureFactory
     */
    @Test
    public void disableDefault () throws DotHibernateException {

        //Getting the default structure
        Structure defaultStructure = StructureFactory.getDefaultStructure();

        if ( defaultStructure.getInode() == null || defaultStructure.getInode().isEmpty() ) {
            //Getting a known structure and make it the default
            defaultStructure = structures.iterator().next();
            defaultStructure.setDefaultStructure( true );
            StructureFactory.saveStructure( defaultStructure );
        }

        //Disable the default structure
        StructureFactory.disableDefault();

        //Getting the one that was the default
        defaultStructure = StructureFactory.getStructureByInode( defaultStructure.getInode() );

        //Validations
        assertFalse( defaultStructure.isDefaultStructure() );

        //Set the default structure back to normal
        defaultStructure.setDefaultStructure( true );
        StructureFactory.saveStructure( defaultStructure );
    }

    /**
     * Testing {@link StructureFactory#getTotalDates(Structure)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotalDates () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the date fields for this structure
        int totalDates = StructureFactory.getTotalDates( structure );

        //Validations
        assertTrue( totalDates > 0 );
    }

    /**
     * Testing {@link StructureFactory#getTotalImages(Structure)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotalImages () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the images fields for this structure
        int totalImages = StructureFactory.getTotalImages( structure );

        //Validations
        assertTrue( totalImages > 0 );
    }

    /**
     * Testing {@link StructureFactory#getTotalFiles(Structure)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotalFiles () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the files fields for this structure
        int totalFiles = StructureFactory.getTotalFiles( structure );

        //Validations
        assertTrue( totalFiles > 0 );
    }

    /**
     * Testing {@link StructureFactory#getTotalTextAreas(Structure)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotalTextAreas () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the text areas fields for this structure
        int totalTextAreas = StructureFactory.getTotalTextAreas( structure );

        //Validations
        assertTrue( totalTextAreas > 0 );
    }

    /**
     * Testing {@link StructureFactory#getTotalWYSIWYG(Structure)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotalWYSIWYG () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the WYSIWYG fields for this structure
        int totalWYSIWYG = StructureFactory.getTotalWYSIWYG( structure );

        //Validations
        assertTrue( totalWYSIWYG > 0 );
    }

    /**
     * Testing {@link StructureFactory#getTotals(Structure, String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTotals () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the text fields for this structure
        int totalText = StructureFactory.getTotals( structure, Field.FieldType.TEXT.toString() );

        //Validations
        assertTrue( totalText > 0 );
    }

    /**
     * Testing {@link StructureFactory#createDefaultStructure()}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Ignore ( "Testing this method is quite difficult, and we actually don't use it, don't test it for now..." )
    @Test
    public void createDefaultStructure () throws DotDataException {

        //Disabled the current default structure
        StructureFactory.disableDefault();

        //Trying to find the default structure
        Structure defaultStructure = StructureFactory.getDefaultStructure();

        //Validations
        assertTrue( defaultStructure.getInode() == null || defaultStructure.getInode().isEmpty() );//It shouldn't be any default structure, we just disabled the default, no one should be found

        //Create a new default structure
        StructureFactory.createDefaultStructure();

        //Find the new default structure
        defaultStructure = StructureFactory.getDefaultStructure();

        //Validations
        assertTrue( defaultStructure.getInode() != null && !defaultStructure.getInode().isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getTagsFields(String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getTagsFields () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting all the tags fields for this structure
        Collection<Field> tagsFields = StructureFactory.getTagsFields( structure.getInode() );

        //Validations
        assertTrue( tagsFields != null && !tagsFields.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getStructuresCount(String)}
     *
     * @see StructureFactory
     */
    @Test
    public void getStructuresCount () {

        //Searching using a condition
        int structuresCount = StructureFactory.getStructuresCount( "structuretype = " + Structure.STRUCTURE_TYPE_CONTENT );

        //Validations
        assertTrue( structuresCount > 0 );
    }

    /**
     * Testing {@link StructureFactory#getImagesFieldsList(Structure, java.util.List, java.util.List)}
     *
     * @see StructureFactory
     */
    @Test
    public void getImagesFieldsList () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Preparing the parameters for the search
        List<String> varNames = new ArrayList<String>();
        varNames.add( UtilMethods.toCamelCase( "JUnit Test Image" ) );

        List<String[]> fieldValues = new ArrayList<String[]>();//Not sure about this parameter, not sure what is for, the method getImagesFieldsList just ask if it is a null value.....
        fieldValues.add( new String[]{ "JUnit Test Image" } );

        //Getting the fields list
        List<Field> fieldList = StructureFactory.getImagesFieldsList( structure, varNames, fieldValues );

        //Validations
        assertTrue( fieldList != null && !fieldList.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#getFilesFieldsList(Structure, java.util.List, java.util.List)}
     *
     * @see StructureFactory
     */
    @Test
    public void getFilesFieldsList () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Preparing the parameters for the search
        List<String> varNames = new ArrayList<String>();
        varNames.add( UtilMethods.toCamelCase( "JUnit Test File" ) );

        List<String[]> fieldValues = new ArrayList<String[]>();//Not sure about this parameter, not sure what is for, the method getImagesFieldsList just ask if it is a null value.....
        fieldValues.add( new String[]{ "JUnit Test File" } );

        //Getting the fields list
        List<Field> fieldList = StructureFactory.getFilesFieldsList( structure, varNames, fieldValues );

        //Validations
        assertTrue( fieldList != null && !fieldList.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#DBSearch(com.dotmarketing.business.query.GenericQueryFactory.Query, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @see StructureFactory
     */
    @Test
    public void DBSearch () throws DotDataException {

        //Build the query
        String sql = "SELECT * FROM structure ORDER BY inode";
        SQLQueryFactory sqlQueryFactory = new SQLQueryFactory( sql );
        GenericQueryFactory.Query query = sqlQueryFactory.getQuery();

        //Make the search
        List<Map<String, Serializable>> resultList = StructureFactory.DBSearch( query, user, false );

        //Validations
        assertTrue( resultList != null && !resultList.isEmpty() );
    }

    /**
     * Testing {@link StructureFactory#findStructuresUserCanUse(com.liferay.portal.model.User, String, Integer, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see StructureFactory
     */
    @Test
    public void findStructuresUserCanUse () throws DotSecurityException, DotDataException {

        //Lets filter by name
        String filter = "JUnit Test Structure_0";

        //Make the search
        //TODO: It's weird, the method ask for a query parameter, but what it actually needs is a name to filter, I mean, the method at the end will just grab that "query" and use it like this: and (lower(structure.name) LIKE '%" + searchString.toLowerCase() + "%'"
        //TODO: Would be good to focus more on the javadoc and in the firm of the methods....
        Collection<Structure> structureCollection = StructureFactory.findStructuresUserCanUse( user, filter, Structure.STRUCTURE_TYPE_CONTENT, 0, 10 );
        //Collection<Structure> structureCollection = StructureFactory.findStructuresUserCanUse( user, null, Structure.STRUCTURE_TYPE_CONTENT, 0, 10 );

        //Validations
        assertTrue( structureCollection != null && !structureCollection.isEmpty() );
    }

}