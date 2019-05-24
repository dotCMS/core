package com.dotcms.rest.api.v1.page;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

/**
 * {@link PageResource}'s form
 */
@JsonDeserialize(builder = PageForm.Builder.class)
class PageForm {

    private final String themeId;
    private final String title;
    private final String hostId;
    private final TemplateLayout layout;
    private final Map<String, ContainerUUIDChanged> changes;
    private final Map<String, String> newlyContainersUUID;

    public PageForm(final String themeId, final String title, final String hostId, final TemplateLayout layout,
                    final Map<String, ContainerUUIDChanged> changes, Map<String, String> newlyContainersUUID) {

        this.themeId = themeId;
        this.title = title;
        this.hostId = hostId;
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
     * @return Layout's host
     */
    public String getHostId() {
        return hostId;
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

    public ContainerUUIDChanged getChange (String identifier, String uuid) {
        ContainerUUIDChanged containerUUIDChanged = this.changes.get(getChangeKey(identifier, uuid));

        if (containerUUIDChanged == null) {
            containerUUIDChanged = ContainerUUID.UUID_LEGACY_VALUE.equals(uuid) ?
                    this.changes.get(getChangeKey(identifier, ContainerUUID.UUID_START_VALUE)) :
                    this.changes.get(getChangeKey(identifier, ContainerUUID.UUID_LEGACY_VALUE));
        }

        return containerUUIDChanged;
    }

    public String getNewlyContainerUUID (String identifier) {
        return this.newlyContainersUUID.get(identifier);
    }

    private static String getChangeKey(ContainerUUID containerUUID) {
        return getChangeKey(containerUUID.getIdentifier(), containerUUID.getUUID());
    }

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

        private TemplateLayout getTemplateLayout() throws BadRequestException {

            if (layout == null) {
                throw new BadRequestException("layout is required");
            }

            try {
                this.setContainersUUID();
                final String layoutString = MAPPER.writeValueAsString(layout);
                return MAPPER.readValue(layoutString, TemplateLayout.class);
            } catch (IOException e) {
                throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
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

        private void setChange(Map<String, Long> maxUUIDByContainer, Map<String, String> container) {
            try {
                final String containerId = container.get("identifier");
                final long currentUUID = maxUUIDByContainer.get(containerId) != null ?
                        maxUUIDByContainer.get(containerId) : 0;
                final long nextUUID = currentUUID + 1;

                if (container.get("uuid") != null) {
                    final ContainerUUID oldContainerUUID = MAPPER.readValue(MAPPER.writeValueAsString(container),
                            ContainerUUID.class);
                    container.put("uuid", String.valueOf(nextUUID));
                    final ContainerUUID newContainerUUID = MAPPER.readValue(MAPPER.writeValueAsString(container),
                            ContainerUUID.class);

                    String changeKey = getChangeKey(oldContainerUUID);
                    changes.put(changeKey, new ContainerUUIDChanged(oldContainerUUID, newContainerUUID));
                } else {
                    container.put("uuid", String.valueOf(nextUUID));
                    newlyContainersUUID.put(containerId, String.valueOf(nextUUID));
                }

                maxUUIDByContainer.put(containerId, nextUUID);

            } catch (IOException e) {
                Logger.error(this.getClass(),"Exception on setContainersUUID exception message: " + e.getMessage(), e);
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
