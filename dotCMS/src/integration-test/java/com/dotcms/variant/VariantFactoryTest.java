package com.dotcms.variant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.util.PSQLException;

public class VariantFactoryTest {

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

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);

        checkFromDataBase(variantSaved);
    }

    private void checkFromDataBase(Variant variantSaved) throws DotDataException {
        assertNotNull(variantSaved);
        assertNotNull(variantSaved.identifier());

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantSaved.name(), variantFromDataBase.name());
        assertEquals(variantSaved.identifier(), variantFromDataBase.identifier());
        assertFalse(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a {@link Variant} object with duplicated name
     * Should: throw a {@link DotDataException}
     *
     * @throws DotDataException
     */
    @Test
    public void saveDuplicatedNamed() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);
        checkFromDataBase(variantSaved);

        final Variant variantWithNameDuplicated = new VariantDataGen().name(variant.name()).next();

        try {
            FactoryLocator.getVariantFactory().save(variantWithNameDuplicated);
            throw new AssertionError("DotDataException expected");
        } catch (DotDataException e) {
            if (DbConnectionFactory.isMsSql()) {
                assertTrue(e.getCause().getClass().equals(SQLServerException.class));
            } else {
                assertTrue(e.getCause().getClass().equals(PSQLException.class));
            }
        }
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a {@link Variant} object with duplicated name
     * Should: throw a {@link DotDataException}
     *
     * @throws DotDataException
     */
    @Test
    public void updateDuplicatedNamed() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);
        checkFromDataBase(variantSaved);

        final Variant variant_2 = new VariantDataGen().nextPersisted();

        final Variant variantWithNameDuplicated = Variant.builder()
                .identifier(variant_2.identifier())
                .name(variant.name())
                .archived(variant_2.archived())
                .build();

        try {
            FactoryLocator.getVariantFactory().update(variantWithNameDuplicated);
            throw new AssertionError("DotDataException expected");
        } catch (DotDataException e) {
            if (DbConnectionFactory.isMsSql()) {
                assertTrue(e.getCause().getClass().equals(SQLServerException.class));
            } else {
                assertTrue(e.getCause().getClass().equals(PSQLException.class));
            }
        }
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Save a Variant it's id should be deterministic
     * Should: Calculate the id value as hash of the name
     *
     * @throws DotDataException
     */
    @Test
    public void saveCalculateID() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.identifier());
        assertEquals("The ID should be a hash of the name",
                DigestUtils.sha256Hex(variant.name()), variantSaved.identifier());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Save a Variant it's id should be deterministic but if it already exists a Variant with the
     * deterministic ID then it is re calculate with a no deterministic ID
     * Should: Calculate the id value as hash of the name
     *
     * @throws DotDataException
     */
    @Test
    public void saveCalculateRepeatID() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        new DotConnect().setSQL("INSERT INTO variant (id, name, archived) VALUES (?, ?, ?)")
                .addParam(DigestUtils.sha256Hex(variant.name()))
                .addParam("Any name_" + System.currentTimeMillis())
                .addParam(false)
                .loadResult();

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.identifier());
        assertNotEquals("The ID should not be a hash of the name",
                DigestUtils.sha256Hex(variant.name()), variantSaved.identifier());
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
        final Variant variant = new VariantDataGen().next();
        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.identifier());

        final Variant variantUpdated = new VariantDataGen()
                .id(variantSaved.identifier())
                .name(variantSaved.name() + "_updated")
                .archived(false)
                .next();

        FactoryLocator.getVariantFactory().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantUpdated.name(), variantFromDataBase.name());
        assertEquals(variantUpdated.identifier(), variantFromDataBase.identifier());
        assertFalse(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update the {@link Variant}'s deleted attribute
     * Should: Update it in Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void updateDeletedField() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final Variant variantSaved = FactoryLocator.getVariantFactory().save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.identifier());
        assertFalse(variantSaved.archived());

        final Variant variantUpdated = new VariantDataGen()
                .id(variantSaved.identifier())
                .name(variantSaved.name())
                .archived(true)
                .next();

        FactoryLocator.getVariantFactory().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantUpdated.name(), variantFromDataBase.name());
        assertEquals(variantUpdated.identifier(), variantFromDataBase.identifier());
        assertTrue(variantFromDataBase.archived());
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to delete a {@link Variant} object
     * Should: remove it from Data base.
     *
     * @throws DotDataException
     */
    @Test
    public void delete() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        FactoryLocator.getVariantFactory().delete(variant.identifier());

        results = getResults(variant);
        assertTrue(results.isEmpty());
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

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory().get(variant.identifier());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.identifier(), variantFromDataBase.get().identifier());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by namee
     * Should: get it
     *
     * @throws DotDataException
     */
    @Test
    public void getByName() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory().getByName(variant.name());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.identifier(), variantFromDataBase.get().identifier());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  archived {@link Variant} by id
     * Should: get it
     *
     * @throws DotDataException
     */
    @Test
    public void getArchived() throws DotDataException {
        final Variant variant = new VariantDataGen().archived(true).nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory().get(variant.identifier());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.identifier(), variantFromDataBase.get().identifier());
        assertTrue(variantFromDataBase.get().archived());
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

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory()
                .get("Not_Exists");

        assertFalse(variantFromDataBase.isPresent());
    }

    /**
     * Method to test: {@link VariantFactory#get(String)}
     * When: Try to get  {@link Variant} by name that not exists
     * Should: return a {@link Optional#empty()}
     *
     * @throws DotDataException
     */
    @Test
    public void getNotExistsByName() throws DotDataException {

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory()
                .getByName("Not_Exists");

        assertFalse(variantFromDataBase.isPresent());
    }

    private ArrayList getResults(Variant variant) throws DotDataException {
        return new DotConnect().setSQL(
                        "SELECT * FROM variant where id = ?")
                .addParam(variant.identifier())
                .loadResults();
    }

    private Variant getVariantFromDataBase(final Variant variant) throws DotDataException {
        final ArrayList results = getResults(variant);

        assertEquals(1, results.size());
        final Map resultMap = (Map) results.get(0);
        return Variant.builder()
                .identifier(resultMap.get("id").toString())
                .name(resultMap.get("name").toString())
                .archived(ConversionUtils.toBooleanFromDb(resultMap.get("archived")))
                .build();
    }
}
