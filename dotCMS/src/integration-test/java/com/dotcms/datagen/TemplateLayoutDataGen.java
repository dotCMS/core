package com.dotcms.datagen;

import com.dotmarketing.portlets.templates.design.bean.*;

import java.util.ArrayList;

import java.util.List;

public class TemplateLayoutDataGen  {

    final List<String> containersIds = new ArrayList();

    public static TemplateLayoutDataGen get(){
        return new TemplateLayoutDataGen();
    }

    public TemplateLayoutDataGen withContainer(final String identifier){
        containersIds.add(identifier);
        return this;
    }

    public TemplateLayout next() {
        final List<ContainerUUID> containers = new ArrayList<>();

        for (int i = 0; i < containersIds.size(); i++) {
            final String containersId = containersIds.get(i);
            containers.add(new ContainerUUID(containersId, "1"));
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
}
