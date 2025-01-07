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

  await waitForVisibleAndCallback(iframe.locator("#results_table tbody tr").first());

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
  await waitForVisibleAndCallback(iframe.locator("#results_table tbody tr").first());
  await contentUtils
    .validateContentExist(page, genericContent1.newTitle)
    .then(assert);
});

/**
 * Test to delete an existing piece of content
 */
test("Delete a generic of content", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  // Delete the content
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
test("Validate you are able to add file assets importing from url", async ({
  page,
}) => {
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  const params = {
    page: page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    fromURL: fileAssetContent.fromURL,
  };
  await contentUtils.fillFileAssetForm(params);
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
  const params = {
    page: page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
    newFileName: fileAssetContent.newFileName,
    newFileText: fileAssetContent.newFileText,
  };
  await contentUtils.fillFileAssetForm(params);
  await contentUtils.workflowExecutionValidationAndClose(page, "Content saved");
  await contentUtils
    .validateContentExist(page, fileAssetContent.newFileName)
    .then(assert);
});

/**
 * Test to validate the required on file asset fields
 */
test("Validate the required on file asset fields", async ({ page }) => {
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  const params = {
    page: page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    action: contentProperties.publishWfAction,
  };
  await contentUtils.fillFileAssetForm(params);
  await waitForVisibleAndCallback(detailsFrame.getByText("Error x"));
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
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await detailsFrame.locator("#fileName").fill("test");
  const params = {
    page: page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    newFileName: fileAssetContent.newFileName,
    newFileText: fileAssetContent.newFileText,
  };
  await contentUtils.fillFileAssetForm(params);
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
  const detailsFrame = page.frameLocator(iFramesLocators.dot_iframe);
  const contentUtils = new ContentUtils(page);

  await contentUtils.addNewContentAction(
    page,
    fileAsset.locator,
    fileAsset.label,
  );
  await detailsFrame.locator("#fileName").fill("test");
  const params = {
    page: page,
    host: fileAssetContent.host,
    title: fileAssetContent.title,
    newFileName: fileAssetContent.newFileName,
    newFileText: fileAssetContent.newFileText,
  };
  await contentUtils.fillFileAssetForm(params);
  const replaceText = detailsFrame.getByText("Do you want to replace the");
  await waitForVisibleAndCallback(replaceText, async () =>
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
  const contentUtils = new ContentUtils(page);
  // Delete the content
  await contentUtils.deleteContent(page, fileAssetContent.title);
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
  const params = {
    page: page,
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    friendlyName: pageAssetContent.friendlyName,
    showOnMenu: pageAssetContent.showOnMenu,
    sortOrder: pageAssetContent.sortOrder,
    cacheTTL: pageAssetContent.cacheTTL,
    action: contentProperties.publishWfAction,
  };
  await contentUtils.fillPageAssetForm(params);
  const dataFrame = page.frameLocator(iFramesLocators.dataTestId);
  await waitForVisibleAndCallback(dataFrame.getByRole("banner"));
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
  const params = {
    page: page,
    title: "",
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    showOnMenu: pageAssetContent.showOnMenu,
    action: contentProperties.publishWfAction,
  };
  await contentUtils.fillPageAssetForm(params);
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
test("Validate auto generation of fields on page asset", async ({ page }) => {
  const contentUtils = new ContentUtils(page);
  const detailFrame = page.frameLocator(iFramesLocators.dot_iframe);

  await contentUtils.addNewContentAction(
    page,
    pageAsset.locator,
    pageAsset.label,
  );
  const params = {
    page: page,
    title: pageAssetContent.title,
    host: pageAssetContent.host,
    template: pageAssetContent.template,
    showOnMenu: pageAssetContent.showOnMenu,
  };
  await contentUtils.fillPageAssetForm(params);

  await expect(detailFrame.locator("#url")).toHaveValue(
    pageAssetContent.title.toLowerCase(),
  );
  await expect(detailFrame.locator("#friendlyName")).toHaveValue(
    pageAssetContent.title,
  );
});
