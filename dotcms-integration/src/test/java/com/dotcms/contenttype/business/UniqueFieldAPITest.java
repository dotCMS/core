package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.StringUtils;
import net.bytebuddy.utility.RandomString;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UniqueFieldAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

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
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method with the right parameters
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insert() throws DotDataException, UniqueFieldValueDupliacatedException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Object value =  new RandomString().nextString();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .build();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), value)
                .host(site)
                .next();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria, contentlet.getIdentifier());

        validateAfterInsert(uniqueFieldCriteria, contentlet);
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method with a 'unique_key_val' duplicated
     * Should: Throw a {@link UniqueFieldValueDupliacatedException}
     */
    @Test
    public void tryToInsertDuplicated() throws DotDataException, UniqueFieldValueDupliacatedException {
        final Field uniqueField = new FieldDataGen().type(TextField.class).unique(true).next();
        final ContentType contentType = new ContentTypeDataGen().field(uniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniqueField)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .build();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(uniqueField.variable(), value)
                .host(site)
                .next();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria, contentlet.getIdentifier());

        final String hash = StringUtils.hashText(contentType.id() + uniqueField.variable() + language.getId() + value);

        final int countBefore = Integer.parseInt(new DotConnect()
                .setSQL("SELECT COUNT(*) as count FROM unique_fields WHERE unique_key_val = ?")
                .addParam(hash).loadObjectResults().get(0).get("count").toString());
        try {
            uniqueFieldAPIHelper.insert(uniqueFieldCriteria, contentlet.getIdentifier());
            throw new AssertionError("UniqueFieldValueDupliacatedException expected");
        } catch (UniqueFieldValueDupliacatedException e) {

            final int countAfter = Integer.parseInt(new DotConnect()
                    .setSQL("SELECT COUNT(*) as count FROM unique_fields WHERE unique_key_val = ?")
                    .addParam(hash).loadObjectResults().get(0).get("count").toString());

            assertEquals(countBefore, countAfter);
        }
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method with a field with uniquePerSite set to true
     * Should: Allow insert the same values in different Host
     */
    @Test
    public void insertWithUniquePerSiteSetToTrue() throws DotDataException, UniqueFieldValueDupliacatedException {
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
                .build();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site)
                .next();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_1, contentlet_1.getIdentifier());

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(uniqueField)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site_2)
                .build();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(uniqueFieldVariable, uniqueValue)
                .host(site_2)
                .next();

        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_2, contentlet_2.getIdentifier());

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
            mapExpected.put("contentletsId", CollectionsUtils.list(contentlets[i].getIdentifier()));
            mapExpected.put("uniquePerSite", true);

            final String valueToHash = contentType.id() + uniqueField.variable() + language.getId() + uniqueValue +
                    sites[i].getIdentifier();
            assertEquals(StringUtils.hashText(valueToHash), result.get("unique_key_val"));
        }
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method with a Not Unique Field
     * Should: thrown an {@link IllegalArgumentException}
     */
    @Test
    public void insertNotUniqueField() throws DotDataException, UniqueFieldValueDupliacatedException {
        final Field notUniqueField = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen().field(notUniqueField).nextPersisted();
        final Object value =  "UniqueValue" + System.currentTimeMillis();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(notUniqueField)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .build();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(notUniqueField.variable(), value)
                .host(site)
                .next();

        try {
            final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
            uniqueFieldAPIHelper.insert(uniqueFieldCriteria, contentlet.getIdentifier());
            throw new AssertionError("IllegalArgumentExceptionΩ Expected");
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
                        "supporting_values->>'fieldVariableName' = ? AND supporting_values->>'fieldValue' = ?")
                .addParam(contentType.id())
                .addParam(field.variable())
                .addParam(value)
                .loadObjectResults();

        assertEquals(contentlets.length, results.size());

        for (Contentlet contentlet : contentlets) {
            final Map<String, Object> mapExpected = new HashMap<>(uniqueFieldCriteria.toMap());
            mapExpected.put("contentletsId", CollectionsUtils.list(contentlet.getIdentifier()));
            mapExpected.put("uniquePerSite", false);

            final String valueToHash = contentType.id() + field.variable() + language.getId() + value;
            assertEquals(StringUtils.hashText(valueToHash), results.get(0).get("unique_key_val"));
        }
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method twice with different Content Type
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentContentType() throws DotDataException, UniqueFieldValueDupliacatedException {
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
                .build();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType_1)
                .setProperty(field_1.variable(), value)
                .host(site)
                .next();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_1, contentlet_1.getIdentifier());
        validateAfterInsert(uniqueFieldCriteria_1, contentlet_1);

        final Field field_2 = new FieldDataGen().type(TextField.class).velocityVarName("unique").unique(true).next();
        final ContentType contentType_2 = new ContentTypeDataGen().field(field_2).nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType_2)
                .setProperty(field_1.variable(), "UniqueValue" + System.currentTimeMillis())
                .host(site)
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType_2)
                .setField(field_2)
                .setValue(value)
                .setLanguage(language)
                .setSite(site)
                .build();

        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_2, contentlet_2.getIdentifier());
        validateAfterInsert(uniqueFieldCriteria_2, contentlet_2);
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method twice with different Field
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentField() throws DotDataException, UniqueFieldValueDupliacatedException {
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
                .host(site)
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field_1)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .build();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_1, contentlet.getIdentifier());

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field_2)
                .setValue(uniqueValue)
                .setLanguage(language)
                .setSite(site)
                .build();

        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_2, contentlet.getIdentifier());

        validateAfterInsert(uniqueFieldCriteria_1, contentlet);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet);
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method twice with different Value
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentValue() throws DotDataException, UniqueFieldValueDupliacatedException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();

        final String uniqueValue_1 = "UniqueValue1" + System.currentTimeMillis();
        final String uniqueValue_2 = "UniqueValue2" + System.currentTimeMillis();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue_1)
                .host(site)
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_1)
                .setLanguage(language)
                .setSite(site)
                .build();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_1, contentlet.getIdentifier());

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_2)
                .setLanguage(language)
                .setSite(site)
                .build();

        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_2, contentlet.getIdentifier());

        validateAfterInsert(uniqueFieldCriteria_1, contentlet);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet);
    }

    /**
     * Method to test: {@link UniqueFieldAPIImpl#insert(UniqueFieldCriteria, String)}
     * When: Called the method twice with different Language
     * Should: Insert a register in the unique_fields table
     */
    @Test
    public void insertWithDifferentLanguage() throws DotDataException, UniqueFieldValueDupliacatedException {
        final Field field = new FieldDataGen().type(TextField.class).unique(true)
                .velocityVarName("field1" + System.currentTimeMillis()).next();
        final ContentType contentType = new ContentTypeDataGen().field(field).nextPersisted();
        final Language language = new LanguageDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        final Language otherLanguage = new LanguageDataGen().nextPersisted();

        final String uniqueValue_1 = "UniqueValue1" + System.currentTimeMillis();
        final String uniqueValue_2 = "UniqueValue2" + System.currentTimeMillis();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(field.variable(), uniqueValue_1)
                .host(site)
                .next();

        final UniqueFieldCriteria uniqueFieldCriteria_1 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_1)
                .setLanguage(language)
                .setSite(site)
                .build();

        final UniqueFieldAPIImpl uniqueFieldAPIHelper = new UniqueFieldAPIImpl();
        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_1, contentlet.getIdentifier());

        final UniqueFieldCriteria uniqueFieldCriteria_2 = new UniqueFieldCriteria.Builder()
                .setContentType(contentType)
                .setField(field)
                .setValue(uniqueValue_2)
                .setLanguage(otherLanguage)
                .setSite(site)
                .build();

        uniqueFieldAPIHelper.insert(uniqueFieldCriteria_2, contentlet.getIdentifier());

        validateAfterInsert(uniqueFieldCriteria_1, contentlet);
        validateAfterInsert(uniqueFieldCriteria_2, contentlet);
    }
}
