import { expect, test } from "@playwright/test";

import { ContentPage } from "@pages";
import { waitForVisibleAndCallback } from "@utils/utils";
import {
  addContent,
  iFramesLocators,
  contentGeneric,
} from "@locators/globalLocators";
import { contentProperties, genericContent1 } from "./contentData";
import { LoginPage } from "@pages";
import { SideMenuComponent } from "@components/sideMenu.component";

/**
 * Test to navigate to the content portlet and login to the dotCMS instance
 * @param page
 */
test.beforeEach("Navigate to content portlet", async ({ page }) => {
  const loginPage = new LoginPage(page);
  const sideMenuPage = new SideMenuComponent(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  await loginPage.login(username, password);
  await sideMenuPage.navigate("Content", "Search All");
});

/**
 * Test to validate the filter is present and usable in the content portlet
 * @param page
 */
test("Search filter", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  // Adding new rich text content
  await contentUtils.addNewContentAction(
    contentGeneric.locator,
    contentGeneric.label,
  );
  await contentUtils.fillRichTextForm({
    title: genericContent1.title,
    body: genericContent1.body,
    action: contentProperties.publishWfAction,
  });
  await contentUtils.workflowExecutionValidationAndClose("Content saved");

  // Validate the content has been created
  await expect
    .soft(iframe.getByRole("link", { name: genericContent1.title }).first())
    .toBeVisible();
  await iframe.locator("#allFieldTB").fill(genericContent1.title);
  await page.keyboard.press("Enter");

  //validate the search filter is working
  await expect(
    iframe.getByRole("link", { name: genericContent1.title }).first(),
  ).toBeVisible();
});

/**
 * Test to validate the views are working in the content portlet
 * @param page
 */
test("Validate views buttons", async ({ page }) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  await iframe.getByLabel("List", { exact: true }).isVisible();
  await iframe.getByLabel("Card").isVisible();

  // Click on the card view
  await iframe.getByLabel("Card").click();
  await iframe
    .locator(".hydrated > dot-contentlet-thumbnail > .hydrated")
    .first()
    .isVisible();
});

/**
 * Test to validate the add content button is present and functional
 * @param page
 */
test("Validate add content button is present and functional", async ({
  page,
}) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  await iframe.locator(addContent.addBtn).click();
  await expect(
    iframe
      .getByLabel(addContent.addNewMenuLabel)
      .getByText(addContent.addNewContentSubMenu),
  ).toBeVisible();
});

/**
 * Test to validate the behavior of the bulk Workflow actions button
 * @param page
 */
test("Validate bulk Workflow actions", async ({ page }) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  // Validate the button is present and disabled by default
  const workflowActionsButton = iframe.getByRole("button", {
    name: "Available Workflow Actions",
  });
  await expect(workflowActionsButton).toBeDisabled();

  // Check the checkbox and validate the button is enabled
  await iframe.locator("#checkbox0").check();
  await expect(workflowActionsButton).toBeEnabled();

  // Click on the button and validate the dialog is present
  await workflowActionsButton.click();
  await expect(
    iframe.locator('.dijitDialog[role="dialog"]').first(),
  ).toBeVisible();
});

/**
 * Test to validate the search query is generating the correct results
 * @param page
 */
test("Validate the search query", async ({ page }) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);
  await new ContentPage(page).showQuery(iframe);
  await expect(iframe.locator("#queryResults")).toBeVisible();
});

/**
 * Test to validate the API button is working in the search query modal
 * @param page
 */
test("Validate the API link in search query modal is working", async ({
  page,
}) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);
  await new ContentPage(page).showQuery(iframe);

  // Wait for the new tab to open
  const queryModal = page.waitForEvent("popup");
  await iframe.getByText("API", { exact: true }).click();
  const newPage = await queryModal;

  // Validate the new tab has opened
  await newPage.waitForLoadState();
  expect(newPage.url()).toContain("about:blank");

  //close the new tab
  await newPage.close();
});

/**
 * Test to validate the clear button in the search filter
 * @param page
 */
test("Validate the clear button in the search filter", async ({ page }) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  //expand the search filter
  const advancedLinkLocator = iframe.getByRole("link", { name: "Advanced" });
  await waitForVisibleAndCallback(advancedLinkLocator, () =>
    advancedLinkLocator.click(),
  );

  // Select the workflow in the search filter
  await iframe
    .locator('#widget_scheme_id [data-dojo-attach-point="_buttonNode"]')
    .click();
  await iframe.getByRole("option", { name: "System Workflow" }).click();
  //TODO remove this
  await page.waitForTimeout(1000);

  // Select the step in the search filter
  const widgetStepIdLocator = iframe.locator(
    "div[id='widget_step_id'] div[data-dojo-attach-point='_buttonNode']",
  );
  await waitForVisibleAndCallback(widgetStepIdLocator, () =>
    widgetStepIdLocator.click(),
  );
  const newOption = iframe.getByRole("option", { name: "New" });
  await waitForVisibleAndCallback(newOption, () => newOption.click());

  // Select the Show in the search filter
  await iframe
    .locator('#widget_showingSelect [data-dojo-attach-point="_buttonNode"]')
    .click();
  await iframe.getByRole("option", { name: "Unpublished" }).click();

  // Click on clear button
  await iframe.getByLabel("Clear").click();

  // Validate the search filter has been cleared
  await expect(
    iframe.locator('input[name="scheme_id_select"]'),
  ).toHaveAttribute("value", "catchall");
  await expect(iframe.locator('input[name="step_id_select"]')).toHaveAttribute(
    "value",
    "catchall",
  );
  await expect(iframe.locator("#showingSelect")).toHaveAttribute(
    "value",
    "All",
  );
});

/**
 * Test to validate the hide button in the search filter
 * @param page
 */
test("Validate the hide button collapse the filter", async ({ page }) => {
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  await expect(iframe.getByRole("button", { name: "Search" })).toBeVisible();
  const advancedLinkLocator = iframe.getByRole("link", { name: "Advanced" });
  await waitForVisibleAndCallback(advancedLinkLocator, () =>
    advancedLinkLocator.click(),
  );

  await page.waitForTimeout(1000);
  await expect(iframe.getByRole("link", { name: "Advanced" })).toBeHidden();

  // Click on the hide button
  await iframe.getByRole("link", { name: "Hide" }).click();
  await page.waitForTimeout(1000);

  // Validate the filter has been collapsed
  await expect(iframe.getByRole("link", { name: "Advanced" })).toBeVisible();
});
