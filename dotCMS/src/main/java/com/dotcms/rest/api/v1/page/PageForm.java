package com.dotcms.rest.api.v1.page;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Represents the layout of a Template in dotCMS.
 *
 * <p>All Containers that make up the structure of a Template -- along with its rows and columns --
 * are organized and transformed into an instance of this class.</p>
 *
 * @author Freddy Rodriguez
 * @since Nov 22nd, 2017
 */
@JsonDeserialize(builder = PageForm.Builder.class)
class PageForm {

    private final String themeId;
    private final String title;
    private final String siteId;
    private final TemplateLayout layout;
    private final Map<String, ContainerUUIDChanged> changes;
    private final Map<String, String> newlyContainersUUID;

    public PageForm(final String themeId, final String title, final String siteId, final TemplateLayout layout,
                    final Map<String, ContainerUUIDChanged> changes, final Map<String, String> newlyContainersUUID) {

        this.themeId = themeId;
        this.title = title;
        this.siteId = siteId;
        this.layout = layout;
        this.changes = ImmutableMap.<String, ContainerUUIDChanged> builder().putAll(changes).build();
        this.newlyContainersUUID = ImmutableMap.copyOf(newlyContainersUUID);
    }


    /**
     *
     * @return Layout's theme id
     */
    public String getThemeId() {
        return themeId;
    }

    /**
     *
     * @return Layout's title
     */
    public String getTitle() {
        return title != null ? title : Template.ANONYMOUS_PREFIX + System.currentTimeMillis();
    }

    /**
     *
     * @return Layout's site
     */
    public String getSiteId() {
        return siteId;
    }

    public boolean isAnonymousLayout() {
        return !UtilMethods.isSet(this.title);
    }

    /**
     *
     * @return TemplateLayout linked with the Template
     */
    public TemplateLayout getLayout() {
        return layout;
    }

    /**
     * Allows you to determine whether the instance ID of a given Container has changed or not based
     * on the modifications that are being persisted. This makes it easier for the API to be able to
     * update the necessary instance IDs across the page's Template so that contents are displayed
     * in the appropriate order.
     *
     * <p>For instance, if a Template has three instances of the Default Container -- "1", "2", and
     * "3" -- and instance "2" is deleted, the change list will be:</p>
     * <ul>
     *     <li>Old Instance ID = "1" / New Instance ID = "1" -- The value remains.</li>
     *     <li>Instance ID "2" is NOT present as it is the one that was removed from the Template
     *     .</li>
     *     <li>Old Instance ID = "3" / New Instance ID = "2" -- Because the second instance was
     *     removed, the third Container now takes the second instance's ID.</li>
     * </ul>
     *
     * @param identifier The ID of the Container, or its path in case it's a Container as File.
     * @param uuid       The current instance ID of the Container.
     *
     * @return The {@link ContainerUUIDChanged} instance that contains the old and new instance IDs.
     */
    public ContainerUUIDChanged getChangeInContainerInstanceIDs(final String identifier, final String uuid) {
        ContainerUUIDChanged containerUUIDChanged = this.changes.get(getChangeKey(identifier, uuid));

        if (containerUUIDChanged == null) {
            containerUUIDChanged = ContainerUUID.UUID_LEGACY_VALUE.equals(uuid) ?
                    this.changes.get(getChangeKey(identifier, ContainerUUID.UUID_START_VALUE)) :
                    this.changes.get(getChangeKey(identifier, ContainerUUID.UUID_LEGACY_VALUE));
        }

        return containerUUIDChanged;
    }

    /**
     * Returns the instance ID of a Container that has just been added to the Template. That is, a
     * Container that didn't exist and was added by this change in particular.
     *
     * @param identifier The ID or file path of the recently-added Container
     *
     * @return The instance ID of the recently-added Container.
     */
    public String getNewlyContainerUUID (String identifier) {
        return this.newlyContainersUUID.get(identifier);
    }

    /**
     * Generates the key that identifies the potential change in a Container instance ID.
     *
     * @param containerUUID The {@link ContainerUUID} object whose information may have changed..
     *
     * @return The key that identifies the potential change in a Container instance ID.
     */
    private static String getChangeKey(ContainerUUID containerUUID) {
        return getChangeKey(containerUUID.getIdentifier(), containerUUID.getUUID());
    }

    /**
     * Generates the key that identifies a potential change in a Container's instance ID.
     *
     * @param identifier The ID or file path to a given Container.
     * @param uuid       The current instance ID of the Container.
     *
     * @return The key that identifies the potential change in a Container instance ID.
     */
    private static String getChangeKey(String identifier, String uuid) {
        return String.format("%s - %s", identifier, uuid);
    }


    public static final class Builder {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @JsonProperty
        private String themeId;

        @JsonProperty
        private String title;

        @JsonProperty
        private String hostId;

