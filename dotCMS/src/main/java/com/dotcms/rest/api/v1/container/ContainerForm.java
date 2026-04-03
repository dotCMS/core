package com.dotcms.rest.api.v1.container;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.beans.ContainerStructure;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Form to create a Container
 * @author jsanca
 */
@JsonDeserialize(builder = ContainerForm.Builder.class)
@Schema(description = "Form used to create or update a Container in dotCMS")
public class ContainerForm  extends Validated {

    @Schema(description = "Identifier of the container. Required for updates, ignored on creation.")
    private final String identifier;

    @Schema(description = "Title of the container", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String title;

    @Schema(description = "Description of the container (displayed as 'Description' in the UI). "
            + "Also accepts 'description' as an alias field name in JSON.")
    private final String friendlyName;

    @Schema(description = "Maximum number of contentlets this container can hold. Set to 0 for no limit.")
    private final int maxContentlets;

    @Schema(description = "Velocity/code used to render the container's content. Used when maxContentlets is 0.")
    private final String code;

    @Schema(description = "Internal notes about this container (not displayed to end users)")
    private final String notes;

    @Schema(description = "Velocity code executed before the content loop")
    private final String preLoop;

    @Schema(description = "Velocity code executed after the content loop")
    private final String postLoop;

    @Schema(description = "Whether this container should appear in navigation menus")
    private final boolean showOnMenu;

    @Schema(description = "Sort order for display purposes")
    private final int sortOrder;

    @Schema(description = "Field name used to sort contentlets within this container")
    private final String sortContentletsBy;

    @Schema(description = "Inode of the content type (structure) associated with this container. "
            + "This is a legacy field; prefer using containerStructures for multiple content type associations.")
    private final String structureInode;

    @Schema(description = "List of content type associations for this container. Each entry maps a content type "
            + "to rendering code. The 'id' field is ignored on input and auto-generated server-side. "
            + "Only 'structureId' and 'code' are required.")
    private final List<ContainerStructure> containerStructures;

    @Schema(description = "Owner user ID of the container")
    private final String owner;

    @Schema(description = "Identifier of the site (host) where this container should be created or moved to. "
            + "If not provided, the container is assigned to the site resolved from the current HTTP request context.")
    private final String hostId;

    @Schema(description = "Whether the container output should be staticified (cached as static HTML)")
    private final boolean staticify;

    @Schema(description = "Whether to wrap content in a div element")
    private final boolean useDiv;

    @Schema(description = "Whether this container uses dynamic content type resolution")
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

        @JsonProperty
        @JsonAlias("description")
        private  String friendlyName;

        @JsonProperty
        private int maxContentlets;

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
        private String owner;


        @JsonProperty
        private String hostId;

        @JsonProperty
        private boolean staticify;
        @JsonProperty
        private boolean useDiv;
        @JsonProperty
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

        public ContainerForm.Builder sortContentletsBy (final String sortContentletsBy) {
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
