package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class holds a view with all attributes of the
 * {@link com.dotmarketing.portlets.templates.model.Template} class for the REST Endpoint to provide
 * it as part of the JSON response.
 *
 * @author Jonathan Sanchez
 * @since Aug 31st, 2020
 */
public class TemplateView {

    private final String  identifier;
    private final String  inode;
    private final String  categoryId;
    private final String  body;
    private final String  selectedimage;
    private final String  image;
    private final boolean drawed;
    private final boolean deleted;
    private final boolean live;
    private final boolean locked;
    private final String lockedBy;
    private final boolean working;
    private final boolean isNew;
    private final boolean hasLiveVersion;
    private final boolean showOnMenu;
    private final String  drawedBody;
    private final int     countAddContainer;
    private final int     countContainers;
    private final String  headCode;
    private final String  theme;
    private final String  themeName;
    private final String  footer;
    private final String  friendlyName;
    private final String  header;
    private final Date    modDate;
    private final String  modUser;
    private final String  owner;
    private final String  name;
    private final String  title;
    private final int     sortOrder;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean canPublish;
    private final TemplateLayoutView layout;
    private final Set<ContainerView> containers;
    private final ThemeView themeInfo;

    private final String hostName;
    private final String hostId;
    private final String fullTitle;
    private final String htmlTitle;

    private TemplateView(final Builder builder) {
        this.identifier = builder.identifier;
        this.inode = builder.inode;
        this.categoryId = builder.categoryId;
        this.body = builder.body;
        this.selectedimage = builder.selectedimage;
        this.image = builder.image;
        this.drawed = builder.drawed;
        this.deleted = builder.deleted;
        this.live = builder.live;
        this.locked = builder.locked;
        this.lockedBy = builder.lockedBy;
        this.working = builder.working;
        this.isNew = builder.isNew;
        this.hasLiveVersion = builder.hasLiveVersion;
        this.showOnMenu = builder.showOnMenu;
        this.drawedBody = builder.drawedBody;
        this.countAddContainer = builder.countAddContainer;
        this.countContainers = builder.countContainers;
        this.headCode = builder.headCode;
        this.theme = builder.theme;
        this.themeName = builder.themeName;
        this.footer = builder.footer;
        this.friendlyName = builder.friendlyName;
        this.header = builder.header;
        this.modDate = builder.modDate;
        this.modUser = builder.modUser;
        this.owner = builder.owner;
        this.name = builder.name;
        this.title = builder.title;
        this.sortOrder = builder.sortOrder;
        this.canRead = builder.canRead;
        this.canWrite = builder.canWrite;
        this.canPublish = builder.canPublish;
        this.layout = builder.layout;
        this.containers = builder.containers;
        this.themeInfo = builder.themeInfo;
        this.hostName = builder.hostName;
        this.hostId = builder.hostId;
        this.fullTitle = builder.fullTitle;
        this.htmlTitle = builder.htmlTitle;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostId() {
        return hostId;
    }

    public String getFullTitle() {
        return fullTitle;
    }

    public String getHtmlTitle() {
        return htmlTitle;
    }

    public Map<String, ContainerView> getContainers() {

        final Map<String, ContainerView> containerMap = new HashMap<>();

        containers.forEach(containerView -> {

            final Container container = containerView.getContainer();
            final String containerId  = container instanceof FileAssetContainer?
                    FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) container):
                    container.getIdentifier();

            containerMap.put(containerId, containerView);
        });

