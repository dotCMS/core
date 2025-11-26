package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldVariableDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.JsonUtil;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.StringPool;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.SITE_ID_ATTR;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This Integration Test verifies that the {@link DBUniqueFieldValidationStrategy} class works as
 * expected.
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class DBUniqueFieldValidationStrategyTest {

    static UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method with the right parameters
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insert() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Object value =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        validateAfterInsert(uniqueFieldCriteria, contentlet);
    }

    /**
     * Method to test:  {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method with a 'unique_key_val' duplicated
     * Should: Throw a {@link UniqueFieldValueDuplicatedException}
     */
    @Test
    public void tryToInsertDuplicated() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field uniqueField = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(uniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniqueField.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, uniqueField);

        final String hash = StringUtils.hashText(contentType.id() + uniqueField.variable() + language.getId() + value);

        final int countBefore = Integer.parseInt(new DotConnect()
                .setSQL("SELECT COUNT(*) as count FROM unique_fields WHERE unique_key_val = ?")
                .addParam(hash).loadObjectResults().get(0).get("count").toString());
        try {

            extraTableUniqueFieldValidationStrategy.validate(contentlet, uniqueField);
            throw new AssertionError("UniqueFieldValueDupliacatedException expected");
        } catch (UniqueFieldValueDuplicatedException e) {

            final int countAfter = Integer.parseInt(new DotConnect()
                    .setSQL("SELECT COUNT(*) as count FROM unique_fields WHERE unique_key_val = ?")
                    .addParam(hash).loadObjectResults().get(0).get("count").toString());

            assertEquals(countBefore, countAfter);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b>
     *     {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}</li>
     *     <li><b>Given Scenario: </b>Create two Contentlets with the same unique value, but each
     *     living under a different Site, and the {@code uniquePerSite} is set to {@code true}
     *     .</li>
     *     <li><b>Expected Result: </b>Even though both contents have the same unique value, they
     *     live in different Sites. So, the creation must be successful.</li>
     * </ul>
     */
    @Test
    public void insertWithUniquePerSiteSetToTrue() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final String uniqueValue = "UniqueValue" + System.currentTimeMillis();
        final DBUniqueFieldValidationStrategy databaseUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        Field uniqueField = new FieldDataGen()
                .type(TextField.class)
                .contentTypeId(contentType.id())
                .unique(true)
                .nextPersisted();
        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value(Boolean.toString(true))
                .field(uniqueField)
                .nextPersisted();
        // Force retrieving the field variables as they're lazily loaded
        uniqueField.fieldVariables();

        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Host site_2 = new SiteDataGen().nextPersisted();
        final String uniqueFieldVariable = uniqueField.variable();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site)
                .languageId(language.getId())
                .next();

        databaseUniqueFieldValidationStrategy.validate(contentlet_1, uniqueField);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site_2)
                .languageId(language.getId())
                .next();

        databaseUniqueFieldValidationStrategy.validate(contentlet_2, uniqueField);

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?")
                .addParam(contentType.id())
                .loadObjectResults();
        assertEquals(String.format("There must be two Unique Field entries related to the test Content Type " +
                "'%s'", contentType.variable()), 2, results.size());

        final Host[] sites = new Host[]{site, site_2};
        for (int i =0; i < results.size(); i++) {
            final Map<String, Object> result = results.get(i);
            // Unique values are case-insensitive, so we convert it to lower case
            final String valueToHash = contentType.id() + uniqueField.variable() + language.getId() + uniqueValue.toLowerCase() +
                    sites[i].getIdentifier();
            assertEquals(StringUtils.hashText(valueToHash), result.get("unique_key_val"));
        }
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method with a Not Unique Field
     * Should: thrown an {@link IllegalArgumentException}
     */
    @Test
    public void insertNotUniqueField() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field notUniqueField = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen().field(notUniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(notUniqueField.variable(), value)
                .languageId(language.getId())
                .host(site)
                .next();

        try {

            final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                    new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
            extraTableUniqueFieldValidationStrategy.validate(contentlet, notUniqueField);
            throw new AssertionError("IllegalArgumentException Expected");
        } catch (IllegalArgumentException e) {
            //expected
            assertEquals("Field '" + notUniqueField.variable() + "' is not marked as 'unique'", e.getMessage());
        }
    }

    private static void validateAfterInsert(UniqueFieldCriteria uniqueFieldCriteria,
                                            Contentlet... contentlets) throws DotDataException {
        validateAfterInsert(uniqueFieldCriteria, false, contentlets);
    }

    private static void validateAfterInsert(final UniqueFieldCriteria uniqueFieldCriteria, final boolean uniquePerSite,
                                            final Contentlet... contentlets) throws DotDataException {

        final ContentType contentType = uniqueFieldCriteria.contentType();
        final Field field =uniqueFieldCriteria.field();
        final Language language = uniqueFieldCriteria.language();
        final Object value = uniqueFieldCriteria.value();

        final String sql = "SELECT * FROM unique_fields WHERE " +
                "supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ? AND " +
                "supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ? AND " +
                "supporting_values->>'" + FIELD_VALUE_ATTR + "' = ? AND " +
                "(supporting_values->>'" + LANGUAGE_ID_ATTR + "')::numeric = ?" +
                (uniquePerSite ? " AND supporting_values->>'" + SITE_ID_ATTR + "' = ?" : "");
        final DotConnect dc = new DotConnect()
                .setSQL(sql)
                .addParam(contentType.id())
                .addParam(field.variable())
                .addParam(value)
                .addParam(language.getId());
        if (uniquePerSite) {
            dc.addParam(contentlets[0].getHost());
        }
        final List<Map<String, Object>> results = dc.loadObjectResults();

        assertEquals("", contentlets.length, results.size());

        for (Contentlet contentlet : contentlets) {
            final String valueToHash = contentType.id() + field.variable() + language.getId() + value
                    + (uniquePerSite ? contentlet.getHost() : "");
            assertEquals(StringUtils.hashText(valueToHash), results.get(0).get("unique_key_val"));
        }
    }

    /**
     * Method to test:  {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method twice with different Content Type
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentContentType() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field field_1 = new FieldDataGen().type(TextField.class).velocityVarName("unique").unique(true).next();
        final ContentType contentType_1 = new ContentTypeDataGen().field(field_1).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType_1)
                .setField(field_1)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType_1)
                .setProperty(field_1.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet_1, field_1);

        validateAfterInsert(uniqueFieldCriteria_1, contentlet_1);

        final Field field_2 = new FieldDataGen().type(TextField.class).velocityVarName("unique").unique(true).next();
        final ContentType contentType_2 = new ContentTypeDataGen().field(field_2).nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType_2)
                .setProperty(field_1.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType_2)
                .setField(field_2)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        extraTableUniqueFieldValidationStrategy.validate(contentlet_2, field_1);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet_2);
    }

    /**
     * Method to test:  {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method twice with different Field
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentField() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field field_1 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final Field field_2 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field2" + System.currentTimeMillis()).next();
        final ContentType contentType = new ContentTypeDataGen().field(field_1).field(field_2).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String uniqueValue = "UniqueValue" + System.currentTimeMillis();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field_1.variable(), uniqueValue)
                .setProperty(field_2.variable(), uniqueValue)
                .languageId(language.getId())
                .host(site)
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field_1)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field_1);

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field_2)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        extraTableUniqueFieldValidationStrategy.validate(contentlet, field_2);

        validateAfterInsert(uniqueFieldCriteria_1, contentlet);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet);
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method twice with different Value
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentValue() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String uniqueValue_1 = "UniqueValue1" + System.currentTimeMillis();
        final String uniqueValue_2 = "UniqueValue2" + System.currentTimeMillis();

        final String id = UUIDGenerator.generateUuid();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue_1)
                .setProperty(IDENTIFIER_KEY, id)
                .host(site)
                .languageId(language.getId())
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_1)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_2)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue_2)
                .setProperty(IDENTIFIER_KEY, id)
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.validate(contentlet_2, field);

        validateDoesNotExists(uniqueFieldCriteria_1);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet_2);
    }

    private static void validateDoesNotExists(final UniqueFieldCriteria uniqueFieldCriteria_1) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeID' = ? AND " +
                        "supporting_values->>'fieldVariableName' = ? AND supporting_values->>'fieldValue' = ? AND " +
                        "(supporting_values->>'languageId')::numeric = ?")
                .addParam(uniqueFieldCriteria_1.contentType().id())
                .addParam(uniqueFieldCriteria_1.field().variable())
                .addParam(uniqueFieldCriteria_1.value())
                .addParam(uniqueFieldCriteria_1.language().getId())
                .loadObjectResults();

        assertTrue(results.isEmpty());
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method twice with different Language
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentLanguage() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Language otherLanguage = new LanguageDataGen().nextPersisted();

        final String uniqueValue = "UniqueValue1" + System.currentTimeMillis();
        final String id = UUIDGenerator.generateUuid();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .setProperty(IDENTIFIER_KEY, id)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);


        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue)
                .setLanguage(otherLanguage)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .setProperty(IDENTIFIER_KEY, id)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(otherLanguage.getId())
                .next();


        extraTableUniqueFieldValidationStrategy.validate(contentlet_2, field);

        validateDoesNotExists(uniqueFieldCriteria_1);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet_2);
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Pretend that we are calling the afterSaved method after saved a new Contentlet
     * Should: Update the unique_fields register created before to add the Id in the contentlet list
     */
    @Test
    public void afterSaved() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException, IOException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Object value =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria, list(StringPool.BLANK));

        final Contentlet contentletSaved = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .setProperty(IDENTIFIER_KEY, UUIDGenerator.generateUuid())
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.afterSaved(contentletSaved, true);

        checkContentIds(uniqueFieldCriteria, list(contentletSaved.getIdentifier()));
    }

    private static void checkContentIds(final UniqueFieldCriteria uniqueFieldCriteria,
                                        final Collection<String> compareWith) throws DotDataException, IOException {
        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE unique_key_val = encode(sha256(?::bytea), 'hex') ")
                .addParam(uniqueFieldCriteria.criteria())
                .loadObjectResults();

        assertEquals(1, results.size());

        final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(
                results.get(0).get("supporting_values").toString());

        assertEquals(compareWith, supportingValues.get(UniqueFieldCriteria.CONTENTLET_IDS_ATTR));
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#afterSaved(Contentlet, boolean)}
     * When: Pretend that we are calling the afterSaved method after updated Contentlet
     * Should: Update the unique_fields register created before to add the Id in the contentlet list
     */
    @Test
    public void afterUpdated() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException, IOException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Object value =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String contentletId = UUIDGenerator.generateUuid();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria, list(contentlet.getIdentifier()));

        final Contentlet contentletSaved = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.afterSaved(contentletSaved, false);

        checkContentIds(uniqueFieldCriteria, list(contentletSaved.getIdentifier()));
    }


    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#afterSaved(Contentlet, boolean)}
     * When: Pretend that we are calling the afterSaved method after saved a new Contentlet with 2 unique fields
     * Should: Update the unique_fields register created before to add the Id in the contentlet list
     */
    @Test
    public void savingWithContentTypeWithMoreThanOneUniqueField()
            throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException, IOException {
        final Field uniquefield_1 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final Field uniquefield_2 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field2" + System.currentTimeMillis()).next();

        final ContentType contentType = new ContentTypeDataGen().fields(list(uniquefield_1, uniquefield_2)).nextPersisted();
        final Object value_1 =  new RandomString().nextString();
        final Object value_2 =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniquefield_1.variable(), value_1)
                .setProperty(uniquefield_2.variable(), value_2)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, uniquefield_1);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, uniquefield_2);

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniquefield_1)
                .setValue(value_1)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_1, list(StringPool.BLANK));

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniquefield_2)
                .setValue(value_2)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_2, list(StringPool.BLANK));

        final Contentlet contentletSaved = new ContentletDataGen(contentType)
                .setProperty(uniquefield_1.variable(), value_1)
                .setProperty(uniquefield_2.variable(), value_2)
                .setProperty(IDENTIFIER_KEY, UUIDGenerator.generateUuid())
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.afterSaved(contentletSaved, true);

        checkContentIds(uniqueFieldCriteria_1, list(contentletSaved.getIdentifier()));
        checkContentIds(uniqueFieldCriteria_2, list(contentletSaved.getIdentifier()));
    }


    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#afterSaved(Contentlet, boolean)}
     * When: Pretend that we are calling the afterSaved method after update a Contentlet with 2 unique fields
     * Should: Update the unique_fields register created before to add the Id in the contentlet list
     */
    @Test
    public void updatingWithContentTypeWithMoreThanOneUniqueField()
            throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException, IOException {
        final Field uniquefield_1 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final Field uniquefield_2 = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field2" + System.currentTimeMillis()).next();

        final ContentType contentType = new ContentTypeDataGen().fields(list(uniquefield_1, uniquefield_2)).nextPersisted();
        final Object value_1 =  new RandomString().nextString();
        final Object value_2 =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String contentletId = UUIDGenerator.generateUuid();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniquefield_1.variable(), value_1)
                .setProperty(uniquefield_2.variable(), value_2)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, uniquefield_1);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, uniquefield_2);

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniquefield_1)
                .setValue(value_1)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_1, list(contentlet.getIdentifier()));

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniquefield_2)
                .setValue(value_2)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_2, list(contentlet.getIdentifier()));

        final Contentlet contentletSaved = new ContentletDataGen(contentType)
                .setProperty(uniquefield_1.variable(), value_1)
                .setProperty(uniquefield_2.variable(), value_2)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.afterSaved(contentletSaved, false);

        checkContentIds(uniqueFieldCriteria_1, list(contentletSaved.getIdentifier()));
        checkContentIds(uniqueFieldCriteria_2, list(contentletSaved.getIdentifier()));
    }

    /**
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Pretend that we are calling the validate method after updated Contentlet, and the unique value is the changed
     * Should: Update the unique_fields register
     */
    @Test
    public void validateUpdating() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException, IOException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Object value_1 =  new RandomString().nextString();
        final Object value_2 =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String contentletId = UUIDGenerator.generateUuid();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value_1)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet, field);

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value_1)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_1, list(contentlet.getIdentifier()));

        final Contentlet contentletSaved = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value_2)
                .setProperty(IDENTIFIER_KEY, contentletId)
                .setProperty(INODE_KEY, UUIDGenerator.generateUuid())
                .host(site)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.validate(contentletSaved, field);

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value_2)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        checkContentIds(uniqueFieldCriteria_2, list(contentletSaved.getIdentifier()));

        List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'"
                        + CONTENT_TYPE_ID_ATTR + "' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals("One Unique Field record matching the specified Content Type should've been returned",
                1, results.size());
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}</li>
     *     <li><b>Given Scenario: </b>Create two Contentlets with the same unique value, but each
     *     living under a different Site, and the {@code uniquePerSite} is set to {@code true}.</li>
     *     <li><b>Expected Result: </b>Even though they share the exact same unique value, the
     *     creation must be successful as they both live in different Sites.</li>
     * </ul>
     */
    @Test
    public void insertWithUniquePerSiteAsTrue() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final boolean uniquePerSite = true;

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Field field = new FieldDataGen().type(TextField.class).contentTypeId(contentType.id()).unique(true).nextPersisted();
        new FieldVariableDataGen().key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME).value(Boolean.toString(uniquePerSite)).field(field).nextPersisted();
        final Object uniqueValue = new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Host secondSite = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy dbValidationStrategy = new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        dbValidationStrategy.validate(contentlet, field);

        UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        validateAfterInsert(uniqueFieldCriteria, uniquePerSite, contentlet);

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Contentlet contentletInOtherSite = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .host(secondSite)
                .languageId(language.getId())
                .next();

        dbValidationStrategy.validate(contentletInOtherSite, field);

        uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(secondSite)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        validateAfterInsert(uniqueFieldCriteria, true, contentletInOtherSite);
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}</li>
     *     <li><b>Given Scenario: </b>Create two Contentlets with the same unique value, but each
     *     living under a different Site, and the {@code uniquePerSite} is set to {@code false}
     *     .</li>
     *     <li><b>Expected Result: </b>Even though both contents live in different Sites, the
     *     creation must FAIL as they have the exact same unique value.</li>
     * </ul>
     */
    @Test
    public void insertWithUniquePerSiteAsFalse() throws DotDataException, DotSecurityException, UniqueFieldValueDuplicatedException {
        // ╔══════════════════╗
        // ║  Initialization  ║
        // ╚══════════════════╝
        final boolean uniquePerSite = false;

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Field field = new FieldDataGen().type(TextField.class).contentTypeId(contentType.id()).unique(true).nextPersisted();
        new FieldVariableDataGen().key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME).value(Boolean.toString(uniquePerSite)).field(field).nextPersisted();
        final Object uniqueValue = new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Host secondSite = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy dbValidationStrategy = new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        dbValidationStrategy.validate(contentlet, field);

        UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        validateAfterInsert(uniqueFieldCriteria, uniquePerSite, contentlet);

        // ╔════════════════════════╗
        // ║  Generating Test data  ║
        // ╚════════════════════════╝
        final Contentlet contentletInOtherSite = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue)
                .host(secondSite)
                .languageId(language.getId())
                .next();

        // ╔══════════════╗
        // ║  Assertions  ║
        // ╚══════════════╝
        try {
            dbValidationStrategy.validate(contentletInOtherSite, field);
        } catch (final UniqueFieldValueDuplicatedException e) {
            assertTrue("The error message is NOT the expected one", e.getMessage()
                    .equalsIgnoreCase("The unique value '" + uniqueValue + "' for the field '"
                            + field.variable() +"' in the Content Type '" + contentType.variable()
                            + "' already exists"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:
     *     </b>{@link DBUniqueFieldValidationStrategy#validateInPreview(Contentlet, Field)}</li>
     *     <li><b>Given Scenario: </b>Creating a Contentlet with a unique field, and try to
     *     create a second one with the same unique field value. The second contentlet is in
     *     preview mode, which means it HAS NOT been created yet.</li>
     *     <li><b>Expected Result: </b>Throw a {@link UniqueFieldValueDuplicatedException}
     *     indicating that there is already another Contentlet with the same unique value.</li>
     * </ul>
     */
    @Test
    public void tryToInsertDuplicatedPreview() throws DotDataException, DotSecurityException {
        final Field uniqueField = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(uniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        // First contentlet with a valid unique field value
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniqueField.variable(), value)
                .host(site)
                .languageId(language.getId())
                .nextPersisted();
        ContentletDataGen.publish(contentlet);

        // Second contentlet with the same unique field value. But NOT persisted to the DB
        final Contentlet invalidContentlet = new ContentletDataGen(contentType)
                .setProperty(uniqueField.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        try {
            extraTableUniqueFieldValidationStrategy.validateInPreview(invalidContentlet, uniqueField);
            throw new AssertionError("UniqueFieldValueDuplicatedException expected");
        } catch (final UniqueFieldValueDuplicatedException e) {
            final int countAfter = Integer.parseInt(new DotConnect()
                    .setSQL("SELECT COUNT(*) as count " +
                            "FROM unique_fields " +
                            "WHERE supporting_values->>'contentTypeId' = ?")
                    .addParam(contentType.id())
                    .loadObjectResults()
                    .get(0).get("count").toString());
            assertEquals("There must be only 1 record in the unique fields table", 1, countAfter);
        }
    }

    /**
     * Method to test:  {@link DBUniqueFieldValidationStrategy#validateInPreview(Contentlet, Field)}
     * When: Called the method with a 'unique_key_val' not duplicated
     * Should: insert nothing in the unique_fields table
     */
    @Test
    public void validatePreview() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field uniqueField = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(uniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniqueField.variable(), value)
                .host(site)
                .languageId(language.getId())
                .next();


        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validateInPreview(contentlet, uniqueField);

        final int countAfter = Integer.parseInt(new DotConnect()
                .setSQL("SELECT COUNT(*) as count " +
                        "FROM unique_fields " +
                        "WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id())
                .loadObjectResults()
                .get(0).get("count").toString());

        assertEquals(0, countAfter);
    }
}
