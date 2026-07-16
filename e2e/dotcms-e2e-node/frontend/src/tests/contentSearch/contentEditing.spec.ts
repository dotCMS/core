import { expect, test } from "@playwright/test";
import { LoginPage } from "@pages";
import { SideMenuComponent } from "@components/sideMenu.component";
import { waitForVisibleAndCallback } from "@utils/utils";
import { ContentPage } from "@pages";
import {
  iFramesLocators,
  contentGeneric,
  fileAsset,
  pageAsset,
} from "../../locators/globalLocators";
import {
  genericContent1,
  contentProperties,
  fileAssetContent,
  pageAssetContent,
} from "./contentData";
import { assert } from "console";

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
 * test to add a new piece of content (generic content)
 */
test.skip("Add a new Generic content", async ({ page }) => {
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
  await waitForVisibleAndCallback(
    iframe.locator("#results_table tbody tr").first(),
  );
  await contentUtils.validateContentExist(genericContent1.title).then(assert);
});

/**
 * Test to edit an existing piece of content and make sure you can discard the changes
 */
test.skip("Edit a generic content and discard changes", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(contentGeneric.locator);
  await contentUtils.editContent({
    title: genericContent1.title,
    newTitle: genericContent1.newTitle,
    newBody: "genericContent1",
  });
  await waitForVisibleAndCallback(page.getByTestId("close-button"), () =>
    page.getByTestId("close-button").click(),
  );
  await waitForVisibleAndCallback(
    page.getByRole("button", { name: "Close" }),
    () => page.getByRole("button", { name: "Close" }).click(),
  );
  await waitForVisibleAndCallback(
    iframe.locator("#results_table tbody tr").first(),
  );
  await contentUtils.validateContentExist(genericContent1.title).then(assert);
});

/**
 * Test to edit an existing piece of content
 */
test.skip("Edit a generic content", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(contentGeneric.locator);
  await contentUtils.editContent({
    title: genericContent1.title,
    newTitle: genericContent1.newTitle,
    newBody: genericContent1.newBody,
    action: contentProperties.publishWfAction,
  });
  await waitForVisibleAndCallback(
    iframe.locator("#results_table tbody tr").first(),
  );
  await contentUtils
    .validateContentExist(genericContent1.newTitle)
    .then(assert);
});

/**
 * Test to delete an existing piece of content
 */
test.skip("Delete a generic of content", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  await contentUtils.deleteContent(genericContent1.newTitle);
});

/**
 * Test to make sure we are validating the required of text fields on the content creation
 * */
test.skip("Validate required on text fields", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const iframe = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    contentGeneric.locator,
    contentGeneric.label,
  );
  await contentUtils.fillRichTextForm({
    title: "",
    body: genericContent1.body,
    action: contentProperties.publishWfAction,
  });
  await expect(iframe.getByText("Error x")).toBeVisible();
  await expect(iframe.getByText("The field Title is required.")).toBeVisible();
});

/** Please enable after fixing the issue #30748
 * Test to make sure we are validating the required of blockEditor fields on the content creation
 */
/**
 test('Validate required on blockContent fields', async ({page}) => {
 const contentUtils = new ContentPage(page);
 const iframe = page.frameLocator(iFramesLocators.main_iframe).first();

 await contentUtils.addNewContentAction(page, contentGeneric.locator, contentGeneric.label);
 await contentUtils.fillRichTextForm(page, genericContent1.title, '', contentProperties.publishWfAction);
 await expect(iframe.getByText('Error x')).toBeVisible();
 await expect(iframe.getByText('The field Title is required.')).toBeVisible();
 });
 */

/**
 * Test to validate you are able to add file assets importing from url
 */
test.skip("Validate adding file assets from URL", async ({ page }) => {
  const contentUtils = new ContentPage(page);

  await contentUtils.addNewContentAction(fileAsset.locator, fileAsset.label);
  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    editContent: true,
    action: contentProperties.publishWfAction,
    fromURL: fileAssetContent.fromURL,
  });
  await contentUtils.workflowExecutionValidationAndClose("Content saved");
  await expect(
    contentUtils.validateContentExist("DotCMS-logo.svg"),
  ).resolves.toBeTruthy();
});

/**
 * Test to validate you are able to add file assets creating a new file
 */
test.skip("Validate you are able to add file assets creating a new file", async ({
  page,
}) => {
  const contentUtils = new ContentPage(page);

  await contentUtils.addNewContentAction(fileAsset.locator, fileAsset.label);
  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileText,
  });
  await contentUtils.workflowExecutionValidationAndClose("Content saved");
  await contentUtils
    .validateContentExist(fileAssetContent.newFileName)
    .then(assert);
});

/**
 * Test to validate you are able to edit file assets text
 */
