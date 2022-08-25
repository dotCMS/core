package com.dotmarketing.portlets.variant.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
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

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.getIdentifier());

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantSaved.getName(), variantFromDataBase.getName());
        assertEquals(variantSaved.getIdentifier(), variantFromDataBase.getIdentifier());
        assertFalse(variantFromDataBase.isArchived());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a {@link Variant} object without name
     * Should: throw {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test
    public void saveWithoutName() throws DotDataException {
        final Variant variant = new Variant("1", null, false);

        try {
            FactoryLocator.getVariantFactory().save(variant);
            throw new AssertionError("DotDataException Expected");
        }catch (DotDataException e) {
            if (DbConnectionFactory.isPostgres()) {
                assertEquals(PSQLException.class, e.getCause().getClass());
            } else if (DbConnectionFactory.isMsSql()){
                assertEquals(SQLServerException.class, e.getCause().getClass());
            } else {
                throw new AssertionError("Database not expected");
            }
        }
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
        assertNotNull(variantSaved.getIdentifier());

        final Variant variantUpdated = new Variant(variantSaved.getIdentifier(),
                variantSaved.getName() + "_updated", false);

        FactoryLocator.getVariantFactory().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantUpdated.getName(), variantFromDataBase.getName());
        assertEquals(variantUpdated.getIdentifier(), variantFromDataBase.getIdentifier());
        assertFalse(variantFromDataBase.isArchived());
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
        assertNotNull(variantSaved.getIdentifier());
        assertFalse(variantSaved.isArchived());

        final Variant variantUpdated = new Variant(variantSaved.getIdentifier(),
                variantSaved.getName(), true);

        FactoryLocator.getVariantFactory().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantUpdated.getName(), variantFromDataBase.getName());
        assertEquals(variantUpdated.getIdentifier(), variantFromDataBase.getIdentifier());
        assertTrue(variantFromDataBase.isArchived());
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update a {@link Variant} object with a Null name
     * Should: throw a {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test()
    public void updateWithoutName() throws DotDataException {
        final Variant variant = new VariantDataGen().next();

        final VariantFactory variantFactory = new VariantFactoryImpl();
        final Variant variantSaved = variantFactory.save(variant);

        assertNotNull(variantSaved);
        assertNotNull(variantSaved.getIdentifier());

        final Variant variantUpdated = new Variant(variantSaved.getIdentifier(), null, false);

        try {
            variantFactory.update(variantUpdated);
            throw new AssertionError("DotDataException Expected");
        }catch (DotDataException e) {
            if (DbConnectionFactory.isPostgres()) {
                assertEquals(PSQLException.class, e.getCause().getClass());
            } else if (DbConnectionFactory.isMsSql()){
                assertEquals(SQLServerException.class, e.getCause().getClass());
            } else {
                throw new AssertionError("Database not expected");
            }
        }
    }

    /**
     * Method to test: {@link VariantFactory#update(Variant)}
     * When: Try to update a {@link Variant} object with a Null ID
     * Should: throw a {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test
    public void updateIDNull() throws DotDataException {
        final Variant variant = new Variant(null, "Name", false);

       FactoryLocator.getVariantFactory().update(variant);

        final ArrayList results = getResults(variant);

        assertTrue(results.isEmpty());
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

        FactoryLocator.getVariantFactory().delete(variant.getIdentifier());

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

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory().get(variant.getIdentifier());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.getIdentifier(), variantFromDataBase.get().getIdentifier());
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

        final Optional<Variant> variantFromDataBase = FactoryLocator.getVariantFactory().get(variant.getIdentifier());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.getIdentifier(), variantFromDataBase.get().getIdentifier());
        assertTrue(variantFromDataBase.get().isArchived());
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

    private ArrayList getResults(Variant variant) throws DotDataException {
        return new DotConnect().setSQL(
                        "SELECT * FROM variant where id = ?")
                .addParam(variant.getIdentifier())
                .loadResults();
    }

    private Variant getVariantFromDataBase(Variant variant) throws DotDataException {
        final ArrayList results = getResults(variant);

        assertEquals(1, results.size());
        final Map resultMap = (Map) results.get(0);
        return new Variant(resultMap.get("id").toString(), resultMap.get("name").toString(),
                ConversionUtils.toBooleanFromDb(resultMap.get("archived")));
    }
}
