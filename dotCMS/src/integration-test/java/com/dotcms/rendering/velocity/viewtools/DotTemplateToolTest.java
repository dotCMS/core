package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImpl;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


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

    /**
     * Method to Test: {@link DotTemplateTool#getTemplateLayout(String)}
     * When: The legacy template's drawBody has a hotname with comma in it
     * Should: Return the Template layout with the right UUID values
     * */
    @Test()
    public void whenHostNameHasComma(){
        final String hostName = "test this hostname, please";
        final String drawBody =
                "<div id=\"resp-template\" name=\"globalContainer\">" +
                    "<div id=\"hd-template\">" +
                        "<h1>Header</h1>" +
                    "</div>" +
                    "<div id=\"bd-template\">" +
                        "<div id=\"yui-main-template\">" +
                            "<div class=\"yui-b-template\" id=\"splitBody0\">" +
                                "<div class=\"addContainerSpan\">" +
                                    "<a href=\"javascript: showAddContainerDialog('splitBody0');\" title=\"Add Container\">" +
                                        "<span class=\"plusBlueIcon\"></span>Add Container" +
                                    "</a>" +
                                "</div>" +
                                "<span class=\"titleContainerSpan\" id=\"splitBody0_span_69b3d24d-7e80-4be6-b04a-d352d16493ee_1562770692396\" title=\"container_69b3d24d-7e80-4be6-b04a-d352d16493ee\">" +
                                    "<div class=\"removeDiv\">" +
                                        "<a href=\"javascript: removeDrawedContainer('splitBody0','69b3d24d-7e80-4be6-b04a-d352d16493ee','1562770692396');\" title=\"Remove Container\">" +
                                            "<span class=\"minusIcon\"></span>Remove Container" +
                                        "</a>" +
                                    "</div>" +
                                    "<div class=\"clear\"></div>" +
                                    "<h2>Container: Default</h2>" +
                                    "<p>Lorem ipsum ...</p>" +
                                "</span>" +
                                "<div style=\"display: none;\" title=\"container_69b3d24d-7e80-4be6-b04a-d352d16493ee\" id=\"splitBody0_div_69b3d24d-7e80-4be6-b04a-d352d16493ee_1562770692396\">" +
                                    "%s" +
                                "</div>" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                        "<div id=\"ft-template\"><h1>Footer</h1></div>" +
                   "</div>";

        final String parseContainer = String.format("#parseContainer('//%s/application/containers/default/','1')",hostName);
        TemplateLayout templateLayout = DotTemplateTool.getTemplateLayout(String.format(drawBody, parseContainer));
        System.out.println("templateLayout = " + templateLayout);
        check(hostName, templateLayout);

        final String parseContainer2 = String.format("#parseContainer('//%s/application/containers/default/'    ,'1')",hostName);
        templateLayout = DotTemplateTool.getTemplateLayout(String.format(drawBody, parseContainer2));
        check(hostName, templateLayout);
    }

    private void check(String hostName, TemplateLayout templateLayout) {
        final List<TemplateLayoutRow> rows = templateLayout.getBody().getRows();
        assertEquals(1, rows.size());

        final List<TemplateLayoutColumn> columns = rows.get(0).getColumns();
        assertEquals(1, columns.size());

        final TemplateLayoutColumn templateLayoutColumn = columns.get(0);
        final List<ContainerUUID> containers = templateLayoutColumn.getContainers();
        assertEquals(1, containers.size());

        final ContainerUUID containerUUID = containers.get(0);

        assertEquals(String.format("//%s/application/containers/default/", hostName), containerUUID.getIdentifier());
        assertEquals("1", containerUUID.getUUID());
    }
}
