package com.dotcms.e2e.test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.dotcms.e2e.page.ContainersPage;
import com.dotcms.e2e.page.GroupEntries;
import com.dotcms.e2e.page.LoginPage;
import com.dotcms.e2e.page.MenuNavigation;
import com.dotcms.e2e.page.ToolEntries;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for site layout containers.
 *
 * This class provides test methods to add and remove containers, ensuring the environment is clean for each test case execution.
 *
 * @author vico
 */
 @DisplayName("Containers Testing")
public class SiteLayoutContainersTests extends BaseE2eTest {

    private LoginPage loginPage;
    private MenuNavigation menuNavigation;
    private ContainersPage containersPage;

    @BeforeEach
    public void setup() {
        loginPage = new LoginPage(page);
        menuNavigation = new MenuNavigation(page);
        containersPage = new ContainersPage(page);

        navigateToBaseUrl();
        loginPage.successfulLogin();
        assertThat(page.getByRole(AriaRole.IMG)).isVisible();
    }

    /**
     * Scenario: Add a new container and validate its creation
     * Given the user is on the site layout containers section
     * When the user adds a new container with the title "test-container"
     * Then the container should be created successfully
     * And the container should be visible in the list
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void addContainer() throws Exception {
        //Validates if exists, and delete
        boolean exist = page.getByText("test-container").isVisible();
        if (exist) {
            deleteContainer();
        }

        menuNavigation.navigateTo(GroupEntries.SITE_LAYOUT, ToolEntries.LAYOUT_CONTAINERS);
        assertThat(page.getByText("Site LayoutContainers")).isVisible();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(" p")).click();
        containersPage.fillContainersForm("test-container",
                "this is a container created by the automated test", "2", "Rich Text",
                "$!{dotContentMap.title}\n" + "$!{dotContentMap.body}");
        assertThat(page.getByText("test-container").first()).isVisible();
    }

    /**
     * Scenario: Remove a container to clean up the environment
     * Given the user is on the site layout containers section
     * When the user removes the container with the title "test-container"
     * Then the container should be removed successfully
     * And the container should no longer be visible in the list
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void deleteContainer() throws Exception {
        menuNavigation.navigateTo(GroupEntries.SITE_LAYOUT, ToolEntries.LAYOUT_CONTAINERS);
        assertThat(page.getByText("Site LayoutContainers")).isVisible();

        //Validates if exists, if not create a new one
        boolean exist = page.getByText("test-container").isVisible();
        if (!exist) {
            addContainer();
        }

        assertThat(page.getByText("test-container")).isVisible();
        containersPage.executeContainerWorkflow("test-container", "Unpublish");
        assertThat(page.getByText("Container unpublished")).isEnabled();
        assertThat(page.getByText("test-container").first()).isVisible();
        containersPage.executeContainerWorkflow("test-container", "Archive");
        assertThat(page.getByText("Container archived")).isEnabled();
        page.getByText("Show Archived").click();
        assertThat(page.getByText("test-container").first()).isVisible();
        containersPage.executeContainerWorkflow("test-container", "Delete");
        assertThat(page.getByText("Container deleted")).isEnabled();
    }

}