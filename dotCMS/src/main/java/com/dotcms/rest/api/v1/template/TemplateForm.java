package com.dotcms.rest.api.v1.template;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Template Input Form
 * @author jsanca
 */
@JsonDeserialize(builder = TemplateForm.Builder.class)
public class TemplateForm  extends Validated {

    private final String siteId;
    private final String  identifier;
    private final String  inode;
    private final String  body;
    private final String  selectedimage;
    private final String  image;
    private final boolean drawed;
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
    private final String  name;
    @NotNull
    private final String  title;
    private final int     sortOrder;
    private final boolean headerCheck;
    private final boolean footerCheck;
    private final TemplateLayoutView layout;

    private TemplateForm(final Builder builder) {

        this.siteId = builder.siteId;
        this.identifier = builder.identifier;
        this.inode = builder.inode;
        this.body = builder.body;
        this.selectedimage = builder.selectedimage;
        this.image = builder.image;
        this.drawed = builder.drawed;
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
        this.name = builder.name;
        this.title = builder.title;
        this.sortOrder = builder.sortOrder;
        this.headerCheck = builder.headerCheck;
        this.footerCheck = builder.footerCheck;
        this.layout      = builder.layout;

    }

    public String getSiteId() {
        return siteId;
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

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isHeaderCheck() {
        return headerCheck;
    }

    public boolean isFooterCheck() {
        return footerCheck;
    }

    public static final class Builder {

        @JsonProperty
        private String siteId;
        @JsonProperty
        private  String identifier;
        @JsonProperty
        private  String inode;
        @JsonProperty
        private  String body;
        @JsonProperty
        private  String selectedimage;
        @JsonProperty
        private  String image;
        @JsonProperty
        private  boolean drawed;
        @JsonProperty
        private  boolean showOnMenu;
        @JsonProperty
        private  String drawedBody;
        @JsonProperty
        private  int countAddContainer;
        @JsonProperty
        private  int countContainers;
        @JsonProperty
        private  String headCode;
        @JsonProperty
        private  String theme;
        @JsonProperty
        private  String themeName;
        @JsonProperty
        private  String footer;
        @JsonProperty
        private  String friendlyName;
        @JsonProperty
        private  String header;
        @JsonProperty
        private  String name;
        @JsonProperty
        private  String title;
        @JsonProperty
        private  int sortOrder;
        @JsonProperty
        private  boolean headerCheck;
        @JsonProperty
        private  boolean footerCheck;
        @JsonProperty
        private TemplateLayoutView layout;


        public Builder siteId (final String  siteId) {

            this.siteId = siteId;
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

        public Builder  headerCheck  (final boolean headerCheck) {
            this.headerCheck = headerCheck;
            return this;
        }

        public Builder  footerCheck  (final boolean footerCheck) {
            this.footerCheck = footerCheck;
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

        public TemplateForm build() {

            return new TemplateForm(this);
        }
    }
}
