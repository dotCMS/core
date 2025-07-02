import { BreadcrumbComponent } from "@components/breadcrumb.component";
import { SideMenuComponent } from "@components/sideMenu.component";
import { Page } from "@playwright/test";

export class BasePage {
  sideMenu: SideMenuComponent;
  breadcrumb: BreadcrumbComponent;

  constructor(protected page: Page) {
    this.sideMenu = new SideMenuComponent(this.page);
    this.breadcrumb = new BreadcrumbComponent(this.page);
  }
}