test.skip("Validate you can edit text on binary fields", async ({ page }) => {
  const contentUtils = new ContentPage(page);

  await contentUtils.selectTypeOnFilter(fileAsset.locator);
  const contentElement = await contentUtils.getContentElement(
    fileAssetContent.newFileName,
  );
  await contentElement.click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );

  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    editContent: true,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileTextEdited,
  });
  await contentUtils.workflowExecutionValidationAndClose("Content saved");

  await contentUtils.selectTypeOnFilter(fileAsset.locator);
  await (
    await contentUtils.getContentElement(fileAssetContent.newFileName)
  ).click();
  const editIframe = page.frameLocator(iFramesLocators.dot_edit_iframe);
  await expect(editIframe.getByRole("code")).toHaveText(
    fileAssetContent.newFileTextEdited,
  );
});

/**
 * Test to validate you are able to remove file assets from the content
 */
test.skip("Validate you are able to delete file on binary fields", async ({
  page,
}) => {
  const contentUtils = new ContentPage(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(fileAsset.locator);
  await waitForVisibleAndCallback(mainFrame.locator("#contentWrapper"));
  const contentElement = await contentUtils.getContentElement(
    fileAssetContent.newFileName,
  );
  await contentElement.click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );

  const detailFrame = page.frameLocator(iFramesLocators.dot_edit_iframe);
  await detailFrame.getByRole("button", { name: " Remove" }).click();
  await waitForVisibleAndCallback(
    detailFrame.getByTestId("ui-message-icon-container"),
  );
  await detailFrame.getByText("Publish", { exact: true }).click();
  await expect(detailFrame.getByText("The field File Asset is")).toBeVisible();
});

/**
 * Test to validate the get info on of the binary field on file assets
 */
test.skip("Validate file assets show corresponding information", async ({
  page,
}) => {
  const contentUtils = new ContentPage(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(fileAsset.locator);
  await waitForVisibleAndCallback(mainFrame.locator("#contentWrapper"));
  await (
    await contentUtils.getContentElement(fileAssetContent.newFileName)
  ).click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );

  const detailFrame = page.frameLocator(iFramesLocators.dot_edit_iframe);
  await detailFrame.getByTestId("info-btn").click();
  await waitForVisibleAndCallback(detailFrame.getByText("Bytes"));
  await expect(detailFrame.getByText("Bytes")).toBeVisible();
  await expect(detailFrame.getByTestId("resource-link-FileLink")).toContainText(
    "http",
  );
  await expect(
    detailFrame.getByTestId("resource-link-Resource-Link"),
  ).not.toBeEmpty();
  await expect(
    detailFrame.getByTestId("resource-link-VersionPath"),
  ).not.toBeEmpty();
  await expect(detailFrame.getByTestId("resource-link-IdPath")).not.toBeEmpty();
});

//* Test to validate the download of binary fields on file assets
test.skip("Validate the download of binary fields on file assets", async ({
                                                                       page,
                                                                     }) => {
  const contentUtils = new ContentPage(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(fileAsset.locator);
  await waitForVisibleAndCallback(mainFrame.locator("#contentWrapper"));

  // ✅ Add null check and retry logic
  const contentElement = await contentUtils.getContentElement(fileAssetContent.newFileName);
  if (!contentElement) {
    throw new Error(`Content element not found: ${fileAssetContent.newFileName}`);
  }

  // ✅ Ensure element is visible and clickable before clicking
  await contentElement.waitFor({ state: 'visible', timeout: 10000 });
  await contentElement.waitFor({ state: 'attached', timeout: 5000 });
  await contentElement.click();

  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );
  const detailFrame = page.frameLocator(iFramesLocators.dot_edit_iframe);
  const downloadLink = detailFrame.getByTestId("download-btn");
  await contentUtils.validateDownload(downloadLink);
});
/**
 * Test to validate the required on file asset fields
 */
test.skip("Validate the required on file asset fields", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(fileAsset.locator, fileAsset.label);
  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
  });
  await waitForVisibleAndCallback(detailsFrame.getByText("Error x"));
  const errorMessage = detailsFrame.getByText("The field File Asset is");
  await waitForVisibleAndCallback(errorMessage, () =>
    expect(errorMessage).toBeVisible(),
  );
});

/**
 * Test to validate the auto complete on FileName field accepting the name change
 */
test.skip("Validate the auto complete on FileName field accepting change", async ({
  page,
}) => {
  const contentUtils = new ContentPage(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(fileAsset.locator, fileAsset.label);
  await detailsFrame.locator("#fileName").fill("test");
  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileText,
  });
  const replaceText = detailsFrame.getByText("Do you want to replace the");
  await waitForVisibleAndCallback(replaceText, () =>
    expect(replaceText).toBeVisible(),
  );
  await detailsFrame.getByLabel("Yes").click();
  await expect(detailsFrame.locator("#fileName")).toHaveValue(
    fileAssetContent.newFileName,
  );
});

/**
 * Test to validate the auto complete on FileName field rejecting file name change
 */
