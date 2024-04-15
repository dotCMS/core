package com.dotcms.variant;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PersonalizedContentlet;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class VariantAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a {@link Variant} object
     * Should: Save it in Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void save() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final Variant variantSaved = APILocator.getVariantAPI().save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.name());

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantSaved.name(), variantFromDataBase.name());
        assertEquals(variantSaved.name(), variantFromDataBase.name());
        assertFalse(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a archived {@link Variant} object without name
     * Should: throw {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = IllegalArgumentException.class)
    public void saveArchive() throws DotDataException {
        final Variant variant = new VariantDataGen().archived(true).next();
        APILocator.getVariantAPI().save(variant);
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update a {@link Variant} object
     * Should: Update it in Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void update() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        assertNotNull(variant);
        assertNotNull(variant.name());

        final Variant variantUpdated = Variant.builder()
                .name(variant.name())
                .description(Optional.of(variant.description().get() + "_updated"))
                .archived(variant.archived())
                .build();

        APILocator.getVariantAPI().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variant);

        assertEquals(variantUpdated.name(), variantFromDataBase.name());
        assertEquals(variantUpdated.description(), variantFromDataBase.description());
        assertFalse(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update a {@link Variant} object that not exists
     * Should: thorw a {@link DoesNotExistException}
     *
     * @throws DotDataException
     */
    @Test(expected = DoesNotExistException.class)
    public void updateNotExists() throws DotDataException {
        final Variant variantToUpdated = new VariantDataGen()
                .name("Not_Exists").next();

        APILocator.getVariantAPI().update(variantToUpdated);
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update the {@link Variant}'s archived attribute
     * Should: Update it in Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void updateArchivedField() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        assertNotNull(variant);
        assertNotNull(variant.name());
        assertFalse(variant.archived());

        final Variant variantUpdated = new VariantDataGen()
                .name(variant.name())
                .name(variant.name())
                .archived(true)
                .next();

        APILocator.getVariantAPI().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variant);

        assertEquals(variantUpdated.name(), variantFromDataBase.name());
        assertEquals(variantUpdated.name(), variantFromDataBase.name());
        assertTrue(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to archive a {@link Variant} object
     * Should: save it with archived equals to true
     *
     * @throws DotDataException
     */
    @Test
    public void archive() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        APILocator.getVariantAPI().archive(variant.name());

        final Variant variantFromDataBase = getVariantFromDataBase(variant);
        assertEquals(variantFromDataBase.name(), variantFromDataBase.name());
        assertEquals(variantFromDataBase.name(), variantFromDataBase.name());
        assertTrue(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to archive a {@link Variant} object that not exists
     * Should: throw {@link com.dotmarketing.exception.DoesNotExistException}
     *
     * @throws DotDataException
     */
    @Test(expected = DoesNotExistException.class)
    public void archiveNotExists() throws DotDataException {
        APILocator.getVariantAPI().archive("Not Exists");
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to delete a archived {@link Variant} object
     * Should: remove it from Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void delete() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().archived(true).nextPersisted();

        final Field textField = new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(textField)
                .nextPersisted();

        final Contentlet defaultContentlet = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "LIVE DEFAULT")
                .nextPersisted();

        ContentletDataGen.publish(defaultContentlet);

        ContentletDataGen.update(defaultContentlet, Map.of(textField.variable(), "WORKING"));
        final Contentlet defaultContentletWorking = APILocator.getContentletAPI().findContentletByIdentifier(
                defaultContentlet.getIdentifier(), false, defaultContentlet.getLanguageId(),
                VariantAPI.DEFAULT_VARIANT.name(), APILocator.getUserAPI().getSystemUser(), false);


        assertFalse(defaultContentletWorking.getInode().equals(defaultContentlet.getInode()));

        final Contentlet variantContentlet = ContentletDataGen.createNewVersion(defaultContentlet, variant,
                Map.of(textField.variable(), "VARIANT"));

        ContentletDataGen.publish(variantContentlet);
        ContentletDataGen.update(variantContentlet, Map.of(textField.variable(), "WORKING"));


        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(defaultContentlet.getIdentifier());
        final List<Contentlet> allVersionsBeforeDeleted = APILocator.getContentletAPI()
                .findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(),
                        false);

        assertEquals(4, allVersionsBeforeDeleted.size());

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        APILocator.getVariantAPI().delete(variant.name());

        results = getResults(variant);
        assertTrue(results.isEmpty());

        final List<String> inodes  = APILocator.getContentletAPI()
                .findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false)
                .stream()
                .map(contentlet -> contentlet.getInode())
                .collect(Collectors.toList());

        assertEquals(2, inodes.size());

        assertTrue(inodes.contains(defaultContentlet.getInode()));
        assertTrue(inodes.contains(defaultContentletWorking.getInode()));
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to delete a not exists {@link Variant} object
     * Should: throw a {@link DoesNotExistException}
     *
     * @throws DotDataException
     */
    @Test(expected = DoesNotExistException.class)
    public void deleteNotExists() throws DotDataException {
        final Variant variant = new VariantDataGen().name("Not Exists").archived(true).next();

        APILocator.getVariantAPI().delete(variant.name());
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to delete a not archived {@link Variant} object
     * Should: throw a {@link DotStateException}
     *
     * @throws DotDataException
     */
    @Test(expected = DotStateException.class)
    public void deleteNotArchived() throws DotDataException {
        final Variant variant = new VariantDataGen().archived(false).nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        APILocator.getVariantAPI().delete(variant.name());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by id
     * Should: get it
     *
     * @throws DotDataException
     */
    @Test
    public void get() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        final Optional<Variant> variantFromDataBase = APILocator.getVariantAPI().get(variant.name());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.name(), variantFromDataBase.get().name());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by name
     * Should: get it
     *
     * @throws DotDataException
     */
    @Test
    public void getByName() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        final Optional<Variant> variantFromDataBase = APILocator.getVariantAPI().get(variant.name());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.name(), variantFromDataBase.get().name());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by id that not exists
     * Should: return a {@link Optional#empty()}
     *
     * @throws DotDataException
     */
    @Test
    public void getNotExists() throws DotDataException {

        final Optional<Variant> variantFromDataBase = APILocator.getVariantAPI()
                .get("Not_Exists");

        assertFalse(variantFromDataBase.isPresent());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by id equals to NULL
     * Should: throw a {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void getWithNull() throws DotDataException {
        APILocator.getVariantAPI().get(null);
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by id that not exists
     * Should: return a {@link Optional#empty()}
     *
     * @throws DotDataException
     */
    @Test
    public void getByNameNotExists() throws DotDataException {

        final Optional<Variant> variantFromDataBase = APILocator.getVariantAPI()
                .get("Not_Exists");

        assertFalse(variantFromDataBase.isPresent());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by id equals to NULL
     * Should: throw a {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void getByNameWithNull() throws DotDataException {
        APILocator.getVariantAPI().get(null);
    }

    private ArrayList getResults(Variant variant) throws DotDataException {
        return new DotConnect().setSQL(
                        "SELECT * FROM variant where name = ?")
                .addParam(variant.name())
                .loadResults();
    }

    private Variant getVariantFromDataBase(final Variant variant) throws DotDataException {
        final ArrayList results = getResults(variant);

        assertEquals(1, results.size());
        final Map resultMap = (Map) results.get(0);
        return Variant.builder()
                .description(Optional.ofNullable((String)resultMap.get("description")))
                .name(resultMap.get("name").toString())
                .archived(ConversionUtils.toBooleanFromDb(resultMap.get("archived")))
                .build();
    }

    /**
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdateDefaultVariant_shouldFail() throws DotDataException {
        final Variant defaultVariant = APILocator.getVariantAPI()
                .get(VariantAPI.DEFAULT_VARIANT.name()).orElseThrow(()->new DotStateException("Unable to find DEFAULT Variant"));
        final Variant alteredDefaultVariant = defaultVariant.withArchived(true)
                .withDescription(Optional.of("Let's alter the Default Variant description"));
        APILocator.getVariantAPI().update(alteredDefaultVariant);
    }

    /**
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDeleteDefaultVariant_shouldFail() throws DotDataException {
        APILocator.getVariantAPI().delete(VariantAPI.DEFAULT_VARIANT.name());
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}}
     * When:
     * - You create a {@link Variant}
     * - Create two {@link Contentlet} and create a version in the newly {@link Variant} also create
     * version to the DEFAULT Variant, not publish any of this versions.
     * - Promote the {@link Variant}
     *
     * Should:
     * - Copy the specific version of the {@link Contentlet} and turn it into the WORKING DEFAULT Variant
     */
    @Test
    public void promoteWorkingVersion() throws DotDataException, DotSecurityException {

        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "contentlet2")
                .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        ContentletDataGen.createNewVersion(contentlet1, variant, Map.of(
                titleField.variable(), "contentlet1_variant"
        ));
        ContentletDataGen.createNewVersion(contentlet2, variant, Map.of(
                titleField.variable(), "contentlet2_variant"
        ));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "contentlet1_variant",
                titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "contentlet2_variant",
                titleField);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create a {@link Variant}
     * - Create two {@link Contentlet} and create a version into the newly {@link Variant} also
     * create version to the DEFAULT Variant, Save and publish them.
     * - Make any change to the {@link Contentlet} and just save.
     * - Promote the {@link Variant}
     *
     * Should:
     * - Copy both version of the specific Variant  and turn it into the WORKING/LIVE DEFAULT Variant
     */
    @Test
    public void promoteWorkingLiveVersion() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final Contentlet contentlet1Variant = ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant"
                ));
        final Contentlet contentlet2Variant = ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant"
                ));

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));
        ContentletDataGen.update(contentlet1Variant, Map.of("title", "WORKING contentlet1_variant"));
        ContentletDataGen.update(contentlet2Variant, Map.of("title", "WORKING contentlet2_variant"));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant", titleField);
        checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant", titleField);


        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant",
                titleField);
        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant", titleField);
        checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant", titleField);
    }

    private static void checkVersion(final Contentlet contentlet, final boolean live,
            final Variant defaultVariant, final String value, final Field titleField)
            throws DotDataException, DotSecurityException {

        checkVersion(contentlet, live, defaultVariant, APILocator.getLanguageAPI().getDefaultLanguage(),
                value, titleField);
    }
    private static void checkVersion(Contentlet contentlet, boolean live, Variant defaultVariant,
            final Language language, final String  value, Field titleField)
            throws DotDataException, DotSecurityException {

        final Contentlet contentlet1DefaultVariantFromDataBase = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(),
                        live, language.getId(),
                        defaultVariant.name(), APILocator.systemUser(),
                        false);

        assertEquals(value, contentlet1DefaultVariantFromDataBase
                .getStringProperty(titleField.variable()));
    }

    private static void checkNull(final Contentlet contentlet, final boolean live, final Variant defaultVariant)
            throws DotDataException, DotSecurityException {
        checkNull(contentlet, live, defaultVariant, APILocator.getLanguageAPI().getDefaultLanguage());
    }

    private static void checkNull(final Contentlet contentlet, final boolean live,
            final Variant defaultVariant, final Language language) throws DotDataException, DotSecurityException {
        final Contentlet contentlet1DefaultVariantFromDataBase = APILocator.getContentletAPI()
                .findContentletByIdentifier(contentlet.getIdentifier(),
                        live, language.getId(),
                        defaultVariant.name(), APILocator.systemUser(),
                        false);

        assertNull(contentlet1DefaultVariantFromDataBase);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create a {@link Variant}
     * - Create two {@link Contentlet} and create versions of them into the newly {@link Variant}
     * also create version to the DEFAULT Variant.
     * - Publish the version for the newly created {@link Variant}.
     * - Promote the {@link Variant}
     *
     * Should:
     * - Copy both version of the specific Variant  and turn it into the WORKING/LIVE DEFAULT Variant
     */
    @Test
    public void promoteWithOutLiveDefaultVersion() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "WORKING contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "WORKING contentlet2")
                .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final Contentlet contentlet1Variant = ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant"
                ));
        final Contentlet contentlet2Variant = ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant"
                ));

        APILocator.getContentletAPI().publish(contentlet1Variant, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1Variant, Map.of("title", "WORKING contentlet1_variant"));
        ContentletDataGen.update(contentlet2Variant, Map.of("title", "WORKING contentlet2_variant"));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant", titleField);
        checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant", titleField);


        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant",
                titleField);
        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant", titleField);
        checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant", titleField);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create two {@link Variant}s
     * - Create two {@link Contentlet} and create version in both {@link Variant}s also create version to the DEFAULT Variant.
     * - Publish all of them.
     * - Promote one of the {@link Variant}'s
     *
     * Should:
     * - Copy both version of the specific Variant that was promoted and turn it into the WORKING/LIVE DEFAULT Variant
     */
    @Test
    public void promoteWithTwoVersion() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Variant variant_1 = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final Contentlet contentlet1Variant1 = ContentletDataGen.createNewVersion(contentlet1,
                variant_1, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant_1"
                ));
        final Contentlet contentlet2Variant1 = ContentletDataGen.createNewVersion(contentlet2,
                variant_1, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant_1"
                ));


        final Contentlet contentlet1Variant2 = ContentletDataGen.createNewVersion(contentlet1,
                variant_2, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant_2"
                ));
        final Contentlet contentlet2Variant2 = ContentletDataGen.createNewVersion(contentlet2,
                variant_2, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant_2"
                ));

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant1, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant2, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant2, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));
        ContentletDataGen.update(contentlet1Variant1, Map.of("title", "WORKING contentlet1_variant_1"));
        ContentletDataGen.update(contentlet2Variant1, Map.of("title", "WORKING contentlet2_variant_1"));
        ContentletDataGen.update(contentlet1Variant2, Map.of("title", "WORKING contentlet1_variant_2"));
        ContentletDataGen.update(contentlet2Variant2, Map.of("title", "WORKING contentlet2_variant_2"));

        APILocator.getVariantAPI().promote(variant_1, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant_1",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant_1",
                titleField);

        checkVersion(contentlet1, false, variant_1, "WORKING contentlet1_variant_1", titleField);
        checkVersion(contentlet1, true, variant_1, "LIVE contentlet1_variant_1", titleField);

        checkVersion(contentlet1, false, variant_2, "WORKING contentlet1_variant_2", titleField);
        checkVersion(contentlet1, true, variant_2, "LIVE contentlet1_variant_2", titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant_1",
                titleField);
        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant_1",
                titleField);

        checkVersion(contentlet2, true, variant_1, "LIVE contentlet2_variant_1", titleField);
        checkVersion(contentlet2, false, variant_1, "WORKING contentlet2_variant_1", titleField);


        checkVersion(contentlet2, true, variant_2, "LIVE contentlet2_variant_2", titleField);
        checkVersion(contentlet2, false, variant_2, "WORKING contentlet2_variant_2", titleField);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create one {@link Variant}s
     * - Create two {@link Contentlet} and create a new version into the  {@link Variant}, no create any version to the DEFAULT Variant.
     * - Publish.
     * - Promote the {@link Variant}'s
     *
     * Should:
     * - Copy both version (WORKING and LIVE) of the specific Variant that was promoted and turn it into the WORKING/LIVE DEFAULT Variant
     */
    @Test
    public void promoteWithoutDefaultVersion() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1_variant")
                .variant(variant)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2_variant")
                .variant(variant)
                .nextPersisted();

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1_variant"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2_variant"));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant", titleField);
        checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant", titleField);


        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant",
                titleField);
        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant", titleField);
        checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant", titleField);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create one {@link Variant}s
     * - Create two {@link Contentlet} just for DEFAULT Variant.
     * - Publish.
     * - Promote the {@link Variant}'s
     *
     * Should: do nothing
     */
    @Test
    public void promoteWithoutVariantVersion() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1",
                titleField);

        checkNull(contentlet1, false, variant);
        checkNull(contentlet1, true, variant);


        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2",
                titleField);
        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2",
                titleField);

        checkNull(contentlet2, true, variant);
        checkNull(contentlet2, false, variant);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - You create one {@link Variant}s.
     * - Create one {@link Contentlet} with a version on this newly variant.
     * - Archived the Variant.
     * - Promote the {@link Variant}'s
     *
     * Should: thorw a Exception
     */
    @Test
    public void promoteArchivedVariant() throws DotDataException, DotSecurityException {
        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "DEFAULT contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "DEFAULT contentlet2")
                .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();


        ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant_2"
                ));
        ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant_2"
                ));

        APILocator.getVariantAPI().archive(variant.name());

        try {
            APILocator.getVariantAPI().promote(variant, APILocator.systemUser());
            fail("Should throw a Exception");
        } catch (IllegalArgumentException e) {
            //Expected
        }
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When: You try to promote a Variant that does not exist
     * Should: throw a Exception
     */
    @Test
    public void promoteNonExistingVariant() throws DotDataException {

        final Variant doesNotExistsVariant = new VariantDataGen().next();
        try {
            APILocator.getVariantAPI().promote(doesNotExistsVariant, APILocator.systemUser());
            fail("Should throw a Exception");
        } catch (DoesNotExistException e) {
            //Expected
        }
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When: You try to promote the DEFAULT Variant.
     * Should: throw a Exception
     */
    @Test
    public void promoteDefaultVariant() throws DotDataException {
        try {
            APILocator.getVariantAPI().promote(VariantAPI.DEFAULT_VARIANT, APILocator.systemUser());
            fail("Should throw a Exception");
        } catch (IllegalArgumentException e) {
            //Expected
        }
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When: You try to promote a Variant that is already promoted.
     * Should: not fail
     */
    @Test
    public void promoteAlreadyPromoteVariant() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();
        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());
        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}}
     * When:
     * - Create a {@link Variant}
     * - Create 3 {@link com.dotmarketing.portlets.languagesmanager.model.Language}s: language_1, language_2 and language_3
     * - Create two {@link Contentlet} and create for each of them versions in:
     *   - DEFAULT Variant and language_1.
     *   - DEFAULT Variant and language_2.
     *   - Newly created Variant and language_1.
     *   - Newly created Variant and language_3.
     *
     * - Promote the newly created {@link Variant}
     *
     * Should:
     * - Copy the version of the language_1 to the language_3 to the DEFAULT Variant and keep the
     * DEFAULT Variant and language_2 as it is.
     */
    @Test
    public void promoteContentletWithSeveralLanguages() throws DotDataException, DotSecurityException {

        final Variant variant = new VariantDataGen().nextPersisted();

        final Language language_1 = new LanguageDataGen().nextPersisted();
        final Language language_2 = new LanguageDataGen().nextPersisted();
        final Language language_3 = new LanguageDataGen().nextPersisted();

        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "contentlet1 Language_1")
                .languageId(language_1.getId())
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "contentlet2 Language_1")
                .languageId(language_1.getId())
                .nextPersisted();

        ContentletDataGen.createNewVersion(contentlet1, VariantAPI.DEFAULT_VARIANT, language_2,
                Map.of(titleField.variable(), "contentlet1 Language_2"));

        ContentletDataGen.createNewVersion(contentlet1, variant, language_1,
                Map.of(titleField.variable(), "contentlet1_variant Language_1"));

        ContentletDataGen.createNewVersion(contentlet1, variant, language_3,
                Map.of(titleField.variable(), "contentlet1_variant Language_3"));


        ContentletDataGen.createNewVersion(contentlet2, VariantAPI.DEFAULT_VARIANT, language_2,
                Map.of(titleField.variable(), "contentlet2 Language_2"));

        ContentletDataGen.createNewVersion(contentlet2, variant, language_1,
                Map.of(titleField.variable(), "contentlet2_variant Language_1"
                ));

        ContentletDataGen.createNewVersion(contentlet2, variant, language_3,
                Map.of(titleField.variable(), "contentlet2_variant Language_3"
        ));

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, language_1, "contentlet1_variant Language_1",
                titleField);

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, language_2, "contentlet1 Language_2",
                titleField);

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, language_3, "contentlet1_variant Language_3",
                titleField);


        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, language_1, "contentlet2_variant Language_1",
                titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, language_2, "contentlet2 Language_2",
                titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, language_3, "contentlet2_variant Language_3",
                titleField);

        checkVersion(contentlet1, false, variant, language_1, "contentlet1_variant Language_1",
                titleField);

        checkNull(contentlet1, false, variant, language_2);

        checkVersion(contentlet1, false, variant, language_3, "contentlet1_variant Language_3",
                titleField);


        checkVersion(contentlet2, false, variant, language_1, "contentlet2_variant Language_1",
                titleField);

        checkNull(contentlet2, false, variant, language_2);

        checkVersion(contentlet2, false, variant, language_3, "contentlet2_variant Language_3",
                titleField);
    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - Create a {@link Variant}.
     * - Create a {@link ContentType} with a {@link TextField} called title.
     * - Create two {@link Contentlet} and create version for DEFAULT and the newly created Variant.
     * - Publish them and later Create a new Working Version for each of them.
     * - Create a Page and Create a version of the page for the newly created Variant.
     * - Add the {@link Contentlet} 1 to the page into the DEFAULT Variant.
     * - Add the {@link Contentlet} 2 to the page into the newly created Variant.
     * - Promote the newly created {@link Variant}
     * Should: Copy the two Contetlet and the page to the DEFAULT Variant, Also should override the {@link com.dotmarketing.beans.MultiTree}
     * from the Variant to Default.
     */
    @Test
    public void promotePage() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Contentlet contentlet1Variant = ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant"
                ));
        final Contentlet contentlet2Variant = ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant"
                ));

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));
        ContentletDataGen.update(contentlet1Variant, Map.of("title", "WORKING contentlet1_variant"));
        ContentletDataGen.update(contentlet2Variant, Map.of("title", "WORKING contentlet2_variant"));

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container.getIdentifier()).nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.createNewVersion(htmlPageAsset, variant, new HashMap<>());

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet2)
                .setVariant(variant)
                .nextPersisted();

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant",
                titleField);

        checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant",
                titleField);

        final List<MultiTree> multiTreesByDefault = APILocator.getMultiTreeAPI()
                .getMultiTreesByVariant(htmlPageAsset.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(1, multiTreesByDefault.size());
        multiTreesByDefault.stream().map(multiTree -> multiTree.getContentlet())
                .forEach(contentletIdentifier ->
                    assertTrue(contentletIdentifier.equals(contentlet2.getIdentifier()))
                );

        final List<MultiTree> multiTreesByVariant = APILocator.getMultiTreeAPI()
                .getMultiTreesByVariant(htmlPageAsset.getIdentifier(), variant.name());

        assertEquals(1, multiTreesByVariant.size());
        multiTreesByVariant.stream().map(multiTree -> multiTree.getContentlet())
                .forEach(contentletIdentifier ->
                        assertTrue(contentletIdentifier.equals(contentlet2Variant.getIdentifier()))
                );

    }

    /**
     * Method to test: {@link VariantAPIImpl#promote(Variant, User)}
     * When:
     * - Create a {@link Variant}.
     * - Create a {@link ContentType} with a {@link TextField} called title.
     * - Create two {@link Contentlet} and create version for DEFAULT and the newly created Variant.
     * - Publish them and later Create a new Working Version for each of them.
     * - Create a Page but NOT Create a version of the page for the newly created Variant.
     * - Add the {@link Contentlet} 1 to the page into the DEFAULT Variant.
     * - Add the {@link Contentlet} 2 to the page into the newly created Variant.
     * - Promote the newly created {@link Variant}
     * Should: Copy the two Contetlet Also should override the {@link com.dotmarketing.beans.MultiTree}
     * from the Variant to Default.
     */
    @Test
    public void pageWithNoVersion() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().nextPersisted();

        final Field titleField = new FieldDataGen()
                .type(TextField.class)
                .name("title")
                .velocityVarName("title")
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(titleField)
                .nextPersisted();

        final Contentlet contentlet1 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet1")
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "LIVE contentlet2")
                .nextPersisted();

        final Contentlet contentlet1Variant = ContentletDataGen.createNewVersion(contentlet1,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet1_variant"
                ));
        final Contentlet contentlet2Variant = ContentletDataGen.createNewVersion(contentlet2,
                variant, Map.of(
                        titleField.variable(), "LIVE contentlet2_variant"
                ));

        APILocator.getContentletAPI().publish(contentlet1, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2, APILocator.systemUser(), false);

        APILocator.getContentletAPI().publish(contentlet1Variant, APILocator.systemUser(), false);
        APILocator.getContentletAPI().publish(contentlet2Variant, APILocator.systemUser(), false);

        ContentletDataGen.update(contentlet1, Map.of("title", "WORKING contentlet1"));
        ContentletDataGen.update(contentlet2, Map.of("title", "WORKING contentlet2"));
        ContentletDataGen.update(contentlet1Variant, Map.of("title", "WORKING contentlet1_variant"));
        ContentletDataGen.update(contentlet2Variant, Map.of("title", "WORKING contentlet2_variant"));

        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container.getIdentifier()).nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet1)
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet2)
                .setVariant(variant)
                .nextPersisted();

        APILocator.getVariantAPI().promote(variant, APILocator.systemUser());

        checkVersion(contentlet1, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, false, variant, "WORKING contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet1, true, variant, "LIVE contentlet1_variant",
                titleField);

        checkVersion(contentlet2, false, VariantAPI.DEFAULT_VARIANT, "WORKING contentlet2_variant",
                titleField);

        checkVersion(contentlet2, false, variant, "WORKING contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, VariantAPI.DEFAULT_VARIANT, "LIVE contentlet2_variant",
                titleField);

        checkVersion(contentlet2, true, variant, "LIVE contentlet2_variant",
                titleField);

        final List<MultiTree> multiTreesByDefault = APILocator.getMultiTreeAPI()
                .getMultiTreesByVariant(htmlPageAsset.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name());

        assertEquals(1, multiTreesByDefault.size());
        multiTreesByDefault.stream().map(multiTree -> multiTree.getContentlet())
                .forEach(contentletIdentifier ->
                        assertTrue(contentletIdentifier.equals(contentlet2.getIdentifier()))
                );

        final List<MultiTree> multiTreesByVariant = APILocator.getMultiTreeAPI()
                .getMultiTreesByVariant(htmlPageAsset.getIdentifier(), variant.name());

        assertEquals(1, multiTreesByVariant.size());
        multiTreesByVariant.stream().map(multiTree -> multiTree.getContentlet())
                .forEach(contentletIdentifier ->
                        assertTrue(contentletIdentifier.equals(contentlet2Variant.getIdentifier()))
                );

        final Optional<Table<String, String, Set<PersonalizedContentlet>>> pageMultiTrees = CacheLocator.getMultiTreeCache()
                .getPageMultiTrees(htmlPageAsset.getIdentifier(), variant.name(), false);

        assertTrue(pageMultiTrees.isEmpty());
    }

    /**
     * Method to test: {@link VariantAPIImpl#delete(String)}
     * When:
     * - Create a Page
     * - Create a Variant and an Experiment
     * - Add a Contentlet inside the page just for the Specific Variant
     * - Remove The Variant
     *
     * Should: remove the Variant and the MultiTree registers
     */
    @Test
    public void removeMultiTreeAfterRemoveVariant() throws DotDataException, DotSecurityException {
        final Variant variant = new VariantDataGen().archived(true).nextPersisted();

        final Field textField = new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(textField)
                .nextPersisted();

        final Contentlet contentlet_1 = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "CONTENT 1")
                .variant(variant)
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .setProperty(textField.variable(), "CONTENT 3")
                .variant(variant)
                .nextPersisted();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final Container container = new ContainerDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        new MultiTreeDataGen()
                .setVariant(VariantAPI.DEFAULT_VARIANT)
                .setContainer(container)
                .setPage(page)
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setVariant(variant)
                .setContainer(container)
                .setPage(page)
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setVariant(variant)
                .setContainer(container)
                .setPage(page)
                .setContentlet(contentlet_2)
                .nextPersisted();

        checkMultiTreeCount(page, variant, 2);
        checkMultiTreeCount(page, VariantAPI.DEFAULT_VARIANT, 1);

        APILocator.getVariantAPI().delete(variant.name());

        checkMultiTreeCount(page, variant, 0);
        checkMultiTreeCount(page, VariantAPI.DEFAULT_VARIANT, 1);

        final List<Map<String, Object>> loadObjectResultsAfterDeleted = new DotConnect().setSQL(
                        "SELECT child, variant_id FROM multi_tree WHERE parent1= ?")
                .addParam(page.getIdentifier()).loadObjectResults();

        assertEquals(1, loadObjectResultsAfterDeleted.size());
        assertEquals(loadObjectResultsAfterDeleted.get(0).get("child").toString(), contentlet_1.getIdentifier());
        assertEquals(loadObjectResultsAfterDeleted.get(0).get("variant_id").toString(), VariantAPI.DEFAULT_VARIANT.name());
    }

    private static void checkMultiTreeCount(final HTMLPageAsset page, Variant variant, int expected) throws DotDataException {
        final List<Map<String, Object>> loadObjectResultsBeforeDeleted = new DotConnect().setSQL(
                        "SELECT count(*) FROM multi_tree WHERE variant_id= ? AND parent1= ?")
                .addParam(variant.name())
                .addParam(page.getIdentifier()).loadObjectResults();

        assertEquals(1, loadObjectResultsBeforeDeleted.size());
        final int count = Integer.parseInt(loadObjectResultsBeforeDeleted.get(0).get("count").toString());
        assertEquals(expected, count);
    }

    /**
     * Method to test: {@link Variant} H22 Serialization
     * When: Try to Serialize a {@link Variant}
     * Should: not throw any exception
     *
     * @throws IOException
     */
    @Test
    public void testVariantSerialization() throws IOException, ClassNotFoundException {

        final Variant variant = new VariantDataGen().nextPersisted();

        byte[] bytes = null;

        try(ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(os, 8192)) ){

                output.writeObject(variant);
                output.flush();

             bytes = os.toByteArray();
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))){
            final Variant variantFromBytes = (Variant) input.readObject();

            assertEquals(variant.name(), variantFromBytes.name());
            assertEquals(variant.description(), variantFromBytes.description());
            assertEquals(variant.archived(), variantFromBytes.archived());
        }


    }
}

