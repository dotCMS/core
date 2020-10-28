package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;

/**
 * @author Jonathan Gamba 2019-05-28
 */
public class SiteDataGen extends AbstractDataGen<Host> {

    private final long currentTime = System.currentTimeMillis();

    private String name = "test" + currentTime + ".dotcms.com";
    private String aliases;
    private boolean isDefault;

    public SiteDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public SiteDataGen aliases(final String aliases) {
        this.aliases = aliases;
        return this;
    }

    public SiteDataGen setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    @Override
    public Host next() {

        final Host site = new Host();
        site.setHostname(name);
        site.setDefault(isDefault);
        site.setLanguageId(language.getId());
        site.setIndexPolicy(IndexPolicy.WAIT_FOR);
        site.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        if (aliases != null) {
            site.setAliases(aliases);
        }

        return site;
    }

    @WrapInTransaction
    public Host persist(final Host site, boolean publish) {
        try {

            site.setIndexPolicy(IndexPolicy.WAIT_FOR);
            site.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            site.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
            final Host newSite = APILocator.getHostAPI().save(site, user, false);
            if (publish) {
                newSite.setIndexPolicy(IndexPolicy.WAIT_FOR);
                newSite.setBoolProperty(Contentlet.IS_TEST_MODE, true);
                newSite.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
                APILocator.getHostAPI().publish(newSite, user, false);
            }

            return newSite;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist Host.", e);
        }
    }

    @Override
    public Host persist(final Host site) {
        return persist(site, true);
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

    /**
     * Creates a new {@link Host} instance and persists it in DB
     *
     * @return A new Role instance persisted in DB
     */
    public Host nextPersisted(boolean publish) {
        return persist(next(), publish);
    }

}