package com.dotmarketing.portlets.variant.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;


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
        assertNotNull(variantSaved.getIdentifier());

        final Variant variantFromDataBase = getVariantFromDataBase(variantSaved);

        assertEquals(variantSaved.getName(), variantFromDataBase.getName());
        assertEquals(variantSaved.getIdentifier(), variantFromDataBase.getIdentifier());
        assertFalse(variantFromDataBase.isDeleted());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to save a {@link Variant} object without name
     * Should: throw {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void saveWithoutName() throws DotDataException {
        final Variant variant = new Variant("1", null, false);
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
        assertNotNull(variant.getIdentifier());

        final Variant variantUpdated = new Variant(variant.getIdentifier(),
                variant.getName() + "_updated", false);

        APILocator.getVariantAPI().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variant);

        assertEquals(variantUpdated.getName(), variantFromDataBase.getName());
        assertEquals(variantUpdated.getIdentifier(), variantFromDataBase.getIdentifier());
        assertFalse(variantFromDataBase.isDeleted());
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to update a {@link Variant} object without name
     * Should: throw {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void updateWithoutName() throws DotDataException {
        final Variant variant = new Variant("1", null, false);
        APILocator.getVariantAPI().update(variant);
    }

    /**
     * Method to test: {@link VariantFactory#save(Variant)}
     * When: Try to update a {@link Variant} object without id
     * Should: throw {@link NullPointerException}
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void updateWithoutID() throws DotDataException {
        final Variant variant = new Variant(null, "NAME", false);
        APILocator.getVariantAPI().update(variant);
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
        final Variant variant = new VariantDataGen().nextPersisted();

        assertNotNull(variant);
        assertNotNull(variant.getIdentifier());
        assertFalse(variant.isDeleted());

        final Variant variantUpdated = new Variant(variant.getIdentifier(),
                variant.getName(), true);

        APILocator.getVariantAPI().update(variantUpdated);

        final Variant variantFromDataBase = getVariantFromDataBase(variant);

        assertEquals(variantUpdated.getName(), variantFromDataBase.getName());
        assertEquals(variantUpdated.getIdentifier(), variantFromDataBase.getIdentifier());
        assertTrue(variantFromDataBase.isDeleted());
    }

    /**
     * Method to test: {@link VariantFactory#delete(String)}
     * When: Try to archive a {@link Variant} object
     * Should: save it with deleted equals to true
     *
     * @throws DotDataException
     */
    @Test
    public void archive() throws DotDataException {
        final Variant variant = new VariantDataGen().nextPersisted();

        ArrayList results = getResults(variant);
        assertFalse(results.isEmpty());

        APILocator.getVariantAPI().archive(variant.getIdentifier());

        final Variant variantFromDataBase = getVariantFromDataBase(variant);
        assertEquals(variantFromDataBase.getName(), variantFromDataBase.getName());
        assertEquals(variantFromDataBase.getIdentifier(), variantFromDataBase.getIdentifier());
        assertTrue(variantFromDataBase.isDeleted());
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

        APILocator.getVariantAPI().delete(variant.getIdentifier());

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

        final Optional<Variant> variantFromDataBase = APILocator.getVariantAPI().get(variant.getIdentifier());

        assertTrue(variantFromDataBase.isPresent());
        assertEquals(variant.getIdentifier(), variantFromDataBase.get().getIdentifier());
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
     * Should:
     *
     * @throws DotDataException
     */
    @Test(expected = NullPointerException.class)
    public void getWithNull() throws DotDataException {
        APILocator.getVariantAPI().get(null);
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
                resultMap.get("deleted").equals("t"));
    }
}
