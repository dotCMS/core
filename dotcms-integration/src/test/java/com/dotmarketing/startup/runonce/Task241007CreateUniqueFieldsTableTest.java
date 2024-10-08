package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.AssertException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static org.junit.Assert.*;

/**
 * Test of {@link Task241007CreateUniqueFieldsTable}
 */
public class Task241007CreateUniqueFieldsTableTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void cleaningUp() throws DotDataException {
        new DotConnect().setSQL("DROP TABLE IF EXISTS unique_fields CASCADE").loadObjectResults();
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#forceRun()}
     * When the table did not exist
     * Should: return true
     */
    @Test
    public void runForce(){
        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#executeUpgrade()}
     * When: the table did not exist and run the method
     * Should: create the table and the forceRUn method must return false
     */
    @Test
    public void createTable() throws DotDataException {

        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());

        task241007CreateUniqueFieldsTable.executeUpgrade();

        assertFalse(task241007CreateUniqueFieldsTable.forceRun());
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#executeUpgrade()}
     * When: Run the method twice
     * Should: not thrown any Exception
     */
    @Test
    public void runTwice() throws DotDataException {
        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());

        task241007CreateUniqueFieldsTable.executeUpgrade();
        assertFalse(task241007CreateUniqueFieldsTable.forceRun());

        task241007CreateUniqueFieldsTable.executeUpgrade();
        assertFalse(task241007CreateUniqueFieldsTable.forceRun());
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#executeUpgrade()}
     * When: Run the method and already exists Contentlet with Unique field
     * Should: populate the table with these values
     */
    @Test
    public void populate() throws DotDataException, NoSuchAlgorithmException, IOException {
        final Field titleField = new FieldDataGen().type(TextField.class).name("title").next();
        final Field uniqueField = new FieldDataGen().type(TextField.class).name("unique").unique(true).next();

        final ContentType contentType = new ContentTypeDataGen().field(titleField).field(uniqueField).nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_1_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), "Unique_1_" + System.currentTimeMillis())
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_2_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), "Unique_2_" + System.currentTimeMillis())
                .nextPersisted();

        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());
        task241007CreateUniqueFieldsTable.executeUpgrade();
        assertFalse(task241007CreateUniqueFieldsTable.forceRun());

        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * from unique_fields").loadObjectResults();

        assertFalse(results.isEmpty());

        final String valueToHash_1 = getHash(contentType, uniqueField, contentlet_1);
        final String valueToHash_2 = getHash(contentType, uniqueField, contentlet_2);

        final Map<String, Object> result_1 = results.stream()
                .filter(result -> result.get("unique_key_val").equals(valueToHash_1))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertException("contenlet_1 expected"));

        final Map<String, Object> result_2 = results.stream()
                .filter(result -> result.get("unique_key_val").equals(valueToHash_2))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertException("contenlet_2 expected"));

        checkSupportingValues(result_1, contentType, uniqueField, contentlet_1);
        checkSupportingValues(result_2, contentType, uniqueField, contentlet_2);
    }

    private static void checkSupportingValues(Map<String, Object> result_1, ContentType contentType, Field uniqueField, Contentlet... contentlets) throws IOException {
        final Map<String, Object> supportingValues_1 = JsonUtil.getJsonFromString(result_1.get("supporting_values").toString());
        assertEquals(contentType.id(), supportingValues_1.get("contentTypeID"));
        assertEquals(uniqueField.variable(), supportingValues_1.get("fieldVariableName"));
        assertEquals(contentlets[0].get(uniqueField.variable()), supportingValues_1.get("fieldValue"));
        assertEquals(contentlets[0].getLanguageId(), Long.parseLong(supportingValues_1.get("languageId").toString()));
        assertEquals(contentlets[0].getHost(), supportingValues_1.get("hostId"));
        assertEquals(false, supportingValues_1.get("uniquePerSite"));
        assertEquals(contentlets.length, ((List) supportingValues_1.get("contentletsId")).size());
        assertEquals(Arrays.stream(contentlets).map(Contentlet::getIdentifier).sorted().collect(Collectors.toList()),
                ((List<String>) supportingValues_1.get("contentletsId")).stream().sorted().collect(Collectors.toList()));
    }

    private static String getHash(ContentType contentType, Field uniqueField, Contentlet contentlet_1) throws NoSuchAlgorithmException {
        final String valueToHash_1 = contentType.id() + uniqueField.variable() + contentlet_1.getLanguageId() +
                contentlet_1.get(uniqueField.variable());
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hashBytes = digest.digest(valueToHash_1.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    private static String getHashIncludeSiteId(ContentType contentType, Field uniqueField, Contentlet contentlet)
            throws NoSuchAlgorithmException {
        final String valueToHash_1 = contentType.id() + uniqueField.variable() + contentlet.getLanguageId() +
                contentlet.get(uniqueField.variable()) + contentlet.getHost();
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hashBytes = digest.digest(valueToHash_1.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#executeUpgrade()}
     * When: Run the method and already exists Contentlet with duplicated values for unique fields
     * Should: populate the table with these values and in the contentlets ids attribute insert an array with all the contentlets
     */
    @Test
    public void populateWhenExistsDuplicatedValues() throws DotDataException, NoSuchAlgorithmException, IOException, DotSecurityException {
        final Field titleField = new FieldDataGen().type(TextField.class).name("title").next();
        final Field uniqueField = new FieldDataGen().type(TextField.class).name("unique").next();

        final ContentType contentType = new ContentTypeDataGen().field(titleField).field(uniqueField).nextPersisted();
        final String uniqueValue = "Unique_" + System.currentTimeMillis();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_1_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), uniqueValue)
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_2_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), uniqueValue)
                .nextPersisted();

        final ImmutableTextField uniqueFieldUpdated = ImmutableTextField.builder()
                .from(uniqueField)
                .unique(true)
                .contentTypeId(contentType.id())
                .build();

        APILocator.getContentTypeFieldAPI().save(uniqueFieldUpdated, APILocator.systemUser());

        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());
        task241007CreateUniqueFieldsTable.executeUpgrade();
        assertFalse(task241007CreateUniqueFieldsTable.forceRun());

        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * from unique_fields").loadObjectResults();

        assertFalse(results.isEmpty());

        final String valueToHash_1 = getHash(contentType, uniqueField, contentlet_1);

        final List<Map<String, Object>> uniqueValuesResult = results.stream()
                .filter(result -> result.get("unique_key_val").equals(valueToHash_1))
                .collect(Collectors.toList());

        assertEquals(1, uniqueValuesResult.size());

        checkSupportingValues(uniqueValuesResult.get(0), contentType, uniqueField, contentlet_1, contentlet_2);
    }

    /**
     * Method to test: {@link Task241007CreateUniqueFieldsTable#executeUpgrade()}
     * When: Run the method and already exists Contentlet with Unique field and uniquePerSite enabled
     * Should: populate the table with these values and use the siteId to calculated the hash
     */
    @Test
    public void populateWithUniquePerSiteEnabled() throws DotDataException, NoSuchAlgorithmException, IOException {
        final Field titleField = new FieldDataGen().type(TextField.class).name("title").next();
        final Field uniqueField = new FieldDataGen().type(TextField.class).name("unique").unique(true).next();

        final ContentType contentType = new ContentTypeDataGen().field(titleField).field(uniqueField).nextPersisted();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
                .field(contentType.fields().stream()
                        .filter(field -> field.variable().equals(uniqueField.variable()))
                        .limit(1)
                        .findFirst()
                        .orElseThrow())
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_1_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), "Unique_1_" + System.currentTimeMillis())
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "Title_2_" + System.currentTimeMillis())
                .setProperty(uniqueField.variable(), "Unique_2_" + System.currentTimeMillis())
                .nextPersisted();

        final Task241007CreateUniqueFieldsTable task241007CreateUniqueFieldsTable = new Task241007CreateUniqueFieldsTable();

        assertTrue(task241007CreateUniqueFieldsTable.forceRun());
        task241007CreateUniqueFieldsTable.executeUpgrade();
        assertFalse(task241007CreateUniqueFieldsTable.forceRun());

        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * from unique_fields").loadObjectResults();

        assertFalse(results.isEmpty());

        final String valueToHash_1 = getHashIncludeSiteId(contentType, uniqueField, contentlet_1);
        final String valueToHash_2 = getHashIncludeSiteId(contentType, uniqueField, contentlet_2);

        final Map<String, Object> result_1 = results.stream()
                .filter(result -> result.get("unique_key_val").equals(valueToHash_1))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertException("contenlet_1 expected"));

        final Map<String, Object> result_2 = results.stream()
                .filter(result -> result.get("unique_key_val").equals(valueToHash_2))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new AssertException("contenlet_2 expected"));

        checkSupportingValues(result_1, contentType, uniqueField, contentlet_1);
        checkSupportingValues(result_2, contentType, uniqueField, contentlet_2);

    }

    @Test
    public void notExistField() {
        throw new AssertException("Not implemented");
    }

}
