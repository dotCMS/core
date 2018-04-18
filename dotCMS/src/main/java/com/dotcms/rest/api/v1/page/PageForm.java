package com.dotcms.rest.api.v1.page;

import java.io.IOException;
import java.util.*;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link PageResource}'s form
 */
@JsonDeserialize(builder = PageForm.Builder.class)
class PageForm {

    private final String themeId;
    private final String title;
    private final String hostId;
    private final TemplateLayout layout;
    private final List<ContainerUUIDChanged> changes;

    public PageForm(final String themeId, final String title, final String hostId, final TemplateLayout layout,
                    final List<ContainerUUIDChanged> changes) {

        this.themeId = themeId;
        this.title = title;
        this.hostId = hostId;
        this.layout = layout;
        this.changes = changes;
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

    public List<ContainerUUIDChanged> getChanges () {
        return changes != null ? changes : Collections.EMPTY_LIST;
    }

    public static final class Builder {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        @JsonProperty
        @NotNull
        private String themeId;

        @JsonProperty
        private String title;

        @JsonProperty
        @NotNull
        private String hostId;

        @JsonProperty
        @NotNull
        private Map<String, Object> layout;

        @JsonIgnore
        private List<ContainerUUIDChanged> changes;

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

            try {
                this.setContainersUUID();
                final String layoutString = MAPPER.writeValueAsString(layout);
                return MAPPER.readValue(layoutString, TemplateLayout.class);
            } catch (IOException e) {
                throw new BadRequestException(e, "An error occurred when proccessing the JSON request");
            }
        }

        private void setContainersUUID() {
            final List<ContainerUUIDChanged> changes = new ArrayList<>();
            final Map<String, Long> maxUUIDByContainer = new HashMap<>();
            final List<Map<String, Map>> rows = (List<Map<String, Map>>) ((Map<String, Object>) layout.get("body")).get("rows");

            rows.stream()
                    .map(row -> (List<Map<String, Map>>) row.get("columns"))
                    .flatMap(columns -> columns.stream())
                    .map(column -> (List<Map<String, String>>) column.get("containers"))
                    .flatMap(containers -> containers.stream())
                    .forEach(container -> {
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
                                changes.add(new ContainerUUIDChanged(oldContainerUUID, newContainerUUID));
                            } else {
                                container.put("uuid", String.valueOf(nextUUID));
                            }

                            maxUUIDByContainer.put(containerId, nextUUID);

                        } catch (IOException e) {
                            Logger.error(this.getClass(),"Exception on setContainersUUID exception message: " + e.getMessage(), e);
                        }
                    });

            this.changes = changes;
        }

        public PageForm build(){
            return new PageForm(themeId, title, hostId, getTemplateLayout(), changes);
        }
    }
}
