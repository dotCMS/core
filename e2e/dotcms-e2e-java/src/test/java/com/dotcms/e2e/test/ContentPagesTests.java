package com.dotcms.e2e.test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.dotcms.e2e.page.GroupEntries;
import com.dotcms.e2e.page.LoginPage;
import com.dotcms.e2e.page.MenuNavigation;
import com.dotcms.e2e.page.PagesPage;
import com.dotcms.e2e.page.ToolEntries;
import com.dotcms.e2e.playwright.PlaywrightSupport;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for content pages.
 *
 * This class provides test methods to add and remove pages, ensuring the environment is clean for each test case execution.
 *
 * @author vico
 */
@DisplayName("Pages Testing")
public class ContentPagesTests extends BaseE2eTest {

    private MenuNavigation menuNavigation;
    private PagesPage pagesPage;

    @BeforeEach
    public void setup() {
        final LoginPage loginPage = new LoginPage(page);
        menuNavigation = new MenuNavigation(page);
        pagesPage = new PagesPage(page);

        page.navigate(getBaseUrl());
        loginPage.successfulLogin();
        assertThat(page.getByRole(AriaRole.IMG)).isVisible();
    }

    /**
     * Scenario: Add a new page and validate its creation
     * Given the user is on the content pages section
     * When the user adds a new page with the title "Testing Page"
     * Then the page should be created successfully
     * And the page should be visible in the list
     */
    @Test
    public void test_addPage() throws Exception {
        menuNavigation.navigateTo(GroupEntries.CONTENT_MENU, ToolEntries.CONTENT_PAGES);
        assertThat(page.getByText("ContentPages")).isVisible();
        assertThat(page.locator("span").filter(new Locator.FilterOptions().setHasText("Bookmarks"))
                .first()).isVisible();

        boolean exist = page.getByText("Testing Page").isVisible();
        if (exist) {
            test_removePage();
        }

        page.getByTestId("createPageButton").click();
        page.getByText("description Page").click();

        final String pagesDetailFrame = "iframe[name='detailFrame']";
        // fill the page form
        pagesPage.fillPagesForm(pagesDetailFrame, "Testing Page", "Default Template");
        // validations
        assertThat(page.locator("li")
                .filter(new Locator.FilterOptions().setHasText("Testing Page")))
                .isVisible(PlaywrightSupport.get().assertVisibleTimeout());
    }


    /**
     * Scenario: Remove a page to clean up the environment
     * Given the user is on the content pages section
     * When the user removes the page with the title "Testing Page"
     * Then the page should be removed successfully
     * And the page should no longer be visible in the list
     */
    @Test
    public void test_removePage() throws Exception {
        menuNavigation.navigateTo(GroupEntries.CONTENT_MENU, ToolEntries.CONTENT_PAGES);
        assertThat(page.getByText("ContentPages")).isVisible();
        page.getByTestId("dot-pages-listing-header__keyword-input").click();

        boolean exist = page.getByText("Testing Page").isVisible();
        if (!exist) {
            test_addPage();
        }

        //menuNavigation.navigateTo(GroupEntries.CONTENT_MENU, ToolEntries.CONTENT_PAGES);
        assertThat(page.getByText("ContentPages")).isVisible();
        page.getByTestId("dot-pages-listing-header__keyword-input").click();
        //execute the unpublish action
        pagesPage.executePagesWorkflow("Testing Page", "Unpublish");
        assertThat(page.getByText("Workflow executed")).isVisible();
        assertThat(page.getByText("Workflow executed")).not().isVisible();
        //execute the archived action
        pagesPage.executePagesWorkflow("Testing Page", "Archive");
        assertThat(page.getByText("Workflow executed")).isVisible();
        assertThat(page.getByText("Workflow executed")).not().isVisible();
        page.locator("p-checkbox div").nth(2).click();
        // Enable the show archived check
        assertThat(page.getByRole(AriaRole.COLUMNHEADER,
                new Page.GetByRoleOptions().setName("Status"))).isVisible();
        //execute the delete action
        pagesPage.executePagesWorkflow("Testing Page", "Delete");
        assertThat(page.getByText("Workflow executed")).isVisible(PlaywrightSupport.get().assertVisibleTimeout());
    }

}
