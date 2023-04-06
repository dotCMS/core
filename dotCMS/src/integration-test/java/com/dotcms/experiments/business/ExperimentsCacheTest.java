package com.dotcms.experiments.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.business.VariantCacheImpl;
import com.dotmarketing.business.CacheLocator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test for {@link ExperimentsCache}
 *
 * @author vico
 */
public class ExperimentsCacheTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentsCacheImpl#put(String, List)} and {@link ExperimentsCacheImpl#get(String)}
     * When: Add a Experiment
     * Should: be able to get it by Name
     */
    @Test
    public void test_putExperiment_andGetExperiment() {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        CacheLocator.getExperimentsCache().put(ExperimentsCache.RUNNING_EXPERIMENTS_KEY, List.of(experiment));

        final List<Experiment> cached = CacheLocator.getExperimentsCache().get(ExperimentsCache.RUNNING_EXPERIMENTS_KEY);

        assertFalse(cached.isEmpty());
        final Experiment cachedExperiment = cached.get(0);
        assertEquals(experiment.id(), cachedExperiment.id());
        assertEquals(experiment.name(), cachedExperiment.name());
    }

    /**
     * Method to test: {@link VariantCacheImpl#get(String)}
     * When: Call the method with a Name that not was put before
     * Should: return null
     */
    @Test
    public void getExperiments_doesNotExist() {
        assertNull(CacheLocator.getExperimentsCache().get("NotExists"));
    }

    /**
     * Method to test: {@link ExperimentsCacheImpl#remove(String)}
     * When: Remove a Experiment from cache
     * Should: get Null when call {@link ExperimentsCacheImpl#get(String)}
     */
    @Test
    public void test_remove() {
        final Experiment experiment1 = new ExperimentDataGen().nextPersisted();
        final Experiment experiment2 = new ExperimentDataGen().nextPersisted();

        final String name = "some-experiments";
        CacheLocator.getExperimentsCache().put(name, List.of(experiment1, experiment2));
        checkFromCacheNotNull(name, 2);

        CacheLocator.getExperimentsCache().remove(name);
        checkFromCacheNull(name);
    }

    /**
     * Method to test: {@link ExperimentsCacheImpl#get(String)}
     * When: Call the method with null
     * Should: throw a {@link NullPointerException}
     */
    @Test(expected = NullPointerException.class)
    public void test_nullName(){
        assertNull(CacheLocator.getExperimentsCache().get(null));
    }

    /**
     * Method to test: {@link ExperimentsCacheImpl#clearCache()}
     * When: Put two lists {@link Experiment} into cache and then clear the cache
     * Should: Return null for the two {@link Experiment}s
     */
    @Test
    public void clear() {
        final Experiment experiment1 = new ExperimentDataGen().nextPersisted();
        final Experiment experiment2 = new ExperimentDataGen().nextPersisted();
        final Experiment experiment3 = new ExperimentDataGen().nextPersisted();

        final String name1 = "some-experiments";
        CacheLocator.getExperimentsCache().put(name1, List.of(experiment1));
        checkFromCacheNotNull(name1, 1);

        final String name2 = "more-experiments";
        CacheLocator.getExperimentsCache().put(name2, List.of(experiment2, experiment3));
        checkFromCacheNotNull(name2, 2);

        CacheLocator.getExperimentsCache().clearCache();

        checkFromCacheNull(name1);
        checkFromCacheNull(name2);
    }

    private void checkFromCacheNotNull(final String name, final int size) {
        final List<Experiment> experiments = CacheLocator.getExperimentsCache().get(name);
        assertNotNull(experiments);
        assertEquals(size, experiments.size());
    }

    private void checkFromCacheNull(final String name) {
        assertNull(CacheLocator.getExperimentsCache().get(name));
    }

}