        return containerMap;
    }

    public TemplateLayoutView getLayout() {
        return layout;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getBody() {
        return body;
    }

    public String getSelectedimage() {
        return selectedimage;
    }

    public String getImage() {
        return image;
    }

    public boolean isDrawed() {
        return drawed;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isLive() {
        return live;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getLockedBy(){ return lockedBy;}

    public boolean isWorking() {
        return working;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isHasLiveVersion() {
        return hasLiveVersion;
    }

    public boolean isShowOnMenu() {
        return showOnMenu;
    }

    public String getDrawedBody() {
        return drawedBody;
    }

    public int getCountAddContainer() {
        return countAddContainer;
    }

    public int getCountContainers() {
        return countContainers;
    }

    public String getHeadCode() {
        return headCode;
    }

    public String getTheme() {
        return theme;
    }

    public String getThemeName() {
        return themeName;
    }

    public String getFooter() {
        return footer;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getHeader() {
        return header;
    }

    public Date getModDate() {
        return modDate;
    }

    public String getModUser() {
        return modUser;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public boolean isCanPublish() {
        return canPublish;
    }

    public ThemeView getThemeInfo() {
        return themeInfo;
    }

    public static final class Builder {

        private  String identifier;
        private  String inode;
        private  String categoryId;
        private  String body;
        private  String selectedimage;
        private  String image;
        private  boolean drawed;
        private  boolean deleted;
        private  boolean live;
        private  boolean locked;
        private String lockedBy;
        private  boolean working;
        private  boolean isNew;
        private  boolean hasLiveVersion;
        private  boolean showOnMenu;
        private  String drawedBody;
        private  int countAddContainer;
        private  int countContainers;
        private  String headCode;
        private  String theme;
        private  String themeName;
        private  String footer;
        private  String friendlyName;
        private  String header;
        private  Date modDate;
        private  String modUser;
        private  String owner;
        private  String name;
        private  String title;
        private  int sortOrder;
        private  boolean canRead;
        private  boolean canWrite;
        private  boolean canPublish;
        private  TemplateLayoutView layout;
        private  Set<ContainerView> containers;
        private ThemeView themeInfo;

        private String hostName;
        private String hostId;
        private String fullTitle;
        private String htmlTitle;

        public Builder hostName(final String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder hostId(final String hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder fullTitle(final String fullTitle) {
            this.fullTitle = fullTitle;
            return this;
        }

        public Builder htmlTitle(final String htmlTitle) {
            this.htmlTitle = htmlTitle;
            return this;
        }

        public Builder containers (final Set<ContainerView> containers) {

            this.containers = containers;
            return this;
        }

        public Builder layout (final TemplateLayoutView layout) {

            this.layout = layout;
            return this;
        }

        public Builder identifier (final String identifier) {

            this.identifier = identifier;
            return this;
        }

        public Builder  inode (final String inode) {
            this.inode = inode;
            return this;
        }

        public Builder  categoryId (final String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder  body (final String body) {
            this.body = body;
            return this;
        }

        public Builder  selectedimage (final String selectedimage) {
            this.selectedimage = selectedimage;
            return this;
        }

        public Builder  image (final String image) {
            this.image = image;
            return this;
        }


        public Builder  drawed (final boolean drawed) {
            this.drawed = drawed;
            return this;
        }

        public Builder  deleted (final boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder  live (final boolean live) {
            this.live = live;
            return this;
        }

        public Builder  locked (final boolean locked) {
            this.locked = locked;
            return this;
        }

        public Builder lockedBy(final String lockedBy){
            this.lockedBy = lockedBy;
            return this;
        }


        public Builder  working (final boolean working) {
            this.working = working;
            return this;
        }

        public Builder  isNew (final boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public Builder  showOnMenu (final boolean showOnMenu) {
            this.showOnMenu = showOnMenu;
            return this;
        }

        public Builder  drawedBody (final String drawedBody) {

            this.drawedBody = drawedBody;
            return this;
        }

        public Builder  countAddContainer (final int countAddContainer) {

            this.countAddContainer = countAddContainer;
            return this;
        }

        public Builder  countContainers (final int countContainers) {
            this.countContainers = countContainers;
            return this;
        }

        public Builder  headCode (final String headCode) {
            this.headCode = headCode;
            return this;
        }

        public Builder  theme (final String theme) {
            this.theme = theme;
            return this;
        }

        public Builder  themeName (final String themeName) {
            this.themeName = themeName;
            return this;
        }

        public Builder  footer (final String footer) {
            this.footer = footer;
            return this;
        }

        public Builder  friendlyName (final String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder  header (final String header) {
            this.header = header;
            return this;
        }

        public Builder  modDate (final Date modDate) {
            this.modDate = modDate;
            return this;
        }

        public Builder  modUser (final String modUser) {
            this.modUser = modUser;
            return this;
        }

        public Builder  owner (final String owner) {
            this.owner = owner;
            return this;
        }

        public Builder  name (final String name) {
            this.name = name;
            return this;
        }

        public Builder  title (final String title) {
            this.title = title;
            return this;
        }

        public Builder  sortOrder (final int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder hasLiveVersion(final boolean hasLiveVersion) {
            this.hasLiveVersion = hasLiveVersion;
            return this;
        }

        public Builder canRead(final boolean canRead) {
            this.canRead = canRead;
            return this;
        }

        public Builder canWrite(final boolean canWrite) {
            this.canWrite = canWrite;
            return this;
        }

        public Builder canPublish(final boolean canPublish) {
            this.canPublish = canPublish;
            return this;
        }

        public Builder themeInfo(final ThemeView themeObj) {
            this.themeInfo = themeObj;
            return this;
        }

        public TemplateView build() {
            return new TemplateView(this);
        }

    }

}
