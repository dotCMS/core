package com.dotcms.experiments.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.IntegrationTestInitService;
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
     * Method to test: {@link ExperimentsCacheImpl#put(List)} and {@link ExperimentsCacheImpl#get()}
     * When: Add a Experiment
     * Should: be able to get it by Name
     */
    @Test
    public void test_putExperiment_andGetExperiment() {
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        CacheLocator.getExperimentsCache().put(List.of(experiment));

        final List<Experiment> cached = CacheLocator.getExperimentsCache().get();

        assertFalse(cached.isEmpty());
        final Experiment cachedExperiment = cached.get(0);
        assertEquals(experiment.id(), cachedExperiment.id());
        assertEquals(experiment.name(), cachedExperiment.name());
    }

    /**
     * Method to test: {@link ExperimentsCacheImpl#remove()}
     * When: Remove a Experiment from cache
     * Should: get Null when call {@link ExperimentsCacheImpl#get()}
     */
    @Test
    public void test_remove() {
        final Experiment experiment1 = new ExperimentDataGen().nextPersisted();
        final Experiment experiment2 = new ExperimentDataGen().nextPersisted();

        CacheLocator.getExperimentsCache().put(List.of(experiment1, experiment2));
        checkFromCacheNotNull(2);

        CacheLocator.getExperimentsCache().remove();
        checkFromCacheNull();
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

        CacheLocator.getExperimentsCache().put(List.of(experiment1, experiment2));
        checkFromCacheNotNull(2);

        CacheLocator.getExperimentsCache().clearCache();

        checkFromCacheNull();
    }

    private void checkFromCacheNotNull(final int size) {
        final List<Experiment> experiments = CacheLocator.getExperimentsCache().get();
        assertNotNull(experiments);
        assertEquals(size, experiments.size());
    }

    private void checkFromCacheNull() {
        assertNull(CacheLocator.getExperimentsCache().get());
    }

}
