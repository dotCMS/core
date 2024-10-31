package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static org.junit.Assert.*;

public class DBUniqueFieldValidationStrategyTest {

    static UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();

        //TODO: Remove this when the whole change is done
        try {
            new DotConnect().setSQL("CREATE TABLE IF NOT EXISTS unique_fields (" +
                    "unique_key_val VARCHAR(64) PRIMARY KEY," +
                    "supporting_values JSONB" +
                    " )").loadObjectResults();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
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
     * Method to test: {@link DBUniqueFieldValidationStrategy#validate(Contentlet, Field)}
     * When: Called the method with a field with uniquePerSite set to true
     * Should: Allow insert the same values in different Host
     */
    @Test
    public void insertWithUniquePerSiteSetToTrue() throws DotDataException, UniqueFieldValueDuplicatedException, DotSecurityException {
        final Field uniqueField = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(uniqueField).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Host site_2 = new SiteDataGen().nextPersisted();
        final String uniqueFieldVariable = uniqueField.variable();
        final String uniqueValue = "UniqueValue" + System.currentTimeMillis();

        new FieldVariableDataGen()
                .key(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .value("true")
                .field(contentType.fields().stream()
                        .filter(field -> field.variable().equals(uniqueFieldVariable))
                        .limit(1)
                        .findFirst()
                        .orElseThrow())
                .nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniqueField)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site)
                .languageId(language.getId())
                .next();

        final DBUniqueFieldValidationStrategy extraTableUniqueFieldValidationStrategy =
                new DBUniqueFieldValidationStrategy(uniqueFieldDataBaseUtil);
        extraTableUniqueFieldValidationStrategy.validate(contentlet_1, uniqueField);

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniqueField)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site_2)
                .setVariantName(VariantAPI.DEFAULT_VARIANT.name())
                .build();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site_2)
                .languageId(language.getId())
                .next();

        extraTableUniqueFieldValidationStrategy.validate(contentlet_2, uniqueField);

        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeID' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals(2, results.size());

        final UniqueFieldCriteria[] uniqueFieldCriterias = new UniqueFieldCriteria[]{uniqueFieldCriteria_1, uniqueFieldCriteria_2};
        final Contentlet[] contentlets = new Contentlet[]{contentlet_1, contentlet_2};
        final Host[] sites = new Host[]{site, site_2};

        for (int i =0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            final Map<String, Object> mapExpected = new HashMap<>(uniqueFieldCriterias[i].toMap());
            mapExpected.put("contentletsId", list(contentlets[i].getIdentifier()));
            mapExpected.put("uniquePerSite", true);

            final String valueToHash = contentType.id() + uniqueField.variable() + language.getId() + uniqueValue +
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
            assertEquals("The Field " + notUniqueField.variable() + " is not unique", e.getMessage());
        }
    }

    private static void validateAfterInsert(UniqueFieldCriteria uniqueFieldCriteria,
                                            Contentlet... contentlets) throws DotDataException {

        final ContentType contentType = uniqueFieldCriteria.contentType();
        final Field field =uniqueFieldCriteria.field();
        final Language language = uniqueFieldCriteria.language();
        final Object value = uniqueFieldCriteria.value();

        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeID' = ? AND " +
                        "supporting_values->>'fieldVariableName' = ? AND supporting_values->>'fieldValue' = ? AND " +
                        "(supporting_values->>'languageId')::numeric = ?")
                .addParam(contentType.id())
                .addParam(field.variable())
                .addParam(value)
                .addParam(language.getId())
                .loadObjectResults();

        assertEquals(contentlets.length, results.size());

        for (Contentlet contentlet : contentlets) {
            final Map<String, Object> mapExpected = new HashMap<>(uniqueFieldCriteria.toMap());

            mapExpected.put("contentletsId", list(contentlet.getIdentifier()));
            mapExpected.put("uniquePerSite", false);

            final String valueToHash = contentType.id() + field.variable() + language.getId() + value;
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
                                        final Collection compareWith) throws DotDataException, IOException {
        final List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE unique_key_val = ?")
                .addParam(uniqueFieldCriteria.hash())
                .loadObjectResults();

        assertEquals(1, results.size());

        final Map<String, Object> supportingValues = JsonUtil.getJsonFromString(
                results.get(0).get("supporting_values").toString());

        assertEquals(compareWith, supportingValues.get("contentletsId"));
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

        List<Map<String, Object>> results = new DotConnect().setSQL("SELECT * FROM unique_fields WHERE supporting_values->>'contentTypeID' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals(1, results.size());
    }
}
