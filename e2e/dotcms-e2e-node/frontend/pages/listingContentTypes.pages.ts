import { Page, expect } from "@playwright/test";
import { MenuEntriesLocators } from "../locators/navigation/menuLocators";
import {
  GroupEntriesLocators,
  ToolEntriesLocators,
} from "../locators/navigation/menuLocators";
import { dotCMSUtils } from "../utils/dotCMSUtils";

export class ListingContentTypesPage {
  private cmsUtils: dotCMSUtils;
  private menuLocators: MenuEntriesLocators;
  private groupsLocators: GroupEntriesLocators;
  private toolsLocators: ToolEntriesLocators;

  constructor(private page: Page) {
    this.cmsUtils = new dotCMSUtils();
    this.menuLocators = new MenuEntriesLocators(this.page);
    this.groupsLocators = new GroupEntriesLocators(this.page);
    this.toolsLocators = new ToolEntriesLocators(this.page);
  }

  async goTo() {
    // Get the username and password from the environment variables
    const username = process.env.USERNAME as string;
    const password = process.env.PASSWORD as string;

    // Login to dotCMS
    await this.cmsUtils.login(this.page, username, password);
    await this.cmsUtils.navigate(
      this.menuLocators.EXPAND,
      this.groupsLocators.CONTENT_MODEL,
      this.toolsLocators.CONTENT_TYPES,
    );

    // Validate the portlet title
    const breadcrumbLocator = this.page.locator("p-breadcrumb");
    await expect(breadcrumbLocator).toContainText("Content Types");
  }

  async addNewContentType(name: string) {
    await this.page.getByRole("button", { name: "" }).click();
    await this.page.getByLabel("Content").locator("a").click();
    await this.page
      .locator('[data-test-id="content-type__new-content-banner"] div')
      .nth(2)
      .click();

    await this.page.getByLabel("Content Name").fill(name);
    await this.page.getByTestId("dotDialogAcceptAction").click();
  }

  async gotToContentTypes() {
    await this.toolsLocators.CONTENT_TYPES.first().click();
  }

  async gotToContentType(contentType: string) {
    const capitalized =
      contentType.charAt(0).toUpperCase() + contentType.slice(1);

    await this.page
      .getByTestId(`row-${capitalized}`)
      .getByRole("link", { name: "View (0)" })
      .click();
    await this.page
      .locator('iframe[name="detailFrame"]')
      .contentFrame()
      .locator("#dijit_form_DropDownButton_0")
      .click();
    await this.page
      .locator('iframe[name="detailFrame"]')
      .contentFrame()
      .getByLabel("▼")
      .getByText("Add New Content")
      .click();
  }
}
