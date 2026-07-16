import { expect, FrameLocator, Locator, Page } from "@playwright/test";
import {
  contentGeneric,
  iFramesLocators,
  fileAsset,
  pageAsset,
} from "@locators/globalLocators";
import { waitForVisibleAndCallback } from "@utils/utils";
import {
  contentProperties,
  fileAssetContent,
} from "../tests/contentSearch/contentData";

export class ContentPage {
  constructor(private page: Page) {}

  /**
   * Fill the rich text form
   * @param params
   */
  async fillRichTextForm(params: RichTextFormParams) {
    const { title, body, action, newBody, newTitle } = params;
    const dotIframe = this.page.frameLocator(iFramesLocators.dot_iframe);

    await waitForVisibleAndCallback(this.page.getByRole("heading"), () =>
      expect
        .soft(this.page.getByRole("heading"))
        .toContainText(contentGeneric.label),
    );

    if (newTitle) {
      await dotIframe.locator("#title").clear();
      await dotIframe.locator("#title").fill(newTitle);
    }
    if (newBody) {
      await dotIframe.locator("#block-editor-body div").nth(1).clear();
      await dotIframe.locator("#block-editor-body div").nth(1).fill(newBody);
    }
    if (!newTitle && !newBody) {
      await dotIframe.locator("#title").fill(title);
      await dotIframe.locator("#block-editor-body div").nth(1).fill(body);
    }
    if (action) {
      await dotIframe.getByText(action).first().click();
    }
  }

  /**
   * Fill the file asset form
   * @param params
   */
  async fillFileAssetForm(params: FileAssetFormParams) {
    const {
      host,
      editContent,
      title,
      action,
      fromURL,
      binaryFileName,
      binaryFileText,
    } = params;
    const dotIframe = this.page.frameLocator(iFramesLocators.dot_iframe);

    if (binaryFileName && binaryFileText) {
      if (editContent) {
        const editFrame = this.page.frameLocator(
          iFramesLocators.dot_edit_iframe,
        );
        await editFrame.getByRole("button", { name: " Edit" }).click();
        await waitForVisibleAndCallback(
          editFrame.getByLabel("Editor content;Press Alt+F1"),
        );
        const editor = editFrame.getByLabel("Editor content;Press Alt+F1");
        await editor.click(); // Focus on the editor
        await this.page.keyboard.press("Control+A"); // Select all text (Cmd+A for Mac)
        await this.page.keyboard.press("Backspace");
        await editFrame
          .getByLabel("Editor content;Press Alt+F1")
          .fill(fileAssetContent.newFileTextEdited);
        await editFrame.getByRole("button", { name: "Save" }).click();
      } else {
        await waitForVisibleAndCallback(
          this.page.getByRole("heading"),
          async () => {
            await expect
              .soft(this.page.getByRole("heading"))
              .toContainText(fileAsset.label);
          },
        );
        await dotIframe.locator("#HostSelector-hostFolderSelect").fill(host);
        await dotIframe
          .getByRole("button", { name: " Create New File" })
          .click();
        await dotIframe.getByTestId("editor-file-name").fill(binaryFileName);
        await dotIframe
          .getByLabel("Editor content;Press Alt+F1")
          .fill(binaryFileText);
        await dotIframe.getByRole("button", { name: "Save" }).click();
      }
    }

    if (fromURL) {
      await dotIframe
        .getByRole("button", { name: " Import from URL" })
        .click();
      await dotIframe.getByTestId("url-input").fill(fromURL);
      await dotIframe.getByRole("button", { name: " Import" }).click();
      await waitForVisibleAndCallback(
        dotIframe.getByRole("button", { name: " Remove" }),
      );
    }

    await waitForVisibleAndCallback(dotIframe.locator("#title"), async () => {
      await dotIframe.locator("#title").fill(title);
    });

    if (action) {
      await dotIframe.getByText(action).first().click();
    }
  }

  /**
   * Validate the workflow execution and close the modal
   * @param page
   * @param message
   */
  async workflowExecutionValidationAndClose(message: string) {
    const dotIframe = this.page.frameLocator(iFramesLocators.dot_iframe);

    const executionConfirmation = dotIframe.getByText(message);
    await waitForVisibleAndCallback(executionConfirmation, () =>
      expect(executionConfirmation).toBeVisible(),
    );
    await expect(executionConfirmation).toBeHidden();
    //Click on close
    const closeBtnLocator = this.page
      .getByTestId("close-button")
      .getByRole("button");
    await waitForVisibleAndCallback(closeBtnLocator, () =>
      closeBtnLocator.click(),
    );
  }

