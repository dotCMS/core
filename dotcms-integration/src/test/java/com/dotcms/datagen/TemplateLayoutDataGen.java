package com.dotcms.datagen;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.templates.design.bean.*;

import java.util.*;

import static com.dotcms.util.CollectionsUtils.list;

public class TemplateLayoutDataGen  {

    private List<TemplateLayoutRow> rows;

    private List<TemplateLayoutColumn> currentColumns;

    private List<ContainerUUID> containersIds = new ArrayList<>();
    private final List<ContainerUUID> containersIdsInSidebar = new ArrayList<>();
    private int currentColumnWidthPercent = 100;
    private int version = 1;

    public static TemplateLayoutDataGen get(){
        return new TemplateLayoutDataGen();
    }

    public TemplateLayoutDataGen withContainer(final String identifier){
        return withContainer(identifier, null);
    }

    public TemplateLayoutDataGen withContainer(final String identifier, final String UUID){
        return withContainer(identifier, UUID, list(UUID));
    }

    public TemplateLayoutDataGen withContainer(final String identifier, final String UUID, final List<String> uuidsHistory){

        containersIds.add(UUID == null ? new ContainerUUID(identifier, ContainerUUID.UUID_START_VALUE, uuidsHistory) :
                new ContainerUUID(identifier, UUID, uuidsHistory));
        return this;
    }

    public TemplateLayoutDataGen withContainerInSidebar(final String identifier, final String UUID){

        containersIdsInSidebar.add(UUID == null ? new ContainerUUID(identifier, ContainerUUID.UUID_START_VALUE, list(UUID)) :
                new ContainerUUID(identifier, UUID, list(UUID)));

        return this;
    }

    public TemplateLayout next() {

        final List<TemplateLayoutRow> innerRows = rows == null ? getDefaultRow() : getRows();

        final Body body = new Body(innerRows);
        final TemplateLayout templateLayout = new TemplateLayout();
        templateLayout.setBody(body);
        templateLayout.setVersion(version);

        final Sidebar sidebar = new Sidebar(containersIdsInSidebar, "left", "20", 20);
        templateLayout.setSidebar(sidebar);

        return templateLayout;
    }

    private List<TemplateLayoutRow> getRows() {
        createNewRow();
        return rows;
    }

    private List<TemplateLayoutRow> getDefaultRow() {
        final List<TemplateLayoutRow> rows = new ArrayList<>();

        final List<TemplateLayoutColumn> columns = new ArrayList<>();
        columns.add(new TemplateLayoutColumn(containersIds, 100, 1, null));

        rows.add(new TemplateLayoutRow(columns, null));

        return rows;
    }

    public TemplateLayoutDataGen withContainer(final Container container, final String UUID, final List<String> uuidsHistory) {
        final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
        return withContainer(fileAssetContainerUtil.isFileAssetContainer(container) ?
                fileAssetContainerUtil.getFullPath((FileAssetContainer) container) : container.getIdentifier(), UUID, uuidsHistory);
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
        currentColumns.add(new TemplateLayoutColumn(containersIds, currentColumnWidthPercent, 1, null));

        containersIds = new ArrayList<>();
    }

    public TemplateLayoutDataGen version(int version) {
        this.version = version;
        return this;
    }
}
