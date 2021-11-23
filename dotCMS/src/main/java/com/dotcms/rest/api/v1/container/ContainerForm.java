package com.dotcms.rest.api.v1.container;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.template.TemplateForm;
import com.dotcms.rest.api.v1.template.TemplateLayoutView;
import com.dotmarketing.beans.ContainerStructure;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(builder = ContainerForm.Builder.class)
public class ContainerForm  extends Validated {

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

    private ContainerForm(final ContainerForm.Builder builder) {

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
        private  String title;


        @JsonProperty
        private  String friendlyName;

        /** nullable persistent field */
        @JsonProperty
        private int maxContentlets;

        /** nullable persistent field */
        @JsonProperty
        private String code;

        @JsonProperty
        private String notes;

        @JsonProperty
        private String preLoop;

        @JsonProperty
        private String postLoop;

        @JsonProperty
        private  boolean showOnMenu;

        @JsonProperty
        private  int sortOrder;

        @JsonProperty
        private String sortContentletsBy;

        @JsonProperty
        private String structureInode;

        @JsonProperty
        private List<ContainerStructure> containerStructures;

        @JsonProperty
        private String owner; // dotcms 472


        @JsonProperty
        private String hostId;

        // todo: what is that?
        private boolean staticify;
        private boolean useDiv;
        private boolean dynamic;

        public ContainerForm.Builder title (final String title) {
            this.title = title;
            return this;
        }

        public ContainerForm.Builder showOnMenu (final boolean showOnMenu) {
            this.showOnMenu = showOnMenu;
            return this;
        }

        public ContainerForm.Builder friendlyName (final String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public ContainerForm.Builder maxContentlets (final int maxContentlets) {
            this.maxContentlets = maxContentlets;
            return this;
        }

        public ContainerForm.Builder code (final String code) {
            this.code = code;
            return this;
        }
        public ContainerForm.Builder notes (final String notes) {
            this.notes = notes;
            return this;
        }

        public ContainerForm.Builder sortContentletsBy (final String showOnMenu) {
            this.sortContentletsBy = sortContentletsBy;
            return this;
        }

        public ContainerForm.Builder structureInode (final String structureInode) {
            this.structureInode = structureInode;
            return this;
        }

        public ContainerForm.Builder preLoop (final String preLoop) {
            this.preLoop = preLoop;
            return this;
        }

        public ContainerForm.Builder postLoop (final String postLoop) {
            this.postLoop = postLoop;
            return this;
        }

        public ContainerForm.Builder containerStructures (final List<ContainerStructure> containerStructures) {
            this.containerStructures = containerStructures;
            return this;
        }

        public ContainerForm.Builder sortOrder (final int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public ContainerForm.Builder owner (final String owner) {
            this.owner = owner;
            return this;
        }

        public ContainerForm.Builder hostId (final String hostId) {
            this.hostId = hostId;
            return this;
        }

        public ContainerForm build() {

            return new ContainerForm(this);
        }
    }
}
