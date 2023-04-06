package com.dotcms.experiments.business;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.DotPreconditions;
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
    public void put(final String name, final List<Experiment> experiments) {
        DotPreconditions.checkNotNull(name);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(name, experiments, getPrimaryGroup());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment> get(final String name) {
        DotPreconditions.checkNotNull(name);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        try {
            @SuppressWarnings("unchecked")
            final List<Experiment> experiments = (List<Experiment>) cache.get(name, getPrimaryGroup());
            return experiments;
        } catch (DotCacheException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final String name) {
        DotPreconditions.checkNotNull(name);

        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(name, getPrimaryGroup());
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
