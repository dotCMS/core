package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link TemplateAPI#findTemplatesUsingContainers(java.util.Collection,
 * com.liferay.portal.model.User)}: which Templates use each Container, hidden page layouts and
 * archived Templates included, with the Templates returned scoped to the requesting user.
 */
public class TemplateAPIFindUsingContainerTest {

    private static TemplateAPIImpl templateAPI;
    private static final List<Template> templates = new ArrayList<>();
    private static final List<Container> containers = new ArrayList<>();
    private static final List<User> users = new ArrayList<>();
    private static final List<Host> sites = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        templateAPI = (TemplateAPIImpl) APILocator.getTemplateAPI();
    }

    @AfterClass
    public static void cleanUp() {
        templates.forEach(template -> Try.run(() -> TemplateDataGen.remove(template))
                .onFailure(e -> Logger.warn(TemplateAPIFindUsingContainerTest.class,
                        "Could not remove template " + template.getIdentifier(), e)));
        containers.forEach(container -> Try.run(() -> ContainerDataGen.remove(container))
                .onFailure(e -> Logger.warn(TemplateAPIFindUsingContainerTest.class,
                        "Could not remove container " + container.getIdentifier(), e)));
        users.forEach(user -> Try.run(() -> UserDataGen.remove(user))
                .onFailure(e -> Logger.warn(TemplateAPIFindUsingContainerTest.class,
                        "Could not remove user " + user.getUserId(), e)));
        sites.forEach(site -> Try.run(() -> {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }).onFailure(e -> Logger.warn(TemplateAPIFindUsingContainerTest.class,
                "Could not remove site " + site.getIdentifier(), e)));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: two templates use a container and a third uses another container
     * ExpectedResult: only the two templates that use the container are returned for it
     */
    @Test
    public void findsTheTemplatesThatUseTheContainer() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Container other = container(site);
        final Template first = template(site, container);
        final Template second = template(site, container);
        template(site, other);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(first.getIdentifier(), second.getIdentifier()),
                identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: no identifiers, or only blank ones
     * ExpectedResult: nothing is returned
     */
    @Test
    public void blankContainerIdentifiersMatchNothing() throws Exception {
        assertTrue(templateAPI.findTemplatesUsingContainers(null, APILocator.systemUser()).isEmpty());
        assertTrue(templateAPI.findTemplatesUsingContainers(List.of(), APILocator.systemUser()).isEmpty());
        assertTrue(templateAPI.findTemplatesUsingContainers(Arrays.asList(null, ""),
                APILocator.systemUser()).isEmpty());
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: an archived template uses the container
     * ExpectedResult: the archived template is still returned
     */
    @Test
    public void archivedTemplatesAreIncluded() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template template = template(site, container);
        templateAPI.unpublishTemplate(template, APILocator.systemUser(), false);
        templateAPI.archive(template, APILocator.systemUser(), false);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(template.getIdentifier()), identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: a page custom layout (an {@code anonymous_layout_} template that is not shown
     * on the menu) uses the container
     * ExpectedResult: the layout is returned, so the container cannot be deleted behind a page's back
     */
    @Test
    public void pageCustomLayoutsAreIncluded() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template layout = pageCustomLayout(site, container);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(layout.getIdentifier()), identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: an advanced (code) template references the container with
     * {@code #parseContainer} but no page has put content in the container yet
     * ExpectedResult: the template is returned, a reference is enough
     */
    @Test
    public void advancedTemplatesCountWithoutContentInTheContainer() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template advanced = new TemplateDataGen().site(site).title("Advanced " + System.nanoTime())
                .drawed(false).withContainer(container, "1").nextPersisted();
        templates.add(advanced);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(advanced.getIdentifier()), identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: a drawn template references a file container by its full path (with the site)
     * ExpectedResult: the template is returned under the container's identifier
     */
    @Test
    public void containersReferencedByPathAreMatched() throws Exception {
        final Host site = freshSite();
        final FileAssetContainer created = new ContainerAsFileDataGen().host(site).nextPersisted();
        // the generator leaves the path out; the saved container has it
        final FileAssetContainer fileContainer = (FileAssetContainer) APILocator.getContainerAPI()
                .find(created.getInode(), APILocator.systemUser(), true);
        final Template template = new TemplateDataGen().site(site).title("By path " + System.nanoTime())
                .drawed(true).withContainer("//" + site.getHostname() + fileContainer.getPath(), "1")
                .nextPersisted();
        templates.add(template);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(fileContainer.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(template.getIdentifier()),
                identifiers(found.get(fileContainer.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: three containers are asked for at once; one is used by two templates, one by a
     * single template and one by none
     * ExpectedResult: one entry per container, in the order asked, each with its own templates
     */
    @Test
    public void severalContainersAreResolvedInOneCall() throws Exception {
        final Host site = freshSite();
        final Container shared = container(site);
        final Container single = container(site);
        final Container unused = container(site);
        final Template both = template(site, shared, single);
        final Template onlyShared = template(site, shared);

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(unused.getIdentifier(), shared.getIdentifier(), single.getIdentifier()),
                APILocator.systemUser());

        assertEquals(List.of(unused.getIdentifier(), shared.getIdentifier(), single.getIdentifier()),
                new ArrayList<>(found.keySet()));
        assertTrue(found.get(unused.getIdentifier()).isEmpty());
        assertEquals(Set.of(both.getIdentifier(), onlyShared.getIdentifier()),
                identifiers(found.get(shared.getIdentifier())));
        assertEquals(Set.of(both.getIdentifier()), identifiers(found.get(single.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: a back-end user who can read two of the templates that use the container
     * (and none of the containers); a third template that uses it is not readable by that user
     * ExpectedResult: only the two readable templates are returned for the user, all three for
     * the system user
     */
    @Test
    public void onlyTheTemplatesTheUserCanReadAreReturned() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template readableA = template(site, container);
        final Template readableB = template(site, container);
        final Template hidden = template(site, container);
        final User limitedUser = backendUser();
        grantRead(limitedUser, readableA);
        grantRead(limitedUser, readableB);

        final Map<String, List<Template>> forUser = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), limitedUser);
        final Map<String, List<Template>> forSystem = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(readableA.getIdentifier(), readableB.getIdentifier()),
                identifiers(forUser.get(container.getIdentifier())));
        assertEquals(Set.of(readableA.getIdentifier(), readableB.getIdentifier(), hidden.getIdentifier()),
                identifiers(forSystem.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPIImpl#findTemplatesUsingContainers(java.util.Collection,
     * com.liferay.portal.model.User, int)}
     * Given Scenario: the Templates in the database take several pages of two, the last one
     * exactly full
     * ExpectedResult: the lookup ends and every template that uses the container is returned once
     */
    @Test(timeout = 120000)
    public void readsEveryPageOfTemplates() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Set<String> used = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            used.add(template(site, container).getIdentifier());
        }

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser(), 2);

        assertEquals(5, found.get(container.getIdentifier()).size());
        assertEquals(used, identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: two advanced Templates name their Containers with literal calls, one with
     * double quotes and one with single quotes
     * ExpectedResult: each Container is matched to its own Template
     */
    @Test
    public void advancedTemplatesWithLiteralCallsAreMatched() throws Exception {
        final Host site = freshSite();
        final Container doubleQuoted = container(site);
        final Container singleQuoted = container(site);
        final Template first = advancedTemplate(site,
                "<div>#parseContainer(\"" + doubleQuoted.getIdentifier() + "\", \"1\")</div>");
        final Template second = advancedTemplate(site,
                "<div>#parseContainer('" + singleQuoted.getIdentifier() + "', '1')</div>");

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(doubleQuoted.getIdentifier(), singleQuoted.getIdentifier()),
                APILocator.systemUser());

        assertEquals(Set.of(first.getIdentifier()), identifiers(found.get(doubleQuoted.getIdentifier())));
        assertEquals(Set.of(second.getIdentifier()), identifiers(found.get(singleQuoted.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: an advanced Template whose {@code #parseContainer} gets the Container from a
     * variable, so the identifier is not written in the body
     * ExpectedResult: the Template is still returned for that Container
     */
    @Test
    public void advancedTemplateWithContainerInAVariableIsMatched() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template advanced = advancedTemplate(site,
                "#set($containerId = '" + container.getIdentifier() + "')\n"
                        + "#parseContainer($containerId, '1')");

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(advanced.getIdentifier()), identifiers(found.get(container.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: an advanced Template that names one Container itself and pulls another from
     * a file with {@code #dotParse}
     * ExpectedResult: the Template is returned for both Containers; the literal call must not hide
     * the one the included file brings
     */
    @Test
    public void advancedTemplateWithContainerInAnIncludedFileIsMatched() throws Exception {
        final Host site = freshSite();
        final Container direct = container(site);
        final Container included = container(site);
        final String includeFolderPath = "application/templates/includes";
        final String includeFileName = "container_includes_" + System.nanoTime() + ".vtl";
        final Folder includeFolder = APILocator.getFolderAPI().createFolders(
                includeFolderPath, site, APILocator.systemUser(), false);
        final File includeFile = FileUtil.createTemporaryFile("includedTemplate", ".vtl",
                "#parseContainer('" + included.getIdentifier() + "', '1')\n");
        final Contentlet savedFile = new FileAssetDataGen(includeFolder, includeFile)
                .setProperty("title", includeFileName)
                .setProperty("fileName", includeFileName)
                .nextPersisted();
        APILocator.getContentletAPI().publish(savedFile, APILocator.systemUser(), false);
        final Template advanced = advancedTemplate(site,
                "#dotParse('//" + site.getHostname() + "/" + includeFolderPath + "/" + includeFileName + "')\n"
                        + "#parseContainer('" + direct.getIdentifier() + "', '1')");

        final Map<String, List<Template>> found = templateAPI.findTemplatesUsingContainers(
                List.of(direct.getIdentifier(), included.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(advanced.getIdentifier()), identifiers(found.get(direct.getIdentifier())));
        assertEquals(Set.of(advanced.getIdentifier()), identifiers(found.get(included.getIdentifier())));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainers}
     * Given Scenario: the same lookup is made twice in a row, over a Template whose Containers come
     * from a variable and so are read by running its body
     * ExpectedResult: both lookups give the same answer
     */
    @Test
    public void aRepeatedLookupGivesTheSameAnswer() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template advanced = advancedTemplate(site,
                "#set($containerId = '" + container.getIdentifier() + "')\n"
                        + "#parseContainer($containerId, '1')");

        final Map<String, List<Template>> first = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());
        final Map<String, List<Template>> second = templateAPI.findTemplatesUsingContainers(
                List.of(container.getIdentifier()), APILocator.systemUser());

        assertEquals(Set.of(advanced.getIdentifier()), identifiers(first.get(container.getIdentifier())));
        assertEquals(identifiers(first.get(container.getIdentifier())),
                identifiers(second.get(container.getIdentifier())));
    }

    private static Template advancedTemplate(final Host site, final String body) {
        final Template template = new TemplateDataGen().site(site)
                .title("Advanced " + System.nanoTime()).drawed(false).body(body).nextPersisted();
        templates.add(template);
        return template;
    }

    private static Set<String> identifiers(final List<Template> found) {
        return found.stream().map(Template::getIdentifier).collect(Collectors.toSet());
    }

    private static Host freshSite() {
        final Host site = new SiteDataGen().nextPersisted();
        sites.add(site);
        return site;
    }

    private static Container container(final Host site) {
        final Container container = new ContainerDataGen().site(site)
                .title("Container " + System.nanoTime()).nextPersisted();
        containers.add(container);
        return container;
    }

    private static Template template(final Host site, final Container... used) {
        final TemplateDataGen dataGen = new TemplateDataGen().site(site)
                .title("Template " + System.nanoTime()).drawed(true);
        for (final Container container : used) {
            dataGen.withContainer(container, "1");
        }
        final Template template = dataGen.nextPersisted();
        templates.add(template);
        return template;
    }

    private static Template pageCustomLayout(final Host site, final Container container) {
        final Template layout = new TemplateDataGen().site(site)
                .title(Template.ANONYMOUS_PREFIX + System.nanoTime()).showOnMenu(false)
                .drawed(true).withContainer(container, "1").nextPersisted();
        templates.add(layout);
        return layout;
    }

    private static User backendUser() throws Exception {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        users.add(user);
        return user;
    }

    private static void grantRead(final User user, final Permissionable asset) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ, true);
        APILocator.getPermissionAPI().save(permission, asset, APILocator.systemUser(), false);
    }
}