test.skip("Validate the auto complete on FileName field rejecting change", async ({
  page,
}) => {
  const contentUtils = new ContentPage(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(fileAsset.locator, fileAsset.label);
  await detailsFrame.locator("#fileName").fill("test");
  await contentUtils.fillFileAssetForm({
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileText,
  });
  const replaceText = detailsFrame.getByText("Do you want to replace the");
  await waitForVisibleAndCallback(replaceText, () =>
    expect(replaceText).toBeVisible(),
  );
  await detailsFrame.getByLabel("No").click();
  await expect(detailsFrame.locator("#fileName")).toHaveValue("test");
});

/**
 * Test to validate the title is not changing in the auto complete on FileName
 */
/** ENABLE AS SOON WE FIXED THE ISSUE #30945
 test('Validate the title field is not changing in the file asset auto complete', async ({page}) => {
 const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);
 const contentUtils = new ContentPage(page);

 await contentUtils.addNewContentAction(page, fileAsset.locator, fileAsset.label);
 await detailsFrame.locator('#title').fill('test');
 await contentUtils.fillFileAssetForm(page, fileAssetContent.host, fileAssetContent.title, null, null, null, fileAssetContent.newFileName, fileAssetContent.newFileText);

 await expect(detailsFrame.locator('#title')).toHaveValue('test');
 });
 */

/**
 * Test to validate you are able to delete a file asset content
 */
test.skip("Delete a file asset content", async ({ page }) => {
  await new ContentPage(page).deleteContent(fileAssetContent.title);
});

/**
 * Test to validate you are able to add new pages
 */
test.skip("Add a new page", async ({ page }) => {
  const contentUtils = new ContentPage(page);

  await contentUtils.addNewContentAction(pageAsset.locator, pageAsset.label);
  await contentUtils.fillPageAssetForm({
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    friendlyName: pageAssetContent.friendlyName,
    showOnMenu: pageAssetContent.showOnMenu,
    sortOrder: pageAssetContent.sortOrder,
    cacheTTL: pageAssetContent.cacheTTL,
    action: contentProperties.publishWfAction,
  });

  const breadcrumbLocator = page.getByTestId("breadcrumb-title");
  await expect(breadcrumbLocator).toContainText(pageAssetContent.title);
});

/**
 * Test to validate the URL is unique on pages
 */
test.skip("Validate URL is unique on pages", async ({ page }) => {
  const contentUtils = new ContentPage(page);

  await contentUtils.addNewContentAction(pageAsset.locator, pageAsset.label);
  await contentUtils.fillPageAssetForm({
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    friendlyName: pageAssetContent.friendlyName,
    showOnMenu: pageAssetContent.showOnMenu,
    sortOrder: pageAssetContent.sortOrder,
    cacheTTL: pageAssetContent.cacheTTL,
    action: contentProperties.publishWfAction,
  });

  const iframe = page.frameLocator(iFramesLocators.dot_iframe);
  await expect(iframe.getByText("Another Page with the same")).toBeVisible();
});

/**
 * Test to validate the required fields on the page form
 */
test.skip("Validate required fields on page asset", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const detailFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(pageAsset.locator, pageAsset.label);
  await contentUtils.fillPageAssetForm({
    title: "",
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    showOnMenu: pageAssetContent.showOnMenu,
    action: contentProperties.publishWfAction,
  });
  await waitForVisibleAndCallback(detailFrame.getByText("Error x"));

  await expect(
    detailFrame.getByText("The field Title is required."),
  ).toBeVisible();
  await expect(
    detailFrame.getByText("The field Url is required."),
  ).toBeVisible();
  await expect(
    detailFrame.getByText("The field Friendly Name is"),
  ).toBeVisible();
});

/**
 * Test to validate the auto generation of fields on page asset
 */
test.skip("Validate auto generation of fields on page asset", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  const detailFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(pageAsset.locator, pageAsset.label);
  await contentUtils.fillPageAssetForm({
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    showOnMenu: pageAssetContent.showOnMenu,
  });

  await expect(detailFrame.locator("#url")).toHaveValue(
    pageAssetContent.title.toLowerCase(),
  );
  await expect(detailFrame.locator("#friendlyName")).toHaveValue(
    pageAssetContent.title,
  );
});

/**
 * Test to validate you are able to unpublish a page asset
 */
test.skip("Validate you are able to unpublish pages", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  await contentUtils.selectTypeOnFilter(pageAsset.locator);
  await contentUtils.performWorkflowAction(
    pageAssetContent.title,
    contentProperties.unpublishWfAction,
  );
  await contentUtils.getContentState(pageAssetContent.title).then(assert);
});

/**
 * Test to validate you are able to delete pages
 */
test.skip("Validate you are able to delete pages", async ({ page }) => {
  const contentUtils = new ContentPage(page);
  await contentUtils.selectTypeOnFilter(pageAsset.locator);
  await contentUtils.deleteContent(pageAssetContent.title);
});

/**
 * Test to do a regression according the axe-standards on accessibility
 */
/**
test('accessibility test', async ({page}) => {
    const accessibility = new accessibilityUtils(page);
    const accessibilityScanResults = await accessibility.generateReport(page, accessibilityReport.name);
    expect(accessibilityScanResults.violations).toEqual([]); // 5
});
 */
