package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.tag.model.Tag;
import com.liferay.portal.model.User;

/**
 * @author Jonathan Gamba 2019-06-28
 */
public class TagDataGen extends AbstractDataGen<Tag> {

    private String name = "Tag" + System.currentTimeMillis();
    private User tagUser = user;
    private Host site = host;
    private Boolean isPersona = Boolean.FALSE;

    public TagDataGen name(final String name) {
        this.name = name;
        return this;
    }

    public TagDataGen user(final User user) {
        this.tagUser = user;
        return this;
    }

    public TagDataGen site(final Host site) {
        this.site = site;
        return this;
    }

    public TagDataGen persona(final Boolean isPersona) {
        this.isPersona = isPersona;
        return this;
    }

    @Override
    public Tag next() {

        final Tag tag = new Tag();
        tag.setTagName(name);
        tag.setUserId(tagUser.getUserId());
        tag.setHostId(site.getIdentifier());
        tag.setPersona(isPersona);

        return tag;
    }

    @WrapInTransaction
    @Override
    public Tag persist(final Tag tag) {

        Tag savedTag;

        try {
            savedTag = APILocator.getTagAPI()
                    .saveTag(tag.getTagName(), tag.getUserId(), tag.getHostId(),
                            tag.isPersona());

        } catch (Exception e) {
            throw new RuntimeException("Error persisting Tag", e);
        }
        return savedTag;
    }

    /**
     * Creates a new {@link Tag} instance and persists it in DB
     *
     * @return A new Tag instance persisted in DB
     */
    @Override
    public Tag nextPersisted() {
        return persist(next());
    }

}