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
     * Add a {@link Experiment} into the cache.
     *
     * @param experiment to be stored
     */
    void put(Experiment experiment);

    /**
     * Get a list of {@link Experiment} from cache by name.
     *
     * @param experimentId experiment id to be used as cache key
     * @return
     */
    Experiment get(String experimentId);

    /**
     * Add a list of {@link Experiment} into the cache.
     *
     * @param key experiments cache key
     * @param experiments to be stored
     */
    void putList(String key, List<Experiment> experiments);

    /**
     * Get a list of {@link Experiment} from cache by name.
     *
     * @param key experiments cache key
     * @return
     */
    List<Experiment> getList(String key);

    /**
     * Remove a list of {@link Experiment} from cache identified by the provided name.
     *
     */
    void removeList(final String key);

}
