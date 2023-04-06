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

    String RUNNING_EXPERIMENTS_KEY = "RUNNING_EXPERIMENTS";

    /**
     * Add a list of {@link Experiment} into the cache.
     *
     * @param name name to use as key
     * @param experiments to be stored
     */
    void put(String name, final List<Experiment> experiments);

    /**
     * Get a list of {@link Experiment} from cache by name.
     *
     * @param name
     * @return
     */
    List<Experiment> get(String name);

    /**
     * Remove a list of {@link Experiment} from cache identified by the provided name.
     *
     * @param name name to use as key
     * @return
     */
    void remove(final String name);

}
