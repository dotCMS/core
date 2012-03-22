package com.dotmarketing.portlets.structure.factories;

import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/6/12
 * Time: 4:34 PM
 */
public class FieldFactoryTest extends ContentletBaseTest {

    /**
     * Testing {@link FieldFactory#getFieldByInode(String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void getFieldByInode () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Verify we found an stored field
        Field foundField = FieldFactory.getFieldByInode( field.getInode() );
        assertNotNull( foundField );
    }

    /**
     * Testing {@link FieldFactory#getFieldsByStructure(String)}
     *
     * @see FieldFactory
     */
    @Test
    public void getFieldsByStructure () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldFactory.getFieldsByStructure( structure.getInode() );

        //Start with the validations
        assertTrue( fields != null && !fields.isEmpty() );
        assertEquals( fields.size(), FIELDS_SIZE );
    }

    /**
     * Testing {@link FieldFactory#getFieldsByStructureSortedBySortOrder(String)}
     *
     * @see FieldFactory
     */
    @Test
    public void getFieldsByStructureSortedBySortOrder () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldFactory.getFieldsByStructureSortedBySortOrder( structure.getInode() );

        //Start with the validations
        assertTrue( fields != null && !fields.isEmpty() );
        assertEquals( fields.size(), FIELDS_SIZE );
    }

    /**
     * Testing {@link FieldFactory#getFieldByStructureNoLock(String)}
     *
     * @see FieldFactory
     */
    @Test
    public void getFieldByStructureNoLock () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldFactory.getFieldByStructureNoLock( structure.getInode() );

        //Start with the validations
        assertTrue( fields != null && !fields.isEmpty() );
        assertEquals( fields.size(), FIELDS_SIZE );
    }

    /**
     * Testing {@link FieldFactory#isTagField(String, com.dotmarketing.portlets.structure.model.Structure)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void isTagField () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Start with the validations
        assertTrue( fields != null && !fields.isEmpty() );

        for ( Field field : fields ) {

            //Its a tag field???
            Boolean isTagField = FieldFactory.isTagField( field.getFieldContentlet(), structure );
            if ( field.getFieldType().equals( Field.FieldType.TAG.toString() ) ) {
                assertTrue( isTagField );
            } else {
                assertFalse( isTagField );
            }
        }

    }

    /**
     * Testing {@link FieldFactory#getFieldsByContentletField(String, String, String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    @SuppressWarnings ( "unchecked" )
    public void getFieldsByContentletField () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Search by the field contentlet
        //TODO: The data type is used everywhere instead the field.getFieldContentlet() as the method name, parameter and even te query suggested...
        Collection<Field> fieldsByContentlet = FieldFactory.getFieldsByContentletField( Field.DataType.TEXT.toString(), field.getInode(), structure.getInode() );

        //Validations
        assertTrue( fieldsByContentlet != null && !fieldsByContentlet.isEmpty() );

        //Search by the field contentlet and with an Inode null
        //TODO: The data type is used everywhere instead the field.getFieldContentlet() as the method name, parameter and even te query suggested...
        fieldsByContentlet = FieldFactory.getFieldsByContentletField( Field.DataType.TEXT.toString(), null, structure.getInode() );

        //Validations
        assertTrue( fieldsByContentlet != null && !fieldsByContentlet.isEmpty() );
    }

    /**
     * Testing {@link FieldFactory#getFieldByVariableName(String, String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void getFieldByVariableName () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Search by variable name
        Field foundField = FieldFactory.getFieldByVariableName( structure.getInode(), field.getVelocityVarName() );

        //Start with the validations
        assertNotNull( foundField );
        assertEquals( foundField.getInode(), field.getInode() );
    }

    /**
     * Testing {@link FieldFactory#getFieldByStructure(String, String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void getFieldByStructure () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Search by field name
        Field foundField = FieldFactory.getFieldByStructure( structure.getInode(), field.getFieldName() );

        //Start with the validations
        assertNotNull( foundField );
        assertEquals( foundField.getInode(), field.getInode() );
    }

    /**
     * Testing {@link FieldFactory#getFieldByName(String, String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void getFieldByName () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Search by field name
        //Field foundField = FieldFactory.getFieldByName( structure.getInode(), field.getFieldName() );
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundField = FieldFactory.getFieldByName( structure.getName(), field.getFieldName() );

        //Start with the validations
        assertNotNull( foundField );
        assertEquals( foundField.getInode(), field.getInode() );
    }

    /**
     * Testing the methods {@link FieldFactory#saveField(com.dotmarketing.portlets.structure.model.Field)}, {@link FieldFactory#deleteField(com.dotmarketing.portlets.structure.model.Field)}
     * and {@link FieldFactory#deleteField(String)}
     *
     * @throws com.dotmarketing.exception.DotHibernateException
     *
     * @see FieldFactory
     */
    @Test
    public void saveDeleteField () throws DotHibernateException {

        String NAME_ORIGINAL = "JUnit Test Text --  test";
        String NAME_UPDATED = "UPDATED --- JUnit Test Text --  test";

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Creating a field
        Field field = new Field( NAME_ORIGINAL, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, false, 1, false, false, false );
        Field field2 = new Field( NAME_ORIGINAL + "_2", Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, true, false, 1, false, false, false );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Testing the save
        FieldFactory.saveField( field );
        FieldFactory.saveField( field2 );

        //Validations
        assertNotNull( field.getInode() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Updating the field
        field.setFieldName( NAME_UPDATED );
        FieldFactory.saveField( field );

        //Getting the field we just updated
        Field foundField = FieldFactory.getFieldByInode( field.getInode() );

        //Validations
        assertEquals( field.getInode(), foundField.getInode() );
        assertEquals( field.getFieldName(), foundField.getFieldName() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Testing one of the deletes
        FieldFactory.deleteField( field );

        //Trying to get the field we just deleted
        Field deletedField = FieldFactory.getFieldByInode( foundField.getInode() );

        //Validations
        assertTrue( deletedField.getInode() == null || deletedField.getInode().isEmpty() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Testing another delete
        String field2Inode = field2.getInode();
        FieldFactory.deleteField( field2 );

        //Trying to get the field we just deleted
        deletedField = FieldFactory.getFieldByInode( field2Inode );

        //Validations
        assertTrue( deletedField.getInode() == null || deletedField.getInode().isEmpty() );
    }

    /**
     * Testing {@link FieldFactory#getNextAvaliableFieldNumber(String, String, String)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void getNextAvailableFieldNumber () {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //FIXME: Work more on this test, verify when the max ammount of fields is reach....
        //Find the next available field number
        String fieldContentlet = FieldFactory.getNextAvaliableFieldNumber( Field.DataType.TEXT.toString(), field.getInode(), field.getStructureInode() );

        //Validations
        assertTrue( fieldContentlet != null && !fieldContentlet.isEmpty() );
    }

    /**
     * Testing the methods {@link FieldFactory#saveFieldVariable(com.dotmarketing.portlets.structure.model.FieldVariable)}, {@link FieldFactory#getFieldVariable(String)},
     * {@link FieldFactory#getFieldVariablesForField(String)}, {@link FieldFactory#getAllFieldVariables()}, {@link FieldFactory#deleteFieldVariable(String)} and {@link FieldFactory#deleteFieldVariable(com.dotmarketing.portlets.structure.model.FieldVariable)}
     *
     * @see FieldFactory
     * @see FieldsCache
     */
    @Test
    public void fieldVariables () {

        String NAME_ORIGINAL = "JUnit Test variable name";
        String NAME_UPDATED = "UPDATED --- Test variable name";

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Getting the fields for this structure
        Collection<Field> fields = FieldsCache.getFieldsByStructureInode( structure.getInode() );

        //Validations
        assertTrue( fields != null && !fields.isEmpty() );

        Iterator<Field> iterator = fields.iterator();
        Field field = iterator.next();

        //Creating the FieldVariables
        FieldVariable fieldVariable = new FieldVariable();
        fieldVariable.setFieldId( field.getInode() );
        fieldVariable.setName( NAME_ORIGINAL );
        fieldVariable.setKey( "test_variable_key" );
        fieldVariable.setValue( "test variable value" );
        fieldVariable.setLastModifierId( user.getUserId() );
        fieldVariable.setLastModDate( new Date() );

        FieldVariable fieldVariable2 = new FieldVariable();
        fieldVariable2.setFieldId( field.getInode() );
        fieldVariable2.setName( NAME_ORIGINAL + "_2" );
        fieldVariable2.setKey( "test_variable_key_2" );
        fieldVariable2.setValue( "test variable value_2" );
        fieldVariable2.setLastModifierId( user.getUserId() );
        fieldVariable2.setLastModDate( new Date() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Saving the field variable
        FieldFactory.saveFieldVariable( fieldVariable );

        //Validations
        assertNotNull( fieldVariable.getId() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Getting the variable we just saved
        FieldVariable savedVariable = FieldFactory.getFieldVariable( fieldVariable.getId() );

        //Validations
        assertEquals( fieldVariable.getId(), savedVariable.getId() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Updating the field variable
        savedVariable.setName( NAME_UPDATED );
        FieldFactory.saveFieldVariable( fieldVariable );

        //Getting again the saved variable
        fieldVariable = FieldFactory.getFieldVariable( fieldVariable.getId() );

        //Validations
        assertNotNull( fieldVariable.getId() );
        assertEquals( fieldVariable.getName(), NAME_UPDATED );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Getting all the variables for a given field
        Collection<FieldVariable> variables = FieldFactory.getFieldVariablesForField( field.getInode() );

        //Validations
        assertTrue( variables != null && !variables.isEmpty() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Getting all the variables
        variables = FieldFactory.getAllFieldVariables();

        //Validations
        assertTrue( variables != null && !variables.isEmpty() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Delete a given variable
        String variableId = fieldVariable.getId();
        FieldFactory.deleteFieldVariable( fieldVariable );
        //Try to get the deleted variable
        FieldVariable tempFieldVariable = FieldFactory.getFieldVariable( variableId );

        //validations
        assertTrue( tempFieldVariable == null || tempFieldVariable.getId() == null || tempFieldVariable.getId().isEmpty() );

        //++++++++++++++++++++++++++++++++++++++++++++
        //Delete a given variable
        variableId = fieldVariable2.getId();
        FieldFactory.deleteFieldVariable( variableId );
        //Try to get the deleted variable
        tempFieldVariable = FieldFactory.getFieldVariable( variableId );

        //validations
        assertTrue( tempFieldVariable == null || tempFieldVariable.getId() == null || tempFieldVariable.getId().isEmpty() );
    }

}