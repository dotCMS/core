import { APIRequestContext, Page } from "@playwright/test";
import { updateFeatureFlag } from "@requests/updateFeatureFlag";

export class ListingContentTypesPage {
  constructor(
    private page: Page,
    private request: APIRequestContext,
  ) {}

  async goToUrl() {
    const responsePromise = this.page.waitForResponse((response) => {
      return (
        response.status() === 200 &&
        response.url().includes("/api/v1/contenttype") &&
        response.request().method() === "GET"
      );
    });
    await this.page.goto("/dotAdmin/#/content-types-angular");
    await responsePromise;
  }

  async toggleNewContentEditor(boolean: boolean) {
    await updateFeatureFlag(this.request, {
      key: "DOT_FEATURE_FLAG_NEW_EDIT_PAGE",
      value: boolean,
    });
    await updateFeatureFlag(this.request, {
      key: "DOT_CONTENT_EDITOR2_ENABLED",
      value: boolean,
    });
    await updateFeatureFlag(this.request, {
      key: "DOT_CONTENT_EDITOR2_CONTENT_TYPE",
      value: "*",
    });
    await this.page.reload();
  }

  async addNewContentType(name: string) {
    await this.page.getByTestId("dot-action-button").click();
    await this.page
      .locator(".p-menu-overlay")
      .getByLabel("Content")
      .locator("a")
      .click();
    await this.page
      .locator('[data-test-id="content-type__new-content-banner"] div')
      .nth(2)
      .click();

    await this.page.getByLabel("Content Name").fill(name);
    const responsePromise = this.page.waitForResponse((response) => {
      return (
        response.status() === 200 &&
        response.url().includes("/api/v1/contenttype")
      );
    });
    await this.page.getByTestId("dotDialogAcceptAction").click();
    await responsePromise;
  }

  async goToAddNewContentType(contentType: string) {
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

  async deleteContentType(contentType: string) {
    const capitalized =
      contentType.charAt(0).toUpperCase() + contentType.slice(1);

    await this.page
      .getByTestId(`row-${capitalized}`)
      .getByTestId("dot-menu-button")
      .click();
    await this.page
      .locator(".p-menu-overlay")
      .getByLabel("Delete")
      .locator("a")
      .click();
  }
}
