import {Page, expect, FrameLocator} from '@playwright/test';
import { iFramesLocators, richText } from '../locators/globalLocators';


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
        await expect.soft( page.getByRole('heading')).toContainText(richText.label);

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
        await page.getByTestId('close-button').getByRole('button').click();
    }

    /**
     * Add new content action on the content portlet
     * @param page
     * @param typeLocator
     * @param typeString
     */
    async addNewContentAction(page: Page, typeLocator: string, typeString: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);
        await expect(iframe.locator('#structure_inode')).toBeVisible();
        await page.waitForTimeout(1000);
        await iframe.locator('#widget_structure_inode div').first().click();
        await page.waitForTimeout(1000);
        await iframe.getByText(typeLocator).click();
        await iframe.locator('#dijit_form_DropDownButton_0').click();
        await expect(iframe.getByLabel('actionPrimaryMenu')).toBeVisible();
        await iframe.getByLabel('▼').getByText('Add New Content').click();
        await expect(page.getByRole('heading')).toHaveText(typeString);
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
        await iframe.getByRole('button', { name: 'createOptions' }).click();

        //Validate the search button has a sub-menu
        await expect (iframe.getByLabel('Search ▼').getByText('Search')).toBeVisible();
        await expect (iframe.getByText('Show Query')).toBeVisible();

        // Click on show query
        await iframe.getByText('Show Query').click();
    }
}


