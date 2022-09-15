package com.dotcms.variant.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.CacheLocator;
import org.junit.BeforeClass;
import org.junit.Test;

public class VariantCacheTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link VariantCacheImpl#put(Variant)} and {@link VariantCacheImpl#getById(String)}
     * When: Add a Variant
     * Should: be able to get it by ID
     */
    @Test
    public void getVariantById(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put(variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .getById(variant.identifier());

        assertEquals(variant.identifier(), variantById.identifier());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#putById(String, Variant)} (Variant)} and {@link VariantCacheImpl#getById(String)}
     * When: Put a Variant by ID
     * Should: be able to get it by the same ID
     */
    @Test
    public void putVariantById(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().putById("ANY_ID", variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .getById("ANY_ID");

        assertEquals(variant.identifier(), variantById.identifier());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#put(Variant)} and {@link VariantCacheImpl#getByName(String)} (String)}
     * When: Add a Variant
     * Should: be able to get it by Name
     */
    @Test
    public void getVariantByName(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put(variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .getByName(variant.name());

        assertEquals(variant.identifier(), variantById.identifier());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#putByName(String, Variant)} (Variant)} and {@link VariantCacheImpl#getByName(String)} (String)}
     * When: Add a Variant by name
     * Should: be able to get it by the same Name
     */
    @Test
    public void putVariantByName(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().putByName("ANY_NAME", variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .getByName("ANY_NAME");

        assertEquals(variant.identifier(), variantById.identifier());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#getById(String)}
     * When: Call the method with an id that not was put before
     * Should: return null
     */
    @Test
    public void getVariantByIdNotExists(){
        assertNull(CacheLocator.getVariantCache().getById("NotExists"));
    }

    /**
     * Method to test: {@link VariantCacheImpl#getByName(String)}
     * When: Call the method with a Name that not was put before
     * Should: return null
     */
    @Test
    public void getVariantByNameNotExists(){
        assertNull(CacheLocator.getVariantCache().getById("NotExists"));
    }

    /**
     * Method to test: {@link VariantCacheImpl#getById(String)}
     * When: Call the method with null
     * Should: throw a {@link NullPointerException}
     */
    @Test(expected = NullPointerException.class)
    public void getVariantByIdWithNull(){
        assertNull(CacheLocator.getVariantCache().getById(null));
    }

    /**
     * Method to test: {@link VariantCacheImpl#getByName(String)}
     * When: Call the method with null
     * Should: throw a {@link NullPointerException}
     */
    @Test(expected = NullPointerException.class)
    public void getVariantByNameWithNull(){
        assertNull(CacheLocator.getVariantCache().getByName(null));
    }

    /**
     * Method to test: {@link VariantCacheImpl#remove(Variant)}
     * When: Remove a Variant from cache
     * Should: get Null when call {@link VariantCacheImpl#getByName(String)} or  {@link VariantCacheImpl#getById(String)} (String)}
     */
    @Test
    public void remove(){
        final Variant variant = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put(variant);
        checkFromCacheNotNull(variant);

        CacheLocator.getVariantCache().put(variant_2);
        checkFromCacheNotNull(variant_2);

        CacheLocator.getVariantCache().remove(variant);
        checkFromCacheNull(variant);

        checkFromCacheNotNull(variant_2);
    }

    /**
     * Method to test: {@link VariantCacheImpl#clearCache()}
     * When: Put two {@link Variant} into cache and then clear the cache
     * Should: Return null for the two {@link Variant}s
     */
    @Test
    public void clear(){
        final Variant variant = new VariantDataGen().nextPersisted();
        final Variant variant_2 = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put(variant);
        checkFromCacheNotNull(variant);

        CacheLocator.getVariantCache().put(variant_2);
        checkFromCacheNotNull(variant_2);

        CacheLocator.getVariantCache().clearCache();

        checkFromCacheNull(variant);
        checkFromCacheNull(variant_2);
    }

    private void checkFromCacheNull(final Variant variant) {
        final Variant variantById = CacheLocator.getVariantCache()
                .getById(variant.identifier());
        assertNull(variantById);

        final Variant variantByName = CacheLocator.getVariantCache()
                .getByName(variant.name());
        assertNull(variantByName);
    }

    private void checkFromCacheNotNull(final Variant variant) {
        final Variant variantById = CacheLocator.getVariantCache()
                .getById(variant.identifier());
        assertNotNull(variantById);

        final Variant variantByName = CacheLocator.getVariantCache()
                .getByName(variant.name());
        assertNotNull(variantByName);
    }
}