  /**
   * Add new content action on the content portlet
   * @param page
   * @param typeLocator
   * @param typeString
   */
  async addNewContentAction(typeLocator: string, typeString: string) {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    const structureINodeLocator = iframe.locator("#structure_inode");
    await waitForVisibleAndCallback(structureINodeLocator, () =>
      expect(structureINodeLocator).toBeVisible(),
    );
    await this.selectTypeOnFilter(typeLocator);

    await waitForVisibleAndCallback(
      iframe.locator("#dijit_form_DropDownButton_0"),
      () => iframe.locator("#dijit_form_DropDownButton_0").click(),
    );
    await waitForVisibleAndCallback(iframe.getByLabel("actionPrimaryMenu"));
    await iframe.getByLabel("▼").getByText("Add New Content").click();
    const headingLocator = this.page.getByRole("heading");
    await waitForVisibleAndCallback(headingLocator, () =>
      expect(headingLocator).toHaveText(typeString),
    );
  }

  /**
   * Select content type on filter on the content portlet
   * @param page
   * @param typeLocator
   */
  async selectTypeOnFilter(typeLocator: string) {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    const structureINodeDivLocator = iframe
      .locator("#widget_structure_inode div")
      .first();
    await waitForVisibleAndCallback(structureINodeDivLocator, () =>
      structureINodeDivLocator.click(),
    );

    await waitForVisibleAndCallback(iframe.getByLabel("structure_inode_popup"));

    const typeLocatorByText = iframe.getByText(typeLocator);
    await waitForVisibleAndCallback(typeLocatorByText, () =>
      typeLocatorByText.click(),
    );
  }

  /**
   * Show query on the content portlet
   * @param iframe
   */
  async showQuery(iframe: FrameLocator) {
    const createOptionsBtnLocator = iframe.getByRole("button", {
      name: "createOptions",
    });
    await waitForVisibleAndCallback(createOptionsBtnLocator, () =>
      createOptionsBtnLocator.click(),
    );

    //Validate the search button has a sub-menu
    await expect(
      iframe.getByLabel("Search ▼").getByText("Search"),
    ).toBeVisible();
    await expect(iframe.getByText("Show Query")).toBeVisible();

    // Click on show query
    await iframe.getByText("Show Query").click();
  }

  /**
   * Validate if the content exists in the results table on the content portlet
   * @param page
   * @param text
   */
  async validateContentExist(text: string) {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    await waitForVisibleAndCallback(
      iframe.locator("#results_table tbody tr:nth-of-type(2)"),
    );
    await this.page.waitForTimeout(1000);

    const cells = iframe.locator("#results_table tbody tr:nth-of-type(2) td");
    const cellCount = await cells.count();

    for (let j = 0; j < cellCount; j++) {
      const cell = cells.nth(j);
      const cellText = await cell.textContent();

      if (cellText && cellText.includes(text)) {
        console.log(`The text "${text}" exists in the results table.`);
        return true;
      }
    }

    console.log(`The text "${text}" does not exist in the results table.`);
    return false;
  }

  /**
   * Get the content element from the results table on the content portlet
   * @param page
   * @param title
   */
  async getContentElement(title: string): Promise<Locator | null> {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    await iframe
      .locator("#results_table tbody tr")
      .first()
      .waitFor({ state: "visible" });
    const rows = iframe.locator("#results_table tbody tr");
    const rowCount = await rows.count();

    for (let i = 0; i < rowCount; i++) {
      const secondCell = rows.nth(i).locator("td:nth-of-type(2)");
      const element = secondCell.locator(`a:text("${title}")`);

      if ((await element.count()) > 0) {
        return element.first();
      }
    }
    console.log(`The content with the title ${title} does not exist`);
    return null;
  }

  /**
   * Edit content on the content portlet
   * @param params
   */
  async editContent(params: RichTextFormParams) {
    const { title, action } = params;
    const contentElement = await this.getContentElement(title);
    if (!contentElement) {
      console.log("Content not found");
      return;
    }
    await contentElement.click();
    await this.fillRichTextForm(params);
    if (action) {
      await this.workflowExecutionValidationAndClose("Content saved");
    }
  }

  /**
   * Delete content on the content portlet
   * @param page
   * @param title
   */
  async deleteContent(title: string) {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    while ((await this.getContentState(title)) !== null) {
      const contentState = await this.getContentState(title);

      if (contentState === "published") {
        await this.performWorkflowAction(
          title,
          contentProperties.unpublishWfAction,
        );
      } else if (contentState === "draft") {
        await this.performWorkflowAction(
          title,
          contentProperties.archiveWfAction,
        );
        await iframe.getByRole("link", { name: "Advanced" }).click();
        await iframe.locator("#widget_showingSelect div").first().click();
        const dropDownMenu = iframe.getByRole("option", {
          name: "Archived",
        });
        await waitForVisibleAndCallback(dropDownMenu, () =>
          dropDownMenu.click(),
        );
        await waitForVisibleAndCallback(iframe.locator("#contentWrapper"));
      } else if (contentState === "archived") {
        await this.performWorkflowAction(
          title,
          contentProperties.deleteWfAction,
        );
        return;
      }

      await this.page.waitForLoadState();
    }
  }

