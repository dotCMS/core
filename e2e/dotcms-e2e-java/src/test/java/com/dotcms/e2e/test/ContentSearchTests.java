package com.dotcms.e2e.test;

import com.dotcms.e2e.page.LoginPage;
import com.dotcms.e2e.page.MenuNavigation;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.dotcms.e2e.page.GroupEntries;
import com.dotcms.e2e.page.ToolEntries;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Test class for content search.
 *
 * This class provides test methods to add and remove content, ensuring the environment is clean for each test case execution.
 *
 * @author
 */
@SuppressWarnings("deprecation")
@DisplayName("Content Search testing")
public class ContentSearchTests extends BaseE2eTest {

    private LoginPage loginPage;
    private MenuNavigation menuNavigation;

    @BeforeEach
    public void setup() {
        loginPage = new LoginPage(page);
        menuNavigation = new MenuNavigation(page);
    }

    /**
     * Scenario: Add a new content and validate its creation
     * Given the user is on the content search section
     * When the user adds a new content with the title "Demo Test"
     * Then the content should be created successfully
     * And the content should be visible in the list
     */
    @Test
    public void test_addContent() {
        page.navigate(getBaseUrl());
        loginPage.successfulLogin();
        assertThat( page.getByRole(AriaRole.IMG)).isVisible();

        // Navigate to some specific portlet
        menuNavigation.navigateTo(GroupEntries.CONTENT_MENU, ToolEntries.CONTENT_SEARCH);

        // Wait until the contents table is loaded
        final String mainFrame = "iframe[id='detailFrame']";
        assertThat(page.frameLocator(mainFrame).locator("[id=nextDiv]")).isVisible();

        // Delete content if it exists
        deleteContentIfExist(mainFrame);

        //Click + button
        page.frameLocator(mainFrame).locator("[id=dijit_form_DropDownButton_0]").click();
        assertThat(page.frameLocator(mainFrame).locator("[id=dijit_MenuItem_2_text]")).isEnabled();

        // Select Add New Content
        page.frameLocator(mainFrame).first().locator("[id=dijit_MenuItem_2_text]").click();
        assertThat(page.frameLocator(mainFrame).locator("[id=sType799f176a-d32e-4844-a07c-1b5fcd107578]")).isVisible();

        //Select Blog
        page.frameLocator(mainFrame).locator("[id=sType799f176a-d32e-4844-a07c-1b5fcd107578]").click();

        final String contentDetailFrame = "dot-iframe-dialog iframe[id='detailFrame']";
        assertThat(page.frameLocator(contentDetailFrame).locator("#contentHost_field")).isVisible();
        fillContentForm(contentDetailFrame);
        assertThat(page.frameLocator(mainFrame).locator("[id=dijit_MenuItem_2_text]")).isEnabled();

        //Validate the content was created successfully
        page.frameLocator(mainFrame).locator("#widget_structure_inode div").first().click();
        page.frameLocator(mainFrame).locator("[id=structure_inode_popup3]").click();
        //page.frameLocator(mainFrame).getByRole(AriaRole.OPTION, new FrameLocator.GetByRoleOptions().setName("All")).click();
        assertThat(page.frameLocator(mainFrame).locator("[id=results_table]")).containsText("Demo Test"  + "en-us" + "lock_open");


        //Archive and Destroy the new content
        deleteContentIfExist(mainFrame);
    }

    private void fillContentForm(final String contentDetailFrame) {
        // Fill the blog content form
        page.frameLocator(contentDetailFrame).locator("[id=title]").type("Demo Test");
        page.frameLocator(contentDetailFrame).locator("[id=dijit_form_ComboButton_0_arrow]").click();
        page.frameLocator(contentDetailFrame).locator("[id=dijit_MenuItem_1_text]").click();

        // Relate to author
        assertThat(page.frameLocator(contentDetailFrame).locator("[id=dijit_Dialog_4]").getByText("Search x")).isVisible();
        page.frameLocator(contentDetailFrame).locator("[id=dijit_form_CheckBox_0]").check();
        page.frameLocator(contentDetailFrame).locator("[id=dijit_form_Button_14]").click();

        //Continue the form
        assertThat(page.frameLocator(contentDetailFrame).locator("[id=teaser]")).isVisible();
        page.frameLocator(contentDetailFrame).locator("[id=teaser]").type("This is a demo content ");

        //Date field
        page.frameLocator(contentDetailFrame).locator("[id=postingDateDate]").fill("8/25/2023");

        //Adding tags
        page.frameLocator(contentDetailFrame).locator("#tags").click();
        page.frameLocator(contentDetailFrame).locator("#tags").fill("tag1");
        page.frameLocator(contentDetailFrame).locator("#tags").press("Enter");
        page.frameLocator(contentDetailFrame).locator("#tags").fill("tag2");
        page.frameLocator(contentDetailFrame).locator("#tags").press("Enter");

        //Adding content to blog field
        page.frameLocator(contentDetailFrame).locator("#block-editor-blogContent div").nth(1).fill("test1");

        //Set image
        // page.frameLocator(contentDetailFrame).getByTestId("choose-file-btn").setInputFiles(Paths.get("files/logo.png"));

        //Select the WF action (Save/Publish)
        page.frameLocator(contentDetailFrame).getByText("Save and Publish").click();
        assertThat(page.frameLocator(contentDetailFrame).getByText("Lock for Editing")).isVisible();
        //assertThat(page.getByText("The content was saved successfully but cannot be published because it is schedule")).isVisible();
        assertThat(page.frameLocator(contentDetailFrame).getByText("Content saved")).isVisible();
        assertThat(page.frameLocator(contentDetailFrame).getByText("Content saved")).not().isVisible();

        //Close the content iframe
        page.getByTestId("close-button").click();
    }

    private void deleteContentIfExist(final String mainFrame) {
        if (page.frameLocator(mainFrame).getByText("Demo Test").count() <= 0) {
            System.out.println("Validating if content exist... Content does not exist");
            return;
        }

        page.frameLocator(mainFrame).getByText("Demo Test").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.frameLocator(mainFrame).getByRole(AriaRole.MENUITEM).getByText("Archive").click();
        // Validate the Workflow Execution Status
        assertThat(page.frameLocator(mainFrame).getByText("Workflow executed")).isEnabled();
        assertThat(page.frameLocator(mainFrame).getByText("Workflow executed")).not().isVisible();
        // Go to archived contents and Destroy if exist
        page.frameLocator(mainFrame).locator("#widget_structure_inode div").first().click();
        page.frameLocator(mainFrame).getByText("file_copyBlog").click();
        page.frameLocator(mainFrame).getByRole(AriaRole.LINK, new FrameLocator.GetByRoleOptions().setName("Advanced")).click();
        page.frameLocator(mainFrame).locator("#widget_showingSelect div").first().click();
        page.frameLocator(mainFrame).getByRole(AriaRole.OPTION, new FrameLocator.GetByRoleOptions().setName("Archived")).click();
        page.frameLocator(mainFrame).getByText("Demo Test").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.frameLocator(mainFrame).getByRole(AriaRole.MENUITEM).getByText("Destroy").click();

        //Validate the workflow has been executed
        assertThat(page.frameLocator(mainFrame).getByText("Workflow executed")).isEnabled();
        assertThat(page.frameLocator(mainFrame).getByText("Workflow executed")).not().isVisible();
    }
}

