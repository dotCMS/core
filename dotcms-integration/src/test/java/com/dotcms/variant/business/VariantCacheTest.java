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
     * Method to test: {@link VariantCacheImpl#put(Variant)} and {@link VariantCacheImpl#get(String)} (String)}
     * When: Add a Variant
     * Should: be able to get it by Name
     */
    @Test
    public void getVariantByName(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put(variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .get(variant.name());

        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#put(String, Variant)} (Variant)} and {@link VariantCacheImpl#get(String)} (String)}
     * When: Add a Variant by name
     * Should: be able to get it by the same Name
     */
    @Test
    public void putVariantByName(){
        final Variant variant = new VariantDataGen().nextPersisted();

        CacheLocator.getVariantCache().put("ANY_NAME", variant);

        final Variant variantById = CacheLocator.getVariantCache()
                .get("ANY_NAME");

        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.name(), variantById.name());
        assertEquals(variant.archived(), variantById.archived());
    }

    /**
     * Method to test: {@link VariantCacheImpl#get(String)}
     * When: Call the method with a Name that not was put before
     * Should: return null
     */
    @Test
    public void getVariantByNameNotExists(){
        assertNull(CacheLocator.getVariantCache().get("NotExists"));
    }

    /**
     * Method to test: {@link VariantCacheImpl#get(String)}
     * When: Call the method with null
     * Should: throw a {@link NullPointerException}
     */
    @Test(expected = NullPointerException.class)
    public void getVariantByNameWithNull(){
        assertNull(CacheLocator.getVariantCache().get(null));
    }

    /**
     * Method to test: {@link VariantCacheImpl#remove(Variant)}
     * When: Remove a Variant from cache
     * Should: get Null when call {@link VariantCacheImpl#get(String)}
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
        final Variant variantByName = CacheLocator.getVariantCache()
                .get(variant.name());
        assertNull(variantByName);
    }

    private void checkFromCacheNotNull(final Variant variant) {
        final Variant variantByName = CacheLocator.getVariantCache()
                .get(variant.name());
        assertNotNull(variantByName);
    }
}
