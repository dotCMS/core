import {Page, expect, FrameLocator} from '@playwright/test';
import { iFramesLocators, richText } from '../locators/globalLocators';
import { waitForVisibleAndCallback} from './dotCMSUtils';

export class ContentUtils {
    page: Page;
    constructor(page: Page) {
      this.page = page;
    }

    /**
     * Fill the rich text form
      * @param page
     * @param title
     * @param body
     * @param action
     */
    async fillRichTextForm(page: Page, title: string, body: string, action: string) {
        const dotIframe = page.frameLocator(iFramesLocators.dot_iframe);

        const headingLocator = page.getByRole('heading');
        await waitForVisibleAndCallback(headingLocator, () => expect.soft(headingLocator).toContainText(richText.label));

        //Fill title
        await dotIframe.locator('#title').fill(title);
        //Fill body
        await dotIframe.locator('#block-editor-body div').nth(1).fill(body);

        //await dotIframe.locator(iFramesLocators.wysiwygFrame).contentFrame().locator('#tinymce').fill(body);
        //Click on action
        await dotIframe.getByText(action).click();
        //Wait for the content to be saved

        await expect(dotIframe.getByText('Content saved')).toBeVisible({ timeout: 9000 });
        await expect(dotIframe.getByText('Content saved')).toBeHidden();
        //Click on close
        const closeBtnLocator = page.getByTestId('close-button').getByRole('button');
        await waitForVisibleAndCallback(closeBtnLocator, () => closeBtnLocator.click());
    }

    /**
     * Add new content action on the content portlet
     * @param page
     * @param typeLocator
     * @param typeString
     */
    async addNewContentAction(page: Page, typeLocator: string, typeString: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);
        const structureINodeLocator = iframe.locator('#structure_inode');
        await waitForVisibleAndCallback(structureINodeLocator, () => expect(structureINodeLocator).toBeVisible());
        //TODO remove this
        await page.waitForTimeout(1000);
        const structureINodeDivLocator = iframe.locator('#widget_structure_inode div').first();
        await waitForVisibleAndCallback(structureINodeDivLocator, () => structureINodeDivLocator.click());
        //TODO remove this
        await page.waitForTimeout(1000);
        const typeLocatorByTextLocator = iframe.getByText(typeLocator);
        await waitForVisibleAndCallback(typeLocatorByTextLocator, () => typeLocatorByTextLocator.click());

        await iframe.locator('#dijit_form_DropDownButton_0').click();
        await expect(iframe.getByLabel('actionPrimaryMenu')).toBeVisible();
        await iframe.getByLabel('▼').getByText('Add New Content').click();
        const headingLocator = page.getByRole('heading');
        await waitForVisibleAndCallback(headingLocator, () => expect(headingLocator).toHaveText(typeString));
    };

    /**
     * Select content type on filter on the content portlet
     * @param page
     * @param typeLocator
     * @param typeString
     */
    async selectTypeOnFilter(page: Page, typeLocator: string, typeString: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await expect.soft(iframe.locator('#structure_inode')).toBeVisible();
        await iframe.locator('#widget_structure_inode div').first().click();
        await iframe.getByText(typeLocator).click();
    }

    /**
     * Show query on the content portlet
     * @param iframe
     */
    async showQuery(iframe : FrameLocator) {
        const createOptionsBtnLocator = iframe.getByRole('button', { name: 'createOptions' });
        await waitForVisibleAndCallback(createOptionsBtnLocator, () => createOptionsBtnLocator.click());

        //Validate the search button has a sub-menu
        await expect (iframe.getByLabel('Search ▼').getByText('Search')).toBeVisible();
        await expect (iframe.getByText('Show Query')).toBeVisible();

        // Click on show query
        await iframe.getByText('Show Query').click();
    }
}