        @JsonProperty(required = true)
        private Map<String, Object> layout;

        @JsonIgnore
        private Map<String, ContainerUUIDChanged> changes;

        @JsonIgnore
        private Map<String, String> newlyContainersUUID;

        public Builder() {
        }

        public Builder themeId(String themeId) {
            this.themeId = themeId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder hostId(String hostId) {
            this.hostId = hostId;
            return this;
        }

        public Builder layout(Map<String, Object> layout) {
            this.layout = layout;
            return this;
        }

        /**
         * Transforms the Template's layout as a data Map into an instance of
         * {@link TemplateLayout}.
         *
         * @return An instance of {@link TemplateLayout} that represents the Template's layout.
         *
         * @throws BadRequestException If the Template's layout is invalid or missing.
         */
        private TemplateLayout getTemplateLayout() throws BadRequestException {

            if (layout == null) {
                throw new BadRequestException("layout is required");
            }

            try {
                this.setContainersUUID();
                final String layoutString = MAPPER.writeValueAsString(layout);
                return MAPPER.readValue(layoutString, TemplateLayout.class);
            } catch (final IOException e) {
                throw new BadRequestException(e, String.format("An error occurred when processing" +
                        " the layout for Template '%s'", UtilMethods.isSet(title) ? title : "- " +
                        "Anonymous Template -"));
            }
        }

        private void setContainersUUID() {
            changes = new HashMap<>();
            newlyContainersUUID = new HashMap<>();

            final Map<String, Long> maxUUIDByContainer = new HashMap<>();

            final Map<String, Object> body = (Map<String, Object>) layout.get("body");

            if (body == null) {
                throw new BadRequestException("body attribute is required");
            }

            final List<Map<String, Map>> rows = (List<Map<String, Map>>) body.get("rows");

            if (rows == null) {
                throw new BadRequestException("body has to have at least one row");
            }

            getAllContainers().forEach(container -> setChange(maxUUIDByContainer, container));
        }

        /**
         * Loads the data Maps of every Container in the Template's layout, with its previous and
         * updated instance ID.
         *
         * @param maxInstanceIDByContainer Keeps track of the maximum instance ID that has been
         *                                 assigned to a previously added Container of the same
         *                                 type. It helps increase the instance ID one by one in
         *                                 order.
         * @param container                The current Container instance inside the Template.
         */
        private void setChange(final Map<String, Long> maxInstanceIDByContainer, final Map<String, String> container) {
            try {
                final String containerId = container.get("identifier");
                final long currentInstanceID = maxInstanceIDByContainer.get(containerId) != null ?
                        maxInstanceIDByContainer.get(containerId) : 0;
                final long nextInstanceID = currentInstanceID + 1;

                if (container.get("uuid") != null) {
                    final ContainerUUID oldContainerInstanceID = MAPPER.readValue(MAPPER.writeValueAsString(container),
                            ContainerUUID.class);
                    container.put("uuid", String.valueOf(nextInstanceID));
                    final ContainerUUID newContainerInstanceID = MAPPER.readValue(MAPPER.writeValueAsString(container),
                            ContainerUUID.class);
                    changes.put(getChangeKey(oldContainerInstanceID), new ContainerUUIDChanged(oldContainerInstanceID, newContainerInstanceID));
                } else {
                    container.put("uuid", String.valueOf(nextInstanceID));
                    newlyContainersUUID.put(containerId, String.valueOf(nextInstanceID));
                }

                maxInstanceIDByContainer.put(containerId, nextInstanceID);
            } catch (final IOException e) {
                Logger.error(this.getClass(),String.format("Failed to map changed Container instance IDs in Template " +
                                "'%s': %s", UtilMethods.isSet(title) ? title : "- Anonymous Template -",
                        ExceptionUtil.getErrorMessage(e)), e);
            }
        }

        private Stream<Map<String, String>> getAllContainers() {
            final Stream<Map<String, String>> bodyContainers =
                    ((List<Map<String, Map>>) ((Map<String, Object>) layout.get("body")).get("rows"))
                            .stream()
                            .map(row -> (List<Map<String, Map>>) row.get("columns"))
                            .flatMap(columns -> columns.stream())
                            .map(column -> (List<Map<String, String>>) column.get("containers"))
                            .flatMap(containers -> containers.stream());

            if (layout.get("sidebar") != null){
                final Stream<Map<String, String>> sidebarContainers =
                        ((List<Map<String, String>>) ((Map<String, Object>) layout.get("sidebar")).get("containers"))
                                .stream();

                return Stream.concat(sidebarContainers, bodyContainers);
            } else {
                return bodyContainers;
            }
        }

        public PageForm build(){
            return new PageForm(themeId, title, hostId, getTemplateLayout(), changes, newlyContainersUUID);
        }

    }

}