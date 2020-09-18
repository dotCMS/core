package com.dotmarketing.startup.runonce;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.ThemeDataGen;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Task05380ChangeContainerPathToAbsoluteTest {
    final String body =
        "<html>" +
            "<head>" +
                "#dotParse('%1$s/application/themes/landing-page/html_head.vtl')" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"/html/css/template/reset-fonts-grids.css\" />" +
            "</head>" +
            "<body>" +
                "<div id=\"resp-template\" name=\"globalContainer\">" +
                    "<div id=\"hd-template\">" +
                    "#dotParse('%1$s/application/themes/landing-page/header.vtl')" +
                "</div>" +
                "<div id=\"bd-template\">" +
                    "<div id=\"yui-main-template\">" +
                        "<div class=\"yui-b-template\" id=\"splitBody0\">" +
                            "#parseContainer('%1$s/application/containers/default/','1')" +
                        "</div>" +
                    "</div>" +
                "</div>" +
            "</body>" +
        "</html>";

    final String jsonDrawBody = "{" +
            "\"title\": \"layout_test\"," +
                "\"body\": {" +
                    "\"rows\": [" +
                        "{" +
                            "\"columns\": [" +
                                "{" +
                                    "\"containers\": [" +
                                        "{" +
                                            "\"identifier\": \"%s/application/containers/default/\"," +
                                            "\"uuid\": \"1\"" +
                                        "}" +
                                    "]," +
                                    "\"widthPercent\": 83," +
                                    "\"leftOffset\": 1," +
                                    "\"width\": 10," +
                                    "\"left\": 0" +
                                "}" +
                            "]" +
                        "}" +
                    "]" +
            "}" +
        "}";

    final String legacyHTMLLayout =
            "<div id=\"resp-template\" name=\"globalContainer\">" +
                "<div id=\"hd-template\"><h1>Header</h1></div>" +
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
                                "<p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>" +
                            "</span>" +
                            "<div style=\"display: none;\" title=\"container_69b3d24d-7e80-4be6-b04a-d352d16493ee\" id=\"splitBody0_div_69b3d24d-7e80-4be6-b04a-d352d16493ee_1562770692396\">" +
                                "#parseContainer('%s/application/containers/default/','1')\n" +
                            "</div>" +
                        "</div>" +
                    "</div>" +
                "</div>" +
                "<div id=\"ft-template\">" +
                    "<h1>Footer</h1>" +
                "</div>" +
            "</div>\n";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A TemplateLayout with relative path container
     * Should: Should turn it into a Absolute Path, using the template's host
     */
    @Test
    public void whenTemplateLayoutHasRelativePathShouldTurnIntoAAbsolutePath() throws IOException, DotDataException, DotSecurityException {

        final String layout = String.format(jsonDrawBody, "");
        final String testBody = String.format(body, "");
        final Host host = new SiteDataGen().nextPersisted();

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(host, template);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A TemplateLayout with not filecontainer
     * Should: Should not change
     */
    @Test
    public void whenTemplateLayoutHasNotFileCOntainer() throws IOException, DotDataException, DotSecurityException {
        final Container container = new ContainerDataGen().nextPersisted();

        final String layout = String.format(jsonDrawBody, container.getIdentifier())
                .replaceAll("/application/containers/default/", "");

        final String testBody = String.format(body, "");
        final Host host = new SiteDataGen().nextPersisted();

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        final ArrayList templates = getTemplateFromDataBase(template);

        final HashMap templateMap = (HashMap) templates.get(0);
        assertEquals(1, templates.size());

        final String drawedBody = templateMap.get("drawed_body").toString();
        assertEquals(layout, drawedBody);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A TemplateLayout with relative path container in the drawed_body fields and the body field is null
     * Should: Should turn it into a Absolute Path, using the template's host
     */
    @Test
    public void whenTemplateLayoutHasRelativePathButBodyIsNullShouldTurnIntoAAbsolutePath() throws IOException, DotDataException {

        final String layout = String.format(jsonDrawBody, "");
        final Host host = new SiteDataGen().nextPersisted();

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .setBodyAsNull()
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(host, template);
    }

    private void checkTemplateFromDataBase(Host host, Template template) throws DotDataException {
        final ArrayList templates = getTemplateFromDataBase(template);

        final HashMap templateMap = (HashMap) templates.get(0);
        assertEquals(1, templates.size());

        final String drawedBody = templateMap.get("drawed_body").toString();
        assertTrue(drawedBody.contains(String.format("//%s/application/containers/default/", host.getHostname())));

        if (!Strings.isNullOrEmpty(templateMap.get("body").toString())) {
            final String body = templateMap.get("body").toString();
            assertTrue(body.contains(String.format("//%s/application/containers/default/", host.getHostname())));
        }
    }

    private ArrayList getTemplateFromDataBase(Template template) throws DotDataException {
        return new DotConnect()
                    .setSQL(String.format("select drawed_body, body from template where inode = '%s'", template.getInode()))
                    .loadResults();
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A TemplateLayout with absolute path container
     * Should: Should does not anything
     */
    @Test
    public void whenTemplateLayoutHasAbsolutePathShouldKeepThePathWithoutChange() throws IOException, DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final String layout = String.format(jsonDrawBody, "//" + host.getHostname());

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(String.format(body, "//" + host.getHostname()))
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(host, template);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A legacy HTML TemplateLayout with relative path container
     * Should: Should change the relative path by absolute path
     */
    @Test
    public void whenLegacyTemplateLayoutHasRelativePathShouldTurnIntoAbsolutePath() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final String layout = String.format(legacyHTMLLayout, "");

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(String.format(body, ""))
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();
        CacheLocator.getTemplateCache().remove(template.getInode());

        checkTemplateFromDataBase(host, template);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A legacy HTML TemplateLayout with absolute path container
     * Should: Should keep the absolute path
     */
    @Test
    public void whenLegacyTemplateLayoutHasAbsolutePath() throws DotDataException {
        final Host host = new SiteDataGen().nextPersisted();
        final String layout = String.format(legacyHTMLLayout, "//" + host.getHostname());

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(String.format(body, "//" + host.getHostname()))
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();
        CacheLocator.getTemplateCache().remove(template.getInode());

        checkTemplateFromDataBase(host, template);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists More than one template with relative path
     * Should: Should change the relative path by absolute path
     */
    @Test
    public void whenHaveMoreThanOneTemplate() throws IOException, DotDataException {
        final String layout = String.format(jsonDrawBody, "");
        final String testBody = String.format(body, "");
        final Host host = new SiteDataGen().nextPersisted();

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();

        final Template template2 = new TemplateDataGen()
                .title("template_test2_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();

        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(host, template);
        checkTemplateFromDataBase(host, template2);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A advance template with relative path container
     * Should: Should not change anything
     */
    @Test
    public void whenTemplateAdvancedHasRelativePath() throws IOException, DotDataException, DotSecurityException {
        final String body = "#parseContainer(\"/application/containers/default\")";

        final Host host = new SiteDataGen().nextPersisted();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .body(body)
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        final ArrayList templates = getTemplateFromDataBase(template);

        final HashMap templateMap = (HashMap) templates.get(0);
        assertEquals(1, templates.size());

        final String templateBody = templateMap.get("body").toString();
        assertEquals(templateBody, body);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A TemplateLayout with absolute path container from another site
     * Should: Should not change anything
     */
    @Test
    public void whenTemplateLayoutHasAbsolutePathToAnotherSite() throws IOException, DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Host anotherHost = new SiteDataGen().nextPersisted();

        final String layout = String.format(jsonDrawBody, "//" + anotherHost.getHostname());
        final String testBody = String.format(body, "//" + anotherHost.getHostname());

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(anotherHost, template);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: Exists A legacy TemplateLayout with absolute path container from another site
     * Should: Should does not anything
     */
    @Test
    public void whenLegacyTemplateLayoutHasAbsolutePathToAnotherSite() throws IOException, DotDataException, DotSecurityException {

        final Host host = new SiteDataGen().nextPersisted();
        final Host anotherHost = new SiteDataGen().nextPersisted();

        final String layout = String.format(legacyHTMLLayout, "//" + anotherHost.getHostname());
        final String testBody = String.format(body, "//" + anotherHost.getHostname());

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();


        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(anotherHost, template);
    }

    private void checkTemplateLayout(final String layout) throws IOException {
        DotTemplateTool.getTemplateLayoutFromJSON(layout);
    }

    /**
     * Method to Test: {@link Task05380ChangeContainerPathToAbsolute#executeUpgrade()}
     * When: A site change the name before run the {@link Task05380ChangeContainerPathToAbsolute}
     * Should: Should use the last name
     */
    @Test
    public void whenSiteChangeNameBeforeRunUT() throws IOException, DotDataException, DotSecurityException {

        final String layout = String.format(jsonDrawBody, "");
        final String testBody = String.format(body, "");
        final Host host = new SiteDataGen().nextPersisted();
        final String oldName = host.getHostname();

        host.setHostname(String.format("new_host_name_%s", System.currentTimeMillis()));
        APILocator.getHostAPI().save(host, APILocator.systemUser(), false);

        checkTemplateLayout(layout);
        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("template_test_" + System.currentTimeMillis())
                .theme(theme)
                .drawedBody(layout)
                .body(testBody)
                .host(host)
                .nextPersisted();

        final Task05380ChangeContainerPathToAbsolute task05380ChangeContainerPathToAbsolute =
                new Task05380ChangeContainerPathToAbsolute();

        task05380ChangeContainerPathToAbsolute.executeUpgrade();

        checkTemplateFromDataBase(host, template);

        final boolean anyMatchWithOldName = new DotConnect()
                .setSQL(Task05380ChangeContainerPathToAbsolute.GET_TEMPLATES_QUERY)
                .loadObjectResults()
                .stream()
                .map(templateMap -> templateMap.get("host_name"))
                .anyMatch(title -> title.equals(oldName));

        assertFalse(anyMatchWithOldName);

        final long count = new DotConnect()
                .setSQL(Task05380ChangeContainerPathToAbsolute.GET_TEMPLATES_QUERY)
                .loadObjectResults()
                .stream()
                .map(templateMap -> templateMap.get("inode"))
                .filter(inode -> inode.equals(template.getInode()))
                .count();

        assertEquals(count, 1);
    }
}
