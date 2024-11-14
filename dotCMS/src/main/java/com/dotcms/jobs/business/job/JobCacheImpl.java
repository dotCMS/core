package com.dotcms.jobs.business.job;

import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class JobCacheImpl implements JobCache {

    static final String JOB_BY_ID = "JOB_BY_ID_";
    static final String JOB_STATE_BY_ID = "JOB_STATE_BY_ID_";

    @Override
    public String getPrimaryGroup() {
        return "JobCacheImpl";
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    private String getKeyById(final String id) {
        return JOB_BY_ID + id;
    }

    private String getStateKeyById(final String id) {
        return JOB_STATE_BY_ID + id;
    }

    @Override
    public void put(final Job job) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(getKeyById(job.id()), job, getPrimaryGroup());
    }

    @Override
    public void putState(final String jobId, final JobState jobState) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.put(getStateKeyById(jobId), jobState, getPrimaryGroup());
    }

    @Override
    public Job get(final String jobId) {
        DotPreconditions.checkNotNull(jobId);

        try {
            DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
            return (Job) cache.get(getKeyById(jobId), getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    @Override
    public JobState getState(final String jobId) {
        DotPreconditions.checkNotNull(jobId);

        try {
            DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
            return (JobState) cache.get(getStateKeyById(jobId), getPrimaryGroup());
        } catch (DotCacheException e) {
            return null;
        }
    }

    @Override
    public void remove(final Job job) {
        remove(job.id());
    }

    @Override
    public void remove(final String jobId) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(getKeyById(jobId), getPrimaryGroup());
    }

    @Override
    public void removeState(final String jobId) {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.remove(getStateKeyById(jobId), getPrimaryGroup());
    }

    @Override
    public void clearCache() {
        DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        cache.flushGroup(getPrimaryGroup());
    }

}
