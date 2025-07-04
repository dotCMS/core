import { Page } from "@playwright/test";

export class BreadcrumbComponent {
  constructor(private page: Page) {}

  getBreadcrumb() {
    const breadcrumb = this.page.getByTestId("breadcrumb-crumbs");
    return breadcrumb;
  }

  getTitle() {
    const breadcrumbLast = this.page.getByTestId("breadcrumb-title");
    return breadcrumbLast;
  }
}
