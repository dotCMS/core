import { Page } from "@playwright/test";

export class BreadcrumbComponent {
    constructor(private page: Page) { }

    getBreadcrumb() {
        return this.page.getByTestId("breadcrumb-crumbs");
    }

    getTitle() {
        return this.page.getByTestId("breadcrumb-title");
    }
}
