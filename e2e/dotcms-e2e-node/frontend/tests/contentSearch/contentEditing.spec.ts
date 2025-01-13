import { expect, test } from "@playwright/test";
import {
  dotCMSUtils,
  waitForVisibleAndCallback,
} from "../../utils/dotCMSUtils";
import {
  GroupEntriesLocators,
  MenuEntriesLocators,
  ToolEntriesLocators,
} from "../../locators/navigation/menuLocators";
import { ContentUtils } from "../../utils/contentUtils";
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
  const cmsUtils = new dotCMSUtils();

  const menuLocators = new MenuEntriesLocators(page);
  const groupsLocators = new GroupEntriesLocators(page);
  const toolsLocators = new ToolEntriesLocators(page);

  // Get the username and password from the environment variables
  const username = process.env.USERNAME as string;
  const password = process.env.PASSWORD as string;

  // Login to dotCMS
  await cmsUtils.login(page, username, password);
  await cmsUtils.navigate(
    menuLocators.EXPAND,
    groupsLocators.CONTENT,
    toolsLocators.SEARCH_ALL,
  );

  // Validate the portlet title
  const breadcrumbLocator = page.locator("p-breadcrumb");
  await waitForVisibleAndCallback(breadcrumbLocator, () =>
    expect(breadcrumbLocator).toContainText("Search All"),
  );
});

/**
 * test to add a new piece of content (generic content)
 */
test("Add a new Generic content", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  // Adding new rich text content
  await contentUtils.addNewContentAction(
    page,
    contentGeneric.locator,
    contentGeneric.label,
  );
  await contentUtils.fillRichTextForm(
    page,
    genericContent1.title,
    genericContent1.body,
    contentProperties.publishWfAction,
  );
  await contentUtils.workflowExecutionValidationAndClose(page, "Content saved");

  await waitForVisibleAndCallback(
    iframe.locator("#results_table tbody tr").first(),
    async () => {},
  );

  await contentUtils
    .validateContentExist(page, genericContent1.title)
    .then(assert);
});

/**
 * Test to edit an existing piece of content
 */
test("Edit a generic content", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const iframe = page.frameLocator(iFramesLocators.main_iframe);

  // Edit the content
  await contentUtils.selectTypeOnFilter(page, contentGeneric.locator);
  await contentUtils.editContent(
    page,
    genericContent1.title,
    genericContent1.newTitle,
    genericContent1.newBody,
    contentProperties.publishWfAction,
  );
  await waitForVisibleAndCallback(
    iframe.locator("#results_table tbody tr").first(),
    async () => {},
  );
  await contentUtils
    .validateContentExist(page, genericContent1.newTitle)
    .then(assert);
});

/**
 * Test to delete an existing piece of content
 */
test("Delete a generic of content", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  await contentUtils.deleteContent(page, genericContent1.newTitle);
});

/**
 * Test to make sure we are validating the required of text fields on the content creation
 * */
test("Validate required on text fields", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const iframe = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    contentGeneric.locator,
    contentGeneric.label,
  );
  await contentUtils.fillRichTextForm(
    page,
    "",
    genericContent1.body,
    contentProperties.publishWfAction,
  );
  await expect(iframe.getByText("Error x")).toBeVisible();
  await expect(iframe.getByText("The field Title is required.")).toBeVisible();
});

/** Please enable after fixing the issue #30748
 * Test to make sure we are validating the required of blockEditor fields on the content creation
 */
/**
 test('Validate required on blockContent fields', async ({page}) => {
 const contentUtils = new ContentUtils(page);
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
test("Validate adding file assets from URL", async ({ page }) => {
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await contentUtils.fillFileAssetForm({
    page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    editContent: true,
    action: contentProperties.publishWfAction,
    fromURL: fileAssetContent.fromURL,
  });
  await contentUtils.workflowExecutionValidationAndClose(page, "Content saved");
  await expect(
    contentUtils.validateContentExist(page, "DotCMS-logo.svg"),
  ).resolves.toBeTruthy();
});

/**
 * Test to validate you are able to add file assets creating a new file
 */
test("Validate you are able to add file assets creating a new file", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await contentUtils.fillFileAssetForm({
    page,
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileText,
  });
  await contentUtils.workflowExecutionValidationAndClose(page, "Content saved");
  await contentUtils
    .validateContentExist(page, fileAssetContent.newFileName)
    .then(assert);
});

/**
 * Test to validate you are able to edit file assets text
 */
test("Validate you can edit text on binary fields", async ({ page }) => {
  const contentUtils = new ContentUtils(page);

  await contentUtils.selectTypeOnFilter(page, fileAsset.locator);
  const contentElement = await contentUtils.getContentElement(
    page,
    fileAssetContent.newFileName,
  );
  await contentElement.click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );

  await contentUtils.fillFileAssetForm({
    page,
    host: fileAssetContent.host,
    editContent: true,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    binaryFileName: fileAssetContent.newFileName,
    binaryFileText: fileAssetContent.newFileTextEdited,
  });
  await contentUtils.workflowExecutionValidationAndClose(page, "Content saved");

  await contentUtils.selectTypeOnFilter(page, fileAsset.locator);
  await (
    await contentUtils.getContentElement(page, fileAssetContent.newFileName)
  ).click();
  const editIframe = page.frameLocator(iFramesLocators.dot_edit_iframe);
  await expect(editIframe.getByRole("code")).toHaveText(
    fileAssetContent.newFileTextEdited,
  );
});

/**
 * Test to validate you are able to remove file assets from the content
 */
