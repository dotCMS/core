package com.dotcms.datagen;

import java.util.Date;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

/**
 * @author Jonathan Gamba 2019-06-05
 */
public class LinkDataGen extends AbstractDataGen<Link> {

    private String title = "LinkTitle" + System.currentTimeMillis();
    private String friendlyName = title;
    private String target = "_blank";
    private String hostId = host.getIdentifier();
    private User owner = user;
    private String linkType = LinkType.EXTERNAL.toString();
    private String protocol = "https://";
    private String url = "www.google.com";
    private String internalLinkIdentifier = StringPool.BLANK;
    private boolean showOnMenu=false;
    
    public LinkDataGen() {
        this.folder = new FolderDataGen().nextPersisted();
    }

    public LinkDataGen(Folder parent) {
        this.folder = parent;

    }

    @SuppressWarnings("unused")
    public LinkDataGen title(String title) {
        this.title = title;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen friendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
    }
    
    @SuppressWarnings("unused")
    public LinkDataGen showOnMenu(boolean showOnMenu) {
        this.showOnMenu = showOnMenu;
        return this;
    }
    
    @SuppressWarnings("unused")
    public LinkDataGen target(String target) {
        this.target = target;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen hostId(String hostId) {
        this.hostId = hostId;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen owner(User owner) {
        this.owner = owner;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen linkType(String linkType) {
        this.linkType = linkType;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen url(String url) {
        this.url = url;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen internalLinkIdentifier(String internalLinkIdentifier) {
        this.internalLinkIdentifier = internalLinkIdentifier;
        return this;
    }

    @SuppressWarnings("unused")
    public LinkDataGen parent(Folder parent) {
        this.folder = parent;
        return this;
    }

    @Override
    public Link next() {

        Link link = new Link();
        link.setTitle(title);
        link.setFriendlyName(friendlyName);
        link.setParent(folder.getInode());
        link.setTarget(target);
        link.setHostId(hostId);
        link.setOwner(owner.getUserId());
        link.setModUser(user.getUserId());
        link.setLinkType(linkType);
        link.setInternalLinkIdentifier(internalLinkIdentifier);
        link.setProtocal(protocol);
        link.setUrl(url);
        link.setShowOnMenu(showOnMenu);
        link.setModDate(new Date());
        return link;
    }

    @WrapInTransaction
    @Override
    public Link persist(final Link link) {
        try {
            APILocator.getMenuLinkAPI().save(link, folder, user, false);
            return link;
        } catch (Exception e) {
            throw new RuntimeException("Unable to persist link.", e);
        }
    }

    /**
     * Creates a new {@link Link} instance and persists it in DB
     *
     * @return A new Link instance persisted in DB
     */
    @Override
    public Link nextPersisted() {
        return persist(next());
    }

}