package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.Cachable;

import java.util.List;

/**
 * Cache for {@link Experiment}
 *
 * @author vico
 */
public interface ExperimentsCache extends Cachable {

    String CACHED_EXPERIMENTS_KEY = "CACHED_EXPERIMENTS";

    /**
     * Add a list of {@link Experiment} into the cache.
     *
     * @param experiments to be stored
     */
    void put(final List<Experiment> experiments);

    /**
     * Get a list of {@link Experiment} from cache by name.
     *
     * @return
     */
    List<Experiment> get(String key);

    /**
     * Remove a list of {@link Experiment} from cache identified by the provided name.
     *
     * @return
     */
    void remove();

}
