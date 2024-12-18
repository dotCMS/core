import {expect, FrameLocator, Locator, Page} from '@playwright/test';
import {contentGeneric, iFramesLocators, fileAsset } from '../locators/globalLocators';
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
        //Click on action
        await dotIframe.getByText(action).first().click();
    }

    /**
     * Fill the file asset form
     * @param page
     * @param host
     * @param title
     * @param action
     * @param fileName
     * @param fromURL
     * @param newFileName
     * @param newFileText
     */
    async fillFileAssetForm(page: Page, host: string, title: string, action?: string, fileName?: string, fromURL?: string, newFileName?: string, newFileText?: string) {
        const dotIframe = page.frameLocator(iFramesLocators.dot_iframe);

        const headingLocator = page.getByRole('heading');
        await waitForVisibleAndCallback(headingLocator, () => expect.soft(headingLocator).toContainText(fileAsset.label));

        await dotIframe.locator('#HostSelector-hostFolderSelect').fill(host);
        if (newFileName && newFileText) {
            await dotIframe.getByRole('button', {name: ' Create New File'}).click();
            await dotIframe.getByTestId('editor-file-name').fill(newFileName);
            await dotIframe.getByLabel('Editor content;Press Alt+F1').fill(newFileText);
            await dotIframe.getByRole('button', {name: 'Save'}).click();
        } else {
            if (fromURL) {
                await dotIframe.getByRole('button', {name: ' Import from URL'}).click();
                await dotIframe.getByTestId('url-input').fill(fromURL);
                await dotIframe.getByRole('button', {name: ' Import'}).click();
            }
        }
        const titleField = dotIframe.locator('#title');
        await waitForVisibleAndCallback(headingLocator, () => titleField.fill(title));
        if (action) {
            await dotIframe.getByText(action).first().click();
        }

    }

    /**
     * Validate the workflow execution and close the modal
     * @param page
     */
    async workflowExecutionValidationAndClose(page: Page, message: string) {
        const dotIframe = page.frameLocator(iFramesLocators.dot_iframe);

        await expect(dotIframe.getByText(message)).toBeVisible({timeout: 9000});
        await expect(dotIframe.getByText(message)).toBeHidden();
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
        await this.selectTypeOnFilter(page, typeLocator);

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
    async selectTypeOnFilter(page: Page, typeLocator: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        const structureINodeDivLocator = iframe.locator('#widget_structure_inode div').first();
        await waitForVisibleAndCallback(structureINodeDivLocator, () => structureINodeDivLocator.click());
        const typeLocatorByTextLocator = iframe.getByText(typeLocator);
        await waitForVisibleAndCallback(typeLocatorByTextLocator, () => typeLocatorByTextLocator.click());
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

        await waitForVisibleAndCallback(iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(2)'), async () => {
        });
        await page.waitForTimeout(1000)
        const secondCell = iframe.locator('#results_table tbody tr:nth-of-type(2) td:nth-of-type(2)');
        const hasAutomationLink = await secondCell.locator(`a:has-text("${title}")`).count() > 0;

        console.log(`The content with the title ${title} ${hasAutomationLink ? 'exists' : 'does not exist'}`);
        return hasAutomationLink;
    }

    async validateContentExist1(page: Page, text: string) {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await waitForVisibleAndCallback(iframe.locator('#results_table tbody tr:nth-of-type(2)'), async () => {
        });
        await page.waitForTimeout(1000);

        const cells = iframe.locator('#results_table tbody tr:nth-of-type(2) td');
        const cellCount = await cells.count();

        for (let j = 0; j < cellCount; j++) {
            const cell = cells.nth(j);
            const cellText = await cell.textContent();

            if (cellText && cellText.includes(text)) {
                console.log(`The text "${text}" exists in the second row of the table.`);
                return true;
            }
        }

        console.log(`The text "${text}" does not exist in the second row of the table.`);
        return false;
    }

    /**
     * Get the content element from the results table on the content portlet
     * @param page
     * @param title
     */
    async getContentElement(page: Page, title: string): Promise<Locator | null> {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await iframe.locator('#results_table tbody tr').first().waitFor({state: 'visible'});
        const rows = iframe.locator('#results_table tbody tr');
        const rowCount = await rows.count();

        for (let i = 0; i < rowCount; i++) {
            const secondCell = rows.nth(i).locator('td:nth-of-type(2)');
            const element = secondCell.locator(`a:text("${title}")`);

            if (await element.count() > 0) {
                return element.first();
            }
        }
        console.log(`The content with the title ${title} does not exist`);
        return null;
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
        } else {
            console.log('Content not found');
            return;
        }
        await this.fillRichTextForm(page, newTitle, newBody, action);
        await this.workflowExecutionValidationAndClose(page, 'Content saved');
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
                await iframe.getByRole('link', {name: 'Advanced'}).click();
                await iframe.locator('#widget_showingSelect div').first().click();
                const dropDownMenu = iframe.getByRole('option', {name: 'Archived'});
                await waitForVisibleAndCallback(dropDownMenu, () => dropDownMenu.click());
                await page.waitForTimeout(1000);
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
        const actionBtnLocator = iframe.getByRole('menuitem', {name: action});
        await waitForVisibleAndCallback(actionBtnLocator, () => actionBtnLocator.getByText(action).click());
        const executionConfirmation = iframe.getByText('Workflow executed');
        await waitForVisibleAndCallback(executionConfirmation, () => expect(executionConfirmation).toBeVisible());
        await waitForVisibleAndCallback(executionConfirmation, () => expect(executionConfirmation).toBeHidden());
    }

    /**
     * Get the content state from the results table on the content portle
     * @param page
     */
    async getContentState(page: Page, title: string): Promise<string | null> {
        const iframe = page.frameLocator(iFramesLocators.main_iframe);

        await iframe.locator('#results_table tbody tr').first().waitFor({state: 'visible'});
        const rows = iframe.locator('#results_table tbody tr');
        const rowCount = await rows.count();

        for (let i = 0; i < rowCount; i++) {
            const secondCell = rows.nth(i).locator('td:nth-of-type(2)');
            const element = secondCell.locator(`a:text("${title}")`);

            if (await element.count() > 0) {
                const stateColumn = rows.nth(i).locator('td:nth-of-type(3)');
                const targetDiv = stateColumn.locator('div#icon');
                return await targetDiv.getAttribute('class');
            }
        }

        console.log('Content not found');
        return null;
    }

}







