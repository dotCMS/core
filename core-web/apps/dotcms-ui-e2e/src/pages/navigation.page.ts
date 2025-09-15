import { Page } from "@playwright/test";

export class NavigationPage {
    constructor(private page: Page) { }

    /**
     * Navigate to Site Browser page
     */
    async navigateToSiteBrowser() {
        await this.page.goto("/dotAdmin/#/c/site-browser");
        await this.page.waitForLoadState();
    }

    /**
     * Navigate to Pages page
     */
    async navigateToPages() {
        await this.page.goto("/dotAdmin/#/pages");
        await this.page.waitForLoadState();
    }

    /**
     * Navigate to Containers page
     */
    async navigateToContainers() {
        await this.page.goto("/dotAdmin/#/containers");
        await this.page.waitForLoadState();
    }

    /**
     * Navigate to Content Types page
     */
    async navigateToContentTypes() {
        await this.page.goto("/dotAdmin/#/content-types-angular");
        await this.page.waitForLoadState();
    }

    /**
     * Navigate to Template edit page
     */
    async navigateToTemplateEdit(templateId: string) {
        await this.page.goto(`/dotAdmin/#/templates/edit/${templateId}`);
        await this.page.waitForLoadState();
    }
}
