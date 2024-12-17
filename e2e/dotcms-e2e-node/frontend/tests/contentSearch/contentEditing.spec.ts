import {expect, Page, test} from '@playwright/test';
import {dotCMSUtils, waitForVisibleAndCallback} from '../../utils/dotCMSUtils';
import {
    GroupEntriesLocators,
    MenuEntriesLocators,
    ToolEntriesLocators
} from '../../locators/navigation/menuLocators';
import {ContentUtils} from "../../utils/contentUtils";
import {iFramesLocators, contentGeneric, fileAsset} from "../../locators/globalLocators";
import {genericContent1, contentProperties, fileAssetContent} from "./contentData";
import {assert} from "console";

const cmsUtils = new dotCMSUtils();

/**
 * Test to navigate to the content portlet and login to the dotCMS instance
 * @param page
 */
test.beforeEach('Navigate to content portlet', async ({page}) => {
    // Instance the menu Navigation locators
    const menuLocators = new MenuEntriesLocators(page);
    const groupsLocators = new GroupEntriesLocators(page);
    const toolsLocators = new ToolEntriesLocators(page);

    // Get the username and password from the environment variables
    const username = process.env.USERNAME as string;
    const password = process.env.PASSWORD as string;

    // Login to dotCMS
    await cmsUtils.login(page, username, password);
    await cmsUtils.navigate(menuLocators.EXPAND, groupsLocators.CONTENT, toolsLocators.SEARCH_ALL);

    // Validate the portlet title
    const breadcrumbLocator = page.locator('p-breadcrumb');
    await waitForVisibleAndCallback(breadcrumbLocator, () => expect(breadcrumbLocator).toContainText('Search All'));
});

/**
 * test to add a new piece of content (generic content)
 */
test('Add a new pice of content', async ({page}) => {
    const contentUtils = new ContentUtils(page);
    const iframe = page.frameLocator(iFramesLocators.main_iframe);

    // Adding new rich text content
    await contentUtils.addNewContentAction(page, contentGeneric.locator, contentGeneric.label);
    await contentUtils.fillRichTextForm(page, genericContent1.title, genericContent1.body, contentProperties.publishWfAction);
    await contentUtils.workflowExecutionValidationAndClose(page, 'Content saved');

    await waitForVisibleAndCallback(iframe.locator('#results_table tbody tr').first(), async () => {});

    await contentUtils.validateContentExist(page, genericContent1.title).then(assert);
});

/**
 * Test to edit an existing piece of content
 */
test('Edit a piece of content', async ({page}) => {
    const contentUtils = new ContentUtils(page);
    const iframe = page.frameLocator(iFramesLocators.main_iframe);

    // Edit the content
    await contentUtils.editContent(page, genericContent1.title, genericContent1.newTitle, genericContent1.newBody, contentProperties.publishWfAction);
    await waitForVisibleAndCallback(iframe.locator('#results_table tbody tr').first(), async () => {});

    await contentUtils.validateContentExist(page, genericContent1.newTitle).then(assert);
});


/**
 * Test to delete an existing piece of content
 */
test('Delete a piece of content', async ({ page }) => {
        const contentUtils = new ContentUtils(page);
        // Delete the content
        await contentUtils.deleteContent(page, genericContent1.newTitle);
    }
);

/**
 * Test to make sure we are validating the required of text fields on the content creation
 * */
test('Validate required on text fields', async ({page}) => {
    const contentUtils = new ContentUtils(page);
    const iframe = page.frameLocator(iFramesLocators.main_iframe).first();

    await contentUtils.addNewContentAction(page, contentGeneric.locator, contentGeneric.label);
    await contentUtils.fillRichTextForm(page, '', genericContent1.body, contentProperties.publishWfAction);
    await expect(iframe.getByText('Error x')).toBeVisible();
    await expect(iframe.getByText('The field Title is required.')).toBeVisible();
});

/**
 * Test to make sure we are validating the required of blockEditor fields on the content creation
 */
test('Validate required on blockContent fields', async ({page}) => {
    const contentUtils = new ContentUtils(page);
    const iframe = page.frameLocator(iFramesLocators.main_iframe).first();

    await contentUtils.addNewContentAction(page, contentGeneric.locator, contentGeneric.label);
    await contentUtils.fillRichTextForm(page, genericContent1.title, '', contentProperties.publishWfAction);
    await expect(iframe.getByText('Error x')).toBeVisible();
    await expect(iframe.getByText('The field Title is required.')).toBeVisible();
});

/**
 * Test to validate you are able to add file assets importing from url
 */
test('Validate you are able to add file assets importing from url', async ({page}) => {
    const contentUtils = new ContentUtils(page);
    const iframe = page.frameLocator(iFramesLocators.main_iframe);

    await contentUtils.addNewContentAction(page, fileAsset.locator, fileAsset.label);
    await contentUtils.fillFileAssetForm(page, fileAssetContent.host, fileAssetContent.title, contentProperties.publishWfAction, null, fileAssetContent.fromURL );
    //fileName?: string, fromURL?: string, newFileName?: string, newFileText?: string) {
    await contentUtils.workflowExecutionValidationAndClose(page, 'Content saved');
    await contentUtils.validateContentExist(page, fileAssetContent.title).then(assert);
});