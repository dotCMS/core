import { Locator, Page } from '@playwright/test';

export class BreadcrumbComponent {
    constructor(private page: Page) { }

    getBreadcrumb(): Locator {
        // TODO: Replace with real data-testid from codegen
        // Temporary fallback using more generic selectors
        return this.page.locator('[data-testid="breadcrumb-crumbs"], .breadcrumb-crumbs, .breadcrumb').first();
    }

    getTitle(): Locator {
        // TODO: Replace with real data-testid from codegen
        // Temporary fallback using more generic selectors
        return this.page.locator('[data-testid="breadcrumb-title"], .breadcrumb-title, .page-title, h1').first();
    }
}
