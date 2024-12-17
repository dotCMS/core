import {expect, FrameLocator, Locator, Page} from '@playwright/test';
import {contentGeneric, iFramesLocators} from '../locators/globalLocators';
import {waitForVisibleAndCallback} from './dotCMSUtils';
import {contentProperties} from "../tests/contentSearch/contentData";

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
        await waitForVisibleAndCallback(headingLocator, () => expect.soft(headingLocator).toContainText(contentGeneric.label));

        //Fill title
        await dotIframe.locator('#title').fill(title);
        //Fill body
        await dotIframe.locator('#block-editor-body div').nth(1).fill(body);

        //await dotIframe.locator(iFramesLocators.wysiwygFrame).contentFrame().locator('#tinymce').fill(body);
        //Click on action
        await dotIframe.getByText(action).first().click();
        //Wait for the content to be saved

        await expect(dotIframe.getByText('Content saved')).toBeVisible({timeout: 9000});
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
    async showQuery(iframe: FrameLocator) {
        const createOptionsBtnLocator = iframe.getByRole('button', {name: 'createOptions'});
        await waitForVisibleAndCallback(createOptionsBtnLocator, () => createOptionsBtnLocator.click());

        //Validate the search button has a sub-menu
        await expect(iframe.getByLabel('Search ▼').getByText('Search')).toBeVisible();
        await expect(iframe.getByText('Show Query')).toBeVisible();

        // Click on show query
        await iframe.getByText('Show Query').click();
    }

    /**
     * Validate if the content exists in the results table on the content portlet
     * @param page
     * @param title
     */
    async validateContentExist(page: Page, title: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await iframe.locator('#results_table tbody tr').first().waitFor({ state: 'visible' });
        const secondCell = iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(2)');
        const hasAutomationLink = await secondCell.locator(`a:has-text("${title}")`).count() > 0;

        console.log(`The content with the title ${title} ${hasAutomationLink ? 'exists' : 'does not exist'}`);
        return hasAutomationLink;
    }

    /**
     * Get the content element from the results table on the content portlet
     * @param page
     * @param title
     */
    async getContentElement(page: Page, title: string): Promise<Locator | null> {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await iframe.locator('#results_table tbody tr').first().waitFor({ state: 'visible' });
        const secondCell = iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(2)');
        const element = secondCell.locator(`a:has-text("${title}")`);

        const elementCount = await element.count();
        if (elementCount > 0) {
            return element.first();
        } else {
            console.log(`The content with the title ${title} does not exist`);
            return null;
        }
    }

    /**
     * Edit content on the content portlet
     * @param page
     * @param title
     * @param newTitle
     * @param newBody
     * @param action
     */
    async editContent(page: Page, title: string, newTitle: string, newBody: string, action: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);
        const contentElement = await this.getContentElement(page, title);
        if (contentElement) {
            await contentElement.click();
        }else {
            console.log('Content not found');
            return;
        }
        await this.fillRichTextForm(page, newTitle, newBody, action);
    }

    /**
     * Delete content on the content portlet
     * @param page
     * @param title
     */
    async deleteContent(page: Page, title: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        while (await this.getContentState(page, title) !== null) {
            const contentState = await this.getContentState(page, title);

            if (contentState === 'published') {
                await this.performWorkflowAction(page, title, contentProperties.unpublishWfAction);
            } else if (contentState === 'draft') {
                await this.performWorkflowAction(page, title, contentProperties.archiveWfAction);
                await iframe.getByRole('link', { name: 'Advanced' }).click();
                await iframe.locator('#widget_showingSelect div').first().click();
                const dropDownMenu = iframe.getByRole('option', { name: 'Archived' });
                await waitForVisibleAndCallback(dropDownMenu, () => dropDownMenu.click());
                await page.waitForTimeout(1000)
            } else if (contentState === 'archived') {
                await this.performWorkflowAction(page, title, contentProperties.deleteWfAction);
                return;
            }

            await page.waitForLoadState();
        }
    }

    /**
     * Perform workflow action for some specific content
     * @param page
     * @param title
     * @param action
     */
    async performWorkflowAction(page: Page, title: string, action: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);
        const contentElement = await this.getContentElement(page, title);
        if (contentElement) {
            await contentElement.click({
                button: 'right'
            });
        }
        const actionBtnLocator = iframe.getByRole('menuitem', { name: action });
        await waitForVisibleAndCallback(actionBtnLocator, () => actionBtnLocator.getByText(action).click());
        await expect.soft(iframe.getByText('Workflow executed')).toBeVisible();
        await expect.soft(iframe.getByText('Workflow executed')).toBeHidden();
    }

    async getContentState(page: Page, title: string): Promise<string | null> {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);
        await iframe.locator('#results_table tbody tr').first().waitFor({ state: 'visible' });

        const titleCell = iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(2)');
        const element = titleCell.locator(`a:has-text("${title}")`);
        const elementCount = await element.count();
        if (elementCount > 0) {
            const stateColumn = iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(3)');
            const targetDiv = stateColumn.locator('div#icon');
            return await targetDiv.getAttribute('class');
        } else {
            console.log('Content not found');
            return null;
        }
    }


}