test("Validate you are able to delete file on binary fields", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(page, fileAsset.locator);
  await waitForVisibleAndCallback(
    mainFrame.locator("#contentWrapper"),
    async () => {},
  );
  const contentElement = await contentUtils.getContentElement(
    page,
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
    async () => {},
  );
  await detailFrame.getByText("Publish", { exact: true }).click();
  await expect(detailFrame.getByText("The field File Asset is")).toBeVisible();
});

/**
 * Test to validate the get info on of the binary field on file assets
 */
test("Validate file assets show corresponding information", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(page, fileAsset.locator);
  await waitForVisibleAndCallback(
    mainFrame.locator("#contentWrapper"),
    async () => {},
  );
  await (
    await contentUtils.getContentElement(page, fileAssetContent.newFileName)
  ).click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );

  const detailFrame = page.frameLocator(iFramesLocators.dot_edit_iframe);
  await detailFrame.getByTestId("info-btn").click();
  await waitForVisibleAndCallback(
    detailFrame.getByText("Bytes"),
    async () => {},
  );
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
test("Validate the download of binary fields on file assets", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);
  const mainFrame = page.frameLocator(iFramesLocators.main_iframe);

  await contentUtils.selectTypeOnFilter(page, fileAsset.locator);
  await waitForVisibleAndCallback(
    mainFrame.locator("#contentWrapper"),
    async () => {},
  );
  await (
    await contentUtils.getContentElement(page, fileAssetContent.newFileName)
  ).click();
  await waitForVisibleAndCallback(page.getByRole("heading"), () =>
    expect.soft(page.getByRole("heading")).toContainText(fileAsset.label),
  );
  const detailFrame = page.frameLocator(iFramesLocators.dot_edit_iframe);
  const downloadLink = detailFrame.getByTestId("download-btn");
  await contentUtils.validateDownload(page, downloadLink);
});

/**
 * Test to validate the required on file asset fields
 */
test("Validate the required on file asset fields", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await contentUtils.fillFileAssetForm({
    page,
    host: fileAssetContent.host,
    editContent: false,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
  });
  await waitForVisibleAndCallback(
    detailsFrame.getByText("Error x"),
    async () => {},
  );
  const errorMessage = detailsFrame.getByText("The field File Asset is");
  await waitForVisibleAndCallback(errorMessage, () =>
    expect(errorMessage).toBeVisible(),
  );
});

/**
 * Test to validate the auto complete on FileName field accepting the name change
 */
test("Validate the auto complete on FileName field accepting change", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await detailsFrame.locator("#fileName").fill("test");
  await contentUtils.fillFileAssetForm({
    page,
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
test("Validate the auto complete on FileName field rejecting change", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await detailsFrame.locator("#fileName").fill("test");
  await contentUtils.fillFileAssetForm({
    page,
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
 const contentUtils = new ContentUtils(page);

 await contentUtils.addNewContentAction(page, fileAsset.locator, fileAsset.label);
 await detailsFrame.locator('#title').fill('test');
 await contentUtils.fillFileAssetForm(page, fileAssetContent.host, fileAssetContent.title, null, null, null, fileAssetContent.newFileName, fileAssetContent.newFileText);

 await expect(detailsFrame.locator('#title')).toHaveValue('test');
 });
 */

/**
 * Test to validate you are able to delete a file asset content
 */
test("Delete a file asset content", async ({ page }) => {
  await new ContentUtils(page).deleteContent(page, fileAssetContent.title);
});

/**
 * Test to validate you are able to add new pages
 */
test("Add a new page", async ({ page }) => {
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    pageAsset.locator,
    pageAsset.label,
  );
  await contentUtils.fillPageAssetForm({
    page: page,
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    friendlyName: pageAssetContent.friendlyName,
    showOnMenu: pageAssetContent.showOnMenu,
    sortOrder: pageAssetContent.sortOrder,
    cacheTTL: pageAssetContent.cacheTTL,
    action: contentProperties.publishWfAction,
  });
  const dataFrame = page.frameLocator(iFramesLocators.dataTestId);
  await waitForVisibleAndCallback(
    dataFrame.getByRole("banner"),
    async () => {},
  );
  await expect(page.locator("ol")).toContainText(
    "Pages" + pageAssetContent.title,
  );
});

/**
 * Test to validate the required fields on the page form
 */
test("Validate required fields on page asset", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const detailFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    pageAsset.locator,
    pageAsset.label,
  );
  await contentUtils.fillPageAssetForm({
    page,
    title: "",
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    showOnMenu: pageAssetContent.showOnMenu,
    action: contentProperties.publishWfAction,
  });
  await waitForVisibleAndCallback(
    detailFrame.getByText("Error x"),
    async () => {},
  );

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
test("Validate auto generation of fields on page asset", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const detailFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    pageAsset.locator,
    pageAsset.label,
  );
  await contentUtils.fillPageAssetForm({
    page,
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
test("Validate you are able to unpublish pages", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  await contentUtils.selectTypeOnFilter(page, pageAsset.locator);
  await contentUtils.performWorkflowAction(
    page,
    pageAssetContent.title,
    contentProperties.unpublishWfAction,
  );
  await contentUtils.getContentState(page, pageAssetContent.title).then(assert);
});

/**
 * Test to validate you are able to delete pages
 */
test("Validate you are able to delete pages", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  await contentUtils.selectTypeOnFilter(page, pageAsset.locator);
  await contentUtils.deleteContent(page, pageAssetContent.title);
});
