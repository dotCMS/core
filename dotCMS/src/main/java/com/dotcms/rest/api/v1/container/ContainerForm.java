package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.beans.ContainerStructure;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(builder = ContainerForm.Builder.class)
public class ContainerForm  extends Validated {

    private final String identifier;
    private final String title;
    // description
    private final String friendlyName;
    /** nullable persistent field */
    private final int maxContentlets;
    /** nullable persistent field */
    private final String code;
    private final String notes;
    private final String preLoop;
    private final String postLoop;
    private final boolean showOnMenu;
    private final int sortOrder;
    private final String sortContentletsBy;
    private final String structureInode;
    private final List<ContainerStructure> containerStructures;
    private final String owner; // dotcms 472
    private final String hostId;
    private final boolean staticify;
    private final boolean useDiv;
    private final boolean dynamic;

    private ContainerForm(final ContainerForm.Builder builder) {

        this.identifier = builder.identifier;
        this.title = builder. title;
        this.friendlyName = builder.friendlyName;
        this.maxContentlets = builder.maxContentlets;
        this.code = builder.code;
        this.notes = builder.notes;
        this.preLoop = builder.preLoop;
        this.postLoop = builder.postLoop;
        this.showOnMenu = builder.showOnMenu;
        this.sortOrder = builder.sortOrder;
        this.sortContentletsBy = builder.sortContentletsBy;
        this.structureInode = builder.structureInode;
        this.containerStructures = builder.containerStructures;
        this.owner = builder.owner;
        this.hostId = builder.hostId;
        this.staticify = builder.staticify;
        this.useDiv = builder.useDiv;
        this.dynamic = builder.dynamic;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public int getMaxContentlets() {
        return maxContentlets;
    }

    public String getCode() {
        return code;
    }

    public String getNotes() {
        return notes;
    }

    public String getPreLoop() {
        return preLoop;
    }

    public String getPostLoop() {
        return postLoop;
    }

    public boolean isShowOnMenu() {
        return showOnMenu;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getSortContentletsBy() {
        return sortContentletsBy;
    }

    public String getStructureInode() {
        return structureInode;
    }

    public List<ContainerStructure> getContainerStructures() {
        return containerStructures;
    }

    public String getOwner() {
        return owner;
    }

    public String getHostId() {
        return hostId;
    }

    public boolean isStaticify() {
        return staticify;
    }

    public boolean isUseDiv() {
        return useDiv;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public static final class Builder {


        @JsonProperty
        private  String identifier;

        @JsonProperty
        private  String title;

        // description
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

        public ContainerForm.Builder identifier (final String identifier) {
            this.identifier = identifier;
            return this;
        }

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
