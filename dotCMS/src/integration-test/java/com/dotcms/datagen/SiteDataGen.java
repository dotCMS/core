package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;

/**
 * @author Jonathan Gamba 2019-05-28
 */
public class SiteDataGen extends AbstractDataGen<Host> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "test" + currentTime + ".dotcms.com";

    public SiteDataGen name(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public Host next() {

        final Host site = new Host();
        site.setHostname(name);
        site.setDefault(false);
        site.setLanguageId(language.getId());
        site.setIndexPolicy(IndexPolicy.FORCE);

        return site;
    }

    @Override
    public Host persist(final Host site) {
        try {
            return APILocator.getHostAPI().save(site, user, false);
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist Role.", e);
        }
    }

    /**
     * Creates a new {@link Host} instance and persists it in DB
     *
     * @return A new Role instance persisted in DB
     */
    @Override
    public Host nextPersisted() {
        return persist(next());
    }

}