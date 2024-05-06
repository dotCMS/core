package com.dotcms.datagen;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.templates.design.bean.*;

import javax.swing.*;
import java.util.*;

public class TemplateLayoutDataGen  {

    private List<TemplateLayoutRow> rows;

    private List<TemplateLayoutColumn> currentColumns;

    private Map<String, List<String>> containersIds = new HashMap<>();
    final Map<String, List<String>> containersIdsInSidebar = new HashMap<>();
    private int currentColumnWidthPercent = 100;

    public static TemplateLayoutDataGen get(){
        return new TemplateLayoutDataGen();
    }

    public TemplateLayoutDataGen withContainer(final String identifier){
        return withContainer(identifier, null);
    }

    public TemplateLayoutDataGen withContainer(final String identifier, final String UUID){
        List<String> uuids = containersIds.get(identifier);

        if (uuids == null) {
            uuids = new ArrayList<>();
        }

        uuids.add(UUID == null ? ContainerUUID.UUID_START_VALUE : UUID);
        containersIds.put(identifier, uuids);
        return this;
    }

    public TemplateLayoutDataGen withContainerInSidebar(final String identifier, final String UUID){
        List<String> uuids = containersIdsInSidebar.get(identifier);

        if (uuids == null) {
            uuids = new ArrayList<>();
        }

        uuids.add(UUID == null ? ContainerUUID.UUID_START_VALUE : UUID);
        containersIdsInSidebar.put(identifier, uuids);
        return this;
    }

    public TemplateLayout next() {

        final List<ContainerUUID> containersInSidebar = createContainerUUIDS(containersIdsInSidebar);
        final List<TemplateLayoutRow> innerRows = rows == null ? getDefaultRow() : getRows();

        final Body body = new Body(innerRows);
        final TemplateLayout templateLayout = new TemplateLayout();
        templateLayout.setBody(body);

        final Sidebar sidebar = new Sidebar(containersInSidebar, "left", "20", 20);
        templateLayout.setSidebar(sidebar);

        return templateLayout;
    }

    private List<TemplateLayoutRow> getRows() {
        createNewRow();
        return rows;
    }

    private List<TemplateLayoutRow> getDefaultRow() {
        final List<TemplateLayoutRow> rows = new ArrayList<>();

        final List<ContainerUUID> containers = createContainerUUIDS(containersIds);

        final List<TemplateLayoutColumn> columns = new ArrayList<>();
        columns.add(new TemplateLayoutColumn(containers, 100, 1, null));

        rows.add(new TemplateLayoutRow(columns, null));

        return rows;
    }

    private static List<ContainerUUID> createContainerUUIDS(Map<String, List<String>> containersIds) {
        final List<ContainerUUID> containers = new ArrayList<>();

        for (final String containersId : containersIds.keySet()) {
            final List<String> uuids = containersIds.get(containersId);

            for (final String uuid : uuids) {
                containers.add(new ContainerUUID(containersId, uuid));
            }
        }
        return containers;
    }

    public TemplateLayoutDataGen withContainer(final Container container, final String UUID) {
        final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
        return withContainer(fileAssetContainerUtil.isFileAssetContainer(container) ?
                fileAssetContainerUtil.getFullPath((FileAssetContainer) container) : container.getIdentifier(), UUID);
    }

    public TemplateLayoutDataGen withContainer(final Container container) {
        return withContainer(container, null);
    }

    public TemplateLayoutDataGen addRow() {

        if (rows ==  null) {
            rows = new ArrayList<>();
            currentColumns = new ArrayList<>();
        } else {
            createNewRow();
        }

        return this;
    }

    private void createNewRow() {
        addNewColumn();

        rows.add(new TemplateLayoutRow(currentColumns, null));
        currentColumns = new ArrayList<>();
    }

    public TemplateLayoutDataGen addColumn(final int widthPercent) {
        if (containersIds != null && !containersIds.isEmpty()) {
            addNewColumn();
        }

        currentColumnWidthPercent = widthPercent;
        return this;
    }

    private void addNewColumn() {
        final List<ContainerUUID> containers = createContainerUUIDS(containersIds);
        currentColumns.add(new TemplateLayoutColumn(containers, currentColumnWidthPercent, 1, null));

        containersIds = new HashMap<>();
    }
}
