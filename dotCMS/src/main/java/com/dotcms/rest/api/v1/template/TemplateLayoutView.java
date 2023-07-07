package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateLayoutView {

    private final String      width;
    private final String      title;
    private final boolean     header;
    private final boolean     footer;
    private final BodyView    body;
    private final SidebarView sidebar;

    @JsonCreator
    public TemplateLayoutView(@JsonProperty("width")   final String width,
                              @JsonProperty("title")   final String title,
                              @JsonProperty("header")  final boolean header,
                              @JsonProperty("footer")  final boolean footer,
                              @JsonProperty("body")    final BodyView body,
                              @JsonProperty("sidebar") final SidebarView sidebar) {
        this.width   = width;
        this.title   = title;
        this.header  = header;
        this.footer  = footer;
        this.body    = body;
        this.sidebar = sidebar;
    }

    public String getWidth() {
        return width;
    }

    public String getTitle() {
        return title;
    }

    public boolean isHeader() {
        return header;
    }

    public boolean isFooter() {
        return footer;
    }

    public BodyView getBody() {
        return body;
    }

    public SidebarView getSidebar() {
        return sidebar;
    }

    /**
     * This method iterates over each container in the template and assign a UUID according the
     * amount of times the container is present in the template.
     * This is for the users can have 2 diff templates with diff design but the same containers
     * so they can change the template without losing the content.
     *
     * TemplateLayoutView with the updated UUID for each container.
     */
    protected ContainerUUIDChanges updateUUIDOfContainers(){
        final ContainerUUIDChanges containerUUIDChanged = new ContainerUUIDChanges();

        final Map<String,Integer> UUIDByContainermap = new HashMap<>();

        this.getBody().getRows().forEach(
                row -> row.getColumns().stream().forEach(
                        column -> column.getContainers().stream().forEach(
                                containerUUID -> {
                                    final String[] uuidValues = setNewUUID(UUIDByContainermap,
                                            containerUUID);
                                    containerUUIDChanged.change(containerUUID.getIdentifier(), uuidValues[0], uuidValues[1]);
                                }
                        )));

        if(this.getSidebar() != null) {
            this.getSidebar().getContainers().stream().forEach(
                    containerUUID -> {
                        final String[] uuidValues = setNewUUID(UUIDByContainermap, containerUUID);
                        containerUUIDChanged.change(containerUUID.getIdentifier(), uuidValues[0], uuidValues[1]);
                    }
            );
        }

        return containerUUIDChanged;
    }

    private static String[] setNewUUID(final Map<String, Integer> UUIDByContainermap,
            final ContainerUUID containerUUID) {
        final String oldValue = containerUUID.getUUID();
        final String containerID = containerUUID.getIdentifier();
        int value = UUIDByContainermap.getOrDefault(containerID,0);
        final int newValue = value + 1;
        UUIDByContainermap.put(containerID, newValue);
        containerUUID.setUuid(UUIDByContainermap.get(containerID).toString());

        return new String[]{oldValue, String.valueOf(newValue)};
    }


    /**
     * Represent a set of UUID change when a Template's Layout is updated
     *
     */
    public static class ContainerUUIDChanges {
        final Map<String, Map<String, String>> changes = new HashMap();
        final Map<String, Collection<String>> newValues = new HashMap<>();
        public void change(final String containerId, final String oldValue, final String newValue) {
            if (ContainerUUID.UUID_DEFAULT_VALUE.equals(oldValue)) {
                final Collection<String> containerNewValues = newValues.getOrDefault(containerId,
                        new HashSet<>());
                containerNewValues.add(newValue);
                newValues.put(containerId, containerNewValues);
            } else if (!oldValue.equals(newValue)) {
                final Map<String, String> containerChanges = changes.getOrDefault(containerId,
                        new HashMap<>());
                containerChanges.put(oldValue, newValue);
                changes.put(containerId, containerChanges);
            }
        }

        /**
         * Return all the UUID that was lost in the Template's Layout update, for Example, if
         * we have a Template with a Layout equals to:
         *
         * <code>
         * {
         *     rows: [
         *      columns: [
         *          containers: [
         *              {
         *                  id: "...",
         *                  uuid: "109839"
         *              }
         *          ]
         *       ]
         *     ]
         * }
         * </code>
         *
         *  The UUID equals to 109839 is not valid for this reason if the Layout is updated this UUID
         *  is changed by 1, and then the 109839 is lost.
         *
         * @return
         */
        public Collection<ContainerUUIDChanged> lostUUIDValues(){
            final List<ContainerUUIDChanged> result = new ArrayList<>();

            for (final String containerId : changes.keySet()) {
                final Map<String, String> containerChanges = changes.get(containerId);
                final Collection<String> newValuesChanged = containerChanges.values();
                final Collection<String> containerNewValues = newValues.get(containerId);

                final List<ContainerUUIDChanged> partialResult = containerChanges.entrySet().stream()
                        .filter(entry -> !newValuesChanged.contains(entry.getKey()))
                        .filter(entry -> !UtilMethods.isSet(containerNewValues) || !containerNewValues.contains(entry.getKey()))
                        .map(entry -> new ContainerUUIDChanged(containerId, entry.getKey(),
                                entry.getValue()))
                        .collect(Collectors.toList());

                result.addAll(partialResult);
            }

            return result;
        }
    }

    /**
     * Represent a Change in just one UUID into the Template's Layout
     */
    public static class ContainerUUIDChanged {
        final String containerId;
        final String newValue;
        final String oldValue;

        public ContainerUUIDChanged(final String containerId, final String oldValue, final String newValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.containerId = containerId;
        }


    }
}
