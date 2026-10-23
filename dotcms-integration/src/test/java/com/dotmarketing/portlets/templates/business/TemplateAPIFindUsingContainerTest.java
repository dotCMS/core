package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ContainerDataGen;
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
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link TemplateAPI#findTemplatesUsingContainer(String, com.liferay.portal.model.User)}:
 * which Templates use a Container, with the Templates inspected scoped to the requesting user.
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
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainer}
     * Given Scenario: two templates use a container and a third uses another container
     * ExpectedResult: only the two templates that use the container are returned
     */
    @Test
    public void findsTheTemplatesThatUseTheContainer() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Container other = container(site);
        final Template first = template(site, container);
        final Template second = template(site, container);
        template(site, other);

        final List<Template> found = templateAPI.findTemplatesUsingContainer(
                container.getIdentifier(), APILocator.systemUser());

        assertEquals(Set.of(first.getIdentifier(), second.getIdentifier()), identifiers(found));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainer}
     * Given Scenario: a blank container identifier
     * ExpectedResult: nothing is returned
     */
    @Test
    public void aBlankContainerIdentifierMatchesNothing() throws Exception {
        assertTrue(templateAPI.findTemplatesUsingContainer(null, APILocator.systemUser()).isEmpty());
        assertTrue(templateAPI.findTemplatesUsingContainer("", APILocator.systemUser()).isEmpty());
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainer}
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

        final List<Template> found = templateAPI.findTemplatesUsingContainer(
                container.getIdentifier(), APILocator.systemUser());

        assertEquals(Set.of(template.getIdentifier()), identifiers(found));
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUsingContainer}
     * Given Scenario: a back-end user who can read two of the templates that use the container
     * (and none of the containers); a third template that uses it is not readable by that user
     * ExpectedResult: only the two readable templates are returned for the user, all three for
     * the system user
     */
    @Test
    public void onlyTheTemplatesTheUserCanReadAreInspected() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final Template readableA = template(site, container);
        final Template readableB = template(site, container);
        final Template hidden = template(site, container);
        final User limitedUser = backendUser();
        grantRead(limitedUser, readableA);
        grantRead(limitedUser, readableB);

        final List<Template> forUser = templateAPI.findTemplatesUsingContainer(
                container.getIdentifier(), limitedUser);
        final List<Template> forSystem = templateAPI.findTemplatesUsingContainer(
                container.getIdentifier(), APILocator.systemUser());

        assertEquals(Set.of(readableA.getIdentifier(), readableB.getIdentifier()), identifiers(forUser));
        assertEquals(Set.of(readableA.getIdentifier(), readableB.getIdentifier(), hidden.getIdentifier()),
                identifiers(forSystem));
    }

    /**
     * Method to test: {@link TemplateAPIImpl#findTemplatesUsingContainer(String,
     * com.liferay.portal.model.User, int)}
     * Given Scenario: the number of templates the user can read is an exact multiple of the page
     * size (the listing serves its first page again for an offset past the end)
     * ExpectedResult: the lookup ends and each template is returned once
     */
    @Test(timeout = 120000)
    public void endsWhenTheTemplateCountIsAMultipleOfThePageSize() throws Exception {
        final Host site = freshSite();
        final Container container = container(site);
        final User limitedUser = backendUser();
        final List<Template> readable = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final Template template = template(site, container);
            grantRead(limitedUser, template);
            readable.add(template);
        }

        final List<Template> found = templateAPI.findTemplatesUsingContainer(
                container.getIdentifier(), limitedUser, 2);

        assertEquals(4, found.size());
        assertEquals(readable.stream().map(Template::getIdentifier).collect(Collectors.toSet()),
                identifiers(found));
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

    private static Template template(final Host site, final Container container) {
        final Template template = new TemplateDataGen().site(site).title("Template " + System.nanoTime())
                .drawed(true).withContainer(container, "1").nextPersisted();
        templates.add(template);
        return template;
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
