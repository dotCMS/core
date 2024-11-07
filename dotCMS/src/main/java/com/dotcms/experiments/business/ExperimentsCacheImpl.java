package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

import java.util.List;

/**
 * Cache for {@link Experiment}
 *
 * @author vico
 */
public class ExperimentsCacheImpl implements ExperimentsCache {

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(Experiment experiment) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        experiment.id().ifPresent(id -> cache.put(id, experiment, getPrimaryGroup()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Experiment get(String experimentId) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            return (Experiment) cache.get(experimentId, getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putList(final String key, final List<Experiment> experiments) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(key, experiments, getPrimaryGroup());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment> getList(final String key) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            @SuppressWarnings("unchecked")
            final List<Experiment> experiments = (List<Experiment>) cache.get(key, getPrimaryGroup());
            return experiments;
        } catch (DotCacheException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeList(final String key) {

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(key, getPrimaryGroup());
    }

    /**
     * Use to get the primary group/region for the concrete cache.
     *
     * @return String
     */
    @Override
    public String getPrimaryGroup() {
        return getClass().getSimpleName();
    }

    /**
     * Use to get all groups the concrete cache belongs to.
     *
     * @return String[]
     */
    @Override
    public String[] getGroups() {
        return new String[0];
    }

    /**
     * Clears experiment cache.
     */
    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.flushGroup(getPrimaryGroup());
    }

}
