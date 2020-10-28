package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DotTemplateToolTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

     /**
      *  Method to Test: {@link DotTemplateTool#getMaxUUID(Template)}
      * When: Get the MaxUUID for a drawed Template
      * Should: Delete it
      * */
    @Test
    public void whenGetMaxUUIDToDrawedTemplate(){
        final Container container_1 = new ContainerDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().nextPersisted();
        final Container container_3 = new ContainerDataGen().nextPersisted();

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container_1, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_2, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_3, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_1, "2")
                .withContainer(container_3, "2")
                .withContainer(container_3, "3")
                .next();

        final Template template = new TemplateDataGen()
                .drawedBody(templateLayout)
                .nextPersisted();

        final Map<String, Long> maxUUID = DotTemplateTool.getMaxUUID(template);

        assertEquals(3, maxUUID.size());

        assertTrue(2l == maxUUID.get(container_1.getIdentifier()));
        assertTrue(1l == maxUUID.get(container_2.getIdentifier()));
        assertTrue(3l == maxUUID.get(container_3.getIdentifier()));
    }

    @Test
    public void whenGetMaxUUIDToAdvancedTemplate(){
        final Container container_1 = new ContainerDataGen().nextPersisted();
        final Container container_2 = new ContainerDataGen().nextPersisted();
        final Container container_3 = new ContainerDataGen().nextPersisted();

        final Template template = new TemplateDataGen()
                .withContainer(container_1, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_2, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_3, ContainerUUID.UUID_START_VALUE)
                .withContainer(container_1, "2")
                .withContainer(container_3, "2")
                .withContainer(container_3, "3")
                .nextPersisted();

        final Map<String, Long> maxUUID = DotTemplateTool.getMaxUUID(template);

        assertEquals(0, maxUUID.size());

    }
}
