package com.dotcms.datagen;

import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.templates.design.bean.*;

import java.util.*;

public class TemplateLayoutDataGen  {

    final Map<String, List<String>> containersIds = new HashMap<>();

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

    public TemplateLayout next() {
        final List<ContainerUUID> containers = new ArrayList<>();

        for (final String containersId : containersIds.keySet()) {
            final List<String> uuids = containersIds.get(containersId);

            for (final String uuid : uuids) {
                containers.add(new ContainerUUID(containersId, uuid));
            }
        }

        final List<TemplateLayoutColumn> columns = new ArrayList<>();
        columns.add(new TemplateLayoutColumn(containers, 100, 1, null));

        final  List<TemplateLayoutRow> rows = new ArrayList<>();
        rows.add(new TemplateLayoutRow(columns, null));

        final Body body = new Body(rows);
        final TemplateLayout templateLayout = new TemplateLayout();
        templateLayout.setBody(body);
        return templateLayout;
    }

    public TemplateLayoutDataGen withContainer(final Container container, final String UUID) {
        final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
        return withContainer(fileAssetContainerUtil.isFileAssetContainer(container) ?
                fileAssetContainerUtil.getFullPath((FileAssetContainer) container) : container.getIdentifier(), UUID);
    }

    public TemplateLayoutDataGen withContainer(final Container container) {
        return withContainer(container, null);
    }
}