  /**
   * Perform workflow action for some specific content
   * @param page
   * @param title
   * @param action
   */
  async performWorkflowAction(title: string, action: string) {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);
    const contentElement = await this.getContentElement(title);
    if (contentElement) {
      await contentElement.click({
        button: "right",
      });
    }
    const actionBtnLocator = iframe.getByRole("menuitem", { name: action });
    await waitForVisibleAndCallback(actionBtnLocator, () =>
      actionBtnLocator.getByText(action).click(),
    );
    const executionConfirmation = iframe.getByText("Workflow executed");
    await waitForVisibleAndCallback(executionConfirmation, () =>
      expect(executionConfirmation).toBeVisible(),
    );
    await waitForVisibleAndCallback(executionConfirmation, () =>
      expect(executionConfirmation).toBeHidden(),
    );
  }

  /**
   * Get the content state from the results table on the content portlet
   * @param page
   * @param title
   */
  async getContentState(title: string): Promise<string | null> {
    const iframe = this.page.frameLocator(iFramesLocators.main_iframe);

    await iframe
      .locator("#results_table tbody tr")
      .first()
      .waitFor({ state: "visible" });
    const rows = iframe.locator("#results_table tbody tr");
    const rowCount = await rows.count();

    for (let i = 0; i < rowCount; i++) {
      const secondCell = rows.nth(i).locator("td:nth-of-type(2)");
      const element = secondCell.locator(`a:text("${title}")`);

      if ((await element.count()) > 0) {
        const stateColumn = rows.nth(i).locator("td:nth-of-type(3)");
        const targetDiv = stateColumn.locator("div#icon");
        return await targetDiv.getAttribute("class");
      }
    }

    console.log("Content not found");
    return null;
  }

  /**
   * Fill the pageAsset form
   * @param params
   */
  async fillPageAssetForm(params: PageAssetFormParams) {
    const {
      title,
      action,
      url,
      host,
      template,
      friendlyName,
      showOnMenu,
      sortOrder,
      cacheTTL,
    } = params;
    const dotIframe = this.page.frameLocator(iFramesLocators.dot_iframe);

    await waitForVisibleAndCallback(this.page.getByRole("heading"), () =>
      expect
        .soft(this.page.getByRole("heading"))
        .toContainText(pageAsset.label),
    );
    await dotIframe.locator("#titleBox").fill(title);

    if (url) await dotIframe.locator("#url").fill(url);
    if (host) {
      await dotIframe.locator("#hostFolder_field div").nth(2).click();
      await dotIframe.getByRole("treeitem", { name: host }).click();
    }
    if (template) {
      await dotIframe.locator("#widget_templateSel div").first().click();
      await dotIframe.getByText(template).click();
    }
    if (friendlyName)
      await dotIframe.locator("#friendlyName").fill(friendlyName);
    if (showOnMenu)
      await dotIframe
        .getByLabel("Content", { exact: true })
        .getByLabel("")
        .check();
    if (sortOrder) await dotIframe.locator("#sortOrder").fill(sortOrder);
    if (cacheTTL)
      await dotIframe.locator("#cachettlbox").fill(cacheTTL.toString());
    if (action) {
      await dotIframe.getByText(action).first().click();
      await expect(dotIframe.locator("#savingContentDialog")).toBeHidden({
        timeout: 10000,
      });
    }
  }

  /**
   * Validate the download of a file
   * @param page
   * @param downloadTriggerSelector
   */
  async validateDownload(downloadTriggerSelector: Locator) {
    // Start waiting for the download event
    const downloadPromise = this.page.waitForEvent("download");

    // Trigger the download
    await downloadTriggerSelector.click();

    // Wait for the download to complete
    const download = await downloadPromise;

    // Assert the download was successful
    const fileName = download.suggestedFilename();
    console.log(`Downloaded file: ${fileName}`);
    expect(fileName).toBeTruthy();
  }
}

/**
 * Base form params
 */
interface BaseFormParams {
  title: string;
  action?: string;
}

interface RichTextFormParams extends BaseFormParams {
  body?: string;
  action?: string;
  newTitle?: string;
  newBody?: string;
}
/**
 * Parameter to fill the file asset form params
 */
interface FileAssetFormParams extends BaseFormParams {
  host: string;
  editContent: boolean;
  fileName?: string;
  fromURL?: string;
  binaryFileName?: string;
  binaryFileText?: string;
}

/**
 * Parameter to fill the page asset form params
 */
interface PageAssetFormParams extends BaseFormParams {
  url?: string;
  host?: string;
  template?: string;
  friendlyName?: string;
  showOnMenu?: boolean;
  sortOrder?: string;
  cacheTTL?: number;
}
